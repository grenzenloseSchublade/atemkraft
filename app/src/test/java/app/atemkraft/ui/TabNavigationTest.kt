package app.atemkraft.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.R
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Navigation zwischen Tabs und weitergeführten Seiten: Re-Tap auf den markierten Tab
 * ([tapTab]), Tab-Wechsel ([switchTab]) und Zurück über den festen Kopf ([PushHeader]).
 * Der NavHost hat dieselben Routen wie MainActivity; Einstellungen, Glossar und Über sind die
 * echten Screens, damit der Test auch den festen Zurück-Pfeil prüft. Gerätegröße wie das A54.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class TabNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var nav: NavHostController

    private val tabs = listOf(AtmenRoute, SituationenRoute, MeditationRoute, LogbuchRoute)

    @Before
    fun setUp() {
        rule.setContent {
            nav = rememberNavController()
            AtemkraftTheme {
                NavHost(nav, startDestination = AtmenRoute) {
                    composable<AtmenRoute> { Text("Atmen") }
                    composable<SituationenRoute> { Text("Situationen") }
                    composable<LogbuchRoute> { Text("Logbuch") }
                    composable<MeditationRoute> { Text("Meditation") }
                    composable<SettingsRoute> { entry ->
                        Settings(focusMeditation = entry.toRoute<SettingsRoute>().focusMeditation)
                    }
                    composable<GlossaryRoute> { GlossaryScreen(onBack = { nav.popBackStack() }) }
                    composable<AboutRoute> { AboutScreen(onBack = { nav.popBackStack() }) }
                    composable<DetailRoute> { Text("Detail") }
                }
            }
        }
        rule.waitForIdle()
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
        rule.onNodeWithText("Post-Exertional", substring = true).assertIsDisplayed()
        backArrow().assertIsDisplayed()
        assertEquals("AtmenRoute > SettingsRoute", step { backArrow().performClick() })
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

    /** Führt [action] aus, wartet die Navigation ab und gibt den Stapel als Text zurück. */
    private fun step(action: () -> Unit): String {
        rule.runOnIdle(action)
        rule.waitForIdle()
        return nav.currentBackStack.value
            .filter { it.destination !is NavGraph }
            .joinToString(" > ") { it.destination.route!!.substringAfterLast('.').substringBefore('?').substringBefore('/') }
    }

    private fun string(id: Int) = rule.activity.getString(id)

    private fun backArrow() = rule.onNodeWithContentDescription(string(R.string.action_back))

    @Composable
    private fun Settings(focusMeditation: Boolean) {
        SettingsScreen(
            soundMode = SoundMode.entries.first(),
            transition = TransitionEmphasis.entries.first(),
            volume = ToneVolume.entries.first(),
            haptics = true,
            showSafetyWarning = true,
            showNextPhase = true,
            onSoundMode = {},
            onTransition = {},
            onVolume = {},
            onToggleHaptics = {},
            onToggleSafety = {},
            onToggleNextPhase = {},
            gongIntervalMin = 5,
            onGongInterval = {},
            gongLong = false,
            onGongLong = {},
            onPreviewGong = {},
            voiceStates = emptyMap(),
            activeVoiceId = null,
            piperEngineReady = true,
            onSampleVoice = {},
            onDownloadVoice = {},
            onSelectVoice = {},
            onDeleteVoice = {},
            onOpenGlossary = { nav.navigate(GlossaryRoute) },
            onOpenAbout = { nav.navigate(AboutRoute) },
            onBack = { nav.popBackStack() },
            onExportPatterns = {},
            onImportPatterns = {},
            focusMeditation = focusMeditation,
        )
    }
}
