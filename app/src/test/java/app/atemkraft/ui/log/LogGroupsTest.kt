package app.atemkraft.ui.log

import app.atemkraft.domain.SessionLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Gruppierung des Logbuchs nach Kalendertagen: Grenzen um Mitternacht, Gestern, die Kante
 * zwischen Montag (Diese Woche) und Sonntag davor (Monat), Monats- und Jahreswechsel, Reihenfolge.
 */
class LogGroupsTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun entry(id: Long, at: LocalDateTime) = SessionLogEntry(
        id = id,
        exerciseId = "box",
        exerciseName = "Box Breathing",
        family = null,
        startedAtEpochMs = at.atZone(zone).toInstant().toEpochMilli(),
        durationMs = 60_000L,
        roundsCompleted = 1,
    )

    /** Zeitraum je Eintrags-Id, um Grenzfälle einzeln zu prüfen. */
    private fun periods(groups: List<LogGroup>): Map<Long, LogPeriod> = groups.flatMap { group -> group.entries.map { it.id to group.period } }.toMap()

    @Test
    fun `leerer Verlauf ergibt keine Gruppen`() {
        assertTrue(groupLogEntries(emptyList(), LocalDate.of(2026, 10, 19), zone).isEmpty())
    }

    @Test
    fun `Mitternacht trennt Heute und Gestern`() {
        val today = LocalDate.of(2026, 10, 22) // Donnerstag
        val groups = groupLogEntries(
            listOf(
                entry(1, today.atStartOfDay()),
                entry(2, today.atStartOfDay().minusSeconds(1)),
                entry(3, today.minusDays(1).atStartOfDay()),
                entry(4, today.minusDays(1).atStartOfDay().minusSeconds(1)),
            ),
            today,
            zone,
        )
        val byId = periods(groups)
        assertEquals(LogPeriod.Today, byId[1])
        assertEquals(LogPeriod.Yesterday, byId[2])
        assertEquals(LogPeriod.Yesterday, byId[3])
        assertEquals(LogPeriod.ThisWeek, byId[4])
    }

    @Test
    fun `Woche beginnt am Montag, der Sonntag davor gehoert zum Monat`() {
        val today = LocalDate.of(2026, 10, 22) // Donnerstag
        val groups = groupLogEntries(
            listOf(
                entry(1, LocalDate.of(2026, 10, 19).atStartOfDay()),
                entry(2, LocalDate.of(2026, 10, 18).atTime(23, 59, 59)),
            ),
            today,
            zone,
        )
        val byId = periods(groups)
        assertEquals(LogPeriod.ThisWeek, byId[1])
        assertEquals(LogPeriod.Month(YearMonth.of(2026, 10)), byId[2])
    }

    @Test
    fun `am Montag steht die Vorwoche nicht unter Diese Woche`() {
        val today = LocalDate.of(2026, 10, 19) // Montag
        val groups = groupLogEntries(
            listOf(
                entry(1, LocalDate.of(2026, 10, 18).atTime(8, 0)),
                entry(2, LocalDate.of(2026, 10, 14).atTime(7, 30)),
            ),
            today,
            zone,
        )
        assertEquals(listOf(LogPeriod.Yesterday, LogPeriod.Month(YearMonth.of(2026, 10))), groups.map { it.period })
    }

    @Test
    fun `Woche reicht ueber den Monatswechsel, danach Monatsgruppen`() {
        val today = LocalDate.of(2026, 10, 3) // Samstag, Woche ab Montag 28.09.
        val groups = groupLogEntries(
            listOf(
                entry(1, LocalDateTime.of(2026, 9, 30, 7, 30)),
                entry(2, LocalDateTime.of(2026, 9, 26, 7, 30)),
                entry(3, LocalDateTime.of(2026, 8, 31, 22, 0)),
                entry(4, LocalDateTime.of(2026, 9, 27, 7, 30)),
            ),
            today,
            zone,
        )
        assertEquals(
            listOf(
                LogPeriod.ThisWeek,
                LogPeriod.Month(YearMonth.of(2026, 9)),
                LogPeriod.Month(YearMonth.of(2026, 8)),
            ),
            groups.map { it.period },
        )
        // 30.09. liegt in dieser Woche; der 27.09. ist der Sonntag davor und steht im September.
        assertEquals(listOf(1L), groups[0].entries.map { it.id })
        assertEquals(listOf(4L, 2L), groups[1].entries.map { it.id })
    }

    @Test
    fun `Jahreswechsel ordnet Dezember vor November ein`() {
        val today = LocalDate.of(2027, 1, 2)
        val groups = groupLogEntries(
            listOf(
                entry(1, LocalDateTime.of(2026, 11, 30, 8, 0)),
                entry(2, LocalDateTime.of(2027, 1, 1, 23, 0)),
                entry(3, LocalDateTime.of(2026, 12, 20, 8, 0)),
                entry(4, LocalDateTime.of(2026, 12, 31, 8, 0)),
                entry(5, LocalDateTime.of(2027, 1, 2, 6, 0)),
            ),
            today,
            zone,
        )
        assertEquals(
            listOf(
                LogPeriod.Today,
                LogPeriod.Yesterday,
                LogPeriod.ThisWeek,
                LogPeriod.Month(YearMonth.of(2026, 12)),
                LogPeriod.Month(YearMonth.of(2026, 11)),
            ),
            groups.map { it.period },
        )
        assertEquals(listOf(5L, 2L, 4L, 3L, 1L), groups.flatMap { g -> g.entries.map { it.id } })
    }

    @Test
    fun `Eintraege einer Gruppe stehen neueste zuerst`() {
        val today = LocalDate.of(2026, 10, 19)
        val groups = groupLogEntries(
            listOf(
                entry(1, today.atTime(7, 30)),
                entry(2, today.atTime(21, 0)),
                entry(3, today.atTime(12, 15)),
            ),
            today,
            zone,
        )
        assertEquals(1, groups.size)
        assertEquals(listOf(2L, 3L, 1L), groups.single().entries.map { it.id })
    }

    @Test
    fun `Sitzungen eines Tages stehen in einem Tag der Gruppe`() {
        val today = LocalDate.of(2026, 10, 19)
        val groups = groupLogEntries(
            listOf(
                entry(1, LocalDate.of(2026, 9, 21).atTime(7, 30)),
                entry(2, LocalDate.of(2026, 9, 21).atTime(20, 0)),
                entry(3, LocalDate.of(2026, 9, 3).atTime(9, 0)),
            ),
            today,
            zone,
        )
        val days = groups.single().days
        assertEquals(listOf(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 3)), days.map { it.date })
        assertEquals(listOf(2L, 1L), days[0].entries.map { it.id })
    }

    @Test
    fun `Eintrag nach heute zaehlt zu Heute`() {
        val today = LocalDate.of(2026, 10, 19)
        val groups = groupLogEntries(listOf(entry(1, today.plusDays(1).atTime(8, 0))), today, zone)
        assertEquals(LogPeriod.Today, groups.single().period)
    }
}
