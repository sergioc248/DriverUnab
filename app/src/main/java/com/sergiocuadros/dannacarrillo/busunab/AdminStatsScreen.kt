package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.viewmodels.StatsViewModel
import io.github.ehsannarmani.compose.charts.bar.BarChart
import io.github.ehsannarmani.compose.charts.line.LineChart
import io.github.ehsannarmani.compose.charts.line.LineChartData
import io.github.ehsannarmani.compose.charts.line.LineChartItem
import io.github.ehsannarmani.compose.charts.bar.BarChartItem

// Data classes for our statistics
data class StopFrequency(
    val stopName: String,
    val frequency: Int
)

data class PassengerFlow(
    val hour: Int,
    val passengerCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(
    onNavigateToStats: () -> Unit,
    onLogout: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stopFrequencies by viewModel.stopFrequencies.collectAsState()
    val passengerFlow by viewModel.passengerFlow.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = { 
            TopNavigationBar(
                headerTitle = "Panel de Administración",
                userName = "Admin1"
            ) 
        },
        bottomBar = {  
            BottomNavigationBar(items = listOf(
                BottomNavItem.PainterIcon(
                    painter = painterResource(R.drawable.icon_log_out),
                    label = stringResource(R.string.log_out_icon_text),
                    onClick = onLogout
                )
            ))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFE6F7FF))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Estadísticas",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF0088CC)
            )
            Text(
                text = "Visualiza información obtenida con datos recopilados",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF00AEEF)
                )
            }

            error?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Most Frequent Stops Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paradas más frecuentes", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (stopFrequencies.isNotEmpty()) {
                        BarChart(
                            items = stopFrequencies.map { 
                                BarChartItem(
                                    label = it.stopName,
                                    value = it.frequency.toFloat(),
                                    color = Color(0xFF00AEEF)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        Text(
                            text = "No hay datos disponibles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Passenger Flow Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Afluencia por hora", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (passengerFlow.isNotEmpty()) {
                        LineChart(
                            data = LineChartData(
                                items = passengerFlow.map { 
                                    LineChartItem(
                                        x = it.hour.toFloat(),
                                        y = it.passengerCount.toFloat()
                                    )
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        Text(
                            text = "No hay datos disponibles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Navegar a gestión de buses */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Volver a gestión de buses", color = Color.White)
            }
        }
    }
}

@Preview
@Composable
fun PreviewStatsScreen() {
    AdminStatsScreen()
}