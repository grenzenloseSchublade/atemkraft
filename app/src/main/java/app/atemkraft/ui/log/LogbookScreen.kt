package app.atemkraft.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.home.color
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** Logbuch-Tab: Streak, 7-Tage-Übersicht und Verlauf der abgeschlossenen Sessions. */
@Composable
fun LogbookScreen(
    entries: List<SessionLogEntry>,
    onClear: () -> Unit,
) {
    val activeDays = remember(entries) { entries.map { it.startedAtEpochMs.toEpochDay() }.toSet() }
    val streak = remember(activeDays) { currentStreak(activeDays) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.tab_logbook),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                StatsCard(entries = entries, streak = streak, activeDays = activeDays, onClear = onClear)
                Spacer(Modifier.height(4.dp))
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

            items(entries, key = { it.startedAtEpochMs }) { entry -> LogEntryCard(entry) }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun StatsCard(
    entries: List<SessionLogEntry>,
    streak: Int,
    activeDays: Set<Long>,
    onClear: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (streak > 0) pluralStringResource(R.plurals.log_streak, streak, streak)
                else stringResource(R.string.log_streak_none),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (streak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            WeekRow(activeDays = activeDays)
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val totalMinutes = entries.sumOf { it.durationMs } / 60000L
                    Text(
                        text = pluralStringResource(R.plurals.log_sessions, entries.size, entries.size) +
                            " · " + stringResource(R.string.log_minutes_total, totalMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                    )
                    TextButton(onClick = onClear) { Text(stringResource(R.string.log_clear)) }
                }
            }
        }
    }
}

@Composable
private fun WeekRow(activeDays: Set<Long>) {
    val today = LocalDate.now()
    val labels = listOf("M", "D", "M", "D", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (offset in 6 downTo 0) {
            val day = today.minusDays(offset.toLong())
            val active = day.toEpochDay() in activeDays
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = labels[(day.dayOfWeek.value - 1).coerceIn(0, 6)],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: SessionLogEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    // Meditation hat keine Familie – eigener, ruhiger Cyan-Ton.
                    .background(entry.family?.color() ?: NeonCyan),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
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
            Column(horizontalAlignment = Alignment.End) {
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

private fun Long.toEpochDay(): Long =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

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

private fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
