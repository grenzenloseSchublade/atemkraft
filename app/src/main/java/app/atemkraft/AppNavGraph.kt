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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.GONG_INTERVAL_DEFAULT_MINUTES
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.OwnCollection
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.UncaughtErrors
import app.atemkraft.domain.toFreePattern
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.detail.DetailDestination
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeActions
import app.atemkraft.ui.home.HomeCatalog
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
import app.atemkraft.ui.navigation.OwnCreationsRoute
import app.atemkraft.ui.navigation.OwnFavoritesRoute
import app.atemkraft.ui.navigation.PatternEditorRoute
import app.atemkraft.ui.navigation.PranayamaRoute
import app.atemkraft.ui.navigation.ProgramsRoute
import app.atemkraft.ui.navigation.SettingsRoute
import app.atemkraft.ui.own.OwnActions
import app.atemkraft.ui.own.OwnCreationsScreen
import app.atemkraft.ui.own.OwnFavoritesScreen
import app.atemkraft.ui.own.OwnPatternsScreen
import app.atemkraft.ui.own.OwnView
import app.atemkraft.ui.patterns.PatternEditorDestination
import app.atemkraft.ui.patterns.PatternEditorPreload
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
    welcomeDismissed: State<Boolean>,
    showStreak: State<Boolean>,
    historyWeeksOpen: State<Boolean>,
    ownCollection: State<OwnCollection>,
) {
    val safetySettings by safetySettings
    val markedExercises by markedExercises
    val logEntries by logEntries
    val meditationConfig by meditationConfig
    val meditationSpeechAvailable by meditationSpeechAvailable
    val gongIntervalMin by gongIntervalMin
    val welcomeDismissed by welcomeDismissed
    val showStreak by showStreak
    val historyWeeksOpen by historyWeeksOpen
    val ownCollection by ownCollection
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
    // Bis DataStore antwortet, gilt die Karte als weggeklickt: Wer sie schon geschlossen hat, sieht
    // sie beim Start nicht kurz aufblitzen; beim ersten Start erscheint sie einen Augenblick später.
    val welcome = settings.welcomeDismissed.collectAsStateWithLifecycle(initialValue = true)
    val streak = settings.showStreak.collectAsStateWithLifecycle(initialValue = true)
    val weeksOpen = settings.historyWeeksOpen.collectAsStateWithLifecycle(initialValue = false)
    val collection = settings.ownCollection.collectAsStateWithLifecycle(initialValue = OwnCollection.CREATIONS)
    return remember(safety, marked, log, meditationConfig, speech, gong, welcome, streak, weeksOpen, collection) {
        GraphState(safety, marked, log, meditationConfig, speech, gong, welcome, streak, weeksOpen, collection)
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
    // Nur, was sich noch öffnen lässt: eingebaute Übungen und vorhandene gespeicherte Muster – nicht
    // die Meditation, nicht das Tagesmuster (heute ein anderes) oder der Entwurf (Logbuch-Einträge;
    // „Zuletzt geübt“ nach derselben Regel über lastPracticed).
    val openable: (String) -> Boolean = { id -> id != ExerciseIds.DAILY && id != ExerciseIds.DRAFT && exercises.byId(id) != null }

    composable<AtmenRoute> {
        // Beim Betreten und bei jeder Rückkehr in die App den Tageswechsel prüfen (die App kann
        // über Mitternacht auf diesem Tab stehen); danach folgt die Karte dem Repository.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { exercises.currentDaily() }
        val dailyPattern by exercises.daily.collectAsStateWithLifecycle()
        // „Gespeichert“ direkt aus der DB ableiten: deckt Speichern, App-Neustart am
        // selben Tag und ein früher gespeichertes Tagesmuster mit gleichen Zahlen ab;
        // ein eigenes Muster mit gleichen Zahlen zählt nicht (Herkunft).
        val patterns by savedPatterns.patterns.collectAsStateWithLifecycle(initialValue = emptyList())
        val catalog = remember {
            HomeCatalog(
                exercisesByFamily = exercises.guidedByFamily,
                situations = Situations.all,
                areas = Areas.all,
                yogaExerciseIds = Pranayama.exerciseIds,
                resolve = { id -> exercises.byId(id) },
            )
        }
        HomeScreen(
            catalog = catalog,
            daily = dailyPattern,
            dailySaved = savedPatterns.isDailySaved(dailyPattern, patterns),
            actions = HomeActions(
                onRegenerateDaily = exercises::regenerateDaily,
                onDailySavedChange = { save ->
                    val pattern = dailyPattern
                    scope.launch {
                        if (save) savedPatterns.save(pattern) else savedPatterns.unsave(pattern)
                    }
                },
                onSelect = openDetail,
                onOpenSettings = { navController.navigate(SettingsRoute()) },
                onDismissWelcome = { scope.launch { settings.dismissWelcome() } },
                onDesignFrom = { spec -> navController.navigate(PatternEditorRoute.of(spec)) },
                // Die Karte verspricht „ein paar Minuten“: Die Resonanz-Atmung öffnet mit 3 statt 10 min.
                onOpenWelcome = { id -> navController.navigate(DetailRoute(id, minutes = WELCOME_MINUTES)) },
            ),
            markedIds = state.markedExercises,
            // Erster Start: ruhiger Einstieg mit Gesundheitshinweis, bis der Nutzer ihn wegklickt.
            welcome = if (state.welcomeDismissed) null else exercises.byId(WELCOME_EXERCISE_ID),
            // „Selbst gestalten“ öffnet den Editor mit dem Tagesmuster: vorab messen, solange das Blatt steht.
            whileDailySheetOpen = { PatternEditorPreload(dailyPattern.spec.toFreePattern()) },
        )
    }
    composable<EigeneRoute> {
        val patterns by savedPatterns.patterns.collectAsStateWithLifecycle(initialValue = emptyList())
        // „Zuletzt geübt“: die neuesten Übungen, die es noch gibt (Liste neueste zuerst), je einmal;
        // neu berechnet, wenn ein Muster gelöscht oder umbenannt wird. Dieselbe Regel wie der Shortcut.
        val recent = remember(state.logEntries, patterns) { recentlyPracticed(state.logEntries, patterns, limit = RECENT_LIMIT) }
        OwnPatternsScreen(
            savedPatterns = patterns,
            actions = OwnActions(
                onNewPattern = { navController.navigate(PatternEditorRoute()) },
                onSelect = openDetail,
                onDeletePattern = { id -> scope.launch { savedPatterns.delete(id) } },
                onOpenSettings = { navController.navigate(SettingsRoute()) },
                onOpenLogbook = { navController.navigate(LogbookRoute) { launchSingleTop = true } },
                // Stift auf der Karte: derselbe Editor wie „Anpassen“ auf der Detailseite des Musters.
                onEditPattern = { id -> navController.navigate(PatternEditorRoute(patternId = id)) },
                onWeeksOpenChange = { open -> scope.launch { settings.setHistoryWeeksOpen(open) } },
                onCollectionChange = { collection -> scope.launch { settings.setOwnCollection(collection) } },
                onOpenAllCreations = { navController.navigate(OwnCreationsRoute) { launchSingleTop = true } },
                onOpenAllFavorites = { navController.navigate(OwnFavoritesRoute) { launchSingleTop = true } },
            ),
            // „Tut mir gut“: markierte eingebaute Übungen in Familien-Reihenfolge.
            favorites = exercises.all().filter { it.id in state.markedExercises },
            logEntries = state.logEntries,
            recentExercises = recent,
            view = OwnView(showStreak = state.showStreak, weeksOpen = state.historyWeeksOpen, collection = state.ownCollection),
        )
        // „+“ öffnet den Editor mit der Vorgabe 4·6: vorab messen, solange der Tab zu sehen ist.
        PatternEditorPreload(FreePattern.DEFAULT)
    }
    composable<MeditationRoute> {
        // System-TTS erst hier anbinden (nicht beim App-Start), idempotent; ein Sprachmodell lädt erst der Start.
        LaunchedEffect(Unit) { container.meditationController.checkSpeech() }
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
            onGongInterval = { minutes -> scope.launch { settings.setGongIntervalMin(minutes) } },
            // Keine deutsche Sprachausgabe: direkt zum aufgeklappten Stimmen-Katalog.
            onOpenVoices = { navController.navigate(SettingsRoute(focusMeditation = true, showVoices = true)) },
        )
    }
    composable<LernenRoute> {
        LearnScreen(
            onOpenSettings = { navController.navigate(SettingsRoute()) },
            onOpenAtemlehre = { navController.navigate(AtemlehreRoute()) },
            onOpenPranayama = { navController.navigate(PranayamaRoute) },
            onOpenPrograms = { navController.navigate(ProgramsRoute) },
            onOpenGlossary = { navController.navigate(GlossaryRoute) },
        )
    }
    pushComposable<DetailRoute> { entry ->
        val route = entry.toRoute<DetailRoute>()
        DetailDestination(
            exerciseId = route.exerciseId,
            initialMinutes = route.minutes,
            exercises = exercises,
            savedPatterns = savedPatterns,
            settings = settings,
            scope = scope,
            safety = state.safetySettings,
            markedIds = state.markedExercises,
            // Die Detailseite geht erst, wenn die Übung läuft (beim ersten Mal nach der Erlaubnis-Frage).
            onStart = { exercise, config ->
                playback.startBreath(exercise, config) { navController.popIfTop(entry) }
            },
            onOpenLore = openLore,
            onOpenEditor = { route -> navController.navigate(route) },
            onBack = back,
            // Unauflösbare Route (z. B. gelöschtes Muster): zurück, nur solange diese Seite oben liegt.
            onMissing = { navController.popIfTop(entry) },
            // Die Seite bleibt unter dem Overlay komponiert (MainActivity, inertWhen).
            covered = playback.overlayOpen,
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
            onConfirmedSafety = { exerciseId ->
                scope.launch { settings.acknowledgeSafety(exerciseId) }
            },
        )
    }
    pushComposable<SettingsRoute> { entry ->
        // System-TTS anbinden, damit die Stimmen-Auswahl auch hier gefüllt ist (ohne Sprachmodell).
        LaunchedEffect(Unit) { container.meditationController.checkSpeech() }
        val patternTransfer = rememberPatternTransfer(savedPatterns, settings, container.logbookRepository)
        val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
        SettingsScreen(
            state = settingsState,
            actions = settingsViewModel.actions,
            onOpenAbout = { navController.navigate(AboutRoute) },
            onBack = back,
            transfer = patternTransfer,
            focusMeditation = entry.toRoute<SettingsRoute>().focusMeditation,
            showVoices = entry.toRoute<SettingsRoute>().showVoices,
        )
    }
    pushComposable<LogbookRoute> {
        LogbookScreen(
            entries = state.logEntries,
            onBack = back,
            canOpen = openable,
            onOpenExercise = openDetail,
            showStreak = state.showStreak,
            weeksOpen = state.historyWeeksOpen,
            onWeeksOpenChange = { open -> scope.launch { settings.setHistoryWeeksOpen(open) } },
        )
    }
    pushComposable<OwnCreationsRoute> {
        val patterns by savedPatterns.patterns.collectAsStateWithLifecycle(initialValue = emptyList())
        OwnCreationsScreen(
            patterns = patterns,
            onBack = back,
            onSelect = openDetail,
            onEdit = { id -> navController.navigate(PatternEditorRoute(patternId = id)) },
            onDelete = { id -> scope.launch { savedPatterns.delete(id) } },
            onNewPattern = { navController.navigate(PatternEditorRoute()) },
        )
    }
    pushComposable<OwnFavoritesRoute> {
        OwnFavoritesScreen(
            favorites = exercises.all().filter { it.id in state.markedExercises },
            onBack = back,
            onSelect = openDetail,
        )
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
        val lastError by UncaughtErrors.last.collectAsStateWithLifecycle()
        AboutScreen(onBack = back, lastError = lastError)
    }
}

/**
 * Übung der Willkommenskarte im Atmen-Tab: die Resonanz-Atmung, die einzige „gut belegte“ Übung,
 * ruhig und ohne Halten (Nutzer 2026-10-24).
 */
private const val WELCOME_EXERCISE_ID = "resonanz"

/**
 * Dauer, mit der die Willkommenskarte die Übung öffnet („ein paar Minuten“), statt ihrer
 * Voreinstellung; nur für diesen Einstieg, nicht als Anpassung gespeichert (Auswertung 2026-10-24).
 */
private const val WELCOME_MINUTES = 3

/** „Zuletzt geübt“ im Tab „Eigene“: höchstens drei Übungen, eine Hand voll Wege zurück, keine Liste. */
private const val RECENT_LIMIT = 3

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
