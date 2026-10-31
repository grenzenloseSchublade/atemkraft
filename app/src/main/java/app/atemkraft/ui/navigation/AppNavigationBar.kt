package app.atemkraft.ui.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import app.atemkraft.R
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.TabBarContainer
import app.atemkraft.ui.theme.TonalDeep
import kotlin.math.roundToInt

/** Ein Tab der unteren Navigationsleiste; [icon] liefert sein Symbol, das Icon setzt die Leiste. */
class AppNavItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Painter,
)

/**
 * Untere Navigationsleiste der Haupt-Tabs. Beschriftungen in `labelLarge` – eine Stufe über der
 * M3-Rolle `labelMedium`, die am A54 mit 9,9 sp zu klein las (S-37) –, brechen nie im Wort
 * (LAYOUT-03) und teilen sich eine Größe. Reicht die Tab-Breite bei großer Schrift nicht, wird die
 * Beschriftung höchstens bis [NAV_LABEL_MIN_SCALE] verkleinert; passt sie auch dann nicht, zeigt
 * die Leiste nur die Icons, und das Icon trägt den Namen als `contentDescription` (ICON-02; Nutzer:
 * lieber Icon-only als geschrumpfte Schrift) – wer die Systemschrift vergrößert, bekommt nie Text
 * in Normalgröße (WCAG 1.4.4).
 *
 * Auswahl: Icon und Beschriftung in `primary` (Pink, Eigentümer 2026-10-31: die Marke auf jedem
 * Screen) auf einer dunklen Pille (`TonalDeep`; Pink darauf 3,99, auf der helleren
 * `secondaryContainer` nur 2,47) – keine gefüllte Neon-Fläche, die gehört dem Start-Button (PRIN-02). Das Gewicht bleibt bei Auswahl gleich
 * (TYPO-01): Ein breiteres SemiBold verlangte im Moment des Wechsels eine kleinere gemeinsame
 * Größe, alle vier Beschriftungen sprangen.
 */
@Composable
fun AppNavigationBar(items: List<AppNavItem>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = scheme.primary,
        selectedTextColor = scheme.primary,
        indicatorColor = TonalDeep,
        unselectedIconColor = scheme.onSurface.copy(alpha = TERTIARY),
        unselectedTextColor = scheme.onSurface.copy(alpha = TERTIARY),
    )
    // Eine Größe für alle Beschriftungen, damit die Leiste ruhig wirkt; neu gemessen, wenn sich
    // Namen, Schrift oder Fensterbreite ändern.
    val fontScale = LocalDensity.current.fontScale
    val windowWidth = LocalWindowInfo.current.containerSize.width
    val keys = arrayOf(items.map { it.label }, fontScale, windowWidth)
    var labelScale by remember(*keys) { mutableFloatStateOf(1f) }
    var iconOnly by remember(*keys) { mutableStateOf(false) }
    NavigationBar(modifier = modifier, containerColor = TabBarContainer) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = {
                    // Mit Beschriftung ist das Icon dekorativ (sie benennt den Tab), ohne trägt es den Namen.
                    Icon(item.icon(), contentDescription = if (iconOnly) item.label else null)
                },
                label = if (iconOnly) {
                    null
                } else {
                    {
                        WholeWordText(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            minScale = NAV_LABEL_MIN_SCALE,
                            sharedScale = labelScale,
                            onSharedScaleTooBig = { labelScale = minOf(labelScale, it) },
                            onDoesNotFit = { iconOnly = true },
                        )
                    }
                },
                colors = colors,
            )
        }
    }
}

/**
 * Kleinste Beschriftung der Tabs (Anteil der Rolle). Darunter Icon-only statt kleinerer Schrift
 * (LAYOUT-03): Mit 0,5 standen die Namen bei Schrift 2,0 wieder in Normalgröße.
 */
private const val NAV_LABEL_MIN_SCALE = 0.85f

/**
 * Tab-Leiste [bar] (in der App [AppNavigationBar]), solange [visible]; sonst bleibt
 * nur der Platz der Systemnavigation [systemInsets].
 *
 * Mit [state] (der App-Rahmen) folgt die Höhe [TabBarState.openness]: Die Leiste wird in voller
 * Höhe gemessen und nur so weit gezeigt, wie sie offen ist (oben bündig, unten beschnitten) – beim
 * Wischen folgt sie dem Finger, sonst rastet sie mit [Motion.TabBar] ein, wenn [visible] wechselt
 * (Griff, Seitenwechsel). Gelesen wird [TabBarState.openness] nur beim Auslegen: Beim Ziehen wird
 * jedes Bild neu ausgelegt, nicht neu komponiert. Eine Startleiste darüber rutscht mit. Wischen
 * nach unten schließt die offene Leiste in der Tiefe ([TabBarState.detectSwipe]); Tipps auf die
 * Tabs bleiben Tipps. Ohne [state] (Screenshots) gleitet nur die Höhe ([Motion.Disclosure]).
 */
