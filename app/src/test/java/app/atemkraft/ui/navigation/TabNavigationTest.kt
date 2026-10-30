package app.atemkraft.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.atemkraft.ui.components.PinnedActionBar
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
 * Push-Seiten; die Seiten selbst bleiben leer. Dazu die Tab-Leiste in der Tiefe ([TabBarState]):
 * Wischen und Griff.
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

    /**
     * Tab-Leiste in der Tiefe wie im App-Rahmen: Wischen nach oben auf der Startleiste öffnet sie,
     * Wischen nach unten auf der offenen Leiste schließt sie, ein Tipp auf Start bleibt ein Tipp
     * (das Wischen wertet erst nach dem Touch-Slop), und der Griff klappt sie per Tipp um.
     */
    @Test
    fun `Wischen oeffnet und schliesst die Tab-Leiste, Tipps bleiben Tipps`() {
        val state = TabBarState().apply { enter(onTab = false, tab = TopTab.START) }
        var started = 0
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        CompositionLocalProvider(LocalTabBarState provides state) {
                            PinnedActionBar(Modifier.align(Alignment.BottomCenter).testTag(PINNED)) {
                                Button(onClick = { started++ }) { Text(START) }
                            }
                        }
                    }
                    Column(Modifier.onGloballyPositioned { state.bottomBarTop = it.positionInRoot().y }) {
                        HideableTabBar(visible = !state.collapsed, state = state, modifier = Modifier.testTag(TAB_BAR)) {
                            AppTabBar(selected = TopTab.START, onTap = {})
                        }
                    }
                }
                TabBarToggle(state, Modifier.align(Alignment.TopCenter))
            }
        }
        rule.onNodeWithTag(PINNED).performTouchInput { swipeUp() }
        rule.runOnIdle { assertFalse(state.collapsed) }
        rule.onNodeWithTag(TAB_BAR).performTouchInput { swipeDown() }
        rule.runOnIdle { assertTrue(state.collapsed) }
        rule.onNodeWithText(START).performClick()
        rule.runOnIdle {
            assertEquals(1, started)
            assertTrue(state.collapsed)
        }
        rule.onNodeWithTag(TAB_BAR_GRIP_TAG).performClick()
        rule.runOnIdle { assertFalse(state.collapsed) }
    }

    private fun assertTab(tab: TopTab, root: Boolean) {
        rule.runOnIdle {
            assertEquals(tab, nav.tabOnStack())
            if (root) assertTrue(nav.currentDestination.isTabRoot()) else assertFalse(nav.currentDestination.isTabRoot())
        }
    }

    private companion object {
        const val PINNED = "pinned"
        const val TAB_BAR = "tabBar"
        const val START = "Start"
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
