package app.atemkraft.playback

import app.atemkraft.data.MeditationCues
import app.atemkraft.domain.MeditationMode
import kotlin.random.Random

/** Eine Ansage der Sprachanleitung: [text] bei [atMs] aktiver (pausenbereinigter) Zeit. */
internal data class SpeechCue(val atMs: Long, val text: String)

/**
 * Plan der Sprachanleitung einer Meditation als Bogen statt Zufallsfolge (Konzept 2026-11-25, D3):
 *
 * - **Ankommen:** der erste Satz 20–30 s nach dem Start, ab [SHORT_MS] Dauer (und bei ∞) der
 *   zweite kurz danach.
 * - **Mitte:** die Sätze in zufälliger Reihenfolge ohne Wiederholung; der Abstand wächst mit jedem
 *   Satz ([GAP_GROWTH_MS], gedeckelt bei [GAP_MAX_MS]) – lange Sitzungen werden still. Bei fester
 *   Dauer endet die Mitte, wenn alle Sätze gesagt sind; bei ∞ beginnt dann eine neue Runde (nie
 *   derselbe Satz zweimal hintereinander).
 * - **Ausklang:** bei fester Dauer ab [SHORT_MS] ein Satz [CLOSING_LEAD_MS] vor dem Ende (dem
 *   End-Gong), sofern er [MIN_SPACING_MS] Abstand zur vorigen Ansage hat; bei ∞ kein Ausklang (das
 *   Ende kennt nur die Person selbst). Die letzten [MIN_SPACING_MS] vor dem Ende bleiben still.
 *
 * Zeiten sind aktive Zeit: Pausen schieben den Plan, statt Ansagen zu verschlucken.
 */
internal fun planMeditationSpeech(mode: MeditationMode, totalMs: Long, random: Random): Sequence<SpeechCue> = sequence {
    val timed = mode == MeditationMode.TIMED
    val closing = timed && totalMs >= SHORT_MS
    val closingAt = totalMs - CLOSING_LEAD_MS
    // Ab hier keine Ansage mehr aus Ankommen oder Mitte: Platz für den Ausklang bzw. den End-Gong.
    val lastAt = when {
        !timed -> Long.MAX_VALUE
        closing -> closingAt - MIN_SPACING_MS
        else -> totalMs - MIN_SPACING_MS
    }
    var at = random.nextLong(FIRST_MIN_MS, FIRST_MAX_MS)
    var spoken = 0L

    val arrivals = if (!timed || totalMs >= SHORT_MS) MeditationCues.arrive else MeditationCues.arrive.take(1)
    for (text in arrivals) {
        if (at > lastAt) break
        yield(SpeechCue(at, text))
        spoken = at
        at += random.nextLong(ARRIVE_GAP_MIN_MS, ARRIVE_GAP_MAX_MS)
    }

    var step = 0
    var previous: String? = null
    middle@ while (true) {
        var round = MeditationCues.middle.shuffled(random)
        if (round.size > 1 && round.first() == previous) round = round.drop(1) + round.first()
        for (text in round) {
            at = spoken + minOf(random.nextLong(GAP_MIN_MS, GAP_MAX_BASE_MS) + step * GAP_GROWTH_MS, GAP_MAX_MS)
            if (at > lastAt) break@middle
            yield(SpeechCue(at, text))
            spoken = at
            previous = text
            step++
        }
        if (timed) break // feste Dauer: jeder Satz der Mitte höchstens einmal
    }

    if (closing && closingAt - spoken >= MIN_SPACING_MS) yield(SpeechCue(closingAt, MeditationCues.closing))
}

private const val FIRST_MIN_MS = 20_000L
private const val FIRST_MAX_MS = 30_000L
private const val ARRIVE_GAP_MIN_MS = 25_000L
private const val ARRIVE_GAP_MAX_MS = 35_000L

/** Unter 3 min Dauer nur ein Satz zum Ankommen und kein Ausklang, sonst reden Anfang und Ende ineinander. */
private const val SHORT_MS = 3 * 60_000L
private const val GAP_MIN_MS = 60_000L
private const val GAP_MAX_BASE_MS = 90_000L
private const val GAP_GROWTH_MS = 20_000L
private const val GAP_MAX_MS = 4 * 60_000L

/** Der Ausklang kommt so lange vor dem Ende, dass er vor dem End-Gong sicher ausgesprochen ist. */
internal const val CLOSING_LEAD_MS = 45_000L

/** Mindestabstand zwischen zwei Ansagen. */
internal const val MIN_SPACING_MS = 20_000L
