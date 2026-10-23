package app.atemkraft.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsBottomHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.atemkraft.R
import app.atemkraft.ui.theme.DarkSurface
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.NeonMagenta
import app.atemkraft.ui.theme.OnNeon
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.TERTIARY

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
 * ([WholeWordText], bis 50 %).
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
    NavigationBar(modifier = modifier, containerColor = DarkSurface) {
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
                        // Ein Viertel der Breite ist bei fontScale 2,0 sehr knapp: „Situationen“
                        // braucht dann etwa 60 %. Das Icon bleibt groß und trägt die Erkennung.
                        minScale = 0.5f,
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

/**
 * Trennlinie und Tab-Leiste [bar] (in der App [AppNavigationBar]), nur auf den vier Tabs
 * ([visible], `isTabRoot`). In der Tiefe bleibt nur der Platz der Systemnavigation [systemInsets];
 * die Höhe gleitet dabei (Default-Spring, MOTION-04), sodass eine Startleiste darüber sanft
 * hinab- bzw. hinaufrutscht, statt um die ganze Leiste zu springen.
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
