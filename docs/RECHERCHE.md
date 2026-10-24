# Recherche- und Entwicklungsplan

Stand: 2026-10-24, App-Version 2.7.0. Dieses Dokument sammelt, was seit dem großen Review am 2026-10-23 untersucht wurde, was daraus geworden ist, und welche Recherchen und Arbeiten als Nächstes anstehen. Die ausführlichen Berichte liegen unter [`docs/recherche/`](recherche/); die Regeln für Texte und Technik stehen im [Styleguide](STYLEGUIDE.md), die für Sicherheit und Recht in [SECURITY.md](SECURITY.md), der Weg in die Stores in [STORE.md](STORE.md).

## 1. Leitlinie

**Atem ist keine Zahl.** Atemkraft folgt dem Gefühl und rechnet im Hintergrund: Quellen, Studienlage und Takt sind da, wenn man sie will, drängen sich aber nicht auf. **Der Datenschutz ist und bleibt die wichtigste Kennzahl:** alles bleibt auf dem Gerät, kein Konto, keine Cloud, keine Werbung. Daraus folgen drei Prüffragen für jede neue Idee:

1. Bleiben alle Daten lokal, und kann man sie exportieren oder löschen?
2. Zeigt die App etwas, oder bewertet sie etwas? Zeigen ist erlaubt; eine Auswertung mit gesundheitlicher Bewertung nicht (SEC-LEGAL-02, kein Medizinprodukt).
3. Wird die Oberfläche ruhiger oder voller? Zahlen gehören ins Detail, nie auf die erste Ebene.

Der Nutzer mag es bunt, die App soll aber „der Masse gefallen“. Deshalb sind Köpfe weiß, Farbe tragen nur Gruppen, Kartenränder und wenige Akzente. Eine Farboption für Liebhaber ist denkbar, aber nicht geplant.

## 2. Was bisher untersucht wurde

| Datum | Thema | Ergebnis | Stand |
|---|---|---|---|
| 2026-10-23 | Großer Review in sechs Dimensionen (Korrektheit, Oberfläche und Barrierefreiheit, Sicherheit und Lizenzen, Inhalte, Architektur, UX) | 165 Befunde | umgesetzt in v2.6.0, alle Pakete; Berichte nicht archiviert, weil in Code und Styleguide eingeflossen |
| 2026-10-23 | Store-Recherche: Google Play, F-Droid, IzzyOnDroid, Entwicklerverifizierung | Ablauf, Formulare, Signatur-Strategie, Blocker | [Bericht](recherche/2026-10-23-store.md), Anleitung in [STORE.md](STORE.md); IzzyOnDroid zurückgestellt (Regel gegen KI-generierten Code) |
| 2026-10-24 | Stimmigkeit der Tabs Atmen und Eigene | zu viele Bausprachen; „Vorschlag B“: Suchfeld als Pille, weiße Köpfe, kleine Filter-Chips, drei gleiche Abschnitte in Eigene; Karten und Merkzeichen bleiben | umgesetzt in v2.7.0 |
| 2026-10-24 | Theoretische Inhalte und Yoga-Geschichte | Bestandsaufnahme des Tabs „Lernen“, 25 Kandidaten mit Quellen, Top 8, Gliederung für ein Kapitel „Geschichte des Atmens“ | [Bericht](recherche/2026-10-24-inhalte-geschichte.md); Umsetzung geplant (Abschnitt 4) |
| 2026-10-24 | UX-Vergleich mit beliebten Apps (Breathwrk, Prana Breath, AtemFlow, Calm, Headspace, Insight Timer u. a.) | läuft | Bericht folgt unter `recherche/2026-10-24-ux-vergleich.md` |

Dazu kleinere, direkt umgesetzte Rückmeldungen vom Gerät (v2.7.1, in Arbeit): Suchtext bleibt beim Schließen der Tastatur stehen, „Muster gestalten ›“ im Leerzustand von Eigene, Stift auf den Karten der Kreationen, Vorschau-Knöpfe im Editor links und rechts in der Kurvenkarte.

## 3. Geplante Recherchen

