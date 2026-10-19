package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationAttributes
import android.os.VibrationEffect
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
 * Rasten: Primitive, wenn der Motor sie kann, sonst One-Shot; gedrosselt, ein Klick hat Vorrang.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HapticPlayerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private var now = 1_000L
    private val player = HapticPlayer(context) { now }

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
    fun `Raste des Dauer-Reglers folgt dem App-Schalter, je 5 Minuten deutlicher`() {
        player.tick(enabled = false, strong = true)
        assertFalse(vibrator().isVibrating)

        player.tick(enabled = true)
        val light = vibrator().milliseconds
        now += TICK_GAP_MS
        player.tick(enabled = true, strong = true)
        val shadow = vibrator()
        assertEquals(VibrationAttributes.USAGE_ALARM, usage(shadow))
        assertTrue("deutliche Raste ($light → ${shadow.milliseconds} ms)", shadow.milliseconds > light)
    }

    @Test
    fun `ohne Primitive spuerbarer One-Shot mit voller Amplitude`() {
        player.tick(enabled = true)
        assertEquals(TICK_ONE_SHOT_MS, vibrator().milliseconds)
        now += TICK_GAP_MS
        player.tick(enabled = true, strong = true)
        assertEquals(STRONG_ONE_SHOT_MS, vibrator().milliseconds)
    }

    @Test
    fun `mit Primitiven leiser Tick je Minute, satter Klick je 5 Minuten`() {
        vibrator().setSupportedPrimitives(listOf(VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_CLICK))
        val player = HapticPlayer(context) { now }
        player.tick(enabled = true)
        assertEquals(
            listOf(ShadowVibrator.PrimitiveEffect(VibrationEffect.Composition.PRIMITIVE_TICK, TICK_PRIMITIVE_SCALE, 0)),
            vibrator().primitiveSegmentsInPrimitiveEffects,
        )
        now += TICK_GAP_MS
        player.tick(enabled = true, strong = true)
        val shadow = vibrator()
        assertEquals(listOf(ShadowVibrator.PrimitiveEffect(VibrationEffect.Composition.PRIMITIVE_CLICK, STRONG_PRIMITIVE_SCALE, 0)), shadow.primitiveSegmentsInPrimitiveEffects)
        assertEquals(VibrationAttributes.USAGE_ALARM, usage(shadow))
    }

    @Test
    fun `dichte Ticks werden gedrosselt, ein Klick geht immer durch`() {
        val v = context.getSystemService(VibratorManager::class.java).defaultVibrator
        player.tick(enabled = true)
        assertTrue(vibrator().isVibrating)
        v.cancel()

        now += TICK_GAP_MS - 1
        player.tick(enabled = true) // zu dicht: bricht den laufenden Tick nicht ab
        assertFalse(vibrator().isVibrating)

        player.tick(enabled = true, strong = true) // Klick hat Vorrang
        assertTrue(vibrator().isVibrating)
        assertEquals(STRONG_ONE_SHOT_MS, vibrator().milliseconds)
        v.cancel()

        now += STRONG_GAP_MS - 1
        player.tick(enabled = true) // der Klick klingt noch aus
        assertFalse(vibrator().isVibrating)

        now += 1
        player.tick(enabled = true)
        assertTrue(vibrator().isVibrating)
        assertEquals(TICK_ONE_SHOT_MS, vibrator().milliseconds)
    }

    @Test
    fun `TickGate laesst Ticks erst nach der Pause durch, Klicks immer`() {
        val gate = TickGate()
        assertTrue(gate.admit(strong = false, now = 0))
        assertFalse(gate.admit(strong = false, now = TICK_GAP_MS - 1))
        assertTrue(gate.admit(strong = false, now = TICK_GAP_MS))
        assertTrue(gate.admit(strong = true, now = TICK_GAP_MS + 1))
        assertTrue(gate.admit(strong = true, now = TICK_GAP_MS + 2))
        assertFalse(gate.admit(strong = false, now = TICK_GAP_MS + 2 + STRONG_GAP_MS - 1))
        assertTrue(gate.admit(strong = false, now = TICK_GAP_MS + 2 + STRONG_GAP_MS))
    }

    @Test
    fun `ein Tick bei ausgeschaltetem App-Schalter sperrt die Drosselung nicht`() {
        player.tick(enabled = false)
        player.tick(enabled = true) // gleiche Zeit: der ausgeschaltete Tick hat nichts gesperrt
        assertTrue(vibrator().isVibrating)
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

    @Test
    @Config(sdk = [29])
    fun `API 29 nimmt vordefinierte Effekte, Klick je 5 Minuten schwer`() {
        @Suppress("DEPRECATION")
        val shadow = shadowOf(context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        player.tick(enabled = true)
        assertEquals(VibrationEffect.EFFECT_TICK, shadow.effectId)
        now += TICK_GAP_MS
        player.tick(enabled = true, strong = true)
        assertEquals(VibrationEffect.EFFECT_HEAVY_CLICK, shadow.effectId)
        assertEquals(AudioAttributes.USAGE_ALARM, shadow.audioAttributesFromLastVibration?.usage)
    }
}
