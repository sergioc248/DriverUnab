package com.sergiocuadros.dannacarrillo.busunab.repository

import com.google.firebase.Timestamp
import com.sergiocuadros.dannacarrillo.busunab.models.PassengerFlowData
import com.sergiocuadros.dannacarrillo.busunab.models.StopFrequency
import kotlinx.coroutines.flow.Flow

interface InterfaceStatsRepository {
    /**
     * Get the top N most frequent stops
     * @param limit Number of stops to return
     * @param startDate Start date for filtering visits
     * @param endDate End date for filtering visits
     */
    fun getMostFrequentStops(
        limit: Int = 5,
        startDate: Timestamp,
        endDate: Timestamp
    ): Flow<List<StopFrequency>>

    /**
     * Get passenger flow data for a specific date range
     * @param startDate Start date for filtering data
     * @param endDate End date for filtering data
     */
    fun getPassengerFlow(
        startDate: Timestamp,
        endDate: Timestamp
    ): Flow<List<PassengerFlowData>>

    /**
     * Record a stop visit
     * @param stopId ID of the stop
     * @param busId ID of the bus
     * @param passengerCount Number of passengers
     */
    suspend fun recordStopVisit(
        stopId: String,
        busId: String,
        passengerCount: Int
    )

    /**
     * Record passenger flow data for a specific hour
     * @param busId ID of the bus
     * @param hour Hour of the day (0-23)
     * @param passengerCount Number of passengers
     */
    suspend fun recordPassengerFlow(
        busId: String,
        hour: Int,
        passengerCount: Int
    )
} 