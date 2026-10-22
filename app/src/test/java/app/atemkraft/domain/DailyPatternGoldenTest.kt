package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest
import java.time.LocalDate

/**
 * Friert das „Muster des Tages" ein: Die Werte stammen aus dem Generator von Version 1.6.8
 * (vor dem Konfigurator) und stehen hier als Literale. Ändert sich ein Wert, hat sich das
 * Tagesmuster für die Nutzer verschoben – das darf nur mit Absicht passieren und braucht dann
 * neue Literale samt Begründung im Commit.
 *
 * Damit ist auch der Determinismus innerhalb einer Version abgedeckt.
 */
class DailyPatternGoldenTest {

    /** Erwartetes Tagesmuster eines Datums; [phases] als „TYP Millis Notiz", durch „, " getrennt. */
    private data class Golden(
        val date: String,
        val spec: PatternSpec,
        val name: String,
        val repeat: Int,
        val phases: String,
    )

    private fun calm(inhale: Double, holdFull: Double?, exhale: Double, holdEmpty: Double?) = PatternSpec(inhale, holdFull, exhale, holdEmpty, activating = false)

    private fun active(inhale: Double, holdFull: Double?, exhale: Double) = PatternSpec(inhale, holdFull, exhale, null, activating = true)

    /** Ein Datum je Stil (ohne Halten, mit Halten, nach 4-7-8, sanfte Box, aktivierend), dazu Screenshot-Tag und Schalttag. */
    private val goldens = listOf(
        // ruhig ohne Halten (Neujahr)
        Golden("2026-01-01", calm(5.0, null, 8.0, null), "Tagesmuster 5·8", 23, "INHALE 5000, EXHALE 8000"),
        // nach 4-7-8
        Golden(
            "2026-01-02",
            calm(3.0, 2.0, 6.0, null),
            "Tagesmuster 3·2·6",
            27,
            "INHALE 3000, HOLD_FULL 2000 voll, EXHALE 6000",
        ),
        // ruhig mit Halten voll
        Golden(
            "2026-01-03",
            calm(3.0, 2.0, 4.0, null),
            "Tagesmuster 3·2·4",
            33,
            "INHALE 3000, HOLD_FULL 2000 voll, EXHALE 4000",
        ),
        // sanfte Box
        Golden(
            "2026-01-05",
            calm(4.0, 2.0, 5.0, 1.0),
            "Tagesmuster 4·2·5·1",
            25,
            "INHALE 4000, HOLD_FULL 2000 voll, EXHALE 5000, HOLD_EMPTY 1000 leer",
        ),
        // sanft aktivierend mit Halten
        Golden(
            "2026-01-09",
            active(4.0, 2.0, 2.0),
            "Tagesmuster 4·2·2",
            38,
            "INHALE 4000, HOLD_FULL 2000 voll, EXHALE 2000",
        ),
        // Screenshot-Tag (ScreenshotTest)
        Golden(
            "2026-10-03",
            calm(5.0, 1.0, 7.0, null),
            "Tagesmuster 5·1·7",
            23,
            "INHALE 5000, HOLD_FULL 1000 voll, EXHALE 7000",
        ),
        // Schalttag
        Golden("2028-02-29", calm(3.0, null, 6.0, null), "Tagesmuster 3·6", 33, "INHALE 3000, EXHALE 6000"),
    )

    @Test
    fun `Tagesmuster ausgewaehlter Daten bleiben unveraendert`() {
        goldens.forEach { golden ->
            val daily = RandomPatternGenerator.forDate(LocalDate.parse(golden.date))
            val segment = daily.exercise.segments.single()
            assertEquals("Spec ${golden.date}", golden.spec, daily.spec)
            assertEquals("Name ${golden.date}", golden.name, daily.exercise.name)
            assertEquals("repeat ${golden.date}", golden.repeat, segment.repeat)
            assertEquals("Phasen ${golden.date}", golden.phases, phasesOf(daily.exercise))
            assertEquals("Charakter ${golden.date}", golden.spec.activating, daily.exercise.activating)
        }
    }

    /**
     * Breite Abdeckung über Hashes: Specs von forSeed 0 bis 4999, alle Tage 2026 (Spec, Name,
     * repeat; forDate nutzt andere Seeds) und die Texte von forSeed 0 bis 199 (Herkunft TODAY).
     */
    @Test
    fun `Hashes von Seeds, Jahr 2026 und Texten bleiben unveraendert`() {
        assertEquals(
            "Specs forSeed 0..4999",
            "44f5e8b775a6a5c48a039a175a38ad7b57f92db5aff706a5c7d74349596aaa7b",
            specsHash((0L until 5000L).map { RandomPatternGenerator.forSeed(it) }),
        )
        assertEquals("Jahr 2026", "d5c0ae95467e7f394301194c39d9100c2b568a6fba3538b49839d99b0103c378", yearHash())
        assertEquals("Texte forSeed 0..199", "90f31fe1a27f428131a5641616c4068f8c313b3524e61b54aa6f7bc1334a5242", textHash())
    }

    private companion object {
        fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        /** Feste Textform eines Specs; Double.toString ist auf der JVM stabil („5.0", „null"). */
        fun canonical(spec: PatternSpec): String = "${spec.inhale}|${spec.holdFull}|${spec.exhale}|${spec.holdEmpty}|${spec.activating}"

        fun phasesOf(exercise: Exercise): String = exercise.segments.single().phases.joinToString(", ") { phase ->
            val millis = (phase.duration as PhaseDuration.Fixed).millis
            listOfNotNull(phase.type.name, millis.toString(), phase.note).joinToString(" ")
        }

        fun specsHash(patterns: List<DailyPattern>): String = sha256(patterns.joinToString("\n") { canonical(it.spec) })

        /** Alle 365 Tage 2026: Spec, Name und repeat. */
        fun yearHash(): String = sha256(
            (0L until 365L).joinToString("\n") { offset ->
                val daily = RandomPatternGenerator.forDate(LocalDate.of(2026, 1, 1).plusDays(offset))
                "${canonical(daily.spec)}|${daily.exercise.name}|${daily.exercise.segments.single().repeat}"
            },
        )

        /** Kurzbeschreibung, Wirkung, Detail, Anleitung und Atemlehre-Einordnung, durch NUL getrennt. */
        fun textHash(): String = sha256(
            (0L until 200L).joinToString("\n") { seed ->
                val exercise = RandomPatternGenerator.forSeed(seed).exercise
                val lore = exercise.lore
                listOf(
                    exercise.shortDescription,
                    exercise.effect,
                    exercise.effectDetail,
                    exercise.instructionHint,
                    lore?.teaser,
                    lore?.detail,
                    lore?.patternId,
                    lore?.source,
                    lore?.morePatternIds?.joinToString(","),
                ).joinToString("\u0000")
            },
        )
    }
}
