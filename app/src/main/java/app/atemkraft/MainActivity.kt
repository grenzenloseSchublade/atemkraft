package app.atemkraft

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.atemkraft.ui.components.inertWhen
import app.atemkraft.ui.components.rememberAccessibilityActive
import app.atemkraft.ui.navigation.AppTabBar
import app.atemkraft.ui.navigation.DetailRoute
import app.atemkraft.ui.navigation.HideableTabBar
import app.atemkraft.ui.navigation.LocalTabBarState
import app.atemkraft.ui.navigation.LogbookRoute
import app.atemkraft.ui.navigation.MeditationRoute
import app.atemkraft.ui.navigation.TabBarToggle
import app.atemkraft.ui.navigation.TopTab
import app.atemkraft.ui.navigation.isTabRoot
import app.atemkraft.ui.navigation.rememberTabBarState
import app.atemkraft.ui.navigation.switchTab
import app.atemkraft.ui.navigation.tabOnStack
import app.atemkraft.ui.navigation.tapTab
import app.atemkraft.ui.settings.SettingsViewModel
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App ist dark-only: Systemleisten-Icons immer hell, unabhängig vom System-Hell/Dunkel-Modus.
        // Ohne expliziten Style würden sie im hellen System-Modus dunkel auf dunklem Indigo gezeichnet.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // App-Shortcut (Long-Press auf das Icon): nur die Aktion zählt, geprüft gegen feste Konstanten
        // (AppShortcut.fromAction, SEC-PLAT-02); nach dem Drehen nicht noch einmal.
        val shortcut = if (savedInstanceState == null) AppShortcut.fromAction(intent?.action) else null
        setContent {
            AtemkraftTheme {
                AtemkraftApp(shortcut = shortcut)
            }
        }
    }
}

/**
 * Rahmen der App: Scaffold mit Mini-Leisten und Tab-Leiste unten, darin der NavHost ([appGraph]);
 * darüber der Griff der Tab-Leiste in der Tiefe und die Overlays laufender Sitzungen
 * ([PlaybackOverlays]).
 */
