package app.atemkraft.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Klebende Überschriften (Eigentümer 2026-10-31, „Titel bleibt kleben“): Eine Überschrift scrollt
 * wie jeder Inhalt, bis sie oben ankommt, und bleibt dort stehen – genau so, wie sie im Text steht.
 * Ebene 2 (Gruppe, Kapitel) hängt unter Ebene 1; die nächste Überschrift derselben oder einer
 * höheren Ebene schiebt die stehende Pixel für Pixel hinaus.
 *
 * So benutzt: Auf einer Spalte ([PushScaffold]) hüllt die Seite ihre Überschrift in [StickyHeader]
 * (oder gibt `CollapsibleSection(sticky = …)`). In einer Liste ([TabListScaffold]) meldet sie sie
 * zusätzlich in der Liste an – [stickyItem] (eigener Eintrag) oder [declareSticky] (Überschrift im
 * nächsten Eintrag) –, damit die Liste auch Überschriften kennt, deren Eintrag nie zu sehen war
 * (zurück von einer Unterseite, Sprung). Das Gerüst legt den [StickyHeaderState] an und zeigt die
 * stehenden Überschriften in einer Schicht über dem oberen Rand ([StickyHeaderOverlay]).
 *
 * Wie: Oben steht eine neu aufgebaute Kopie – dieselbe Überschrift, noch einmal komponiert, mit
 * Breite und Lage des Originals –, wie es die Sticky-Header-Bibliotheken der RecyclerView machen.
 * Keine aufgezeichneten Pixel des Originals: Scrollt dessen Eintrag aus dem Bild, verwirft Android
 * sie, und oben bliebe ein leeres Band (Hochscrollen, 2026-10-31). Neu komponiert wird nur, wenn
 * eine andere Überschrift zu kleben beginnt; die Lage rechnet die Schicht in jedem Bild aus dem
 * Scrollstand ([LazyListState.layoutInfo] bzw. [ScrollState.value]) im Layout – an den Finger
 * gekoppelt, ohne Dauer (MOTION-02 gilt nicht: Es bewegt sich nichts von selbst). Die Kopie ist für
 * TalkBack unsichtbar: Die echte Überschrift steht weiter in der Liste.
 */
@Stable
class StickyHeaderState internal constructor(internal val source: StickySource) {
    /** Die Überschriften der Seite in ihrer Reihenfolge; Liste: aus der letzten Anmeldung. */
    internal var entries: Map<Any, StickyEntry> = emptyMap()
        private set

    /** Zählt jede Änderung an [entries]: Die Schicht liest ihn mit und rechnet dann neu. */
    private var version by mutableIntStateOf(0)

    /** Koordinaten des sichtbaren Bereichs (Box um Liste bzw. Spalte); Bezug aller Lagen. */
    internal var viewport: LayoutCoordinates? = null

    /** Höhe des sichtbaren Bereichs, für den Rückfall bei zu hohem Stapel. */
    internal var viewportHeight by mutableIntStateOf(0)

    /** Rand links und rechts in px: Lage und Breite einer Überschrift, die noch nie zu sehen war. */
    internal var inset = 0

    private var declaring: LinkedHashMap<Any, StickyEntry>? = null

    /** Liste: Beginn der Anmeldung (jedes Mal, wenn die Liste ihren Inhalt neu aufbaut). */
    internal fun beginDeclarations() {
        declaring = LinkedHashMap()
    }

    /** Liste: eine Überschrift an Eintrag [index]; [content] null = Inhalt kommt vom [StickyHeader]. */
    internal fun declare(sticky: Sticky, index: Int, content: (@Composable () -> Unit)?) {
        val target = declaring ?: return
        val entry = entries[sticky.key] ?: StickyEntry(sticky.key)
        entry.level = sticky.level
        entry.pins = sticky.pins
        entry.itemKey = sticky.itemKey ?: sticky.key
        entry.index = index
        if (content != null) entry.declared = content
        target[sticky.key] = entry
    }

    /** Liste: Ende der Anmeldung; nicht mehr angemeldete Überschriften fallen weg. */
    internal fun endDeclarations() {
        val declared = declaring ?: return
        declaring = null
        if (declared.keys.toList() != entries.keys.toList() || declared.values.zip(entries.values).any { (a, b) -> a !== b }) {
            entries = declared
            version++
        }
    }

