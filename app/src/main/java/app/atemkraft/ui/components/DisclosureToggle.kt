package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.atemkraft.domain.withoutLeadingDot
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Einheitlicher Ein-/Ausklapp-Umschalter: animierter Caret + Label. Ersetzt die früher pro
 * Screen handgebauten „▾/▴“-TextButtons und liefert konsistente TalkBack-Semantik. Rechts
 * vom Text steht [Dimens.GapTiny] Rand. Tippbar ist die ganze Zeile, nicht nur der Text – sonst
 * trifft ein Tipp rechts daneben nichts (Nutzer 2026-10-20); Caret und Text bleiben links,
 * das Tipp-Echo hat die mittlere Rundung statt der Pille. [enabled] = false, solange der
 * Inhalt offen bleiben muss (Stimmen-Katalog während eines Downloads); Zustand und Name bleiben
 * für TalkBack lesbar. [note] steht leise direkt hinter dem Text („· angepasst“) und gehört zum
 * Namen des Knopfs – neben einem Knopf über die volle Breite hätte es keinen Platz. Text und
 * [note] folgen der Anatomie „Titel · Status“ ([TitleWithStatus], STYLEGUIDE §4.5): passt der
 * Zusatz nicht dahinter, steht er ganz darunter, ohne Punkt. [noteVisible] = false hält nur seinen
 * Platz, damit die Zeile beim Erscheinen nicht springt; TalkBack hört ihn ohne Trennpunkt.
 *
 * Farbe: Aufklappen ist weder Aktion noch Auswahl, der Aufklapper ist eine Unterüberschrift mit
 * Caret – Text in `onSurface`, Caret gedimmt (`SECONDARY`) wie bei `SectionHeader` und
 * `ExpanderSection` (FARBE-04, -07), nie in `primary` (sonst liest er sich wie ein Link oder
 * Hauptknopf). Der Caret steht mittig auf der ersten Textzeile (§4.5), auch wenn [note] darunter
 * rutscht, und sein Glyph bündig an der Textkante: Das Icon hat innen knapp `GapTiny` Rand, um den
 * die Zeile in den Rand ragt ([flushStart]).
 */
@Composable
fun DisclosureToggle(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    note: String? = null,
    noteVisible: Boolean = true,
) {
    val state = expandedStateText(expanded)
    val textStyle = MaterialTheme.typography.labelMedium
    val firstLine = rememberFirstLineHeight(textStyle)
    val onSurface = MaterialTheme.colorScheme.onSurface
    TextButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier.flushStart(Dimens.GapTiny).fillMaxWidth().heightIn(min = Sizes.ButtonHeight).semantics {
            stateDescription = state
        },
        colors = ButtonDefaults.textButtonColors(contentColor = onSurface),
        // Endrand: ohne ihn endet der Text genau an der Pillen-Rundung, deren Ecke (Tipp-Echo,
        // Clip) die letzten Glyphen anschneidet (LAYOUT-03, ZUGESCHNITTEN). Vorne steht der Caret.
        contentPadding = PaddingValues(top = Dimens.GapTiny, bottom = Dimens.GapTiny, end = Dimens.GapTiny),
        shape = MaterialTheme.shapes.medium,
    ) {
        // Gesperrt in der gedämpften Inhaltsfarbe des Buttons, sonst gedimmt wie jeder Caret.
        val caretTint = if (enabled) onSurface.copy(alpha = SECONDARY) else LocalContentColor.current
        // Eigene Zeile, oben ausgerichtet: Der Button zentriert seinen Inhalt in der Mindesthöhe, der
        // Caret steht so auf der ersten Textzeile, nicht oben in der Button-Fläche. Volle Breite
        // schiebt Caret und Text an den Anfang.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Top) {
            Box(Modifier.heightIn(min = firstLine), contentAlignment = Alignment.Center) {
                ExpandCaret(expanded = expanded, tint = caretTint, size = Sizes.IconInButton)
            }
            Spacer(Modifier.width(Dimens.GapTiny))
            if (note == null) {
                Text(text, style = textStyle)
            } else {
                TitleWithStatus(
                    title = text,
                    color = LocalContentColor.current,
                    status = note,
                    statusDescription = note.withoutLeadingDot(),
                    statusStyle = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    isHeading = false,
                    titleStyle = textStyle,
                    statusVisible = noteVisible,
                )
            }
        }
    }
}
