package app.atemkraft.ui.session

import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.RuntimePhase
import app.atemkraft.domain.buildTimeline
import app.atemkraft.playback.SessionStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * MOTION-03: Der Atemkreis springt an Phasengrenzen nicht – die Skala am Ende einer Phase
 * (inklusive Start-Countdown) ist die Skala am Anfang der nächsten. Geprüft wird über
 * [circleFraction] mit denselben Zustandswerten, die der SessionViewModel setzt: feste Phase
 * beginnt mit `remainingMs = phaseTotalMs` und endet mit `remainingMs = 0`, offene Phase hat
 * `phaseTotalMs = 0`.
 *
 * Bekannte Sprünge stehen mit Backlog-ID in [knownJumps]. Der Test schlägt fehl bei einem
 * neuen Sprung und bei einem Eintrag, der nicht mehr auftritt (dann Backlog und Liste kürzen).
 */
class FractionContinuityTest {

    /** Übergang `von → nach` (Phasentypen; `PREPARE` = Start-Countdown) → Backlog-ID. */
    private val knownJumps = mapOf(
        "PREPARE → INHALE" to "S-11", // Countdown 0,5 → Einatmen beginnt bei 0
        "PREPARE → EXHALE" to "S-11", // Feueratmung beginnt mit Ausatmen (1)
        "INHALE → INHALE_TOP_UP" to "S-11", // Seufzer: zweiter Atemzug beginnt wieder bei 0
        "INHALE → REST" to "S-11", // Feueratmung: Ruhe 0,5
        "REST → EXHALE" to "S-11", // Feueratmung: nach der Ruhe wieder Ausatmen ab 1
        "EXHALE → EXHALE" to "S-11", // Wim Hof: letztes Ausatmen vor dem Halten startet bei 1
        "HOLD_FULL → INHALE" to "S-11", // Wim Hof: nach dem Erholungsatemzug neue Runde ab 0
    )

    private val exercises: List<Exercise> =
        BuiltInExercises.all + (0L..6L).map { RandomPatternGenerator.forSeed(it).exercise }

    private fun stateAt(phase: RuntimePhase, atEnd: Boolean): SessionUiState {
        val total = (phase.duration as? PhaseDuration.Fixed)?.millis ?: 0L
        return SessionUiState(
            status = if (phase.duration is PhaseDuration.Fixed) SessionStatus.RUNNING else SessionStatus.WAITING_FOR_USER,
            phaseType = phase.type,
            phaseLevel = phase.level,
            phaseTotalMs = total,
            remainingMs = if (atEnd) 0L else total,
        )
    }

    private val prepareState = SessionUiState(status = SessionStatus.PREPARING, countdown = 1)

    /** Alle Sprünge als Übergangsschlüssel → betroffene Übungen (mit Skalenwerten). */
    private fun findJumps(): Map<String, List<String>> {
        val jumps = linkedMapOf<String, MutableList<String>>()
        for (exercise in exercises) {
            val timeline = exercise.buildTimeline()
            if (timeline.isEmpty()) continue
            val boundaries = buildList {
                add(Triple("PREPARE", circleFraction(prepareState), timeline.first()))
                timeline.zipWithNext().forEach { (a, b) ->
                    add(Triple(a.type.name, circleFraction(stateAt(a, atEnd = true)), b))
                }
            }
            boundaries.forEach { (from, endScale, next) ->
                val startScale = circleFraction(stateAt(next, atEnd = false))
                if (abs(endScale - startScale) > TOLERANCE) {
                    val key = "$from → ${next.type.name}"
                    val entry = "${exercise.id} ($endScale → $startScale)"
                    jumps.getOrPut(key) { mutableListOf() }.let { if (entry !in it) it += entry }
                }
            }
        }
        return jumps
    }

    @Test
    fun `keine neuen Skalensprünge an Phasengrenzen`() {
        val unknown = findJumps().filterKeys { it !in knownJumps }
        assertTrue(
            "Neue Skalensprünge (MOTION-03): " +
                unknown.entries.joinToString("; ") { (k, v) -> "$k in ${v.joinToString()}" },
            unknown.isEmpty(),
        )
    }

    @Test
    fun `jeder bekannte Sprung tritt noch auf`() {
        val stale = knownJumps.keys - findJumps().keys
        assertTrue(
            "Behoben – aus knownJumps und Backlog streichen: ${stale.joinToString()}",
            stale.isEmpty(),
        )
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }

    /**
     * Viloma (Atmen in Stufen): Der Kreis läuft an keiner Grenze zurück, auch nicht von Runde zu
     * Runde, und steht in den Pausen bei ⅓ bzw. ⅔. Unabhängig von [knownJumps], die für Wim Hof
     * dieselben Übergangstypen erlauben.
     */
    @Test
    fun `Viloma waechst und schrumpft stufenweise ohne Sprung`() {
        listOf("viloma-ein", "viloma-aus").forEach { id ->
            val timeline = BuiltInExercises.all.first { it.id == id }.buildTimeline()
            timeline.zipWithNext().forEach { (a, b) ->
                val end = circleFraction(stateAt(a, atEnd = true))
                val start = circleFraction(stateAt(b, atEnd = false))
                assertTrue("$id: ${a.type} → ${b.type} springt ($end → $start)", abs(end - start) <= TOLERANCE)
            }
            val pauses = timeline.filter { it.level != null && it.level!!.from == it.level!!.to }
            assertTrue("$id: Pausen mit festem Füllstand", pauses.isNotEmpty())
            pauses.forEach { p ->
                val atStart = circleFraction(stateAt(p, atEnd = false))
                val atEnd = circleFraction(stateAt(p, atEnd = true))
                assertTrue("$id: Kreis steht in der Pause ($atStart → $atEnd)", abs(atStart - atEnd) <= TOLERANCE)
                assertTrue("$id: Pause zwischen leer und voll ($atStart)", atStart > 0.2f && atStart < 0.8f)
            }
        }
    }
}
