package com.sergiocuadros.dannacarrillo.busunab.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sergiocuadros.dannacarrillo.busunab.models.Bus
import com.sergiocuadros.dannacarrillo.busunab.models.LogEntry
import com.sergiocuadros.dannacarrillo.busunab.models.SeatDocument
import com.sergiocuadros.dannacarrillo.busunab.models.Stop
import com.sergiocuadros.dannacarrillo.busunab.repository.BusRepository
import com.sergiocuadros.dannacarrillo.busunab.repository.LogRepository
import com.sergiocuadros.dannacarrillo.busunab.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BusViewModel : ViewModel() {

    private val busRepository = BusRepository()
    private val statsRepository = StatsRepository(Firebase.firestore)

    private val _buses = MutableStateFlow<List<Bus>>(emptyList())
    val buses: StateFlow<List<Bus>> = _buses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _allStops = MutableStateFlow<List<Stop>>(emptyList())
    val allStops: StateFlow<List<Stop>> = _allStops

    private val _selectedBus = MutableStateFlow<Bus?>(null)
    val selectedBus: StateFlow<Bus?> = _selectedBus

    private val _selectedBusSeats = MutableStateFlow<List<SeatDocument>>(emptyList())
    val selectedBusSeats: StateFlow<List<SeatDocument>> = _selectedBusSeats

    init {
        loadAllStops()
    }

    fun loadBuses(currentUserId: String?, isUserAdmin: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (isUserAdmin) {
                    busRepository.getAllBuses { fetchedBuses ->
                        _buses.value = fetchedBuses
                        _isLoading.value = false
                    }
                } else if (currentUserId != null) {
                    busRepository.getBusesForDriver(currentUserId) { fetchedBuses ->
                        _buses.value = fetchedBuses
                        _isLoading.value = false
                    }
                } else {
                    // Not admin and no user ID, should not happen if logged in as driver
                    _buses.value = emptyList()
                    _isLoading.value = false
                    _error.value = "Usuario no identificado para cargar buses."
                }
            } catch (e: Exception) {
                _error.value = "Failed to load buses: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun loadAllStops() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                statsRepository.getAllStops().collect {
                    _allStops.value = it
                }
            } catch (e: Exception) {
                _error.value = "Error cargando paradas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBusByPlate(plate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedBus.value = null // Clear previous selection
            _selectedBusSeats.value = emptyList() // Clear previous seats
            try {
                busRepository.getBusByPlate(plate) { bus ->
                    _selectedBus.value = bus
                    if (bus != null) {
                        busRepository.getSeatsForBus(bus.plate) { seats ->
                            _selectedBusSeats.value = seats
                            _isLoading.value = false // Loading finishes after seats are fetched
                        }
                    } else {
                        _error.value = "No se encontró el bus con placa $plate."
                        _isLoading.value = false
                    }
                }
                // isLoading is now set to false inside the callbacks
            } catch (e: Exception) {
                _error.value = "Error al cargar el bus: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun logSeatChangeActivity(
        busPlate: String,
        seatNumber: Int,
        action: String,
        driverId: String
    ) {
        viewModelScope.launch {
            val bus = _selectedBus.value
            if (bus != null && bus.plate == busPlate) {
                val randomStopId = bus.route.randomOrNull()
                viewModelScope.launch {
                    LogRepository.addLog(
                        LogEntry(
                            busId = busPlate,
                            userId = driverId,
                            seatNumber = seatNumber,
                            action = action, // "seat_occupied" or "seat_deoccupied"
                            stopId = randomStopId,
                            verifiedPersonIdentity = if (action == "seat_occupied") "N/A" else "", // Or some other placeholder
                            verified = true // Assuming this action is always "verified" in terms of system operation
                        )
                    )
                }
            } else {
                // Fetch bus details if not available or different from selectedBus
                busRepository.getBusByPlate(busPlate) { fetchedBus ->
                    fetchedBus?.let {
                        val randomStopId = it.route.randomOrNull()
                        viewModelScope.launch {
                            LogRepository.addLog(
                                LogEntry(
                                    busId = busPlate,
                                    userId = driverId,
                                    seatNumber = seatNumber,
                                    action = action,
                                    stopId = randomStopId,
                                    verifiedPersonIdentity = if (action == "seat_occupied") "N/A" else "",
                                    verified = true
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun occupySeat(busPlate: String, seatNumber: Int, driverId: String) {
        viewModelScope.launch {
            busRepository.updateSeatOccupationInBus(busPlate, seatNumber, true) { success ->
                if (success) {
                    // Refresh local seat state for immediate UI update
                    val updatedSeats = _selectedBusSeats.value.map {
                        if (it.number == seatNumber) it.copy(isOccupied = true) else it
                    }
                    _selectedBusSeats.value = updatedSeats
                    logSeatChangeActivity(busPlate, seatNumber, "seat_occupied", driverId)
                } else {
                    _error.value = "Error al ocupar asiento $seatNumber en bus $busPlate"
                }
            }
        }
    }

    fun deOccupySeat(busPlate: String, seatNumber: Int, driverId: String) {
        viewModelScope.launch {
            busRepository.updateSeatOccupationInBus(busPlate, seatNumber, false) { success ->
                if (success) {
                    val updatedSeats = _selectedBusSeats.value.map {
                        if (it.number == seatNumber) it.copy(isOccupied = false) else it
                    }
                    _selectedBusSeats.value = updatedSeats
                    logSeatChangeActivity(busPlate, seatNumber, "seat_deoccupied", driverId)
                } else {
                    _error.value = "Error al desocupar asiento $seatNumber en bus $busPlate"
                }
            }
        }
    }

    fun logStudentBoardedAndOccupySeat(
        busPlate: String,
        driverId: String,
        verifiedPersonIdentity: String,
        seatNumberHint: Int
    ) {
        viewModelScope.launch {
            // Ensure we have the latest seat information for the target bus
            // This is important if selectedBus is not the busPlate or seats are stale.
            busRepository.getBusByPlate(busPlate) { bus ->
                if (bus == null) {
                    _error.value = "Error al registrar embarque: Bus $busPlate no encontrado."
                    return@getBusByPlate
                }
                busRepository.getSeatsForBus(busPlate) { currentSeats ->
                    var seatToOccupy = seatNumberHint

                    if (seatToOccupy == -1) { // Find first available seat
                        val firstAvailable = currentSeats.filter { !it.isOccupied }
                            .minByOrNull { it.number } // Find the smallest seat number
                        if (firstAvailable != null) {
                            seatToOccupy = firstAvailable.number
                        } else {
                            _error.value =
                                "Error al registrar embarque: No hay asientos disponibles en el bus $busPlate."
                            // Log only boarding attempt without seat
                            val randomStopId = bus.route.randomOrNull()
                            viewModelScope.launch {
                                LogRepository.addLog(
                                    LogEntry(
                                        busId = busPlate,
                                        userId = driverId,
                                        verifiedPersonIdentity = verifiedPersonIdentity,
                                        action = "student_boarded_no_seat",
                                        stopId = randomStopId,
                                        seatNumber = null, // No seat occupied
                                        verified = true
                                    )
                                )
                            }
                            return@getSeatsForBus
                        }
                    }

                    // Check if the determined seatToOccupy is valid and not already occupied
                    val targetSeat = currentSeats.find { it.number == seatToOccupy }
                    if (targetSeat == null) {
                        _error.value =
                            "Error al registrar embarque: Asiento $seatToOccupy no existe en el bus $busPlate."
                        return@getSeatsForBus
                    }
                    if (targetSeat.isOccupied) {
                        _error.value =
                            "Error al registrar embarque: Asiento $seatToOccupy ya está ocupado."
                        // Optionally, log this as a specific type of event if needed
                        return@getSeatsForBus
                    }

                    // Proceed to occupy the seat
                    busRepository.updateSeatOccupationInBus(
                        busPlate,
                        seatToOccupy,
                        true
                    ) { success ->
                        if (success) {
                            val randomStopId = bus.route.randomOrNull()
                            viewModelScope.launch {
                                LogRepository.addLog(
                                    LogEntry(
                                        busId = busPlate,
                                        userId = driverId,
                                        verifiedPersonIdentity = verifiedPersonIdentity,
                                        action = "student_boarded_and_seated",
                                        stopId = randomStopId,
                                        seatNumber = seatToOccupy,
                                        verified = true
                                    )
                                )
                            }
                            // If this is the currently viewed bus, refresh its seats in the UI
                            if (_selectedBus.value?.plate == busPlate) {
                                val updatedSeats = _selectedBusSeats.value.map {
                                    if (it.number == seatToOccupy) it.copy(isOccupied = true) else it
                                }
                                _selectedBusSeats.value = updatedSeats
                            }
                            // Optionally, trigger a full refresh of selected bus seats if not the current one
                            // or rely on the next loadBusByPlate if the user navigates back.
                        } else {
                            _error.value = "Error al ocupar asiento $seatToOccupy para el embarque."
                        }
                    }
                }
            }
        }
    }

    fun addStop(stopName: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            // Generate random GeoPoint
            val randomLatitude =
                (-90..90).random().toDouble() + Math.random() // Add random fraction
            val randomLongitude =
                (-180..180).random().toDouble() + Math.random() // Add random fraction
            val randomLocation = GeoPoint(randomLatitude, randomLongitude)

            val result = statsRepository.addStop(stopName, randomLocation)
            if (result.isSuccess) {
                loadAllStops() // Refresh the list of stops
                onComplete(true, null)
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Error al agregar parada")
            }
        }
    }

    fun addBus(bus: Bus, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Check if bus plate already exists
                val existingBusDoc = busRepository.getBusDocument(bus.plate)
                if (existingBusDoc != null && existingBusDoc.exists()) {
                    onComplete(false, "Error: La placa '${bus.plate}' ya existe.")
                    return@launch
                }

                busRepository.addBus(bus)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to add bus")
            }
        }
    }

    fun updateBus(bus: Bus, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                busRepository.updateBus(bus) // Assumes bus.plate is the identifier and is not changed
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to update bus")
            }
        }
    }

    fun deleteBus(plate: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                busRepository.deleteBus(plate)
                onComplete(true, null)
                // Optionally, you might want to refresh the bus list here if getAllBuses isn't a snapshot listener
                // or rely on the snapshot listener to update the list automatically.
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to delete bus")
            }
        }
    }

    // Make this suspend as it calls a suspend function in repository
    suspend fun initializeSeatsForBus(busPlate: String, capacity: Int): Boolean {
        return busRepository.initializeSeatsForBus(busPlate, capacity)
    }
} 