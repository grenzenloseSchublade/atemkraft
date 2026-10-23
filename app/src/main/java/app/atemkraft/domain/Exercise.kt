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
 * @param evidence Studienlage als Etikett; Pflicht für jede eingebaute Übung, `null` nur bei
 *   generierten Mustern (TEXT-07).
 * @param caution Etikett „nur gesund & ausgeruht“ und Sicherheitsdialog vor dem Start
 *   (MUSTER-06); unabhängig von der Studienlage.
 * @param lore Deutung der Yoga-Lehre (eigener Abschnitt, keine Studienlage); nur bei Übungen
 *   yogischen Ursprungs und bei generierten Mustern.
 * @param guided true = getakteter Atemkreis-Ablauf; false = Programm/Gewohnheit
 *   ohne festen Takt (nur Infoseite, kein Start).
 * @param generated true = generiertes Muster (Muster des Tages, „Eigene Kreationen“): keine
 *   eingebaute Übung, die Familie ist nur Einordnung für die Farbe, nicht für den Namen.
 * @param activating Charakter eines generierten Musters: „sanft aktivierend“ statt „ruhig“
 *   (aus `PatternSpec.activating`; eingebaute Übungen zeigen „intensiv“ über [caution]).
 * @param silenceAfterAllowed Darf nach der Übung die stille Meditation folgen („Danach in die
 *   Stille“, „In die Stille ›“)? false bei Übungen, nach denen sich niemand eben hinsetzt
 *   (Atmen im Gehen); eine Eigenschaft der Übung statt einer festen Id in der Oberfläche.
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
    val evidence: EvidenceLevel? = null,
    val caution: Boolean = false,
    val lore: YogaLore? = null,
    val guided: Boolean = true,
    val generated: Boolean = false,
    val activating: Boolean = false,
    val silenceAfterAllowed: Boolean = true,
    val perRound: (phase: Phase, roundIndex: Int) -> Phase = { phase, _ -> phase },
)
