package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.atemkraft.ui.theme.Dimens

/**
 * Auswahl einer Listenzeile ohne Häkchen: Zeile in [shape] beschnitten (auch das Tipp-Echo), die
 * gewählte mit Fläche [fill] und dünnem Rand [borderColor] – Fläche und Rand tragen die Auswahl,
 * nicht die Farbe allein (FARBE-05). Vor `selectable`/`toggleable` und dem Innenpolster setzen.
 * Vorlagen und Ablauf des Muster-Editors (`secondaryContainer`, `SegmentSelected`), vāyu-Liste der
 * Atemlehre (`surfaceVariant`, Farbe des vāyu).
 */
fun Modifier.selectedRow(selected: Boolean, shape: Shape, borderColor: Color, fill: Color): Modifier = clip(shape)
    .then(if (selected) Modifier.background(fill).border(Dimens.BorderThin, borderColor, shape) else Modifier)
