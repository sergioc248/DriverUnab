package com.sergiocuadros.dannacarrillo.busunab.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.sergiocuadros.dannacarrillo.busunab.models.Bus
import com.google.firebase.firestore.DocumentSnapshot
import com.sergiocuadros.dannacarrillo.busunab.models.SeatDocument
import kotlinx.coroutines.tasks.await

class BusRepository {
    private val db = FirebaseFirestore.getInstance()
    private val busesRef = db.collection("buses")
    private val seatsRef = db.collection("seats")

    // Bus operations
    fun addBus(bus: Bus) {
        busesRef.document(bus.plate).set(bus)
    }

    fun deleteBus(plate: String) {
        busesRef.document(plate).delete()
    }

    fun updateBus(bus: Bus) {
        busesRef.document(bus.plate).set(bus)
    }

    fun getBus(plate: String, onBusFetched: (Bus?) -> Unit) {
        busesRef.document(plate).get()
            .addOnSuccessListener { document ->
                val bus = document.toObject(Bus::class.java)
                onBusFetched(bus)
            }
            .addOnFailureListener {
                onBusFetched(null)
            }
    }

    fun getAllBuses(onBusesFetched: (List<Bus>) -> Unit) {
        busesRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val buses = snapshot.toObjects(Bus::class.java)
                onBusesFetched(buses)
            }
        }
    }

    fun getBusesForDriver(driverId: String, onBusesFetched: (List<Bus>) -> Unit) {
        busesRef.whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error, e.g., pass empty list or an error state
                    onBusesFetched(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val buses = snapshot.toObjects(Bus::class.java)
                    onBusesFetched(buses)
                }
            }
    }

    // Function to get a document snapshot, used for existence check
    suspend fun getBusDocument(plate: String): DocumentSnapshot? {
        return try {
            busesRef.document(plate).get().await()
        } catch (e: Exception) {
            // Log error or handle as needed, but for existence check, null indicates error/not found easily
            null
        }
    }

    // Seat operations (Now as subcollection of a bus)

    fun getSeatsForBus(busPlate: String, onResult: (List<SeatDocument>) -> Unit) {
        busesRef.document(busPlate).collection("seats")
            .orderBy("seatNumberStr") // Assuming seatNumberStr is stored as string like "1", "2", etc.
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BusRepository", "Error fetching seats for bus $busPlate", error)
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val seats = snapshot.toObjects(SeatDocument::class.java)
                    onResult(seats)
                } else {
                    onResult(emptyList())
                }
            }
    }

    fun updateSeatOccupationInBus(busPlate: String, seatNumber: Int, isOccupied: Boolean, onComplete: (Boolean) -> Unit) {
        busesRef.document(busPlate).collection("seats").document(seatNumber.toString())
            .update("isOccupied", isOccupied)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                Log.e("BusRepository", "Error updating seat $seatNumber for bus $busPlate", it)
                onComplete(false)
            }
    }

    suspend fun initializeSeatsForBus(busPlate: String, capacity: Int): Boolean {
        return try {
            val seatsCollection = busesRef.document(busPlate).collection("seats")

            val batch = db.batch()
            for (i in 1..capacity) {
                val seatDocRef = seatsCollection.document(i.toString())
                // Only create if it doesn't exist, or overwrite if you want to reset all
                // For simplicity, this overwrites/creates.
                batch.set(seatDocRef, SeatDocument(seatNumberStr = i.toString(), isOccupied = false))
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("BusRepository", "Error initializing seats for bus $busPlate", e)
            false
        }
    }

    // Remove old seat operations
    /*
    fun updateSeatStatus(plate: String, seatNumber: Int, isOccupied: Boolean) {
        seatsRef.document("${plate}_${seatNumber}")
            .set(mapOf(
                "plate" to plate,
                "seatNumber" to seatNumber,
                "isOccupied" to isOccupied
            ))
    }

    fun getBusSeats(plate: String, onSeatsFetched: (List<Seat>) -> Unit) {
        seatsRef.whereEqualTo("plate", plate)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val seats = snapshot.documents.mapNotNull { doc ->
                        val seatNumber = doc.getLong("seatNumber")?.toInt() ?: return@mapNotNull null
                        val isOccupied = doc.getBoolean("isOccupied") ?: false
                        Seat(number = seatNumber, isOccupied = isOccupied)
                    }
                    onSeatsFetched(seats)
                }
            }
    }

    fun clearBusSeats(plate: String) {
        seatsRef.whereEqualTo("plate", plate)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
            }
    }
    */

    fun getBusByPlate(plate: String, onBusFetched: (Bus?) -> Unit) {
        busesRef.document(plate).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val bus = document.toObject(Bus::class.java)
                    onBusFetched(bus)
                } else {
                    onBusFetched(null)
                }
            }
            .addOnFailureListener {
                // Log error or handle as needed
                onBusFetched(null)
            }
    }
}