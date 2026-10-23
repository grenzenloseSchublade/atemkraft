package app.atemkraft.ui.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.PhaseType
import app.atemkraft.ui.TestMatrix.Device
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.learn.LearnScreen
import app.atemkraft.ui.patterns.PatternEditorState
import app.atemkraft.ui.theme.DarkBackground
import app.atemkraft.ui.theme.DarkOnBackground
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.NeonMagenta
import app.atemkraft.ui.theme.SurfaceContainerLowest
import app.atemkraft.ui.theme.SynthCircleCenter
import app.atemkraft.ui.theme.SynthCircleEdge
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Bilder für Google Play und F-Droid (`fastlane/metadata/android/<sprache>/images/`, docs/STORE.md):
 * App-Icon 512 × 512 aus dem adaptiven Launcher-Icon, Feature-Grafik 1024 × 500 und sechs
 * Telefon-Screenshots 1080 × 1920 (9:16, ohne Alpha) in repräsentativen Zuständen. Durch dieselben
 * Gates wie die übrigen Screenshots ([ScreenshotHarness]).
 *
 * Läuft nur auf Wunsch; im normalen Testlauf (`scripts/check.sh`) schließt Gradle die Klasse aus:
 *
 *     scripts/build.sh testDebugUnitTest --tests '*StoreScreenshotTest' -PstoreScreenshots
 *
 * Gradle setzt dann `atemkraft.storeOut` auf `fastlane/metadata/android`; de-DE und en-US
 * bekommen dieselben Dateien.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// 384 dp wie das A54 bei 450 dpi: 1080 px breit; 683 dp hoch ergibt 1921 px, die letzte Zeile fällt weg.
@Config(qualifiers = "w384dp-h683dp-450dpi")
class StoreScreenshotTest : ScreenshotHarness(STORE, 1f) {

    companion object {
        private val STORE = Device("store", 384, 683)
        private const val SHOT_WIDTH = 1080
        private const val SHOT_HEIGHT = 1920
        private const val STATUS_BAR_DP = 24
        private val LOCALES = listOf("de-DE", "en-US")
    }

    private val out: File? = System.getProperty("atemkraft.storeOut")?.let(::File)

    private val app get() = RuntimeEnvironment.getApplication()

    @Before
    fun onlyOnRequest() = assumeTrue("Store-Bilder nur mit -PstoreScreenshots", out != null)

    /**
     * Sechs Zustände, die zeigen, was die App ausmacht: laufende Sitzung, Tab „Atmen“, Detailseite
     * mit Studienlage, Meditation mit Zeitring, Muster-Editor mit Atemkurve, Tab „Lernen“.
     */
    @Test
    fun phoneScreenshots() {
        val resonance = BuiltInExercises.all.first { it.id == "resonanz" }
        var inhales = 0
        val session = sessionAt(resonance, elapsedMs = 2_000) { it.type == PhaseType.INHALE && ++inhales == 12 }
        shot(1) { SessionShot(session) }
        // Anlass „Einschlafen“ gewählt, Liste bis zum Block „Wie fühlst du dich?“ gescrollt.
        shot(2, tab = Tab.ATMEN, prepare = {
            compose.onNode(hasScrollToKeyAction()).performScrollToKey("feeling")
            compose.onNodeWithText("Einschlafen").performClick()
        }) { BreatheTab() }
        shot(3) {
            WithStatusBar {
                ExerciseDetailScreen(
                    exercise = resonance,
                    requireSafetyConfirm = false,
                    savedIntervals = null,
                    onIntervalsChange = { _, _, _, _ -> },
                    onIntervalsReset = {},
                    onConfirmedSafety = {},
                    onStart = {},
                    onBack = {},
                    onOpenLore = {},
                    marked = true,
                    onMarkedChange = {},
                )
            }
        }
        shot(4, tab = Tab.MEDITATION) { MeditationTab(MeditationConfig(minutes = 10)) }
        shot(5, tab = Tab.EIGENE, tabBar = false) {
            Editor { PatternEditorState.forEdit(it, 5L, template("Viloma I"), 5, "Stufen am Abend").apply { select(1) } }
        }
        shot(6, tab = Tab.LERNEN) {
            LearnScreen(onOpenSettings = {}, onOpenAtemlehre = {}, onOpenPranayama = {}, onOpenPrograms = {}, onOpenGlossary = {})
        }
    }

