package app.atemkraft.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.PatternTexts
import app.atemkraft.ui.components.AppBottomSheet
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.BreathMarkIcon
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.PatternCard
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Kopfzeile „Muster des Tages“ im Atmen-Tab (Nutzer 2026-10-30): ein Abschnittskopf mit Chevron
 * wie „Klang & Stimme“ in der Meditation (`SectionHeader` mit `onClick`, MUSTER-11), davor die
 * Atemkurve in `secondary` als Zeichen des Einstiegs. Der Teaser nennt den Charakter von heute und
 * den Weg zum Selbstgestalten, ohne Ziffern (die Zahlen stehen im Blatt; „Atem ist keine Zahl“);
 * die ganze Zeile öffnet das
 * [DailyPatternSheet]. Sie steht in jedem Zustand des Tabs – auch bei Anlass, Suche, Bereich und
 * Filter –, damit sie beim Wechsel nicht springt.
 */
@Composable
internal fun DailyPatternEntry(daily: DailyPattern, onClick: () -> Unit) {
    val character = stringResource(if (daily.exercise.activating) R.string.daily_pattern_gentle_up else R.string.detail_calm)
    SectionHeader(
        title = stringResource(R.string.home_daily_pattern),
        teaser = stringResource(R.string.home_daily_teaser, character),
        onClick = onClick,
        onClickLabel = stringResource(R.string.cd_daily_open),
        leadingIcon = painterResource(R.drawable.ic_breath_curve),
        // Darüber stehen schon ListGap der Liste und die unsichtbare Tippfläche unter den
        // Anlass-Chips (40 in 48): mit dem inneren GapSmall sichtbar SectionHeaderTop vom Chip bis
        // zum Titel. Ein zweiter GapSmall vor der Tippfläche machte daraus 32.
        top = 0.dp,
    )
}

/**
 * Blatt „Muster des Tages“ (Nutzer 2026-10-30): oben das generierte Muster als Karte mit ↻ „Neu
 * generieren“ und ◉ „In Eigene speichern“ (Tipp auf die Karte öffnet die Detailseite), darunter
 * die Karte „Selbst gestalten ›“ ([NavCard]), die den Muster-Editor mit dem Muster von heute als
 * Ausgangspunkt öffnet ([onNewPattern]) – so knüpft das Gestalten an das an, was man gerade sieht;
 * eine andere Vorlage wählt man im Editor über „Startpunkt“. Beides an einem Ort:
 * Wer mit dem Tagesmuster spielt, findet hier den nächsten Schritt, sein eigenes zu bauen.
 *
 * Gerüst: [AppBottomSheet], der Satz unter dem Titel ist dessen Intro (Teaser des Titels). Keine
 * gefüllte Neon-Fläche; schließen per Wischen, Tipp daneben oder Zurück ([onDismiss]).
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
    AppBottomSheet(
        title = stringResource(R.string.home_daily_pattern),
        onDismiss = onDismiss,
        intro = stringResource(R.string.home_daily_sheet_intro),
    ) {
        DailyPatternCard(daily = daily, saved = saved, onClick = onOpen, onSavedChange = onSavedChange, onRegenerate = onRegenerate)
        Spacer(Modifier.height(Dimens.ListGap))
        // Selbst gestalten als ganze Karte: Die Kernfunktion ist so groß antippbar wie das
        // Tagesmuster darüber, nicht nur ein Textverweis (Nutzer 2026-10-30). Karten sind hier
        // richtig: Beides öffnet etwas (Anatomie „Bottom Sheet“, §4.5).
        NavCard(
            title = stringResource(R.string.home_create_title),
            hint = stringResource(R.string.home_create_intro),
            onClick = onNewPattern,
        )
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
