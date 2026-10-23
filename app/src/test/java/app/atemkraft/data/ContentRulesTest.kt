package app.atemkraft.data

import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.domain.GeneratedExercises
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternRules
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.PatternTexts
import app.atemkraft.domain.PhaseNames
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.Reference
import app.atemkraft.domain.SituationRecommendation
import app.atemkraft.domain.withinCreationRules
import app.atemkraft.domain.withoutMarks
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Ehrliche Texte (STYLEGUIDE §11): Teaser (TEXT-06, -13, MUSTER-03), Heilversprechen samt
 * Store-Disclaimer (TEXT-09, -11, SEC-LEGAL-01/-02) und Typografie (TEXT-10), geprüft über alle
 * Fachinhalte: eingebaute Übungen samt Yoga-Lehre, Muster des Tages (Seeds 0–6), eigene und
 * gespeicherte Muster (alle Vorschläge des Muster-Editors), Atemlehre, Pranayama, Glossar,
 * Situationen samt Suchbegriffen, Quellen und `strings.xml`; dazu die öffentlichen Texte (README,
 * Datenschutzerklärung, Release-Notes, Store) auf Heilversprechen. Außerdem lösen alle
 * Querverweise zwischen diesen Inhalten auf (`cross-references`).
 *
 * Bekannte Verstöße stehen in `config/content-baseline.txt` (`Fall | Datensatz | S-nn`). Ein
 * Test sammelt seine Fälle und schlägt fehl bei einem neuen Verstoß und bei einem
 * Baseline-Eintrag, der nicht mehr zutrifft (dann Eintrag streichen, META-01).
 */
class ContentRulesTest {

    // ---- Datensätze ----------------------------------------------------------------------

    /**
     * Ein Text mit stabilem Namen für Baseline und Meldung, z. B. `buteyko.effect`. Geprüft wird
     * der gelesene Wortlaut [value], ohne die Sternchen der Sanskrit-Auszeichnung (TEXT-14).
     */
    private data class Text(val id: String, private val raw: String) {
        val value: String get() = raw.withoutMarks()
    }

    /** Muster des Tages, Seeds 0–6. */
    private val daily: List<Exercise> = (0L..6L).map { RandomPatternGenerator.forSeed(it).exercise }

    /**
     * Alle Muster in ganzen Sekunden, die die Erzeugungsregeln einhalten ([withinCreationRules]),
     * ruhig und sanft aktivierend: jedes, das als Muster des Tages (und damit gespeichert oder als
     * ältere eigene Vier-Felder-Zeile) vorkommen kann.
     */
    private val composerSpecs: List<PatternSpec> = buildList {
        for (activating in listOf(false, true)) {
            for (inhale in PatternRules.INHALE_MIN..PatternRules.EXHALE_MAX) {
                for (exhale in PatternRules.EXHALE_MIN..PatternRules.EXHALE_MAX) {
                    for (holdFull in 0..PatternRules.CALM_HOLD_FULL_MAX) {
                        for (holdEmpty in 0..PatternRules.CALM_HOLD_EMPTY_MAX) {
                            add(PatternSpec(inhale.toDouble(), holdFull.takeIf { it > 0 }?.toDouble(), exhale.toDouble(), holdEmpty.takeIf { it > 0 }?.toDouble(), activating))
                        }
                    }
                }
            }
        }
    }.filter { it.withinCreationRules() }

    /** Ältere eigene Muster mit vier Feldern (Herkunft CUSTOM) über alle diese Specs. */
    private val custom: List<Exercise> = composerSpecs.map { spec ->
        GeneratedExercises.exerciseFrom(
            spec,
            "muster-1",
            "Eigenes Muster ${PatternTexts.numbersOf(spec)}",
            PatternOrigin.CUSTOM,
            5,
        )
    }

    /** Gespeicherte Muster des Tages (Herkunft SAVED_DAILY) über dieselben Specs. */
    private val savedDaily: List<Exercise> = composerSpecs.map { spec ->
        GeneratedExercises.exerciseFrom(
            spec,
            "muster-1",
            "Tagesmuster ${PatternTexts.numbersOf(spec)}",
            PatternOrigin.SAVED_DAILY,
            5,
        )
    }

    private val exercises: List<Exercise> = BuiltInExercises.all + daily + custom + savedDaily

