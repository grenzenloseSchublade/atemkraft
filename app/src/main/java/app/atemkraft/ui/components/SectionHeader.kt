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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import app.atemkraft.R
import app.atemkraft.domain.splitAtDots
import app.atemkraft.domain.withoutLeadingDot
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
 *
 * [chip] ist ein Zustands-Chip am Kopf (z. B. „intensiv“ im Muster-Editor): rechts, mit
 * `GapSmall` Abstand zum Titel und mittig auf dessen erster Zeile – auch wenn der Status
 * darunter mehrzeilig wird. Titel und Status messen gegen die Breite neben ihm.
 * [statusLiveRegion] macht den Status zur Live-Region (A11Y-04), nur solange er gerade eine
 * eigene Änderung ansagt. [statusColor] färbt den Status, wenn er gerade etwas mit Bedeutung zeigt
 * (Muster-Editor: Phase der laufenden Vorschau in Phasenfarbe, FARBE-04); sonst `SECONDARY`.
 * [top] ist der Abstand über einem festen Kopf (Standard `SectionHeaderTop`); kleiner nur, wo ein
 * Bildschirm ohne Scrollen auskommen muss (Muster-Editor, „Atemzug“ direkt unter der Startzeile).
 *
 * [statusReserve] sind weitere Texte, die der Status im Wechsel zeigen kann (Muster-Editor: der
 * Phasenname der laufenden Vorschau, der längste Status): Ob der Status hinter oder unter dem
 * Titel steht und wie hoch er ist, richtet sich dann nach dem längsten von allen – der Kopf
 * behält seine Höhe, und was darunter steht, springt nicht (LAYOUT-03). Mit [trailingOnTitle]
 * steht die [trailingAction] (48 dp hoch) mittig auf der ersten Titelzeile wie [chip] und Caret,
 * statt unten bündig – für Köpfe mit Chip und Knöpfen nebeneinander (Muster-Editor).
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
    chip: (@Composable () -> Unit)? = null,
    statusLiveRegion: Boolean = false,
    statusColor: Color? = null,
    top: Dp = Dimens.SectionHeaderTop,
    statusReserve: List<String> = emptyList(),
    trailingOnTitle: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val titleStyle = MaterialTheme.typography.titleLarge
    val density = LocalDensity.current
    val titleLine = with(density) { (if (titleStyle.lineHeight.isSpecified) titleStyle.lineHeight else titleStyle.fontSize).toDp() }
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
        Modifier.padding(top = top, bottom = Dimens.GapSmall)
    } else {
        Modifier.padding(vertical = Dimens.GapSmall)
    }
    Row(
        modifier = modifier.fillMaxWidth().then(toggle),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f).then(inner)) {
            // Balken, Caret und [chip] stehen mittig neben der ersten Titelzeile, nicht mittig am
            // ganzen Block: Rutscht der Status darunter (bis zu drei Zeilen), bliebe der Balken
            // sonst neben dem Status stehen.
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            ) {
                Box(Modifier.height(titleLine), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            // Balken so hoch wie die Schrift – folgt der Systemschriftgröße.
                            .size(width = 4.dp, height = with(density) { titleStyle.fontSize.toDp() })
                            .clip(RoundedCornerShape(2.dp))
                            .background(color),
                    )
                }
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
                    statusLiveRegion = statusLiveRegion,
                    statusColor = statusColor,
                    statusReserve = statusReserve,
                )
                if (chip != null) {
                    Box(Modifier.heightIn(min = titleLine), contentAlignment = Alignment.Center) { chip() }
                }
                if (expanded != null) {
                    Box(Modifier.heightIn(min = titleLine), contentAlignment = Alignment.Center) {
                        ExpandCaret(expanded = expanded, tint = color)
                    }
                }
            }
            if (teaser != null) {
                Text(
                    text = sanskritText(teaser),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    modifier = Modifier.padding(top = Dimens.GapSmall),
                )
            }
        }
        // Glyph an der Inhaltskante wie der Caret aufklappbarer Köpfe (LAYOUT-07).
        if (trailingAction != null && trailingOnTitle) {
            // Mitte der Knöpfe auf der Mitte der ersten Titelzeile (wie Chip und Caret).
            val above = if (expanded == null) top else Dimens.GapSmall * 2
            val shift = (above + titleLine / 2 - Dimens.MinTouchTarget / 2).coerceAtLeast(0.dp)
            Box(Modifier.align(Alignment.Top).padding(top = shift).flushEnd()) { trailingAction() }
        } else if (trailingAction != null) {
            Box(Modifier.flushEnd()) { trailingAction() }
        }
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
 * [headerModifier] gilt für die Kopfzeile selbst (Tippfläche), z. B. ein `FocusRequester`, wenn
 * eine Wahl im Inhalt den Abschnitt zuklappt (A11Y-07).
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
    headerModifier: Modifier = Modifier,
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
            modifier = Modifier.padding(top = topFill).then(headerModifier),
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
 * einem Satzzeichen beginnt (LAYOUT-03); passt er auch dort nicht in eine Zeile, steht jeder Teil
 * zwischen den Punkten in einer eigenen Zeile, ohne Punkt ([splitAtDots]). Gemessen wird vorab
 * ([rememberWidestTextWidth]), so entsteht nur die gezeigte Variante (ein Knoten für TalkBack).
 * [statusReserve]: Die Variante gilt nur, wenn sie auch für jeden dieser Texte passt, und unter
 * dem Titel ist der Status so hoch wie der höchste von ihnen – ein Wechsel des Texts ändert die
 * Höhe nicht.
 */