@Composable
private fun AtemkraftApp(
    // Geöffnet über einen App-Shortcut: einmal dorthin navigieren.
    shortcut: AppShortcut? = null,
    // An der Activity und ab App-Start gesammelt: Beim Öffnen springt kein Schalter (ARC-3).
    settingsViewModel: SettingsViewModel = viewModel(factory = appContainer().settingsViewModelFactory),
) {
    val container = appContainer()
    val navController = rememberNavController()
    val playback = rememberPlaybackState(container)
    val graphState = rememberGraphState(container)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // Markiert ist der Tab, zu dessen Stapel die Seite gehört – auch auf Push-Seiten und nach
    // einem wiederhergestellten Stapel (Eigene › Editor, dann Meditation und zurück); ein erneuter
    // Tipp darauf führt zu seiner Startseite zurück (tapTab).
    val selectedTab = remember(navBackStackEntry) { navController.tabOnStack() }

    // Tab-Leiste in der Tiefe: eingeklappt, wer von einem Tab kommt; Griff oder Wischen (Startleiste,
    // Band über den unteren Leisten, Griff) klappen sie auf, das gilt bis zur Rückkehr auf einen Tab.
    val tabBar = rememberTabBarState()
    // Mit Bedienhilfe (TalkBack, Switch Access) bleibt sie auch in der Tiefe offen.
    val accessibility = rememberAccessibilityActive()
    LaunchedEffect(accessibility) { tabBar.keepOpen = accessibility }
    LaunchedEffect(navBackStackEntry?.id) { tabBar.enter(onTab = currentDestination.isTabRoot(), tab = selectedTab) }
    // Was in den Systemgesten am unteren Rand aufsetzt (Home), bleibt dem System.
    val gestureBottom = WindowInsets.systemGestures.getBottom(LocalDensity.current)
    val windowHeight = LocalWindowInfo.current.containerSize.height
    SideEffect { tabBar.gestureZoneTop = (windowHeight - gestureBottom).toFloat() }
    // Oberkante des Inhalts (px im Fenster) für das Wischband über den unteren Leisten.
    var contentTop by remember { mutableFloatStateOf(0f) }

    // Shortcut: Resonanz-Atmung und „Zuletzt geübt“ als Detailseite im Stapel von „Atmen“, die
    // Meditation als Tab. „Zuletzt“ liest die App jetzt selbst aus dem Logbuch – der Shortcut trägt
    // keine Daten. Gibt es nichts mehr zu öffnen, bleibt die App auf ihrer Startseite.
    LaunchedEffect(Unit) {
        if (shortcut == null) return@LaunchedEffect
        // Erst navigieren, wenn der NavHost seinen Graphen gesetzt hat.
        navController.currentBackStackEntryFlow.first()
        when (shortcut) {
            AppShortcut.RESONANCE -> navController.navigate(DetailRoute(AppShortcut.RESONANCE_EXERCISE_ID))

            AppShortcut.MEDITATION -> navController.switchTab(MeditationRoute)

            AppShortcut.LAST -> {
                val last = lastPracticed(container.logbookRepository.entries.first(), container.savedPatternsRepository.patterns.first())
                last?.let { navController.navigate(DetailRoute(it.id)) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Laufende Session/Meditation liegen bei Bedarf als Vollbild-Overlay über dem Scaffold;
        // minimiert erscheinen sie als „Now-Playing“-Mini-Leiste über der Tab-Leiste.
        Scaffold(
            modifier = Modifier.inertWhen(playback.overlayOpen),
            bottomBar = {
                // Ihre Oberkante trägt den Griff der Tab-Leiste, solange die Seite keine Startleiste hat.
                Column(Modifier.onGloballyPositioned { tabBar.bottomBarTop = it.positionInRoot().y }) {
                    PlaybackMiniBars(playback)
                    // Auf den vier Tabs immer; in der Tiefe nur, wenn per Griff oder Wischen aufgeklappt.
                    HideableTabBar(visible = currentDestination.isTabRoot() || !tabBar.collapsed, state = tabBar) {
                        AppTabBar(selected = selectedTab, onTap = { tab -> navController.tapTab(tab.route, selected = tab == selectedTab) })
                    }
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(LocalTabBarState provides tabBar) {
                // Push-Ziele (Detail/Settings/…) gleiten hierarchisch herein/heraus (pushComposable);
                // Tab-Wechsel blenden nur dezent – kein Ghosting mehr.
                NavHost(
                    navController = navController,
                    startDestination = TopTab.START.route,
                    // Tablets/Foldables: Inhalte auf lesbare Breite begrenzen und mittig setzen;
                    // die Seitenränder zeigen den (identischen) Fensterhintergrund.
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                        // Wischen nach oben im unteren Bereich einer Seite ohne Startleiste öffnet die
                        // Tab-Leiste, über die ganze Breite und vor dem Scrollen des Inhalts.
                        .onGloballyPositioned { contentTop = it.positionInRoot().y }
                        .pointerInput(tabBar) { with(tabBar) { detectBottomSwipe { contentTop } } }
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = Dimens.ContentMaxWidth),
                    enterTransition = { fadeIn(tween(Motion.NavTab)) },
                    exitTransition = { fadeOut(tween(Motion.NavTab)) },
                    popEnterTransition = { fadeIn(tween(Motion.NavTab)) },
                    popExitTransition = { fadeOut(tween(Motion.NavTab)) },
                ) {
                    appGraph(navController, container, playback, graphState, settingsViewModel)
                }
            }
        }

        // Griff der Tab-Leiste in der Tiefe: über der Oberkante der Startleiste bzw. der unteren Leisten,
        // über dem Inhalt; Overlays darüber verdecken ihn.
        if (!currentDestination.isTabRoot()) TabBarToggle(tabBar, Modifier.align(Alignment.TopCenter))

        PlaybackOverlays(
            playback = playback,
            onOpenLogbook = { navController.navigate(LogbookRoute) { launchSingleTop = true } },
        )
    }
}

/** Der [AppContainer] der laufenden App. */
@Composable
private fun appContainer(): AppContainer = (LocalContext.current.applicationContext as AtemkraftApplication).container
