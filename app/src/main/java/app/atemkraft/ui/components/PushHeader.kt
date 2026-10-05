package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes

/**
 * Fester Kopf weitergeführter Seiten (Push): Zurück-Pfeil und optional der Seitentitel in einer
 * Zeile über dem scrollenden Inhalt. Er steht außerhalb der Scroll-Fläche, damit der Weg zurück
 * nie aus dem Bild scrollt – vorher sprangen die Einstellungen aus der Meditation sofort zur
 * Meditations-Karte, und der Pfeil war nie zu sehen.
 *
 * Keine eigene Fläche, keine Erhöhung: Der Kopf liegt auf dem Hintergrund des Screens (PRIN-03).
 * Ohne [title] steht nur der Pfeil fest – für Seiten, deren Kopf zum Inhalt gehört (Markenkopf
 * auf „Über“, Titel mit Chips auf der Detailseite). Rand (`ScreenPadding`) und Abstand oben
 * (`ScreenTopSub`) bringt er selbst mit, weil er neben und nicht in der Scroll-Spalte steht.
 * Der Titel bricht nie im Wort um ([WholeWordText]); wird er zweizeilig, wächst die Zeile mit.
 */
@Composable
fun PushHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    // Um diesen Rand rückt BackButton den Pfeil an die Inhaltskante (Tippfläche 48 − Icon, halbiert).
    val inset = (Dimens.MinTouchTarget - Sizes.IconDefault) / 2
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenPadding, top = Dimens.ScreenTopSub, end = Dimens.ScreenPadding)
            .heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Die Tippfläche des Pfeils steht links um [inset] über; rechts melden wir sie um
        // denselben Betrag schmaler. So folgt der Titel dem Pfeil im Abstand [inset] statt
        // doppelt so weit („weit versetzt“). Die Tippfläche selbst bleibt 48 dp (A11Y-05).
        Box(
            Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = (placeable.width - inset.roundToPx()).coerceAtLeast(0)
                layout(width, placeable.height) { placeable.place(0, 0) }
            },
        ) {
            BackButton(onClick = onBack)
        }
        if (title != null) {
            WholeWordText(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
        }
    }
}

@Preview
@Composable
private fun PushHeaderPreview() {
    AtemkraftTheme { PushHeader(onBack = {}, title = stringResource(R.string.glossary_title)) }
}

@Preview
@Composable
private fun PushHeaderNurPfeilPreview() {
    AtemkraftTheme { PushHeader(onBack = {}) }
}
