package app.atemkraft.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.atemkraft.R
import app.atemkraft.domain.ExerciseFilter
import app.atemkraft.domain.ExerciseFilters
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ChoiceChip
import app.atemkraft.ui.components.FilterChipRow
import app.atemkraft.ui.components.SecondaryText
import app.atemkraft.ui.theme.Dimens

/**
 * Filterreihe über der Übungsliste im Atmen-Tab (MUSTER-12, Nutzer 2026-10-23): „Bis 3 min“,
 * „Ohne Halten“, „Aus dem Yoga“ – quer zu den Familien, deren Köpfe darunter bleiben. Dieselben
 * Chips wie die Anlässe (neutral, gewählt `SegmentSelected`), aber Kontrollkästchen in einer
 * Zeile ([FilterChipRow]); passt sie nicht, scrollt sie seitlich bis an den Bildschirmrand
 * (`ScreenPadding` aufgehoben). Mehrfachauswahl; ein zweiter Tipp hebt einen Filter auf.
 */
@Composable
internal fun ExerciseFilterRow(active: Set<ExerciseFilter>, onToggle: (ExerciseFilter) -> Unit) {
    FilterChipRow(
        groupName = stringResource(R.string.filter_group),
        items = ExerciseFilter.entries,
        sideBleed = Dimens.ScreenPadding,
    ) { filter ->
        ChoiceChip(
            label = filter.label(),
            selected = filter in active,
            onClick = { onToggle(filter) },
        )
    }
}

/** Beschriftung eines Filters, kurz und sachlich (TEXT-07: kein Versprechen). */
@Composable
private fun ExerciseFilter.label(): String = when (this) {
    ExerciseFilter.SHORT -> stringResource(R.string.filter_short, ExerciseFilters.SHORT_MAX_MINUTES)
    ExerciseFilter.NO_HOLD -> stringResource(R.string.filter_no_hold)
    ExerciseFilter.YOGA -> stringResource(R.string.filter_yoga)
}

/**
 * Trefferstatus unter der Filterreihe, nur bei mindestens einem Filter: „4 Übungen“, als
 * Live-Region einmal höflich angesagt, wenn sich die Zahl ändert (A11Y-04). Ohne Treffer „Keine
 * Übung passt zu allen Filtern“ mit „Filter zurücksetzen“ (MUSTER-04) – der Knopf rückt unter den
 * Satz, wenn beide nicht in eine Zeile passen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterStatus(count: Int, onReset: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        SecondaryText(
            text = if (count == 0) stringResource(R.string.filter_none) else pluralStringResource(R.plurals.filter_count, count, count),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (count == 0) {
            AppTextButton(onClick = onReset) { Text(stringResource(R.string.action_filter_reset)) }
        }
    }
}
