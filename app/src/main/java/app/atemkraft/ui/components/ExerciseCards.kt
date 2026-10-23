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
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.cardBreath
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.domain.formatDecimal
import app.atemkraft.domain.hasOpenPhases
import app.atemkraft.domain.inStages
import app.atemkraft.domain.minutesRounded
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Übungskarte in Listen. Die Studienlage steht nur auf der Detailseite: Auf der Liste trüge fast
 * jede Karte dasselbe Etikett. Die Karte zeigt, was vor dem Antippen zählt – Vorsicht, „Programm“
 * und die persönliche Markierung ([marked], Atemkreis rechts auf Höhe der ersten Titelzeile) –,
 * unter dem Titel die Metazeile mit Takt und Dauer wie die Musterzeile der [PatternCard]
 * („Einatmen 4 s · Ausatmen 6 s · ca. 10 min“, [exerciseMetaLine], Nutzer 2026-10-24), darunter
 * die Kurzbeschreibung.
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
            exerciseMetaLine(exercise)?.let { meta ->
                Spacer(Modifier.height(Dimens.GapTiny))
                // Eine Stufe heller als die Kurzbeschreibung: die Zeile, nach der man entscheidet.
                BodyText(meta)
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            SecondaryText(exercise.shortDescription)
        }
    }
}

/**
 * Metazeile einer angeleiteten Übung: ein Atemzug in Worten ([cardBreath], „Einatmen 4 s ·
 * Ausatmen 6 s“; bei Stufen „in Stufen“; bei mehreren Abschnitten nichts) und die Dauer wie am
 * Startknopf der Detailseite – ohne eigene Anpassungen, wie der Filter „Bis 3 min“: „ca. 10 min“,
 * bei offenen Phasen „ab ca. 3 min“, unter einer Minute „unter 1 min“. Programme ohne Takt: null.
 */
@Composable
private fun exerciseMetaLine(exercise: Exercise): String? {
    if (!exercise.guided || exercise.segments.isEmpty()) return null
    val rhythm = exercise.cardBreath()?.map { phase ->
        val seconds = ((phase.duration as PhaseDuration.Fixed).millis / 1000.0).formatDecimal()
        stringResource(R.string.card_phase_seconds, phaseName(phase.type), seconds)
    } ?: listOfNotNull(stringResource(R.string.card_in_stages).takeIf { exercise.inStages })
    val seconds = exercise.estimatedTotalSeconds()
    val duration = when {
        exercise.hasOpenPhases -> stringResource(R.string.duration_approx_from, (seconds / 60).coerceAtLeast(1))
        seconds < 60 -> stringResource(R.string.card_duration_under_minute)
        else -> stringResource(R.string.duration_approx, minutesRounded(seconds))
    }
    return (rhythm + duration).joinToString(DOT_SEPARATOR)
}

/** Phasenname für die Metazeile; „Halten“ ohne voll/leer wie in der Sitzung (`phase_hold_*`). */
@Composable
private fun phaseName(type: PhaseType): String = stringResource(
    when (type) {
        PhaseType.INHALE -> R.string.phase_inhale
        PhaseType.EXHALE -> R.string.phase_exhale
        PhaseType.HOLD_FULL -> R.string.phase_hold_full
        PhaseType.HOLD_EMPTY -> R.string.phase_hold_empty
        PhaseType.INHALE_TOP_UP -> R.string.phase_inhale_top_up
        PhaseType.REST -> R.string.phase_rest
    },
)

/**
 * Karte eines generierten Musters – Muster des Tages und „Eigene Kreationen“ sehen gleich aus: Name
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
                SecondaryText(patternLine + DOT_SEPARATOR + stringResource(R.string.duration_approx, exercise.defaultMinutes()))
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) { action() }
        }
    }
}
