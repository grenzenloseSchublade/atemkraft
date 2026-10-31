package app.atemkraft

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.domain.SessionLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * App-Shortcuts (Long-Press auf das Icon, Auswertung 2026-10-24): statisch „Resonanz-Atmung“ und
 * „Meditation“ (`res/xml/shortcuts.xml`), dynamisch „Zuletzt geübt“. Jeder Shortcut startet
 * `MainActivity` explizit mit einer festen Aktion und **ohne Daten oder Extras**: Die App vergleicht
 * nur die Aktion mit diesen Konstanten ([fromAction]); was „zuletzt“ war, liest sie beim Öffnen
 * selbst aus dem Logbuch (SEC-PLAT-02). Kein neuer exportierter Einstieg.
 */
enum class AppShortcut(val action: String) {
    RESONANCE("app.atemkraft.action.OPEN_RESONANCE"),
    MEDITATION("app.atemkraft.action.OPEN_MEDITATION"),
    LAST("app.atemkraft.action.OPEN_LAST"),
    ;

    companion object {
        /** Übung des statischen Shortcuts „Resonanz-Atmung“. */
        const val RESONANCE_EXERCISE_ID = "resonanz"

        /** Allowlist: nur eine der drei Aktionen, alles andere (auch MAIN) ist kein Shortcut. */
        fun fromAction(action: String?): AppShortcut? = entries.firstOrNull { it.action == action }
    }
}

/**
 * Die zuletzt geübte Übung, die sich noch öffnen lässt: der neueste Logbuch-Eintrag ([entries],
 * neueste zuerst) einer eingebauten Übung oder eines noch gespeicherten Musters ([saved]) – nicht
 * Meditation, Tagesmuster (heute ein anderes) oder Entwurf. Für „Zuletzt: … ›“ im Tab „Eigene“ und
 * den Shortcut „Zuletzt geübt“.
 */
internal fun lastPracticed(entries: List<SessionLogEntry>, saved: List<SavedPattern>): Exercise? = entries.firstNotNullOfOrNull { openable(it, saved) }

/**
 * Die zuletzt geübten Übungen nach derselben Regel wie [lastPracticed], jede einmal, neueste zuerst,
 * höchstens [limit] – für „Zuletzt geübt“ im Tab „Eigene“ (Nutzer 2026-10-31).
 */
internal fun recentlyPracticed(entries: List<SessionLogEntry>, saved: List<SavedPattern>, limit: Int): List<Exercise> = entries.asSequence().mapNotNull { openable(it, saved) }.distinctBy { it.id }.take(limit).toList()

private fun openable(entry: SessionLogEntry, saved: List<SavedPattern>): Exercise? {
    val id = entry.exerciseId
    val savedId = ExerciseIds.savedIdOf(id)
    return when {
        id == ExerciseIds.DAILY || id == ExerciseIds.DRAFT -> null
        savedId != null -> saved.firstOrNull { it.id == savedId }?.exercise
        else -> BuiltInExercises.all.firstOrNull { it.id == id }
    }
}

/**
 * Hält den dynamischen Shortcut „Zuletzt geübt“ aktuell: Name der zuletzt geübten Übung ([last]);
 * fehlt sie oder ist es die Resonanz-Atmung (die hat ihren statischen Shortcut), entfällt er.
 * Geschrieben nur bei einer Änderung.
 */
internal fun followLastPracticedShortcut(context: Context, last: Flow<Exercise?>, scope: CoroutineScope) {
    scope.launch {
        last.map { exercise -> exercise?.takeIf { it.id != AppShortcut.RESONANCE_EXERCISE_ID }?.name }
            .distinctUntilChanged()
            .collect { name ->
                // Ein Launcher ohne Shortcut-Unterstützung oder ein Systemfehler darf die App nicht stören.
                runCatching {
                    if (name == null) {
                        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(LAST_SHORTCUT_ID))
                    } else {
                        ShortcutManagerCompat.pushDynamicShortcut(context, lastShortcut(context, name))
                    }
                }
            }
    }
}

private fun lastShortcut(context: Context, name: String): ShortcutInfoCompat {
    // Explizit, ohne Daten; wie die statischen Shortcuts in einer frischen Aufgabe.
    val intent = Intent(AppShortcut.LAST.action)
        .setClass(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return ShortcutInfoCompat.Builder(context, LAST_SHORTCUT_ID)
        .setShortLabel(name)
        .setLongLabel(context.getString(R.string.shortcut_last_long, name))
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_shortcut_last))
        .setIntent(intent)
        .build()
}

private const val LAST_SHORTCUT_ID = "last"
