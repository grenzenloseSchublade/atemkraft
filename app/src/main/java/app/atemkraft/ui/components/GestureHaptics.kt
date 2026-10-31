package app.atemkraft.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Ein Impuls, wenn eine Geste ihre Schwelle überschreitet (Tab-Leiste in der Tiefe, Schließen eines
 * Bottom Sheets; Eigentümer 2026-10-31): [reached] = beim Loslassen würde es jetzt auslösen
 * (`GestureThresholdActivate`), sonst das Gegenstück beim Zurückziehen. Compose kennt kein
 * `GestureThresholdDeactivate`; der leiseste vordefinierte Tick (`SegmentFrequentTick`) steht dafür.
 * Nur bei Gesten, nie bei Tipps, nie während des Ziehens dazwischen. Folgt der Systemeinstellung
 * (`LocalHapticFeedback`).
 */
internal fun HapticFeedback.gestureThreshold(reached: Boolean) {
    performHapticFeedback(if (reached) HapticFeedbackType.GestureThresholdActivate else HapticFeedbackType.SegmentFrequentTick)
}
