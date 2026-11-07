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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.atemkraft.R
import app.atemkraft.data.PatternBackup
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.MarqueeText
import app.atemkraft.ui.components.NoPersonalizedLearning
import app.atemkraft.ui.components.TitleWithStatus
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes

/**
 * Diskette im Kopf eines Entwurfs (MUSTER-14, Eigentümer 2026-11-07): legt das Muster in „Meins“ an
 * ([onSave]); danach ist es ein bestehendes Muster, die Diskette verschwindet und der Kopf zeigt
 * „· gespeichert“ ([NameTitle], dort die eine Ansage). Ein Zeichen, eine Bedeutung (ICON-03): ◉
 * bleibt „zu mir“ („Tut mir gut“, Muster des Tages), Speichern ist die Diskette (`ic_save`). Gleiche
 * Tönung wie der Startpunkt daneben (LAYOUT-04). [onFocusChanged] meldet den Tastatur-Fokus, damit
 * der Aufrufer ihn weitergibt, wenn die Diskette verschwindet (A11Y-07).
 */
@Composable
internal fun SaveButton(onSave: () -> Unit, onFocusChanged: (Boolean) -> Unit) {
    AppIconButton(onClick = onSave, modifier = Modifier.onFocusChanged { onFocusChanged(it.hasFocus) }) {
        Icon(
            painterResource(R.drawable.ic_save),
            contentDescription = stringResource(R.string.cd_pattern_editor_save),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        )
    }
}

/**
 * Seitentitel (Nachtrag 7): der Name des Musters groß (`headlineSmall`), darunter klein „Eigenes
 * Muster“ als Einordnung, bei einem bestehenden Muster mit dem Status „· gespeichert“ (Anatomie
 * Titel · Status, [TitleWithStatus]; MUSTER-14): Er steht, wenn der Entwurf dem geschriebenen Stand
 * entspricht, und fehlt, solange ein Schreiben aussteht – sein Platz bleibt reserviert, damit
 * nichts springt (im Entwurf gibt es ihn nicht; er kommt mit dem Speichern, wenn die Diskette geht). TalkBack hört ihn als Zustand des Titels und genau einmal je
 * Sitzung als Ansage, beim ersten Schreiben ([PatternEditorState.announceSaved], A11Y-04).
 * „intensiv“ steht seit 2026-10-23 rechts in der Zeile der Werte über der
 * Kurve ([InfoLine]), der Titel bekommt die ganze Breite vor Startpunkt und Diskette. Der
 * Name steht in einer Zeile und läuft als Laufschrift, wenn er nicht passt – nie umbrochen: Der Kopf
 * behält seine Höhe, auch wenn der Vorschlagsname mit den Zahlen wächst. Ein kleiner Stift direkt
 * hinter dem Namen zeigt, dass er änderbar ist (Nutzer 2026-10-23); er bleibt stehen, der Name
 * bekommt die Restbreite. Antippen des ganzen Titels benennt um ([onRename]); TalkBack: „Name: …,
 * umbenennen“ als Überschrift, der Stift ist dekorativ.
 */
@Composable
internal fun NameTitle(state: PatternEditorState, onRename: () -> Unit) {
    val name = state.savedName
    val description = stringResource(R.string.cd_pattern_name_rename, name)
    val savedState = stringResource(R.string.state_pattern_saved)
    val saved = state.saved
    val announce = state.announceSaved
    val colors = MaterialTheme.colorScheme
    val subtitleColor = colors.onBackground.copy(alpha = SECONDARY)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .semantics {
                heading()
                contentDescription = description
                if (saved) stateDescription = savedState
                if (announce) liveRegion = LiveRegionMode.Polite
            }
            .clickable(role = Role.Button, onClick = onRename),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Für TalkBack unsichtbar (der Titel sagt „Name: …, umbenennen“); nicht clearAndSetSemantics,
        // damit die Umbruch- und Zuschnittprüfung die Text-Layouts sieht.
        Column(Modifier.weight(1f).semantics { hideFromAccessibility() }) {
            NameWithPencil(name)
            TitleWithStatus(
                title = stringResource(R.string.pattern_editor_title),
                color = subtitleColor,
                // Im Entwurf kein Status und kein reservierter Platz: Bei großer Schrift stünde er
                // sonst unsichtbar unter dem Titel und machte den Kopf höher (MUSTER-10, ohne Scrollen).
                status = if (state.savedId != null || state.savePending) stringResource(R.string.pattern_editor_saved_status) else null,
                statusDescription = null,
                statusStyle = MaterialTheme.typography.labelMedium,
                titleStyle = MaterialTheme.typography.labelMedium,
                isHeading = false,
                statusVisible = saved,
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
 * höchstens [PatternEditorState.NAME_MAX] Zeichen; es ändert den Namen sofort (ein bestehendes
 * Muster speichert ihn wie jede Änderung). Leer steht der Vorschlagsname als Platzhalter da – so wird
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
 * „Muster speichern?“ beim Verlassen eines geänderten Entwurfs (MUSTER-14): M3-Basisdialog wie
 * [RenameDialog], die Knöpfe nach ihrer Wirkung benannt – „Speichern“ legt das Muster in „Meins“ an
 * und schließt den Editor, „Verwerfen“ schließt ohne Speichern. Wegtippen und Zurück schließen nur
 * den Dialog: Man bearbeitet weiter.
 */
@Composable
internal fun LeaveDialog(onSave: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pattern_editor_leave_title)) },
        text = { Text(stringResource(R.string.pattern_editor_leave_text)) },
        confirmButton = { AppTextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { AppTextButton(onClick = onDiscard) { Text(stringResource(R.string.action_discard)) } },
    )
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
