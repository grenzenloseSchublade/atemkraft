package app.atemkraft.ui.meditation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.ui.components.FillViewportColumn
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens

/** Gong-Wahl im Tab; die Intervall-Länge steht in den Einstellungen. */
internal enum class GongChoice {
    OFF,
    START_END,
    INTERVAL,
}

/**
 * Meditations-Tab: Auswahl (Zeitring für die Dauer samt offener Sitzung „∞“, darunter die Zeile
 * „Klang & Stimme“, die Gong und Sprachanleitung im Bottom Sheet öffnet) und laufende Sitzung als
 * Overlay (ruhiger Ring + Zeit). Reiner Zustand + Callbacks – der Ablauf liegt im
 * [app.atemkraft.playback.MeditationController].
 */
@Composable
fun MeditationScreen(
    initialConfig: MeditationConfig,
    speechAvailable: Boolean,
    gongIntervalMin: Int,
    onOpenSettings: () -> Unit,
    onDurationTick: (major: Boolean) -> Unit,
    onStart: (MeditationConfig) -> Unit,
) {
    // Der Tab zeigt nur die Auswahl; die laufende/abgeschlossene Sitzung liegt als Vollbild-Overlay
    // darüber ([MeditationOverlay]) – so ist sie (wie die Atem-Session) minimierbar und über die
    // Mini-Leiste erreichbar.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        SelectionContent(initialConfig, speechAvailable, gongIntervalMin, onOpenSettings, onDurationTick, onStart)
    }
}

@Composable
private fun SelectionContent(
    initialConfig: MeditationConfig,
    speechAvailable: Boolean,
    gongIntervalMin: Int,
    onOpenSettings: () -> Unit,
    onDurationTick: (major: Boolean) -> Unit,
    onStart: (MeditationConfig) -> Unit,
) {
    // Saveable: Die Auswahl überlebt den Abstecher in die Einstellungen und Tab-Wechsel. Schlüssel
    // ohne die Intervall-LÄNGE (die ist global, s. SettingsRepository) – sonst würde eine Änderung
    // in den Einstellungen die noch nicht gestartete Auswahl zurücksetzen.
    val key = initialConfig.copy(gongEveryMin = initialConfig.gongEveryMin?.let { 0 })
    // Dauer samt ∞ liegt im DurationPickerState – die Darstellung (Zeitring) liest und ändert nur ihn.
    val duration = rememberDurationPickerState(initialConfig.minutes, initialConfig.mode == MeditationMode.FREE, key)
    var startEndGong by rememberSaveable(key) { mutableStateOf(initialConfig.startEndGong) }
    var intervalOn by rememberSaveable(key) { mutableStateOf(initialConfig.gongEveryMin != null) }
    var speech by rememberSaveable(key) { mutableStateOf(initialConfig.speech) }
    // Offen überlebt das Blatt Drehen; die Seite darunter bleibt unverändert (MUSTER-11).
    var soundSheetOpen by rememberSaveable { mutableStateOf(false) }
    val mode = if (duration.free) MeditationMode.FREE else MeditationMode.TIMED
    // Der Zustand bleibt startEndGong/intervalOn; Zeile und Blatt bilden ihn nur auf eine Wahl ab.
    val gong = when {
        intervalOn -> GongChoice.INTERVAL
        startEndGong -> GongChoice.START_END
        else -> GongChoice.OFF
    }

    // Scrollt nur als Notlösung (LAYOUT-08); dann zeigt der feste Kopf seine Haarlinie.
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.meditation_title),
            action = { SettingsAction(onClick = onOpenSettings) },
            scrolled = scrollState.canScrollBackward,
        )
        // Die Seite scrollt nicht: Der Regler bekommt den ganzen Platz zwischen Kopf und „Klang &
        // Stimme“ und wird nur bei großer Schrift kleiner; Gong und Stimme liegen im Bottom Sheet und
        // nehmen ihm nichts weg. Erst wenn selbst seine Mindestgröße nicht passt (Schrift 2,0),
        // scrollt die Seite als Notlösung – abgeschnitten wird nie.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val minCenter = durationPickerMinSide() + Dimens.GapSmall * 2
            // Kopf oben, der Regler im restlichen Platz, mindestens in seiner Mindestgröße.
            FillViewportColumn(
                viewportHeight = constraints.maxHeight,
                centerHeight = { free -> maxOf(free, minCenter.roundToPx()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = Dimens.ScreenPadding),
                top = { ScreenSubtitle(stringResource(R.string.meditation_subtitle)) },
                center = {
                    // Oben Luft wie über einem Abschnittskopf: Mit nur GapSmall klebte die ∞-Marke am
                    // Untertitel, während unter dem Ring viel Platz frei blieb (Nutzer 2026-10-20). Wird
                    // es knapp, gibt zuerst diese Luft nach – erst dann scrollt die Seite (LAYOUT-08).
                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        val top = (maxHeight - durationPickerMinSide() - Dimens.GapSmall)
                            .coerceIn(Dimens.GapSmall, Dimens.SectionHeaderTop + Dimens.SectionGap)
                        Box(Modifier.fillMaxSize().padding(top = top, bottom = Dimens.GapSmall), contentAlignment = Alignment.TopCenter) {
                            DurationPicker(state = duration, onTick = onDurationTick)
                        }
                    }
                },
                bottom = {
                    SoundRow(
                        gong = gong,
                        gongIntervalMin = gongIntervalMin,
                        speech = speech && speechAvailable,
                        onClick = { soundSheetOpen = true },
                    )
                },
            )
        }
        if (soundSheetOpen) {
            SoundSheet(
                gong = gong,
                gongIntervalMin = gongIntervalMin,
                speech = speech,
                speechAvailable = speechAvailable,
                onGong = { choice ->
                    startEndGong = choice != GongChoice.OFF
                    intervalOn = choice == GongChoice.INTERVAL
                },
                onSpeech = { speech = it },
                onDismiss = { soundSheetOpen = false },
            )
        }

        // Gepinnter Start über der Tab-Leiste (wie auf der Detailseite) – als Split-Button:
        // weicht die Auswahl von den App-Standards ab, gleitet rechts der Kreispfeil herein.
        PinnedActionBar {
            val defaults = MeditationConfig()
            StartSplitButton(
                label = stringResource(R.string.action_start),
                resetVisible = mode != defaults.mode || duration.minutes != defaults.minutes ||
                    startEndGong != defaults.startEndGong ||
                    intervalOn != (defaults.gongEveryMin != null) || speech != defaults.speech,
                onStart = {
                    onStart(
                        MeditationConfig(
                            mode = mode,
                            minutes = duration.minutes,
                            startEndGong = startEndGong,
                            gongEveryMin = if (intervalOn) gongIntervalMin else null,
                            speech = speech && speechAvailable,
                        ),
                    )
                },
                onReset = {
                    duration.select(defaults.minutes)
                    if (defaults.mode == MeditationMode.FREE) duration.selectFree()
                    if (duration.fineAdjust) duration.toggleFineAdjust()
                    startEndGong = defaults.startEndGong
                    intervalOn = defaults.gongEveryMin != null
                    speech = defaults.speech
                },
            )
        }
    }
}

