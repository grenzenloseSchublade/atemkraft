package app.atemkraft.data

import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import java.time.DayOfWeek

/**
 * Nutzer-Einstellungen für die Begleit-Reize (Cues), persistent via DataStore. Lautstärke ab Werk
 * „Mittel“ (Nutzer 2026-10-24; vorher „Laut“ – riskant beim ersten Start im Büro oder im Bett).
 */
data class CueSettings(
    val soundMode: SoundMode = SoundMode.CUES,
    val haptics: Boolean = true,
    val transition: TransitionEmphasis = TransitionEmphasis.MEDIUM,
    val volume: ToneVolume = ToneVolume.MEDIUM,
    /** Gong-Ausklang lang/kurz (app-weit, auch für den Sitzungs-Abschlussgong). */
    val gongLong: Boolean = true,
)

/**
 * Sicherheits-Einstellungen. [acknowledgedIds] = Schlüssel ([keyOf]) der Übungen, deren Hinweis
 * schon einmal bestätigt wurde – je Übung, nicht einmal für alle (Nutzer 2026-10-24): Wim Hof hat
 * andere Risiken als die Feueratmung (Atempause, nie im Wasser), eine Bestätigung bei der einen gilt
 * nicht für die andere.
 */
data class SafetySettings(
    val showWarning: Boolean = true,
    val acknowledgedIds: Set<String> = emptySet(),
) {
    /**
     * Muss vor dem Start von [exercise] der Sicherheitsdialog kommen (MUSTER-06)? Nur bei
     * intensiven Übungen (`caution`, auch intensive eigene Muster), und dann, solange der Hinweis
     * für genau diese Übung mit genau diesem Inhalt ([keyOf]) noch nie bestätigt wurde oder in den
     * Einstellungen eingeschaltet ist. Die eine Regel für Detailseite und Muster-Editor.
     */
    fun requiresConfirm(exercise: Exercise): Boolean = exercise.caution && (keyOf(exercise) !in acknowledgedIds || showWarning)

    companion object {
        /**
         * Wofür eine Bestätigung gilt (Eigentümer 2026-11-07): bei eingebauten Übungen ihre Id
         * („wim-hof“, Inhalt fest), bei generierten Mustern – gespeichert, eigenes, Entwurf – Id
         * **und** Folge („muster-7:INHALE 4000,HOLD 12000,EXHALE 6000“). Ein umgebautes Muster oder
         * ein anderer Entwurf fragt so wieder; die Dauer zählt nicht, sie ändert das Risiko nicht.
         * Bestätigungen von vor v2.12 (nur „muster-7“, „muster-entwurf“) passen damit zu keinem
         * Muster mehr und verfallen – der sichere Weg: einmal mehr gefragt.
         */
        fun keyOf(exercise: Exercise): String {
            if (!exercise.generated) return exercise.id
            val phases = exercise.segments.flatMap { it.phases }.joinToString(",") { phase ->
                val duration = phase.duration
                "${phase.type.name} ${if (duration is PhaseDuration.Fixed) duration.millis else duration}"
            }
            return "${exercise.id}$KEY_SEPARATOR$phases"
        }

        /** Trennt Id und Folge im Schlüssel; eine Id enthält ihn nie. */
        const val KEY_SEPARATOR = ":"
    }
}

/**
 * Was die laufende Atem-Session zeigt (Karte „Während der Atemübung“). [autoCalm] blendet die
 * Bedienelemente nach wenigen Sekunden von selbst aus (Ruhe-Ansicht, SessionOverlay).
 * [silenceAfter]: Nach dem Ende geht es nahtlos in die stille Meditation, mit Gong und Stimme wie
 * zuletzt im Tab Meditation (Nutzer 2026-10-23, Yoga-Sutra 2,52–2,53: Pranayama bereitet die
 * Sammlung vor); Standard aus. Nur der Startwert: Je Übung wählt das Blatt „Anpassen“ die Dauer
 * der Stille oder „Aus“ ([IntervalOverrides.silence], Eigentümer 2026-11-24).
 */
data class SessionDisplay(
    val showNextPhase: Boolean = true,
    val showProgress: Boolean = true,
    val showSeconds: Boolean = true,
    val autoCalm: Boolean = true,
    val silenceAfter: Boolean = false,
)

/**
 * Was die laufende Meditation zeigt (Meditations-Karte der Einstellungen). [autoCalm] wie bei
 * [SessionDisplay]; [showTime] = Restzeit bzw. verstrichene Zeit im Kreis.
 */
data class MeditationDisplay(
    val autoCalm: Boolean = true,
    val showProgress: Boolean = true,
    val showTime: Boolean = true,
)

/**
 * Lokale Erinnerung (Karte „Erinnerung“): aus ab Werk; an zur Uhrzeit [minuteOfDay] (Minuten nach
 * Mitternacht, Raster [STEP_MINUTES]) an den Tagen [days]. Ohne Netz, ohne Konto; geplant über den
 * JobScheduler des Systems (`playback/ReminderScheduler`). Mindestens ein Tag bleibt gewählt.
 */
data class ReminderSettings(
    val enabled: Boolean = false,
    val minuteOfDay: Int = DEFAULT_MINUTE,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
) {
    companion object {
        /** 8:00 Uhr: früh genug für den Tag, ohne zu wecken. */
        const val DEFAULT_MINUTE = 8 * 60

        /** Raster der Uhrzeit (Stepper in Viertelstunden). */
        const val STEP_MINUTES = 15

        const val MINUTES_PER_DAY = 24 * 60
    }
}

/** Vom Nutzer angepasste Session-Werte einer Übung: Dauer (Minuten bzw. Runden),
 *  Phasenlängen in Sekunden und „Danach Stille“ in Minuten ([silence], 0 = aus); null = nicht
 *  angepasst bzw. Phase nicht vorhanden. */
data class IntervalOverrides(
    val duration: Int? = null,
    val inhale: Int? = null,
    val hold: Int? = null,
    val exhale: Int? = null,
    val silence: Int? = null,
) {
    /** Dieselben Werte für die Sicherung. */
    fun toBackup(): PatternBackupOverrides = PatternBackupOverrides(duration, inhale, hold, exhale, silence)
}
