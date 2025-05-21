package com.sergiocuadros.dannacarrillo.busunab.models

import com.google.firebase.firestore.DocumentId

data class SeatDocument(
    @DocumentId
    val seatNumberStr: String = "", // Firestore document ID will be "1", "2", etc.
    val isOccupied: Boolean = false,
    // val lastOccupiedBy: String? = null, // Optional: If you want to track who last occupied it
    // val lastOccupiedTimestamp: Timestamp? = null // Optional
) {
    // Helper to get integer seat number, assuming ID is the number
    val number: Int
        get() = seatNumberStr.toIntOrNull() ?: 0
} 