package app.atemkraft.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.math.cbrt
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * FARBE-03 / FARBE-04: Kontrast (WCAG 2.x) und Farbabstand (ΔE76) der Theme-Tokens, gemessen
 * am realen Grund: Alpha wird vorher über die Hintergrund-Kette gemischt (Chip-Fläche über
 * Karte, Leiste mit Tonal-Elevation, Kreiszentrum). Reine Rechnung auf der JVM.
 *
 * Bekannte Verstöße stehen in `config/contrast-baseline.txt` (mit Backlog-ID). Der Test schlägt
 * fehl bei jedem neuen Verstoß und bei jedem Baseline-Eintrag, der nicht mehr verletzt ist –
 * dann gehört er aus der Baseline (und der Backlog-Punkt aus dem STYLEGUIDE) gestrichen.
 */
class ThemeContrastTest {

    /** Ein Prüfpaar: Vordergrund (ggf. mit Alpha) auf deckendem Grund, mit Mindestwert. */
    private data class Pair(val name: String, val fg: Color, val bg: Color, val min: Double)

    // Mindestwerte nach FARBE-03: Text < 24 sp (< 18,66 sp fett) 4,5:1, großer Text und
    // Zustandsgrafik 3:1.
    private val text = 4.5
    private val large = 3.0
    private val graphic = 3.0

    // Gründe, wie sie auf dem Bildschirm entstehen.
    private val background = DarkBackground

    /** M3-Card: containerColor = surfaceContainerHighest (Theme.kt). */
    private val card = DarkSurfaceVariant

    /** Dialog: surfaceContainerHigh. */
    private val dialog = SurfaceContainerHigh

    /**
     * Untere Leiste (NavigationBar, Mini-Bar): `DarkSurface` mit M3-Tonal-Elevation 3 dp, also
     * primary mit α = (4,5 · ln(3 + 1) + 2) / 100 darüber gemischt (≈ #321C48).
     */
    private val bar = NeonMagenta.copy(alpha = ((4.5 * ln(3.0 + 1.0) + 2.0) / 100.0).toFloat()) over DarkSurface

    /** Kritischer Punkt des Atemkreises: das Zentrum des radialen Verlaufs. */
    private val circleCenter = SynthCircleCenter

    // Alpha-Werte, die (noch) als Literal in Komponenten stehen; Quelle jeweils genannt.
    // Chips.kt: Fläche color.copy(alpha = 0.16f), InfoChip-Text onSurface 0.5f.
    private val chipFill = 0.16f
    private val infoChipText = 0.5f

    // HomeScreen.CharacterChip: Fläche color.copy(alpha = 0.14f).
    private val characterChipFill = 0.14f

    // AppNavigationBar: unselektierte Icons/Labels onSurface 0.55f.
    private val navUnselected = 0.55f

    // LogbookScreen.WeekRow: Punkt-Stufen und Wochentags-Buchstaben.
    private val weekLevels = listOf(0.12f to DarkOnSurface, 0.40f to NeonMagenta, 0.70f to NeonMagenta)
    private val weekdayText = 0.5f

