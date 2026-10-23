package app.atemkraft.ui.atemlehre

import android.animation.ValueAnimator
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.isSpecified
import app.atemkraft.R
import app.atemkraft.data.Atemlehre
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.LoreVayu
import app.atemkraft.domain.Vayu
import app.atemkraft.domain.withoutMarks
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.rememberFirstLineHeight
import app.atemkraft.ui.components.sanskritText
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.VAYU_PULSE_HIGH
import app.atemkraft.ui.theme.VAYU_PULSE_LOW
import app.atemkraft.ui.theme.VayuAlpha
import app.atemkraft.ui.theme.VayuApana
import app.atemkraft.ui.theme.VayuAuraAlpha
import app.atemkraft.ui.theme.VayuFieldAlpha
import app.atemkraft.ui.theme.VayuFigureFill
import app.atemkraft.ui.theme.VayuFigureOutline
import app.atemkraft.ui.theme.VayuLabelAlpha
import app.atemkraft.ui.theme.VayuLineAlpha
import app.atemkraft.ui.theme.VayuPrana
import app.atemkraft.ui.theme.VayuSamana
import app.atemkraft.ui.theme.VayuUdana
import app.atemkraft.ui.theme.VayuVyana
import app.atemkraft.ui.theme.VayuWholeAlpha

/**
 * Inhalt des Abschnitts „Die fünf *vāyus*“ der Atemlehre (Design Vayus.dc.html, Nutzer
 * 2026-10-22): kurze Einleitung, das Bild ([VayuFigure]), ein Hinweis und die Liste der fünf als
 * Schalter ([VayuRow]), darunter die Quelle. Tippen auf einen Eintrag oder auf die Stelle am Körper
 * (Nutzer 2026-10-23) hebt eine Strömung im Bild und in der Liste hervor, ein zweiter Tipp oder ein
 * anderer vāyu löst sie wieder. Die Wahl überlebt den Weg in eine Übung.
 */
@Composable
internal fun VayusContent() {
    var selected by rememberSaveable { mutableStateOf<Vayu?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
        HintText(Atemlehre.vayusIntro)
        // Fläche wie in der Vorlage `surface`, eine Stufe dunkler als die Karten: das Bild steht
        // für sich, ohne wie eine antippbare Karte zu wirken.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = Dimens.GapSmall),
            contentAlignment = Alignment.Center,
        ) {
            VayuFigure(selected = selected, onSelect = { vayu -> selected = if (selected == vayu) null else vayu })
        }
        HintText(stringResource(R.string.atemlehre_vayus_hint))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
            Atemlehre.vayus.forEach { vayu ->
                VayuRow(
                    vayu = vayu,
                    selected = selected == vayu.vayu,
                    onToggle = { selected = if (selected == vayu.vayu) null else vayu.vayu },
                )
            }
        }
        HintText(Atemlehre.vayusSource)
    }
}

/** Farbe eines vāyu, nur in Bild und Liste (Color.kt, FARBE-04). */
internal fun Vayu.color(): Color = when (this) {
    Vayu.PRANA -> VayuPrana
    Vayu.APANA -> VayuApana
    Vayu.SAMANA -> VayuSamana
    Vayu.UDANA -> VayuUdana
    Vayu.VYANA -> VayuVyana
}

/**
 * Ein Eintrag der Liste als Schalter (Rolle Switch, Zustand an/aus; die ganze Zeile ist die
 * Tippfläche): Farbpunkt, Name kursiv in der Farbe des vāyu · Ort, darunter der Satz der Lehre.
 * Gewählt mit Kartenfläche und Rand in der vāyu-Farbe – Fläche und Rand tragen die Auswahl, nicht
 * die Farbe allein (FARBE-05).
 */
