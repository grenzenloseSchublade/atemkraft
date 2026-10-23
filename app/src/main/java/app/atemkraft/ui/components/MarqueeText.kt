package app.atemkraft.ui.components

import android.animation.ValueAnimator
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Einzeiliger Text, der als Laufschrift läuft, wenn er nicht in die Breite passt – statt umzubrechen
 * (Nutzer 2026-10-23: Titel bleiben eine Zeile, die Kopfzeile behält überall ihre Höhe). Er läuft
 * dreimal und steht dann wieder am Anfang, ruhig wie die App; ein neuer Text startet von vorn. Ohne
 * System-Animationen (Animator-Skala 0) endet er stattdessen mit „…“ (MOTION-02). TalkBack liest
 * immer den ganzen Text.
 */
@Composable
fun MarqueeText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val marquee = remember { ValueAnimator.areAnimatorsEnabled() }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        overflow = if (marquee) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (marquee) modifier.basicMarquee() else modifier,
    )
}
