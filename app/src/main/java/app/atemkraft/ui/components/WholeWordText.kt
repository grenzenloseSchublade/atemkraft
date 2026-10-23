package app.atemkraft.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * Text, dessen Wörter nie umbrechen (LAYOUT-03). Ist ein einzelnes Wort breiter als die Zeile
 * (lange Komposita bei großer Systemschrift auf schmalen Geräten), wird die Schrift in Schritten
 * von 0,05 verkleinert, bis jedes Wort ganz in eine Zeile passt – höchstens bis [minScale]
 * (Standard 0,7). Die passende Stufe sucht ein [TextMeasurer] mit denselben Vorgaben wie das
 * erste Layout auf einmal; der Text komponiert danach nur noch einmal neu statt einmal je Stufe
 * (A-09). Bis dahin wird nicht gezeichnet, es flackert also nichts. Gleichrangige Texte (z. B.
 * Tab-Beschriftungen) teilen sich eine Größe: Der Aufrufer reicht [sharedScale] herein und
 * übernimmt per [onSharedScaleTooBig] den kleineren Wert – die kleinste nötige gilt für alle.
 * [singleLine]: Der Text muss zusätzlich in eine Zeile passen (Chip-Beschriftungen, LAYOUT-03);
 * auch dafür wird höchstens bis [minScale] verkleinert.
 * [onDoesNotFit]: Passt der Text selbst bei [minScale] nicht, meldet er das, statt sich zu zeigen –
 * der Aufrufer wechselt dann das Layout (Tab-Leiste nur mit Icons, Verweis-Chip zweizeilig), statt
 * die große Systemschrift weiter zurückzunehmen (LAYOUT-03 Mittel 1 vor 2, WCAG 1.4.4).
 *
 * Für Titel, Namen und Beschriftungen; Fließtext wird stattdessen so formuliert, dass kein
 * Wort die Zeile sprengt. Sanskrit-Auszeichnung („*vāyus*“) wird kursiv ([sanskritText]); mit
 * [marks] `false` bleibt der Text wörtlich – für Namen, die der Nutzer selbst eingibt (eigene
 * Muster), in denen ein „*“ einfach ein Zeichen ist.
 */