    private fun contrastPairs(): List<Pair> = buildList {
        // Primärtext und Alpha-Stufen (§2.2).
        add(Pair("onBackground auf background", DarkOnBackground, background, text))
        add(Pair("onSurface auf Card", DarkOnSurface, card, text))
        add(Pair("onSurface SECONDARY auf background", DarkOnSurface.copy(alpha = SECONDARY), background, text))
        add(Pair("onSurface SECONDARY auf Card", DarkOnSurface.copy(alpha = SECONDARY), card, text))
        add(Pair("onSurface TERTIARY auf background", DarkOnSurface.copy(alpha = TERTIARY), background, text))
        add(Pair("onSurface TERTIARY auf Card", DarkOnSurface.copy(alpha = TERTIARY), card, text))
        add(Pair("onSurfaceVariant auf Dialog", DarkOnSurfaceVariant, dialog, text))
        add(Pair("onSurfaceVariant auf Leiste", DarkOnSurfaceVariant, bar, text))

        // Akzent- und Semantikfarben als Text.
        val accents = listOf(
            "NeonMagenta" to NeonMagenta,
            "NeonCyan" to NeonCyan,
            "WarnAmber" to WarnAmber,
            "EvidenceCaution" to EvidenceCaution,
            "FamilyVagal" to FamilyVagal,
            "FamilySympathetic" to FamilySympathetic,
            "FamilyBalance" to FamilyBalance,
            "FamilyFunctional" to FamilyFunctional,
        )
        accents.forEach { (n, c) ->
            add(Pair("$n auf background", c, background, text))
            add(Pair("$n auf Card", c, card, text))
        }
        add(Pair("EvidenceCaution auf Leiste", EvidenceCaution, bar, text))
        add(Pair("OnNeon auf NeonMagenta", OnNeon, NeonMagenta, text))

        // Chips: Text in Chipfarbe auf der eigenen, über den Grund gemischten Fläche – Karte im
        // Atmen-Tab, Screen-Grund im Detail-Kopf. Studienlage (drei Stufen) und Vorsicht.
        listOf(
            "EvidenceBest" to EvidenceBest,
            "EvidenceStudied" to EvidenceStudied,
            "EvidenceLittle" to EvidenceLittle,
            "EvidenceCaution" to EvidenceCaution,
        ).forEach { (n, c) ->
            add(Pair("Chip $n auf Card", c, c.copy(alpha = chipFill) over card, text))
            add(Pair("Chip $n auf background", c, c.copy(alpha = chipFill) over background, text))
        }
        val infoColor = DarkOnSurface.copy(alpha = infoChipText)
        add(Pair("InfoChip auf Card", infoColor, infoColor.copy(alpha = chipFill) over card, text))
        listOf("WarnAmber" to WarnAmber, "NeonCyan" to NeonCyan).forEach { (n, c) ->
            add(Pair("CharacterChip $n auf Card", c, c.copy(alpha = characterChipFill) over card, text))
        }

        // Navigationsleiste.
        add(Pair("Nav unselektiert auf Leiste", DarkOnSurface.copy(alpha = navUnselected), bar, text))
        add(Pair("Nav selektiert NeonMagenta auf Leiste", NeonMagenta, bar, text))

        // Logbuch-Wochenzeile: Wochentage sind Text, die Punkte Zustandsgrafik (FARBE-05).
        add(Pair("Wochentag auf Card", DarkOnSurface.copy(alpha = weekdayText), card, text))
        weekLevels.forEach { (a, c) ->
            add(Pair("WeekRow-Punkt α$a auf Card", c.copy(alpha = a), card, graphic))
        }
        add(Pair("Heute-Ring SynthTrack auf Card", SynthTrack, card, graphic))

        // Ausgewähltes Segment (SegmentedButton-Fläche) gegen die Karte.
        add(Pair("SecondaryContainer auf Card", SecondaryContainer, card, graphic))

        // Session: Kreiszentrum ist der kritische Punkt (FARBE-03).
        add(Pair("Phase SessionTextYellow auf Kreiszentrum", SessionTextYellow, circleCenter, large))
        add(Pair("Notiz SessionNoteAmber auf Kreiszentrum", SessionNoteAmber, circleCenter, text))
        add(Pair("Notiz SessionNoteAmber auf background", SessionNoteAmber, background, text))
        add(Pair("Session-Label SessionTextGlow auf SessionButtonCyan", SessionTextGlow, SessionButtonCyan, text))
        add(Pair("SessionButtonCyan auf background", SessionButtonCyan, background, text))
        add(Pair("SessionButtonPink auf background", SessionButtonPink, background, text))
    }

    /** FARBE-04: semantische Farben, die gemeinsam auf einem Screen stehen, ΔE76 ≥ 25. */
    private fun deltaEPairs(): List<Triple<String, Color, Color>> {
        // Atmen-Tab: Familien-Akzente, Evidenz-Chips (drei Stufen), Charakter-Chips, Marke.
        val home = listOf(
            "primary" to NeonMagenta,
            "EvidenceBest" to EvidenceBest,
            "EvidenceStudied" to EvidenceStudied,
            "EvidenceLittle" to EvidenceLittle,
            "EvidenceCaution" to EvidenceCaution,
            "WarnAmber" to WarnAmber,
            "FamilyVagal" to FamilyVagal,
            "FamilySympathetic" to FamilySympathetic,
            "FamilyBalance" to FamilyBalance,
            "FamilyFunctional" to FamilyFunctional,
        )
        val homePairs = home.flatMapIndexed { i, a -> home.drop(i + 1).map { b -> Triple("${a.first} ↔ ${b.first}", a.second, b.second) } }
        // Die drei Pinktöne (S-18): Marke, Vorsicht, „Beenden“.
        val pinks = listOf(
            Triple("primary ↔ SessionButtonPink", NeonMagenta, SessionButtonPink),
            Triple("EvidenceCaution ↔ SessionButtonPink", EvidenceCaution, SessionButtonPink),
        )
        return homePairs + pinks
    }

