package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Start-Knopf „Übung starten“ der Leiste unten im Muster-Editor (Nachtrag 6 und 7): gefüllter Kreis
 * in `primary`, Play-Dreieck in `onPrimary` – wie [AppIconButton] sichtbar [Sizes.IconButtonSize],
 * Tippfläche 48 dp. Gleich groß wie Startpunkt-Pille und Dauer-Stepper daneben (ein Größentoken je
 * Leiste, LAYOUT-04); hervorgehoben nur durch die Füllung, die einzige gefüllte Neon-Fläche des
 * Screens (PRIN-02), deutlich anders als die kleine, ungefüllte Vorschau-▶. [description] ist der
 * ganze Wortlaut für TalkBack („Übung starten, ca. 5 Minuten“).
 */
@Composable
fun StartPlayButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier) {
    AppIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = description)
    }
}

/**
 * Tonale Pille mit Symbol [icon] und Beschriftung [label] für knappe Leisten (Muster-Editor:
 * Startpunkt „▦ Box“, Nachtrag 7): sichtbar so hoch wie die Kreise daneben ([Sizes.IconButtonSize],
 * ein Größentoken je Leiste, LAYOUT-04; wächst bei großer Schrift mit), Fläche wie die
 * Stepper-Knöpfe (`filledTonal`), Tippfläche 48 dp. Ohne [label] nur das Symbol im Kreis – für
 * Leisten, in denen die Beschriftung keinen Platz hat. [description] ist der ganze Wortlaut für
 * TalkBack; die sichtbare Beschriftung liest er nicht noch einmal.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TonalPillButton(onClick: () -> Unit, icon: Painter, label: String?, description: String, modifier: Modifier = Modifier) {
    val colors = IconButtonDefaults.filledTonalIconButtonColors()
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = Sizes.IconButtonSize)
            .widthIn(min = Sizes.IconButtonSize)
            .clip(CircleShape)
            .background(colors.containerColor)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (label == null) Modifier else Modifier.padding(horizontal = Dimens.ListGap)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = colors.contentColor, modifier = Modifier.size(Sizes.IconInButton)) // dekorativ: description benennt den Knopf
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.contentColor,
                maxLines = 1,
                softWrap = false,
                // Für TalkBack unsichtbar (description sagt es); nicht clearAndSetSemantics, damit die
                // Umbruch- und Zuschnittprüfung das Text-Layout sieht.
                modifier = Modifier.semantics { invisibleToUser() },
            )
        }
    }
}

@Preview
@Composable
private fun StartPlayButtonPreview() {
    AtemkraftTheme { StartPlayButton(onClick = {}, description = "Übung starten, ca. 5 Minuten") }
}

@Preview
@Composable
private fun TonalPillButtonPreview() {
    AtemkraftTheme { TonalPillButton(onClick = {}, icon = painterResource(R.drawable.ic_grid_view), label = "Box", description = "Startpunkt wählen, aktuell: Box") }
}
