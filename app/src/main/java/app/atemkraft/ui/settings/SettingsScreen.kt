package app.atemkraft.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceCatalog
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.cue.tts.VoiceGender
import app.atemkraft.cue.tts.VoiceQuality
import app.atemkraft.cue.tts.VoiceSpec
import app.atemkraft.data.Refs
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.GONG_INTERVAL_CHOICES
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CardTitle
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.DisclosureToggle
import app.atemkraft.ui.components.ForwardChevron
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.WarnAmber
import kotlinx.coroutines.flow.first

/**
 * Einstellungen: oben das Wissen (Atemlehre, Glossar), dann Ton, Atmen, Sitzung & Sicherheit,
 * Meditation und Daten, unten Über.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onOpenGlossary: () -> Unit,
    onOpenAtemlehre: () -> Unit,
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fest über der Scroll-Fläche: Nach dem Sprung zur Meditations-Karte bliebe der
            // Pfeil sonst außerhalb des Bildes (einziger sichtbarer Weg zurück).
            PushHeader(onBack = onBack, title = stringResource(R.string.settings_title))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.SectionGap))
                if (showResetNotice) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        BodyText(stringResource(R.string.settings_reset_notice), modifier = Modifier.padding(Dimens.CardPadding))
                    }
                    Spacer(Modifier.height(Dimens.SectionGap))
                }

                // Die Kartentitel sind die Köpfe dieser Seite und gehen reihum durch den Ring, ohne
                // Bedeutung (FARBE-07): Wissen 0, Klang 1, Atmen 2, Sitzung 3, Meditation 4, Daten 5.
                // Wissen zuerst: Atemlehre und Glossar sollen nicht unter den Karten verschwinden.
                KnowledgeCard(onOpenAtemlehre = onOpenAtemlehre, onOpenGlossary = onOpenGlossary)

                Spacer(Modifier.height(Dimens.ListGap))
                TonCard(state.volume, actions::setVolume, state.gongLong, actions::setGongLong, actions::previewGong)

                Spacer(Modifier.height(Dimens.ListGap))
                AtmenCard(state.soundMode, state.transition, actions::setSoundMode, actions::setTransition)

                Spacer(Modifier.height(Dimens.ListGap))
                SessionCard(
                    state.haptics,
                    state.showSafetyWarning,
                    state.showNextPhase,
                    actions::setHaptics,
                    actions::setShowSafetyWarning,
                    actions::setShowNextPhase,
                )

                Spacer(Modifier.height(Dimens.ListGap))
                // Box nur als Messpunkt für den Sprung aus dem Meditations-Tab.
                Box(Modifier.onPlaced { meditationCardY = it.positionInParent().y.toInt() }) {
                    MeditationCard(
                        state.gongIntervalMin, actions::setGongInterval,
                        state.voiceStates, state.activeVoiceId, state.piperEngineReady,
                        actions::sampleVoice, actions::downloadVoice, actions::selectVoice, actions::deleteVoice,
                        columns,
                    )
                }

                Spacer(Modifier.height(Dimens.ListGap))
                DataCard(transfer, state.logCount, actions::clearLog)

                Spacer(Modifier.height(Dimens.ListGap))
                NavRow(stringResource(R.string.settings_about_entry), onOpenAbout)

                Spacer(Modifier.height(Dimens.SectionGap))
                HintText(stringResource(R.string.settings_about))
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
        }
    }
}

/** Wissen: Atemlehre und Glossar als Verweise in einer Karte, je mit kurzem Untertitel. */
@Composable
private fun KnowledgeCard(onOpenAtemlehre: () -> Unit, onOpenGlossary: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_knowledge_title), color = headingColor(0))
            Spacer(Modifier.height(Dimens.GapTiny))
            KnowledgeRow(
                title = stringResource(R.string.settings_atemlehre),
                hint = stringResource(R.string.settings_atemlehre_hint),
                onClick = onOpenAtemlehre,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
            KnowledgeRow(
                title = stringResource(R.string.settings_glossary),
                hint = stringResource(R.string.settings_glossary_hint),
                onClick = onOpenGlossary,
            )
        }
    }
}

/**
 * Verweis in der Wissen-Karte: Titel, Untertitel, Chevron. Die ganze Zeile ist ein Button
 * (TalkBack liest Titel und Untertitel als einen Namen), mindestens Touch-Ziel-hoch.
 */
