package app.atemkraft.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.Feeling
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SessionButtonCyan
import app.atemkraft.ui.theme.SessionButtonPink
import app.atemkraft.ui.theme.SessionTextGlow
import app.atemkraft.ui.theme.SessionTextYellow
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

// Geteilte Bausteine der Vollbild-Sitzungen (Atem-Session UND Meditation): Glow-Schrift,
// die Session-Buttons in den festen Farb-Tokens, der Abschluss-Screen und der Tap-Flash.
// Zentral, damit beide Abläufe garantiert identisch aussehen und sich gemeinsam ändern.

/** Schrift mit weichem dunklen Schimmer (kein harter Rand) – ruhig und lesbar auf Magenta & Dunkel. */
@Composable
fun GlowText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    // Zentriert und ohne Wortbruch: Session-Texte stehen mittig über dem Kreis (LAYOUT-03).
    WholeWordText(
        text = text,
        style = style.merge(
            TextStyle(
                color = SessionTextYellow,
                shadow = Shadow(color = SessionTextGlow, offset = Offset.Zero, blurRadius = 18f),
            ),
        ),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

// Schmaler als der M3-Standard (24 dp): „Nochmal“ und „Ende“ passen so auch bei großer Schrift
// auf 360 dp nebeneinander. Sichtbare Höhe Sizes.ButtonHeight, Tippfläche weiter 48 dp.
private val SessionButtonPadding = PaddingValues(horizontal = Dimens.CardPadding, vertical = Dimens.GapSmall)

/** Gefüllter Primär-Button („Nochmal“ im [FinishedPanel]) im Session-Cyan. */
@Composable
fun SessionPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.ButtonHeight),
        contentPadding = SessionButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = SessionButtonCyan,
            contentColor = SessionTextGlow,
        ),
    ) { Text(text) }
}

/** Umrandeter Sekundär-Button („Ende“ im [FinishedPanel]) im Session-Cyan. */
@Composable
fun SessionSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.ButtonHeight),
        contentPadding = SessionButtonPadding,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SessionButtonCyan),
        border = BorderStroke(Dimens.BorderThin, SessionButtonCyan),
    ) { Text(text) }
}

/**
 * Steuerung der laufenden Sitzung (Atem-Session UND Meditation), Icon-only (PRIN-03): links
 * „Von vorne“ (tonal, Kreispfeil), Mitte Pause/Weiter (größer, gefüllt im Session-Cyan),
 * rechts „Beenden“ (tonal, Stopp in Session-Pink). Feste Größen ([Dimens.SessionMainButton] ff.). [playing] zeigt Pause, sonst Play mit dem
 * Namen „Weiter“ – für pausiert wie für den offenen Hold (MUSTER-07); was [onMain] dann tut,
 * entscheidet der Aufrufer. In der Vorbereitung ([preparing]) bleibt nur „Beenden“: die anderen
 * sind unsichtbar statt ausgegraut und behalten ihren Platz, damit beim Start nichts springt.
 */
@Composable
fun SessionControlBar(
    playing: Boolean,
    onMain: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    preparing: Boolean = false,
) {
    val tonal = MaterialTheme.colorScheme.secondaryContainer
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SessionControlGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionRoundButton(onClick = onRestart, size = Dimens.SessionSideButton, container = tonal, visible = !preparing) {
            Icon(
                painter = painterResource(R.drawable.ic_reset),
                contentDescription = stringResource(R.string.action_restart),
                tint = SessionButtonCyan,
                modifier = Modifier.size(Dimens.SessionSideIcon),
            )
        }
        SessionRoundButton(onClick = onMain, size = Dimens.SessionMainButton, container = SessionButtonCyan, visible = !preparing) {
            val name = stringResource(if (playing) R.string.action_pause else R.string.action_resume)
            val iconModifier = Modifier.size(Dimens.SessionMainIcon)
            if (playing) {
                Icon(painterResource(R.drawable.ic_pause), contentDescription = name, tint = SessionTextGlow, modifier = iconModifier)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = name, tint = SessionTextGlow, modifier = iconModifier)
            }
        }
        SessionRoundButton(onClick = onStop, size = Dimens.SessionSideButton, container = tonal, visible = true) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = stringResource(R.string.action_stop),
                tint = SessionButtonPink,
                modifier = Modifier.size(Dimens.SessionSideIcon),
            )
        }
    }
}

/**
 * Runder Knopf der [SessionControlBar]: Kreis [size] (Ripple nur dort), selbst Tippfläche (≥ 48 dp).
 * Unsichtbar ([visible] = false) blendet er aus und behält seinen Platz; er ist sofort nicht
 * mehr antippbar, und sein Icon (mit der `contentDescription`, ICON-02) verschwindet nach dem
 * Ausblenden – danach findet TalkBack nichts mehr, ohne Semantik zu überschreiben (A11Y-02).
 */