@Composable
private fun VayuRow(vayu: LoreVayu, selected: Boolean, onToggle: () -> Unit) {
    val color = vayu.vayu.color()
    val shape = MaterialTheme.shapes.small
    val nameStyle = MaterialTheme.typography.bodyMedium
    val firstLine = rememberFirstLineHeight(nameStyle)
    val placeColor = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY)
    // Fett über das Gewicht einer Titel-Rolle (TYPO-01: kein eigenes FontWeight).
    val nameWeight = MaterialTheme.typography.titleSmall.fontWeight
    val title = remember(vayu, color, placeColor, nameWeight) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = color, fontWeight = nameWeight)) { append(sanskritText(vayu.name)) }
            withStyle(SpanStyle(color = placeColor)) { append(DOT_SEPARATOR + vayu.place) }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .border(Dimens.BorderThin, if (selected) color else Color.Transparent, shape)
            .toggleable(value = selected, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = Dimens.ListGap, vertical = Dimens.GapSmall),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        // Punkt mittig auf der ersten Zeile, auch wenn Name und Ort umbrechen.
        Box(Modifier.height(firstLine), contentAlignment = Alignment.Center) {
            Box(Modifier.size(Dimens.GapSmall).clip(CircleShape).background(color))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline)) {
            Text(text = title, style = nameStyle)
            BodyText(vayu.text)
        }
    }
}

/**
 * Bild der fünf vāyus: stehende Piktogramm-Figur (runder Kopf, Arme leicht abgespreizt, getrennte
 * Beine, nur Umriss) mit weichen Farbfeldern, auf die Figur zugeschnitten – udāna in Hals und
 * Kopf, prāṇa im Brustraum, samāna an der Nabelmitte, apāna in Becken und Beinen, vyāna als
 * ganze Figur und Aura –, dazu gepunktete Strömungslinien in der Richtung der Lehre, die langsam
 * fließen. Ist [selected] gesetzt, steht dieser vāyu kräftig da, pulsiert ruhig und seine Linien
 * fließen schneller; die anderen treten zurück. Bei abgeschalteten System-Animationen
 * (Animator-Skala 0) steht alles still (MOTION-02).
 *
 * Geometrie in den Einheiten der Vorlage (viewBox 352 × 450), skaliert auf die Breite; die
 * Beschriftungen folgen der Systemschrift (`labelMedium`, kursiv) und bleiben im Bild.
 * Für TalkBack ein Knoten mit Beschreibung; die Liste darunter trägt dieselbe Information und ist
 * der zugängliche Weg zur Wahl.
 *
 * Antippen wählt den vāyu an dieser Stelle ([onSelect], Nutzer 2026-10-23): seine Beschriftung,
 * sein Farbfeld (das kleinere zuerst, samāna vor apāna), sonst im Bereich von Figur, Aura und
 * Strömungslinien vyāna; daneben nichts. Das Bild ist ein Fünftel schmaler als die Spalte
 * ([FIGURE_WIDTH], Nutzer 2026-10-23).
 */
