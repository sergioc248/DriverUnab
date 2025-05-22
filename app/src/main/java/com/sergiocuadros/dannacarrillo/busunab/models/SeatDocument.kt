package com.sergiocuadros.dannacarrillo.busunab.models

import com.google.firebase.firestore.DocumentId

data class SeatDocument(
    @DocumentId
    val seatNumberStr: String = "", // Firestore document ID will be "1", "2", etc.
    val occupied: Boolean = false,
) {
    // Helper to get integer seat number, assuming ID is the number
    val number: Int
        get() = seatNumberStr.toIntOrNull() ?: 0
} 