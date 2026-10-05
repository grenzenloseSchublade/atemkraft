package app.atemkraft.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Tipp auf einen Tab der Leiste. Ist er schon markiert ([selected]), geht es zurück zu seiner
 * Startseite (Material-Regel „Re-Tap“): Aus Einstellungen, Glossar oder Detailseite führt der
 * markierte Tab heraus, statt nichts zu tun. Sonst normaler Tab-Wechsel ([switchTab]).
 *
 * Rückfall auf [switchTab], wenn `popBackStack` nichts entfernt – die Startseite liegt schon
 * oben, oder die Markierung (gemerkter Wert in der Activity) passt nicht zum Stapel.
 */
internal fun NavController.tapTab(route: Any, selected: Boolean) {
    if (selected && popBackStack(route, inclusive = false)) return
    switchTab(route)
}

/**
 * Tab-Wechsel mit erhaltenen Back-Stacks (NiA-Muster): Jeder Tab merkt sich beim Verlassen
 * seinen Stapel und bekommt ihn bei der Rückkehr zurück.
 *
 * Achtung beim markierten Tab: Ein gemerkter Stand wird wiederhergestellt, `launchSingleTop`
 * greift dann nicht. Ein Re-Tap über diese Funktion würde den offenen Stapel also nur speichern
 * und sofort wiederherstellen – dafür gibt es [tapTab].
 */
internal fun NavController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
