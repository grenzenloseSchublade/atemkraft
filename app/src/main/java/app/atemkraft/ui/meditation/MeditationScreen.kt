package app.atemkraft.ui.meditation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.ui.components.BreathingCircle
import app.atemkraft.ui.components.CircleTapTarget
import app.atemkraft.ui.components.FinishedPanel
import app.atemkraft.ui.components.GlowText
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.components.OverlayChrome
import app.atemkraft.ui.components.PauseFlash
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SessionControlBar
import app.atemkraft.ui.components.SessionRunningLayout
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.breathingDiscDiameter
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.rememberTapFlash
import app.atemkraft.ui.components.sessionSummaryParts
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion

/** Gong-Wahl im Tab; die Intervall-Länge steht in den Einstellungen. */
private enum class GongChoice {
    OFF,
    START_END,
    INTERVAL,
}

/**
 * Meditations-Tab: Auswahl (Zeitring für die Dauer samt offener Sitzung „∞“, eingeklappt Gong und
 * Sprachanleitung) und laufende Sitzung als Overlay (ruhiger Ring + Zeit). Reiner Zustand + Callbacks – der Ablauf
 * liegt im [app.atemkraft.playback.MeditationController].
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
    // Standardmäßig zu: Aufgeklappt darf die Seite auf 360 dp scrollen (LAYOUT-08, Ausnahme S-40).
    var soundExpanded by rememberSaveable { mutableStateOf(false) }
    val mode = if (duration.free) MeditationMode.FREE else MeditationMode.TIMED

    // Scrollt nur als Notlösung (LAYOUT-08); dann zeigt der feste Kopf seine Haarlinie.
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.meditation_title),
            action = { SettingsAction(onClick = onOpenSettings) },
            scrolled = scrollState.canScrollBackward,
        )
        // Die Seite scrollt nicht: Der Regler bekommt den Platz zwischen Kopf und „Klang & Stimme“
        // und wird bei großer Schrift oder aufgeklapptem Klang kleiner. Er hängt oben unter dem Kopf,
        // damit er beim Aufklappen nur schrumpft und nicht wandert. Erst wenn selbst seine
        // Mindestgröße nicht passt, scrollt die Seite als Notlösung – abgeschnitten wird nie.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxHeight
            FillBetweenLayout(
                viewportHeight = viewport,
                minCenterHeight = durationPickerMinSide() + Dimens.GapSmall * 2,
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
                    SoundSection(
                        startEndGong = startEndGong,
                        intervalOn = intervalOn,
                        gongIntervalMin = gongIntervalMin,
                        speech = speech,
                        speechAvailable = speechAvailable,
                        expanded = soundExpanded,
                        onExpandedChange = { soundExpanded = it },
                        onGong = { choice ->
                            startEndGong = choice != GongChoice.OFF
                            intervalOn = choice == GongChoice.INTERVAL
                        },
                        onSpeech = { speech = it },
                    )
                },
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
 * Kopf oben, [center] im restlichen Platz der Ansichtshöhe [viewportHeight], [bottom] darunter.
 * [top] und [bottom] behalten ihre natürliche Höhe; [center] bekommt die volle Breite und den
 * Rest, mindestens [minCenterHeight]. Reicht die Ansicht dafür nicht, wird das Ganze höher als
 * [viewportHeight] (der Aufrufer scrollt dann) – so wird nie etwas abgeschnitten. Gedacht für
 * einen Eltern-`verticalScroll`, in dem die Höhe sonst unbegrenzt wäre.
 */
@Composable
private fun FillBetweenLayout(
    viewportHeight: Dp,
    minCenterHeight: Dp,
    top: @Composable () -> Unit,
    center: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(top, center, bottom), modifier = modifier) { (topM, centerM, bottomM), constraints ->
        val w = constraints.maxWidth
        val loose = Constraints(maxWidth = w)
        val topP = topM.map { it.measure(loose) }
        val bottomP = bottomM.map { it.measure(loose) }
        val topH = topP.sumOf { it.height }
        val bottomH = bottomP.sumOf { it.height }
        val centerH = maxOf(viewportHeight.roundToPx() - topH - bottomH, minCenterHeight.roundToPx())
        val centerP = centerM.map { it.measure(Constraints.fixed(w, centerH)) }
        layout(w, topH + centerH + bottomH) {
            var y = 0
            topP.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
            centerP.forEach { it.placeRelative(0, y) }
            y += centerH
            bottomP.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}

/**
 * „Klang & Stimme“: zugeklappt nur eine Zeile mit der aktuellen Wahl (MUSTER-03) – eine schlichte
 * Sitzung braucht nur die Dauer. Aufgeklappt Gong und Sprachanleitung. Der Zustand bleibt
 * startEndGong/intervalOn, die Reihe bildet ihn nur auf eine Wahl ab.
 */
@Composable
private fun SoundSection(
    startEndGong: Boolean,
    intervalOn: Boolean,
    gongIntervalMin: Int,
    speech: Boolean,
    speechAvailable: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGong: (GongChoice) -> Unit,
    onSpeech: (Boolean) -> Unit,
) {
    val gong = when {
        intervalOn -> GongChoice.INTERVAL
        startEndGong -> GongChoice.START_END
        else -> GongChoice.OFF
    }
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
                stringResource(
                    if (speech && speechAvailable) R.string.meditation_voice_on else R.string.meditation_voice_off,
                ),
            ),
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        )
        // Zügig auf- und zuklappen: Der Ring darüber folgt der Höhe Bild für Bild und schrumpft
        // stetig statt zu springen. Bei Animator-Skala 0 springt beides ohne Übergang (MOTION-02).
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(Motion.Expand)) + fadeIn(tween(Motion.Expand)),
            exit = shrinkVertically(tween(Motion.Expand)) + fadeOut(tween(Motion.Expand)),
        ) {
            SoundOptions(
                gong = gong,
                gongIntervalMin = gongIntervalMin,
                speech = speech,
                speechAvailable = speechAvailable,
                onGong = onGong,
                onSpeech = onSpeech,
            )
        }
        // Die gepinnte Start-Leiste hat selbst ListGap Innenrand – hier nur ein kleiner Abstand.
        Spacer(Modifier.height(Dimens.GapSmall))
    }
}

/** Aufgeklappter Inhalt von „Klang & Stimme“: Gong-Wahl und Sprachanleitung. */
@Composable
private fun SoundOptions(
    gong: GongChoice,
    gongIntervalMin: Int,
    speech: Boolean,
    speechAvailable: Boolean,
    onGong: (GongChoice) -> Unit,
    onSpeech: (Boolean) -> Unit,
) {
    Column {
        // Kopf → Inhalt wie überall HeaderGap; GapSmall davon polstert schon der aufklappbare Kopf.
        Spacer(Modifier.height(Dimens.HeaderGap - Dimens.GapSmall))
        SubLabel(stringResource(R.string.meditation_gong_label), Modifier.choiceGroupHeading(), color = MaterialTheme.colorScheme.onBackground)
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
