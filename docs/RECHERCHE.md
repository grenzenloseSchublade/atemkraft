# Recherche- und Entwicklungsplan

Stand: 2026-10-30, App-Version 2.9.0 (lokal; öffentlich zuletzt 2.6.0). Dieses Dokument sammelt, was seit dem großen Review am 2026-10-23 untersucht wurde, was daraus geworden ist, und welche Recherchen und Arbeiten als Nächstes anstehen. Die ausführlichen Berichte liegen unter [`docs/recherche/`](recherche/); die Regeln für Texte und Technik stehen im [Styleguide](STYLEGUIDE.md), die für Sicherheit und Recht in [SECURITY.md](SECURITY.md), der Weg in die Stores in [STORE.md](STORE.md).

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
| 2026-10-24 | Theoretische Inhalte und Yoga-Geschichte | Bestandsaufnahme des Tabs „Lernen“, 25 Kandidaten mit Quellen, Top 8, Gliederung für ein Kapitel „Geschichte des Atmens“ | [Bericht](recherche/2026-10-24-inhalte-geschichte.md); als Lesekapitel verworfen, kommt bebildert zurück (Abschnitt 3.4) |
| 2026-10-24 | UX-Vergleich mit 13 Apps (Breathwrk, Prana Breath, AtemFlow, Calm, Headspace, Insight Timer u. a.) | Gesamtnote 3,9/5; Inhalte 5, Wiederkommen und Store-Wirkung 3; 20 Empfehlungen, Einzigartiges geprüft, Minimalvorschlag für Befinden und Animation | [Bericht](recherche/2026-10-24-ux-vergleich.md); Entscheidungen getroffen (Abschnitt 3.1), umgesetzt in 2.8.0 und 2.9.0 |
| 2026-10-30 | Qualität nach ISO/IEC 25010:2023 (neun Merkmale, Gegenprüfung) | Gesamt 3,7/5; Security und Safety 4,0, Leistung 3,0 (nicht gemessen); 28 Maßnahmen, zwei mit Schwere hoch (Rulesets, R-A11Y-Protokoll) | [Bericht](recherche/2026-10-30-iso25010.md); Maßnahmen offen |

Dazu kleinere, direkt umgesetzte Rückmeldungen vom Gerät (v2.7.1): Suchtext bleibt beim Schließen der Tastatur stehen, „Muster gestalten ›“ im Leerzustand von Eigene, Stift auf den Karten der Kreationen, Vorschau-Knöpfe im Editor links und rechts in der Kurvenkarte.

## 3. Geplante Recherchen

### 3.1 UX-Vergleich – Entscheidungen

Der [Bericht](recherche/2026-10-24-ux-vergleich.md) bestätigt: In der Substanz sauber und intuitiv (3,9/5). Einzigartig und in keiner der 13 Apps gefunden: Evidenz-Etikett je Übung mit Quellen, Sicherheitshinweise je Übung mit Dialog, Yoga-Lehre getrennt von Studienlage, Muster-Editor an der Kurve, Befindens-Suche in Alltagswörtern, „Danach in die Stille“, kein Konto und keine Cloud. Schwächer: Wiederkommen (keine Erinnerung, kein Schnellstart) und Store-Wirkung (Screenshots ohne Bildunterschrift).

Zu entscheiden, nach Aufwand:
- **S:** Bildunterschriften auf den Store-Screenshots; „Zuletzt geübt ›“ unter „Verlauf“ in Eigene; Willkommenskarte verspricht „ein paar Minuten“ und öffnet 10 min – auf 3 bis 5 min stellen; „Alle Übungen ›“ bei gewähltem Anlass.
- **M:** optionale lokale Erinnerung (Uhrzeit, Wochentage; ohne Netz); Backup als eine Datei mit Logbuch und Einstellungen (Vorbild Prana Breath, ohne Drive); App-Shortcuts.
- **M–L:** Sprachansage der Phasen in der Atem-Session (Stimmen sind da, nur die Meditation spricht).
- Prüfen: Tagesmuster als erste Karte unter „Übungen“ lässt Erstnutzer stutzen; drei Speicher-Zeichen (Lesezeichen, Ring, Tab-Icon) vereinheitlichen.
- Bewusst nicht: Onboarding-Wizard, Lungentest, Paywall, Streak-Druck, Levels, Emoji-Check-in, Soundscape-Bibliotheken, Kontrollpausen-Messung mit Trend (SEC-LEGAL-02).

