package com.sergiocuadros.dannacarrillo.busunab.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.sergiocuadros.dannacarrillo.busunab.models.PassengerFlowData
import com.sergiocuadros.dannacarrillo.busunab.models.StopFrequency
import com.sergiocuadros.dannacarrillo.busunab.repository.InterfaceStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val interfaceStatsRepository: InterfaceStatsRepository
) : ViewModel() {

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
                val endDate = Timestamp.now()
                val startDate = Timestamp(Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -7)
                }.time)

                // Load stop frequencies
                interfaceStatsRepository.getMostFrequentStops(
                    limit = 5,
                    startDate = startDate,
                    endDate = endDate
                ).catch { e ->
                    _error.value = "Error loading stop frequencies: ${e.message}"
                }.collect { frequencies ->
                    _stopFrequencies.value = frequencies
                }

                // Load passenger flow
                interfaceStatsRepository.getPassengerFlow(
                    startDate = startDate,
                    endDate = endDate
                ).catch { e ->
                    _error.value = "Error loading passenger flow: ${e.message}"
                }.collect { flow ->
                    _passengerFlow.value = flow
                }
            } catch (e: Exception) {
                _error.value = "Error loading statistics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun recordStopVisit(stopId: String, busId: String, passengerCount: Int) {
        viewModelScope.launch {
            try {
                interfaceStatsRepository.recordStopVisit(stopId, busId, passengerCount)
            } catch (e: Exception) {
                _error.value = "Error recording stop visit: ${e.message}"
            }
        }
    }

    fun recordPassengerFlow(busId: String, hour: Int, passengerCount: Int) {
        viewModelScope.launch {
            try {
                interfaceStatsRepository.recordPassengerFlow(busId, hour, passengerCount)
            } catch (e: Exception) {
                _error.value = "Error recording passenger flow: ${e.message}"
            }
        }
    }
} 