package app.atemkraft.ui.navigation

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Ob eine Bedienhilfe läuft, der Wischgesten mit einem Finger gehören oder die ohne Berührung
 * navigiert – dann steht die Tab-Leiste auch in der Tiefe offen ([TabBarState.keepOpen]):
 * Touch-Erkundung (TalkBack), ein Dienst mit gesprochener oder Braille-Rückmeldung oder einer, der
 * Tasten filtert (Switch Access, Universal Switch). Andere Dienste (Passwortmanager, Automatisierer,
 * Gestenhelfer) zählen nicht: Mit ihnen stand die Leiste vorher je nach Gerät offen oder sprang auf
 * (Review 2026-11-24). Bewusst enger als `rememberAccessibilityActive` (Bestätigungen, Ruhe-Ansicht),
 * wo die breite Regel die sichere Richtung ist. Änderungen gelten sofort.
 */
@Composable
fun rememberTabBarAssistance(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var active by remember(manager) { mutableStateOf(manager.needsOpenTabBar()) }
    DisposableEffect(manager) {
        val state = AccessibilityManager.AccessibilityStateChangeListener { active = manager.needsOpenTabBar() }
        val touch = AccessibilityManager.TouchExplorationStateChangeListener { active = manager.needsOpenTabBar() }
        manager?.addAccessibilityStateChangeListener(state)
        manager?.addTouchExplorationStateChangeListener(touch)
        onDispose {
            manager?.removeAccessibilityStateChangeListener(state)
            manager?.removeTouchExplorationStateChangeListener(touch)
        }
    }
    return active
}

private fun AccessibilityManager?.needsOpenTabBar(): Boolean {
    if (this == null || !isEnabled) return false
    if (isTouchExplorationEnabled) return true
    val feedback = AccessibilityServiceInfo.FEEDBACK_SPOKEN or AccessibilityServiceInfo.FEEDBACK_BRAILLE
    return getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
        info.feedbackType and feedback != 0 ||
            info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS != 0
    }
}
