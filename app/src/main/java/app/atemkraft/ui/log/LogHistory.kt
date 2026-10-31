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
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.minutesRounded
import app.atemkraft.ui.components.AppLocale
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ExerciseRow
import app.atemkraft.ui.components.ExerciseRows
import app.atemkraft.ui.components.ExpandCaret
import app.atemkraft.ui.components.ForwardChevron
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.expandedStateText
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.theme.ALPHA_BORDER
import app.atemkraft.ui.theme.ALPHA_HEAT_LOW
import app.atemkraft.ui.theme.ALPHA_HEAT_MID
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.TodayGold
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

private val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.", AppLocale)

/** Datum für TalkBack: „27. Oktober“. */
private val SPOKEN_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM", AppLocale)

/** Kurzes Datum der Wochen-Spanne: „27. Okt“ (eigenständige Monatsform, ohne Abkürzungspunkt). */
private val RANGE_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. LLL", AppLocale)

/** Wochen im aufgeklappten Verlauf (Nutzer 2026-10-31: eine Woche, aufgeklappt bis vier). */
internal const val HISTORY_WEEKS_OPEN = 4

/**
 * Zuletzt geübte Übungen unter der Woche (Eigentümer 2026-10-31): zugeklappt eine – der schnelle
 * Weg „das von gestern nochmal“ –, aufgeklappt mit den vier Wochen [RECENT_OPEN] und darunter
 * „Logbuch ›“. Ein Schalter öffnet den ganzen Verlauf; zugeklappt stehen die Sammlungen ohne
 * Scrollen im Bild.
 */
internal const val RECENT_CLOSED = 1

/** Zuletzt geübte Übungen im aufgeklappten Verlauf (siehe [RECENT_CLOSED]). */
internal const val RECENT_OPEN = 3

/**
 * Verlauf im Tab „Meins“, der erste Eintrag des Tabs ohne eigenen Kopf (STYLEGUIDE §4.3, Tab: der
 * Seitentitel benennt schon): die Kalenderwoche als Punkte ([WeekCalendar], Mo–So, das Datum unter
 * jedem Punkt, darüber „27. Okt – 2. Nov · 45 min“ – die Minuten genau des gezeigten Zeitraums), ein
 * Tipp darauf klappt den ganzen Verlauf auf ([weeksOpen], dauerhaft gemerkt): vier Wochen, statt
 * [RECENT_CLOSED] dann [RECENT_OPEN] zuletzt geübte Übungen und „Logbuch ›“. Direkt unter der Woche
 * die zuletzt geübten Übungen ([recent], neueste zuerst, je einmal) als schlanke Zeilen mit
 * Datum, ohne eigenes Label – das Datum verknüpft sie mit den Punkten; ein Tipp öffnet die
 * Detailseite ([onOpenExercise]). Aufgeklappt am Ende rechtsbündig „Logbuch ›“ ([onOpenLogbook]), dasselbe
 * Verweis-Muster wie „Alle 5 ›“ unter den Karteikarten: Der Verweis steht, wo die Liste weitergeht
 * (Eigentümer 2026-10-31: „viel Text, der gleichzeitig abgrenzt“). Eine Zählung ohne Ziel, große
 * Zahl oder Urteil (TEXT-16); die Serie steht nur im Logbuch. Ohne Sitzungen steht nur der Satz
 * (MUSTER-04) – ohne Verweis, das Logbuch wäre leer.
 *
 * TalkBack hört den Aufklapper als eine Schaltfläche (Spanne, Minuten, Zustand) und je Zeile einen
 * Satz; die Punkte sind hier stumm, die je Tag stehen auf der Logbuch-Seite.
 */
