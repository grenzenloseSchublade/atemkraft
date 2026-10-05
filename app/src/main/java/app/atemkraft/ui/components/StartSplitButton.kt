package app.atemkraft.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens

/**
 * Gepinnter Start als Split-Button: links Starten in voller Restbreite, rechts gleitet – nur
 * wenn [resetVisible] – ein Kreispfeil zum Zurücksetzen herein (Icon-only, TalkBack über
 * contentDescription). Beide Knöpfe teilen dieselbe Höhe ([ButtonDefaults.MinHeight]).
 * Überall verwenden, wo eine konfigurierbare Session gestartet wird.
 */
@Composable
fun StartSplitButton(
    label: String,
    resetVisible: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        Button(onClick = onStart, modifier = Modifier.weight(1f)) {
            Text(label)
        }
        AnimatedVisibility(
            visible = resetVisible,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier.height(ButtonDefaults.MinHeight),
                contentPadding = PaddingValues(horizontal = Dimens.CardPadding),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reset),
                    contentDescription = stringResource(R.string.adjust_reset),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
