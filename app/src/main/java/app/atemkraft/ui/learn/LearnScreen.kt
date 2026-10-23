package app.atemkraft.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.components.ForwardChevron
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.ScreenSubtitle
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Tab „Lernen“ (Nutzer 2026-10-23): Yoga-Theorie und Praxis an einem Ort, ehrlich eingeordnet.
 * Drei Kapitel als Karten, jede öffnet ihre Seite: Atemlehre (wie die Lehre Ein, Aus und Halten
 * deutet), Pranayama (Grundlagen und Techniken, mit „Üben ›“ zur Übung) und Begriffe. Vorher
 * standen Atemlehre und Begriffe oben in den Einstellungen.
 *
 * Die Kapiteltitel sind die eine Kopf-Ebene des Screens und gehen reihum durch den Kopf-Ring
 * (FARBE-07): Atemlehre 0, Pranayama 1, Begriffe 2.
 */
@Composable
fun LearnScreen(onOpenAtemlehre: () -> Unit, onOpenPranayama: () -> Unit, onOpenGlossary: () -> Unit) {
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    val listState = rememberLazyListState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.tab_learn), scrolled = listState.canScrollBackward)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenBottom),
                verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
            ) {
                item { ScreenSubtitle(stringResource(R.string.learn_subtitle)) }
                item(key = "atemlehre") {
                    ChapterCard(
                        title = stringResource(R.string.settings_atemlehre),
                        hint = stringResource(R.string.learn_atemlehre_hint),
                        heading = 0,
                        onClick = onOpenAtemlehre,
                    )
                }
                item(key = "pranayama") {
                    ChapterCard(
                        title = stringResource(R.string.pranayama_title),
                        hint = stringResource(R.string.learn_pranayama_hint),
                        heading = 1,
                        onClick = onOpenPranayama,
                    )
                }
                item(key = "glossary") {
                    ChapterCard(
                        title = stringResource(R.string.settings_glossary),
                        hint = stringResource(R.string.learn_glossary_hint),
                        heading = 2,
                        onClick = onOpenGlossary,
                    )
                }
            }
        }
    }
}

/**
 * Ein Kapitel: Titel in der Farbe seiner Lage ([heading], FARBE-07), darunter ein Satz, was drin
 * steht, rechts der Chevron. Die ganze Karte ist der Button (Card(onClick), Tippfläche ≥ 48 dp).
 */
@Composable
private fun ChapterCard(title: String, hint: String, heading: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WholeWordText(text = title, style = MaterialTheme.typography.titleMedium, color = headingColor(heading))
                HintText(hint)
            }
            ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
        }
    }
}

@Preview
@Composable
private fun LearnScreenPreview() {
    AtemkraftTheme { LearnScreen(onOpenAtemlehre = {}, onOpenPranayama = {}, onOpenGlossary = {}) }
}