@Composable
private fun KnowledgeRow(title: String, hint: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Dimens.GapTiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            WholeWordText(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HintText(hint)
        }
        ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
    }
}

/**
 * Daten: gespeicherte Muster als Datei sichern und wieder einlesen (Storage Access Framework,
 * keine Berechtigung, keine Cloud), der Handywechsel als Aufklapper; unten das Logbuch als Zeile
 * mit Papierkorb, zweistufig (MUSTER-05).
 */
@Composable
private fun DataCard(transfer: PatternTransfer, logCount: Int, onClearLog: () -> Unit) {
    var transferInfoExpanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_data_title), color = headingColor(5))
            Spacer(Modifier.height(Dimens.GapTiny))
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
}

/**
 * Zeile „Logbuch“, gebaut wie eine Stimmen-Zeile: links Titel und Zahl der Sitzungen, rechts der
 * Papierkorb in derselben Icon-Spalte. Leeren zweistufig im selben Element (MUSTER-05,
 * [ConfirmIconButton]: „Logbuch wirklich leeren?“). Ohne Einträge gibt es nichts zu leeren – dann
 * fehlt der Papierkorb, die Zeile behält ihre Höhe.
 */
@Composable
private fun LogRow(count: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.GapTiny)
            .heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_tone_title), color = headingColor(1))
            Spacer(Modifier.height(Dimens.GapSmall))
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
            // Vorhören: spielt den Gong im aktuell gewählten Profil (Kurz/Lang umschalten → erneut
            // tippen). Steht an der Kartenkante wie die Icons der übrigen Zeilen – in der Spalte der
            // Stimmenliste schwebte er allein mitten in der Zeile (Nutzer 2026-10-20).
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubLabel(stringResource(R.string.settings_gong_length), Modifier.weight(1f).choiceGroupHeading())
                TrailingIconCell { PreviewButton(stringResource(R.string.cd_gong_preview), onPreviewGong) }
            }
            SegmentedChoiceRow(listOf(false, true), gongLong, onGongLong, groupLabel = stringResource(R.string.settings_gong_length)) { long ->
                stringResource(if (long) R.string.gong_length_long else R.string.gong_length_short)
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            HintText(stringResource(R.string.settings_gong_length_hint))
        }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_breathing_title), color = headingColor(2))
            Spacer(Modifier.height(Dimens.GapSmall))
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
}

@Composable
private fun SessionCard(
    haptics: Boolean,
    showSafetyWarning: Boolean,
    showNextPhase: Boolean,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleSafety: (Boolean) -> Unit,
    onToggleNextPhase: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_session_title), color = headingColor(3))
            Spacer(Modifier.height(Dimens.GapTiny))
            ToggleRow(stringResource(R.string.settings_haptics), haptics, onToggleHaptics)
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
            ToggleRow(
                label = stringResource(R.string.settings_safety),
                checked = showSafetyWarning,
                onCheckedChange = onToggleSafety,
                hint = stringResource(R.string.settings_safety_hint),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
            ToggleRow(
                label = stringResource(R.string.settings_next_phase),
                checked = showNextPhase,
                onCheckedChange = onToggleNextPhase,
                hint = stringResource(R.string.settings_next_phase_hint),
            )
        }
    }
}

private const val GONG_COLUMNS = 2

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
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_meditation_title), color = headingColor(4))

            Spacer(Modifier.height(Dimens.GapSmall))
            SubLabel(stringResource(R.string.settings_gong_interval), Modifier.choiceGroupHeading())
            Spacer(Modifier.height(Dimens.GapSmall))
            // 2 × 2 wie das Dauer-Raster im Meditations-Tab: „✓ 15 min“ passt in der Karte nicht
            // in ein Viertel der Breite, sonst stünde hier schon bei normaler Schrift eine Radioliste.
            SegmentedChoiceRow(
                GONG_INTERVAL_CHOICES,
                gongIntervalMin,
                onGongInterval,
                columns = GONG_COLUMNS,
                groupLabel = stringResource(R.string.settings_gong_interval),
            ) { m ->
                stringResource(R.string.meditation_minutes, m)
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            HintText(stringResource(R.string.settings_gong_interval_hint))

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
            // Zugeklappt nur die aktive Stimme; der Katalog klappt darunter auf (Umschalter wie „Wie
            // funktioniert das?“). Solange eine Stimme lädt, bleibt er offen, damit Fortschritt und
            // Abbrechen im Bild bleiben.
            var catalogExpanded by rememberSaveable { mutableStateOf(false) }
            val downloading = voiceStates.values.any { it is VoiceDownloadState.Downloading }
            val showCatalog = catalogExpanded || downloading
            val active = VoiceCatalog.all.firstOrNull { it.id == activeVoiceId && voiceStates[it.id] is VoiceDownloadState.Downloaded }
            SubLabel(stringResource(R.string.settings_voice))
            // Aufgeklappt zeigt der Katalog die aktive Stimme selbst; hier stünde sie doppelt.
            if (!showCatalog) {
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
            }
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
}

