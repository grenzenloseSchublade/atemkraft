package app.atemkraft.ui.theme

/**
 * Zentrale Bewegungs-Tokens (MOTION-04): Dauern in ms. Tap-Flash und Navigation; weitere
 * Dauern (Schimmer) ziehen nach, wenn ihre Stelle angefasst wird.
 */
object Motion {
    /** Push-Ziele (Detail, Einstellungen, …): Hinein-/Hinausgleiten samt Blende. */
    val NavPush = 300

    /** Tab-Wechsel: nur dezentes Überblenden, kein Gleiten. */
    val NavTab = 180

    /**
     * Ruhe-Ansicht der Atem-Session: Bedienelemente blenden sanft aus und ein (`HideableSlot`).
     * Bewusst langsamer als neue UI-Bewegung (MOTION-04): Das Ausblenden soll nicht auffallen.
     */
    val CalmFade = 500

    /** Knöpfe der Session-Steuerung erscheinen nach der Vorbereitung (`SessionControlBar`). */
    val ControlFade = 300

    /** Auf- und Zuklappen eines Abschnitts, dessen Nachbarn mitwachsen (Meditation „Klang & Stimme“). */
    val Expand = 250

    /** Tap-Flash: Halt in voller Deckkraft, bevor das Symbol ausblendet. */
    val TapFlashHold = 220L

    /** Tap-Flash: Dauer eines Ausblend-Schritts ([TapFlashFadeSteps] Schritte, zusammen ≈ 0,5 s). */
    val TapFlashFadeStep = 34L

    /** Tap-Flash: Anzahl der Ausblend-Schritte. */
    val TapFlashFadeSteps = 16

    /**
     * Tap-Flash bei abgeschalteten Animationen (Animator-Skala 0): so lange steht das Symbol
     * still, dann verschwindet es ohne Übergang (MOTION-02).
     */
    val TapFlashStatic = 500L

    /** vāyu-Bild: Strömungslinien rücken in dieser Zeit um zwei Strich-Abstände weiter (ruhig). */
    val VayuFlow = 2400

    /** vāyu-Bild: Linien der gewählten Strömung fließen schneller. */
    val VayuFlowSelected = 1100

    /** vāyu-Bild: ein Pulsieren der gewählten Fläche, hin und zurück. */
    val VayuPulse = 2600
}
