package app.atemkraft.playback

import android.app.Activity
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import app.atemkraft.R
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.formatClock
import app.atemkraft.domain.supervisedScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Was der [PlaybackService] von der App braucht: die beiden Abläufe und die Activity, die ein Tipp
 * auf die Benachrichtigung öffnet. Die Application implementiert es; so kennt `playback/` weder die
 * Application- noch die Activity-Klasse und hängt nicht am Wurzelpaket (A-10).
 */
interface PlaybackDependencies {
    val meditationController: MeditationController
    val breathSessionController: BreathSessionController
    val launchActivity: Class<out Activity>
}

/**
 * Foreground-Service (`mediaPlayback`), der eine laufende Meditation oder Atem-Session am Leben
 * hält, damit Gong, Sprache und Phasen-Töne auch bei ausgeschaltetem Bildschirm und nach der
 * Home-Taste pünktlich kommen (Doze/App-Standby; Android 17 stummt Hintergrund-Audio ohne FGS).
 * Ein Service für beide Abläufe: Geht eine Atemübung nahtlos in die Stille über, bleibt er einfach
 * im Vordergrund – ein zweiter Start aus dem Hintergrund wäre ab Android 12 verboten.
 *
 * Die Abläufe selbst liegen im [MeditationController] und im [BreathSessionController]; dieser
 * Service zeigt nur die ruhige Dauer-Benachrichtigung des laufenden Ablaufs (es läuft höchstens
 * einer, ein neuer Start ersetzt den alten; im Moment des Übergangs hat die Meditation Vorrang) und
 * hält die CPU wach. Wann er läuft, regeln die Halter über
 * [PlaybackServiceLinks].
 */
class PlaybackService : Service() {

    private val scope = supervisedScope(Dispatchers.Main.immediate)

    private val dependencies get() = application as PlaybackDependencies
    private val meditation get() = dependencies.meditationController
    private val breath get() = dependencies.breathSessionController

    /**
     * Partial-Wakelock: hält NUR die CPU wach (Bildschirm darf aus), solange eine Sitzung aktiv
     * läuft. Ohne ihn pausiert der delay()-Timer-Loop in tiefem Doze und Gongs bzw. Phasen-Töne
     * kämen erheblich zu spät – gerade beim Kern-Use-Case „stille Sitzung, Bildschirm aus".
     * Bei Pause wird er freigegeben (Timer steht ohnehin), bei Resume neu gehalten.
     */
    private var wakeLock: PowerManager.WakeLock? = null

    /** Verlängert den Wakelock einer langen Sitzung (∞-Meditation), siehe [WAKELOCK_RENEW_MS]. */
    private var wakeLockRenewal: Job? = null

