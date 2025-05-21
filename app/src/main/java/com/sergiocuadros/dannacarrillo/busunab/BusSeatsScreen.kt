package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // Icon for deoccupy mode active
import androidx.compose.material.icons.filled.Done // Using this as a fallback for checkmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.sergiocuadros.dannacarrillo.busunab.models.SeatDocument
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.BusViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.AuthViewModel
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.CurrentUserState
import com.sergiocuadros.dannacarrillo.busunab.models.Bus

// Seat data class for UI purposes, derived from SeatDocument and capacity
data class DisplaySeat(
    val number: Int,
    var isOccupied: Boolean = false,
    var isSelectedForAction: Boolean = false // UI state for confirming action
)

@Composable
fun BusSeatsScreen(
    onBusView: () -> Unit,
    onNavigateToScan: (Int) -> Unit,
    busPlate: String,
    driverId: String,
    busViewModel: BusViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val busDetails by busViewModel.selectedBus.collectAsState()
    val busSeatsState by busViewModel.selectedBusSeats.collectAsState()
    val isLoading by busViewModel.isLoading.collectAsState()
    val error by busViewModel.error.collectAsState()
    val currentUserState by authViewModel.currentUserData.collectAsState()

    var isInDeoccupyMode by remember { mutableStateOf(false) }

    LaunchedEffect(busPlate) {
        busViewModel.loadBusByPlate(busPlate) // This will also load seats
    }

    // Derive displayable seats from busDetails (for capacity) and busSeatsState (for occupation)
    val displaySeats = remember(busDetails, busSeatsState) {
        val capacity = busDetails?.capacity ?: 0
        List(capacity) { index ->
            val seatNumber = index + 1
            val seatDoc = busSeatsState.find { it.number == seatNumber }
            DisplaySeat(
                number = seatNumber,
                isOccupied = seatDoc?.isOccupied ?: false
            )
        }
    }

    Scaffold(
        topBar = {
            val currentUserName = (currentUserState as? CurrentUserState.Authenticated)?.user?.name ?: driverId
            TopNavigationBar(
                headerTitle = "Asientos del Bus",
                userName = currentUserName
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = listOf(
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_backarrow),
                        label = "Vista Buses",
                        onClick = onBusView
                    ),
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_user),
                        label = "Escanear Pasajero", 
                        onClick = { 
                            // Pass -1 to indicate first available seat logic
                            onNavigateToScan(-1) 
                        }
                    ),
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isInDeoccupyMode = !isInDeoccupyMode },
                containerColor = if (isInDeoccupyMode) Color(0xFF004B8D) else Color(0xFF00AEEF)
            ) {
                if (isInDeoccupyMode) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Salir Modo Desocupar",
                        tint = Color.White
                    )
                } else {
                    Box(modifier = Modifier.size(28.dp)) { // Increased container for the icon
                        Icon(
                            painter = painterResource(id = R.drawable.icon_armchair),
                            contentDescription = "Activar Modo Desocupar",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize() // Make icon fill the Box
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFE6F7FF))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading && busDetails == null) { // Show loading only if bus details are not yet available
                CircularProgressIndicator()
            } else if (error != null) {
                Text("Error: $error", color = Color.Red)
            } else if (busDetails != null) {
                val bus = busDetails!!

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_bus),
                            contentDescription = "Icono de Bus",
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 16.dp)
                        )

                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Bus ${bus.plate}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00AEEF)
                            )
                            Text(
                                text = "Capacidad: ${bus.capacity} asientos",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            val occupiedCount = displaySeats.count { it.isOccupied }
                            Text(
                                text = "Ocupados: $occupiedCount",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            if(isInDeoccupyMode){
                                 Text(
                                    text = "MODO DESOCUPAR ACTIVO",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF004B8D),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(displaySeats) { seat ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = if (seat.number % 4 == 3 || seat.number % 4 == 0 && seat.number % 2 == 0 ) 16.dp else 0.dp)
                                // Added padding for aisle visualization:
                                // Pad after 2nd seat (index 1 for seat 2) if fixed 4.
                                // The logic for aisle needs to be robust.
                                // Example: if seat.number % 4 == 2, add 16.dp padding to the right
                        ) {
                            SeatItem(
                                seat = seat,
                                onClick = {
                                    if (isInDeoccupyMode) {
                                        if (seat.isOccupied) {
                                            busViewModel.deOccupySeat(bus.plate, seat.number, driverId)
                                        }
                                        // else: do nothing if trying to de-occupy an empty seat
                                    } else {
                                        if (!seat.isOccupied) {
                                           onNavigateToScan(seat.number)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Text("Cargando información del bus...")
            }
        }
    }
}

@Composable
fun SeatItem(
    seat: DisplaySeat,
    onClick: () -> Unit
) {
    val tintColor = when {
        seat.isOccupied -> Color(0xFF004B8D)  // Dark blue
        else -> Color(0xFF00AEEF)  // Blue like top bar
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable(enabled = true) { onClick() }, // Always clickable, logic inside onClick handles mode
        contentAlignment = Alignment.Center
    ) {
        // Seat number in top left
        Text(
            text = seat.number.toString(),
            color = tintColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(2.dp)
        )
        
        // Armchair image centered
        Image(
            painter = painterResource(id = R.drawable.icon_armchair),
            contentDescription = "Asiento ${seat.number}",
            modifier = Modifier
                .size(45.dp)
                .padding(2.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tintColor)
        )
    }
}
