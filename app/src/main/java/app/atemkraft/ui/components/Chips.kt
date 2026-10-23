package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import app.atemkraft.R
import app.atemkraft.domain.EvidenceLevel
import app.atemkraft.ui.theme.CHIP_FILL
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.EvidenceBest
import app.atemkraft.ui.theme.EvidenceCaution
import app.atemkraft.ui.theme.EvidenceLittle
import app.atemkraft.ui.theme.EvidenceStudied
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.WarnAmber

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

/**
 * Vorsichts-Chip „nur gesund & ausgeruht“ (unabhängig von der Studienlage). Umriss statt Fläche:
 * So hält der Text auch auf der Card 4,5:1, ohne dass `EvidenceCaution` heller und damit dem
 * Pink von „Beenden“ noch ähnlicher werden müsste (FARBE-03, -04).
 */
@Composable
fun CautionChip() {
    Chip(text = stringResource(R.string.tag_caution), color = EvidenceCaution, outlined = true)
}

/**
 * „Programm"-Chip für nicht-getaktete Info-Einträge. Umriss statt Füllung: Er nennt eine Art
 * von Eintrag, keine Studienlage, und darf deshalb nicht wie das graue „kaum untersucht“ aussehen.
 */
@Composable
fun InfoChip() {
    NeutralChip(stringResource(R.string.tag_info))
}

/**
 * Charakter einer Übung: „intensiv“ ([intense], Wim Hof, Feueratmung) als einziger gefüllt in
 * `WarnAmber`; „sanft aktivierend“ ([activating], Tagesmuster) und „ruhig“ neutral umrandet wie
 * „Programm“. Ein Charakter ist keine Studienlage und keine Warnung (FARBE-04): In Cyan sähe
 * „ruhig“ aus wie „gut belegt“, in Gelb „sanft aktivierend“ wie eine Warnung.
 */
@Composable
fun CharacterChip(intense: Boolean, activating: Boolean) {
    when {
        intense -> Chip(text = stringResource(R.string.detail_intense), color = WarnAmber)
        activating -> NeutralChip(stringResource(R.string.daily_pattern_gentle_up))
        else -> NeutralChip(stringResource(R.string.detail_calm))
    }
}

/** Neutrale Pille: nur Umriss, Text und Rand in `onSurface` · `SECONDARY`. */
@Composable
private fun NeutralChip(text: String) {
    Chip(text = text, color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY), outlined = true)
}

/**
 * Nicht klickbare Info-Pille. [spokenText] ersetzt für TalkBack den sichtbaren Text (als
 * Text-, nicht als Inhaltsbeschreibung: so bleibt er in einer zusammengeführten Karte in der
 * Lesereihenfolge, und das Text-Layout bleibt für die Umbruchprüfung erhalten).
 */
@Composable
fun Chip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    spokenText: String? = null,
    outlined: Boolean = false,
) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = modifier
            .clip(shape)
            // Umrandet in BorderThin wie AppOutlinedButton: Vorher ein Haarstrich von einem Pixel (≈ 0,36 dp am A54),
            // noch dünner als der 1-dp-Rand, der um enge Rundungen schon ausgefranst wirkte.
            .then(if (outlined) Modifier.border(Dimens.BorderThin, color, shape) else Modifier.background(color.copy(alpha = CHIP_FILL)))
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