### 3.2 Befinden erfassen und zeigen

Idee: vor einer Übung optional, danach auf dem Ende-Screen mit einem Tipp das Befinden erfassen; im Tab „Eigene“ unter „Verlauf“ als zweite Ebene der Wochenpunkte zeigen.

Minimalvorschlag aus dem UX-Vergleich: nur „nachher“, relativ und überspringbar – „ruhiger · wie vorher · unruhiger“ (bei aktivierenden Übungen „wacher · wie vorher · müder“); im Logbuch nur als Wort, nie gerechnet; optionaler Schalter; Satz in PRIVACY.md; vorher in SECURITY.md festhalten: Selbstauskunft ohne Auswertung, keine Aggregation, keine Empfehlung.

Zu klären:
- **Skala:** fünf Stufen oder drei Wörter? Welche Wörter sind laienverständlich und frei von Diagnose-Anklang (nicht „Angst“, eher „unruhig“)? Vorbilder: AtemFlow, Daylio, Apple Health „Stimmung“.
- **Zeitpunkt:** vorher und nachher, oder nur nachher? Studien zu Mood-Tracking (Reaktivität, Abbruchquoten) sichten.
- **Darstellung:** Punkte und eine Linie, keine Scores, keine Prozent, keine Vergleiche mit anderen. Was zeigt eine Woche, was ein Monat?
- **Grenze:** Die App zeigt den Verlauf, bewertet ihn nicht und leitet nichts daraus ab (keine „dein Stress sank um 20 %“, keine Empfehlungen aus den Daten). Formulierungen gegen `config/health-claims.txt` prüfen.
- **Daten:** im Logbuch-Eintrag (Room, Migration), Export und Import wie bisher, Löschen mit dem Logbuch.

### 3.3 „Was passiert“ – die Wirkung einer Übung zeigen

Idee: auf der Detailseite ein Abschnitt mit zwei Sichten, **Körper** (Studienlage: längerer Ausatem, Rate, Halten) und **Yoga-Lehre** (das vāyu-Modell, das die Atemlehre schon beschreibt), dazu eine ruhige, abstrakte Animation im Takt der Übung; optional als zweite Ansicht hinter dem Kreis in der Session.

Vorschlag aus dem UX-Vergleich: zuerst eine parametrierte Animation „Ein Atemzug“ (Zwerchfell, Lunge, Herzschlag-Abstände beim Ausatmen weiter) statisch auf der Atemlehre-Seite mit Umschalter „Lehre: vāyus“ (die vāyu-Figur existiert schon), später Detailseite und Session. Keine App im Vergleich zeigt die Wirkung als Animation, alle erklären als Text.

Zu klären:
- **Ehrlichkeit:** Die Körper-Sicht darf nur zeigen, was die Übung belegbar tut; Mechanismen (Vagus, Baroreflex, CO₂) als Modelle kennzeichnen. Die Yoga-Sicht ist Lehre, kein Körperbild – die Animation darf beides nicht vermischen.
- **Form:** abstrakt (Strom, Richtung, Farbe der Übung), nicht anatomisch. Wie zeigen apāna (nach unten und aus) und prāṇa (nach innen und auf) ohne Körperumriss?
- **Barrierefreiheit:** ohne Systemanimationen eine stehende Darstellung; TalkBack liest den Text, nicht die Animation.
- **Aufwand:** je Übung Daten (Richtung, Phase), eine Animations-Komponente, zwei Textabschnitte je Übung.