    /**
     * Play-Icon aus dem echten adaptiven Icon (`mipmap/ic_launcher`): Hintergrund und Vordergrund
     * so skaliert, dass die sichtbaren 72 dp der 108-dp-Ebenen die 512 px füllen; die Ecken
     * rundet Play selbst.
     */
    @Test
    fun icon() {
        val icon = ResourcesCompat.getDrawable(app.resources, R.mipmap.ic_launcher, null) as AdaptiveIconDrawable
        val size = 512
        val layer = size * 108 / 72
        val inset = (layer - size) / 2
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        listOfNotNull(icon.background, icon.foreground).forEach { d ->
            d.setBounds(-inset, -inset, size + inset, size + inset)
            d.draw(canvas)
        }
        write(bitmap, "icon.png", alpha = true)
    }

    /** Feature-Grafik 1024 × 500 px (mdpi, also dp = px): Atemringe des Icons und der App-Name. */
    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphic() = snap("featureGraphic", heightDp = 500, widthDp = 1024, export = { write(it.asAndroidBitmap(), "featureGraphic.png") }) {
        FeatureGraphic()
    }

    /** Ein Screenshot in Gerätehöhe; [tab] wie in der App mit Status-, Tab- und Systemleiste. */
    private fun shot(n: Int, tab: Tab? = null, tabBar: Boolean = true, prepare: () -> Unit = {}, content: @Composable () -> Unit) = snap(
        "store_$n",
        heightDp = device.heightDp,
        tab = tab,
        tabBar = tabBar,
        prepare = prepare,
        export = { image ->
            val bitmap = image.asAndroidBitmap()
            assertTrue("Screenshot zu klein: ${bitmap.width} × ${bitmap.height}", bitmap.width >= SHOT_WIDTH && bitmap.height >= SHOT_HEIGHT)
            write(Bitmap.createBitmap(bitmap, 0, 0, SHOT_WIDTH, SHOT_HEIGHT), "phoneScreenshots/$n.png")
        },
        content = content,
    )

    /** Seiten der Tiefe liegen in der App unter der Statusleiste. */
    @Composable
    private fun WithStatusBar(content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Spacer(Modifier.height(STATUS_BAR_DP.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }

    @Composable
    private fun FeatureGraphic() {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SurfaceContainerLowest, DarkBackground, SynthCircleEdge))),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 96.dp), verticalAlignment = Alignment.CenterVertically) {
                BreathRings(Modifier.size(320.dp))
                Spacer(Modifier.width(64.dp))
                Column {
                    Text(
                        text = app.getString(R.string.app_name),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp, lineHeight = 96.sp),
                        fontWeight = FontWeight.Medium,
                        color = DarkOnBackground,
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(
                        Modifier
                            .width(140.dp)
                            .height(4.dp)
                            .background(Brush.horizontalGradient(listOf(NeonMagenta, NeonCyan))),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Atemübungen und Meditation",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 40.sp),
                        color = NeonCyan,
                    )
                }
            }
        }
    }

    /** Die Ringe des Launcher-Icons (`ic_launcher_foreground`: Radien 26, 17, 8 von 108) auf weichem Schein. */
    @Composable
    private fun BreathRings(modifier: Modifier = Modifier) {
        Canvas(modifier) {
            val unit = size.minDimension / 2f / 26f * 0.86f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(listOf(SynthCircleCenter.copy(alpha = 0.45f), Color.Transparent), center, size.minDimension / 2f),
                radius = size.minDimension / 2f,
                center = center,
            )
            drawCircle(Color(0xFF9D4EDD), radius = 26f * unit, center = center, style = Stroke(width = 2.5f * unit))
            drawCircle(Color(0xFFC77DFF), radius = 17f * unit, center = center, style = Stroke(width = 2.5f * unit))
            drawCircle(Color(0xFFF3E5FF), radius = 8f * unit, center = center)
        }
    }

    /** Schreibt [bitmap] als PNG in jede Sprache; ohne [alpha] als 24-Bit-RGB (Play: Screenshots und Feature-Grafik ohne Alpha). */
    private fun write(bitmap: Bitmap, path: String, alpha: Boolean = false) {
        System.setProperty("java.awt.headless", "true")
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
        val image = BufferedImage(w, h, if (alpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, w, h, pixels, 0, w)
        LOCALES.forEach { locale ->
            val file = File(checkNotNull(out), "$locale/images/$path")
            file.parentFile?.mkdirs()
            check(ImageIO.write(image, "png", file)) { "Kein PNG-Writer für $file" }
        }
    }
}
