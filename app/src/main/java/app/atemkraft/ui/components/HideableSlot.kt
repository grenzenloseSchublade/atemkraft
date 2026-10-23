package app.atemkraft.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.atemkraft.ui.theme.Motion
import kotlinx.coroutines.launch

/**
 * Bereich der Vollbild-Session, der sich ausblenden lässt (Ruhe-Ansicht): blendet sanft aus,
 * behält dabei seinen Platz (nichts springt) und ist ausgeblendet weder antippbar noch für
 * TalkBack oder Tastatur/D-Pad erreichbar ([inertWhen]). Ein Tipp auf die leere Fläche fällt
 * dann zum Elternteil durch – dort schaltet die Session die Elemente wieder ein.
 *
 * [absorbTaps]: Sichtbar fängt der Bereich Tipps ab, die knapp neben seine Knöpfe gehen, statt
 * sie als „daneben“ durchzureichen – wer „Beenden“ knapp verfehlt, blendet nicht alles aus.
 */
@Composable
fun HideableSlot(
    visible: Boolean,
    modifier: Modifier = Modifier,
    absorbTaps: Boolean = false,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(Motion.CalmFade),
        label = "hideableSlot",
    )
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .pointerInput(visible, absorbTaps) { if (visible && absorbTaps) detectTapGestures { } }
            .inertWhen(!visible),
    ) {
        content()
        // Deckt den Inhalt ab wie ein Surface: Knöpfe darunter bekommen keinen Tipp, der Tipp
        // selbst bleibt unverbraucht und erreicht die Session (Einblenden).
        if (!visible) Box(Modifier.matchParentSize().pointerInput(Unit) {})
    }
}

/**
 * Macht einen Bereich unerreichbar für Bedienungshilfen und Tastatur, solange [hidden]: keine
 * Semantik (TalkBack, Voice Access, Switch Access) und kein Fokus per Tab oder D-Pad hinein; einen
 * Fokus, der beim Verbergen darin lag, gibt er ab. Was man sieht und antippen kann, ändert er
 * nicht – das regelt der Aufrufer: [HideableSlot] blendet aus, MainActivity legt das
 * Session-Overlay über die App (A11Y-07, MUSTER-07).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.inertWhen(hidden: Boolean): Modifier = this
    .then(if (hidden) Modifier.clearAndSetSemantics {} else Modifier)
    .focusProperties { if (hidden) enter = { FocusRequester.Cancel } }
    .then(ReleaseFocusElement(hidden))
    .focusGroup()

/** Gibt einen Fokus ab, der beim Verbergen ([hidden] wird true) im Bereich lag. */
private data class ReleaseFocusElement(val hidden: Boolean) : ModifierNodeElement<ReleaseFocusNode>() {
    override fun create() = ReleaseFocusNode(hidden)

    override fun update(node: ReleaseFocusNode) = node.update(hidden)
}

private class ReleaseFocusNode(private var hidden: Boolean) :
    Modifier.Node(),
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    private var hasFocus = false

    override fun onFocusEvent(focusState: FocusState) {
        hasFocus = focusState.hasFocus
    }

    fun update(hidden: Boolean) {
        if (hidden && !this.hidden && hasFocus) {
            // Nach dem Anwenden der Änderung, nicht mitten darin.
            coroutineScope.launch { currentValueOf(LocalFocusManager).clearFocus() }
        }
        this.hidden = hidden
    }
}

/**
 * Ob TalkBack (o. Ä.) per Touch-Erkundung bedient wird; folgt Änderungen live. Dann darf nichts
 * per Tipp ausgeblendet werden, sonst wären die Bedienelemente unerreichbar (A11Y).
 */
@Composable
fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}
