package app.atemkraft.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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
 * Gleiche Zeile wie der Kopf der Tabs ([ScreenHeader], [HeaderBar]): Beim Wechsel von Tab zu
 * Unterseite springt nichts, nur der Pfeil kommt dazu. Keine eigene Fläche, keine Erhöhung: Der
 * Kopf liegt auf dem Hintergrund des Screens (PRIN-03); [scrolled] (Inhalt liegt unter dem Kopf)
 * blendet eine Haarlinie an seiner Unterkante ein.
 * Ohne [title] steht nur der Pfeil fest – für Seiten, deren Kopf zum Inhalt gehört (Markenkopf
 * auf „Über“).
 * Der Titel steht in einer Zeile; passt er nicht (lange Übungsnamen auf der Detailseite), läuft er
 * als Laufschrift ([HeaderTitle]). [action] steht rechts außen, ihr Icon bündig zur
 * Inhaltskante wie der Pfeil links; der Titel nimmt nur die Breite dazwischen (`weight`).
 * [titleContent] ersetzt [title] durch einen eigenen Titelbereich in derselben Breite (Muster-Editor:
 * Name des Musters, zum Umbenennen antippbar, Nachtrag 7). Der Titel – Seitentitel oder Name,
 * auch eines eigenen Musters – steht wörtlich, ohne Sanskrit-Auszeichnung (TEXT-14).
 */
@Composable
fun PushHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    scrolled: Boolean = false,
) {
    HeaderBar(modifier = modifier.scrolledHairline(scrolled)) {
        // Die Tippfläche des Pfeils ragt links in den Rand ([flushStart]); so steht der Pfeil an
        // der Inhaltskante, und der Titel folgt ihm im Abstand des Überstands statt doppelt so
        // weit („weit versetzt“). Die Tippfläche selbst bleibt 48 dp (A11Y-05).
        Box(Modifier.flushStart()) { BackButton(onClick = onBack) }
        if (titleContent != null) {
            Box(Modifier.weight(1f)) { titleContent() }
        } else if (title != null) {
            HeaderTitle(title, Modifier.weight(1f))
        }
        if (action != null) {
            if (title == null && titleContent == null) Spacer(Modifier.weight(1f))
            // Spiegelbild zum Pfeil: Die Tippfläche ragt rechts in den Rand.
            Box(Modifier.flushEnd()) { action() }
        }
    }
}

/**
 * Gerüst jeder Push-Seite mit scrollendem Inhalt (§4.3): `Surface(background)`, darin fest der
 * [PushHeader] (Haarlinie, sobald Inhalt unter ihm liegt) und darunter die Scroll-Spalte mit Rand
 * `ScreenPadding`, oben `SectionGap` bis zum ersten Inhalt, unten `ScreenBottom`. Darunter fest
 * optional [bottomBar] (`PinnedActionBar` der Detailseite). Titel, [titleContent] und [action]
 * wie beim [PushHeader]. [contentModifier] liegt zwischen Scroll und Rand – dort misst die
 * Atemlehre Sprungziele gegen den Scroll-Inhalt. Eine Einleitung steht als [ScreenSubtitle] oben
 * im Inhalt.
 */
@Composable
fun PushScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            PushHeader(
                onBack = onBack,
                title = title,
                titleContent = titleContent,
                action = action,
                scrolled = scrollState.canScrollBackward,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .then(contentModifier)
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.SectionGap))
                content()
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
            bottomBar?.invoke()
        }
    }
}

/**
 * Die eine Kopfzeile aller Seiten (Tabs und Push): Rand `ScreenPadding`, Abstand oben
 * `ScreenTopSub`, mindestens eine Tippfläche hoch – fest über dem scrollenden Inhalt.
 */
@Composable
internal fun HeaderBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenPadding, top = Dimens.ScreenTopSub, end = Dimens.ScreenPadding)
            .heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Titel der Kopfzeile, auf Tabs und Push-Seiten gleich groß (`headlineSmall`): eine Zeile, die als
 * Laufschrift läuft, wenn sie nicht passt ([MarqueeText], Nutzer 2026-10-23) – nie umbrochen, nie
 * verkleinert, also überall gleich hoch.
 */
@Composable
internal fun HeaderTitle(text: String, modifier: Modifier = Modifier) {
    MarqueeText(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Haarlinie (`outlineVariant`) an der Unterkante des festen Kopfs, sanft eingeblendet, solange
 * [scrolled] – ganz oben bleibt der Kopf ohne Linie. Gezeichnet, nicht gelegt: Die Höhe ändert
 * sich nicht. Vor dem Rand anwenden, damit sie von Kante zu Kante reicht.
 */
@Composable
internal fun Modifier.scrolledHairline(scrolled: Boolean): Modifier {
    val alpha by animateFloatAsState(if (scrolled) 1f else 0f, label = "hairline")
    val color = MaterialTheme.colorScheme.outlineVariant
    return drawWithContent {
        drawContent()
        if (alpha > 0f) {
            val stroke = Dimens.BorderThin.toPx()
            val y = size.height - stroke / 2f
            drawLine(color.copy(alpha = color.alpha * alpha), Offset(0f, y), Offset(size.width, y), stroke)
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