### 3.1 UX-Vergleich (läuft)

Fragen: Ist die App in zehn Sekunden verständlich? Wie viele Tipps bis zur ersten Übung? Was machen AtemFlow (Tagebuch, „wie wirkt der Atem“) und Prana Breath (Muster-Engineering, Reihen) besser, was überladen sie? Was ist an Atemkraft einzigartig (Evidenz-Etiketten, Yoga-Lehre, Muster-Editor, keine Cloud)? Ergebnis: Noten je Dimension, Vergleichstabelle, höchstens 20 Empfehlungen, drei Dinge, die bewusst nicht übernommen werden.

### 3.2 Befinden erfassen und zeigen

Idee: vor einer Übung optional, danach auf dem Ende-Screen mit einem Tipp das Befinden erfassen; im Tab „Eigene“ unter „Verlauf“ als zweite Ebene der Wochenpunkte zeigen.

Zu klären:
- **Skala:** fünf Stufen oder drei Wörter? Welche Wörter sind laienverständlich und frei von Diagnose-Anklang (nicht „Angst“, eher „unruhig“)? Vorbilder: AtemFlow, Daylio, Apple Health „Stimmung“.
- **Zeitpunkt:** vorher und nachher, oder nur nachher? Studien zu Mood-Tracking (Reaktivität, Abbruchquoten) sichten.
- **Darstellung:** Punkte und eine Linie, keine Scores, keine Prozent, keine Vergleiche mit anderen. Was zeigt eine Woche, was ein Monat?
- **Grenze:** Die App zeigt den Verlauf, bewertet ihn nicht und leitet nichts daraus ab (keine „dein Stress sank um 20 %“, keine Empfehlungen aus den Daten). Formulierungen gegen `config/health-claims.txt` prüfen.
- **Daten:** im Logbuch-Eintrag (Room, Migration), Export und Import wie bisher, Löschen mit dem Logbuch.

### 3.3 „Was passiert“ – die Wirkung einer Übung zeigen

Idee: auf der Detailseite ein Abschnitt mit zwei Sichten, **Körper** (Studienlage: längerer Ausatem, Rate, Halten) und **Yoga-Lehre** (das vāyu-Modell, das die Atemlehre schon beschreibt), dazu eine ruhige, abstrakte Animation im Takt der Übung; optional als zweite Ansicht hinter dem Kreis in der Session.

Zu klären:
- **Ehrlichkeit:** Die Körper-Sicht darf nur zeigen, was die Übung belegbar tut; Mechanismen (Vagus, Baroreflex, CO₂) als Modelle kennzeichnen. Die Yoga-Sicht ist Lehre, kein Körperbild – die Animation darf beides nicht vermischen.
- **Form:** abstrakt (Strom, Richtung, Farbe der Übung), nicht anatomisch. Wie zeigen apāna (nach unten und aus) und prāṇa (nach innen und auf) ohne Körperumriss?
- **Barrierefreiheit:** ohne Systemanimationen eine stehende Darstellung; TalkBack liest den Text, nicht die Animation.
- **Aufwand:** je Übung Daten (Richtung, Phase), eine Animations-Komponente, zwei Textabschnitte je Übung.

### 3.4 Kapitel „Geschichte des Atmens“ und Kapitel „Körper“

Grundlage ist der [Inhalte-Bericht](recherche/2026-10-24-inhalte-geschichte.md). Reihenfolge nach dessen Top 8:

1. Haṭha-Yoga: Woher die Haṭhayogapradīpikā kommt (Sammeltext um 1450; *haṭha* heißt „Kraft“).
2. Patañjali ehrlich datiert (etwa 325 bis 425 n. Chr.), Pranayama als viertes Glied.
3. Atem im Labor: von Kuvalayananda 1924 über Swami Rama 1970 zur Resonanz bei 6 Atemzügen pro Minute (Vaschillo 2002).
4. Atemmechanik und CO₂ als Atemantrieb – Grundlage des neuen Kapitels „Körper“.
5. Moderner Yoga: Krishnamacharya, Iyengar, Sivananda; warum die Atemlehre Viniyoga-Begriffe nutzt.
6. Atem und Gefühl: Panik und Hyperventilation (bei Panik nicht tief atmen).
7. Grenzen einer App, wann Fachleute.
8. „Wie alt ist Yoga?“ und Respekt vor der Herkunft als Schlusskarte.

