package app.atemkraft.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import app.atemkraft.R
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreeTemplate
import app.atemkraft.domain.breathRhythm
import app.atemkraft.ui.components.AppBottomSheet
import app.atemkraft.ui.components.AppBottomSheetScope
import app.atemkraft.ui.components.MiniCurve
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.bleed
import app.atemkraft.ui.components.selectedRow
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.TonalDeep

/**
 * Vorlagen als Bottom Sheet (Nachtrag 6; wann ein Bottom Sheet passt: MUSTER-11, wie „Klang &
 * Stimme“ in der Meditation, `SoundSheet`): „Startpunkt“ und je Vorlage eine Zeile mit Name und
 * Mini-Kurve ([MiniCurve]), die passende markiert (`selectable`, Rolle RadioButton, Fläche
 * `TonalDeep`, Rand `SegmentSelected` wie in der Ablauf-Liste); unter dem Titel ein Satz,
 * was die Wahl tut (Nutzer 2026-10-24), als Intro des Titels. Eine Wahl setzt die Folge (letzte
 * Phase gewählt) und schließt das Sheet ([AppBottomSheetScope.closeThen]); danach meldet es
 * [onPick]. Gerüst: [AppBottomSheet].
 */
@Composable
internal fun TemplateSheet(state: PatternEditorState, onPick: () -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(
        title = stringResource(R.string.pattern_editor_start),
        onDismiss = onDismiss,
        intro = stringResource(R.string.pattern_editor_start_hint),
    ) {
        val sheet = this
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline)) {
            FreePatternTemplates.all.forEach { template ->
                TemplateRow(template, selected = template == state.template) {
                    state.applyTemplate(template)
                    // Erst nach dem Schließen: Der Fokus geht zurück auf den Startpunkt im Kopf.
                    sheet.closeThen(onPick)
                }
            }
        }
    }
}

/**
 * Eine Vorlage im [TemplateSheet]: Name links, Mini-Kurve rechts; die passende markiert. Der Name
 * steht an der Textkante des Blatts wie der Titel (Anatomie „Bottom Sheet“, §4.5); die Fläche der
 * Auswahl ragt um ihr Polster ([Dimens.GapSmall], wie die Ablauf-Liste des Editors) in den Rand.
 */
@Composable
private fun TemplateRow(template: FreeTemplate, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = Modifier
            .bleed(start = Dimens.GapSmall, end = Dimens.GapSmall)
            .fillMaxWidth()
            .selectedRow(selected, shape, borderColor = SegmentSelected, fill = TonalDeep)
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        WholeWordText(text = template.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        MiniCurve(remember(template) { template.pattern.breathRhythm() })
    }
}
