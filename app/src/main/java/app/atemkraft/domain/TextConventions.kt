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
