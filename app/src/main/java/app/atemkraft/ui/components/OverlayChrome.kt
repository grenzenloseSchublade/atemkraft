package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.TERTIARY

/**
 * Geteilter Rahmen der Vollbild-Overlays (Atem-Session + Meditation): Hintergrund-Surface mit
 * safeDrawingPadding, oben Minimieren-Chevron (˅) links und In-Session-Stummschalter rechts,
 * darunter der eigentliche Inhalt. [chromeVisible] = false blendet die Kopfleiste sanft aus
 * (Ruhe-Ansicht der Atem-Session, [HideableSlot]); ihr Platz bleibt, der Inhalt springt nicht.
 *
 * Für TalkBack ist das Overlay ein eigener Bereich mit [title] als `paneTitle` (Übungsname bzw.
 * „Meditation“, A11Y-07); den Rest der App blendet MainActivity derweil aus ([inertWhen]).
 * Der Ton ist ein Schalter: fester Name „Ton“, Rolle Switch, Zustand „An“/„Aus“ (A11Y-01).
 */
@Composable
fun OverlayChrome(
    title: String,
    muted: Boolean,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    chromeVisible: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize().semantics { paneTitle = title },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            HideableSlot(visible = chromeVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.action_minimize),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    SoundSwitch(muted = muted, onToggleMute = onToggleMute)
                }
            }
            Box(modifier = Modifier.weight(1f), content = content)
        }
    }
}

/**
 * Schneller Ton-Schalter für die laufende Sitzung (die Einstellung bleibt unberührt). Optik wie
 * der M3-`IconButton` daneben (Kreis [Dimens.OverlayIconButton], Icon 24 dp, fest wie die übrige
 * Kopfleiste), aber `toggleable` mit `Role.Switch`: TalkBack liest „Ton, Schalter, An“. Ohne
 * Live-Region – der fokussierte Schalter sagt seinen neuen Zustand selbst an.
 */
@Composable
private fun SoundSwitch(muted: Boolean, onToggleMute: () -> Unit) {
    val state = stringResource(if (muted) R.string.state_switch_off else R.string.state_switch_on)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(Dimens.OverlayIconButton)
            .clip(CircleShape)
            .toggleable(
                value = !muted,
                interactionSource = null,
                indication = ripple(),
                role = Role.Switch,
                onValueChange = { onToggleMute() },
            )
            .semantics { stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (muted) R.drawable.ic_sound_off else R.drawable.ic_sound_on),
            contentDescription = stringResource(R.string.action_sound),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (muted) TERTIARY else 1f),
        )
    }
}
