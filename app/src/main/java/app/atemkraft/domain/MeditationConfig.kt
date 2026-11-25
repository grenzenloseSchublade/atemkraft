package app.atemkraft.domain

/** Zwei Betriebsarten des Meditations-Tabs. */
enum class MeditationMode {
    /** Feste Dauer, Countdown bis 0. */
    TIMED,

    /** Offene Sitzung, Stoppuhr zählt hoch, endet per „Beenden“. */
    FREE,
}

/** Kürzeste wählbare Dauer im [MeditationMode.TIMED] (Minuten). */
const val MEDITATION_MIN_MINUTES = 1

/** Längste wählbare Dauer im [MeditationMode.TIMED] (Minuten) – eine Runde des Zeitrings. */
const val MEDITATION_MAX_MINUTES = 60

/** Kürzester Abstand des Intervall-Gongs (Minuten); kleinere gespeicherte Werte gelten als ungesetzt. */
const val GONG_INTERVAL_MIN_MINUTES = 3

/** Abstand des Intervall-Gongs, solange nichts gewählt ist (Minuten). */
const val GONG_INTERVAL_DEFAULT_MINUTES = 5

/** Längster gespeicherter Abstand des Intervall-Gongs (Minuten) – eine Runde des Zeitrings. */
const val GONG_INTERVAL_MAX_MINUTES = 60

/**
 * Längste Zeit „Vorher atmen“ im Tab Meditation (Minuten): Resonanz-Atmung vor der Stille, ein kurzes
 * Ankommen; 0 = aus.
 */
const val BREATH_BEFORE_MAX_MINUTES = 10

/** Wählbare Abstände des Intervall-Gongs in den Einstellungen (Minuten). */
val GONG_INTERVAL_CHOICES = listOf(3, 5, 10, 15)

/**
 * Nutzer-Einstellung für eine Meditations-Sitzung (persistent via DataStore).
 *
 * Gong-Modus ergibt sich aus [startEndGong] + [gongEveryMin]:
 * - Aus: `startEndGong = false`, `gongEveryMin = null`
 * - Start & Ende: `startEndGong = true`, `gongEveryMin = null`
 * - Alle X min: `startEndGong = true`, `gongEveryMin = X` (plus Start-/End-Gong)
 *
 * @param minutes Dauer im [MeditationMode.TIMED], [MEDITATION_MIN_MINUTES]..[MEDITATION_MAX_MINUTES] (im FREE-Modus ignoriert).
 * @param startEndGong Gong zu Beginn und am Ende der Sitzung.
 * @param gongEveryMin Zusätzlicher Intervall-Gong alle N Minuten; null = kein Intervall.
 * @param speech Gesprochene Kurz-Anleitung (TTS) in unregelmäßigen Abständen an/aus.
 * @param breathBeforeMinutes „Vorher atmen“: so viele Minuten Resonanz-Atmung, dann ohne Bruch die
 *   Stille (0 = aus, bis [BREATH_BEFORE_MAX_MINUTES]; Eigentümer 2026-11-25).
 */
data class MeditationConfig(
    val mode: MeditationMode = MeditationMode.TIMED,
    val minutes: Int = 10,
    val startEndGong: Boolean = true,
    val gongEveryMin: Int? = null,
    val speech: Boolean = false,
    val breathBeforeMinutes: Int = 0,
)
