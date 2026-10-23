package app.atemkraft.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceCatalog
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.cue.tts.VoiceSpec
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.Refs
import app.atemkraft.data.SessionDisplay
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.GONG_INTERVAL_CHOICES
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CardTitle
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.DisclosureToggle
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.SECONDARY
import kotlinx.coroutines.flow.first

/**
 * Einstellungen: Ton, Atmen, Sitzung & Sicherheit, Während der Atemübung, Meditation und Daten,
 * unten Über. Atemlehre und Begriffe stehen seit 2026-10-23 im Tab „Lernen“.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    transfer: PatternTransfer = PatternTransfer.Idle,
    focusMeditation: Boolean = false,
) {
    val scrollState = rememberScrollState()
    val columns = rememberTrailingColumns()
    // Vom Meditations-Tab aus geöffnet: einmal direkt zur Meditations-Karte springen. Gemerkt
    // (rememberSaveable), damit die Rückkehr aus Glossar/Über die Scroll-Position nicht überschreibt.
    var meditationCardY by remember { mutableIntStateOf(-1) }
    val topMarginPx = with(LocalDensity.current) { Dimens.CardPadding.roundToPx() }
    var focusDone by rememberSaveable { mutableStateOf(!focusMeditation) }
    LaunchedEffect(focusDone) {
        if (focusDone) return@LaunchedEffect
        val y = snapshotFlow { meditationCardY }.first { it >= 0 }
        // Etwas Luft über der Karte, damit sie nicht an der Oberkante klebt.
        scrollState.scrollTo((y - topMarginPx).coerceAtLeast(0))
        focusDone = true
    }
    // Nach einer beschädigten Einstellungsdatei einmal melden: Der Hinweis bleibt für diesen
    // Besuch der Seite stehen (auch beim Drehen), die Marke gilt sofort als gemeldet.
    var showResetNotice by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.settingsResetPending) {
        if (state.settingsResetPending) {
            showResetNotice = true
            actions.acknowledgeSettingsReset()
        }
    }
    // Kopf fest über der Scroll-Fläche: Nach dem Sprung zur Meditations-Karte bliebe der Pfeil
    // sonst außerhalb des Bildes (einziger sichtbarer Weg zurück).
    PushScaffold(onBack = onBack, title = stringResource(R.string.settings_title), scrollState = scrollState) {
        if (showResetNotice) {
            Card(modifier = Modifier.fillMaxWidth()) {
                BodyText(stringResource(R.string.settings_reset_notice), modifier = Modifier.padding(Dimens.CardPadding))
            }
            Spacer(Modifier.height(Dimens.SectionGap))
        }

        // Die Kartentitel sind die Köpfe dieser Seite und gehen reihum durch den Ring, ohne
        // Bedeutung (FARBE-07): Klang 0, Atmen 1, Sitzung 2, Während der Atemübung 3,
        // Meditation 4, Daten 5.
        TonCard(state.volume, actions::setVolume, state.gongLong, actions::setGongLong, actions::previewGong)

        Spacer(Modifier.height(Dimens.ListGap))
        AtmenCard(state.soundMode, state.transition, actions::setSoundMode, actions::setTransition)

        Spacer(Modifier.height(Dimens.ListGap))
        SessionCard(state.haptics, state.showSafetyWarning, actions::setHaptics, actions::setShowSafetyWarning)

        Spacer(Modifier.height(Dimens.ListGap))
        SessionDisplayCard(state.sessionDisplay, actions)

        Spacer(Modifier.height(Dimens.ListGap))
        // Box nur als Messpunkt für den Sprung aus dem Meditations-Tab.
        Box(Modifier.onPlaced { meditationCardY = it.positionInParent().y.toInt() }) {
            MeditationCard(
                state.gongIntervalMin, actions::setGongInterval,
                state.voiceStates, state.activeVoiceId, state.piperEngineReady,
                actions::sampleVoice, actions::downloadVoice, actions::selectVoice, actions::deleteVoice,
                columns,
                display = { MeditationDisplayRows(state.meditationDisplay, actions) },
            )
        }

        Spacer(Modifier.height(Dimens.ListGap))
        DataCard(transfer, state.logCount, actions::clearLog)

        Spacer(Modifier.height(Dimens.ListGap))
        NavCard(title = stringResource(R.string.settings_about_entry), onClick = onOpenAbout)

        Spacer(Modifier.height(Dimens.SectionGap))
        HintText(stringResource(R.string.settings_about))
    }
}

/**
 * Daten: gespeicherte Muster als Datei sichern und wieder einlesen (Storage Access Framework,
 * keine Berechtigung, keine Cloud), der Handywechsel als Aufklapper; unten die Zahl der
 * Logbuch-Sitzungen mit Papierkorb, zweistufig (MUSTER-05).
 */
