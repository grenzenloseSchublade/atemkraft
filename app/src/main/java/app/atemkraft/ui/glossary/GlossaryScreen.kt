package app.atemkraft.ui.glossary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.theme.Dimens

private data class Term(val name: String, val definition: String)

private val terms = listOf(
    Term(
        "HRV – Herzratenvariabilität",
        "Die natürliche Schwankung der Zeit zwischen zwei Herzschlägen. Eine höhere HRV gilt als " +
            "Zeichen für Erholung, Stressresistenz und einen aktiven Vagus.",
    ),
    Term(
        "Vagus / Vagustonus",
        "Der Vagusnerv ist der Hauptnerv des Ruhe-Systems (Parasympathikus). Hoher Vagustonus " +
            "bedeutet bessere Beruhigung, Erholung und HRV.",
    ),
    Term(
        "Parasympathikus / Sympathikus",
        "Die zwei Gegenspieler des vegetativen Nervensystems: der Parasympathikus beruhigt " +
            "(„Bremse“), der Sympathikus aktiviert („Gas“).",
    ),
    Term(
        "Baroreflex",
        "Ein Regelkreis, der den Blutdruck stabil hält. Langsames Atmen (~6/min) bringt ihn in " +
            "Resonanz und trainiert ihn.",
    ),
    Term(
        "RSA – Respiratorische Sinusarrhythmie",
        "Die Herzfrequenz steigt beim Einatmen und sinkt beim Ausatmen. Eine ausgeprägte RSA " +
            "spricht für einen guten Vagustonus.",
    ),
    Term(
        "CO₂-Toleranz",
        "Wie gut der Körper einen leichten Anstieg von Kohlendioxid verträgt. Höhere Toleranz " +
            "ermöglicht ruhigeres, effizienteres Atmen.",
    ),
    Term(
        "Hormese",
        "Kleine, dosierte Stressreize (z. B. Wim Hof), an die sich der Körper anpasst und dadurch " +
            "widerstandsfähiger wird.",
    ),
    Term(
        "PEM – Post-Exertional Malaise",
        "Verschlechterung des Zustands nach Anstrengung (z. B. bei ME/CFS oder Long COVID). Dann nur " +
            "sanfte, vagale Übungen.",
    ),
)

/** Glossar: erklärt Abkürzungen und Fachbegriffe aus den Übungsbeschreibungen. */
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            PushHeader(onBack = onBack, title = stringResource(R.string.glossary_title))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.SectionGap))
                // Karte ↔ Karte über spacedBy statt Außen-Padding je Karte: exakt ListGap, kein
                // zusätzlicher Rand über der ersten bzw. unter der letzten Karte.
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                    terms.forEach { term ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                                WholeWordText(
                                    text = term.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(Dimens.GapTiny))
                                Text(
                                    text = term.definition,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
        }
    }
}
