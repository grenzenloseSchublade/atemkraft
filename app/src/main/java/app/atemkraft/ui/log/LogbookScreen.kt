package app.atemkraft.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.home.color
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SynthTrack
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Familienpunkt vor jedem Eintrag. */
private val FAMILY_DOT = 10.dp

private val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")

/**
 * Logbuch-Tab: Statistik mit 7-Tage-Übersicht, darunter der Verlauf nach Tagen gruppiert
 * (Heute / Gestern / Diese Woche / je Monat), je Gruppe eine Karte. Leeren liegt bewusst in
 * den Einstellungen, nicht hier neben der Statistik. [today] nur für reproduzierbare Screenshots.
 */
@Composable
fun LogbookScreen(
    entries: List<SessionLogEntry>,
    onOpenSettings: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val zone = ZoneId.systemDefault()
    val activeDays = remember(entries) { entries.map { it.startedAtEpochMs.toEpochDay() }.toSet() }
    val minutesPerDay = remember(entries) {
        entries.groupBy { it.startedAtEpochMs.toEpochDay() }
            .mapValues { (_, list) -> list.sumOf { it.durationMs } / 60_000L }
    }
    val streak = remember(activeDays, today) { currentStreak(activeDays, today) }
    val groups = remember(entries, today) { groupLogEntries(entries, today, zone) }

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
                ScreenHeader(
                    title = stringResource(R.string.tab_logbook),
                    subtitle = stringResource(R.string.log_subtitle),
                    action = { SettingsAction(onClick = onOpenSettings) },
                )
                // Titel und Statistik gehören zusammen – kleine Lücke statt Abschnitts-Abstand.
                Spacer(Modifier.height(Dimens.GapSmall))
                StatsCard(entries = entries, streak = streak, minutesPerDay = minutesPerDay, today = today)
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

            items(groups, key = { "group-${it.period}" }) { group -> LogGroupSection(group, zone) }
            item { Spacer(Modifier.height(Dimens.ScreenBottom)) }
        }
    }
}

/**
 * Titel „7 Sitzungen · 65 min gesamt“, die Serie als eigene Zeile erst ab zwei Tagen in Folge
 * (ein einzelner Tag ist noch keine Serie – und „keine Serie“ soll nicht entmutigen), darunter
 * die Wochenpunkte. Leer: „Noch keine Sitzung“.
 */
@Composable
private fun StatsCard(
    entries: List<SessionLogEntry>,
    streak: Int,
    minutesPerDay: Map<Long, Long>,
    today: LocalDate,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            val totalMinutes = entries.sumOf { it.durationMs } / 60_000L
            WholeWordText(
                text = if (entries.isEmpty()) {
                    stringResource(R.string.log_sessions_none)
                } else {
                    pluralStringResource(R.plurals.log_sessions, entries.size, entries.size) +
                        " · " + stringResource(R.string.log_minutes_total, totalMinutes)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (streak >= 2) {
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = pluralStringResource(R.plurals.log_streak, streak, streak),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(Dimens.ListGap))
            WeekRow(minutesPerDay = minutesPerDay, today = today)
        }
    }
}

/**
 * Ruhige 7-Tage-Übersicht: pro Tag ein Punkt, dessen Intensität mit den geübten Minuten
 * wächst (EINE Farbe als Alpha-Rampe – Magnitude, keine Wertung). Bewusst ohne Zahlen und
 * ohne Druck; heute bekommt nur einen sanften Ring. Beschriftung zweibuchstabig („Mo“ … „So“),
 * weil einzelne Buchstaben doppeldeutig sind (M/D/S). Details stehen für TalkBack bereit.
 */
@Composable
private fun WeekRow(minutesPerDay: Map<Long, Long>, today: LocalDate) {
    val labels = stringArrayResource(R.array.log_weekday_short)
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
            val dayDescription = if (minutes > 0) {
                pluralStringResource(R.plurals.cd_log_day_minutes, minutes.toInt(), dayName, minutes.toInt())
            } else {
                stringResource(R.string.cd_log_day_none, dayName)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics { contentDescription = dayDescription },
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

/**
 * Eine Gruppe des Verlaufs: Abschnittskopf in `primary` (wie Glossar/Situationen), darunter je Tag
 * eine Karte. Heute und Gestern sind ein Tag und brauchen keine Tageszeile; in „Diese Woche“ und
 * den Monaten steht über jeder Karte der Tag („Mi 15.10.“), die Zeilen zeigen nur die Uhrzeit.
 */
@Composable
private fun LogGroupSection(group: LogGroup, zone: ZoneId) {
    val weekdays = stringArrayResource(R.array.log_weekday_short)
    val title = when (val period = group.period) {
        LogPeriod.Today -> stringResource(R.string.log_group_today)

        LogPeriod.Yesterday -> stringResource(R.string.log_group_yesterday)

        LogPeriod.ThisWeek -> stringResource(R.string.log_group_week)

        is LogPeriod.Month -> stringResource(
            R.string.log_group_month,
            period.month.month.getDisplayName(TextStyle.FULL, Locale.GERMAN),
            period.month.year,
        )
    }
    val showDay = group.period !is LogPeriod.Today && group.period !is LogPeriod.Yesterday
    Column {
        SectionHeader(title = title, color = MaterialTheme.colorScheme.primary)
        group.days.forEachIndexed { dayIndex, day ->
            if (dayIndex > 0) Spacer(Modifier.height(Dimens.ListGap))
            Column {
                if (showDay) {
                    SubLabel(
                        stringResource(
                            R.string.log_day_label,
                            weekdays[(day.date.dayOfWeek.value - 1).coerceIn(0, 6)],
                            day.date.format(DAY_MONTH_FORMAT),
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    )
                    Spacer(Modifier.height(Dimens.GapTiny))
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = Dimens.CardPadding)) {
                        day.entries.forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider()
                            LogEntryRow(
                                entry = entry,
                                time = Instant.ofEpochMilli(entry.startedAtEpochMs).atZone(zone).format(TIME_FORMAT),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogEntryRow(entry: SessionLogEntry, time: String) {
    // Der Punkt gehört zum Namen: oben ausgerichtet und mittig zur ersten Namenszeile, nicht zur
    // Höhe des ganzen Eintrags – sonst schwebt er bei umgebrochener Dauer zwischen den Zeilen.
    val nameStyle = MaterialTheme.typography.titleMedium
    val dotTop = with(LocalDensity.current) { (nameStyle.lineHeight.toDp() - FAMILY_DOT) / 2 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.CardPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        Box(
            modifier = Modifier
                .padding(top = dotTop.coerceAtLeast(0.dp))
                .size(FAMILY_DOT)
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
                    style = nameStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            // weight: füllt den Rest der Zeile, damit die Dauer auch umgebrochen rechtsbündig steht.
            // FlowRow entscheidet den Umbruch eines gewichteten Kinds nach seiner min-intrinsischen
            // Breite (nur das längste Wort); IntrinsicSize.Max macht „8 Runden“ als Ganzes zur
            // Mindestbreite, sonst bliebe die Spalte daneben und bräche in „8“ / „Runden“.
            Box(
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Column(modifier = Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDuration(entry.durationMs),
                        style = nameStyle,
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
private fun currentStreak(activeDays: Set<Long>, today: LocalDate): Int {
    if (activeDays.isEmpty()) return 0
    val todayEpoch = today.toEpochDay()
    var day = if (todayEpoch in activeDays) todayEpoch else todayEpoch - 1
    var streak = 0
    while (day in activeDays) {
        streak++
        day--
    }
    return streak
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
