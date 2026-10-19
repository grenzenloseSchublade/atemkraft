package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Aufzählungspunkt: Glyph „•“ und [text] in einer Zeile, der Text bricht neben dem Glyph um.
 * Glyph in derselben Rolle wie der Text, damit er auf Höhe der ersten Zeile steht. TalkBack
 * liest nur den Text, der Glyph ist kein Fokus-Stopp (A11Y-06). [color] ist die Textfarbe, der
 * Glyph steht zurückgenommen in `SECONDARY`.
 */
@Composable
fun Bullet(text: AnnotatedString, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    val style = MaterialTheme.typography.bodySmall
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
        Text(
            text = BULLET_GLYPH,
            style = style,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            modifier = DecorativeGlyph,
        )
        Text(text = text, style = style, color = color)
    }
}

/** [Bullet] für schlichten Text. */
@Composable
fun Bullet(text: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Bullet(AnnotatedString(text), modifier, color)
}

private const val BULLET_GLYPH = "•"

/**
 * Für TalkBack unsichtbar. Nicht `clearAndSetSemantics`, damit das Text-Layout für die Umbruch-
 * und Zuschnittprüfung im Screenshot-Test sichtbar bleibt.
 */
@OptIn(ExperimentalComposeUiApi::class)
private val DecorativeGlyph = Modifier.semantics { invisibleToUser() }
