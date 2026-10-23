package app.atemkraft.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.components.CardTitle
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.DotSeparatedFlow
import app.atemkraft.ui.components.ForwardChevron
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.durationClockText
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.rememberFirstLineHeight
import app.atemkraft.ui.components.spokenDuration
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.ALPHA_HEAT_LOW
import app.atemkraft.ui.theme.ALPHA_HEAT_MID
import app.atemkraft.ui.theme.ALPHA_HEAT_NONE
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.SynthTrack
import app.atemkraft.ui.theme.TERTIARY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Familienpunkt vor jedem Eintrag. */
private val FAMILY_DOT = 10.dp

/** Listen-Schlüssel des Satzes unter dem Verlauf. */
private const val CLEAR_HINT_KEY = "clear-hint"

private val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")

/**
 * Logbuch als Push-Seite (aus der Karte „Verlauf“ im Tab „Eigene“ und vom Ende einer
 * Übung): Statistik mit 7-Tage-Übersicht, darunter der Verlauf nach Tagen gruppiert (Heute /
 * Gestern / Diese Woche / je Monat), je Gruppe eine Karte. Der Kopf mit Zurück-Pfeil steht fest über der Liste ([PushHeader]). Leeren liegt
 * bewusst in der Logbuch-Zeile der Einstellungen, nicht hier neben der Statistik; ein Satz unter
 * dem Verlauf sagt, wo (Nutzer 2026-10-24). Ein Eintrag, dessen Übung es noch gibt ([canOpen]),
 * öffnet sie ([onOpenExercise]) – „das von gestern Abend nochmal“. [today] nur für
 * reproduzierbare Screenshots.
 */
@Composable
fun LogbookScreen(
    entries: List<SessionLogEntry>,
    onBack: () -> Unit,
    today: LocalDate = LocalDate.now(),
    canOpen: (exerciseId: String) -> Boolean = { false },
    onOpenExercise: (exerciseId: String) -> Unit = {},
) {
    val zone = ZoneId.systemDefault()
    val groups = remember(entries, today) { groupLogEntries(entries, today, zone) }

    // Fest über der Liste: Der Weg zurück scrollt nie aus dem Bild. Oben SectionGap wie auf jeder
    // Push-Seite.
    TabListScaffold(
        header = { scrolled -> PushHeader(onBack = onBack, title = stringResource(R.string.settings_log_title), scrolled = scrolled) },
        top = Dimens.SectionGap,
    ) {
        item(key = "stats") { LogStatsCard(entries = entries, today = today) }
        // Köpfe reihum nach ihrer Lage in der Liste (FARBE-07).
        itemsIndexed(groups, key = { _, group -> "group-${group.period}" }) { index, group ->
            LogGroupSection(index, group, zone, canOpen, onOpenExercise)
        }
        if (entries.isNotEmpty()) {
            item(key = CLEAR_HINT_KEY) { HintText(stringResource(R.string.log_clear_hint)) }
        }
    }
}

/**
 * Titel „7 Sitzungen · 65 min gesamt“, die Serie als eigene Zeile erst ab zwei Tagen in Folge
 * (ein einzelner Tag ist noch keine Serie – und „keine Serie“ soll nicht entmutigen), darunter
 * die Wochenpunkte. Leer: „Noch keine Sitzung“ und eine Einladung. Auf der Logbuch-Seite; im Tab
 * „Eigene“ führt [LogHistoryCard] hierher.
 */
@Composable
internal fun LogStatsCard(entries: List<SessionLogEntry>, today: LocalDate = LocalDate.now()) {
    val streak = rememberLogStreak(entries, today)
    val minutesPerDay = rememberMinutesPerDay(entries)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            val totalMinutes = entries.sumOf { it.durationMs } / 60_000L
            WholeWordText(
                text = if (entries.isEmpty()) {
                    stringResource(R.string.log_sessions_none)
                } else {
                    pluralStringResource(R.plurals.log_sessions, entries.size, entries.size) +
                        DOT_SEPARATOR + stringResource(R.string.log_minutes_total, totalMinutes)
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (entries.isEmpty()) {
                // Sanfte Einladung statt leerer Karte (MUSTER-04).
                Spacer(Modifier.height(Dimens.GapTiny))
                HintText(stringResource(R.string.log_empty_invite))
            }
            if (streak >= 2) {
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = pluralStringResource(R.plurals.log_streak, streak, streak),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(Dimens.ListGap))
            WeekRow(minutesPerDay = minutesPerDay, today = today)
        }
    }
}

