package app.atemkraft.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import app.atemkraft.ui.theme.Dimens
import kotlin.math.roundToInt

/**
 * Klebende Überschriften (Eigentümer 2026-10-31, „Titel bleibt kleben“): Eine Überschrift scrollt
 * wie jeder Inhalt, bis sie oben ankommt, und bleibt dort stehen – genau so, wie sie im Text steht.
 * Ebene 2 (Gruppe, Kapitel) hängt unter Ebene 1; die nächste Überschrift derselben oder einer
 * höheren Ebene schiebt die stehende Pixel für Pixel hinaus.
 *
 * Eine Seite markiert nur ihre Überschriften mit [stickyHeader]; das Gerüst ([TabListScaffold],
 * [PushScaffold]) legt den [StickyHeaderState] an und zeichnet die stehenden Überschriften in einer
 * Schicht über dem oberen Rand ([StickyHeaderOverlay]).
 *
 * Wie: Jede markierte Überschrift zeichnet sich zusätzlich in eine eigene [GraphicsLayer]; oben
 * steht also keine nachgebaute Kopie, sondern dieselben Pixel. Die Lage rechnet die Schicht in
 * jedem Bild aus dem Scrollstand ([LazyListState.layoutInfo] bzw. [ScrollState.value]) – an den
 * Finger gekoppelt, ohne Dauer und ohne Neuaufbau (MOTION-02 gilt nicht: Es bewegt sich nichts
 * von selbst). Die Schicht hat keine Semantik: Die echten Überschriften stehen weiter in der Liste.
 */
@Stable
class StickyHeaderState internal constructor(internal val source: StickySource) {
    internal val entries = mutableStateMapOf<Any, StickyEntry>()

    /** Koordinaten des sichtbaren Bereichs (Box um Liste bzw. Spalte); Bezug aller Lagen. */
    internal var viewport: LayoutCoordinates? = null

    /** Liste: Zahl der Einträge beim letzten Platzieren. */
    private var itemCount = -1

    internal fun attach(key: Any, level: Int, itemKey: Any?, owner: Any, context: GraphicsContext): StickyEntry {
        val entry = entries[key] ?: StickyEntry(level, itemKey, context.createGraphicsLayer(), context).also { entries[key] = it }
        entry.level = level
        entry.itemKey = itemKey
        entry.owner = owner
        return entry
    }

    /**
     * Eine Überschrift verlässt die Komposition. In einer Liste heißt das meist: Ihr Eintrag ist
     * aus dem Bild gescrollt. Dann bleibt sie gemerkt, solange sie schon einmal platziert war – sie
     * kann noch kleben; ob sie über oder unter dem Bild liegt, sagt ihr Index ([yOf]). Auf einer
     * Spalte (alles komponiert) und ohne Platzierung fällt sie weg. Nur der Knoten, dem der Eintrag
     * gerade gehört, löst ihn.
     */
    internal fun detach(key: Any, owner: Any) {
        val entry = entries[key]?.takeIf { it.owner === owner } ?: return
        entry.owner = null
        if (source is StickySource.Lazy && entry.index >= 0) return
        entries.remove(key)
        entry.release()
    }

    internal fun place(entry: StickyEntry, coordinates: LayoutCoordinates) {
        val viewport = viewport?.takeIf { it.isAttached } ?: return
        if (!coordinates.isAttached) return
        val position = viewport.localPositionOf(coordinates, Offset.Zero)
        val y = position.y.roundToInt()
        entry.x = position.x.roundToInt()
        entry.height = coordinates.size.height
        when (source) {
            is StickySource.Scroll -> entry.offset = y + source.state.value

            is StickySource.Lazy -> {
                val item = source.state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == entry.itemKey } ?: return
                entry.offset = y - item.offset
                entry.index = item.index
                // Hat sich die Liste geändert (Filter, Suche, neuer Eintrag), stimmen die gemerkten
                // Indizes abgelöster Überschriften nicht mehr: Sie fallen weg.
                val total = source.state.layoutInfo.totalItemsCount
                if (total != itemCount) {
                    itemCount = total
                    entries.entries.filter { it.value.owner == null }.forEach { (key, stale) ->
                        entries.remove(key)
                        stale.release()
                    }
                }
            }
        }
    }

    /** Lage der Überschrift im sichtbaren Bereich; [ABOVE] bzw. [BELOW], wenn ihr Eintrag nicht zu sehen ist. */
    private fun yOf(entry: StickyEntry): Int = when (source) {
        is StickySource.Scroll -> entry.offset - source.state.value

        is StickySource.Lazy -> {
            val info = source.state.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == entry.itemKey }
            when {
                item != null && entry.owner != null -> item.offset + entry.offset
                entry.index >= 0 && entry.index < (info.visibleItemsInfo.firstOrNull()?.index ?: 0) -> ABOVE
                else -> BELOW
            }
        }
    }

    /** Die stehenden Überschriften dieses Bilds (liest den Scrollstand). */
    internal fun pinned(viewportHeight: Int): List<Pinned<StickyEntry>> {
        if (entries.isEmpty()) return emptyList()
        val headers = entries.values.map { entry ->
            val order = when (source) {
                is StickySource.Scroll -> entry.offset.toLong()
                is StickySource.Lazy -> entry.index.toLong() * ORDER_STRIDE + entry.offset
            }
            StickyHeader(entry, entry.level, order, yOf(entry), entry.height)
        }
        return pinHeaders(headers, viewportHeight)
    }

    internal fun release() {
        entries.values.forEach { it.release() }
        entries.clear()
    }
}

