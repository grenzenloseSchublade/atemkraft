package app.atemkraft.ui.theme

import androidx.compose.ui.graphics.Color

// 80er-Synthwave/Outrun-Palette, dunkel-zuerst: tiefes Indigo + Neon-Magenta/Cyan/Gelb.
// Akzente bewusst dosiert (Atemübungen oft abends/mit geschlossenen Augen).
val NeonMagenta = Color(0xFFF25CA2)
val NeonCyan = Color(0xFF34E0E8)

/**
 * Helles Synthwave-Gelb: ΔE76 ≥ 25 zu `FamilySympathetic` (Amber), damit Warnung und Familie B
 * nebeneinander unterscheidbar bleiben (FARBE-04, ThemeContrastTest).
 */
val NeonYellow = Color(0xFFFAD84A)

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

/**
 * Fläche des gewählten Segments (`SegmentedChoiceRow`): dasselbe Indigo, so hell, dass die
 * Auswahl auch ohne Häkchen ≥ 3:1 gegen das ungewählte Segment (surface) steht und die helle
 * Beschriftung ≥ 4,5:1 behält (FARBE-03, ThemeContrastTest).
 */
val SegmentSelected = Color(0xFF715AAF)
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
// Studienlage (EvidenceLevel): drei ruhige, klar unterscheidbare Töne, Chip-Text ≥ 4,5:1 und
// ΔE76 ≥ 25 zu allen Farben des Atmen-Tabs (ThemeContrastTest).
val EvidenceBest = NeonCyan // „gut belegt" – Cyan
val EvidenceStudied = Color(0xFF78B0CC) // „in Studien geprüft" – gedämpftes Stahlblau
val EvidenceLittle = Color(0xFFC8B8B4) // „kaum untersucht" – neutrales, warmes Grau
val EvidenceCaution = Color(0xFFFF6B8B) // „nur gesund & ausgeruht" / Vorsicht – Neon-Pink/Rot
val WarnAmber = NeonYellow // Hinweis/Warnung – Synthwave-Gelb

// Session-Atemkreis (synthwave): Magenta-Verlauf + Cyan-Ring, damit die helle Schrift
// sowohl auf dem Kreis als auch auf dunklem Grund klar lesbar ist.
// Satteres Magenta → Violett: lebendiger Verlauf. Schrift ist creme & groß → genug Kontrastreserve.
val SynthCircleCenter = Color(0xFFCB3C9A)
val SynthCircleEdge = Color(0xFF4A2080)
val SynthTrack = NeonCyan

// Session-Schrift: warmes, entsättigtes Pastellgold/Creme mit weichem Schimmer – ruhig statt grell,
// passt zur kühlen Cyan/Lila-Palette, ohne den Bildschirm zu „brechen". Zusatzinfo dezenter.
val SessionTextYellow = Color(0xFFEBDCB0)

// Anleitungszeile unter dem Kreis, mit α SECONDARY auf background (früher im Kreis, daher so hell).
val SessionNoteAmber = Color(0xFFF0E4C0)
val SessionTextGlow = Color(0xFF160B22)

// Session-Buttons: entsättigtes, „cremiges" Cyan (kein Vollneon) – präsent, aber dezent,
// damit der Atemkreis der Blickfang bleibt. Pink für „Beenden" im selben gedeckten Ton.
val SessionButtonCyan = Color(0xFF6FB6BA)
val SessionButtonPink = Color(0xFFFF8FB0)

// Textstufen (§2.2): Primär 1,0 · BODY · SECONDARY · TERTIARY – keine weiteren Deckkräfte für Text.

/** Fließtext: Beschreibungen, Detail-, Glossar- und Über-Text, aufgeklappter Teaser – app-weit. */
const val BODY = 0.8f

/** Einheitliche Deckkraft für Sekundärtext (dezente Unter-/Beschriftungen) – app-weit. */
const val SECONDARY = 0.7f

/**
 * Noch dezentere Ebene (Quellen-Kennungen, Zeitstempel, Wochentage, Session-Hilfszeilen,
 * unselektierte Tabs) – app-weit; 0,55 hält auf der Card noch 4,5:1 (FARBE-03).
 */
const val TERTIARY = 0.55f

/** Fläche gefüllter Info-Chips (`Chip`: Studienlage, „intensiv“) über dem Grund. */
const val CHIP_FILL = 0.16f

/** Deckkraft des Akzent-Rands navigierender Inhaltskarten (KOMP-03, `accentBorder`). */
const val ALPHA_BORDER = 0.35f

/**
 * Einziger Trenner in Karten und Expandern (KOMP-03): leise Linie aus dem Primärtext mit α 0,08 –
 * auf Card und Screen-Grund gleich zurückhaltend, dekorativ (FARBE-03 ausgenommen).
 */
val DividerSubtle = DarkOnSurface.copy(alpha = 0.08f)

// Stufen der Logbuch-Wochenzeile (Zustandsgrafik, FARBE-05): kein Eintrag (onSurface) und
// wenig / mittel geübt (primary); viel geübt = voller Ton.
const val ALPHA_HEAT_NONE = 0.12f
const val ALPHA_HEAT_LOW = 0.40f
const val ALPHA_HEAT_MID = 0.70f

/**
 * Ausatmen in den Rhythmusbildern der Atemlehre (Koralle), Einatmen ist Cyan. Pink trägt dort die
 * Abschnittsköpfe wie im Glossar und kann deshalb nicht zugleich Ausatmen heißen (FARBE-04, -07).
 */
val BreathExhale = Color(0xFFFF9E7A)
