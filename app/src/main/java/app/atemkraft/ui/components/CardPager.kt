package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Anteil einer Karteikarte an der Inhaltsbreite (0,86): Von der nächsten Karte ragt ein Streifen
 * herein (gut 12 % samt Rand), der zeigt, dass es weitergeht – mehr wäre Unruhe, weniger übersähe man.
 */
private const val PAGE_FRACTION = 0.86f

/**
 * Karteikarten (Nutzer 2026-10-31, Tab „Eigene“): eine Karte je Seite in der Inhaltsbreite, von der
 * nächsten ragt ein Streifen herein; Wischen rastet auf jeder Karte ein (`HorizontalPager`). Die
 * Reihe reicht bis an die Bildschirmkanten (`bleed`), die erste Karte steht an der Textkante.
 *
 * **Alle Karten gleich hoch:** Vor dem Pager misst eine stille Probe jede Karte in der Kartenbreite
 * (ohne Semantik, nie platziert); die höchste setzt die Höhe – beim Wischen springt nichts. Jede
 * [page] füllt ihre Fläche (`fillMaxSize`), damit Karten gleich groß erscheinen.
 *
 * Darunter (`GapSmall`) eine Zeile: links leise die Lage „2 / 7“ (`bodySmall` `SECONDARY`), TalkBack
 * hört „Karte 2 von 7“ (bei einer Karte entfällt sie); rechts mit [onAll] der Verweis [allLabel]
 * („Alle 4 ›“, `AppTextButton` mit [ForwardLinkText] in `secondary`, bündig). Mit [onAll] ist auch
 * die letzte Karte „Alle ansehen ›“ ([AllCard]) – wer bis zum Ende wischt, kommt dorthin weiter.
 * [pageCount] zählt nur die Einträge. Den Zustand ([state], `rememberPagerState` mit
 * [pagerPageCount]) hält der Aufrufer, damit er einen Tab-Wechsel übersteht.
 */
@Composable
fun CardPager(
    pageCount: Int,
    state: PagerState,
    modifier: Modifier = Modifier,
    key: ((Int) -> Any)? = null,
    allLabel: String? = null,
    onAll: (() -> Unit)? = null,
    page: @Composable (Int) -> Unit,
) {
    val pages = pagerPageCount(pageCount, onAll != null)
    val content: @Composable (Int) -> Unit = { index ->
        if (index < pageCount) page(index) else AllCard(onClick = onAll ?: {})
    }
    Column(modifier = modifier.fillMaxWidth()) {
        SubcomposeLayout(Modifier.bleed(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)) { constraints ->
            val padding = Dimens.ScreenPadding.roundToPx()
            val pageWidth = ((constraints.maxWidth - 2 * padding) * PAGE_FRACTION).roundToInt().coerceAtLeast(0)
            val probe = Constraints(minWidth = pageWidth, maxWidth = pageWidth)
            val tallest = subcompose("probe") {
                // Die Probe wird nur gemessen, nie platziert: ohne Semantik, sonst stünde jede Karte doppelt im Baum.
                repeat(pages) { index -> Box(Modifier.clearAndSetSemantics {}) { content(index) } } // Abweichung A11Y-02: stille Messprobe
            }.maxOfOrNull { it.measure(probe).height } ?: 0
            val pager = subcompose("pager") {
                HorizontalPager(
                    state = state,
                    modifier = Modifier.height(tallest.toDp()),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                    pageSize = PageSize.Fixed(pageWidth.toDp()),
                    pageSpacing = Dimens.ListGap,
                    key = { index -> if (index < pageCount) key?.invoke(index) ?: index else ALL_PAGE_KEY },
                ) { index ->
                    Box(Modifier.fillMaxSize()) { content(index) }
                }
            }.first().measure(constraints.copy(minHeight = 0))
            layout(pager.width, pager.height) { pager.place(0, 0) }
        }
        if (pageCount > 1 || onAll != null) {
            Spacer(Modifier.height(Dimens.GapTiny))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val position = (state.currentPage + 1).coerceAtMost(pageCount)
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

/** Seiten des Pagers: die Einträge und mit „Alle ansehen“ eine Karte mehr. */
fun pagerPageCount(entries: Int, withAll: Boolean): Int = entries + if (withAll) 1 else 0

private const val ALL_PAGE_KEY = "all"

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
