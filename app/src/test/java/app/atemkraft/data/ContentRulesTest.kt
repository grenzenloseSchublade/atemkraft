package app.atemkraft.data

import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.Reference
import java.io.File
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Inhaltsregeln aus STYLEGUIDE §11 (TEXT-02, -04 … -06, -09 … -12), MUSTER-03 und MOTION-02,
 * geprüft über alle Fachinhalte: eingebaute Übungen, Muster des Tages (Seeds 0–6),
 * Situationen, Quellen und `strings.xml`.
 *
 * Bekannte Verstöße stehen in `config/content-baseline.txt` (`Testfall | Datensatz | S-nn`).
 * Ein Fall schlägt fehl bei einem neuen Verstoß und bei einem Baseline-Eintrag, der nicht mehr
 * zutrifft (dann Eintrag streichen, META-01). Fälle mit Stufe SOLL geben ihre Funde nur aus.
 */
class ContentRulesTest {

    // ---- Datensätze ----------------------------------------------------------------------

    /** Ein Text mit stabilem Namen für Baseline und Meldung, z. B. `buteyko.effect`. */
    private data class Text(val id: String, val value: String)

    private val exercises: List<Exercise> =
        BuiltInExercises.all + (0L..6L).map { RandomPatternGenerator.forSeed(it).exercise }

    /** Name eines Übungs-Datensatzes; die generierten Muster teilen sich eine Id, daher mit Seed. */
    private fun Exercise.key(): String {
        val i = exercises.indexOf(this)
        return if (id == RandomPatternGenerator.ID) "$id#seed${i - BuiltInExercises.all.size}" else id
    }

    private fun Exercise.texts(): List<Text> = buildList {
        add(Text("${key()}.name", name))
        add(Text("${key()}.shortDescription", shortDescription))
        add(Text("${key()}.effect", effect))
        effectDetail?.let { add(Text("${key()}.effectDetail", it)) }
        instructionHint?.let { add(Text("${key()}.instructionHint", it)) }
        cautions.forEachIndexed { i, c -> add(Text("${key()}.cautions[$i]", c)) }
        segments.flatMap { it.phases }.forEachIndexed { i, p ->
            p.label?.let { add(Text("${key()}.phase[$i].label", it)) }
            p.note?.let { add(Text("${key()}.phase[$i].note", it)) }
        }
    }

    private val exerciseTexts: List<Text> by lazy { exercises.flatMap { it.texts() }.distinct() }

    private val situationTexts: List<Text> by lazy {
        Situations.all.flatMap {
            listOf(
                Text("situation.${it.situation}.title", it.title),
                Text("situation.${it.situation}.rationale", it.rationale),
            )
        }
    }

    /** Alle Quellen aus [Refs] mit ihrem Property-Namen (per Reflexion, damit keine fehlt). */
    private val refs: Map<String, Reference> by lazy {
        Refs::class.java.declaredFields
            .filter { it.type == Reference::class.java }
            .associate { f ->
                f.isAccessible = true
                f.name to f.get(Refs) as Reference
            }
    }

