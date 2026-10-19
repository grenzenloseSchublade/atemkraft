package app.atemkraft.ui.theme

/**
 * Zentrale Bewegungs-Tokens (MOTION-04): Dauern in ms. Bisher nur der Tap-Flash; weitere
 * Dauern (Navigation, Schimmer) ziehen nach, wenn ihre Stelle angefasst wird.
 */
object Motion {
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
}
