package app.atemkraft.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.R
import app.atemkraft.data.Situations
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.components.HideableTabBar
import app.atemkraft.ui.components.TabBarSpace
import app.atemkraft.ui.components.inertWhen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.own.OwnPatternsScreen
import app.atemkraft.ui.settings.NoOpSettingsActions
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.settings.SettingsUiState
import app.atemkraft.ui.situations.SituationsScreen
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Navigation zwischen Tabs und weitergeführten Seiten: Re-Tap auf den markierten Tab
 * ([tapTab]), Tab-Wechsel ([switchTab]) und Zurück über den festen Kopf ([PushHeader]).
 * Der NavHost hat dieselben Routen wie MainActivity; die vier Tabs, Einstellungen, Glossar, Über und
 * Atemlehre sind die echten Screens (Kalender zum Logbuch nur im Atmen-Tab), damit der Test auch den festen Zurück-Pfeil prüft. Gerätegröße wie das A54.
 * Darüber liegt wie in MainActivity bei Bedarf ein Session-Overlay; die App darunter ist dann
 * per [inertWhen] für TalkBack und Tastatur nicht da.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class TabNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var nav: NavHostController

    private val tabs = listOf(AtmenRoute, SituationenRoute, EigeneRoute, MeditationRoute)

    private var overlayOpen by mutableStateOf(false)
    private val tabBarSpace = TabBarSpace()
    private lateinit var focusManager: FocusManager
    private val tabFocus = FocusRequester()
    private var tabFocused = false
    private val focusedEver = mutableSetOf<String>()

    @Before
    fun setUp() {
        rule.setContent {
            nav = rememberNavController()
            focusManager = LocalFocusManager.current
            AtemkraftTheme {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().inertWhen(overlayOpen)) {
                        Box(Modifier.weight(1f)) { Routes() }
                        // Wie MainActivity: Nur der Muster-Editor darf die Leiste ausblenden.
                        val destination = nav.currentBackStackEntryAsState().value?.destination
                        HideableTabBar(tabBarSpace, allowHide = destination.mayHideTabBar()) {
                            // Stellvertreter der Tab-Leiste: ein Knopf, der sich merkt, ob er Fokus bekam.
                            Text(
                                TAB_BAR,
                                Modifier
                                    .focusRequester(tabFocus)
                                    .onFocusChanged {
                                        tabFocused = it.isFocused
                                        if (it.isFocused) focusedEver += TAB_BAR
                                    }
                                    .clickable(role = Role.Button) {},
                            )
                        }
                    }
                    if (overlayOpen) {
                        Column(Modifier.fillMaxSize()) {
                            Text(
                                OVERLAY_STOP,
                                Modifier
                                    .onFocusChanged { if (it.isFocused) focusedEver += OVERLAY_STOP }
                                    .clickable(role = Role.Button) {},
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun Routes() {
        NavHost(nav, startDestination = AtmenRoute) {
            // Echte Tabs wie in MainActivity: Nur der Kopf von Atmen trägt den Kalender zum Logbuch
            // (links neben dem Zahnrad, Nutzer 2026-10-22).
            composable<AtmenRoute> {
                HomeScreen(
                    exercisesByFamily = emptyList(),
                    programs = emptyList(),
                    daily = remember { RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 22)) },
                    dailySaved = false,
                    onRegenerateDaily = {},
                    onDailySavedChange = {},
                    onSelect = { id -> nav.navigate(DetailRoute(id)) },
                    onOpenSettings = { nav.navigate(SettingsRoute()) },
                    onOpenLogbook = { nav.navigate(LogbookRoute) { launchSingleTop = true } },
                )
            }
            composable<SituationenRoute> {
                SituationsScreen(recommendations = Situations.all, resolve = { null }, onSelect = {})
            }
            composable<EigeneRoute> {
                OwnPatternsScreen(
                    savedPatterns = emptyList(),
                    onNewPattern = { nav.navigate(PatternEditorRoute()) },
                    onSelect = {},
                    onDeletePattern = {},
                    onOpenSettings = { nav.navigate(SettingsRoute()) },
                )
            }
            composable<MeditationRoute> {
                MeditationScreen(
                    initialConfig = MeditationConfig(),
                    speechAvailable = true,
                    gongIntervalMin = 5,
                    onOpenSettings = { nav.navigate(SettingsRoute(focusMeditation = true)) },
                    onDurationTick = {},
                    onStart = {},
                )
            }
            composable<SettingsRoute> { entry ->
                Settings(focusMeditation = entry.toRoute<SettingsRoute>().focusMeditation)
            }
            composable<LogbookRoute> { LogbookScreen(entries = emptyList(), onBack = { nav.popBackStack() }) }
            composable<GlossaryRoute> { GlossaryScreen(onBack = { nav.popBackStack() }) }
            composable<AboutRoute> { AboutScreen(onBack = { nav.popBackStack() }) }
            composable<AtemlehreRoute> { entry ->
                AtemlehreScreen(
                    onBack = { nav.popBackStack() },
                    onOpenExercise = { id -> nav.navigate(DetailRoute(id)) },
                    focus = entry.toRoute<AtemlehreRoute>().focus,
                )
            }
            composable<DetailRoute> { Text("Detail") }
            // Stellvertreter des Editors bei Platzmangel: braucht den Platz der Tab-Leiste.
            composable<PatternEditorRoute> {
                LaunchedEffect(Unit) { tabBarSpace.request(true) }
                Text("Editor")
            }
        }
    }

    @Test
    fun `offenes Overlay blendet die App darunter fuer TalkBack und Tastatur aus, geschlossen ist sie wieder da`() {
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(1)
        // Fokus liegt in der App (Tastatur), dann öffnet sich die Session darüber.
        rule.runOnIdle { tabFocus.requestFocus() }
        rule.waitForIdle()
        assertTrue(tabFocused)

        step { overlayOpen = true }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(0)
        rule.onAllNodesWithText("Atmen").assertCountEquals(0)
        rule.onAllNodesWithText(OVERLAY_STOP).assertCountEquals(1)
        assertFalse("Fokus bleibt nicht unsichtbar unter dem Overlay", tabFocused)
        focusedEver.clear()
        repeat(3) { rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) } }
        rule.waitForIdle()
        assertEquals("Tab läuft nur durch das Overlay", setOf(OVERLAY_STOP), focusedEver)

        step { overlayOpen = false }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(1)
        rule.onAllNodesWithText("Atmen").assertCountEquals(1)
        rule.runOnIdle { focusManager.clearFocus() }
        // Der echte Atmen-Tab hat vor der Tab-Leiste eigene Ziele (Kalender, Zahnrad, Tageskarte …).
        repeat(MAX_FOCUS_STEPS) {
            if (TAB_BAR !in focusedEver) rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        }
        rule.waitForIdle()
        assertTrue("Tab erreicht die App wieder", TAB_BAR in focusedEver)
    }

    /**
     * MUSTER-08: Erneuter Tipp auf den markierten Tab lässt die Startseite unverändert und führt aus
     * jeder darüber geöffneten Seite zurück zur Startseite – auch aus tiefem Stapel (Atmen über den
     * Kalender ins Logbuch, Meditation › Einstellungen › Atemlehre, sonst Einstellungen › Glossar).
     */
    @Test
    fun `jeder der vier Tabs schließt per erneutem Tipp eine darüber geöffnete Seite`() {
        tabs.forEach { tab ->
            val start = step { nav.tapTab(tab, selected = false) }
            assertEquals(start, step { nav.tapTab(tab, selected = true) })
            when (tab) {
                AtmenRoute -> assertEquals("AtmenRoute > LogbookRoute", step { rule.onNodeWithContentDescription(string(R.string.settings_log_title)).performClick() })

                MeditationRoute -> {
                    step { nav.navigate(SettingsRoute(focusMeditation = true)) }
                    step { nav.navigate(AtemlehreRoute(focus = "langhana")) }
                }

                else -> {
                    step { nav.navigate(SettingsRoute()) }
                    step { nav.navigate(GlossaryRoute) }
                }
            }
            assertEquals(start, step { nav.tapTab(tab, selected = true) })
        }
    }

    @Test
    fun `Tab-Leiste fehlt nur im Muster-Editor bei Platzmangel und ist nach dem Verlassen sofort wieder da`() {
        step { nav.tapTab(EigeneRoute, selected = false) }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(1)
        step { nav.navigate(PatternEditorRoute()) }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(0)

        // Weiter in die Atemlehre („Aus der Yoga-Lehre“): dort wieder da, zurück im Editor wieder weg.
        step { nav.navigate(AtemlehreRoute("langhana")) }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(1)
        step { nav.popBackStack() }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(0)

        // Zurück-Pfeil bzw. System-Zurück verlassen den Editor: Leiste wieder da.
        step { nav.popBackStack() }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(1)
        assertFalse(tabBarSpace.hidden)

        // Erneuter Tipp auf den Tab aus dem Editor heraus: zur Startseite, Leiste wieder da.
        step { nav.navigate(PatternEditorRoute()) }
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(0)
        assertEquals("AtmenRoute > EigeneRoute", step { nav.tapTab(EigeneRoute, selected = true) })
        rule.onAllNodesWithText(TAB_BAR).assertCountEquals(1)
    }

    /**
     * Material-Regel: Der Stapel des verlassenen Tabs bleibt erhalten (mit festem Zurück-Pfeil keine
     * Sackgasse), ein Re-Tap verwirft ihn; markiert ist immer der Tab des Stapels – auch wenn er
     * seinen Editor wiederherstellt.
     */
    @Test
    fun `Tab-Wechsel behält den Stapel des verlassenen Tabs, Re-Tap verwirft ihn`() {
        step { nav.tapTab(MeditationRoute, selected = false) }
        step { nav.navigate(SettingsRoute(focusMeditation = true)) }
        step { nav.tapTab(AtmenRoute, selected = false) }
        assertEquals(AtmenRoute, nav.tabOnStack())
        assertEquals("AtmenRoute > MeditationRoute > SettingsRoute", step { nav.tapTab(MeditationRoute, selected = false) })
        assertEquals(MeditationRoute, nav.tabOnStack())

        // Re-Tap verwirft die Einstellungen; sie kommen beim nächsten Wechsel nicht zurück.
        step { nav.tapTab(MeditationRoute, selected = true) }
        step { nav.tapTab(AtmenRoute, selected = false) }
        step { nav.navigate(DetailRoute("box")) }
        assertEquals(AtmenRoute, nav.tabOnStack())
        assertEquals("AtmenRoute", step { nav.tapTab(AtmenRoute, selected = true) })
        assertEquals("AtmenRoute > MeditationRoute", step { nav.tapTab(MeditationRoute, selected = false) })

        // Zurück zu „Eigene“: Der Editor liegt sofort oben, markiert ist trotzdem „Eigene“.
        step { nav.tapTab(EigeneRoute, selected = false) }
        step { nav.navigate(PatternEditorRoute()) }
        step { nav.tapTab(MeditationRoute, selected = false) }
        assertEquals("AtmenRoute > EigeneRoute > PatternEditorRoute", step { nav.tapTab(EigeneRoute, selected = false) })
        assertEquals(EigeneRoute, nav.tabOnStack())
    }

    // Kleinstes Gerät der Screenshot-Matrix: Das Glossar ist dort deutlich höher als der Bildschirm.
    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun `Zurück-Pfeil im Glossar bleibt beim Scrollen im Bild und führt zu den Einstellungen, System-Zurück schließt sie`() {
        step { nav.navigate(SettingsRoute()) }
        step { nav.navigate(GlossaryRoute) }

        // Ganz ans Ende scrollen: Der alte Kopf wäre damit sicher aus dem Bild.
        rule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, Float.MAX_VALUE) }
        rule.onNodeWithText("Uddiyana", substring = true).assertIsDisplayed()
        backArrow().assertIsDisplayed()
        assertEquals("AtmenRoute > SettingsRoute", step { backArrow().performClick() })
        assertEquals("AtmenRoute", step { rule.activity.onBackPressedDispatcher.onBackPressed() })
    }

    /** Führt [action] aus, wartet die Navigation ab und gibt den Stapel als Text zurück. */
    private fun step(action: () -> Unit): String {
        rule.runOnIdle(action)
        rule.waitForIdle()
        return nav.currentBackStack.value
            .filter { it.destination !is NavGraph }
            .joinToString(" > ") { it.destination.route!!.substringAfterLast('.').substringBefore('?').substringBefore('/') }
    }

    private fun string(id: Int) = rule.activity.getString(id)

    private companion object {
        const val TAB_BAR = "Tab-Leiste"
        const val OVERLAY_STOP = "Beenden"
        const val MAX_FOCUS_STEPS = 30
    }

    private fun backArrow() = rule.onNodeWithContentDescription(string(R.string.action_back))

    @Composable
    private fun Settings(focusMeditation: Boolean) {
        SettingsScreen(
            state = SettingsUiState(gongIntervalMin = 5, gongLong = false, piperEngineReady = true),
            actions = NoOpSettingsActions,
            onOpenGlossary = { nav.navigate(GlossaryRoute) },
            onOpenAtemlehre = { nav.navigate(AtemlehreRoute()) },
            onOpenAbout = { nav.navigate(AboutRoute) },
            onBack = { nav.popBackStack() },
            focusMeditation = focusMeditation,
        )
    }
}
