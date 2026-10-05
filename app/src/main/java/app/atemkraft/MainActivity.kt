package app.atemkraft

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.data.CueSettings
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.Situations
import app.atemkraft.domain.EvidenceTag
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.ui.AboutRoute
import app.atemkraft.ui.AtmenRoute
import app.atemkraft.ui.DetailRoute
import app.atemkraft.ui.GlossaryRoute
import app.atemkraft.ui.LogbuchRoute
import app.atemkraft.ui.MeditationRoute
import app.atemkraft.ui.SettingsRoute
import app.atemkraft.ui.SituationenRoute
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.components.AppNavItem
import app.atemkraft.ui.components.AppNavigationBar
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.meditation.MeditationStatus
import app.atemkraft.ui.meditation.MiniMeditationBar
import app.atemkraft.ui.session.MiniSessionBar
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.session.SessionStatus
import app.atemkraft.ui.session.SessionViewModel
import app.atemkraft.ui.settings.DataStatus
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.settings.rememberPatternTransfer
import app.atemkraft.ui.situations.SituationsScreen
import app.atemkraft.ui.tapTab
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.NeonMagenta
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
        // App ist dark-only: Systemleisten-Icons immer hell, unabhängig vom System-Hell/Dunkel-Modus.
        // Ohne expliziten Style würden sie im hellen System-Modus dunkel auf dunklem Indigo gezeichnet.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
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
    // Hier oben gesammelt (nicht erst im Tab), damit der gespeicherte Zustand beim ersten
    // Öffnen der Situationen schon da ist und „Meine Muster“ nicht sichtbar umspringt.
    val savedPatternsExpanded by container.settingsRepository.savedPatternsExpanded
        .collectAsStateWithLifecycle(initialValue = false)

    val meditationController = container.meditationController
    val meditationState by meditationController.state.collectAsStateWithLifecycle()
    val meditationConfig by container.settingsRepository.meditationSettings
        .collectAsStateWithLifecycle(initialValue = MeditationConfig())
    val meditationSpeechAvailable by meditationController.speechAvailable
        .collectAsStateWithLifecycle()
    val gongIntervalMin by container.settingsRepository.gongIntervalMin
        .collectAsStateWithLifecycle(initialValue = 5)
    val gongLong by container.settingsRepository.gongLong
        .collectAsStateWithLifecycle(initialValue = true)
    val voiceStates by container.voiceModelManager.states.collectAsStateWithLifecycle()
    val activeVoiceId by container.settingsRepository.neuralVoiceId
        .collectAsStateWithLifecycle(initialValue = null)
    val piperEngineReady by meditationController.piperReady.collectAsStateWithLifecycle()

    val sessionActive = sessionState.status != SessionStatus.IDLE
    var sessionExpanded by rememberSaveable { mutableStateOf(false) }

    // „aktiv" (inkl. FINISHED) steuert Overlay + Mini-Leiste; die Wach-Halten-Logik nur bei RUNNING/PREPARING.
    val meditationActive = meditationState.status != MeditationStatus.IDLE
    var meditationExpanded by rememberSaveable { mutableStateOf(false) }

    KeepScreenOn(
        enabled = sessionState.status == SessionStatus.RUNNING ||
            sessionState.status == SessionStatus.WAITING_FOR_USER ||
            meditationState.status == MeditationStatus.RUNNING ||
            meditationState.status == MeditationStatus.PREPARING,
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
    // gewählte Tab bleibt markiert, während man in einer daraus geöffneten Seite ist; ein
    // erneuter Tipp darauf führt zu seiner Startseite zurück (tapTab).
    val currentTopTab = when {
        currentDestination.isOn(AtmenRoute) -> TopTab.ATMEN
        currentDestination.isOn(SituationenRoute) -> TopTab.SITUATIONEN
        currentDestination.isOn(LogbuchRoute) -> TopTab.LOGBUCH
        currentDestination.isOn(MeditationRoute) -> TopTab.MEDITATION
        else -> null
    }
    var selectedTab by rememberSaveable { mutableStateOf(TopTab.ATMEN) }
    LaunchedEffect(currentTopTab) { if (currentTopTab != null) selectedTab = currentTopTab }

    // System-Zurück auf einem Top-Level-Tab bei minimierter Session/Meditation: App nur in den
    // Hintergrund schieben statt die Activity zu beenden – sonst stirbt die Atem-Session (ihr
    // ViewModel hängt an der Activity), obwohl die Mini-Leiste „läuft weiter" signalisiert.
    // Auf Push-Seiten (currentTopTab == null) und in Overlays greifen deren eigene Back-Handler.
    BackHandler(
        enabled = (sessionActive || meditationActive) && currentTopTab != null &&
            !sessionExpanded && !meditationExpanded,
    ) {
        (context as? Activity)?.moveTaskToBack(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Laufende Session/Meditation liegen bei Bedarf als Vollbild-Overlay über dem Scaffold;
        // minimiert erscheinen sie als „Now-Playing"-Mini-Leiste über der Tab-Leiste.
        Scaffold(
            bottomBar = {
                Column {
                    if (sessionActive && !sessionExpanded) {
                        MiniSessionBar(state = sessionState, onClick = { sessionExpanded = true })
                    }
                    if (meditationActive && !meditationExpanded) {
                        MiniMeditationBar(state = meditationState, onClick = { meditationExpanded = true })
                    }
                    HorizontalDivider(thickness = 1.dp, color = NeonMagenta.copy(alpha = 0.22f))
                    AppNavigationBar(
                        listOf(
                            AppNavItem(
                                label = stringResource(R.string.tab_breathe),
                                selected = selectedTab == TopTab.ATMEN,
                                onClick = { navController.tapTab(AtmenRoute, selected = selectedTab == TopTab.ATMEN) },
                            ) { Icon(Icons.Filled.Home, contentDescription = null) },
                            AppNavItem(
                                label = stringResource(R.string.tab_situations),
                                selected = selectedTab == TopTab.SITUATIONEN,
                                onClick = { navController.tapTab(SituationenRoute, selected = selectedTab == TopTab.SITUATIONEN) },
                            ) { Icon(Icons.Filled.Search, contentDescription = null) },
                            AppNavItem(
                                label = stringResource(R.string.tab_meditation),
                                selected = selectedTab == TopTab.MEDITATION,
                                onClick = { navController.tapTab(MeditationRoute, selected = selectedTab == TopTab.MEDITATION) },
                            ) { Icon(painterResource(R.drawable.ic_meditation), contentDescription = null) },
                            AppNavItem(
                                label = stringResource(R.string.tab_logbook),
                                selected = selectedTab == TopTab.LOGBUCH,
                                onClick = { navController.tapTab(LogbuchRoute, selected = selectedTab == TopTab.LOGBUCH) },
                            ) { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        ),
                    )
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
                // Tablets/Foldables: Inhalte auf lesbare Breite begrenzen und mittig setzen;
                // die Seitenränder zeigen den (identischen) Fensterhintergrund.
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(180)) },
            ) {
                composable<AtmenRoute> {
                    var dailyPattern by remember {
                        mutableStateOf(container.exerciseRepository.daily())
                    }
                    // „Gespeichert" direkt aus der DB ableiten: deckt Speichern, App-Neustart am
                    // selben Tag und Namensgleichheit mit einem früher gespeicherten Muster ab.
                    val savedPatterns by container.savedPatternsRepository.patterns
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val dailySaved = savedPatterns.any {
                        it.exercise.name == container.savedPatternsRepository.savedName(dailyPattern)
                    }
                    HomeScreen(
                        daily = dailyPattern,
                        dailySaved = dailySaved,
                        onRegenerateDaily = {
                            dailyPattern = container.exerciseRepository.regenerateDaily()
                        },
                        onDailySavedChange = { save ->
                            val pattern = dailyPattern
                            scope.launch {
                                if (save) {
                                    container.savedPatternsRepository.save(pattern)
                                } else {
                                    container.savedPatternsRepository.unsave(pattern)
                                }
                            }
                        },
                        exercisesByFamily = guidedByFamily,
                        programs = programs,
                        onSelect = { id -> navController.navigate(DetailRoute(id)) },
                        onOpenSettings = { navController.navigate(SettingsRoute()) },
                    )
                }
                composable<SituationenRoute> {
                    val savedPatterns by container.savedPatternsRepository.patterns
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    SituationsScreen(
                        recommendations = Situations.all,
                        savedPatterns = savedPatterns,
                        savedPatternsExpanded = savedPatternsExpanded,
                        onSavedPatternsExpandedChange = { expanded ->
                            scope.launch { container.settingsRepository.setSavedPatternsExpanded(expanded) }
                        },
                        resolve = { id -> container.exerciseRepository.byId(id) },
                        onSelect = { id -> navController.navigate(DetailRoute(id)) },
                        onDeleteSaved = { id ->
                            scope.launch { container.savedPatternsRepository.delete(id) }
                        },
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
                        initialConfig = meditationConfig,
                        speechAvailable = meditationSpeechAvailable,
                        gongIntervalMin = gongIntervalMin,
                        onOpenSettings = { navController.navigate(SettingsRoute(focusMeditation = true)) },
                        onStart = { config ->
                            scope.launch { container.settingsRepository.setMeditationConfig(config) }
                            meditationController.start(config)
                            meditationExpanded = true // Vollbild-Overlay öffnen (wie Session-Start)
                        },
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
                    // Intervall-Anpassungen pro Übung merken – außer beim Muster des Tages: dessen
                    // Id ist konstant, der Inhalt wechselt aber täglich; gespeicherte Werte gälten
                    // sonst morgen für ein anderes Muster.
                    val persistIntervals = id != RandomPatternGenerator.ID
                    val savedIntervals by remember(id) {
                        container.settingsRepository.exerciseIntervals(id)
                    }.collectAsStateWithLifecycle(initialValue = null)
                    ExerciseDetailScreen(
                        exercise = exercise,
                        requireSafetyConfirm = requireSafety,
                        savedIntervals = if (persistIntervals) savedIntervals else null,
                        onIntervalsChange = { duration, inhale, hold, exhale ->
                            if (persistIntervals) {
                                scope.launch {
                                    container.settingsRepository.setExerciseIntervals(id, duration, inhale, hold, exhale)
                                }
                            }
                        },
                        onIntervalsReset = {
                            if (persistIntervals) {
                                scope.launch { container.settingsRepository.clearExerciseIntervals(id) }
                            }
                        },
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
                ) { entry ->
                    // TTS vorbereiten, damit die Stimmen-Auswahl auch hier gefüllt ist.
                    LaunchedEffect(Unit) { meditationController.prepareSpeech() }
                    val patternTransfer = rememberPatternTransfer(
                        container.savedPatternsRepository,
                        container.settingsRepository,
                    )
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
                        gongLong = gongLong,
                        onGongLong = { scope.launch { container.settingsRepository.setGongLong(it) } },
                        onPreviewGong = {
                            container.gongPreviewPlayer.setVolume(cueSettings.volume)
                            container.gongPreviewPlayer.previewGong(gongLong)
                        },
                        voiceStates = voiceStates,
                        activeVoiceId = activeVoiceId,
                        piperEngineReady = piperEngineReady,
                        onSampleVoice = { spec -> container.voiceSamplePlayer.play(spec.sampleAsset) },
                        onDownloadVoice = { container.voiceModelManager.download(it) },
                        onSelectVoice = meditationController::selectNeuralVoice,
                        onDeleteVoice = { id -> meditationController.deleteNeuralVoice(id) },
                        onOpenGlossary = { navController.navigate(GlossaryRoute) },
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onBack = { navController.popBackStack() },
                        onExportPatterns = patternTransfer.export,
                        onImportPatterns = patternTransfer.import,
                        dataStatus = DataStatus(patternTransfer.exportLabel, patternTransfer.importLabel, patternTransfer.note),
                        focusMeditation = entry.toRoute<SettingsRoute>().focusMeditation,
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
                    if (sessionState.status == SessionStatus.PAUSED) {
                        sessionViewModel.resume()
                    } else {
                        sessionViewModel.pause()
                    }
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

        // Vollbild-Meditation über allem (gespiegelt von der Session): minimierbar zur Mini-Leiste.
        if (meditationActive && meditationExpanded) {
            MeditationOverlay(
                state = meditationState,
                onMinimize = { meditationExpanded = false },
                onToggleMute = meditationController::toggleMute,
                onTogglePause = {
                    if (meditationState.status == MeditationStatus.PAUSED) {
                        meditationController.resume()
                    } else {
                        meditationController.pause()
                    }
                },
                onRestart = meditationController::restart,
                onEnd = {
                    meditationController.end()
                    meditationExpanded = false
                },
            )
            BackHandler { meditationExpanded = false }
        }
    }
}

/** Die vier Top-Level-Tabs (für die Markierung der Leiste, auch auf Push-Zielen). */
private enum class TopTab { ATMEN, SITUATIONEN, LOGBUCH, MEDITATION }

/** True, wenn das aktuelle Ziel zu dieser Top-Level-Route gehört. */
private fun androidx.navigation.NavDestination?.isOn(route: Any): Boolean = this?.hierarchy?.any { it.hasRoute(route::class) } == true

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
