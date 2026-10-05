package app.atemkraft.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.home.color
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SynthTrack
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** Logbuch-Tab: Streak, 7-Tage-Übersicht und Verlauf der abgeschlossenen Sessions. */
@Composable
fun LogbookScreen(
    entries: List<SessionLogEntry>,
    onClear: () -> Unit,
) {
    val activeDays = remember(entries) { entries.map { it.startedAtEpochMs.toEpochDay() }.toSet() }
    val minutesPerDay = remember(entries) {
        entries.groupBy { it.startedAtEpochMs.toEpochDay() }
            .mapValues { (_, list) -> list.sumOf { it.durationMs } / 60_000L }
    }
    val streak = remember(activeDays) { currentStreak(activeDays) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            item {
                ScreenHeader(title = stringResource(R.string.tab_logbook))
                // Titel und Statistik gehören zusammen – kleine Lücke statt Abschnitts-Abstand.
                Spacer(Modifier.height(Dimens.GapSmall))
                StatsCard(entries = entries, streak = streak, minutesPerDay = minutesPerDay, onClear = onClear)
                // Zusammen mit dem ListGap ergibt das den SectionGap zur Verlaufsliste.
                Spacer(Modifier.height(Dimens.GapTiny))
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.log_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    )
                }
            }

            items(entries, key = { it.id }) { entry -> LogEntryCard(entry) }
            item { Spacer(Modifier.height(Dimens.ScreenBottom)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsCard(
    entries: List<SessionLogEntry>,
    streak: Int,
    minutesPerDay: Map<Long, Long>,
    onClear: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Text(
                text = if (streak > 0) {
                    pluralStringResource(R.plurals.log_streak, streak, streak)
                } else {
                    stringResource(R.string.log_streak_none)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (streak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.ListGap))
            WeekRow(minutesPerDay = minutesPerDay)
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.ListGap))
                // FlowRow: Reicht der Platz nicht (große Schrift), rutscht „Logbuch leeren“ in die
                // nächste Zeile, statt Buchstabe für Buchstabe umzubrechen (LAYOUT-03).
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val totalMinutes = entries.sumOf { it.durationMs } / 60000L
                    Text(
                        text = pluralStringResource(R.plurals.log_sessions, entries.size, entries.size) +
                            " · " + stringResource(R.string.log_minutes_total, totalMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    AppTextButton(onClick = onClear) { Text(stringResource(R.string.log_clear)) }
                }
            }
        }
    }
}

/**
 * Ruhige 7-Tage-Übersicht: pro Tag ein Punkt, dessen Intensität mit den geübten Minuten
 * wächst (EINE Farbe als Alpha-Rampe – Magnitude, keine Wertung). Bewusst ohne Zahlen und
 * ohne Druck; heute bekommt nur einen sanften Ring. Details stehen für TalkBack bereit.
 */
@Composable
private fun WeekRow(minutesPerDay: Map<Long, Long>) {
    val today = LocalDate.now()
    val labels = listOf("M", "D", "M", "D", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (offset in 6 downTo 0) {
            val day = today.minusDays(offset.toLong())
            val minutes = minutesPerDay[day.toEpochDay()] ?: 0L
            // Sequenzielle Rampe (eine Hue): leer → leise → präsent → voll.
            val fill = when {
                minutes <= 0L -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                minutes < 10L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                minutes < 25L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                else -> MaterialTheme.colorScheme.primary
            }
            val isToday = offset == 0
            val dayName = day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics {
                    contentDescription = if (minutes > 0) {
                        "$dayName: $minutes Minuten geübt"
                    } else {
                        "$dayName: keine Übung"
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .then(
                            if (isToday) {
                                Modifier.border(1.dp, SynthTrack.copy(alpha = 0.55f), CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                )
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = labels[(day.dayOfWeek.value - 1).coerceIn(0, 6)],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogEntryCard(entry: SessionLogEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    // Meditation hat keine Familie – eigener, ruhiger Cyan-Ton.
                    .background(entry.family?.color() ?: NeonCyan),
            )
            // FlowRow: Passt die Dauer nicht mehr neben den Namen (große Schrift), rutscht sie
            // darunter – der Name behält die volle Breite und bricht nicht im Wort (LAYOUT-03).
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
            ) {
                Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                    WholeWordText(
                        text = entry.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formatDateTime(entry.startedAtEpochMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = formatDuration(entry.durationMs),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    // „Runden" nur für Atemübungen; Meditationen zeigen nur die Dauer.
                    if (entry.kind == SessionKind.BREATHING) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.log_rounds,
                                entry.roundsCompleted,
                                entry.roundsCompleted,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                        )
                    }
                }
            }
        }
    }
}

private fun Long.toEpochDay(): Long = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** Aufeinanderfolgende Tage mit ≥1 Session, von heute (oder gestern) rückwärts. */
private fun currentStreak(activeDays: Set<Long>): Int {
    if (activeDays.isEmpty()) return 0
    val today = LocalDate.now().toEpochDay()
    var day = if (today in activeDays) today else today - 1
    var streak = 0
    while (day in activeDays) {
        streak++
        day--
    }
    return streak
}

private fun formatDateTime(epochMs: Long): String = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
