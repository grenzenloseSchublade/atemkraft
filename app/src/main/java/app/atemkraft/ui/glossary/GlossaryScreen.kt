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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.Glossary
import app.atemkraft.data.GlossaryTerm
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.WholeWordText
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
                TermGroup(stringResource(R.string.glossary_group_body), Glossary.bodyTerms)
                TermGroup(stringResource(R.string.glossary_group_yoga), Glossary.yogaTerms)
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
        }
    }
}

/** Eine Begriffsgruppe: einheitlicher Abschnitts-Header, darunter die Karten. */
@Composable
private fun TermGroup(title: String, terms: List<GlossaryTerm>) {
    SectionHeader(title = title, color = MaterialTheme.colorScheme.primary)
    // Karte ↔ Karte über spacedBy statt Außen-Padding je Karte: exakt ListGap, kein
    // zusätzlicher Rand über der ersten bzw. unter der letzten Karte.
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
        terms.forEach { term ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    WholeWordText(
                        text = term.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(Dimens.GapTiny))
                    Text(
                        text = term.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}
