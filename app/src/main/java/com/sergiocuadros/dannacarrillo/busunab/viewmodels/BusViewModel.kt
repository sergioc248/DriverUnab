package com.sergiocuadros.dannacarrillo.busunab.viewmodels

import android.util.Log
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
import java.util.Calendar

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
                            if (seats.isEmpty() && bus.capacity > 0) {
                                // Seats might not be initialized, attempt to initialize them
                                viewModelScope.launch {
                                    val initialized = busRepository.initializeSeatsForBus(bus.plate, bus.capacity)
                                    if (initialized) {
                                        // Re-fetch seats after initialization
                                        busRepository.getSeatsForBus(bus.plate) { newSeats ->
                                            _selectedBusSeats.value = newSeats
                                            _isLoading.value = false
                                        }
                                    } else {
                                        _error.value = "Error al inicializar asientos para el bus ${bus.plate}."
                                        _selectedBusSeats.value = emptyList() // Keep seats empty if init failed
                                        _isLoading.value = false
                                    }
                                }
                            } else {
                                _selectedBusSeats.value = seats
                                _isLoading.value = false // Loading finishes after seats are fetched
                            }
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
                        if (it.number == seatNumber) it.copy(occupied = true) else it
                    }
                    _selectedBusSeats.value = updatedSeats
                    logSeatChangeActivity(busPlate, seatNumber, "seat_occupied", driverId)

                    // Stats Logging for seat occupation (treat as a boarding event for stats)
                    // This part assumes occupySeat is primarily called after a scan/boarding.
                    // If occupySeat can be called independently, this stats logic might need context.
                    val bus = _selectedBus.value
                    if (bus != null && bus.plate == busPlate) {
                        val randomStopId = bus.route.randomOrNull()
                        val totalOccupiedSeats = _selectedBusSeats.value.count { it.occupied }
                        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                        if (randomStopId != null) {
                            viewModelScope.launch {
                                statsRepository.recordStopVisit(randomStopId, busPlate, totalOccupiedSeats)
                            }
                        }
                        viewModelScope.launch {
                            statsRepository.recordPassengerFlow(busPlate, currentHour, totalOccupiedSeats)
                        }
                    }

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
                        if (it.number == seatNumber) it.copy(occupied = false) else it
                    }
                    _selectedBusSeats.value = updatedSeats
                    logSeatChangeActivity(busPlate, seatNumber, "seat_deoccupied", driverId)

                    // Stats Logging for deOccupation (treat as an alighting event for stats)
                    val bus = _selectedBus.value
                    if (bus != null && bus.plate == busPlate) {
                        val randomStopId = bus.route.randomOrNull()
                        // Count occupied seats *after* de-occupation
                        val totalOccupiedSeats = _selectedBusSeats.value.count { it.occupied }
                        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                        if (randomStopId != null) {
                            viewModelScope.launch {
                                statsRepository.recordStopVisit(randomStopId, busPlate, totalOccupiedSeats)
                            }
                        }
                        viewModelScope.launch {
                            statsRepository.recordPassengerFlow(busPlate, currentHour, totalOccupiedSeats)
                        }
                    }

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
            Log.d("BusViewModel", "Starting boarding process for bus $busPlate, driver $driverId, person $verifiedPersonIdentity, seatHint: $seatNumberHint")
            _isLoading.value = true
            _error.value = null

            try {
                val bus = busRepository.getBusByPlateSuspend(busPlate)
                if (bus == null) {
                    Log.e("BusViewModel", "Bus not found: $busPlate")
                    _error.value = "Error al registrar embarque: Bus $busPlate no encontrado."
                    _isLoading.value = false
                    return@launch
                }
                Log.d("BusViewModel", "Found bus: ${bus.plate} with capacity ${bus.capacity}")

                val currentSeats = busRepository.getSeatsForBusOnce(busPlate)
                Log.d("BusViewModel", "Retrieved ${currentSeats.size} seats for bus $busPlate (once)")

                if (currentSeats.isEmpty() && bus.capacity > 0) {
                    // Attempt to initialize if truly empty and capacity exists
                    Log.d("BusViewModel", "Seats empty, attempting initialization for bus ${bus.plate}")
                    val initialized = busRepository.initializeSeatsForBus(bus.plate, bus.capacity)
                    if (initialized) {
                        // Re-fetch after initialization
                        val newSeats = busRepository.getSeatsForBusOnce(busPlate)
                        if (newSeats.isEmpty()) {
                             Log.e("BusViewModel", "Seats still empty after initialization for bus $busPlate")
                            _error.value = "Error al registrar embarque: No se pudieron inicializar/cargar los asientos para el bus $busPlate."
                            _isLoading.value = false
                            return@launch
                        }
                        // Continue with newSeats from here, but the logic below handles it generally
                    } else {
                        Log.e("BusViewModel", "Failed to initialize seats for bus $busPlate")
                        _error.value = "Error al registrar embarque: Falló la inicialización de asientos para el bus $busPlate."
                        _isLoading.value = false
                        return@launch
                    }
                }
                 // Re-fetch currentSeats again in case they were initialized above or to ensure latest state
                val finalCurrentSeats = busRepository.getSeatsForBusOnce(busPlate)
                if (finalCurrentSeats.isEmpty() && bus.capacity > 0) {
                     Log.e("BusViewModel", "No seats configured for bus $busPlate even after potential initialization.")
                    _error.value = "Error al registrar embarque: No hay asientos configurados en el bus $busPlate."
                    _isLoading.value = false
                    return@launch
                }


                var seatToOccupy = seatNumberHint
                Log.d("BusViewModel", "Initial seat to occupy: $seatToOccupy")

                if (seatToOccupy == -1) {
                    Log.d("BusViewModel", "No specific seat requested, finding first available seat")
                    val firstAvailable = finalCurrentSeats
                        .filter { !it.occupied }
                        .minByOrNull { it.seatNumberStr.toIntOrNull() ?: Int.MAX_VALUE }

                    if (firstAvailable == null) {
                        Log.e("BusViewModel", "No available seats found in bus $busPlate")
                        _error.value = "Error al registrar embarque: No hay asientos disponibles en el bus $busPlate."
                        val randomStopId = bus.route.randomOrNull()
                        // Log this attempt even if no seat is available
                        LogRepository.addLog(
                            LogEntry(
                                busId = busPlate,
                                userId = driverId,
                                verifiedPersonIdentity = verifiedPersonIdentity,
                                action = "student_boarded_no_seat_available",
                                stopId = randomStopId,
                                seatNumber = null,
                                verified = true
                            )
                        )
                        _isLoading.value = false
                        return@launch
                    }
                    seatToOccupy = firstAvailable.seatNumberStr.toIntOrNull() ?: -1
                    Log.d("BusViewModel", "Selected first available seat: $seatToOccupy")
                }

                val targetSeat = finalCurrentSeats.find { it.seatNumberStr.toIntOrNull() == seatToOccupy }
                if (targetSeat == null) {
                    Log.e("BusViewModel", "Requested seat $seatToOccupy does not exist in bus $busPlate")
                    _error.value = "Error al registrar embarque: Asiento $seatToOccupy no existe en el bus $busPlate."
                    _isLoading.value = false
                    return@launch
                }

                if (targetSeat.occupied) {
                    Log.e("BusViewModel", "Requested seat $seatToOccupy is already occupied in bus $busPlate")
                    _error.value = "Error al registrar embarque: Asiento $seatToOccupy ya está ocupado."
                    _isLoading.value = false
                    return@launch
                }

                Log.d("BusViewModel", "Attempting to occupy seat $seatToOccupy in bus $busPlate")
                busRepository.updateSeatOccupationInBus(
                    busPlate,
                    seatToOccupy,
                    true
                ) { success ->
                    viewModelScope.launch { // Ensure this callback also launches a coroutine for suspend functions
                        if (success) {
                            Log.d("BusViewModel", "Successfully occupied seat $seatToOccupy in bus $busPlate")
                            val randomStopId = bus.route.randomOrNull()
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

                            // Update UI if this is the currently viewed bus
                            if (_selectedBus.value?.plate == busPlate) {
                                Log.d("BusViewModel", "Updating UI for currently viewed bus (after successful occupation)")
                                // Fetch the latest seats for UI update to be absolutely sure
                                busRepository.getSeatsForBus(bus.plate) { updatedSeatsForUI ->
                                    _selectedBusSeats.value = updatedSeatsForUI
                                }
                            }

                            // Calculate total occupied seats using a fresh fetch for accuracy
                            val latestSeatsAfterOccupation = busRepository.getSeatsForBusOnce(busPlate)
                            val totalOccupiedSeats = latestSeatsAfterOccupation.count { it.occupied }
                            Log.d("BusViewModel", "Total occupied seats after update: $totalOccupiedSeats")

                            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                            if (randomStopId != null) {
                                Log.d("BusViewModel", "Recording stop visit at stop $randomStopId for bus $busPlate")
                                statsRepository.recordStopVisit(randomStopId, busPlate, totalOccupiedSeats)
                            }
                            Log.d("BusViewModel", "Recording passenger flow for hour $currentHour for bus $busPlate")
                            statsRepository.recordPassengerFlow(busPlate, currentHour, totalOccupiedSeats)
                        } else {
                            Log.e("BusViewModel", "Failed to occupy seat $seatToOccupy in bus $busPlate")
                            _error.value = "Error al ocupar asiento $seatToOccupy para el embarque."
                        }
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("BusViewModel", "Exception during boarding process for bus $busPlate: ${e.message}", e)
                _error.value = "Error crítico durante el proceso de embarque: ${e.message}"
                _isLoading.value = false
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

                busRepository.addBus(bus) // Add the bus document

                // After bus is added, initialize its seats
                val seatsInitialized = busRepository.initializeSeatsForBus(bus.plate, bus.capacity)
                if (seatsInitialized) {
                    onComplete(true, null) // Signal success only after seats are also initialized
                } else {
                    // Bus was added, but seats failed. This is a partial success/error state.
                    // For simplicity, treat as error for now. Might need more nuanced handling.
                    onComplete(false, "Bus agregado, pero falló la inicialización de asientos para ${bus.plate}.")
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to add bus or initialize seats")
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