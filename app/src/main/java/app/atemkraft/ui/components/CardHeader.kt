package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.Dimens

// Test-Tags der Layout-Grammatik (STYLEGUIDE §4.5): Das Gate ANATOMIE im ScreenshotTest liest
// über sie, wo Titel, Aktionen und Zeichen eines Bausteins liegen, und prüft ihre Maße.

/** Titelzeile einer Karte ([CardHeader]); darunter läuft der Inhalt in voller Breite. */
const val CARD_HEADER_TAG = "anatomie-kartenkopf"

/** Kopf- oder Labelzeile eines Abschnitts ([SectionHeader], [GroupLabel]). */
const val TITLE_ROW_TAG = "anatomie-titelzeile"

/** Der Text, auf dessen erster Zeile Aktionen und Zeichen mittig stehen. */
const val TITLE_ROW_TITLE_TAG = "anatomie-titel"

/** Hülle der Aktionen am Zeilenende (Icon-Knöpfe mit 48-dp-Tippfläche). */
const val TITLE_ROW_ACTIONS_TAG = "anatomie-aktionen"

/** Hülle eines nicht klickbaren Zeichens am Zeilenende (Atemkreis, Chevron). */
const val TITLE_ROW_INDICATOR_TAG = "anatomie-zeichen"

/** Zeile mit Steuerelement (Schalter, Stepper, Suchfeld): mindestens 48 dp hoch. */
const val CONTROL_ROW_TAG = "anatomie-steuerzeile"

/**
 * Titelzeile einer Karte – die Anatomie „Karte mit Aktionen“ (STYLEGUIDE §4.5) an einer Stelle,
 * damit kein Aufrufer sie nachbaut: [title] mit optionalen [chips] ([TitleWithChips]) links, am
 * Zeilenende entweder ein nicht klickbares [indicator] (Atemkreis „tut mir gut“, Chevron) oder
 * [actionCount] Icon-Knöpfe in [actions] (höchstens zwei, zerstörende außen). Die Knöpfe stehen
 * mit der Glyph-Mitte auf der Mitte der ersten Titelzeile ([rememberFirstLineHeight]) und mit dem
 * Glyph bündig an der Inhaltskante ([flushEnd], `inCard`); nur die Titelzeile hält ihre Breite
 * frei, der Inhalt darunter läuft in voller Breite unter den Knöpfen durch – die Karte wird durch
 * Aktionen nicht höher ([centeredOverflow] ohne Grenze: Die Tippfläche ragt in den Inhalt).
 *
 * Der Aufrufer setzt Karte, Rand und `CardPadding` (KOMP-03) und hängt den Inhalt als Geschwister
 * darunter an; `[CardHeader]` ist die erste Zeile dieser Spalte. Mit [heading] ist der Titel für
 * TalkBack eine Überschrift (Einstellungen, A11Y-01) – nicht in klickbaren Karten, deren Knoten
 * den Titel schon als Namen trägt. [marks] setzt Sanskrit kursiv (`sanskritText`) für Titel aus
 * Inhaltstexten; Namen von Übungen und Mustern stehen wörtlich (Standard).
 *
 * Ohne Karte gilt dieselbe Zeile für den Kopf eines Expanders ([ExpanderSection], [ExpanderCard]):
 * Titel und Caret als [indicator], der Teaser läuft darunter in voller Breite.
 */
@Composable
fun CardHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    chips: (@Composable RowScope.() -> Unit)? = null,
    indicator: (@Composable () -> Unit)? = null,
    actionCount: Int = 0,
    heading: Boolean = false,
    marks: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
) {
    require(actionCount in 0..2) { "höchstens zwei Aktionen in einer Kartenzeile (KOMP-02)" }
    val titleStyle = MaterialTheme.typography.titleMedium
    val titleSemantics = if (heading) Modifier.semantics { heading() } else Modifier
    val titleLine = rememberFirstLineHeight(titleStyle)
    Row(
        modifier = modifier.fillMaxWidth().testTag(CARD_HEADER_TAG),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        TitleWithChips(
            title = title,
            style = titleStyle,
            color = titleColor,
            modifier = Modifier.weight(1f),
            titleModifier = Modifier.testTag(TITLE_ROW_TITLE_TAG).then(titleSemantics),
            marks = marks,
            chips = chips,
        )
        if (indicator != null) {
            // Box in Höhe einer Titelzeile: Das Zeichen steht mittig zur ersten Zeile, auch wenn
            // Titel und Chips umbrechen.
            Box(
                modifier = Modifier.height(titleLine).testTag(TITLE_ROW_INDICATOR_TAG),
                contentAlignment = Alignment.Center,
            ) { indicator() }
        }
        if (actions != null) {
            Box(
                Modifier
                    .centeredOverflow(titleLine / 2, maxOverhang = null)
                    .flushEnd(inCard = true)
                    .testTag(TITLE_ROW_ACTIONS_TAG),
            ) { actions() }
        }
    }
}

/**
 * Stellt den Inhalt mit seiner Mitte auf die Höhe [center] unter der Oberkante und meldet nur so
 * viel Höhe, dass er höchstens [maxOverhang] darunter ragt. `null` (Kartenzeile): gar keine Höhe
 * und keine Grenze nach oben – die 48-dp-Tippfläche einer Kartenaktion ragt oben ins Polster der
 * Karte und unten über den Inhalt, die Karte wird nicht höher. Für Abschnittsköpfe begrenzt
 * [maxOverhang] den Überstand auf den Abstand zum Inhalt, damit die Tippfläche nie bis an die
 * erste Karte reicht (A11Y-05); fehlt Platz, wird der Kopf um den Rest höher. Kein `offset` – die
 * Breite zählt für die Nachbarn wie gewohnt.
 */
internal fun Modifier.centeredOverflow(center: Dp, maxOverhang: Dp?): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val centered = center.roundToPx() - placeable.height / 2
    val y = if (maxOverhang == null) centered else centered.coerceAtLeast(0)
    val height = if (maxOverhang == null) 0 else (y + placeable.height - maxOverhang.roundToPx()).coerceAtLeast(0)
    layout(placeable.width, height) {
        placeable.placeRelative(0, y)
    }
}
