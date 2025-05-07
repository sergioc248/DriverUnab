package com.sergiocuadros.dannacarrillo.busunab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergiocuadros.dannacarrillo.busunab.R
import com.sergiocuadros.dannacarrillo.busunab.ui.components.TopNavigationBar
import com.sergiocuadros.dannacarrillo.busunab.ui.components.BottomNavigationBar

@Composable
fun BusViewScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F7FF))
    ) {
        TopNavigationBar(headerTitle = "Bienvenido", userName = "Conductor1")

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

            // Muestra 5 buses como ejemplo
            repeat(5) {
                BusCard(
                    plate = "XYZ-123",
                    route = "2",
                    capacity = "36",
                    startTime = "6:00 pm"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        BottomNavigationBar()
    }
}

@Composable
fun BusCard(
    plate: String,
    route: String,
    capacity: String,
    startTime: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .padding(horizontal = 36.dp)
            .shadow(2.dp, shape = MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.bus_ic),
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
                InfoColumnCentered("Placa", plate)
                InfoColumnCentered("Ruta", route)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoColumnCentered("Capac.", capacity)
                InfoColumnCentered("Inicio", startTime)
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
fun PreviewBusScreen()
{
    BusViewScreen()
}

