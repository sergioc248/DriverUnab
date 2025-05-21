package com.sergiocuadros.dannacarrillo.busunab.models

import com.google.firebase.Timestamp

data class LogEntry(
    val busId: String = "",
    val userId: String = "",
    val verifiedPersonIdentity: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val verified: Boolean = false,
    val action: String = "boarded",
    val seatNumber: Int? = null,
    val stopId: String? = null
)