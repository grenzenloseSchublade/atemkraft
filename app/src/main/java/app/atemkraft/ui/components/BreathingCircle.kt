package app.atemkraft.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.util.lerp
import app.atemkraft.ui.theme.SynthCircleCenter
import app.atemkraft.ui.theme.SynthCircleEdge
import app.atemkraft.ui.theme.SynthTrack

/**
 * Der Atemkreis. [fraction] (0f = klein/ausgeatmet, 1f = groß/eingeatmet) wird direkt
 * aus dem Session-Zustand abgeleitet, nicht separat animiert – so bleiben Kreis,
 * Countdown und Ton synchron, und in der Pause friert alles gemeinsam ein.
 */
@Composable
fun BreathingCircle(
    fraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val scale = lerp(MIN_SCALE, MAX_SCALE, fraction.coerceIn(0f, 1f))

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            // Dezente Bahn, die den maximalen Umfang andeutet.
            drawCircle(color = SynthTrack.copy(alpha = 0.10f), radius = maxRadius, center = center)
            // Gefüllter, synthwave-lila Kreis, der mit dem Atem wächst/schrumpft.
            val radius = maxRadius * scale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SynthCircleCenter.copy(alpha = 1f), SynthCircleEdge.copy(alpha = 0.72f)),
                    center = center,
                    radius = radius.coerceAtLeast(1f),
                ),
                radius = radius,
                center = center,
            )
        }
        content()
    }
}

private const val MIN_SCALE = 0.42f
private const val MAX_SCALE = 1f
