package app.atemkraft.ui.meditation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import app.atemkraft.R
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.theme.Dimens
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Bausteine, die jede Darstellung der Dauerwahl teilt (Zeitring E, später Skala F): Anzeige in
 * der Mitte mit Feinwahl und ihrer Tippfläche, Texte, TalkBack-Schieberegler und ∞-Marke. Eine
 * neue Darstellung zeichnet nur Skala und Gesten und setzt diese Teile ein.
 */

/** Texte der Dauerwahl zum aktuellen Zustand (Anzeige und TalkBack). */
@Immutable
internal class DurationTexts(
    val freeSymbol: String,
    val unit: String,
    val sliderLabel: String,
    val sliderState: String,
    val freeLabel: String,
    val fineState: String,
    val fineAction: String,
    val less: String,
    val more: String,
)

@Composable
internal fun durationTexts(state: DurationPickerState): DurationTexts {
    val minutes = state.minutes
    val free = state.free
    return DurationTexts(
        freeSymbol = stringResource(R.string.meditation_free_symbol),
        unit = if (free) stringResource(R.string.meditation_dial_free_unit) else pluralStringResource(R.plurals.meditation_dial_unit, minutes),
        sliderLabel = stringResource(R.string.cd_meditation_dial),
        sliderState = if (free) stringResource(R.string.meditation_dial_state_free) else pluralStringResource(R.plurals.meditation_dial_state, minutes, minutes),
        freeLabel = stringResource(R.string.cd_meditation_dial_free),
        fineState = stringResource(if (state.fineAdjust) R.string.meditation_dial_fine_on else R.string.meditation_dial_fine_off),
        fineAction = stringResource(R.string.cd_meditation_dial_fine),
        less = stringResource(R.string.cd_meditation_dial_less),
        more = stringResource(R.string.cd_meditation_dial_more),
    )
}

/** Alle Einheiten, die unter der Zahl stehen können – zum Ausmessen der breitesten. */
@Composable
internal fun durationUnitSamples(): List<String> = listOf(
    pluralStringResource(R.plurals.meditation_dial_unit, DURATION_MIN_MINUTES),
    pluralStringResource(R.plurals.meditation_dial_unit, DURATION_MAX_MINUTES),
    stringResource(R.string.meditation_dial_free_unit),
)

/** Polster links/rechts von Zahl und Einheit (Abstand zu − / +). */
internal val DurationReadoutPad = Dimens.GapTiny

/** Schrift der Einheit unter der Zahl. */
internal val durationUnitStyle: TextStyle
    @Composable get() = MaterialTheme.typography.labelLarge

/**
 * TalkBack: Die Darstellung ist ein Schieberegler 1..60 – Name „Dauer“, Wert „12 Minuten“ bzw.
 * „offene Sitzung“; Wischen und Lautstärketasten stellen ihn über [setProgress].
 */
internal fun Modifier.durationSlider(state: DurationPickerState, texts: DurationTexts, onTick: (major: Boolean) -> Unit): Modifier = semantics {
    contentDescription = texts.sliderLabel
    stateDescription = texts.sliderState
    progressBarRangeInfo = ProgressBarRangeInfo(
        current = state.minutes.toFloat(),
        range = DURATION_MIN_MINUTES.toFloat()..DURATION_MAX_MINUTES.toFloat(),
        steps = DURATION_MAX_MINUTES - DURATION_MIN_MINUTES - 1,
    )
    setProgress { target ->
        state.selectWithTick(target.roundToInt(), onTick)
        true
    }
}

/**
 * Antippbare ∞-Marke: runde Tippfläche mit rundem Tipp-Echo, Name und Auswahl. Die Größe legt der
 * Aufrufer fest – mindestens 48 dp und so groß, dass das Echo die gezeichnete Marke ganz umfasst.
 */
internal fun Modifier.freeMark(state: DurationPickerState, texts: DurationTexts, onTick: (major: Boolean) -> Unit): Modifier = this
    .clip(CircleShape)
    .clickable(role = Role.Button, interactionSource = null, indication = ripple()) { state.toggleFreeWithTick(onTick) }
    .semantics {
        contentDescription = texts.freeLabel
        selected = state.free
    }

/**
 * Tippfläche der Feinwahl unter der [DurationReadout]: [shape] begrenzt Treffer und Tipp-Echo (beim
 * Zeitring die ganze innere Scheibe), die Anzeige darüber bleibt ungeschnitten. Die Fläche spricht
 * für die Anzeige: Wert, „Feinwahl an/aus“, Rolle Button; nur bei offener Feinwahl als
 * Live-Region, denn sonst sagt der Schieberegler den Wert an.
 */
internal fun Modifier.fineToggle(state: DurationPickerState, texts: DurationTexts, shape: Shape): Modifier = this
    .clip(shape)
    .clickable(role = Role.Button, onClickLabel = texts.fineAction, interactionSource = null, indication = ripple(), onClick = state::toggleFineAdjust)
    .semantics {
        contentDescription = texts.sliderState
        stateDescription = texts.fineState
        if (state.fineAdjust) liveRegion = LiveRegionMode.Polite
    }

