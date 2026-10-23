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
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import app.atemkraft.R
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.NeonMagenta
import app.atemkraft.ui.theme.OnNeon
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
 * Untere Navigationsleiste der Haupt-Tabs. Beschriftungen brechen nie im Wort (LAYOUT-03):
 * Reicht die Tab-Breite bei großer Schrift nicht, wird die Beschriftung verkleinert
 * ([WholeWordText], bis 40 %, damit fünf Tabs bei Schrift 2,0 passen).
 */
@Composable
fun AppNavigationBar(items: List<AppNavItem>, modifier: Modifier = Modifier) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = OnNeon,
        selectedTextColor = NeonMagenta,
        indicatorColor = NeonMagenta,
        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY),
        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TERTIARY),
    )
    // Eine Größe für alle Beschriftungen, damit die Leiste ruhig wirkt.
    val config = LocalConfiguration.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    var labelScale by remember(items.map { it.label }, config.fontScale, windowWidth) { mutableFloatStateOf(1f) }
    NavigationBar(modifier = modifier, containerColor = TabBarContainer) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = item.icon,
                label = {
                    WholeWordText(
                        text = item.label,
                        style = LocalTextStyle.current.copy(
                            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        // Ein Fünftel der Breite ist bei fontScale 2,0 sehr knapp: „Meditation“
                        // braucht dann gut 40 % (360 dp). Das Icon bleibt groß und trägt die Erkennung.
                        minScale = NAV_LABEL_MIN_SCALE,
                        sharedScale = labelScale,
                        onSharedScaleTooBig = { labelScale = minOf(labelScale, it) },
                    )
                },
                colors = colors,
            )
        }
    }
}

/** Deckkraft der Trennlinie über der Tab-Leiste. */
private const val TAB_DIVIDER_ALPHA = 0.22f

/** Kleinste Beschriftung der Tabs (Anteil der Rolle), damit fünf Namen bei Schrift 2,0 ganz bleiben. */
private const val NAV_LABEL_MIN_SCALE = 0.5f

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
                HorizontalDivider(thickness = Dimens.BorderThin, color = NeonMagenta.copy(alpha = TAB_DIVIDER_ALPHA))
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
 * Tippfläche ([Dimens.MinTouchTarget] im Quadrat, A11Y-05) liegt halb über dem Inhalt, halb über der
 * Leiste; sie darf in den großen Start-Button ragen (Nutzer 2026-10-23).
 */
@Composable
fun TabBarToggle(state: TabBarState, modifier: Modifier = Modifier) {
    val top = state.anchorTop
    if (top.isNaN()) return
    val turn by animateFloatAsState(if (state.collapsed) 0f else HALF_TURN, label = "tabBarToggle")
    val touch = with(LocalDensity.current) { Dimens.MinTouchTarget.toPx() }
    val fill = if (state.anchoredToPinnedBar) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(Dimens.PinnedBarElevation)
    } else {
        TabBarContainer
    }
    Box(
        modifier = modifier
            .offset { IntOffset(0, (top - touch / 2f).roundToInt()) }
            .size(Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = state::toggle),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Der Reiter endet genau auf der Oberkante der Leiste (Mitte der Tippfläche).
        Box(
            modifier = Modifier
                .padding(top = Dimens.MinTouchTarget / 2 - Sizes.TabBarHandleHeight)
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

/** Pfeil des [TabBarToggle] nach unten gedreht (Grad). */
private const val HALF_TURN = 180f
