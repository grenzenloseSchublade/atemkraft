package app.atemkraft.ui.meditation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.GONG_INTERVAL_CHOICES
import app.atemkraft.ui.components.AppBottomSheet
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.flushStart
import app.atemkraft.ui.theme.Dimens

/**
 * „Klang & Stimme“ als Bottom Sheet (MUSTER-11, wie das Startpunkt-Sheet `TemplateSheet` im
 * Muster-Editor): Titel als Abschnittskopf, darunter die Gong-Wahl – bei „alle … min“ gleich
 * darunter der Gong-Abstand ([onGongInterval]; seit 2026-10-24 hier statt in den Einstellungen,
 * wo man ihn neben „alle 5 min“ suchte, ohne ihn ändern zu können) – und der Schalter
 * „Sprachanleitung“. Kann das Gerät kein Deutsch sprechen, führt „Stimme laden ›“ zum
 * Stimmen-Katalog der Einstellungen ([onOpenVoices]) statt in eine Sackgasse. Jede Änderung gilt
 * sofort, ein Bestätigen gibt es nicht; schließen per Wischen nach unten, Tipp daneben oder Zurück
 * ([onDismiss]). Der Zeitring darunter bleibt, wie er ist.
 *
 * Gerüst: [AppBottomSheet]. Vorbild für den Inhalt eines Einstell-Blatts (§4.5): Gruppen mit
 * [SubLabel], Steuerungen direkt auf dem Blatt, ohne Karte und Trenner, alle an einer Textkante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoundSheet(
    gong: GongChoice,
    gongIntervalMin: Int,
    speech: Boolean,
    speechAvailable: Boolean,
    onGong: (GongChoice) -> Unit,
    onGongInterval: (Int) -> Unit,
    onSpeech: (Boolean) -> Unit,
    onOpenVoices: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(title = stringResource(R.string.meditation_sound_voice_label), onDismiss = onDismiss) {
        SubLabel(stringResource(R.string.meditation_gong_label), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow(GongChoice.entries, gong, onGong, groupLabel = stringResource(R.string.meditation_gong_label)) { choice ->
            when (choice) {
                GongChoice.OFF -> stringResource(R.string.meditation_off)
                GongChoice.START_END -> stringResource(R.string.meditation_gong_startstop)
                GongChoice.INTERVAL -> stringResource(R.string.meditation_interval_minutes, gongIntervalMin)
            }
        }
        // Der Abstand gehört zur Wahl „alle … min“ und steht nur bei ihr (app-weit gespeichert).
        if (gong == GongChoice.INTERVAL) {
            Spacer(Modifier.height(Dimens.GapSmall))
            SubLabel(stringResource(R.string.settings_gong_interval), Modifier.choiceGroupHeading())
            Spacer(Modifier.height(Dimens.GapSmall))
            SegmentedChoiceRow(
                GONG_INTERVAL_CHOICES,
                gongIntervalMin,
                onGongInterval,
                groupLabel = stringResource(R.string.settings_gong_interval),
            ) { m ->
                stringResource(R.string.meditation_minutes, m)
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
        // Ohne deutsche Sprachausgabe: der Weg zu den Stimmen der App, bündig unter dem Hinweis.
        if (!speechAvailable) {
            AppTextButton(onClick = onOpenVoices, modifier = Modifier.flushStart()) {
                ForwardLinkText(text = stringResource(R.string.meditation_voice_get), color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
