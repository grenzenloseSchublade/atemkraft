package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.BODY
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.WarnAmber

// Textbausteine für die Textrollen außerhalb der Köpfe: Fließtext, Hinweis, Sekundärtext,
// Aufzählung und Kartentitel. Die Farbe folgt der Fläche (`LocalContentColor`: onBackground auf dem Screen,
// onSurface in der Karte), die Deckkraft kommt aus den Text-Tokens (§2.2).

/**
 * Fließtext: Beschreibungen, Detail- und Expander-Text, Glossar, Über – `bodySmall` in `BODY`.
 * [style] nur für die Kurzbeschreibung eine Stufe größer (`bodyMedium`). Sanskrit kursiv
 * ([sanskritText]).
 */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) {
    Text(text = sanskritText(text), style = style, color = LocalContentColor.current.copy(alpha = BODY), modifier = modifier)
}

/**
 * Hinweis: zurückgenommener Zusatz unter einer Kontrolle, Quellenzeile, Zitat, Fußzeile –
 * `bodySmall` in `SECONDARY`, nie im Wort umbrochen ([WholeWordText]).
 */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    WholeWordText(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = LocalContentColor.current.copy(alpha = SECONDARY),
        modifier = modifier,
    )
}

/**
 * Sekundärtext: zurückgenommene Zeile mit normalem Umbruch – Kurzbeschreibung und Musterzeile auf
 * Karten, Teaser unter einem Abschnittskopf, Quellenzitat, Uhrzeit und Runden im Logbuch,
 * Trefferzahl der Suche, Anleitung unter dem Atemkreis. `bodySmall` in `SECONDARY` wie [HintText],
 * aber als schlichter `Text`: Er bricht an Wortgrenzen um wie Fließtext und verkleinert nie.
 * [textAlign] nur für zentrierte Zeilen (Session). Sanskrit kursiv ([sanskritText]).
 */
@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text = sanskritText(text),
        style = MaterialTheme.typography.bodySmall,
        color = LocalContentColor.current.copy(alpha = SECONDARY),
        textAlign = textAlign,
        modifier = modifier,
    )
}

/**
 * Warnzeile „⚠ …“ in `WarnAmber`, `bodySmall`, normaler Umbruch wie [SecondaryText]: Sicherheit
 * und Intensität mit Zeichen und Text, nie nur Farbe (FARBE-05). Detailseite intensiver Übungen,
 * Atemlehre-Karten mit Warnung, intensive Empfehlung und Bereich Hochregeln im Atmen-Tab.
 * [text] ohne das Zeichen.
 */
@Composable
fun WarnText(text: String, modifier: Modifier = Modifier) {
    Text(text = "⚠ $text", style = MaterialTheme.typography.bodySmall, color = WarnAmber, modifier = modifier)
}

/**
 * Titel einer Karte (Einstellungen, Glossar): `titleMedium`, für TalkBack eine Überschrift, damit
 * sich lange Seiten per Überschriften-Navigation durchspringen lassen (A11Y-01). [color] ist
 * `onSurface`, außer die Kartentitel sind die eine farbige Kopf-Ebene des Screens (Einstellungen:
 * [headingColor], FARBE-07).
 */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    WholeWordText(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Aufzählungspunkt: Glyph „•“ und [text] in einer Zeile, der Text bricht neben dem Glyph um.
 * Glyph in derselben Rolle wie der Text, damit er auf Höhe der ersten Zeile steht. TalkBack
 * liest nur den Text, der Glyph ist kein Fokus-Stopp (A11Y-06). [color] ist die Textfarbe, der
 * Glyph steht zurückgenommen in `SECONDARY`.
 */
@Composable
fun Bullet(text: AnnotatedString, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    val style = MaterialTheme.typography.bodySmall
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
        Text(
            text = BULLET_GLYPH,
            style = style,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            modifier = DecorativeGlyph,
        )
        Text(text = text, style = style, color = color)
    }
}

/** [Bullet] für schlichten Text (Sanskrit kursiv, [sanskritText]). */
@Composable
fun Bullet(text: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Bullet(sanskritText(text), modifier, color)
}

private const val BULLET_GLYPH = "•"

/**
 * Für TalkBack unsichtbar. Nicht `clearAndSetSemantics`, damit das Text-Layout für die Umbruch-
 * und Zuschnittprüfung im Screenshot-Test sichtbar bleibt.
 */
@OptIn(ExperimentalComposeUiApi::class)
private val DecorativeGlyph = Modifier.semantics { invisibleToUser() }

/**
 * Höhe der ersten Textzeile in [style], gemessen mit der aktuellen Schrift – für Marken, Punkte
 * und Icons, die mittig auf der ersten Zeile eines Texts stehen sollen. Gemessen statt aus
 * `lineHeight` gerechnet: Ab Android 14 wächst große Schrift nichtlinear, die gerenderte Zeile
 * ist dann höher als ihr `lineHeight` in dp.
 */
@Composable
fun rememberFirstLineHeight(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(measurer, density, style) { with(density) { measurer.measure(ONE_LINE, style, maxLines = 1).size.height.toDp() } }
}

/** Ein Zeichen für die Messung einer Zeile ([rememberFirstLineHeight]). */
private const val ONE_LINE = "A"
