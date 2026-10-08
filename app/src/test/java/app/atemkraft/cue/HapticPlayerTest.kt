package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

/**
 * AUDIO-05: Jede Vibration folgt dem App-Schalter „Vibration“ – auch der Tipp-Tick am Kreis.
 * Phasen-Haptik und Tipp-Tick laufen als Alarm-Vibration (nicht abhängig von der System-
 * Tippvibration); ab API 33 über `VibrationAttributes`, davor über die `AudioAttributes`-Usage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HapticPlayerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val player = HapticPlayer(context)

    private fun vibrator(): ShadowVibrator {
        val manager = context.getSystemService(VibratorManager::class.java)
        return shadowOf(manager.defaultVibrator)
    }

    private fun usage(shadow: ShadowVibrator): Int = (shadow.vibrationAttributesFromLastVibration as VibrationAttributes).usage

    @Test
    fun `Tipp-Tick vibriert nicht, wenn der App-Schalter aus ist`() {
        player.tick(enabled = false)
        assertFalse(vibrator().isVibrating)
    }

    @Test
    fun `Tipp-Tick vibriert als Alarm, wenn der App-Schalter an ist`() {
        player.tick(enabled = true)
        val shadow = vibrator()
        assertTrue(shadow.isVibrating)
        assertEquals(VibrationAttributes.USAGE_ALARM, usage(shadow))
    }

    @Test
    fun `Phasen-Haptik vibriert nicht, wenn der App-Schalter aus ist`() {
        CueEvent.entries.forEach { player.play(it, enabled = false) }
        assertFalse(vibrator().isVibrating)
    }

    @Test
    fun `Phasen-Haptik vibriert als Alarm, wenn der App-Schalter an ist`() {
        player.play(CueEvent.INHALE, enabled = true)
        val shadow = vibrator()
        assertTrue(shadow.isVibrating)
        assertEquals(VibrationAttributes.USAGE_ALARM, usage(shadow))
    }

    @Test
    @Config(sdk = [28])
    fun `vor API 33 gelten dieselben Regeln über AudioAttributes`() {
        @Suppress("DEPRECATION")
        val shadow = shadowOf(context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        player.tick(enabled = false)
        player.play(CueEvent.HOLD, enabled = false)
        assertFalse(shadow.isVibrating)

        player.tick(enabled = true)
        assertEquals(AudioAttributes.USAGE_ALARM, shadow.audioAttributesFromLastVibration?.usage)
        player.play(CueEvent.HOLD, enabled = true)
        assertEquals(AudioAttributes.USAGE_ALARM, shadow.audioAttributesFromLastVibration?.usage)
    }
}
