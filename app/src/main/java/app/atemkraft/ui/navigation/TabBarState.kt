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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import app.atemkraft.ui.components.gestureThreshold
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.Sizes
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Zustand der Tab-Leiste auf Seiten in der Tiefe: Wer von einem Tab in die Tiefe geht, sieht sie
 * offen (E4, Eigentümer 2026-11-25: „standardmäßig auf, aber das Einklappen soll aktiv bleiben“;
 * vorher ab 2026-10-23 eingeklappt), eingeklappt ist sie nur auf Wunsch ([collapsed]) oder in einem
 * niedrigen Fenster ([compact]: 360 × 640, quer). Der Griff
 * auf der Oberkante der untersten Leiste ([TabBarToggle]) klappt sie zu und wieder auf ([toggle]), ebenso Wischen ([detectSwipe]): nach
 * oben auf Startleiste, Mini-Leiste, Griff oder im unteren Bereich einer Seite ohne Startleiste
 * ([detectBottomSwipe]) öffnet ([show]), nach unten auf denselben Leisten und der offenen Tab-Leiste
 * schließt ([hide]). Diese Wahl gilt auf allen weiteren Seiten der Tiefe, vor und zurück, bis der
 * Nutzer wieder auf einem Tab steht ([enter]); Drehen und Prozess-Tod überlebt sie ([Saver]). Auf
 * den vier Tabs selbst steht die Leiste immer (das entscheidet der Aufrufer), Wischen ändert dort
 * nichts ([inDepth]).
 *
 * Wo der Griff sitzt: auf der Oberkante der Startleiste der gezeigten Seite (`PinnedActionBar`
 * meldet sich mit [anchorPinnedBar] an), sonst auf der Oberkante der unteren Leisten
 * ([bottomBarTop]). Beides in px im Fenster, gerechnet für die Lage der Leiste im selben Bild
 * ([anchorTop]).
 *
 * Wie weit die Leiste gerade offen ist, sagt [openness] (0 zu, 1 offen): Beim Wischen folgt sie dem
 * Finger, beim Loslassen rastet sie mit [Motion.TabBar] ein und übernimmt das Tempo des Fingers
 * (Eigentümer 2026-10-31, nach M3-Bottom-Sheet und Apple „Designing Fluid Interfaces“). Die
 * Animation führt [HideableTabBar] aus ([settleRequest]); der Zustand selbst hält keinen Scope (CODE-02).
 */
@Stable
class TabBarState {
    var collapsed by mutableStateOf(false)
        private set

    /** Wie weit die Leiste offen ist, 0…1; gelesen nur beim Auslegen und Zeichnen, nie in der Komposition. */
    var openness by mutableFloatStateOf(1f)
        internal set

    /**
     * Bedienhilfe, die Wischgesten selbst braucht (TalkBack, Switch Access; [rememberTabBarAssistance]):
     * Dann steht die Leiste auch in der Tiefe offen – Wischgesten mit einem Finger gehören dort der
     * Bedienhilfe, und eine eingeklappte Navigation wäre nur über den Griff zu finden (wie M3
     * `exitAlwaysScrollBehavior`, das sich mit TalkBack abschaltet). Der Griff bleibt eine
     * Schaltfläche; wer ihn nutzt, hört dort den neuen Zustand. Endet die Bedienhilfe, bleibt die
     * Leiste offen wie ab Werk; einklappen lässt sie sich dann wieder.
     */
    var keepOpen: Boolean = false
        set(value) {
            field = value
            if (value) collapsed = false
        }

