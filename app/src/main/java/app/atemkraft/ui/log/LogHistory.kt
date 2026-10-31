package app.atemkraft.ui.log

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import app.atemkraft.R
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.minutesRounded
import app.atemkraft.ui.components.AppLocale
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.DotSeparatedFlow
import app.atemkraft.ui.components.ExerciseRow
import app.atemkraft.ui.components.ExerciseRows
import app.atemkraft.ui.components.ExpandCaret
import app.atemkraft.ui.components.ForwardChevron
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.expandedStateText
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.theme.ALPHA_BORDER
import app.atemkraft.ui.theme.ALPHA_HEAT_LOW
import app.atemkraft.ui.theme.ALPHA_HEAT_MID
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.SynthTrack
import app.atemkraft.ui.theme.TERTIARY
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

private val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.", AppLocale)

/** Kurzes Datum der Wochen-Spanne: „27. Okt“ (eigenständige Monatsform, ohne Abkürzungspunkt). */
private val RANGE_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. LLL", AppLocale)

/** Wochen im aufgeklappten Verlauf (Nutzer 2026-10-31: eine Woche, aufgeklappt bis vier). */
internal const val HISTORY_WEEKS_OPEN = 4

/**
 * Verlauf im Tab „Eigene“, der erste Eintrag des Tabs ohne eigenen Kopf (STYLEGUIDE §4.3, Tab:
 * der Seitentitel benennt schon; Eigentümer 2026-10-31): die Kalenderwoche als Punkte ([WeekCalendar], Mo–So, das Datum unter jedem Punkt, darüber die
 * Spanne „27. Okt – 2. Nov“), ein Tipp darauf klappt bis zu vier Wochen auf ([weeksOpen], dauerhaft
 * gemerkt, Nutzer 2026-10-31). Darunter die dezente Zeile „3 Tage in Folge · 45 min in den letzten
 * 7 Tagen“ (Serie erst ab zwei Tagen wie in [LogStatsCard]; Minuten der letzten sieben Tage,
 * [recentMinutes]; fehlt beides, die Zahl der Sitzungen) – eine Zählung ohne Ziel, große Zahl oder
 * Urteil (TEXT-16). Rechts in dieser Zeile führt „Logbuch ›“ zur Logbuch-Seite ([onOpenLogbook],
 * dasselbe Verweis-Muster wie „Alle 4 ›“ unter den Karteikarten). Ohne Sitzungen steht statt
 * Punkten und Zeile nur der Satz (MUSTER-04) – ohne Verweis, das Logbuch wäre leer. Ohne
 * [showStreak] fehlt die Serie, die Minuten bleiben.
 *
 * Darunter „Zuletzt geübt“: bis zu drei Übungen ([recent], neueste zuerst, je einmal) als schlanke
 * Zeilen ([ExerciseRow]) mit „gestern Abend · 10 min“; ein Tipp öffnet die Detailseite
 * ([onOpenExercise]) – „das von gestern Abend nochmal“.
 *
 * TalkBack hört den Aufklapper als eine Schaltfläche (Spanne, Zustand), die Zählzeile und je Zeile
 * einen Satz; die Punkte sind hier stumm, die je Tag stehen auf der Logbuch-Seite.
 */
