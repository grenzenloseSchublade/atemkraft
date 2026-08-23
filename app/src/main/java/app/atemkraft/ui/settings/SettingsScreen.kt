package app.atemkraft.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import app.atemkraft.cue.VoiceOption
import app.atemkraft.data.Refs
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.ui.components.BackButton
import app.atemkraft.ui.components.DisclosureToggle
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SelectChip
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
    voices: List<VoiceOption>,
    selectedVoiceId: String?,
    onSelectVoice: (String?) -> Unit,
    onPreviewVoice: () -> Unit,
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

            SoundCard(soundMode, transition, volume, onSoundMode, onTransition, onVolume)

            Spacer(Modifier.height(16.dp))
            SessionCard(haptics, showSafetyWarning, showNextPhase, onToggleHaptics, onToggleSafety, onToggleNextPhase)

            Spacer(Modifier.height(16.dp))
            MeditationCard(
                gongIntervalMin, onGongInterval, voices, selectedVoiceId, onSelectVoice, onPreviewVoice,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeditationCard(
    gongIntervalMin: Int,
    onGongInterval: (Int) -> Unit,
    voices: List<VoiceOption>,
    selectedVoiceId: String?,
    onSelectVoice: (String?) -> Unit,
    onPreviewVoice: () -> Unit,
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
            Spacer(Modifier.height(6.dp))
            if (voices.isEmpty()) {
                Hint(stringResource(R.string.settings_voice_none))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(
                        label = stringResource(R.string.meditation_voice_auto),
                        selected = selectedVoiceId == null,
                        onClick = { onSelectVoice(null) },
                    )
                    voices.forEach { v ->
                        SelectChip(label = v.label, selected = selectedVoiceId == v.id, onClick = { onSelectVoice(v.id) })
                    }
                }
                val cd = stringResource(R.string.cd_voice_preview)
                TextButton(
                    onClick = onPreviewVoice,
                    modifier = Modifier.semantics { contentDescription = cd },
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.settings_voice_preview))
                }
                Hint(stringResource(R.string.settings_voice_hint))
            }
        }
    }
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
