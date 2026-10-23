package app.atemkraft

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.atemkraft.data.Areas
import app.atemkraft.data.Pranayama
import app.atemkraft.data.SafetySettings
import app.atemkraft.data.Situations
import app.atemkraft.domain.GONG_INTERVAL_DEFAULT_MINUTES
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.detail.DetailDestination
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.learn.LearnScreen
import app.atemkraft.ui.learn.PranayamaScreen
import app.atemkraft.ui.learn.ProgramsScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.navigation.AboutRoute
import app.atemkraft.ui.navigation.AtemlehreRoute
import app.atemkraft.ui.navigation.AtmenRoute
import app.atemkraft.ui.navigation.DetailRoute
import app.atemkraft.ui.navigation.EigeneRoute
import app.atemkraft.ui.navigation.GlossaryRoute
import app.atemkraft.ui.navigation.LernenRoute
import app.atemkraft.ui.navigation.LogbookRoute
import app.atemkraft.ui.navigation.MeditationRoute
import app.atemkraft.ui.navigation.PatternEditorRoute
import app.atemkraft.ui.navigation.PranayamaRoute
import app.atemkraft.ui.navigation.ProgramsRoute
import app.atemkraft.ui.navigation.SettingsRoute
import app.atemkraft.ui.own.OwnPatternsScreen
import app.atemkraft.ui.patterns.PatternEditorDestination
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.settings.SettingsViewModel
import app.atemkraft.ui.settings.rememberPatternTransfer
import app.atemkraft.ui.theme.Motion
import kotlinx.coroutines.launch

/**
 * Was die Ziele des Graphen aus den Repositories lesen, gesammelt in der App-Wurzel ab App-Start:
 * Beim Öffnen einer Seite springt nichts von einem Standardwert auf den gespeicherten (ARC-3).
 */
@Stable
internal class GraphState(
    safetySettings: State<SafetySettings>,
    markedExercises: State<Set<String>>,
    logEntries: State<List<SessionLogEntry>>,
    meditationConfig: State<MeditationConfig>,
    meditationSpeechAvailable: State<Boolean>,
    gongIntervalMin: State<Int>,
) {
    val safetySettings by safetySettings
    val markedExercises by markedExercises
    val logEntries by logEntries
    val meditationConfig by meditationConfig
    val meditationSpeechAvailable by meditationSpeechAvailable
    val gongIntervalMin by gongIntervalMin
}

@Composable
internal fun rememberGraphState(container: AppContainer): GraphState {
    val settings = container.settingsRepository
    val safety = settings.safetySettings.collectAsStateWithLifecycle(initialValue = SafetySettings())
    val marked = settings.markedExercises.collectAsStateWithLifecycle(initialValue = emptySet())
    val log = container.logbookRepository.entries.collectAsStateWithLifecycle(initialValue = emptyList())
    val meditationConfig = settings.meditationSettings.collectAsStateWithLifecycle(initialValue = MeditationConfig())
    val speech = container.meditationController.speechAvailable.collectAsStateWithLifecycle()
    val gong = settings.gongIntervalMin.collectAsStateWithLifecycle(initialValue = GONG_INTERVAL_DEFAULT_MINUTES)
    return remember(safety, marked, log, meditationConfig, speech, gong) {
        GraphState(safety, marked, log, meditationConfig, speech, gong)
    }
}

/**
 * Die Ziele der App je Feature: die vier Tabs ([app.atemkraft.ui.navigation.TopTab]) und die
 * Push-Seiten ([pushComposable]). Schreibvorgänge laufen im App-weiten Scope
 * ([AppContainer.appScope]); Sitzungen starten über [playback].
 */
