package app.atemkraft.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.PhaseType
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.components.BreathingCircle
import app.atemkraft.ui.components.CircleTapTarget
import app.atemkraft.ui.components.DotSeparatedFlow
import app.atemkraft.ui.components.FinishedPanel
import app.atemkraft.ui.components.GlowText
import app.atemkraft.ui.components.HideableSlot
import app.atemkraft.ui.components.PauseFlash
import app.atemkraft.ui.components.SessionControlBar
import app.atemkraft.ui.components.SessionRunningLayout
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.breathingDiscDiameter
import app.atemkraft.ui.components.expandedStateText
import app.atemkraft.ui.components.rememberTapFlash
import app.atemkraft.ui.components.sessionSummaryParts
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SessionNoteAmber
import app.atemkraft.ui.theme.SessionTextYellow
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import kotlin.math.ceil

/**
 * Zeigt die laufende Session: Start-Countdown, Atemkreis mit Fortschrittsbogen, im Kreis nur
 * Phasenwort und Sekunden, darunter fest die Anleitungszeile, Steuerung und – optional, dezent –
 * die kommende Phase. Am Ende „✓ Geschafft“, die offene Frage und eine kurze Bilanz.
 * [controlsVisible] = false ist die Ruhe-Ansicht ([SessionOverlay]): alles außer Kreis, Wort,
 * Sekunden und Bogen blendet aus, ohne dass der Kreis springt.
 * [onTapHaptic] spielt den Tipp-Tick beim Antippen des Kreises; ob er vibriert, entscheidet
 * der Aufrufer anhand des App-Schalters „Vibration“.
 */
