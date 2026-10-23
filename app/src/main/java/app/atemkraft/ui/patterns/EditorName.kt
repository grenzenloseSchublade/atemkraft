package app.atemkraft.ui.patterns

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.atemkraft.R
import app.atemkraft.data.PatternBackup
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.MarqueeText
import app.atemkraft.ui.components.NoPersonalizedLearning
import app.atemkraft.ui.components.rememberArmedConfirm
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Lesezeichen im Kopf (MUSTER-02): leer → gefüllt in `secondary`, Zustand per `stateDescription`
 * und Live-Region (nur für den eigenen Tipp, nicht wenn eine Wertänderung es leert). Bei einem
 * Muster, das es vor dem Öffnen schon gab, schärft der erste Tipp auf das gefüllte Lesezeichen nur
 * (Papierkorb mit Kreuz in `error`, „‚Name‘ wirklich löschen?“), erst der zweite entfernt es
 * (MUSTER-05).
 */
@Composable
internal fun SaveToggle(state: PatternEditorState, onSavedChange: (Boolean) -> Unit) {
    val saved = state.saved
    val confirm = rememberArmedConfirm()
    // Geänderte Werte leeren das Lesezeichen: Eine halb bestätigte Löschung gilt dann nicht mehr.
    LaunchedEffect(saved) { if (!saved) confirm.disarm() }
    val armed = confirm.armed && saved
    // Eine Ansage je Tipp (A11Y-04): Die Live-Region gilt nur ab dem eigenen Tipp und nur, bis sich
    // Phasen, Dauer oder Name ändern. Leert oder füllt eine solche Änderung das Lesezeichen, sagt
    // also nur der geänderte Wert etwas.
    val draft = PatternSnapshot(state.pattern, state.minutes, state.savedName)
    var announce by remember(draft) { mutableStateOf(false) }
    AppIconToggle(
        checked = saved,
        announce = announce,
        onCheckedChange = { checked ->
            announce = true
            when {
                checked -> onSavedChange(true)
                state.existedBefore -> confirm.tap { onSavedChange(false) }
                else -> onSavedChange(false)
            }
        },
        stateDescription = stringResource(if (saved) R.string.state_saved else R.string.state_not_saved),
        modifier = confirm.focusModifier,
    ) {
        when {
            armed -> Icon(
                painterResource(R.drawable.ic_delete_forever),
                contentDescription = stringResource(R.string.cd_delete_confirm, state.snapshot?.name ?: state.savedName),
                tint = MaterialTheme.colorScheme.error,
            )

            else -> Icon(
                painterResource(if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                contentDescription = stringResource(R.string.cd_pattern_save),
                tint = if (saved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            )
        }
    }
}

/**
 * Seitentitel (Nachtrag 7): der Name des Musters groß (`headlineSmall`), darunter klein „Eigenes
 * Muster“ als Einordnung; „intensiv“ steht seit 2026-10-23 rechts in der Zeile der Werte über der
 * Kurve ([InfoLine]), der Titel bekommt die ganze Breite vor Startpunkt und Lesezeichen. Der
 * Name steht in einer Zeile und läuft als Laufschrift, wenn er nicht passt – nie umbrochen: Der Kopf
 * behält seine Höhe, auch wenn der Vorschlagsname mit den Zahlen wächst. Ein kleiner Stift direkt
 * hinter dem Namen zeigt, dass er änderbar ist (Nutzer 2026-10-23); er bleibt stehen, der Name
 * bekommt die Restbreite. Antippen des ganzen Titels benennt um ([onRename]); TalkBack: „Name: …,
 * umbenennen“ als Überschrift, der Stift ist dekorativ.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NameTitle(state: PatternEditorState, onRename: () -> Unit) {
    val name = state.savedName
    val description = stringResource(R.string.cd_pattern_name_rename, name)
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .semantics {
                heading()
                contentDescription = description
            }
            .clickable(role = Role.Button, onClick = onRename),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Für TalkBack unsichtbar (der Titel sagt „Name: …, umbenennen“); nicht clearAndSetSemantics,
        // damit die Umbruch- und Zuschnittprüfung die Text-Layouts sieht.
        Column(Modifier.weight(1f).semantics { invisibleToUser() }) {
            NameWithPencil(name)
            Text(
                text = stringResource(R.string.pattern_editor_title),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onBackground.copy(alpha = SECONDARY),
            )
        }
    }
}

/**
 * Der Name als Laufschrift ([MarqueeText], `headlineSmall`) mit dem Stift direkt dahinter: Der
 * Stift steht immer, der Name bekommt höchstens die Restbreite (kein `weight(fill = false)`,
 * LAYOUT-03). Senkrecht mittig auf der Namenszeile.
 */
@Composable
private fun NameWithPencil(name: String) {
    val colors = MaterialTheme.colorScheme
    Layout(
        content = {
            // Ein langer Name läuft als Laufschrift wie jeder Seitentitel, statt umzubrechen.
            MarqueeText(text = name, style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
            Icon(
                Icons.Filled.Edit,
                contentDescription = null, // dekorativ: Der ganze Titel ist die Tippfläche („Name: …, umbenennen“).
                tint = colors.onBackground.copy(alpha = SECONDARY),
                modifier = Modifier.size(Sizes.IconSmall),
            )
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val pencil = measurables[1].measure(loose)
        val gap = Dimens.GapTiny.roundToPx()
        val text = measurables[0].measure(loose.copy(maxWidth = (constraints.maxWidth - pencil.width - gap).coerceAtLeast(0)))
        val height = maxOf(text.height, pencil.height)
        layout(constraints.maxWidth, height) {
            text.placeRelative(0, (height - text.height) / 2)
            pencil.placeRelative(text.width + gap, (height - pencil.height) / 2)
        }
    }
}

/**
 * Umbenennen (Nachtrag 7, Tipp auf den Seitentitel): ein Feld, einzeilig, scrollt waagrecht,
 * höchstens [PatternEditorState.NAME_MAX] Zeichen; es ändert den Namen sofort (das Lesezeichen
 * leert sich wie bei jeder Änderung). Leer steht der Vorschlagsname als Platzhalter da – so wird
 * auch gespeichert. Zeichen, die der Import nicht annimmt (Steuer- und Formatzeichen, Emoji
 * außerhalb der Grundebene), nimmt das Feld gar nicht erst an; Zeilenumbrüche aus der
 * Zwischenablage werden Leerzeichen ([nameInput]). „Fertig“ auf der Tastatur oder im Dialog schließt.
 */
@Composable
internal fun RenameDialog(state: PatternEditorState, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_editor_name)) },
        text = {
            // Wie das Suchfeld: Die Tastatur lernt den Namen nicht (SEC-PRIV-06).
            NoPersonalizedLearning {
                BasicTextField(
                    value = state.name,
                    onValueChange = { state.updateName(nameInput(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .onFocusChanged { focused = it.hasFocus },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDismiss() }),
                    decorationBox = { inner ->
                        // Label im Feld: So liest TalkBack es mit dem Inhalt als einen Knoten.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.MinTouchTarget)
                                .border(Dimens.BorderThin, if (focused) colors.primary else colors.outline, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = Dimens.CardPadding, vertical = Dimens.GapSmall),
                            verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline),
                        ) {
                            Text(
                                text = stringResource(R.string.pattern_editor_name_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface.copy(alpha = SECONDARY),
                            )
                            Box {
                                if (state.name.isEmpty()) {
                                    Text(
                                        text = state.suggestedName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurface.copy(alpha = SECONDARY),
                                        maxLines = 1,
                                    )
                                }
                                inner()
                            }
                        }
                    },
                )
            }
        },
        confirmButton = { AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.pattern_editor_name_done)) } },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/**
 * Nimmt nur, was auch der Import als Namen annimmt ([PatternBackup.cleanName]): Leerraum wird zum
 * Leerzeichen, verborgene oder trennende Zeichen (auch die Hälften von Emoji außerhalb der
 * Grundebene) fallen weg, statt erst beim Speichern still zu Leerzeichen zu werden.
 */
internal fun nameInput(raw: String): String = buildString {
    raw.forEach { char ->
        when {
            char.isWhitespace() -> append(' ')
            PatternBackup.cleanName(char.toString()).isNotEmpty() -> append(char)
        }
    }
}
