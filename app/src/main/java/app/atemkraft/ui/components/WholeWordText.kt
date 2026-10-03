package app.atemkraft.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * Text, dessen Wörter nie umbrechen (LAYOUT-03). Ist ein einzelnes Wort breiter als die Zeile
 * (lange Komposita bei großer Systemschrift auf schmalen Geräten), wird die Schrift in kleinen
 * Schritten verkleinert, bis jedes Wort ganz in eine Zeile passt – höchstens bis [minScale]
 * (Standard 0,7; nur sehr schmale Plätze wie Tab-Beschriftungen gehen tiefer).
 * Bis dahin wird nicht gezeichnet, es flackert also nichts. Mit [sharedScale] teilen sich
 * gleichrangige Texte (z. B. Tab-Beschriftungen) eine Größe: Die kleinste nötige gilt für alle.
 *
 * Für Titel, Namen und Beschriftungen; Fließtext wird stattdessen so formuliert, dass kein
 * Wort die Zeile sprengt.
 */
@Composable
fun WholeWordText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    minScale: Float = MIN_SCALE,
    sharedScale: MutableFloatState? = null,
) {
    // Neu messen, wenn sich Text, Stil, Schriftgröße oder Bildschirmbreite ändern.
    val config = LocalConfiguration.current
    val ownScale = remember(text, style, config.fontScale, config.screenWidthDp) { mutableFloatStateOf(1f) }
    var scale by (sharedScale ?: ownScale)
    var ready by remember(text, style, config.fontScale, config.screenWidthDp) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        style = if (scale < 1f) style.copy(
            fontSize = style.fontSize.scaled(scale), // Abweichung TYPO-01: skaliert die übergebene Rolle
            lineHeight = style.lineHeight.scaled(scale), // Abweichung TYPO-01: skaliert die übergebene Rolle
        ) else style,
        color = color,
        textAlign = textAlign ?: TextAlign.Unspecified,
        onTextLayout = { layout ->
            if (breaksInsideWord(layout) && scale > minScale) {
                scale = (scale - STEP).coerceAtLeast(minScale)
            } else {
                ready = true
            }
        },
    )
}

private const val STEP = 0.05f
private const val MIN_SCALE = 0.7f

private fun TextUnit.scaled(factor: Float): TextUnit = if (isSpecified) this * factor else this

/**
 * Zeilenumbruch an einer Stelle, an der kein Wort endet: zwischen zwei Buchstaben/Ziffern
 * („Resonan|z“) oder vor einem Bindestrich bzw. Satzzeichen, das dann eine Zeile beginnt
 * („Resonanz|-Atmung“, „Sigh|)“). Ein Umbruch nach Leerzeichen oder nach einem Bindestrich
 * („Resonanz-|Atmung“) ist erlaubt. Gemeinsame Regel für [WholeWordText] und ScreenshotTest.
 */
fun breaksInsideWord(layout: TextLayoutResult): Boolean =
    (0 until layout.lineCount - 1).any { badBreakAt(layout.layoutInput.text.text, layout.getLineEnd(it)) }

/** Siehe [breaksInsideWord]; [end] ist der erste Index der neuen Zeile. */
fun badBreakAt(text: String, end: Int): Boolean {
    if (end !in 1 until text.length) return false
    val before = text[end - 1]
    val after = text[end]
    if (before.isWhitespace()) return false
    return (before.isLetterOrDigit() && after.isLetterOrDigit()) || after in NO_LINE_START
}

private const val NO_LINE_START = "-‐‑–—)]}.,;:!?…“”\"'»«/·%" // Abweichung TEXT-10: Zeichenliste, kein UI-Text
