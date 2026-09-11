package com.melo.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.ui.NavDestination
import com.melo.desktop.ui.theme.MeloDarkSurface
import com.melo.desktop.ui.theme.MeloPrimary
import com.melo.desktop.ui.theme.MeloSurfaceVariant

@Composable
fun Sidebar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(MeloDarkSurface)
            .padding(18.dp),
    ) {
        // Логотип приложения
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MeloPrimary),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Melo",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Desktop",
                fontSize = 11.sp,
                color = MeloPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(MeloSurfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Пункты меню
        NavDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination
            val bgColor = if (isSelected) MeloSurfaceVariant else Color.Transparent
            val contentColor = if (isSelected) MeloPrimary else Color.White.copy(alpha = 0.7f)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onNavigate(destination) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = destination.title,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
