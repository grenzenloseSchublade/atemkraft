package app.atemkraft.ui.screenshots

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.toSize
import app.atemkraft.ui.components.CARD_HEADER_TAG
import app.atemkraft.ui.components.CONTROL_ROW_TAG
import app.atemkraft.ui.components.TITLE_ROW_ACTIONS_TAG
import app.atemkraft.ui.components.TITLE_ROW_INDICATOR_TAG
import app.atemkraft.ui.components.TITLE_ROW_TAG
import app.atemkraft.ui.components.TITLE_ROW_TITLE_TAG
import app.atemkraft.ui.navigation.TAB_BAR_GRIP_TAG
import app.atemkraft.ui.theme.Dimens
import java.util.Locale
import kotlin.math.abs

/**
 * Prüft die Layout-Grammatik (STYLEGUIDE §4.5) an jedem gerenderten Bild über die Test-Tags der
 * Bausteine (`CardHeader`, `SectionHeader`, `GroupLabel`, Steuerzeilen). Eine Prüfung über alle
 * Szenen, kein Test je Karte. Jeder Befund `ANATOMIE` ist ein Testfehler:
 * - Glyph einer Aktion oder eines Zeichens nicht mittig auf der ersten Titelzeile (± 4 dp),
 * - Glyph nicht bündig mit der Inhaltskante (± 1 dp),
 * - Glyph liegt über Text, oder die Tippfläche einer Aktion liegt über dem Titel (der Titel hält
 *   die Breite der Aktionen frei),
 * - Fließtext einer Karte mit Aktionen endet vor der Inhaltskante (er muss unter den Aktionen
 *   durchlaufen; ohne Aktionen – Zeichen, Expander, Einstellungen – gibt es nichts zu unterlaufen,
 *   und Zeilen im Inhalt dürfen dort eigene Endspalten haben),
 * - Steuerzeile oder tippbare Kopfzeile niedriger als 48 dp,
 * - Tippfläche des Griffs der Tab-Leiste (Tiefe) über einem Bedienelement, das nicht wegscrollen
 *   kann (Startleiste, „Anpassen ›“, Ende der Seite).
 */
internal object AnatomyFindings {

    /** Toleranz der Glyph-Mitte zur Titelzeile, in dp. */
    private const val CENTER_TOLERANCE_DP = 4f

    /** Toleranz der bündigen Kante, in dp. */
    private const val EDGE_TOLERANCE_DP = 1f

    fun findings(nodes: List<SemanticsNode>, density: Float, screen: String): List<String> {
        fun dp(px: Float) = "%.1f".format(Locale.ROOT, px / density)
        val out = mutableListOf<String>()
        val placed = nodes.filter { it.layoutInfo.isPlaced && (it.layoutInfo.parentInfo?.isPlaced ?: true) }
        val min = Dimens.MinTouchTarget.value * density - 0.5f

        val controlRows = placed.filter { it.tag() == CONTROL_ROW_TAG }
        controlRows.forEach { row ->
            if (row.size.height < min) out += "ANATOMIE\t$screen\tSteuerzeile „${nameOf(row)}“ ${dp(row.size.height.toFloat())} dp hoch < 48"
        }

        val rows = placed.filter { it.tag() == CARD_HEADER_TAG || it.tag() == TITLE_ROW_TAG }
        val allTexts = placed.filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }
        rows.forEach { row ->
            val where = "$screen\t${nameOf(row)}"
            val inCard = row.tag() == CARD_HEADER_TAG
            val below = row.descendants().filter { it.layoutInfo.isPlaced }.toList()
            val title = below.firstOrNull { it.tag() == TITLE_ROW_TITLE_TAG }
            val titleText = title?.let { t -> (sequenceOf(t) + t.descendants()).firstOrNull { it.config.contains(SemanticsActions.GetTextLayoutResult) } }
            if (title == null || titleText == null) {
                out += "ANATOMIE\t$where\tTitelzeile ohne markierten Titel"
                return@forEach
            }
            if (row.config.contains(SemanticsActions.OnClick) && row.size.height < min) {
                out += "ANATOMIE\t$where\ttippbare Kopfzeile ${dp(row.size.height.toFloat())} dp hoch < 48"
            }
            val line = firstLine(titleText)
            val lineCenter = titleText.positionInRoot.y + (line.top + line.bottom) / 2
            val lineHalf = (line.bottom - line.top) / 2
            val rowRect = row.rect()
            val contentEdge = rowRect.right
            // Der Baustein reicht senkrecht bis zum nächsten markierten Baustein darunter (Titel- oder
            // Steuerzeile; ohne eigene Semantik für Karte oder Abschnitt ist das sein Fenster); Glyphen
            // dürfen oben ins Polster ragen.
            val nextTop = (rows + controlRows).map { it.positionInRoot.y }.filter { it > rowRect.top + density }.minOrNull() ?: Float.POSITIVE_INFINITY
            val window = Rect(rowRect.left, rowRect.top - Dimens.MinTouchTarget.value * density, rowRect.right, nextTop)
            val end = below.firstOrNull { it.tag() == TITLE_ROW_ACTIONS_TAG } ?: below.firstOrNull { it.tag() == TITLE_ROW_INDICATOR_TAG }
            if (end != null) {
                val endNodes = end.descendants().filter { it.layoutInfo.isPlaced }.toList()
                // Glyphen: Icons mit Namen; ein dekoratives Zeichen ohne Knoten vertritt seine Hülle.
                val glyphs = endNodes.filter { it.children.isEmpty() && it.config.contains(SemanticsProperties.ContentDescription) }
                    .ifEmpty { listOf(end) }
                glyphs.forEach { g ->
                    val r = g.rect()
                    if (abs(r.center.y - lineCenter) > lineHalf + CENTER_TOLERANCE_DP * density) {
                        out += "ANATOMIE\t$where\tGlyph „${nameOf(g)}“ Mitte ${dp(r.center.y - rowRect.top)} dp, Titelzeile ${dp(lineCenter - rowRect.top)} dp"
                    }
                }
                val right = glyphs.maxOf { it.rect().right }
                if (abs(right - contentEdge) > EDGE_TOLERANCE_DP * density) {
                    out += "ANATOMIE\t$where\tGlyph endet ${dp(right - rowRect.left)} dp, Inhaltskante ${dp(contentEdge - rowRect.left)} dp"
                }
                // Aktionen überlappen keinen Text: der Glyph nie, die Tippfläche nicht den Titel. Sichtbare
                // Lagen (beschnitten): Was aus einer Liste gescrollt ist, liegt nicht über der Leiste darunter.
                val texts = allTexts.filter { it !in endNodes && it.rect().overlaps(window) && it.visible() }
                glyphs.filter { it.visible() }.forEach { g ->
                    texts.filter { it.boundsInRoot.overlaps(g.boundsInRoot) }.forEach { t ->
                        out += "ANATOMIE\t$where\tGlyph „${nameOf(g)}“ über Text „${nameOf(t)}“"
                    }
                }
                ControlFindings.controls(endNodes).forEach { c ->
                    if (c.rect().overlaps(titleText.rect())) out += "ANATOMIE\t$where\tTippfläche „${nameOf(c)}“ über dem Titel"
                }
            }
            // Karte mit Aktionen: Mehrzeiliger Text unter der Titelzeile reicht bis zur Inhaltskante.
            if (inCard && below.any { it.tag() == TITLE_ROW_ACTIONS_TAG }) {
                allTexts
                    .filter { it !in below && it.positionInRoot.y >= rowRect.bottom - density && it.positionInRoot.y < nextTop && it.rect().overlaps(window) }
                    .forEach { t ->
                        if (lineCount(t) > 1 && t.rect().right < contentEdge - EDGE_TOLERANCE_DP * density) {
                            out += "ANATOMIE\t$where\tFließtext „${nameOf(t)}“ endet ${dp(t.rect().right - rowRect.left)} dp vor der Inhaltskante ${dp(contentEdge - rowRect.left)} dp"
                        }
                    }
            }
        }

