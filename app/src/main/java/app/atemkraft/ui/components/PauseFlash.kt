package app.atemkraft.ui.components

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
import app.atemkraft.ui.theme.SessionButtonCyan

/**
 * Großes Pause-/Play-Symbol, das beim Antippen des Kreises kurz aufblinkt (alpha von außen
 * gesteuert). Ohne dunkle Platte und ohne Halo – nur das klare Symbol in der Pause-Button-Farbe,
 * damit es zum Glow-Design passt und ohne harte Umrandung aufpoppt. Per Canvas gezeichnet.
 */
@Composable
fun PauseFlash(alpha: Float, isPause: Boolean) {
    if (alpha <= 0.01f) return
    Canvas(modifier = Modifier.size(150.dp)) {
        val w = size.width
        val h = size.height

        fun drawSymbol(color: Color) {
            if (isPause) {
                val barW = w * 0.15f
                val barH = h * 0.42f
                val top = (h - barH) / 2f
                val gap = w * 0.12f
                val leftX = w / 2f - gap / 2f - barW
                val rightX = w / 2f + gap / 2f
                val radius = CornerRadius(barW * 0.4f)
                drawRoundRect(color = color, topLeft = Offset(leftX, top), size = Size(barW, barH), cornerRadius = radius)
                drawRoundRect(color = color, topLeft = Offset(rightX, top), size = Size(barW, barH), cornerRadius = radius)
            } else {
                // Play-Dreieck, um die Mitte skaliert (optisch leicht nach rechts versetzt).
                val cx = w * 0.53f
                val cy = h * 0.5f
                val tw = w * 0.30f
                val th = h * 0.40f
                val path = Path().apply {
                    moveTo(cx - tw * 0.5f, cy - th * 0.5f)
                    lineTo(cx - tw * 0.5f, cy + th * 0.5f)
                    lineTo(cx + tw * 0.5f, cy)
                    close()
                }
                drawPath(path, color = color)
            }
        }

        // Klares Symbol im Session-Cyan (Pause-Button-Farbe), ohne Halo.
        drawSymbol(SessionButtonCyan.copy(alpha = alpha))
    }
}
