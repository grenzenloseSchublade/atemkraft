package app.atemkraft.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Blendet Status- und Navigationsleiste aus, solange [hidden] (Ruhe-Ansicht der Atem-Session):
 * keine Uhr, keine Benachrichtigungs-Symbole. Ein Wisch vom Rand holt sie kurz zurück, danach
 * verschwinden sie wieder; verlässt der Aufrufer die Komposition, kommen sie zurück. Damit der
 * Inhalt dabei nicht springt, rechnet [OverlayChrome] mit den Leisten, auch wenn sie fehlen.
 */
@Composable
fun HideSystemBars(hidden: Boolean) {
    val view = LocalView.current
    DisposableEffect(hidden) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (hidden && controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { if (hidden) controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