@Composable
internal fun VayuFigure(selected: Vayu?, onSelect: (Vayu) -> Unit, modifier: Modifier = Modifier) {
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    val flow: State<Float>
    val flowFast: State<Float>
    val pulse: State<Float>
    if (animate) {
        val transition = rememberInfiniteTransition(label = "vayu")
        flow = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.VayuFlow, easing = LinearEasing)), label = "flow")
        flowFast = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.VayuFlowSelected, easing = LinearEasing)), label = "flowFast")
        pulse = transition.animateFloat(
            VAYU_PULSE_LOW,
            VAYU_PULSE_HIGH,
            infiniteRepeatable(tween(Motion.VayuPulse / 2, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "pulse",
        )
    } else {
        // Still: Linien stehen, die gewählte Fläche zeigt ihre feste Stufe (alphaOf).
        flow = remember { mutableFloatStateOf(0f) }
        flowFast = flow
        pulse = flow
    }
    val description = stringResource(R.string.cd_vayu_figure)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontStyle = FontStyle.Italic)
    val geometry = remember { VayuGeometry() }
    // Lage der Beschriftungen in px, beim Zeichnen gemessen; nur das Antippen liest sie (kein State).
    val labelRects = remember { mutableMapOf<Vayu, Rect>() }
    val select by rememberUpdatedState(onSelect)

    Canvas(
        modifier = modifier
            .widthIn(max = Sizes.VayuFigureMaxWidth)
            .fillMaxWidth(FIGURE_WIDTH)
            .aspectRatio(VIEW_W / VIEW_H)
            .semantics {
                contentDescription = description
                role = Role.Image
            }
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val scale = size.width / VIEW_W
                    vayuAt(position, scale, labelRects)?.let(select)
                }
            },
    ) {
        val scale = size.width / VIEW_W

        // Gewählter vāyu: Fläche und Aura pulsieren in der Spanne der Vorlage, sonst die feste Stufe.
        fun alphaOf(vayu: Vayu, levels: VayuAlpha, pulsing: Boolean = false): Float = when {
            selected == vayu -> if (animate && pulsing) pulse.value else levels.selected
            selected == null -> levels.idle
            else -> levels.dimmed
        }
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawAura(geometry, VayuVyana.copy(alpha = alphaOf(Vayu.VYANA, VayuAuraAlpha, pulsing = true)))
            drawPath(geometry.figure, VayuFigureFill)
            clipPath(geometry.figure) {
                drawRect(VayuVyana.copy(alpha = alphaOf(Vayu.VYANA, VayuWholeAlpha, pulsing = true)), size = geometry.viewSize)
                FIELDS.forEach { field ->
                    drawSoftEllipse(field, field.vayu.color().copy(alpha = alphaOf(field.vayu, VayuFieldAlpha, pulsing = true)))
                }
            }
            drawPath(geometry.figure, VayuFigureOutline, style = Stroke(width = OUTLINE_WIDTH, join = StrokeJoin.Round))
            geometry.flows.forEach { (vayu, path) ->
                val on = selected == vayu
                val progress = if (on) flowFast.value else flow.value
                drawPath(
                    path = path,
                    color = vayu.color().copy(alpha = alphaOf(vayu, VayuLineAlpha)),
                    style = Stroke(
                        width = if (on) FLOW_WIDTH_SELECTED else FLOW_WIDTH,
                        cap = StrokeCap.Round,
                        // Die Striche rücken in Pfadrichtung vor: Die Phase sinkt (wie stroke-dashoffset → −24).
                        pathEffect = PathEffect.dashPathEffect(DASH, DASH_PERIOD * 2 * (1f - progress)),
                    ),
                )
            }
            LABELS.forEach { label ->
                label.leader?.let { (from, to) ->
                    drawLine(label.vayu.color().copy(alpha = alphaOf(label.vayu, VayuLabelAlpha)), from, to, strokeWidth = LEADER_WIDTH)
                }
            }
        }
        LABELS.forEach { label ->
            val layout = measurer.measure(Atemlehre.vayus.first { it.vayu == label.vayu }.name.withoutMarks(), labelStyle)
            val width = layout.size.width.toFloat()
            val anchor = label.x * scale
            // samāna steht links der Figur und wächst nach links, damit es die Führungslinie nicht verdeckt.
            val left = (if (label.endAnchored) anchor - width else anchor).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
            val top = (label.baseline * scale - layout.firstBaseline).coerceIn(0f, (size.height - layout.size.height).coerceAtLeast(0f))
            drawText(layout, color = label.vayu.color(), topLeft = Offset(left, top), alpha = alphaOf(label.vayu, VayuLabelAlpha))
            labelRects[label.vayu] = Rect(Offset(left, top), Size(width, layout.size.height.toFloat()))
        }
    }
}

/**
 * Der vāyu an [position] (px; [scale] px je Einheit der Vorlage): Beschriftung ([labels], mit
 * [LABEL_SLACK] Rand), dann die Farbfelder vom kleinsten an, dann vyāna im Bereich von Figur,
 * Aura und Strömungslinien ([VYANA_AREA]); sonst null.
 */
