package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY

/**
 * Icon-only-Lautsprecher-Schalter: Ton an ([R.drawable.ic_sound_on] in [tint]) oder aus
 * ([R.drawable.ic_sound_off], [tint] mit [TERTIARY]), gebaut auf [IconSwitch]. Genutzt im Kopf der
 * Vollbild-Overlays („Ton“, `OverlayChrome`) und in der Vorschau des Muster-Editors („Ton der
 * Vorschau“).
 */
@Composable
fun SoundSwitch(
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    size: Dp = Sizes.IconButtonSize,
) {
    IconSwitch(
        on = on,
        onToggle = onToggle,
        name = name,
        icon = painterResource(if (on) R.drawable.ic_sound_on else R.drawable.ic_sound_off),
        modifier = modifier,
        enabled = enabled,
        tint = tint,
        size = size,
    )
}

/**
 * Icon-only-Schalter: [icon] in [tint], aus bzw. abgeschaltet mit [TERTIARY]. Kreis [size]
 * (Standard wie `AppIconButton`, Overlays `Dimens.OverlayIconButton`), Icon 24 dp, Tippfläche
 * 48 dp. `toggleable` mit `Role.Switch` und `stateDescription` „An“/„Aus“: TalkBack liest „[name],
 * Schalter, An“. Ohne Live-Region – der fokussierte Schalter sagt seinen neuen Zustand selbst an
 * (A11Y-01, A11Y-04). Ton ([SoundSwitch]) und „Danach in die Stille“ im Kopf der Atem-Session.
 */
@Composable
fun IconSwitch(
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    name: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    size: Dp = Sizes.IconButtonSize,
) {
    val state = stringResource(if (on) R.string.state_switch_on else R.string.state_switch_off)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .toggleable(
                value = on,
                enabled = enabled,
                interactionSource = null,
                indication = ripple(),
                role = Role.Switch,
                onValueChange = onToggle,
            )
            .semantics { stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = name,
            modifier = Modifier.size(Sizes.IconDefault),
            tint = tint.copy(alpha = if (on && enabled) 1f else TERTIARY),
        )
    }
}

@Preview
@Composable
private fun SoundSwitchPreview() {
    AtemkraftTheme {
        SoundSwitch(on = true, onToggle = {}, name = stringResource(R.string.cd_preview_sound), tint = MaterialTheme.colorScheme.secondary)
    }
}
