package app.atemkraft.domain

import kotlinx.serialization.Serializable

/**
 * Ein abgeschlossener Session-Durchlauf im Logbuch. Wird lokal persistiert.
 *
 * @param kind Atemübung oder Meditation.
 * @param family Familie der Atemübung; bei [SessionKind.MEDITATION] `null`.
 * @param startedAtEpochMs Startzeit (Unix-ms) – für Datum/Uhrzeit und Sortierung.
 * @param durationMs Aktive Zeit ohne Pausen.
 * @param roundsCompleted Anzahl abgeschlossener Runden (bei Meditation 0).
 */
@Serializable
data class SessionLogEntry(
    val exerciseId: String,
    val exerciseName: String,
    val family: BreathingFamily?,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val roundsCompleted: Int,
    val kind: SessionKind = SessionKind.BREATHING,
)
