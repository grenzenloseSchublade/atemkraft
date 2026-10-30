package app.atemkraft.ui.settings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.Refs
import app.atemkraft.data.ReminderSettings
import app.atemkraft.data.SessionDisplay
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.GONG_INTERVAL_CHOICES
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.domain.VoiceCatalog
import app.atemkraft.domain.VoiceSpec
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.CardHeader
import app.atemkraft.ui.components.ConfirmIconButton
import app.atemkraft.ui.components.DisclosureToggle
import app.atemkraft.ui.components.FilterChipFlow
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.NavCard
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.Stepper
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.TonalChip
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.DividerSubtle
import app.atemkraft.ui.theme.SECONDARY
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek

/**
 * Einstellungen: Klang (beide Sitzungsarten), Atemübung (Ton, Vibration, „Danach in die Stille“,
 * Sicherheitshinweis), Anzeige der Atemübung, Meditation, Erinnerung und Daten, unten Über. Atemlehre und
 * Begriffe stehen seit 2026-10-23 im Tab „Lernen“.
 *
 * Texte nach einem Muster (STYLEGUIDE TEXT-15): Kartentitel, optional ein Satz, wofür die Karte
 * gilt; je Einstellung der Name und darunter höchstens ein erklärender Satz, nur wenn er etwas
 * hinzufügt; Auswahlreihen mit Unterüberschrift darüber und dem Satz darunter. Nichts doppelt.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    transfer: PatternTransfer = PatternTransfer.Idle,
    focusMeditation: Boolean = false,
    showVoices: Boolean = false,
) {
    val scrollState = rememberScrollState()
    val columns = rememberTrailingColumns()
    // Vom Meditations-Tab aus geöffnet: einmal direkt zur Meditations-Karte springen. Gemerkt
    // (rememberSaveable), damit die Rückkehr aus Glossar/Über die Scroll-Position nicht überschreibt.
    var meditationCardY by remember { mutableIntStateOf(-1) }
    val topMarginPx = with(LocalDensity.current) { Dimens.CardPadding.roundToPx() }
    var focusDone by rememberSaveable { mutableStateOf(!focusMeditation) }
    LaunchedEffect(focusDone) {
        if (focusDone) return@LaunchedEffect
        val y = snapshotFlow { meditationCardY }.first { it >= 0 }
        // Etwas Luft über der Karte, damit sie nicht an der Oberkante klebt.
        scrollState.scrollTo((y - topMarginPx).coerceAtLeast(0))
        focusDone = true
    }
    // Nach einer beschädigten Einstellungsdatei einmal melden: Der Hinweis bleibt für diesen
    // Besuch der Seite stehen (auch beim Drehen), die Marke gilt sofort als gemeldet.
    var showResetNotice by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.settingsResetPending) {
        if (state.settingsResetPending) {
            showResetNotice = true
            actions.acknowledgeSettingsReset()
        }
    }
    // Kopf fest über der Scroll-Fläche: Nach dem Sprung zur Meditations-Karte bliebe der Pfeil
    // sonst außerhalb des Bildes (einziger sichtbarer Weg zurück).
    PushScaffold(onBack = onBack, title = stringResource(R.string.settings_title), scrollState = scrollState) {
        if (showResetNotice) {
            Card(modifier = Modifier.fillMaxWidth()) {
                BodyText(stringResource(R.string.settings_reset_notice), modifier = Modifier.padding(Dimens.CardPadding))
            }
            Spacer(Modifier.height(Dimens.SectionGap))
        }

        // Die Kartentitel sind die Köpfe dieser Seite und gehen reihum durch den Ring, ohne
        // Bedeutung (FARBE-07): Klang 0, Atemübung 1, Anzeige der Atemübung 2, Meditation 3,
        // Daten 4.
        TonCard(state.volume, actions::setVolume, state.gongLong, actions::setGongLong, actions::previewGong)

        Spacer(Modifier.height(Dimens.ListGap))
        BreathCard(state, actions)

        Spacer(Modifier.height(Dimens.ListGap))
        SessionDisplayCard(state.sessionDisplay, actions)

        Spacer(Modifier.height(Dimens.ListGap))
        // Box nur als Messpunkt für den Sprung aus dem Meditations-Tab.
        Box(Modifier.onPlaced { meditationCardY = it.positionInParent().y.toInt() }) {
            MeditationCard(
                state.voiceStates, state.activeVoiceId, state.piperEngineReady,
                actions::sampleVoice, actions::downloadVoice, actions::selectVoice, actions::deleteVoice,
                columns,
                catalogInitiallyExpanded = showVoices,
                display = { MeditationDisplayRows(state.meditationDisplay, actions) },
            )
        }

        Spacer(Modifier.height(Dimens.ListGap))
        ReminderCard(state.reminder, actions)

        Spacer(Modifier.height(Dimens.ListGap))
        DataCard(transfer, state, actions)

        Spacer(Modifier.height(Dimens.ListGap))
        NavCard(title = stringResource(R.string.settings_about_entry), onClick = onOpenAbout)

        Spacer(Modifier.height(Dimens.SectionGap))
        HintText(stringResource(R.string.settings_about))
    }
}

/**
 * Daten: die Sicherung „alles“ (Kreationen, Logbuch samt Befinden, „Tut mir gut“, Einstellungen)
 * als Datei schreiben und wieder einlesen, mit Vorschau vor dem Übernehmen (Storage Access
 * Framework, keine Berechtigung, keine Cloud), der Handywechsel als Aufklapper; unten die
 * Zahl der Logbuch-Sitzungen mit Papierkorb, zweistufig (MUSTER-05), und der Schalter „Serie
 * zeigen“ (Tage in Folge in Verlauf und Logbuch; Auswertung 2026-10-24 – passt zu „Atem ist keine
 * Zahl“: wer die Zählung nicht will, schaltet sie ab) und „Befinden am Ende fragen“ (das Wort am
 * Eintrag, SEC-LEGAL-04; „Logbuch leeren“ löscht es mit).
 */
