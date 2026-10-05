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

        fun matchesKeyword(word: String) = keywords.any { it.startsWith(word) }
        fun matches(word: String) = matchesKeyword(word) || textWords.any { it.startsWith(word) }
    }

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

        val scores = index.associateWith { s ->
            searchIdx.count { i -> owner[i]?.let { it === s } ?: s.matches(words[i]) }
        }
        val keywordHit = index.any { s ->
            !s.rec.warn && searchIdx.any { i -> owner[i] === s || (owner[i] == null && s.matchesKeyword(words[i])) }
        }
        return index
            .filter { scores.getValue(it) > 0 && !(it.rec.warn && keywordHit) }
            .sortedByDescending { scores.getValue(it) } // stabil: Gleichstand behält die Datenreihenfolge
            .map { it.rec }
    }
}