        // Griff der Tab-Leiste in der Tiefe: Seine Tippfläche überdeckt kein Bedienelement – weder in
        // der Startleiste noch im Inhalt, der dort steht, wenn die Seite nicht weiter scrollen kann.
        // Inhalt, der weiterscrollt, läuft nur unter ihm durch (am Ende hält TabBarGripClearance frei).
        placed.firstOrNull { it.tag() == TAB_BAR_GRIP_TAG }?.let { grip ->
            val zone = grip.rect().deflate(EDGE_TOLERANCE_DP * density / 2)
            ControlFindings.controls(placed)
                .filter { it.id != grip.id && it.visible() && !it.scrollsOn() && it.boundsInRoot.overlaps(zone) }
                .forEach { out += "ANATOMIE\t$screen\tGriff der Tab-Leiste über „${nameOf(it)}“" }
        }
        return out
    }

    /** Liegt in einer senkrechten Scroll-Fläche, die noch weiter nach unten scrollen kann. */
    private fun SemanticsNode.scrollsOn(): Boolean = generateSequence(parent) { it.parent }.any { a ->
        a.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.let { it.value() < it.maxValue() } == true
    }

    private fun SemanticsNode.tag() = config.getOrNull(SemanticsProperties.TestTag)

    private fun SemanticsNode.rect() = Rect(positionInRoot, size.toSize())

    /** Sichtbar: nicht von einem Elternteil (Liste, Rahmen) ganz beschnitten. */
    private fun SemanticsNode.visible() = boundsInRoot.width > 0f && boundsInRoot.height > 0f

    private fun SemanticsNode.descendants(): Sequence<SemanticsNode> = children.asSequence().flatMap { sequenceOf(it) + it.descendants() }

    private fun layout(node: SemanticsNode): TextLayoutResult? {
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.firstOrNull()
    }

    /** Erste Zeile des Texts (oben, unten) relativ zum Knoten, in px; ohne Layout der ganze Knoten. */
    private fun firstLine(node: SemanticsNode): Rect {
        val l = layout(node) ?: return Rect(0f, 0f, node.size.width.toFloat(), node.size.height.toFloat())
        return Rect(0f, l.getLineTop(0), node.size.width.toFloat(), l.getLineBottom(0))
    }

    private fun lineCount(node: SemanticsNode): Int = layout(node)?.lineCount ?: 1

    private fun nameOf(n: SemanticsNode): String {
        fun own(x: SemanticsNode) = x.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            ?: x.config.getOrNull(SemanticsProperties.Text)?.joinToString()
        fun walk(x: SemanticsNode): String? = own(x) ?: x.children.firstNotNullOfOrNull { walk(it) }
        return (walk(n) ?: "?").take(40).replace("\n", " ")
    }
}