@Composable
private fun DataCard(transfer: PatternTransfer, logCount: Int, onClearLog: () -> Unit) {
    var transferInfoExpanded by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.settings_data_title), heading = 5, titleGap = Dimens.GapTiny) {
        HintText(stringResource(R.string.settings_data_hint))
        Spacer(Modifier.height(Dimens.GapSmall))
        // Kurze Labels, damit beide gleich breit nebeneinander passen; dass es um Muster geht,
        // sagt der Hinweis darüber. Passen sie nicht, stehen sie untereinander (LAYOUT-03).
        AdaptiveButtonRow(modifier = Modifier.fillMaxWidth(), spacing = Dimens.GapSmall) {
            DataButton(transfer.exportLabel ?: stringResource(R.string.settings_data_export), transfer.export)
            DataButton(transfer.importLabel ?: stringResource(R.string.settings_data_import), transfer.import)
        }
        if (transfer.note != null) {
            Spacer(Modifier.height(Dimens.GapTiny))
            Text(
                text = transfer.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        // Wie „Wie funktioniert das?“ in der Atmen-Karte: Der lange Hinweis steht erst auf Wunsch da.
        DisclosureToggle(
            text = stringResource(R.string.settings_data_transfer_more),
            expanded = transferInfoExpanded,
            onToggle = { transferInfoExpanded = !transferInfoExpanded },
        )
        if (transferInfoExpanded) BodyText(stringResource(R.string.settings_data_transfer_hint))
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        LogRow(logCount, onClearLog)
    }
}

/**
 * Zeile „Logbuch“: links Titel und Zahl der Sitzungen, rechts in derselben Icon-Spalte wie bei
 * den Stimmen der Papierkorb. Leeren zweistufig im selben Element (MUSTER-05,
 * [ConfirmIconButton]: „Logbuch wirklich leeren?“). Ohne Einträge gibt es nichts zu leeren – dann
 * fehlt der Papierkorb, die Zeile behält ihre Höhe. Geöffnet wird das Logbuch über den Kalender
 * im Kopf des Atmen-Tabs und am Ende einer Übung (Nutzer 2026-10-22), nicht hier.
 */
@Composable
private fun LogRow(count: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = Dimens.GapTiny)) {
            WholeWordText(
                text = stringResource(R.string.settings_log_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HintText(
                if (count > 0) pluralStringResource(R.plurals.log_sessions, count, count) else stringResource(R.string.settings_log_empty),
            )
        }
        if (count > 0) {
            TrailingIconCell {
                ConfirmIconButton(
                    name = stringResource(R.string.settings_log_title),
                    description = stringResource(R.string.settings_data_clear_log),
                    onConfirm = onClear,
                    confirmDescription = stringResource(R.string.cd_log_clear_confirm),
                )
            }
        }
    }
}

/** Umrandeter Button der Daten-Karte; das Label wechselt zum Ergebnis und wird angesagt. */
@Composable
private fun DataButton(label: String, onClick: () -> Unit) {
    AppOutlinedButton(onClick = onClick, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(label)
    }
}

