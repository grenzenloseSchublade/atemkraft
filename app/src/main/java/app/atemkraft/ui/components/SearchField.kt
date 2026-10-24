package app.atemkraft.ui.components

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Einzeiliges Suchfeld als Pille (Nutzer 2026-10-24, Vorschlag B): `MinTouchTarget` hoch, Fläche
 * `surfaceContainerHigh`, `CircleShape`, ohne Rand; links die Lupe ([searchDescription] als cd),
 * Text in `bodyMedium`, rechts das X ([onClear]) als `AppIconButton`. Ohne [onClear] kein X
 * (leeres Feld: nichts zu leeren); sein Kreis bleibt innerhalb der Pille, die Tippfläche ragt um
 * `CircleOverhang` über sie hinaus. Heute die Befindens-Suche im Atmen-Tab (MUSTER-09), dort ohne
 * umgebenden Kasten direkt über den Anlass-Chips.
 *
 * Ein Zustand: Der Text lebt allein im hochgezogenen [state] (`TextFieldState`,
 * `rememberTextFieldState` beim Aufrufer – das ist `rememberSaveable`). Der Aufrufer liest
 * `state.text` und setzt den Text über `state.clearText()` bzw. `state.edit { … }`; das Feld
 * hält keinen zweiten Wert daneben (Nutzer 2026-10-24: Am Gerät stand das Feld nach dem Schließen
 * der Tastatur ohne „Suchen“ leer da, während die Treffer stehen blieben; der String-Overload von
 * `BasicTextField` führte einen inneren `TextFieldValue` neben dem Text des Aufrufers – mit einem
 * Zustand gibt es nichts, das auseinanderlaufen kann). [inputTransformation]
 * läuft nur bei Eingaben des Nutzers, nicht bei `clearText()` (Atmen-Tab: eine Eingabe hebt die
 * Wahl auf). [interactionSource] gibt dem Aufrufer den Fokus (`collectIsFocusedAsState`).
 *
 * Datenschutz (SEC-PRIV-03): Eingaben zum Befinden sind gesundheitsnah. Die Tastatur bekommt
 * deshalb keine Autokorrektur und `IME_FLAG_NO_PERSONALIZED_LEARNING` – sie soll die Wörter
 * weder lernen noch synchronisieren. Das Feld selbst speichert nichts; den Text hält der
 * Aufrufer (nur `rememberSaveable`, nie DataStore oder Log).
 *
 * Die IME-Aktion „Suchen“ schließt nur die Tastatur, gefiltert wird schon beim Tippen.
 * [clearDescription] ist die `contentDescription` des X („Eingabe löschen“).
 */
@Composable
fun SearchField(
    state: TextFieldState,
    placeholder: String,
    searchDescription: String,
    clearDescription: String,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null,
    inputTransformation: InputTransformation? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    NoPersonalizedLearning {
        Row(
            modifier = modifier
                .testTag(CONTROL_ROW_TAG)
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                // Lupe und X stehen um das Polster vom runden Ende ab; das X bringt mit seiner
                // 48-dp-Tippfläche schon CircleOverhang davon mit.
                .padding(start = Dimens.CardPadding, end = Dimens.CardPadding - Sizes.CircleOverhang),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = searchDescription,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                modifier = Modifier.size(Sizes.IconDefault),
            )
            Spacer(Modifier.width(Dimens.ListGap))
            BasicTextField(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    // Das Feld selbst ist die Tippfläche (A11Y-05), nicht nur die Pille um es herum.
                    .heightIn(min = Dimens.MinTouchTarget)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                inputTransformation = inputTransformation,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                onKeyboardAction = KeyboardActionHandler { keyboard?.hide() },
                lineLimits = TextFieldLineLimits.SingleLine,
                interactionSource = interactionSource,
                decorator = TextFieldDecorator { inner ->
                    Box(modifier = Modifier.heightIn(min = Dimens.MinTouchTarget), contentAlignment = Alignment.CenterStart) {
                        // Immer im Baum, sichtbar nur bei leerem Feld: Der Platzhalter wird mit dem
                        // Feld zusammengeführt und bleibt so sein Name wie das Label eines
                        // M3-Textfelds – auch wenn Text drinsteht (TalkBack „Erschö, Wie fühlst du
                        // dich?“, Voice Access „Tippe auf …“; A11Y-01). Nur nicht gezeichnet, nicht
                        // per Alpha: Durchsichtige Knoten blendet die Bedienungshilfe aus. Bei
                        // großer Schrift bricht er an Wortgrenzen um, die Pille wächst mit.
                        val empty = state.text.isEmpty()
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                            modifier = Modifier.drawWithContent { if (empty) drawContent() },
                        )
                        inner()
                    }
                },
            )
            // Ohne Eingabe hält ein Platzhalter die Breite des X: Das Feld springt beim ersten
            // Buchstaben nicht.
            if (onClear != null) {
                AppIconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = clearDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Spacer(Modifier.size(Dimens.MinTouchTarget))
            }
        }
    }
}

/**
 * Setzt für alle Textfelder in [content] `IME_FLAG_NO_PERSONALIZED_LEARNING`: Die Tastatur
 * merkt sich die Eingaben nicht (Gboard, SwiftKey u. a. halten sich daran). Compose bietet
 * dafür keine `KeyboardOptions`, nur diesen Eingriff in die Eingabe-Sitzung. Auch das Namensfeld
 * des Muster-Editors nutzt ihn.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NoPersonalizedLearning(content: @Composable () -> Unit) {
    InterceptPlatformTextInput(interceptor = NoLearningInterceptor, content = content)
}

@OptIn(ExperimentalComposeUiApi::class)
private object NoLearningInterceptor : PlatformTextInputInterceptor {
    override suspend fun interceptStartInputMethod(
        request: PlatformTextInputMethodRequest,
        nextHandler: PlatformTextInputSession,
    ): Nothing {
        val withoutLearning = object : PlatformTextInputMethodRequest {
            override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                val connection = request.createInputConnection(outAttributes)
                outAttributes.imeOptions = outAttributes.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                return connection
            }
        }
        nextHandler.startInputMethod(withoutLearning)
    }
}

@Preview
@Composable
private fun SearchFieldPreview() {
    AtemkraftTheme {
        SearchField(
            state = rememberTextFieldState("Prüfung"),
            placeholder = stringResource(R.string.situations_search_placeholder),
            searchDescription = stringResource(R.string.cd_search),
            clearDescription = stringResource(R.string.cd_search_clear),
            onClear = {},
        )
    }
}