internal fun NavGraphBuilder.appGraph(
    navController: NavController,
    container: AppContainer,
    playback: PlaybackState,
    state: GraphState,
    settingsViewModel: SettingsViewModel,
) {
    val scope = container.appScope
    val exercises = container.exerciseRepository
    val savedPatterns = container.savedPatternsRepository
    val settings = container.settingsRepository
    val back: () -> Unit = { navController.popBackStack() }
    val openDetail: (String) -> Unit = { id -> navController.navigate(DetailRoute(id)) }
    val openLore: (String) -> Unit = { patternId -> navController.navigate(AtemlehreRoute(patternId)) }

    composable<AtmenRoute> {
        // Beim Betreten den Tageswechsel prüfen; danach folgt die Karte dem Repository.
        remember { exercises.currentDaily() }
        val dailyPattern by exercises.daily.collectAsStateWithLifecycle()
        // „Gespeichert" direkt aus der DB ableiten: deckt Speichern, App-Neustart am
        // selben Tag und Namensgleichheit mit einem früher gespeicherten Tagesmuster ab;
        // ein eigenes Muster mit gleichen Zahlen zählt nicht (Herkunft).
        val patterns by savedPatterns.patterns.collectAsStateWithLifecycle(initialValue = emptyList())
        HomeScreen(
            daily = dailyPattern,
            dailySaved = savedPatterns.isDailySaved(dailyPattern, patterns),
            onRegenerateDaily = exercises::regenerateDaily,
            onDailySavedChange = { save ->
                val pattern = dailyPattern
                scope.launch {
                    if (save) savedPatterns.save(pattern) else savedPatterns.unsave(pattern)
                }
            },
            exercisesByFamily = exercises.guidedByFamily,
            situations = Situations.all,
            areas = Areas.all,
            resolve = { id -> exercises.byId(id) },
            yogaExerciseIds = Pranayama.exerciseIds,
            onSelect = openDetail,
            onOpenSettings = { navController.navigate(SettingsRoute()) },
            markedIds = state.markedExercises,
            onOpenPatternEditor = { navController.navigate(PatternEditorRoute()) },
        )
    }
    composable<EigeneRoute> {
        val patterns by savedPatterns.patterns.collectAsStateWithLifecycle(initialValue = emptyList())
        OwnPatternsScreen(
            savedPatterns = patterns,
            onNewPattern = { navController.navigate(PatternEditorRoute()) },
            // „Tut mir gut“: markierte eingebaute Übungen in Familien-Reihenfolge.
            favorites = exercises.all().filter { it.id in state.markedExercises },
            onSelect = openDetail,
            onDeletePattern = { id -> scope.launch { savedPatterns.delete(id) } },
            onOpenSettings = { navController.navigate(SettingsRoute()) },
            logEntries = state.logEntries,
            onOpenLogbook = { navController.navigate(LogbookRoute) { launchSingleTop = true } },
        )
    }
    composable<MeditationRoute> {
        // TTS-Engine erst hier vorbereiten (nicht beim App-Start), idempotent.
        LaunchedEffect(Unit) { container.meditationController.prepareSpeech() }
        MeditationScreen(
            initialConfig = state.meditationConfig,
            speechAvailable = state.meditationSpeechAvailable,
            gongIntervalMin = state.gongIntervalMin,
            onOpenSettings = { navController.navigate(SettingsRoute(focusMeditation = true)) },
            // Raste des Zeitrings: leicht je Minute, deutlicher je 5. Bedien-Rückmeldung,
            // kein Session-Cue: unabhängig vom App-Schalter, Android regelt sie (AUDIO-05).
            onDurationTick = { major -> container.hapticPlayer.dialTick(strong = major) },
            onStart = { config ->
                scope.launch { settings.setMeditationConfig(config) }
                playback.startMeditation(config)
            },
        )
    }
    composable<LernenRoute> {
        LearnScreen(
            onOpenAtemlehre = { navController.navigate(AtemlehreRoute()) },
            onOpenPranayama = { navController.navigate(PranayamaRoute) },
            onOpenPrograms = { navController.navigate(ProgramsRoute) },
            onOpenGlossary = { navController.navigate(GlossaryRoute) },
        )
    }
    pushComposable<DetailRoute> { entry ->
        DetailDestination(
            exerciseId = entry.toRoute<DetailRoute>().exerciseId,
            exercises = exercises,
            savedPatterns = savedPatterns,
            settings = settings,
            scope = scope,
            safety = state.safetySettings,
            markedIds = state.markedExercises,
            onStart = { exercise, config ->
                playback.startBreath(exercise, config)
                navController.popBackStack()
            },
            onOpenLore = openLore,
            onOpenEditor = { route -> navController.navigate(route) },
            onBack = back,
            // Unauflösbare Route (z. B. gelöschtes Muster): zurück, nur solange diese Seite oben liegt.
            onMissing = { navController.popIfTop(entry) },
        )
    }
    pushComposable<PatternEditorRoute> { entry ->
        val route = entry.toRoute<PatternEditorRoute>()
        PatternEditorDestination(
            patternId = route.patternId,
            fromDaily = route.fromDaily,
            repository = savedPatterns,
            dailySpec = { route.dailySpec() ?: exercises.currentDaily().spec },
            scope = scope,
            // Ausprobieren ohne Speichern: Der Editor bleibt unter der Session offen –
            // probieren, nachstellen, noch einmal probieren.
            onStart = playback::startBreath,
            onOpenLore = openLore,
            onBack = back,
            // Muster gelöscht: zurück, nur solange der Editor oben liegt (CODE-05).
            onMissing = { navController.popIfTop(entry) },
            // Vorschau-Ton mit den Klängen der App, still neben einer laufenden Übung.
            cueSettings = playback.cueSettings,
            otherAudioActive = playback.anyActive,
            // Intensive eigene Muster: derselbe Sicherheitsdialog wie auf der Detailseite.
            safety = state.safetySettings,
            onConfirmedSafety = {
                scope.launch { settings.setSafetyAcknowledged(true) }
            },
        )
    }
    pushComposable<SettingsRoute> { entry ->
        // TTS vorbereiten, damit die Stimmen-Auswahl auch hier gefüllt ist.
        LaunchedEffect(Unit) { container.meditationController.prepareSpeech() }
        val patternTransfer = rememberPatternTransfer(savedPatterns, settings)
        val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
        SettingsScreen(
            state = settingsState,
            actions = settingsViewModel.actions,
            onOpenAbout = { navController.navigate(AboutRoute) },
            onBack = back,
            transfer = patternTransfer,
            focusMeditation = entry.toRoute<SettingsRoute>().focusMeditation,
        )
    }
    pushComposable<LogbookRoute> {
        LogbookScreen(entries = state.logEntries, onBack = back)
    }
    pushComposable<GlossaryRoute> {
        GlossaryScreen(onBack = back)
    }
    pushComposable<AtemlehreRoute> { entry ->
        AtemlehreScreen(
            onBack = back,
            onOpenExercise = openDetail,
            focus = entry.toRoute<AtemlehreRoute>().focus,
        )
    }
    pushComposable<PranayamaRoute> {
        PranayamaScreen(onBack = back, onOpenExercise = openDetail)
    }
    pushComposable<ProgramsRoute> {
        // Programme: Infoseiten ohne Start, gelesen statt geübt (Nutzer 2026-10-23).
        ProgramsScreen(programs = exercises.programs, onBack = back, onOpenExercise = openDetail, markedIds = state.markedExercises)
    }
    pushComposable<AboutRoute> {
        AboutScreen(onBack = back)
    }
}

/** Zurück, nur solange [entry] oben liegt: Ein spätes „fehlt“ nimmt keine fremde Seite vom Stapel. */
private fun NavController.popIfTop(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
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
