package app.atemkraft.domain

/** Zwei Betriebsarten des Meditations-Tabs. */
enum class MeditationMode {
    /** Feste Dauer, Countdown bis 0. */
    TIMED,

    /** Offene Sitzung, Stoppuhr zählt hoch, endet per „Beenden". */
    FREE,
}

/**
 * Nutzer-Einstellung für eine Meditations-Sitzung (persistent via DataStore).
 *
 * Gong-Modus ergibt sich aus [startEndGong] + [gongEveryMin]:
 * - Aus: `startEndGong = false`, `gongEveryMin = null`
 * - Start & Ende: `startEndGong = true`, `gongEveryMin = null`
 * - Alle X min: `startEndGong = true`, `gongEveryMin = X` (plus Start-/End-Gong)
 *
 * @param minutes Dauer im [MeditationMode.TIMED] (im FREE-Modus ignoriert).
 * @param startEndGong Gong zu Beginn und am Ende der Sitzung.
 * @param gongEveryMin Zusätzlicher Intervall-Gong alle N Minuten; null = kein Intervall.
 * @param speech Gesprochene Kurz-Anleitung (TTS) in unregelmäßigen Abständen an/aus.
 */
data class MeditationConfig(
    val mode: MeditationMode = MeditationMode.TIMED,
    val minutes: Int = 10,
    val startEndGong: Boolean = true,
    val gongEveryMin: Int? = null,
    val speech: Boolean = false,
)
