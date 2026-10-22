package app.atemkraft.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.HeadingRing
import app.atemkraft.ui.theme.SECONDARY

/**
 * Farbe des Kopfes an Stelle [index] eines Screens (FARBE-07): reihum durch [HeadingRing], der
 * oberste Kopf ist 0. Der Index ist die Lage des Kopfes, ausdrücklich gesetzt, kein Zähler der
 * Komposition – ein zugeklappter Abschnitt behält seine Farbe. Die Farbe trägt keine Bedeutung.
 * Nicht im Atmen-Tab: Dort tragen die Köpfe die Familienfarben.
 */
fun headingColor(index: Int): Color = HeadingRing[index.mod(HeadingRing.size)]

/**
 * Einheitlicher Abschnitts-Header: farbiger Akzentbalken + Titel. App-weit genutzt (Atmen,
 * Situationen, Meditation), damit Sektionsüberschriften überall identisch lesen. [color] kommt
 * außerhalb des Atmen-Tabs aus [headingColor] (FARBE-07).
 *
 * [trailingAction] ist eine optionale Aktion für den ganzen Abschnitt (z. B. „Muster neu
 * generieren“), rechts als `AppIconButton`, ihr Glyph bündig an der Inhaltskante ([flushEnd]).
 * Sie steht unten bündig neben dem Kopf samt Abstand darüber: So belegt die 48-dp-Tippfläche
 * den Platz, den der Kopf ohnehin hat, und der Abschnitt rückt nicht nach unten.
 *
 * [teaser] steht als ganzer Satz unter dem Titel (nie gekürzt, LAYOUT-03). Mit [expanded]
 * wird der Kopf zur aufklappbaren Zeile (MUSTER-03): Caret am Titel, die ganze Zeile samt
 * Teaser ist die Tippfläche (≥ 48 dp, Rolle Button, Ausgeklappt/Eingeklappt), den Inhalt
 * zeigt der Aufrufer darunter (in Listen über [CollapsibleSection]). So bleibt ein langer Tab
 * eine kurze Übersicht, ohne neue Fläche – der Abschnittskopf wechselt nur seinen Zustand.
 * `null` heißt: nicht aufklappbar.
 *
 * [status] steht direkt hinter dem Titel in gleicher Größe, aber gedämpft („Meine Muster · 5“):
 * lesbar wie der Titel, tritt aber hinter ihn zurück; [statusDescription] ist sein Wortlaut für
 * TalkBack. Mit [smallStatus] ist er ein leiser Zustands-Suffix in `labelMedium` („Genau
 * einstellen · angepasst“, MUSTER-02). Passt er nicht mehr hinter den Titel, steht er darunter,
 * ohne den führenden Punkt.
 */
@Composable
fun SectionHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    teaser: String? = null,
    expanded: Boolean? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    status: String? = null,
    statusDescription: String? = null,
    smallStatus: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val toggle = if (expanded == null) {
        Modifier
    } else {
        val state = expandedStateText(expanded)
        // Der Abstand über dem Kopf liegt außerhalb der Tippfläche, damit die Welle nicht im
        // Leerraum über dem Titel beginnt; innen polstert GapSmall die Zeile oben und unten.
        Modifier
            .padding(top = Dimens.GapSmall)
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button) { onExpandedChange(!expanded) }
            .semantics {
                heading()
                stateDescription = state
            }
    }
    val inner = if (expanded == null) {
        Modifier.padding(top = Dimens.SectionHeaderTop, bottom = Dimens.GapSmall)
    } else {
        Modifier.padding(vertical = Dimens.GapSmall)
    }
    Row(
        modifier = modifier.fillMaxWidth().then(toggle),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f).then(inner)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            ) {
                Box(
                    modifier = Modifier
                        // Balken so hoch wie die Schrift – folgt der Systemschriftgröße.
                        .size(width = 4.dp, height = with(LocalDensity.current) { MaterialTheme.typography.titleLarge.fontSize.toDp() })
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
                // LAYOUT-03: Das Suffix steht direkt hinter dem Titel und rutscht in die nächste
                // Zeile, statt ihn zu kürzen, wenn der Platz nicht reicht – dann ohne führenden
                // Punkt, denn keine Zeile beginnt mit einem Satzzeichen ([TitleWithStatus]).
                TitleWithStatus(
                    title = title,
                    color = color,
                    status = status,
                    statusDescription = statusDescription,
                    statusStyle = if (smallStatus) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (expanded != null) ExpandCaret(expanded = expanded, tint = color)
            }
            if (teaser != null) {
                Text(
                    text = teaser,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    modifier = Modifier.padding(top = Dimens.GapSmall),
                )
            }
        }
        // Glyph an der Inhaltskante wie der Caret aufklappbarer Köpfe (LAYOUT-07).
        if (trailingAction != null) Box(Modifier.flushEnd()) { trailingAction() }
    }
}

