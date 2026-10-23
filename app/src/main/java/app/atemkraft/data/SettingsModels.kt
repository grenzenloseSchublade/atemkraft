package app.atemkraft.data

import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis

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

/** Sicherheits-Einstellungen. [acknowledged] = wurde der Hinweis schon einmal bestätigt. */
data class SafetySettings(
    val showWarning: Boolean = true,
    val acknowledged: Boolean = false,
) {
    /**
     * Muss vor dem Start von [exercise] der Sicherheitsdialog kommen (MUSTER-06)? Nur bei
     * intensiven Übungen (`caution`, auch intensive eigene Muster), und dann, solange der Hinweis
     * noch nie bestätigt wurde oder in den Einstellungen eingeschaltet ist. Die eine Regel für
     * Detailseite und Muster-Editor.
     */
    fun requiresConfirm(exercise: Exercise): Boolean = exercise.caution && (!acknowledged || showWarning)
}

/**
 * Was die laufende Atem-Session zeigt (Karte „Während der Atemübung“). [autoCalm] blendet die
 * Bedienelemente nach wenigen Sekunden von selbst aus (Ruhe-Ansicht, SessionOverlay).
 * [silenceAfter]: Nach dem Ende geht es nahtlos in die stille Meditation, eingestellt wie zuletzt im
 * Tab Meditation (Nutzer 2026-10-23, Yoga-Sutra 2,52–2,53: Pranayama bereitet die Sammlung vor);
 * Standard aus.
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

/** Vom Nutzer angepasste Session-Werte einer Übung: Dauer (Minuten bzw. Runden) und
 *  Phasenlängen in Sekunden; null = nicht angepasst bzw. Phase nicht vorhanden. */
data class IntervalOverrides(
    val duration: Int? = null,
    val inhale: Int? = null,
    val hold: Int? = null,
    val exhale: Int? = null,
)
