package app.atemkraft.cue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat

/**
 * Meldet, wenn der Kopfhörer abgezogen wird oder Bluetooth trennt (`ACTION_AUDIO_BECOMING_NOISY`):
 * Ohne Eingriff liefen Ton, Gong und Stimme sonst über den Lautsprecher weiter. Der Empfänger ist
 * nur registriert, solange etwas klingen kann – eine laufende Übung, Meditation oder Editor-Vorschau –
 * und nie im Manifest (SEC-PLAT-01); er braucht kein Recht. Aufrufe und Rückruf auf dem Main-Thread.
 */
class UnplugWatcher(context: Context) {
    private val appContext = context.applicationContext
    private var onUnplugged: (() -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onUnplugged?.invoke()
        }
    }

    /** Ab jetzt [onUnplugged] melden (idempotent; ein neuer Aufruf ersetzt den Rückruf). */
    fun watch(onUnplugged: () -> Unit) {
        val registered = this.onUnplugged != null
        this.onUnplugged = onUnplugged
        if (registered) return
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /** Nicht mehr melden und den Empfänger abmelden (mehrfach aufrufbar). */
    fun stop() {
        if (onUnplugged == null) return
        onUnplugged = null
        runCatching { appContext.unregisterReceiver(receiver) }
    }
}
