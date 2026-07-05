package app.atemkraft

import android.app.Application
import app.atemkraft.data.AppContainer

class AtemkraftApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
