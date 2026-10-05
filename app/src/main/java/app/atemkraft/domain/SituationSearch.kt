package app.atemkraft.domain

import java.text.Normalizer

/**
 * Befindens-Suche im Situationen-Tab: findet zu einer freien Eingabe („kann nicht schlafen“,
 * „müde“) die passenden [SituationRecommendation]s – deterministisch, offline, ohne Modell.
 *
 * Regeln (bewusst eng, damit jede Zuordnung nachvollziehbar und prüfbar bleibt):
 * - **Normalisierung:** klein, ä/ö/ü/ß gleichwertig zu ae/oe/ue/ss, andere Akzente weg,
 *   Satzzeichen trennen Wörter.
 * - **Suchwörter:** Wörter ab [MIN_WORD] Zeichen, ohne Füllwörter („ich“, „bin“, „nicht“ …).
 *   Gibt es keins, ist die Suche noch nicht aktiv (`null`): Beim Tippen von „m“, „mü“ springt
 *   die Ansicht nicht auf „Nichts gefunden“.
 * - **Treffer:** Ein Suchwort trifft ein Wort aus Titel, Begründungssatz oder einem
 *   einwortigen Begriff, wenn es dieses Wort ganz ist oder dessen Anfang („müd“ → „müde“).
 *   Nie mitten im Wort, keine Tippfehler-Toleranz.
 * - **Gebeugt und zusammengesetzt:** Nur bei kuratierten Einzel-Begriffen ab [MIN_STEM]
 *   Buchstaben trifft auch ein Suchwort, das mit dem Begriff beginnt („Prüfungen“,
 *   „Prüfungsstress“ → „Prüfung“; „gestresste“ → „gestresst“). Titel und Begründungssätze
 *   bleiben beim Wortanfang, sonst fände „Ruhepuls“ jeden Satz mit „ruhig“. Enthält das
 *   Suchwort einen Diagnose-, Symptom-, Medikamenten- oder Verletzungs-Wortteil
 *   ([DIAGNOSIS_PARTS]: „Angststörung“, „Ruhepuls“, „Schlaftabletten“, „Sportverletzung“), gilt
 *   diese Verlängerung nicht – so etwas führt nie zu einer Übung.
 * - **Genauester Begriff gewinnt:** Treffen die Begriffe mehrerer Situationen dasselbe Suchwort,
 *   zählt nur der genaueste – gleich vor Wortanfang vor Verlängerung, unter Verlängerungen der
 *   längere Begriff. „Nachtschicht(en)“ führt so zu „Erschöpft“ (eigener Begriff), nicht über
 *   „nachts“ zum Einschlafen; „nachts“ bleibt beim Einschlafen, obwohl es „Nachtschicht“
 *   anfängt.
 * - **Wendungen:** Mehrwortige Begriffe („wach liegen“) treffen nur als Ganzes, ihr letztes
 *   Wort darf noch unvollständig sein. Die Wörter einer gefundenen Wendung gehören dann nur
 *   dieser Situation – „kann nachts wach liegen“ führt nicht über „wach“ zu „Wach &
 *   energiegeladen werden“.
 * - **Warn-Situationen** treffen nur über ihren Titel, nie über den Begründungssatz: Dessen
 *   Vorbehalt („nur, wenn du dich stabil und gesund fühlst“) würde sonst „nicht gesund“ oder
 *   „fühle mich nicht stabil“ ausgerechnet zu Wim Hof führen. Und sie erscheinen nur, wenn
 *   kein kuratierter Begriff einer anderen Situation trifft („müde, will wach werden“ zeigt
 *   „Erschöpft oder ausgelaugt“, nicht „Wach & energiegeladen werden“).
 * - **Verneinungen** werden nicht gedeutet: „nicht müde“ trifft wie „müde“. Raten wäre
 *   unzuverlässiger als ein ehrlicher Treffer, den man selbst einordnen kann.
 * - **Rangfolge:** mehr getroffene Suchwörter zuerst, sonst die Reihenfolge der Daten.
 */
object SituationSearch {

    /** Kürzere Wörter sind zu unspezifisch für einen Wortanfang-Treffer. */
    const val MIN_WORD = 3

    /** Kürzere Begriffe verlängern kein Suchwort („Uni“ trifft nicht „Unikat“). */
    const val MIN_STEM = 4

    /**
     * Diagnose-, Symptom-, Medikamenten- und Verletzungs-Wortteile (normalisiert). Ein Suchwort,
     * das einen davon enthält, trifft keinen Begriff über dessen Verlängerung: „Angststörung“
     * ist nicht „Angst“, „Schlafapnoe“ und „Schlaftabletten“ nicht „Schlaf“, „Ruhepuls“ nicht
     * „Ruhe“, „Sportverletzung“ nicht „Sport“.
     */
    val DIAGNOSIS_PARTS = Regex(
        "asthma|copd|depress|stoerung|krank|syndrom|schmerz|atemnot|herz|blutdruck|burnout|covid|" +
            "attacke|anfall|tinnitus|migraene|apnoe|allergi|phobie|trauma|zustand|zustaend|puls|" +
            "tablett|medikament|mittel|unfall|verletz",
    )