    /**
     * Name eines Übungs-Datensatzes; generierte Muster teilen sich ihre Id, daher mit Nummer
     * (`#seedN`, beim Zusammenfassen der Funde entfernt): `muster-des-tages`, `muster-eigen`
     * (Herkunft CUSTOM) und `muster-gespeichert` (SAVED_DAILY).
     */
    private fun Exercise.key(): String {
        val i = exercises.indexOf(this)
        val first = BuiltInExercises.all.size
        return when {
            i < first -> id
            i < first + daily.size -> "$id#seed${i - first}"
            i < first + daily.size + custom.size -> "muster-eigen#seed${i - first - daily.size}"
            else -> "muster-gespeichert#seed${i - first - daily.size - custom.size}"
        }
    }

    private fun Exercise.texts(): List<Text> = buildList {
        val k = key()
        add(Text("$k.name", name))
        add(Text("$k.shortDescription", shortDescription))
        add(Text("$k.effect", effect))
        effectDetail?.let { add(Text("$k.effectDetail", it)) }
        instructionHint?.let { add(Text("$k.instructionHint", it)) }
        cautions.forEachIndexed { i, c -> add(Text("$k.cautions[$i]", c)) }
        segments.flatMap { it.phases }.forEachIndexed { i, p ->
            p.label?.let { add(Text("$k.phase[$i].label", it)) }
            p.note?.let { add(Text("$k.phase[$i].note", it)) }
        }
        lore?.let { l ->
            add(Text("$k.lore.teaser", l.teaser))
            add(Text("$k.lore.detail", l.detail))
            l.source?.let { add(Text("$k.lore.source", it)) }
        }
    }

    private val exerciseTexts: List<Text> by lazy { exercises.flatMap { it.texts() }.distinct() }

    private val situationTexts: List<Text> by lazy {
        Situations.all.flatMap {
            listOfNotNull(
                Text("situation.${it.situation}.title", it.title),
                Text("situation.${it.situation}.rationale", it.rationale),
                it.warning?.let { w -> Text("situation.${it.situation}.warning", w) },
            ) + keywordTexts(it)
        } + Areas.all.flatMap { a -> a.keywords.mapIndexed { i, k -> Text("area.${a.family}.keywords[$i]", k) } }
    }

    /** Texte der Seite „Atemlehre“ (TEXT-13) – sie folgen denselben Inhaltsregeln wie die Übungen. */
    private val atemlehreTexts: List<Text> by lazy {
        buildList {
            add(Text("atemlehre.intro", Atemlehre.intro))
            Atemlehre.phases.forEachIndexed { i, p ->
                add(Text("atemlehre.phase[$i].name", p.name))
                add(Text("atemlehre.phase[$i].sanskrit", p.sanskrit))
                add(Text("atemlehre.phase[$i].meaning", p.meaning))
            }
            add(Text("atemlehre.vayusTeaser", Atemlehre.vayusTeaser))
            add(Text("atemlehre.vayusIntro", Atemlehre.vayusIntro))
            Atemlehre.vayus.forEach { v ->
                val k = "atemlehre.vayu.${v.vayu}"
                add(Text("$k.name", v.name))
                add(Text("$k.place", v.place))
                add(Text("$k.text", v.text))
            }
            add(Text("atemlehre.vayusSource", Atemlehre.vayusSource))
            add(Text("atemlehre.rulesTeaser", Atemlehre.rulesTeaser))
            Atemlehre.rules.forEachIndexed { i, (core, rest) -> add(Text("atemlehre.rules[$i]", core + rest)) }
            add(Text("atemlehre.samanaNote", Atemlehre.samanaNote))
            add(Text("atemlehre.rulesCheck", Atemlehre.rulesCheck))
            Atemlehre.patterns.forEach { p ->
                val k = "atemlehre.${p.id}"
                add(Text("$k.title", p.title))
                add(Text("$k.linkName", p.linkName))
                add(Text("$k.counts", p.counts))
                add(Text("$k.teaser", p.teaser))
                add(Text("$k.teaching", p.teaching))
                add(Text("$k.typical", p.typical))
                add(Text("$k.studies", p.studies))
                p.appNote?.let { add(Text("$k.appNote", it)) }
                p.origin?.let { add(Text("$k.origin", it)) }
                p.warning?.let { add(Text("$k.warning", it)) }
            }
            add(Text("atemlehre.influencesTeaser", Atemlehre.influencesTeaser))
            Atemlehre.influences.forEachIndexed { i, inf ->
                add(Text("atemlehre.influence[$i].label", inf.label))
                add(Text("atemlehre.influence[$i].text", inf.text))
            }
            add(Text("atemlehre.quote", Atemlehre.influencesQuote))
            add(Text("atemlehre.sourcesTeaser", Atemlehre.sourcesTeaser))
            Atemlehre.textSources.forEachIndexed { i, t -> add(Text("atemlehre.textSources[$i]", t)) }
        }
    }

