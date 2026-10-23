package app.atemkraft.ui.screenshots

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import app.atemkraft.ui.theme.Dimens
import java.util.Locale

/**
 * Prüft die Bedienelemente eines gerenderten Bildes (A11Y-05, A11Y-02, LAYOUT-05). Bedienelement
 * ist jeder platzierte Knoten mit Klick-Semantik ODER mit einem clickable-/toggleable-/
 * selectable-Element im Layout – der zweite Weg findet auch Knöpfe, deren Semantik
 * `clearAndSetSemantics` geleert hat.
 *
 * Befunde (jeder ist ein Testfehler):
 * - `TIPPFLAECHE`: Platz im Layout < 48 × 48 dp. Bewusst nicht `touchBoundsInRoot`: Das vergrößert
 *   Compose für jeden Klick-Knoten automatisch auf 48, auch wenn das Layout nur 40 reserviert und
 *   Nachbarn in die Zone ragen. Ausnahme: Link mitten im Fließtext (WCAG 2.5.8 „Inline“).
 * - `OHNE_KLICK`: klickbar im Layout, aber ohne Klick-Aktion für TalkBack/Switch Access.
 * - `BESCHNITTEN`: ein Elternteil schneidet das Element ab – in jeder Richtung außerhalb einer
 *   Scroll-Fläche dieser Richtung (dort ist es nur weggescrollt; seitlich heute nur die
 *   Anlass-Chips, MUSTER-09).
 * - `AUSSERHALB`: das Element ragt links oder rechts aus dem Bild (außer in einer seitlichen
 *   Scroll-Fläche).
 */
internal object ControlFindings {
    // Klassennamen der Modifier-Elemente aus compose-foundation (kein offizielles API: nach einem
    // Compose-Update prüfen, ob die Erkennung noch greift).
    private val CLICK_ELEMENTS = setOf(
        "ClickableElement",
        "CombinedClickableElement",
        "ToggleableElement",
        "TriStateToggleableElement",
        "SelectableElement",
    )

    private fun hasClickElement(node: SemanticsNode) = node.layoutInfo.getModifierInfo().any { it.modifier::class.simpleName in CLICK_ELEMENTS }

    private fun hasClickSemantics(node: SemanticsNode) = node.config.contains(SemanticsActions.OnClick)

    /** Platzierte Bedienelemente; SubcomposeLayout-Messproben (SegmentedChoiceRow) sind ungeplatzt. */
    fun controls(nodes: List<SemanticsNode>): List<SemanticsNode> = nodes.filter { n ->
        n.layoutInfo.isPlaced &&
            (n.layoutInfo.parentInfo?.isPlaced ?: true) &&
            (hasClickSemantics(n) || hasClickElement(n))
    }

    /**
     * Alle Befunde eines Bildes. [frame] ist das Bild in Root-Koordinaten (px), [density] px je dp;
     * jede Zeile beginnt mit Befund und [screen].
     */
    fun findings(nodes: List<SemanticsNode>, frame: Rect, density: Float, screen: String): List<String> {
        fun dp(px: Float) = "%.1f".format(Locale.ROOT, px / density)
        val min = Dimens.MinTouchTarget.value * density - 0.5f
        val tolerance = density // 1 dp
        return controls(nodes).flatMap { n ->
            val where = "$screen\t${nameOf(n)}"
            val out = mutableListOf<String>()
            val ancestors = generateSequence(n.parent) { it.parent }.toList()
            val inlineLink = ancestors.any { it.config.contains(SemanticsActions.GetTextLayoutResult) }
            val w = n.layoutInfo.width.toFloat()
            val h = n.layoutInfo.height.toFloat()
            if (!inlineLink && (w < min || h < min)) out += "TIPPFLAECHE\t$where\tLayout ${dp(w)} × ${dp(h)} dp < 48"
            if (!hasClickSemantics(n)) out += "OHNE_KLICK\t$where\tklickbar, aber ohne Klick-Aktion (A11Y-02)"

            val p = n.positionInRoot
            val visible = Rect(p.x, p.y, p.x + n.size.width, p.y + n.size.height)
            val clipped = n.boundsInRoot
            val vScroll = ancestors.any { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) }
            // Seitlich scrollende Fläche (Anlass-Chips, MUSTER-09): waagerecht nur weggescrollt.
            val hScroll = ancestors.any { it.config.contains(SemanticsProperties.HorizontalScrollAxisRange) }
            val scrolledAway = (vScroll && clipped.height <= 0f) || (hScroll && clipped.width <= 0f)
            val cutX = !hScroll && clipped.width < visible.width - tolerance
            val cutY = !vScroll && clipped.height < visible.height - tolerance
            if (!scrolledAway && (cutX || cutY)) {
                out += "BESCHNITTEN\t$where\tsichtbar ${dp(clipped.width)} × ${dp(clipped.height)} von ${dp(visible.width)} × ${dp(visible.height)} dp"
            }
            if (!hScroll && (visible.left < frame.left - 0.5f || visible.right > frame.right + 0.5f)) {
                out += "AUSSERHALB\t$where\tx ${dp(visible.left - frame.left)} … ${dp(visible.right - frame.left)} dp bei Bildbreite ${dp(frame.width)}"
            }
            out
        }
    }

    /** Erste Beschreibung oder erster Text im Knoten (für lesbare Befunde). */
    private fun nameOf(n: SemanticsNode): String {
        fun own(x: SemanticsNode) = x.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            ?: x.config.getOrNull(SemanticsProperties.Text)?.joinToString()
        fun walk(x: SemanticsNode): String? = own(x) ?: x.children.firstNotNullOfOrNull { walk(it) }
        return (walk(n) ?: "?").take(40).replace("\n", " ")
    }
}