@Composable
fun WholeWordText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    minScale: Float = MIN_SCALE,
    singleLine: Boolean = false,
    sharedScale: Float? = null,
    onSharedScaleTooBig: (Float) -> Unit = {},
    marks: Boolean = true,
    onDoesNotFit: (() -> Unit)? = null,
) {
    // Neu messen, wenn sich Text, Stil, Schriftgröße oder Bildschirmbreite ändern.
    val fontScale = LocalDensity.current.fontScale
    val windowWidth = LocalWindowInfo.current.containerSize.width
    var ownScale by remember(text, style, fontScale, windowWidth) { mutableFloatStateOf(1f) }
    val scale = sharedScale ?: ownScale
    var ready by remember(text, style, fontScale, windowWidth) { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()

    val shown = remember(text, marks) { if (marks) sanskritText(text) else AnnotatedString(text) }
    Text(
        text = shown,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        style = if (scale < 1f) {
            style.copy(
                fontSize = style.fontSize.scaled(scale), // Abweichung TYPO-01: skaliert die übergebene Rolle
                lineHeight = style.lineHeight.scaled(scale), // Abweichung TYPO-01: skaliert die übergebene Rolle
            )
        } else {
            style
        },
        color = color,
        textAlign = textAlign ?: TextAlign.Unspecified,
        onTextLayout = { layout ->
            val tooBig = layout.tooBig(singleLine)
            if (tooBig && scale > minScale) {
                val smaller = measurer.fittingScale(layout, style, scale, minScale, singleLine)
                if (sharedScale != null) onSharedScaleTooBig(smaller) else ownScale = smaller
            } else if (tooBig && onDoesNotFit != null) {
                onDoesNotFit()
            } else {
                ready = true
            }
        },
    )
}

private const val STEP = 0.05f
private const val MIN_SCALE = 0.7f

private fun TextUnit.scaled(factor: Float): TextUnit = if (isSpecified) this * factor else this

/** Bricht der Text in einem Wort um oder – mit [singleLine] – überhaupt? */
private fun TextLayoutResult.tooBig(singleLine: Boolean): Boolean = breaksInsideWord(this) || (singleLine && lineCount > 1)

/**
 * Die erste Stufe unter [from] (in Schritten von [STEP], wie bisher Komposition für Komposition),
 * bei der der Text passt; passt er nirgends, [minScale]. Gemessen wird mit den Vorgaben des
 * Layouts [layout] (Text, Breite, Dichte, Schriftfamilien, Zeilenregeln) und nur Schriftgröße und
 * Zeilenhöhe aus [base] skaliert – so wie der Text sie bei dieser Stufe bekäme.
 */
private fun TextMeasurer.fittingScale(layout: TextLayoutResult, base: TextStyle, from: Float, minScale: Float, singleLine: Boolean): Float {
    val input = layout.layoutInput
    var scale = from
    while (scale > minScale) {
        scale = (scale - STEP).coerceAtLeast(minScale)
        val probe = measure(
            text = input.text,
            style = input.style.copy(
                fontSize = if (base.fontSize.isSpecified) base.fontSize.scaled(scale) else input.style.fontSize, // Abweichung TYPO-01: skaliert die übergebene Rolle
                lineHeight = if (base.lineHeight.isSpecified) base.lineHeight.scaled(scale) else input.style.lineHeight, // Abweichung TYPO-01: skaliert die übergebene Rolle
            ),
            overflow = input.overflow,
            softWrap = input.softWrap,
            maxLines = input.maxLines,
            placeholders = input.placeholders,
            constraints = input.constraints,
            layoutDirection = input.layoutDirection,
            density = input.density,
            fontFamilyResolver = input.fontFamilyResolver,
        )
        if (!probe.tooBig(singleLine)) return scale
    }
    return minScale
}

/**
 * Zeilenumbruch an einer Stelle, an der kein Wort endet: zwischen zwei Buchstaben/Ziffern
 * („Resonan|z“) oder vor einem Bindestrich bzw. Satzzeichen, das dann eine Zeile beginnt
 * („Resonanz|-Atmung“, „Sigh|)“). Ein Umbruch nach Leerzeichen oder nach einem Bindestrich
 * („Resonanz-|Atmung“) ist erlaubt – nur nicht vor „·“: Der Trennpunkt beginnt nie eine Zeile,
 * auch nach einem Leerzeichen nicht (TEXT-10, geschütztes Leerzeichen davor). Gemeinsame Regel
 * für [WholeWordText] und ScreenshotTest.
 */
fun breaksInsideWord(layout: TextLayoutResult): Boolean = (0 until layout.lineCount - 1).any { badBreakAt(layout.layoutInput.text.text, layout.getLineEnd(it)) }

/** Siehe [breaksInsideWord]; [end] ist der erste Index der neuen Zeile. */
fun badBreakAt(text: String, end: Int): Boolean {
    if (end !in 1 until text.length) return false
    val before = text[end - 1]
    val after = text[end]
    if (after == DOT) return true
    if (before.isWhitespace()) return false
    return (before.isWordChar() && after.isWordChar()) || after in NO_LINE_START
}

/**
 * Teil eines Worts: Buchstabe, Ziffer oder Längenzeichen. Vorkomponierte Zeichen der
 * Sanskrit-Umschrift („ā“, „ṅ“, TEXT-14) sind Buchstaben; ein nachgestelltes Kombinationszeichen
 * (U+0304 …) zählt mit, damit „a|̄“ als Bruch im Wort gilt und nicht als Wortgrenze.
 */
private fun Char.isWordChar(): Boolean = isLetterOrDigit() || Character.getType(this) == Character.NON_SPACING_MARK.toInt()

private const val NO_LINE_START = "-‐‑–—)]}.,;:!?…“”\"'»«/%" // Abweichung TEXT-10: Zeichenliste, kein UI-Text
private const val DOT = '·'
