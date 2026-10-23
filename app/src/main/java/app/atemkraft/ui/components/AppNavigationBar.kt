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

/** Pfeil des [TabBarToggle] nach unten gedreht (Grad). */
private const val HALF_TURN = 180f

/**
 * Platz der Tab-Leiste für eine Seite, die sie ausblenden darf: der Muster-Editor bei Platzmangel
 * (MUSTER-10, Nutzer 2026-10-22), die Detailseite beim Scrollen und über [TabBarToggle] (Nutzer
 * 2026-10-23). Der Rahmen ([HideableTabBar]) misst [heightPx] und setzt [hidden]; die Seite meldet
 * über [request], ob sie den Platz braucht. Nur die Seiten, die das dürfen, bekommen ihn als Parameter.
 */
@Stable
class TabBarSpace {
    /** So viel Höhe (px) nimmt die Leiste samt Trennlinie dem Inhalt, ohne Systemnavigation; 0, solange nie gemessen. */
    var heightPx by mutableIntStateOf(0)
        internal set

    /** Wunsch der Seite: Leiste ausblenden ([request]). */
    var requested by mutableStateOf(false)
        private set

    /** True, solange die Leiste ausgeblendet ist (setzt [HideableTabBar]). */
    var hidden by mutableStateOf(false)
        internal set

    /** Die Seite braucht den Platz der Leiste ([hide]) bzw. nicht. */
    fun request(hide: Boolean) {
        requested = hide
    }
}

/**
 * Trennlinie und Tab-Leiste [bar] (in der App [AppNavigationBar]). Ausgeblendet, wenn [allowHide]
 * (die Seite, die das darf, liegt oben) und sie es über [space] wünscht – dann bleibt nur der Platz
 * der Systemnavigation [systemInsets], die Leiste der Seite sitzt darüber. Verlässt man die Seite
 * ([allowHide] false), ist die Leiste sofort wieder da.
 */
@Composable
fun HideableTabBar(
    space: TabBarSpace,
    allowHide: Boolean,
    modifier: Modifier = Modifier,
    systemInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    bar: @Composable () -> Unit,
) {
    val hide = allowHide && space.requested
    SideEffect { space.hidden = hide }
    val inset by rememberUpdatedState(systemInsets.getBottom(LocalDensity.current))
    // Die Höhe gleitet (Default-Spring, MOTION-04): Die Startleiste darüber rutscht sanft hinab
    // bzw. hinauf, statt um die ganze Leiste zu springen.
    Box(modifier.animateContentSize()) {
        if (hide) {
            Spacer(Modifier.windowInsetsBottomHeight(systemInsets))
        } else {
            Column(Modifier.onSizeChanged { space.heightPx = it.height - inset }) {
                HorizontalDivider(thickness = Dimens.BorderThin, color = NeonMagenta.copy(alpha = TAB_DIVIDER_ALPHA))
                bar()
            }
        }
    }
}

/**
 * Kleiner Pfeil am Ende der Startleiste, der die Tab-Leiste ein- und ausblendet ([space]): nach oben,
 * solange sie weg ist („Tab-Leiste zeigen“), nach unten, solange sie da ist. Statt hochzuscrollen,
 * bis die Leiste vielleicht wiederkommt, holt ein Tipp sie sicher zurück (Nutzer 2026-10-23).
 * [onToggle] meldet der Seite die Wahl von Hand – danach blendet Scrollen nicht mehr aus.
 */
@Composable
fun TabBarToggle(space: TabBarSpace, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val turn by animateFloatAsState(if (space.requested) 0f else HALF_TURN, label = "tabBarToggle")
    AppIconButton(
        onClick = {
            space.request(!space.requested)
            onToggle()
        },
        modifier = modifier,
    ) {
        Icon(
            Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(if (space.requested) R.string.cd_tab_bar_show else R.string.cd_tab_bar_hide),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            modifier = Modifier.rotate(turn),
        )
    }
}