    /** Texte der Seite „Pranayama“ (TEXT-13): Grundlagen, Techniken und Quellen. */
    private val pranayamaTexts: List<Text> by lazy {
        buildList {
            add(Text("pranayama.intro", Pranayama.intro))
            add(Text("pranayama.topicsTeaser", Pranayama.topicsTeaser))
            Pranayama.topics.forEach { t ->
                val k = "pranayama.topic.${t.id}"
                add(Text("$k.title", t.title))
                add(Text("$k.teaser", t.teaser))
                t.paragraphs.forEachIndexed { i, para -> add(Text("$k.paragraphs[$i]", para)) }
                t.source?.let { add(Text("$k.source", it)) }
            }
            add(Text("pranayama.techniquesTeaser", Pranayama.techniquesTeaser))
            Pranayama.techniques.forEach { t ->
                val k = "pranayama.${t.id}"
                add(Text("$k.name", t.name))
                add(Text("$k.sanskrit", t.sanskrit))
                t.pronunciation?.let { add(Text("$k.pronunciation", it)) }
                add(Text("$k.teaser", t.teaser))
                add(Text("$k.tradition", t.tradition))
                add(Text("$k.howTo", t.howTo))
                add(Text("$k.studies", t.studies))
                add(Text("$k.safety", t.safety))
                t.source?.let { add(Text("$k.source", it)) }
            }
            add(Text("pranayama.sourcesTeaser", Pranayama.sourcesTeaser))
            Pranayama.textSources.forEachIndexed { i, t -> add(Text("pranayama.textSources[$i]", t)) }
        }
    }

    /** Suchbegriffe einer Situation (MUSTER-09) – sie folgen denselben Inhaltsregeln wie die Wörter der Bereiche ([Areas]). */
    private fun keywordTexts(rec: SituationRecommendation): List<Text> = rec.keywords.mapIndexed { i, k -> Text("situation.${rec.situation}.keywords[$i]", k) }

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

    /** Glossar-Einträge (Körper & Studien, Yoga-Lehre): dieselben Regeln wie Übungstexte. */
    private val glossaryTexts: List<Text> by lazy {
        (Glossary.bodyTerms + Glossary.yogaTerms).flatMapIndexed { i, t ->
            listOf(Text("glossary[$i].name", t.name), Text("glossary[$i].definition", t.definition))
        }
    }

    private val contentTexts get() = exerciseTexts + atemlehreTexts + pranayamaTexts + glossaryTexts + situationTexts
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
    private fun List<Finding>.collapseSeeds(): List<Finding> = map { it.copy(dataset = it.dataset.replace(Regex("#seed\\d+"), "")) }.distinctBy { it.dataset to it.why }

    /** MUSS-Fall gegen die Baseline: neue Funde und veraltete Baseline-Einträge als Meldungen. */
    private fun againstBaseline(case: String, allFindings: List<Finding>): List<String> {
        val findings = allFindings.collapseSeeds()
        val known = baseline[case].orEmpty()
        val found = findings.map { it.dataset }.toSet()
        return findings.filter { it.dataset !in known }.distinctBy { it.dataset }.map { "$case neu: ${it.dataset} – ${it.why}" } +
            (known - found).map { "$case behoben, Baseline-Eintrag streichen: $case | $it" }
    }

    /** Fall ohne Baseline: jeder Fund ist ein Fehler, ein Baseline-Eintrag ebenso. */
    private fun strict(case: String, findings: List<Finding>): List<String> = (if (baseline[case].orEmpty().isNotEmpty()) listOf("$case: keine Baseline erlaubt, Einträge streichen") else emptyList()) +
        findings.collapseSeeds().map { "$case: ${it.dataset} – ${it.why}" }

    private fun assertNone(test: String, messages: List<String>) {
        if (messages.isNotEmpty()) fail("ContentRulesTest $test:\n" + messages.joinToString("\n") { "  $it" })
    }

    // ---- TEXT-06, MUSTER-03: Teaser -----------------------------------------------------

