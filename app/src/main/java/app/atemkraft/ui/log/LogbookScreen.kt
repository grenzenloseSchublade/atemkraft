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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.minutesRounded
import app.atemkraft.ui.components.AppLocale
import app.atemkraft.ui.components.CardTitle
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.DotSeparatedFlow
import app.atemkraft.ui.components.ForwardChevron
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.components.Sticky
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.declareSticky
import app.atemkraft.ui.components.durationClockText
import app.atemkraft.ui.components.feelingText
import app.atemkraft.ui.components.rememberFirstLineHeight
import app.atemkraft.ui.components.spokenDuration
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.ALPHA_HEAT_LOW
import app.atemkraft.ui.theme.ALPHA_HEAT_MID
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.SynthTrack
import app.atemkraft.ui.theme.TERTIARY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", AppLocale)

/** Listen-Schlüssel des Satzes unter dem Verlauf. */
private const val CLEAR_HINT_KEY = "clear-hint"

private val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.", AppLocale)

/**
 * Logbuch als Push-Seite (aus der Karte „Verlauf“ im Tab „Eigene“ und vom Ende einer
 * Übung): Statistik mit der Kalenderwoche, darunter der Verlauf nach Tagen gruppiert (Heute /
 * Gestern / Letzte 7 Tage / je Monat), je Gruppe eine Karte. Der Kopf mit Zurück-Pfeil steht fest über der Liste ([PushHeader]). Leeren liegt
 * bewusst in der Logbuch-Zeile der Einstellungen, nicht hier neben der Statistik; ein Satz unter
 * dem Verlauf sagt, wo (Nutzer 2026-10-24). Ein Eintrag, dessen Übung es noch gibt ([canOpen]),
 * öffnet sie ([onOpenExercise]) – „das von gestern Abend nochmal“. [showStreak]: Einstellung
 * „Serie zeigen“ (aus = keine Tage in Folge). [today] nur für reproduzierbare Screenshots.
 */
@Composable
fun LogbookScreen(
    entries: List<SessionLogEntry>,
    onBack: () -> Unit,
    today: LocalDate = LocalDate.now(),
    canOpen: (exerciseId: String) -> Boolean = { false },
    onOpenExercise: (exerciseId: String) -> Unit = {},
    showStreak: Boolean = true,
) {
    val zone = ZoneId.systemDefault()
    val groups = remember(entries, today) { groupLogEntries(entries, today, zone) }

    // Fest über der Liste: Der Weg zurück scrollt nie aus dem Bild. Oben SectionGap, unten Platz für
    // den Griff der Tab-Leiste wie auf jeder Push-Seite.
    TabListScaffold(
        header = { PushHeader(onBack = onBack, title = stringResource(R.string.settings_log_title)) },
        top = Dimens.SectionGap,
        bottom = Dimens.TabBarGripClearance,
    ) {
        item(key = "stats") { LogStatsCard(entries = entries, today = today, showStreak = showStreak) }
        groups.forEach { group ->
            // Der Kopf der Gruppe klebt; angemeldet hier, sein Inhalt kommt aus LogGroupSection.
            declareSticky(logGroupSticky(group))
            item(key = logGroupSticky(group).key) { LogGroupSection(group, zone, canOpen, onOpenExercise) }
        }
        if (entries.isNotEmpty()) {
            item(key = CLEAR_HINT_KEY) { HintText(stringResource(R.string.log_clear_hint)) }
        }
    }
}

/**
 * Titel „7 Sitzungen · 65 min gesamt“, die Serie als eigene Zeile erst ab zwei Tagen in Folge
 * (ein einzelner Tag ist noch keine Serie – und „keine Serie“ soll nicht entmutigen), darunter
 * die Kalenderwoche ([WeekCalendar], dieselbe wie im Tab „Eigene“). Leer: „Noch keine Sitzung“
 * und eine Einladung. Auf der Logbuch-Seite; im Tab
 * „Eigene“ führt der Kopf „Verlauf“ über [LogHistory] hierher. Ohne [showStreak] (Einstellung
 * „Serie zeigen“ aus) fehlt die Serie ganz.
 */
@Composable
internal fun LogStatsCard(entries: List<SessionLogEntry>, today: LocalDate = LocalDate.now(), showStreak: Boolean = true) {
    val streak = rememberLogStreak(entries, today).takeIf { showStreak } ?: 0
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
            WeekCalendar(minutesPerDay = minutesPerDay, today = today, weeks = 1, spoken = true)
        }
    }
}

/** Kopf einer Gruppe: klebt (Ebene 1), ihr Listeneintrag hat denselben Schlüssel. */
private fun logGroupSticky(group: LogGroup) = Sticky("group-${group.period}", level = 1)

/**
 * Eine Gruppe des Verlaufs: Abschnittskopf, darunter je Tag eine Karte. Heute und Gestern sind ein Tag und brauchen keine Tageszeile; in
 * „Letzte 7 Tage“ und den Monaten steht über jeder Karte der Tag („Mi 15.10.“), die Zeilen zeigen
 * nur die Uhrzeit.
 */
@Composable
private fun LogGroupSection(
    group: LogGroup,
    zone: ZoneId,
    canOpen: (String) -> Boolean,
    onOpenExercise: (String) -> Unit,
) {
    val weekdays = stringArrayResource(R.array.log_weekday_short)
    val title = when (val period = group.period) {
        LogPeriod.Today -> stringResource(R.string.log_group_today)

        LogPeriod.Yesterday -> stringResource(R.string.log_group_yesterday)

        LogPeriod.LastSevenDays -> stringResource(R.string.log_group_recent)

        is LogPeriod.Month -> stringResource(
            R.string.log_group_month,
            period.month.month.getDisplayName(TextStyle.FULL, AppLocale),
            period.month.year,
        )
    }
    val showDay = group.period !is LogPeriod.Today && group.period !is LogPeriod.Yesterday
    // Der Kopf der Gruppe klebt, solange man in ihr liest (Eintrag "group-…", siehe Liste).
    CollapsibleSection(title = title, sticky = logGroupSticky(group)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogEntryRow(entry: SessionLogEntry, time: String, onClick: (() -> Unit)?) {
    // Der Punkt gehört zum Namen: oben ausgerichtet und mittig zur ersten Namenszeile, nicht zur
    // Höhe des ganzen Eintrags – sonst schwebt er bei umgebrochener Dauer zwischen den Zeilen.
    val nameStyle = MaterialTheme.typography.titleSmall
    val dotTop = (rememberFirstLineHeight(nameStyle) - Sizes.FamilyDot) / 2
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
                .size(Sizes.FamilyDot)
                .clip(CircleShape)
                // Meditation hat keine Familie – Cyan wie der Zeitring der Meditation (secondary).
                .background(entry.family?.color() ?: MaterialTheme.colorScheme.secondary)
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
                // Befinden danach (SEC-LEGAL-04): nur das Wort, gedämpft, ohne Farbe, Icon oder Summe.
                entry.feeling?.let { feeling ->
                    val word = feelingText(feeling)
                    val spoken = stringResource(R.string.log_feeling_spoken, word)
                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY),
                        modifier = Modifier.semantics { contentDescription = spoken },
                    )
                }
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
                    // „Runden“ nur für Atemübungen; Meditationen zeigen nur die Dauer.
                    if (entry.kind == SessionKind.BREATHING) {
                        SecondaryText(pluralStringResource(R.plurals.log_rounds, entry.roundsCompleted, entry.roundsCompleted))
                    }
                }
            }
        }
    }
}
