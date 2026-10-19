package app.atemkraft.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Dauer des sanften Aus-/Einblendens der Session-Bedienelemente (ms). */
private const val FADE_MS = 500

/**
 * Bereich der Vollbild-Session, der sich ausblenden lässt (Ruhe-Ansicht): blendet sanft aus,
 * behält dabei seinen Platz (nichts springt) und ist ausgeblendet weder antippbar noch für
 * TalkBack oder Tastatur/D-Pad erreichbar. Ein Tipp auf die leere Fläche fällt dann zum
 * Elternteil durch – dort schaltet die Session die Elemente wieder ein.
 *
 * [absorbTaps]: Sichtbar fängt der Bereich Tipps ab, die knapp neben seine Knöpfe gehen, statt
 * sie als „daneben“ durchzureichen – wer „Beenden“ knapp verfehlt, blendet nicht alles aus.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HideableSlot(
    visible: Boolean,
    modifier: Modifier = Modifier,
    absorbTaps: Boolean = false,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(FADE_MS),
        label = "hideableSlot",
    )
    // Hatte ein Knopf darin den Tastatur-Fokus, gibt er ihn beim Ausblenden ab.
    val focusManager = LocalFocusManager.current
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (!visible && hasFocus) focusManager.clearFocus() }
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {})
            .pointerInput(visible, absorbTaps) { if (visible && absorbTaps) detectTapGestures { } }
            // Ausgeblendet lässt die Gruppe keinen Fokus hinein (Tab, D-Pad).
            .focusProperties { if (!visible) enter = { FocusRequester.Cancel } }
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusGroup(),
    ) {
        content()
        // Deckt den Inhalt ab wie ein Surface: Knöpfe darunter bekommen keinen Tipp, der Tipp
        // selbst bleibt unverbraucht und erreicht die Session (Einblenden).
        if (!visible) Box(Modifier.matchParentSize().pointerInput(Unit) {})
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
