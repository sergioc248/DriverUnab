package com.sergiocuadros.dannacarrillo.busunab.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sergiocuadros.dannacarrillo.busunab.models.PassengerFlowData
import com.sergiocuadros.dannacarrillo.busunab.models.StopFrequency
import com.sergiocuadros.dannacarrillo.busunab.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import android.util.Log

class StatsViewModel constructor(
) : ViewModel() {

    // Create the repository instance internally
    private val firestore = Firebase.firestore
    private val statsRepository: StatsRepository = StatsRepository(firestore)

    private val _stopFrequencies = MutableStateFlow<List<StopFrequency>>(emptyList())
    val stopFrequencies: StateFlow<List<StopFrequency>> = _stopFrequencies

    private val _passengerFlow = MutableStateFlow<List<PassengerFlowData>>(emptyList())
    val passengerFlow: StateFlow<List<PassengerFlowData>> = _passengerFlow

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Get data for the last 7 days
                val endDate = Timestamp.Companion.now()
                val startDate = Timestamp(Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -7)
                }.time)

                // Load stop frequencies
                statsRepository.getMostFrequentStops(
                    limit = 5,
                    startDate = startDate,
                    endDate = endDate
                ).catch { e ->
                    _error.value = "Error loading stop frequencies: ${e.message}"
                }.collect { frequencies ->
                    _stopFrequencies.value = frequencies
                }

                // Load passenger flow
                statsRepository.getPassengerFlow(
                    startDate = startDate,
                    endDate = endDate
                ).catch { e ->
                    _error.value = "Error loading passenger flow: ${e.message}"
                }.collect { flow ->
                    _passengerFlow.value = flow
                }
            } catch (e: Exception) {
                _error.value = "Error loading statistics: ${e.message}"
                Log.e("StatsViewModel", "Error in loadStats: ", e)
            } finally {
                _isLoading.value = false
                Log.d("StatsViewModel", "loadStats finally block executed. isLoading set to false.")
            }
        }
    }

    fun recordStopVisit(stopId: String, busId: String, passengerCount: Int) {
        viewModelScope.launch {
            try {
                statsRepository.recordStopVisit(stopId, busId, passengerCount)
            } catch (e: Exception) {
                _error.value = "Error recording stop visit: ${e.message}"
            }
        }
    }

    fun recordPassengerFlow(busId: String, hour: Int, passengerCount: Int) {
        viewModelScope.launch {
            try {
                statsRepository.recordPassengerFlow(busId, hour, passengerCount)
            } catch (e: Exception) {
                _error.value = "Error recording passenger flow: ${e.message}"
            }
        }
    }
}