@Composable
private fun TitleWithStatus(
    title: String,
    color: Color,
    status: String?,
    statusDescription: String?,
    statusStyle: TextStyle,
    modifier: Modifier = Modifier,
    statusLiveRegion: Boolean = false,
    statusColor: Color? = null,
    statusReserve: List<String> = emptyList(),
) {
    val titleStyle = MaterialTheme.typography.titleLarge
    val heading = Modifier.semantics { heading() }
    if (status == null) {
        WholeWordText(text = title, style = titleStyle, color = color, modifier = modifier.then(heading))
        return
    }
    val shownStatusColor = statusColor ?: MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY)
    val statusSemantics = Modifier.semantics {
        if (statusDescription != null) contentDescription = statusDescription
        if (statusLiveRegion) liveRegion = LiveRegionMode.Polite
    }
    // Einzeilige Breiten vorab (rememberWidestTextWidth), so entsteht nur die gezeigte Variante.
    val texts = listOf(status) + statusReserve
    val titleWidth = rememberWidestAnnotatedWidth(listOf(sanskritText(title)), titleStyle)
    val statusWidth = rememberWidestTextWidth(texts, statusStyle)
    val belowWidth = rememberWidestTextWidth(texts.map { it.withoutLeadingDot() }, statusStyle)
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val titlePlaceable = subcompose(StatusSlot.TITLE) {
            WholeWordText(text = title, style = titleStyle, color = color, modifier = heading)
        }.first().measure(loose)
        val gap = Dimens.GapTiny.roundToPx()
        val inline = titleWidth.roundToPx() + gap + statusWidth.roundToPx() <= constraints.maxWidth
        if (inline) {
            val statusPlaceable = subcompose(StatusSlot.INLINE) {
                Text(text = status, style = statusStyle, color = shownStatusColor, maxLines = 1, softWrap = false, modifier = statusSemantics)
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
            // Passt auch darunter nicht jeder Text in eine Zeile, steht jeder Teil („24 s“, „2,5
            // Atemzüge/min“) in einer eigenen Zeile – sonst endete eine Zeile auf „·“.
            val split = belowWidth.roundToPx() > constraints.maxWidth
            fun below(text: String): String = text.withoutLeadingDot().let { if (split) it.splitAtDots().joinToString("\n") else it }
            val statusPlaceable = subcompose(StatusSlot.BELOW) {
                WholeWordText(text = below(status), style = statusStyle, color = shownStatusColor, modifier = statusSemantics)
            }.first().measure(loose)
            val reserved = if (statusReserve.isEmpty()) {
                0
            } else {
                subcompose(StatusSlot.RESERVE) {
                    MeasureOnly { Box { statusReserve.forEach { Text(below(it), style = statusStyle) } } }
                }.first().measure(loose).height
            }
            val statusHeight = maxOf(statusPlaceable.height, reserved)
            // Breite in den Grenzen melden: Unter der Mindestbreite (weight) zentrierte das Layout
            // den Block sonst, und Titel und Status rückten vom Balken ab.
            layout(constraints.constrainWidth(maxOf(titlePlaceable.width, statusPlaceable.width)), titlePlaceable.height + statusHeight) {
                titlePlaceable.place(0, 0)
                statusPlaceable.place(0, titlePlaceable.height)
            }
        }
    }
}

/** Slots von [TitleWithStatus]: nur die gezeigte Status-Variante wird komponiert. */
private enum class StatusSlot { TITLE, INLINE, BELOW, RESERVE }

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