    /** Spalte: Die Überschrift ist komponiert. Liste: Sie liefert ihren Inhalt, falls die Anmeldung keinen hat. */
    internal fun attach(sticky: Sticky, content: @Composable () -> Unit): StickyEntry? {
        if (source is StickySource.Lazy) {
            val entry = entries[sticky.key] ?: return null
            entry.composed = content
            return entry
        }
        val entry = entries[sticky.key] ?: StickyEntry(sticky.key).also {
            entries = entries + (sticky.key to it)
            version++
        }
        entry.level = sticky.level
        entry.pins = sticky.pins
        entry.composed = content
        return entry
    }

    /** Spalte: Die Überschrift verlässt die Seite. In einer Liste bleibt sie angemeldet. */
    internal fun detach(sticky: Sticky) {
        if (source is StickySource.Lazy || sticky.key !in entries) return
        entries = entries - sticky.key
        version++
    }

    internal fun place(entry: StickyEntry, coordinates: LayoutCoordinates) {
        val viewport = viewport?.takeIf { it.isAttached } ?: return
        if (!coordinates.isAttached) return
        val position = viewport.localPositionOf(coordinates, Offset.Zero)
        val y = position.y.roundToInt()
        entry.x = position.x.roundToInt()
        entry.width = coordinates.size.width
        entry.height = coordinates.size.height
        when (source) {
            is StickySource.Scroll -> entry.offset = y + source.state.value

            is StickySource.Lazy -> {
                val item = source.state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entry.itemKey } ?: return
                entry.offset = y - item.offset
            }
        }
    }

    /**
     * Lage der Überschrift im sichtbaren Bereich. Liste: Ist ihr Eintrag zu sehen, zählt seine Lage;
     * sonst sagt der angemeldete Index, ob sie darüber ([ABOVE]) oder darunter ([BELOW]) liegt.
     */
    private fun yOf(entry: StickyEntry): Int = when (source) {
        is StickySource.Scroll -> entry.offset - source.state.value

        is StickySource.Lazy -> {
            val visible = source.state.layoutInfo.visibleItemsInfo
            val item = visible.firstOrNull { it.key == entry.itemKey }
            when {
                item != null -> item.offset + entry.offset
                entry.index < (visible.firstOrNull()?.index ?: 0) -> ABOVE
                else -> BELOW
            }
        }
    }

    /** Die stehenden Überschriften dieses Bilds (liest den Scrollstand). */
    internal fun pinned(): List<Pinned<StickyEntry>> {
        version // mitlesen: neue oder entfernte Überschriften
        // Ein Kopf, der nicht klebt, zählt mit: Er beendet den Abschnitt darüber.
        val all = entries.values.filter { it.content != null || !it.pins }
        if (all.isEmpty()) return emptyList()
        // Reihenfolge der Seite; innerhalb eines Listeneintrags nach Lage, gleich lagig nach Anmeldung.
        val headers = all.map { entry ->
            val order = when (source) {
                is StickySource.Scroll -> entry.offset.toLong()
                is StickySource.Lazy -> entry.index.toLong() * ORDER_STRIDE + entry.offset
            }
            HeaderPosition(entry, entry.level, order, yOf(entry), entry.height, entry.pins)
        }
        return pinHeaders(headers, viewportHeight)
    }
}

/** Wo die Seite scrollt: Liste oder Spalte. */
internal sealed interface StickySource {
    class Lazy(val state: LazyListState) : StickySource

    class Scroll(val state: ScrollState) : StickySource
}

/** Eine klebende Überschrift: Ebene, Lage, Maße und ihr Inhalt für die Kopie. */
internal class StickyEntry(val key: Any) {
    var level by mutableIntStateOf(1)
    var itemKey: Any? = null
    var pins = true

    /** Liste: Index ihres Eintrags aus der Anmeldung. */
    var index = 0
    var x by mutableIntStateOf(-1)
    var width by mutableIntStateOf(0)

