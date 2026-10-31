package app.atemkraft.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/**
 * FARBE-03 (A-21): Die Kontrastwerte der Tabellen in STYLEGUIDE §2.1 („vs BG“, „vs Card“) und
 * §2.2 („min. Kontrast BG / Card“) werden aus den Tokens nachgerechnet (WCAG 2.x, Alpha vorher
 * mit `compositeOver` über den Grund gemischt, in 8 bit je Kanal wie auf dem Bildschirm). Gemeldet wird jeder Wert unter seiner Schwelle (Text 4,5, großer Text
 * und Grafik 3; Flächen und Dekoratives ohne) und jede Abweichung über 0,05 zur Tabelle – dann
 * stimmt entweder das Token oder die Tabelle nicht mehr. Reine Rechnung auf der JVM.
 */
class ThemeContrastTest {

    /** Ein Wert der Tabelle: [fg] (ggf. mit Alpha) auf deckendem [bg], Mindestwert [min] oder keiner. */
    private data class Probe(val fg: Color, val bg: Color, val min: Double?)

    /** Eine Tabellenzeile, gefunden über ihre erste Zelle [cell]; je Spalte die Werte in Reihenfolge. */
    private data class Row(val cell: String, val vsBg: List<Probe>, val vsCard: List<Probe> = emptyList())

    private val background = DarkBackground
    private val card = DarkSurfaceVariant

    private fun text(fg: Color, bg: Color) = Probe(fg, bg, TEXT)
    private fun graphic(fg: Color, bg: Color) = Probe(fg, bg, GRAPHIC)
    private fun surface(fg: Color, bg: Color) = Probe(fg, bg, null)

    /** Farbe als Text auf Grund und Karte. */
    private fun textRow(cell: String, colors: List<Color>) = Row(cell, colors.map { text(it, background) }, colors.map { text(it, card) })

    /** §2.1: Token-Farbe als Vordergrund gegen `background` und Card. */
    private val tokenRows = listOf(
        textRow("`NeonMagenta`", listOf(NeonMagenta)),
        textRow("`NeonCyan`", listOf(NeonCyan)),
        textRow("`NeonYellow`", listOf(NeonYellow)),
        Row("`OnNeon` / `OnCyan`", listOf(text(OnNeon, NeonMagenta), text(OnNeon, EvidenceCaution))),
        Row("`DarkSurfaceVariant`", listOf(surface(DarkSurfaceVariant, background))),
        Row("`SecondaryContainer`", listOf(surface(SecondaryContainer, background)), listOf(surface(SecondaryContainer, card))),
        Row("`TonalDeep`", listOf(surface(TonalDeep, background)), listOf(surface(TonalDeep, card))),
        // Die Auswahl trägt der Abstand zum ungewählten Segment (≥ 3, Bedeutung-Spalte), nicht der zum Grund.
        Row("`SegmentSelected`", listOf(surface(SegmentSelected, background)), listOf(surface(SegmentSelected, card))),
        Row("`OutlineVariantIndigo`", listOf(surface(OutlineVariantIndigo, background)), listOf(surface(OutlineVariantIndigo, card))),
        Row("`OutlineIndigo`", listOf(graphic(OutlineIndigo, background)), listOf(graphic(OutlineIndigo, card))),
        textRow("`DarkOnBackground/OnSurface`", listOf(DarkOnBackground)),
        textRow("`DarkOnSurfaceVariant`", listOf(DarkOnSurfaceVariant)),
        textRow("`FamilyVagal` / `Sympathetic` / `Balance` / `Functional`", listOf(FamilyVagal, FamilySympathetic, FamilyBalance, FamilyFunctional)),
        textRow("`EvidenceBest`", listOf(EvidenceBest)),
        textRow("`TodayGold`", listOf(TodayGold)),
        textRow("`EvidenceStudied`", listOf(EvidenceStudied)),
        textRow("`EvidenceLittle`", listOf(EvidenceLittle)),
        textRow("`EvidenceCaution`", listOf(EvidenceCaution)),
        textRow("`WarnAmber`", listOf(WarnAmber)),
        textRow("`BreathExhale`", listOf(BreathExhale)),
        textRow("`HeadingRing`", HeadingRing),
        textRow("`VayuPrana` / `Apana` / `Samana` / `Udana` / `Vyana`", listOf(VayuPrana, VayuApana, VayuSamana, VayuUdana, VayuVyana)),
        Row(
            "`SynthCircleCenter` / `Edge`",
            listOf(graphic(SynthCircleCenter, background), surface(SynthCircleEdge.copy(alpha = ALPHA_CIRCLE_EDGE), background)),
        ),
        Row(
            "`SessionTextYellow` / `SessionNoteAmber`",
            listOf(text(SessionTextYellow, background), text(SessionNoteAmber.copy(alpha = SECONDARY), background)),
        ),
        Row("`SessionButtonCyan`", listOf(text(SessionButtonCyan, background))),
    )

