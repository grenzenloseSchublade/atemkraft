package app.atemkraft.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize

/**
 * Laufen System-Animationen (Animator-Skala > 0)? Einmal je Komposition gelesen. Sind sie aus,
 * steht alles still, was sich von selbst bewegt (MOTION-02): Laufschrift, Vorschau der Atemkurve,
 * vāyu-Bild, Tap-Flash.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean = remember { ValueAnimator.areAnimatorsEnabled() }

/**
 * Zentrale Bewegungs-Tokens (MOTION-04): Dauern in ms und die wenigen Animations-Specs, die mehr
 * als eine Stelle teilen (Aufklappen, Aufdecken, Schimmer).
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

    /**
     * Trefferstatus der Befindens-Suche (`SearchStatus`): so lange muss die Zahl stehen, bevor sie
     * erscheint und angesagt wird – beim Tippen spräche die Live-Region sonst je Buchstabe (A11Y-04).
     */
    val SearchStatusSettle = 600L

    /** vāyu-Bild: Strömungslinien rücken in dieser Zeit um zwei Strich-Abstände weiter (ruhig). */
    val VayuFlow = 2400

    /** vāyu-Bild: Linien der gewählten Strömung fließen schneller. */
    val VayuFlowSelected = 1100

    /** vāyu-Bild: ein Pulsieren der gewählten Fläche, hin und zurück. */
    val VayuPulse = 2600

    /**
     * Auf- und Zuklappen: Höhe bzw. Breite von Expandern, Abschnitten und der Tab-Leiste
     * (`animateContentSize`) und von aufgedeckten Teilen (`expand…`/`shrink…`). Die Default-Spring
     * von Compose (weich, ohne Bounce), hier nur benannt, damit alle Stellen denselben Wert tragen.
     */
    val Disclosure: FiniteAnimationSpec<IntSize> = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)

    /** Ein- und Ausblenden eines aufgedeckten Teils (`fadeIn`/`fadeOut` zu [Disclosure]): Default-Spring von Compose. */
    val Reveal: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)

    /**
     * Schimmer des Scheins hinter dem Atemkreis (`BreathingCircle`): 8 s hin, 8 s zurück, sanft
     * an den Enden – Yoga-Tempo, kein Puls-Gefühl. Bewusst langsamer als neue UI-Bewegung
     * (MOTION-04); bei Animator-Skala 0 steht er still (MOTION-02).
     */
    val AmbientShimmer: DurationBasedAnimationSpec<Float> = tween(durationMillis = 8000, easing = EaseInOutSine)
}