/**
 * Aufklappbarer Abschnitt einer Liste (MUSTER-03): [SectionHeader] mit [expanded] und darunter
 * [content], in einer Spalte mit `animateContentSize`, damit das Auf- und Zuklappen die Höhe
 * animiert. `expanded = null` heißt nicht aufklappbar: fester Kopf, Inhalt immer sichtbar.
 *
 * Höhenausgleich: Der aufklappbare Kopf polstert sich selbst nur 2 × `GapSmall` (Tippfläche
 * ohne Leerraum darüber); bis `SectionHeaderTop` fehlt der Rest, abzüglich [spacingAbove], den
 * die umgebende Liste schon setzt (`spacedBy`). So steht er so weit vom Abschnitt darüber wie
 * ein fester Kopf. [contentSpacing] trennt Kopf und Inhalt sowie die Inhaltsteile.
 *
 * Für die Atemlehre und den Situationen-Tab. Nicht im Meditations-Tab: Dort kostete jede
 * Höhenänderung den Zeitring Platz (LAYOUT-08).
 */
@Composable
fun CollapsibleSection(
    title: String,
    color: Color,
    expanded: Boolean?,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    teaser: String? = null,
    status: String? = null,
    statusDescription: String? = null,
    smallStatus: Boolean = false,
    spacingAbove: Dp = 0.dp,
    contentSpacing: Dp = Dimens.ListGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    val topFill = if (expanded == null) 0.dp else (Dimens.SectionHeaderTop - Dimens.GapSmall * 2 - spacingAbove).coerceAtLeast(0.dp)
    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
    ) {
        SectionHeader(
            title = title,
            color = color,
            modifier = Modifier.padding(top = topFill),
            teaser = teaser,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            status = status,
            statusDescription = statusDescription,
            smallStatus = smallStatus,
        )
        if (expanded != false) content()
    }
}

/**
 * Titel eines [SectionHeader] mit optionalem [status] dahinter. Passt der Status in einer Zeile
 * hinter den einzeiligen Titel, steht er dort auf dessen Grundlinie; sonst steht er unter dem
 * Titel und verliert seinen führenden Punkt („· angepasst“ → „angepasst“), damit keine Zeile mit
 * einem Satzzeichen beginnt (LAYOUT-03). Gemessen wird vorab mit dem [rememberTextMeasurer], so
 * entsteht nur die gezeigte Variante (ein Knoten für TalkBack).
 */
@Composable
private fun TitleWithStatus(
    title: String,
    color: Color,
    status: String?,
    statusDescription: String?,
    statusStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val titleStyle = MaterialTheme.typography.titleLarge
    val heading = Modifier.semantics { heading() }
    if (status == null) {
        WholeWordText(text = title, style = titleStyle, color = color, modifier = modifier.then(heading))
        return
    }
    val statusColor = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY)
    val statusSemantics = Modifier.semantics { if (statusDescription != null) contentDescription = statusDescription }
    val measurer = rememberTextMeasurer()
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val titlePlaceable = subcompose(StatusSlot.TITLE) {
            WholeWordText(text = title, style = titleStyle, color = color, modifier = heading)
        }.first().measure(loose)
        val gap = Dimens.GapTiny.roundToPx()
        val titleWidth = measurer.measure(title, titleStyle, softWrap = false, maxLines = 1).size.width
        val statusWidth = measurer.measure(status, statusStyle, softWrap = false, maxLines = 1).size.width
        val inline = titleWidth + gap + statusWidth <= constraints.maxWidth
        if (inline) {
            val statusPlaceable = subcompose(StatusSlot.INLINE) {
                Text(text = status, style = statusStyle, color = statusColor, maxLines = 1, softWrap = false, modifier = statusSemantics)
            }.first().measure(loose)
            // Auf gemeinsamer Grundlinie wie früher in der FlowRow (alignByBaseline).
            val baseline = maxOf(titlePlaceable[FirstBaseline], statusPlaceable[FirstBaseline])
            val titleTop = baseline - titlePlaceable[FirstBaseline]
            val statusTop = baseline - statusPlaceable[FirstBaseline]
            val height = maxOf(titleTop + titlePlaceable.height, statusTop + statusPlaceable.height)
            layout(constraints.constrainWidth(titlePlaceable.width + gap + statusPlaceable.width), height) {
                titlePlaceable.place(0, titleTop)
                statusPlaceable.place(titlePlaceable.width + gap, statusTop)
            }
        } else {
            val bare = status.trimStart().removePrefix("·").trimStart()
            val statusPlaceable = subcompose(StatusSlot.BELOW) {
                WholeWordText(text = bare, style = statusStyle, color = statusColor, modifier = statusSemantics)
            }.first().measure(loose)
            layout(maxOf(titlePlaceable.width, statusPlaceable.width), titlePlaceable.height + statusPlaceable.height) {
                titlePlaceable.place(0, 0)
                statusPlaceable.place(0, titlePlaceable.height)
            }
        }
    }
}

/** Slots von [TitleWithStatus]: nur die gezeigte Status-Variante wird komponiert. */
private enum class StatusSlot { TITLE, INLINE, BELOW }

@Preview
@Composable
private fun SectionHeaderPreview() {
    AtemkraftTheme {
        val accent = MaterialTheme.colorScheme.secondary
        SectionHeader(title = stringResource(R.string.home_daily_pattern), color = accent) {
            AppIconButton(onClick = {}) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_pattern_regenerate), tint = accent)
            }
        }
    }
}

@Preview
@Composable
private fun SectionHeaderExpandablePreview() {
    AtemkraftTheme {
        SectionHeader(
            title = stringResource(R.string.situations_my_patterns),
            color = headingColor(0),
            teaser = stringResource(R.string.situations_subtitle),
            expanded = false,
            status = stringResource(R.string.situations_my_patterns_count, 5),
        )
    }
}
