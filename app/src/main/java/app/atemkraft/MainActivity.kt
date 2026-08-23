package app.atemkraft

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.data.CueSettings
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.Situations
import app.atemkraft.domain.EvidenceTag
import app.atemkraft.ui.AboutRoute
import app.atemkraft.ui.AtmenRoute
import app.atemkraft.ui.DetailRoute
import app.atemkraft.ui.GlossaryRoute
import app.atemkraft.ui.LogbuchRoute
import app.atemkraft.ui.MeditationRoute
import app.atemkraft.ui.SettingsRoute
import app.atemkraft.ui.SituationenRoute
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.meditation.MeditationStatus
import app.atemkraft.domain.MeditationConfig
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
        // Für die dezente Meditations-Dauer-Notification (ab Android 13 laufzeit-abgefragt;
        // bei Ablehnung läuft der Timer weiter, nur ohne sichtbare Notification).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
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

    val meditationController = container.meditationController
    val meditationState by meditationController.state.collectAsStateWithLifecycle()
    val meditationConfig by container.settingsRepository.meditationSettings
        .collectAsStateWithLifecycle(initialValue = MeditationConfig())
    val meditationSpeechAvailable by meditationController.speechAvailable
        .collectAsStateWithLifecycle()
    val meditationVoices by meditationController.voices.collectAsStateWithLifecycle()
    val meditationVoiceId by container.settingsRepository.ttsVoiceId
        .collectAsStateWithLifecycle(initialValue = null)
    val gongIntervalMin by container.settingsRepository.gongIntervalMin
        .collectAsStateWithLifecycle(initialValue = 5)

    val sessionActive = sessionState.status != SessionStatus.IDLE
    var sessionExpanded by rememberSaveable { mutableStateOf(false) }

    val meditationRunning = meditationState.status == MeditationStatus.RUNNING ||
        meditationState.status == MeditationStatus.PREPARING ||
        meditationState.status == MeditationStatus.PAUSED

    KeepScreenOn(
        enabled = sessionState.status == SessionStatus.RUNNING ||
            sessionState.status == SessionStatus.WAITING_FOR_USER ||
            meditationRunning,
    )

    val guidedByFamily = remember {
        container.exerciseRepository.byFamily()
            .map { (family, list) -> family to list.filter { it.guided } }
            .filter { (_, list) -> list.isNotEmpty() }
    }
    val programs = remember { container.exerciseRepository.all().filter { !it.guided } }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Aktueller Top-Level-Tab (null auf Push-Zielen wie Detail/Settings). Der zuletzt
    // gewählte Tab bleibt markiert, während man in einer daraus geöffneten Seite ist.
    val currentTopTab = when {
        currentDestination.isOn(AtmenRoute) -> TopTab.ATMEN
        currentDestination.isOn(SituationenRoute) -> TopTab.SITUATIONEN
        currentDestination.isOn(LogbuchRoute) -> TopTab.LOGBUCH
        currentDestination.isOn(MeditationRoute) -> TopTab.MEDITATION
        else -> null
    }
    var selectedTab by rememberSaveable { mutableStateOf(TopTab.ATMEN) }
    LaunchedEffect(currentTopTab) { if (currentTopTab != null) selectedTab = currentTopTab }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tab-Leiste dauerhaft sichtbar (jederzeit Sektion wechseln, Steuerung immer erreichbar).
        // Die Vollbild-Session liegt bei Bedarf als Overlay darüber. Bewusst NICHT bei laufender
        // Meditation ausgeblendet: sonst strandet System-Zurück den Nutzer auf einer Seite ohne
        // Leiste und ohne Meditations-Steuerung (Sackgasse).
        Scaffold(
            bottomBar = {
                Column {
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
                            selected = selectedTab == TopTab.ATMEN,
                            onClick = { navController.switchTab(AtmenRoute) },
                            icon = Icons.Filled.Home,
                            label = stringResource(R.string.tab_breathe),
                            colors = navColors,
                        )
                        TabItem(
                            selected = selectedTab == TopTab.SITUATIONEN,
                            onClick = { navController.switchTab(SituationenRoute) },
                            icon = Icons.Filled.Search,
                            label = stringResource(R.string.tab_situations),
                            colors = navColors,
                        )
                        TabItem(
                            selected = selectedTab == TopTab.LOGBUCH,
                            onClick = { navController.switchTab(LogbuchRoute) },
                            icon = Icons.Filled.DateRange,
                            label = stringResource(R.string.tab_logbook),
                            colors = navColors,
                        )
                        TabItem(
                            selected = selectedTab == TopTab.MEDITATION,
                            onClick = { navController.switchTab(MeditationRoute) },
                            painter = painterResource(R.drawable.ic_meditation),
                            label = stringResource(R.string.tab_meditation),
                            colors = navColors,
                        )
                    }
                }
            },
        ) { innerPadding ->
            // Push-Ziele (Detail/Settings/…) gleiten hierarchisch herein/heraus; Tab-Wechsel
            // blenden nur dezent – kein Ghosting mehr.
            val dur = 300
            val pushEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(dur)) +
                    fadeIn(tween(dur))
            }
            val pushExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(dur)) +
                    fadeOut(tween(dur))
            }
            val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(dur)) +
                    fadeIn(tween(dur))
            }
            val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(dur)) +
                    fadeOut(tween(dur))
            }
            NavHost(
                navController = navController,
                startDestination = AtmenRoute,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(180)) },
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
                composable<MeditationRoute> {
                    // TTS-Engine erst hier vorbereiten (nicht beim App-Start), idempotent.
                    LaunchedEffect(Unit) { meditationController.prepareSpeech() }
                    MeditationScreen(
                        state = meditationState,
                        initialConfig = meditationConfig,
                        speechAvailable = meditationSpeechAvailable,
                        gongIntervalMin = gongIntervalMin,
                        onStart = { config ->
                            scope.launch { container.settingsRepository.setMeditationConfig(config) }
                            meditationController.start(config)
                        },
                        onTogglePause = {
                            if (meditationState.status == MeditationStatus.PAUSED) {
                                meditationController.resume()
                            } else {
                                meditationController.pause()
                            }
                        },
                        onEnd = meditationController::end,
                    )
                }
                composable<DetailRoute>(
                    enterTransition = pushEnter,
                    exitTransition = pushExit,
                    popEnterTransition = popEnter,
                    popExitTransition = popExit,
                ) { entry ->
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
                composable<SettingsRoute>(
                    enterTransition = pushEnter,
                    exitTransition = pushExit,
                    popEnterTransition = popEnter,
                    popExitTransition = popExit,
                ) {
                    // TTS vorbereiten, damit die Stimmen-Auswahl auch hier gefüllt ist.
                    LaunchedEffect(Unit) { meditationController.prepareSpeech() }
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
                        gongIntervalMin = gongIntervalMin,
                        onGongInterval = { scope.launch { container.settingsRepository.setGongIntervalMin(it) } },
                        voices = meditationVoices,
                        selectedVoiceId = meditationVoiceId,
                        onSelectVoice = meditationController::selectVoice,
                        onOpenGlossary = { navController.navigate(GlossaryRoute) },
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<GlossaryRoute>(
                    enterTransition = pushEnter,
                    exitTransition = pushExit,
                    popEnterTransition = popEnter,
                    popExitTransition = popExit,
                ) {
                    GlossaryScreen(onBack = { navController.popBackStack() })
                }
                composable<AboutRoute>(
                    enterTransition = pushEnter,
                    exitTransition = pushExit,
                    popEnterTransition = popEnter,
                    popExitTransition = popExit,
                ) {
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
                onToggleMute = sessionViewModel::toggleMute,
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

/** Wie [TabItem], aber mit einem Painter-Icon (für eigene Vektor-Drawables ohne Core-Icon). */
@Composable
private fun RowScope.TabItem(
    selected: Boolean,
    onClick: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    colors: NavigationBarItemColors,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(painter = painter, contentDescription = null) },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        colors = colors,
    )
}

/** Die vier Top-Level-Tabs (für die Markierung der Leiste, auch auf Push-Zielen). */
private enum class TopTab { ATMEN, SITUATIONEN, LOGBUCH, MEDITATION }

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
