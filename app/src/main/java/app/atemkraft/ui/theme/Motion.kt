package app.atemkraft.ui.theme

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Laufen System-Animationen (Animator-Skala > 0)? Einmal je Komposition gelesen. Sind sie aus,
 * steht alles still, was sich von selbst bewegt (MOTION-02): Laufschrift, Vorschau der Atemkurve,
 * vāyu-Bild, Tap-Flash.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean = remember { ValueAnimator.areAnimatorsEnabled() }

/**
 * Zentrale Bewegungs-Tokens (MOTION-04): Dauern in ms. Tap-Flash und Navigation; weitere
 * Dauern (Schimmer) ziehen nach, wenn ihre Stelle angefasst wird.
 */
object Motion {
    /** Push-Ziele (Detail, Einstellungen, …): Hinein-/Hinausgleiten samt Blende. */
    val NavPush = 300

    /** Tab-Wechsel: nur dezentes Überblenden, kein Gleiten. */
    val NavTab = 180

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
