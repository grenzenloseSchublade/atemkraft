package app.atemkraft.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.asDotSuffix
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle

/** Ein anpassbarer Intervall-Wert in der Einstell-Karte; als Parameter null = Phase fehlt. */
data class PhaseAdjust(
    val value: Int,
    val default: Int,
    val range: IntRange,
    val onChange: (Int) -> Unit,
)

/**
 * Einstell-Karte einer Übung: Dauer-/Runden-Stepper, dahinter aufklappbar die Intervall-Stepper.
 * Eingeklappt markiert „· angepasst" abweichende Intervalle; der Hinweis verschwindet von
 * selbst, sobald die Werte – auch manuell zurückgesteppt – wieder den Defaults entsprechen.
 * Das Zurücksetzen selbst sitzt beim Start-Button (Split-Button im Detail-Screen).
 * Fehlen alle drei Phasen (Stufen-Übungen, Phasen unter 1 s, `AdjustDefaults.of`), bleibt nur
 * der Dauer- bzw. Runden-Stepper, ohne „Phasen anpassen“.
 * [durationSpokenLabel]/[durationUnit]: Wortlaut für TalkBack wie beim [Stepper]
 * („Dauer: 10 Minuten“).
 */
@Composable
fun SessionAdjustCard(
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
) {
    val intervalsModified = listOfNotNull(inhale, hold, exhale).any { it.value != it.default }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = Dimens.GapTiny)) {
            Stepper(
                label = durationLabel,
                value = duration,
                range = durationRange,
                onChange = onDuration,
                vertical = Dimens.GapSmall,
                spokenLabel = durationSpokenLabel,
                valueUnit = durationUnit,
            )

            if (inhale != null || hold != null || exhale != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimens.CardPadding),
                    color = DividerSubtle,
                )
                DisclosureToggle(
                    text = stringResource(R.string.adjust_intervals),
                    expanded = intervalsExpanded,
                    onToggle = onToggleIntervals,
                    // Das Caret-Glyph (knapp ein Drittel Innenrand im Icon) steht so etwa an
                    // der Stepper-Label-Kante (CardPadding).
                    modifier = Modifier.padding(start = Dimens.GapSmall),
                    note = if (!intervalsExpanded && intervalsModified) stringResource(R.string.adjust_modified).asDotSuffix() else null,
                )
                if (intervalsExpanded) {
                    inhale?.let {
                        Stepper(
                            label = stringResource(R.string.adjust_inhale),
                            value = it.value,
                            range = it.range,
                            onChange = it.onChange,
                            vertical = Dimens.GapTiny,
                            spokenLabel = stringResource(R.string.phase_inhale),
                            valueUnit = R.plurals.cd_duration_seconds,
                        )
                    }
                    hold?.let {
                        Stepper(
                            label = stringResource(R.string.adjust_hold),
                            value = it.value,
                            range = it.range,
                            onChange = it.onChange,
                            vertical = Dimens.GapTiny,
                            spokenLabel = stringResource(R.string.phase_hold_full),
                            valueUnit = R.plurals.cd_duration_seconds,
                        )
                    }
                    exhale?.let {
                        Stepper(
                            label = stringResource(R.string.adjust_exhale),
                            value = it.value,
                            range = it.range,
                            onChange = it.onChange,
                            vertical = Dimens.GapTiny,
                            spokenLabel = stringResource(R.string.phase_exhale),
                            valueUnit = R.plurals.cd_duration_seconds,
                        )
                    }
                    Spacer(Modifier.height(Dimens.GapHairline))
                }
            }
        }
    }
}
