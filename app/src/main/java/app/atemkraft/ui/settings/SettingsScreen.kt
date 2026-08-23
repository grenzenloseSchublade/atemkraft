package app.atemkraft.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
import app.atemkraft.ui.components.BackButton
import app.atemkraft.ui.components.DisclosureToggle
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.theme.SECONDARY

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
) {
    BackHandler { onBack() }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            BackButton(onClick = onBack)
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))

            SoundCard(soundMode, transition, volume, onSoundMode, onTransition, onVolume, gongLong, onGongLong)

            Spacer(Modifier.height(16.dp))
            SessionCard(haptics, showSafetyWarning, showNextPhase, onToggleHaptics, onToggleSafety, onToggleNextPhase)

            Spacer(Modifier.height(16.dp))
            MeditationCard(
                gongIntervalMin, onGongInterval,
                voiceStates, activeVoiceId, piperEngineReady,
                onSampleVoice, onDownloadVoice, onSelectVoice, onDeleteVoice,
            )

            Spacer(Modifier.height(16.dp))
            NavRow(stringResource(R.string.settings_glossary), onOpenGlossary)
            Spacer(Modifier.height(10.dp))
            NavRow(stringResource(R.string.settings_about_entry), onOpenAbout)

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoundCard(
    soundMode: SoundMode,
    transition: TransitionEmphasis,
    volume: ToneVolume,
    onSoundMode: (SoundMode) -> Unit,
    onTransition: (TransitionEmphasis) -> Unit,
    onVolume: (ToneVolume) -> Unit,
    gongLong: Boolean,
    onGongLong: (Boolean) -> Unit,
) {
    var soundInfoExpanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardTitle(stringResource(R.string.settings_breathing_title))
            Spacer(Modifier.height(10.dp))
            SubLabel(stringResource(R.string.settings_sound_title))
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = SoundMode.entries
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == soundMode,
                        onClick = { onSoundMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    ) {
                        Text(
                            when (mode) {
                                SoundMode.OFF -> stringResource(R.string.sound_mode_off)
                                SoundMode.CUES -> stringResource(R.string.sound_mode_cues)
                                SoundMode.CONTINUOUS -> stringResource(R.string.sound_mode_continuous)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Hint(stringResource(R.string.settings_sound_appetizer))
            DisclosureToggle(
                text = stringResource(R.string.settings_sound_more),
                expanded = soundInfoExpanded,
                onToggle = { soundInfoExpanded = !soundInfoExpanded },
            )
            if (soundInfoExpanded) {
                Hint(stringResource(R.string.settings_sound_hint))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_sources),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
                Spacer(Modifier.height(4.dp))
                listOf(Refs.respeRate, Refs.shaffer2020, Refs.zaccaro2018).forEach { ref ->
                    ReferenceItem(ref)
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (soundMode == SoundMode.CONTINUOUS) {
                Spacer(Modifier.height(10.dp))
                SubLabel(stringResource(R.string.emphasis_title))
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val items = TransitionEmphasis.entries
                    items.forEachIndexed { index, emphasis ->
                        SegmentedButton(
                            selected = emphasis == transition,
                            onClick = { onTransition(emphasis) },
                            shape = SegmentedButtonDefaults.itemShape(index, items.size),
                        ) {
                            Text(
                                when (emphasis) {
                                    TransitionEmphasis.SOFT -> stringResource(R.string.emphasis_soft)
                                    TransitionEmphasis.MEDIUM -> stringResource(R.string.emphasis_medium)
                                    TransitionEmphasis.STRONG -> stringResource(R.string.emphasis_strong)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Hint(stringResource(R.string.emphasis_hint))
            }
            if (soundMode != SoundMode.OFF) {
                Spacer(Modifier.height(10.dp))
                SubLabel(stringResource(R.string.volume_title))
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val vols = ToneVolume.entries
                    vols.forEachIndexed { index, v ->
                        SegmentedButton(
                            selected = v == volume,
                            onClick = { onVolume(v) },
                            shape = SegmentedButtonDefaults.itemShape(index, vols.size),
                        ) {
                            Text(
                                when (v) {
                                    ToneVolume.QUIET -> stringResource(R.string.volume_quiet)
                                    ToneVolume.MEDIUM -> stringResource(R.string.volume_medium)
                                    ToneVolume.LOUD -> stringResource(R.string.volume_loud)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Hint(stringResource(R.string.volume_hint))
            }

            // Gong-Ausklang: app-weit (Sitzungs-Abschluss- UND Meditations-Gong), daher hier in der
            // allgemeinen Ton-Karte – nicht meditationsspezifisch. Immer sichtbar.
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            SubLabel(stringResource(R.string.settings_gong_length))
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !gongLong,
                    onClick = { onGongLong(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(R.string.gong_length_short)) }
                SegmentedButton(
                    selected = gongLong,
                    onClick = { onGongLong(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(R.string.gong_length_long)) }
            }
            Spacer(Modifier.height(4.dp))
            Hint(stringResource(R.string.settings_gong_length_hint))
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
        Column(modifier = Modifier.padding(16.dp)) {
            CardTitle(stringResource(R.string.settings_session_title))
            Spacer(Modifier.height(4.dp))
            ToggleRow(stringResource(R.string.settings_haptics), haptics, onToggleHaptics)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            ToggleRow(stringResource(R.string.settings_safety), showSafetyWarning, onToggleSafety)
            Hint(stringResource(R.string.settings_safety_hint))
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
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
        Column(modifier = Modifier.padding(16.dp)) {
            CardTitle(stringResource(R.string.settings_meditation_title))

            Spacer(Modifier.height(12.dp))
            SubLabel(stringResource(R.string.settings_gong_interval))
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                GONG_INTERVALS.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = m == gongIntervalMin,
                        onClick = { onGongInterval(m) },
                        shape = SegmentedButtonDefaults.itemShape(index, GONG_INTERVALS.size),
                    ) {
                        Text(stringResource(R.string.meditation_minutes, m))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Hint(stringResource(R.string.settings_gong_interval_hint))

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            SubLabel(stringResource(R.string.settings_voice))
            Hint(stringResource(R.string.settings_voice_catalog_hint))
            Spacer(Modifier.height(4.dp))

            // Stimmen-Katalog: pro Stimme Vorhören → Laden → Wählen/Löschen. Mehrere behaltbar.
            VoiceCatalog.all.forEachIndexed { index, spec ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (active && engineReady) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Hint(voiceSubtitle(spec, state, active, engineReady))
        }

        // Vorhören (funktioniert immer, auch vor dem Download).
        val cdSample = stringResource(R.string.cd_voice_preview)
        IconButton(onClick = onSample, modifier = Modifier.semantics { contentDescription = cdSample }) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
        }

        when (state) {
            is VoiceDownloadState.Downloading ->
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
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
            if (state.extracting) stringResource(R.string.settings_speech_pack_extracting)
            else if (state.indeterminate) stringResource(R.string.voice_loading)
            else stringResource(R.string.settings_speech_pack_downloading, (state.fraction * 100).toInt())
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

/** Unter-Überschrift innerhalb einer Karte. */
@Composable
private fun SubLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
}

/** Dezenter Hinweis-/Beschriftungstext (einheitliche Sekundär-Deckkraft). */
@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
    )
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
