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
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

/**
 * AUDIO-05: Jede Session-Vibration folgt dem App-Schalter „Vibration“ – auch der Tipp-Tick am
 * Kreis. Phasen-Haptik und Tipp-Tick laufen als Alarm-Vibration (nicht abhängig von der System-
 * Tippvibration); ab API 33 über `VibrationAttributes`, davor über die `AudioAttributes`-Usage.
 * Die Rasten des Zeitrings ([HapticPlayer.dialTick]) sind Bedien-Rückmeldung: ohne App-Schalter,
 * als Berührungs-Vibration, die Android regelt.
 * Rasten: Primitive, wenn der Motor sie kann, sonst One-Shot; gedrosselt ([TickGate]), ein Klick hat Vorrang.
 */
@RunWith(RobolectricTestRunner::class)
// Feste Reihenfolge: Die Tests auf aktuellem SDK laufen vor denen auf API 28/29 („vor API 33 …“).
// Lädt zuerst ein älteres SDK die native Laufzeit von Robolectric, scheitert sie danach im
// aktuellen SDK (FileSystemAlreadyExistsException beim Kopieren der Schriften) – und mit ihr jeder
// folgende Robolectric-Test derselben JVM.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
    fun `App-Schalter aus - weder Tipp-Tick noch Phasen-Haptik, Drosselung bleibt frei`() {
        player.tick(enabled = false)
        CueEvent.entries.forEach { player.play(it, enabled = false) }
        assertFalse(vibrator().isVibrating)

        player.tick(enabled = true) // gleiche Zeit: der ausgeschaltete Tick hat nichts gesperrt
        assertTrue(vibrator().isVibrating)
    }

    @Test
    fun `App-Schalter an - Tipp-Tick und Phasen-Haptik vibrieren als Alarm`() {
        player.tick(enabled = true)
        assertEquals(VibrationAttributes.USAGE_ALARM, usage(vibrator()))
        now += TICK_GAP_MS
        player.play(CueEvent.INHALE, enabled = true)
        val shadow = vibrator()
        assertTrue(shadow.isVibrating)
        assertEquals(VibrationAttributes.USAGE_ALARM, usage(shadow))
    }

    @Test
    fun `Raste des Dauer-Reglers ist Beruehrungs-Vibration ohne App-Schalter, je 5 Minuten deutlicher`() {
        player.dialTick(strong = false)
        val light = vibrator().milliseconds
        assertEquals(VibrationAttributes.USAGE_TOUCH, usage(vibrator()))
        now += TICK_GAP_MS
        player.dialTick(strong = true)
        val shadow = vibrator()
        assertEquals(VibrationAttributes.USAGE_TOUCH, usage(shadow))
        assertTrue("deutliche Raste ($light → ${shadow.milliseconds} ms)", shadow.milliseconds > light)
    }

    @Test
    fun `mit Primitiven leiser Tick je Minute, satter Klick je 5 Minuten`() {
        vibrator().setSupportedPrimitives(listOf(VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_CLICK))
        val player = HapticPlayer(context) { now }
        player.tick(enabled = true)
        assertEquals(listOf(VibrationEffect.Composition.PRIMITIVE_TICK), vibrator().primitiveSegmentsInPrimitiveEffects.map { it.id })
        now += TICK_GAP_MS
        player.dialTick(strong = true)
        val shadow = vibrator()
        assertEquals(listOf(VibrationEffect.Composition.PRIMITIVE_CLICK), shadow.primitiveSegmentsInPrimitiveEffects.map { it.id })
        assertEquals(VibrationAttributes.USAGE_TOUCH, usage(shadow))
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
    @Config(sdk = [28])
    fun `vor API 33 - gelten dieselben Regeln über AudioAttributes`() {
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
    fun `vor API 33 - API 29 nimmt vordefinierte Effekte, Klick je 5 Minuten schwer`() {
        @Suppress("DEPRECATION")
        val shadow = shadowOf(context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        player.tick(enabled = true)
        assertEquals(VibrationEffect.EFFECT_TICK, shadow.effectId)
        now += TICK_GAP_MS
        player.dialTick(strong = true)
        assertEquals(VibrationEffect.EFFECT_HEAVY_CLICK, shadow.effectId)
        assertEquals(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, shadow.audioAttributesFromLastVibration?.usage)
    }
}