/** Allgemeine Klang-Karte: gilt app-weit (Atem-Cues, Schluss-Gong der Atemübungen, Meditations-Gongs). */
@Composable
private fun TonCard(
    volume: ToneVolume,
    onVolume: (ToneVolume) -> Unit,
    gongLong: Boolean,
    onGongLong: (Boolean) -> Unit,
    onPreviewGong: () -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_tone_title), heading = 0) {
        SubLabel(stringResource(R.string.volume_title), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow(ToneVolume.entries, volume, onVolume, groupLabel = stringResource(R.string.volume_title)) { v ->
            when (v) {
                ToneVolume.QUIET -> stringResource(R.string.volume_quiet)
                ToneVolume.MEDIUM -> stringResource(R.string.volume_medium)
                ToneVolume.LOUD -> stringResource(R.string.volume_loud)
            }
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(stringResource(R.string.volume_hint))

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        SubLabel(stringResource(R.string.settings_gong_length), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        // Drittel-Raster wie die übrigen Auswahlreihen: Kurz/Lang belegen zwei Drittel, das
        // Vorhören steht im freien dritten Platz auf Höhe der Felder (Nutzer 2026-10-20).
        // Spielt den Gong im aktuell gewählten Profil.
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedChoiceRow(
                listOf(false, true),
                gongLong,
                onGongLong,
                modifier = Modifier.weight(2f),
                groupLabel = stringResource(R.string.settings_gong_length),
            ) { long ->
                stringResource(if (long) R.string.gong_length_long else R.string.gong_length_short)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PreviewButton(stringResource(R.string.cd_gong_preview), onPreviewGong)
            }
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(stringResource(R.string.settings_gong_length_hint))
    }
}

/** Atem-spezifische Klang-Einstellungen (nur für Atem-Sessions). */
@Composable
private fun AtmenCard(
    soundMode: SoundMode,
    transition: TransitionEmphasis,
    onSoundMode: (SoundMode) -> Unit,
    onTransition: (TransitionEmphasis) -> Unit,
) {
    var soundInfoExpanded by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.settings_breathing_title), heading = 1) {
        SubLabel(stringResource(R.string.settings_sound_title), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow(SoundMode.entries, soundMode, onSoundMode, groupLabel = stringResource(R.string.settings_sound_title)) { mode ->
            when (mode) {
                SoundMode.OFF -> stringResource(R.string.sound_mode_off)
                SoundMode.CUES -> stringResource(R.string.sound_mode_cues)
                SoundMode.CONTINUOUS -> stringResource(R.string.sound_mode_continuous)
            }
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(stringResource(R.string.settings_sound_appetizer))
        DisclosureToggle(
            text = stringResource(R.string.settings_sound_more),
            expanded = soundInfoExpanded,
            onToggle = { soundInfoExpanded = !soundInfoExpanded },
        )
        if (soundInfoExpanded) {
            // Aufgeklappt Fließtext wie im Expander; die Quellen folgen ohne Zwischenzeile –
            // der Umschalter benennt den Inhalt schon (wie die Detailseite).
            BodyText(stringResource(R.string.settings_sound_hint))
            Spacer(Modifier.height(Dimens.GapSmall))
            listOf(Refs.respeRate, Refs.shaffer2020, Refs.zaccaro2018).forEach { ref ->
                ReferenceItem(ref)
                Spacer(Modifier.height(Dimens.GapTiny))
            }
        }
        if (soundMode == SoundMode.CONTINUOUS) {
            Spacer(Modifier.height(Dimens.GapSmall))
            SubLabel(stringResource(R.string.emphasis_title), Modifier.choiceGroupHeading())
            Spacer(Modifier.height(Dimens.GapSmall))
            SegmentedChoiceRow(TransitionEmphasis.entries, transition, onTransition, groupLabel = stringResource(R.string.emphasis_title)) { emphasis ->
                when (emphasis) {
                    TransitionEmphasis.SOFT -> stringResource(R.string.emphasis_soft)
                    TransitionEmphasis.MEDIUM -> stringResource(R.string.emphasis_medium)
                    TransitionEmphasis.STRONG -> stringResource(R.string.emphasis_strong)
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            HintText(stringResource(R.string.emphasis_hint))
        }
    }
}

@Composable
private fun SessionCard(
    haptics: Boolean,
    showSafetyWarning: Boolean,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleSafety: (Boolean) -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_session_title), heading = 2, titleGap = Dimens.GapTiny) {
        ToggleRow(stringResource(R.string.settings_haptics), haptics, onToggleHaptics)
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_safety),
            checked = showSafetyWarning,
            onCheckedChange = onToggleSafety,
            hint = stringResource(R.string.settings_safety_hint),
        )
    }
}

/** Was die laufende Atem-Session zeigt; alles abschaltbar für eine möglichst ruhige Ansicht. */
@Composable
private fun SessionDisplayCard(display: SessionDisplay, actions: SettingsActions) {
    SettingsCard(stringResource(R.string.settings_display_title), heading = 3, titleGap = Dimens.GapTiny) {
        ToggleRow(
            label = stringResource(R.string.settings_auto_calm),
            checked = display.autoCalm,
            onCheckedChange = actions::setAutoCalm,
            hint = stringResource(R.string.settings_auto_calm_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_progress),
            checked = display.showProgress,
            onCheckedChange = actions::setShowProgress,
            hint = stringResource(R.string.settings_progress_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_seconds),
            checked = display.showSeconds,
            onCheckedChange = actions::setShowSeconds,
            hint = stringResource(R.string.settings_seconds_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_next_phase),
            checked = display.showNextPhase,
            onCheckedChange = actions::setShowNextPhase,
            hint = stringResource(R.string.settings_next_phase_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_silence_after),
            checked = display.silenceAfter,
            onCheckedChange = actions::setSilenceAfter,
            hint = stringResource(R.string.settings_silence_after_hint),
        )
    }
}

/** Was die laufende Meditation zeigt; wie bei der Atemübung alles abschaltbar. */
@Composable
private fun MeditationDisplayRows(display: MeditationDisplay, actions: SettingsActions) {
    Column {
        ToggleRow(
            label = stringResource(R.string.settings_auto_calm),
            checked = display.autoCalm,
            onCheckedChange = actions::setMeditationAutoCalm,
            hint = stringResource(R.string.settings_auto_calm_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_progress),
            checked = display.showProgress,
            onCheckedChange = actions::setMeditationShowProgress,
            hint = stringResource(R.string.settings_med_progress_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_med_time),
            checked = display.showTime,
            onCheckedChange = actions::setMeditationShowTime,
            hint = stringResource(R.string.settings_med_time_hint),
        )
    }
}