@Composable
private fun DataCard(transfer: PatternTransfer, state: SettingsUiState, actions: SettingsActions) {
    var transferInfoExpanded by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.settings_data_title), intro = stringResource(R.string.settings_data_intro)) {
        SubLabel(stringResource(R.string.settings_data_backup))
        Spacer(Modifier.height(Dimens.GapSmall))
        // Kurze Labels, damit beide gleich breit nebeneinander passen; worum es geht, sagt die
        // Unterüberschrift darüber. Passen sie nicht, stehen sie untereinander (LAYOUT-03).
        AdaptiveButtonRow(modifier = Modifier.fillMaxWidth(), spacing = Dimens.GapSmall) {
            DataButton(transfer.exportLabel ?: stringResource(R.string.settings_data_export), transfer.export)
            DataButton(transfer.importLabel ?: stringResource(R.string.settings_data_import), transfer.import)
        }
        if (transfer.note != null) {
            Spacer(Modifier.height(Dimens.GapTiny))
            Text(
                text = transfer.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        transfer.preview?.let { BackupPreviewBlock(it, transfer) }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(stringResource(R.string.settings_data_hint))
        // Wie „Wie funktioniert das?“ in der Atmen-Karte: Der lange Hinweis steht erst auf Wunsch da.
        DisclosureToggle(
            text = stringResource(R.string.settings_data_transfer_more),
            expanded = transferInfoExpanded,
            onToggle = { transferInfoExpanded = !transferInfoExpanded },
        )
        if (transferInfoExpanded) BodyText(stringResource(R.string.settings_data_transfer_hint))
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        LogRow(state.logCount, actions::clearLog)
        ToggleRow(
            label = stringResource(R.string.settings_show_streak),
            checked = state.showStreak,
            onCheckedChange = actions::setShowStreak,
            hint = stringResource(R.string.settings_show_streak_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_ask_feeling),
            checked = state.askFeeling,
            onCheckedChange = actions::setAskFeeling,
            hint = stringResource(R.string.settings_ask_feeling_hint),
        )
    }
}

/**
 * Erinnerung (Auswertung B, Nutzer 2026-10-24): aus ab Werk; an mit Uhrzeit (Stepper in
 * Viertelstunden) und Wochentagen (Filter-Chips, mindestens einer bleibt). Beim Einschalten fragt
 * Android ab 13 einmal nach der Benachrichtigung – im Moment, in dem sie Sinn ergibt; abgelehnt oder
 * in den Systemeinstellungen aus, sagt der Hinweis das, statt still nichts zu tun. Keine Serie,
 * kein Zähler, kein Druck: Die Benachrichtigung fragt nur „Zeit für ein paar Atemzüge?“.
 */
@Composable
private fun ReminderCard(reminder: ReminderSettings, actions: SettingsActions) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = notificationsAllowed(context) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = notificationsAllowed(context) }
    SettingsCard(stringResource(R.string.settings_reminder_title), titleGap = Dimens.GapTiny) {
        ToggleRow(
            label = stringResource(R.string.settings_reminder_toggle),
            checked = reminder.enabled,
            onCheckedChange = { on ->
                actions.setReminderEnabled(on)
                if (on && !allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            hint = stringResource(if (reminder.enabled && !allowed) R.string.settings_reminder_blocked else R.string.settings_reminder_hint),
        )
        if (reminder.enabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
            val step = ReminderSettings.STEP_MINUTES
            val hour = reminder.minuteOfDay / 60
            val minute = reminder.minuteOfDay % 60
            Stepper(
                label = stringResource(R.string.settings_reminder_time),
                value = reminder.minuteOfDay / step,
                range = 0 until ReminderSettings.MINUTES_PER_DAY / step,
                onChange = { actions.setReminderMinute(it * step) },
                horizontal = 0.dp,
                vertical = 0.dp,
                valueText = stringResource(R.string.reminder_time_value, hour, minute),
                spokenValue = stringResource(R.string.cd_reminder_time, hour, minute),
            )
            Spacer(Modifier.height(Dimens.GapSmall))
            val days = stringResource(R.string.settings_reminder_days)
            SubLabel(days)
            Spacer(Modifier.height(Dimens.GapSmall))
            val short = stringArrayResource(R.array.log_weekday_short)
            val long = stringArrayResource(R.array.reminder_weekday_long)
            FilterChipFlow(groupName = days, items = DayOfWeek.entries) { day ->
                val selected = day in reminder.days
                val index = day.value - 1
                TonalChip(
                    label = short[index],
                    selected = selected,
                    // Der letzte gewählte Tag bleibt: ohne Tag gäbe es keine Erinnerung, dafür ist der Schalter da.
                    onClick = {
                        if (!selected) {
                            actions.setReminderDays(reminder.days + day)
                        } else if (reminder.days.size > 1) {
                            actions.setReminderDays(reminder.days - day)
                        }
                    },
                    contentDescription = long[index],
                )
            }
        }
    }
}

/** Darf die App Benachrichtigungen zeigen (Erlaubnis ab Android 13 und Schalter in den Systemeinstellungen)? */
private fun notificationsAllowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

/**
 * Vorschau einer gelesenen Sicherung, bevor etwas geschrieben wird: was darin ist (Kreationen,
 * Logbuch-Einträge, Markierungen, Einstellungen), dass zusammengeführt wird, auf Wunsch
 * „Einstellungen ersetzen“ (Standard aus), dann „Übernehmen“ und „Abbrechen“ als gleichrangige
 * Knöpfe. Ein Satz statt einer Tabelle; angesagt wird er als Live-Region, wenn er erscheint.
 */
@Composable
private fun BackupPreviewBlock(preview: BackupPreview, transfer: PatternTransfer) {
    val parts = buildList {
        if (preview.patterns > 0) add(pluralStringResource(R.plurals.data_preview_patterns, preview.patterns, preview.patterns))
        if (preview.logbook > 0) add(pluralStringResource(R.plurals.data_preview_logbook, preview.logbook, preview.logbook))
        if (preview.marked > 0) add(pluralStringResource(R.plurals.data_preview_marked, preview.marked, preview.marked))
        if (preview.hasSettings) add(stringResource(R.string.data_preview_settings))
    }
    Column(modifier = Modifier.padding(top = Dimens.GapSmall)) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            SubLabel(stringResource(R.string.data_preview_title))
            BodyText(parts.joinToString(DOT_SEPARATOR))
            HintText(stringResource(R.string.data_preview_merge))
        }
        if (preview.hasSettings) {
            ToggleRow(
                label = stringResource(R.string.data_import_settings),
                checked = transfer.withSettings,
                onCheckedChange = transfer.toggleSettings,
                hint = stringResource(R.string.data_import_settings_hint),
            )
        }
        Spacer(Modifier.height(Dimens.GapSmall))
        AdaptiveButtonRow(modifier = Modifier.fillMaxWidth(), spacing = Dimens.GapSmall) {
            AppOutlinedButton(onClick = transfer.confirm) { Text(stringResource(R.string.data_import_apply)) }
            AppOutlinedButton(onClick = transfer.cancel) { Text(stringResource(R.string.data_import_cancel)) }
        }
    }
}

