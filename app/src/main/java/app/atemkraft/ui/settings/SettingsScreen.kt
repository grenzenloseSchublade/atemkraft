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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import app.atemkraft.R
import app.atemkraft.cue.VoiceOption
import app.atemkraft.data.Refs
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.ui.components.BackButton

/** Einstellungen: Begleit-Reize, Sicherheitshinweis, Quellen/Über. */
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
    onOpenGlossary: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
) {
    var soundInfoExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler { onBack() }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_sound_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                    Text(
                        text = stringResource(R.string.settings_sound_appetizer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    TextButton(
                        onClick = { soundInfoExpanded = !soundInfoExpanded },
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        Text(
                            (if (soundInfoExpanded) "▴ " else "▾ ") +
                                stringResource(R.string.settings_sound_more),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (soundInfoExpanded) {
                        Text(
                            text = stringResource(R.string.settings_sound_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.settings_sources),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(4.dp))
                        listOf(Refs.respeRate, Refs.shaffer2020, Refs.zaccaro2018).forEach { ref ->
                            Text(
                                text = ref.citation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            ref.identifier?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    if (soundMode == SoundMode.CONTINUOUS) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.emphasis_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
                        Text(
                            text = stringResource(R.string.emphasis_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    if (soundMode != SoundMode.OFF) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.volume_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
                        Text(
                            text = stringResource(R.string.volume_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ToggleRow(stringResource(R.string.settings_haptics), haptics, onToggleHaptics)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ToggleRow(stringResource(R.string.settings_safety), showSafetyWarning, onToggleSafety)
                    Text(
                        text = stringResource(R.string.settings_safety_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ToggleRow(stringResource(R.string.settings_next_phase), showNextPhase, onToggleNextPhase)
                    Text(
                        text = stringResource(R.string.settings_next_phase_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            MeditationCard(
                gongIntervalMin = gongIntervalMin,
                onGongInterval = onGongInterval,
                voices = voices,
                selectedVoiceId = selectedVoiceId,
                onSelectVoice = onSelectVoice,
            )

            Spacer(Modifier.height(16.dp))
            NavRow(stringResource(R.string.settings_glossary), onOpenGlossary)
            Spacer(Modifier.height(10.dp))
            NavRow(stringResource(R.string.settings_about_entry), onOpenAbout)

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(24.dp))
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
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_meditation_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_gong_interval),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
            Text(
                text = stringResource(R.string.settings_gong_interval_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Text(
                text = stringResource(R.string.settings_voice),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            if (voices.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_voice_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedVoiceId == null,
                        onClick = { onSelectVoice(null) },
                        label = { Text(stringResource(R.string.meditation_voice_auto)) },
                    )
                    voices.forEach { v ->
                        FilterChip(
                            selected = selectedVoiceId == v.id,
                            onClick = { onSelectVoice(v.id) },
                            label = { Text(v.label) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_voice_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