    /** Liste: Abstand von der Oberkante ihres Eintrags; Spalte: Lage im ganzen Inhalt. */
    var offset by mutableIntStateOf(0)
    var height by mutableIntStateOf(0)

    /** Inhalt aus der Anmeldung in der Liste ([stickyItem], [declareSticky]). */
    var declared: (@Composable () -> Unit)? by mutableStateOf(null)

    /** Inhalt vom [StickyHeader], sobald die Überschrift einmal komponiert war. */
    var composed: (@Composable () -> Unit)? by mutableStateOf(null)

    /** Was die Kopie oben zeigt. */
    val content: (@Composable () -> Unit)? get() = declared ?: composed
}

/** Eingabe der Rechnung: eine Überschrift mit Ebene, Reihenfolge, Lage [y] und Höhe. */
internal class HeaderPosition<T>(val value: T, val level: Int, val order: Long, val y: Int, val height: Int, val pins: Boolean = true)

/** Ergebnis: [value] steht bei [y] (oben im sichtbaren Bereich, ab 0; negativ beim Hinausschieben). */
internal data class Pinned<T>(val value: T, val y: Int, val height: Int)

/**
 * Die Rechnung der klebenden Überschriften, ohne Compose: Je Ebene (aufsteigend) steht die letzte
 * Überschrift, die ihre Linie erreicht hat – die Linie von Ebene 1 ist der obere Rand, jede tiefere
 * hängt unter der stehenden Überschrift darüber, und sie muss nach dieser kommen (ihr Kapitel).
 * Die nächste Überschrift derselben oder einer höheren Ebene schiebt sie hinaus:
 * `min(Linie, nächste.y − Höhe)`. Wäre der Stapel höher als [MAX_PINNED_SHARE] des sichtbaren
 * Bereichs (große Schrift, quer), steht nur die tiefste Ebene.
 */
internal fun <T> pinHeaders(headers: List<HeaderPosition<T>>, viewportHeight: Int): List<Pinned<T>> {
    val sorted = headers.sortedBy { it.order }
    val levels = sorted.map { it.level }.distinct().sorted()
    val all = pinLevels(sorted, levels)
    if (all.size > 1 && all.sumOf { it.height } > viewportHeight * MAX_PINNED_SHARE) {
        return pinLevels(sorted, listOf(levels.last()))
    }
    return all
}

private fun <T> pinLevels(sorted: List<HeaderPosition<T>>, levels: List<Int>): List<Pinned<T>> {
    val result = mutableListOf<Pinned<T>>()
    var line = 0
    var parent = -1
    for (level in levels) {
        val current = sorted.indices.lastOrNull { i -> i > parent && sorted[i].level == level && sorted[i].y < line } ?: continue
        val header = sorted[current]
        // Ein Kopf, der nicht klebt (aufklappbarer Abschnitt), beendet den klebenden darüber.
        if (!header.pins) {
            parent = current
            continue
        }
        val next = sorted.drop(current + 1).firstOrNull { it.level <= level && it.level in levels }
        val y = if (next != null) minOf(line, next.y - header.height) else line
        result += Pinned(header.value, y, header.height)
        line = y + header.height
        parent = current
    }
    return result
}

/** Höchstens dieser Anteil des sichtbaren Bereichs klebt, sonst nur die tiefste Ebene. */
internal const val MAX_PINNED_SHARE = 0.3f

/** Liste: Reihenfolge nach Eintrag, darin nach Lage (ein Eintrag ist nie so hoch). */
private const val ORDER_STRIDE = 1_000_000L
private const val ABOVE = Int.MIN_VALUE / 4
private const val BELOW = Int.MAX_VALUE / 4

/** Das Gerüst der Seite stellt hier seinen [StickyHeaderState] bereit; außerhalb klebt [StickyHeader] nicht. */
internal val LocalStickyHeaders = compositionLocalOf<StickyHeaderState?> { null }

@Composable
internal fun rememberStickyHeaderState(listState: LazyListState): StickyHeaderState = remember(listState) { StickyHeaderState(StickySource.Lazy(listState)) }

@Composable
internal fun rememberStickyHeaderState(scrollState: ScrollState): StickyHeaderState = remember(scrollState) { StickyHeaderState(StickySource.Scroll(scrollState)) }