/**
 * Zeile „Logbuch“: links Titel und Zahl der Sitzungen, rechts in derselben Icon-Spalte wie bei
 * den Stimmen der Papierkorb. Leeren zweistufig im selben Element (MUSTER-05,
 * [ConfirmIconButton]: „Logbuch wirklich leeren?“). Ohne Einträge gibt es nichts zu leeren – dann
 * fehlt der Papierkorb, die Zeile behält ihre Höhe. Geöffnet wird das Logbuch im Tab „Eigene“
 * (Abschnitt „Logbuch“, Nutzer 2026-10-23) und am Ende einer Übung, nicht hier.
 */
@Composable
private fun LogRow(count: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = Dimens.GapTiny)) {
            WholeWordText(
                text = stringResource(R.string.settings_log_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HintText(
                if (count > 0) pluralStringResource(R.plurals.log_sessions, count, count) else stringResource(R.string.settings_log_empty),
            )
        }
        if (count > 0) {
            TrailingIconCell {
                ConfirmIconButton(
                    name = stringResource(R.string.settings_log_title),
                    description = stringResource(R.string.settings_data_clear_log),
                    onConfirm = onClear,
                    confirmDescription = stringResource(R.string.cd_log_clear_confirm),
                )
            }
        }
    }
}

/** Umrandeter Button der Daten-Karte; das Label wechselt zum Ergebnis und wird angesagt. */
@Composable
private fun DataButton(label: String, onClick: () -> Unit) {
    AppOutlinedButton(onClick = onClick, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(label)
    }
}

