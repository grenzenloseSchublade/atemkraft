package app.atemkraft.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/**
 * Großes Pause-/Play-Symbol, das beim Antippen des Kreises kurz aufblinkt (alpha von außen
 * gesteuert). Dunkler Scrim-Kreis + helles Symbol, damit es klar aufpoppt. Per Canvas gezeichnet.
 */
@Composable
fun PauseFlash(alpha: Float, isPause: Boolean) {
    if (alpha <= 0.01f) return
    Canvas(modifier = Modifier.size(150.dp)) {
        val w = size.width
        val h = size.height
        // Dunkler Scrim-Kreis als Kontrastfläche.
        drawCircle(color = Color.Black.copy(alpha = alpha * 0.42f), radius = w * 0.5f)
        val symbol = Color(0xFFFFF3D6).copy(alpha = alpha) // cremeweiß
        if (isPause) {
            val barW = w * 0.15f
            val barH = h * 0.42f
            val top = (h - barH) / 2f
            val gap = w * 0.12f
            val leftX = w / 2f - gap / 2f - barW
            val rightX = w / 2f + gap / 2f
            val radius = CornerRadius(barW * 0.4f)
            drawRoundRect(color = symbol, topLeft = Offset(leftX, top), size = Size(barW, barH), cornerRadius = radius)
            drawRoundRect(color = symbol, topLeft = Offset(rightX, top), size = Size(barW, barH), cornerRadius = radius)
        } else {
            val path = Path().apply {
                moveTo(w * 0.4f, h * 0.32f)
                lineTo(w * 0.4f, h * 0.68f)
                lineTo(w * 0.7f, h * 0.5f)
                close()
            }
            drawPath(path, color = symbol)
        }
    }
}
