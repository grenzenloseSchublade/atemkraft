package app.atemkraft.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Wiederverwendbarer Inline-Expander (Progressive Disclosure): Titel + Appetizer, tippbar zum
 * Ausklappen. Der Appetizer steht **immer ganz** da – nie per Ellipse gekürzt, denn er trägt
 * Wirkung und Sicherheit als geschlossenen Satz (TEXT-06, -08, LAYOUT-03). [content] erscheint
 * aufgeklappt darunter und vertieft ihn. Ohne [content] gibt es nichts aufzuklappen: dann
 * ein schlichter Abschnitt ohne Caret und ohne Klick. Caret dreht sich, sanftes
 * Größen-Animieren, TalkBack bekommt Überschrift + Erweitert/Eingeklappt.
 */
@Composable
fun ExpanderSection(
    title: String,
    appetizer: String,
    accent: Color,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val expandable = content != null
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "caret")

    Column(modifier = modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (expandable) Modifier.clickable { expanded = !expanded } else Modifier)
                .semantics {
                    heading()
                    if (expandable) stateDescription = if (expanded) "Erweitert" else "Eingeklappt"
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WholeWordText(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = accent,
                )
                Text(
                    text = appetizer,
                    // Eine Rolle für beide Zustände (kein Reflow beim Aufklappen); Betonung nur
                    // über die Deckkraft.
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                        .copy(alpha = if (expanded || !expandable) 0.8f else 0.6f),
                )
            }
            if (expandable) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
        if (expanded && content != null) {
            content()
            Spacer(Modifier.height(10.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
    }
}
