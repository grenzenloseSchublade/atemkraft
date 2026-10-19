package app.atemkraft.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Runde Tippfläche über dem Atemkreis (Atem-Session, Meditation): Kantenlänge [side] wie der
 * Kreis, Treffer nur bis zur Spur, nicht im umschließenden Quadrat (`clip` begrenzt auch den
 * Hit-Test). Eigene Fläche statt `clip` am Kreis, damit dessen Glow bleibt. Ohne Ripple-Kästchen:
 * Die Rückmeldung gibt [PauseFlash] ([TapFlashState.toggle]).
 *
 * [onTap] `null` = der Kreis tut gerade nichts (Countdown). Mit [swallowWhenIdle] fängt er den
 * Tipp dann trotzdem ab, damit er nicht zur Ruhe-Ansicht der Atem-Session durchfällt und
 * „Beenden“ ausblendet. [description] ist der Name für TalkBack.
 */
@Composable
fun CircleTapTarget(
    side: Dp,
    description: String,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    swallowWhenIdle: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(side)
            .clip(CircleShape)
            .semantics { contentDescription = description }
            .then(
                when {
                    onTap != null -> Modifier.clickable(role = Role.Button, interactionSource = null, indication = null, onClick = onTap)
                    swallowWhenIdle -> Modifier.pointerInput(Unit) { detectTapGestures { } }
                    else -> Modifier
                },
            ),
    )
}
