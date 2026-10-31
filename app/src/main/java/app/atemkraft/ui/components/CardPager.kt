package app.atemkraft.ui.components

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import kotlin.math.roundToInt

/**
 * Anteil der schmalen Kachel vor den Karten ([CardPager] `leading`) an der Inhaltsbreite (0,25).
 * Die Karten bekommen den Rest: Kachel, Abstand und erste Karte füllen genau die Inhaltsbreite.
 */
private const val LEADING_FRACTION = 0.25f

/**
 * Ab dieser Schriftgröße (1,5) sind die Karten 86 % der Inhaltsbreite breit statt Inhalt − Kachel:
 * Neben Stift und Löschen bliebe dem Titel sonst zu wenig Platz, Namen wie „Tagesmuster“ brächen im
 * Wort (LAYOUT-03). Die erste Karte steht dann neben der Kachel angeschnitten.
 */
private const val LARGE_FONT = 1.5f
private const val LARGE_FONT_PAGE_FRACTION = 0.86f

/**
 * Karteikarten (Nutzer 2026-10-31, Tab „Meins“): eine einrastende Reihe (`LazyRow` mit
 * `SnapPosition.Start`), die bis an die Bildschirmkanten reicht (`bleed`); jede Karte rastet an der
 * Textkante ein, von der nächsten ragt ein Streifen herein.
 *
 * **Breiten:** Eine Karte ist so breit wie Inhaltsbreite − Kachel − `ListGap`, die Kachel ein
 * Viertel der Inhaltsbreite (360 dp: Inhalt 328, Kachel 82, Karte 234 ≈ 71 %). Mit [leading] steht
 * ganz links eine schmale Kachel (Eigene Kreationen: „+ Neu“), daneben ist die erste Karte vollständig
 * zu sehen (Eigentümer 2026-10-31: „Neues Muster“ soll nicht als erste Karte kommen). Ohne [leading]
 * (Tut mir gut) ist die Karte gleich breit, die nächste ragt weiter herein – beide Sammlungen wirken
 * gleich. Wischen zurück zeigt wieder Kachel und erste Karte. Ab Schrift 1,5 ([LARGE_FONT]) sind
 * Karten 86 % breit, damit Namen neben Stift und Löschen nicht im Wort brechen; die erste Karte steht
 * dann angeschnitten neben der Kachel.
 *
 * **Alle Karten gleich hoch:** Vor der Reihe misst eine stille Probe jede Karte in der Kartenbreite
 * (ohne Semantik, nie platziert); die höchste setzt die Höhe – beim Wischen springt nichts. Jede
 * [page] füllt ihre Fläche (`fillMaxSize`), die Kachel ebenso. Mit [sameHeightAs] misst die Probe auch
 * diese Karten mit: Mehrere Reihen, zwischen denen man umschaltet (Kreationen | Tut mir gut), stehen
 * gleich hoch, beim Umschalten springt nichts (Eigentümer 2026-10-31).
 *
 * Darunter (`GapTiny`) eine Zeile: links leise die Lage „2 / 7“ (`bodySmall` `SECONDARY`, nur die
 * Einträge – Kachel und „Alle ansehen“ zählen nicht), TalkBack hört „Karte 2 von 7“ (bei einer Karte
 * entfällt sie); rechts mit [onAll] der Verweis [allLabel] („Alle 4 ›“, `AppTextButton` mit
 * [ForwardLinkText] in `secondary`, bündig). Mit [onAll] ist auch die letzte Karte „Alle ansehen ›“
 * ([AllCard]). [pageCount] zählt nur die Einträge. Den Zustand ([state], `rememberLazyListState`) hält
 * der Aufrufer, damit er Umschalten und Tab-Wechsel übersteht.
 */
