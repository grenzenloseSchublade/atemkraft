package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.domain.BreathingFamily
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
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens

/**
 * Übungskarte in Listen. Die Studienlage steht nur auf der Detailseite: Auf der Liste trüge fast
 * jede Karte dasselbe Etikett. Die Karte zeigt, was vor dem Antippen zählt – Vorsicht, „zum Nachlesen“
 * und die persönliche Markierung ([marked], Atemkreis rechts auf Höhe der ersten Titelzeile,
 * [CardHeader] mit `indicator`) –, unter dem Titel die Metazeile mit Takt und Dauer wie die
 * Musterzeile der [PatternCard] („Einatmen 4 s · Ausatmen 6 s · ca. 10 min“, [exerciseMetaLine],
 * Nutzer 2026-10-24), darunter die Kurzbeschreibung. Anatomie: STYLEGUIDE §4.5 „Karte mit Aktionen“.
 */
@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit, modifier: Modifier = Modifier, marked: Boolean = false) {
    val accent = exercise.family.color()
    // Card(onClick) statt Modifier.clickable: Ripple folgt der Kartenform, Tippfläche ≥ 48 dp
    // bringt die M3-Surface selbst mit (KOMP-02).
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = accentBorder(accent),
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardHeader(
                title = exercise.name,
                chips = if (!exercise.caution && exercise.guided) {
                    null
                } else {
                    {
                        if (exercise.caution) CautionChip()
                        if (!exercise.guided) InfoChip()
                    }
                },
                indicator = if (marked) ({ PersonalMarkIndicator() }) else null,
            )
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

/** Phasenname für die Metazeile; „Halten voll“/„Halten leer“ wie in der Sitzung (`phase_hold_*`, TEXT-02). */
@Composable
internal fun phaseName(type: PhaseType): String = stringResource(
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
 * [patternLine] ist die kurze Musterzeile (Muster des Tages aus seinen vier Feldern, eigene Muster
 * ihre Folge); der Chip zeigt „intensiv“, wenn die Übung `caution` trägt.
 *
 * Die [actionCount] Icon-Knöpfe in [action] stehen auf der Titelzeile ([CardHeader]): Glyph mittig
 * auf der ersten Titelzeile und bündig an der Inhaltskante, nur die Titelzeile hält ihre Breite
 * frei, die Musterzeile läuft darunter durch, die Karte wird nicht höher (Nutzer 2026-10-24;
 * Kreationen-Karte: Stift und Mülleimer; Tagesmuster: ↻ und ◉; nie mehr als zwei, KOMP-02).
 * Anatomie: STYLEGUIDE §4.5.
 */
@Composable
fun PatternCard(
    exercise: Exercise,
    patternLine: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionCount: Int = 1,
    action: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = accentBorder(MaterialTheme.colorScheme.secondary),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
        ) {
            CardHeader(
                title = exercise.name,
                // Ein eigenes intensives Muster sieht schon in der Liste so aus wie auf der Detailseite.
                chips = { CharacterChip(intense = exercise.caution, activating = exercise.activating) },
                actionCount = actionCount,
                actions = action,
            )
            Spacer(Modifier.height(Dimens.GapTiny))
            // Kurze Musterzeile statt instructionHint: Die Einladung zur Bauchatmung steht
            // auf der Detailseite unter „Anleitung“, nicht in jeder Karte.
            SecondaryText(patternLine + DOT_SEPARATOR + stringResource(R.string.duration_approx, exercise.defaultMinutes()))
        }
    }
}

@Preview
@Composable
private fun ExerciseCardPreview() {
    val exercise = Exercise(
        id = "vorschau",
        name = stringResource(R.string.welcome_title),
        family = BreathingFamily.DOWNREGULATE,
        shortDescription = stringResource(R.string.welcome_body),
        effect = stringResource(R.string.welcome_body),
        caution = true,
    )
    AtemkraftTheme { ExerciseCard(exercise = exercise, onClick = {}, marked = true) }
}
