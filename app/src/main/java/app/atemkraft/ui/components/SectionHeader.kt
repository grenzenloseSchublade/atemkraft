package app.atemkraft.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.splitAtDots
import app.atemkraft.domain.withoutLeadingDot
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.FamilyVagal
import app.atemkraft.ui.theme.HeadingRing
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.SECONDARY

/**
 * Farbe des Kapiteltitels an Stelle [index] im Tab „Lernen“ (FARBE-07): reihum durch
 * [HeadingRing], die oberste Karte ist 0. Der Index ist die Lage, ausdrücklich gesetzt, kein
 * Zähler der Komposition. Die Farbe trägt keine Bedeutung. Seit 2026-10-24 (Vorschlag B) die
 * einzige farbige Kopf-Ebene: Abschnittsköpfe ([SectionHeader]) sind überall weiß, Farbe tragen
 * nur noch Gruppenlabels ([GroupLabel], Familienfarbe) und Kartenränder.
 */
fun headingColor(index: Int): Color = HeadingRing[index.mod(HeadingRing.size)]

/**
 * Einheitlicher Abschnitts-Header: Titel in `titleLarge`, `onSurface` – ohne Farbbalken, ohne
 * Akzentfarbe (Nutzer 2026-10-24, Vorschlag B: eine Kopf-Art, FARBE-07). App-weit genutzt (Atmen
 * „Übungen“, Eigene, Meditation, Atemlehre, Pranayama, Glossar, Logbuch), damit
 * Sektionsüberschriften überall identisch lesen. Eine Ebene darunter steht das kleine, farbige
 * [GroupLabel] (Familien im Atmen-Tab).
 *
 * Abstände: Über einem festen Kopf steht [top] (Standard `SectionHeaderTop`; kleiner nur, wo ein
 * Bildschirm ohne Scrollen auskommen muss). **Unten trägt der Kopf keinen Abstand:** Kopf → Inhalt
 * ist überall `HeaderGap` – in Listen über `spacedBy(ListGap)`, in Spalten über
 * [CollapsibleSection]. So steht der erste Inhalt unter jedem Kopf gleich weit.
 *
 * [trailingAction] ist eine optionale Aktion für den ganzen Abschnitt (z. B. „Neues Muster“),
 * rechts als `AppIconButton` in `onSurface`, ihr Glyph bündig an der Inhaltskante ([flushEnd]).
 * Sie steht mittig auf der ersten Titelzeile; ihre 48-dp-Tippfläche ragt dabei in den Abstand
 * darunter, ohne den Kopf höher zu machen – der Abschnitt rückt nicht nach unten. Mit [expanded]
 * stehen Caret und Aktion zusammen: Der Caret bleibt am Titel, die Aktion rechts davon an der
 * Inhaltskante; die Tippfläche des Auf- und Zuklappens endet vor der Aktion, beide sind eigene
 * Bedienelemente („Eigene Kreationen“ im Tab „Eigene“).
 *
 * [teaser] steht als ganzer Satz unter dem Titel (nie gekürzt, LAYOUT-03). Mit [expanded]
 * wird der Kopf zur aufklappbaren Zeile (MUSTER-03): Caret am Titel (gedimmt, `SECONDARY`), die
 * ganze Zeile samt Teaser ist die Tippfläche (≥ 48 dp, Rolle Button, Ausgeklappt/Eingeklappt),
 * den Inhalt zeigt der Aufrufer darunter (über [CollapsibleSection]). So bleibt ein langer Tab eine
 * kurze Übersicht, ohne neue Fläche – der Abschnittskopf wechselt nur seinen Zustand. `null`
 * heißt: nicht aufklappbar.
 *
 * Mit [onClick] öffnet der Kopf etwas (Bottom Sheet, MUSTER-11: „Klang & Stimme“ im
 * Meditations-Tab; Logbuch-Seite: „Verlauf“ im Tab „Eigene“): dieselbe Zeile wie aufklappbar, aber
 * mit [ForwardChevron] statt Caret, ohne Zustand. TalkBack liest sie als eine Schaltfläche aus
 * Titel und Teaser, die Trennpunkte als Kommas („Klang & Stimme, Gong Start & Ende, ohne Stimme,
 * Schaltfläche“); [onClickLabel] benennt, was der Tipp tut („Logbuch öffnen“).
 *
 * [status] steht direkt hinter dem Titel in gleicher Größe, aber gedämpft („Eigene Kreationen · 5“):
 * lesbar wie der Titel, tritt aber hinter ihn zurück; [statusDescription] ist sein Wortlaut für
 * TalkBack. Mit [smallStatus] ist er ein leiser Zustands-Suffix in `labelMedium` („Ablauf ·
 * 4 von 12 Phasen“, MUSTER-02). Passt er nicht mehr hinter den Titel, steht er darunter, ohne den
 * führenden Punkt.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    teaser: String? = null,
    expanded: Boolean? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    status: String? = null,
    statusDescription: String? = null,
    smallStatus: Boolean = false,
    top: Dp = Dimens.SectionHeaderTop,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val titleStyle = MaterialTheme.typography.titleLarge
    val color = MaterialTheme.colorScheme.onSurface
    val dimmed = color.copy(alpha = SECONDARY)
    // Gemessen statt aus lineHeight: ab Android 14 wächst die Schrift nichtlinear (rememberFirstLineHeight).
    val titleLine = rememberFirstLineHeight(titleStyle)
    // Zeile wie aufklappbar: Tippfläche ohne den Leerraum darüber, innen oben und unten GapSmall.
    val tappable = expanded != null || onClick != null
    val toggle = if (onClick != null) {
        val spoken = listOfNotNull(title, teaser?.splitAtDots()?.joinToString(", ")).joinToString(", ")
        Modifier
            .padding(top = Dimens.GapSmall)
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
            // Die Beschreibung geht dem sichtbaren Text vor; der Titel ist hier keine Überschrift.
            .semantics { contentDescription = spoken }
    } else if (expanded == null) {
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
    val inner = if (!tappable) {
        Modifier.padding(top = top)
    } else {
        Modifier.padding(vertical = Dimens.GapSmall)
    }
    // Mitte der Tippfläche einer Kopf-Aktion auf der Mitte der ersten Titelzeile (wie der Caret).
    val above = if (!tappable) top else Dimens.GapSmall * 2
    TitleRow(actionCenter = above + titleLine / 2, modifier = modifier, trailingAction = trailingAction) {
        // Die Tippfläche des Auf- und Zuklappens liegt auf der Spalte, nicht auf der ganzen Zeile:
        // So steckt eine Kopf-Aktion daneben nicht in einem zweiten Button.
        Column(modifier = Modifier.weight(1f).then(toggle).then(inner)) {
            // Caret und Chevron stehen mittig neben der ersten Titelzeile, nicht mittig am ganzen
            // Block: Rutscht der Status darunter (bis zu drei Zeilen), blieben sie sonst neben dem
            // Status stehen.
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            ) {
                // LAYOUT-03: Das Suffix steht direkt hinter dem Titel und rutscht in die nächste
                // Zeile, statt ihn zu kürzen, wenn der Platz nicht reicht – dann ohne führenden
                // Punkt, denn keine Zeile beginnt mit einem Satzzeichen ([TitleWithStatus]).
                TitleWithStatus(
                    title = title,
                    color = color,
                    status = status,
                    statusDescription = statusDescription,
                    statusStyle = if (smallStatus) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).testTag(TITLE_ROW_TITLE_TAG),
                    isHeading = onClick == null,
                )
                if (onClick != null) {
                    Box(Modifier.heightIn(min = titleLine), contentAlignment = Alignment.Center) {
                        ForwardChevron(tint = dimmed)
                    }
                } else if (expanded != null) {
                    Box(Modifier.heightIn(min = titleLine), contentAlignment = Alignment.Center) {
                        ExpandCaret(expanded = expanded, tint = dimmed)
                    }
                }
            }
            if (teaser != null) {
                SecondaryText(teaser, modifier = Modifier.padding(top = Dimens.GapSmall))
            }
        }
    }
}

/**
 * Kleines farbiges Gruppenlabel, eine Ebene unter dem [SectionHeader] (Nutzer 2026-10-24,
 * Vorschlag B): `titleSmall` in [color] – im Atmen-Tab die Familienfarbe über jeder Gruppe von
 * Übungskarten und über einem geöffneten Bereich (`AreaHeader`, dort mit ✕ als [trailingAction]).
 * Kein Balken, kein Leerraum darunter: Label → Inhalt ist wie bei jedem Kopf `HeaderGap`, in
 * Listen über `spacedBy(ListGap)`. Darüber steht [top] (Standard `SectionGap`; in einer Liste
 * mit `spacedBy` entsprechend weniger).
 *
 * [trailingAction] steht als `AppIconButton` mittig auf der Textzeile, Glyph an der Inhaltskante
 * ([flushEnd]); die 48-dp-Tippfläche ragt in den Abstand darunter, ohne das Label höher zu machen.
 * TalkBack: Überschrift (`heading()`), damit sich die Liste per Überschriften-Navigation
 * durchspringen lässt (A11Y-01).
 */
