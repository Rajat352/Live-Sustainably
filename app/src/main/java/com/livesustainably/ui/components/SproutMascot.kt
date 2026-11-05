package com.livesustainably.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics

// Custom shared mascot icon
@Composable
fun SproutMascot(
    modifier: Modifier = Modifier
) {

    Canvas(modifier.clearAndSetSemantics {}) {
        val factor = minOf(size.width, size.height) / 120f
        scale(factor, factor, pivot = Offset.Zero) {
            drawCircle(Color.White.copy(alpha = 0.14f), 53f, Offset(61f, 64f))
            drawOval(Color(0xFF254D81).copy(alpha = 0.14f), Offset(28f, 104f), Size(66f, 10f))
            drawLine(Color(0xFFFFF5D8), Offset(34f, 77f), Offset(18f, 67f), 12f, StrokeCap.Round)
            drawLine(Color(0xFFFFF5D8), Offset(88f, 75f), Offset(104f, 59f), 12f, StrokeCap.Round)
            drawOval(Color(0xFFE2DCC4), Offset(36f, 95f), Size(18f, 15f))
            drawOval(Color(0xFFE2DCC4), Offset(70f, 95f), Size(18f, 15f))
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFFFFEE), Color(0xFFFFEAC6))),
                Offset(28f, 37f), Size(67f, 69f), CornerRadius(32f, 32f))
            drawPath(Path().apply { moveTo(61f, 40f); cubicTo(58f, 24f, 66f, 20f, 67f, 12f) },
                Color(0xFF28845B), style = Stroke(4f, cap = StrokeCap.Round))
            drawPath(Path().apply { moveTo(63f, 31f); cubicTo(39f, 35f, 36f, 14f, 39f, 11f); cubicTo(56f, 9f, 66f, 18f, 63f, 31f) },
                Brush.linearGradient(listOf(Color(0xFFB1E66D), Color(0xFF3CAF7E))))
            drawPath(Path().apply { moveTo(64f, 25f); cubicTo(65f, 5f, 84f, 5f, 91f, 10f); cubicTo(87f, 27f, 74f, 30f, 64f, 25f) },
                Brush.linearGradient(listOf(Color(0xFF85D577), Color(0xFF25896C))))
            listOf(48f, 76f).forEach { x ->
                drawOval(Color.White, Offset(x-10f, 55f), Size(21f, 26f))
                drawOval(Color(0xFF445B40), Offset(x-7f, 58f), Size(16f, 21f))
                drawOval(Color(0xFF30314F), Offset(x-4f, 61f), Size(10f, 15f))
                drawCircle(Color.White, 3f, Offset(x+1f, 63f))
            }
            drawOval(Color(0xFFF5B3A2).copy(alpha = 0.65f), Offset(34f, 78f), Size(14f, 7f))
            drawOval(Color(0xFFF5B3A2).copy(alpha = 0.65f), Offset(77f, 78f), Size(14f, 7f))
            drawArc(Color(0xFF664A40), 15f, 150f, false, Offset(54f, 79f), Size(15f, 12f), style = Stroke(2.7f, cap = StrokeCap.Round))
            drawLine(Color(0xFFFFD878), Offset(10f, 32f), Offset(18f, 32f), 3f, StrokeCap.Round)
            drawLine(Color(0xFFFFD878), Offset(14f, 28f), Offset(14f, 36f), 3f, StrokeCap.Round)
            drawCircle(Color(0xFFB3EDF1), 3f, Offset(105f, 92f))
        }
    }
}
