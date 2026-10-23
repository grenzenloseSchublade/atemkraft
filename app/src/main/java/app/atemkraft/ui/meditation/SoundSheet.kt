package app.atemkraft.ui.meditation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens

/**
 * „Klang & Stimme“ als Bottom Sheet (MUSTER-11, wie das Startpunkt-Sheet `TemplateSheet` im
 * Muster-Editor): Titel als Abschnittskopf, darunter die Gong-Wahl und der Schalter
 * „Sprachanleitung“. Jede Änderung gilt sofort, ein Bestätigen gibt es nicht; schließen per
 * Wischen nach unten, Tipp daneben oder Zurück ([onDismiss]). Der Zeitring darunter bleibt, wie er
 * ist.
 *
 * TalkBack: Der Inhalt wird vor dem Griff gelesen (`traversalIndex`), so ist der Titel das erste
 * Element im Blatt und bekommt beim Öffnen den Fokus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoundSheet(
    gong: GongChoice,
    gongIntervalMin: Int,
    speech: Boolean,
    speechAvailable: Boolean,
    onGong: (GongChoice) -> Unit,
    onSpeech: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Eigener, dekorativer Griff statt des M3-Griffs: Dessen Knoten ist nur 32 dp breit (A11Y-05).
    // Unten hält das Blatt schon die Systemleisten frei (contentWindowInsets), ScreenBottom kommt dazu.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BottomSheetDefaults.DragHandle()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Vor dem Griff gelesen: TalkBack beginnt beim Titel (MUSTER-11).
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                }
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenBottom),
        ) {
            SectionHeader(title = stringResource(R.string.meditation_sound_voice_label), color = headingColor(0), top = 0.dp)
            Spacer(Modifier.height(Dimens.HeaderGap))
            SubLabel(stringResource(R.string.meditation_gong_label), Modifier.choiceGroupHeading())
            Spacer(Modifier.height(Dimens.GapSmall))
            // Nur der Modus wird hier gewählt; die Intervall-Länge steht in den Einstellungen.
            SegmentedChoiceRow(GongChoice.entries, gong, onGong, groupLabel = stringResource(R.string.meditation_gong_label)) { choice ->
                when (choice) {
                    GongChoice.OFF -> stringResource(R.string.meditation_off)
                    GongChoice.START_END -> stringResource(R.string.meditation_gong_startstop)
                    GongChoice.INTERVAL -> stringResource(R.string.meditation_interval_minutes, gongIntervalMin)
                }
            }
            // Abstand wie nach dem Dauer-Raster; der Touch-Rand der Segmente allein wirkte zu eng.
            Spacer(Modifier.height(Dimens.GapSmall))
            // Schalter in der Zeile seines Namens, Hinweis darunter; die ganze Zeile schaltet.
            ToggleRow(
                label = stringResource(R.string.meditation_speech_label),
                checked = speech && speechAvailable,
                onCheckedChange = onSpeech,
                hint = if (speechAvailable) {
                    stringResource(R.string.meditation_speech_hint)
                } else {
                    stringResource(R.string.meditation_speech_unavailable)
                },
                enabled = speechAvailable,
            )
        }
    }
}
