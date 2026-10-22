package app.atemkraft.domain

/**
 * Trennpunkt zwischen Teilen einer Zeile („Einatmen 4 s · Ausatmen 6 s“, „5 Sitzungen · 40 min“).
 * Das geschützte Leerzeichen vor dem Punkt hält ihn am Zeilenende: Bei großer Schrift beginnt
 * eine Zeile nie mit „·“ (LAYOUT-03). Liegt in domain/, weil auch der Mustergenerator Zeilen
 * baut; String-Ressourcen und Datentexte (`data/`) schreiben dasselbe als `\u00A0· `
 * (ContentRulesTest `dot-separator`).
 */
const val DOT_SEPARATOR = " · "

/**
 * Uhrzeit-Form m:ss einer Dauer („0:05“, „12:30“, „60:00“), abgerundet auf volle Sekunden;
 * negative Werte gelten als 0:00. Geteilt von Meditations-Overlay, Mini-Leiste und Benachrichtigung.
 */
fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

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
