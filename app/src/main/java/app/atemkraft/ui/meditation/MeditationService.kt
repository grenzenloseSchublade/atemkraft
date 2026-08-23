package app.atemkraft.ui.meditation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.atemkraft.AtemkraftApplication
import app.atemkraft.MainActivity
import app.atemkraft.R
import app.atemkraft.domain.MeditationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Foreground-Service (`mediaPlayback`), der eine laufende Meditation am Leben hält, damit
 * Gong/Sprache auch bei ausgeschaltetem Bildschirm zuverlässig kommen (Doze/App-Standby).
 * Der eigentliche Ablauf liegt im [MeditationController]; dieser Service zeigt nur die
 * ruhige Dauer-Notification und beendet sich selbst, sobald die Sitzung endet.
 */
class MeditationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val controller get() =
        (application as AtemkraftApplication).container.meditationController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // Notification bei jeder Sekundenänderung aktualisieren – aber NICHT in Endzuständen
        // (IDLE/FINISHED), sonst würde beim Beenden noch „0:00" nachgepostet und bliebe hängen.
        // Das eigentliche Beenden steuert der Controller (stopService); onDestroy räumt die
        // Notification sicher weg.
        scope.launch {
            controller.state
                .map { it.status to notificationBody(it) }
                .distinctUntilChanged()
                .collect { (status, body) ->
                    if (status != MeditationStatus.IDLE && status != MeditationStatus.FINISHED) {
                        notificationManager().notify(NOTIF_ID, buildNotification(body))
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            controller.end()
            // Auch eine bereits verwaiste Notification sicher entfernen und den Service beenden.
            removeNotificationAndStop()
            return START_NOT_STICKY
        }
        // Muss innerhalb weniger Sekunden nach startForegroundService geschehen.
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(notificationBody(controller.state.value)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Foreground-Bindung lösen UND die per notify() aktualisierte Notification explizit
        // löschen – sonst bleibt sie (v. a. auf Samsung) als Waise hängen.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager().cancel(NOTIF_ID)
        scope.cancel()
        super.onDestroy()
    }

    private fun removeNotificationAndStop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager().cancel(NOTIF_ID)
        stopSelf()
    }

    private fun notificationBody(state: MeditationUiState): String {
        val paused = state.status == MeditationStatus.PAUSED
        val base = when {
            state.status == MeditationStatus.PREPARING -> getString(R.string.session_get_ready)
            state.mode == MeditationMode.TIMED ->
                getString(R.string.meditation_notification_remaining, formatTime(state.remainingMs))
            else -> formatTime(state.elapsedMs)
        }
        return if (paused) "$base · ${getString(R.string.session_paused)}" else base
    }

    private fun buildNotification(body: String): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MeditationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_meditation)
            .setContentTitle(getString(R.string.meditation_notification_title))
            .setContentText(body)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.meditation_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "meditation"
        private const val NOTIF_ID = 42
        private const val ACTION_STOP = "app.atemkraft.action.MEDITATION_STOP"

        fun startIntent(context: Context): Intent =
            Intent(context, MeditationService::class.java)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