/**
 * Darstellung der Dauerwahl – die EINZIGE Stelle, die sie kennt. Variante F (wischbare Skala)
 * tauscht nur `TimeDial` gegen `TimeRuler` mit derselben Signatur; Zustand, Haptik, Start, Reset
 * und Platzverteilung bleiben unverändert. Beide bekommen den ganzen freien Platz (der Ring nimmt
 * darin das größte Quadrat, oben bündig).
 */
@Composable
private fun DurationPicker(state: DurationPickerState, onTick: (major: Boolean) -> Unit) {
    TimeDial(state = state, onTick = onTick, modifier = Modifier.fillMaxSize(), alignment = Alignment.TopCenter)
}

/**
 * Mindestkante der Dauerwahl: [Dimens.DurationPickerMin], bei großer Schrift so viel mehr, dass die
 * Mitte ganz im Ring bleibt (LAYOUT-03) – die Seite scrollt dann, abgeschnitten wird nie.
 */
@Composable
private fun durationPickerMinSide(): Dp = timeDialMinSide(Dimens.DurationPickerMin)

/**
 * „Klang & Stimme“: eine Zeile mit der aktuellen Wahl darunter und Chevron (Abschnittskopf mit
 * `onClick`); die ganze Zeile öffnet das [SoundSheet] (MUSTER-11). Eine schlichte Sitzung braucht
 * nur die Dauer, der Zeitring darüber behält so immer seine volle Größe (LAYOUT-08).
 */
@Composable
private fun SoundRow(gong: GongChoice, gongIntervalMin: Int, speech: Boolean, onClick: () -> Unit) {
    Column {
        SectionHeader(
            title = stringResource(R.string.meditation_sound_voice_label),
            // Einziger Abschnittskopf des Tabs: erste Farbe des Rings (FARBE-07).
            color = headingColor(0),
            teaser = stringResource(
                R.string.meditation_sound_summary,
                when (gong) {
                    GongChoice.OFF -> stringResource(R.string.meditation_gong_off_short)
                    GongChoice.START_END -> stringResource(R.string.meditation_gong_startstop)
                    GongChoice.INTERVAL -> stringResource(R.string.meditation_interval_minutes, gongIntervalMin)
                },
                stringResource(if (speech) R.string.meditation_voice_on else R.string.meditation_voice_off),
            ),
            onClick = onClick,
        )
        // Die gepinnte Start-Leiste hat selbst ListGap Innenrand – hier nur ein kleiner Abstand.
        Spacer(Modifier.height(Dimens.GapSmall))
    }
}
