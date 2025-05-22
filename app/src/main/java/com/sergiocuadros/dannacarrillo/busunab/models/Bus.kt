package com.sergiocuadros.dannacarrillo.busunab.models

data class Bus(
    val plate: String = "",
    val capacity: Int = 36,
    val driverId: String = "",
    val isActive: Boolean = true,
    val route: List<String> = emptyList(),
    val startTime: String = "",
    val seats: List<SeatDocument> = emptyList()
) 