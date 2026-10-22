package app.atemkraft.ui.patterns

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.R
import app.atemkraft.data.ExerciseRepository
import app.atemkraft.data.FakeSavedPatternDao
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.toFreePattern
import app.atemkraft.ui.DetailRoute
import app.atemkraft.ui.EigeneRoute
import app.atemkraft.ui.PatternEditorRoute
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.detail.rememberDetailExercise
import app.atemkraft.ui.theme.AtemkraftTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verdrahtung des Muster-Editors wie in MainActivity: Detailseite eines gespeicherten Musters
 * (live aus dem Repository, [rememberDetailExercise]) → Stift → Editor ([PatternEditorDestination])
 * → Speichern → Zurück. Dazu: Start aus dem Editor lässt ihn auf dem Stapel, ein neues Muster aus
 * dem Tab „Eigene“, das Tagesmuster als Ausgang, ein unauflösbares Muster.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class PatternEditorFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var nav: NavHostController
    private val dao = FakeSavedPatternDao()
    private val repo = SavedPatternsRepository(dao)
    private val exercises = ExerciseRepository(repo)
    private val started = mutableListOf<Pair<Exercise, SessionConfig>>()
    private val detailStarts = mutableListOf<Pair<Exercise, SessionConfig>>()

    private fun show() {
        rule.setContent {
            nav = rememberNavController()
            val scope = rememberCoroutineScope()
            AtemkraftTheme {
                NavHost(nav, startDestination = EigeneRoute) {
                    composable<EigeneRoute> { Text("Eigene") }
                    composable<DetailRoute> { entry -> Detail(entry.toRoute<DetailRoute>().exerciseId, entry.id) }
                    composable<PatternEditorRoute> { entry ->
                        val route = entry.toRoute<PatternEditorRoute>()
                        PatternEditorDestination(
                            patternId = route.patternId,
                            fromDaily = route.fromDaily,
                            repository = repo,
                            dailySpec = { route.dailySpec() ?: exercises.daily().spec },
                            scope = scope,
                            onStart = { exercise, config -> started += exercise to config },
                            onOpenLore = {},
                            onBack = { nav.popBackStack() },
                            onMissing = { if (nav.currentBackStackEntry?.id == entry.id) nav.popBackStack() },
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    /** Detailziel wie in MainActivity: gespeicherte Muster live, Stift nur an generierten Mustern. */
    @Composable
    private fun Detail(id: String, entryId: String) {
        val lookup = rememberDetailExercise(id, exercises::byId) { exerciseId ->
            SavedPatternsRepository.idOf(exerciseId)?.let { repo.observe(exerciseId) }
        }
        val exercise = lookup.exercise
        if (exercise == null) {
            if (lookup.missing) {
                LaunchedEffect(entryId) { if (nav.currentBackStackEntry?.id == entryId) nav.popBackStack() }
            }
            return
        }
        val savedId = SavedPatternsRepository.idOf(id)
        ExerciseDetailScreen(
            exercise = exercise,
            requireSafetyConfirm = false,
            savedIntervals = null,
            onIntervalsChange = { _, _, _, _ -> },
            onIntervalsReset = {},
            onConfirmedSafety = {},
            onStart = { config -> detailStarts += exercise to config },
            onBack = { nav.popBackStack() },
            onEditPattern = when {
                id == RandomPatternGenerator.ID -> {
                    { nav.navigate(RandomPatternGenerator.specOf(exercise)?.let(PatternEditorRoute::of) ?: PatternEditorRoute(fromDaily = true)) }
                }

                savedId != null -> {
                    { nav.navigate(PatternEditorRoute(patternId = savedId)) }
                }

                else -> null
            },
        )
    }

    /** Freie Folge Ein [inhale], Halten [hold] (ohne bei null), Aus [exhale]. */
    private fun free(inhale: Int, hold: Int?, exhale: Int) = FreePattern(
        listOfNotNull(
            FreePhase(FreePhaseType.IN, inhale),
            hold?.let { FreePhase(FreePhaseType.HOLD, it) },
            FreePhase(FreePhaseType.OUT, exhale),
        ),
    )

    private fun saveCustom(pattern: FreePattern, name: String) = runBlocking { repo.saveCustom(pattern, 5, name).id }

    private fun string(id: Int) = rule.activity.getString(id)

    /** Führt [action] aus, wartet ab und gibt den Stapel als Text zurück. */
    private fun step(action: () -> Unit): String {
        rule.runOnIdle(action)
        rule.waitForIdle()
        return nav.currentBackStack.value
            .filter { it.destination !is NavGraph }
            .joinToString(" > ") { it.destination.route!!.substringAfterLast('.').substringBefore('?').substringBefore('/') }
    }

    private fun bookmark() = rule.onNodeWithContentDescription(string(R.string.cd_pattern_save))

    private fun back() = rule.onNodeWithContentDescription(string(R.string.action_back))

    private fun stepper(spoken: String, action: Int) = rule.onNodeWithContentDescription("$spoken, ${string(action)}")

    /** „Ablauf“ aufklappen (eingeklappt beim Öffnen) und die Zeile mit dem Phasennamen [label] wählen. */
    private fun selectRow(label: String) {
        rule.onNode(hasText(string(R.string.pattern_editor_sequence)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
            .performScrollTo()
            .performClick()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) and hasText(label))
            .performScrollTo()
            .performClick()
    }

    @Test
    fun `Detail - Stift - Werte aendern - Speichern - Zurueck zeigt und startet die neuen Werte`() {
        val id = saveCustom(free(4, 1, 6), "")
        show()
        step { nav.navigate(DetailRoute("muster-$id")) }
        rule.onNodeWithText("Eigenes Muster 4·1·6").assertIsDisplayed()

        val editor = step { rule.onNodeWithContentDescription(string(R.string.cd_pattern_edit)).performClick() }
        assertEquals("EigeneRoute > DetailRoute > PatternEditorRoute", editor)
        bookmark().assertIsDisplayed()
        selectRow("Ausatmen")
        stepper(rule.activity.getString(R.string.pattern_editor_phase_spoken, 3, "Ausatmen"), R.string.adjust_increase).performScrollTo().performClick()
        // Die Dauer steht in der festen Leiste unten (kein Scrollen nötig).
        repeat(5) { stepper(string(R.string.cd_adjust_duration), R.string.adjust_increase).performClick() }
        step { bookmark().performClick() }

        // Dieselbe Zeile, neue Folge und Dauer, Name folgt den Zahlen.
        val row = dao.rows.value.single()
        assertEquals(id, row.id)
        assertEquals("in:4,hold:1,out:7", row.phases)
        assertEquals(10, row.minutes)
        assertEquals("Eigenes Muster 4·1·7", row.name)
        assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, row.origin)

        assertEquals("EigeneRoute > DetailRoute", step { back().performClick() })
        rule.onNodeWithText("Eigenes Muster 4·1·7").assertIsDisplayed()
        rule.onAllNodesWithText("Ausatmen\u00A07\u00A0s", substring = true).assertCountEquals(2)
        rule.onAllNodesWithText("Ausatmen\u00A06\u00A0s", substring = true).assertCountEquals(0)
        rule.onNodeWithText("Eigenes Muster 4·1·6").assertDoesNotExist()

        // Die Detailseite startet das neue Muster ohne alte Phasen und mit der neuen Dauer.
        step { rule.onNodeWithText(string(R.string.action_start), substring = true).performClick() }
        val (exercise, config) = detailStarts.single()
        assertEquals("muster-$id", exercise.id)
        assertEquals(listOf(PhaseType.INHALE, PhaseType.HOLD_FULL, PhaseType.EXHALE), exercise.segments.single().phases.map { it.type })
        assertEquals(SessionConfig(minutes = 10), config)
    }

    @Test
    fun `Starten aus dem Editor laesst den Editor auf dem Stapel und nimmt den Entwurf`() {
        show()
        step { nav.navigate(PatternEditorRoute()) }
        // Großer Play-Knopf der festen Leiste („Übung starten, ca. 5 Minuten“).
        val stack = step { rule.onNodeWithContentDescription(string(R.string.cd_pattern_editor_play).substringBefore(","), substring = true).performClick() }

        assertEquals("EigeneRoute > PatternEditorRoute", stack)
        val (exercise, config) = started.single()
        assertEquals(RandomPatternGenerator.DRAFT_ID, exercise.id)
        assertEquals(SessionConfig(minutes = 5), config)
        assertEquals(listOf(PhaseType.INHALE, PhaseType.EXHALE), exercise.segments.single().phases.map { it.type })
        assertTrue("Ausprobieren speichert nicht", dao.rows.value.isEmpty())
    }

    @Test
    fun `Neues Muster - Lesezeichen legt an, erneuter Tipp nimmt es wieder heraus`() {
        show()
        step { nav.navigate(PatternEditorRoute()) }

        step { bookmark().performClick() }
        val row = dao.rows.value.single()
        assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, row.origin)
        assertEquals(5, row.minutes)
        assertEquals("Eigenes Muster 4·6", row.name)

        // In dieser Sitzung angelegt: ein Tipp genügt.
        step { bookmark().performClick() }
        assertTrue(dao.rows.value.isEmpty())
        assertEquals("EigeneRoute > PatternEditorRoute", step {})
    }

    @Test
    fun `Bestehendes Muster im Editor zweistufig entfernt - Zurueck ueberspringt die leere Detailseite`() {
        val id = saveCustom(free(4, 1, 6), "Abendruhe")
        show()
        step { nav.navigate(DetailRoute("muster-$id")) }
        step { rule.onNodeWithContentDescription(string(R.string.cd_pattern_edit)).performClick() }

        step { bookmark().performClick() }
        assertEquals("Erster Tipp schärft nur", 1, dao.rows.value.size)
        step { rule.onNodeWithContentDescription(rule.activity.getString(R.string.cd_delete_confirm, "Abendruhe")).performClick() }
        assertTrue(dao.rows.value.isEmpty())

        assertEquals("EigeneRoute", step { back().performClick() })
    }

    @Test
    fun `Muster des Tages als Ausgang - gespeichert wird ein eigenes Muster, nicht das Tagesmuster`() {
        show()
        step { nav.navigate(DetailRoute(RandomPatternGenerator.ID)) }
        val daily = exercises.daily()
        // Wie über Mitternacht: Das Repository liefert inzwischen ein anderes Tagesmuster – der
        // Stift übernimmt trotzdem das angezeigte.
        var next = exercises.regenerateDaily()
        while (next.spec == daily.spec) next = exercises.regenerateDaily()
        step { rule.onNodeWithContentDescription(string(R.string.cd_pattern_adapt_daily)).performClick() }
        step { bookmark().performClick() }

        val row = dao.rows.value.single()
        assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, row.origin)
        val saved = runBlocking { repo.loadForEdit(row.id)!! }
        assertEquals("als freie Folge gespeichert", daily.spec.toFreePattern(), saved.pattern)
        assertEquals(daily.spec.toFreePattern().toCompact(), row.phases)
        assertFalse("Lesezeichen der Tageskarte bleibt leer", repo.isDailySaved(daily, listOf(saved)))
    }

    @Test
    fun `Unaufloesbares Muster - der Editor geht zurueck statt leer zu bleiben`() {
        show()
        assertEquals("EigeneRoute", step { nav.navigate(PatternEditorRoute(patternId = 99)) })
    }
}
