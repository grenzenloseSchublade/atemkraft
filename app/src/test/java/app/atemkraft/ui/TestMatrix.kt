package app.atemkraft.ui

/**
 * Gemeinsame Test-Matrix: Geräte, Schriftgrößen und Robolectric-Qualifier an einer Stelle für
 * ScreenshotTest, DeviceFitTest und die Interaktionstests. Das SDK (35) steht in
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

    /** Samsung-Voreinstellung des A54 (Einstellung des Nutzers). */
    const val A54_FONT_SCALE = 1.1f

    /** Größte Android-Schriftgröße (LAYOUT-03). */
    const val MAX_FONT_SCALE = 2.0f

    /** Breiten-Matrix des ScreenshotTest: die schmalste Breite bei Standard- und größter Schrift. */
    val WIDTH_CONFIGS: List<Pair<Device, Float>> = listOf(COMPACT to 1.0f, COMPACT to MAX_FONT_SCALE)

    /**
     * Gerätehöhen-Matrix des DeviceFitTest (Muster-Editor ohne Scrollen bis Schrift 1,3, ohne
     * Tab-Leiste): 360 × 640 bei 1,3 (engster Fall), das A54 bei seiner Voreinstellung 1,1 (Status
     * neben der Startpunkt-Pille) und bei 1,3.
     */
    val DEVICE_CONFIGS: List<Pair<Device, Float>> = listOf(COMPACT to 1.3f, A54 to A54_FONT_SCALE, A54 to 1.3f)

    /** Fenster des A54 für Interaktionstests, in xxhdpi wie das Gerät. */
    const val A54_QUALIFIERS = "w384dp-h832dp-xxhdpi"
}
