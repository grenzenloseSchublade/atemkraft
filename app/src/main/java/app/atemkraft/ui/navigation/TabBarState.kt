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

/**
 * Zustand der Tab-Leiste auf Seiten in der Tiefe (Nutzer 2026-10-23): Wer von einem Tab in die
 * Tiefe geht, sieht sie eingeklappt ([collapsed]). Der Knopf auf der Oberkante der untersten Leiste
 * ([TabBarToggle]) klappt sie auf und wieder zu ([toggle]); diese Wahl gilt auf allen weiteren Seiten
 * der Tiefe, vor und zurück, bis der Nutzer wieder auf einem Tab steht ([enter]). Auf den vier Tabs
 * selbst steht die Leiste immer (das entscheidet der Aufrufer).
 *
 * Wo der Knopf sitzt: auf der Oberkante einer Startleiste, wenn die Seite eine hat
 * (`PinnedActionBar` meldet sich mit [anchorPinnedBar] an), sonst auf der Oberkante der unteren
 * Leisten ([bottomBarTop]). Beides in px im Fenster.
 */
@Stable
class TabBarState {
    var collapsed by mutableStateOf(true)
        private set

    // Tab, zu dessen Stapel die zuletzt gezeigte Seite gehörte: Ein Tab-Wechsel, der gleich in einem
    // wiederhergestellten Stapel in der Tiefe landet, zählt wie der Weg von einem Tab.
    private var lastTab: Any? = null

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

    /**
     * Neue Seite: Auf einem Tab ([onTab]) oder nach einem Wechsel des Tabs ([tab]) gilt wieder
     * „eingeklappt“ für den nächsten Weg in die Tiefe; in der Tiefe bleibt die Wahl von Hand stehen.
     */
    fun enter(onTab: Boolean, tab: Any) {
        if (onTab || tab != lastTab) collapsed = true
        lastTab = tab
    }

    /** Knopf angetippt: Leiste auf bzw. zu, bis zur Rückkehr auf einen Tab. */
    fun toggle() {
        collapsed = !collapsed
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
}

/** Der [TabBarState] der App für Seiten in der Tiefe; null auf den Tabs, in Vorschauen und Tests. */
val LocalTabBarState = staticCompositionLocalOf<TabBarState?> { null }

/** Der [TabBarState] der App. */
@Composable
fun rememberTabBarState(): TabBarState = remember { TabBarState() }
