package com.sergiocuadros.dannacarrillo.busunab.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Stop(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val location: GeoPoint? = null,
    val visits: List<StopVisit> = emptyList()
)

data class StopVisit(
    val timestamp: Timestamp = Timestamp.now(),
    val busId: String = "",
    val passengerCount: Int = 0
)

data class PassengerFlow(
    @DocumentId
    val id: String = "",
    val busId: String = "",
    val date: Timestamp = Timestamp.now(),
    val hourlyData: List<HourlyData> = emptyList()
)

data class HourlyData(
    val hour: Int = 0,
    val passengerCount: Int = 0
)

// UI Models
data class StopFrequency(
    val stopName: String,
    val frequency: Int
)

data class PassengerFlowData(
    val hour: Int,
    val passengerCount: Int
) 