@Composable
private fun SessionRoundButton(
    onClick: () -> Unit,
    size: Dp,
    container: Color,
    visible: Boolean,
    icon: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(Motion.ControlFade), label = "sessionControl")
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(container)
            .then(
                if (visible) {
                    Modifier.clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (visible || alpha > 0f) icon()
    }
}

/**
 * Abschluss-Screen beider Sitzungsarten: „✓ Geschafft"-Eyebrow, [title], mit [feelingAsk] darunter
 * die Antwortreihe zum Befinden aus [feelingOptions] (gewählt: [feeling]; überspringbar, nur wenn ein
 * Logbuch-Eintrag entstand, SEC-LEGAL-04), optional eine kurze Bilanz aus den
 * Teilen [summary] (z. B. „8 Runden · 6:24 min“, [sessionSummaryParts]), darunter mit
 * [onOpenLogbook] der Link „Im Logbuch ansehen ›“, dann Nochmal/Ende.
 *
 * Für TalkBack ein eigener Bereich („Geschafft“ als `paneTitle`, beim Erscheinen angesagt). Wer
 * per Tastatur oder D-Pad bedient, landet auf „Nochmal“ – der zuletzt fokussierte Pause-Knopf
 * ist mit dem Ende verschwunden (A11Y-07). Bei Touch bleibt der Fokus unberührt, sonst zeigte
 * „Nochmal“ eine Fokusfläche.
 */
