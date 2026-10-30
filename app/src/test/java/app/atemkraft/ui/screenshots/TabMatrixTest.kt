package app.atemkraft.ui.screenshots

import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.PhaseType
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMatrix.Device
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Referenzbilder (A-12) und Tablet/Querformat (S-07): die Startseiten der vier Tabs mit den
 * Leisten der App und die laufende Atemübung und Meditation, je in Gerätegröße auf
 * [TestMatrix.TAB_CONFIGS] (A54 hoch und quer, Tablet hoch und quer). Alle Gates des
 * [ScreenshotHarness] laufen mit; dazu ist jedes Bild ein Referenzbild im Repo
 * (`app/src/test/screenshots/`), das `scripts/check.sh` pixelgenau vergleicht – so fällt auch eine
 * rein optische Änderung auf. Gewollte Änderungen: `scripts/build.sh recordRoborazziDebug` und die
 * neuen Bilder mitcommitten (STYLEGUIDE §13.1).
 *
 * Bewusst nur Grundzustände in Gerätegröße: Die hohen Listenbilder der Breiten-Matrix wären als
 * Referenz rund 30 MB und änderten sich mit jedem Inhaltstext.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Fenster breit genug für das Tablet quer; gerendert wird nur der Testknoten.
@Config(qualifiers = "w1280dp-h1280dp-xhdpi")
class TabMatrixTest(device: Device, fontScale: Float) : ScreenshotHarness(device, fontScale) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_fs{1}")
        fun params(): List<Array<Any>> = TestMatrix.TAB_CONFIGS.map { (d, fs) -> arrayOf<Any>(d, fs) }
    }

    @Test
    fun tabs() {
        snap("tab_atmen", tab = Tab.ATMEN, reference = true) { BreatheTab() }
        snap("tab_eigene", tab = Tab.EIGENE, reference = true) { OwnTab() }
        snap("tab_meditation", tab = Tab.MEDITATION, reference = true) { MeditationTab(MeditationConfig(minutes = 12)) }
        snap("tab_lernen", tab = Tab.LERNEN, reference = true) { LearnTab() }
    }

    @Test
    fun sessions() {
        val exercise = BuiltInExercises.all.first { it.id == "4-7-8" }
        val inhale = sessionAt(exercise, elapsedMs = 1_600) { it.roundIndex == 2 && it.type == PhaseType.INHALE }
        snap("sitzung_einatmen", heightDp = device.heightDp, reference = true) { SessionShot(inhale) }
        snap("meditation_laeuft", heightDp = device.heightDp, reference = true) {
            MeditationShot(MeditationUiState(status = MeditationStatus.RUNNING, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L))
        }
    }
}
