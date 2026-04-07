// 📄 app/src/main/java/com/perceptnote/ui/components/SensorStatusBar.kt
package com.perceptnote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.perceptnote.ui.theme.*

/**
 * Barre de statut des capteurs — affichée en haut de CaptureScreen.
 * Indique visuellement quels capteurs sont actifs en temps réel.
 */
@Composable
fun SensorStatusBar(
    isMicActive: Boolean,
    isCameraActive: Boolean,
    isGpsActive: Boolean,
    isAccelActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SensorIndicator(
                icon = Icons.Default.Mic,
                label = "Micro",
                isActive = isMicActive,
                activeColor = RecordingRed
            )
            SensorIndicator(
                icon = Icons.Default.CameraAlt,
                label = "Caméra",
                isActive = isCameraActive,
                activeColor = OcrGreen
            )
            SensorIndicator(
                icon = Icons.Default.LocationOn,
                label = "GPS",
                isActive = isGpsActive,
                activeColor = GpsOrange
            )
            SensorIndicator(
                icon = Icons.Default.Sensors,
                label = "Accel",
                isActive = isAccelActive,
                activeColor = AccelPurple
            )
        }
    }
}

@Composable
private fun SensorIndicator(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "sensorColor"
    )

    // Animation pulsation si actif
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(if (isActive) scale else 1f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
