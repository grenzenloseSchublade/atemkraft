package app.atemkraft.playback

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import app.atemkraft.data.ReminderSettings
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Nächster Zeitpunkt der Erinnerung ab [now] (ausschließlich): der nächste gewählte Wochentag zur
 * gewählten Uhrzeit in der Zeitzone von [now]; `null`, wenn die Erinnerung aus ist. An einem Tag mit
 * Zeitumstellung gilt die Uhrzeit, die es an dem Tag gibt (`ZonedDateTime` verschiebt eine fehlende
 * Stunde nach vorn).
 */
internal fun nextReminderAt(settings: ReminderSettings, now: ZonedDateTime): ZonedDateTime? {
    if (!settings.enabled || settings.days.isEmpty()) return null
    val time = LocalTime.of(settings.minuteOfDay / 60, settings.minuteOfDay % 60)
    return (0L..7L)
        .map { now.toLocalDate().plusDays(it).atTime(time).atZone(now.zone) }
        .firstOrNull { it.isAfter(now) && it.dayOfWeek in settings.days }
}

/**
 * Ist [now] die Zeit für eine Erinnerung, die der Job gerade ausführt? Nur an einem gewählten Tag
 * und höchstens [ReminderScheduler.LATE_LIMIT] nach der Uhrzeit: Kommt der Job viel später (Gerät
 * lange im Tiefschlaf, Neustart), bleibt die Erinnerung lieber aus, statt zur Unzeit zu kommen.
 */
internal fun reminderDue(settings: ReminderSettings, now: ZonedDateTime): Boolean {
    if (!settings.enabled || now.dayOfWeek !in settings.days) return false
    val planned = now.toLocalDate().atTime(settings.minuteOfDay / 60, settings.minuteOfDay % 60).atZone(now.zone)
    val late = Duration.between(planned, now)
    return !late.isNegative && late <= ReminderScheduler.LATE_LIMIT
}

/**
 * Plant die lokale Erinnerung über den JobScheduler des Systems – ohne Netz, ohne neue Bibliothek,
 * ohne exakten Wecker (keine Berechtigung SCHEDULE_EXACT_ALARM): ein einzelner Job mit
 * Mindestwartezeit bis zur nächsten Uhrzeit und einem Fenster von [WINDOW]. Android darf ihn im
 * Tiefschlaf etwas schieben; das reicht für „Zeit für ein paar Atemzüge?“. Der Job übersteht einen
 * Neustart (`setPersisted`, dafür RECEIVE_BOOT_COMPLETED – ohne eigenen Receiver); nach dem Lauf
 * plant [ReminderJobService] den nächsten, beim App-Start plant der AppContainer neu.
 */
class ReminderScheduler(private val context: Context) {

    private val jobs get() = context.getSystemService(JobScheduler::class.java)

    /** Plant die nächste Erinnerung nach [settings] ab [now] neu oder hebt sie auf (aus). */
    fun plan(settings: ReminderSettings, now: ZonedDateTime = ZonedDateTime.now()) {
        val next = nextReminderAt(settings, now)
        if (next == null) {
            jobs?.cancel(JOB_ID)
            return
        }
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(0L)
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, ReminderJobService::class.java))
            .setMinimumLatency(delay)
            .setOverrideDeadline(delay + WINDOW.toMillis())
            .setPersisted(true)
            .build()
        jobs?.schedule(job)
    }

    companion object {
        /** Eine Erinnerung zur Zeit, also ein fester Job. */
        const val JOB_ID = 2_900

        /** Spielraum nach der Uhrzeit, in dem Android den Job ausführen soll. */
        val WINDOW: Duration = Duration.ofMinutes(15)

        /** Später als das zeigt der Job nichts mehr an (siehe [reminderDue]). */
        val LATE_LIMIT: Duration = Duration.ofHours(3)
    }
}
