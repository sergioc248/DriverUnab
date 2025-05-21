package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import androidx.compose.foundation.clickable
import com.sergiocuadros.dannacarrillo.busunab.models.Bus
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.BusViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sergiocuadros.dannacarrillo.busunab.models.Stop
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.AuthViewModel
import com.sergiocuadros.dannacarrillo.busunab.models.UserRole
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.CurrentUserState

@Composable
fun BusViewScreen(
    onLogout: () -> Unit,
    onBusClick: (String) -> Unit = { },
    busViewModel: BusViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val buses by busViewModel.buses.collectAsState()
    val isLoading by busViewModel.isLoading.collectAsState()
    val error by busViewModel.error.collectAsState()
    val allStops by busViewModel.allStops.collectAsState()
    val currentUserState by authViewModel.currentUserData.collectAsState()

    LaunchedEffect(currentUserState) {
        (currentUserState as? CurrentUserState.Authenticated)?.user?.let { user ->
            if (user.role == UserRole.DRIVER) {
                busViewModel.loadBuses(currentUserId = user.id, isUserAdmin = false)
            } else if (user.role == UserRole.ADMIN) {
                // Admin should not typically be on this screen, but if so, load all.
                busViewModel.loadBuses(currentUserId = null, isUserAdmin = true)
            }
        }
    }

    Scaffold(
        topBar = {
            val currentUserName = (currentUserState as? CurrentUserState.Authenticated)?.user?.name ?: "Usuario"
            TopNavigationBar(
                headerTitle = "Bienvenido",
                userName = currentUserName
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = listOf(
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_log_out),
                        label = stringResource(R.string.log_out_icon_text),
                        modifier = Modifier.graphicsLayer(scaleX = -1f),
                        onClick = onLogout
                    )
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFE6F7FF)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Selecciona tu bus",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF00AEEF))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Text("Cargando buses...") // Show loading indicator
                } else if (error != null) {
                    Text("Error: $error") // Show error message
                } else if (buses.isEmpty()) {
                    Text("No hay buses disponibles.")
                } else {
                    buses.forEach { bus ->
                        BusCard(
                            bus = bus,
                            onBusClick = {
                                onBusClick(bus.plate)
                            },
                            busViewModel = busViewModel
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BusCard(
    bus: Bus,
    onBusClick: () -> Unit = {},
    busViewModel: BusViewModel
) {
    val allStops by busViewModel.allStops.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .padding(horizontal = 36.dp)
            .shadow(2.dp, shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onBusClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.icon_bus),
                contentDescription = "Bus icon",
                modifier = Modifier
                    .height(60.dp)
                    .padding(bottom = 6.dp)
            )

            DividerLine()
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                InfoColumnCentered("Placa", bus.plate)
                InfoColumnCentered(
                    title = "Ruta",
                    value = bus.route.mapNotNull { stopId ->
                        allStops.find { it.id == stopId }?.name
                    }.joinToString(", ")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                InfoColumnCentered("Capac.", bus.capacity.toString())
                InfoColumnCentered("Inicio", bus.startTime)
            }
        }
    }
}

@Composable
fun InfoColumnCentered(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF005F83),
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = Color.DarkGray,
            fontSize = 13.sp
        )
    }
}

@Composable
fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color(0xFF00AEEF))
    )
}


