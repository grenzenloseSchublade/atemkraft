package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.EvidenceTag
import app.atemkraft.ui.home.label
import app.atemkraft.ui.theme.EvidenceBest
import app.atemkraft.ui.theme.EvidenceCaution

/** Einheitliches Evidenz-Tag-Chip – überall mit derselben semantischen Farbe. */
@Composable
fun TagChip(tag: EvidenceTag) {
    val color = when (tag) {
        EvidenceTag.BEST_EVIDENCE -> EvidenceBest
        EvidenceTag.CAUTION -> EvidenceCaution
    }
    Chip(text = tag.label(), color = color)
}

/** „Programm"-Chip für nicht-getaktete Info-Einträge. */
@Composable
fun InfoChip() {
    Chip(
        text = stringResource(R.string.tag_info),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    )
}

@Composable
fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