/**
 * Zeit + Einheit; bei offener Feinwahl stehen − und + (je 1 min, 48-dp-Tippfläche) links und
 * rechts der Zahl. Zahl und Einheit selbst sind nicht antippbar und nie zugeschnitten: Den Tipp
 * nimmt die Fläche darunter ([fineToggle]), die auch für TalkBack spricht. [numberStyle] wählt die
 * Darstellung passend zu ihrem Platz ([fitNumberStyle]); die Zahl ist mindestens so breit wie
 * „60“, damit nichts springt.
 */
@Composable
internal fun DurationReadout(
    state: DurationPickerState,
    texts: DurationTexts,
    onTick: (major: Boolean) -> Unit,
    numberStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val free = state.free
    val minutes = state.minutes
    val fineAdjust = state.fineAdjust
    val measurer = rememberTextMeasurer()
    val numberMinWidth = with(LocalDensity.current) {
        remember(measurer, numberStyle) { measurer.measure(WIDEST_VALUE, numberStyle).size.width }.toDp()
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = fineAdjust,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            AppIconButton(
                onClick = { state.stepWithTick(-1, onTick) },
                enabled = free || minutes > DURATION_MIN_MINUTES,
                colors = IconButtonDefaults.filledTonalIconButtonColors(),
            ) {
                Icon(painterResource(R.drawable.ic_remove), contentDescription = texts.less)
            }
        }
        Column(
            // Kein Tippziel, kein Zuschnitt: Taps fallen auf [fineToggle] darunter.
            modifier = Modifier.padding(horizontal = DurationReadoutPad),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (free) texts.freeSymbol else minutes.toString(),
                style = numberStyle,
                color = if (free) colors.primary else colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.widthIn(min = numberMinWidth).then(SpokenByToggle),
            )
            Text(text = texts.unit, style = durationUnitStyle, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false, modifier = SpokenByToggle)
        }
        AnimatedVisibility(
            visible = fineAdjust,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            AppIconButton(
                onClick = { state.stepWithTick(1, onTick) },
                enabled = free || minutes < DURATION_MAX_MINUTES,
                colors = IconButtonDefaults.filledTonalIconButtonColors(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = texts.more)
            }
        }
    }
}

/**
 * Zahl und Einheit sind für TalkBack kein eigener Stopp: [fineToggle] sagt Wert und Zustand an.
 * Nicht `clearAndSetSemantics`, damit die Text-Layouts für die Umbruch- und Zuschnittprüfung im
 * Screenshot-Test sichtbar bleiben.
 */
@OptIn(ExperimentalComposeUiApi::class)
private val SpokenByToggle = Modifier.semantics { invisibleToUser() }

/**
 * Halbe Maße der [DurationReadout] in px: [textHalfWidth]/[textHalfHeight] für Zahl + Einheit,
 * [rowHalfWidth] mit offener Feinwahl (Polster und beide 48-dp-Knöpfe), [buttonHalf] deren halbe
 * Höhe.
 */
internal class ReadoutSize(val textHalfWidth: Float, val textHalfHeight: Float, val rowHalfWidth: Float, val buttonHalf: Float)

/**
 * Misst die Anzeige mit [numberStyle]; die Einheit zählt mit ihrer breitesten Form ([units]).
 * [padPx] ist [DurationReadoutPad] in px.
 */
internal fun measureReadout(
    measurer: TextMeasurer,
    numberStyle: TextStyle,
    unitStyle: TextStyle,
    units: List<String>,
    padPx: Float,
    touchPx: Float,
): ReadoutSize {
    val number = measurer.measure(WIDEST_VALUE, numberStyle).size
    val unitWidth = units.maxOf { measurer.measure(it, unitStyle).size.width }
    val unitHeight = units.maxOf { measurer.measure(it, unitStyle).size.height }
    val textHalfWidth = max(number.width, unitWidth) / 2f
    return ReadoutSize(
        textHalfWidth = textHalfWidth,
        textHalfHeight = (number.height + unitHeight) / 2f,
        rowHalfWidth = textHalfWidth + padPx + touchPx,
        buttonHalf = touchPx / 2f,
    )
}

/**
 * Größter Zahlenstil aus [candidates], für den [fits] gilt; passt keiner, der kleinste – die
 * Anzeige wird nie abgeschnitten. Ziffern gleich breit, damit beim Drehen nichts springt.
 */
internal fun fitNumberStyle(candidates: List<TextStyle>, fits: (TextStyle) -> Boolean): TextStyle {
    val styles = candidates.map { it.copy(fontFeatureSettings = TABULAR) }
    return styles.firstOrNull(fits) ?: styles.last()
}

/** Kandidaten für die große Zahl, vom größten zum kleinsten. */
internal val durationNumberStyles: List<TextStyle>
    @Composable get() = MaterialTheme.typography.run { listOf(displayMedium, displaySmall, headlineSmall, titleLarge) }

// Ziffern gleich breit, damit Mitte und Zahlen beim Drehen nicht springen.
internal const val TABULAR = "tnum"

// Breitester Wert, an dem sich Mindestbreite und Stil der Anzeige ausrichten.
private const val WIDEST_VALUE = "60"
