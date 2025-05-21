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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavItem
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.AuthViewModel
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.CurrentUserState
import com.sergiocuadros.dannacarrillo.busunab.viewmodels.StatsViewModel
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(
    onLogout: () -> Unit,
    onNavigateToBusManagement: () -> Unit,
    viewModel: StatsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val stopFrequenciesFromVM by viewModel.stopFrequencies.collectAsState()
    val passengerFlowFromVM by viewModel.passengerFlow.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUserState by authViewModel.currentUserData.collectAsState()

    // Chart Colors
    val chartColorBlue = Color(0xFF00AEEF)
    val chartTextColor = Color(0xFF005F83)


    Scaffold(
        topBar = {
            val currentUserName = (currentUserState as? CurrentUserState.Authenticated)?.user?.name ?: "Admin"
            TopNavigationBar(
                headerTitle = "Panel de Administración",
                userName = currentUserName
            )
        },
        bottomBar = {
            BottomNavigationBar(items = listOf(
                BottomNavItem.PainterIcon(
                    painter = painterResource(R.drawable.icon_log_out),
                    label = stringResource(R.string.log_out_icon_text),
                    onClick = onLogout
                ),
                BottomNavItem.PainterIcon( // Assuming this icon was intended for bus management
                    painter = painterResource(R.drawable.icon_bus), // Changed for clarity
                    label = "Gestión de Buses",
                    onClick = onNavigateToBusManagement
                )
            ))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFE6F7FF)) // Light blue background
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Estadísticas",
                style = MaterialTheme.typography.headlineSmall,
                color = chartTextColor // Consistent color
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
                    color = chartColorBlue
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
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paradas más frecuentes", style = MaterialTheme.typography.titleMedium, color = chartTextColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (stopFrequenciesFromVM.isNotEmpty()) {
                        val columnChartData = remember(stopFrequenciesFromVM) {
                            stopFrequenciesFromVM.map { stopFreq ->
                                Bars(
                                    label = stopFreq.stopName, 
                                    values = listOf(
                                        Bars.Data(
                                            label = stopFreq.stopName, 
                                            value = stopFreq.frequency.toDouble(), //
                                            color = SolidColor(chartColorBlue)
                                        )
                                    )
                                )
                            }
                        }
                        ColumnChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            data = columnChartData
                        )
                    } else {
                        Text(
                            text = "No hay datos de paradas disponibles",
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
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Afluencia por hora", style = MaterialTheme.typography.titleMedium, color = chartTextColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (passengerFlowFromVM.isNotEmpty()) {
                        val lineChartData = remember(passengerFlowFromVM) {
                            listOf(
                                Line(
                                    label = "Pasajeros", // Single line representing passenger count
                                    values = passengerFlowFromVM.sortedBy { it.hour }.map { it.passengerCount.toDouble() },
                                    color = SolidColor(chartColorBlue),
                                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                                    firstGradientFillColor = chartColorBlue.copy(alpha = .5f),
                                    secondGradientFillColor = Color.Transparent,
                                )
                            )
                        }
                        val xAxisLabels = remember(passengerFlowFromVM) {
                            passengerFlowFromVM.sortedBy { it.hour }.map { "${it.hour}:00" } // Create labels like "7:00", "8:00"
                        }
                        LineChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp), // Increased height slightly for labels
                            data = lineChartData,
                            animationMode = AnimationMode.Together(),
                            labelProperties = LabelProperties(
                                enabled = true,
                                labels = xAxisLabels,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = chartTextColor)
                            )
                        )
                    } else {
                        Text(
                            text = "No hay datos de afluencia disponibles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
