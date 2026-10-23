package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Übungskarte in Listen. Die Studienlage steht nur auf der Detailseite: Auf der Liste trüge fast
 * jede Karte dasselbe Etikett. Die Karte zeigt, was vor dem Antippen zählt – Vorsicht, „Programm“
 * und die persönliche Markierung ([marked], Atemkreis rechts auf Höhe der ersten Titelzeile).
 */
@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit, modifier: Modifier = Modifier, marked: Boolean = false) {
    val accent = exercise.family.color()
    val titleStyle = MaterialTheme.typography.titleMedium
    // Card(onClick) statt Modifier.clickable: Ripple folgt der Kartenform, Tippfläche ≥ 48 dp
    // bringt die M3-Surface selbst mit (KOMP-02).
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = accentBorder(accent),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                TitleWithChips(
                    title = exercise.name,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    chips = if (!exercise.caution && exercise.guided) {
                        null
                    } else {
                        {
                            if (exercise.caution) CautionChip()
                            if (!exercise.guided) InfoChip()
                        }
                    },
                )
                if (marked) {
                    // Box in Höhe einer Titelzeile: Der Kreis steht mittig zur ersten Zeile,
                    // auch wenn Titel und Chips umbrechen.
                    Box(
                        modifier = Modifier.height(with(LocalDensity.current) { titleStyle.lineHeight.toDp() }),
                        contentAlignment = Alignment.Center,
                    ) { PersonalMarkIndicator() }
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            Text(
                text = exercise.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
            )
        }
    }
}

/**
 * Karte eines generierten Musters – Muster des Tages und „Meine Muster“ sehen gleich aus: Name
 * mit Charakter-Chip, darunter die kurze Musterzeile mit Dauer, Rand in Cyan (eigene Muster).
 * [action] ist die eine weitere Aktion (KOMP-02) und liegt als Overlay in der Kartenecke statt
 * in der Titelzeile: Die volle 48-dp-Tippfläche liegt über dem Karten-Padding, die Karte wird
 * nicht höher als ihr Text. Der Text hält rechts die Breite der Tippfläche frei, damit nichts
 * darunter liegt. [patternLine] ist die kurze Musterzeile (Muster des Tages aus seinen vier
 * Feldern, eigene Muster ihre Folge); der Chip zeigt „intensiv“, wenn die Übung `caution` trägt.
 */
@Composable
fun PatternCard(
    exercise: Exercise,
    patternLine: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = accentBorder(MaterialTheme.colorScheme.secondary),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(
                    start = Dimens.CardPadding,
                    top = Dimens.CardPadding,
                    end = Dimens.MinTouchTarget,
                    bottom = Dimens.CardPadding,
                ),
            ) {
                TitleWithChips(
                    title = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                ) {
                    // Ein eigenes intensives Muster sieht schon in der Liste so aus wie auf der Detailseite.
                    CharacterChip(intense = exercise.caution, activating = exercise.activating)
                }
                Spacer(Modifier.height(Dimens.GapTiny))
                // Kurze Musterzeile statt instructionHint: Die Einladung zur Bauchatmung steht
                // auf der Detailseite unter „Anleitung“, nicht in jeder Karte.
                Text(
                    text = patternLine + DOT_SEPARATOR +
                        stringResource(R.string.duration_approx, exercise.defaultMinutes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) { action() }
        }
    }
}
