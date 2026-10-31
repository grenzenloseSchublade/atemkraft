package app.atemkraft.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.scrolledHairline
import app.atemkraft.ui.theme.Dimens

/** Gruppe, in der man gerade liest: Name und Farbe wie ihr Gruppenlabel bzw. ihre Überschrift. */
@Immutable
internal data class StuckGroup(val title: String, val color: Color)

/** Die Leiste klebt; [group] fehlt, solange unter ihr noch keine Gruppe beginnt (Filterreihe). */
@Immutable
internal data class Stuck(val group: StuckGroup?)

/**
 * Was oben klebt (Nutzer 2026-10-31): nichts, solange der Kopf [anchorKey] („Übungen“ bzw.
 * „Passende Übungen“) noch ganz zu sehen ist; danach die Gruppe des Eintrags, der unter der
 * Unterkante der Leiste ([barHeight] px) liegt, nachgeschlagen in [groups] (Schlüssel der Einträge).
 * Gruppen stehen nur unter dem Kopf: Ist er weggescrollt und liegt eine Gruppe unter der Leiste,
 * klebt sie.
 */
internal fun stuckAt(listState: LazyListState, anchorKey: Any, groups: Map<Any, StuckGroup>, barHeight: Int): Stuck? {
    val items = listState.layoutInfo.visibleItemsInfo
    val anchor = items.firstOrNull { it.key == anchorKey }
    val group = items.firstOrNull { it.offset + it.size > barHeight }?.let { groups[it.key] }
    return when {
        anchor != null -> if (anchor.offset < 0) Stuck(group) else null
        group != null -> Stuck(group)
        else -> null
    }
}

/**
 * Klebende Leiste über der Liste des Atmen-Tabs: „Übungen ·“ weiß, dahinter die Gruppe in ihrer
 * Farbe („Beruhigen“ in `FamilyVagal`; ein Anlass weiß), `titleSmall` wie das [GroupLabel][
 * app.atemkraft.ui.components.GroupLabel]. Die nächste Gruppe ersetzt sie, sobald ihr Label unter
 * die Leiste rutscht – ohne Bewegung (MOTION-02). Passt beides bei großer Schrift nicht in eine
 * Zeile, rückt die Gruppe ganz in die zweite (kein Wortbruch). Unten die Haarlinie des Kopfs; der
 * Kopf darüber zeigt seine dann nicht.
 *
 * Für TalkBack unsichtbar: Kopf und Gruppenlabel stehen als Überschriften in der Liste. Wischen auf
 * der Leiste scrollt die Liste ([listState]), ein Tipp fällt nicht auf die Karte darunter.
 */
@Composable
internal fun StuckSectionBar(title: String, group: StuckGroup?, listState: LazyListState, onHeight: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeight(it.height) }
            .background(MaterialTheme.colorScheme.background)
            .scrolledHairline(true)
            .scrollable(listState, Orientation.Vertical, reverseDirection = true)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.GapSmall),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
    ) {
        val style = MaterialTheme.typography.titleSmall
        WholeWordText(if (group != null) "$title ·" else title, style = style, color = MaterialTheme.colorScheme.onBackground, modifier = Hidden)
        if (group != null) WholeWordText(group.title, style = style, color = group.color, modifier = Hidden)
    }
}

/** Für TalkBack unsichtbar, ohne `clearAndSetSemantics`: Die Text-Layouts bleiben für die Umbruchprüfung sichtbar. */
private val Hidden = Modifier.semantics { hideFromAccessibility() }