/** Merkt sich den sichtbaren Bereich, auf den sich alle Lagen beziehen; an die Box um Liste bzw. Spalte. */
internal fun Modifier.stickyViewport(state: StickyHeaderState): Modifier = onPlaced {
    state.viewport = it
    if (state.viewportHeight != it.size.height) state.viewportHeight = it.size.height
}

/**
 * Wie eine Überschrift klebt: [key] eindeutig auf der Seite, [level] 1 (Abschnitt, „Übungen“) oder
 * 2 (Gruppe darunter, „Beruhigen“); in einer Liste nennt [itemKey] den Schlüssel des Eintrags, in
 * dem die Überschrift steht (Standard: [key], die Überschrift ist ihr eigener Eintrag). Ohne
 * [pins] klebt sie selbst nicht, beendet aber den klebenden Abschnitt darüber (aufklappbare Köpfe
 * der Atemlehre: Sonst stünde „Die vier Phasen“ über „Die Grundregeln“).
 */
@Immutable
data class Sticky(val key: Any, val level: Int, val itemKey: Any? = null, val pins: Boolean = true)

/**
 * Hüllt eine Überschrift, die oben klebt, sobald sie dort ankommt: [content] steht hier wie sonst
 * und wird oben als Kopie noch einmal aufgebaut. Nur für Überschriften ohne Bedienung – oben stünde
 * ein Knopf ohne Wirkung. In einer Liste zusätzlich anmelden ([stickyItem], [declareSticky]). Ohne
 * [sticky] oder ohne Gerüst mit [StickyHeaderState] steht nur [content].
 */
@Composable
fun StickyHeader(sticky: Sticky?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val latest = rememberUpdatedState(content)
    // Ein fester Inhalt für die Kopie, der immer den neuesten liest: Die Kopie baut sich mit der
    // Überschrift neu auf, nicht bei jedem Komponieren der Seite.
    val copy: @Composable () -> Unit = remember { { latest.value() } }
    val sticks = sticky != null && LocalStickyHeaders.current != null
    Box(modifier.then(if (sticks && sticky != null) StickyHeaderElement(sticky, copy) else Modifier)) { content() }
}

/**
 * Liste: eine klebende Überschrift als eigener Eintrag (Schlüssel `sticky.itemKey ?: sticky.key`).
 * Die Liste kennt sie damit, auch wenn ihr Eintrag noch nie zu sehen war.
 */
fun LazyListScope.stickyItem(sticky: Sticky, content: @Composable () -> Unit) {
    declareSticky(sticky, content)
    item(key = sticky.itemKey ?: sticky.key) { StickyHeader(sticky, content = content) }
}

/**
 * Liste: meldet eine klebende Überschrift an, die im unmittelbar folgenden Eintrag (`sticky.itemKey`)
 * steht, dort in [StickyHeader] gehüllt. Mit [content] (derselbe Inhalt wie dort) klebt sie auch, wenn
 * der Eintrag noch nie zu sehen war; ohne erst, nachdem er einmal zu sehen war.
 */
fun LazyListScope.declareSticky(sticky: Sticky, content: (@Composable () -> Unit)? = null) {
    (this as? StickyListScope)?.declare(sticky, content)
}

/**
 * Die Liste des [TabListScaffold] meldet hierüber ihre klebenden Überschriften an und zählt die
 * Einträge mit, damit jede Überschrift ihren Index kennt. Gezählt werden `item` und `items` (auch
 * alle `items(list)`); `stickyHeader` von Compose wird hier nicht benutzt.
 */
internal class StickyListScope(private val inner: LazyListScope, private val state: StickyHeaderState) : LazyListScope by inner {
    private var count = 0

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        inner.item(key, contentType, content)
        count++
    }

    override fun items(count: Int, key: ((index: Int) -> Any)?, contentType: (index: Int) -> Any?, itemContent: @Composable LazyItemScope.(index: Int) -> Unit) {
        inner.items(count, key, contentType, itemContent)
        this.count += count
    }

    fun declare(sticky: Sticky, content: (@Composable () -> Unit)?) = state.declare(sticky, count, content)
}

