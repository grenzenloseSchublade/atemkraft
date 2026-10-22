package app.atemkraft.domain

/**
 * Woher ein generiertes Muster stammt. Danach richten sich die Texte, die
 * [RandomPatternGenerator.exerciseFrom] baut, damit sie wahr bleiben: Nur das heutige Muster
 * des Tages ist „heute zufällig erzeugt“, ein gespeichertes ist es nicht mehr, und ein eigenes
 * Muster hat die Person selbst zusammengestellt.
 *
 * - [TODAY]: das Muster des Tages auf dem Atmen-Tab (Texte unverändert seit 1.6.8);
 * - [SAVED_DAILY]: ein Muster des Tages, das in „Meine Muster“ gespeichert wurde;
 * - [CUSTOM]: ein im Muster-Editor zusammengestelltes Muster („Eigenes Muster“). Seit „Frei
 *   gestalten“ ist das eine freie Folge ([FreePattern], Texte aus
 *   [RandomPatternGenerator.exerciseFrom] mit [FreePattern]); die Variante mit [PatternSpec]
 *   bleibt für Muster mit vier festen Feldern.
 *
 * Gespeichert wird die Herkunft als String mit Rückfall (Datenschicht), nie als Enum direkt.
 */
enum class PatternOrigin { TODAY, SAVED_DAILY, CUSTOM }