### 3.4 Theorie neu: bebildert und mit den Übungen verknüpft

Die Erweiterung der theoretischen Grundlage bleibt ein Ziel (Eigentümer 2026-10-30: „nicht vergessen“). Die Kapitel „Geschichte des Atmens“ und „Körper“ aus 2.9.0 wurden vor der Veröffentlichung entfernt, weil die Umsetzung als reiner Lesetext nicht trug, nicht weil die Inhalte falsch waren. Der Entwurf bleibt in docs/recherche/ als Archiv.

**Grundsätze für den zweiten Anlauf**
- Jedes Thema ist ein **Bild oder eine Animation** mit höchstens zwei, drei Sätzen, nie eine neue Leseseite.
- Jedes Thema hängt an einer **Stelle, an der man übt**: Detailseite einer Übung („Was passiert“, 3.3), Anlass, Session-Ende oder Atemlehre. Wer übt, findet die Theorie dort, wo sie etwas erklärt.
- Zuerst zeichnen, rendern und ansehen, dann dem Eigentümer als Entwurf zeigen, erst danach einbauen. Körper-Sicht und Yoga-Lehre bleiben getrennt (3.3).

**Themen und ihr Ort** (Top 8 des [Inhalte-Berichts](recherche/2026-10-24-inhalte-geschichte.md))

| Thema | Bild | Ort |
|---|---|---|
| Atemmechanik, CO₂ als Atemantrieb | Animation „Ein Atemzug“ (Zwerchfell, Lunge); kleiner CO₂-Pegel, der beim Halten steigt | Zwerchfellatmung, *Dirgha*, Buteyko, Übungen mit Halten |
| Atem im Labor, Resonanz bei 6 pro Minute | Herzschlag-Abstände, die mit dem Atem schwingen; Zeitleiste 1924 → 1970 → 2002 | Resonanz-Atmung |
| *Haṭha*-Yoga und die Haṭhayogapradīpikā | eine Zeile im Zeitstrahl, Quelle am Detail | *Pranayama*-Übungen (*Nāḍī Śodhana*, *Ujjāyī*, *Bhastrikā*) |
| Patañjali, *Prāṇāyāma* als viertes Glied | acht Glieder als Kreis, das vierte hervorgehoben | Atemlehre |
| Moderner Yoga (Krishnamacharya, Iyengar, Sivananda) | Lehrlinie als kleiner Stammbaum | Atemlehre, *Viloma* |
| Panik und Hyperventilation | „lang aus statt tief ein“ als Kurve | Anlässe „Stress“ und „Kurzatmig“, mit Sicherheitszeile |
| Grenzen einer App, wann Fachleute | ein Satz, kein Kapitel | Willkommenskarte und Sicherheitshinweise |
| „Wie alt ist Yoga?“, Respekt vor der Herkunft | Zeitstrahl, der ehrlich datiert | Tab „Lernen“ als Grafik |

Was aus dem ersten Anlauf gilt weiter:

1. Haṭha-Yoga: Woher die Haṭhayogapradīpikā kommt (Sammeltext um 1450; *haṭha* heißt „Kraft“).
2. Patañjali ehrlich datiert (etwa 325 bis 425 n. Chr.), Pranayama als viertes Glied.
3. Atem im Labor: von Kuvalayananda 1924 über Swami Rama 1970 zur Resonanz bei 6 Atemzügen pro Minute (Vaschillo 2002).
4. Atemmechanik und CO₂ als Atemantrieb – Grundlage des neuen Kapitels „Körper“.
5. Moderner Yoga: Krishnamacharya, Iyengar, Sivananda; warum die Atemlehre Viniyoga-Begriffe nutzt.
6. Atem und Gefühl: Panik und Hyperventilation (bei Panik nicht tief atmen).
7. Grenzen einer App, wann Fachleute.
8. „Wie alt ist Yoga?“ und Respekt vor der Herkunft als Schlusskarte.

