package app.atemkraft.ui

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
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

/**
 * Ob dieses Ziel einer der vier Tabs ist: Nur dort steht die Tab-Leiste. Sobald man in die Tiefe
 * geht (Detailseite, Muster-Editor, Einstellungen, Glossar, Atemlehre, Logbuch, Über), weicht sie
 * und gibt ihren Platz dem Inhalt; zurück geht es über den Pfeil im Kopf (Nutzer 2026-10-23).
 * Vor dem ersten Ziel (null) gilt der Start-Tab.
 */
internal fun NavDestination?.isTabRoot(): Boolean = this == null || hierarchy.any { destination ->
    TAB_ROOTS.any { destination.hasRoute(it) }
}

private val TAB_ROOTS = listOf(AtmenRoute::class, SituationenRoute::class, EigeneRoute::class, MeditationRoute::class)

/**
 * Der Tab, zu dessen Stapel die aktuelle Seite gehört – auch auf Push-Seiten und nach einem
 * wiederhergestellten Stapel (`switchTab`, `restoreState`: Eigene › Editor liegt dann sofort oben,
 * ohne dass „Eigene“ selbst je aktuell war). Atmen ist Startziel und liegt immer unten; die
 * anderen Tabs stehen nur in ihrem eigenen Stapel. Für die Markierung der Tab-Leiste.
 */
internal fun NavController.tabOnStack(): Any = listOf(SituationenRoute, EigeneRoute, MeditationRoute)
    // getBackStackEntry wirft, wenn das Ziel nicht im Stapel liegt; die öffentliche API kennt
    // keine Abfrage ohne Ausnahme (currentBackStack ist bibliotheksintern).
    .firstOrNull { route -> runCatching { getBackStackEntry(route) }.isSuccess } ?: AtmenRoute
