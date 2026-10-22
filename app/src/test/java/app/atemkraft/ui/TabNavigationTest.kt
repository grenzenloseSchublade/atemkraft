package app.atemkraft.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Navigations-Grundregel (MUSTER-08) über die Routen der App und [tapTab] wie in MainActivity. */
@RunWith(RobolectricTestRunner::class)
class TabNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var nav: NavHostController

    /**
     * Erneuter Tipp auf den markierten Tab lässt die Startseite unverändert und führt aus jeder
     * darüber geöffneten Seite zurück zur Startseite – auch aus tiefem Stapel.
     */
    @Test
    fun `Re-Tap auf den markierten Tab fuehrt zur Startseite`() {
        rule.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = AtmenRoute) {
                composable<AtmenRoute> {}
                composable<SituationenRoute> {}
                composable<EigeneRoute> {}
                composable<MeditationRoute> {}
                composable<SettingsRoute> {}
                composable<GlossaryRoute> {}
            }
        }
        listOf(AtmenRoute, SituationenRoute, EigeneRoute, MeditationRoute).forEach { tab ->
            val start = step { nav.tapTab(tab, selected = false) }
            assertEquals(start, step { nav.tapTab(tab, selected = true) })
            step { nav.navigate(SettingsRoute()) }
            step { nav.navigate(GlossaryRoute) }
            assertEquals(start, step { nav.tapTab(tab, selected = true) })
        }
    }

    /** Führt [action] aus, wartet die Navigation ab und gibt den Stapel als Text zurück. */
    private fun step(action: () -> Unit): String {
        rule.runOnIdle(action)
        rule.waitForIdle()
        return nav.currentBackStack.value
            .filter { it.destination !is NavGraph }
            .joinToString(" > ") { it.destination.route!!.substringAfterLast('.').substringBefore('?').substringBefore('/') }
    }
}