/**
 * Eine Zeile im Stimmen-Katalog: Name/Info links, rechts die festen Spalten ([TrailingCells]):
 * Vorhören, Zustands-Aktion (Laden · Wählen · „✓ Aktiv“ · Fortschritt), Löschen bzw. Abbrechen.
 */
@Composable
private fun VoiceRow(
    spec: VoiceSpec,
    state: VoiceDownloadState,
    active: Boolean,
    engineReady: Boolean,
    columns: TrailingColumns,
    onSample: () -> Unit,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    TrailingRow(
        columns,
        modifier = Modifier.padding(vertical = Dimens.GapTiny),
        // Vorhören (funktioniert immer, auch vor dem Download).
        play = { PreviewButton(stringResource(R.string.cd_voice_preview, spec.displayName), onSample) },
        label = { VoiceStateAction(state, active, engineReady, onDownload, onSelect) },
        icon = {
            when (state) {
                // Abbrechen: delete() bricht den laufenden Download-Job ab und räumt Reste weg.
                is VoiceDownloadState.Downloading -> AppIconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_voice_cancel),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                    )
                }

                // Zweistufig (MUSTER-05): Eine Stimme neu zu laden kostet bis zu 60 MB.
                is VoiceDownloadState.Downloaded -> ConfirmIconButton(
                    name = spec.displayName,
                    description = stringResource(R.string.cd_voice_delete, spec.displayName),
                    onConfirm = onDelete,
                )

                else -> Unit
            }
        },
    ) {
        Column {
            // Name immer in onSurface: Den Zustand trägt allein „✓ Aktiv“ (FARBE-05).
            WholeWordText(
                text = spec.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HintText(voiceSubtitle(spec, state, active, engineReady))
            if (spec.nonCommercialOnly) {
                // Eigene Zeile in Warnfarbe statt im grauen Untertitel: Die Einschränkung soll vor
                // dem Laden auffallen (SEC-LEGAL-03), ohne die Zeile breiter zu machen (LAYOUT-03).
                WholeWordText(
                    text = stringResource(R.string.voice_non_commercial),
                    style = MaterialTheme.typography.bodySmall,
                    color = WarnAmber,
                )
            }
        }
    }
}

