package app.atemkraft.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

// Eigene Typo-Skala auf Basis der Material-3-Defaults: nur die Überschrift-Rollen werden
// kräftiger/größer, damit die Hierarchie klar liest (Screen-Titel > Abschnitts-Header >
// Kartentitel). Body/Label bleiben Standard – Lesbarkeit im Dunkeln und kein Layout-Bruch.
private val base = Typography()

// Überschriften und Titel: Umbruch wie bei Überschriften (gleichmäßig lange Zeilen) und
// ausdrücklich ohne Silbentrennung – Wörter bleiben ganz (LAYOUT-03, TYPO-03). Dass kein Wort
// breiter als seine Zeile wird, sichern Layout (FlowRow, AdaptiveButtonRow) und ScreenshotTest.
private fun TextStyle.asHeading() = copy(
    lineBreak = LineBreak.Heading,
    hyphens = Hyphens.None,
)

val AtemkraftTypography = base.copy(
    // Screen-Titel der Tabs (Atmen/Situationen/Logbuch): groß & kräftig.
    headlineLarge = base.headlineLarge.copy(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
    ).asHeading(),
    // Sub-Screen-Titel (Detail, Glossar, Über): fetter, gleiche Größe.
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold).asHeading(),
    // Neue Abschnitts-Header-Ebene (Familien-/Sektionsüberschriften): 24sp statt bisher 16sp.
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold).asHeading(),
    // Kartentitel (Übungs-/Log-Namen): 22sp, etwas kräftiger.
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold).asHeading(),
    // Kleinere Titel (Stepper-Labels, Streak): gleiche Größe, kräftiger.
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold).asHeading(),
    // Derzeit ungenutzt; trennt trotzdem wie alle Titel, falls es später verwendet wird.
    titleSmall = base.titleSmall.asHeading(),
)
