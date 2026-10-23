package app.atemkraft.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Navigations-Grundregel (MUSTER-08) über alle Tabs aus [TopTab] und [tapTab] wie in der App. Der
 * NavHost trägt die Tab-Startseiten aus derselben Liste wie der Graph der App (`appGraph`) und zwei
 * Push-Seiten; die Seiten selbst bleiben leer.
 */
@RunWith(RobolectricTestRunner::class)
class TabNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var nav: NavHostController

    /**
     * Erneuter Tipp auf den markierten Tab lässt die Startseite unverändert und führt aus jeder
     * darüber geöffneten Seite zurück zur Startseite – auch aus tiefem Stapel. Markiert bleibt dabei
     * der Tab, zu dessen Stapel die Seite gehört ([tabOnStack]); die Leiste steht fest nur auf den
     * Startseiten ([isTabRoot]).
     */
    @Test
    fun `Re-Tap auf den markierten Tab fuehrt zur Startseite`() {
        rule.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = TopTab.START.route) {
                TopTab.entries.forEach { tab -> composable(tab.route::class) {} }
                composable<SettingsRoute> {}
                composable<GlossaryRoute> {}
            }
        }
        assertEquals(4, TopTab.entries.size)
        TopTab.entries.forEach { tab ->
            val start = step { nav.tapTab(tab.route, selected = false) }
            assertTab(tab, root = true)
            assertEquals(start, step { nav.tapTab(tab.route, selected = true) })
            step { nav.navigate(SettingsRoute()) }
            step { nav.navigate(GlossaryRoute) }
            assertTab(tab, root = false)
            assertEquals(start, step { nav.tapTab(tab.route, selected = true) })
            assertTab(tab, root = true)
        }
    }

    private fun assertTab(tab: TopTab, root: Boolean) {
        rule.runOnIdle {
            assertEquals(tab, nav.tabOnStack())
            if (root) assertTrue(nav.currentDestination.isTabRoot()) else assertFalse(nav.currentDestination.isTabRoot())
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
