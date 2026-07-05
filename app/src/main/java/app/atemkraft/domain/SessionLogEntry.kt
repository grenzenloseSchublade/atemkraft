package app.atemkraft.domain

import kotlinx.serialization.Serializable

/**
 * Ein abgeschlossener Session-Durchlauf im Logbuch. Wird lokal persistiert.
 *
 * @param startedAtEpochMs Startzeit (Unix-ms) – für Datum/Uhrzeit und Sortierung.
 * @param durationMs Aktive Atemzeit ohne Pausen.
 * @param roundsCompleted Anzahl abgeschlossener Runden.
 */
@Serializable
data class SessionLogEntry(
    val exerciseId: String,
    val exerciseName: String,
    val family: BreathingFamily,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val roundsCompleted: Int,
)
