package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.atemkraft.ui.theme.Dimens

/**
 * Einheitlicher Abschnitts-Header: farbiger Akzentbalken + Titel. App-weit genutzt (Atmen,
 * Situationen, Meditation), damit Sektionsüberschriften überall identisch lesen.
 */
@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier.padding(top = Dimens.SectionHeaderTop, bottom = Dimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        Box(
            modifier = Modifier
                // Balken so hoch wie die Schrift – folgt TEXT_SCALE und der Systemschriftgröße.
                .size(width = 4.dp, height = with(LocalDensity.current) { MaterialTheme.typography.headlineSmall.fontSize.toDp() })
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        WholeWordText(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
        )
    }
}
