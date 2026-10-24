package app.atemkraft.playback

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.atemkraft.R
import app.atemkraft.data.ReminderSettings
import app.atemkraft.domain.supervisedScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Was der [ReminderJobService] von der App braucht; die Application liefert es (wie [PlaybackDependencies]). */
interface ReminderDependencies {
    val reminderSettings: Flow<ReminderSettings>
    val launchActivity: Class<out Activity>
}

/**
 * Führt die geplante Erinnerung aus ([ReminderScheduler]): liest die Einstellung, zeigt – wenn es
 * noch die Zeit dafür ist ([reminderDue]) – eine schlichte Benachrichtigung „Zeit für ein paar
 * Atemzüge?“ auf eigenem Kanal und plant die nächste. Tippen öffnet die App; kein Zähler, keine
 * Serie, kein Übungsname (auch auf dem Sperrbildschirm, SEC-PRIV-03). Ohne Erlaubnis für
 * Benachrichtigungen bleibt sie aus, geplant wird trotzdem weiter. Nicht exportiert: Nur der
 * JobScheduler des Systems bindet ihn (`BIND_JOB_SERVICE`).
 */
class ReminderJobService : JobService() {

    private val scope = supervisedScope(Dispatchers.Default)

    private val dependencies get() = application as ReminderDependencies

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            try {
                val settings = dependencies.reminderSettings.first()
                val now = ZonedDateTime.now()
                if (reminderDue(settings, now)) notifyReminder()
                ReminderScheduler(this@ReminderJobService).plan(settings, now)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notifyReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.reminder_channel_description)
                setShowBadge(false)
            },
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, dependencies.launchActivity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_breath_mark_filled)
            .setContentTitle(getString(R.string.reminder_title))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Erlaubnis zwischen Prüfung und Anzeige entzogen: dann eben keine Erinnerung.
        }
    }

    private companion object {
        const val CHANNEL_ID = "reminder"
        const val NOTIFICATION_ID = 2_901
    }
}