    private fun updateWakeLock(shouldHold: Boolean) {
        val held = wakeLock?.isHeld == true
        if (shouldHold && !held) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "atemkraft:session").apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_TIMEOUT_MS)
            }
            wakeLock = lock
            // Erneutes acquire(timeout) setzt beim nicht gezählten Wakelock nur die Frist neu.
            // Schluss nach WAKELOCK_MAX_MS: Hinge eine Sitzung fest, liefe er nicht ewig.
            wakeLockRenewal?.cancel()
            wakeLockRenewal = scope.launch {
                repeat(((WAKELOCK_MAX_MS - WAKELOCK_TIMEOUT_MS) / WAKELOCK_RENEW_MS).toInt()) {
                    delay(WAKELOCK_RENEW_MS)
                    if (lock.isHeld) lock.acquire(WAKELOCK_TIMEOUT_MS)
                }
            }
        } else if (!shouldHold && held) {
            wakeLockRenewal?.cancel()
            wakeLockRenewal = null
            wakeLock?.release()
            wakeLock = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        createChannel()
        // Benachrichtigung bei jeder sichtbaren Änderung aktualisieren – aber NICHT, wenn nichts
        // mehr läuft (IDLE/FINISHED), sonst würde beim Beenden noch „0:00" nachgepostet und bliebe
        // hängen. Das eigentliche Beenden steuern die Controller (über die Halter); onDestroy räumt
        // die Benachrichtigung sicher weg.
        scope.launch {
            combine(meditation.state, breath.state) { m, b -> shown(m, b) to holdsCpu(m, b) }
                .distinctUntilChanged()
                .collect { (shown, wake) ->
                    updateWakeLock(wake)
                    if (shown != null) notificationManager().notify(NOTIF_ID, buildNotification(shown))
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            when {
                // Bei aktiver Sitzung übernimmt der CONTROLLER den Teardown (bei der freien
                // Meditation erst NACH dem End-Gong und dem Logbuch – sofortiges stopSelf() würde
                // den FGS-Schutz hinter dem Sperrbildschirm mitten im Gong entziehen).
                meditation.state.value.status.isLive() -> meditation.end()

                breath.state.value.status.isLive() -> breath.stop()

                // Nur eine bereits verwaiste Benachrichtigung räumen wir hier direkt weg.
                else -> removeNotificationAndStop()
            }
            return START_NOT_STICKY
        }
        // Muss innerhalb weniger Sekunden nach startForegroundService geschehen – auch wenn die
        // Sitzung schon wieder beendet ist (dann gleich danach wieder weg).
        // FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK ist ein inlined const (API 29+-Wert);
        // ServiceCompat ignoriert den Typ auf API < 29 – der Lint-Hinweis ist gegenstandslos.
        val shown = shown(meditation.state.value, breath.state.value)
        @Suppress("InlinedApi")
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(shown ?: Shown(getString(R.string.app_name), null, R.drawable.ic_breath_mark_filled)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (shown == null) removeNotificationAndStop()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Foreground-Bindung lösen UND die per notify() aktualisierte Notification explizit
        // löschen – sonst bleibt sie (v. a. auf Samsung) als Waise hängen.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager().cancel(NOTIF_ID)
        wakeLockRenewal?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        scope.cancel()
        running = false
        super.onDestroy()
    }

    private fun removeNotificationAndStop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager().cancel(NOTIF_ID)
        stopSelf()
    }

    /** Inhalt der Benachrichtigung; null, wenn nichts läuft (dann wird nichts gepostet). */
    private data class Shown(val title: String, val body: String?, @param:DrawableRes val icon: Int)

    private fun shown(m: MeditationUiState, b: SessionUiState): Shown? = when {
        m.status.isLive() -> Shown(getString(R.string.meditation_notification_title), meditationBody(m), R.drawable.ic_meditation)
        b.status.isLive() -> Shown(getString(R.string.session_notification_title), breathBody(b), R.drawable.ic_breath_mark_filled)
        else -> null
    }

    /** CPU wach halten, solange eine der Sitzungen zählt (nicht in der Pause, nicht am Ende). */
    private fun holdsCpu(m: MeditationUiState, b: SessionUiState): Boolean = m.status == MeditationStatus.RUNNING || m.status == MeditationStatus.PREPARING ||
        b.status == SessionStatus.PREPARING || b.status == SessionStatus.RUNNING || b.status == SessionStatus.WAITING_FOR_USER

    private fun meditationBody(state: MeditationUiState): String {
        val base = when {
            state.status == MeditationStatus.PREPARING -> getString(R.string.session_get_ready)

            state.mode == MeditationMode.TIMED ->
                getString(R.string.meditation_notification_remaining, formatClock(state.remainingMs))

            else -> formatClock(state.elapsedMs)
        }
        return if (state.status == MeditationStatus.PAUSED) base + DOT_SEPARATOR + getString(R.string.session_paused) else base
    }

    /**
     * Atem-Session: Runde statt Phase – Phasen wechseln bis zu zweimal je Sekunde (Feueratmung),
     * so oft darf eine Benachrichtigung nicht neu kommen. Der Name der Übung steht bewusst nicht
     * darin: Sie ist auch auf dem Sperrbildschirm zu sehen (docs/PRIVACY.md).
     */
    private fun breathBody(state: SessionUiState): String? {
        val base = when {
            state.status == SessionStatus.PREPARING -> getString(R.string.session_get_ready)
            state.roundCount > 1 -> getString(R.string.session_round, state.roundIndex + 1, state.roundCount)
            else -> null
        }
        if (state.status != SessionStatus.PAUSED) return base
        val paused = getString(R.string.session_paused)
        return if (base == null) paused else base + DOT_SEPARATOR + paused
    }

    private fun buildNotification(shown: Shown): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, dependencies.launchActivity)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(shown.icon)
            .setContentTitle(shown.title)
            .setContentText(shown.body)
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
            getString(R.string.session_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.session_notification_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        /**
         * Kanal-Kennung aus der Zeit, als nur die Meditation den Service nutzte: bleibt, damit
         * Einstellungen, die jemand am Kanal vorgenommen hat, erhalten bleiben.
         */
        private const val CHANNEL_ID = "meditation"
        internal const val NOTIF_ID = 42
        private const val ACTION_STOP = "app.atemkraft.action.SESSION_STOP"

        /**
         * Sicherheits-Timeout des Wakelocks: Der Timer läuft höchstens 60 min
         * (MEDITATION_MAX_MINUTES), dazu reichlich Reserve. Läuft die Sitzung noch (die offene
         * Sitzung ∞ hat kein Ende), verlängert der Service ihn alle [WAKELOCK_RENEW_MS] um eine
         * neue Frist, insgesamt bis [WAKELOCK_MAX_MS]; danach läuft er still aus, und
         * Intervall-Gongs können im Doze verspätet kommen.
         */
        private const val WAKELOCK_TIMEOUT_MS = 3 * 60 * 60 * 1000L
        private const val WAKELOCK_RENEW_MS = 60 * 60 * 1000L
        private const val WAKELOCK_MAX_MS = 12 * 60 * 60 * 1000L

        /**
         * Läuft der Service gerade (zwischen onCreate und onDestroy)? Die Halter in
         * [PlaybackServiceLinks] allein wissen das nicht: Der Service kann sich selbst beenden
         * („Stopp“ in der Benachrichtigung am Ende-Screen) oder vom System beendet werden.
         */
        @Volatile
        internal var running = false
            private set

        fun startIntent(context: Context): Intent = Intent(context, PlaybackService::class.java)
    }
}

