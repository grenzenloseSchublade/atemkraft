package app.atemkraft.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.SessionButtonCyan
import app.atemkraft.ui.theme.SessionButtonPink
import app.atemkraft.ui.theme.SessionTextGlow
import app.atemkraft.ui.theme.SessionTextYellow
import kotlinx.coroutines.delay

// Geteilte Bausteine der Vollbild-Sitzungen (Atem-Session UND Meditation): Glow-Schrift,
// die Session-Buttons in den festen Farb-Tokens, der Abschluss-Screen und der Tap-Flash.
// Zentral, damit beide Abläufe garantiert identisch aussehen und sich gemeinsam ändern.

/** Schrift mit weichem dunklen Schimmer (kein harter Rand) – ruhig und lesbar auf Magenta & Dunkel. */
@Composable
fun GlowText(
    text: String,
    style: TextStyle,
    fill: Color = SessionTextYellow,
    modifier: Modifier = Modifier,
) {
    // Zentriert und ohne Wortbruch: Session-Texte stehen mittig über dem Kreis (LAYOUT-03).
    WholeWordText(
        text = text,
        style = style.merge(
            TextStyle(
                color = fill,
                shadow = Shadow(color = SessionTextGlow, offset = Offset.Zero, blurRadius = 18f),
            ),
        ),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

// Schmaler als der M3-Standard (24 dp): Drei Buttons passen so auch bei fontScale 1,3 auf
// 360 dp in eine Zeile. Die Höhe (≥ 48 dp Touch-Ziel) bleibt unverändert.
private val SessionButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/** Gefüllter Primär-Button (Pause/Weiter, Nochmal) im Session-Cyan. */
@Composable
fun SessionPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = SessionButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = SessionButtonCyan,
            contentColor = SessionTextGlow,
            disabledContainerColor = SessionButtonCyan.copy(alpha = 0.25f),
            disabledContentColor = SessionTextGlow.copy(alpha = 0.5f),
        ),
    ) { Text(text) }
}

/** Umrandeter Sekundär-Button (Von vorne, Ende) im Session-Cyan. */
@Composable
fun SessionSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = SessionButtonPadding,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SessionButtonCyan,
            disabledContentColor = SessionButtonCyan.copy(alpha = 0.4f),
        ),
        border = BorderStroke(1.dp, SessionButtonCyan.copy(alpha = if (enabled) 1f else 0.4f)),
    ) { Text(text) }
}

/** Umrandeter „Beenden"-Button im Session-Pink. */
@Composable
fun SessionStopButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = SessionButtonPadding,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SessionButtonPink),
        border = BorderStroke(1.dp, SessionButtonPink),
    ) { Text(text) }
}

/** Abschluss-Screen beider Sitzungsarten: „✓ Geschafft"-Eyebrow, [title], Nochmal/Ende. */
@Composable
fun FinishedPanel(title: String, onAgain: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "✓ " + stringResource(R.string.session_done_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        AdaptiveButtonRow(modifier = Modifier.fillMaxWidth()) {
            SessionPrimaryButton(
                text = stringResource(R.string.action_again),
                onClick = onAgain,
            )
            SessionSecondaryButton(
                text = stringResource(R.string.action_end),
                onClick = onExit,
            )
        }
    }
}

/** Zustand des Tap-Flashs (großes Pause/Play-Symbol beim Antippen des Kreises). */
@Stable
class TapFlashState internal constructor() {
    var alpha by mutableFloatStateOf(0f)
        internal set
    var isPause by mutableStateOf(true)
        private set
    internal var trigger by mutableIntStateOf(0)

    /** Flash auslösen; [isPause] = true zeigt das Pause-Symbol, sonst Play. */
    fun flash(isPause: Boolean) {
        this.isPause = isPause
        trigger++
    }
}

/**
 * Tap-Flash-Logik (geteilt von Session + Meditation). Alpha wird per delay-Schleife animiert,
 * NICHT per Animatable.animateTo: animateTo respektiert animator_duration_scale – ist die
 * System-Animation aus (Entwickleroptionen/Energiesparen), spränge es sofort auf den Zielwert
 * und der Flash bliebe unsichtbar. delay() ist davon unabhängig.
 */
@Composable
fun rememberTapFlash(): TapFlashState {
    val state = remember { TapFlashState() }
    LaunchedEffect(state.trigger) {
        if (state.trigger > 0) {
            state.alpha = 1f
            delay(220) // kurzer Halt
            val steps = 16
            for (i in 1..steps) {
                delay(34)
                state.alpha = (1f - i.toFloat() / steps).coerceAtLeast(0f)
            }
            state.alpha = 0f
        }
    }
    return state
}