    /** Kürzel, deren Punkt keinen Satz beendet. */
    private val abbreviations = Regex("""\b(z\. ?B|ca|bzw|u\. ?a|d\. ?h|vgl|ggf|inkl|evtl|Nr|min|max|et al|s\. ?u|o\. ?Ä|[A-Z])\.""")

    /**
     * Teaser-Regel aus TEXT-06: ein geschlossener Satz, endet auf „.“, ≤ 95 Zeichen. Ob ein
     * finites Verb vorkommt, lässt sich nicht verlässlich automatisch prüfen (R-TEXT). Gezählt
     * wird der gelesene Wortlaut ohne die Sternchen der Sanskrit-Auszeichnung (TEXT-14).
     */
    private fun teaserProblems(marked: String): List<String> = buildList {
        val text = marked.withoutMarks()
        if (!text.trimEnd().endsWith(".")) add("endet nicht auf „.“")
        if (text.length > 95) add("${text.length} Zeichen > 95")
        val sentences = abbreviations.replace(text.trimEnd().removeSuffix("."), "")
            .split(Regex("""[.!?](\s|$)""")).filter { it.isNotBlank() }
        if (sentences.size > 1) add("${sentences.size} Sätze")
    }

    /**
     * TEXT-06, MUSTER-03: Teaser und erster Vorsicht-Hinweis; TEXT-13: Teaser der Yoga-Lehre
     * (Detailseite, Muster-Karten und Abschnitte der Atemlehre, Grundlagen, Techniken und
     * Abschnitte der Seite „Pranayama“) ohne Baseline, zuschreibend statt
     * Wirkung zu behaupten („gilt in der Yoga-Lehre als …“, nie „wirkt“). Das Detail beginnt nie
     * mit dem Teaser.
     */
    @Test
    fun teaser() {
        val teaser = exercises.flatMap { e ->
            val own = teaserProblems(e.effect).map { Finding("${e.key()}.effect", it) }
            val detail = e.effectDetail
            val repeats = detail != null && detail.startsWith(e.effect.trimEnd('.').take(40))
            own + if (repeats) listOf(Finding("${e.key()}.effectDetail", "beginnt mit dem Teaser")) else emptyList()
        }
        val firstCaution = exercises.filter { it.cautions.isNotEmpty() }.flatMap { e ->
            teaserProblems(e.cautions.first()).map { Finding("${e.key()}.cautions[0]", it) }
        }
        val lore = exercises.mapNotNull { e -> e.lore?.let { Triple("${e.key()}.lore", it.teaser, it.detail) } }
        val patterns = Atemlehre.patterns.map { Triple("atemlehre.${it.id}", it.teaser, it.teaching) } +
            Pranayama.topics.map { Triple("pranayama.topic.${it.id}", it.teaser, it.paragraphs.first()) } +
            Pranayama.techniques.map { Triple("pranayama.${it.id}", it.teaser, it.tradition) }
        val sections = listOf(
            "atemlehre.vayusTeaser" to Atemlehre.vayusTeaser,
            "atemlehre.rulesTeaser" to Atemlehre.rulesTeaser,
            "atemlehre.influencesTeaser" to Atemlehre.influencesTeaser,
            "atemlehre.sourcesTeaser" to Atemlehre.sourcesTeaser,
            "pranayama.topicsTeaser" to Pranayama.topicsTeaser,
            "pranayama.techniquesTeaser" to Pranayama.techniquesTeaser,
            "pranayama.sourcesTeaser" to Pranayama.sourcesTeaser,
        )
        val claim = Regex("""\b(wirkt|bewirkt|hilft gegen)\b""", RegexOption.IGNORE_CASE)
        val loreTeaser = (lore + patterns).flatMap { (key, teaser, detail) ->
            val repeats = detail.startsWith(teaser.trimEnd('.').take(40))
            teaserProblems(teaser).map { Finding("$key.teaser", it) } +
                if (repeats) listOf(Finding("$key.detail", "beginnt mit dem Teaser")) else emptyList()
        } + sections.flatMap { (key, teaser) -> teaserProblems(teaser).map { Finding(key, it) } } +
            (lore.map { it.first to it.second } + patterns.map { it.first to it.second } + sections).mapNotNull { (key, teaser) ->
                claim.find(teaser)?.let { Finding("$key.teaser", "Wirkbehauptung „${it.value}“ statt Zuschreibung") }
            }
        assertNone(
            "teaser",
            againstBaseline("teaser", teaser) + againstBaseline("first-caution", firstCaution) + strict("lore-teaser", loreTeaser),
        )
    }

