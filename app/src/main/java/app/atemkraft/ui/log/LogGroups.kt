package app.atemkraft.ui.log

import app.atemkraft.domain.SessionLogEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Zeitraum einer Logbuch-Gruppe; bestimmt den Kopf und das Zeitformat ihrer Zeilen. */
sealed interface LogPeriod {
    data object Today : LogPeriod

    data object Yesterday : LogPeriod

    /** Die übrigen Tage der laufenden Kalenderwoche (ab Montag, ohne Heute und Gestern). */
    data object ThisWeek : LogPeriod

    /** Alles davor, je Kalendermonat. */
    data class Month(val month: YearMonth) : LogPeriod
}

/** Die Sitzungen eines Kalendertags, neueste zuerst. */
data class LogDay(val date: LocalDate, val entries: List<SessionLogEntry>)

/** Eine Gruppe im Verlauf, Tage neueste zuerst; [entries] alle Sitzungen der Gruppe in dieser Reihenfolge. */
data class LogGroup(val period: LogPeriod, val days: List<LogDay>) {
    val entries: List<SessionLogEntry> get() = days.flatMap { it.entries }
}

/**
 * Teilt den Verlauf in Heute / Gestern / Diese Woche / je Kalendermonat, neueste Gruppe zuerst.
 * Maßgeblich ist der Kalendertag in [zone], nicht der Abstand in Stunden: 00:00 ist schon heute.
 * „Diese Woche“ ist die Kalenderwoche ab Montag, kein rollendes Fenster – sonst stünde montags
 * der Mittwoch der Vorwoche als „Mi“ unter „Diese Woche“. Am Montag und Dienstag ist sie leer.
 * Einträge mit Datum nach [today] (Uhr verstellt) zählen zu Heute. Innerhalb einer Gruppe sind
 * die Sitzungen je Kalendertag gebündelt ([LogDay]).
 */
fun groupLogEntries(entries: List<SessionLogEntry>, today: LocalDate, zone: ZoneId): List<LogGroup> {
    val weekStart = today.with(DayOfWeek.MONDAY)
    // Absteigend sortiert liegen die Zeiträume schon in der richtigen Reihenfolge – groupBy
    // behält die Reihenfolge des ersten Auftretens.
    return entries
        .sortedByDescending { it.startedAtEpochMs }
        .groupBy { entry -> Instant.ofEpochMilli(entry.startedAtEpochMs).atZone(zone).toLocalDate() }
        .map { (day, list) -> LogDay(day, list) }
        .groupBy { (day, _) ->
            when {
                !day.isBefore(today) -> LogPeriod.Today
                day == today.minusDays(1) -> LogPeriod.Yesterday
                !day.isBefore(weekStart) -> LogPeriod.ThisWeek
                else -> LogPeriod.Month(YearMonth.from(day))
            }
        }
        .map { (period, days) -> LogGroup(period, days) }
}
