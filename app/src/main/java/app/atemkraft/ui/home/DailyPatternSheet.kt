package app.atemkraft.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.PatternTexts
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.BreathMarkIcon
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.PatternCard
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Kopfzeile „Muster des Tages“ im Atmen-Tab (Nutzer 2026-10-30): ein Abschnittskopf mit Chevron
 * wie „Klang & Stimme“ in der Meditation (`SectionHeader` mit `onClick`, MUSTER-11). Der Teaser
 * nennt die Zahlen von heute und den Weg zum Selbstgestalten; die ganze Zeile öffnet das
 * [DailyPatternSheet]. Sie steht in jedem Zustand des Tabs – auch bei Anlass, Suche, Bereich und
 * Filter –, damit sie beim Wechsel nicht springt.
 */
@Composable
internal fun DailyPatternEntry(daily: DailyPattern, onClick: () -> Unit) {
    val character = stringResource(if (daily.exercise.activating) R.string.daily_pattern_gentle_up else R.string.detail_calm)
    SectionHeader(
        title = stringResource(R.string.home_daily_pattern),
        teaser = stringResource(R.string.home_daily_teaser, PatternTexts.numbersOf(daily.spec), character),
        onClick = onClick,
        onClickLabel = stringResource(R.string.cd_daily_open),
    )
}

/**
 * Blatt „Muster des Tages“ (Nutzer 2026-10-30): oben das generierte Muster als Karte mit ↻ „Neu
 * generieren“ und ◉ „In Eigene speichern“ (Tipp auf die Karte öffnet die Detailseite), darunter
 * die Karte „Selbst gestalten ›“ ([NavCard]), die den Muster-Editor öffnet. Beides an einem Ort:
 * Wer mit dem Tagesmuster spielt, findet hier den nächsten Schritt, sein eigenes zu bauen.
 *
 * Gerüst wie [app.atemkraft.ui.meditation.SoundSheet] (Anatomie „Bottom Sheet“, §4.5): ganz
 * geöffnet, eigener stummer Griff, Titel als Abschnittskopf mit `top = 0`, TalkBack beginnt beim
 * Titel. Keine gefüllte Neon-Fläche; schließen per Wischen, Tipp daneben oder Zurück ([onDismiss]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyPatternSheet(
    daily: DailyPattern,
    saved: Boolean,
    onOpen: () -> Unit,
    onSavedChange: (Boolean) -> Unit,
    onRegenerate: () -> Unit,
    onNewPattern: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Stumm für TalkBack wie im SoundSheet (A11Y-05, -06).
            BottomSheetDefaults.DragHandle(Modifier.semantics { invisibleToUser() })
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                }
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.ScreenBottom),
        ) {
            SectionHeader(title = stringResource(R.string.home_daily_pattern), top = 0.dp)
            Spacer(Modifier.height(Dimens.HeaderGap))
            BodyText(stringResource(R.string.home_daily_sheet_intro))
            Spacer(Modifier.height(Dimens.ListGap))
            DailyPatternCard(daily = daily, saved = saved, onClick = onOpen, onSavedChange = onSavedChange, onRegenerate = onRegenerate)
            Spacer(Modifier.height(Dimens.ListGap))
            // Selbst gestalten als ganze Karte: Die Kernfunktion ist so groß antippbar wie das
            // Tagesmuster darüber, nicht nur ein Textverweis (Nutzer 2026-10-30).
            NavCard(
                title = stringResource(R.string.home_create_title),
                hint = stringResource(R.string.home_create_intro),
                onClick = onNewPattern,
            )
        }
    }
}

/**
 * Karte des generierten „Muster des Tages“ im Blatt: [PatternCard] mit zwei Aktionen auf der
 * Titelzeile (§4.5) – ↻ „Neu generieren“ ([onRegenerate], betrifft das Tagesmuster als Ganzes:
 * „gib mir ein anderes“) und der Atemkreis ◉ „In Eigene speichern“ (Speichern ↔ wieder entfernen),
 * beide in `onSurface` · `SECONDARY`, gespeichert gefüllt in `secondary`. Ohne Eyebrow: Der Titel
 * des Blatts sagt schon, was die Karte ist.
 */
@Composable
private fun DailyPatternCard(
    daily: DailyPattern,
    saved: Boolean,
    onClick: () -> Unit,
    onSavedChange: (Boolean) -> Unit,
    onRegenerate: () -> Unit,
) {
    PatternCard(
        exercise = daily.exercise,
        patternLine = PatternTexts.cardLineFor(daily.spec),
        onClick = onClick,
        actionCount = 2,
    ) {
        Row {
            AppIconButton(onClick = onRegenerate) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.cd_pattern_regenerate),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            // Dasselbe Zeichen wie „Tut mir gut“: ◉ heißt „zu mir“ (Auswertung 2026-10-24, 3.2);
            // gefüllt im Akzent = gespeichert (KOMP-02), erneutes Tippen nimmt es wieder heraus.
            AppIconToggle(
                checked = saved,
                onCheckedChange = onSavedChange,
                stateDescription = stringResource(if (saved) R.string.state_saved else R.string.state_not_saved),
            ) {
                BreathMarkIcon(filled = saved, contentDescription = stringResource(R.string.cd_pattern_save))
            }
        }
    }
}