Aufbau: fünfte Kapitelkarte im Tab „Lernen“, Daten wie `Pranayama.topics` (Teaser ≤ 95 Zeichen, zwei bis drei Absätze, Lehrquelle), vier Abschnitte Indien, Andere Wege, Der Westen, Heute; Literatur (Bücher) getrennt von Studien (`Refs`). Vor dem Einbau: TEXT-14 um Pāli, Tibetisch und Chinesisch erweitern, etwa zehn neue Quellen mit DOI anlegen, „Atemtherapie“ gegen die Sperrwortliste testen. Bewusst weggelassen: „5000 Jahre“, Chakren als Fakt, Heilversprechen der alten Texte, Lehrer-Legenden, Evidenz-Etiketten auf Geschichtskarten.

### 3.5 Reihen mit Start

Prana Breath ist stark bei Trainingsreihen mit Steigerung; Atemkraft hat Programme (SKY, Buteyko, Nasenatmung) nur als Text. Zu klären: eine 7-Tage-Reihe mit Start-Knopf und Fortschritt im Verlauf, ohne Streak-Druck und ohne Erinnerungen, die stören. Erst nach 3.2 und 3.3.

### 3.6 Store-Veröffentlichung

Nach [STORE.md](STORE.md): Play-Konto anlegen (Prüfungen dauern Tage), eigenen Signaturschlüssel vor dem ersten App Bundle hochladen, geschlossener Test mit 15 bis 20 Testern über 14 Tage, parallel den Antrag bei F-Droid stellen. Offen beim Nutzer: Kontakt-E-Mail nur für die App, zwei verschlüsselte Offline-Backups der Schlüssel, Store-Titel („Atemkraft – Atemübungen“ oder nur „Atemkraft“).

### 3.7 Kleineres

- Baseline Profile brauchen ein Gerät oder einen Emulator (siehe STORE.md).
- Kapitelkarten im Tab „Lernen“ sind noch bunt; prüfen, ob sie den weißen Köpfen folgen sollen.
- Kurzbeschreibungen der 17 Übungen auf den Karten kürzen (Inhaltsentscheidung).
- Später: Embedding-Suche als Studienprojekt, iOS-Weg über Kotlin Multiplatform (siehe [IOS_PLAN.md](IOS_PLAN.md)).

## 4. Reihenfolge

| Phase | Inhalt | Voraussetzung |
|---|---|---|
| 1 | v2.7.1 am Gerät prüfen; UX-Vergleich lesen und Entscheidungen treffen | läuft |
| 2 | Befinden (3.2) und „Was passiert“ (3.3) als Canvas zeichnen, entscheiden, umsetzen | Phase 1 |
| 3 | Kapitel „Geschichte des Atmens“ und „Körper“ schreiben (Texte zuerst, Freigabe, dann Einbau) | unabhängig, parallel zu 2 möglich |
| 4 | Store: Konto, Test, Veröffentlichung | Nutzer-Entscheidungen aus 3.6 |
| 5 | Reihen mit Start (3.5) | nach 2 |

## 5. Regeln für jede Recherche

- Quellen nennen, mit DOI oder ISBN, wo es sie gibt; Sekundärquellen nur zur Orientierung. Unsicheres als unsicher markieren.
- Texte in der Form der bestehenden Karten (Teaser als geschlossener Satz, Laiensprache, Lehre zuschreibend, Studien mit Name und Jahr).
- Jede Formulierung gegen `config/health-claims.txt`; keine Heil-, Therapie- oder Präventionsversprechen, auch nicht als Zitat ohne Einordnung.
- Datenschutz zuerst: Keine Funktion, die Daten aus dem Gerät trägt oder eine gesundheitliche Bewertung vornimmt.
- Weniger ist mehr: Jede neue Fläche muss eine alte ruhiger machen oder ersetzen.
