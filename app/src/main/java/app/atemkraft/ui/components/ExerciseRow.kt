package app.atemkraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.breathRhythm
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Ab dieser Schriftgröße steht eine [ExerciseRow] ohne Kurve – sonst bräche ein Name im Wort
 * (LAYOUT-03).
 */
internal const val LARGE_FONT_SCALE = 1.5f

/**
 * Schlanke Zeile einer Übung (Nutzer 2026-10-31: „Zuletzt geübt“ im Tab „Eigene“ und die Listen
 * „Eigene Kreationen“ und „Tut mir gut“):
 * vorn die Form des Atemzugs als [MiniCurve] (ohne Takt – Feueratmung, freies Atmen, offene Phasen
 * – ein Punkt in der Familienfarbe; ab Schrift 1,5 keins), daneben der Name (`titleSmall`, bricht nur zwischen Wörtern),
 * darunter [hint] („gestern Abend · 10 min“; ohne [hint] „ca. 10 min“), bei einer Übung mit Vorsicht
 * der [CautionChip]; am Ende [trailing] (Chevron, ◉ oder Stift und Löschen), mittig auf der ersten
 * Namenszeile. Die
 * ganze Zeile ist der Button (≥ 48 dp), ein Fokus-Stopp: Name, Hinweis ([hintDescription]
 * ausgeschrieben, A11Y-06) und Zeichen in einer Ansage. Mehrere Zeilen stehen in [ExerciseRows].
 *
 * Statt der [ExerciseCard], wo eine Sammlung kurz bleiben soll: Die Karte zeigt Beschreibung und
 * Phasen, die Zeile nur, was man zum Wiederfinden braucht – die Kurve unterscheidet die Muster.
 */
@Composable
fun ExerciseRow(
    exercise: Exercise,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    hintDescription: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    val nameStyle = MaterialTheme.typography.titleSmall
    val firstLine = rememberFirstLineHeight(nameStyle)
    val shownHint = hint ?: stringResource(R.string.duration_approx, exercise.defaultMinutes())
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Dimens.CardPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        // Ab Schrift 1,5 entfällt die Kurve: Ihre 64 dp braucht der Name, sonst bräche er im Wort.
        if (LocalDensity.current.fontScale < LARGE_FONT_SCALE) {
            Box(modifier = Modifier.heightIn(min = firstLine), contentAlignment = Alignment.Center) { ExerciseCurve(exercise) }
        }
        Column(modifier = Modifier.weight(1f)) {
            WholeWordText(text = exercise.name, style = nameStyle, color = MaterialTheme.colorScheme.onSurface)
            HintText(shownHint, modifier = if (hintDescription != null) Modifier.semantics { contentDescription = hintDescription } else Modifier)
            if (exercise.caution) {
                Spacer(Modifier.height(Dimens.GapTiny))
                CautionChip()
            }
        }
        Box(modifier = Modifier.heightIn(min = firstLine), contentAlignment = Alignment.Center) { trailing() }
    }
}

/**
 * Form des Atemzugs einer Übung ([MiniCurve], `MiniCurveWidth` × `MiniCurveHeight`); ohne Takt
 * (Feueratmung, freies Atmen, offene Phasen) ein Punkt in der Familienfarbe am Anfang der Fläche.
 * Dekorativ, ohne Semantik: Name und Hinweis daneben tragen die Bedeutung.
 */
@Composable
fun ExerciseCurve(exercise: Exercise, modifier: Modifier = Modifier) {
    val rhythm = remember(exercise) { exercise.breathRhythm() }
    Box(modifier = modifier.size(width = Sizes.MiniCurveWidth, height = Sizes.MiniCurveHeight), contentAlignment = Alignment.CenterStart) {
        if (rhythm != null) {
            MiniCurve(rhythm)
        } else {
            Box(Modifier.size(Sizes.FamilyDot).clip(CircleShape).background(exercise.family.color()))
        }
    }
}

/**
 * Mehrere [ExerciseRow] in einer Karte, getrennt durch `DividerSubtle` – gebaut wie die Tageskarten
 * des Logbuchs (Rand `CardPadding` seitlich, die Zeilen polstern oben und unten selbst).
 */
@Composable
fun ExerciseRows(rows: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = Dimens.CardPadding)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = DividerSubtle)
                row()
            }
        }
    }
}

@Preview
@Composable
private fun ExerciseRowPreview() {
    AtemkraftTheme {
        ExerciseRows(
            rows = BuiltInExercises.all.take(2).map { exercise ->
                {
                    ExerciseRow(exercise = exercise, onClick = {}) {
                        ForwardChevron(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY))
                    }
                }
            },
        )
    }
}
