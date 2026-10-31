package app.atemkraft.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.splitAtDots
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Gepinnte Startleiste einer Übung – ein Bauteil für Detailseite und Muster-Editor (KOMP-01,
 * Eigentümer 2026-10-31: zwei verschiedene Startleisten „sahen mies aus“): in der
 * [PinnedActionBar] oben die Zeile „[adjustTitle] · [adjustStatus] ›“, darunter der
 * [StartSplitButton] mit [startLabel] („Starten · ca. 10 min“) und – nur mit [resetVisible] – dem
 * Zurücksetzen. Erst die Einstellung, dann die Aktion.
 *
 * Die Zeile ist ein Abschnittskopf mit `onClick` wie „Klang & Stimme“ (MUSTER-11, `titleLarge` auch
 * in der Leiste) und öffnet ein Blatt ([onOpenAdjust]); der aktuelle Wert steht als leiser Status
 * dahinter (`smallStatus`, „Anpassen · 10 min“, „Dauer · 5 min“), bei großer Schrift laut
 * [SectionHeader] darunter. TalkBack hört eine Schaltfläche aus Titel und
 * [adjustStatusDescription] (ausgeschrieben, „10 Minuten“) mit [adjustOpenLabel]. Keine
 * Live-Region: Den Wert sagt der Stepper im Blatt an.
 *
 * **Knapp in der Höhe** steht alles in einer Zeile: links nur der Wert mit Chevron („10 min ›“,
 * dieselbe Schaltfläche mit derselben Ansage und demselben Blatt), rechts der Start in der
 * Restbreite – passt „Starten · ca. 5 min“ dort nicht in eine Zeile, nur „Starten“ (die Dauer steht
 * links daneben). Knapp heißt: Die Höhe, die die Seite der Leiste anbietet (in Detailseite und Editor
 * misst die Spalte sie vor dem Inhalt, also die Höhe unter dem Kopf), geteilt durch die
 * Schriftgröße liegt unter [Sizes.StartBarCompactBelow] – kleine Geräte bei großer Schrift, quer –,
 * und Wert und Start passen nebeneinander (Start mit seiner breitesten Zeile und Platz für das
 * Zurücksetzen); sonst bleibt es bei zwei Zeilen, kein Wort bricht. Die Leiste entscheidet das
 * selbst, nicht der Aufrufer – Detailseite und Editor verhalten sich gleich (Eigentümer
 * 2026-10-31, MUSTER-10).
 */
@Composable
fun StartBar(
    adjustTitle: String,
    adjustStatus: String,
    adjustStatusDescription: String,
    onOpenAdjust: () -> Unit,
    startLabel: String,
    resetVisible: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    adjustOpenLabel: String = stringResource(R.string.cd_adjust_open),
) {
    val fontScale = LocalDensity.current.fontScale
    val valueWidth = rememberWidestTextWidth(listOf(adjustStatus), MaterialTheme.typography.titleMedium)
    val labelStyle = LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge)
    val startParts = startLabel.splitAtDots()
    val startWidth = rememberWidestTextWidth(startParts, labelStyle)
    val fullStartWidth = rememberWidestTextWidth(listOf(startLabel), labelStyle)
    val padding = ButtonDefaults.ContentPadding
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier) {
        val tight = maxHeight / fontScale < Sizes.StartBarCompactBelow
        // Innenbreite der Leiste (ohne ihren Rand) gegen Wert mit Chevron + Abstand + schmalsten Start samt Reset.
        val inner = maxWidth - Dimens.ScreenPadding * 2
        val value = valueWidth + Dimens.GapTiny + Sizes.IconInButton + Dimens.ListGap
        val buttonPadding = padding.calculateLeftPadding(direction) + padding.calculateRightPadding(direction)
        val compact = tight && value + startWidth + buttonPadding + Dimens.ListGap + ResetWidth <= inner
        // Einzeilig nur, wenn auch der Start eine Zeile bleibt: Passt „Starten · ca. 5 min“ neben
        // Wert und Zurücksetzen nicht, steht nur „Starten“ – die Dauer steht ja links daneben.
        val startRoom = inner - value - buttonPadding - if (resetVisible) Dimens.ListGap + ResetWidth else 0.dp
        StartBarContent(
            compact = compact,
            adjustTitle = adjustTitle,
            adjustStatus = adjustStatus,
            adjustStatusDescription = adjustStatusDescription,
            onOpenAdjust = onOpenAdjust,
            startLabel = if (compact && fullStartWidth > startRoom) startParts.first() else startLabel,
            resetVisible = resetVisible,
            onStart = onStart,
            onReset = onReset,
            adjustOpenLabel = adjustOpenLabel,
        )
    }
}

@Composable
private fun StartBarContent(
    compact: Boolean,
    adjustTitle: String,
    adjustStatus: String,
    adjustStatusDescription: String,
    onOpenAdjust: () -> Unit,
    startLabel: String,
    resetVisible: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
    adjustOpenLabel: String,
) {
    PinnedActionBar {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                CompactAdjust(
                    status = adjustStatus,
                    spoken = stringResource(R.string.cd_start_bar_adjust, adjustTitle, adjustStatusDescription),
                    openLabel = adjustOpenLabel,
                    onClick = onOpenAdjust,
                )
                Box(Modifier.weight(1f)) {
                    StartSplitButton(label = startLabel, resetVisible = resetVisible, onStart = onStart, onReset = onReset)
                }
            }
        } else {
            Column {
                SectionHeader(
                    title = adjustTitle,
                    status = stringResource(R.string.adjust_status, adjustStatus),
                    statusDescription = adjustStatusDescription,
                    smallStatus = true,
                    // Erstes Element der Leiste: Ihr Rand (ListGap) ist der Abstand über der Tippfläche, kein
                    // zweiter GapSmall davor – oben wie unten am Start steht nur der Leistenrand.
                    top = 0.dp,
                    onClick = onOpenAdjust,
                    onClickLabel = adjustOpenLabel,
                )
                StartSplitButton(label = startLabel, resetVisible = resetVisible, onStart = onStart, onReset = onReset)
            }
        }
    }
}

/**
 * Die Einstellzeile in knapper Höhe: nur der Wert mit Chevron („10 min ›“, `titleMedium`), eine
 * Schaltfläche (≥ 48 dp) mit der Ansage der langen Zeile („Anpassen, 10 Minuten“) und ihrer Aktion.
 */
@Composable
private fun CompactAdjust(status: String, spoken: String, openLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onClick)
            .semantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
    ) {
        // Für TalkBack unsichtbar: Die Schaltfläche sagt Titel und Wert ausgeschrieben.
        WholeWordText(
            text = status,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { hideFromAccessibility() },
        )
        ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
    }
}

@Preview
@Composable
private fun StartBarPreview() {
    AtemkraftTheme {
        StartBar(
            adjustTitle = stringResource(R.string.adjust_title),
            adjustStatus = "10 min",
            adjustStatusDescription = "10 Minuten",
            onOpenAdjust = {},
            startLabel = "Starten · ca. 10 min",
            resetVisible = false,
            onStart = {},
            onReset = {},
        )
    }
}