/** Liste: Inhalt mit Anmeldung der klebenden Überschriften. */
internal fun LazyListScope.withStickyHeaders(state: StickyHeaderState, content: LazyListScope.() -> Unit) {
    state.beginDeclarations()
    StickyListScope(this, state).content()
    state.endDeclarations()
}

/** Test-Tag einer Kopie oben, mit dem Schlüssel ihrer Überschrift („stickyCopy:exercises“). */
internal fun stickyCopyTag(key: Any) = "stickyCopy:$key"

/**
 * Die Schicht über dem oberen Rand: so hoch wie die stehenden Überschriften, darin von der tiefsten
 * zur obersten je eine Kopie auf `background` über die volle Breite (eine hinausgeschobene gleitet
 * unter die darüber). Komponiert wird nur der Satz der stehenden Überschriften; Lage und Höhe
 * rechnet das Layout in jedem Bild. Wischen auf ihr scrollt die Seite; ein Tipp fällt nicht auf den
 * Inhalt darunter. Keine Linie (Eigentümer 2026-10-31: Das Violett grenzt genug ab).
 */
@Composable
internal fun BoxScope.StickyHeaderOverlay(state: StickyHeaderState) {
    val background = MaterialTheme.colorScheme.background
    val scroll = when (val source = state.source) {
        is StickySource.Lazy -> source.state
        is StickySource.Scroll -> source.state
    }
    val shown by remember(state) { derivedStateOf { state.pinned().map { it.value } } }
    Layout(
        content = {
            shown.forEach { entry ->
                key(entry.key) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(background)
                            .clearAndSetSemantics { testTag = stickyCopyTag(entry.key) } // Abweichung A11Y-02: Kopie einer Überschrift, die in der Liste steht – TalkBack hört sie dort
                            .layout { measurable, constraints ->
                                // Noch nie zu sehen (Liste nach Rückkehr): volle Breite zwischen den Rändern.
                                val x = if (entry.x >= 0) entry.x else state.inset
                                val width = (if (entry.width > 0) entry.width else constraints.maxWidth - 2 * state.inset).coerceIn(0, constraints.maxWidth)
                                val placeable = measurable.measure(Constraints.fixedWidth(width))
                                if (entry.height == 0) entry.height = placeable.height
                                layout(constraints.maxWidth, placeable.height) { placeable.place(x, 0) }
                            },
                    ) { entry.content?.invoke() }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .scrollable(scroll, Orientation.Vertical, reverseDirection = true),
    ) { measurables, constraints ->
        val byEntry = shown.zip(measurables.map { it.measure(constraints.copy(minHeight = 0)) }).toMap()
        val placed = state.pinned().filter { it.value in byEntry }
        val height = placed.maxOfOrNull { it.y + it.height }?.coerceAtLeast(0) ?: 0
        layout(constraints.maxWidth, height) {
            placed.asReversed().forEach { pinned -> byEntry.getValue(pinned.value).place(0, pinned.y) }
        }
    }
}

private data class StickyHeaderElement(val sticky: Sticky, val content: @Composable () -> Unit) : ModifierNodeElement<StickyHeaderNode>() {
    override fun create() = StickyHeaderNode(sticky, content)

    override fun update(node: StickyHeaderNode) {
        if (node.sticky != sticky || node.content !== content) {
            node.detachEntry()
            node.sticky = sticky
            node.content = content
            node.attachEntry()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "stickyHeader"
        properties["sticky"] = sticky
    }
}

/** Meldet Lage und Größe der Überschrift und liefert ihren Inhalt. */
private class StickyHeaderNode(var sticky: Sticky, var content: @Composable () -> Unit) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var state: StickyHeaderState? = null

    override fun onAttach() = attachEntry()

    override fun onDetach() = detachEntry()

    fun attachEntry() {
        if (!isAttached) return
        state = currentValueOf(LocalStickyHeaders)
        state?.attach(sticky, content)
    }

    fun detachEntry() {
        state?.detach(sticky)
        state = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val state = state ?: return
        // In der Liste kann die Anmeldung nach dem Einhängen kommen: den Eintrag jedes Mal suchen.
        val entry = state.entries[sticky.key] ?: state.attach(sticky, content) ?: return
        state.place(entry, coordinates)
    }
}
