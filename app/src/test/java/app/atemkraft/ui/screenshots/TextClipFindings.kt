package app.atemkraft.ui.screenshots

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.toSize

/**
 * LAYOUT-03: Schneidet eine runde Form einen Text an? Fehlerbild aus v1.6.5: Die Mitte des
 * Zeitrings war eng um Zahl und Einheit mit `clip(shapes.large)` gerundet, die Ecken schnitten
 * „Minuten“ an – ohne Ellipse und ohne Umbruch, also unsichtbar für die übrigen Textprüfungen.
 *
 * Geprüft wird jeder `clip(shape)` mit Rundung (CircleShape, abgerundete Ecken) am Textknoten und
 * an allen Vorfahren im Layout, auch ohne eigene Semantik. Je Zeile müssen die vier Ecken der
 * Glyphen in der Form liegen: waagerecht die Zeile, senkrecht nur das Band, in dem Glyphen stehen
 * (Grundlinie − [INK_ASCENT] … + [INK_DESCENT] Schriftgrößen). Die leeren Ecken der Zeilenbox
 * darf eine Rundung schneiden (Pillen-Buttons). Das Band gilt für jede Zeile ganz, auch wenn
 * ihr letztes Zeichen keine Unterlänge hat: Text, der ohne Rand bis an eine Pillen-Rundung
 * reicht, wird gemeldet (Abhilfe: Endrand, siehe `DisclosureToggle`). In einer senkrechten
 * Scrollfläche zählen nur Ecken im sichtbaren Ausschnitt – was weggescrollt ist, schneidet die
 * Scrollfläche selbst ab (Hinweisliste im Sicherheitsdialog). Nicht geprüft: eckige Clips (Scrollflächen),
 * eigene Pfad-Formen, `graphicsLayer { clip = true }` mit Block, Eingabefelder (ihr Text liegt
 * nicht am Ursprung des Knotens).
 */
internal object TextClipFindings {
    // Roboto: Ober-/Versalhöhe ≈ 0,73 em, Unterlänge ≈ 0,21 em; etwas Reserve nach außen.
    private const val INK_ASCENT = 0.75f
    private const val INK_DESCENT = 0.22f

    // Kantenglättung und Rundung im Subpixel zählen nicht.
    private const val TOLERANCE_DP = 0.5f

    // Element hinter `Modifier.clip(shape)` / `graphicsLayer(shape = …, clip = true)` (kein
    // offizielles API; der Selbsttest in ControlTouchTest meldet, wenn ein Update es umbenennt).
    private const val CLIP_ELEMENT = "GraphicsLayerElement"

    /** Texte des Knotens, die eine runde Form anschneidet (leer = alles sichtbar). */
    fun findings(node: SemanticsNode): List<String> {
        if (!node.layoutInfo.isPlaced || SemanticsProperties.EditableText in node.config) return emptyList()
        val clips = roundedClips(node.layoutInfo)
        if (clips.isEmpty()) return emptyList()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val origin = node.layoutInfo.coordinates.positionInRoot()
        val inset = node.layoutInfo.density.density * TOLERANCE_DP
        val viewport = generateSequence(node.parent) { it.parent }
            .firstOrNull { SemanticsProperties.VerticalScrollAxisRange in it.config }?.boundsInRoot
        return layouts.mapNotNull { layout ->
            val cut = inkCorners(layout, inset).map { origin + it }
                .filter { viewport == null || viewport.contains(it) }
                .any { corner -> clips.any { !it.contains(corner) } }
            if (cut) layout.layoutInput.text.text else null
        }
    }

    /** Runde Clips am Knoten und über ihm, in Root-Koordinaten. */
    private fun roundedClips(start: LayoutInfo): List<RoundRect> = generateSequence(start) { it.parentInfo }
        .flatMap { info ->
            info.getModifierInfo().asSequence().mapNotNull { m ->
                if (m.modifier::class.simpleName != CLIP_ELEMENT) return@mapNotNull null
                val props = (m.modifier as? InspectableValue)?.inspectableElements?.associate { it.name to it.value }.orEmpty()
                val shape = props["shape"] as? Shape
                if (props["clip"] != true || shape == null) return@mapNotNull null
                val outline = shape.createOutline(m.coordinates.size.toSize(), info.layoutDirection, info.density)
                (outline as? Outline.Rounded)?.roundRect?.translate(m.coordinates.positionInRoot())
            }
        }
        .toList()

    /**
     * Ecken des Glyphenbands jeder Zeile, um [inset] nach innen gerückt (Text-Koordinaten).
     *
     * Das Layout aus `GetTextLayoutResult` ist neu gesetzt, mit der vollen verfügbaren Breite
     * (`layoutInput.constraints.maxWidth`), gezeichnet wird aber in der gemessenen Breite
     * (`size.width`, z. B. Stepper-Wert: 528 px Platz, 76 px breit). Bei zentriertem oder
     * rechtsbündigem Text stehen die Zeilen darin um den Unterschied (bzw. die Hälfte) versetzt –
     * ohne Ausgleich lag die „10“ im Stepper rechts außerhalb der Karte (Fehlalarm). Umbrüche
     * ändern sich dadurch nicht: Die gezeichnete Breite reicht für die längste Zeile.
     */
    private fun inkCorners(layout: TextLayoutResult, inset: Float): List<Offset> {
        val input = layout.layoutInput
        val em = input.style.fontSize.takeIf { it.isSp }?.let { with(input.density) { it.toPx() } }
        val drawnWidth = layout.size.width.toFloat()
        val extra = (layout.multiParagraph.width - drawnWidth).coerceAtLeast(0f)
        return (0 until layout.lineCount).flatMap { line ->
            val shift = extra * alignFactor(input.style.textAlign, layout.getParagraphDirection(layout.getLineStart(line)))
            val left = layout.getLineLeft(line) - shift + inset
            // Blocksatz füllt die gesetzte Breite; gezeichnet endet die Zeile an der gemessenen.
            val right = (layout.getLineRight(line) - shift).let { if (extra > 0f) minOf(it, drawnWidth) else it } - inset
            val baseline = layout.getLineBaseline(line)
            val top = maxOf(layout.getLineTop(line), em?.let { baseline - INK_ASCENT * it } ?: Float.NEGATIVE_INFINITY) + inset
            val bottom = minOf(layout.getLineBottom(line), em?.let { baseline + INK_DESCENT * it } ?: Float.POSITIVE_INFINITY) - inset
            if (right <= left || bottom <= top) emptyList() else listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
        }
    }

    /** Anteil des Breitenunterschieds, um den die Zeilen versetzt stehen: links 0, Mitte ½, rechts 1. */
    private fun alignFactor(align: TextAlign, direction: ResolvedTextDirection): Float {
        val rtl = direction == ResolvedTextDirection.Rtl
        return when (align) {
            TextAlign.Center -> 0.5f
            TextAlign.Left -> 0f
            TextAlign.Right -> 1f
            TextAlign.End -> if (rtl) 0f else 1f
            else -> if (rtl) 1f else 0f // Start, Justify (letzte Zeile), Unspecified
        }
    }
}