private fun vayuAt(position: Offset, scale: Float, labels: Map<Vayu, Rect>): Vayu? {
    labels.entries.firstOrNull { it.value.inflate(LABEL_SLACK * scale).contains(position) }?.let { return it.key }
    val x = position.x / scale
    val y = position.y / scale
    FIELDS.sortedBy { it.rx * it.ry }.firstOrNull { field ->
        val dx = (x - field.cx) / field.rx
        val dy = (y - field.cy) / field.ry
        dx * dx + dy * dy <= 1f
    }?.let { return it.vayu }
    return if (VYANA_AREA.contains(Offset(x, y))) Vayu.VYANA else null
}

/** vyāna als weiche Aura: der Umriss mehrfach, breit und leise übereinander – ein Weichzeichner ohne Blur-Filter. */
private fun DrawScope.drawAura(geometry: VayuGeometry, color: Color) {
    AURA_WIDTHS.forEach { width ->
        drawPath(geometry.figure, color.copy(alpha = color.alpha * AURA_LAYER), style = Stroke(width = width, join = StrokeJoin.Round))
    }
}

/**
 * Weiches Farbfeld wie ein Gaußscher Weichzeichner (σ ≈ [SOFT]) über einer Ellipse: radialer
 * Verlauf, voll bis 2σ vor dem Rand, bis 2σ dahinter ausgeblendet.
 */
private fun DrawScope.drawSoftEllipse(field: Field, color: Color) {
    val outerX = field.rx + 2 * SOFT
    val outerY = field.ry + 2 * SOFT
    val solid = ((field.rx - 2 * SOFT) / outerX).coerceAtLeast(0f)
    val center = Offset(field.cx, field.cy)
    withTransform({ scale(1f, outerY / outerX, pivot = center) }) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                solid to color,
                1f to color.copy(alpha = 0f),
                center = center,
                radius = outerX,
            ),
            radius = outerX,
            center = center,
        )
    }
}

/** Ein Farbfeld der Vorlage: Ellipse um ([cx], [cy]) mit den Halbachsen [rx], [ry]. */
private class Field(val vayu: Vayu, val cx: Float, val cy: Float, val rx: Float, val ry: Float)

/**
 * Beschriftung im Bild: Grundlinie bei ([x], [baseline]), [endAnchored] = Text endet bei [x];
 * [leader] die dünne Führungslinie zur Fläche.
 */
private class Label(
    val vayu: Vayu,
    val x: Float,
    val baseline: Float,
    val endAnchored: Boolean = false,
    val leader: Pair<Offset, Offset>? = null,
)

/** Pfade der Figur und der Strömungslinien, einmal gebaut (Einheiten der Vorlage). */
private class VayuGeometry {
    val viewSize = Size(VIEW_W, VIEW_H)

    /** Kopf und Körper als ein Pfad: Fläche, Zuschnitt der Farbfelder, Umriss und Aura. */
    val figure = Path().apply {
        addOval(Rect(center = Offset(176f, 44f), radius = 24f))
        moveTo(140f, 80f)
        quadraticTo(176f, 74f, 212f, 80f)
        quadraticTo(232f, 84f, 236f, 104f)
        lineTo(246f, 226f)
        quadraticTo(247f, 240f, 236f, 240f)
        quadraticTo(226f, 240f, 225f, 228f)
        lineTo(214f, 124f)
        lineTo(212f, 250f)
        lineTo(214f, 410f)
        quadraticTo(214f, 424f, 200f, 424f)
        quadraticTo(186f, 424f, 186f, 410f)
        lineTo(182f, 272f)
        quadraticTo(176f, 262f, 170f, 272f)
        lineTo(166f, 410f)
        quadraticTo(166f, 424f, 152f, 424f)
        quadraticTo(138f, 424f, 138f, 410f)
        lineTo(140f, 250f)
        lineTo(138f, 124f)
        lineTo(127f, 228f)
        quadraticTo(126f, 240f, 116f, 240f)
        quadraticTo(105f, 240f, 106f, 226f)
        lineTo(116f, 104f)
        quadraticTo(120f, 84f, 140f, 80f)
        close()
    }