    /**
     * Niedriges Fenster (unter [Sizes.TabBarOpenMinHeight]: 360 × 640, jedes Gerät quer): Dann ist
     * die Leiste in der Tiefe ab Werk eingeklappt – offen nähme sie Editor und Detailseite den Platz
     * (MUSTER-10, Leitplanke 3). Ändert sich das beim Drehen, folgt die Leiste, solange der Nutzer in
     * dieser Tiefe nichts von Hand gewählt hat ([manual]).
     */
    var compact: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (!manual && !keepOpen) collapsed = value
        }

    // Von Hand auf- oder zugeklappt seit dem letzten Tab: Dann gilt die Wahl, nicht die Vorgabe.
    private var manual = false

    /** Weg der Leiste in px (volle Höhe minus Platz der Systemnavigation); misst [HideableTabBar]. */
    internal var travel = 0f

    /** Wohin die Leiste gerade einrasten soll; [HideableTabBar] führt es aus, `null` = Finger führt. */
    internal var settleRequest by mutableStateOf<SettleRequest?>(null)
        private set

    /** Tempo des laufenden Einrastens (Anteil je Sekunde): Ein Tipp mitten darin kehrt weich um. */
    internal var settleVelocity = 0f

    /** Für den einen Impuls an der Schwelle; setzt [HideableTabBar] (`LocalHapticFeedback`, folgt der Systemeinstellung). */
    internal var haptics: HapticFeedback? = null

    /**
     * Ein Finger liegt auf einer Fläche, die die Leiste öffnen kann: [HideableTabBar] komponiert sie
     * schon, damit sie beim ersten Bild des Ziehens da ist und wächst.
     */
    internal var engaged by mutableStateOf(false)

    /** Ob die gezeigte Seite in der Tiefe liegt (kein Tab): nur dort klappt Wischen die Leiste. */
    var inDepth by mutableStateOf(false)
        private set

    /**
     * Oberkante der Systemgesten am unteren Rand (px im Fenster): Was dort aufsetzt, gehört dem
     * System (Home, Zurück), nie dem Wischen der Tab-Leiste. Ohne Wert (Tests) gilt nichts als
     * Systemgeste.
     */
    var gestureZoneTop by mutableFloatStateOf(Float.POSITIVE_INFINITY)

    /** Höhe des Fensters in px; begrenzt das Wischband quer ([detectBottomSwipe]). */
    var windowHeight by mutableFloatStateOf(Float.POSITIVE_INFINITY)

    // Tab, zu dessen Stapel die zuletzt gezeigte Seite gehörte: Ein Tab-Wechsel, der gleich in einem
    // wiederhergestellten Stapel in der Tiefe landet, zählt wie der Weg von einem Tab.
    private var lastTab: Any? = null

    // Die gezeigte Seite ([enter]); nur ihre Startleiste trägt den Griff. Ohne Wert (Tests) jede.
    private var page: Any? = null

    // Zählt die Seitenwechsel: Ein Wischen, das über einen Seitenwechsel läuft, endet ([detectSwipe]).
    private var epoch = 0

    // Lagen in px im Fenster bei eingeklappter Leiste: gemessen plus der damals gezeigte Teil der
    // Leiste. So stimmt die Kante im selben Bild, in dem sich die Leiste bewegt ([anchorTop]).
    private var bottomBarBase by mutableFloatStateOf(Float.NaN)
    private val pinnedOrder = mutableStateListOf<Any>()
    private val pinnedBars = mutableStateMapOf<Any, PinnedBar>()

    /** Oberkante der unteren Leisten (Mini-Leiste, Tab-Leiste bzw. Systemnavigation), px im Fenster. */
    var bottomBarTop: Float
        get() = bottomBarBase - shownPx()
        set(value) {
            val base = value + shownPx()
            if (base != bottomBarBase) bottomBarBase = base
        }

    /** Ob der Griff auf der Startleiste der gezeigten Seite sitzt (sonst auf den unteren Leisten). */
    val anchoredToPinnedBar: Boolean
        get() = currentPinned() != null

    /** Ob die Kante des Griffs schon bekannt ist (Komposition; ohne Lesen der Lage der Leiste). */
    val anchorKnown: Boolean
        get() = !(currentPinned()?.let { pinnedBars[it]?.base } ?: bottomBarBase).isNaN()

    /**
     * Oberkante, auf der der Griff sitzt (px im Fenster): die Startleiste der gezeigten Seite, sonst
     * die unteren Leisten – für die Lage der Leiste in diesem Bild. Nur beim Auslegen lesen.
     */
    val anchorTop: Float
        get() = (currentPinned()?.let { pinnedBars[it]?.base } ?: bottomBarBase) - shownPx()

    /** Gezeigter Teil der Leiste über der Systemnavigation in px; dieselbe Rundung wie [HideableTabBar]. */
    internal fun shownPx(): Int = (travel * openness).roundToInt()

    private fun currentPinned(): Any? = pinnedOrder.lastOrNull { owner ->
        val bar = pinnedBars[owner] ?: return@lastOrNull false
        page == null || bar.page == null || bar.page == page
    }

    /**
     * Neue Seite [page]: Auf einem Tab ([onTab]) oder nach einem Wechsel des Tabs ([tab]) gilt wieder
     * „offen“ für den nächsten Weg in die Tiefe; in der Tiefe bleibt das Einklappen von Hand stehen,
     * vor und zurück, bis der Nutzer wieder auf einem Tab steht. Ein laufendes Wischen endet.
     */
    fun enter(onTab: Boolean, tab: Any, page: Any? = null) {
        epoch++
        if (onTab || tab != lastTab) {
            collapsed = compact && !keepOpen
            manual = false
        }
        lastTab = tab
        this.page = page
        inDepth = !onTab
    }

    /** Leiste ohne Animation auf [open] (erster Frame, damit beim Start nichts gleitet). */
    fun snap(open: Boolean) {
        settleRequest = null
        settleVelocity = 0f
        openness = if (open) 1f else 0f
    }

    /**
     * Leiste auf [open] einrasten lassen, mit [Motion.TabBar] und dem Tempo [velocity] (Anteil der
     * Leiste je Sekunde, positiv = auf; ohne Angabe das des laufenden Einrastens). Läuft schon ein
     * Einrasten zum selben Ziel, bleibt es; ein Wischen oder [snap] beendet es.
     */
    fun settle(open: Boolean, velocity: Float = settleVelocity) {
        val target = if (open) 1f else 0f
        // Schon unterwegs (oder angekommen) zu diesem Ziel: Das Tempo des Fingers bleibt erhalten.
        if (settleRequest?.target == target) return
        settleRequest = SettleRequest(target, velocity)
    }

    /** Griff angetippt: Leiste auf bzw. zu, bis zur Rückkehr auf einen Tab. */
    fun toggle() {
        collapsed = !collapsed
        manual = true
    }

    /** Nach oben gewischt: Leiste auf. */
    fun show() {
        collapsed = false
        manual = true
    }

    /** Nach unten gewischt: Leiste zu – nicht, solange eine Bedienhilfe sie offen hält ([keepOpen]). */
    fun hide() {
        if (!keepOpen) {
            collapsed = true
            manual = true
        }
    }

    /**
     * Eine Startleiste ([owner]) der Seite [page] mit ihrer Oberkante [top] (px im Fenster) an- bzw.
     * nachmelden. Es zählt nur die Startleiste der gezeigten Seite ([enter]): Beim Seitenwechsel
     * gleitet die alte noch hinaus, der Griff klebt nicht an ihr.
     */
    fun anchorPinnedBar(owner: Any, page: Any?, top: Float) {
        if (owner !in pinnedOrder) pinnedOrder += owner
        val bar = PinnedBar(page, top + shownPx())
        if (pinnedBars[owner] != bar) pinnedBars[owner] = bar
    }

    /** Oberkante der Startleiste [owner] (px im Fenster), solange sie angemeldet ist. */
    fun pinnedTop(owner: Any): Float = pinnedBars[owner]?.let { it.base - shownPx() } ?: Float.NaN

    /** Startleiste [owner] verschwindet mit ihrer Seite. */
    fun releasePinnedBar(owner: Any) {
        pinnedOrder -= owner
        pinnedBars -= owner
    }

    /**
     * Wischen an den Leisten der Tiefe (Nutzer 2026-10-30: „mit von unten nach oben wischen im unteren
     * Bereich soll die Tab-Leiste geöffnet werden“): nach oben öffnet die Leiste ([opens]), nach unten
     * schließt sie ([closes]). Mini-Leiste, Griff und Tab-Leiste können beides (Eigentümer
     * 2026-11-24: wo hoch öffnet, schließt runter); die Startleiste nur schließen – geöffnet wird an
     * ihrem unteren Rand vom Streifen über dem Inhalt ([detectBottomSwipe], Eigentümer 2026-11-25),
     * der nur öffnet, nach unten scrollt dort der Inhalt. Zentral in `PinnedActionBar`, [HideableTabBar],
     * [TabBarToggle] und im App-Rahmen (`with(state) { detectSwipe(…) }` in `pointerInput`); keine
     * Seite tut etwas dafür.
     *
     * Gewertet wird erst nach dem Touch-Slop: Bis dahin bleibt ein Tipp auf Start oder „Anpassen ›“
     * ein Tipp; geht der Finger zuerst zur Seite, bleibt die Geste dem Inhalt. Auf einer Leiste
     * ([claim]) gehört ihr danach jede senkrechte Bewegung, auch die wirkungslose (nach oben bei
     * offener Leiste): Ein Wischen wird nie zum Tipp auf Start, einen Tab oder den Griff. Das Band
     * nimmt nur die Richtung, die etwas bewirkt; die andere bleibt dem Inhalt.
     *
     * Der Weg zählt aus den Änderungen je Ereignis (`positionChange`), nicht aus Positionen: Die
     * Flächen wandern mit der Leiste und ihre Koordinaten mit ihnen – so folgt die Leiste dem Finger
     * überall 1:1 (vorher auf Startleiste, Griff und Tab-Leiste nur halb so schnell, Strecke und
     * Tempo wirkten dort doppelt so hoch). Die Leiste folgt ab dem Slop ohne Sprung ([openness]),
     * entschieden wird beim Loslassen (Eigentümer 2026-10-31): Ist der Finger schnell
     * ([Sizes.TabBarSwipeVelocity] je Sekunde), zählt seine Richtung, sonst die Strecke ab dem
     * Aufsetzen ([Sizes.TabBarSwipeDistance]); wer die Leiste mitten im Einrasten fängt, bekommt die
     * nähere Lage (halb offen). Zurückziehen bricht ab. Ein kleiner Impuls beim Überschreiten der
     * Strecke, das Gegenstück erst einen Slop darunter ([gestureThreshold]), damit ein Zittern an der
     * Schwelle nicht brummt. Endet die Geste anders (Seitenwechsel, Abbruch), rastet die Leiste
     * dorthin, wo sie hingehört. Nur in der Tiefe ([inDepth]); was in den Systemgesten am unteren
     * Rand aufsetzt ([gestureZoneTop]) oder über [from] (px im Fenster), bleibt anderen. [pass]
     * `Initial` wertet vor dem Inhalt darunter. [windowTop] ist die Oberkante der wischbaren Fläche
     * in px im Fenster.
     */
    suspend fun PointerInputScope.detectSwipe(
        opens: Boolean,
        closes: Boolean,
        claim: Boolean = true,
        pass: PointerEventPass = PointerEventPass.Main,
        from: () -> Float = { Float.NEGATIVE_INFINITY },
        windowTop: () -> Float,
    ) {
        val swipe = Swipe(
            distance = Sizes.TabBarSwipeDistance.toPx(),
            fast = Sizes.TabBarSwipeVelocity.toPx(),
            // Weg der Leiste, solange sie noch nie gemessen wurde (eine M3-Navigationsleiste ist 80 hoch).
            fallbackTravel = Sizes.TabBarFallbackTravel.toPx(),
            claim = claim,
            pass = pass,
        )
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
            val y = windowTop() + down.position.y
            // So herum gelesen, dass eine noch unbekannte Kante (NaN) nichts freigibt.
            if (!inDepth || !(y >= from()) || y >= gestureZoneTop) return@awaitEachGesture
            val up = opens && collapsed
            val downward = closes && !collapsed && !keepOpen
            if (!up && !downward && !claim) return@awaitEachGesture
            // Kann die Geste die Leiste öffnen, wird sie schon jetzt komponiert.
            if (up) engaged = true
            try {
                follow(down, up, downward, swipe)
            } finally {
                engaged = false
            }
        }
    }

    private suspend fun AwaitPointerEventScope.follow(down: PointerInputChange, up: Boolean, downward: Boolean, swipe: Swipe) {
        val slop = viewConfiguration.touchSlop
        val startPage = epoch
        val tracker = VelocityTracker()
        // Fingerweg seit dem Aufsetzen (px), aufsummiert aus den Änderungen je Ereignis.
        var dx = 0f
        var dy = 0f
        tracker.addPosition(down.uptimeMillis, Offset.Zero)
        var change: PointerInputChange
        while (true) {
            change = awaitPointerEvent(swipe.pass).changes.firstOrNull { it.id == down.id } ?: return
            if (!change.pressed || change.isConsumed) return
            val delta = change.positionChangeIgnoreConsumed()
            dx += delta.x
            dy += delta.y
            tracker.addPosition(change.uptimeMillis, Offset(0f, dy))
            if (abs(dx) > slop && abs(dx) > abs(dy)) return
            if (abs(dy) > slop) break
        }
        if (!((up && dy < 0f) || (downward && dy > 0f))) {
            // Wirkungslose Richtung: Auf einer Leiste gehört die Bewegung trotzdem ihr, kein Tipp darunter.
            if (swipe.claim) hold(change, swipe.pass)
            return
        }
        // Ab hier gehört die Geste der Leiste: Sie folgt dem Finger, ab dem Slop ohne Sprung.
        settleRequest = null
        settleVelocity = 0f
        val start = openness
        val caught = start > 0f && start < 1f
        var follow = dy - (if (dy < 0f) -slop else slop)
        var signalled = false
        var released = false
        try {
            while (true) {
                change.consume()
                openness = (start - follow / travelOr(swipe.fallbackTravel)).coerceIn(0f, 1f)
                val reach = if (up) -dy else dy
                if (!signalled && reach >= swipe.distance) {
                    signalled = true
                    haptics?.gestureThreshold(reached = true)
                } else if (signalled && reach < swipe.distance - slop) {
                    signalled = false
                    haptics?.gestureThreshold(reached = false)
                }
                val next = awaitPointerEvent(swipe.pass).changes.firstOrNull { it.id == down.id }
                if (next == null || next.isConsumed || epoch != startPage) break
                change = next
                val delta = change.positionChangeIgnoreConsumed().y
                dy += delta
                follow += delta
                tracker.addPosition(change.uptimeMillis, Offset(0f, dy))
                if (!change.pressed) {
                    change.consume()
                    released = true
                    break
                }
            }
        } finally {
            val vy = tracker.calculateVelocity().y
            if (released) {
                val towards = if (up) -vy else vy
                val commit = when {
                    abs(vy) >= swipe.fast -> towards > 0f
                    caught -> if (up) openness >= HALF_OPEN else openness <= HALF_OPEN
                    else -> (if (up) -dy else dy) >= swipe.distance
                }
                when {
                    !commit -> Unit
                    up -> show()
                    else -> hide()
                }
            }
            // Immer einrasten, auch nach einem Abbruch – dorthin, wo die Leiste gerade hingehört.
            // Tempo als Anteil der Leiste je Sekunde, positiv = auf.
            settle(open = !inDepth || !collapsed, velocity = -vy / travelOr(swipe.fallbackTravel))
        }
    }

    /** Hält eine wirkungslose Bewegung bis zum Loslassen fest, ohne die Leiste zu bewegen. */
    private suspend fun AwaitPointerEventScope.hold(first: PointerInputChange, pass: PointerEventPass) {
        first.consume()
        while (true) {
            val change = awaitPointerEvent(pass).changes.firstOrNull { it.id == first.id } ?: return
            if (change.isConsumed) return
            change.consume()
            if (!change.pressed) return
        }
    }

    private fun travelOr(fallback: Float): Float = travel.takeIf { it > 0f } ?: fallback

    /**
     * Wischen nach oben am unteren Rand jeder Seite der Tiefe (im App-Rahmen auf dem Inhalt): Die
     * eingeklappte Leiste ist dort nur der Platz der Systemnavigation, und der gehört dem System.
     * Darum zählt ein schmaler Streifen über den unteren Leisten, über die ganze Breite –
     * [Sizes.TabBarSwipeBand] hoch, quer höchstens [Sizes.TabBarSwipeBandMaxShare] der Fensterhöhe
     * ([windowHeight]). Auf jeder Seite gleich, auch über einer Startleiste (dort ihr unterer Rand;
     * Eigentümer 2026-11-25: weiter oben, etwa auf „Anpassen ›“, öffnet Wischen nicht mehr). Gewertet
     * vor dem Inhalt ([PointerEventPass.Initial]), sonst nähme ein scrollender Inhalt oder die
     * Startleiste die Geste; Tipps (auch auf Start), Wischen nach unten und zur Seite bleiben ihm.
     */
    suspend fun PointerInputScope.detectBottomSwipe(windowTop: () -> Float) {
        val band = Sizes.TabBarSwipeBand.toPx()
        detectSwipe(
            opens = true,
            closes = false,
            claim = false,
            pass = PointerEventPass.Initial,
            from = { bottomBarTop - minOf(band, windowHeight * Sizes.TabBarSwipeBandMaxShare) },
            windowTop = windowTop,
        )
    }

    companion object {
        /** Lage, ab der eine mitten im Einrasten gefangene Leiste als offen gilt. */
        private const val HALF_OPEN = 0.5f

        /**
         * Hält über Drehen, Schriftwechsel und Prozess-Tod fest, ob die Leiste in der Tiefe offen ist
         * und zu welchem Tab die Seite gehörte; sonst klappte sie dabei zu.
         */
        val Saver: Saver<TabBarState, Any> = listSaver(
            save = { listOf(it.collapsed, (it.lastTab as? TopTab)?.name, it.manual) },
            restore = { saved ->
                TabBarState().apply {
                    collapsed = saved[0] as Boolean
                    lastTab = (saved[1] as String?)?.let { name -> TopTab.entries.firstOrNull { it.name == name } }
                    manual = saved[2] as Boolean
                }
            },
        )
    }
}

/** Schwellen und Art einer Geste ([TabBarState.detectSwipe]); Längen in px. */
private class Swipe(val distance: Float, val fast: Float, val fallbackTravel: Float, val claim: Boolean, val pass: PointerEventPass)

/** Eine angemeldete Startleiste: ihre Seite und ihre Oberkante bei eingeklappter Leiste (px im Fenster). */
@Immutable
private data class PinnedBar(val page: Any?, val base: Float)

/** Ziel ([target] 0 oder 1) und Anfangstempo ([velocity], Anteil je Sekunde) eines Einrastens. */
@Immutable
internal class SettleRequest(val target: Float, val velocity: Float)

/** Der [TabBarState] der App für ihre Seiten (`PinnedActionBar` meldet sich an); null in Vorschauen und Tests. */
val LocalTabBarState = staticCompositionLocalOf<TabBarState?> { null }

/** Der [TabBarState] der App; übersteht Drehen und Prozess-Tod ([TabBarState.Saver]). */
@Composable
fun rememberTabBarState(): TabBarState = rememberSaveable(saver = TabBarState.Saver) { TabBarState() }
