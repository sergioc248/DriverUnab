package com.sergiocuadros.dannacarrillo.busunab.models

data class Bus(
    val id: String = "",
    val plate: String = "",
    val capacity: Int = 36,
    val driverId: String = "",
    val isActive: Boolean = true,
    val route: String = "",
    val startTime: String = ""
) 