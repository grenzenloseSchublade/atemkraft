package app.atemkraft.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Feste Endspalten der Einstellungs-Zeilen (S-38): [labelWidth] ist die Breite der mittleren
 * Spalte, gemessen an der breitesten Zustands-Beschriftung ([rememberTrailingColumns]).
 */
@Immutable
internal class TrailingColumns(val labelWidth: Dp)

/**
 * Misst die breiteste Beschriftung der Zustands-Spalte („Laden“, „Wählen“ im Text-Button,
 * „✓ Aktiv“) in der aktuellen Schriftgröße, plus Innenpolster des Text-Buttons. So passt jede
 * einzeilig hinein (LAYOUT-03), und die Spalten stehen in allen Zeilen gleich.
 */
@Composable
internal fun rememberTrailingColumns(): TrailingColumns {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val button = MaterialTheme.typography.labelLarge
    val status = MaterialTheme.typography.labelMedium
    val labels = listOf(
        stringResource(R.string.settings_voice_download) to button,
        stringResource(R.string.settings_voice_choose) to button,
        stringResource(R.string.settings_voice_active) to status,
    )
    return remember(measurer, density, direction, labels) {
        val padding = ButtonDefaults.TextButtonContentPadding
        with(density) {
            val text = labels.maxOf { (label, style) -> measurer.measure(label, style, maxLines = 1).size.width }
            // Polster wie im Button pro Seite gerundet; dazu eine Haarlinie Luft gegen Rundung.
            val px = text + padding.calculateStartPadding(direction).roundToPx() + padding.calculateEndPadding(direction).roundToPx() +
                Dimens.GapHairline.roundToPx()
            TrailingColumns(labelWidth = px.toDp())
        }
    }
}

/**
 * Zeile mit festen rechten Spalten (S-38): [lead] links, dann Vorhören ([play], 48 dp), Zustand
 * ([label], [TrailingColumns.labelWidth]) und Icon-Aktion ([icon], [TrailingIconCell]). Leere
 * Fächer behalten ihre Breite, damit Play-Knöpfe und Aktionen über alle Zeilen der Seite
 * untereinander stehen – auch die Gong-Zeile, die nur [play] hat ([label] = null).
 *
 * Passt [lead] nicht daneben (große Schrift auf schmalen Geräten), bricht kein Wort um (LAYOUT-03):
 * Zeilen mit Aktionen brauchen dafür nur ihr längstes Wort, und reicht das nicht, stehen die
 * Spalten unter dem Text, weiter rechtsbündig untereinander. Die Gong-Zeile hält ihr Label
 * einzeilig; ihr Play rückt dafür erst ans Zeilenende, bevor es unter das Label geht.
 */
@Composable
internal fun TrailingRow(
    columns: TrailingColumns,
    play: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    icon: @Composable () -> Unit = {},
    lead: @Composable () -> Unit,
) {
    val cells: @Composable () -> Unit = {
        if (label != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(columns.labelWidth), contentAlignment = Alignment.Center) { label() }
                TrailingIconCell(icon)
            }
        }
    }
    Layout(
        contents = listOf(lead, { Box(Modifier.width(Dimens.MinTouchTarget), contentAlignment = Alignment.Center) { play() } }, cells),
        modifier = modifier.fillMaxWidth(),
    ) { (leadMeasurables, playMeasurables, cellMeasurables), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints()
        val playCell = playMeasurables.single().measure(loose)
        val cellsPlaced = cellMeasurables.singleOrNull()?.measure(loose)
        // Breite der Fächer rechts vom Play, auch wenn die Zeile selbst keine hat (Gong).
        val restWidth = cellsPlaced?.width ?: (columns.labelWidth + Dimens.MinTouchTarget - Sizes.TouchOverhang).roundToPx()
        val leadMeasurable = leadMeasurables.single()
        val needed = if (label != null) {
            leadMeasurable.minIntrinsicWidth(Constraints.Infinity)
        } else {
            leadMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        }
        val beside = width - playCell.width - restWidth
        val playEnd = width - playCell.width
        when {
            needed <= beside || (label == null && needed <= playEnd) -> {
                val leadWidth = if (needed <= beside) beside else playEnd
                val leadPlaced = leadMeasurable.measure(Constraints(maxWidth = leadWidth))
                val height = maxOf(leadPlaced.height, playCell.height, cellsPlaced?.height ?: 0)
                layout(width, height) {
                    leadPlaced.placeRelative(0, (height - leadPlaced.height) / 2)
                    playCell.placeRelative(leadWidth, (height - playCell.height) / 2)
                    cellsPlaced?.placeRelative(leadWidth + playCell.width, (height - cellsPlaced.height) / 2)
                }
            }

            else -> {
                val leadPlaced = leadMeasurable.measure(Constraints(maxWidth = width))
                val cellsHeight = maxOf(playCell.height, cellsPlaced?.height ?: 0)
                val playX = (width - restWidth - playCell.width).coerceAtLeast(0)
                layout(width, leadPlaced.height + cellsHeight) {
                    leadPlaced.placeRelative(0, 0)
                    playCell.placeRelative(playX, leadPlaced.height + (cellsHeight - playCell.height) / 2)
                    cellsPlaced?.placeRelative(playX + playCell.width, leadPlaced.height + (cellsHeight - cellsPlaced.height) / 2)
                }
            }
        }
    }
}

/**
 * Icon-Spalte am Zeilenende: 48 dp breit, um den Touch-Rand schmaler gemeldet ([flushEnd]), damit
 * der Glyph an der Inhaltskante steht (LAYOUT-07) – Papierkorb von Stimmen und Logbuch untereinander.
 */
@Composable
internal fun TrailingIconCell(content: @Composable () -> Unit) {
    Box(Modifier.flushEnd(inCard = true).width(Dimens.MinTouchTarget), contentAlignment = Alignment.Center) { content() }
}

/** Vorhören (Gong, Stimmen): Play in voller Deckkraft, überall derselbe Knopf. */
@Composable
internal fun PreviewButton(description: String, onClick: () -> Unit) {
    AppIconButton(onClick = onClick) {
        Icon(Icons.Filled.PlayArrow, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}
