package app.atemkraft.ui.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import app.atemkraft.R
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.TabBarContainer
import kotlin.math.roundToInt

/** Ein Tab der unteren Navigationsleiste. */
class AppNavItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
)

/**
 * Untere Navigationsleiste der Haupt-Tabs. Beschriftungen brechen nie im Wort (LAYOUT-03) und
 * teilen sich eine Größe. Reicht die Tab-Breite bei großer Schrift nicht, wird die Beschriftung
 * höchstens bis [NAV_LABEL_MIN_SCALE] verkleinert; passt sie auch dann nicht, zeigt die Leiste nur
 * die Icons und trägt den Namen als `contentDescription` des Tabs (Nutzer: lieber Icon-only als
 * geschrumpfte Schrift) – wer die Systemschrift vergrößert, bekommt nie Text in Normalgröße
 * (WCAG 1.4.4).
 *
 * Auswahl: tonale Pille (`secondaryContainer`) mit Icon und Beschriftung in `primary` – keine
 * gefüllte Neon-Fläche, die gehört dem Start-Button (PRIN-02). Das Gewicht bleibt bei Auswahl gleich
 * (TYPO-01): Ein breiteres SemiBold verlangte im Moment des Wechsels eine kleinere gemeinsame
 * Größe, alle vier Beschriftungen sprangen.
 */
@Composable
fun AppNavigationBar(items: List<AppNavItem>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = scheme.primary,
        selectedTextColor = scheme.primary,
        indicatorColor = scheme.secondaryContainer,
        unselectedIconColor = scheme.onSurface.copy(alpha = TERTIARY),
        unselectedTextColor = scheme.onSurface.copy(alpha = TERTIARY),
    )
    // Eine Größe für alle Beschriftungen, damit die Leiste ruhig wirkt; neu gemessen, wenn sich
    // Namen, Schrift oder Fensterbreite ändern.
    val fontScale = LocalDensity.current.fontScale
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val keys = arrayOf(items.map { it.label }, fontScale, windowWidth)
    var labelScale by remember(*keys) { mutableFloatStateOf(1f) }
    var iconOnly by remember(*keys) { mutableStateOf(false) }
    NavigationBar(modifier = modifier, containerColor = TabBarContainer) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = item.icon,
                // Ohne Beschriftung benennt die Beschreibung den Tab (das Icon ist dekorativ).
                modifier = if (iconOnly) Modifier.semantics { contentDescription = item.label } else Modifier,
                label = if (iconOnly) {
                    null
                } else {
                    {
                        WholeWordText(
                            text = item.label,
                            minScale = NAV_LABEL_MIN_SCALE,
                            sharedScale = labelScale,
                            onSharedScaleTooBig = { labelScale = minOf(labelScale, it) },
                            onDoesNotFit = { iconOnly = true },
                        )
                    }
                },
                colors = colors,
            )
        }
    }
}

/** Deckkraft der Trennlinie über der Tab-Leiste. */
private const val TAB_DIVIDER_ALPHA = 0.22f

/**
 * Kleinste Beschriftung der Tabs (Anteil der Rolle). Darunter Icon-only statt kleinerer Schrift
 * (LAYOUT-03): Mit 0,5 standen die Namen bei Schrift 2,0 wieder in Normalgröße.
 */
private const val NAV_LABEL_MIN_SCALE = 0.85f

/**
 * Trennlinie und Tab-Leiste [bar] (in der App [AppNavigationBar]), solange [visible]; sonst bleibt
 * nur der Platz der Systemnavigation [systemInsets]. Die Höhe gleitet dabei (Default-Spring,
 * MOTION-04), sodass eine Startleiste darüber sanft hinab- bzw. hinaufrutscht, statt um die ganze
 * Leiste zu springen.
 */
@Composable
fun HideableTabBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    systemInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    bar: @Composable () -> Unit,
) {
    Box(modifier.animateContentSize()) {
        if (visible) {
            Column {
                HorizontalDivider(thickness = Dimens.BorderThin, color = MaterialTheme.colorScheme.primary.copy(alpha = TAB_DIVIDER_ALPHA))
                bar()
            }
        } else {
            Spacer(Modifier.windowInsetsBottomHeight(systemInsets))
        }
    }
}

/**
 * Knopf, der die Tab-Leiste in der Tiefe auf- und zuklappt (Nutzer 2026-10-23, Variante B „Lasche“):
 * ein kleiner Reiter ([Sizes.TabBarHandleWidth] × [Sizes.TabBarHandleHeight], oben abgerundet), der
 * aus der Oberkante der untersten Leiste wächst – in deren Farbe, damit er sichtbar zu ihr gehört:
 * die Startleiste ([TabBarState.anchoredToPinnedBar], `surface` mit ihrer tonalen Erhöhung), sonst
 * die Tab-Leiste (`DarkSurface`, auch eingeklappt: der Reiter der verborgenen Leiste). Ohne eigene
 * Zeile, eingeklappt gewinnt der Inhalt die ganze Leiste. Pfeil nach oben, solange sie zu ist
 * („Tab-Leiste zeigen“), nach unten, solange sie offen ist („Tab-Leiste ausblenden“). Die
 * Tippfläche ist so breit wie der Reiter und [Dimens.MinTouchTarget] hoch (A11Y-05). Sie liegt zu drei
 * Vierteln über der Kante und ragt nur [TOUCH_BELOW_EDGE] in die Leiste darunter – das ist das
 * Polster der Startleiste, der Start-Button selbst bleibt frei (vorher die halbe Tippfläche, sie
 * deckte dessen obere 12 dp ab).
 */
@Composable
fun TabBarToggle(state: TabBarState, modifier: Modifier = Modifier) {
    val top = state.anchorTop
    if (top.isNaN()) return
    val turn by animateFloatAsState(if (state.collapsed) 0f else HALF_TURN, label = "tabBarToggle")
    // Abstand von der Oberkante der Tippfläche bis zur Kante der Leiste.
    val above = Dimens.MinTouchTarget - TOUCH_BELOW_EDGE
    val abovePx = with(LocalDensity.current) { above.toPx() }
    val fill = if (state.anchoredToPinnedBar) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(Dimens.PinnedBarElevation)
    } else {
        TabBarContainer
    }
    Box(
        modifier = modifier
            .offset { IntOffset(0, (top - abovePx).roundToInt()) }
            .size(width = Sizes.TabBarHandleWidth, height = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = state::toggle),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Der Reiter endet genau auf der Oberkante der Leiste.
        Box(
            modifier = Modifier
                .padding(top = above - Sizes.TabBarHandleHeight)
                .size(width = Sizes.TabBarHandleWidth, height = Sizes.TabBarHandleHeight)
                .background(fill, Sizes.TabBarHandleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(if (state.collapsed) R.string.cd_tab_bar_show else R.string.cd_tab_bar_hide),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                modifier = Modifier.size(Sizes.IconSmall).rotate(turn),
            )
        }
    }
}

/**
 * Teil der Tippfläche des [TabBarToggle] unter der Kante: so viel, wie die Startleiste über ihrem
 * Button polstert (`PinnedActionBar`, `ListGap`), ein Viertel der Tippfläche.
 */
private val TOUCH_BELOW_EDGE = Dimens.ListGap

/** Pfeil des [TabBarToggle] nach unten gedreht (Grad). */
private const val HALF_TURN = 180f