@Composable
fun GroupLabel(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    top: Dp = Dimens.SectionGap,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val style = MaterialTheme.typography.titleSmall
    val line = rememberFirstLineHeight(style)
    TitleRow(actionCenter = top + line / 2, modifier = modifier, trailingAction = trailingAction) {
        WholeWordText(
            text = title,
            style = style,
            color = color,
            modifier = Modifier
                .weight(1f)
                .padding(top = top)
                .testTag(TITLE_ROW_TITLE_TAG)
                .semantics { heading() },
        )
    }
}

/**
 * Gemeinsame Zeile von [SectionHeader] und [GroupLabel] – die Anatomie „Abschnittskopf“ (§4.5) an
 * einer Stelle: [title] links (der Aufrufer gibt ihm `weight(1f)` und markiert den Titeltext mit
 * [TITLE_ROW_TITLE_TAG]), oben ausgerichtet; [trailingAction] rechts als genau eine Aktion, ihre
 * Mitte auf [actionCenter] unter der Oberkante (Mitte der ersten Titelzeile), der Glyph bündig an
 * der Inhaltskante ([flushEnd], LAYOUT-07). Die 48-dp-Tippfläche ragt höchstens [MAX_OVERHANG] in
 * den Abstand darunter, sonst wird die Zeile höher. Das Gate ANATOMIE findet die Zeile über
 * [TITLE_ROW_TAG].
 */
