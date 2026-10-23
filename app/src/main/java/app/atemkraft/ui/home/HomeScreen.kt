package app.atemkraft.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternTexts
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.ExerciseCard
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.PatternCard
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.TabListScaffold
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.flushStart
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/** Atmen-Tab: startbare Übungen nach Familien + Abschnitt „Programme & Wissen". */
@Composable
fun HomeScreen(
    exercisesByFamily: List<Pair<BreathingFamily, List<Exercise>>>,
    programs: List<Exercise>,
    daily: DailyPattern,
    dailySaved: Boolean,
    onRegenerateDaily: () -> Unit,
    onDailySavedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onOpenSettings: () -> Unit,
    markedIds: Set<String> = emptySet(),
    // Einstieg in den Muster-Editor unter der Tageskarte; ohne ihn kein Link.
    onOpenPatternEditor: (() -> Unit)? = null,
    // Kalender im Kopf: öffnet das Logbuch; ohne ihn kein Icon (Vorschauen, Tests).
    onOpenLogbook: (() -> Unit)? = null,
) {
    // Der Kopf steht fest über der Liste; liegt Inhalt darunter, zeigt er eine Haarlinie.
    TabListScaffold(
        header = { scrolled ->
            ScreenHeader(
                title = stringResource(R.string.tab_breathe),
                action = { SettingsAction(onClick = onOpenSettings) },
                onOpenLogbook = onOpenLogbook,
                scrolled = scrolled,
            )
        },
        subtitle = stringResource(R.string.home_subtitle),
    ) {
        item(key = "daily-header") {
            // „Neu generieren“ betrifft den ganzen Abschnitt („gib mir ein anderes“), nicht
            // die Karte – deshalb im Kopf, die Karte behält nur ihre eine Aktion (KOMP-02).
            SectionHeader(
                title = stringResource(R.string.home_daily_pattern),
                color = MaterialTheme.colorScheme.secondary,
            ) {
                AppIconButton(onClick = onRegenerateDaily) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.cd_pattern_regenerate),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        item(key = "daily-pattern") {
            DailyPatternCard(
                daily = daily,
                saved = dailySaved,
                onClick = { onSelect(daily.exercise.id) },
                onSavedChange = onDailySavedChange,
            )
            // Eigener Weg zum Editor neben der Karte, nicht in ihr: Die Karte behält ihre
            // eine Aktion (KOMP-02). Bündig: Das Polster des Text-Buttons ragt in den Rand.
            if (onOpenPatternEditor != null) {
                AppTextButton(onClick = onOpenPatternEditor, modifier = Modifier.flushStart()) {
                    ForwardLinkText(
                        text = stringResource(R.string.home_own_pattern_link),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        exercisesByFamily.forEach { (family, exercises) ->
            item(key = "header-${family.name}") {
                SectionHeader(title = family.title(), color = family.color())
            }
            items(exercises, key = { it.id }) { exercise ->
                ExerciseCard(exercise = exercise, onClick = { onSelect(exercise.id) }, marked = exercise.id in markedIds)
            }
        }

        if (programs.isNotEmpty()) {
            item(key = "header-programs") {
                SectionHeader(
                    title = stringResource(R.string.home_programs),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )
            }
            items(programs, key = { it.id }) { program ->
                ExerciseCard(exercise = program, onClick = { onSelect(program.id) }, marked = program.id in markedIds)
            }
        }
    }
}

/**
 * Karte für das generierte „Muster des Tages": [PatternCard] mit dem Lesezeichen als einziger
 * weiterer Aktion (Speichern ↔ wieder entfernen).
 */
@Composable
private fun DailyPatternCard(
    daily: DailyPattern,
    saved: Boolean,
    onClick: () -> Unit,
    onSavedChange: (Boolean) -> Unit,
) {
    PatternCard(exercise = daily.exercise, patternLine = PatternTexts.hintFor(daily.spec), onClick = onClick) {
        // Gespeicherte Muster erscheinen im Tab „Eigene“ unter „Meine Muster“ – der
        // Atmen-Tab bleibt schlank. Gefüllt im Akzent = gespeichert (Zustandsakzent, KOMP-02);
        // erneutes Tippen nimmt das Muster wieder heraus.
        AppIconToggle(
            checked = saved,
            onCheckedChange = onSavedChange,
            stateDescription = stringResource(if (saved) R.string.state_saved else R.string.state_not_saved),
        ) {
            Icon(
                painterResource(if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                contentDescription = stringResource(R.string.cd_pattern_save),
                tint = if (saved) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY)
                },
            )
        }
    }
}
