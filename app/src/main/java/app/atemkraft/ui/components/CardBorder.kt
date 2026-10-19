package app.atemkraft.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import app.atemkraft.ui.theme.ALPHA_BORDER
import app.atemkraft.ui.theme.Dimens

/**
 * Rand navigierender Inhaltskarten (Übung, Muster des Tages, gespeichertes Muster): dünn, in der
 * Akzentfarbe [color] mit [ALPHA_BORDER] – an einer Stelle statt als Literal je Karte (KOMP-03).
 */
fun accentBorder(color: Color): BorderStroke = BorderStroke(Dimens.BorderThin, color.copy(alpha = ALPHA_BORDER))