    // ---- Querverweise --------------------------------------------------------------------

    /**
     * Jeder Verweis zwischen den Inhalten führt auf ein Ziel, das es gibt: Übungen aus Atemlehre
     * (Karten und „Weitere Einflüsse“), Pranayama-Techniken und Situationen auf
     * [BuiltInExercises.all] oder das Muster des Tages ([ExerciseIds.DAILY]); die
     * Yoga-Lehre jeder Übung (auch der generierten Muster) auf eine Karte in [Atemlehre.patterns].
     */
    @Test
    fun `cross-references`() {
        val exerciseIds = BuiltInExercises.all.map { it.id }.toSet() + ExerciseIds.DAILY
        val patternIds = Atemlehre.patterns.map { it.id }.toSet()
        val toExercises = Atemlehre.patterns.flatMap { p -> p.exerciseIds.map { "atemlehre.${p.id}" to it } } +
            Atemlehre.influences.flatMap { inf -> inf.exerciseIds.map { "atemlehre.influence ${inf.label}" to it } } +
            Pranayama.techniques.flatMap { t -> (listOfNotNull(t.exerciseId) + t.moreExerciseIds).map { "pranayama.${t.id}" to it } } +
            Situations.all.flatMap { rec -> rec.exerciseIds.map { "situation.${rec.situation}" to it } }
        val toPatterns = exercises.flatMap { e ->
            e.lore?.let { l -> (listOf(l.patternId) + l.morePatternIds).map { "${e.key()}.lore" to it } }.orEmpty()
        }
        val broken = toExercises.filter { (_, id) -> id !in exerciseIds }.map { (from, id) -> "$from → Übung „$id“" } +
            toPatterns.filter { (_, id) -> id !in patternIds }.map { (from, id) -> "$from → Atemlehre-Karte „$id“" }
        assertNone("cross-references", broken.distinct())
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

    private fun healthClaimFindings(texts: List<Text>): List<Finding> = texts.flatMap { t ->
        // Eine Ausnahme deckt nur Treffer innerhalb ihres eigenen Wortlauts ab, nicht den
        // ganzen Datensatz – ein zweites Versprechen im selben Text fällt weiter auf.
        val allowed = healthClaimsAllow.flatMap { a ->
            Regex(Regex.escape(a)).findAll(t.value).map { it.range }.toList()
        }
        healthClaims.flatMap { re -> re.findAll(t.value).toList() }
            .filter { m -> allowed.none { m.range.first >= it.first && m.range.last <= it.last } }
            .map { m -> Finding(t.id, "„${m.value}“") }
    }

    /**
     * Öffentliche Texte außerhalb der App (SEC-LEGAL-01): README, Datenschutzerklärung,
     * Release-Notes und Store-Texte samt Changelogs, je Zeile ein Datensatz `datei:zeile`.
     */
    private val publicTexts: List<Text> by lazy {
        val files = listOf(repoFile("README.md"), repoFile("docs/PRIVACY.md")) +
            repoFile("docs/release-notes").listFiles { f -> f.extension == "md" }.orEmpty().sorted() +
            repoFile("fastlane/metadata/android/de-DE").walkTopDown().filter { it.isFile && it.extension == "txt" }.sorted()
        val root = checkNotNull(repoFile("README.md").absoluteFile.parentFile)
        files.filter { it.isFile }.flatMap { f ->
            val name = f.absoluteFile.relativeTo(root).path
            f.readLines().mapIndexed { i, line -> Text("$name:${i + 1}", line) }
        }
    }

    /**
     * TEXT-09 ohne Baseline: App-Texte und öffentliche Texte in einem Fall; nur bewusste
     * Verneinungen und Disclaimer stehen zeilengenau in health-claims.allow. Dazu SEC-LEGAL-02:
     * Die Store-Beschreibung enthält den Disclaimer wörtlich.
     */
    @Test
    fun `health-claims`() {
        val disclaimer = "Kein Medizinprodukt; diagnostiziert, behandelt, heilt oder verhindert keine Erkrankung. " +
            "Für medizinischen Rat wende dich an Fachpersonal."
        val description = repoFile("fastlane/metadata/android/de-DE/full_description.txt")
        val missingDisclaimer = if (description.isFile && disclaimer !in description.readText()) {
            listOf("store-texts: full_description.txt enthält den Disclaimer aus SEC-LEGAL-02 nicht wörtlich")
        } else {
            emptyList()
        }
        assertNone("health-claims", strict("health-claims", healthClaimFindings(allTexts + publicTexts)) + missingDisclaimer)
    }

    // ---- TEXT-10: Zeichen ---------------------------------------------------------------

    /** Kennungen, in denen ein Punkt zwischen Ziffern richtig ist (DOI, Versionen, URLs, Lizenzen wie „Apache-2.0“). */
    private val identifiers = Regex("""doi:\S+|https?://\S+|\b\w[\w.-]*\.\w+/\S*|\bv?\d+\.\d+\.\d+\b|\b\p{L}[\p{L}-]*-\d+\.\d+\b""")

    /**
     * TEXT-10: Dezimalkomma (`\d\.\d` außerhalb Kennungen), nur „…“ und ’ (kein ASCII-`"`, kein
     * `”`, kein ASCII-Apostroph zwischen Buchstaben), Trennpunkt „·“ nur nach geschütztem
     * Leerzeichen (sonst könnte der Umbruch ihn an den Zeilenanfang setzen, `DOT_SEPARATOR`), Zahl
     * und Einheit („4\u00A0s“, „ca.\u00A0%1$d\u00A0min“, „7\u00A0mmHg“, „93\u00A0%“) und Fallzahl
     * („n\u00A0=\u00A040“) in `strings.xml`, allen Fachinhalten und Quellen zusammen.
     */
    @Test
    fun typography() {
        val decimal = allTexts.mapNotNull { t ->
            Regex("""\d\.\d""").find(identifiers.replace(t.value, ""))?.let { Finding(t.id, "„${it.value}“ statt Dezimalkomma") }
        }
        val apostrophe = Regex("""\p{L}'\p{L}|\p{L}\\'""")
        val quotes = (contentTexts + rawStrings).flatMap { t ->
            val v = if (t.id.startsWith("strings:")) t.value.replace(Regex("""^\\?"|\\?"$"""), "") else t.value
            listOfNotNull(
                if ('"' in v.replace("\\\"", "\"").replace(Regex("""<[^>]*>"""), "")) Finding(t.id, "ASCII-\"") else null,
                if ('”' in v) Finding(t.id, "„”“ statt „““") else null,
                apostrophe.find(v)?.let { Finding(t.id, "ASCII-Apostroph „${it.value}“") },
            )
        }
        val refIds = refs.mapNotNull { (name, ref) -> ref.identifier?.let { Text("refs.$name.identifier", it) } }
        val dots = (contentTexts + strings + refIds).mapNotNull { t ->
            if (Regex("""\s·""").containsMatchIn(t.value.replace("\u00A0·", ""))) Finding(t.id, "„ ·“ ohne geschütztes Leerzeichen") else null
        }
        val refTexts = refs.map { (name, ref) -> Text("refs.$name.citation", ref.citation) }
        val unitPattern = Regex("""\d (s|min|Hz|mmHg)\b|\d %|\bn ?= ?\d""")
        val units = (contentTexts + strings + refTexts).mapNotNull { t ->
            unitPattern.find(t.value)?.let { Finding(t.id, "„${it.value}“ ohne geschütztes Leerzeichen") }
        }
        // Die Phasennamen der Domain (PhaseNames) spiegeln strings.xml.
        val phaseNames = mapOf(
            "phase_inhale" to PhaseNames.INHALE,
            "phase_exhale" to PhaseNames.EXHALE,
            "phase_hold_full" to PhaseNames.HOLD,
            "phase_hold_empty" to PhaseNames.HOLD,
        ).mapNotNull { (key, name) ->
            val value = strings.firstOrNull { it.id == "strings:$key" }?.value
            if (value != name) Finding("strings:$key", "„$value“ statt „$name“ wie PhaseNames") else null
        }
        assertNone(
            "typography",
            againstBaseline("decimal-comma", decimal) + againstBaseline("quotes", quotes) +
                againstBaseline("dot-separator", dots) + againstBaseline("number-unit", units) +
                strict("phase-names", phaseNames),
        )
    }

    // ---- Pfade --------------------------------------------------------------------------

    /** Unit-Tests laufen im Modulverzeichnis `app/`; `config/` liegt im Repo-Root. */
    private fun repoFile(path: String): File = listOf(File("../$path"), File(path)).firstOrNull { it.exists() } ?: File("../$path")

    private fun moduleFile(path: String): File = listOf(File(path), File("app/$path")).first { it.exists() }
}
