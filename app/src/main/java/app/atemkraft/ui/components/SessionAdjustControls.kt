package app.atemkraft.ui.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.asDotSuffix
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion

/** Ein anpassbarer Intervall-Wert im Blatt „Anpassen“; als Parameter null = Phase fehlt. */
data class PhaseAdjust(
    val value: Int,
    val default: Int,
    val range: IntRange,
    val onChange: (Int) -> Unit,
)

/**
 * Einstellungen einer Übung im Blatt „Anpassen“ der Detailseite (`AdjustSheet`): Dauer-/Runden-
 * Stepper, darunter aufklappbar die Intervall-Stepper. Alles steht direkt auf dem Blatt, ohne Karte
 * und ohne Trenner (Anatomie „Bottom Sheet“, §4.5: das Blatt ist schon die Fläche); die Labels
 * stehen an der Textkante des Titels, der Kreis von „+“ bündig rechts (LAYOUT-07). Gegliedert wird
 * mit Abstand wie in „Klang & Stimme“: Dauer → Aufklapper `GapSmall` (der 48-dp-Rand der Knöpfe
 * zählt sichtbar mit).
 *
 * Eingeklappt markiert „· angepasst“ abweichende Intervalle; der Hinweis verschwindet von selbst,
 * sobald die Werte – auch manuell zurückgesteppt – wieder den Defaults entsprechen. Das
 * Zurücksetzen selbst sitzt beim Start-Button (Split-Button im Detail-Screen). Fehlen alle drei
 * Phasen (Stufen-Übungen, Phasen unter 1 s, `AdjustDefaults.of`), bleibt nur der Dauer- bzw.
 * Runden-Stepper, ohne „Phasen anpassen“.
 *
 * Die Einheit steht beim Wert („5 s“, mit [durationValue] „10 min“), nicht in Klammern am Label;
 * alle Stepper halten die Breite des größten möglichen Werts, so springt beim Steppen nichts und
 * die Knöpfe stehen untereinander in einer Flucht.
 * [durationSpokenLabel]/[durationUnit]: Wortlaut für TalkBack wie beim [Stepper]
 * („Dauer: 10 Minuten“).
 */
@Composable
fun SessionAdjustControls(
    durationLabel: String,
    duration: Int,
    durationDefault: Int,
    durationRange: IntRange,
    onDuration: (Int) -> Unit,
    inhale: PhaseAdjust?,
    hold: PhaseAdjust?,
    exhale: PhaseAdjust?,
    intervalsExpanded: Boolean,
    onToggleIntervals: () -> Unit,
    modifier: Modifier = Modifier,
    durationSpokenLabel: String = durationLabel,
    @PluralsRes durationUnit: Int? = null,
    @StringRes durationValue: Int? = null,
) {
    val intervalsModified = listOfNotNull(inhale, hold, exhale).any { it.value != it.default }
    // Breiteste Werte aller Zeilen: eine gemeinsame Wertspalte für Dauer und Phasen.
    val widest = listOf(durationValue?.let { stringResource(it, durationRange.last) } ?: durationRange.last.toString()) +
        listOfNotNull(inhale, hold, exhale).map { stringResource(R.string.adjust_value_seconds, it.range.last) }

    Column(modifier = modifier.fillMaxWidth()) {
        Stepper(
            label = durationLabel,
            value = duration,
            range = durationRange,
            onChange = onDuration,
            horizontal = 0.dp,
            vertical = 0.dp,
            spokenLabel = durationSpokenLabel,
            valueUnit = durationUnit,
            valueText = durationValue?.let { stringResource(it, duration) } ?: duration.toString(),
            widestValues = widest,
        )

        if (inhale != null || hold != null || exhale != null) {
            Spacer(Modifier.height(Dimens.GapSmall))
            DisclosureToggle(
                text = stringResource(R.string.adjust_intervals),
                expanded = intervalsExpanded,
                onToggle = onToggleIntervals,
                // „· angepasst“ als Status des Aufklappers (Anatomie „Titel · Status“), sein Platz
                // bleibt reserviert: Erscheint er beim Zuklappen, springt nichts (S-17).
                note = stringResource(R.string.adjust_modified).asDotSuffix(),
                noteVisible = !intervalsExpanded && intervalsModified,
            )
            // Nur der Inhalt animiert seine Höhe (wie CollapsibleSection), der Aufklapper darüber
            // bleibt stehen; das Blatt wächst und schrumpft weich statt zu springen (S-34).
            Column(modifier = Modifier.fillMaxWidth().animateContentSize(Motion.Disclosure)) {
                if (intervalsExpanded) {
                    inhale?.let { PhaseStepper(R.string.adjust_inhale, R.string.phase_inhale, it, widest) }
                    // Ein Wert für Halten voll und leer zugleich (SessionConfig.holdSeconds).
                    hold?.let { PhaseStepper(R.string.adjust_hold, R.string.phase_hold, it, widest) }
                    exhale?.let { PhaseStepper(R.string.adjust_exhale, R.string.phase_exhale, it, widest) }
                }
            }
        }
    }
}

/** Ein Phasen-Stepper im Blatt: Label ohne Einheit, Wert „5 s“, gesprochen „Einatmen: 5 Sekunden“. */
@Composable
private fun PhaseStepper(@StringRes label: Int, @StringRes spoken: Int, phase: PhaseAdjust, widest: List<String>) {
    Stepper(
        label = stringResource(label),
        value = phase.value,
        range = phase.range,
        onChange = phase.onChange,
        horizontal = 0.dp,
        vertical = Dimens.GapTiny,
        spokenLabel = stringResource(spoken),
        valueUnit = R.plurals.cd_duration_seconds,
        valueText = stringResource(R.string.adjust_value_seconds, phase.value),
        widestValues = widest,
    )
}
