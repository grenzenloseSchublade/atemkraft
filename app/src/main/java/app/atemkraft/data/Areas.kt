package app.atemkraft.data

import app.atemkraft.domain.BreathingArea
import app.atemkraft.domain.BreathingFamily

/**
 * Die vier Bereiche (Familien) als zweite Ebene im Block „Wie fühlst du dich?“ (MUSTER-09) mit
 * ihren breiten Suchwörtern, kuratiert wie die Anlass-Begriffe in [Situations]: Laiensprache,
 * keine Symptome oder Diagnosen, nichts Versprechendes, je Familie ihr eigener Name
 * („Beruhigen“, „Aktivieren“, „Ausgleichen“, „Atem schulen“; seit 2026-10-24 ohne die Buchstaben
 * A–D) und ihr früherer Name („Herunterregeln“), falls jemand ihn noch kennt. **Kein Wort, das schon zu einem Anlass führt** – „Ruhe“, „entspannen“,
 * „runterkommen“ (Einschlafen) und „Sport“ (Kurzatmig) bleiben dort.
 *
 * Bewusst nicht dabei: „Energie“, „Schwung“ und „munter“ – „keine Energie“, „nicht munter“
 * meinen meist Müdigkeit, und die führt zu „Erschöpft oder ausgelaugt“, nie zu „Aktivieren“
 * (Verneinungen deutet die Suche nicht). „Nase“ und „Mundatmung“ – über den Wortanfang führten
 * „verstopfte Nase“ und „trockener Mund“, also Beschwerden, zu einer Übung. „Aktivieren“ trägt die
 * Warnung der intensiven Übungen ([Situations.INTENSE_WARNING]) und erscheint nur, wenn kein
 * anderer kuratierter Begriff passt.
 */
object Areas {

    val all: List<BreathingArea> = listOf(
        BreathingArea(
            family = BreathingFamily.DOWNREGULATE,
            keywords = listOf(
                "Herunterregeln",
                "runterregeln",
                "beruhigen",
                "Beruhigung",
                "gelassen",
                "Gelassenheit",
                "entschleunigen",
                "abregen",
            ),
        ),
        BreathingArea(
            family = BreathingFamily.UPREGULATE,
            keywords = listOf("Hochregeln", "aktivieren", "anregen"),
            warning = Situations.INTENSE_WARNING,
        ),
        BreathingArea(
            family = BreathingFamily.BALANCE,
            keywords = listOf(
                "Balancieren",
                "Balance",
                "ausgleichen",
                "Ausgleich",
                "ausgeglichen",
                "Gleichgewicht",
                "Mitte",
            ),
        ),
        BreathingArea(
            family = BreathingFamily.FUNCTIONAL,
            keywords = listOf("Atem schulen", "Funktionell", "Atemtechnik", "Atemmuster", "Haltung", "Ausdauer"),
        ),
    )
}
