package app.atemkraft.ui.glossary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.data.Glossary
import app.atemkraft.data.GlossaryTerm
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CardTitle
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.theme.Dimens

/**
 * Glossar: erklärt Abkürzungen und Fachbegriffe aus den Übungsbeschreibungen, in zwei Gruppen
 * („Körper & Studien“, „Aus der Yoga-Lehre“) mit einheitlichem Abschnitts-Header.
 */
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    PushScaffold(onBack = onBack, title = stringResource(R.string.glossary_title)) {
        // Die erste Gruppe steht wie jeder erste Inhalt SectionGap unter dem Kopf, ohne den
        // Abstand über weiteren Abschnittsköpfen (SectionHeaderTop).
        TermGroup(0, stringResource(R.string.glossary_group_body), Glossary.bodyTerms, top = 0.dp)
        TermGroup(1, stringResource(R.string.glossary_group_yoga), Glossary.yogaTerms)
    }
}

/**
 * Eine Begriffsgruppe: Abschnitts-Header in der Farbe seiner Lage [index] (reihum, FARBE-07),
 * darunter die Karten mit neutralen Titeln, Karte ↔ Karte `ListGap`.
 */
@Composable
private fun TermGroup(index: Int, title: String, terms: List<GlossaryTerm>, top: Dp = Dimens.SectionHeaderTop) {
    CollapsibleSection(title = title, color = headingColor(index), headerTop = top) {
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
