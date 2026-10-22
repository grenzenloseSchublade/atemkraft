package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens

/**
 * Fester Kopf weitergeführter Seiten (Push): Zurück-Pfeil und optional der Seitentitel in einer
 * Zeile über dem scrollenden Inhalt. Er steht außerhalb der Scroll-Fläche, damit der Weg zurück
 * nie aus dem Bild scrollt – vorher sprangen die Einstellungen aus der Meditation sofort zur
 * Meditations-Karte, und der Pfeil war nie zu sehen.
 *
 * Keine eigene Fläche, keine Erhöhung: Der Kopf liegt auf dem Hintergrund des Screens (PRIN-03).
 * Ohne [title] steht nur der Pfeil fest – für Seiten, deren Kopf zum Inhalt gehört (Markenkopf
 * auf „Über“). Rand (`ScreenPadding`) und Abstand oben (`ScreenTopSub`) bringt er selbst mit,
 * weil er neben und nicht in der Scroll-Spalte steht.
 * Der Titel bricht nie im Wort um ([WholeWordText]); wird er zweizeilig (lange Übungsnamen auf
 * der Detailseite), wächst die Zeile mit. [action] steht rechts außen, ihr Icon bündig zur
 * Inhaltskante wie der Pfeil links; der Titel nimmt nur die Breite dazwischen (`weight`).
 * [titleContent] ersetzt [title] durch einen eigenen Titelbereich in derselben Breite (Muster-Editor:
 * Name des Musters, zum Umbenennen antippbar, Nachtrag 7).
 */
@Composable
fun PushHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenPadding, top = Dimens.ScreenTopSub, end = Dimens.ScreenPadding)
            .heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Die Tippfläche des Pfeils ragt links in den Rand ([flushStart]); so steht der Pfeil an
        // der Inhaltskante, und der Titel folgt ihm im Abstand des Überstands statt doppelt so
        // weit („weit versetzt“). Die Tippfläche selbst bleibt 48 dp (A11Y-05).
        Box(Modifier.flushStart()) { BackButton(onClick = onBack) }
        if (titleContent != null) {
            Box(Modifier.weight(1f)) { titleContent() }
        } else if (title != null) {
            WholeWordText(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
        }
        if (action != null) {
            if (title == null && titleContent == null) Spacer(Modifier.weight(1f))
            // Spiegelbild zum Pfeil: Die Tippfläche ragt rechts in den Rand.
            Box(Modifier.flushEnd()) { action() }
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
