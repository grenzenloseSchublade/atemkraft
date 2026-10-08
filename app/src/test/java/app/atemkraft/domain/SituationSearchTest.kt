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
        // „Nur üben, wenn du dich gesund und ausgeruht fühlst“: Wer „nicht gesund“ oder „nicht
        // ausgeruht“ tippt, darf nicht bei Wim Hof landen – Warn-Situationen treffen nur über
        // ihren Titel.
        listOf(
            "nicht gesund",
            "gesund",
            "nicht ausgeruht",
            "ausgeruht",
            "fühle mich nicht stabil",
            "kräftig",
            "Reiz",
            "aktiviert",
        ).forEach { q ->
            val hits = find(q)!!
            assertTrue("$q → $hits", warn.none { it.situation in hits })
        }
    }

    @Test
    fun `Diagnosen und Symptome führen zu keinem Treffer`() {
        listOf(
            "Asthma", "COPD", "Depression", "Angststörung", "Panikstörung", "Bluthochdruck", "Brustschmerz",
            "Brustschmerzen", "Atemnot", "Schlafstörung", "Herzrasen", "Long Covid", "Burnout",
            "Panik", "Panikattacke", "Schwindel", "Herzklopfen", "Engegefühl", "Kribbeln",
        ).forEach { assertEquals(it, emptyList<Situation>(), find(it)) }
    }

    @Test
    fun `Diagnose-Wortteile verlängern keinen Begriff`() {
        // „Angst“, „Schlaf“, „Stress“ und „Erschöpfung“ sind Begriffe – ihre Diagnose-Komposita nicht.
        listOf("Angststörung", "Angstattacke", "Schlafapnoe", "Schlafstörungen", "Stressasthma", "Erschöpfungssyndrom")
            .forEach { assertEquals(it, emptyList<Situation>(), find(it)) }
    }

    @Test
    fun `Puls, Medikamente, Verletzungen und Zustände verlängern keinen Begriff`() {
        // „Ruhe“, „Schlaf“, „Sport“, „Arbeit“ und „Angst“ sind Begriffe – diese Komposita
        // gehören zu Ärztin oder Apotheke, nicht zu einer Atemübung.
        listOf(
            "Ruhepuls",
            "Schlaftabletten",
            "Schlafmittel",
            "Sportverletzung",
            "Sportunfall",
            "Arbeitsunfall",
            "Angstzustände",
            "Erschöpfungszustand",
        ).forEach { assertEquals(it, emptyList<Situation>(), find(it)) }
    }

    @Test
    fun `gebeugte und zusammengesetzte Wörter treffen den Begriff, mit dem sie beginnen`() {
        mapOf(
            "Ruhe" to Situation.SLEEP,
            "Entspannung" to Situation.SLEEP,
            "Prüfungen" to Situation.FOCUS,
            "Prüfungsstress" to Situation.FOCUS,
            "gestresste" to Situation.ACUTE_STRESS,
            "Flugangst" to Situation.ACUTE_STRESS,
            "Flugzeug" to Situation.ACUTE_STRESS,
            "Nachtschicht" to Situation.CRASH,
            "Bühnenangst" to Situation.BREATHLESSNESS,
            "Müdigkeit" to Situation.CRASH,
            "Ängste" to Situation.ACUTE_STRESS,
            "ängstlich" to Situation.ACUTE_STRESS,
            "Anspannung" to Situation.ACUTE_STRESS,
            "Überforderung" to Situation.ACUTE_STRESS,
            "Nervosität" to Situation.BREATHLESSNESS,
            "ruhelos" to Situation.BREATHLESSNESS,
            "Gedankenkreisen" to Situation.SLEEP,
            "Schichtarbeit" to Situation.CRASH,
        ).forEach { (q, s) -> assertEquals(q, listOf(s), find(q)) }
    }

    @Test
    fun `Titel und Begründungssätze werden nicht verlängert, kurze Begriffe auch nicht`() {
        // „Ruhig“ steht in mehreren Begründungssätzen, „Fokus“ im Titel, „Uni“ ist ein Begriff
        // mit nur drei Buchstaben.
        assertEquals(emptyList<Situation>(), find("ruhiges"))
        assertEquals(emptyList<Situation>(), find("Fokusgruppe"))
        assertEquals(emptyList<Situation>(), find("Unikat"))
    }

    @Test
    fun `der genaueste Begriff gewinnt, wenn einer den anderen anfängt`() {
        // „nachts“ (Einschlafen) ist der Anfang von „Nachtschicht“ (Erschöpft): Jeder Begriff
        // landet trotzdem allein bei seiner Situation.
        assertEquals(listOf(Situation.SLEEP), find("nachts"))
        assertEquals(listOf(Situation.CRASH), find("Nachtschicht"))
        assertEquals(listOf(Situation.CRASH), find("Nachtschichten"))
        // Beide sind nur Wortanfang: Beide Situationen erscheinen, in Datenreihenfolge.
        assertEquals(listOf(Situation.SLEEP, Situation.CRASH), find("Nacht"))
        // „Ruhe“ (Einschlafen) fängt „ruhelos“ (Kurzatmig oder aufgeregt) an: Unruhe ist kein
        // Wunsch nach Schlaf.
        assertEquals(listOf(Situation.SLEEP), find("Ruhe"))
        assertEquals(listOf(Situation.BREATHLESSNESS), find("ruhelos"))
    }

    @Test
    fun `jeder Begriff führt als erster Treffer zu seiner eigenen Situation`() {
        val wrong = Situations.all.flatMap { s -> s.keywords.map { it to s.situation } }
            .filter { (k, s) -> find(k)?.firstOrNull() != s }
            .map { (k, s) -> "$k → ${find(k)} statt $s" }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `kein Begriff fängt einen Begriff einer anderen Situation an, außer den belegten Ausnahmen`() {
        // Ausnahmen löst „der genaueste Begriff gewinnt“; jede steht im Test darüber.
        val exceptions = setOf("nachts" to "nachtschicht", "ruhe" to "ruhelos")
        val all = Situations.all.flatMap { s -> s.keywords.map { SituationSearch.normalize(it) to s.situation } }
        val conflicts = all.flatMap { (a, sa) ->
            all.filter { (b, sb) -> sa != sb && b.startsWith(a) }.map { (b, _) -> a to b }
        }.filter { it !in exceptions }
        assertEquals(emptyList<Pair<String, String>>(), conflicts)
    }

    @Test
    fun `die Warn-Situation erscheint nie über einen Begriff`() {
        val hits = Situations.all.flatMap { it.keywords }.filter { k -> warn.any { it.situation in find(k)!! } }
        assertEquals(emptyList<String>(), hits)
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
