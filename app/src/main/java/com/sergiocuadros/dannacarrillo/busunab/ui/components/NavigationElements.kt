package com.sergiocuadros.dannacarrillo.busunab.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergiocuadros.dannacarrillo.busunab.R

private val TopBarHeight = 120.dp
private val BottomBarHeight = 80.dp
private val DarkBlue = Color(0xFF009FE3)
private val LightBlue = Color(0xFF00AEEF)

@Composable
fun TopNavigationBar(
    headerTitle: String,
    userName: String
) {
    val baseHeight = 80f
    val scaleFactor = TopBarHeight.value / baseHeight
    val textSize = (16 * scaleFactor).sp
    val iconHeight = (56 * scaleFactor).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            .background(LightBlue)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.logo_unab_blanco),
            contentDescription = "Logo UNAB",
            tint = Color.Unspecified,
            modifier = Modifier.height(iconHeight)
        )
        Text(
            text = "$headerTitle,\n$userName",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

sealed class BottomNavItem {
    data class VectorIcon(
        val icon: ImageVector,
        val label: String,
        val modifier: Modifier = Modifier
    ) : BottomNavItem()

    data class PainterIcon(
        val painter: Painter,
        val label: String,
        val modifier: Modifier = Modifier
    ) : BottomNavItem()
}

@Composable
fun BottomNavigationBar(items: List<BottomNavItem>) {
    val baseHeight = 80f
    val scaleFactor = BottomBarHeight.value / baseHeight
    val iconSize = (32 * scaleFactor).dp
    val textSize = (12 * scaleFactor).sp
    val verticalPadding = (12 * scaleFactor).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarHeight)
            .background(LightBlue)
            .padding(vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            when (item) {
                is BottomNavItem.VectorIcon -> {
                    IconWithText(
                        icon = item.icon,
                        label = item.label,
                        iconSize = iconSize,
                        textSize = textSize,
                        modifier = item.modifier
                    )
                }

                is BottomNavItem.PainterIcon -> {
                    IconWithText(
                        painter = item.painter,
                        label = item.label,
                        iconSize = iconSize,
                        textSize = textSize,
                        modifier = item.modifier
                    )
                }
            }
        }
    }
}

@Composable
fun IconWithText(
    icon: ImageVector,
    label: String,
    iconSize: Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .padding(bottom = 4.dp)
                .then(modifier)
        )
        Text(label, color = Color.White, fontSize = textSize)
    }
}

@Composable
fun IconWithText(
    painter: Painter,
    label: String,
    iconSize: Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .padding(bottom = 4.dp)
                .then(modifier)
        )
        Text(label, color = Color.White, fontSize = textSize)
    }
}
