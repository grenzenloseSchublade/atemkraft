package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.minutesSecondsOf

/**
 * Sichtbare Dauer „6:05 min“ im Logbuch (abgerundet auf volle Sekunden, negativ = 0:00). Über die
 * String-Ressource, nicht [app.atemkraft.domain.formatClock]: Die Einheit gehört in die Übersetzung
 * (TEXT-03).
 */
@Composable
fun durationClockText(durationMs: Long): String {
    val (minutes, seconds) = minutesSecondsOf(durationMs)
    return stringResource(R.string.session_summary_duration, minutes, seconds)
}

/**
 * Dauer für TalkBack ausgeschrieben: „6 Minuten 5 Sekunden“, „10 Minuten“, „45 Sekunden“ – statt
 * „sechs Doppelpunkt null fünf“ (A11Y-06). Sichtbar bleibt die kurze Form „6:05 min“.
 */
@Composable
fun spokenDuration(durationMs: Long): String {
    val (minutes, seconds) = minutesSecondsOf(durationMs)
    val min = pluralStringResource(R.plurals.cd_duration_minutes, minutes, minutes)
    val sec = pluralStringResource(R.plurals.cd_duration_seconds, seconds, seconds)
    return when {
        seconds == 0 && minutes > 0 -> min
        minutes == 0 -> sec
        else -> stringResource(R.string.cd_duration_min_sec, min, sec)
    }
}
