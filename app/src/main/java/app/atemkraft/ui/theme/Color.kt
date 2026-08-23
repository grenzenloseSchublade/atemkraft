package app.atemkraft.ui.theme

import androidx.compose.ui.graphics.Color

// 80er-Synthwave/Outrun-Palette, dunkel-zuerst: tiefes Indigo + Neon-Magenta/Cyan/Gelb.
// Akzente bewusst dosiert (Atemübungen oft abends/mit geschlossenen Augen).
val NeonMagenta = Color(0xFFF25CA2)
val NeonCyan = Color(0xFF34E0E8)
val NeonYellow = Color(0xFFF9C80E)
val OnNeon = Color(0xFF1E0A16)
val OnCyan = Color(0xFF042A2C)

val DarkBackground = Color(0xFF160F2E)
val DarkSurface = Color(0xFF211640)
val DarkSurfaceVariant = Color(0xFF2E2150)
val DarkOnBackground = Color(0xFFF0E6FF)
val DarkOnSurface = Color(0xFFF0E6FF)

// M3-Container-Leiter + Sekundär-Rollen, alle aus dem Indigo abgeleitet (Theme.kt).
val SurfaceContainerLowest = Color(0xFF120C26)
val SurfaceContainerLow = Color(0xFF1B1236)
val SurfaceContainerHigh = Color(0xFF281B48)
val SecondaryContainer = Color(0xFF3A2C60)
val OutlineVariantIndigo = Color(0xFF453763)

/** Gedämpfter Sekundärtext (M3 onSurfaceVariant) – Lavendel statt Voll-Weiß. */
val DarkOnSurfaceVariant = Color(0xFFC3B8DD)

// Familien-Akzentfarben (wie in der Vorlage F3_Atmung): vagal/blau, sympathisch/amber,
// balance/violett, funktionell/grün.
val FamilyVagal = Color(0xFF60A5FA)
val FamilySympathetic = Color(0xFFF59E0B)
val FamilyBalance = Color(0xFFA78BFA)
val FamilyFunctional = Color(0xFF34D399)

// Semantische Tokens (zentral, statt mehrfach hardcodierter Hex in den Screens).
val EvidenceBest = NeonCyan             // „am besten belegt" – Cyan
val EvidenceCaution = Color(0xFFFF6B8B) // „nur stabile Phase" / Vorsicht – Neon-Pink/Rot
val WarnAmber = NeonYellow              // Hinweis/Warnung – Synthwave-Gelb

// Session-Atemkreis (synthwave): Magenta-Verlauf + Cyan-Ring, damit die helle Schrift
// sowohl auf dem Kreis als auch auf dunklem Grund klar lesbar ist.
// Satteres Magenta → Violett: lebendiger Verlauf. Schrift ist creme & groß → genug Kontrastreserve.
val SynthCircleCenter = Color(0xFFCB3C9A)
val SynthCircleEdge = Color(0xFF4A2080)
val SynthTrack = NeonCyan
val SynthText = Color(0xFFFFEAF7)
val SynthGlow = Color(0xFF2A0A2E)

// Session-Schrift: warmes, entsättigtes Pastellgold/Creme mit weichem Schimmer – ruhig statt grell,
// passt zur kühlen Cyan/Lila-Palette, ohne den Bildschirm zu „brechen". Zusatzinfo dezenter.
val SessionTextYellow = Color(0xFFEBDCB0)
// Aufgehellt (war #D7C7A0 ≈ 2,7:1 auf dem Magenta-Kreiszentrum – AA-Fail auch für großen Text).
val SessionNoteAmber = Color(0xFFF0E4C0)
val SessionTextGlow = Color(0xFF160B22)

// Session-Buttons: entsättigtes, „cremiges" Cyan (kein Vollneon) – präsent, aber dezent,
// damit der Atemkreis der Blickfang bleibt. Pink für „Beenden" im selben gedeckten Ton.
val SessionButtonCyan = Color(0xFF6FB6BA)
val SessionButtonPink = Color(0xFFFF8FB0)

/** Einheitliche Deckkraft für Sekundärtext (dezente Unter-/Beschriftungen) – app-weit. */
const val SECONDARY = 0.7f

/** Noch dezentere Ebene (z. B. Quellen-Kennungen, Zeitstempel) – app-weit. */
const val TERTIARY = 0.5f

/** Creme-Ton des Pause-/Play-Symbols (PauseFlash) – zentral statt Inline-Hex. */
val PauseGlyphCreme = Color(0xFFFFF3D6)
