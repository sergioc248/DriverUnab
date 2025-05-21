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

private val LightBlue = Color(0xFFE5F7FF)
private val DarkBlue  = Color(0xFF009FE3)

@Composable
fun BusManagementScreen(
    onNavigateToStats: () -> Unit,
    onLogout: () -> Unit
) {
    val busList = listOf(
        BusData("1", "QWE-321", "16", "6:00 am"),
        BusData("2", "QWE-321", "32", "6:00 am"),
        BusData("3", "QWE-321", "32", "6:00 am")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopNavigationBar(
                headerTitle = "Panel de Administración",
                userName = "admin1",
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
                busList.forEach {
                    TableRow(
                        cells = listOf(it.route, it.plate, it.capacity, it.startTime)
                    )
                }

                // Fill remaining space with empty rows
                repeat(7) {
                    TableRow(cells = listOf("", "", "", ""))
                }
            }
        }
    }
}

@Composable
fun TableRow(cells: List<String>, isHeader: Boolean = false) {
    val cellColor = if (isHeader) DarkBlue else Color.Black
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        cells.forEachIndexed { index, cell ->
            Box(
                modifier = Modifier
                    .weight(1f)
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