private fun MeditationStatus.isLive() = this != MeditationStatus.IDLE && this != MeditationStatus.FINISHED

private fun SessionStatus.isLive() = this != SessionStatus.IDLE && this != SessionStatus.FINISHED

/**
 * Startet und beendet den [PlaybackService] für beide Abläufe. Jeder Ablauf bekommt mit [link] einen
 * eigenen Halter; der Service läuft, solange mindestens einer ihn hält. So übernimmt die Meditation
 * nach „Danach in die Stille“ den laufenden Service der Atemübung, ohne ihn neu zu starten (das ginge
 * aus dem Hintergrund ab Android 12 nicht). Beim Beenden räumt er die Benachrichtigung zusätzlich
 * app-seitig weg: Der Service tut das in onDestroy zwar selbst, aber dessen Teardown-Timing ist
 * herstellerabhängig (v. a. Samsung) – so bleibt keine Waise zurück. Nur vom Main-Thread nutzen.
 *
 * Ob der Service wirklich läuft, sagt [PlaybackService.running], nicht die Halter-Menge: Hat er sich
 * selbst beendet, während ein Halter eingetragen blieb, startet der nächste Start aus der sichtbaren
 * App ihn neu, statt die Sitzung ohne Foreground-Service laufen zu lassen.
 */
class PlaybackServiceLinks(context: Context) {
    private val appContext = context.applicationContext
    private val holders = mutableSetOf<String>()

    /** Halter für einen Ablauf ([owner] = stabile Kennung, z. B. „meditation“). */
    fun link(owner: String): ForegroundLink = object : ForegroundLink {
        override fun start() {
            holders.add(owner)
            // Aus der sichtbaren App immer (idempotent: der laufende Service zeigt nur neu an). Aus dem
            // Hintergrund nur, wenn er nicht schon läuft – sonst übernimmt der Ablauf ihn einfach.
            if (PlaybackService.running && !appVisible()) return
            try {
                ContextCompat.startForegroundService(appContext, PlaybackService.startIntent(appContext))
            } catch (_: IllegalStateException) {
                // Start aus dem Hintergrund verboten (ab Android 12): Die Sitzung läuft dann ohne
                // Service weiter, statt die App zu beenden. Die Aufrufer prüfen vorher [canStartNow].
            }
        }

        override fun stop() {
            if (!holders.remove(owner) || holders.isNotEmpty()) return
            appContext.stopService(PlaybackService.startIntent(appContext))
            appContext.getSystemService<NotificationManager>()?.cancel(PlaybackService.NOTIF_ID)
        }
    }

    /**
     * Darf jetzt eine Sitzung mit Foreground-Service beginnen? Ja, wenn der Service schon läuft (er
     * bleibt einfach im Vordergrund) oder die App sichtbar ist; sonst verbietet Android ab 12 den
     * Start aus dem Hintergrund.
     */
    fun canStartNow(): Boolean = (holders.isNotEmpty() && PlaybackService.running) || appVisible()

    private fun appVisible(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
}
