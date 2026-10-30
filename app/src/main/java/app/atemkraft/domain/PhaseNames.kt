package app.atemkraft.domain

/**
 * Phasennamen in Texten der Domain: Muster in Worten („Einatmen 4 s · Halten 2 s“), Phasen des
 * Muster-Editors, TalkBack-Beschreibung der Kurve.
 *
 * Quelle (festgelegt 2026-10-23): Die UI-Namen der Phasen stehen in `strings.xml` (`phase_*`,
 * übersetzbar, TEXT-01). domain/ kennt keine Android-Ressourcen; diese Konstanten spiegeln sie.
 * [INHALE], [EXHALE], [HOLD], [HOLD_FULL] und [HOLD_EMPTY] müssen gleich `phase_inhale`,
 * `phase_exhale`, `phase_hold`, `phase_hold_full` und `phase_hold_empty` lauten (ContentRulesTest
 * `phase-names`). Die freie Folge benennt ein Halten nach seiner Höhe ([FreePattern.labelAt]), die
 * Session nach seinem Typ; [HOLD] allein heißt die Pause zwischen zwei Stufen (Viloma), dort als
 * eigenes Label der Phase (TEXT-02).
 */
object PhaseNames {
    const val INHALE = "Einatmen"
    const val EXHALE = "Ausatmen"
    const val HOLD = "Halten"
    const val HOLD_FULL = "Halten voll"
    const val HOLD_EMPTY = "Halten leer"
}
