package app.atemkraft.data

/**
 * Kurze, ruhige, einladende Hinweise für die gesprochene Anleitung (TTS) während einer
 * stillen Meditation. Bewusst zurückhaltend und rein aufmerksamkeitslenkend – **keine
 * Wirkversprechen** (konsistent mit der evidenz-ehrlichen Content-Linie der App). Jede
 * Zeile ist ein abgeschlossener, kurzer Satz, der auch allein verständlich ist (die Mitte kommt
 * in zufälliger Reihenfolge).
 *
 * Drei Teile wie ein Bogen ([app.atemkraft.playback.planMeditationSpeech]): [arrive] am Anfang in
 * dieser Reihenfolge, [middle] spärlich und ohne Wiederholung, [closing] kurz vor dem Ende einer
 * Sitzung mit fester Dauer. Gesprochen wird zur Laufzeit (System-TTS oder Piper), neue Sätze
 * brauchen keine Audiodateien.
 */
object MeditationCues {
    val arrive: List<String> = listOf(
        "Lass deinen Körper im Sitz zur Ruhe kommen.",
        "Spüre, wo dein Körper den Boden berührt.",
    )

    val middle: List<String> = listOf(
        "Nimm den Atem wahr, so wie er gerade ist.",
        "Spüre, wie die Luft ein- und wieder ausströmt.",
        "Es gibt gerade nichts zu tun.",
        "Wenn die Gedanken abschweifen, ist das in Ordnung.",
        "Komm sanft zurück zum Atem.",
        "Lass die Schultern weich werden.",
        "Gedanken dürfen kommen und gehen.",
        "Du musst den Gedanken nicht folgen.",
        "Kehre ruhig zu diesem Atemzug zurück.",
        "Was auch immer da ist, lass es da sein.",
    )

    val closing: String = "Die Stille geht gleich zu Ende. Bleib noch einen Moment hier."
}
