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

@Composable
fun BusViewScreen(
    onBusClick: (String) -> Unit = { }
) {
    // Example bus list - in a real app, this would come from a ViewModel
    val buses = listOf(
        Bus("XYZ-123", "2", 36, "6:00 pm"),
        Bus("ABC-456", "3", 36, "7:00 pm"),
        Bus("DEF-789", "4", 36, "8:00 pm"),
        Bus("GHI-012", "5", 36, "9:00 pm"),
        Bus("JKL-345", "6", 36, "10:00 pm")
    )

    Scaffold(
        topBar = {
            TopNavigationBar(
                headerTitle = "Bienvenido",
                userName = "Conductor1"
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = listOf(
                    BottomNavItem.PainterIcon(
                        painter = painterResource(R.drawable.icon_log_out),
                        label = stringResource(R.string.log_out_icon_text),
                        modifier = Modifier.graphicsLayer(scaleX = -1f)
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

                buses.forEach { bus ->
                    BusCard(
                        bus = bus,
                        onBusClick = {
                            onBusClick(bus.plate)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun BusCard(
    bus: Bus,
    onBusClick: () -> Unit = {}
) {
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumnCentered("Placa", bus.plate)
                InfoColumnCentered("Ruta", bus.route)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumnCentered("Capac.", bus.capacity.toString())
                InfoColumnCentered("Inicio", bus.startTime)
            }
        }
    }
}

@Composable
fun InfoColumnCentered(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

@Preview
@Composable
fun PreviewBusScreen() {
    BusViewScreen()
}