/** Mittlere Spalte einer Stimmen-Zeile: was mit der Stimme gerade geht oder ist. */
@Composable
private fun VoiceStateAction(
    state: VoiceDownloadState,
    active: Boolean,
    engineReady: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
) {
    when (state) {
        // Mit bekannter Größe zeigt der Kreis den Fortschritt (dazu „40 %“ im Untertitel für
        // TalkBack); beim Entpacken oder ohne Größe dreht er unbestimmt.
        is VoiceDownloadState.Downloading ->
            if (state.extracting || state.indeterminate) {
                CircularProgressIndicator(modifier = Modifier.size(Sizes.ProgressInline), strokeWidth = Sizes.ProgressStroke)
            } else {
                CircularProgressIndicator(
                    progress = { state.fraction },
                    modifier = Modifier.size(Sizes.ProgressInline),
                    strokeWidth = Sizes.ProgressStroke,
                )
            }

        is VoiceDownloadState.Downloaded -> when {
            // Aktiv-Label erst zeigen, wenn die Engine wirklich bereit ist (gleiche Quelle
            // wie der Untertitel → keine widersprüchlichen Signale beim Umschalten).
            active && engineReady -> Text(
                text = stringResource(R.string.settings_voice_active),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            active -> CircularProgressIndicator(modifier = Modifier.size(Sizes.ProgressInline), strokeWidth = Sizes.ProgressStroke)

            else -> AppTextButton(onClick = onSelect) { Text(stringResource(R.string.settings_voice_choose), maxLines = 1) }
        }

        else -> // NotDownloaded / Failed
            AppTextButton(onClick = onDownload) { Text(stringResource(R.string.settings_voice_download), maxLines = 1) }
    }
}

/**
 * Feste Endspalten der Einstellungs-Zeilen (S-38): [labelWidth] ist die Breite der mittleren
 * Spalte, gemessen an der breitesten Zustands-Beschriftung ([rememberTrailingColumns]).
 */
@Immutable
private class TrailingColumns(val labelWidth: Dp)

/**
 * Misst die breiteste Beschriftung der Zustands-Spalte („Laden“, „Wählen“ im Text-Button,
 * „✓ Aktiv“) in der aktuellen Schriftgröße, plus Innenpolster des Text-Buttons. So passt jede
 * einzeilig hinein (LAYOUT-03), und die Spalten stehen in allen Zeilen gleich.
 */
@Composable
private fun rememberTrailingColumns(): TrailingColumns {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val button = MaterialTheme.typography.labelLarge
    val status = MaterialTheme.typography.labelMedium
    val labels = listOf(
        stringResource(R.string.settings_voice_download) to button,
        stringResource(R.string.settings_voice_choose) to button,
        stringResource(R.string.settings_voice_active) to status,
    )
    return remember(measurer, density, direction, labels) {
        val padding = ButtonDefaults.TextButtonContentPadding
        with(density) {
            val text = labels.maxOf { (label, style) -> measurer.measure(label, style, maxLines = 1).size.width }
            // Polster wie im Button pro Seite gerundet; dazu eine Haarlinie Luft gegen Rundung.
            val px = text + padding.calculateStartPadding(direction).roundToPx() + padding.calculateEndPadding(direction).roundToPx() +
                Dimens.GapHairline.roundToPx()
            TrailingColumns(labelWidth = px.toDp())
        }
    }
}

/**
 * Zeile mit festen rechten Spalten (S-38): [lead] links, dann Vorhören ([play], 48 dp), Zustand
 * ([label], [TrailingColumns.labelWidth]) und Icon-Aktion ([icon], [TrailingIconCell]). Leere
 * Fächer behalten ihre Breite, damit Play-Knöpfe und Aktionen über alle Zeilen der Seite
 * untereinander stehen – auch die Gong-Zeile, die nur [play] hat ([label] = null).
 *
 * Passt [lead] nicht daneben (große Schrift auf schmalen Geräten), bricht kein Wort um (LAYOUT-03):
 * Zeilen mit Aktionen brauchen dafür nur ihr längstes Wort, und reicht das nicht, stehen die
 * Spalten unter dem Text, weiter rechtsbündig untereinander. Die Gong-Zeile hält ihr Label
 * einzeilig; ihr Play rückt dafür erst ans Zeilenende, bevor es unter das Label geht.
 */
@Composable
private fun TrailingRow(
    columns: TrailingColumns,
    play: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    icon: @Composable () -> Unit = {},
    lead: @Composable () -> Unit,
) {
    val cells: @Composable () -> Unit = {
        if (label != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(columns.labelWidth), contentAlignment = Alignment.Center) { label() }
                TrailingIconCell(icon)
            }
        }
    }
    Layout(
        contents = listOf(lead, { Box(Modifier.width(Dimens.MinTouchTarget), contentAlignment = Alignment.Center) { play() } }, cells),
        modifier = modifier.fillMaxWidth(),
    ) { (leadMeasurables, playMeasurables, cellMeasurables), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints()
        val playCell = playMeasurables.single().measure(loose)
        val cellsPlaced = cellMeasurables.singleOrNull()?.measure(loose)
        // Breite der Fächer rechts vom Play, auch wenn die Zeile selbst keine hat (Gong).
        val restWidth = cellsPlaced?.width ?: (columns.labelWidth + Dimens.MinTouchTarget - Sizes.TouchOverhang).roundToPx()
        val leadMeasurable = leadMeasurables.single()
        val needed = if (label != null) {
            leadMeasurable.minIntrinsicWidth(Constraints.Infinity)
        } else {
            leadMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        }
        val beside = width - playCell.width - restWidth
        val playEnd = width - playCell.width
        when {
            needed <= beside || (label == null && needed <= playEnd) -> {
                val leadWidth = if (needed <= beside) beside else playEnd
                val leadPlaced = leadMeasurable.measure(Constraints(maxWidth = leadWidth))
                val height = maxOf(leadPlaced.height, playCell.height, cellsPlaced?.height ?: 0)
                layout(width, height) {
                    leadPlaced.placeRelative(0, (height - leadPlaced.height) / 2)
                    playCell.placeRelative(leadWidth, (height - playCell.height) / 2)
                    cellsPlaced?.placeRelative(leadWidth + playCell.width, (height - cellsPlaced.height) / 2)
                }
            }

            else -> {
                val leadPlaced = leadMeasurable.measure(Constraints(maxWidth = width))
                val cellsHeight = maxOf(playCell.height, cellsPlaced?.height ?: 0)
                val playX = (width - restWidth - playCell.width).coerceAtLeast(0)
                layout(width, leadPlaced.height + cellsHeight) {
                    leadPlaced.placeRelative(0, 0)
                    playCell.placeRelative(playX, leadPlaced.height + (cellsHeight - playCell.height) / 2)
                    cellsPlaced?.placeRelative(playX + playCell.width, leadPlaced.height + (cellsHeight - cellsPlaced.height) / 2)
                }
            }
        }
    }
}

