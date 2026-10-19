package app.atemkraft.ui.glossary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.Glossary
import app.atemkraft.data.GlossaryTerm
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CardTitle
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens

/**
 * Glossar: erklärt Abkürzungen und Fachbegriffe aus den Übungsbeschreibungen, in zwei Gruppen
 * („Körper & Studien“, „Aus der Yoga-Lehre“) mit einheitlichem Abschnitts-Header.
 */
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            PushHeader(onBack = onBack, title = stringResource(R.string.glossary_title))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                // Abstand über den Gruppen bringt der SectionHeader selbst mit (SectionHeaderTop).
                TermGroup(0, stringResource(R.string.glossary_group_body), Glossary.bodyTerms)
                TermGroup(1, stringResource(R.string.glossary_group_yoga), Glossary.yogaTerms)
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
        }
    }
}

/**
 * Eine Begriffsgruppe: Abschnitts-Header in der Farbe seiner Lage [index] (reihum, FARBE-07),
 * darunter die Karten mit neutralen Titeln.
 */
@Composable
private fun TermGroup(index: Int, title: String, terms: List<GlossaryTerm>) {
    SectionHeader(title = title, color = headingColor(index))
    // Karte ↔ Karte über spacedBy statt Außen-Padding je Karte: exakt ListGap, kein
    // zusätzlicher Rand über der ersten bzw. unter der letzten Karte.
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
        terms.forEach { term ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    // Begriff als Überschrift: Die lange Liste lässt sich per TalkBack durchspringen.
                    CardTitle(term.name)
                    Spacer(Modifier.height(Dimens.GapTiny))
                    BodyText(term.definition)
                }
            }
        }
    }
}