@Composable
internal fun LogHistory(
    entries: List<SessionLogEntry>,
    today: LocalDate = LocalDate.now(),
    showStreak: Boolean = true,
    weeksOpen: Boolean = false,
    onWeeksOpenChange: (Boolean) -> Unit = {},
    recent: List<Exercise> = emptyList(),
    onOpenExercise: (exerciseId: String) -> Unit = {},
    onOpenLogbook: () -> Unit = {},
) {
    val streak = rememberLogStreak(entries, today).takeIf { showStreak } ?: 0
    val minutesPerDay = rememberMinutesPerDay(entries)
    val weekMinutes = remember(entries, today) { recentMinutes(entries, today, ZoneId.systemDefault()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (entries.isEmpty()) {
            HintText(stringResource(R.string.own_history_empty))
        } else {
            WeekCalendar(minutesPerDay = minutesPerDay, today = today, expanded = weeksOpen, onExpandedChange = onWeeksOpenChange)
            Spacer(Modifier.height(Dimens.ListGap))
            val parts = buildList {
                if (streak >= 2) add(pluralStringResource(R.plurals.log_streak, streak, streak))
                if (weekMinutes > 0) add(stringResource(R.string.log_minutes_recent, weekMinutes))
                if (isEmpty()) add(pluralStringResource(R.plurals.log_sessions, entries.size, entries.size))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Passt die Zeile nicht, steht jeder Teil für sich – keine Zeile beginnt oder endet mit „·“.
                DotSeparatedFlow(
                    parts = parts.map { part -> { SecondaryText(part) } },
                    separator = { SecondaryText(DOT_SEPARATOR) },
                    modifier = Modifier.weight(1f),
                    centered = false,
                )
                // Bündig: Das Polster des Text-Buttons ragt in den Rand.
                AppTextButton(onClick = onOpenLogbook, modifier = Modifier.flushEnd()) {
                    ForwardLinkText(text = stringResource(R.string.settings_log_title), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.SectionGap))
            SubLabel(stringResource(R.string.own_recent), color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(Dimens.GapTiny))
            RecentPractice(recent = recent, entries = entries, today = today, onOpenExercise = onOpenExercise)
        }
    }
}

/**
 * „Zuletzt geübt“: bis zu drei [ExerciseRow] in einer Karte ([ExerciseRows]). Hinweis „gestern
 * Abend · 10 min“ – wann ([lastPracticeWhen]) und wie lange die neueste Sitzung dieser Übung
 * dauerte, auf ganze Minuten gerundet ([minutesRounded]); TalkBack hört die Dauer ausgeschrieben
 * (A11Y-06).
 */
@Composable
private fun RecentPractice(recent: List<Exercise>, entries: List<SessionLogEntry>, today: LocalDate, onOpenExercise: (String) -> Unit) {
    ExerciseRows(
        rows = recent.map { exercise ->
            {
                // Neueste Sitzung dieser Übung (Liste neueste zuerst): Sie liefert „gestern Abend · 10 min“.
                val entry = remember(entries, exercise.id) { entries.firstOrNull { it.exerciseId == exercise.id } }
                val whenText = entry?.let { lastPracticeWhen(it.startedAtEpochMs, today) }
                val minutes = entry?.let { minutesRounded((it.durationMs / 1_000L).toInt()) } ?: 0
                ExerciseRow(
                    exercise = exercise,
                    onClick = { onOpenExercise(exercise.id) },
                    hint = whenText?.let { stringResource(R.string.own_last_hint, it, minutes) },
                    hintDescription = whenText?.let {
                        stringResource(R.string.cd_own_last_hint, it, pluralStringResource(R.plurals.cd_duration_minutes, minutes, minutes))
                    },
                ) {
                    ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
                }
            }
        },
    )
}

/**
 * Wann, in Laiensprache: „heute Morgen“, „gestern Abend“, in den letzten 7 Tagen „am
 * Donnerstagnachmittag“ (Wochentag und Tageszeit als ein Wort), davor „am 15.10.“. Tage wie die
 * Gruppen im Logbuch ([periodOf]), Tageszeit nach [dayPartOf].
 *
 * Die Nacht gehört zum Abend davor: Eine Sitzung Samstag 01:30 ist am Samstag „gestern Nacht“ (die
 * Nacht Fr→Sa) und in der Woche „in der Freitagnacht“ – „die Nacht“ ist weiblich, „am …nacht“
 * wäre falsch. Das Datum bleibt der Kalendertag des Beginns.
 */
@Composable
private fun lastPracticeWhen(startedAtEpochMs: Long, today: LocalDate): String {
    val start = Instant.ofEpochMilli(startedAtEpochMs).atZone(ZoneId.systemDefault())
    val dayPart = dayPartOf(start.hour)
    // 0–4 Uhr: die Nacht des Vortags.
    val day = if (dayPart == DayPart.NIGHT && start.hour < NIGHT_END_HOUR) {
        start.toLocalDate().minusDays(1)
    } else {
        start.toLocalDate()
    }
    val part = stringArrayResource(R.array.own_day_parts)[dayPart.ordinal]
    return when (periodOf(day, today)) {
        LogPeriod.Today -> stringResource(R.string.own_last_today, part)

        LogPeriod.Yesterday -> stringResource(R.string.own_last_yesterday, part)

        LogPeriod.LastSevenDays -> {
            val weekday = day.dayOfWeek.getDisplayName(TextStyle.FULL, AppLocale)
            if (dayPart == DayPart.NIGHT) {
                stringResource(R.string.own_last_weekday_night, weekday)
            } else {
                stringResource(R.string.own_last_weekday, weekday, part.lowercase(AppLocale))
            }
        }

        is LogPeriod.Month -> stringResource(R.string.own_last_date, start.toLocalDate().format(DAY_MONTH_FORMAT))
    }
}

/**
 * Montag der Kalenderwochen, die der Verlauf zeigt: die laufende Woche zuerst, dann [weeks] − 1
 * davor (Nutzer 2026-10-31: Kalenderwochen Mo–So statt gleitender sieben Tage – Beginn und Ende
 * sind so eindeutig, die Spalten wandern nicht täglich).
 */
internal fun calendarWeeks(today: LocalDate, weeks: Int): List<LocalDate> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return List(weeks) { monday.minusWeeks(it.toLong()) }
}

/**
 * Ruhige Wochenübersicht in Kalenderwochen Mo–So (Tab „Eigene“ und Logbuch, eine Komponente): oben
 * die Spanne „27. Okt – 2. Nov“ (Beginn und Ende der gezeigten Wochen), darunter einmal die
 * Wochentage, dann je Woche eine Reihe – die laufende oben. Pro Tag ein Punkt (`WeekDot`), dessen
 * Intensität mit den geübten Minuten wächst (EINE Farbe als Alpha-Rampe – Magnitude, keine
 * Wertung), darunter der Tag des Monats. Ein Tag ohne Übung ist ein leerer Ring, heute trägt den
 * Ring in Cyan, Tage nach heute nur einen leisen Ring (`ALPHA_BORDER`). Die sieben Tage teilen sich
 * die volle Breite in gleiche Spalten.
 *
 * Der ganze Block ist ein Aufklapper (Role.Button, Zustand per `stateDescription`, Caret am Ende
 * der Spannen-Zeile): eine Woche oder [HISTORY_WEEKS_OPEN] Wochen, mit `Motion.Disclosure`. Tab
 * „Eigene“ und Logbuch zeigen ihn gleich und teilen den gemerkten Zustand ([expanded]) – kein
 * Ort bekommt eine abgespeckte Fassung (Eigentümer 2026-10-31: Das Logbuch ließ sich nicht
 * aufklappen). TalkBack hört Spanne und Zustand; die Punkte sind stumm (28 Stopps wären Rauschen).
 */
@Composable
internal fun WeekCalendar(
    minutesPerDay: Map<Long, Long>,
    today: LocalDate,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = if (expanded) HISTORY_WEEKS_OPEN else 1
    val labels = stringArrayResource(R.array.log_weekday_short)
    val mondays = calendarWeeks(today, weeks)
    val range = stringResource(
        R.string.log_weeks_range,
        mondays.last().format(RANGE_DAY_FORMAT),
        mondays.first().plusDays(DAYS_PER_WEEK - 1L).format(RANGE_DAY_FORMAT),
    )
    val state = expandedStateText(expanded)
    val toggle = Modifier
        .clickable(role = Role.Button, onClick = { onExpandedChange(!expanded) })
        .semantics { stateDescription = state }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(toggle)
            .animateContentSize(Motion.Disclosure),
    ) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.IconInButton), verticalAlignment = Alignment.CenterVertically) {
            SecondaryText(range, modifier = Modifier.weight(1f))
            ExpandCaret(expanded = expanded, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY), size = Sizes.IconInButton)
        }
        Spacer(Modifier.height(Dimens.GapSmall))
        // Gitter stumm: Die Spanne sagt, was zu sehen ist; 28 Stopps wären Rauschen.
        Column(
            modifier = Modifier.semantics { hideFromAccessibility() },
            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).semantics { hideFromAccessibility() },
                    )
                }
            }
            mondays.forEach { monday ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until DAYS_PER_WEEK) {
                        val day = monday.plusDays(i.toLong())
                        DayCell(day = day, today = today, minutes = minutesPerDay[day.toEpochDay()] ?: 0L, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Ein Tag der [WeekCalendar]: Punkt, darunter der Tag des Monats; für TalkBack Wochentag und Minuten. */
@Composable
private fun DayCell(day: LocalDate, today: LocalDate, minutes: Long, modifier: Modifier = Modifier) {
    val future = day.isAfter(today)
    // Sequenzielle Rampe (eine Hue): leise → präsent → voll, jede Stufe ≥ 3:1 zum Grund.
    // Ohne Eintrag kein Punkt, sondern ein leerer Ring: Die Form trennt „nichts“ von „wenig“.
    val fill = when {
        minutes <= 0L -> null
        minutes < 10L -> MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_HEAT_LOW)
        minutes < 25L -> MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_HEAT_MID)
        else -> MaterialTheme.colorScheme.primary
    }
    val isToday = day == today
    // Heute trägt den Cyan-Ring (auch um einen leeren Tag); die Zukunft nur einen leisen Ring.
    val ring = when {
        isToday -> SynthTrack.copy(alpha = TERTIARY)
        future -> MaterialTheme.colorScheme.onSurface.copy(alpha = ALPHA_BORDER)
        fill == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY)
        else -> null
    }
    val dayName = day.dayOfWeek.getDisplayName(TextStyle.FULL, AppLocale)
    val dayText = when {
        future -> null
        minutes > 0 -> pluralStringResource(R.plurals.cd_log_day_minutes, minutes.toInt(), dayName, minutes.toInt())
        else -> stringResource(R.string.cd_log_day_none, dayName)
    }
    // „Heute“ trägt sonst nur der dünne Ring (FARBE-05): auch in Worten. Kommende Tage sind still.
    val dayDescription = if (isToday && dayText != null) stringResource(R.string.cd_log_day_today, dayText) else dayText
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics {
            if (dayDescription != null) contentDescription = dayDescription else hideFromAccessibility()
        },
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.WeekDot)
                .clip(CircleShape)
                .then(if (fill != null) Modifier.background(fill) else Modifier)
                .then(if (ring != null) Modifier.border(Dimens.BorderThin, ring, CircleShape) else Modifier),
        )
        Spacer(Modifier.height(Dimens.GapTiny))
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isToday) SECONDARY else TERTIARY),
        )
    }
}

private const val DAYS_PER_WEEK = 7

private fun Long.toEpochDay(): Long = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** Geübte Minuten je Tag (Epochentag → Minuten), nur für die Stufe des Punkts; die Summe rechnet [recentMinutes]. */
@Composable
internal fun rememberMinutesPerDay(entries: List<SessionLogEntry>): Map<Long, Long> = remember(entries) {
    entries.groupBy { it.startedAtEpochMs.toEpochDay() }
        .mapValues { (_, list) -> list.sumOf { it.durationMs } / 60_000L }
}

/** Tage in Folge bis heute bzw. gestern ([currentStreak]). */
@Composable
internal fun rememberLogStreak(entries: List<SessionLogEntry>, today: LocalDate): Int {
    val activeDays = remember(entries) { entries.map { it.startedAtEpochMs.toEpochDay() }.toSet() }
    return remember(activeDays, today) { currentStreak(activeDays, today) }
}

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
