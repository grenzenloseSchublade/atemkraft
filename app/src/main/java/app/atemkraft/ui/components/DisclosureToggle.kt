package app.atemkraft.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.domain.withoutLeadingDot
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
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
    TextButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.ButtonHeight).semantics {
            stateDescription = state
        },
        // Endrand: ohne ihn endet der Text genau an der Pillen-Rundung, deren Ecke (Tipp-Echo,
        // Clip) die letzten Glyphen anschneidet (LAYOUT-03, ZUGESCHNITTEN). Vorne steht der Caret.
        contentPadding = PaddingValues(top = Dimens.GapTiny, bottom = Dimens.GapTiny, end = Dimens.GapTiny),
        shape = MaterialTheme.shapes.medium,
    ) {
        ExpandCaret(expanded = expanded, tint = LocalContentColor.current, size = Sizes.IconInButton)
        Spacer(Modifier.width(Dimens.GapTiny))
        if (note == null) {
            Text(text, style = MaterialTheme.typography.labelMedium)
            // TextButton zentriert seinen Inhalt; der Rest der Zeile schiebt ihn an den Anfang.
            Spacer(Modifier.weight(1f))
        } else {
            TitleWithStatus(
                title = text,
                color = LocalContentColor.current,
                status = note,
                statusDescription = note.withoutLeadingDot(),
                statusStyle = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
                isHeading = false,
                titleStyle = MaterialTheme.typography.labelMedium,
                statusVisible = noteVisible,
            )
        }
    }
}
