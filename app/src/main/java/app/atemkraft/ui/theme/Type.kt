package app.atemkraft.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak

// Typo-Skala = Material-3-Defaults (Größe, Zeilenhöhe, Laufweite unverändert, kein eigener
// Skalierfaktor – Nutzerentscheidung 2026-10-19). Überschrieben werden nur Gewicht und Umbruch
// der Überschrift-Rollen, damit die Hierarchie klar liest (Screen-Titel > Abschnitts-Header >
// Kartentitel). Die Systemschriftgröße des Geräts wirkt wie überall in Android darauf.
private val base = Typography()

// Überschriften und Titel: Umbruch wie bei Überschriften (gleichmäßig lange Zeilen) und
// ausdrücklich ohne Silbentrennung – Wörter bleiben ganz (LAYOUT-03, TYPO-03). Dass kein Wort
// breiter als seine Zeile wird, sichern Layout (FlowRow, AdaptiveButtonRow) und ScreenshotTest.
private fun TextStyle.asHeading() = copy(
    lineBreak = LineBreak.Heading,
    hyphens = Hyphens.None,
)

val AtemkraftTypography = base.copy(
    // Derzeit ungenutzt; kräftig und ohne Trennung wie alle Überschriften, falls es später kommt.
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold).asHeading(),
    // Screen-Titel der Tabs (ScreenHeader) und Markenkopf „Über“: kräftig.
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold).asHeading(),
    // Titel der Push-Screens, Phasenname im Kreis, Abschluss-Titel der Sitzung.
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold).asHeading(),
    // Abschnitts-Header (SectionHeader: Familien-/Sektionsüberschriften).
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold).asHeading(),
    // Karten- und Expander-Titel, Stepper-Wert: etwas kräftiger.
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold).asHeading(),
    // Kleine Titel (Stepper-Label, Overlay-Titel, Mini-Leiste, Kennzahlen, Zeitring-Ziffern).
    titleSmall = base.titleSmall.asHeading(),
)
