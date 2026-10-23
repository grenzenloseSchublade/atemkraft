package app.atemkraft.domain

/**
 * Übungs-Kennungen generierter Muster an einer Stelle: das Muster des Tages, der noch nicht
 * gespeicherte Entwurf im Muster-Editor und gespeicherte Muster („muster-7“ für den Eintrag mit
 * der Datenbank-Id 7). Eingebaute Übungen tragen ihre Kennung selbst (Datenschicht,
 * `BuiltInExercises`).
 */
object ExerciseIds {
    /** Das Muster des Tages auf dem Atmen-Tab. */
    const val DAILY = "muster-des-tages"

    /** Der noch nicht gespeicherte Entwurf im Muster-Editor („Starten“ ohne Speichern). */
    const val DRAFT = "muster-entwurf"

    /** Präfix gespeicherter Muster (z. B. „muster-7“). */
    const val SAVED_PREFIX = "muster-"

    /** Übungs-Kennung des gespeicherten Musters mit der Datenbank-Id [id]. */
    fun saved(id: Long): String = "$SAVED_PREFIX$id"

    /** Datenbank-Id aus einer Übungs-Kennung („muster-7“ → 7); null für andere Übungen. */
    fun savedIdOf(exerciseId: String): Long? = exerciseId.takeIf { it.startsWith(SAVED_PREFIX) }?.removePrefix(SAVED_PREFIX)?.toLongOrNull()
}
