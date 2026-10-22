package app.atemkraft.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.R
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.components.inertWhen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.settings.NoOpSettingsActions
import app.atemkraft.ui.settings.SettingsActions
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.settings.SettingsUiState
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

/**
 * Navigation zwischen Tabs und weitergeführten Seiten: Re-Tap auf den markierten Tab
 * ([tapTab]), Tab-Wechsel ([switchTab]) und Zurück über den festen Kopf ([PushHeader]).
 * Der NavHost hat dieselben Routen wie MainActivity; Einstellungen, Glossar, Über und Atemlehre
 * sind die echten Screens, damit der Test auch den festen Zurück-Pfeil prüft. Gerätegröße wie das A54.
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

    // Sitzungen im Logbuch (Zeile in Einstellungen › Daten) und Tipps auf „Logbuch leeren“.
    private var logCount by mutableIntStateOf(3)
    private var logCleared = 0

    private var overlayOpen by mutableStateOf(false)
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
            composable<AtmenRoute> { Text("Atmen") }
            composable<SituationenRoute> { Text("Situationen") }
            composable<EigeneRoute> { Text("Eigene") }
            composable<MeditationRoute> { Text("Meditation") }
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
            composable<PatternEditorRoute> { Text("Editor") }
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
        repeat(2) { rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) } }
        rule.waitForIdle()
        assertTrue("Tab erreicht die App wieder", TAB_BAR in focusedEver)
    }

    @Test
    fun `erneuter Tipp auf den markierten Tab führt aus Einstellungen und Glossar zur Tab-Startseite`() {
        step { nav.tapTab(MeditationRoute, selected = false) }
        step { nav.navigate(SettingsRoute(focusMeditation = true)) }
        step { nav.navigate(GlossaryRoute) }

        assertEquals("AtmenRoute > MeditationRoute", step { nav.tapTab(MeditationRoute, selected = true) })
    }

    @Test
    fun `erneuter Tipp auf Atmen schließt die Detailseite und lässt den Meditations-Stapel sauber`() {
        step { nav.tapTab(MeditationRoute, selected = false) }
        step { nav.navigate(SettingsRoute(focusMeditation = true)) }
        step { nav.tapTab(MeditationRoute, selected = true) }
        step { nav.tapTab(AtmenRoute, selected = false) }
        step { nav.navigate(DetailRoute("box")) }

        assertEquals("AtmenRoute", step { nav.tapTab(AtmenRoute, selected = true) })
        // Die verworfenen Einstellungen kommen beim nächsten Tab-Wechsel nicht zurück.
        assertEquals("AtmenRoute > MeditationRoute", step { nav.tapTab(MeditationRoute, selected = false) })
    }

    @Test
    fun `jeder der vier Tabs schließt per erneutem Tipp eine darüber geöffnete Seite`() {
        tabs.forEach { tab ->
            step { nav.tapTab(tab, selected = false) }
            step { nav.navigate(SettingsRoute()) }
            val expected = if (tab == AtmenRoute) "AtmenRoute" else "AtmenRoute > ${tab::class.simpleName}"
            assertEquals(expected, step { nav.tapTab(tab, selected = true) })
        }
    }

    @Test
    fun `erneuter Tipp auf den Tab führt aus dem Muster-Editor zur Tab-Startseite`() {
        // Tab „Eigene“ › Neues Muster.
        step { nav.tapTab(EigeneRoute, selected = false) }
        step { nav.navigate(PatternEditorRoute()) }
        assertEquals("AtmenRoute > EigeneRoute", step { nav.tapTab(EigeneRoute, selected = true) })

        // Atmen › Detail des gespeicherten Musters › Stift.
        step { nav.tapTab(AtmenRoute, selected = false) }
        step { nav.navigate(DetailRoute("muster-7")) }
        step { nav.navigate(PatternEditorRoute(patternId = 7)) }
        assertEquals("AtmenRoute", step { nav.tapTab(AtmenRoute, selected = true) })
    }

    @Test
    fun `erneuter Tipp auf der Tab-Startseite lässt den Stapel unverändert`() {
        tabs.forEach { tab ->
            val before = step { nav.tapTab(tab, selected = false) }
            assertEquals(before, step { nav.tapTab(tab, selected = true) })
        }
    }

    @Test
    fun `Tab-Wechsel behält den Stapel des verlassenen Tabs`() {
        step { nav.tapTab(MeditationRoute, selected = false) }
        step { nav.navigate(SettingsRoute(focusMeditation = true)) }
        step { nav.tapTab(AtmenRoute, selected = false) }

        // Material-Regel: Die Einstellungen bleiben dem Meditations-Tab erhalten; mit festem
        // Zurück-Pfeil ist das keine Sackgasse mehr.
        assertEquals(
            "AtmenRoute > MeditationRoute > SettingsRoute",
            step { nav.tapTab(MeditationRoute, selected = false) },
        )
    }

    @Test
    fun `Zurück-Pfeil der Einstellungen steht nach dem Sprung zur Meditations-Karte im Bild und führt zurück`() {
        step { nav.tapTab(MeditationRoute, selected = false) }
        step { nav.navigate(SettingsRoute(focusMeditation = true)) }

        rule.onNodeWithText(string(R.string.settings_title)).assertIsDisplayed()
        backArrow().assertIsDisplayed()
        assertEquals("AtmenRoute > MeditationRoute", step { backArrow().performClick() })
    }

    // Kleinstes Gerät der Screenshot-Matrix: Das Glossar ist dort deutlich höher als der Bildschirm.
    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun `Zurück-Pfeil im Glossar bleibt beim Scrollen im Bild und führt zu den Einstellungen`() {
        step { nav.navigate(SettingsRoute()) }
        step { nav.navigate(GlossaryRoute) }

        // Ganz ans Ende scrollen: Der alte Kopf wäre damit sicher aus dem Bild.
        rule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, Float.MAX_VALUE) }
        rule.onNodeWithText("Uddiyana", substring = true).assertIsDisplayed()
        backArrow().assertIsDisplayed()
        assertEquals("AtmenRoute > SettingsRoute", step { backArrow().performClick() })
    }

    @Test
    fun `Zurück-Pfeil der Atemlehre führt zu den Einstellungen, erneuter Tab-Tipp zur Startseite`() {
        step { nav.tapTab(MeditationRoute, selected = false) }
        step { nav.navigate(SettingsRoute(focusMeditation = true)) }
        step { nav.navigate(AtemlehreRoute()) }

        rule.onNodeWithText(string(R.string.atemlehre_title)).assertIsDisplayed()
        assertEquals("AtmenRoute > MeditationRoute > SettingsRoute", step { backArrow().performClick() })

        step { nav.navigate(AtemlehreRoute(focus = "langhana")) }
        assertEquals("AtmenRoute > MeditationRoute", step { nav.tapTab(MeditationRoute, selected = true) })
    }

    @Test
    fun `System-Zurück schließt die Einstellungen`() {
        step { nav.tapTab(MeditationRoute, selected = false) }
        step { nav.navigate(SettingsRoute(focusMeditation = true)) }

        assertEquals(
            "AtmenRoute > MeditationRoute",
            step { rule.activity.onBackPressedDispatcher.onBackPressed() },
        )
    }

    /** Die Navigationszeile „Logbuch“ der Daten-Karte (Titel und Zahl als ein Button). */
    private fun logRow() = rule.onNode(
        SemanticsMatcher("Klick-Label „Logbuch öffnen“") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == string(R.string.settings_logbook_open)
        },
    )

    @Test
    fun `Logbuch-Zeile der Einstellungen öffnet das Logbuch, Zurück-Pfeil führt zu den Einstellungen`() {
        step { nav.tapTab(EigeneRoute, selected = false) }
        step { nav.navigate(SettingsRoute()) }

        logRow()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTextContains(string(R.string.settings_log_title))
        assertEquals("AtmenRoute > EigeneRoute > SettingsRoute > LogbookRoute", step { logRow().performScrollTo().performClick() })
        rule.onNodeWithText(string(R.string.log_sessions_none)).assertIsDisplayed()

        assertEquals("AtmenRoute > EigeneRoute > SettingsRoute", step { backArrow().performClick() })
        step { logRow().performScrollTo().performClick() }
        // Re-Tap auf den markierten Tab führt auch aus dem Logbuch zur Tab-Startseite.
        assertEquals("AtmenRoute > EigeneRoute", step { nav.tapTab(EigeneRoute, selected = true) })
    }

    @Test
    fun `Papierkorb der Logbuch-Zeile leert zweistufig und öffnet das Logbuch nicht`() {
        step { nav.navigate(SettingsRoute()) }
        val trash = rule.onNodeWithContentDescription(string(R.string.settings_data_clear_log))
        trash.performScrollTo()
        assertEquals("AtmenRoute > SettingsRoute", step { trash.performClick() })
        assertEquals(0, logCleared)
        assertEquals(
            "AtmenRoute > SettingsRoute",
            step { rule.onNodeWithContentDescription(string(R.string.cd_log_clear_confirm)).performClick() },
        )
        assertEquals(1, logCleared)

        // Ohne Einträge kein Papierkorb – die Zeile öffnet das leere Logbuch weiterhin.
        step { logCount = 0 }
        rule.onNodeWithContentDescription(string(R.string.settings_data_clear_log)).assertDoesNotExist()
        assertEquals("AtmenRoute > SettingsRoute > LogbookRoute", step { logRow().performScrollTo().performClick() })
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
    }

    private fun backArrow() = rule.onNodeWithContentDescription(string(R.string.action_back))

    @Composable
    private fun Settings(focusMeditation: Boolean) {
        SettingsScreen(
            state = SettingsUiState(gongIntervalMin = 5, gongLong = false, piperEngineReady = true, logCount = logCount),
            actions = object : SettingsActions by NoOpSettingsActions {
                override fun clearLog() {
                    logCleared++
                }
            },
            onOpenGlossary = { nav.navigate(GlossaryRoute) },
            onOpenAtemlehre = { nav.navigate(AtemlehreRoute()) },
            onOpenAbout = { nav.navigate(AboutRoute) },
            onBack = { nav.popBackStack() },
            onOpenLogbook = { nav.navigate(LogbookRoute) },
            focusMeditation = focusMeditation,
        )
    }
}
