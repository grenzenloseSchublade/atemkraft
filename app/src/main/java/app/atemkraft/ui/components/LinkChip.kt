package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Antippbarer Verweis-Chip (öffnet z. B. eine Übung), auf Basis von Material3 SuggestionChip.
 * Sichtbar [Sizes.ChipHeight] hoch (wächst mit großer Schrift), Tippfläche 48 dp. Die Beschriftung
 * steht in einer Zeile und wird dafür höchstens bis [CHIP_MIN_SCALE] verkleinert. Passt ein langer
 * Name („Nadi Shodhana (Wechselatmung)“ bei großer Schrift auf 360 dp) auch dann nicht, bricht er
 * zwischen den Wörtern um und der Chip wird höher – Layout statt kleinerer Schrift (LAYOUT-03,
 * WCAG 1.4.4); vorher schrumpfte er bis 60 %, bei Schrift 2,0 also fast auf Normalgröße.
 * Einzeilig trägt der Text [CHIP_TEXT_TAG], damit der ScreenshotTest einen trotzdem umbrechenden
 * Chip meldet; der zweizeilige Rückfall ist gewollt und trägt ihn nicht.
 */
@Composable
fun LinkChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val config = LocalConfiguration.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    var wrap by remember(label, config.fontScale, windowWidth) { mutableStateOf(false) }
    SuggestionChip(
        onClick = onClick,
        label = {
            if (wrap) {
                WholeWordText(text = label, minScale = CHIP_MIN_SCALE)
            } else {
                WholeWordText(
                    text = label,
                    singleLine = true,
                    minScale = CHIP_MIN_SCALE,
                    onDoesNotFit = { wrap = true },
                    modifier = Modifier.testTag(CHIP_TEXT_TAG),
                )
            }
        },
        // Mindesthöhe statt Höhe: ersetzt die feste M3-Mindesthöhe, ohne Inhalt zu kappen.
        modifier = modifier.heightIn(min = Sizes.ChipHeight),
        // Rahmen in outline wie bis M3 1.3; outlineVariant (Standard ab 1.4) ist auf dem dunklen
        // Grund kaum zu sehen, der Chip wirkte dann nicht mehr antippbar.
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outline,
            borderWidth = Dimens.BorderChip,
        ),
    )
}

/** Untergrenze der einzeiligen Chip-Beschriftung; darunter bricht sie zwischen Wörtern um. */
private const val CHIP_MIN_SCALE = 0.85f

/**
 * Antippbare Übungen als [LinkChip]s (Atemlehre, Pranayama): je Kennung in [ids] ein Chip mit dem
 * Namen aus [exercises], das Muster des Tages ([ExerciseIds.DAILY]) mit seinem festen Namen.
 * M3-Chip statt eigener Pille: Rolle Button und 48-dp-Tippfläche (minimumInteractiveComponentSize)
 * kommen mit. Unbekannte Kennungen fallen still weg.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseChips(ids: List<String>, exercises: List<Exercise>, onOpenExercise: (String) -> Unit, modifier: Modifier = Modifier) {
    if (ids.isEmpty()) return
    val daily = stringResource(R.string.atemlehre_daily_pattern)
    val entries = ids.mapNotNull { id ->
        val name = if (id == ExerciseIds.DAILY) daily else exercises.firstOrNull { it.id == id }?.name
        name?.let { id to it }
    }
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        entries.forEach { (id, name) ->
            LinkChip(label = name, onClick = { onOpenExercise(id) })
        }
    }
}
