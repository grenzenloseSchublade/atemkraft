package app.atemkraft.ui

/**
 * Gemeinsame Test-Matrix (TEST-5): Geräte, Schriftgrößen und Robolectric-Qualifier an einer
 * Stelle für ScreenshotTest, ControlTouchTest und die Interaktionstests. Das SDK (35) steht in
 * `src/test/resources/robolectric.properties`.
 */
object TestMatrix {

    /** Breite × Höhe in dp, wie das Gerät sie der App meldet (Standard-Anzeigegröße). */
    data class Device(val name: String, val widthDp: Int, val heightDp: Int) {
        override fun toString() = name
    }

    /** Kleine Geräte und große Anzeigegröße auf Mittelklasse-Geräten. */
    val COMPACT = Device("kompakt360", 360, 640)

    /** Galaxy A54 (1080 × 2340 px, 450 dpi): das Testgerät des Maintainers. */
    val A54 = Device("a54", 384, 832)

    /** Pixel 7/8, Galaxy S24+ und ähnliche. */
    val LARGE = Device("gross412", 412, 915)

    val DEVICES = listOf(COMPACT, A54, LARGE)

    /** Samsung-Voreinstellung des A54. */
    const val A54_FONT_SCALE = 1.1f

    /** Größte Android-Schriftgröße (LAYOUT-03). */
    const val MAX_FONT_SCALE = 2.0f

    /**
     * 1,0 Standard · 1,3 groß · 2,0 maximal (LAYOUT-03). Die 1,1 des A54 liegt dazwischen und
     * bringt für Wortbruch, Tippflächen und Scrollen nichts Neues (360 dp bei 1,3 ist schmaler und
     * größer); sie läuft nur in [DEVICE_CONFIGS] mit.
     */
    val FONT_SCALES = listOf(1.0f, 1.3f, MAX_FONT_SCALE)

    /**
     * Breiten-Matrix des ScreenshotTest: die schmalste Breite bei jeder Schrift (ungünstigster Fall
     * für Wortbruch, Abschneiden, Tippflächen und – mit 640 dp – Höhe), dazu das A54 bei 1,0 als
     * Bild des Hauptgeräts und Quelle der README-Bilder.
     */
    val WIDTH_CONFIGS: List<Pair<Device, Float>> = FONT_SCALES.map { COMPACT to it } + (A54 to 1.0f)

    /**
     * Gerätehöhen-Matrix für die höhenabhängigen Gates (Muster-Editor, Meditations-Tab): jedes Gerät
     * bei jeder Schrift, dazu das A54 bei seiner Voreinstellung 1,1 (Einstellung des Nutzers;
     * `editorFit` verlangt nur dort den Status in der Zeile von „Atemzug“).
     */
    val DEVICE_CONFIGS: List<Pair<Device, Float>> = DEVICES.flatMap { d -> FONT_SCALES.map { d to it } } + (A54 to A54_FONT_SCALE)

    /**
     * Fenster des A54 für Interaktions- und Semantiktests, in xxhdpi wie das Gerät. Der
     * ScreenshotTest rendert dagegen in xhdpi: Die Bilder bleiben halb so groß, an Wortbruch,
     * Tippflächen und Scrollen ändert die Dichte nichts.
     */
    const val A54_QUALIFIERS = "w384dp-h832dp-xxhdpi"

    /**
     * Die zwei aussagekräftigsten Kombinationen für teure oder seltene Bilder (Querformat,
     * Vorlagen-Sheet, Leerzustände): Testgerät bei 1,0 und kleinstes Gerät mit größter Schrift.
     */
    fun isKeyConfig(device: Device, fontScale: Float): Boolean = (device == A54 && fontScale == 1.0f) || (device == COMPACT && fontScale == MAX_FONT_SCALE)

    /**
     * Detailseiten (eine je Übung): die Schlüssel-Kombinationen und dazu das kleinste Gerät bei
     * 1,3 – lange Fachtexte und Chips brechen dort zuerst, das Querformat kommt dagegen erst bei
     * 2,0 an seine Grenze.
     */
    fun isDetailConfig(device: Device, fontScale: Float): Boolean = isKeyConfig(device, fontScale) || (device == COMPACT && fontScale >= 1.3f)
}
