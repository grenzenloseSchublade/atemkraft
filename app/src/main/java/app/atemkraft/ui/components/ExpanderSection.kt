package app.atemkraft.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.BODY
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.SECONDARY

/**
 * Wiederverwendbarer Inline-Expander (Progressive Disclosure): Titel + Appetizer, tippbar zum
 * Ausklappen. Der Appetizer steht **immer ganz** da – nie per Ellipse gekürzt, denn er trägt
 * Wirkung und Sicherheit als geschlossenen Satz (TEXT-06, -08, LAYOUT-03). [content] erscheint
 * aufgeklappt darunter und vertieft ihn. Ohne [content] gibt es nichts aufzuklappen: dann
 * ein schlichter Abschnitt ohne Caret und ohne Klick. Titel in `onSurface` wie jeder Kopf
 * (FARBE-07), Caret gedimmt; er dreht sich, sanftes Größen-Animieren, TalkBack bekommt Überschrift,
 * Rolle Button und Ausgeklappt/Eingeklappt.
 * [divider] = false lässt die Trennlinie weg, wo der Abschnitt allein steht. Als Karte: [ExpanderCard].
 */
@Composable
fun ExpanderSection(
    title: String,
    appetizer: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    divider: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Expander(
            title = title,
            appetizer = appetizer,
            initiallyExpanded = initiallyExpanded,
            rowTop = Dimens.ListGap,
            rowBottom = Dimens.ListGap,
            contentBottom = Dimens.GapSmall,
            content = content,
        )
        if (divider) HorizontalDivider(color = DividerSubtle)
    }
}

/**
 * Aufklappbare Karte (Pranayama: Grundlagen und Techniken; Atemlehre: Muster-Karten): Titel
 * neutral in `onSurface`, Appetizer und [content] wie bei [ExpanderSection], ringsum `CardPadding`
 * – auch unter dem aufgeklappten Inhalt. [header] steht über dem Titel und gehört zur Karte, nicht
 * zur Tippfläche (Rhythmusbalken und Takt der Atemlehre); darunter folgt der Titel mit `ListGap`.
 */
@Composable
fun ExpanderCard(
    title: String,
    appetizer: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = Dimens.CardPadding)) {
            if (header != null) {
                Spacer(Modifier.height(Dimens.CardPadding))
                header()
            }
            Expander(
                title = title,
                appetizer = appetizer,
                initiallyExpanded = initiallyExpanded,
                rowTop = if (header != null) Dimens.ListGap else Dimens.CardPadding,
                rowBottom = Dimens.CardPadding,
                contentBottom = Dimens.CardPadding,
                content = content,
            )
        }
    }
}

/**
 * Gemeinsamer Bau von [ExpanderSection] und [ExpanderCard]: tippbarer Kopf mit [rowTop] und
 * [rowBottom] Polster, aufgeklappt [content] und darunter [contentBottom]. Der Kopf folgt der
 * Anatomie „Karte mit Aktionen“ (§4.5) über [CardHeader]: Titel und Caret als Zeichen mittig auf
 * der ersten Titelzeile, der Teaser läuft darunter in voller Breite bis zur Inhaltskante.
 */
@Composable
private fun Expander(
    title: String,
    appetizer: String,
    rowTop: Dp,
    rowBottom: Dp,
    contentBottom: Dp,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val expandable = content != null
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val state = expandedStateText(expanded)

    Column(modifier = modifier.fillMaxWidth().animateContentSize(Motion.Disclosure)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Eigene Klickfläche: Mindesthöhe unabhängig vom (kompakten) Padding.
                .then(
                    if (expandable) {
                        Modifier.heightIn(min = Dimens.MinTouchTarget).clickable(role = Role.Button) { expanded = !expanded }
                    } else {
                        Modifier
                    },
                )
                // Ohne Inhalt fehlt das clickable und mit ihm das Zusammenführen: Die Überschrift
                // läge an einer Zeile ohne eigenen Text, „nächste Überschrift“ fände sie nicht
                // (A11Y-01). Dann führt die Zeile Titel und Teaser selbst zu einem Stopp zusammen.
                .semantics(mergeDescendants = !expandable) {
                    heading()
                    if (expandable) stateDescription = state
                }
                .padding(top = rowTop, bottom = rowBottom),
        ) {
            CardHeader(
                title = title,
                marks = true,
                indicator = if (expandable) {
                    { ExpandCaret(expanded = expanded, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)) }
                } else {
                    null
                },
            )
            Text(
                text = sanskritText(appetizer),
                // Eine Rolle für beide Zustände (kein Reflow beim Aufklappen); Betonung nur
                // über die Deckkraft: zugeklappt Sekundärtext wie jeder Teaser, aufgeklappt
                // (und ohne Inhalt) Fließtext. Farbe der Fläche: onBackground auf dem Screen,
                // onSurface in der Karte.
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current
                    .copy(alpha = if (expanded || !expandable) BODY else SECONDARY),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (expanded && content != null) {
            content()
            Spacer(Modifier.height(contentBottom))
        }
    }
}

@Preview
@Composable
private fun ExpanderCardPreview() {
    AtemkraftTheme {
        ExpanderCard(
            title = stringResource(R.string.about_licenses_title),
            appetizer = stringResource(R.string.about_licenses_teaser),
        ) { BodyText(stringResource(R.string.volume_hint)) }
    }
}