@Composable
internal fun TitleRow(
    actionCenter: Dp,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
    title: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth().testTag(TITLE_ROW_TAG), verticalAlignment = Alignment.Top) {
        title()
        if (trailingAction != null) {
            Box(Modifier.centeredOverflow(actionCenter, MAX_OVERHANG).flushEnd().testTag(TITLE_ROW_ACTIONS_TAG)) { trailingAction() }
        }
    }
}

/** Größter Überstand einer Kopf-Aktion unter den Kopf: der Abstand zum Inhalt bis auf eine Haarlinie. */
private val MAX_OVERHANG = Dimens.HeaderGap - Dimens.GapHairline

/**
 * Abschnitt mit Kopf und Inhalt (MUSTER-03): [SectionHeader] und darunter [content]; der
 * Inhaltsteil steht in einer Spalte mit `animateContentSize`, damit das Auf- und Zuklappen die
 * Höhe animiert. Nur der Inhalt, nicht der Kopf: `animateContentSize` schneidet an seinen Rändern
 * ab, und die bündige Kopf-Aktion ([flushEnd]) ragt um ihre Tippfläche über die Inhaltskante
 * hinaus – im Kopf wäre ihr Kreis angeschnitten (LAYOUT-05).
 * `expanded = null` heißt nicht aufklappbar: fester Kopf, Inhalt immer sichtbar – so setzen auch
 * Spalten ohne Liste (Glossar, Pranayama, Atemlehre, Logbuch-Gruppen) den Abstand Kopf → Inhalt
 * zentral.
 *
 * Abstände: Kopf → Inhalt ist sichtbar immer `HeaderGap` (beim aufklappbaren Kopf abzüglich des
 * GapSmall, mit dem er seine Tippfläche unten polstert). [contentSpacing] trennt nur die
 * Inhaltsteile untereinander. Höhenausgleich oben: Der aufklappbare Kopf polstert sich selbst nur
 * 2 × `GapSmall` (Tippfläche ohne Leerraum darüber); bis `SectionHeaderTop` fehlt der Rest,
 * abzüglich [spacingAbove], den die umgebende Liste schon setzt (`spacedBy`). So steht er so weit
 * vom Abschnitt darüber wie ein fester Kopf. [headerTop] ist der Abstand über einem festen Kopf
 * (0 für den ersten Kopf einer Push-Seite, der schon `SectionGap` unter dem Seitenkopf steht).
 * [headerModifier] gilt für die Kopfzeile selbst (Tippfläche), z. B. ein `FocusRequester`, wenn
 * eine Wahl im Inhalt den Abschnitt zuklappt (A11Y-07). [trailingAction] reicht an den Kopf
 * durch (Aktion neben dem Caret, z. B. „+“ bei „Eigene Kreationen“).
 *
 * Nicht im Meditations-Tab: Dort kostete jede Höhenänderung den Zeitring Platz (LAYOUT-08).
 */
