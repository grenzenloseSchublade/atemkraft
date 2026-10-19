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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SynthCircleCenter
import app.atemkraft.ui.theme.SynthCircleEdge

/**
 * Der Atemkreis. [fraction] (0f = klein/ausgeatmet, 1f = groß/eingeatmet) wird direkt
 * aus dem Session-Zustand abgeleitet, nicht separat animiert – so bleiben Kreis,
 * Countdown und Ton synchron, und in der Pause friert alles gemeinsam ein.
 *
 * Dezenter Glow: ein weicher Schein hinter dem Kreis atmet mit [fraction] mit und
 * schimmert zusätzlich sehr langsam (~8 s), damit der Kreis lebendig wirkt, ohne zu
 * flackern. Bei deaktivierter System-Animation bleibt der Schimmer einfach statisch.
 *
 * Außen eine dünne Spur am maximalen Umfang; [progress] (0..1, Gesamtsitzung) zeichnet darauf
 * einen Cyan-Bogen ab 12 Uhr im Uhrzeigersinn – dieselbe Sprache wie der Zeitring. null = nur Spur.
 * Der Kreis bleibt auch voll eingeatmet knapp innerhalb der Spur, damit der Bogen lesbar ist.
 */
@Composable
fun BreathingCircle(
    fraction: Float,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    content: @Composable () -> Unit = {},
) {
    val scale = lerp(MIN_SCALE, MAX_SCALE, fraction.coerceIn(0f, 1f))
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = MaterialTheme.colorScheme.secondary

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
            // Dünne Spur am Außenrand und Fortschrittsbogen der Gesamtsitzung darauf.
            val stroke = Dimens.SessionRingStroke.toPx()
            val trackRadius = maxRadius - stroke / 2f
            drawCircle(color = trackColor, radius = trackRadius, center = center, style = Stroke(stroke))
            val sweep = (progress ?: 0f).coerceIn(0f, 1f) * FULL_TURN
            if (sweep > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = ARC_START,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = center - Offset(trackRadius, trackRadius),
                    size = Size(trackRadius * 2f, trackRadius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
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

/** Durchmesser der gefüllten Scheibe bei Kantenlänge [side] und [fraction] – z. B. als Breitengrenze für Text darin. */
fun breathingDiscDiameter(side: Dp, fraction: Float): Dp = side * lerp(MIN_SCALE, MAX_SCALE, fraction.coerceIn(0f, 1f))

// Kreis relativ zum Außenradius: voll eingeatmet wächst er bis knapp an die Spur (der Bogen liegt
// eng darum, Nutzerwunsch 2026-10-19), das Verhältnis klein ↔ groß (0,42) ist dasselbe wie früher.
private const val MAX_SCALE = 0.97f
private const val MIN_SCALE = 0.42f * MAX_SCALE

/** 12 Uhr in Canvas-Winkeln (0° = 3 Uhr) und eine volle Umdrehung. */
private const val ARC_START = -90f
private const val FULL_TURN = 360f