@Composable
internal fun LogHistory(
    entries: List<SessionLogEntry>,
    today: LocalDate = LocalDate.now(),
    weeksOpen: Boolean = false,
    onWeeksOpenChange: (Boolean) -> Unit = {},
    recent: List<Exercise> = emptyList(),
    onOpenExercise: (exerciseId: String) -> Unit = {},
    onOpenLogbook: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (entries.isEmpty()) {
            HintText(stringResource(R.string.own_history_empty))
            return@Column
        }
        WeekCalendar(entries = entries, today = today, expanded = weeksOpen, onExpandedChange = onWeeksOpenChange)
        // Zeilen und Logbuch-Verweis klappen mit der Woche auf (ein Schalter, ein Gleiten).
        Column(modifier = Modifier.fillMaxWidth().animateContentSize(Motion.Disclosure)) {
            val shown = recent.take(if (weeksOpen) RECENT_OPEN else RECENT_CLOSED)
            if (shown.isNotEmpty()) {
                // Woche und Zeilen sind ein Block „Verlauf“: der Listenabstand, kein Abschnittsabstand.
                Spacer(Modifier.height(Dimens.ListGap))
                RecentPractice(recent = shown, entries = entries, onOpenExercise = onOpenExercise)
            }
            if (weeksOpen) {
                Spacer(Modifier.height(Dimens.GapTiny))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    // Bündig: Das Polster des Text-Buttons ragt in den Rand.
                    AppTextButton(onClick = onOpenLogbook, modifier = Modifier.flushEnd()) {
                        ForwardLinkText(text = stringResource(R.string.settings_log_title), color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/**
 * „Zuletzt geübt“: die [ExerciseRow]s frei wie die Woche darüber, ohne Karte ([ExerciseRows] mit
 * `inCard = false`; Eigentümer 2026-10-31: Die Karte zog zwischen Woche und Zuletzt eine Grenze, wo
 * keine hingehört). Hinweis „Di 27.10. · nachmittags · 10 min“ – wann ([lastPracticeWhen]) und wie lange die neueste Sitzung dieser Übung
 * dauerte, auf ganze Minuten gerundet ([minutesRounded]); TalkBack hört die Dauer ausgeschrieben
 * (A11Y-06).
 */
@Composable
private fun RecentPractice(recent: List<Exercise>, entries: List<SessionLogEntry>, onOpenExercise: (String) -> Unit) {
    ExerciseRows(
        inCard = false,
        rows = recent.map { exercise ->
            {
                // Neueste Sitzung dieser Übung (Liste neueste zuerst): Sie liefert „gestern Abend · 10 min“.
                val entry = remember(entries, exercise.id) { entries.firstOrNull { it.exerciseId == exercise.id } }
                val whenText = entry?.let { lastPracticeWhen(it.startedAtEpochMs) }
                val minutes = entry?.let { minutesRounded((it.durationMs / 1_000L).toInt()) } ?: 0
                ExerciseRow(
                    exercise = exercise,
                    onClick = { onOpenExercise(exercise.id) },
                    hint = whenText?.let { stringResource(R.string.own_last_hint, it.first, minutes) },
                    hintDescription = whenText?.let {
                        stringResource(R.string.cd_own_last_hint, it.second, pluralStringResource(R.plurals.cd_duration_minutes, minutes, minutes))
                    },
                ) {
                    ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
                }
            }
        },
    )
}

/**
 * Wann, sichtbar und gesprochen: „Di 27.10. · nachmittags“ bzw. „Dienstag, 27. Oktober, nachmittags“
 * (Eigentümer 2026-10-31: das Datum verknüpft die Zeile mit dem Punkt der Woche darüber). Der Tag
 * steht wie die Tageszeilen im Logbuch (`log_day_label`), die Tageszeit nach [dayPartOf] als Adverb.
 * Die Nacht gehört zum Tag davor: Eine Sitzung Samstag 01:30 steht unter Freitag, „nachts“.
 */
@Composable
private fun lastPracticeWhen(startedAtEpochMs: Long): Pair<String, String> {
    val start = Instant.ofEpochMilli(startedAtEpochMs).atZone(ZoneId.systemDefault())
    val dayPart = dayPartOf(start.hour)
    // 0–4 Uhr: die Nacht des Vortags.
    val day = if (dayPart == DayPart.NIGHT && start.hour < NIGHT_END_HOUR) {
        start.toLocalDate().minusDays(1)
    } else {
        start.toLocalDate()
    }
    val part = stringArrayResource(R.array.own_day_parts)[dayPart.ordinal]
    val weekdayShort = stringArrayResource(R.array.log_weekday_short)[day.dayOfWeek.value - 1]
    val shown = stringResource(R.string.own_recent_when, stringResource(R.string.log_day_label, weekdayShort, day.format(DAY_MONTH_FORMAT)), part)
    val spokenDay = day.dayOfWeek.getDisplayName(TextStyle.FULL, AppLocale) + ", " + day.format(SPOKEN_DATE_FORMAT)
    return shown to stringResource(R.string.cd_own_recent_when, spokenDay, part)
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
 * Ruhige Wochenübersicht in Kalenderwochen Mo–So (Tab „Meins“ und Logbuch, eine Komponente): oben
 * die Spanne „27. Okt – 2. Nov · 45 min“ (Beginn und Ende der gezeigten Wochen, dahinter die
 * Minuten genau dieses Zeitraums, bei 0 keine; Eigentümer 2026-10-31), darunter einmal die
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
    entries: List<SessionLogEntry>,
    today: LocalDate,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = if (expanded) HISTORY_WEEKS_OPEN else 1
    val labels = stringArrayResource(R.array.log_weekday_short)
    val mondays = calendarWeeks(today, weeks)
    val minutesPerDay = rememberMinutesPerDay(entries)
    val first = mondays.last()
    val last = mondays.first().plusDays(DAYS_PER_WEEK - 1L)
    // Minuten genau des gezeigten Zeitraums: erst summiert, dann einmal abgerundet (je Tag gerundet fehlten Minuten).
    val spanMinutes = remember(entries, first, last) {
        entries.filter { LocalDate.ofEpochDay(it.startedAtEpochMs.toEpochDay()) in first..last }.sumOf { it.durationMs } / 60_000L
    }
    val rangeText = stringResource(R.string.log_weeks_range, first.format(RANGE_DAY_FORMAT), last.format(RANGE_DAY_FORMAT))
    val range = if (spanMinutes > 0) stringResource(R.string.log_weeks_range_minutes, rangeText, spanMinutes) else rangeText
    val spokenRange = stringResource(R.string.cd_log_weeks_range, first.format(SPOKEN_DATE_FORMAT), last.format(SPOKEN_DATE_FORMAT))
    val spoken = if (spanMinutes > 0) {
        spokenRange + ", " + pluralStringResource(R.plurals.cd_duration_minutes, spanMinutes.toInt(), spanMinutes.toInt())
    } else {
        spokenRange
    }
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
            SecondaryText(range, modifier = Modifier.weight(1f).semantics { contentDescription = spoken })
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
        isToday -> TodayGold
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

/** Geübte Minuten je Tag (Epochentag → Minuten), nur für die Stufe des Punkts; die Summe eines Zeitraums rechnet [WeekCalendar] aus den Sitzungen. */
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

/**
 * Die Serie „3 Tage in Folge“ in [TodayGold] (Eigentümer 2026-10-31): warm, ohne Lob – nur gezählt.
 * Eine Stelle für Tab „Meins“ und Logbuch, damit sie überall gleich aussieht.
 */
@Composable
internal fun StreakText(streak: Int) {
    Text(
        text = pluralStringResource(R.plurals.log_streak, streak, streak),
        style = MaterialTheme.typography.bodySmall,
        color = TodayGold,
    )
}
