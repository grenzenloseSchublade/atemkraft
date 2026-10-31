package app.atemkraft.ui.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import app.atemkraft.ui.components.gestureThreshold
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.Sizes
import kotlin.math.abs

/**
 * Zustand der Tab-Leiste auf Seiten in der Tiefe (Nutzer 2026-10-23): Wer von einem Tab in die
 * Tiefe geht, sieht sie eingeklappt ([collapsed]). Der Griff auf der Oberkante der untersten Leiste
 * ([TabBarToggle]) klappt sie auf und wieder zu ([toggle]), ebenso Wischen ([detectSwipe],
 * Nutzer 2026-10-30): nach oben auf der Startleiste, im unteren Bereich einer Seite ohne Startleiste
 * ([detectBottomSwipe]) oder am Griff öffnet ([show]), nach unten auf der offenen Tab-Leiste oder am
 * Griff schließt ([hide]). Diese Wahl gilt auf allen weiteren Seiten
 * der Tiefe, vor und zurück, bis der Nutzer wieder auf einem Tab steht ([enter]). Auf den vier Tabs
 * selbst steht die Leiste immer (das entscheidet der Aufrufer), Wischen ändert dort nichts
 * ([inDepth]).
 *
 * Wo der Griff sitzt: auf der Oberkante einer Startleiste, wenn die Seite eine hat
 * (`PinnedActionBar` meldet sich mit [anchorPinnedBar] an), sonst auf der Oberkante der unteren
 * Leisten ([bottomBarTop]). Beides in px im Fenster.
 *
 * Wie weit die Leiste gerade offen ist, sagt [openness] (0 zu, 1 offen): Beim Wischen folgt sie dem
 * Finger, beim Loslassen rastet sie mit [Motion.TabBar] ein und übernimmt das Tempo des Fingers
 * (Eigentümer 2026-10-31, nach M3-Bottom-Sheet und Apple „Designing Fluid Interfaces“). Die
 * Animation führt [HideableTabBar] aus ([settleRequest]); der Zustand selbst hält keinen Scope (CODE-02).
 */
@Stable
class TabBarState {
    var collapsed by mutableStateOf(true)
        private set

    /** Wie weit die Leiste offen ist, 0…1; gelesen nur beim Auslegen ([HideableTabBar]), nie in der Komposition. */
    var openness by mutableFloatStateOf(1f)
        internal set

    /**
     * Bedienhilfe aktiv (TalkBack, Switch Access): Dann steht die Leiste auch in der Tiefe offen –
     * Wischgesten mit einem Finger gehören dort der Bedienhilfe, und eine eingeklappte Navigation
     * wäre nur über den Griff zu finden (wie M3 `exitAlwaysScrollBehavior`, das sich mit TalkBack
     * abschaltet). Der Griff bleibt eine Schaltfläche; wer ihn nutzt, hört dort den neuen Zustand.
     */
    var keepOpen: Boolean = false
        set(value) {
            field = value
            if (value) collapsed = false
        }

    /** Weg der Leiste in px (volle Höhe minus Platz der Systemnavigation); misst [HideableTabBar]. */
    internal var travel = 0f

    /** Wohin die Leiste gerade einrasten soll; [HideableTabBar] führt es aus, `null` = Finger führt. */
    internal var settleRequest by mutableStateOf<SettleRequest?>(null)
        private set

