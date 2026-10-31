package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Fester Kopf der Top-Level-Tabs: dieselbe Zeile wie der Kopf der Push-Seiten ([HeaderBar],
 * Titel `headlineSmall`), nur ohne Pfeil – beim Wechsel springt nichts. Rechts die Icon-Aktionen,
 * immer an derselben Stelle: außen höchstens eine Aktion ([action], ein `AppIconButton`, etwa das
 * Zahnrad [SettingsAction]), Das äußere Icon steht bündig an der Inhaltskante ([flushEnd]). Der Titel bleibt eine
 * Zeile und läuft notfalls als Laufschrift ([HeaderTitle]).
 *
 * Der Kopf steht außerhalb der Liste und scrollt nie weg (Nutzer 2026-10-23: „Rahmen fest, Inhalt
 * scrollt“), damit das Zahnrad immer erreichbar ist. Die Tabs tragen keinen Untertitel mehr
 * (Nutzer 2026-10-24, Vorschlag B); Push-Seiten leiten mit [ScreenSubtitle] ein. [scrolled] (Inhalt liegt unter dem Kopf) blendet
 * die Haarlinie an der Unterkante ein. Der Titel ist zugleich `paneTitle` des Tabs ([HeaderBar],
 * A11Y-07): TalkBack sagt ihn beim Tab-Wechsel an.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    scrolled: Boolean = false,
) {
    Column(modifier.fillMaxWidth().scrolledHairline(scrolled)) {
        HeaderBar(paneTitle = title) {
            HeaderTitle(title, Modifier.weight(1f))
            // Glyph an der Inhaltskante wie der Caret der Abschnittsköpfe darunter; die Tippfläche
            // ragt in den Screen-Rand (LAYOUT-07).
            if (action != null) Box(Modifier.flushEnd()) { action() }
        }
    }
}

/**
 * Gerüst der Tabs mit Liste (Atmen, Eigene, Lernen) und des Logbuchs (Push-Variante, §4.3):
 * `Surface(background)`, fest oben der Kopf aus [header] – er bekommt `scrolled` (Inhalt liegt
 * unter ihm) für die Haarlinie –, darunter die `LazyColumn` mit `spacedBy(ListGap)`. [top] ist der
 * Abstand über dem ersten Eintrag (Tabs 0: der erste Eintrag bringt seinen Abstand selbst mit;
 * Logbuch `SectionGap` wie jede Push-Seite), unten [bottom] (Tabs `ScreenBottom`, Logbuch
 * `TabBarGripClearance` wie jede Seite der Tiefe), beides als `contentPadding`. Überschriften mit
 * [stickyHeader] kleben oben ([StickyHeaderOverlay]); klebt etwas, trägt die Schicht die Haarlinie
 * und [header] bekommt `scrolled = false`.
 */
@Composable
fun TabListScaffold(
    header: @Composable (scrolled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    top: Dp = 0.dp,
    bottom: Dp = Dimens.ScreenBottom,
    content: LazyListScope.() -> Unit,
) {
    val sticky = rememberStickyHeaderState(listState)
    val pinned by sticky.anyPinned()
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            header(listState.canScrollBackward && !pinned)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().stickyViewport(sticky)) {
                CompositionLocalProvider(LocalStickyHeaders provides sticky) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // Rand als contentPadding, nicht als Modifier: Die Liste reicht bis an den Bildrand, und
                        // bündige Icon-Buttons (flushEnd) ragen mit ihrer Tippfläche in den Rand, ohne dass der
                        // Listen-Clip sie anschneidet.
                        contentPadding = PaddingValues(
                            start = Dimens.ScreenPadding,
                            top = top,
                            end = Dimens.ScreenPadding,
                            bottom = bottom,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
                    ) {
                        content()
                    }
                }
                StickyHeaderOverlay(sticky)
            }
        }
    }
}

/**
 * Einleitung einer Push-Seite unter dem festen Kopf (`bodyMedium` in `SECONDARY`): Atemlehre,
 * Pranayama, Über. Die Tabs tragen seit 2026-10-24 keinen Untertitel. Sanskrit kursiv
 * ([sanskritText]).
 */
@Composable
fun ScreenSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = sanskritText(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        modifier = modifier,
    )
}

/** Zahnrad für [ScreenHeader]`.action`: öffnet die Einstellungen (cd `settings_title`). */
@Composable
fun SettingsAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.settings_title),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview
@Composable
private fun ScreenHeaderPreview() {
    AtemkraftTheme {
        ScreenHeader(
            title = stringResource(R.string.tab_breathe),
            action = { SettingsAction(onClick = {}) },
        )
    }
}
