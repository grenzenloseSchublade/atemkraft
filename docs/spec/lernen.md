# Spezifikation: Tab „Lernen“ – Atemlehre, Pranayama, Programme, Begriffe

Regeln: [STYLEGUIDE TEXT-13](../STYLEGUIDE.md#text) (Yoga-Lehre getrennt von der Studienlage), TEXT-14 (Sanskrit), FARBE-04/-07, MOTION-02; Anatomie der Karten und Köpfe in [§4.5](../STYLEGUIDE.md#anatomie). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/learn/`, `ui/atemlehre/`, `ui/glossary/`, Inhalte in `data/Atemlehre.kt`, `data/Pranayama.kt`, `data/Glossary.kt`.

## Tab „Lernen“ (`LearnScreen`)

Vier gleich gebaute Kapitelkarten (`NavCard`): Atemlehre, Pranayama, Programme & Gewohnheiten, Begriffe – je mit Hinweis und eigener Push-Seite. Nur hier färben sich die Kartentitel reihum durch den Kopf-Ring (`headingColor(index)`: Pink → Koralle → Lila → Gelb → Grün; Lage der Karte, ohne Bedeutung; Ring-Töne dürfen Inhaltsfarben gleichen, deshalb aus der ΔE-Prüfung ausgenommen, jeder Ton ≥ 4,5:1 auf Grund und Karte). Zahnrad im Kopf wie auf jedem Tab.

## Atemlehre (`AtemlehreScreen`)

- Push-Seite mit `ScreenSubtitle`; aufklappbare Abschnitte (`CollapsibleSection`, Standard zu, `rememberSaveable`): „Die vier Phasen“ (Phasen-Karte: Cyan = Einatmen, Koralle `BreathExhale` = Ausatmen, gestrichelt = Halten, in Worten erklärt), „Die fünf *vāyus*“, Grundregeln, Muster-Karten (`ExpanderCard` mit Rhythmusbild `PhaseMark`/`HoldDash` und Takt als `header`), „Weitere Einflüsse“, Quellen. Verweise auf Übungen als `LinkChip`s (`ExerciseChips(ids, exercises, onOpenExercise)`, „In der App“); `focus` springt zu einer Karte (`contentModifier` des `PushScaffold`). Die offenen Abschnitte „Die vier Phasen“ und „Muster“ kleben mit ihrem Kopf oben (`CollapsibleSection(sticky = …)`; der Sprung aus der Detailseite zu einer Musterkarte rechnet die Höhe des klebenden Kopfs ab); aufklappbare kleben nicht, der Caret oben täte nichts – sie beenden aber den klebenden Abschnitt darüber, sonst stünde „Die vier Phasen“ über „Die Grundregeln“.
- **vāyu-Bild** (`VayuFigure` / `VayusContent`, `ui/atemlehre/VayuFigure.kt`; Design Vayus.dc.html): Einleitung, Bild, Hinweis, Liste, Quelle. Bild als `Canvas` (Vorlage 352 × 450, höchstens `VayuFigureMaxWidth` 280, sonst 80 % der Spalte; Striche, Weichzeichner und Aura in Einheiten der Vorlage) auf `surface` (`shapes.medium`): Piktogramm-Figur (Kopf frei, Arme mit Spalt zum Rumpf, getrennte Beine, Umriss `VayuFigureOutline` 2,5) über `VayuFigureFill`; weiche Farbfelder je vāyu als radiale Verläufe (σ 9), auf die Figur geclippt (udāna Hals/Kopf, prāṇa Brust, samāna Nabelmitte, apāna Becken/Beine, vyāna ganze Figur); vyāna zusätzlich als Aura (vier breite, leise Lagen); gepunktete Strömungslinien (3 · 9) in Flussrichtung (udāna aufwärts, prāṇa von beiden Seiten in die Brust, samāna kreisend, apāna die Beine hinab, vyāna nach außen), Beschriftungen kursiv (`labelMedium`, wächst mit der Systemschrift, bleibt im Bild) mit Führungslinie. Farben `VayuPrana` … `VayuVyana` (prāṇa Cyan wie das Einatmen, apāna Koralle wie das Ausatmen; Gelb und Grün hier weder Warnung noch Familie), Zustände über `VayuAlpha(selected, idle, dimmed)` (§2.2), Bewegung `Motion.VayuFlow` / `VayuFlowSelected` / `VayuPulse` (Pulsieren 0,55 ↔ 0,95 in 2,6 s nur nach Antippen – Ausnahme MOTION-02; bei Animator-Skala 0 steht alles still).
- Liste: je vāyu eine Zeile (Punkt `GapSmall`, Name kursiv in der vāyu-Farbe · Ort, darunter der Satz der Lehre), Polster `ListGap` × `GapSmall`, gewählt `surfaceVariant` mit Rand `BorderThin` in der vāyu-Farbe (`shapes.small`, `selectedRow`). Antippen einer Zeile oder der Stelle am Körper (Beschriftung, Farbfeld – das kleinere zuerst –, sonst im Bereich von Figur, Aura und Linien vyāna) hebt hervor: gewählte Fläche kräftig und ruhig pulsierend, Linien schneller, Rest gedimmt; ein zweiter Tipp löst. Gedimmte Beschriftungen sind Zustand, nicht Inhalt: Name, Ort und Deutung stehen in der Liste voll lesbar.
- TalkBack: Bild ein Knoten (`Role.Image`, `cd_vayu_figure`), das Antippen im Bild ist eine Abkürzung – der zugängliche Weg ist die Liste; Liste als Einzelwahl (`selectableGroup`, `selectable(Role.RadioButton)`, Name aus dem Zeilentext).

## Pranayama (`PranayamaScreen`)

Grundlagen und Techniken als `ExpanderCard`s unter `SectionHeader`s (beide Köpfe kleben samt Teaser oben, `CollapsibleSection(sticky = …)`; „Quellen“ ist aufklappbar und klebt nicht); je Technik Tradition, „So geht’s“, Studien mit Etikett, Sicherheit und „Üben“ (Unter-Überschrift `Labeled` mit der Übung als `LinkChip`). Quellen als `CollapsibleSection`. Immer zuschreibend formuliert („gilt in der Yoga-Lehre als …“), nie als Wirkung; Verse deutsch zitiert („Hatha Yoga Pradipika 2,68“); Teaser-Regel wie TEXT-06; „Studien“-Zeilen mit Quelle nach TEXT-12.

## Programme & Gewohnheiten (`ProgramsScreen`)

Die nicht angeleiteten Übungen (`guided = false`, Chip „zum Nachlesen“) als `ExerciseCard`s ohne Metazeile; persönliche Markierung sichtbar.

## Begriffe (`GlossaryScreen`)

Zwei Gruppen (Körper & Studien, Yoga-Lehre) als `CollapsibleSection`s (erster Kopf mit `headerTop = 0`; der Kopf klebt oben, solange man in seiner Gruppe liest, `CollapsibleSection(sticky = …)`), Einträge als Karten mit `CardTitle` und `BodyText`. Jeder Fachbegriff aus Übungen, Situationen und Lehre steht hier (TEXT-05; manuell R-TEXT).

## „Aus der Yoga-Lehre“ auf der Detailseite

Eigener Abschnitt nur für Übungen yogischen Ursprungs und generierte Muster (`Exercise.lore`): Teaser ganz (ohne liveRegion), Detail, Quelle, Verweise „… in der Atemlehre ›“ in `secondary`, nie in der Familienfarbe. Generierte Muster werden nach den Summen eingeordnet (Aus > Ein *laṅghana*, < *bṛṃhaṇa*, = *samāna*).

## Schreibweisen (TEXT-14)

- Fachwörter der Lehre klein, kursiv, IAST, in den Daten zwischen Sternchen: *prāṇa*, *vāyu*, *laṅghana*; `sanskritText` setzt sie kursiv.
- Aufrecht als Eigenname: Übungen und Techniken (Nadi Shodhana, Ujjayi, Viloma, Kapalabhati), Personen, Schulen, Texttitel. Geht es um die Wortbedeutung, steht der Name als Wort (klein, kursiv).
- Eingedeutscht und normal gesetzt: Yoga, Ayurveda.
- Namen, die Nutzer selbst vergeben, stehen wörtlich (`marks = false`).

## Prüfung

`ScreenshotTest` (`15_lernen`, `12_atemlehre_offen` samt vāyus, `16_pranayama_offen`, `17_programme`, `06_glossar`), `ContentRulesTest` (`lore-teaser`, `cross-references`), manuell R-TEXT (Auszeichnung vollständig, zuschreibend, jeder Fachbegriff im Glossar), R-A11Y.
