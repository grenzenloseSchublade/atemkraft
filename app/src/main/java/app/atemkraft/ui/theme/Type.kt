package app.atemkraft.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Eigene Typo-Skala auf Basis der Material-3-Defaults: nur die Überschrift-Rollen werden
// kräftiger/größer, damit die Hierarchie klar liest (Screen-Titel > Abschnitts-Header >
// Kartentitel). Body/Label bleiben Standard – Lesbarkeit im Dunkeln und kein Layout-Bruch.
private val base = Typography()

val AtemkraftTypography = base.copy(
    // Screen-Titel der Tabs (Atmen/Situationen/Logbuch): groß & kräftig.
    headlineLarge = base.headlineLarge.copy(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
    ),
    // Sub-Screen-Titel (Detail, Glossar, Über): fetter, gleiche Größe.
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    // Neue Abschnitts-Header-Ebene (Familien-/Sektionsüberschriften): 24sp statt bisher 16sp.
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    // Kartentitel (Übungs-/Log-Namen): 22sp, etwas kräftiger.
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    // Kleinere Titel (Stepper-Labels, Streak): gleiche Größe, kräftiger.
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
