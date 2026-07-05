package app.atemkraft.domain

/**
 * Dauer einer Phase. Drei Fälle, damit auch Wim-Hof von Tag eins funktioniert:
 *
 * - [Fixed]: feste Dauer in Millisekunden (Box, 4-7-8, kohärentes Atmen).
 * - [OpenEnded]: läuft, bis der Nutzer auf „weiter" tippt (z. B. freie Ruhephase).
 * - [UntilUrge]: Retention bis zum Atemreiz – Nutzer tippt selbst, die verstrichene
 *   Zeit wird hochgezählt und angezeigt (Wim-Hof-Halt auf leerer Lunge).
 */
sealed interface PhaseDuration {
    data class Fixed(val millis: Long) : PhaseDuration
    data object OpenEnded : PhaseDuration
    data object UntilUrge : PhaseDuration

    /** True, wenn die Phase erst durch eine Nutzeraktion endet. */
    val isUserPaced: Boolean
        get() = this is OpenEnded || this is UntilUrge
}
