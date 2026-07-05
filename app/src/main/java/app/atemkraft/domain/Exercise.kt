package app.atemkraft.domain

/**
 * Eine vollständige Atemübung inkl. Einordnung, Beschreibung und Belegen.
 *
 * Eine Übung besteht aus [rounds] Runden; jede Runde durchläuft alle [segments]
 * (ein Segment kann seine Phasen mehrfach wiederholen, siehe [Segment.repeat]).
 *
 * @param family Familie für Sortierung/Farbe im Startscreen.
 * @param shortDescription Knappe Beschreibung der Technik (1–3 Sätze).
 * @param effect Teaser im „Wirkung"-Abschnitt: ein abgeschlossener Satz mit Punkt,
 *   der in ~2 Zeilen (≈95 Zeichen) passt – nichts darf abgeschnitten wirken. Bleibt
 *   auch aufgeklappt sichtbar.
 * @param effectDetail Ausführliche Vertiefung desselben Sachverhalts (Mechanismus,
 *   Studienlage, Einordnung), die ausgeklappt zusätzlich unter [effect] erscheint –
 *   knüpft inhaltlich an den Teaser an, ersetzt ihn nicht (null = nur Teaser).
 * @param instructionHint Kurzanleitung in Worten (ergänzend zum getakteten Ablauf).
 * @param cautions Sicherheits-/Anwendungshinweise (können leer sein).
 * @param references Verifizierte Quellen zu den Wirkaussagen.
 * @param tag Optionales Badge (gut belegt / Vorsicht).
 * @param guided true = getakteter Atemkreis-Ablauf; false = Programm/Gewohnheit
 *   ohne festen Takt (nur Infoseite, kein Start).
 * @param perRound Hook, um Phasen pro Runde anzupassen (z. B. Wim-Hof-Retention).
 */
data class Exercise(
    val id: String,
    val name: String,
    val family: BreathingFamily,
    val shortDescription: String,
    val effect: String,
    val effectDetail: String? = null,
    val segments: List<Segment> = emptyList(),
    val rounds: Int = 1,
    val instructionHint: String? = null,
    val cautions: List<String> = emptyList(),
    val references: List<Reference> = emptyList(),
    val tag: EvidenceTag? = null,
    val guided: Boolean = true,
    val perRound: (phase: Phase, roundIndex: Int) -> Phase = { phase, _ -> phase },
)
