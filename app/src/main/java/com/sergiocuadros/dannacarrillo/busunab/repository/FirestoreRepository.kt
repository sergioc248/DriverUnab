package com.sergiocuadros.dannacarrillo.busunab.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sergiocuadros.dannacarrillo.busunab.Seat
import com.sergiocuadros.dannacarrillo.busunab.models.Bus

class FirestoreRepository {
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

    // Seat operations
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
}