@Composable
fun FinishedPanel(
    title: String,
    onAgain: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    summary: List<String> = emptyList(),
    onOpenLogbook: (() -> Unit)? = null,
    onIntoSilence: (() -> Unit)? = null,
    feelingOptions: List<Feeling> = emptyList(),
    feeling: Feeling? = null,
    feelingAsk: FeelingAsk? = null,
) {
    val pane = stringResource(R.string.session_done_title)
    val againFocus = remember { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    LaunchedEffect(Unit) { if (inputMode.inputMode == InputMode.Keyboard) againFocus.requestFocus() }
    // Mittig, solange alles passt; mit Antwortreihe und großer Schrift scrollt der Ende-Screen, statt
    // „Nochmal“ und „Ende“ abzuschneiden (LAYOUT-05).
    BoxWithConstraints(modifier = modifier.fillMaxSize().semantics { paneTitle = pane }) {
        FinishedColumn(minHeight = maxHeight) {
            FinishedContent(title, onAgain, onExit, againFocus, summary, onOpenLogbook, onIntoSilence, feelingOptions, feeling, feelingAsk)
        }
    }
}

/** Spalte des Ende-Screens: mindestens [minHeight] hoch, Inhalt mittig, darüber hinaus scrollbar. */
@Composable
private fun FinishedColumn(minHeight: Dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .heightIn(min = minHeight)
            .padding(Dimens.SessionPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun ColumnScope.FinishedContent(
    title: String,
    onAgain: () -> Unit,
    onExit: () -> Unit,
    againFocus: FocusRequester,
    summary: List<String>,
    onOpenLogbook: (() -> Unit)?,
    onIntoSilence: (() -> Unit)?,
    feelingOptions: List<Feeling>,
    feeling: Feeling?,
    feelingAsk: FeelingAsk?,
) {
    Text(
        text = "✓ " + stringResource(R.string.session_done_title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = TERTIARY),
    )
    Spacer(Modifier.height(Dimens.GapSmall))
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    if (feelingAsk != null && feelingOptions.isNotEmpty()) FeelingRow(feelingOptions, feeling, feelingAsk)
    if (summary.isNotEmpty()) {
        Spacer(Modifier.height(Dimens.GapSmall))
        val summaryStyle = MaterialTheme.typography.bodySmall
        val summaryColor = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY)
        // Eine Ansage für TalkBack statt einzelner Teile, mit Pause statt vorgelesenem Trennpunkt.
        val spoken = summary.joinToString(", ")
        DotSeparatedFlow(
            parts = summary.map { part ->
                { WholeWordText(text = part, style = summaryStyle, color = summaryColor, textAlign = TextAlign.Center) }
            },
            separator = { Text(text = DOT_SEPARATOR, style = summaryStyle, color = summaryColor) },
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        )
    }
    // Unter der Bilanz der Weg zum Eintrag (Nachtrag 2, Nutzer 2026-10-22): beendet wie
    // „Ende“ und öffnet das Logbuch.
    if (onOpenLogbook != null) {
        Spacer(Modifier.height(Dimens.GapTiny))
        AppTextButton(onClick = onOpenLogbook) {
            ForwardLinkText(text = stringResource(R.string.session_open_logbook), color = MaterialTheme.colorScheme.secondary)
        }
    }
    // Nach einer Atemübung: auch ohne die Einstellung „Danach in die Stille“ einmal in die
    // stille Meditation (Nutzer 2026-10-23).
    if (onIntoSilence != null) {
        AppTextButton(onClick = onIntoSilence) {
            ForwardLinkText(text = stringResource(R.string.session_into_silence), color = MaterialTheme.colorScheme.secondary)
        }
    }
    Spacer(Modifier.height(Dimens.SessionPadding))
    AdaptiveButtonRow(modifier = Modifier.fillMaxWidth()) {
        SessionPrimaryButton(
            text = stringResource(R.string.action_again),
            onClick = onAgain,
            modifier = Modifier.focusRequester(againFocus),
        )
        SessionSecondaryButton(
            text = stringResource(R.string.action_end),
            onClick = onExit,
        )
    }
}

/**
 * Antwortreihe unter der Einladung „Spür kurz nach, wie es dir geht.“ (Anatomie §4.5): drei gleich
 * breite, stille Segmente ohne Häkchen ([SegmentedChoiceRow]), keine gewählt, bis jemand tippt; kein
 * Pflichtfeld – „Nochmal“ und „Ende“ gehen jederzeit. Der Hinweis des ersten Mals bleibt für diesen
 * Ende-Screen stehen, auch wenn [FeelingAsk.onIntroShown] den Schalter schon gesetzt hat.
 */
@Composable
private fun FeelingRow(options: List<Feeling>, selected: Feeling?, ask: FeelingAsk) {
    var showIntro by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ask.intro) {
        if (ask.intro) {
            showIntro = true
            ask.onIntroShown()
        }
    }
    Column(modifier = Modifier.padding(top = Dimens.ListGap), horizontalAlignment = Alignment.CenterHorizontally) {
        SegmentedChoiceRow<Feeling?>(
            items = options,
            selected = selected,
            onSelect = { it?.let(ask.onAnswer) },
            groupLabel = stringResource(R.string.feeling_group),
        ) { option -> option?.let { feelingText(it) }.orEmpty() }
        if (showIntro) {
            Spacer(Modifier.height(Dimens.GapSmall))
            WholeWordText(
                text = stringResource(R.string.feeling_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = TERTIARY),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Zustand des Tap-Flashs (großes Pause/Play-Symbol beim Antippen des Kreises). */
@Stable
class TapFlashState internal constructor() {
    var alpha by mutableFloatStateOf(0f)
        internal set
    var isPause by mutableStateOf(true)
        private set
    internal var trigger by mutableIntStateOf(0)

    /** Flash auslösen; [isPause] = true zeigt das Pause-Symbol, sonst Play. */
    fun flash(isPause: Boolean) {
        this.isPause = isPause
        trigger++
    }

    /**
     * Kreis-Tipp beider Sitzungsarten: kurzes, weiches Haptik-Tick ([haptic], folgt dem
     * App-Schalter „Vibration“), Flash mit dem Symbol des neuen Zustands ([running] → Pause),
     * dann [toggle].
     */
    fun toggle(running: Boolean, haptic: () -> Unit, toggle: () -> Unit) {
        haptic()
        flash(isPause = running)
        toggle()
    }
}

/**
 * Tap-Flash-Logik (geteilt von Session + Meditation). Alpha wird per delay-Schleife animiert,
 * NICHT per Animatable.animateTo: animateTo respektiert animator_duration_scale – ist die
 * System-Animation aus (Entwickleroptionen/Energiesparen), spränge es sofort auf den Zielwert
 * und der Flash bliebe unsichtbar. delay() ist davon unabhängig. Sind Animationen aus, steht das
 * Symbol still ([Motion.TapFlashStatic]) und verschwindet ohne Ausblenden (MOTION-02).
 */
@Composable
fun rememberTapFlash(): TapFlashState {
    val state = remember { TapFlashState() }
    val animate = rememberAnimationsEnabled()
    LaunchedEffect(state.trigger) {
        if (state.trigger > 0) {
            state.alpha = 1f
            if (animate) {
                delay(Motion.TapFlashHold) // kurzer Halt
                val steps = Motion.TapFlashFadeSteps
                for (i in 1..steps) {
                    delay(Motion.TapFlashFadeStep)
                    state.alpha = (1f - i.toFloat() / steps).coerceAtLeast(0f)
                }
            } else {
                delay(Motion.TapFlashStatic)
            }
            state.alpha = 0f
        }
    }
    return state
}
