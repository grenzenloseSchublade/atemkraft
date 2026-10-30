package app.atemkraft.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

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
 * (`Modifier.widthIn(min = …)`), und für Entscheidungen „passt in eine Zeile“. Jeder Text wird je
 * Schrift-Umgebung nur einmal im Prozess gemessen ([TextWidths]).
 */
@Composable
fun rememberWidestTextWidth(texts: List<String>, style: TextStyle): Dp = rememberWidestAnnotatedWidth(texts.map(::AnnotatedString), style)

/** [rememberWidestTextWidth] für ausgezeichnete Texte (z. B. Sanskrit kursiv, [sanskritText]). */
@Composable
fun rememberWidestAnnotatedWidth(texts: List<AnnotatedString>, style: TextStyle): Dp {
    // Ohne eigenen Layout-Cache: Gemerkt wird nur die Breite, in TextWidths.
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val density = LocalDensity.current
    val environment = rememberTextEnvironment()
    return remember(measurer, density, environment, texts, style) {
        with(density) { TextWidths.widest(measurer, texts, style, environment).toDp() }
    }
}

/**
 * Was außer Text und Stil die Breite eines Texts bestimmt: Dichte und Schriftgröße (die
 * nichtlineare Skalierung ab Android 14 hängt nur an ihr), Schreibrichtung, Sprachen und „Fette
 * Schrift“ der Systemeinstellungen. Ändert sich eins davon, gilt ein neuer Schlüssel in
 * [TextWidths]; geleert werden muss nichts.
 */
@Immutable
internal data class TextEnvironment(
    val density: Float,
    val fontScale: Float,
    val layoutDirection: LayoutDirection,
    val locales: String,
    val fontWeightAdjustment: Int,
)

/** Die [TextEnvironment] der aktuellen Komposition. */
@Composable
internal fun rememberTextEnvironment(): TextEnvironment {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val locales = configuration.locales.toLanguageTags()
    val weight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) configuration.fontWeightAdjustment else 0
    return remember(density.density, density.fontScale, direction, locales, weight) {
        TextEnvironment(density.density, density.fontScale, direction, locales, weight)
    }
}

/**
 * Einzeilige Breiten (px) einzelner Texte, prozessweit gemerkt je Text samt Auszeichnung, Stil und
 * [TextEnvironment]. Reserven sind wenige Texte, die bei jedem Aufbau einer Seite wiederkehren: Der
 * Muster-Editor maß bei jedem Öffnen 20 davon neu, im ersten Frame (2026-10-30). So misst nur das
 * erste Mal – oder vorab das Vorladen (`PatternEditorPreload`). Höchstens [MAX_ENTRIES] Einträge,
 * die am längsten unbenutzten fallen heraus. Nur vom Main-Thread (Komposition, `LaunchedEffect`).
 */
internal object TextWidths {
    private const val MAX_ENTRIES = 256
    private const val LOAD_FACTOR = 0.75f

    private data class Key(val text: AnnotatedString, val style: TextStyle, val environment: TextEnvironment)

    private val widths = object : LinkedHashMap<Key, Int>(MAX_ENTRIES, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Int>?): Boolean = size > MAX_ENTRIES
    }

    /** Breite von [text] in [style]; [measurer] muss zur [environment] gehören (dieselbe Komposition). */
    fun width(measurer: TextMeasurer, text: AnnotatedString, style: TextStyle, environment: TextEnvironment): Int = widths.getOrPut(Key(text, style, environment)) {
        measurer.measure(text, style, softWrap = false, maxLines = 1).size.width
    }

    /** Breite des breitesten der [texts]; 0 ohne Texte. */
    fun widest(measurer: TextMeasurer, texts: List<AnnotatedString>, style: TextStyle, environment: TextEnvironment): Int = texts.maxOfOrNull { width(measurer, it, style, environment) } ?: 0
}