    /** Strömungslinien je vāyu, jeweils in Flussrichtung gezeichnet. */
    val flows: List<Pair<Vayu, Path>> = listOf(
        // udāna: aufwärts durch Hals und Kopf.
        Vayu.UDANA to line(176f, 112f, 176f, 6f),
        // prāṇa: von beiden Seiten in die Brust.
        Vayu.PRANA to Path().apply {
            moveTo(96f, 128f)
            quadraticTo(136f, 116f, 168f, 136f)
        },
        Vayu.PRANA to Path().apply {
            moveTo(256f, 128f)
            quadraticTo(216f, 116f, 184f, 136f)
        },
        // samāna: kreisend um die Nabelmitte (Ellipse 16 × 10 um 176/197,5, im Uhrzeigersinn).
        Vayu.SAMANA to Path().apply {
            arcTo(Rect(160f, 187.5f, 192f, 207.5f), SAMANA_START, SAMANA_SWEEP, forceMoveTo = true)
        },
        // apāna: die Beine hinab.
        Vayu.APANA to line(158f, 232f, 152f, 440f),
        Vayu.APANA to line(194f, 232f, 200f, 440f),
        // vyāna: von der Mitte nach außen.
        Vayu.VYANA to line(150f, 150f, 80f, 110f),
        Vayu.VYANA to line(202f, 150f, 272f, 110f),
        Vayu.VYANA to line(150f, 300f, 84f, 340f),
        Vayu.VYANA to line(202f, 300f, 268f, 340f),
    )

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float) = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }
}

// ---- Maße der Vorlage (viewBox-Einheiten; skaliert mit dem Bild, daher keine dp-Tokens) ----

private const val VIEW_W = 352f
private const val VIEW_H = 450f

/** Breite des Bilds als Anteil der Spalte: ein Fünftel kleiner (Nutzer 2026-10-23). */
private const val FIGURE_WIDTH = 0.8f

/** Rand um die Beschriftungen, in dem ein Tipp sie noch trifft. */
private const val LABEL_SLACK = 8f

/** Figur samt Aura und Strömungslinien: Ein Tipp hier, aber in keinem Farbfeld, wählt vyāna. */
private val VYANA_AREA = Rect(76f, 0f, 276f, 444f)
private const val OUTLINE_WIDTH = 2.5f
private const val FLOW_WIDTH = 2f
private const val FLOW_WIDTH_SELECTED = 3f
private const val LEADER_WIDTH = 1f
private const val DASH_PERIOD = 12f
private val DASH = floatArrayOf(3f, 9f)

/** Weichzeichner der Farbfelder (σ 9 in der Vorlage). */
private const val SOFT = 9f

/** Aura: Breiten der Lagen (Strich 10, σ 7) und Deckkraft je Lage; zusammen etwa die Hälfte. */
private val AURA_WIDTHS = floatArrayOf(38f, 28f, 18f, 8f)
private const val AURA_LAYER = 0.16f

/** Bogen von samāna: von 192/198 bis 191/194, fast ein ganzer Umlauf. */
private const val SAMANA_START = 2.9f
private const val SAMANA_SWEEP = 336.6f

private val FIELDS = listOf(
    Field(Vayu.UDANA, 176f, 66f, 44f, 46f),
    Field(Vayu.PRANA, 176f, 136f, 62f, 40f),
    Field(Vayu.SAMANA, 176f, 198f, 40f, 26f),
    Field(Vayu.APANA, 176f, 320f, 56f, 120f),
)

private val LABELS = listOf(
    Label(Vayu.UDANA, 232f, 40f, leader = Offset(230f, 36f) to Offset(200f, 44f)),
    Label(Vayu.PRANA, 270f, 170f, leader = Offset(268f, 166f) to Offset(236f, 150f)),
    Label(Vayu.SAMANA, 82f, 202f, endAnchored = true, leader = Offset(86f, 198f) to Offset(150f, 198f)),
    Label(Vayu.APANA, 236f, 300f, leader = Offset(234f, 296f) to Offset(212f, 290f)),
    Label(Vayu.VYANA, 282f, 440f),
)
