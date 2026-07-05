package app.atemkraft.data

import android.content.Context
import app.atemkraft.data.local.AtemkraftDatabase

/**
 * Manuelles DI: hält die Repositories einmalig (keine Instanziierung in Composables).
 * Wird von [app.atemkraft.AtemkraftApplication] erzeugt und an ViewModels/Composables
 * weitergereicht.
 */
class AppContainer(context: Context) {

    private val database = AtemkraftDatabase.build(context)

    val exerciseRepository: ExerciseRepository = ExerciseRepository()
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val logbookRepository: LogbookRepository = LogbookRepository(database.logbookDao())
}
