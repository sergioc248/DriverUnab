    package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

data class Seat(
    val number: Int,
    var isOccupied: Boolean = false,
    var isSelected: Boolean = false
)

@Composable
fun BusSeatsScreen(
    onNavigateToStats: () -> Unit,
    onLogout: () -> Unit,
    onBusView: () -> Unit,
    onNavigateToScan: () -> Unit,
    busPlate: String,
    onSeatSelected: (Seat) -> Unit = {}
) {
    // TODO: Fetch bus data from ViewModel/Repository using busPlate
    val capacity = 36 // This should come from the fetched bus data
    var selectedMode by remember { mutableIntStateOf(0) }

    val seats = remember {
        List(capacity) { index -> Seat(number = index + 1) }
    }

    Scaffold(
        topBar = {
            TopNavigationBar(
                headerTitle = "Asientos del Bus",
                userName = "Conductor1"
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = listOf(
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_backarrow),
                        label = "Vista Buses",
                        modifier = Modifier.clickable { selectedMode = 0 },
                        onClick = onBusView
                    ),
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_user),
                        label = "Reconocimiento Facial",
                        modifier = Modifier.clickable { selectedMode = 0 },
                        onClick = onNavigateToScan
                    ),
                )
            )
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
            // Bus info header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bus $busPlate",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00AEEF)
                    )
                    Text(
                        text = "Capacidad: $capacity asientos",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }

            // Seats grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(seats) { seat ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = if (seat.number % 4 == 3) 16.dp else 0.dp)
                    ) {
                        SeatItem(
                            seat = seat,
                            onClick = { onSeatSelected(seat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeatItem(
    seat: Seat,
    onClick: () -> Unit
) {
    val tintColor = when {
        seat.isSelected -> Color.Gray
        seat.isOccupied -> Color(0xFF004B8D)  // Dark blue
        else -> Color(0xFF00AEEF)  // Blue like top bar
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable(enabled = !seat.isOccupied) { onClick() },
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
