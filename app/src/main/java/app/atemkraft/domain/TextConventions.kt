package app.atemkraft.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Trennpunkt zwischen Teilen einer Zeile („Einatmen 4 s · Ausatmen 6 s“, „5 Sitzungen · 40 min“).
 * Das geschützte Leerzeichen vor dem Punkt hält ihn am Zeilenende: Bei großer Schrift beginnt
 * eine Zeile nie mit „·“ (LAYOUT-03). Liegt in domain/, weil auch der Mustergenerator Zeilen
 * baut; String-Ressourcen und Datentexte (`data/`) schreiben dasselbe als `\u00A0· `
 * (ContentRulesTest `dot-separator`).
 */
const val DOT_SEPARATOR = " · "

/**
 * Ein Trennpunkt „·“ mit (auch geschützten) Leerzeichen drumherum: die Stelle, an der eine Zeile
 * aus Teilen ([DOT_SEPARATOR]) umbrechen darf. Regel „keine Zeile beginnt oder endet mit ·“
 * (LAYOUT-03, TEXT-10): Bricht eine solche Zeile um, dann an einem Trennpunkt, und der Punkt
 * entfällt dort. Als Layout `DotSeparatedFlow` (Teile gierig je Zeile), für fertige Texte
 * [splitAtDots] (Teile je eine Zeile: Status unter dem Titel, „Starten · ca. 5 min“).
 */
val DOT_SPLIT = Regex("[\\s ]*·[\\s ]*")

/** Die Teile zwischen den Trennpunkten ([DOT_SPLIT]), ohne Punkt und ohne leere Teile. */
fun String.splitAtDots(): List<String> = split(DOT_SPLIT).filter { it.isNotEmpty() }

/**
 * Ohne führenden Trennpunkt: Ein Zustands-Suffix wie „· angepasst“ ([asDotSuffix]) verliert ihn,
 * wenn er in eine eigene Zeile rutscht – keine Zeile beginnt mit „·“.
 */
fun String.withoutLeadingDot(): String = trimStart().removePrefix("·").trimStart()

/**
 * Zustands-Suffix hinter einem Titel oder Knopftext: „· angepasst“ (wie in `strings.xml` der
 * Status „· 3 von 12 Phasen“). Steht er in einer eigenen Zeile, gilt [withoutLeadingDot].
 */
fun String.asDotSuffix(): String = "· $this"

/**
 * Uhrzeit-Form m:ss einer Dauer („0:05“, „12:30“, „60:00“), abgerundet auf volle Sekunden;
 * negative Werte gelten als 0:00. Geteilt von Meditations-Overlay, Mini-Leiste und Benachrichtigung.
 */
fun formatClock(ms: Long): String {
    val (minutes, seconds) = minutesSecondsOf(ms)
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Ganze Minuten und Restsekunden einer Dauer in Millisekunden, abgerundet auf volle Sekunden;
 * negative Werte gelten als 0:00. Gemeinsam für [formatClock], Bilanz und Logbuch („6:05 min“,
 * gesprochen „6 Minuten 5 Sekunden“).
 */
fun minutesSecondsOf(ms: Long): Pair<Int, Int> {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return (totalSeconds / 60L).toInt() to (totalSeconds % 60L).toInt()
}

/**
 * Zahl auf eine Nachkommastelle gerundet, mit deutschem Komma und ohne unnötige Null (TEXT-10):
 * „6“, „4,5“, „4,6“. Für Sekunden mit halben Werten und Atemzüge pro Minute in Domain-Texten.
 */
fun Double.formatDecimal(): String {
    val tenths = (this * TENTHS).roundToInt()
    val whole = tenths / TENTHS
    val rest = abs(tenths % TENTHS)
    return if (rest == 0) whole.toString() else "$whole,$rest"
}

private const val TENTHS = 10

/**
 * Auszeichnung für Sanskrit in Datentexten und Strings (TEXT-14): Ein Begriff steht zwischen
 * Sternchen, klein und in IAST-Umschrift mit Längenzeichen („*laṅghana*“, „*antara kumbhaka*“).
 * Die UI setzt ihn kursiv (`sanskritText` in `ui/components`), TalkBack liest ihn ohne
 * Sternchen. Erkannt wird nur ein Paar um Buchstaben und Leerzeichen; ein einzelnes „*“ bleibt
 * stehen. Längenzeichen stehen vorkomponiert (NFC, „ā“ = U+0101).
 */
val SANSKRIT_SPAN = Regex("""\*(\p{L}\p{M}*(?:[\p{L} ]\p{M}*)*)\*""")

/** Der Text, wie er gelesen wird: ohne die Sternchen der Sanskrit-Auszeichnung ([SANSKRIT_SPAN]). */
fun String.withoutMarks(): String = replace(SANSKRIT_SPAN) { it.groupValues[1] }
