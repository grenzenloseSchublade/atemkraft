package app.atemkraft.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R

/**
 * Die vier Tabs der App in Leisten-Reihenfolge – die eine Quelle für Startseiten ([TAB_ROOTS],
 * [isTabRoot]), Markierung ([tabOnStack]) und Leiste ([AppTabBar]). [route] ist die Startseite des
 * Tabs, [label] seine Beschriftung, [icon] sein Symbol (dekorativ: die Beschriftung benennt den Tab).
 */
enum class TopTab(val route: Any, @StringRes val label: Int, val icon: @Composable () -> Painter) {
    ATMEN(AtmenRoute, R.string.tab_breathe, { rememberVectorPainter(Icons.Filled.Home) }),
    EIGENE(EigeneRoute, R.string.tab_own, { painterResource(R.drawable.ic_bookmark_filled) }),
    MEDITATION(MeditationRoute, R.string.tab_meditation, { painterResource(R.drawable.ic_meditation) }),
    LERNEN(LernenRoute, R.string.tab_learn, { painterResource(R.drawable.ic_learn) }),
    ;

    companion object {
        /** Startziel des NavHost; liegt immer unten im Stapel. */
        val START = ATMEN
    }
}

/**
 * Die Tab-Leiste der App aus [TopTab]: [selected] markiert, ein Tipp meldet den Tab an [onTap]
 * (in der App `tapTab`). In [HideableTabBar] einsetzen.
 */
@Composable
fun AppTabBar(selected: TopTab, onTap: (TopTab) -> Unit, modifier: Modifier = Modifier) {
    AppNavigationBar(
        items = TopTab.entries.map { tab ->
            AppNavItem(
                label = stringResource(tab.label),
                selected = tab == selected,
                onClick = { onTap(tab) },
            ) { Icon(tab.icon(), contentDescription = null) } // dekorativ: das Label benennt den Tab
        },
        modifier = modifier,
    )
}