/** Wo die Seite scrollt: Liste oder Spalte. */
internal sealed interface StickySource {
    class Lazy(val state: LazyListState) : StickySource

    class Scroll(val state: ScrollState) : StickySource
}

/** Eine markierte Überschrift: Ebene, Lage und ihre aufgezeichneten Pixel. */
internal class StickyEntry(level: Int, itemKey: Any?, val layer: GraphicsLayer, private val context: GraphicsContext) {
    var level by mutableIntStateOf(level)
    var itemKey: Any? = itemKey
    var x by mutableIntStateOf(0)

    /** Liste: Abstand von der Oberkante ihres Eintrags; Spalte: Lage im ganzen Inhalt. */
    var offset by mutableIntStateOf(0)
    var height by mutableIntStateOf(0)

    /** Liste: Index ihres Eintrags beim letzten Platzieren (-1: nie platziert). */
    var index by mutableIntStateOf(-1)

    /** Der Knoten, der die Überschrift gerade zeigt; `null`, wenn ihr Eintrag aus dem Bild ist. */
    var owner: Any? = null

    fun release() = context.releaseGraphicsLayer(layer)
}

/** Eingabe der Rechnung: eine Überschrift mit Ebene, Reihenfolge, Lage [y] und Höhe. */
internal class StickyHeader<T>(val value: T, val level: Int, val order: Long, val y: Int, val height: Int)

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
internal fun <T> pinHeaders(headers: List<StickyHeader<T>>, viewportHeight: Int): List<Pinned<T>> {
    val sorted = headers.sortedBy { it.order }
    val levels = sorted.map { it.level }.distinct().sorted()
    val all = pinLevels(sorted, levels)
    if (all.size > 1 && all.sumOf { it.height } > viewportHeight * MAX_PINNED_SHARE) {
        return pinLevels(sorted, listOf(levels.last()))
    }
    return all
}

