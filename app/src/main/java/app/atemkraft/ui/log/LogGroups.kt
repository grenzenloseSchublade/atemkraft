package app.atemkraft.ui.log

import app.atemkraft.domain.SessionLogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Zeitraum einer Logbuch-Gruppe; bestimmt den Kopf und das Zeitformat ihrer Zeilen. */
sealed interface LogPeriod {
    data object Today : LogPeriod

    data object Yesterday : LogPeriod

    /** Die übrigen der letzten 7 Tage ([isRecentDay], ohne Heute und Gestern). */
    data object LastSevenDays : LogPeriod

    /** Alles davor, je Kalendermonat. */
    data class Month(val month: YearMonth) : LogPeriod
}

/** Die Sitzungen eines Kalendertags, neueste zuerst. */
data class LogDay(val date: LocalDate, val entries: List<SessionLogEntry>)

/** Eine Gruppe im Verlauf, Tage neueste zuerst; [entries] alle Sitzungen der Gruppe in dieser Reihenfolge. */
data class LogGroup(val period: LogPeriod, val days: List<LogDay>) {
    val entries: List<SessionLogEntry> get() = days.flatMap { it.entries }
}

/** Tage der Übersicht: heute und die sechs davor – die Wochenpunkte, die Minuten daneben und die Gruppe im Verlauf. */
const val RECENT_DAYS = 7

/**
 * Gehört [day] zu den letzten 7 Tagen bis [today] (beide eingeschlossen)? Die eine Definition für
 * Wochenpunkte, „45 min in den letzten 7 Tagen“ und die Gruppe „Letzte 7 Tage“: Die Punkte zeigen
 * rollend die sieben Tage bis heute, also zählen Minuten und Gruppe dieselben Tage. Früher zählten
 * die Minuten rollend und die Gruppe „Diese Woche“ ab Montag – zweierlei unter einem Wort.
 */
fun isRecentDay(day: LocalDate, today: LocalDate): Boolean = !day.isAfter(today) && day.isAfter(today.minusDays(RECENT_DAYS.toLong()))

/** Kalendertag des Sitzungsbeginns in [zone]. */
private fun SessionLogEntry.day(zone: ZoneId): LocalDate = Instant.ofEpochMilli(startedAtEpochMs).atZone(zone).toLocalDate()

/**
 * Geübte Minuten der letzten 7 Tage ([isRecentDay]): erst die Millisekunden aller Sitzungen
 * summiert, dann einmal abgerundet – je Tag abgerundet fehlten bis zu 6 min.
 */
fun recentMinutes(entries: List<SessionLogEntry>, today: LocalDate, zone: ZoneId): Long = entries.filter { isRecentDay(it.day(zone), today) }.sumOf { it.durationMs } / 60_000L

/**
 * Teilt den Verlauf in Heute / Gestern / Letzte 7 Tage / je Kalendermonat, neueste Gruppe zuerst.
 * Maßgeblich ist der Kalendertag in [zone], nicht der Abstand in Stunden: 00:00 ist schon heute.
 * „Letzte 7 Tage“ sind dieselben Tage wie die Wochenpunkte ([isRecentDay]) ohne Heute und Gestern;
 * die Tageszeile („Mi 15.10.“) nennt das Datum, ein Mittwoch der Vorwoche ist so eindeutig.
 * Einträge mit Datum nach [today] (Uhr verstellt) zählen zu Heute. Innerhalb einer Gruppe sind
 * die Sitzungen je Kalendertag gebündelt ([LogDay]).
 */
fun groupLogEntries(entries: List<SessionLogEntry>, today: LocalDate, zone: ZoneId): List<LogGroup> {
    // Absteigend sortiert liegen die Zeiträume schon in der richtigen Reihenfolge – groupBy
    // behält die Reihenfolge des ersten Auftretens.
    return entries
        .sortedByDescending { it.startedAtEpochMs }
        .groupBy { entry -> entry.day(zone) }
        .map { (day, list) -> LogDay(day, list) }
        .groupBy { (day, _) ->
            when {
                !day.isBefore(today) -> LogPeriod.Today
                day == today.minusDays(1) -> LogPeriod.Yesterday
                isRecentDay(day, today) -> LogPeriod.LastSevenDays
                else -> LogPeriod.Month(YearMonth.from(day))
            }
        }
        .map { (period, days) -> LogGroup(period, days) }
}
