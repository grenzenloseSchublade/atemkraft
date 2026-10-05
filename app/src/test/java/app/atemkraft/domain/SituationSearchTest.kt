package app.atemkraft.domain

import app.atemkraft.data.Situations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Befindens-Suche (MUSTER-09): Normalisierung, Wortanfang statt Teilwort, kuratierte Begriffe
 * ohne Doppel und ohne Diagnosen, Leitplanken für die Warn-Situation – gegen die echten Daten.
 */
class SituationSearchTest {

    private fun find(query: String): List<Situation>? = SituationSearch.search(query, Situations.all)?.map { it.situation }

    private val warn = Situations.all.filter { it.warn }

    @Test
    fun `Normalisierung macht klein, löst Umlaute und ß auf und entfernt Satzzeichen`() {
        assertEquals("muede gruebeln kopf voll", SituationSearch.normalize("Müde, GRÜBELN – Kopf voll!"))
        assertEquals("strasse", SituationSearch.normalize("Straße"))
        assertEquals("nervoes", SituationSearch.normalize("nervös")) // zerlegtes ö
        assertEquals("cafe", SituationSearch.normalize("Café"))
    }

    @Test
    fun `Umlaut, Umschreibung und Groß- oder Kleinschreibung finden dasselbe`() {
        val expected = listOf(Situation.CRASH)
        listOf("müde", "Müde", "MUEDE", "muede").forEach { assertEquals(it, expected, find(it)) }
        assertEquals(find("Prüfung"), find("pruefung"))
    }

    @Test
    fun `Wortanfang ab drei Zeichen trifft`() {
        assertEquals(listOf(Situation.CRASH), find("müd"))
        assertEquals(listOf(Situation.SLEEP), find("Gedanken"))
        assertEquals(listOf(Situation.FOCUS), find("konzentr"))
    }

    @Test
    fun `Teilwort mitten im Wort trifft nicht`() {
        // „karussell“ steckt in „Gedankenkarussell“, „schlafen“ in „einschlafen“ als Endung.
        assertEquals(emptyList<Situation>(), find("karussell"))
        assertEquals(emptyList<Situation>(), find("aufen"))
        assertEquals(emptyList<Situation>(), find("ruefung"))
    }

    @Test
    fun `ohne Suchwort ist die Suche noch nicht aktiv`() {
        listOf("", "  ", "m", "zu", "ich bin", "wie geht’s").forEach { assertNull(it, find(it)) }
    }

    @Test
    fun `müde, erschöpft und schlapp führen zu Erschöpft und nie zur Warn-Situation`() {
        listOf("müde", "erschöpft", "schlapp", "ausgelaugt", "kaputt").forEach {
            assertEquals(it, listOf(Situation.CRASH), find(it))
        }
        // Auch wenn jemand „wach werden“ möchte: Müdigkeit ist kuratiert, die Warn-Situation
        // tritt zurück („wach“ trifft weiter den Satz von Konzentration & Fokus, Gleichstand
        // nach Datenreihenfolge).
        val hits = find("bin müde und will wach werden")!!
        assertTrue(hits.toString(), Situation.CRASH in hits && warn.none { it.situation in hits })
    }

    @Test
    fun `wer nachts wach liegt, landet beim Einschlafen und nicht bei der Warn-Situation`() {
        listOf(
            "kann nachts wach liegen",
            "ich liege nachts wach",
            "ich liege wach",
            "liege wach",
            "bin wach und kann nicht schlafen",
        ).forEach { q ->
            val hits = find(q)!!
            assertEquals(q, Situation.SLEEP, hits.first())
            assertTrue("$q → $hits", warn.none { it.situation in hits })
        }
    }

    @Test
    fun `die Warn-Situation trifft über ihren Titel, solange nichts Kuratiertes passt`() {
        assertTrue(Situation.HIGH_PHASE in find("energiegeladen")!!)
        assertTrue(Situation.HIGH_PHASE in find("wach werden")!!)
    }

    @Test
    fun `der Vorbehalt im Satz der Warn-Situation führt nicht zu ihr`() {
        // „Nur üben, wenn du dich stabil und gesund fühlst“: Wer „nicht gesund“ tippt, darf
        // nicht bei Wim Hof landen – Warn-Situationen treffen nur über ihren Titel.
        listOf("nicht gesund", "gesund", "fühle mich nicht stabil", "kräftig", "Reiz", "aktiviert").forEach { q ->
            val hits = find(q)!!
            assertTrue("$q → $hits", warn.none { it.situation in hits })
        }
    }

    @Test
    fun `Diagnosen und Symptome führen zu keinem Treffer`() {
        listOf(
            "Asthma", "COPD", "Depression", "Angststörung", "Panikstörung", "Bluthochdruck", "Brustschmerz",
            "Brustschmerzen", "Atemnot", "Schlafstörung", "Herzrasen", "Long Covid", "Burnout",
        ).forEach { assertEquals(it, emptyList<Situation>(), find(it)) }
    }

    @Test
    fun `mehr getroffene Wörter stehen zuerst, sonst gilt die Reihenfolge der Daten`() {
        // Angst → Akuter Stress (1), Prüfung + lernen → Fokus (2).
        assertEquals(listOf(Situation.FOCUS, Situation.ACUTE_STRESS), find("Angst vor der Prüfung, muss lernen"))
        // Gleichstand: Datenreihenfolge (Akuter Stress steht vor Fokus).
        assertEquals(listOf(Situation.ACUTE_STRESS, Situation.FOCUS), find("Angst vor der Prüfung"))
    }

    @Test
    fun `Beispiele aus dem Alltag finden ihre Situation`() {
        mapOf(
            "nervös" to Situation.BREATHLESSNESS,
            "Prüfung" to Situation.FOCUS,
            "Gedankenkarussell" to Situation.SLEEP,
            "kann nicht schlafen" to Situation.SLEEP,
            "Kopf voll" to Situation.SLEEP,
            "gestresst" to Situation.ACUTE_STRESS,
            "nach der Arbeit" to Situation.CRASH,
        ).forEach { (q, s) -> assertEquals(q, s, find(q)!!.first()) }
    }

    @Test
    fun `kein Begriff steht in zwei Situationen`() {
        val all = Situations.all.flatMap { s -> s.keywords.map { SituationSearch.normalize(it) to s.situation } }
        val doubles = all.groupBy({ it.first }, { it.second }).filterValues { it.size > 1 }
        assertEquals(emptyMap<String, List<Situation>>(), doubles)
    }

    @Test
    fun `Warn-Situationen haben keine einladenden Begriffe`() {
        warn.forEach { assertEquals(it.title, emptyList<String>(), it.keywords) }
    }

    @Test
    fun `Begriffe nennen keine Diagnosen oder Symptome`() {
        val diagnosis = Regex(
            "asthma|copd|depress|stoerung|krank|syndrom|schmerz|atemnot|herz|blutdruck|burnout|covid|attacke|tinnitus|migraene",
        )
        val found = Situations.all.flatMap { it.keywords }.filter { diagnosis.containsMatchIn(SituationSearch.normalize(it)) }
        assertEquals(emptyList<String>(), found)
    }
}
