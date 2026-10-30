package app.atemkraft.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.cue.tts.VoiceFailure
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.VoiceGender
import app.atemkraft.domain.VoiceQuality
import app.atemkraft.domain.VoiceSpec
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.rememberAccessibilityActive
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.WarnAmber
import kotlin.reflect.KClass

/**
 * Eine Zeile im Stimmen-Katalog: Name/Info links, rechts die festen Spalten ([TrailingCells]):
 * Vorhören, Zustands-Aktion (Laden · Wählen · „✓ Aktiv“ · Fortschritt), Löschen bzw. Abbrechen.
 *
 * Fokus (A11Y-07): Laden, Abbrechen und Löschen verschwinden mit dem Zustand, den sie auslösen.
 * Lag der Fokus darauf (Tastatur, D-Pad) oder läuft ein Bedienungshilfe-Dienst, geht er nach dem
 * Zustandswechsel auf den Knopf, der an ihre Stelle tritt: Abbrechen, sobald die Stimme lädt;
 * Laden nach Abbrechen, Löschen oder Fehler.
 */
@Composable
internal fun VoiceRow(
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
    val focus = rememberVoiceRowFocus(state)
    TrailingRow(
        columns,
        modifier = Modifier.padding(vertical = Dimens.GapTiny),
        // Vorhören (funktioniert immer, auch vor dem Download).
        play = { PreviewButton(stringResource(R.string.cd_voice_preview, spec.displayName), onSample) },
        label = {
            VoiceStateAction(
                state,
                active,
                engineReady,
                onDownload = { focus.leaving(state, focus.loadFocused, onDownload) },
                onSelect = onSelect,
                focus = focus,
            )
        },
        icon = {
            val tracked = Modifier.onFocusChanged { focus.iconFocused = it.hasFocus }
            when (state) {
                // Abbrechen: delete() bricht den laufenden Download-Job ab und räumt Reste weg.
                is VoiceDownloadState.Downloading -> AppIconButton(
                    onClick = { focus.leaving(state, focus.iconFocused, onDelete) },
                    modifier = Modifier.focusRequester(focus.cancel).then(tracked),
                ) {
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
                    onConfirm = { focus.leaving(state, focus.iconFocused, onDelete) },
                    modifier = tracked,
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
            // Jeden Wechsel des Downloads sagt der Untertitel einmal an (A11Y-04): Start („lädt …“),
            // Entpacken, fertig („geladen“, weil der Text dann nur Merkmale nennt), Fehler,
            // Abbrechen. Die Live-Region hört auf den gesprochenen Text ohne Prozentwert – „lädt …
            // 40 %“ spräche sonst bei jedem Schritt; den Fortschritt nennt der Kreis daneben. Der
            // sichtbare Text bleibt im Baum (Umbruch- und Zuschnittprüfung), nur für TalkBack stumm.
            val spoken = voiceSubtitle(spec, state, active, engineReady, spoken = true)
            val downloaded = stringResource(R.string.voice_state_downloaded)
            Box(
                Modifier.semantics {
                    contentDescription = spoken
                    liveRegion = LiveRegionMode.Polite
                    if (state is VoiceDownloadState.Downloaded) stateDescription = downloaded
                },
            ) {
                HintText(voiceSubtitle(spec, state, active, engineReady), modifier = SilentForTalkBack)
            }
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
    focus: VoiceRowFocus,
) {
    when (state) {
        // Mit bekannter Größe zeigt der Kreis den Fortschritt (sichtbar dazu „40 %“ im Untertitel;
        // TalkBack liest den Wert am Kreis); beim Entpacken oder ohne Größe dreht er unbestimmt.
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
            AppTextButton(
                onClick = onDownload,
                modifier = Modifier.focusRequester(focus.load).onFocusChanged { focus.loadFocused = it.hasFocus },
            ) { Text(stringResource(R.string.settings_voice_download), maxLines = 1) }
    }
}

/**
 * Fokusziele einer Stimmen-Zeile (A11Y-07): [load] („Laden“) und [cancel] („Abbrechen“). [leaving]
 * merkt sich vor der Aktion den Zustand, wenn der Fokus mitgehen soll; beim nächsten Wechsel des
 * Zustands bekommt der Knopf des neuen Zustands den Fokus.
 */
@Stable
private class VoiceRowFocus(private val a11yActive: () -> Boolean) {
    val load = FocusRequester()
    val cancel = FocusRequester()
    var loadFocused by mutableStateOf(false)
    var iconFocused by mutableStateOf(false)
    var from by mutableStateOf<KClass<out VoiceDownloadState>?>(null)

    fun leaving(state: VoiceDownloadState, focused: Boolean, action: () -> Unit) {
        if (focused || a11yActive()) from = state::class
        action()
    }
}

@Composable
private fun rememberVoiceRowFocus(state: VoiceDownloadState): VoiceRowFocus {
    val a11yActive by rememberUpdatedState(rememberAccessibilityActive())
    val focus = remember { VoiceRowFocus { a11yActive } }
    LaunchedEffect(focus.from, state::class) {
        val from = focus.from ?: return@LaunchedEffect
        if (state::class == from) return@LaunchedEffect // Wechsel steht noch aus
        focus.from = null
        when (state) {
            is VoiceDownloadState.Downloading -> focus.cancel.requestFocus()
            is VoiceDownloadState.NotDownloaded, is VoiceDownloadState.Failed -> focus.load.requestFocus()
            is VoiceDownloadState.Downloaded -> Unit // „Wählen“ oder „✓ Aktiv“ steht schon da
        }
    }
    return focus
}

/** Für TalkBack unsichtbar, aber im Semantik-Baum (Umbruch- und Zuschnittprüfung im Screenshot-Test). */
private val SilentForTalkBack = Modifier.semantics { hideFromAccessibility() }

/**
 * Untertitel einer Katalog-Zeile: Geschlecht · Qualität · Lizenz (+ Zustand). [spoken]: für die
 * Ansage, ohne Prozentwert („lädt …“).
 */
@Composable
private fun voiceSubtitle(
    spec: VoiceSpec,
    state: VoiceDownloadState,
    active: Boolean,
    engineReady: Boolean,
    spoken: Boolean = false,
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
            } else if (state.indeterminate || spoken) {
                stringResource(R.string.voice_loading)
            } else {
                stringResource(R.string.settings_speech_pack_downloading, (state.fraction * 100).toInt())
            }

        is VoiceDownloadState.Failed -> stringResource(
            when (state.reason) {
                VoiceFailure.NETWORK -> R.string.voice_failed_network
                VoiceFailure.STORAGE -> R.string.voice_failed_storage
                VoiceFailure.DAMAGED -> R.string.voice_failed_damaged
                VoiceFailure.OTHER -> R.string.voice_failed_short
            },
        )

        is VoiceDownloadState.Downloaded ->
            if (active && !engineReady) stringResource(R.string.settings_speech_pack_preparing) else null
    }
    return if (extra != null) base + DOT_SEPARATOR + extra else base
}
