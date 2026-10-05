package app.atemkraft.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceCatalog
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.cue.tts.VoiceGender
import app.atemkraft.cue.tts.VoiceSpec
import app.atemkraft.data.Refs
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.BackButton
import app.atemkraft.ui.components.DisclosureToggle
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import kotlinx.coroutines.flow.first

/** Einstellungen: Ton (Atmen), Sitzung & Sicherheit, Meditation, Quellen/Über. */
@Composable
fun SettingsScreen(
    soundMode: SoundMode,
    transition: TransitionEmphasis,
    volume: ToneVolume,
    haptics: Boolean,
    showSafetyWarning: Boolean,
    showNextPhase: Boolean,
    onSoundMode: (SoundMode) -> Unit,
    onTransition: (TransitionEmphasis) -> Unit,
    onVolume: (ToneVolume) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleSafety: (Boolean) -> Unit,
    onToggleNextPhase: (Boolean) -> Unit,
    gongIntervalMin: Int,
    onGongInterval: (Int) -> Unit,
    gongLong: Boolean,
    onGongLong: (Boolean) -> Unit,
    onPreviewGong: () -> Unit,
    voiceStates: Map<String, VoiceDownloadState>,
    activeVoiceId: String?,
    piperEngineReady: Boolean,
    onSampleVoice: (VoiceSpec) -> Unit,
    onDownloadVoice: (String) -> Unit,
    onSelectVoice: (String?) -> Unit,
    onDeleteVoice: (String) -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    onExportPatterns: () -> Unit,
    onImportPatterns: () -> Unit,
    dataStatus: DataStatus = DataStatus(),
    focusMeditation: Boolean = false,
) {
    val scrollState = rememberScrollState()
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            Spacer(Modifier.height(Dimens.ScreenTopSub))
            BackButton(onClick = onBack)
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Dimens.SectionGap))

            TonCard(volume, onVolume, gongLong, onGongLong, onPreviewGong)

            Spacer(Modifier.height(Dimens.ListGap))
            AtmenCard(soundMode, transition, onSoundMode, onTransition)

            Spacer(Modifier.height(Dimens.ListGap))
            SessionCard(haptics, showSafetyWarning, showNextPhase, onToggleHaptics, onToggleSafety, onToggleNextPhase)

            Spacer(Modifier.height(Dimens.ListGap))
            // Box nur als Messpunkt für den Sprung aus dem Meditations-Tab.
            Box(Modifier.onPlaced { meditationCardY = it.positionInParent().y.toInt() }) {
                MeditationCard(
                    gongIntervalMin, onGongInterval,
                    voiceStates, activeVoiceId, piperEngineReady,
                    onSampleVoice, onDownloadVoice, onSelectVoice, onDeleteVoice,
                )
            }

            Spacer(Modifier.height(Dimens.ListGap))
            DataCard(onExportPatterns, onImportPatterns, dataStatus)

            Spacer(Modifier.height(Dimens.ListGap))
            NavRow(stringResource(R.string.settings_glossary), onOpenGlossary)
            Spacer(Modifier.height(Dimens.ListGap))
            NavRow(stringResource(R.string.settings_about_entry), onOpenAbout)

            Spacer(Modifier.height(Dimens.SectionGap))
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            )
            Spacer(Modifier.height(Dimens.ScreenBottom))
        }
    }
}

/** Rückmeldung der Daten-Karte: Erfolg als Button-Label (MUSTER-02), sonst eine Hinweiszeile. */
data class DataStatus(val exportLabel: String? = null, val importLabel: String? = null, val note: String? = null)

/**
 * Daten: gespeicherte Muster als Datei sichern und wieder einlesen (Storage Access Framework,
 * keine Berechtigung, keine Cloud).
 */
@Composable
private fun DataCard(onExport: () -> Unit, onImport: () -> Unit, status: DataStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_data_title))
            Spacer(Modifier.height(Dimens.GapTiny))
            Hint(stringResource(R.string.settings_data_hint))
            AdaptiveButtonRow(modifier = Modifier.fillMaxWidth(), spacing = Dimens.GapSmall) {
                DataButton(status.exportLabel ?: stringResource(R.string.settings_data_export), onExport)
                DataButton(status.importLabel ?: stringResource(R.string.settings_data_import), onImport)
            }
            if (status.note != null) {
                Text(
                    text = status.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall))
            Hint(stringResource(R.string.settings_data_transfer_hint))
        }
    }
}

