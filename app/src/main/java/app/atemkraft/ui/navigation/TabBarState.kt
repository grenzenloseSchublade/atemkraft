package app.atemkraft.ui.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
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
import androidx.compose.ui.input.pointer.PointerInputScope
import app.atemkraft.ui.theme.Sizes

/**
 * Zustand der Tab-Leiste auf Seiten in der Tiefe (Nutzer 2026-10-23): Wer von einem Tab in die
 * Tiefe geht, sieht sie eingeklappt ([collapsed]). Der Griff auf der Oberkante der untersten Leiste
 * ([TabBarToggle]) klappt sie auf und wieder zu ([toggle]), ebenso Wischen ([detectSwipe],
 * Nutzer 2026-10-30): nach oben auf der Startleiste oder am Griff öffnet ([show]), nach unten auf
 * der offenen Tab-Leiste oder am Griff schließt ([hide]). Diese Wahl gilt auf allen weiteren Seiten
 * der Tiefe, vor und zurück, bis der Nutzer wieder auf einem Tab steht ([enter]). Auf den vier Tabs
 * selbst steht die Leiste immer (das entscheidet der Aufrufer), Wischen ändert dort nichts
 * ([inDepth]).
 *
 * Wo der Griff sitzt: auf der Oberkante einer Startleiste, wenn die Seite eine hat
 * (`PinnedActionBar` meldet sich mit [anchorPinnedBar] an), sonst auf der Oberkante der unteren
 * Leisten ([bottomBarTop]). Beides in px im Fenster.
 */
@Stable
class TabBarState {
    var collapsed by mutableStateOf(true)
        private set

    /** Ob die gezeigte Seite in der Tiefe liegt (kein Tab): nur dort klappt Wischen die Leiste. */
    var inDepth by mutableStateOf(false)
        private set

    /**
     * Oberkante der Systemgesten am unteren Rand (px im Fenster): Was dort aufsetzt, gehört dem
     * System (Home, Zurück), nie dem Wischen der Tab-Leiste. Ohne Wert (Tests) gilt nichts als
     * Systemgeste.
     */
    var gestureZoneTop by mutableFloatStateOf(Float.POSITIVE_INFINITY)

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
        inDepth = !onTab
    }

    /** Griff angetippt: Leiste auf bzw. zu, bis zur Rückkehr auf einen Tab. */
    fun toggle() {
        collapsed = !collapsed
    }

    /** Nach oben gewischt: Leiste auf. */
    fun show() {
        collapsed = false
    }

    /** Nach unten gewischt: Leiste zu. */
    fun hide() {
        collapsed = true
    }

    /** Eine Startleiste ([owner]) mit ihrer Oberkante [top] (px im Fenster) an- bzw. nachmelden. */
    fun anchorPinnedBar(owner: Any, top: Float) {
        if (owner !in pinnedOwners) pinnedOwners += owner
        pinnedTops[owner] = top
    }

    /** Oberkante der Startleiste [owner] (px im Fenster), solange sie angemeldet ist. */
    fun pinnedTop(owner: Any): Float = pinnedTops[owner] ?: Float.NaN

    /** Startleiste [owner] verschwindet mit ihrer Seite. */
    fun releasePinnedBar(owner: Any) {
        pinnedOwners -= owner
        pinnedTops -= owner
    }

    /**
     * Wischen an den Leisten der Tiefe (Nutzer 2026-10-30: „mit von unten nach oben wischen im unteren
     * Bereich soll die Tab-Leiste geöffnet werden“): nach oben öffnet die Leiste ([opens]), nach unten
     * schließt sie ([closes]) – je nachdem, wo gewischt wird: Startleiste nur auf, offene Tab-Leiste nur
     * zu, Griff beides. Zentral in `PinnedActionBar`, [HideableTabBar] und [TabBarToggle]
     * (`with(state) { detectSwipe(…) }` in `pointerInput`); keine Seite tut etwas dafür.
     *
     * Gewertet wird erst nach dem Touch-Slop und nur in der Richtung, die gerade etwas bewirkt: Bis dahin
     * bleibt ein Tipp auf Start oder „Anpassen ›“ ein Tipp; erst ein Wischen verbraucht die Bewegung
     * (der Knopf darunter bricht dann ab). Die Leiste klappt, sobald der Finger [Sizes.TabBarSwipeDistance]
     * vom Aufsetzen entfernt ist, einmal je Geste. Nur in der Tiefe ([inDepth]); was in den
     * Systemgesten am unteren Rand aufsetzt ([gestureZoneTop]), bleibt dem System.
     * [windowTop] ist die Oberkante der wischbaren Fläche in px im Fenster.
     */
    suspend fun PointerInputScope.detectSwipe(opens: Boolean, closes: Boolean, windowTop: () -> Float) {
        val distance = Sizes.TabBarSwipeDistance.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!inDepth || windowTop() + down.position.y >= gestureZoneTop) return@awaitEachGesture
            val up = opens && collapsed
            val downward = closes && !collapsed
            if (!up && !downward) return@awaitEachGesture
            val slop = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                // Nur die Richtung, die etwas bewirkt, wird zum Wischen; die andere bleibt den Knöpfen.
                if ((up && overSlop < 0f) || (downward && overSlop > 0f)) change.consume()
            } ?: return@awaitEachGesture
            var done = false
            fun follow(y: Float) {
                val dy = y - down.position.y
                if (done) return
                if (up && dy <= -distance) {
                    done = true
                    show()
                } else if (downward && dy >= distance) {
                    done = true
                    hide()
                }
            }
            follow(slop.position.y)
            verticalDrag(slop.id) { change ->
                change.consume()
                follow(change.position.y)
            }
        }
    }
}

/** Der [TabBarState] der App für ihre Seiten (`PinnedActionBar` meldet sich an); null in Vorschauen und Tests. */
val LocalTabBarState = staticCompositionLocalOf<TabBarState?> { null }

/** Der [TabBarState] der App. */
@Composable
fun rememberTabBarState(): TabBarState = remember { TabBarState() }
