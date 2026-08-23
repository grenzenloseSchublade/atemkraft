package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Einheitlicher Abschnitts-Header: farbiger Akzentbalken + Titel. App-weit genutzt (Atmen,
 * Situationen, Meditation), damit Sektionsüberschriften überall identisch lesen.
 */
@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
        )
    }
}
