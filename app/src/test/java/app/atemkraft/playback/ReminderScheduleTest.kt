package app.atemkraft.playback

import app.atemkraft.data.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Planung der lokalen Erinnerung: nächster gewählter Tag zur Uhrzeit, aus = kein Termin, über die
 * Zeitumstellung hinweg dieselbe Uhrzeit, und ein viel zu später Job zeigt nichts mehr an.
 */
class ReminderScheduleTest {

    private val berlin = ZoneId.of("Europe/Berlin")

    private fun at(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(berlin)

    @Test
    fun `nächster Termin, Zeitumstellung und Verspätung`() {
        // Mo–Fr um 7:30; Freitag 2026-10-23, 8:00 Uhr.
        val weekdays = ReminderSettings(enabled = true, minuteOfDay = 7 * 60 + 30, days = DayOfWeek.entries.take(5).toSet())
        assertNull(nextReminderAt(weekdays.copy(enabled = false), at("2026-10-23T08:00")))
        // Heute schon vorbei, Wochenende nicht gewählt: Montag.
        assertEquals(at("2026-10-26T07:30"), nextReminderAt(weekdays, at("2026-10-23T08:00")))
        // Vor der Uhrzeit: noch heute.
        assertEquals(at("2026-10-23T07:30"), nextReminderAt(weekdays, at("2026-10-23T07:00")))
        // Über die Umstellung auf Winterzeit (So 2026-10-25) bleibt es 7:30 Ortszeit.
        val daily = weekdays.copy(days = DayOfWeek.entries.toSet())
        val sunday = nextReminderAt(daily, at("2026-10-24T08:00"))!!
        assertEquals(at("2026-10-25T07:30"), sunday)
        assertEquals(7, sunday.hour)

        // Fällig nur am gewählten Tag und höchstens drei Stunden nach der Uhrzeit.
        assertTrue(reminderDue(weekdays, at("2026-10-23T07:40")))
        assertFalse(reminderDue(weekdays, at("2026-10-23T07:20")))
        assertFalse(reminderDue(weekdays, at("2026-10-23T11:00")))
        assertFalse(reminderDue(weekdays, at("2026-10-24T07:40")))
    }
}
