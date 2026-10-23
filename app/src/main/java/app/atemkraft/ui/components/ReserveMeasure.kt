package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

// Platz reservieren, ohne zu springen (LAYOUT-03): Zeilen entscheiden ein- oder zweizeilig bzw.
// halten ihre Breite nach dem breitesten Inhalt, den sie zeigen können – nicht nach dem gerade
// gezeigten. Die Bausteine hier messen dieses Breiteste; niemand baut die Messung mehr von Hand.

/**
 * Inhalt, der nur gemessen wird: nie platziert, ohne Semantik und Fokus ([inertWhen]), falls doch
 * jemand den Baum durchsucht. Für Reserven in `Layout`-Inhalten ([InlineOrBelow]) und in
 * [widestOf].
 */
@Composable
internal fun MeasureOnly(content: @Composable () -> Unit) {
    Box(Modifier.inertWhen(true)) { content() }
}

/**
 * Einzeilige Breite (`maxIntrinsicWidth`) des Breitesten in [content], komponiert in [slot] als
 * [MeasureOnly]: Alles, was [content] nebeneinander oder übereinander zeigt (z. B. jede mögliche
 * Beschriftung in einer `Box`), zählt mit. Für `SubcomposeLayout`s, deren Anordnung von Reserven
 * abhängt.
 */
fun SubcomposeMeasureScope.widestOf(slot: Any, content: @Composable () -> Unit): Int = subcompose(slot) { MeasureOnly(content) }.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0

/**
 * Einzeilige Breite des breitesten der [texts] in [style], gemessen mit der aktuellen Schrift
 * (fontScale, auch nichtlinear ab Android 14). Für Platzhalter, die nicht springen sollen
 * (`Modifier.widthIn(min = …)`), und für Entscheidungen „passt in eine Zeile“.
 */
@Composable
fun rememberWidestTextWidth(texts: List<String>, style: TextStyle): Dp = rememberWidestAnnotatedWidth(texts.map(::AnnotatedString), style)

/** [rememberWidestTextWidth] für ausgezeichnete Texte (z. B. Sanskrit kursiv, [sanskritText]). */
@Composable
fun rememberWidestAnnotatedWidth(texts: List<AnnotatedString>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, texts, style) {
        val px = texts.maxOfOrNull { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width } ?: 0
        with(density) { px.toDp() }
    }
}