@Composable
fun SessionScreen(
    state: SessionUiState,
    showNextPhase: Boolean,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    controlsVisible: Boolean = true,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state.status == SessionStatus.FINISHED) {
            FinishedPanel(
                title = stringResource(R.string.session_done_question),
                onAgain = onRestart,
                onExit = onStop,
                summary = state.summary?.let { sessionSummaryParts(it.durationMs, rounds = it.rounds, breaths = it.breaths) }.orEmpty(),
            )
        } else {
            ActiveContent(
                state = state,
                showNextPhase = showNextPhase,
                controlsVisible = controlsVisible,
                onTogglePause = onTogglePause,
                onTapHaptic = onTapHaptic,
                onContinue = onContinue,
                onRestart = onRestart,
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun ActiveContent(
    state: SessionUiState,
    showNextPhase: Boolean,
    controlsVisible: Boolean,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
) {
    val preparing = state.status == SessionStatus.PREPARING
    val waiting = state.status == SessionStatus.WAITING_FOR_USER
    val paused = state.status == SessionStatus.PAUSED
    val pausedText = stringResource(R.string.session_paused)

    val label = if (preparing) {
        stringResource(R.string.session_get_ready)
    } else {
        phaseDisplayLabel(state.phaseType, state.phaseLabel)
    }
    val fraction = circleFraction(state)
    val secondsText = if (preparing) state.countdown.toString() else secondsText(state)
    val note = if (preparing) null else state.phaseNote

    // Tap-Flash (geteilt mit der Meditation): großes Pause/Play-Symbol beim Antippen; die
    // UI-Haptik spielt der Player im AppContainer (AUDIO-04, -05).
    val tapFlash = rememberTapFlash()

    // Gerüst teilt sich die Atem-Session mit der Meditation: Kopf oben, Kreis im freien Platz
    // dazwischen, Steuerung unten (SessionRunningLayout). Die ausgeklappte Anleitung legt sich
    // über den Kreisrand, statt ihn zu verschieben.
    // Unter dem Kreis fest reserviert (LAYOUT-03), damit weder der Kreis noch „Als Nächstes“
    // springt, wenn Zeilen erscheinen oder wechseln: die Anleitungszeile mit zwei Zeilen ihres
    // Stils, die „Als Nächstes“-Zeile mit einer. Echt gemessen statt lineHeight × n: Ab Android 14
    // wächst große Schrift nichtlinear, die gerenderte Zeile ist dann höher als ihr lineHeight.
    val guidanceStyle = MaterialTheme.typography.bodySmall.copy(lineBreak = LineBreak.Heading)
    val nextStyle = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val guidanceHeight = with(density) { measurer.measure(TWO_LINES, guidanceStyle).size.height.toDp() }
    val nextRowHeight = with(density) { measurer.measure(ONE_LINE, nextStyle).size.height.toDp() }
    val nextBlock = if (showNextPhase) Dimens.GapSmall + nextRowHeight else 0.dp
    SessionRunningLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SessionPadding),
        centerExtra = Dimens.GapSmall + guidanceHeight + nextBlock,
        chromeVisible = controlsVisible,
        top = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SessionTitle(state)
                // Anleitung auf Abruf: dezente „ⓘ Anleitung"-Zeile unter dem Titel, die den
                // Pattern-Hint ein-/ausklappt – jederzeit, nicht nur im Countdown.
                state.patternHint?.let { hint ->
                    var hintVisible by rememberSaveable { mutableStateOf(false) }
                    // In der Ruhe-Ansicht zuklappen: Aufgeklappt läge der unsichtbare Text über dem
                    // Kreisrand und finge dort den Tipp ab, der pausieren soll.
                    LaunchedEffect(controlsVisible) { if (!controlsVisible) hintVisible = false }
                    val hintColor = MaterialTheme.colorScheme.onBackground.copy(alpha = TERTIARY)
                    val hintState = expandedStateText(hintVisible)
                    // Optisch leise wie bisher; die Ripple (Pillenform) zeigt Tipp und Tastatur-Fokus.
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, interactionSource = null, indication = ripple()) { hintVisible = !hintVisible }
                            .semantics {
                                stateDescription = hintState
                            }
                            // 48-dp-Mindest-Touch-Target (Bedienung mitten in der Session).
                            .heightIn(min = Dimens.MinTouchTarget)
                            .padding(horizontal = Dimens.CardPadding, vertical = Dimens.GapTiny),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null, // dekorativ: „Anleitung“ daneben benennt die Zeile
                            tint = hintColor,
                            modifier = Modifier.size(Sizes.IconSmall),
                        )
                        Spacer(Modifier.width(Dimens.GapTiny))
                        Text(
                            text = stringResource(R.string.detail_instruction),
                            style = MaterialTheme.typography.labelSmall,
                            color = hintColor,
                        )
                    }
                    AnimatedVisibility(
                        visible = hintVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                            modifier = Modifier.padding(top = Dimens.GapHairline),
                        )
                    }
                }
            }
        },
        bottom = {
            // Im offenen Hold wird Pause zu „Weiter“ (wie der Kreis-Tap, MUSTER-07); im
            // Countdown bleibt nur „Beenden“, die anderen halten ihren Platz (kein Sprung).
            SessionControlBar(
                playing = state.status == SessionStatus.RUNNING,
                onMain = if (waiting) onContinue else onTogglePause,
                onRestart = onRestart,
                onStop = onStop,
                preparing = preparing,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { side ->
        val circleDescription = stringResource(R.string.cd_breathing_circle)
        val circleAction = when (state.status) {
            SessionStatus.RUNNING -> stringResource(R.string.circle_action_pause)
            SessionStatus.PAUSED -> stringResource(R.string.circle_action_resume)
            SessionStatus.WAITING_FOR_USER -> stringResource(R.string.circle_action_continue)
            else -> null
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                BreathingCircle(fraction = fraction, progress = state.progress, modifier = Modifier.size(side))
                // Tippfläche ist der Kreis bis zur Spur: Ecken und Rand daneben gehören zur
                // Ruhe-Ansicht (SessionOverlay). Laufend/pausiert = Pause/Weiter, offener Hold =
                // weiter; im Countdown tut der Tipp nichts, fällt aber auch nicht zur Ruhe-Ansicht
                // durch und blendet „Beenden“ nicht aus.
                CircleTapTarget(
                    side = side,
                    description = circleDescription,
                    clickLabel = circleAction,
                    onTap = when (state.status) {
                        SessionStatus.RUNNING, SessionStatus.PAUSED -> {
                            { tapFlash.toggle(running = state.status == SessionStatus.RUNNING, haptic = onTapHaptic, toggle = onTogglePause) }
                        }

                        SessionStatus.WAITING_FOR_USER -> onContinue

                        else -> null
                    },
                    swallowWhenIdle = true,
                )
                // Im Kreis nur Phasenwort und Sekunden, als Geschwister ÜBER dem Kreis statt in
                // ihm: Sie bekommen die volle Breite und brechen nie im Wort, auch wenn der Kreis
                // bei großer Schrift klein wird. Antippen erreicht weiterhin den Kreis.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // LiveRegion sagt Phasenname + Anleitung an (der Kreis selbst hat nur eine
                    // statische Beschreibung, sonst liest TalkBack den Phasennamen doppelt). Die
                    // Pause steht davor („Pausiert, Einatmen“): Auch ein Doppeltipp auf den Kreis
                    // wird so angesagt, ohne eine zweite Live-Region (A11Y-04).
                    val phaseText = note?.let { "$label, $it" } ?: label
                    val liveText = if (paused) "$pausedText, $phaseText" else phaseText
                    // Im Countdown kleiner und nie breiter als die Scheibe: „Gleich geht's los“ ist
                    // länger als jedes Phasenwort und bricht bei großer Schrift zwischen den Wörtern um,
                    // statt über den Kreis hinauszuragen.
                    GlowText(
                        text = label,
                        style = if (preparing) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fill = SessionTextYellow,
                        modifier = Modifier
                            .then(if (preparing) Modifier.widthIn(max = breathingDiscDiameter(side, fraction)) else Modifier)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = liveText
                            },
                    )
                    if (secondsText.isNotEmpty()) {
                        Spacer(Modifier.height(Dimens.GapTiny))
                        GlowText(
                            text = secondsText,
                            style = MaterialTheme.typography.displaySmall,
                            fill = SessionTextYellow,
                        )
                    }
                }
                // Flash-Overlay ÜBER dem Kreis (Geschwister, nicht im Kreis-Content) → sicher sichtbar.
                PauseFlash(alpha = tapFlash.alpha, isPause = tapFlash.isPause, maxSize = side)
            }
            Spacer(Modifier.height(Dimens.GapSmall))
            // Anleitungszeile fest unter dem Kreis; im offenen Hold steht hier der Hinweis zum
            // Weiteratmen – der bleibt auch in der Ruhe-Ansicht, sonst wüsste man nicht weiter.
            HideableSlot(visible = controlsVisible || waiting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = guidanceHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val guidance = if (waiting) stringResource(R.string.session_tap_to_continue) else note
                    guidance?.let {
                        WholeWordText(
                            text = it,
                            style = guidanceStyle,
                            color = SessionNoteAmber.copy(alpha = SECONDARY),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            // Dezent darunter: die kommende Phase (abschaltbar). Feste Höhe reservieren, damit
            // der Kreis NICHT springt, wenn die Zeile erscheint/verschwindet oder der Text wechselt.
            if (showNextPhase) {
                Spacer(Modifier.height(Dimens.GapSmall))
                HideableSlot(visible = controlsVisible) {
                    NextPhaseRow(
                        nextPhaseType = if (preparing) null else state.nextPhaseType,
                        minHeight = nextRowHeight,
                    )
                }
            }
        }
    }
}

/**
 * Kopf: Übungsname, die Runde als Zusatz dahinter („Resonanz-Atmung · Runde 3 von 8“). Reicht die
 * Breite nicht, rutscht die Runde als Ganzes in die nächste Zeile, ohne Trennpunkt am Umbruch
 * ([DotSeparatedFlow], LAYOUT-03). Die Runde steht schon im Countdown und ist so breit wie ihr
 * längster Wert („Runde 8 von 8“): Der Kopf ändert seine Höhe während der Session nie, sonst
 * rechnet SessionRunningLayout mit dem niedrigeren Kopf und der Kreis rückt hinein.
 */
@Composable
private fun SessionTitle(state: SessionUiState) {
    val titleStyle = MaterialTheme.typography.titleSmall
    val roundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY)
    val name: @Composable () -> Unit = {
        WholeWordText(
            text = state.exerciseName,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
    if (state.roundCount <= 1) {
        name()
        return
    }
    val widest = stringResource(R.string.session_round, state.roundCount, state.roundCount)
    val widestWidth = with(LocalDensity.current) { rememberTextMeasurer().measure(widest, titleStyle).size.width.toDp() }
    val round = stringResource(R.string.session_round, state.roundIndex + 1, state.roundCount)
    DotSeparatedFlow(
        parts = listOf(
            name,
            {
                WholeWordText(
                    text = round,
                    modifier = Modifier.widthIn(min = widestWidth),
                    style = titleStyle,
                    color = roundColor,
                    textAlign = TextAlign.Center,
                )
            },
        ),
        separator = { Text(text = DOT_SEPARATOR, style = titleStyle, color = roundColor) },
    )
}

/**
 * Dezente „Als Nächstes · Phase“-Zeile unter dem Kreis; leer (mit Höhe), wenn nichts folgt. Als
 * Ganzes mittig wie die Anleitungszeile darüber; die Höhe steht fest, nur die Breite folgt dem
 * Phasennamen.
 */
@Composable
private fun NextPhaseRow(nextPhaseType: PhaseType?, minHeight: Dp) {
    val hintColor = MaterialTheme.colorScheme.onBackground.copy(alpha = TERTIARY)
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (nextPhaseType != null) {
            // Bei sehr großer Schrift auf schmalem Gerät bricht die Zeile zwischen Wörtern um,
            // ganz statt abgeschnitten (LAYOUT-03).
            WholeWordText(
                text = stringResource(R.string.session_next) + DOT_SEPARATOR + phaseDisplayLabel(nextPhaseType, null),
                style = MaterialTheme.typography.labelSmall,
                color = hintColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Messtexte für die reservierten Höhen: zwei Zeilen Anleitung (Hinweise sind kurz), eine „Als Nächstes“. */
private const val TWO_LINES = "A\nA"
private const val ONE_LINE = "A"

/** Sekundentext: Countdown bei fester Dauer, Hochzählen bei offener Phase. */
private fun secondsText(state: SessionUiState): String = when {
    state.status == SessionStatus.WAITING_FOR_USER -> (state.elapsedMs / 1000L).toString()
    state.phaseTotalMs > 0L -> ceil(state.remainingMs / 1000.0).toInt().coerceAtLeast(0).toString()
    else -> ""
}