/** Button der Daten-Karte; das Label wechselt zum Ergebnis und wird angesagt. */
@Composable
private fun DataButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(label)
    }
}

/** Allgemeine Ton-Karte: gilt app-weit (Atem-Cues, Sitzungs- und Meditations-Gong). */
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
            CardTitle(stringResource(R.string.settings_tone_title))
            Spacer(Modifier.height(Dimens.GapSmall))
            SubLabel(stringResource(R.string.volume_title))
            Spacer(Modifier.height(Dimens.GapSmall))
            SegmentedChoiceRow(ToneVolume.entries, volume, onVolume) { v ->
                when (v) {
                    ToneVolume.QUIET -> stringResource(R.string.volume_quiet)
                    ToneVolume.MEDIUM -> stringResource(R.string.volume_medium)
                    ToneVolume.LOUD -> stringResource(R.string.volume_loud)
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            Hint(stringResource(R.string.volume_hint))

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubLabel(stringResource(R.string.settings_gong_length))
                Spacer(Modifier.weight(1f))
                // Vorhören: spielt den Gong im aktuell gewählten Profil (Kurz/Lang umschalten → erneut tippen).
                val cdGong = stringResource(R.string.cd_gong_preview)
                IconButton(onClick = onPreviewGong, modifier = Modifier.semantics { contentDescription = cdGong }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                }
            }
            SegmentedChoiceRow(listOf(false, true), gongLong, onGongLong) { long ->
                stringResource(if (long) R.string.gong_length_long else R.string.gong_length_short)
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            Hint(stringResource(R.string.settings_gong_length_hint))
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
            CardTitle(stringResource(R.string.settings_breathing_title))
            Spacer(Modifier.height(Dimens.GapSmall))
            SubLabel(stringResource(R.string.settings_sound_title))
            Spacer(Modifier.height(Dimens.GapSmall))
            SegmentedChoiceRow(SoundMode.entries, soundMode, onSoundMode) { mode ->
                when (mode) {
                    SoundMode.OFF -> stringResource(R.string.sound_mode_off)
                    SoundMode.CUES -> stringResource(R.string.sound_mode_cues)
                    SoundMode.CONTINUOUS -> stringResource(R.string.sound_mode_continuous)
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            Hint(stringResource(R.string.settings_sound_appetizer))
            DisclosureToggle(
                text = stringResource(R.string.settings_sound_more),
                expanded = soundInfoExpanded,
                onToggle = { soundInfoExpanded = !soundInfoExpanded },
            )
            if (soundInfoExpanded) {
                Hint(stringResource(R.string.settings_sound_hint))
                Spacer(Modifier.height(Dimens.GapSmall))
                Text(
                    text = stringResource(R.string.settings_sources),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
                Spacer(Modifier.height(Dimens.GapTiny))
                listOf(Refs.respeRate, Refs.shaffer2020, Refs.zaccaro2018).forEach { ref ->
                    ReferenceItem(ref)
                    Spacer(Modifier.height(Dimens.GapTiny))
                }
            }
            if (soundMode == SoundMode.CONTINUOUS) {
                Spacer(Modifier.height(Dimens.GapSmall))
                SubLabel(stringResource(R.string.emphasis_title))
                Spacer(Modifier.height(Dimens.GapSmall))
                SegmentedChoiceRow(TransitionEmphasis.entries, transition, onTransition) { emphasis ->
                    when (emphasis) {
                        TransitionEmphasis.SOFT -> stringResource(R.string.emphasis_soft)
                        TransitionEmphasis.MEDIUM -> stringResource(R.string.emphasis_medium)
                        TransitionEmphasis.STRONG -> stringResource(R.string.emphasis_strong)
                    }
                }
                Spacer(Modifier.height(Dimens.GapTiny))
                Hint(stringResource(R.string.emphasis_hint))
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
            CardTitle(stringResource(R.string.settings_session_title))
            Spacer(Modifier.height(Dimens.GapTiny))
            ToggleRow(stringResource(R.string.settings_haptics), haptics, onToggleHaptics)
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny))
            ToggleRow(stringResource(R.string.settings_safety), showSafetyWarning, onToggleSafety)
            Hint(stringResource(R.string.settings_safety_hint))
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny))
            ToggleRow(stringResource(R.string.settings_next_phase), showNextPhase, onToggleNextPhase)
            Hint(stringResource(R.string.settings_next_phase_hint))
        }
    }
}

