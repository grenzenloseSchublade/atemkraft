package app.atemkraft.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SessionButtonCyan

/**
 * Runde Tippfläche über dem Atemkreis (Atem-Session, Meditation): Kantenlänge [side] wie der
 * Kreis, Treffer nur bis zur Spur, nicht im umschließenden Quadrat (`clip` begrenzt auch den
 * Hit-Test). Eigene Fläche statt `clip` am Kreis, damit dessen Glow bleibt. Ohne Ripple-Kästchen:
 * Die Rückmeldung gibt [PauseFlash] ([TapFlashState.toggle]).
 *
 * [onTap] `null` = der Kreis tut gerade nichts (Countdown): dann auch kein Name, also kein leerer
 * Halt für TalkBack. Mit [swallowWhenIdle] fängt er den Tipp trotzdem ab, damit er nicht zur
 * Ruhe-Ansicht der Atem-Session durchfällt und „Beenden“ ausblendet. [description] ist der Name
 * für TalkBack, [clickLabel] sagt, was der Tipp tut („Zum Pausieren doppeltippen“).
 *
 * Fokus per Tastatur oder D-Pad zeigt ein Ring in [SessionButtonCyan] knapp außerhalb der Spur
 * (A11Y-07: ohne Ripple ein eigener Fokus); bei Touch erscheint er nie, denn ein Tipp fokussiert
 * nicht.
 */
@Composable
fun CircleTapTarget(
    side: Dp,
    description: String,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    clickLabel: String? = null,
    swallowWhenIdle: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    // Ohne Aktion gibt es keinen Fokus mehr (der Klick-Knoten ist weg), also auch keinen Ring.
    val ring = focused && onTap != null
    Box(
        modifier = modifier
            .size(side)
            // Vor dem clip, damit der Ring außerhalb der Spur liegen darf.
            .drawBehind {
                if (ring) {
                    val stroke = Dimens.FocusRing.toPx()
                    drawCircle(
                        color = SessionButtonCyan,
                        radius = size.minDimension / 2f + Dimens.GapTiny.toPx() + stroke / 2f,
                        style = Stroke(stroke),
                    )
                }
            }
            .clip(CircleShape)
            .then(
                when {
                    onTap != null ->
                        Modifier
                            .semantics { contentDescription = description }
                            .onFocusChanged { focused = it.isFocused }
                            .clickable(role = Role.Button, onClickLabel = clickLabel, interactionSource = null, indication = null, onClick = onTap)

                    swallowWhenIdle -> Modifier.pointerInput(Unit) { detectTapGestures { } }

                    else -> Modifier
                },
            ),
    )
}
