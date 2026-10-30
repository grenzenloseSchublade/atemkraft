package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest
import java.time.LocalDate

/**
 * Friert das „Muster des Tages“ ein: Die Hashes stammen aus dem Generator von Version 1.6.8
 * (vor dem Konfigurator). Ändert sich einer, hat sich das Tagesmuster für die Nutzer verschoben –
 * das darf nur mit Absicht passieren und braucht dann neue Hashes samt Begründung im Commit.
 * Damit ist auch der Determinismus innerhalb einer Version abgedeckt.
 */
class DailyPatternGoldenTest {

    /**
     * Specs von forSeed 0 bis 4999, alle Tage 2026 (Spec, Name, repeat; forDate nutzt andere Seeds)
     * und die Texte von forSeed 0 bis 199 (Herkunft TODAY; zuletzt geändert 2026-10-24: Teaser `effect` unter 95 Zeichen, TEXT-06 ohne Baseline).
     */
    @Test
    fun `Specs, Namen und Texte der Tagesmuster bleiben unveraendert`() {
        assertEquals(
            "Specs forSeed 0..4999",
            "44f5e8b775a6a5c48a039a175a38ad7b57f92db5aff706a5c7d74349596aaa7b",
            specsHash((0L until 5000L).map { RandomPatternGenerator.forSeed(it) }),
        )
        assertEquals("Jahr 2026", "d5c0ae95467e7f394301194c39d9100c2b568a6fba3538b49839d99b0103c378", yearHash())
        assertEquals("Texte forSeed 0..199", "c553660932b90e1b36026e7420387ab7c6c6a09d7f44904ebbc4e6dc3e4360d0", textHash())
    }

    /**
     * Safety: Jedes Tagesmuster hält die Erzeugungsregeln ein und ist nie intensiv – weder als
     * Übung ([Exercise.caution], Sicherheitsdialog) noch nach den Schwellen der freien Folge, mit
     * denen „Selbst gestalten“ es im Editor öffnet ([GeneratedExercises.freePatternOf]). Die
     * Hashes oben frieren nur ein, was erzeugt wird; das hier sichert zu, dass es harmlos ist.
     */
    @Test
    fun `jedes Tagesmuster haelt die Erzeugungsregeln ein und ist nie intensiv`() {
        val days = (0L until 365L).map { RandomPatternGenerator.forEpochDay(LocalDate.of(2026, 1, 1).plusDays(it).toEpochDay()) }
        val problems = ((0L until 5000L).map { RandomPatternGenerator.forSeed(it) } + days).mapNotNull { daily ->
            val free = GeneratedExercises.freePatternOf(daily.exercise)
            when {
                !daily.spec.withinCreationRules() -> "${canonical(daily.spec)}: außerhalb der Erzeugungsregeln"
                daily.exercise.caution -> "${canonical(daily.spec)}: caution gesetzt"
                free == null -> "${canonical(daily.spec)}: nicht als freie Folge zu öffnen"
                free.isIntense -> "${canonical(daily.spec)}: intensiv (${free.intensityReasons()})"
                else -> null
            }
        }.distinct()
        assertEquals("", problems.joinToString("\n"))
    }

    private companion object {
        fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        /** Feste Textform eines Specs; Double.toString ist auf der JVM stabil („5.0“, „null“). */
        fun canonical(spec: PatternSpec): String = "${spec.inhale}|${spec.holdFull}|${spec.exhale}|${spec.holdEmpty}|${spec.activating}"

        fun specsHash(patterns: List<DailyPattern>): String = sha256(patterns.joinToString("\n") { canonical(it.spec) })

        /** Alle 365 Tage 2026: Spec, Name und repeat. */
        fun yearHash(): String = sha256(
            (0L until 365L).joinToString("\n") { offset ->
                val daily = RandomPatternGenerator.forEpochDay(LocalDate.of(2026, 1, 1).plusDays(offset).toEpochDay())
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
