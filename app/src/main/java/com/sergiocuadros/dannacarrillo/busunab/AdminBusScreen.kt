package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

@Composable
fun BusManagementScreen() {
    val busList = listOf(
        BusData("1", "QWE-321", "16", "6:00 am"),
        BusData("2", "QWE-321", "32", "6:00 am"),
        BusData("2", "QWE-321", "32", "6:00 am")
    )

    Scaffold(
        topBar = { TopNavigationBar(userName = "admin1") },
        bottomBar = {
            BottomNavigationBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // Table Container
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(Color(0xFFE5F7FF), shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Gestión de Buses",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF009FE3)
                )
                Text(
                    text = "Agrega, edita o elimina buses",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    TableHeader("Ruta", Modifier.weight(1f))
                    TableHeader("Placa", Modifier.weight(1f))
                    TableHeader("Capacidad", Modifier.weight(1f))
                    TableHeader("Inicio", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Table Rows
                busList.forEach {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        TableCell(it.route, Modifier.weight(1f))
                        TableCell(it.plate, Modifier.weight(1f))
                        TableCell(it.capacity, Modifier.weight(1f))
                        TableCell(it.startTime, Modifier.weight(1f))
                    }
                }

                // Empty Rows (to match image layout)
                repeat(7) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(4) {
                            TableCell("", Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeader(title: String, modifier: Modifier) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = modifier
    )
}

@Composable
fun TableCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontSize = 14.sp,
        modifier = modifier
    )
}

@Composable
fun IconWithText(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = Color.White)
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

data class BusData(
    val route: String,
    val plate: String,
    val capacity: String,
    val startTime: String
)


@Preview
@Composable
fun PreviewScreen()
{
    BusManagementScreen()
}
