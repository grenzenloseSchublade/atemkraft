package app.atemkraft.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp
import app.atemkraft.ui.theme.SynthCircleCenter
import app.atemkraft.ui.theme.SynthCircleEdge
import app.atemkraft.ui.theme.SynthTrack

/**
 * Der Atemkreis. [fraction] (0f = klein/ausgeatmet, 1f = groß/eingeatmet) wird direkt
 * aus dem Session-Zustand abgeleitet, nicht separat animiert – so bleiben Kreis,
 * Countdown und Ton synchron, und in der Pause friert alles gemeinsam ein.
 *
 * Dezenter Glow: ein weicher Schein hinter dem Kreis atmet mit [fraction] mit und
 * schimmert zusätzlich sehr langsam (~8 s), damit der Kreis lebendig wirkt, ohne zu
 * flackern. Bei deaktivierter System-Animation bleibt der Schimmer einfach statisch.
 */
@Composable
fun BreathingCircle(
    fraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val scale = lerp(MIN_SCALE, MAX_SCALE, fraction.coerceIn(0f, 1f))

    // Sehr langsamer, ruhiger Schimmer (Yoga-Tempo, kein Puls-Gefühl).
    val shimmer by rememberInfiniteTransition(label = "circleGlow").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowStrength",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = maxRadius * scale
            // Weicher Außen-Glow: atmet mit (fraction) und schimmert langsam. Bewusst leise.
            val glowAlpha = (0.06f + 0.10f * fraction.coerceIn(0f, 1f)) * shimmer
            val glowRadius = radius * 1.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SynthCircleCenter.copy(alpha = glowAlpha), Color.Transparent),
                    center = center,
                    radius = glowRadius.coerceAtLeast(1f),
                ),
                radius = glowRadius,
                center = center,
            )
            // Dezente Bahn, die den maximalen Umfang andeutet.
            drawCircle(color = SynthTrack.copy(alpha = 0.10f), radius = maxRadius, center = center)
            // Gefüllter, synthwave-lila Kreis, der mit dem Atem wächst/schrumpft.
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
