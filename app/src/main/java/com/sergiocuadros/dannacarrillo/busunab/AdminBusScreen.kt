package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.BusViewModel
import com.sergiocuadros.dannacarrillo.busunab.models.Bus
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.LaunchedEffect
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.AuthViewModel
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.CurrentUserState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private val LightBlue = Color(0xFFE5F7FF)
private val DarkBlue  = Color(0xFF009FE3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusManagementScreen(
    onNavigateToStats: () -> Unit,
    onLogout: () -> Unit,
    busViewModel: BusViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val busListFromFirestore by busViewModel.buses.collectAsState()
    val isLoading by busViewModel.isLoading.collectAsState()
    val error by busViewModel.error.collectAsState()

    // State for Add Bus Dialog
    var showAddBusDialog by remember { mutableStateOf(false) }
    var newBusPlate by remember { mutableStateOf("") }
    var newBusRoute by remember { mutableStateOf("") }
    var newBusCapacity by remember { mutableStateOf("") }
    var newBusStartTime by remember { mutableStateOf("") }
    var addBusError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // State for Delete Bus Dialog
    var busToDeletePlate by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // State for Delete Mode
    var isInDeleteMode by remember { mutableStateOf(false) }

    // State for Add Stop Dialog
    var showAddStopDialog by remember { mutableStateOf(false) }
    var newStopName by remember { mutableStateOf("") }
    var addStopError by remember { mutableStateOf<String?>(null) }

    // State for Editing Bus
    var busToEdit by remember { mutableStateOf<Bus?>(null) }
    var showEditBusDialog by remember { mutableStateOf(false) }

    // Driver List for dropdown
    val driverList by authViewModel.driverList.collectAsState()
    var selectedDriverIdForForm by remember { mutableStateOf<String?>(null) }
    var driverDropdownExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Collect current user data to display name in TopBar
    val currentUserState by authViewModel.currentUserData.collectAsState()

    // Collect all stops from ViewModel
    val allStops by busViewModel.allStops.collectAsState()
    var showStopSelectorDialog by remember { mutableStateOf(false) }
    var selectedStopIdsForNewBus by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        busViewModel.loadBuses(currentUserId = null, isUserAdmin = true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val currentUserName = (currentUserState as? CurrentUserState.Authenticated)?.user?.name ?: "Admin"
            TopNavigationBar(
                headerTitle = "Panel de Administración",
                userName = currentUserName,
            )
        },
        bottomBar = {
            BottomNavigationBar(items = listOf(
                BottomNavItem.PainterIcon(
                    painter = painterResource(R.drawable.icon_log_out),
                    label = stringResource(R.string.log_out_icon_text),
                    onClick = onLogout
                ),
                BottomNavItem.VectorIcon(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Estadísticas",
                    onClick = onNavigateToStats
                ),
            ))
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { 
                        if (isInDeleteMode) {
                            isInDeleteMode = false // Exit delete mode
                        } else {
                            showAddBusDialog = true // Open add bus dialog
                        }
                    },
                    containerColor = DarkBlue,
                    modifier = Modifier.padding(bottom = if (isInDeleteMode) 8.dp else 0.dp) // Add padding if second FAB is visible
                ) {
                    Icon(
                        imageVector = if (isInDeleteMode) Icons.Filled.Done else Icons.Filled.Add,
                        contentDescription = if (isInDeleteMode) "Done Deleting" else "Add Bus",
                        tint = Color.White
                    )
                }
                if (!isInDeleteMode) { // Show Delete Mode FAB only if not already in delete mode
                    FloatingActionButton(
                        onClick = { isInDeleteMode = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer, // A different color for delete mode toggle
                        modifier = Modifier.padding(top=8.dp) // Spacing between FABs
                    ) {
                        Icon(Icons.Filled.Delete, "Toggle Delete Mode", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                if (!isInDeleteMode) { // Add Stop FAB, only if not in delete mode
                     FloatingActionButton(
                        onClick = { showAddStopDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top=8.dp)
                    ) {
                        Icon(Icons.Filled.Add, "Add Stop", tint = MaterialTheme.colorScheme.onSecondaryContainer) // Could use a more specific stop icon
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(LightBlue)
                .padding(16.dp) // Outer margin for the table
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Table Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Gestión de Buses",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkBlue,
                )
                Text(
                    text = "Agrega, edita o elimina buses",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Header row
                TableRow(
                    cells = listOf("Ruta", "Placa", "Capacidad", "Inicio"), 
                    isHeader = true
                )

                // Data rows
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Text(
                        text = "Error: $error",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    )
                } else if (busListFromFirestore.isEmpty()) {
                    Text(
                        text = "No hay buses registrados.",
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    // Fill remaining space with empty rows if list is empty after loading
                    repeat(7) {
                        TableRow(cells = listOf("", "", "", "")) // 4 empty strings
                    }
                } else {
                    busListFromFirestore.forEach { bus -> 
                        // Convert route IDs to names for display
                        val routeNames = bus.route.mapNotNull { stopId ->
                            allStops.find { it.id == stopId }?.name
                        }.joinToString(", ")
                        TableRow(
                            cells = listOf(routeNames, bus.plate, bus.capacity.toString(), bus.startTime),
                            actionIcon = if (isInDeleteMode) { 
                                {
                                    IconButton(onClick = {
                                        busToDeletePlate = bus.plate
                                        showDeleteConfirmDialog = true
                                    }) {
                                        Icon(Icons.Filled.Delete, "Eliminar Bus", tint = Color.Red)
                                    }
                                }
                            } else { // Show Edit button if not in delete mode
                                {
                                    IconButton(onClick = {
                                        busToEdit = bus
                                        // Pre-fill states for the edit dialog
                                        newBusPlate = bus.plate // Plate likely not editable, but shown
                                        selectedStopIdsForNewBus = bus.route
                                        newBusCapacity = bus.capacity.toString()
                                        newBusStartTime = bus.startTime
                                        selectedDriverIdForForm = bus.driverId
                                        addBusError = null // Clear any previous add/edit errors
                                        showEditBusDialog = true
                                    }) {
                                        Icon(Icons.Filled.Edit, "Editar Bus", tint = DarkBlue)
                                    }
                                }
                            }
                        )
                    }
                    // Fill remaining space with empty rows
                    val emptyRowCount = 7 - busListFromFirestore.size
                    if (emptyRowCount > 0) {
                        repeat(emptyRowCount) {
                            TableRow(cells = listOf("", "", "", "")) // 4 empty strings
                        }
                    }
                }
            }
        }

        if (showAddBusDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddBusDialog = false 
                    // Reset add form states
                    newBusPlate = ""
                    selectedStopIdsForNewBus = emptyList()
                    newBusCapacity = ""
                    newBusStartTime = ""
                    selectedDriverIdForForm = null
                    addBusError = null
                },
                title = { Text("Agregar Nuevo Bus") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) { // Make dialog content scrollable
                        TextField(
                            value = newBusPlate,
                            onValueChange = { newBusPlate = it },
                            label = { Text("Placa") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Button to open stop selector
                        Button(onClick = { showStopSelectorDialog = true }) {
                            Text(
                                if (selectedStopIdsForNewBus.isEmpty()) "Seleccionar Paradas"
                                else selectedStopIdsForNewBus.mapNotNull { stopId ->
                                    allStops.find { it.id == stopId }?.name
                                }.joinToString(", ")
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = newBusCapacity,
                            onValueChange = { newBusCapacity = it },
                            label = { Text("Capacidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = newBusStartTime,
                            onValueChange = { newBusStartTime = it },
                            label = { Text("Hora de Inicio (e.g., 6:00 am)") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Driver Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = driverDropdownExpanded,
                            onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded }
                        ) {
                            TextField(
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                readOnly = true,
                                value = driverList.find { it.id == selectedDriverIdForForm }?.name ?: "Seleccionar Conductor",
                                onValueChange = {}, // Not directly changed
                                label = { Text("Conductor Asignado") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverDropdownExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = driverDropdownExpanded,
                                onDismissRequest = { driverDropdownExpanded = false }
                            ) {
                                driverList.forEach { driver ->
                                    DropdownMenuItem(
                                        text = { Text(driver.name) },
                                        onClick = {
                                            selectedDriverIdForForm = driver.id
                                            driverDropdownExpanded = false
                                        }
                                    )
                                }
                                if (driverList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hay conductores disponibles") },
                                        enabled = false,
                                        onClick = {}
                                    )
                                }
                            }
                        }

                        addBusError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val capacityInt = newBusCapacity.toIntOrNull()
                            if (newBusPlate.isBlank() || selectedStopIdsForNewBus.isEmpty() || capacityInt == null || newBusStartTime.isBlank()) {
                                addBusError = "Todos los campos son requeridos, incluyendo paradas."
                                return@TextButton
                            }
                            if (capacityInt <= 0) {
                                addBusError = "La capacidad debe ser mayor a 0."
                                return@TextButton
                            }
                            addBusError = null
                            val newBus = Bus(
                                plate = newBusPlate.uppercase(),
                                route = selectedStopIdsForNewBus, 
                                capacity = capacityInt,
                                startTime = newBusStartTime,
                                driverId = selectedDriverIdForForm ?: "", // Assign selected driver
                                isActive = true 
                            )
                            busViewModel.addBus(newBus) { success, errorMsg ->
                                if (success) {
                                    showAddBusDialog = false
                                    // Clear fields are now part of onDismissRequest
                                    Toast.makeText(context, "Bus agregado: ${newBus.plate}", Toast.LENGTH_SHORT).show()
                                    // Initialize seats for the new bus
                                    coroutineScope.launch { // ViewModel functions can be called from coroutineScope
                                        val initSuccess = busViewModel.initializeSeatsForBus(newBus.plate, newBus.capacity)
                                        if (!initSuccess) {
                                            Toast.makeText(context, "Error inicializando asientos para ${newBus.plate}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    addBusError = errorMsg ?: "Error al agregar bus."
                                }
                            }
                        }
                    ) { Text("Agregar") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddBusDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showDeleteConfirmDialog && busToDeletePlate != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    busToDeletePlate = null
                },
                title = { Text("Confirmar Eliminación") },
                text = { Text("¿Estás seguro de que quieres eliminar el bus con placa ${busToDeletePlate}? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            busToDeletePlate?.let {
                                busViewModel.deleteBus(it) { success, errorMsg ->
                                    if (success) {
                                        Toast.makeText(context, "Bus $it eliminado", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error al eliminar bus: ${errorMsg ?: "Desconocido"}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            showDeleteConfirmDialog = false
                            busToDeletePlate = null
                        }
                    ) { Text("Eliminar", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog = false
                        busToDeletePlate = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showStopSelectorDialog) {
            AlertDialog(
                onDismissRequest = { showStopSelectorDialog = false },
                title = { Text("Seleccionar Paradas para la Ruta") },
                text = {
                    if (allStops.isEmpty()) {
                        Text("No hay paradas disponibles para seleccionar. Agregue paradas primero.")
                    } else {
                        LazyColumn {
                            items(allStops.sortedBy { it.name }) { stop ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val currentSelected = selectedStopIdsForNewBus.toMutableList()
                                            if (currentSelected.contains(stop.id)) {
                                                currentSelected.remove(stop.id)
                                            } else {
                                                currentSelected.add(stop.id)
                                            }
                                            selectedStopIdsForNewBus = currentSelected
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedStopIdsForNewBus.contains(stop.id),
                                        onCheckedChange = { checked ->
                                            val currentSelected = selectedStopIdsForNewBus.toMutableList()
                                            if (checked) {
                                                if (!currentSelected.contains(stop.id)) currentSelected.add(stop.id)
                                            } else {
                                                currentSelected.remove(stop.id)
                                            }
                                            selectedStopIdsForNewBus = currentSelected
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stop.name) // Assuming Stop has a 'name' field
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStopSelectorDialog = false }) {
                        Text("Aceptar")
                    }
                },
                dismissButton = null // No dismiss for this one, only confirm
            )
        }

        if (showAddStopDialog) {
            AlertDialog(
                onDismissRequest = { showAddStopDialog = false },
                title = { Text("Agregar Nueva Parada") },
                text = {
                    Column {
                        TextField(
                            value = newStopName,
                            onValueChange = { newStopName = it },
                            label = { Text("Nombre de la Parada") },
                            singleLine = true
                        )
                        addStopError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newStopName.isBlank()) {
                                addStopError = "El nombre de la parada es requerido."
                                return@TextButton
                            }
                            addStopError = null
                            busViewModel.addStop(newStopName) { success, errorMsg ->
                                if (success) {
                                    showAddStopDialog = false
                                    newStopName = ""
                                    Toast.makeText(context, "Parada agregada: $newStopName", Toast.LENGTH_SHORT).show()
                                } else {
                                    addStopError = errorMsg ?: "Error al agregar parada."
                                }
                            }
                        }
                    ) { Text("Agregar") }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddStopDialog = false 
                        newStopName = ""
                        addStopError = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Edit Bus Dialog (similar to Add Bus Dialog, but for editing)
        if (showEditBusDialog && busToEdit != null) {
            val currentEditingBus = busToEdit!! 
            // Initialize selectedDriverIdForForm when dialog opens for editing
            LaunchedEffect(busToEdit) {
                if (busToEdit != null) {
                    selectedDriverIdForForm = busToEdit?.driverId?.takeIf { it.isNotBlank() }
                    // pre-fill other form states is already done when setting busToEdit
                }
            }
            AlertDialog(
                onDismissRequest = { 
                    showEditBusDialog = false 
                    busToEdit = null
                    newBusPlate = "" 
                    selectedStopIdsForNewBus = emptyList() 
                    newBusCapacity = "" 
                    newBusStartTime = "" 
                    selectedDriverIdForForm = null // Reset driver selection
                    addBusError = null
                },
                title = { Text("Editar Bus: ${currentEditingBus.plate}") }, 
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) { // Make dialog content scrollable
                        Text("Placa: ${currentEditingBus.plate}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(onClick = { showStopSelectorDialog = true }) {
                            Text(
                                if (selectedStopIdsForNewBus.isEmpty()) "Seleccionar Paradas"
                                else selectedStopIdsForNewBus.mapNotNull { stopId ->
                                    allStops.find { it.id == stopId }?.name
                                }.joinToString(", ")
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = newBusCapacity, // Already pre-filled
                            onValueChange = { newBusCapacity = it },
                            label = { Text("Capacidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = newBusStartTime, 
                            onValueChange = { newBusStartTime = it },
                            label = { Text("Hora de Inicio (e.g., 6:00 am)") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Driver Selector Dropdown for Edit
                        ExposedDropdownMenuBox(
                            expanded = driverDropdownExpanded,
                            onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded }
                        ) {
                            TextField(
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                readOnly = true,
                                value = driverList.find { it.id == selectedDriverIdForForm }?.name ?: "Seleccionar Conductor",
                                onValueChange = {},
                                label = { Text("Conductor Asignado") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverDropdownExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = driverDropdownExpanded,
                                onDismissRequest = { driverDropdownExpanded = false }
                            ) {
                                driverList.forEach { driver ->
                                    DropdownMenuItem(
                                        text = { Text(driver.name) },
                                        onClick = {
                                            selectedDriverIdForForm = driver.id
                                            driverDropdownExpanded = false
                                        }
                                    )
                                }
                                if (driverList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hay conductores disponibles") },
                                        enabled = false,
                                        onClick = {}
                                    )
                                }
                            }
                        }

                        addBusError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val capacityInt = newBusCapacity.toIntOrNull()
                            if (selectedStopIdsForNewBus.isEmpty() || capacityInt == null || newBusStartTime.isBlank()) {
                                addBusError = "Paradas, capacidad y hora de inicio son requeridos."
                                return@TextButton
                            }
                            if (capacityInt <= 0) {
                                addBusError = "La capacidad debe ser mayor a 0."
                                return@TextButton
                            }
                            addBusError = null
                            val updatedBus = currentEditingBus.copy(
                                route = selectedStopIdsForNewBus,
                                capacity = capacityInt,
                                startTime = newBusStartTime,
                                driverId = selectedDriverIdForForm ?: "" // Update driverId
                            )
                            busViewModel.updateBus(updatedBus) { success, errorMsg -> 
                                if (success) {
                                    showEditBusDialog = false
                                    busToEdit = null
                                    Toast.makeText(context, "Bus ${updatedBus.plate} actualizado", Toast.LENGTH_SHORT).show()
                                    // If capacity changed, re-initialize seats
                                    if (currentEditingBus.capacity != updatedBus.capacity) {
                                        coroutineScope.launch {
                                            val initSuccess = busViewModel.initializeSeatsForBus(updatedBus.plate, updatedBus.capacity)
                                            if (!initSuccess) {
                                                Toast.makeText(context, "Error reinicializando asientos para ${updatedBus.plate}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } else {
                                    addBusError = errorMsg ?: "Error al actualizar bus."
                                }
                            }
                        }
                    ) { Text("Guardar Cambios") }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showEditBusDialog = false
                        busToEdit = null
                        // Clear form fields
                        newBusPlate = "" 
                        selectedStopIdsForNewBus = emptyList() 
                        newBusCapacity = "" 
                        newBusStartTime = "" 
                        selectedDriverIdForForm = null
                        addBusError = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun TableRow(cells: List<String>, isHeader: Boolean = false, actionIcon: @Composable (() -> Unit)? = null) {
    val cellColor = if (isHeader) DarkBlue else Color.Black
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
    // Adjusted weights: Ruta, Placa, Capacidad, Inicio (action icon will be separate or overlay)
    val columnWeights = if (actionIcon != null && !isHeader) listOf(2f, 2f, 1.5f, 1.5f, 0.5f) else listOf(2f, 2f, 1.5f, 2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        cells.forEachIndexed { index, cell ->
            Box(
                modifier = Modifier
                    .weight(columnWeights.getOrElse(index) { 1f })
                    .fillMaxHeight()
                    .padding(4.dp)
            ) {
                Text(
                    text = cell,
                    fontSize = 14.sp,
                    color = cellColor,
                    fontWeight = fontWeight,
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)
                )
            }

            // Draw vertical divider (except after last column)
            if (index != cells.lastIndex) {
                VerticalDivider(
                    color = DarkBlue,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
            } 
        }
        // Add action icon at the end of the row if provided and not a header row
        if (actionIcon != null && !isHeader) {
            Box(modifier = Modifier.weight(columnWeights.last()).fillMaxHeight(), contentAlignment = Alignment.Center) {
                actionIcon()
            }
        } else if (isHeader && cells.size == 4) { // Add an empty weighted box for alignment if headers don't have action
             Box(modifier = Modifier.weight(0.5f).fillMaxHeight()) {}
        }
    }

    // Draw horizontal divider
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = DarkBlue
    )
}


@Composable
fun TableHeader(title: String, modifier: Modifier) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = modifier.padding(8.dp)
    )
}

@Composable
fun TableCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontSize = 14.sp,
        modifier = modifier.padding(8.dp)
    )
}

