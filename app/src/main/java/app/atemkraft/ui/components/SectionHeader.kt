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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.isSpecified
import app.atemkraft.R
import app.atemkraft.domain.splitAtDots
import app.atemkraft.domain.withoutLeadingDot
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.HeadingRing
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Farbe des Kopfes an Stelle [index] eines Screens (FARBE-07): reihum durch [HeadingRing], der
 * oberste Kopf ist 0. Der Index ist die Lage des Kopfes, ausdrücklich gesetzt, kein Zähler der
 * Komposition – ein zugeklappter Abschnitt behält seine Farbe. Die Farbe trägt keine Bedeutung.
 * Im Atmen-Tab tragen die Köpfe die Familienfarben; nur „Alle Übungen“ über der Filterreihe steht
 * in `headingColor(0)`.
 */
fun headingColor(index: Int): Color = HeadingRing[index.mod(HeadingRing.size)]

/**
 * Einheitlicher Abschnitts-Header: farbiger Akzentbalken + Titel. App-weit genutzt (Atmen, Eigene,
 * Meditation, Atemlehre, Pranayama, Glossar, Logbuch), damit Sektionsüberschriften überall
 * identisch lesen. [color] kommt aus [headingColor] (FARBE-07), im Atmen-Tab aus der Familie.
 *
 * Abstände: Über einem festen Kopf steht [top] (Standard `SectionHeaderTop`; kleiner nur, wo ein
 * Bildschirm ohne Scrollen auskommen muss). **Unten trägt der Kopf keinen Abstand:** Kopf → Inhalt
 * ist überall `HeaderGap` – in Listen über `spacedBy(ListGap)`, in Spalten über
 * [CollapsibleSection]. So steht der erste Inhalt unter jedem Kopf gleich weit.
 *
 * [trailingAction] ist eine optionale Aktion für den ganzen Abschnitt (z. B. „Muster neu
 * generieren“, „Neues Muster“), rechts als `AppIconButton`, ihr Glyph bündig an der Inhaltskante
 * ([flushEnd]). Sie steht mittig auf der ersten Titelzeile; ihre 48-dp-Tippfläche ragt dabei in den
 * Abstand darunter, ohne den Kopf höher zu machen – der Abschnitt rückt nicht nach unten. Mit
 * [expanded] stehen Caret und Aktion zusammen: Der Caret bleibt am Titel, die Aktion rechts davon
 * an der Inhaltskante; die Tippfläche des Auf- und Zuklappens endet vor der Aktion, beide sind
 * eigene Bedienelemente („Eigene Kreationen“ im Tab „Eigene“).
 *
 * [teaser] steht als ganzer Satz unter dem Titel (nie gekürzt, LAYOUT-03). Mit [expanded]
 * wird der Kopf zur aufklappbaren Zeile (MUSTER-03): Caret am Titel, die ganze Zeile samt
 * Teaser ist die Tippfläche (≥ 48 dp, Rolle Button, Ausgeklappt/Eingeklappt), den Inhalt
 * zeigt der Aufrufer darunter (über [CollapsibleSection]). So bleibt ein langer Tab eine kurze
 * Übersicht, ohne neue Fläche – der Abschnittskopf wechselt nur seinen Zustand. `null` heißt:
 * nicht aufklappbar.
 *
 * Mit [onClick] öffnet der Kopf etwas (Bottom Sheet, MUSTER-11: „Klang & Stimme“ im
 * Meditations-Tab): dieselbe Zeile wie aufklappbar, aber mit [ForwardChevron] statt Caret, ohne
 * Zustand. TalkBack liest sie als eine Schaltfläche aus Titel und Teaser, die Trennpunkte als
 * Kommas („Klang & Stimme, Gong Start & Ende, ohne Stimme, Schaltfläche“).
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
    color: Color,
    modifier: Modifier = Modifier,
    teaser: String? = null,
    expanded: Boolean? = null,
    onExpandedChange: (Boolean) -> Unit = {},
    status: String? = null,
    statusDescription: String? = null,
    smallStatus: Boolean = false,
    top: Dp = Dimens.SectionHeaderTop,
    onClick: (() -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val titleStyle = MaterialTheme.typography.titleLarge
    val density = LocalDensity.current
    // Gemessen statt aus lineHeight: ab Android 14 wächst die Schrift nichtlinear (rememberFirstLineHeight).
    val titleLine = rememberFirstLineHeight(titleStyle)
    // Zeile wie aufklappbar: Tippfläche ohne den Leerraum darüber, innen oben und unten GapSmall.
    val tappable = expanded != null || onClick != null
    val toggle = if (onClick != null) {
        val spoken = listOfNotNull(title, teaser?.splitAtDots()?.joinToString(", ")).joinToString(", ")
        Modifier
            .padding(top = Dimens.GapSmall)
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Die Tippfläche des Auf- und Zuklappens liegt auf der Spalte, nicht auf der ganzen Zeile:
        // So steckt eine Kopf-Aktion daneben nicht in einem zweiten Button.
        Column(modifier = Modifier.weight(1f).then(toggle).then(inner)) {
            // Balken und Caret stehen mittig neben der ersten Titelzeile, nicht mittig am ganzen
            // Block: Rutscht der Status darunter (bis zu drei Zeilen), bliebe der Balken sonst
            // neben dem Status stehen.
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            ) {
                Box(Modifier.height(titleLine), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            // Balken so hoch wie die Schrift – folgt der Systemschriftgröße.
                            .size(width = Sizes.AccentBar, height = with(density) { titleStyle.fontSize.toDp() })
                            .clip(CircleShape)
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
                    isHeading = onClick == null,
                )
                if (onClick != null) {
                    Box(Modifier.heightIn(min = titleLine), contentAlignment = Alignment.Center) {
                        ForwardChevron(tint = color)
                    }
                } else if (expanded != null) {
                    Box(Modifier.heightIn(min = titleLine), contentAlignment = Alignment.Center) {
                        ExpandCaret(expanded = expanded, tint = color)
                    }
                }
            }
            if (teaser != null) {
                SecondaryText(teaser, modifier = Modifier.padding(top = Dimens.GapSmall))
            }
        }
        // Glyph an der Inhaltskante wie der Caret aufklappbarer Köpfe (LAYOUT-07).
        if (trailingAction != null) {
            // Mitte der Tippfläche auf der Mitte der ersten Titelzeile (wie Balken und Caret).
            val above = if (!tappable) top else Dimens.GapSmall * 2
            Box(Modifier.centeredOverflow(above + titleLine / 2).flushEnd()) { trailingAction() }
        }
    }
}

/**
 * Stellt den Inhalt mit seiner Mitte auf die Höhe [center] unter der Oberkante und meldet nur so
 * viel Höhe, dass er höchstens [MAX_OVERHANG] unter den Kopf ragt: Die 48-dp-Tippfläche einer
 * Kopf-Aktion liegt so im Abstand `HeaderGap` unter dem Kopf, statt ihn höher zu machen – aber
 * nie bis an den ersten Inhalt (A11Y-05). Bei kleiner Schrift (0,85) endete sie sonst genau an der
 * Oberkante der ersten Karte; dann wird der Kopf um die fehlenden 2 dp höher. Kein `offset` – die
 * Breite zählt für die Nachbarn wie gewohnt.
 */
