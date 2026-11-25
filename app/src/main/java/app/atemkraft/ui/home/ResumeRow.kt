package app.atemkraft.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.Exercise
import app.atemkraft.ui.components.ExerciseCurve
import app.atemkraft.ui.components.LARGE_FONT_SCALE
import app.atemkraft.ui.components.SectionHeader

/**
 * Was die Zeile „Weiter“ zeigt: die zuletzt geübte Übung ([exercise], dieselbe Regel wie der
 * Shortcut „Zuletzt geübt“) und die Dauer, mit der ihre Detailseite startet ([duration] Minuten bzw.
 * Runden bei [roundBased]) – die gespeicherte Anpassung, sonst die Voreinstellung.
 */
@Immutable
data class ResumeEntry(val exercise: Exercise, val duration: Int, val roundBased: Boolean)

/**
 * „Weiter: Resonanz-Atmung · 10 min ›“ oben im Atmen-Tab (Konzeptprüfung 2026-11-25, Phase 1): Auf
 * dem Platz der Willkommenskarte, sobald das Logbuch eine Übung kennt, die sich noch öffnen lässt.
 * Wer wiederkommt, ist so mit zwei Tipps beim Atmen (Zeile, Start), ohne den Tab zu wechseln.
 *
 * Dieselbe Kopf-Art wie „Muster des Tages ›“ ([SectionHeader] mit `onClick`, Chevron, kleiner Status);
 * davor die Kurve der Übung ([ExerciseCurve]) als Zeichen eines Einstiegs, damit die Zeile nicht wie
 * eine Überschrift wirkt – ab Schrift 1,5 ohne sie wie in [app.atemkraft.ui.components.ExerciseRow].
 * Kein Befinden, kein Datum: Die Zeile ist ein Weg zurück, kein Rückblick (der steht in „Meins“).
 * TalkBack: eine Schaltfläche „Weiter: Resonanz-Atmung, 10 Minuten“ mit der Aktion „Übung öffnen“.
 */
@Composable
internal fun ResumeRow(entry: ResumeEntry, onClick: () -> Unit) {
    val d = entry.duration
    val rounds = pluralStringResource(R.plurals.detail_meta_rounds, d, d)
    // Gezeigt kurz („10 min“), gesprochen ausgeschrieben („10 Minuten“, A11Y-06).
    val shown = if (entry.roundBased) rounds else stringResource(R.string.adjust_status_minutes, d)
    val spoken = if (entry.roundBased) rounds else pluralStringResource(R.plurals.cd_duration_minutes, d, d)
    val withCurve = LocalDensity.current.fontScale < LARGE_FONT_SCALE
    SectionHeader(
        title = stringResource(R.string.home_resume_title, entry.exercise.name),
        status = shown,
        statusDescription = spoken,
        smallStatus = true,
        onClick = onClick,
        onClickLabel = stringResource(R.string.cd_resume_open),
        leading = if (withCurve) {
            { ExerciseCurve(entry.exercise) }
        } else {
            null
        },
        // Erstes Element der Liste: Darüber steht nur der Rand des Tabs, kein zweiter Abstand.
        top = 0.dp,
    )
}
