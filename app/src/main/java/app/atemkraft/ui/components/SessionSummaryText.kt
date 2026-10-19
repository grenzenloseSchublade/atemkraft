package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R

/**
 * Teile der kurzen Bilanz für den Ende-Bildschirm ([FinishedPanel]), z. B. „8 Runden“, „6:24 min“,
 * „60 Atemzüge“ oder nur „10:00 min“. Nur übergebene, echte Werte; null lässt den Teil weg.
 * Zusammengesetzt werden sie mit [DotSeparatedFlow].
 */
@Composable
fun sessionSummaryParts(durationMs: Long, rounds: Int? = null, breaths: Int? = null): List<String> {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return listOfNotNull(
        rounds?.let { pluralStringResource(R.plurals.session_summary_rounds, it, it) },
        stringResource(R.string.session_summary_duration, totalSeconds / 60L, totalSeconds % 60L),
        breaths?.let { pluralStringResource(R.plurals.session_summary_breaths, it, it) },
    )
}

/**
 * Dauer für TalkBack ausgeschrieben: „6 Minuten 5 Sekunden“, „10 Minuten“, „45 Sekunden“ – statt
 * „sechs Doppelpunkt null fünf“ (A11Y-06). Sichtbar bleibt die kurze Form „6:05 min“.
 */
@Composable
fun spokenDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = (totalSeconds / 60L).toInt()
    val seconds = (totalSeconds % 60L).toInt()
    val min = pluralStringResource(R.plurals.cd_duration_minutes, minutes, minutes)
    val sec = pluralStringResource(R.plurals.cd_duration_seconds, seconds, seconds)
    return when {
        seconds == 0 && minutes > 0 -> min
        minutes == 0 -> sec
        else -> stringResource(R.string.cd_duration_min_sec, min, sec)
    }
}

/**
 * Teile nebeneinander, getrennt durch [separator] („·“), jede Zeile für sich zentriert. Passt ein
 * Teil nicht mehr in die Zeile, beginnt er die nächste – und der Trenner entfällt an dieser
 * Stelle: Er steht nur zwischen zwei Teilen derselben Zeile, keine Zeile beginnt oder endet mit
 * „·“ (LAYOUT-03). Gemeinsam für den Kopf der Atem-Session („Name · Runde 3 von 8“) und die
 * Bilanz am Ende.
 */
@Composable
fun DotSeparatedFlow(
    parts: List<@Composable () -> Unit>,
    separator: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(
            { parts.forEach { it() } },
            { repeat((parts.size - 1).coerceAtLeast(0)) { separator() } },
        ),
        modifier = modifier,
    ) { (partMeasurables, separatorMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val partP = partMeasurables.map { it.measure(loose) }
        val sepP = separatorMeasurables.map { it.measure(loose) }
        // Zeilen gierig füllen; ein Trenner kommt nur vor einen Teil, der in derselben Zeile bleibt.
        val lines = mutableListOf<MutableList<Int>>()
        var lineWidth = 0
        partP.forEachIndexed { i, p ->
            val sepW = if (i > 0) sepP[i - 1].width else 0
            if (lines.isEmpty() || lineWidth + sepW + p.width > constraints.maxWidth) {
                lines += mutableListOf(i)
                lineWidth = p.width
            } else {
                lines.last() += i
                lineWidth += sepW + p.width
            }
        }
        fun widthOf(line: List<Int>) = line.sumOf { partP[it].width } + line.drop(1).sumOf { sepP[it - 1].width }
        fun heightOf(line: List<Int>) = line.maxOf { i -> maxOf(partP[i].height, if (i != line.first()) sepP[i - 1].height else 0) }
        val width = (lines.maxOfOrNull(::widthOf) ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = lines.sumOf(::heightOf).coerceAtLeast(constraints.minHeight)
        layout(width, height) {
            var y = 0
            lines.forEach { line ->
                val lineH = heightOf(line)
                var x = (width - widthOf(line)) / 2
                line.forEach { i ->
                    if (i != line.first()) x = placeCentered(sepP[i - 1], x, y, lineH)
                    x = placeCentered(partP[i], x, y, lineH)
                }
                y += lineH
            }
        }
    }
}

/** Setzt [p] bei [x] vertikal mittig in die Zeile ab [y] und gibt das rechte Ende zurück. */
private fun Placeable.PlacementScope.placeCentered(p: Placeable, x: Int, y: Int, lineH: Int): Int {
    p.placeRelative(x, y + (lineH - p.height) / 2)
    return x + p.width
}