@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    teaser: String? = null,
    status: String? = null,
    statusDescription: String? = null,
    smallStatus: Boolean = false,
    spacingAbove: Dp = 0.dp,
    headerTop: Dp = Dimens.SectionHeaderTop,
    contentSpacing: Dp = Dimens.ListGap,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val topFill = if (expanded == null) 0.dp else (Dimens.SectionHeaderTop - Dimens.GapSmall * 2 - spacingAbove).coerceAtLeast(0.dp)
    val headerGap = if (expanded == null) Dimens.HeaderGap else (Dimens.HeaderGap - Dimens.GapSmall).coerceAtLeast(0.dp)
    Column(modifier = modifier) {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(top = topFill).then(headerModifier),
            teaser = teaser,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            status = status,
            statusDescription = statusDescription,
            smallStatus = smallStatus,
            top = headerTop,
            trailingAction = trailingAction,
        )
        // Volle Breite, damit beim Aufklappen nur die Höhe gleitet, nicht auch die Breite.
        Column(modifier = Modifier.fillMaxWidth().animateContentSize(Motion.Disclosure)) {
            if (expanded != false) {
                Column(
                    modifier = Modifier.padding(top = headerGap),
                    verticalArrangement = Arrangement.spacedBy(contentSpacing),
                    content = content,
                )
            }
        }
    }
}

/**
 * Titel mit optionalem [status] dahinter – die Anatomie „Titel · Status“ des [SectionHeader], auch
 * für den Zustands-Suffix des [DisclosureToggle] („Phasen anpassen · angepasst“). Passt der Status
 * in einer Zeile hinter den einzeiligen Titel, steht er dort auf dessen Grundlinie; sonst steht er
 * unter dem Titel und verliert seinen führenden Punkt („· angepasst“ → „angepasst“,
 * [withoutLeadingDot]), damit keine Zeile mit einem Satzzeichen beginnt (LAYOUT-03); passt er auch
 * dort nicht in eine Zeile, steht jeder Teil zwischen den Punkten in einer eigenen Zeile, ohne
 * Punkt ([splitAtDots]). Gemessen wird vorab ([rememberWidestTextWidth]), so entsteht nur die
 * gezeigte Variante (ein Knoten für TalkBack). Ohne [isHeading] ist der Titel keine Überschrift
 * (Kopf als Schaltfläche, `onClick`). Mit [statusVisible] = false hält der Status nur seinen Platz
 * ([MeasureOnly]): Erscheint er später, springt nichts (§4.5 „Nichts springt“).
 */
