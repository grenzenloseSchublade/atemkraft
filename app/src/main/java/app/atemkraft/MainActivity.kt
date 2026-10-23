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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.CueSettings
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.data.SessionDisplay
import app.atemkraft.data.SettingsRepository
import app.atemkraft.data.Situations
import app.atemkraft.domain.GONG_INTERVAL_DEFAULT_MINUTES
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.components.inertWhen
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.detail.rememberDetailExercise
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.learn.LearnScreen
import app.atemkraft.ui.learn.PranayamaScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.meditation.MiniMeditationBar
import app.atemkraft.ui.navigation.AboutRoute
import app.atemkraft.ui.navigation.AppTabBar
import app.atemkraft.ui.navigation.AtemlehreRoute
import app.atemkraft.ui.navigation.AtmenRoute
import app.atemkraft.ui.navigation.DetailRoute
import app.atemkraft.ui.navigation.EigeneRoute
import app.atemkraft.ui.navigation.GlossaryRoute
import app.atemkraft.ui.navigation.HideableTabBar
import app.atemkraft.ui.navigation.LernenRoute
import app.atemkraft.ui.navigation.LocalTabBarState
import app.atemkraft.ui.navigation.LogbookRoute
import app.atemkraft.ui.navigation.MeditationRoute
import app.atemkraft.ui.navigation.PatternEditorRoute
import app.atemkraft.ui.navigation.PranayamaRoute
import app.atemkraft.ui.navigation.SettingsRoute
import app.atemkraft.ui.navigation.SituationenRoute
import app.atemkraft.ui.navigation.TabBarState
import app.atemkraft.ui.navigation.TabBarToggle
import app.atemkraft.ui.navigation.TopTab
import app.atemkraft.ui.navigation.isTabRoot
import app.atemkraft.ui.navigation.startsWithTabBarCollapsed
import app.atemkraft.ui.navigation.tabOnStack
import app.atemkraft.ui.navigation.tapTab
import app.atemkraft.ui.own.OwnPatternsScreen
import app.atemkraft.ui.patterns.PatternEditorDestination
import app.atemkraft.ui.session.MiniSessionBar
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.settings.SettingsViewModel
import app.atemkraft.ui.settings.rememberPatternTransfer
import app.atemkraft.ui.situations.SituationsScreen
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
private fun AtemkraftApp(
    // An der Activity und ab App-Start gesammelt: Beim Öffnen springt kein Schalter (ARC-3).
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val container = (context.applicationContext as AtemkraftApplication).container
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    // Atem-Session app-weit wie die Meditation (Controller im AppContainer, Foreground-Service).
    val breathController = container.breathSessionController
    // In der Wurzel nur der Status: Der volle Zustand tickt während einer Sitzung alle 33 ms und
    // wird erst in [SessionHost] gelesen (Mini-Leiste, Overlay) – sonst setzte die ganze App
    // rund 30-mal pro Sekunde neu an (ARC-3).
    val sessionStatus by breathController.status.collectAsStateWithLifecycle()
    val cueSettings by container.settingsRepository.cueSettings
        .collectAsStateWithLifecycle(initialValue = CueSettings())
    val safetySettings by container.settingsRepository.safetySettings
        .collectAsStateWithLifecycle(initialValue = SafetySettings())
    val logEntries by container.logbookRepository.entries
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val sessionDisplay by container.settingsRepository.sessionDisplay
        .collectAsStateWithLifecycle(initialValue = SessionDisplay())
    val markedExercises by container.settingsRepository.markedExercises
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val meditationController = container.meditationController
    val meditationStatus by meditationController.status.collectAsStateWithLifecycle()
    val meditationConfig by container.settingsRepository.meditationSettings
        .collectAsStateWithLifecycle(initialValue = MeditationConfig())
    val meditationSpeechAvailable by meditationController.speechAvailable
        .collectAsStateWithLifecycle()
    val meditationDisplay by container.settingsRepository.meditationDisplay
        .collectAsStateWithLifecycle(initialValue = MeditationDisplay())
    val gongIntervalMin by container.settingsRepository.gongIntervalMin
        .collectAsStateWithLifecycle(initialValue = GONG_INTERVAL_DEFAULT_MINUTES)

    val sessionActive = sessionStatus != SessionStatus.IDLE
    var sessionExpanded by rememberSaveable { mutableStateOf(false) }

    // „aktiv" (inkl. FINISHED) steuert Overlay + Mini-Leiste; die Wach-Halten-Logik nur bei RUNNING/PREPARING.
    val meditationActive = meditationStatus != MeditationStatus.IDLE
    var meditationExpanded by rememberSaveable { mutableStateOf(false) }

    // Danach in die Stille: Den Übergang macht der Ablauf selbst ([SilenceAfterBreath], auch hinter
    // dem Sperrbildschirm). Die Oberfläche übernimmt nur, ob das Session-Overlay offen war – auch
    // erst beim nächsten Sichtbarwerden, wenn der Übergang im Hintergrund kam.
    val silenceAfterBreath = container.silenceAfterBreath
    LaunchedEffect(meditationActive) {
        if (meditationActive && silenceAfterBreath.consumeHandover()) {
            meditationExpanded = sessionExpanded
            sessionExpanded = false
        }
    }
    // Erlaubnis für die Benachrichtigung beim ersten Start einer Sitzung, gleich welcher Art (ARC-6).
    val askNotifications = rememberNotificationAsk(container.settingsRepository)

    KeepScreenOn(
        enabled = sessionStatus == SessionStatus.RUNNING ||
            sessionStatus == SessionStatus.WAITING_FOR_USER ||
            meditationStatus == MeditationStatus.RUNNING ||
            meditationStatus == MeditationStatus.PREPARING,
    )

    val guidedByFamily = remember {
        container.exerciseRepository.byFamily()
            .map { (family, list) -> family to list.filter { it.guided } }
            .filter { (_, list) -> list.isNotEmpty() }
    }
    val programs = remember { container.exerciseRepository.all().filter { !it.guided } }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // Tab-Leiste in der Tiefe: klappt beim Runterscrollen ein, Lasche holt sie zurück; jede neue
    // Seite startet frisch (Muster-Editor eingeklappt).
    val density = LocalDensity.current
    val tabBar = remember(density) { TabBarState(threshold = with(density) { Dimens.SectionGap.toPx() }) }
    LaunchedEffect(navBackStackEntry?.id) { tabBar.enter(startCollapsed = currentDestination.startsWithTabBarCollapsed()) }

    // Markiert ist der Tab, zu dessen Stapel die Seite gehört – auch auf Push-Seiten und nach
    // einem wiederhergestellten Stapel (Eigene › Editor, dann Meditation und zurück); ein erneuter
    // Tipp darauf führt zu seiner Startseite zurück (tapTab).
    val selectedTab = remember(navBackStackEntry) { navController.tabOnStack() }

    // Liegt ein Overlay über allem, ist die App darunter für TalkBack und Tastatur nicht da
    // (A11Y-07, MUSTER-07): Sonst führte Wischen hinter „Beenden“ auf unsichtbare Karten und Tabs.
    val overlayOpen = (sessionActive && sessionExpanded) || (meditationActive && meditationExpanded)

    Box(modifier = Modifier.fillMaxSize()) {
        // Laufende Session/Meditation liegen bei Bedarf als Vollbild-Overlay über dem Scaffold;
        // minimiert erscheinen sie als „Now-Playing"-Mini-Leiste über der Tab-Leiste.
        Scaffold(
            modifier = Modifier.inertWhen(overlayOpen),
            bottomBar = {
                // Ihre Oberkante trägt den Knopf der Tab-Leiste, solange die Seite keine Startleiste hat.
                Column(Modifier.onGloballyPositioned { tabBar.bottomBarTop = it.positionInRoot().y }) {
                    if (sessionActive && !sessionExpanded) {
                        SessionHost(breathController.state) { state ->
                            MiniSessionBar(state = state, onClick = { sessionExpanded = true })
                        }
                    }
                    if (meditationActive && !meditationExpanded) {
                        SessionHost(meditationController.state) { state ->
                            MiniMeditationBar(state = state, onClick = { meditationExpanded = true })
                        }
                    }
                    // Auf den fünf Tabs immer; in der Tiefe, bis gescrollt oder per Knopf zugeklappt wird.
                    HideableTabBar(visible = currentDestination.isTabRoot() || !tabBar.collapsed) {
                        AppTabBar(selected = selectedTab, onTap = { tab -> navController.tapTab(tab.route, selected = tab == selectedTab) })
                    }
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(LocalTabBarState provides tabBar) {
                // Push-Ziele (Detail/Settings/…) gleiten hierarchisch herein/heraus ([pushComposable]);
                // Tab-Wechsel blenden nur dezent – kein Ghosting mehr.
                NavHost(
                    navController = navController,
                    startDestination = TopTab.START.route,
                    // Tablets/Foldables: Inhalte auf lesbare Breite begrenzen und mittig setzen;
                    // die Seitenränder zeigen den (identischen) Fensterhintergrund.
                    modifier = Modifier
                        .padding(innerPadding)
                        // Misst das Scrollen jeder Seite für die Tab-Leiste, ohne etwas zu verbrauchen.
                        .nestedScroll(tabBar.connection)
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 600.dp),
                    enterTransition = { fadeIn(tween(Motion.NavTab)) },
                    exitTransition = { fadeOut(tween(Motion.NavTab)) },
                    popEnterTransition = { fadeIn(tween(Motion.NavTab)) },
                    popExitTransition = { fadeOut(tween(Motion.NavTab)) },
                ) {
                    composable<AtmenRoute> {
                        var dailyPattern by remember {
                            mutableStateOf(container.exerciseRepository.daily())
                        }
                        // „Gespeichert" direkt aus der DB ableiten: deckt Speichern, App-Neustart am
                        // selben Tag und Namensgleichheit mit einem früher gespeicherten Tagesmuster ab;
                        // ein eigenes Muster mit gleichen Zahlen zählt nicht (Herkunft).
                        val savedPatterns by container.savedPatternsRepository.patterns
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        val dailySaved = container.savedPatternsRepository.isDailySaved(dailyPattern, savedPatterns)
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
                            markedIds = markedExercises,
                            onOpenPatternEditor = { navController.navigate(PatternEditorRoute()) },
                            onOpenLogbook = { navController.navigate(LogbookRoute) { launchSingleTop = true } },
                        )
                    }
                    composable<SituationenRoute> {
                        SituationsScreen(
                            recommendations = Situations.all,
                            resolve = { id -> container.exerciseRepository.byId(id) },
                            onSelect = { id -> navController.navigate(DetailRoute(id)) },
                            markedIds = markedExercises,
                        )
                    }
                    composable<EigeneRoute> {
                        val savedPatterns by container.savedPatternsRepository.patterns
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        OwnPatternsScreen(
                            savedPatterns = savedPatterns,
                            onNewPattern = { navController.navigate(PatternEditorRoute()) },
                            // „Tut mir gut“: markierte eingebaute Übungen in Familien-Reihenfolge.
                            favorites = container.exerciseRepository.all().filter { it.id in markedExercises },
                            onSelect = { id -> navController.navigate(DetailRoute(id)) },
                            onDeletePattern = { id ->
                                scope.launch { container.savedPatternsRepository.delete(id) }
                            },
                            onOpenSettings = { navController.navigate(SettingsRoute()) },
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
                            // Raste des Zeitrings: leicht je Minute, deutlicher je 5. Bedien-Rückmeldung,
                            // kein Session-Cue: unabhängig vom App-Schalter, Android regelt sie (AUDIO-05).
                            onDurationTick = { major -> container.hapticPlayer.dialTick(strong = major) },
                            onStart = { config ->
                                scope.launch { container.settingsRepository.setMeditationConfig(config) }
                                meditationController.start(config)
                                meditationExpanded = true // Vollbild-Overlay öffnen (wie Session-Start)
                                askNotifications()
                            },
                        )
                    }
                    composable<LernenRoute> {
                        LearnScreen(
                            onOpenAtemlehre = { navController.navigate(AtemlehreRoute()) },
                            onOpenPranayama = { navController.navigate(PranayamaRoute) },
                            onOpenGlossary = { navController.navigate(GlossaryRoute) },
                        )
                    }
                    pushComposable<DetailRoute> { entry ->
                        val id = entry.toRoute<DetailRoute>().exerciseId
                        // Eingebaute Übungen und das Tagesmuster einmal auflösen (ARC-5); gespeicherte
                        // Muster folgen der DB und zeigen nach dem Bearbeiten im Editor die neuen Werte.
                        val lookup = rememberDetailExercise(
                            id = id,
                            resolve = container.exerciseRepository::byId,
                            live = { exerciseId ->
                                SavedPatternsRepository.idOf(exerciseId)?.let { container.savedPatternsRepository.observe(exerciseId) }
                            },
                        )
                        val exercise = lookup.exercise
                        if (exercise == null) {
                            // Unauflösbare Route (z. B. gelöschtes Muster): zurück statt leerer Screen
                            // (CODE-05) – nur, solange dieses Ziel oben liegt. Lädt das Muster noch, bleibt
                            // die Seite kurz leer.
                            if (lookup.missing) {
                                LaunchedEffect(entry.id) {
                                    if (navController.currentBackStackEntry?.id == entry.id) navController.popBackStack()
                                }
                            }
                            return@pushComposable
                        }
                        val savedPatternId = SavedPatternsRepository.idOf(id)
                        val requireSafety = exercise.caution &&
                            (!safetySettings.acknowledged || safetySettings.showWarning)
                        // Intervall-Anpassungen pro Übung merken – außer beim Muster des Tages: dessen
                        // Id ist konstant, der Inhalt wechselt aber täglich; gespeicherte Werte gälten
                        // sonst morgen für ein anderes Muster.
                        val persistIntervals = id != RandomPatternGenerator.ID
                        // Markieren nur eingebaute Übungen: Ihre Id ist stabil und ihre Karten stehen
                        // in den Listen; Muster wechseln oder lassen sich löschen.
                        val markable = exercise in BuiltInExercises.all
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
                                breathController.start(exercise, config)
                                sessionExpanded = true
                                navController.popBackStack()
                                askNotifications()
                            },
                            onBack = { navController.popBackStack() },
                            onOpenLore = { patternId -> navController.navigate(AtemlehreRoute(patternId)) },
                            marked = id in markedExercises,
                            onMarkedChange = if (markable) {
                                { marked -> scope.launch { container.settingsRepository.setExerciseMarked(id, marked) } }
                            } else {
                                null
                            },
                            // Stift nur an generierten Mustern: das Tagesmuster als eigenes anpassen,
                            // ein gespeichertes bearbeiten.
                            onEditPattern = when {
                                // Die Zahlen genau dieses Musters mitgeben: Ein erneutes daily() im
                                // Editor wäre nach Mitternacht schon das nächste.
                                id == RandomPatternGenerator.ID -> {
                                    {
                                        val shown = RandomPatternGenerator.specOf(exercise)
                                        navController.navigate(shown?.let(PatternEditorRoute::of) ?: PatternEditorRoute(fromDaily = true))
                                    }
                                }

                                savedPatternId != null -> {
                                    { navController.navigate(PatternEditorRoute(patternId = savedPatternId)) }
                                }

                                else -> null
                            },
                        )
                    }
                    pushComposable<PatternEditorRoute> { entry ->
                        val route = entry.toRoute<PatternEditorRoute>()
                        PatternEditorDestination(
                            patternId = route.patternId,
                            fromDaily = route.fromDaily,
                            repository = container.savedPatternsRepository,
                            dailySpec = { route.dailySpec() ?: container.exerciseRepository.daily().spec },
                            scope = scope,
                            // Ausprobieren ohne Speichern: Der Editor bleibt unter der Session offen –
                            // probieren, nachstellen, noch einmal probieren.
                            onStart = { exercise, config ->
                                breathController.start(exercise, config)
                                sessionExpanded = true
                                askNotifications()
                            },
                            onOpenLore = { patternId -> navController.navigate(AtemlehreRoute(patternId)) },
                            onBack = { navController.popBackStack() },
                            // Muster gelöscht: zurück, nur solange der Editor oben liegt (CODE-05).
                            onMissing = {
                                if (navController.currentBackStackEntry?.id == entry.id) navController.popBackStack()
                            },
                            // Vorschau-Ton mit den Klängen der App, still neben einer laufenden Übung.
                            cueSettings = cueSettings,
                            otherAudioActive = sessionActive || meditationActive,
                            // Intensive eigene Muster: derselbe Sicherheitsdialog wie auf der Detailseite.
                            requireSafetyConfirm = !safetySettings.acknowledged || safetySettings.showWarning,
                            onConfirmedSafety = {
                                scope.launch { container.settingsRepository.setSafetyAcknowledged(true) }
                            },
                        )
                    }
                    pushComposable<SettingsRoute> { entry ->
                        // TTS vorbereiten, damit die Stimmen-Auswahl auch hier gefüllt ist.
                        LaunchedEffect(Unit) { meditationController.prepareSpeech() }
                        val patternTransfer = rememberPatternTransfer(
                            container.savedPatternsRepository,
                            container.settingsRepository,
                        )
                        val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
                        SettingsScreen(
                            state = settingsState,
                            actions = settingsViewModel.actions,
                            onOpenAbout = { navController.navigate(AboutRoute) },
                            onBack = { navController.popBackStack() },
                            transfer = patternTransfer,
                            focusMeditation = entry.toRoute<SettingsRoute>().focusMeditation,
                        )
                    }
                    pushComposable<LogbookRoute> {
                        LogbookScreen(entries = logEntries, onBack = { navController.popBackStack() })
                    }
                    pushComposable<GlossaryRoute> {
                        GlossaryScreen(onBack = { navController.popBackStack() })
                    }
                    pushComposable<AtemlehreRoute> { entry ->
                        AtemlehreScreen(
                            onBack = { navController.popBackStack() },
                            onOpenExercise = { id -> navController.navigate(DetailRoute(id)) },
                            focus = entry.toRoute<AtemlehreRoute>().focus,
                        )
                    }
                    pushComposable<PranayamaRoute> {
                        PranayamaScreen(
                            onBack = { navController.popBackStack() },
                            onOpenExercise = { id -> navController.navigate(DetailRoute(id)) },
                        )
                    }
                    pushComposable<AboutRoute> {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }

        // Knopf der Tab-Leiste in der Tiefe: auf der Oberkante der Startleiste bzw. der unteren Leisten,
        // über dem Inhalt; Overlays darüber verdecken ihn.
        if (!currentDestination.isTabRoot()) TabBarToggle(tabBar, Modifier.align(Alignment.TopCenter))

        // Vollbild-Session über allem; System-Zurück regelt das Overlay selbst (minimieren, am
        // Ende-Screen wie „Ende“).
        if (sessionActive && sessionExpanded) {
            SessionHost(breathController.state) { state ->
                SessionOverlay(
                    state = state,
                    display = sessionDisplay,
                    onMinimize = { sessionExpanded = false },
                    onToggleMute = breathController::toggleMute,
                    onTogglePause = {
                        if (state.status == SessionStatus.PAUSED) {
                            breathController.resume()
                        } else {
                            breathController.pause()
                        }
                    },
                    // Tipp-Tick nur bei eingeschaltetem App-Schalter „Vibration“ (AUDIO-05).
                    onTapHaptic = { container.hapticPlayer.tick(enabled = cueSettings.haptics) },
                    onContinue = breathController::continueFromUserPaced,
                    onRestart = breathController::restart,
                    onStop = {
                        breathController.stop()
                        sessionExpanded = false
                    },
                    // Derselbe Weg wie „Danach in die Stille“; nicht nach Atmen im Gehen.
                    onIntoSilence = if (state.silenceAfterAllowed) silenceAfterBreath::intoSilence else null,
                    // „Im Logbuch ansehen ›“ am Ende: beendet wie „Ende“ und öffnet das Logbuch.
                    onOpenLogbook = {
                        breathController.stop()
                        sessionExpanded = false
                        navController.navigate(LogbookRoute) { launchSingleTop = true }
                    },
                )
            }
        }

        // Vollbild-Meditation über allem (gespiegelt von der Session): minimierbar zur Mini-Leiste.
        if (meditationActive && meditationExpanded) {
            SessionHost(meditationController.state) { state ->
                MeditationOverlay(
                    state = state,
                    display = meditationDisplay,
                    onMinimize = { meditationExpanded = false },
                    onToggleMute = meditationController::toggleMute,
                    onTogglePause = {
                        if (state.status == MeditationStatus.PAUSED) {
                            meditationController.resume()
                        } else {
                            meditationController.pause()
                        }
                    },
                    onTapHaptic = { container.hapticPlayer.tick(enabled = cueSettings.haptics) },
                    onRestart = meditationController::restart,
                    onEnd = {
                        meditationController.end()
                        meditationExpanded = false
                    },
                    onOpenLogbook = {
                        meditationController.end()
                        meditationExpanded = false
                        navController.navigate(LogbookRoute) { launchSingleTop = true }
                    },
                )
            }
        }
    }
}

/**
 * Sammelt den vollen Sitzungszustand erst hier: Nur dieser Teilbaum (Mini-Leiste oder Overlay)
 * setzt bei jedem Frame-Tick neu an, nicht die App-Wurzel (ARC-3).
 */
@Composable
private fun <T> SessionHost(state: StateFlow<T>, content: @Composable (T) -> Unit) {
    val current by state.collectAsStateWithLifecycle()
    content(current)
}

/**
 * Push-Ziel (Detail, Einstellungen, …): gleitet hierarchisch herein und beim Zurück wieder hinaus,
 * jeweils mit Blende. Ein Ort für die vier Übergänge statt fünf Kopien (S-29).
 */
private inline fun <reified T : Any> NavGraphBuilder.pushComposable(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(Motion.NavPush)) +
                fadeIn(tween(Motion.NavPush))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(Motion.NavPush)) +
                fadeOut(tween(Motion.NavPush))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(Motion.NavPush)) +
                fadeIn(tween(Motion.NavPush))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(Motion.NavPush)) +
                fadeOut(tween(Motion.NavPush))
        },
        content = content,
    )
}

/**
 * Fragt ab Android 13 einmalig nach der Benachrichtigungs-Erlaubnis – im Moment, in dem sie Sinn
 * ergibt: beim ersten Start einer Sitzung (Meditation oder Atemübung), nicht beim App-Start
 * (ARC-6). Gebraucht wird sie nur für die Benachrichtigung der laufenden Sitzung; bei Ablehnung
 * läuft die Sitzung trotzdem. Weil auch die Atemübung fragt, ist sie vor „Danach in die Stille“
 * geklärt – den Übergang startet der Ablauf ohne Oberfläche. Danach fragt die App nie wieder. Bis der gespeicherte Stand geladen ist, gilt „schon gefragt“ – lieber einmal nicht
 * fragen als doppelt.
 */
@Composable
private fun rememberNotificationAsk(settings: SettingsRepository): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val asked by settings.notificationAsked.collectAsStateWithLifecycle(initialValue = true)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !asked &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            scope.launch { settings.setNotificationAsked() }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
