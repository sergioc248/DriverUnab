package com.sergiocuadros.dannacarrillo.busunab.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergiocuadros.dannacarrillo.busunab.IconWithText
import com.sergiocuadros.dannacarrillo.busunab.R

@Composable
fun TopNavigationBar(headerTitle: String = "Bienvenido", userName: String = "Admin1") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00AEEF))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.logo_unab_blanco),
            contentDescription = "Logo UNAB",
            modifier = Modifier
                .height(60.dp)
                .padding(end = 12.dp)
        )
        Text(
            text = "$headerTitle,\n$userName",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}


@Composable
fun BottomNavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00AEEF))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWithText(Icons.Default.ArrowBack, "Cerrar\nsesión")
        IconWithText(Icons.Default.Clear, "Estadísticas")
        IconWithText(Icons.Default.Add, "Agregar\nbus")
    }
}