/**
 * Einstieg ins Logbuch oben im Tab „Eigene“ (Nutzer 2026-10-23): Titel „Verlauf“ mit Chevron, die
 * Wochenpunkte wie auf der Logbuch-Seite und eine Zeile „3 Tage in Folge · 45 min diese Woche“
 * (Serie erst ab zwei Tagen wie in [LogStatsCard]; Minuten der sieben gezeigten Tage; fehlt beides,
 * die Zahl der Sitzungen). Die ganze Karte ist der Button zur Logbuch-Seite ([onClick]). Ohne
 * Sitzungen steht statt Punkten und Zeile ein Satz; die Karte bleibt (MUSTER-04).
 *
 * TalkBack hört „Verlauf“ und die Zeile; die Punkte je Tag stehen auf der Logbuch-Seite.
 */
@Composable
internal fun LogHistoryCard(entries: List<SessionLogEntry>, onClick: () -> Unit, today: LocalDate = LocalDate.now()) {
    val streak = rememberLogStreak(entries, today)
    val minutesPerDay = rememberMinutesPerDay(entries)
    val weekMinutes = (0L..6L).sumOf { minutesPerDay[today.minusDays(it).toEpochDay()] ?: 0L }
    // Card(onClick): Ripple in Kartenform, Tippfläche ≥ 48 dp aus der M3-Surface (KOMP-02).
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                CardTitle(stringResource(R.string.own_history), modifier = Modifier.weight(1f))
                ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
            }
            if (entries.isEmpty()) {
                Spacer(Modifier.height(Dimens.GapTiny))
                HintText(stringResource(R.string.own_history_empty))
            } else {
                Spacer(Modifier.height(Dimens.ListGap))
                WeekRow(minutesPerDay = minutesPerDay, today = today, spoken = false)
                Spacer(Modifier.height(Dimens.ListGap))
                val parts = buildList {
                    if (streak >= 2) add(pluralStringResource(R.plurals.log_streak, streak, streak))
                    if (weekMinutes > 0) add(stringResource(R.string.log_minutes_week, weekMinutes))
                    if (isEmpty()) add(pluralStringResource(R.plurals.log_sessions, entries.size, entries.size))
                }
                // Passt die Zeile nicht, steht jeder Teil für sich – keine Zeile beginnt oder endet mit „·“.
                DotSeparatedFlow(
                    parts = parts.map { part -> { SecondaryText(part) } },
                    separator = { SecondaryText(DOT_SEPARATOR) },
                )
            }
        }
    }
}

/**
 * Ruhige 7-Tage-Übersicht: pro Tag ein Punkt, dessen Intensität mit den geübten Minuten
 * wächst (EINE Farbe als Alpha-Rampe – Magnitude, keine Wertung). Bewusst ohne Zahlen und
 * ohne Druck; heute bekommt nur einen sanften Ring. Beschriftung zweibuchstabig („Mo“ … „So“),
 * weil einzelne Buchstaben doppeldeutig sind (M/D/S). Details stehen für TalkBack bereit, außer
 * mit `spoken = false` ([LogHistoryCard]).
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun WeekRow(minutesPerDay: Map<Long, Long>, today: LocalDate, spoken: Boolean = true) {
    val labels = stringArrayResource(R.array.log_weekday_short)
    Row(
        // spoken = false: Die Zeile ist Teil eines Buttons, der sie in Worten zusammenfasst.
        modifier = Modifier.fillMaxWidth().then(if (spoken) Modifier else Modifier.semantics { invisibleToUser() }),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (offset in 6 downTo 0) {
            val day = today.minusDays(offset.toLong())
            val minutes = minutesPerDay[day.toEpochDay()] ?: 0L
            // Sequenzielle Rampe (eine Hue): leer → leise → präsent → voll.
            val fill = when {
                minutes <= 0L -> MaterialTheme.colorScheme.onSurface.copy(alpha = ALPHA_HEAT_NONE)
                minutes < 10L -> MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_HEAT_LOW)
                minutes < 25L -> MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_HEAT_MID)
                else -> MaterialTheme.colorScheme.primary
            }
            val isToday = offset == 0
            val dayName = day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
            val dayText = if (minutes > 0) {
                pluralStringResource(R.plurals.cd_log_day_minutes, minutes.toInt(), dayName, minutes.toInt())
            } else {
                stringResource(R.string.cd_log_day_none, dayName)
            }
            // „Heute“ trägt sonst nur der dünne Ring (FARBE-05): auch in Worten.
            val dayDescription = if (isToday) stringResource(R.string.cd_log_day_today, dayText) else dayText
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics { contentDescription = dayDescription },
            ) {
                Box(
                    modifier = Modifier
                        .size(Sizes.WeekDot)
                        .clip(CircleShape)
                        .background(fill)
                        .then(
                            if (isToday) {
                                Modifier.border(Dimens.BorderThin, SynthTrack.copy(alpha = TERTIARY), CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                )
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = labels[(day.dayOfWeek.value - 1).coerceIn(0, 6)],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY),
                )
            }
        }
    }
}

/**
 * Eine Gruppe des Verlaufs: Abschnittskopf in der Farbe seiner Lage [index] (reihum, FARBE-07),
 * darunter je Tag eine Karte. Heute und Gestern sind ein Tag und brauchen keine Tageszeile; in
 * „Diese Woche“ und den Monaten steht über jeder Karte der Tag („Mi 15.10.“), die Zeilen zeigen
 * nur die Uhrzeit.
 */