/** Klang-Karte: gilt für beide Sitzungsarten (Atem-Töne, Schluss-Gong der Atemübungen, Meditations-Gongs). */
@Composable
private fun TonCard(
    volume: ToneVolume,
    onVolume: (ToneVolume) -> Unit,
    gongLong: Boolean,
    onGongLong: (Boolean) -> Unit,
    onPreviewGong: () -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_tone_title), intro = stringResource(R.string.settings_tone_intro)) {
        SubLabel(stringResource(R.string.volume_title), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow(ToneVolume.entries, volume, onVolume, groupLabel = stringResource(R.string.volume_title)) { v ->
            when (v) {
                ToneVolume.QUIET -> stringResource(R.string.volume_quiet)
                ToneVolume.MEDIUM -> stringResource(R.string.volume_medium)
                ToneVolume.LOUD -> stringResource(R.string.volume_loud)
            }
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(stringResource(R.string.volume_hint))

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        SubLabel(stringResource(R.string.settings_gong_length), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        // Drittel-Raster wie die übrigen Auswahlreihen: Kurz/Lang belegen zwei Drittel, das
        // Vorhören steht im freien dritten Platz auf Höhe der Felder (Nutzer 2026-10-20).
        // Spielt den Gong im aktuell gewählten Profil.
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedChoiceRow(
                listOf(false, true),
                gongLong,
                onGongLong,
                modifier = Modifier.weight(2f),
                groupLabel = stringResource(R.string.settings_gong_length),
            ) { long ->
                stringResource(if (long) R.string.gong_length_long else R.string.gong_length_short)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PreviewButton(stringResource(R.string.cd_gong_preview), onPreviewGong)
            }
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(stringResource(R.string.settings_gong_length_hint))
    }
}

/**
 * Atemübung: wie die Übung klingt und sich anfühlt (Ton, beim Dauerton die Wechsel-Betonung,
 * Vibration), was danach kommt („Danach in die Stille“, nur der Startwert) und ob der
 * Sicherheitshinweis vor intensiven Übungen jedes Mal erscheint. Der Satz unter „Ton“ beschreibt
 * die gewählte Option; „Wie funktioniert das?“ gehört zum Dauerton und steht nur bei ihm.
 */
@Composable
private fun BreathCard(state: SettingsUiState, actions: SettingsActions) {
    var soundInfoExpanded by rememberSaveable { mutableStateOf(false) }
    SettingsCard(stringResource(R.string.settings_breathing_title)) {
        SubLabel(stringResource(R.string.settings_sound_title), Modifier.choiceGroupHeading())
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow(SoundMode.entries, state.soundMode, actions::setSoundMode, groupLabel = stringResource(R.string.settings_sound_title)) { mode ->
            when (mode) {
                SoundMode.OFF -> stringResource(R.string.sound_mode_off)
                SoundMode.CUES -> stringResource(R.string.sound_mode_cues)
                SoundMode.CONTINUOUS -> stringResource(R.string.sound_mode_continuous)
            }
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        HintText(
            stringResource(
                when (state.soundMode) {
                    SoundMode.OFF -> R.string.sound_mode_off_hint
                    SoundMode.CUES -> R.string.sound_mode_cues_hint
                    SoundMode.CONTINUOUS -> R.string.sound_mode_continuous_hint
                },
            ),
        )
        if (state.soundMode == SoundMode.CONTINUOUS) {
            DisclosureToggle(
                text = stringResource(R.string.settings_sound_more),
                expanded = soundInfoExpanded,
                onToggle = { soundInfoExpanded = !soundInfoExpanded },
            )
            if (soundInfoExpanded) {
                // Aufgeklappt Fließtext wie im Expander; die Quellen folgen ohne Zwischenzeile –
                // der Umschalter benennt den Inhalt schon (wie die Detailseite).
                BodyText(stringResource(R.string.settings_sound_hint))
                Spacer(Modifier.height(Dimens.GapSmall))
                listOf(Refs.respeRate, Refs.shaffer2020, Refs.zaccaro2018).forEach { ref ->
                    ReferenceItem(ref)
                    Spacer(Modifier.height(Dimens.GapTiny))
                }
            }
            Spacer(Modifier.height(Dimens.GapSmall))
            SubLabel(stringResource(R.string.emphasis_title), Modifier.choiceGroupHeading())
            Spacer(Modifier.height(Dimens.GapSmall))
            SegmentedChoiceRow(
                TransitionEmphasis.entries,
                state.transition,
                actions::setTransition,
                groupLabel = stringResource(R.string.emphasis_title),
            ) { emphasis ->
                when (emphasis) {
                    TransitionEmphasis.SOFT -> stringResource(R.string.emphasis_soft)
                    TransitionEmphasis.MEDIUM -> stringResource(R.string.emphasis_medium)
                    TransitionEmphasis.STRONG -> stringResource(R.string.emphasis_strong)
                }
            }
            Spacer(Modifier.height(Dimens.GapTiny))
            HintText(stringResource(R.string.emphasis_hint))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_haptics),
            checked = state.haptics,
            onCheckedChange = actions::setHaptics,
            hint = stringResource(R.string.settings_haptics_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_silence_after),
            checked = state.sessionDisplay.silenceAfter,
            onCheckedChange = actions::setSilenceAfter,
            hint = stringResource(R.string.settings_silence_after_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_safety),
            checked = state.showSafetyWarning,
            onCheckedChange = actions::setShowSafetyWarning,
            hint = stringResource(R.string.settings_safety_hint),
        )
    }
}

/**
 * Was die laufende Atem-Session zeigt; alles abschaltbar für eine möglichst ruhige Ansicht. Die
 * Erklärungen stehen hier; die gleichnamigen Schalter der Meditation verweisen nur darauf.
 */
@Composable
private fun SessionDisplayCard(display: SessionDisplay, actions: SettingsActions) {
    SettingsCard(stringResource(R.string.settings_display_title), titleGap = Dimens.GapTiny) {
        ToggleRow(
            label = stringResource(R.string.settings_auto_calm),
            checked = display.autoCalm,
            onCheckedChange = actions::setAutoCalm,
            hint = stringResource(R.string.settings_auto_calm_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_progress),
            checked = display.showProgress,
            onCheckedChange = actions::setShowProgress,
            hint = stringResource(R.string.settings_progress_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_seconds),
            checked = display.showSeconds,
            onCheckedChange = actions::setShowSeconds,
            hint = stringResource(R.string.settings_seconds_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_next_phase),
            checked = display.showNextPhase,
            onCheckedChange = actions::setShowNextPhase,
            hint = stringResource(R.string.settings_next_phase_hint),
        )
    }
}

/**
 * Was die laufende Meditation zeigt; wie bei der Atemübung alles abschaltbar. Ausblenden und
 * Fortschrittsring wirken wie dort und werden nicht ein zweites Mal erklärt: Der Hinweis unter
 * „Automatisch ausblenden“ verweist auf die Atemübung, der Ring braucht keinen.
 */
@Composable
private fun MeditationDisplayRows(display: MeditationDisplay, actions: SettingsActions) {
    Column {
        ToggleRow(
            label = stringResource(R.string.settings_auto_calm),
            checked = display.autoCalm,
            onCheckedChange = actions::setMeditationAutoCalm,
            hint = stringResource(R.string.settings_med_auto_calm_hint),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_progress),
            checked = display.showProgress,
            onCheckedChange = actions::setMeditationShowProgress,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapTiny), color = DividerSubtle)
        ToggleRow(
            label = stringResource(R.string.settings_med_time),
            checked = display.showTime,
            onCheckedChange = actions::setMeditationShowTime,
            hint = stringResource(R.string.settings_med_time_hint),
        )
    }
}

