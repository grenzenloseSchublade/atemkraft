package app.atemkraft.ui.patterns

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.rememberTextMeasurer
import app.atemkraft.domain.FreePattern
import app.atemkraft.ui.components.TextWidths
import app.atemkraft.ui.components.rememberTextEnvironment
import app.atemkraft.ui.patterns.curve.rememberCurveMetrics
import app.atemkraft.ui.theme.Dimens
import kotlinx.coroutines.delay

/**
 * Vorladen des Muster-Editors (Eigentümer 2026-10-30): Solange eine Seite zu sehen ist, von der aus
 * man den Editor öffnet – das Blatt „Muster des Tages“ ([pattern] = das Tagesmuster) und der Tab
 * „Eigene“ (die Vorgabe 4·6 von „+“) –, misst es einmal, kurz nachdem die Seite steht, was der
 * erste Frame des Editors sonst selbst messen müsste: die Reserven der Werte über der Kurve
 * ([idleReserveTexts], 15 von 20 Reserve-Texten des Editors) und Zahlen und Beschriftungen der
 * Kurve ([app.atemkraft.ui.patterns.curve.CurveLayout.preload]). Beides liegt danach in den
 * prozessweiten Zwischenspeichern (`TextWidths`, gemeinsamer Messer der Kurve) und gilt, solange
 * Schriftgröße, Sprache und Fenster gleich bleiben; liegt es schon dort, kostet es nichts.
 *
 * Zeigt nichts, legt keinen Zustand an und läuft genau einmal je Muster und Schrift-Umgebung
 * (PERF-01); verlässt man die Seite vorher, entfällt es, und der Editor misst wie bisher selbst.
 * Die Breite der Kurve wird wie im Editor gerechnet (Inhaltsbreite höchstens `ContentMaxWidth`,
 * abzüglich `ScreenPadding` je Seite); stimmt sie nicht (quer mit Aussparung), fehlen nur die Zahlen
 * der Zeitleiste im Speicher – falsch wird nichts.
 *
 * Den Editor-Zustand legt es nicht vorab an: Er entsteht in 0,1–0,3 ms (JVM, warm) und lebt in
 * `rememberSaveable` des Ziels; das erste Anlegen im Prozess kostet vor allem das Laden der
 * Klassen, und das übernimmt das Baseline-Profil (`app/src/main/baseline-prof.txt`).
 */
@Composable
internal fun PatternEditorPreload(pattern: FreePattern) {
    val style = infoLineStyle()
    val reserves = idleReserveTexts(pattern.totalSeconds)
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val environment = rememberTextEnvironment()
    val metrics = rememberCurveMetrics()
    val density = LocalDensity.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val curveWidth = with(density) { (minOf(windowWidth.toDp(), Dimens.ContentMaxWidth) - Dimens.ScreenPadding * 2).toPx() }
    LaunchedEffect(pattern, reserves, environment, metrics, curveWidth) {
        // Erst wenn das Blatt hereingeglitten bzw. der Tab eingeblendet ist: nicht in dessen Frames.
        delay(PRELOAD_DELAY_MS)
        reserves.forEach { TextWidths.widest(measurer, it, style, environment) }
        metrics.layout(curveWidth, pattern.axisSeconds().toFloat()).preload(pattern)
    }
}

/**
 * Wartezeit vor dem Vorladen: länger als das Hereingleiten des Blatts (M3, Feder) und das
 * Einblenden eines Tabs (`Motion.NavTab`), kürzer als man braucht, um „Selbst gestalten“ zu lesen
 * und anzutippen.
 */
private const val PRELOAD_DELAY_MS = 500L
