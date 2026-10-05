package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import kotlin.math.roundToInt

// Zentrale Bedienelemente: Die SICHTBARE Größe kommt aus Sizes (∝ Schrift, LAYOUT-04), die
// Tippfläche bleibt ≥ 48 dp (A11Y-05). Bauregeln (LAYOUT-06):
// 1. Höhe an M3-Elementen nur als Mindestwert (heightIn) – M3-Surfaces reichen sie per
//    propagateMinConstraints durch, der Inhalt wächst bei großer Schrift mit; nie height()/size().
// 2. Eigene feste Größe nur als minimumInteractiveComponentSize().size(x), in dieser Reihenfolge:
//    Das Layout reserviert 48 dp, sichtbar ist x.
// 3. Kein Modifier.scale() auf bedienbaren Knoten – der graphicsLayer verkleinert den Hit-Test
//    mit. Komponenten ohne Größenparameter (Switch, Radio) über scaledLayout, die Tippfläche
//    trägt ein äußerer toggleable-/selectable-Knoten.

/**
 * Misst den Inhalt in voller Größe, belegt aber nur die [f]-fache Fläche und zeichnet ihn
 * entsprechend verkleinert. Nur für M3-Komponenten ohne Größenparameter, deren Tippfläche ein
 * äußerer Knoten trägt (Bauregel 3).
 */
internal fun Modifier.scaledLayout(f: Float): Modifier = if (f == 1f) {
    this
} else {
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val w = (placeable.width * f).roundToInt()
        val h = (placeable.height * f).roundToInt()
        layout(w, h) {
            placeable.placeWithLayer((w - placeable.width) / 2, (h - placeable.height) / 2) {
                scaleX = f
                scaleY = f
            }
        }
    }
}

/**
 * Runder Icon-Button: sichtbar [Sizes.IconButtonSize] (Ripple nur auf dieser Fläche), Icon in
 * [Sizes.IconDefault], Tippfläche 48 dp. [colors] wie bei M3 (`IconButtonDefaults.*Colors()`),
 * der Stepper nutzt die gefüllte Variante. Das Icon trägt die `contentDescription` (ICON-02).
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(Sizes.IconButtonSize)
            .clip(CircleShape)
            .background(if (enabled) colors.containerColor else colors.disabledContainerColor)
            .clickable(role = Role.Button, enabled = enabled, interactionSource = null, indication = ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (enabled) colors.contentColor else colors.disabledContentColor
        CompositionLocalProvider(LocalContentColor provides tint) {
            // Begrenzt jedes Icon (Vektor 24 dp oder Painter) auf die Standardgröße.
            Box(Modifier.size(Sizes.IconDefault), contentAlignment = Alignment.Center) { content() }
        }
    }
}

/**
 * Icon-only-Umschalter mit Zustand (z. B. Muster speichern): Optik und Tippfläche wie
 * [AppIconButton], aber `toggleable` mit `Role.Checkbox` und [stateDescription], damit TalkBack
 * Name, Rolle und Zustand liest (A11Y-01). Die Live-Region sagt den Wechsel an, ohne dass der
 * Fokus springt (A11Y-04, MUSTER-02) – statt eines ausgegrauten „✓ Gespeichert“. Den Zustand
 * zeigt der Aufrufer im Icon (gefüllt/leer); das Icon trägt die `contentDescription` (ICON-02).
 */
@Composable
fun AppIconToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    stateDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(Sizes.IconButtonSize)
            .clip(CircleShape)
            .toggleable(
                value = checked,
                interactionSource = null,
                indication = ripple(),
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .semantics {
                this.stateDescription = stateDescription
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(Sizes.IconDefault), contentAlignment = Alignment.Center) { content() }
    }
}

/** Text-Button mit an die Schrift gekoppelter Mindesthöhe ([Sizes.ButtonHeight]), Tippfläche 48 dp. */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.ButtonHeight),
        enabled = enabled,
        content = content,
    )
}

/**
 * Schalterzeile (A11Y-03): Die ganze Zeile schaltet um (`Role.Switch`, Name = [label]), der
 * Schalter steht in der Zeile seines Namens, [hint] darunter. Der Switch ist nur Anzeige,
 * verkleinert auf [Sizes.ControlScale] und bündig an der Inhaltskante.
 */
@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = ripple(),
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = Dimens.GapTiny),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WholeWordText(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Dimens.GapSmall))
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.scaledLayout(Sizes.ControlScale),
                enabled = enabled,
                interactionSource = interactions,
            )
        }
        if (hint != null) {
            WholeWordText(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            )
        }
    }
}

@Preview
@Composable
private fun AppIconButtonPreview() {
    AtemkraftTheme {
        AppIconButton(onClick = {}) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_voice_delete)) }
    }
}

@Preview
@Composable
private fun AppIconTogglePreview() {
    AtemkraftTheme {
        AppIconToggle(checked = true, onCheckedChange = {}, stateDescription = stringResource(R.string.state_saved)) {
            Icon(
                painterResource(R.drawable.ic_bookmark_filled),
                contentDescription = stringResource(R.string.cd_pattern_save),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Preview
@Composable
private fun AppTextButtonPreview() {
    AtemkraftTheme { AppTextButton(onClick = {}) { Text(stringResource(R.string.settings_voice_download)) } }
}

@Preview
@Composable
private fun ToggleRowPreview() {
    AtemkraftTheme {
        ToggleRow(
            label = stringResource(R.string.meditation_speech_label),
            checked = true,
            onCheckedChange = {},
            hint = stringResource(R.string.meditation_speech_hint),
        )
    }
}