    /** Werte aus `strings.xml` (Strings und Plural-Items), XML-Escapes aufgelöst. */
    private val strings: List<Text> by lazy {
        val xml = moduleFile("src/main/res/values/strings.xml").readText()
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val single = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { Text("strings:${it.groupValues[1]}", unescape(it.groupValues[2])) }
        val plurals = Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).flatMap { p ->
                Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(p.groupValues[2])
                    .map { Text("strings:${p.groupValues[1]}.${it.groupValues[1]}", unescape(it.groupValues[2])) }
            }
        (single + plurals).toList()
    }

    /** Rohwerte aus `strings.xml`, für Zeichen, die erst durch die Escapes entstehen. */
    private val rawStrings: List<Text> by lazy {
        val xml = moduleFile("src/main/res/values/strings.xml").readText()
        Regex("""<(?:string|item) (?:name|quantity)="([^"]+)"[^>]*>(.*?)</(?:string|item)>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).map { Text("strings:${it.groupValues[1]}", it.groupValues[2]) }.toList()
    }

    private fun unescape(s: String) = s
        .replace("\\n", "\n").replace("\\'", "'").replace("\\\"", "\"")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace(Regex("%(\\d+\\$)?[sd]"), "0")

    private val contentTexts get() = exerciseTexts + situationTexts
    private val allTexts get() = contentTexts + strings

    // ---- Baseline -----------------------------------------------------------------------

    /** Ein Fund: Datensatz plus lesbare Begründung (die Begründung zählt nicht für die Baseline). */
    private data class Finding(val dataset: String, val why: String)

    private val baseline: Map<String, Set<String>> by lazy {
        repoFile("config/content-baseline.txt").readLines()
            .map { it.substringBefore("#").trim() }
            .filter { it.isNotEmpty() }
            .map { line -> line.split("|").map(String::trim) }
            .onEach { require(it.size == 3 && it[2].matches(Regex("S-\\d+"))) { "Baseline-Zeile kaputt: $it" } }
            .groupBy({ it[0] }, { it[1] })
            .mapValues { it.value.toSet() }
    }

    /**
     * Die Muster des Tages teilen sich ihre Texte; ein Fund, der in jedem Seed gleich auftritt,
     * zählt einmal als `muster-des-tages.<Feld>`.
     */
    private fun List<Finding>.collapseSeeds(): List<Finding> =
        map { it.copy(dataset = it.dataset.replace(Regex("#seed\\d+"), "")) }.distinctBy { it.dataset to it.why }

    /** MUSS-Fall: neue Funde und veraltete Baseline-Einträge lassen den Test scheitern. */
    private fun assertAgainstBaseline(case: String, allFindings: List<Finding>) {
        val findings = allFindings.collapseSeeds()
        val known = baseline[case].orEmpty()
        val found = findings.map { it.dataset }.toSet()
        val new = findings.filter { it.dataset !in known }.distinctBy { it.dataset }
        val stale = known - found
        if (new.isEmpty() && stale.isEmpty()) return
        fail(
            buildString {
                appendLine("ContentRulesTest $case:")
                new.forEach { appendLine("  neu: ${it.dataset} – ${it.why}") }
                stale.forEach { appendLine("  behoben, Baseline-Eintrag streichen: $case | $it") }
            },
        )
    }

    /** SOLL-Fall oder noch nicht scharf geschalteter Fall: Funde nur ausgeben. */
    private fun report(case: String, findings: List<Finding>) {
        findings.collapseSeeds().forEach { println("ContentRulesTest $case (Hinweis): ${it.dataset} – ${it.why}") }
    }

    // ---- TEXT-06, MUSTER-03: Teaser -----------------------------------------------------

    /** Kürzel, deren Punkt keinen Satz beendet. */
    private val abbreviations = Regex("""\b(z\. ?B|ca|bzw|u\. ?a|d\. ?h|vgl|ggf|inkl|evtl|Nr|min|max|et al|s\. ?u|o\. ?Ä|[A-Z])\.""")

    /**
     * Teaser-Regel aus TEXT-06: ein geschlossener Satz, endet auf „.“, ≤ 95 Zeichen. Ob ein
     * finites Verb vorkommt, lässt sich nicht verlässlich automatisch prüfen (R-TEXT).
     */
    private fun teaserProblems(text: String): List<String> = buildList {
        if (!text.trimEnd().endsWith(".")) add("endet nicht auf „.“")
        if (text.length > 95) add("${text.length} Zeichen > 95")
        val sentences = abbreviations.replace(text.trimEnd().removeSuffix("."), "")
            .split(Regex("""[.!?](\s|$)""")).filter { it.isNotBlank() }
        if (sentences.size > 1) add("${sentences.size} Sätze")
    }

    @Test
    fun teaser() {
        val findings = exercises.flatMap { e ->
            val own = teaserProblems(e.effect).map { Finding("${e.key()}.effect", it) }
            val detail = e.effectDetail
            val repeats = detail != null && detail.startsWith(e.effect.trimEnd('.').take(40))
            own + if (repeats) listOf(Finding("${e.key()}.effectDetail", "beginnt mit dem Teaser")) else emptyList()
        }
        assertAgainstBaseline("teaser", findings)
    }

    @Test
    fun `first-caution`() {
        val findings = exercises.filter { it.cautions.isNotEmpty() }.flatMap { e ->
            teaserProblems(e.cautions.first()).map { Finding("${e.key()}.cautions[0]", it) }
        }
        assertAgainstBaseline("first-caution", findings)
    }

    /** SOLL ≤ 700 Zeichen: nur Hinweis, kein Testfehler. */
    @Test
    fun `detail-length`() {
        report(
            "detail-length",
            exercises.mapNotNull { e ->
                e.effectDetail?.takeIf { it.length > 700 }?.let { Finding("${e.key()}.effectDetail", "${it.length} Zeichen > 700") }
            },
        )
    }

    // ---- TEXT-09: Heilversprechen -------------------------------------------------------

    private val healthClaims: List<Regex> by lazy {
        repoFile("config/health-claims.txt").readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { Regex("""(?<![\p{L}\d])(?:$it)(?![\p{L}\d])""", RegexOption.IGNORE_CASE) }
    }

    private val healthClaimsAllow: List<String> by lazy {
        repoFile("config/health-claims.allow").readLines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
    }

    @Test
    fun `health-claims`() {
        val findings = allTexts.flatMap { t ->
            // Eine Ausnahme deckt nur Treffer innerhalb ihres eigenen Wortlauts ab, nicht den
            // ganzen Datensatz – ein zweites Versprechen im selben Text fällt weiter auf.
            val allowed = healthClaimsAllow.flatMap { a ->
                Regex(Regex.escape(a)).findAll(t.value).map { it.range }.toList()
            }
            healthClaims.flatMap { re -> re.findAll(t.value).toList() }
                .filter { m -> allowed.none { m.range.first >= it.first && m.range.last <= it.last } }
                .map { m -> Finding(t.id, "„${m.value}“") }
        }
        // TODO(S-08): scharf schalten (assertAgainstBaseline ohne Baseline), sobald die
        // Overclaims abgebaut sind – TEXT-09 lässt für Heilversprechen keine Baseline zu.
        report("health-claims", findings)
    }

    // ---- TEXT-05: Jargon ----------------------------------------------------------------

    private val jargon = listOf(
        "Schub", "Crash", "stabile[rn]? Phase", "vagal", "sympathisch", "(Para)?[Ss]ympathikus",
        "Hormese", "hormetisch", "CO₂-Toleranz", "CO2-Toleranz",
    ).map { Regex("""(?<![\p{L}])(?:$it)(?![\p{L}])""", RegexOption.IGNORE_CASE) }

    /** Laiensprache in Situationen, Labels und Tags (UI-Strings, Übungsnamen); Fachbegriffe nur im Detail. */
    @Test
    fun jargon() {
        val scope = situationTexts + strings + exerciseTexts.filter { it.id.endsWith(".name") }
        val findings = scope.flatMap { t ->
            jargon.mapNotNull { re -> re.find(t.value)?.let { Finding(t.id, "„${it.value}“") } }
        }
        assertAgainstBaseline("jargon", findings)
    }

    // ---- TEXT-04: Anrede ----------------------------------------------------------------

    private val formal = Regex("""(?<![.!?:]\s|^|„)\b(Sie|Ihnen|Ihr|Ihre[mnrs]?)\b""")
    private val capitalDu = Regex("""(?<![.!?:]\s|^|„)\b(Du|Dich|Dir|Dein\w*)\b""")

    @Test
    fun `formal-address`() {
        val findings = allTexts.flatMap { t ->
            listOfNotNull(
                formal.find(t.value)?.let { Finding(t.id, "Siezen „${it.value}“") },
                capitalDu.find(t.value)?.let { Finding(t.id, "„${it.value}“ groß") },
            )
        }
        assertAgainstBaseline("formal-address", findings)
    }

    // ---- TEXT-10: Zeichen ---------------------------------------------------------------

    /** Kennungen, in denen ein Punkt zwischen Ziffern richtig ist (DOI, Versionen, URLs, Lizenzen wie „Apache-2.0“). */
    private val identifiers = Regex("""doi:\S+|https?://\S+|\b\w[\w.-]*\.\w+/\S*|\bv?\d+\.\d+\.\d+\b|\b\p{L}[\p{L}-]*-\d+\.\d+\b""")

    @Test
    fun `decimal-comma`() {
        val findings = allTexts.mapNotNull { t ->
            Regex("""\d\.\d""").find(identifiers.replace(t.value, ""))?.let { Finding(t.id, "„${it.value}“ statt Dezimalkomma") }
        }
        assertAgainstBaseline("decimal-comma", findings)
    }

    /** Nur „…“ und ’: kein ASCII-`"`, kein `”`, kein ASCII-Apostroph zwischen Buchstaben. */
    @Test
    fun quotes() {
        val apostrophe = Regex("""\p{L}'\p{L}|\p{L}\\'""")
        val findings = (contentTexts + rawStrings).flatMap { t ->
            val v = if (t.id.startsWith("strings:")) t.value.replace(Regex("""^\\?"|\\?"$"""), "") else t.value
            listOfNotNull(
                if ('"' in v.replace("\\\"", "\"").replace(Regex("""<[^>]*>"""), "")) Finding(t.id, "ASCII-\"") else null,
                if ('”' in v) Finding(t.id, "„”“ statt „““") else null,
                apostrophe.find(v)?.let { Finding(t.id, "ASCII-Apostroph „${it.value}“") },
            )
        }
        assertAgainstBaseline("quotes", findings)
    }

    // ---- TEXT-12: Quellen ---------------------------------------------------------------

    private val citationFormat = Regex(
        """^\p{Lu}[\p{L}'-]+(?: [\p{Lu}][\p{L}'-]+)* (?:\p{Lu}\. ?)+(?:et al\.|& \p{Lu}[\p{L}'-]+ (?:\p{Lu}\. ?)+)? """ +
            """\(\d{4}\): .+\. .+""",
    )
    private val identifierFormat = Regex("""^(doi:10\.\S+|PMID \d+|PMC\d+)( · (PMID \d+|PMC\d+))*$""")

    /** Zitierformat und Kennung jeder Quelle; jede „(Name Jahr)“-Nennung im Text hat eine Quelle. */
    @Test
    fun `ref-format`() {
        val formatFindings = refs.flatMap { (name, ref) ->
            listOfNotNull(
                if (!citationFormat.matches(ref.citation)) Finding("refs.$name", "Zitat nicht „Nachname I. et al. (Jahr): Titel. Journal …“") else null,
                if (ref.identifier == null || !identifierFormat.matches(ref.identifier!!)) Finding("refs.$name.identifier", "Kennung nicht „doi:10.… · PMID n“: ${ref.identifier}") else null,
            )
        }
        val mention = Regex("""\((\p{Lu}[\p{L}-]+)(?: et al\.| & \p{Lu}[\p{L}-]+)? (\d{4})\)""")
        val missing = contentTexts.flatMap { t ->
            mention.findAll(t.value).mapNotNull { m ->
                val (author, year) = m.destructured
                val known = refs.values.any { it.citation.startsWith(author) && "($year)" in it.citation }
                if (known) null else Finding("${t.id}:$author $year", "„${m.value}“ fehlt in Refs.kt")
            }.toList()
        }
        assertAgainstBaseline("ref-format", formatFindings + missing)
    }

    @Test
    fun `ref-unused`() {
        val used = exercises.flatMap { it.references }.toSet()
        val findings = refs.filterValues { it !in used }.keys.map { Finding("refs.$it", "von keiner Übung genutzt") }
        assertAgainstBaseline("ref-unused", findings)
    }

    // ---- MOTION-02, TEXT-02: Phasen -----------------------------------------------------

    /** Pacer-Phasen unter 0,5 s blinken (MOTION-02); geprüft je Runde inklusive `perRound`. */
    @Test
    fun `phase-min-duration`() {
        val findings = exercises.flatMap { e ->
            (0 until e.rounds).flatMap { round ->
                e.segments.flatMap { it.phases }.map { e.perRound(it, round) }
            }.mapNotNull { p ->
                val d = p.duration
                if (d is PhaseDuration.Fixed && d.millis < 500) Finding("${e.key()}.${p.type}", "${d.millis} ms < 500 ms") else null
            }.distinctBy { it.dataset }
        }
        assertAgainstBaseline("phase-min-duration", findings)
    }

    /** Verschiedene Phasenarten brauchen verschiedene Labels (Anzeige, TalkBack, Ansage). */
    @Test
    fun `phase-labels-distinct`() {
        val defaults = mapOf(
            PhaseType.INHALE to "phase_inhale",
            PhaseType.EXHALE to "phase_exhale",
            PhaseType.HOLD_FULL to "phase_hold_full",
            PhaseType.HOLD_EMPTY to "phase_hold_empty",
            PhaseType.INHALE_TOP_UP to "phase_inhale_top_up",
            PhaseType.REST to "phase_rest",
        ).mapValues { (_, key) -> strings.first { it.id == "strings:$key" }.value }
        val defaultClashes = defaults.entries.groupBy { it.value }.filterValues { it.size > 1 }
            .map { (label, types) -> Finding("strings:phase_*:$label", "„$label“ für ${types.map { it.key }}") }
        val exerciseClashes = exercises.flatMap { e ->
            e.segments.flatMap { it.phases }
                .groupBy { it.label ?: defaults.getValue(it.type) }
                .filterValues { phases -> phases.map { it.type }.distinct().size > 1 }
                .filterKeys { label -> defaultClashes.none { it.dataset.endsWith(":$label") } }
                .map { (label, phases) -> Finding("${e.key()}.label:$label", "„$label“ für ${phases.map { it.type }.distinct()}") }
        }
        assertAgainstBaseline("phase-labels-distinct", defaultClashes + exerciseClashes)
    }

    // ---- TEXT-11: Store-Texte -----------------------------------------------------------

    @Test
    fun `store-lengths`() {
        val dir = repoFile("fastlane/metadata/android/de-DE")
        assumeTrue("fastlane/ gibt es noch nicht", dir.isDirectory)
        val limits = mapOf("title.txt" to 30, "short_description.txt" to 80, "full_description.txt" to 4000)
        val findings = limits.mapNotNull { (file, max) ->
            File(dir, file).takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.length > max }
                ?.let { Finding("fastlane:$file", "${it.length} Zeichen > $max") }
        } + (File(dir, "changelogs").listFiles().orEmpty()).mapNotNull { f ->
            f.readText().trim().takeIf { it.length > 500 }?.let { Finding("fastlane:changelogs/${f.name}", "${it.length} Zeichen > 500") }
        }
        assertAgainstBaseline("store-lengths", findings)
    }

    // ---- Pfade --------------------------------------------------------------------------

    /** Unit-Tests laufen im Modulverzeichnis `app/`; `config/` liegt im Repo-Root. */
    private fun repoFile(path: String): File =
        listOf(File("../$path"), File(path)).firstOrNull { it.exists() } ?: File("../$path")

    private fun moduleFile(path: String): File =
        listOf(File(path), File("app/$path")).first { it.exists() }
}
