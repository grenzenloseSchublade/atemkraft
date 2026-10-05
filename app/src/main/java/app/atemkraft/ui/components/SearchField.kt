package app.atemkraft.ui.components

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Einzeiliges Suchfeld ohne eigene Fläche: Text in `bodyLarge` auf einer Grundlinie
 * (`HorizontalDivider`), daneben das X ([onClear]) als `AppIconButton`. Es ersetzt im
 * [ScreenHeader] den Untertitel (Zustandswechsel statt neuer Fläche, PRIN-03).
 *
 * Datenschutz (SEC-PRIV-03): Eingaben zum Befinden sind gesundheitsnah. Die Tastatur bekommt
 * deshalb keine Autokorrektur und `IME_FLAG_NO_PERSONALIZED_LEARNING` – sie soll die Wörter
 * weder lernen noch synchronisieren. Das Feld selbst speichert nichts; den Text hält der
 * Aufrufer (nur `rememberSaveable`, nie DataStore oder Log).
 *
 * Die IME-Aktion „Suchen“ schließt nur die Tastatur, gefiltert wird schon beim Tippen.
 * [clearDescription] ist die `contentDescription` des X – je nach Zustand „Eingabe löschen“
 * oder „Suche schließen“.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    NoPersonalizedLearning {
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouchTarget)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                decorationBox = { inner ->
                    Column(
                        modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                                )
                            }
                            inner()
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = Dimens.GapTiny))
                    }
                },
            )
            AppIconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = clearDescription,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

/**
 * Setzt für alle Textfelder in [content] `IME_FLAG_NO_PERSONALIZED_LEARNING`: Die Tastatur
 * merkt sich die Eingaben nicht (Gboard, SwiftKey u. a. halten sich daran). Compose bietet
 * dafür keine `KeyboardOptions`, nur diesen Eingriff in die Eingabe-Sitzung.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NoPersonalizedLearning(content: @Composable () -> Unit) {
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
            query = "",
            onQueryChange = {},
            placeholder = stringResource(R.string.situations_search_placeholder),
            clearDescription = stringResource(R.string.cd_search_close),
            onClear = {},
        )
    }
}
