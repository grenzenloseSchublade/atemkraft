package app.atemkraft.ui.meditation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
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

    /**
     * Partial-Wakelock: hält NUR die CPU wach (Bildschirm darf aus), solange die Sitzung aktiv
     * läuft. Ohne ihn pausiert der delay()-Timer-Loop in tiefem Doze und End-/Intervall-Gongs
     * kämen erheblich zu spät – gerade beim Kern-Use-Case „stille Sitzung, Bildschirm aus".
     * Bei Pause wird er freigegeben (Timer steht ohnehin), bei Resume neu gehalten.
     */
    private var wakeLock: PowerManager.WakeLock? = null

    private fun updateWakeLock(status: MeditationStatus) {
        val shouldHold = status == MeditationStatus.RUNNING || status == MeditationStatus.PREPARING
        val held = wakeLock?.isHeld == true
        if (shouldHold && !held) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "atemkraft:meditation").apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_TIMEOUT_MS)
            }
        } else if (!shouldHold && held) {
            wakeLock?.release()
            wakeLock = null
        }
    }

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
                    updateWakeLock(status)
                    if (status != MeditationStatus.IDLE && status != MeditationStatus.FINISHED) {
                        notificationManager().notify(NOTIF_ID, buildNotification(body))
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val statusBefore = controller.state.value.status
            controller.end()
            // Bei aktiver Sitzung übernimmt der CONTROLLER den Teardown (bei FREE erst NACH dem
            // End-Gong + Logbuch-Schreiben – sofortiges stopSelf() würde den FGS-Schutz hinter dem
            // Sperrbildschirm mitten im Gong entziehen). Nur eine bereits verwaiste Notification
            // (IDLE/FINISHED) räumen wir hier direkt weg.
            if (statusBefore == MeditationStatus.IDLE || statusBefore == MeditationStatus.FINISHED) {
                removeNotificationAndStop()
            }
            return START_NOT_STICKY
        }
        // Muss innerhalb weniger Sekunden nach startForegroundService geschehen.
        // FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK ist ein inlined const (API 29+-Wert);
        // ServiceCompat ignoriert den Typ auf API < 29 – der Lint-Hinweis ist gegenstandslos.
        @Suppress("InlinedApi")
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
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
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
                getString(R.string.meditation_notification_remaining, formatMeditationTime(state.remainingMs))
            else -> formatMeditationTime(state.elapsedMs)
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
        internal const val NOTIF_ID = 42
        private const val ACTION_STOP = "app.atemkraft.action.MEDITATION_STOP"

        /** Sicherheits-Timeout des Wakelocks (längste Sitzung 90 min + Reserve). */
        private const val WAKELOCK_TIMEOUT_MS = 3 * 60 * 60 * 1000L

        fun startIntent(context: Context): Intent =
            Intent(context, MeditationService::class.java)
    }
}