private fun <T> pinLevels(sorted: List<StickyHeader<T>>, levels: List<Int>): List<Pinned<T>> {
    val result = mutableListOf<Pinned<T>>()
    var line = 0
    var parent = -1
    for (level in levels) {
        val current = sorted.indices.lastOrNull { i -> i > parent && sorted[i].level == level && sorted[i].y <= line } ?: continue
        val header = sorted[current]
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

/** Das Gerüst der Seite stellt hier seinen [StickyHeaderState] bereit; außerhalb tut [stickyHeader] nichts. */
internal val LocalStickyHeaders = compositionLocalOf<StickyHeaderState?> { null }

@Composable
internal fun rememberStickyHeaderState(listState: LazyListState): StickyHeaderState = rememberStickyHeaderState(listState) { StickySource.Lazy(listState) }

@Composable
internal fun rememberStickyHeaderState(scrollState: ScrollState): StickyHeaderState = rememberStickyHeaderState(scrollState) { StickySource.Scroll(scrollState) }

@Composable
private fun rememberStickyHeaderState(key: Any, source: () -> StickySource): StickyHeaderState {
    val state = remember(key) { StickyHeaderState(source()) }
    DisposableEffect(state) { onDispose { state.release() } }
    return state
}

/** Ob gerade etwas klebt: Dann trägt die Schicht die Haarlinie, nicht der Kopf. */
@Composable
internal fun StickyHeaderState.anyPinned(): State<Boolean> = remember(this) { derivedStateOf { pinned(Int.MAX_VALUE).isNotEmpty() } }

/** Merkt sich den sichtbaren Bereich, auf den sich alle Lagen beziehen; an die Box um Liste bzw. Spalte. */
internal fun Modifier.stickyViewport(state: StickyHeaderState): Modifier = onPlaced { state.viewport = it }

/**
 * Die Schicht über dem oberen Rand: so hoch wie die stehenden Überschriften, darin von der tiefsten
 * zur obersten je Hintergrund und aufgezeichnete Überschrift (eine hinausgeschobene gleitet unter
 * die darüber), unten die Haarlinie des Kopfs. Wischen auf ihr scrollt die Seite ([scroll]); ein
 * Tipp fällt nicht auf den Inhalt darunter. Ohne Semantik.
 */
@Composable
internal fun BoxScope.StickyHeaderOverlay(state: StickyHeaderState) {
    val background = MaterialTheme.colorScheme.background
    val hairline = MaterialTheme.colorScheme.outlineVariant
    val scroll = when (val source = state.source) {
        is StickySource.Lazy -> source.state
        is StickySource.Scroll -> source.state
    }
    var viewportHeight by remember { mutableIntStateOf(0) }
    Spacer(
        Modifier
            .layout { measurable, constraints ->
                viewportHeight = constraints.maxHeight
                val height = state.pinned(constraints.maxHeight).maxOfOrNull { it.y + it.height }?.coerceAtLeast(0) ?: 0
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .fillMaxWidth()
            .clipToBounds()
            .scrollable(scroll, Orientation.Vertical, reverseDirection = true)
            .drawBehind {
                val stack = state.pinned(viewportHeight)
                stack.asReversed().forEach { pinned ->
                    drawRect(background, topLeft = Offset(0f, pinned.y.toFloat()), size = Size(size.width, pinned.height.toFloat()))
                    translate(pinned.value.x.toFloat(), pinned.y.toFloat()) { drawLayer(pinned.value.layer) }
                }
                if (size.height <= 0f) return@drawBehind
                val stroke = Dimens.BorderThin.toPx()
                drawLine(hairline, Offset(0f, size.height - stroke / 2f), Offset(size.width, size.height - stroke / 2f), stroke)
            },
    )
}

/**
 * Markiert eine Überschrift als klebend: [level] 1 (Abschnitt, „Übungen“) oder 2 (Gruppe darunter,
 * „Beruhigen“). [key] ist auf der Seite eindeutig; in einer `LazyColumn` nennt [itemKey] den
 * Schlüssel des Eintrags, in dem die Überschrift steht. Ohne Gerüst mit [StickyHeaderState] wirkungslos.
 */
fun Modifier.stickyHeader(key: Any, level: Int, itemKey: Any? = null): Modifier = this then StickyHeaderElement(key, level, itemKey)

private data class StickyHeaderElement(val key: Any, val level: Int, val itemKey: Any?) : ModifierNodeElement<StickyHeaderNode>() {
    override fun create() = StickyHeaderNode(key, level, itemKey)

    override fun update(node: StickyHeaderNode) {
        if (node.key != key || node.level != level || node.itemKey != itemKey) {
            node.detachEntry()
            node.key = key
            node.level = level
            node.itemKey = itemKey
            node.attachEntry()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "stickyHeader"
        properties["key"] = key
        properties["level"] = level
    }
}

private class StickyHeaderNode(var key: Any, var level: Int, var itemKey: Any?) :
    Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var state: StickyHeaderState? = null
    private var entry: StickyEntry? = null

    override fun onAttach() = attachEntry()

    override fun onDetach() = detachEntry()

    fun attachEntry() {
        val state = currentValueOf(LocalStickyHeaders) ?: return
        this.state = state
        entry = state.attach(key, level, itemKey, owner = this, requireGraphicsContext())
    }

    fun detachEntry() {
        state?.detach(key, owner = this)
        state = null
        entry = null
    }

    override fun ContentDrawScope.draw() {
        val layer = entry?.layer
        if (layer == null) {
            drawContent()
            return
        }
        layer.record { this@draw.drawContent() }
        drawLayer(layer)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val entry = entry ?: return
        state?.place(entry, coordinates)
    }
}
