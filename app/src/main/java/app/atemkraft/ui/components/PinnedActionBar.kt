package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.atemkraft.ui.theme.Dimens

/**
 * Gepinnte Start-Leiste unter dem scrollenden Inhalt (Detailseite, Meditations-Tab): Fläche
 * `surface` mit tonaler Erhöhung [Dimens.PinnedBarElevation], Rand `ScreenPadding` seitlich und
 * `ListGap` oben und unten – überall gleich hoch. Das knappe Polster ist Absicht: Im
 * Meditations-Tab ginge jedes dp dem Zeitring ab (LAYOUT-08). Inhalt ist der `StartSplitButton`.
 */
@Composable
fun PinnedActionBar(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(modifier = modifier, tonalElevation = Dimens.PinnedBarElevation, color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ListGap),
            content = content,
        )
    }
}