@Composable
fun CardPager(
    pageCount: Int,
    state: LazyListState,
    modifier: Modifier = Modifier,
    key: ((Int) -> Any)? = null,
    allLabel: String? = null,
    onAll: (() -> Unit)? = null,
    sameHeightAs: CardSet? = null,
    leading: (@Composable () -> Unit)? = null,
    page: @Composable (Int) -> Unit,
) {
    val pages = pagerPageCount(pageCount, onAll != null)
    val content: @Composable (Int) -> Unit = { index ->
        if (index < pageCount) page(index) else AllCard(onClick = onAll ?: {})
    }
    val lead = if (leading != null) 1 else 0
    Column(modifier = modifier.fillMaxWidth()) {
        SubcomposeLayout(Modifier.bleed(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)) { constraints ->
            val padding = Dimens.ScreenPadding.roundToPx()
            val gap = Dimens.ListGap.roundToPx()
            val contentWidth = (constraints.maxWidth - 2 * padding).coerceAtLeast(0)
            val leadingWidth = (contentWidth * LEADING_FRACTION).roundToInt()
            val pageWidth = if (fontScale >= LARGE_FONT) {
                (contentWidth * LARGE_FONT_PAGE_FRACTION).roundToInt()
            } else {
                (contentWidth - leadingWidth - gap).coerceAtLeast(0)
            }
            val probe = Constraints(minWidth = pageWidth, maxWidth = pageWidth)
            val tallest = subcompose("probe") {
                // Die Probe wird nur gemessen, nie platziert: ohne Semantik, sonst stünde jede Karte doppelt im Baum.
                repeat(pages) { index -> Box(Modifier.clearAndSetSemantics {}) { content(index) } } // Abweichung A11Y-02: stille Messprobe
                sameHeightAs?.let { set -> repeat(set.count) { index -> Box(Modifier.clearAndSetSemantics {}) { set.card(index) } } } // Abweichung A11Y-02: stille Messprobe
            }.maxOfOrNull { it.measure(probe).height } ?: 0
            val row = subcompose("row") {
                LazyRow(
                    state = state,
                    modifier = Modifier.height(tallest.toDp()),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
                    flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Start),
                ) {
                    if (leading != null) {
                        item(key = LEADING_KEY) { Box(Modifier.width(leadingWidth.toDp()).fillMaxSize()) { leading() } }
                    }
                    items(pages, key = { index -> if (index < pageCount) key?.invoke(index) ?: index else ALL_PAGE_KEY }) { index ->
                        Box(Modifier.width(pageWidth.toDp()).fillMaxSize()) { content(index) }
                    }
                }
            }.first().measure(constraints.copy(minHeight = 0))
            layout(row.width, row.height) { row.place(0, 0) }
        }
        if (pageCount > 1 || onAll != null) {
            Spacer(Modifier.height(Dimens.GapTiny))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Die eingerastete Karte steht links; die Kachel gilt als Lage 1, „Alle ansehen“ als letzte.
                val position by remember(state, lead, pageCount) {
                    derivedStateOf { (state.firstVisibleItemIndex - lead + 1).coerceIn(1, pageCount.coerceAtLeast(1)) }
                }
                if (pageCount > 1) {
                    val spoken = stringResource(R.string.cd_card_position, position, pageCount)
                    Text(
                        text = stringResource(R.string.card_position, position, pageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                        modifier = Modifier.semantics { contentDescription = spoken },
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onAll != null && allLabel != null) {
                    // Bündig: Das Polster des Text-Buttons ragt in den Rand.
                    AppTextButton(onClick = onAll, modifier = Modifier.flushEnd()) {
                        ForwardLinkText(text = allLabel, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/**
 * Karten, an deren Höhe sich ein [CardPager] mit ausrichtet ([CardPager] `sameHeightAs`): [count]
 * Karten, [card] baut die Karte Nummer i (nur gemessen, nie gezeigt; Klicks dürfen leer sein).
 */
class CardSet(val count: Int, val card: @Composable (Int) -> Unit)

/** Karten der Reihe: die Einträge und mit „Alle ansehen“ eine Karte mehr (ohne die Kachel davor). */
fun pagerPageCount(entries: Int, withAll: Boolean): Int = entries + if (withAll) 1 else 0

private const val ALL_PAGE_KEY = "all"
private const val LEADING_KEY = "leading"

/** Letzte Karte „Alle ansehen ›“: ruhige Fläche wie ein Leerzustand, der Verweis mittig. */
@Composable
private fun AllCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(Dimens.CardPadding), contentAlignment = Alignment.Center) {
            ForwardLinkText(text = stringResource(R.string.own_all_card), color = MaterialTheme.colorScheme.secondary)
        }
    }
}
