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
 * Höhenabhängige Gates in Gerätehöhe über alle Geräte ([TestMatrix.DEVICE_CONFIGS]): Muster-Editor
 * ohne Scrollen bis Schrift 1,3 samt Tab-Leisten-Ausblendung (MUSTER-10, [editorFit]) und
 * Meditations-Tab ohne Scrollen (LAYOUT-08). Dazu das A54 bei 1,1, der Einstellung des Nutzers:
 * Nur dort verlangt [editorFit] den Status in der Zeile von „Atemzug“ (Nachtrag 7).
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
     * Gespeichertes Muster mit langem eigenem Namen in Gerätehöhe wie in der App (Tab „Eigene“):
     * Lesezeichen gefüllt, Name im einzeiligen Feld, Start fest über der Tab-Leiste. Kurve und
     * Werkzeugleiste stehen ohne Scrollen ganz da ([editorFit], Nachtrag 5).
     */
    @Test
    fun patternEditorDevice() = snap("14_muster_editor_geraet", tab = Tab.EIGENE, measure = { editorFit("14_muster_editor_geraet") }) { EditSaved() }

    /**
     * Kompakt im ungünstigen Fall, in Gerätehöhe: intensives Muster (4-7-8 mit Halten voll 12 s,
     * Chip „intensiv“ am Kopf „Atemzug“, Hinweiskarte), Startpunkt „angepasst“, längster
     * Phasenname „Halten voll“ in der Werkzeugleiste – Kurve und Werkzeugleiste trotzdem ohne
     * Scrollen ([editorFit]).
     */
    @Test
    fun patternEditorCompact() = snap("14_muster_editor_kompakt", tab = Tab.EIGENE, measure = { editorFit("14_muster_editor_kompakt") }) {
        Editor {
            PatternEditorState.new(it, template("4-7-8")).apply {
                setSeconds(1, 12)
                select(1)
            }
        }
    }

    /** Zeitring auf 10 min (Standard): Zahl 10 in Cyan, Seite füllt die Höhe über der Tab-Leiste ohne Scrollen. */
    @Test
    fun meditation() = snap("03_meditation", tab = Tab.MEDITATION, noScroll = true) {
        MeditationTab(MeditationConfig())
    }
}
