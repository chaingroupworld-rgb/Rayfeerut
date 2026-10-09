package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    val speedY: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)

@Composable
fun ConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val particles = remember {
        val colors = listOf(
            Color(0xFF6366F1), // Indigo
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFFEC4899), // Pink
            Color(0xFF3B82F6), // Blue
            Color(0xFFF8FAFC)  // White
        )
        List(70) {
            ConfettiParticle(
                x = Random.nextFloat(),
                speedY = Random.nextFloat() * 600f + 400f,
                size = Random.nextFloat() * 10f + 8f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = Random.nextFloat() * 360f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti_anim")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val curY = (time * p.speedY) % h
            val curX = (p.x * w + (time * 100f * (if (p.x > 0.5f) 1 else -1))) % w
            drawRect(
                color = p.color,
                topLeft = Offset(curX, curY),
                size = Size(p.size, p.size * 1.5f)
            )
        }
    }
}
