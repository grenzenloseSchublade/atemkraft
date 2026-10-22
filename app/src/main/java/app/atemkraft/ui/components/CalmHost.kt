package app.atemkraft.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * Ruhe-Ansicht der Vollbild-Overlays (Atem-Session und Meditation): Ein Tipp auf freie Fläche
 * blendet die Bedienelemente aus, ein weiterer wieder ein; [content] erfährt, ob sie sichtbar
 * sind. Ausgeblendet verschwinden auch Status- und Navigationsleiste ([HideSystemBars]). Der Tipp
 * auf den Kreis bleibt Pause/Weiter: Der Kreis verbraucht ihn selbst, hier kommen nur Tipps auf
 * freie Fläche an. Ein Tastendruck (Hardware-Tastatur, D-Pad) blendet wieder ein, statt unsichtbar
 * etwas auszulösen. Mit TalkBack (Touch-Erkundung) wird nie ausgeblendet.
 *
 * @param canHide der Zustand erlaubt Ruhe; in Pause und am Ende ist alles sichtbar (sonst käme
 *   man nicht an „Beenden“), danach gilt wieder die gewählte Ansicht.
 * @param autoCalm die Ruhe kommt [AUTO_CALM_DELAY_MS] nach der letzten Berührung oder Taste von
 *   selbst (Einstellung „Automatisch ausblenden“; nicht im Countdown, das regelt der Aufrufer).
 * @param reset true stellt alles wieder sichtbar (Ende, Neustart im Countdown).
 */
@Composable
fun CalmHost(
    canHide: Boolean,
    autoCalm: Boolean,
    reset: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (controlsVisible: Boolean) -> Unit,
) {
    var calm by rememberSaveable { mutableStateOf(false) }
    val hideable = canHide && !rememberTouchExplorationEnabled()
    val controlsVisible = !(calm && hideable)
    val currentHideable by rememberUpdatedState(hideable)
    LaunchedEffect(reset) { if (reset) calm = false }
    // Jede Berührung und jeder Tastendruck zählt als Aktivität und schiebt die automatische Ruhe auf.
    var activity by remember { mutableIntStateOf(0) }
    val autoPending = autoCalm && hideable && !calm
    LaunchedEffect(autoPending, activity) {
        if (autoPending) {
            delay(AUTO_CALM_DELAY_MS)
            calm = true
        }
    }
    HideSystemBars(hidden = !controlsVisible)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) activity++
                    }
                }
            }
            .pointerInput(Unit) { detectTapGestures { if (currentHideable) calm = !calm } }
            .onPreviewKeyEvent { event ->
                val hidden = !controlsVisible
                if (event.type == KeyEventType.KeyDown) {
                    activity++
                    if (hidden) calm = false
                }
                hidden
            },
    ) {
        content(controlsVisible)
    }
}

/** Ruhe ohne Berührung: so lange bleiben die Bedienelemente nach Start oder letztem Tipp stehen. */
const val AUTO_CALM_DELAY_MS = 5_000L
