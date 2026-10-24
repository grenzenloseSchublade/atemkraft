package app.atemkraft

import android.app.Activity
import android.app.Application
import app.atemkraft.data.ReminderSettings
import app.atemkraft.playback.BreathSessionController
import app.atemkraft.playback.MeditationController
import app.atemkraft.playback.PlaybackDependencies
import app.atemkraft.playback.ReminderDependencies
import kotlinx.coroutines.flow.Flow

class AtemkraftApplication :
    Application(),
    PlaybackDependencies,
    ReminderDependencies {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override val meditationController: MeditationController get() = container.meditationController

    override val breathSessionController: BreathSessionController get() = container.breathSessionController

    override val launchActivity: Class<out Activity> get() = MainActivity::class.java

    override val reminderSettings: Flow<ReminderSettings> get() = container.settingsRepository.reminderSettings
}