    /**
     * Füllwörter typischer Antworten auf „Wie fühlst du dich?“ (normalisiert). Sie treffen
     * nichts, weder in der Eingabe noch in Titel und Begründung.
     */
    private val stopWords = setOf(
        "ich", "mich", "mir", "mein", "meine", "meinen", "meinem", "meiner", "du", "dich", "dir",
        "sich", "uns", "wir", "man", "bin", "bist", "ist", "sind", "war", "waere", "habe", "hab",
        "hast", "hat", "haben", "kann", "kannst", "will", "moechte", "mag", "muss", "werde",
        "werden", "wird", "fuehle", "fuehlst", "fuehlt", "fuehlen", "fuehl", "geht", "gehts",
        "und", "oder", "aber", "auch", "noch", "schon", "nicht", "nichts", "kein", "keine",
        "keinen", "sehr", "total", "ganz", "etwas", "irgendwie", "bisschen", "einfach", "mal",
        "gerade", "jetzt", "heute", "immer", "wieder", "oft", "viel", "nur", "nie", "die", "der",
        "das", "dem", "den", "des", "ein", "eine", "einen", "einem", "einer", "zum", "zur", "mit",
        "von", "vom", "bei", "beim", "auf", "aus", "fuer", "vor", "nach", "seit", "wie", "was",
        "wenn", "weil", "dass", "hilft", "macht",
        // In einer Atem-App sagen diese Wörter nichts über das Befinden.
        "atem", "atmen", "atmung", "uebung", "ueben",
    )

    /** Normalisierte Form für den Vergleich (siehe Klassen-KDoc). */
    fun normalize(text: String): String {
        val lower = Normalizer.normalize(text, Normalizer.Form.NFC).lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
            .trim()
    }

    private fun words(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }

    private fun isSearchWord(word: String) = word.length >= MIN_WORD && word !in stopWords

    /** Vergleichsdaten einer Situation, einmal pro Suche aufbereitet. */
    private class Index(val rec: SituationRecommendation) {
        private val keywordWords = rec.keywords.map(::words)
        val phrases = keywordWords.filter { it.size > 1 }
        val keywords = keywordWords.filter { it.size == 1 }.map { it.single() }
        val textWords = (words(rec.title) + if (rec.warn) emptyList() else words(rec.rationale)).filter(::isSearchWord)

        /** Genauigkeit des besten Begriffs für [word]; 0 = kein Begriff trifft. */
        fun keywordTier(word: String) = keywords.maxOfOrNull { tier(word, it) } ?: 0
        fun matchesText(word: String) = textWords.any { it.startsWith(word) }
    }

    /**
     * Wie genau [keyword] das Suchwort [word] trifft, höher ist genauer: gleich vor Wortanfang
     * des Begriffs vor Verlängerung; unter Verlängerungen zählt der längere Begriff. 0 = nichts.
     */
    private fun tier(word: String, keyword: String): Int = when {
        keyword == word -> TIER_EQUAL
        keyword.startsWith(word) -> TIER_START
        keyword.length >= MIN_STEM && word.startsWith(keyword) && !DIAGNOSIS_PARTS.containsMatchIn(word) -> keyword.length
        else -> 0
    }

    // Über jeder möglichen Wortlänge, damit Verlängerungen (Wert = Begriffslänge) darunter bleiben.
    private const val TIER_EQUAL = Int.MAX_VALUE
    private const val TIER_START = Int.MAX_VALUE - 1

    /** Startpositionen, an denen [phrase] in [words] steht; das letzte Wort darf ein Anfang sein. */
    private fun phraseStarts(words: List<String>, phrase: List<String>): List<Int> = (0..words.size - phrase.size).filter { start ->
        phrase.indices.all { j ->
            val w = words[start + j]
            w == phrase[j] || (j == phrase.lastIndex && w.length >= MIN_WORD && phrase[j].startsWith(w))
        }
    }

    /**
     * Passende Situationen zu [query], beste zuerst. `null`, solange die Eingabe kein Suchwort
     * enthält (dann zeigt der Tab die normale Übersicht); eine leere Liste heißt „nichts
     * gefunden“.
     */
    fun search(query: String, situations: List<SituationRecommendation>): List<SituationRecommendation>? {
        val words = words(query)
        val searchIdx = words.indices.filter { isSearchWord(words[it]) }
        if (searchIdx.isEmpty()) return null
        val index = situations.map(::Index)

        // Wendungen zuerst: Ihre Wörter gehören der Situation der Wendung (erste gewinnt).
        val owner = HashMap<Int, Index>()
        index.forEach { s ->
            s.phrases.forEach { phrase ->
                phraseStarts(words, phrase).forEach { start ->
                    phrase.indices.forEach { owner.putIfAbsent(start + it, s) }
                }
            }
        }

        // Je Suchwort die Genauigkeit des besten Begriffs über alle Situationen.
        val best = searchIdx.associateWith { i -> index.maxOf { it.keywordTier(words[i]) } }
        fun keywordMatch(s: Index, i: Int) = best.getValue(i).let { b -> b > 0 && s.keywordTier(words[i]) == b }

        val scores = index.associateWith { s ->
            searchIdx.count { i -> owner[i]?.let { it === s } ?: (keywordMatch(s, i) || s.matchesText(words[i])) }
        }
        val keywordHit = index.any { s ->
            !s.rec.warn && searchIdx.any { i -> owner[i] === s || (owner[i] == null && keywordMatch(s, i)) }
        }
        return index
            .filter { scores.getValue(it) > 0 && !(it.rec.warn && keywordHit) }
            .sortedByDescending { scores.getValue(it) } // stabil: Gleichstand behält die Datenreihenfolge
            .map { it.rec }
    }
}
