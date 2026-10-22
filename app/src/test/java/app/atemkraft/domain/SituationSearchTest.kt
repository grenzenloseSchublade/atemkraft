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
        assertEquals("nervoes", SituationSearch.normalize("nervös")) // zerlegtes ö
        assertEquals("cafe", SituationSearch.normalize("Café"))
        // Umlaut, Umschreibung und Groß- oder Kleinschreibung finden dasselbe.
        assertEquals(find("müde"), find("MUEDE"))
    }

    @Test
    fun `Wortanfang trifft, Teilwort, zu kurze Eingabe und Titel-Verlaengerung nicht`() {
        assertEquals(listOf(Situation.CRASH), find("müd"))
        assertEquals(listOf(Situation.FOCUS), find("konzentr"))
        // „karussell“ steckt in „Gedankenkarussell“ nur als Teilwort.
        assertEquals(emptyList<Situation>(), find("karussell"))
        listOf("", "zu").forEach { assertNull(it, find(it)) }
        // Titel („Fokus“) und kurze Begriffe („Uni“) werden nicht verlängert.
        assertEquals(emptyList<Situation>(), find("Fokusgruppe"))
        assertEquals(emptyList<Situation>(), find("Unikat"))
    }

    /** Die Warn-Situation (Wim Hof) erreicht man nicht über Befinden, nur über ihren Titel. */
    @Test
    fun `Warn-Situation nur ueber ihren Titel - Muedigkeit, Wachliegen und Vorbehalt fuehren nicht hin`() {
        listOf("bin müde und will wach werden", "ich liege nachts wach", "nicht gesund", "ausgeruht").forEach { q ->
            val hits = find(q)!!
            assertTrue("$q → $hits", warn.none { it.situation in hits })
        }
        assertEquals(Situation.SLEEP, find("ich liege nachts wach")!!.first())
        assertTrue(Situation.HIGH_PHASE in find("energiegeladen")!!)
    }

    @Test
    fun `Diagnosen und Symptome führen zu keinem Treffer`() {
        listOf(
            "Asthma", "Panikattacke", "Herzrasen", "Burnout", "Schwindel", "Atemnot",
            // Diagnose-, Medikamenten- und Puls-Wortteile verlängern keinen Begriff.
            "Angststörung", "Schlafapnoe", "Schlaftabletten", "Ruhepuls",
        ).forEach { assertEquals(it, emptyList<Situation>(), find(it)) }
    }

    @Test
    fun `gebeugte und zusammengesetzte Wörter treffen den Begriff, mit dem sie beginnen`() {
        mapOf(
            "Prüfungsstress" to Situation.FOCUS,
            "Flugangst" to Situation.ACUTE_STRESS,
            "Müdigkeit" to Situation.CRASH,
            "Nervosität" to Situation.BREATHLESSNESS,
            "Gedankenkreisen" to Situation.SLEEP,
        ).forEach { (q, s) -> assertEquals(q, listOf(s), find(q)) }
        // Wendungen aus dem Alltag.
        assertEquals(Situation.SLEEP, find("kann nicht schlafen")!!.first())
        assertEquals(Situation.CRASH, find("nach der Arbeit")!!.first())
    }

    @Test
    fun `Rangfolge - mehr Woerter zuerst, genauester Begriff gewinnt, sonst Datenreihenfolge`() {
        // Angst → Akuter Stress (1), Prüfung + lernen → Fokus (2); bei Gleichstand Datenreihenfolge.
        assertEquals(listOf(Situation.FOCUS, Situation.ACUTE_STRESS), find("Angst vor der Prüfung, muss lernen"))
        assertEquals(listOf(Situation.ACUTE_STRESS, Situation.FOCUS), find("Angst vor der Prüfung"))
        // „nachts“ fängt „Nachtschicht“ an, „Ruhe“ fängt „ruhelos“ an: Jeder landet allein bei
        // seiner Situation; nur als Wortanfang erscheinen beide.
        assertEquals(listOf(Situation.SLEEP), find("nachts"))
        assertEquals(listOf(Situation.CRASH), find("Nachtschicht"))
        assertEquals(listOf(Situation.SLEEP, Situation.CRASH), find("Nacht"))
        assertEquals(listOf(Situation.SLEEP), find("Ruhe"))
        assertEquals(listOf(Situation.BREATHLESSNESS), find("ruhelos"))
    }

    @Test
    fun `jeder Begriff führt als erster Treffer zu seiner eigenen Situation, nie zur Warn-Situation`() {
        val wrong = Situations.all.flatMap { s -> s.keywords.map { it to s.situation } }
            .filter { (k, s) -> find(k)?.firstOrNull() != s || warn.any { it.situation in find(k)!! } }
            .map { (k, s) -> "$k → ${find(k)} statt $s" }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `Begriffsdaten - keine Doppel, keine Praefixkonflikte, Warn ohne Begriffe, keine Diagnosen`() {
        val all = Situations.all.flatMap { s -> s.keywords.map { SituationSearch.normalize(it) to s.situation } }
        val doubles = all.groupBy({ it.first }, { it.second }).filterValues { it.size > 1 }
        assertEquals(emptyMap<String, List<Situation>>(), doubles)

        // Ausnahmen löst „der genaueste Begriff gewinnt“ (Rangfolge-Test).
        val exceptions = setOf("nachts" to "nachtschicht", "ruhe" to "ruhelos")
        val conflicts = all.flatMap { (a, sa) ->
            all.filter { (b, sb) -> sa != sb && b.startsWith(a) }.map { (b, _) -> a to b }
        }.filter { it !in exceptions }
        assertEquals(emptyList<Pair<String, String>>(), conflicts)

        warn.forEach { assertEquals(it.title, emptyList<String>(), it.keywords) }

        val diagnosis = Regex(
            "asthma|copd|depress|stoerung|krank|syndrom|schmerz|atemnot|herz|blutdruck|burnout|covid|attacke|tinnitus|migraene",
        )
        assertEquals(emptyList<String>(), all.map { it.first }.filter { diagnosis.containsMatchIn(it) })
    }
}