/**
 * Icon-Spalte am Zeilenende: 48 dp breit, um den Touch-Rand schmaler gemeldet ([flushEnd]), damit
 * der Glyph an der Inhaltskante steht (LAYOUT-07) – Papierkorb von Stimmen und Logbuch untereinander.
 */
@Composable
private fun TrailingIconCell(content: @Composable () -> Unit) {
    Box(Modifier.flushEnd(inCard = true).width(Dimens.MinTouchTarget), contentAlignment = Alignment.Center) { content() }
}

/** Vorhören (Gong, Stimmen): Play in voller Deckkraft, überall derselbe Knopf. */
@Composable
private fun PreviewButton(description: String, onClick: () -> Unit) {
    AppIconButton(onClick = onClick) {
        Icon(Icons.Filled.PlayArrow, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}

/** Untertitel einer Katalog-Zeile: Geschlecht · Qualität · Lizenz (+ Zustand). */
@Composable
private fun voiceSubtitle(
    spec: VoiceSpec,
    state: VoiceDownloadState,
    active: Boolean,
    engineReady: Boolean,
): String {
    val gender = when (spec.gender) {
        VoiceGender.MALE -> stringResource(R.string.voice_gender_male)
        VoiceGender.FEMALE -> stringResource(R.string.voice_gender_female)
    }
    val quality = stringResource(
        when (spec.quality) {
            VoiceQuality.STUDIO_CLEAR -> R.string.voice_quality_studio_clear
            VoiceQuality.STUDIO -> R.string.voice_quality_studio
            VoiceQuality.SIMPLE -> R.string.voice_quality_simple
            VoiceQuality.SIMPLE_COMPACT -> R.string.voice_quality_simple_compact
        },
    )
    val base = listOf(gender, quality, spec.license).joinToString(DOT_SEPARATOR)
    val extra = when (state) {
        is VoiceDownloadState.NotDownloaded -> stringResource(R.string.voice_size_mb, spec.approxMb)

        is VoiceDownloadState.Downloading ->
            if (state.extracting) {
                stringResource(R.string.settings_speech_pack_extracting)
            } else if (state.indeterminate) {
                stringResource(R.string.voice_loading)
            } else {
                stringResource(R.string.settings_speech_pack_downloading, (state.fraction * 100).toInt())
            }

        is VoiceDownloadState.Failed -> stringResource(R.string.voice_failed_short)

        is VoiceDownloadState.Downloaded ->
            if (active && !engineReady) stringResource(R.string.settings_speech_pack_preparing) else null
    }
    return if (extra != null) base + DOT_SEPARATOR + extra else base
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            // Mindesthöhe hält die klickbare Karte bei kompaktem Padding auf Touch-Ziel-Größe.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            WholeWordText(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
        }
    }
}
