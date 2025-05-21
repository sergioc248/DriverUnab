package com.sergiocuadros.dannacarrillo.busunab.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.sergiocuadros.dannacarrillo.busunab.models.HourlyData
import com.sergiocuadros.dannacarrillo.busunab.models.PassengerFlow
import com.sergiocuadros.dannacarrillo.busunab.models.PassengerFlowData
import com.sergiocuadros.dannacarrillo.busunab.models.Stop
import com.sergiocuadros.dannacarrillo.busunab.models.StopFrequency
import com.sergiocuadros.dannacarrillo.busunab.models.StopVisit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class StatsRepository constructor(
    private val firestore: FirebaseFirestore
) {

    fun getMostFrequentStops(
        limit: Int,
        startDate: Timestamp,
        endDate: Timestamp
    ): Flow<List<StopFrequency>> = flow {
        try {
            val snapshot = firestore.collection("stops")
                .get()
                .await()

            val stops = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Stop::class.java)
            } ?: emptyList()

            val stopFrequencies = stops.map { stop ->
                val visitsInRange = stop.visits.filter { visit ->
                    visit.timestamp >= startDate && visit.timestamp <= endDate
                }
                StopFrequency(
                    stopName = stop.name,
                    frequency = visitsInRange.size
                )
            }
                .sortedByDescending { it.frequency }
                .take(limit)

            emit(stopFrequencies)
        } catch (e: Exception) {
            throw e
        }
    }

    fun getPassengerFlow(
        startDate: Timestamp,
        endDate: Timestamp
    ): Flow<List<PassengerFlowData>> = flow {
        try {
            val snapshot = firestore.collection("passenger_flow")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            val flows = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(PassengerFlow::class.java)
            } ?: emptyList()

            // Aggregate hourly data
            val hourlyData = flows.flatMap { it.hourlyData }
                .groupBy { it.hour }
                .map { (hour, data) ->
                    PassengerFlowData(
                        hour = hour,
                        passengerCount = data.sumOf { it.passengerCount }
                    )
                }
                .sortedBy { it.hour }

            emit(hourlyData)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun recordStopVisit(
        stopId: String,
        busId: String,
        passengerCount: Int
    ) {
        val visit = StopVisit(
            timestamp = Timestamp.now(),
            busId = busId,
            passengerCount = passengerCount
        )

        firestore.collection("stops")
            .document(stopId)
            .update("visits", FieldValue.arrayUnion(visit))
            .await()
    }

    suspend fun recordPassengerFlow(
        busId: String,
        hour: Int,
        passengerCount: Int
    ) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val hourlyData = HourlyData(
            hour = hour,
            passengerCount = passengerCount
        )

        // Try to update existing document for today
        val todayDoc = firestore.collection("passenger_flow")
            .whereEqualTo("busId", busId)
            .whereEqualTo("date", Timestamp(today.time))
            .get()
            .await()
            .documents
            .firstOrNull()

        if (todayDoc != null) {
            // Update existing document
            todayDoc.reference.update(
                "hourlyData",
                FieldValue.arrayUnion(hourlyData)
            ).await()
        } else {
            // Create new document
            val newFlow = PassengerFlow(
                busId = busId,
                date = Timestamp(today.time),
                hourlyData = listOf(hourlyData)
            )
            firestore.collection("passenger_flow")
                .add(newFlow)
                .await()
        }
    }

    // New function to get all stops
    fun getAllStops(): Flow<List<Stop>> = flow {
        try {
            val snapshot = firestore.collection("stops").get().await()
            val stops = snapshot.documents.mapNotNull { it.toObject(Stop::class.java) }
            emit(stops)
        } catch (e: Exception) {
            // Log.e("StatsRepository", "Error fetching all stops", e)
            throw e // Re-throw the exception
        }
    }

    suspend fun addStop(stopName: String, location: GeoPoint? = null): Result<Stop> {
        return try {
            val newStopRef = firestore.collection("stops").document() // Auto-generate ID
            val stop = Stop(
                id = newStopRef.id, 
                name = stopName, 
                location = location
            )
            newStopRef.set(stop).await()
            Result.success(stop)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 