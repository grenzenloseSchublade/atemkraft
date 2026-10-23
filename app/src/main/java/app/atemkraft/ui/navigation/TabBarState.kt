package app.atemkraft.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import app.atemkraft.ui.theme.Dimens

/**
 * Zustand der Tab-Leiste auf Seiten in der Tiefe (Nutzer 2026-10-23): Jede neue Seite startet mit
 * der Leiste ([enter]); scrollt der Inhalt mehr als [threshold] px nach unten, klappt sie ein
 * ([collapsed]). Der Knopf auf der Oberkante der untersten Leiste ([TabBarToggle]) klappt sie auf
 * und wieder zu ([toggle]) – danach entscheidet der Nutzer, Scrollen klappt auf dieser Seite nicht
 * mehr ein. Die Scroll-Wege aller Seiten misst [connection] zentral im Rahmen der App; auf den fünf
 * Tabs selbst bleibt die Leiste immer stehen (das entscheidet der Aufrufer).
 *
 * Wo der Knopf sitzt: auf der Oberkante einer Startleiste, wenn die Seite eine hat
 * (`PinnedActionBar` meldet sich mit [anchorPinnedBar] an), sonst auf der Oberkante der unteren
 * Leisten ([bottomBarTop]). Beides in px im Fenster.
 */
@Stable
class TabBarState(private val threshold: Float) {
    var collapsed by mutableStateOf(false)
        private set
    private var byHand = false
    private var travelled = 0f

    /** Oberkante der unteren Leisten (Mini-Leiste, Tab-Leiste bzw. Systemnavigation), px im Fenster. */
    var bottomBarTop by mutableFloatStateOf(Float.NaN)

    // Startleisten der gerade gezeigten Seiten: Beim Seitenwechsel stehen kurz zwei da, es gilt die
    // zuletzt angemeldete.
    private val pinnedOwners = mutableStateListOf<Any>()
    private val pinnedTops = mutableStateMapOf<Any, Float>()

    /** Ob der Knopf auf einer Startleiste sitzt (sonst auf den unteren Leisten). */
    val anchoredToPinnedBar: Boolean
        get() = pinnedOwners.lastOrNull()?.let { it in pinnedTops } == true

    /** Oberkante, auf der der Knopf sitzt: die jüngste Startleiste, sonst die unteren Leisten. */
    val anchorTop: Float
        get() = pinnedOwners.lastOrNull()?.let { pinnedTops[it] } ?: bottomBarTop

    /** Neue Seite: Leiste da bzw. eingeklappt ([startCollapsed], Muster-Editor), noch keine Wahl von Hand. */
    fun enter(startCollapsed: Boolean) {
        collapsed = startCollapsed
        byHand = false
        travelled = 0f
    }

    /** Knopf angetippt: Leiste auf bzw. zu; Scrollen klappt sie auf dieser Seite nicht mehr ein. */
    fun toggle() {
        collapsed = !collapsed
        byHand = true
    }

    /** Eine Startleiste ([owner]) mit ihrer Oberkante [top] (px im Fenster) an- bzw. nachmelden. */
    fun anchorPinnedBar(owner: Any, top: Float) {
        if (owner !in pinnedOwners) pinnedOwners += owner
        pinnedTops[owner] = top
    }

    /** Startleiste [owner] verschwindet mit ihrer Seite. */
    fun releasePinnedBar(owner: Any) {
        pinnedOwners -= owner
        pinnedTops -= owner
    }

    /** Zählt nur, was der Inhalt wirklich nach unten gescrollt hat (negatives `consumed.y`). */
    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!byHand && !collapsed && consumed.y < 0f) {
                travelled -= consumed.y
                if (travelled > threshold) collapsed = true
            }
            return Offset.Zero
        }
    }
}

/** Der [TabBarState] der App für Seiten in der Tiefe; null auf den Tabs, in Vorschauen und Tests. */
val LocalTabBarState = staticCompositionLocalOf<TabBarState?> { null }

/** Der [TabBarState] der App: klappt nach [Dimens.SectionGap] Scrollweg ein, neu je Bildschirmdichte. */
@Composable
fun rememberTabBarState(): TabBarState {
    val density = LocalDensity.current
    return remember(density) { TabBarState(threshold = with(density) { Dimens.SectionGap.toPx() }) }
}
