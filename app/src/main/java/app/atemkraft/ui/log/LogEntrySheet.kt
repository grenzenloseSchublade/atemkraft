package app.atemkraft.ui.log

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.Feeling
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.components.AppBottomSheet
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.feelingText
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.theme.Dimens

/**
 * Blatt „Eintrag“ aus dem Stift einer Logbuch-Zeile (Eigentümer 2026-11-25, Konzeptprüfung M4):
 * Titel ist der Name der Übung, darin das Befinden danach als Antwortreihe wie am Ende-Screen
 * (dieselbe Reihe, `ruhiger · wie vorher · unruhiger` bzw. nach aktivierenden Übungen
 * `wacher · wie vorher · müder`) – nachtragen, ändern, mit einem zweiten Tipp auf die gewählte
 * Antwort wieder zurücknehmen – und am Ende „Eintrag löschen“, zweistufig (MUSTER-05). Jede Änderung
 * gilt sofort (MUSTER-11); nach dem Löschen schließt das Blatt. Die App rechnet nie mit dem Befinden
 * (SEC-LEGAL-04), sie zeigt nur das Wort.
 */
@Composable
internal fun LogEntrySheet(
    entry: SessionLogEntry,
    onSetFeeling: (Feeling?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Welche Reihe: wie am Ende-Screen nach der Familie; ein schon gewähltes Wort der aktivierenden
    // Reihe zeigt diese (eigene sanft aktivierende Muster kennt der Eintrag nur über sein Wort).
    val scale = if (entry.feeling in Feeling.ACTIVATING_SCALE && entry.feeling != Feeling.SAME) {
        Feeling.ACTIVATING_SCALE
    } else {
        Feeling.scaleFor(entry.family, activating = false)
    }
    val feelingLabel = stringResource(R.string.log_entry_feeling)
    AppBottomSheet(title = entry.exerciseName, onDismiss = onDismiss) {
        // Löschen schließt erst das Blatt, dann verschwindet der Eintrag (closeThen).
        val sheet = this
        SubLabel(feelingLabel, Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow<Feeling?>(
            items = scale,
            selected = entry.feeling,
            onSelect = { choice -> onSetFeeling(choice.takeIf { it != entry.feeling }) },
            groupLabel = feelingLabel,
        ) { option -> option?.let { feelingText(it) }.orEmpty() }
        Spacer(Modifier.height(Dimens.SectionGap))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WholeWordText(
                text = stringResource(R.string.log_entry_delete),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Dimens.GapSmall))
            ConfirmIconButton(
                name = entry.exerciseName,
                description = stringResource(R.string.cd_log_entry_delete, entry.exerciseName),
                onConfirm = { sheet.closeThen(onDelete) },
                modifier = Modifier.flushEnd(),
                confirmDescription = stringResource(R.string.cd_log_entry_delete_confirm),
            )
        }
    }
}
