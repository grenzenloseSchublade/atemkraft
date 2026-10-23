package app.atemkraft

import android.app.Activity
import android.app.Application
import app.atemkraft.playback.BreathSessionController
import app.atemkraft.playback.MeditationController
import app.atemkraft.playback.PlaybackDependencies

class AtemkraftApplication :
    Application(),
    PlaybackDependencies {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override val meditationController: MeditationController get() = container.meditationController

    override val breathSessionController: BreathSessionController get() = container.breathSessionController

    override val launchActivity: Class<out Activity> get() = MainActivity::class.java
}
