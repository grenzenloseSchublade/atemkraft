package app.atemkraft.domain

/**
 * Was die Yoga-Lehre über eine Übung sagt: Deutung der Tradition, **keine Studienlage**
 * (TEXT-13). Steht auf der Detailseite als eigener Abschnitt „Aus der Yoga-Lehre“, getrennt von
 * „Wirkung“, und verweist auf die passende Muster-Karte der Atemlehre.
 *
 * @param teaser Ein abgeschlossener Satz mit Punkt, ≤ 95 Zeichen (Teaser-Regel wie `effect`),
 *   immer zuschreibend formuliert („gilt in der Yoga-Lehre als …“), nie als Wirkbehauptung.
 * @param detail Vertiefung desselben Gedankens, aufgeklappt unter dem Teaser.
 * @param patternId Kennung der Muster-Karte in der Atemlehre ([LorePattern.id]).
 * @param source Klassischer Text als Kurznachweis, Verse deutsch zitiert („Hatha Yoga Pradipika
 *   2,68“); `null`, wenn die Deutung aus der Atemlehre selbst folgt (generierte Muster).
 * @param morePatternIds Weitere passende Karten nach der Hauptkarte, z. B. „Halten voll“ für ein
 *   generiertes Muster mit Halten nach dem Einatmen; je Karte ein eigener Verweis.
 */
data class YogaLore(
    val teaser: String,
    val detail: String,
    val patternId: String,
    val source: String? = null,
    val morePatternIds: List<String> = emptyList(),
)

/** Ein Abschnitt des Rhythmusbildes einer Muster-Karte; [passive] wird blasser gezeichnet. */
data class RhythmStep(
    val type: PhaseType,
    val seconds: Double,
    val passive: Boolean = false,
)

/**
 * Eine Muster-Karte der Atemlehre: ein Atemverhältnis und wie die Yoga-Lehre es deutet.
 *
 * @param id Stabile Kennung, Sprungziel aus der Detailseite ([YogaLore.patternId]).
 * @param linkName Kurzname im Verweis „<linkName> in der Atemlehre“.
 * @param counts Beispielwerte unter dem Rhythmusbild, z. B. „Ein 4 · Aus 6“.
 * @param teaser Geschlossener Satz ≤ 95 Zeichen (Teaser-Regel).
 * @param teaching Was die Lehre dazu sagt.
 * @param typical Wofür die Lehre das Muster einsetzt.
 * @param studies Was Studien dazu zeigen, ehrlich eingeordnet.
 * @param exerciseIds Übungen der App, die dem Muster folgen (antippbar).
 * @param appNote Ergänzung zu den Übungen (z. B. „Noch keine Übung in der App.“).
 * @param origin Hinweis zur Herkunft, wo die Zuordnung eine Systematisierung ist.
 * @param caution Etikett „nur gesund & ausgeruht“.
 * @param warning Harte Sicherheitsregel, sichtbar unter „Typisch“.
 */
data class LorePattern(
    val id: String,
    val title: String,
    val linkName: String,
    val rhythm: List<RhythmStep>,
    val counts: String,
    val teaser: String,
    val teaching: String,
    val typical: String,
    val studies: String,
    val exerciseIds: List<String> = emptyList(),
    val appNote: String? = null,
    val origin: String? = null,
    val caution: Boolean = false,
    val warning: String? = null,
)

/** Eine der vier Atemphasen mit Sanskrit-Namen und ihrer Bedeutung in der Lehre. */
data class LorePhase(
    val type: PhaseType,
    val name: String,
    val sanskrit: String,
    val meaning: String,
)

/** Die fünf *vāyus* der Lehre (Praśna-Upaniṣad 3): Kennung für Ort, Farbe und Richtung im Bild. */
enum class Vayu { PRANA, APANA, SAMANA, UDANA, VYANA }

/**
 * Ein *vāyu* in der Atemlehre: [name] mit Sanskrit-Auszeichnung („*prāṇa*“), [place] im Körper
 * nach der Lehre, [text] ein zuschreibender Satz zu Richtung und Deutung.
 */
data class LoreVayu(
    val vayu: Vayu,
    val name: String,
    val place: String,
    val text: String,
)

/** Ein Eintrag unter „Weitere Einflüsse“ (Atemweg, Tageszeit, Typ). */
data class LoreInfluence(
    val label: String,
    val text: String,
    val exerciseIds: List<String> = emptyList(),
)