@Composable
private fun LogGroupSection(
    index: Int,
    group: LogGroup,
    zone: ZoneId,
    canOpen: (String) -> Boolean,
    onOpenExercise: (String) -> Unit,
) {
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
    CollapsibleSection(title = title, color = headingColor(index)) {
        group.days.forEach { day ->
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
                            if (index > 0) HorizontalDivider(color = DividerSubtle)
                            LogEntryRow(
                                entry = entry,
                                time = Instant.ofEpochMilli(entry.startedAtEpochMs).atZone(zone).format(TIME_FORMAT),
                                onClick = if (canOpen(entry.exerciseId)) ({ onOpenExercise(entry.exerciseId) }) else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Geübte Minuten je Tag (Epochentag → Minuten). */
@Composable
private fun rememberMinutesPerDay(entries: List<SessionLogEntry>): Map<Long, Long> = remember(entries) {
    entries.groupBy { it.startedAtEpochMs.toEpochDay() }
        .mapValues { (_, list) -> list.sumOf { it.durationMs } / 60_000L }
}

/** Tage in Folge bis heute bzw. gestern ([currentStreak]). */
@Composable
private fun rememberLogStreak(entries: List<SessionLogEntry>, today: LocalDate): Int {
    val activeDays = remember(entries) { entries.map { it.startedAtEpochMs.toEpochDay() }.toSet() }
    return remember(activeDays, today) { currentStreak(activeDays, today) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogEntryRow(entry: SessionLogEntry, time: String, onClick: (() -> Unit)?) {
    // Der Punkt gehört zum Namen: oben ausgerichtet und mittig zur ersten Namenszeile, nicht zur
    // Höhe des ganzen Eintrags – sonst schwebt er bei umgebrochener Dauer zwischen den Zeilen.
    val nameStyle = MaterialTheme.typography.titleSmall
    val dotTop = (rememberFirstLineHeight(nameStyle) - FAMILY_DOT) / 2
    // Ein Eintrag ist ein Fokus-Stopp (A11Y-06): Name, Uhrzeit, Dauer und Runden in einer Ansage.
    // Antippbar, wenn es die Übung noch gibt (nicht Meditation, Tagesmuster eines anderen Tages,
    // gelöschte Muster); Name samt Uhrzeit, Dauer und Runden ist die Tippfläche.
    val openLabel = stringResource(R.string.log_open_exercise)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Dimens.CardPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        // Die Familie trägt der Punkt nur in Farbe; im zusammengeführten Eintrag nennt ihn seine
        // Beschreibung (FARBE-05: „A · Herunterregeln, Resonanz-Atmung, …“).
        val familyName = entry.family?.title()
        Box(
            modifier = Modifier
                .padding(top = dotTop.coerceAtLeast(0.dp))
                .size(FAMILY_DOT)
                .clip(CircleShape)
                // Meditation hat keine Familie – eigener, ruhiger Cyan-Ton.
                .background(entry.family?.color() ?: NeonCyan)
                .then(if (familyName != null) Modifier.semantics { contentDescription = familyName } else Modifier),
        )
        // FlowRow: Passt die Dauer nicht mehr neben den Namen (große Schrift), rutscht sie
        // darunter – der Name behält die volle Breite und bricht nicht im Wort (LAYOUT-03).
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
        ) {
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                // Name wörtlich, auch ein eigener mit „*“ (ohne Sanskrit-Auszeichnung, TEXT-14).
                WholeWordText(
                    text = entry.exerciseName,
                    style = nameStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    marks = false,
                )
                SecondaryText(time)
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
                    // Sichtbar „6:05 min“ wie die Bilanz am Sitzungsende (Zahl und Einheit geschützt
                    // verbunden), TalkBack hört „6 Minuten 5 Sekunden“.
                    val spoken = spokenDuration(entry.durationMs)
                    Text(
                        text = durationClockText(entry.durationMs),
                        style = nameStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { contentDescription = spoken },
                    )
                    // „Runden" nur für Atemübungen; Meditationen zeigen nur die Dauer.
                    if (entry.kind == SessionKind.BREATHING) {
                        SecondaryText(pluralStringResource(R.plurals.log_rounds, entry.roundsCompleted, entry.roundsCompleted))
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