@Composable
fun HideableTabBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    state: TabBarState? = null,
    systemInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    bar: @Composable () -> Unit,
) {
    val shown = if (state == null) visible else rememberFollowing(state, visible)
    val inset = systemInsets.getBottom(LocalDensity.current)
    var top by remember { mutableFloatStateOf(0f) }
    val sized = if (state == null) {
        modifier.animateContentSize(Motion.Disclosure)
    } else {
        modifier
            .onGloballyPositioned { top = it.positionInRoot().y }
            .pointerInput(state) { with(state) { detectSwipe(opens = false, closes = true) { top } } }
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val full = maxOf(placeable.height, inset)
                if (full > inset) state.travel = (full - inset).toFloat()
                val height = inset + ((full - inset) * state.openness).roundToInt()
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
    }
    Box(sized) {
        if (shown) {
            // Keine Linie über der Leiste: Ihr Violett grenzt genug ab (Eigentümer 2026-10-31).
            bar()
        } else {
            Spacer(Modifier.windowInsetsBottomHeight(systemInsets))
        }
    }
}

/**
 * Verbindet [HideableTabBar] mit [state]: setzt die Haptik fürs Wischen, führt jedes Einrasten aus
 * ([TabBarState.settleRequest], [Motion.TabBar] mit dem Tempo des Fingers), rastet bei jedem Wechsel
 * von [visible] ein (der erste Frame springt, damit beim Start nichts gleitet) und sagt, ob die
 * Leiste komponiert sein muss – offen, gerade aufgehend oder [visible].
 */
@Composable
private fun rememberFollowing(state: TabBarState, visible: Boolean): Boolean {
    val haptics = LocalHapticFeedback.current
    SideEffect { state.haptics = haptics }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(visible) {
        if (first[0]) state.snap(visible) else state.settle(visible)
        first[0] = false
    }
    val request = state.settleRequest
    LaunchedEffect(request) {
        if (request == null) return@LaunchedEffect
        animate(state.openness, request.target, request.velocity, Motion.TabBar) { value, _ ->
            state.openness = value.coerceIn(0f, 1f)
        }
    }
    val open by remember(state) { derivedStateOf { state.openness > 0f } }
    return visible || open
}

/** Test-Tag des Griffs ([TabBarToggle]): Die Screenshot-Gates prüfen, dass seine Tippfläche nichts überdeckt. */
const val TAB_BAR_GRIP_TAG = "tabBarGrip"

/**
 * Griff, der die Tab-Leiste in der Tiefe auf- und zuklappt (Nutzer 2026-10-30, ersetzt die Lasche
 * mit Pfeil, die mit „Anpassen ›“ der Startleiste kollidierte): ein schmaler Strich wie oben an
 * einem Bottom Sheet ([Sizes.TabBarGripWidth] × [Sizes.TabBarGripHeight], `onSurface` in
 * [SECONDARY], ≥ 3:1), mittig über der Oberkante der untersten Leiste – der Startleiste, wenn die
 * Seite eine hat ([TabBarState.anchoredToPinnedBar]), sonst der unteren Leisten. Die Tippfläche
 * ([Dimens.MinTouchTarget] im Quadrat, A11Y-05) liegt ganz über der Kante und überdeckt nichts in
 * der Leiste; den Platz darüber halten die Seiten am Ende frei ([Dimens.TabBarGripClearance]).
 * Antippen klappt um ([TabBarState.toggle]), Wischen nach oben öffnet, nach unten schließt
 * ([TabBarState.detectSwipe]). TalkBack: Schaltfläche „Tab-Leiste zeigen“ bzw. „Tab-Leiste ausblenden“
 * mit dem Zustand der Leiste – so ist die Funktion auch ohne Geste erreichbar.
 *
 * [modifier] setzt den Griff oben mittig in einen Rahmen, der wie der App-Rahmen das Fenster füllt;
 * die Kante kommt in px im Fenster, darum misst der Griff die Lage dieses Rahmens selbst.
 */
@Composable
fun TabBarToggle(state: TabBarState, modifier: Modifier = Modifier) {
    val edge = state.anchorTop
    var origin by remember { mutableFloatStateOf(0f) }
    val touch = with(LocalDensity.current) { Dimens.MinTouchTarget.toPx() }
    val name = stringResource(if (state.collapsed) R.string.cd_tab_bar_show else R.string.cd_tab_bar_hide)
    val status = stringResource(if (state.collapsed) R.string.tab_bar_state_hidden else R.string.tab_bar_state_shown)
    Box(modifier.fillMaxWidth().onGloballyPositioned { origin = it.positionInRoot().y }, contentAlignment = Alignment.TopCenter) {
        if (edge.isNaN()) return@Box
        Box(
            modifier = Modifier
                // Unterkante der Tippfläche auf der Kante der Leiste.
                .offset { IntOffset(0, (edge - origin - touch).roundToInt()) }
                .size(Dimens.MinTouchTarget)
                .testTag(TAB_BAR_GRIP_TAG)
                .pointerInput(state) { with(state) { detectSwipe(opens = true, closes = true) { anchorTop - touch } } }
                .clickable(role = Role.Button, onClick = state::toggle)
                .semantics {
                    contentDescription = name
                    stateDescription = status
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier
                    .padding(bottom = Dimens.GapSmall)
                    .size(width = Sizes.TabBarGripWidth, height = Sizes.TabBarGripHeight)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY), CircleShape),
            )
        }
    }
}

@Preview
@Composable
private fun AppNavigationBarPreview() {
    AtemkraftTheme {
        AppNavigationBar(
            items = TopTab.entries.map { tab ->
                AppNavItem(label = stringResource(tab.label), selected = tab == TopTab.START, onClick = {}, icon = tab.icon)
            },
        )
    }
}