Vor dem Einbau: TEXT-14 um Pāli, Tibetisch und Chinesisch erweitern, etwa zehn neue Quellen mit DOI anlegen, „Atemtherapie“ gegen die Sperrwortliste testen. Bewusst weggelassen: „5000 Jahre“, Chakren als Fakt, Heilversprechen der alten Texte, Lehrer-Legenden, Evidenz-Etiketten auf Geschichtskarten.

### 3.5 Reihen mit Start

Prana Breath ist stark bei Trainingsreihen mit Steigerung; Atemkraft hat Programme (SKY, Buteyko, Nasenatmung) nur als Text. Zu klären: eine 7-Tage-Reihe mit Start-Knopf und Fortschritt im Verlauf, ohne Streak-Druck und ohne Erinnerungen, die stören. Erst nach 3.2 und 3.3.

### 3.6 Store-Veröffentlichung

Nach [STORE.md](STORE.md): Play-Konto anlegen (Prüfungen dauern Tage), eigenen Signaturschlüssel vor dem ersten App Bundle hochladen, geschlossener Test mit 15 bis 20 Testern über 14 Tage, parallel den Antrag bei F-Droid stellen. Offen beim Nutzer: Kontakt-E-Mail nur für die App, zwei verschlüsselte Offline-Backups der Schlüssel, Store-Titel („Atemkraft – Atemübungen“ oder nur „Atemkraft“).

### 3.7 Kleineres

- Baseline Profile brauchen ein Gerät oder einen Emulator (siehe STORE.md).
- Kapitelkarten im Tab „Lernen“ sind noch bunt; prüfen, ob sie den weißen Köpfen folgen sollen.
- App-Icon: Der UX-Vergleich nannte die drei Ringe austauschbar; der Eigentümer mag das Symbol. Nur Gesprächsthema, keine Änderung.
- Kurzbeschreibungen der 17 Übungen auf den Karten kürzen (Inhaltsentscheidung).
- Später: Embedding-Suche als Studienprojekt, iOS-Weg über Kotlin Multiplatform (siehe [IOS_PLAN.md](IOS_PLAN.md)).

## 4. Reihenfolge

Die Auswertung aller Berichte mit Reihenfolge je Release und den offenen Entscheidungen steht in [recherche/2026-10-24-auswertung.md](recherche/2026-10-24-auswertung.md); die Tabelle hier ist die Kurzform.


| Phase | Inhalt | Voraussetzung |
|---|---|---|
| 1 | UX-Vergleich entscheiden, Befinden (3.2) umsetzen | erledigt in 2.8.0 und 2.9.0 |
| 2 | 2.9.0 am Gerät prüfen, Altlasten abbauen, veröffentlichen | läuft |
| 3 | 3.0: „Was passiert“ (3.3) mit der Animation „Ein Atemzug“ und Theorie neu (3.4) – zeichnen, Entwurf zeigen, umsetzen; Sprachansage der Phasen | Phase 2 |
| 4 | Store: Konto, Test, Veröffentlichung | Nutzer-Entscheidungen aus 3.6 |
| 5 | Reihen mit Start (3.5) | nach 2 |

## 5. Regeln für jede Recherche

- Quellen nennen, mit DOI oder ISBN, wo es sie gibt; Sekundärquellen nur zur Orientierung. Unsicheres als unsicher markieren.
- Texte in der Form der bestehenden Karten (Teaser als geschlossener Satz, Laiensprache, Lehre zuschreibend, Studien mit Name und Jahr).
- Jede Formulierung gegen `config/health-claims.txt`; keine Heil-, Therapie- oder Präventionsversprechen, auch nicht als Zitat ohne Einordnung.
- Datenschutz zuerst: Keine Funktion, die Daten aus dem Gerät trägt oder eine gesundheitliche Bewertung vornimmt.
- Weniger ist mehr: Jede neue Fläche muss eine alte ruhiger machen oder ersetzen.