private fun Modifier.centeredOverflow(center: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val y = (center.roundToPx() - placeable.height / 2).coerceAtLeast(0)
    val height = (y + placeable.height - MAX_OVERHANG.roundToPx()).coerceAtLeast(0)
    layout(placeable.width, height) {
        placeable.placeRelative(0, y)
    }
}

/** Größter Überstand einer Kopf-Aktion unter den Kopf: der Abstand zum Inhalt bis auf eine Haarlinie. */
private val MAX_OVERHANG = Dimens.HeaderGap - Dimens.GapHairline

/**
 * Abschnitt mit Kopf und Inhalt (MUSTER-03): [SectionHeader] und darunter [content], in einer
 * Spalte mit `animateContentSize`, damit das Auf- und Zuklappen die Höhe animiert.
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
    color: Color,
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
    Column(modifier = modifier.animateContentSize()) {
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
            top = headerTop,
            trailingAction = trailingAction,
        )
        if (expanded != false) {
            Column(
                modifier = Modifier.padding(top = headerGap),
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
                content = content,
            )
        }
    }
}

/**
 * Titel eines [SectionHeader] mit optionalem [status] dahinter. Passt der Status in einer Zeile
 * hinter den einzeiligen Titel, steht er dort auf dessen Grundlinie; sonst steht er unter dem
 * Titel und verliert seinen führenden Punkt („· angepasst“ → „angepasst“, [withoutLeadingDot]),
 * damit keine Zeile mit einem Satzzeichen beginnt (LAYOUT-03); passt er auch dort nicht in eine
 * Zeile, steht jeder Teil zwischen den Punkten in einer eigenen Zeile, ohne Punkt
 * ([splitAtDots]). Gemessen wird vorab ([rememberWidestTextWidth]), so entsteht nur die gezeigte
 * Variante (ein Knoten für TalkBack). Ohne [isHeading] ist der Titel keine Überschrift (Kopf als
 * Schaltfläche, `onClick`).
 */
@Composable
private fun TitleWithStatus(
    title: String,
    color: Color,
    status: String?,
    statusDescription: String?,
    statusStyle: TextStyle,
    modifier: Modifier = Modifier,
    isHeading: Boolean = true,
) {
    val titleStyle = MaterialTheme.typography.titleLarge
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
            // Passt er auch darunter nicht in eine Zeile, steht jeder Teil („24 s“, „2,5
            // Atemzüge/min“) in einer eigenen Zeile – sonst endete eine Zeile auf „·“.
            val shown = if (belowWidth.roundToPx() > constraints.maxWidth) below.splitAtDots().joinToString("\n") else below
            val statusPlaceable = subcompose(StatusSlot.BELOW) {
                WholeWordText(text = shown, style = statusStyle, color = statusColor, modifier = statusSemantics)
            }.first().measure(loose)
            // Breite in den Grenzen melden: Unter der Mindestbreite (weight) zentrierte das Layout
            // den Block sonst, und Titel und Status rückten vom Balken ab.
            layout(constraints.constrainWidth(maxOf(titlePlaceable.width, statusPlaceable.width)), titlePlaceable.height + statusPlaceable.height) {
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
            title = stringResource(R.string.own_creations),
            color = headingColor(0),
            teaser = stringResource(R.string.home_subtitle),
            expanded = false,
            status = stringResource(R.string.situations_my_patterns_count, 5),
        )
    }
}
