package app.atemkraft.ui.screenshots

import app.atemkraft.domain.MeditationConfig
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMatrix.Device
import app.atemkraft.ui.patterns.PatternEditorState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Muster-Editor und Meditations-Tab in Gerätehöhe ([TestMatrix.DEVICE_CONFIGS]): Kurve und
 * Werkzeugleiste stehen bis Schrift 1,3 ohne Scrollen da, die Tab-Leiste steht in der Tiefe nie
 * (MUSTER-10, [editorFit]); der Meditations-Tab scrollt bis 1,3 nicht (LAYOUT-08).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h8000dp-xhdpi")
class DeviceFitTest(device: Device, fontScale: Float) : ScreenshotHarness(device, fontScale) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_fs{1}")
        fun params(): List<Array<Any>> = TestMatrix.DEVICE_CONFIGS.map { (d, fs) -> arrayOf<Any>(d, fs) }
    }

    /**
     * Ungünstigster Fall: intensives Muster (4-7-8 mit Halten voll 12 s, Chip „intensiv“ neben dem
     * Namen, Hinweiskarte), Startpunkt „angepasst“, längster Phasenname „Halten voll“ in der
     * Werkzeugleiste.
     */
    @Test
    fun patternEditor() = snap("14_muster_editor_kompakt", tab = Tab.EIGENE, tabBar = false, measure = { editorFit("14_muster_editor_kompakt") }) {
        Editor {
            PatternEditorState.new(it, template("4-7-8")).apply {
                setSeconds(1, 12)
                select(1)
            }
        }
    }

    /**
     * Meditations-Tab in Gerätehöhe mit Tab-Leiste: scrollt bis Schrift 1,3 nicht (LAYOUT-08), der
     * Zeitring bekommt den ganzen Platz; seine Kante steht als `RING` in der `.tsv`.
     */
    @Test
    fun meditation() = snap("03_meditation", tab = Tab.MEDITATION, noScroll = true, measure = { meditationRing("03_meditation") }) {
        MeditationTab(MeditationConfig(minutes = 12))
    }
}
