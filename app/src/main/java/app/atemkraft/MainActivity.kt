package app.atemkraft

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.ContinuousTonePlayer
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.data.CueSettings
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.Situations
import app.atemkraft.domain.EvidenceTag
import app.atemkraft.domain.SoundMode
import app.atemkraft.ui.AboutRoute
import app.atemkraft.ui.AtmenRoute
import app.atemkraft.ui.DetailRoute
import app.atemkraft.ui.GlossaryRoute
import app.atemkraft.ui.LogbuchRoute
import app.atemkraft.ui.SettingsRoute
import app.atemkraft.ui.SituationenRoute
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.session.MiniSessionBar
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.session.SessionStatus
import app.atemkraft.ui.session.SessionViewModel
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.situations.SituationsScreen
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.DarkSurface
import app.atemkraft.ui.theme.NeonMagenta
import app.atemkraft.ui.theme.OnNeon
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtemkraftTheme {
                AtemkraftApp()
            }
        }
    }
}

@Composable
private fun AtemkraftApp() {
    val context = LocalContext.current
    val container = (context.applicationContext as AtemkraftApplication).container
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory)
    val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()
    val cueSettings by container.settingsRepository.cueSettings
        .collectAsStateWithLifecycle(initialValue = CueSettings())
    val safetySettings by container.settingsRepository.safetySettings
        .collectAsStateWithLifecycle(initialValue = SafetySettings())
    val logEntries by container.logbookRepository.entries
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val showNextPhase by container.settingsRepository.showNextPhase
        .collectAsStateWithLifecycle(initialValue = true)

    val sessionActive = sessionState.status != SessionStatus.IDLE
    var sessionExpanded by rememberSaveable { mutableStateOf(false) }

    // Ton + Audio-Fokus auf App-Ebene (laufen über Tab-Wechsel hinweg weiter).
    val tonePlayer = remember { ToneCuePlayer() }
    DisposableEffect(Unit) { onDispose { tonePlayer.release() } }
    val continuousPlayer = remember { ContinuousTonePlayer() }
    DisposableEffect(Unit) { onDispose { continuousPlayer.stop() } }
    val audioFocus = remember { AudioFocusController(context) }
    val hapticPlayer = remember { HapticPlayer(context) }
    val currentCueSettings by rememberUpdatedState(cueSettings)

    LaunchedEffect(sessionViewModel) {
        sessionViewModel.cues.collect { event ->
            if (currentCueSettings.soundMode == SoundMode.CUES) tonePlayer.play(event)
            if (currentCueSettings.haptics) hapticPlayer.play(event)
        }
    }
    // Durchgehender Ton: Player nur bei CONTINUOUS + aktiver Session laufen lassen, je Phase füttern,
    // bei Nicht-Laufen (Pause/Countdown/Ende) ausblenden.
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.phaseAudio.collect { pa ->
            if (currentCueSettings.soundMode == SoundMode.CONTINUOUS) {
                continuousPlayer.onPhase(pa.type, pa.durationMs, pa.open)
            }
        }
    }
    DisposableEffect(cueSettings.soundMode, sessionActive) {
        if (cueSettings.soundMode == SoundMode.CONTINUOUS && sessionActive) continuousPlayer.start()
        else continuousPlayer.stop()
        onDispose { continuousPlayer.stop() }
    }
    LaunchedEffect(cueSettings.transition) { continuousPlayer.setEmphasis(cueSettings.transition) }
    LaunchedEffect(cueSettings.volume) {
        continuousPlayer.setVolume(cueSettings.volume)
        tonePlayer.setVolume(cueSettings.volume)
    }
    LaunchedEffect(sessionState.status, cueSettings.soundMode) {
        val running = sessionState.status == SessionStatus.RUNNING ||
            sessionState.status == SessionStatus.WAITING_FOR_USER
        if (cueSettings.soundMode == SoundMode.CONTINUOUS && !running) continuousPlayer.mute()
    }
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.completions.collect { entry -> container.logbookRepository.append(entry) }
    }
    DisposableEffect(sessionActive, cueSettings.soundMode) {
        if (sessionActive && cueSettings.soundMode != SoundMode.OFF) audioFocus.request()
        else audioFocus.abandon()
        onDispose { audioFocus.abandon() }
    }
    KeepScreenOn(
        enabled = sessionState.status == SessionStatus.RUNNING ||
            sessionState.status == SessionStatus.WAITING_FOR_USER,
    )

    val guidedByFamily = remember {
        container.exerciseRepository.byFamily()
            .map { (family, list) -> family to list.filter { it.guided } }
            .filter { (_, list) -> list.isNotEmpty() }
    }
    val programs = remember { container.exerciseRepository.all().filter { !it.guided } }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(modifier = Modifier.fillMaxSize()) {
        val onTopLevel = currentDestination.isOn(AtmenRoute) ||
            currentDestination.isOn(SituationenRoute) ||
            currentDestination.isOn(LogbuchRoute)
        Scaffold(
            bottomBar = {
                // Tab-Leiste + Mini-Player nur auf den Top-Level-Tabs; auf Detail/Settings
                // führt der Zurück-Pfeil, und keine Leiste ohne hervorgehobenen Tab.
                if (onTopLevel) Column {
                    if (sessionActive && !sessionExpanded) {
                        MiniSessionBar(state = sessionState, onClick = { sessionExpanded = true })
                    }
                    HorizontalDivider(thickness = 1.dp, color = NeonMagenta.copy(alpha = 0.22f))
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OnNeon,
                        selectedTextColor = NeonMagenta,
                        indicatorColor = NeonMagenta,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    NavigationBar(containerColor = DarkSurface) {
                        TabItem(
                            selected = currentDestination.isOn(AtmenRoute),
                            onClick = { navController.switchTab(AtmenRoute) },
                            icon = Icons.Filled.Home,
                            label = stringResource(R.string.tab_breathe),
                            colors = navColors,
                        )
                        TabItem(
                            selected = currentDestination.isOn(SituationenRoute),
                            onClick = { navController.switchTab(SituationenRoute) },
                            icon = Icons.Filled.Search,
                            label = stringResource(R.string.tab_situations),
                            colors = navColors,
                        )
                        TabItem(
                            selected = currentDestination.isOn(LogbuchRoute),
                            onClick = { navController.switchTab(LogbuchRoute) },
                            icon = Icons.Filled.DateRange,
                            label = stringResource(R.string.tab_logbook),
                            colors = navColors,
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AtmenRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable<AtmenRoute> {
                    HomeScreen(
                        exercisesByFamily = guidedByFamily,
                        programs = programs,
                        onSelect = { id -> navController.navigate(DetailRoute(id)) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable<SituationenRoute> {
                    SituationsScreen(
                        recommendations = Situations.all,
                        resolve = { id -> container.exerciseRepository.byId(id) },
                        onSelect = { id -> navController.navigate(DetailRoute(id)) },
                    )
                }
                composable<LogbuchRoute> {
                    LogbookScreen(
                        entries = logEntries,
                        onClear = { scope.launch { container.logbookRepository.clear() } },
                    )
                }
                composable<DetailRoute> { entry ->
                    val id = entry.toRoute<DetailRoute>().exerciseId
                    val exercise = container.exerciseRepository.byId(id) ?: return@composable
                    val requireSafety = exercise.tag == EvidenceTag.CAUTION &&
                        (!safetySettings.acknowledged || safetySettings.showWarning)
                    ExerciseDetailScreen(
                        exercise = exercise,
                        requireSafetyConfirm = requireSafety,
                        onConfirmedSafety = {
                            scope.launch { container.settingsRepository.setSafetyAcknowledged(true) }
                        },
                        onStart = { config ->
                            sessionViewModel.start(id, config)
                            sessionExpanded = true
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(
                        soundMode = cueSettings.soundMode,
                        transition = cueSettings.transition,
                        volume = cueSettings.volume,
                        haptics = cueSettings.haptics,
                        showSafetyWarning = safetySettings.showWarning,
                        showNextPhase = showNextPhase,
                        onSoundMode = { scope.launch { container.settingsRepository.setSoundMode(it) } },
                        onTransition = { scope.launch { container.settingsRepository.setTransitionEmphasis(it) } },
                        onVolume = { scope.launch { container.settingsRepository.setToneVolume(it) } },
                        onToggleHaptics = { scope.launch { container.settingsRepository.setHaptics(it) } },
                        onToggleSafety = { scope.launch { container.settingsRepository.setShowSafetyWarning(it) } },
                        onToggleNextPhase = { scope.launch { container.settingsRepository.setShowNextPhase(it) } },
                        onOpenGlossary = { navController.navigate(GlossaryRoute) },
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<GlossaryRoute> {
                    GlossaryScreen(onBack = { navController.popBackStack() })
                }
                composable<AboutRoute> {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        // Vollbild-Session über allem.
        if (sessionActive && sessionExpanded) {
            SessionOverlay(
                state = sessionState,
                showNextPhase = showNextPhase,
                onMinimize = { sessionExpanded = false },
                onTogglePause = {
                    if (sessionState.status == SessionStatus.PAUSED) sessionViewModel.resume()
                    else sessionViewModel.pause()
                },
                onContinue = sessionViewModel::continueFromUserPaced,
                onRestart = sessionViewModel::restart,
                onStop = {
                    sessionViewModel.stop()
                    sessionExpanded = false
                },
            )
            // System-Zurück minimiert zur Mini-Leiste (Session läuft weiter, App bleibt offen) –
            // konsistent mit dem Minimieren-Chevron.
            BackHandler { sessionExpanded = false }
        }
    }
}

/** Tab mit hervorgehobenem aktiven Zustand: gefüllte Magenta-Pille + dunkles Icon + fettes Label. */
@Composable
private fun RowScope.TabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    colors: NavigationBarItemColors,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        colors = colors,
    )
}

/** True, wenn das aktuelle Ziel zu dieser Top-Level-Route gehört. */
private fun androidx.navigation.NavDestination?.isOn(route: Any): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true

/** Tab-Wechsel mit erhaltenen Back-Stacks (NiA-Muster). */
private fun androidx.navigation.NavController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
