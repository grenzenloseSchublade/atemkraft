package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens

/**
 * Geteilter Rahmen der Vollbild-Overlays (Atem-Session + Meditation): Hintergrund-Surface mit
 * sicherem Rand (safeDrawing plus Systemleisten, auch wenn die Ruhe-Ansicht sie ausblendet –
 * sonst rückte der Inhalt nach oben, [HideSystemBars]), oben Minimieren-Chevron (˅) links und
 * In-Session-Stummschalter rechts, darunter der eigentliche Inhalt. [chromeVisible] = false blendet die Kopfleiste sanft aus
 * (Ruhe-Ansicht der Atem-Session, [HideableSlot]); ihr Platz bleibt, der Inhalt springt nicht.
 *
 * Für TalkBack ist das Overlay ein eigener Bereich mit [title] als `paneTitle` (Übungsname bzw.
 * „Meditation“, A11Y-07); den Rest der App blendet MainActivity derweil aus ([inertWhen]).
 * Der Ton ist ein Schalter: fester Name „Ton“, Rolle Switch, Zustand „An“/„Aus“ (A11Y-01).
 */
@Composable
fun OverlayChrome(
    title: String,
    muted: Boolean,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    chromeVisible: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize().semantics { paneTitle = title },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(StableInsets)) {
            HideableSlot(visible = chromeVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.action_minimize),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    // Schneller Ton-Schalter für die laufende Sitzung (die Einstellung bleibt
                    // unberührt); Kreis wie der M3-IconButton daneben, fest wie die Kopfleiste.
                    SoundSwitch(
                        on = !muted,
                        onToggle = { onToggleMute() },
                        name = stringResource(R.string.action_sound),
                        size = Dimens.OverlayIconButton,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f), content = content)
        }
    }
}

/** Rand der Overlays, unabhängig davon, ob die Systemleisten gerade zu sehen sind. */
private val StableInsets: WindowInsets
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    get() = WindowInsets.safeDrawing.union(WindowInsets.systemBarsIgnoringVisibility)