@Composable
internal fun TitleWithStatus(
    title: String,
    color: Color,
    status: String?,
    statusDescription: String?,
    statusStyle: TextStyle,
    modifier: Modifier = Modifier,
    isHeading: Boolean = true,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    statusVisible: Boolean = true,
) {
    val heading = if (isHeading) Modifier.semantics { heading() } else Modifier
    if (status == null) {
        WholeWordText(text = title, style = titleStyle, color = color, modifier = modifier.then(heading))
        return
    }
    val statusColor = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY)
    val statusSemantics = Modifier.semantics { if (statusDescription != null) contentDescription = statusDescription }
    val below = status.withoutLeadingDot()
    val titleWidth = rememberWidestAnnotatedWidth(listOf(sanskritText(title)), titleStyle)
    val statusWidth = rememberWidestTextWidth(listOf(status), statusStyle)
    val belowWidth = rememberWidestTextWidth(listOf(below), statusStyle)
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val titlePlaceable = subcompose(StatusSlot.TITLE) {
            WholeWordText(text = title, style = titleStyle, color = color, modifier = heading)
        }.first().measure(loose)
        val gap = Dimens.GapTiny.roundToPx()
        if (titleWidth.roundToPx() + gap + statusWidth.roundToPx() <= constraints.maxWidth) {
            val statusPlaceable = subcompose(StatusSlot.INLINE) {
                ShownOrReserved(statusVisible) {
                    Text(text = status, style = statusStyle, color = statusColor, maxLines = 1, softWrap = false, modifier = statusSemantics)
                }
            }.first().measure(loose)
            // Auf gemeinsamer Grundlinie wie früher in der FlowRow (alignByBaseline).
            val baseline = maxOf(titlePlaceable[FirstBaseline], statusPlaceable[FirstBaseline])
            val titleTop = baseline - titlePlaceable[FirstBaseline]
            val statusTop = baseline - statusPlaceable[FirstBaseline]
            val height = maxOf(titleTop + titlePlaceable.height, statusTop + statusPlaceable.height)
            layout(constraints.constrainWidth(titlePlaceable.width + gap + statusPlaceable.width), height) {
                titlePlaceable.place(0, titleTop)
                if (statusVisible) statusPlaceable.place(titlePlaceable.width + gap, statusTop)
            }
        } else {
            // Passt er auch darunter nicht in eine Zeile, steht jeder Teil („24 s“, „2,5
            // Atemzüge/min“) in einer eigenen Zeile – sonst endete eine Zeile auf „·“.
            val shown = if (belowWidth.roundToPx() > constraints.maxWidth) below.splitAtDots().joinToString("\n") else below
            val statusPlaceable = subcompose(StatusSlot.BELOW) {
                ShownOrReserved(statusVisible) {
                    WholeWordText(text = shown, style = statusStyle, color = statusColor, modifier = statusSemantics)
                }
            }.first().measure(loose)
            // Breite in den Grenzen melden: Unter der Mindestbreite (weight) zentrierte das Layout
            // den Block sonst, und Titel und Status rückten von der Kante ab.
            layout(constraints.constrainWidth(maxOf(titlePlaceable.width, statusPlaceable.width)), titlePlaceable.height + statusPlaceable.height) {
                titlePlaceable.place(0, 0)
                if (statusVisible) statusPlaceable.place(0, titlePlaceable.height)
            }
        }
    }
}

/** Slots von [TitleWithStatus]: nur die gezeigte Status-Variante wird komponiert. */
private enum class StatusSlot { TITLE, INLINE, BELOW }

/** Zeigt [content] oder misst es nur als Reserve ([MeasureOnly]: ohne Semantik, nie platziert). */
@Composable
private fun ShownOrReserved(shown: Boolean, content: @Composable () -> Unit) {
    if (shown) content() else MeasureOnly(content)
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    AtemkraftTheme {
        SectionHeader(title = stringResource(R.string.own_creations)) {
            AppIconButton(onClick = {}) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.own_new_pattern), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Preview
@Composable
private fun SectionHeaderExpandablePreview() {
    AtemkraftTheme {
        SectionHeader(
            title = stringResource(R.string.own_creations),
            teaser = stringResource(R.string.own_empty),
            expanded = false,
            status = stringResource(R.string.situations_my_patterns_count, 5),
        )
    }
}

@Preview
@Composable
private fun GroupLabelPreview() {
    AtemkraftTheme {
        GroupLabel(title = stringResource(R.string.family_downregulate), color = FamilyVagal) {
            AppIconButton(onClick = {}) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_show_all), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