/**
 * Meditation: was die laufende Meditation zeigt und die Stimme der Sprachanleitung. Den
 * Gong-Abstand wählt seit 2026-10-24 das Blatt „Klang & Stimme“ im Tab bei „alle … min“ – dort,
 * wo man ihn sucht. [catalogInitiallyExpanded]: aus „Stimme laden ›“ geöffnet, Katalog offen.
 */
@Composable
private fun MeditationCard(
    voiceStates: Map<String, VoiceDownloadState>,
    activeVoiceId: String?,
    piperEngineReady: Boolean,
    onSampleVoice: (VoiceSpec) -> Unit,
    onDownloadVoice: (String) -> Unit,
    onSelectVoice: (String?) -> Unit,
    onDeleteVoice: (String) -> Unit,
    columns: TrailingColumns,
    catalogInitiallyExpanded: Boolean,
    display: @Composable () -> Unit,
) {
    SettingsCard(stringResource(R.string.settings_meditation_title), intro = stringResource(R.string.settings_meditation_intro), titleGap = Dimens.GapTiny) {
        display()

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapSmall), color = DividerSubtle)
        // Zugeklappt nur die aktive Stimme; der Katalog klappt darunter auf (Umschalter wie „Wie
        // funktioniert das?“). Solange eine Stimme lädt, bleibt er offen, damit Fortschritt und
        // Abbrechen im Bild bleiben.
        var catalogExpanded by rememberSaveable { mutableStateOf(catalogInitiallyExpanded) }
        val downloading = voiceStates.values.any { it is VoiceDownloadState.Downloading }
        val showCatalog = catalogExpanded || downloading
        val active = VoiceCatalog.all.firstOrNull { it.id == activeVoiceId && voiceStates[it.id] is VoiceDownloadState.Downloaded }
        SubLabel(stringResource(R.string.settings_voice))
        // Bleibt auch aufgeklappt stehen: Verschwände die Zeile, spränge der Umschalter beim
        // Tippen eine Zeile nach oben, weg vom Finger (Nutzer 2026-10-20) – wie der Teaser von
        // „Klang & Stimme“ in der Meditation.
        HintText(
            when {
                active != null && piperEngineReady -> active.displayName

                active != null -> active.displayName + DOT_SEPARATOR + stringResource(R.string.settings_speech_pack_preparing)

                else -> stringResource(
                    if (voiceStates.values.any { it is VoiceDownloadState.Downloaded }) {
                        R.string.settings_voice_none_chosen
                    } else {
                        R.string.settings_voice_none_installed
                    },
                )
            },
        )
        DisclosureToggle(
            text = stringResource(if (showCatalog) R.string.settings_voice_hide else R.string.settings_voice_show),
            expanded = showCatalog,
            onToggle = { catalogExpanded = !showCatalog },
            enabled = !downloading,
        )
        if (showCatalog) {
            // Quelle vor dem ersten „Laden“ (SECURITY SEC-NET-02).
            HintText(stringResource(R.string.settings_voice_catalog_hint))
            Spacer(Modifier.height(Dimens.GapTiny))

            // Stimmen-Katalog: pro Stimme Vorhören → Laden → Wählen/Löschen. Mehrere behaltbar.
            VoiceCatalog.all.forEachIndexed { index, spec ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.GapHairline), color = DividerSubtle)
                // key: Der Bestätigungszustand des Löschens gehört zur Stimme, nicht zur Position.
                key(spec.id) {
                    VoiceRow(
                        spec = spec,
                        state = voiceStates[spec.id] ?: VoiceDownloadState.NotDownloaded,
                        active = spec.id == activeVoiceId,
                        engineReady = piperEngineReady,
                        columns = columns,
                        onSample = { onSampleVoice(spec) },
                        onDownload = { onDownloadVoice(spec.id) },
                        onSelect = { onSelectVoice(spec.id) },
                        onDelete = { onDeleteVoice(spec.id) },
                    )
                }
            }
        }
    }
}

/**
 * Eine Karte der Einstellungen: Titel über [CardHeader] (Anatomie §4.5, Überschrift für TalkBack) in
 * `onSurface` (FARBE-07: Köpfe weiß),
 * direkt darunter optional [intro] (ein Satz, wofür die Karte gilt – nur, wo er etwas hinzufügt,
 * wie der Hinweis einer [NavCard]), nach [titleGap] der Inhalt; Polster `CardPadding`. [titleGap]
 * ist `GapTiny`, wo der Inhalt mit einer Zeile beginnt, die sich selbst polstert (Schalter), sonst
 * `GapSmall`.
 */
@Composable
private fun SettingsCard(
    title: String,
    titleGap: Dp = Dimens.GapSmall,
    intro: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            CardHeader(title = title, heading = true)
            if (intro != null) HintText(intro)
            Spacer(Modifier.height(titleGap))
            content()
        }
    }
}