    @Test
    fun `Kontrast und Farbabstand halten die Mindestwerte oder stehen in der Baseline`() {
        val violations = mutableSetOf<String>()
        val report = StringBuilder()

        contrastPairs().forEach { p ->
            val ratio = contrast(p.fg over p.bg, p.bg)
            report.appendLine("kontrast | ${p.name} | %.2f (min %.1f)".format(ratio, p.min))
            if (ratio < p.min) violations += "kontrast | ${p.name}"
        }
        deltaEPairs().forEach { (name, a, b) ->
            val d = deltaE76(a, b)
            report.appendLine("delta-e | $name | %.1f (min 25)".format(d))
            if (d < 25.0) violations += "delta-e | $name"
        }
        println(report)

        val baseline = readBaseline()
        val new = violations - baseline.keys
        val stale = baseline.keys - violations
        if (new.isNotEmpty() || stale.isNotEmpty()) {
            fail(
                buildString {
                    if (new.isNotEmpty()) appendLine("Neue Verstöße (FARBE-03/-04):\n" + new.sorted().joinToString("\n"))
                    if (stale.isNotEmpty()) {
                        appendLine("Baseline senken – nicht mehr verletzt:\n" + stale.sorted().joinToString("\n") { "$it | ${baseline[it]}" })
                    }
                    appendLine("\nMesswerte:\n$report")
                },
            )
        }
    }

    /** `Testfall | Token-Paar | S-nn` → Schlüssel `Testfall | Token-Paar`, Wert S-ID. */
    private fun readBaseline(): Map<String, String> {
        // Unit-Tests laufen mit app/ als Arbeitsverzeichnis.
        val file = listOf(File("../config/contrast-baseline.txt"), File("config/contrast-baseline.txt")).first { it.isFile }
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line ->
                val parts = line.split("|").map { it.trim() }
                require(parts.size == 3 && parts[2].matches(Regex("S-\\d+"))) { "Baseline-Zeile ungültig: $line" }
                "${parts[0]} | ${parts[1]}" to parts[2]
            }
    }
}

/** Alpha-Mischung im sRGB-Raum (wie Android/Compose zeichnet) über einen deckenden Grund. */
private infix fun Color.over(bg: Color): Color {
    val a = alpha
    return Color(
        red = red * a + bg.red * (1 - a),
        green = green * a + bg.green * (1 - a),
        blue = blue * a + bg.blue * (1 - a),
        alpha = 1f,
    )
}

private fun linear(c: Float): Double {
    val v = c.toDouble()
    return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
}

/** WCAG 2.x relative Luminanz. */
private fun luminance(c: Color): Double = 0.2126 * linear(c.red) + 0.7152 * linear(c.green) + 0.0722 * linear(c.blue)

private fun contrast(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

/** CIE76-Farbabstand in CIELAB (D65). */
private fun deltaE76(a: Color, b: Color): Double {
    val la = lab(a)
    val lb = lab(b)
    return sqrt((la[0] - lb[0]).pow(2) + (la[1] - lb[1]).pow(2) + (la[2] - lb[2]).pow(2))
}

private fun lab(c: Color): DoubleArray {
    val r = linear(c.red)
    val g = linear(c.green)
    val b = linear(c.blue)
    val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    val y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 1.0
    val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
    fun f(t: Double) = if (t > 216.0 / 24389.0) cbrt(t) else (24389.0 / 27.0 * t + 16.0) / 116.0
    val fx = f(x)
    val fy = f(y)
    val fz = f(z)
    return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
}
