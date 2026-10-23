package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import app.atemkraft.ui.navigation.LocalTabBarState
import app.atemkraft.ui.navigation.TabBarState
import app.atemkraft.ui.theme.Dimens

/**
 * Gepinnte Start-Leiste unter dem scrollenden Inhalt (Detailseite, Meditations-Tab, Muster-Editor):
 * Fläche `surface` mit tonaler Erhöhung [Dimens.PinnedBarElevation], Rand `ScreenPadding` seitlich und
 * `ListGap` oben und unten – überall gleich hoch. Das knappe Polster ist Absicht: Im
 * Meditations-Tab ginge jedes dp dem Zeitring ab (LAYOUT-08). Inhalt ist der `StartSplitButton`.
 *
 * In der Tiefe meldet sie ihre Oberkante beim [TabBarState] an ([LocalTabBarState]): Dort sitzt
 * dann der Knopf, der die Tab-Leiste auf- und zuklappt ([TabBarToggle]).
 */
@Composable
fun PinnedActionBar(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val tabBar = LocalTabBarState.current
    val owner = remember { Any() }
    if (tabBar != null) DisposableEffect(tabBar) { onDispose { tabBar.releasePinnedBar(owner) } }
    Surface(
        modifier = modifier.then(
            if (tabBar != null) Modifier.onGloballyPositioned { tabBar.anchorPinnedBar(owner, it.positionInRoot().y) } else Modifier,
        ),
        tonalElevation = Dimens.PinnedBarElevation,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ListGap),
            content = content,
        )
    }
}