private val GONG_INTERVALS = listOf(3, 5, 10, 15)

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
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardTitle(stringResource(R.string.settings_meditation_title))

            Spacer(Modifier.height(Dimens.GapSmall))
            SubLabel(stringResource(R.string.settings_gong_interval))
            Spacer(Modifier.height(Dimens.GapSmall))
            SegmentedChoiceRow(GONG_INTERVALS, gongIntervalMin, onGongInterval) { m ->
                stringResource(R.string.meditation_minutes, m)
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            Hint(stringResource(R.string.settings_gong_interval_hint))

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall))
            SubLabel(stringResource(R.string.settings_voice))
            Hint(stringResource(R.string.settings_voice_catalog_hint))
            Spacer(Modifier.height(Dimens.GapTiny))

            // Stimmen-Katalog: pro Stimme Vorhören → Laden → Wählen/Löschen. Mehrere behaltbar.
            VoiceCatalog.all.forEachIndexed { index, spec ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapHairline))
                VoiceRow(
                    spec = spec,
                    state = voiceStates[spec.id] ?: VoiceDownloadState.NotDownloaded,
                    active = spec.id == activeVoiceId,
                    engineReady = piperEngineReady,
                    onSample = { onSampleVoice(spec) },
                    onDownload = { onDownloadVoice(spec.id) },
                    onSelect = { onSelectVoice(spec.id) },
                    onDelete = { onDeleteVoice(spec.id) },
                )
            }
        }
    }
}

/** Eine Zeile im Stimmen-Katalog: Name/Info + Vorhören + Zustands-Aktion + Löschen. */
@Composable
private fun VoiceRow(
    spec: VoiceSpec,
    state: VoiceDownloadState,
    active: Boolean,
    engineReady: Boolean,
    onSample: () -> Unit,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.GapTiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            WholeWordText(
                text = spec.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (active && engineReady) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Hint(voiceSubtitle(spec, state, active, engineReady))
        }

        // Vorhören (funktioniert immer, auch vor dem Download).
        val cdSample = stringResource(R.string.cd_voice_preview)
        IconButton(onClick = onSample, modifier = Modifier.semantics { contentDescription = cdSample }) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
        }

        when (state) {
            is VoiceDownloadState.Downloading -> {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                // Abbrechen: delete() bricht den laufenden Download-Job ab und räumt Reste weg.
                val cdCancel = stringResource(R.string.cd_voice_cancel)
                IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = cdCancel }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                    )
                }
            }

            is VoiceDownloadState.Downloaded -> {
                when {
                    // Aktiv-Label erst zeigen, wenn die Engine wirklich bereit ist (gleiche Quelle
                    // wie Highlight/Untertitel → keine widersprüchlichen Signale beim Umschalten).
                    active && engineReady -> Text(
                        text = stringResource(R.string.settings_voice_active),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    active && !engineReady ->
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)

                    else ->
                        TextButton(onClick = onSelect) { Text(stringResource(R.string.settings_voice_choose)) }
                }
                val cdDelete = stringResource(R.string.cd_voice_delete)
                IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = cdDelete }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                    )
                }
            }

            else -> // NotDownloaded / Failed
                TextButton(onClick = onDownload) { Text(stringResource(R.string.settings_voice_download)) }
        }
    }
}

/** Untertitel einer Katalog-Zeile: Geschlecht · Qualität (+ Zustand). */
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
        VoiceGender.SPECIAL -> stringResource(R.string.voice_gender_special)
    }
    val base = "$gender · ${spec.qualityLabel}"
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
    return if (extra != null) "$base · $extra" else base
}

/** Karten-Titel (einheitlich titleLarge, app-weit). */
@Composable
private fun CardTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
}

/** Dezenter Hinweis-/Beschriftungstext (einheitliche Sekundär-Deckkraft). */
@Composable
private fun Hint(text: String) {
    WholeWordText(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
    )
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            // Mindesthöhe hält die klickbare Karte bei kompaktem Padding auf Touch-Ziel-Größe.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.GapTiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WholeWordText(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