    /** §2.2: Textstufen auf Grund und Karte; „Flächen“ ist die leiseste Stufe der Logbuch-Wochenzeile. */
    private val alphaRows = listOf(
        textRow("Primär", listOf(DarkOnSurface)),
        textRow("Fließtext", listOf(DarkOnSurface.copy(alpha = BODY))),
        textRow("Sekundär", listOf(DarkOnSurface.copy(alpha = SECONDARY))),
        textRow("Tertiär", listOf(DarkOnSurface.copy(alpha = TERTIARY))),
        Row("Flächen", listOf(graphic(NeonMagenta.copy(alpha = ALPHA_HEAT_LOW), background)), listOf(graphic(NeonMagenta.copy(alpha = ALPHA_HEAT_LOW), card))),
    )

    @Test
    fun `Kontrast-Tabelle im STYLEGUIDE stimmt mit den Tokens`() {
        val guide = repoFile("docs/STYLEGUIDE.md").readText()
        val tokens = guide.substringAfter("### 2.1 ").substringBefore("### 2.2 ")
        val alphas = guide.substringAfter("### 2.2 ").substringBefore("### 2.3 ")
        val problems = tokenRows.flatMap { row ->
            val cells = cellsOf(tokens, row.cell) ?: return@flatMap listOf("§2.1 ${row.cell}: Zeile fehlt")
            compare("§2.1 ${row.cell} vs BG", row.vsBg, numbers(cells[cells.size - 2])) +
                compare("§2.1 ${row.cell} vs Card", row.vsCard, numbers(cells.last()))
        } + alphaRows.flatMap { row ->
            val cells = cellsOf(alphas, row.cell) ?: return@flatMap listOf("§2.2 ${row.cell}: Zeile fehlt")
            // „BG / Card“ in einer Zelle; bei „Flächen“ steht davor, für welche Farbe („`primary` 0,70: …“).
            val values = numbers(cells[3].substringAfter(": "))
            val half = values.size / 2
            compare("§2.2 ${row.cell} BG", row.vsBg, values.take(half)) +
                compare("§2.2 ${row.cell} Card", row.vsCard, values.drop(half))
        }
        if (problems.isNotEmpty()) fail("ThemeContrastTest (FARBE-03):\n" + problems.joinToString("\n") { "  $it" })
    }

    private fun compare(where: String, probes: List<Probe>, table: List<Double>): List<String> {
        if (probes.size != table.size) return listOf("$where: ${table.size} Werte in der Tabelle, ${probes.size} erwartet")
        return probes.zip(table).flatMapIndexed { i, (probe, documented) ->
            val actual = contrast(probe.fg.compositeOver(probe.bg), probe.bg)
            val label = if (probes.size > 1) "$where [${i + 1}]" else where
            listOfNotNull(
                if (abs(actual - documented) > TOLERANCE) "$label: gerechnet ${actual.de()}, Tabelle ${documented.de()}" else null,
                if (probe.min != null && actual < probe.min) "$label: ${actual.de()} unter ${probe.min.de()}" else null,
            )
        }
    }

    private companion object {
        const val TEXT = 4.5
        const val GRAPHIC = 3.0
        const val TOLERANCE = 0.05

        /** Zellen der Tabellenzeile, deren erste Zelle [first] ist; `null`, wenn es sie nicht gibt. */
        fun cellsOf(section: String, first: String): List<String>? = section.lineSequence()
            .firstOrNull { it.startsWith("| $first |") }
            ?.trim()?.removePrefix("|")?.removeSuffix("|")?.split(" | ")?.map { it.trim() }

        /** Zahlen einer Zelle mit Dezimalkomma; Klammerzusätze („(Leiste 4,78)“) und „–“ zählen nicht. */
        fun numbers(cell: String): List<Double> = Regex("""\d+,\d+""")
            .findAll(cell.replace(Regex("""\([^)]*\)"""), ""))
            .map { it.value.replace(',', '.').toDouble() }
            .toList()

        /** WCAG 2.x: (L1 + 0,05) / (L2 + 0,05) der relativen Leuchtdichten. */
        fun contrast(a: Color, b: Color): Double {
            val la = luminance(a)
            val lb = luminance(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }

        fun luminance(c: Color): Double {
            fun channel(v: Float): Double = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
        }

        fun Double.de(): String = "%.2f".format(Locale.ROOT, this).replace('.', ',')

        /** Unit-Tests laufen im Modulverzeichnis `app/`; `docs/` liegt im Repo-Root. */
        fun repoFile(path: String): File = listOf(File("../$path"), File(path)).firstOrNull { it.exists() } ?: File("../$path")
    }
}
