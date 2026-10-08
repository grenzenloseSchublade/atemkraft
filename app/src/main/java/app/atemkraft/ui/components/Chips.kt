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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.EvidenceLevel
import app.atemkraft.ui.home.label
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.EvidenceBest
import app.atemkraft.ui.theme.EvidenceCaution
import app.atemkraft.ui.theme.EvidenceLittle
import app.atemkraft.ui.theme.EvidenceStudied

/** Test-Tag des Chip-Texts: Der ScreenshotTest meldet einen Chip, dessen Text umbricht (LAYOUT-03). */
const val CHIP_TEXT_TAG = "chip-text"

/** Studienlage-Chip – überall mit derselben semantischen Farbe je Stufe; TalkBack: „Studienlage: …“. */
@Composable
fun EvidenceChip(level: EvidenceLevel) {
    val color = when (level) {
        EvidenceLevel.WELL_SUPPORTED -> EvidenceBest
        EvidenceLevel.STUDIED -> EvidenceStudied
        EvidenceLevel.LITTLE_STUDIED -> EvidenceLittle
    }
    val text = level.label()
    Chip(text = text, color = color, spokenText = stringResource(R.string.cd_evidence, text))
}

/** Vorsichts-Chip „nur gesund & ausgeruht“ (unabhängig von der Studienlage). */
@Composable
fun CautionChip() {
    Chip(text = stringResource(R.string.tag_caution), color = EvidenceCaution)
}

/** „Programm"-Chip für nicht-getaktete Info-Einträge. */
@Composable
fun InfoChip() {
    Chip(
        text = stringResource(R.string.tag_info),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    )
}

/**
 * Nicht klickbare Info-Pille. [spokenText] ersetzt für TalkBack den sichtbaren Text (als
 * Text-, nicht als Inhaltsbeschreibung: so bleibt er in einer zusammengeführten Karte in der
 * Lesereihenfolge, und das Text-Layout bleibt für die Umbruchprüfung erhalten).
 */
@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier, spokenText: String? = null) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier
                .testTag(CHIP_TEXT_TAG)
                .then(if (spokenText == null) Modifier else Modifier.semantics { this.text = AnnotatedString(spokenText) }),
        )
    }
}
