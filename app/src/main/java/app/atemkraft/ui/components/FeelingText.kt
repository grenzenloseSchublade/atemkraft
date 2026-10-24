package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.Feeling

/**
 * Das Wort eines Befindens („ruhiger“, „wie vorher“, „unruhiger“, „wacher“, „müder“) – am
 * Ende-Screen als Beschriftung der Antwortreihe, im Logbuch als stilles Wort am Eintrag. Nur ein
 * Wort, nie eine Farbe, ein Icon oder eine Zahl (SEC-LEGAL-04).
 */
@Composable
fun feelingText(feeling: Feeling): String = stringResource(
    when (feeling) {
        Feeling.CALMER -> R.string.feeling_calmer
        Feeling.SAME -> R.string.feeling_same
        Feeling.RESTLESS -> R.string.feeling_restless
        Feeling.MORE_AWAKE -> R.string.feeling_more_awake
        Feeling.MORE_TIRED -> R.string.feeling_more_tired
    },
)

/**
 * Frage nach dem Befinden am Ende-Screen ([FinishedPanel], Anatomie „Antwortreihe“ in STYLEGUIDE
 * §4.5); fehlt sie, fragt der Ende-Screen nicht (Einstellung aus). [onAnswer] setzt die Antwort.
 * [intro]: Die Reihe erscheint zum ersten Mal – darunter steht einmal, dass sie nur auf dem Gerät
 * bleibt und abschaltbar ist; [onIntroShown] meldet das Erscheinen (danach gilt der Schalter).
 */
@Immutable
data class FeelingAsk(
    val onAnswer: (Feeling) -> Unit,
    val intro: Boolean = false,
    val onIntroShown: () -> Unit = {},
)