@Composable
private fun MeditationCard(
    gongIntervalMin: Int,
    onGongInterval: (Int) -> Unit,
    voiceStates: Map<String, VoiceDownloadState>,
    activeVoiceId: String?,
    piperEngineReady: Boolean,
    onSampleVoice: (VoiceSpec) -> Unit,
    onDownloadVoice: (String) -> Unit,
    onSelectVoice: (String?) -> Unit,
    onDeleteVoice: (String) -> Unit,
    columns: TrailingColumns,
    display: @Composable () -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_meditation_title), heading = 4) {
        SubLabel(stringResource(R.string.settings_gong_interval), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        // Eine Reihe à vier: ohne Häkchen passt „15 min“ in ein Viertel der Kartenbreite.
        SegmentedChoiceRow(
            GONG_INTERVAL_CHOICES,
            gongIntervalMin,
            onGongInterval,
            groupLabel = stringResource(R.string.settings_gong_interval),
        ) { m ->
            stringResource(R.string.meditation_minutes, m)
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(stringResource(R.string.settings_gong_interval_hint))

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        display()

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        // Zugeklappt nur die aktive Stimme; der Katalog klappt darunter auf (Umschalter wie „Wie
        // funktioniert das?“). Solange eine Stimme lädt, bleibt er offen, damit Fortschritt und
        // Abbrechen im Bild bleiben.
        var catalogExpanded by rememberSaveable { mutableStateOf(false) }
        val downloading = voiceStates.values.any { it is VoiceDownloadState.Downloading }
        val showCatalog = catalogExpanded || downloading
        val active = VoiceCatalog.all.firstOrNull { it.id == activeVoiceId && voiceStates[it.id] is VoiceDownloadState.Downloaded }
        SubLabel(stringResource(R.string.settings_voice))
        // Bleibt auch aufgeklappt stehen: Verschwände die Zeile, spränge der Umschalter beim
        // Tippen eine Zeile nach oben, weg vom Finger (Nutzer 2026-10-20) – wie der Teaser von
        // „Klang & Stimme“ in der Meditation.
        HintText(
            when {
                active != null && piperEngineReady -> active.displayName

                active != null -> active.displayName + DOT_SEPARATOR + stringResource(R.string.settings_speech_pack_preparing)

                else -> stringResource(
                    if (voiceStates.values.any { it is VoiceDownloadState.Downloaded }) {
                        R.string.settings_voice_none_chosen
                    } else {
                        R.string.settings_voice_none_installed
                    },
                )
            },
        )
        DisclosureToggle(
            text = stringResource(if (showCatalog) R.string.settings_voice_hide else R.string.settings_voice_show),
            expanded = showCatalog,
            onToggle = { catalogExpanded = !showCatalog },
            enabled = !downloading,
        )
        if (showCatalog) {
            // Quelle vor dem ersten „Laden“ (SECURITY SEC-NET-02).
            HintText(stringResource(R.string.settings_voice_catalog_hint))
            Spacer(Modifier.height(Dimens.GapTiny))

            // Stimmen-Katalog: pro Stimme Vorhören → Laden → Wählen/Löschen. Mehrere behaltbar.
            VoiceCatalog.all.forEachIndexed { index, spec ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapHairline), color = DividerSubtle)
                // key: Der Bestätigungszustand des Löschens gehört zur Stimme, nicht zur Position.
                key(spec.id) {
                    VoiceRow(
                        spec = spec,
                        state = voiceStates[spec.id] ?: VoiceDownloadState.NotDownloaded,
                        active = spec.id == activeVoiceId,
                        engineReady = piperEngineReady,
                        columns = columns,
                        onSample = { onSampleVoice(spec) },
                        onDownload = { onDownloadVoice(spec.id) },
                        onSelect = { onSelectVoice(spec.id) },
                        onDelete = { onDeleteVoice(spec.id) },
                    )
                }
            }
        }
    }
}

/**
 * Eine Karte der Einstellungen: Titel als farbiger Kopf der Seite ([heading], reihum, FARBE-07),
 * darunter nach [titleGap] der Inhalt; Polster `CardPadding`. [titleGap] ist `GapTiny`, wo der
 * Inhalt mit einer Zeile beginnt, die sich selbst polstert (Schalter, Hinweis), sonst `GapSmall`.
 */
@Composable
private fun SettingsCard(
    title: String,
    heading: Int,
    titleGap: Dp = Dimens.GapSmall,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(title, color = headingColor(heading))
            Spacer(Modifier.height(titleGap))
            content()
        }
    }
}