    /** Für den einen Impuls an der Schwelle; setzt [HideableTabBar] (`LocalHapticFeedback`, folgt der Systemeinstellung). */
    internal var haptics: HapticFeedback? = null

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
        if (onTab || tab != lastTab) collapsed = !keepOpen
        lastTab = tab
        inDepth = !onTab
    }

    /** Leiste ohne Animation auf [open] (erster Frame, damit beim Start nichts gleitet). */
    fun snap(open: Boolean) {
        settleRequest = null
        openness = if (open) 1f else 0f
    }

    /**
     * Leiste auf [open] einrasten lassen, mit [Motion.TabBar] und dem Tempo [velocity] (Anteil der
     * Leiste je Sekunde, positiv = auf). Läuft schon ein Einrasten zum selben Ziel, bleibt es; ein
     * Wischen oder [snap] beendet es.
     */
    fun settle(open: Boolean, velocity: Float = 0f) {
        val target = if (open) 1f else 0f
        // Schon unterwegs (oder angekommen) zu diesem Ziel: Das Tempo des Fingers bleibt erhalten.
        if (settleRequest?.target == target) return
        settleRequest = SettleRequest(target, velocity)
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
     * zu, Griff beides; ohne Startleiste das Band über den unteren Leisten ([detectBottomSwipe]). Zentral
     * in `PinnedActionBar`, [HideableTabBar], [TabBarToggle] und im App-Rahmen
     * (`with(state) { detectSwipe(…) }` in `pointerInput`); keine Seite tut etwas dafür.
     *
     * Gewertet wird erst nach dem Touch-Slop und nur in der Richtung, die gerade etwas bewirkt: Bis dahin
     * bleibt ein Tipp auf Start oder „Anpassen ›“ ein Tipp; geht der Finger zuerst in die andere Richtung
     * oder zur Seite, bleibt die Geste ganz dem Inhalt. Erst ein Wischen verbraucht die Bewegung (der
     * Knopf darunter bricht dann ab). Ab da folgt die Leiste dem Finger ([openness]); entschieden wird
     * beim Loslassen (Eigentümer 2026-10-31): Ist der Finger schnell ([Sizes.TabBarSwipeVelocity] je
     * Sekunde), zählt seine Richtung, sonst die Strecke ab dem Aufsetzen ([Sizes.TabBarSwipeDistance]).
     * Zurückziehen bricht ab, die Leiste rastet dann zurück. Beim Überschreiten der Strecke ein
     * kleiner Impuls, beim Zurückziehen darunter das Gegenstück ([gestureThreshold]); dazwischen und
     * bei Tipps still. Nur in der Tiefe ([inDepth]); was in den Systemgesten
     * am unteren Rand aufsetzt ([gestureZoneTop]) oder über [from] (px im Fenster), bleibt anderen.
     * [pass] `Initial` wertet vor dem Inhalt darunter (ein Scrollen bricht dann ab).
     * [windowTop] ist die Oberkante der wischbaren Fläche in px im Fenster.
     */
    suspend fun PointerInputScope.detectSwipe(
        opens: Boolean,
        closes: Boolean,
        pass: PointerEventPass = PointerEventPass.Main,
        from: () -> Float = { Float.NEGATIVE_INFINITY },
        windowTop: () -> Float,
    ) {
        val distance = Sizes.TabBarSwipeDistance.toPx()
        val fast = Sizes.TabBarSwipeVelocity.toPx()
        // Weg der Leiste, solange sie noch nie gemessen wurde (eine M3-Navigationsleiste ist 80 hoch).
        val fallbackTravel = Sizes.TabBarFallbackTravel.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
            val y = windowTop() + down.position.y
            // So herum gelesen, dass eine noch unbekannte Kante (NaN) nichts freigibt.
            if (!inDepth || !(y >= from()) || y >= gestureZoneTop) return@awaitEachGesture
            val up = opens && collapsed
            val downward = closes && !collapsed
            if (!up && !downward) return@awaitEachGesture
            val slop = viewConfiguration.touchSlop
            // Das Tempo zählt ab dem Aufsetzen, auch über den Touch-Slop.
            val tracker = VelocityTracker()
            tracker.addPointerInputChange(down)
            var change: PointerInputChange
            while (true) {
                change = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                tracker.addPointerInputChange(change)
                if (!change.pressed || change.isConsumed) return@awaitEachGesture
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (abs(dx) > slop && abs(dx) > abs(dy)) return@awaitEachGesture
                if (abs(dy) <= slop) continue
                // Nur die Richtung, die etwas bewirkt, wird zum Wischen; die andere bleibt Knöpfen und Inhalt.
                if ((up && dy > 0f) || (downward && dy < 0f)) return@awaitEachGesture
                break
            }
            // Ab hier gehört die Geste der Leiste: Sie folgt dem Finger.
            settleRequest = null
            val start = openness
            val travel = travel.takeIf { it > 0f } ?: fallbackTravel
            var signalled = false
            var released = false
            var dy = 0f
            var first = true
            while (true) {
                change.consume()
                if (!first) tracker.addPointerInputChange(change)
                first = false
                dy = change.position.y - down.position.y
                openness = (start - dy / travel).coerceIn(0f, 1f)
                // Strecke in der Richtung, die etwas bewirkt (auf: nach oben, zu: nach unten).
                val reach = if (up) -dy else dy
                // Ein Impuls beim Überschreiten der Strecke, das Gegenstück beim Zurückziehen; sonst still.
                val beyond = reach >= distance
                if (beyond != signalled) {
                    signalled = beyond
                    haptics?.gestureThreshold(reached = beyond)
                }
                val next = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id }
                if (next == null || next.isConsumed) break
                change = next
                if (!change.pressed) {
                    tracker.addPointerInputChange(change)
                    released = true
                    break
                }
            }
            val vy = tracker.calculateVelocity().y
            val reach = if (up) -dy else dy
            val towards = if (up) -vy else vy
            // Bricht ein anderer die Geste ab, gilt sie nicht: Die Leiste rastet zurück.
            val commit = released && if (abs(vy) >= fast) towards > 0f else reach >= distance
            if (commit) {
                if (up) show() else hide()
            }
            // Tempo als Anteil der Leiste je Sekunde, positiv = auf.
            settle(open = !collapsed, velocity = -vy / travel)
        }
    }

    /**
     * Wischen nach oben im unteren Bereich einer Seite ohne Startleiste (im App-Rahmen auf dem Inhalt):
     * Die eingeklappte Leiste ist dort nur der Platz der Systemnavigation, und der gehört dem System.
     * Darum zählt ein Band [Sizes.TabBarSwipeBand] hoch über den unteren Leisten, über die ganze
     * Breite – leicht zu treffen; am Ende jeder Seite der Tiefe liegt in seinem unteren Teil nichts. Gewertet vor dem Inhalt
     * ([PointerEventPass.Initial]), sonst nähme ein scrollender Inhalt die Geste; Tipps, Wischen nach
     * unten und zur Seite bleiben ihm. Mit Startleiste wischt man auf ihr ([anchoredToPinnedBar]).
     */
    suspend fun PointerInputScope.detectBottomSwipe(windowTop: () -> Float) {
        val band = Sizes.TabBarSwipeBand.toPx()
        detectSwipe(
            opens = true,
            closes = false,
            pass = PointerEventPass.Initial,
            from = { if (anchoredToPinnedBar) Float.NaN else bottomBarTop - band },
            windowTop = windowTop,
        )
    }
}

/** Ziel ([target] 0 oder 1) und Anfangstempo ([velocity], Anteil je Sekunde) eines Einrastens. */
@Immutable
internal class SettleRequest(val target: Float, val velocity: Float)

/** Der [TabBarState] der App für ihre Seiten (`PinnedActionBar` meldet sich an); null in Vorschauen und Tests. */
val LocalTabBarState = staticCompositionLocalOf<TabBarState?> { null }

/** Der [TabBarState] der App. */
@Composable
fun rememberTabBarState(): TabBarState = remember { TabBarState() }
