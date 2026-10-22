package app.atemkraft.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import app.atemkraft.domain.SANSKRIT_SPAN

/**
 * Text mit Sanskrit-Auszeichnung („*prāṇa*“, [SANSKRIT_SPAN]) als [AnnotatedString]: der Begriff
 * kursiv, die Sternchen fallen weg (TEXT-14). Der Klartext ist der gelesene Text – TalkBack und
 * die Wortbruch-Prüfung ([breaksInsideWord]) sehen keine Markierungszeichen. Ohne Auszeichnung
 * kommt der Text unverändert zurück.
 *
 * Zentral für alle Textbausteine, die Fachinhalte zeigen: [WholeWordText] (und damit [HintText],
 * [CardTitle], [LinkChip], Abschnittsköpfe), [BodyText], [Bullet], die Teaser von
 * [ExpanderSection] und [SectionHeader] sowie [ForwardLinkText].
 */
fun sanskritText(text: String): AnnotatedString {
    if ('*' !in text) return AnnotatedString(text)
    return buildAnnotatedString {
        var last = 0
        SANSKRIT_SPAN.findAll(text).forEach { match ->
            append(text, last, match.range.first)
            withStyle(SanskritStyle) { append(match.groupValues[1]) }
            last = match.range.last + 1
        }
        append(text, last, text.length)
    }
}

/** Kursiv, sonst wie der umgebende Text: Größe, Gewicht und Farbe bleiben bei der Rolle. */
private val SanskritStyle = SpanStyle(fontStyle = FontStyle.Italic)
