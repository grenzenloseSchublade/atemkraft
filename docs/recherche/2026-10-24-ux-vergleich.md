# Atemkraft v2.7.0 – UX-Vergleich mit den gängigen Atem- und Meditations-Apps

Stand: 2026-10-24, `main` 5af8193 (v2.7.0). Nur gelesen, nichts geändert.

**Frage des Nutzers:** „Haben wir nun eine saubere, nutzerfreundliche und intuitive App?“

**Kurzantwort:** Ja, in der Substanz. Die App ist ruhig, konsistent und auf dem Weg Anlass → Übung → Sitzung klar; die Inhalte sind in dieser Kategorie ohne Gegenstück. Die Lücken liegen nicht in der Bedienung, sondern beim *Wiederkommen* (keine Erinnerung, kein Schnellstart der letzten Übung) und bei der *Store-Erstwirkung* (rohe Screenshots ohne Bildunterschrift). Gesamtnote 3,9 / 5; Inhalte 5, Wiederkommen und Store je 3.

## Grundlage

- **App:** Store-Screenshots `fastlane/metadata/android/de-DE/images/phoneScreenshots/1–6.png` (v2.7.0, 11:52 Uhr), Roborazzi-Bilder `app/build/outputs/roborazzi/kompakt360/fs100/` (01:50 Uhr, also **vor** dem Umbau 2.7.0 – nur für unveränderte Screens genutzt: Session, Meditation, Editor, Lernen, Logbuch, Einstellungen), Code unter `app/src/main/java/app/atemkraft/ui/` (HomeScreen, WelcomeCard, ExerciseFilterRow, OwnPatternsScreen, SessionScreen, ExerciseDetailScreen, SettingsScreen), `strings.xml`, `docs/STYLEGUIDE.md` (§1, §14 Backlog), `docs/STORE.md`, `fastlane`-Texte, `README.md`.
- **Konkurrenz:** Websuche am 2026-10-24 (Store-Listings, Teardown-Seiten, Reviews; Quellen am Ende); AtemFlow und Prana Breath zusätzlich vertieft (eigener Abschnitt vor den Quellen, inkl. Einschätzung der Idee „Befinden vorher/nachher + Atem-Animation“). Play-Zahlen stammen aus Drittquellen und sind „ca.“; Onboarding-Details aus Teardowns, nicht aus eigener Nutzung.
- **Nicht bewertet** (laufen parallel in v2.7.1): Suchfeld-Bug, Link „Muster gestalten“ im Leerzustand Eigene, Stift auf Kreationen-Karten, Vorschau-Knöpfe im Editor.

## Was die Konkurrenz macht (Kurzfassung)

| | Erststart | Auswahl | Session | Ende | Gewohnheit | Personalisierung | Modell |
|---|---|---|---|---|---|---|---|
| **Breathwrk** (Peloton) | 14 Schritte, Lungentest → „Lung Score“, harte Paywall *vor* Account | nach Ziel: Calm/Sleep/Energize/Focus/Health; Klassen, 7-Tage-Programme | Visualizer, Audio-Cues, Haptik, Coach-Stimmen, Soundscapes | Zusammenfassung, Zitate | Streaks, Levels, Erinnerungen, Apple Health | Sounds/Visuals/Coach; kein Muster-Editor | Abo ~9 $/Monat; Tracking; Play ca. 3,7 ★ |
| **Prana Breath** | kein Konto, Essentials gratis | 80+ Praktiken: 27 Pranayamas, Traditionen, Anwendungen | Kreis mit Zeiten, Ton je Phase, 34 Hintergrundklänge, Bildschirm aus | Log, Level | Statistik, Streaks, Erinnerungen, Gesundheitstests | Editor mit Verhältnis × Sekunden, dynamische Trainings (Guru) | Einmalkauf ~10 $; UX „veraltet“, „setzt Vorwissen voraus“ |
| **Breathly** (FOSS) | direkt Technikwahl | Handvoll Kacheln + Custom | Text wächst/schrumpft, Glocke/2 Stimmen, Vibration | nichts | nichts | Schritte 0,5 s, Hell/Dunkel | gratis, MPL-2.0, keine Daten |
| **Insight Timer** | Registrierung im Flow | riesige Bibliothek, Lehrer, Kurse | Timer mit Intervallglocken, Ambient | Statistik; „als beendet markieren“ zählt | Streak (abschaltbar), Gruppen | Timer-Klänge (teils Premium) | Gratis-Kern, Plus 60 $/Jahr; Tracking; Play ca. 4,7 ★ |
| **Calm / Headspace** | Zielfragen (Mehrfachauswahl), Konto, Trial-Paywall | Daily, Kategorien | Audio-zentriert, Breathe-Bubble | Calm: Stimmungs-Emoji | Streak (Headspace abschaltbar), Mindful Minutes | Stimme/Klang | 70–80 $/Jahr |
| **Paced Breathing** | kein Login, Werbung | Profile | Welle, Töne/Stimme, Vibration, Bildschirm aus | – | Streaks, Erinnerungen | 0,1 s, Ramp-Modus | gratis + Ads; Play ca. 4,6 ★ |
| **iBreathe / Oak / Breathe r&f / Apple** | iOS-only bzw. Watch; direkt nutzbar | Presets + eigene Muster | minimal, Haptik, Glocke | iBreathe: Trinkgeld-Knopf | Erinnerungen, Widgets, Health | Intervalle 0,1–0,5 s | gratis + IAP/Ads |
| **Othership** | 9 Schritte, Video, weiche Paywall | UP/DOWN/ALL AROUND/BODY/BRAIN | Musik + Stimme | Streaks, Challenges | – | – | 100–130 $/Jahr; Wim Hof/Breath of Fire **ohne** Warnhinweise |
| **AtemFlow** (DE) | Konto, 14 Tage Test | 200+ Video/Audio-Übungen, „Atemcheck“ nach Befinden, „Dein Anker“ | Trainerin-Stimme, Atemwelle, Atemraum gratis | Befindlichkeits-Check-in (freiwillig) | Dashboard, KP-Kurve/Trend, Mandala-Reise, Erinnerung | Hell/Dunkel, 4 Schriftgrößen | 19 €/Monat; Werte lokal verschlüsselt, Konto/Nutzung in Cloud; Play 4,7 ★ (≈320), 50 K+ |

Querschnitt: Zielkategorien dominieren; Streaks fast überall (Gegenbewegung: abschaltbar bei Headspace/Insight); Bildschirm-aus-Betrieb ist der häufigste Nutzerwunsch; **keine App kennzeichnet Evidenz pro Übung**; Pranayama systematisch nur bei Prana Breath (als Timer mit Text) und Insight Timer (Lehrer-Kurse); Sicherheitswarnungen bei Hyperventilation nur in der offiziellen Wim-Hof-App und bei Prana Breath („what can go wrong“).

## Bewertung je Dimension

### (a) Erster Eindruck und Orientierung in 10 s – **4 / 5**

Was ein neuer Nutzer sieht (Store-Bild 2, `HomeScreen.kt`, `WelcomeCard.kt`): Titel „Atmen“, Zahnrad, Suchpille „Wie fühlst du dich?“, sechs große Anlass-Chips (Stress, Kurzatmig, Einschlafen, Fokus, Wach werden, Erschöpft), beim ersten Start darüber die Karte „Neu hier?“ mit Link „Resonanz-Atmung ›“ und Gesundheitshinweis; vier Tabs Atmen · Eigene · Meditation · Lernen. Kein Onboarding, kein Konto, keine Paywall – das ist gegenüber Breathwrk/Calm/Headspace (4–14 Schritte, Karte vorab) ein echter Vorteil und entspricht dem Werkzeug-Lager (Prana Breath, Breathly, Paced). Die Anlass-Chips sind die *In-situ*-Variante der Zielfrage „What brings you to …?“ – gleiche Funktion, kein Wizard.

Abzüge:
- Unter dem Kopf „Übungen“ ist die **erste Karte ein generiertes „Tagesmuster 5·1·7“** (`HomeScreen.kt:246–255`). Ein Neuling sieht vor den kuratierten Übungen ein Muster ohne Wirkversprechen und ohne Namen, den er einordnen kann. Die Eyebrow „MUSTER DES TAGES“ erklärt die Herkunft, nicht den Zweck. (Entscheidung von heute, „Vorschlag B“ – trotzdem der Punkt, an dem ein Erstnutzer am ehesten stutzt.)
- Auf 360 dp liegt die erste echte Übungskarte beim ersten Start unter dem Falz (Willkommen + Pille + Chips + Kopf + Filter + Tagesmuster). Der Link in der Willkommenskarte fängt das auf.
- Tab-Name **„Eigene“** ist für sich allein unvollständig (eigene *was*?) und sein Lesezeichen-Icon kollidiert mit dem Lesezeichen „Tagesmuster speichern“ (siehe f).
- Keine „Hero“-Fläche wie bei Content-Apps – bewusst (PRIN-02), für die Masse aber etwas nüchtern. Die Neon-Palette auf Dunkel ist markant und ruhig; nur dunkel (PRIN-01) ist in der Kategorie üblich (Breathwrk, Othership, Schlaf-Apps).

### (b) Weg zur ersten Übung – **4 / 5**

Chip → ein Satz („Langer, ruhiger Ausatem zum Herunterfahren vor dem Schlafen.“) → zwei bis drei Karten mit Phasenzeile und Dauer → Detail mit Bereich, Evidenz-Etikett, Dauer-Chip, Charakter-Chip, Anleitung, Wirkung (Expander), Hinweise & Sicherheit, Quellen, Phasen/Dauer anpassen, fester Knopf „Starten · ca. 10 min“. Das ist klarer als bei Calm (Kritik: keine Filter) und viel klarer als bei Prana Breath (80 Praktiken, „setzt voraus, dass man weiß, welche Zeiten man braucht“). Die Suche nach Befinden („Angst vor der Prüfung“ → Stress + Fokus) ist in keiner Konkurrenz-App vorhanden.

Abzüge:
- **Die Willkommenskarte verspricht „ein paar Minuten“, der Link führt zur Resonanz-Atmung mit 10 min Voreinstellung** (`welcome_body`, `BuiltInExercises.kt` Resonanz `defaultMinutes` 10). Ein Erstnutzer, der nicht bis „Dauer“ scrollt, startet 10 Minuten. Breathwrk/Oak beginnen mit 1–3 Minuten.
- Bei gewähltem Chip gibt es keinen sichtbaren Weg „Alle Übungen“ außer dem zweiten Tipp auf den Chip oder System-Zurück (`HomeScreen.kt:189–194`). Üblich und gelernt, aber nicht angezeigt.
- Die Detailseite ist vor dem Start textreich (vier Abschnitte); der Start-Knopf ist fest unten, das rettet es. Die Dauer sitzt unter den Abschnitten – für Anfänger die wichtigste Einstellung.
- Kein „Zuletzt geübt“-Schnellstart (siehe d).

### (c) Session-Erlebnis – **4 / 5**

Store-Bild 1, `SessionScreen.kt`, `09_session_*`: Countdown „Gleich geht's los · 3“, Kreis wächst/schrumpft, im Kreis Phasenwort und Sekunden, Fortschrittsbogen, Anleitungszeile („durch die Nase“), „Als Nächstes · Halten“, Pause/Neustart/Ende, Ton-Schalter, Stille-Umschalter (Uhr-Icon), Ruhe-Ansicht nach wenigen Sekunden, **Minimieren zur Mini-Leiste** (weiter in der App stöbern, Sitzung läuft – das hat keine Konkurrenz-App), Vorder­grund-Service (Bildschirm aus), Töne/Dauerton/Vibration, Sicherheitsdialog vor Wim Hof/Feueratmung. Ende: „✓ Geschafft – Spür kurz nach, wie es dir geht.“, Bilanz (Runden · Zeit · Atemzüge), „Im Logbuch ansehen ›“, „Nochmal / Ende“, optional „In die Stille ›“. Das Ende ist ruhiger und ehrlicher als Breathwrk (Zitate) oder iBreathe (Trinkgeld-Knopf).

Abzüge:
- **Keine Sprachansage der Phasen in Atem-Sessions** (nur die Meditation spricht; `settings_voice` unter „Meditation“, kein Cue-Audio in `BreathSessionController`). Breathwrk, Prana Breath, Breathly, Paced Breathing und Oak sprechen „Einatmen/Ausatmen“ – für Augen-zu-Nutzung neben Ton und Vibration der dritte Kanal. Die Stimmen-Infrastruktur (sherpa-onnx) ist bereits da.
- Dauerton blendet in 15 ms aus (Backlog S-36); Skalensprünge bei Phasenwechseln (S-11) – hörbar/sichtbar kleine Härten.
- Keine Stimmungsabfrage am Ende – für den Nutzergeschmack richtig; Calm hat sie (siehe „nicht übernehmen“).

### (d) Wiederkommen – **3 / 5**

Vorhanden: Logbuch (Heute/Gestern/Woche, Wochenpunkte, „2 Tage in Folge“, Minuten gesamt), Verlauf im Tab Eigene, „Tut mir gut“-Markierung, Export/Import der Muster, Hinweis zum Handywechsel. Die Serie ist unaufdringlich (nur eine Zeile, keine Verlust-Meldung) – das ist die Variante, die Headspace und Insight Timer gerade nachrüsten (abschaltbar).

Fehlt gegenüber praktisch allen Konkurrenten:
- **Keine Erinnerung.** Apple, Prana Breath, Breathwrk, Paced, iBreathe, Breathe r&f, Calm, Headspace haben sie. Eine *lokale* Erinnerung (Uhrzeit, Wochentage, AlarmManager/WorkManager + lokale Benachrichtigung) braucht weder Netz noch Cloud und widerspricht „keine Cloud“ nicht.
- **Kein Schnellstart der letzten Übung.** Wer gestern Resonanz geübt hat, muss sie im Atmen-Tab suchen oder Logbuch → Eintrag → „Übung öffnen“ gehen. Content-Apps zeigen „Weiter“/„Heute“ oben.
- Keine App-Shortcuts (Long-Press auf das Icon), kein Widget, kein Wear OS, kein Health Connect. Wear OS hat allerdings auch keine der Android-Konkurrenten belegt.

### (e) Lernen und Inhalte – **5 / 5**

Hier ist Atemkraft allein. Belegt im Code und in den Screens:
- **Evidenz-Etiketten pro Übung** in drei Stufen (`EvidenceLevel`: „gut belegt“ / „in Studien geprüft“ / „kaum untersucht“), im Detail als Chip, mit Quellen (DOI/PMID), Begriff „Studienlage“ im Glossar erklärt (`06_glossar`), Overclaims per `ContentRulesTest` verboten. **Keine Konkurrenz-App kennzeichnet Evidenz pro Übung.** Breathwrk hat eine Web-Seite mit ~25 PubMed-Links ohne Stärkegrad; Prana Breath verspricht Quellen pro Praxis, aber kein Etikett.
- **Sicherheitshinweise je Übung + Dialog vor intensiven Übungen** (`SafetyDialog`, `cautions`, Notfall-Hinweis bei Lippenbremse/„Kurzatmig“). Othership und Breathwrk führen Wim Hof/Breath of Fire ohne Kontraindikationen.
- **Yoga-Lehre:** Seite „Atemlehre“ (vier Phasen mit Sanskrit-Namen, fünf vāyus mit Körperfigur), „Pranayama“ (Grundlagen, Techniken mit Übung zum Ausprobieren, „Lehre, keine Studienlage“ sauber getrennt), 20 Übungen inkl. Ujjayi, Dirgha, Viloma I/II, Nadi Shodhana, Bhramari, Sitali, Kapalabhati. Prana Breath hat mehr Techniken (27 Pranayamas), aber als Timer mit Text, ohne Lehre-Seite; Insight Timer nur über Lehrer-Kurse.
- **Muster-Editor mit Atemkurve** (`14_muster_editor_*`): Punkte ziehen, Phasen einfügen (+Ein/+Halten/+Aus), Stufen, Vorschau mit Ton, Ablaufliste, Dauer. Prana Breath rechnet mit Verhältnissen (abstrakt), alle anderen mit Zahlenfeldern. Die Kurve ist die einzige *visuelle* Muster-Erstellung im Sample.
- **Muster des Tages** (deterministisch, Leitplanken, speicherbar) – kein Gegenstück.
- **Keine Cloud, kein Konto, kein Tracking, quelloffen**, Stimmen offline nach einmaligem Laden – teilt nur Breathly, das dafür weder Log noch Lehre noch Editor hat.

Kleine Abzüge (ohne Notenwirkung): „Programme“ (SKY, Buteyko, Nasenatmung) sind reine Leseseiten mit Chip „Programm“ – der Satz „kein getakteter Ablauf in der App“ steht erst im Detail; Detailtexte > 700 Zeichen (Backlog S-21) sind für die Masse lang, aber hinter Expandern.

### (f) Konsistenz und Ruhe der Oberfläche – **4 / 5**

Nach 2.7.0: weiße Abschnittsköpfe, Farbe nur bei Gruppenlabels und Kartenrändern, eine gefüllte Neon-Fläche je Screen (Starten), Suchpille statt Karte, Tab-Untertitel weg, Tab Eigene mit drei gleichen Köpfen und Aktion rechts. Das Ergebnis ist ruhiger als Prana Breath (Symbolik, Klänge) und sachlicher als Breathwrk. Styleguide mit automatischen Prüfungen (dark-only, neon-fill, no-fab-toast, cd-style) sichert das ab – das hat keine Konkurrenz-App dokumentiert.

Abzüge:
- **Drei Speicher-Metaphern:** Lesezeichen = Tagesmuster speichern (Kartenecke), Atemkreis-Ring = „Tut mir gut“ (Detail-Kopf, Karten), Tab „Eigene“ trägt *wieder* ein Lesezeichen, „+“ = neues Muster. Beide Markierungen landen im selben Tab.
- Zwei Chip-Größen auf einem Screen (große Anlass-Chips, kleine Filter-Chips). Als Hierarchie gewollt, liest sich für Ungeübte als Inkonsistenz.
- Tab-Leisten-Label klein (Backlog S-37, ~10 sp am A54).
- Titelzeile des Editors „Eigenes Muster…“ wird abgeschnitten (`14_muster_editor_auswahl`, Laufschrift-Titel seit 1.7.1 – ok, aber im Standbild unschön).

### (g) Barrierefreiheit und Einstellungen – **4 / 5**

Einstellungen: Lautstärke (3 Stufen), Gong-Nachhall mit Vorhören, Ton-Modus (Aus/Töne/Dauerton), Betonung, Vibration, „Danach in die Stille“, Sicherheitshinweis immer zeigen, Auto-Ausblenden, Fortschrittsring, Sekunden, Nächste Phase, Zeit im Kreis, Gong-Abstand, Stimmen-Katalog mit Lizenz, Export/Import, Logbuch leeren zweistufig. Das ist feiner als jede Konkurrenz außer Prana Breath und ohne deren Überladung. TalkBack: Rollen, Live-Regions, `paneTitle`, Überschriften, 48-dp-Ziele, Kontrast ≥ 4,5 geprüft, Material-Schriftskalierung ohne eigene Skalierung, Animator-Skala 0 berücksichtigt. Keine Konkurrenz-Quelle thematisiert Barrierefreiheit überhaupt (Breathly wird dafür kritisiert).

Abzüge: nur dunkel (bewusst), nur Deutsch (Play en-US-Text sagt es offen), kein Hell-Modus für helle Umgebungen tagsüber; Gerätecheck TalkBack laut Backlog noch offen (S-06 „am Gerät noch R-A11Y“).

### (h) Store-Erstwirkung – **3 / 5**

Texte sind gut: Titel 23 Zeichen, Kurzbeschreibung als Nutzen, Langtext mit „Was Atemkraft anders macht“, Datenschutz, Disclaimer. Icon (drei Ringe) und Feature-Grafik sind sauber, aber generisch. **Die sechs Screenshots sind rohe Screens ohne Bildunterschrift** (`StoreScreenshotTest` schreibt Vollbild). Alle Konkurrenten im Sample (Breathwrk, Calm, Headspace, Prana Breath, Othership) nutzen Geräte-Rahmen plus Nutzensatz je Bild; die Play-Suche zeigt die ersten zwei bis drei Bilder ohne Text daneben. Reihenfolge ist richtig (Sitzung, Atmen mit Anlass, Detail mit „gut belegt“, Meditation, Editor, Lernen). Kleine Unschärfe: Der Langtext sagt „Am Ende fragt die App, wie du dich fühlst“ – tatsächlich ist es ein Satz ohne Eingabe (`session_done_prompt`).

## Vergleichstabelle Atemkraft vs. Hauptkonkurrenten

| Dimension | **Atemkraft 2.7.0** | Prana Breath | Breathwrk | Breathly | Insight Timer |
|---|---|---|---|---|---|
| Erststart | Willkommenskarte, sofort nutzbar | sofort nutzbar | 14 Schritte, Paywall vor Konto | sofort nutzbar | Registrierung |
| Auswahl | 6 Anlässe + Befindens-Suche + 4 Bereiche + Filter | 80+ Praktiken, 7 Sammlungen | nach Ziel, Klassen | Kacheln | Bibliothek, Lehrer |
| Session | Kreis + Bogen + Nächste Phase, Töne/Dauerton/Vibration, Ruhe-Ansicht, Mini-Leiste, Bildschirm aus | Kreis, Ton je Phase, 34 Klänge, Bildschirm aus | Visualizer, Stimme, Haptik, Soundscapes | Text, Glocke/Stimme, Bildschirm an | Timer, Glocken, Ambient |
| Stimme in Atem-Session | **nein** (nur Meditation) | ja (Guru) | ja | ja | – |
| Ende | ruhiger Satz, Bilanz, Logbuch-Link | Log, Level | Zusammenfassung, Zitate | nichts | Statistik |
| Verlauf/Serie | Logbuch, Wochenpunkte, Tage in Folge | Statistik, Level, Tests | Streaks, Levels | nichts | Streak abschaltbar |
| Erinnerung | **nein** | ja | ja | nein | ja |
| Eigene Muster | Kurven-Editor, Stufen, Vorschau | Verhältnis-Editor, dynamisch | nein | Custom (Zahlen) | nein |
| Evidenz pro Übung | **3-stufiges Etikett + DOI/PMID** | Quellen ohne Etikett | Web-Seite, kein Grad | nein | nein |
| Sicherheit | Hinweise je Übung + Dialog | „what can go wrong“ | nein | nein | nein |
| Lehre | Atemlehre, Pranayama, Glossar, Programme | Erklärtexte | Science-Seite | nein | Lehrer-Kurse |
| Daten | kein Konto, keine Cloud, kein Tracking, FOSS | kein Konto, nicht FOSS | Tracking | keine Daten, FOSS | Tracking |
| Preis | gratis | Einmalkauf | Abo | gratis | Abo-Kern |
| Hell/Dunkel, Sprachen | nur dunkel, nur Deutsch | beides, 30+ Sprachen | dunkel | beides | beides |

## Priorisierte Empfehlungen (höchstens 20)

Aufwand S < 1 h · M ≤ 1 Tag · L > 1 Tag. Fundstellen relativ zum Repo.

| # | Prio | Aufwand | Empfehlung | Fundstelle / Begründung |
|---|---|---|---|---|
| 1 | HOCH | S | Store-Screenshots mit Bildunterschrift je Bild („Ein Kreis atmet mit dir“, „Zu jeder Übung die Studienlage“, „Kein Konto, keine Cloud“, „Eigene Muster an der Kurve formen“), optional Geräterahmen. | `StoreScreenshotTest`, `fastlane/…/phoneScreenshots/1–6.png`; alle Konkurrenten nutzen Nutzensätze; Play zeigt die ersten Bilder ohne Kontext |
| 2 | HOCH | M | Optionale **lokale** Erinnerung: Uhrzeit, Wochentage, Standard aus; AlarmManager/WorkManager + lokale Benachrichtigung, keine Netzverbindung. Im Play-Formular ergänzen. | Einstellungen (`SettingsScreen.kt`), neue Karte „Erinnerung“; einziges Gewohnheits-Werkzeug, das fast alle Konkurrenten haben und das ohne Cloud geht |
| 3 | HOCH | S | „Zuletzt geübt: Resonanz-Atmung ›“ unter „Verlauf“ im Tab Eigene (öffnet das Detail), ggf. zusätzlich oben im Atmen-Tab, sobald es Einträge gibt. | `OwnPatternsScreen.kt:105–117`, `LogHistory`; Wiederkommen ohne Suchen |
| 4 | HOCH | S | Erste Übung kurz: Willkommens-Link mit 3–5 min starten (Übergabe der Dauer) oder Text „stell die Dauer unten auf 3 min“. | `WelcomeCard.kt`, `welcome_body` („ein paar Minuten“) vs. Resonanz `defaultMinutes` 10 |
| 5 | MITTEL | M–L | Sprachansage der Phasen in Atem-Sessions („Einatmen … Halten … Ausatmen“, optional Sekundenzahl), mit den vorhandenen Stimmen; Schalter neben Ton/Vibration. | `playback/BreathSessionController.kt`, `MeditationAudioCoordinator` als Vorbild; Memory „CueAudioSpeechGuide auf Zuruf“; alle Werkzeug-Apps sprechen |
| 6 | MITTEL | S | Tagesmuster nicht als erste Karte unter „Übungen“: ans Ende der Familienliste oder als eigener kleiner Block unter „Beruhigen“; die erste Karte sollte eine kuratierte Übung sein. | `HomeScreen.kt:246–255`; Erstnutzer sieht sonst ein Muster ohne Zweck vor den Übungen |
| 7 | MITTEL | S | Speicher-Metaphern vereinheitlichen: „Tut mir gut“ und „Muster speichern“ beide als Lesezeichen (oder beide als Ring), Tab-Icon Eigene dann Person/Kreis statt Lesezeichen. | `ExerciseCards.kt`, `PatternCard`, `AppNavigationBar.kt`, `cd_personal_mark` |
| 8 | MITTEL | S | Bei gewähltem Anlass einen Textlink „Alle Übungen ›“ unter den Empfehlungen (neben „Mehr: Bereich ›“). | `HomeScreen.kt:203–231` (`AreaLink`); zweiter Chip-Tipp ist nicht sichtbar erklärt |
| 9 | MITTEL | S | Statische App-Shortcuts (Long-Press auf Icon): „Resonanz-Atmung“, „Meditation 10 min“, „Zuletzt geübt“. | `shortcuts.xml` + Deep-Link auf `Routes.kt`; billigster Schnellstart ohne Widget |
| 10 | MITTEL | S | Dauer im Detail nach oben ziehen oder im Start-Knopf als Split („Starten · 10 min ▾“) änderbar machen; Phasen-Anpassung bleibt unten. | `ExerciseDetailScreen.kt:313–385`; Dauer ist die Einstellung, die Anfänger brauchen |
| 11 | MITTEL | S | Tab-Label „Eigene“ prüfen: „Meins“ / „Für mich“ / „Du“ – oder „Eigene“ behalten und das Icon sprechender machen (Person). | `tab_*` in `strings.xml`, `AppNavigationBar.kt` |
| 12 | MITTEL | S | Backlog-Härten in der Session beheben: Dauerton-Ausblendung ≥ 60 ms (S-36), Skalensprünge (S-11). | `ContinuousTonePlayer.GAIN_FADE_SECONDS`, `MOTION-03` |
| 13 | MITTEL | M | Tab-Leiste: eigenes Label-Format oder größere Rolle (S-37), damit die vier Tab-Namen auf dem A54 nicht ~10 sp bleiben. | `AppNavigationBar.kt` |
| 14 | NIEDRIG | S | Store-Langtext: „Am Ende fragt die App, wie du dich fühlst“ → „Am Ende lädt sie ein, kurz nachzuspüren, und zeigt eine kurze Bilanz“. | `fastlane/…/de-DE/full_description.txt`, Abschnitt „Sitzung“ |
| 15 | NIEDRIG | S | Programme-Karten im Lernen-Tab: Chip „Programm“ → „zum Nachlesen“, damit klar ist, dass kein Start folgt. | `ProgramsScreen.kt`, `detail_info_only` |
| 16 | NIEDRIG | S | Ende-Screen nach der ersten Sitzung: einmalig ein Satz „Im Tab Eigene siehst du deinen Verlauf“ statt nur Logbuch-Link. | `FinishedPanel`, `SessionScreen.kt:119–127` |
| 17 | NIEDRIG | M | Icon schärfen: Die drei Ringe sind sauber, aber austauschbar (ähnliche Ring-Icons bei Timer-Apps). Eine atmende Kontur oder ein asymmetrischer Bogen wäre eigenständiger. | `fastlane/…/images/icon.png`, Launcher |
| 18 | MITTEL | M | Export auf eine Datei für alles ausweiten: Logbuch, Einstellungen, eigene Kreationen (lokal per SAF, wie heute die Muster). Vorbild Prana Breath „Data tab“, ohne dessen Google-Drive-Teil. | `settings/PatternTransfer.kt`, `data/PatternBackup.kt`; heute sichert der Export nur Muster, das Logbuch geht bei Deinstallation verloren (README „Installieren“) |
| 19 | NIEDRIG | L | Englische Lokalisierung der UI (Inhalte später): Play-Reichweite; en-US-Text sagt „German-only“. | `strings.xml`, `fastlane/…/en-US` |
| 20 | NIEDRIG | L | Helles Theme nur, wenn Store-Feedback es fordert; vorher nicht (PRIN-01, Abendnutzung, Palette). | `STYLEGUIDE.md` PRIN-01 |

## Drei Dinge bewusst NICHT übernehmen

1. **Onboarding-Fragebogen mit Zielwahl, Lungentest, Konto und Trial-Paywall** (Breathwrk, Calm, Headspace, Othership). Die Anlass-Chips auf dem ersten Screen beantworten dieselbe Frage ohne Wizard; der Lungentest erzeugt einen „Score“ und damit ein Leistungsversprechen, das die evidenz-ehrliche Haltung unterläuft. Breathwrks Play-Bewertung (ca. 3,7) leidet genau daran.
2. **Streak-Druck und Levels** (Breathwrk, Insight Timer, Prana Breath „Mastery“): Verlust-Warnungen, Meilenstein-Pop-ups, „als beendet markieren“ zum Retten der Serie. Die stille Zeile „2 Tage in Folge“ ist die Form, zu der Headspace (abschaltbar) und Insight Timer (wählbar, ausblendbar) gerade zurückrudern. Höchstens: Serie in den Einstellungen ausblendbar machen.
3. **Stimmungs-Emoji-Check-in nach jeder Sitzung** (Calm) und Soundscape-/Musik-Bibliotheken (Breathwrk, Othership, Prana Breath). Der Check-in erzeugt Gesundheitsdaten, die die App bewusst nicht erhebt, und verlängert das Ende; Musik verschiebt die App Richtung Content-Abo und widerspricht „Ruhe“ (PRIN-02). Der Satz „Spür kurz nach, wie es dir geht“ ist die richtige Antwort.

## Was an Atemkraft einzigartig ist (geprüft)

- Evidenz-Etikett in drei Stufen **pro Übung**, mit DOI/PMID und erklärtem Begriff „Studienlage“ – in keiner der zwölf Apps.
- Sicherheitshinweise je Übung plus bestätigter Dialog vor intensiven Übungen – sonst nur offizielle Wim-Hof-App und Prana Breath („what can go wrong“).
- Yoga-Lehre als eigene Seiten (Phasen mit Sanskrit, fünf vāyus mit Figur, Pranayama-Grundlagen), sauber von der Studienlage getrennt – Prana Breath hat mehr Techniken, aber keine Lehre-Seite; Insight Timer nur Kurse.
- Muster-Editor an der **Atemkurve** mit Stufen und Ton-Vorschau – alle anderen Editoren sind Zahlenfelder oder Verhältnisse.
- Befindens-Suche in Alltagswörtern, offline, ohne Speicherung.
- Mini-Leiste (Sitzung minimieren und weiter stöbern), Ruhe-Ansicht, „Danach in die Stille“.
- Kein Konto, keine Cloud, kein Tracking, keine Werbung, quelloffen, reproduzierbarer Build, dieselbe Signatur in allen Kanälen – nur Breathly ist ähnlich frei, ohne Log, Lehre, Editor.

## Vertiefung: AtemFlow und Prana Breath – und die Idee „Befinden vorher/nachher + Atem-Animation“

### AtemFlow (AtemFlow GbR, Freiberg; `atemflow.studio`)

**Zahlen (Play, abgerufen 2026-10-24):** 4,7 ★ bei 309–326 Rezensionen, 50.000+ Downloads, aktualisiert 15.10.2026; App Store 4,9 ★ (140). Abo 19 €/Monat oder 150 €/Jahr nach 14 Tagen Test; „Atemraum“ und einige Übungen dauerhaft gratis. Konto nötig (App-Zugangsdaten gelten auch am Computer, Apple-Login; Fortschritt „auf allen Geräten“).

**Wie sie „wie der Atem wirkt“ erklärt:** nicht in der App als Animation, sondern (1) auf der Website in vier Sätzen – längerer Ausatem → Vagus → Puls sinkt; ruhigeres Atmen → CO₂-Toleranz → Sauerstoffabgabe; ~6 Atemzüge/min → Resonanz von Herz und Atem; Buteyko → reduzierte Nasenatmung –, (2) im „Journal“ (35+ Artikel „mit Quellenangaben“) und (3) im „FlowCast“ (Fünf-Minuten-Antworten auf häufige Fragen, als Audio/Video von der Trainerin). Die Übungen selbst sind Video/Audio mit Stimme; eine „Atemwelle“ zeigt Ein-, Halte- und Aus-Phasen. Keine Physiologie-Animation belegt.

**Tagebuch / Befindens-Verlauf:** Der Store-Text nennt „Befindlichkeits-Check-in und Kontrollpausenmessung, beides freiwillig“ und ein „persönliches Dashboard“. Der Versionsverlauf zeigt den Aufbau: KP-Wert-Kurve und Atemzeitkurve „mit Werten der letzten 14 Tage“ (1.1), „Redesign des Dashboards“ (1.3), KP „All-Time, Trendberechnung“ (1.9.0), „dein Verlauf mit Mandala-Reise“ und „überarbeiteter Atemcheck“ (3.0.0), „eigene Mandalas“ als Visualisierung im Atemraum (3.4.11). Der „Atemcheck“ fragt, wie es dir *gerade* geht, und empfiehlt dazu Übungen – funktional dasselbe wie Atemkrafts Chips „Wie fühlst du dich?“. **Skala, Zeitpunkt (vor/nach Übung oder täglich) und die genaue Darstellung des Befindens-Verlaufs sind öffentlich nicht dokumentiert** (weder Store, Website, Versionsverlauf noch Rezensionen). Belegt ist nur: Werte werden gespeichert, als Kurve/Trend gezeigt (für die Kontrollpause) und als Mandala-Reise visualisiert.

**Datenschutz:** Text: „Alles, was du misst, dein Befinden und deine Kontrollpause, wird verschlüsselt auf deinem eigenen Gerät gespeichert. Wir können es nicht einsehen, Google kann es nicht einsehen.“ (AES, Schlüssel nur in der App). Gleichzeitig: Konto, Server in Frankfurt, Play-Datensicherheit „erhebt personenbezogene Daten, Finanzdaten und 5 weitere; gibt personenbezogene Daten, Finanzdaten und 2 weitere an Dritte weiter“; Apple-Label: Kaufhistorie, E-Mail, Name, Nutzer-ID, Nutzungsdaten für Analyse/Personalisierung. Also: **Gesundheitswerte lokal, Konto und Nutzung in der Cloud** – ein Hybrid.

**Was zu Atemkraft passt:** der Grundsatz „Befinden nur lokal, niemand wertet es aus“ (Atemkraft erfüllt ihn strenger: ohne Konto); „Dein Anker“ (eine Übung fest auf der Startseite) entspricht Empfehlung 3/9; der Atemcheck ist bereits da; vier Schriftgrößen/Hellmodus deckt Atemkraft über die Systemskalierung ab. **Was nicht passt:** Kontrollpausen-Messung mit Trend (Buteyko-KP ist ein Gesundheitsindex → SEC-LEGAL-02), Konto/Server, Abo, Mandala-Belohnungsreise (Gamification), Trainerin-Stimme als Produktkern.

### Prana Breath (Oleksandr Albul/Olekdia; `com.abdula.pranabreath`)

**Zahlen (Play, 2026-10-24):** 4,8 ★ bei 95.500 Rezensionen, 5 Mio.+ Downloads, Play Pass, Datensicherheit „Keine Daten erhoben, keine Daten geteilt“, Exodus: keine Tracker. Gratis-Kern (Grundlagen, Muster-Editor, Erinnerungen, Statistik), Guru als Abo oder Einmalkauf. Store-Text wörtlich: „Zu jeder Übung gibt es eine echte Erklärung: was sie bewirkt, wie Sie sitzen, was schiefgehen kann und woher sie kommt, mit nachprüfbaren Quellen.“

**Was sie stark macht (Wiki, pranabreath.info):**
- *Muster-Engineering:* Jedes Muster ist ein **Verhältnis** Ein : Halten : Aus : Innehalten (z. B. 1 : 4 : 2 : 0) mal „Sekunden pro Einheit“; Guru erlaubt Zehntel-Indizes, Millisekunden und je Phase „konstante Zeit“ statt Verhältnis. Daraus ergibt sich automatisch „Atemzüge/min“ als Komplexitätsmaß und „Level“, die mit dem Üben steigen. **Dynamic training:** Phasendauern ändern sich von Zyklus zu Zyklus (leicht beginnen, steigern, Muster kombinieren).
- *Statistik:* Log-Tab (jede Sitzung mit Details, Notiz, min/avg/max bei Dynamik), Progress-Tab (Atemzüge/min, Trainingszeit, Zyklen – Tag/Woche/Monat als Balkenchart), Experience-Tab (Serien, „Meisterschaftsstufen“), Health-Tab (Shtange-, Genchi-, Buteyko-Test, Puls, periphere Durchblutung – selbst gemessen, als Kurve). CSV-Export.
- *Visualisierung:* Animation folgt den Phasen; Ton „steigt beim Einatmen und fällt beim Ausatmen“ (wie Atemkrafts Dauerton); bei Schlafmustern „atmet der Bildschirm mit: heller beim Einatmen, dunkler beim Ausatmen“ – für Augen-zu im Dunkeln.
- *Datenschutz/Backup:* kein Konto, offline; **Data tab**: Backup-Datei `prana_breath_backup_<Datum>.db` (Muster, Statistik, Erinnerungen, Einstellungen) auf SD-Karte – Google-Drive-Backup und -Sync nur Guru; Import einzelner Trainings (`.trng`) und Sound-Styles; Export eines Trainings als Datei oder Link zum Teilen.

**Was überladen ist (eigene Rezensionen auf Play):** „das Design außerhalb der Übungen wirkt auf mich unübersichtlich und kleinteilig. Den Dynamischen Modus verstehe ich nicht“ (31 Hilfreich-Stimmen); zur aktuellen Version: „Die Benutzeroberfläche ist weniger strukturiert.“ Externe Reviews: „UX wirkt veraltet“, „setzt voraus, dass man schon weiß, welche Zeiten man braucht“, spirituelle Symbolik stört säkulare Nutzer. Zehn Tabs (Training, Control, Dynamic, Experience, Log, Progress, Health, Reminders, Motivators, Settings, Sounds, Data) sind das Gegenteil von Atemkrafts vier.

**Was zu Atemkraft passt:** die ehrliche Kommunikation („Kostenlos, und ehrlich beim Rest“, „was schiefgehen kann“ – Atemkraft hat das als Etikett + Dialog bereits schärfer); **Backup als eine Datei inkl. Logbuch und Einstellungen, lokal** (Empfehlung 18); der mitatmende Bildschirm für Schlafmuster (Helligkeit statt Grafik, passt zu „Ruhe-Ansicht“; NIEDRIG/S); nüchterne Statistik als *Fakten* (Zeit, Atemzüge, Atemzüge/min) ohne Bewertung – Atemkraft zeigt Minuten und Atemzüge schon, „Atemzüge/min“ wäre ein ehrlicher Zusatz in der Bilanz. **Was nicht passt:** Verhältnis-Editor (die Atemkurve ist für Laien das bessere Modell; Prana Breaths eigene Nutzer verstehen den Dynamik-Modus nicht), Level/Meisterschaft, Gesundheitstests (Messung mit Gesundheitsbewertung → SEC-LEGAL-02), Google-Drive-Sync, Klangbibliothek, Motivationssprüche in Erinnerungen.

### Einschätzung der Nutzeridee

**Idee:** Befinden vorher/nachher mit einem Tipp erfassen, lokal als Verlauf zeigen; auf der Übungsseite mit einer Animation zeigen, was mit dem Atem passiert (Physiologie evidenz-ehrlich + Yoga-Modell der vāyus).

**Chancen**
- Vorher/nachher mit *einem* Tipp ist ehrlicher als Calms Emoji-Check-in (nur „nachher“, absolut) und als AtemFlows Dashboard (Skala unbekannt): Die Differenz ist die Information, die ein Nutzer wirklich will („Hat mir das jetzt geholfen?“), und sie bleibt subjektiv, ohne Messgerät.
- Es passt zur bestehenden Ende-Seite: „Spür kurz nach, wie es dir geht.“ ist schon die Einladung; ein Tipp darunter ist die natürliche Fortsetzung, keine neue Seite.
- Lokal, ohne Konto: Atemkraft kann es strenger halten als AtemFlow (die Konto und Server braucht) – und genau damit werben („nur du siehst deinen Verlauf“ – AtemFlow sagt den Satz, Atemkraft kann ihn ohne Einschränkung sagen).
- Die Animation füllt eine Lücke, die *keine* App im Sample füllt: Alle erklären Wirkung als Text (Breathwrk-Webseite, Prana-Erklärtexte, AtemFlow-Journal/FlowCast); eine ruhige, physiologisch korrekte Bewegung (Zwerchfell senkt sich, Lunge füllt sich, Herzschlag wird beim Ausatmen langsamer – respiratorische Sinusarrhythmie, Lehrbuchwissen) ist einzigartig und passt zum Kreis. Die vāyus hat Atemkraft als Figur bereits (`ui/atemlehre/VayuFigure.kt`, tippbar).

**Risiken**
- *Health-Claims / MDR (SEC-LEGAL-02):* Ein roher Verlauf („Mo ruhiger, Di gleich“) ist ein Tagebuch, keine Auswertung. Die Grenze wird überschritten, sobald die App **bewertet oder empfiehlt**: „Resonanz-Atmung hebt dein Befinden um +1,3“, „Dir hilft X am besten“, Trendpfeile, Durchschnitte je Übung, Ampelfarben, Wochen-„Score“. Das wäre eine Auswertungsfunktion mit gesundheitlicher Bewertung und zugleich eine Wirkaussage, die der Evidenz-Haltung (TEXT-07/09) widerspricht. MDR-Regel 11 greift nach meinem Verständnis erst bei einer *medizinischen* Zweckbestimmung (Information für diagnostische/therapeutische Entscheidungen, MDCG 2019-11); ein Wellness-Tagebuch ohne Interpretation liegt davor – aber jede Aggregation rückt es näher heran. Keine Rechtsberatung; vor Umsetzung in SECURITY.md als Entscheidung festhalten.
- *Datenschutz:* Befinden ist ein sensibles Datum. Lokal in Room ist in Ordnung (wie das Logbuch, vom Cloud-Backup ausgeschlossen, SEC-STORE-02); Export muss es bewusst einschließen oder auslassen; PRIVACY.md und Play-Datensicherheit bleiben „keine Daten erhoben“, weil nichts das Gerät verlässt. In Screenshots/Testdaten keine echten Werte.
- *Reibung:* Eine Pflichtabfrage **vor** dem Start verlängert den Weg zur ersten Übung (Dimension b, heute 4/5) und widerspricht „Icon-only, ruhig“. Zwei Abfragen je Sitzung sind zu viel.
- *Ehrlichkeit der Animation:* Sie darf Mechanismen zeigen (Zwerchfell, Lunge, Herzrate), nicht Wirkungen („Stress verlässt den Körper“, „Energie steigt“). Das Yoga-Modell muss sichtbar als Lehre markiert sein (wie heute „Das ist Lehre, keine Studienlage“), sonst vermischt die Animation beides. Bewegung unterliegt MOTION-Regeln (Animator-Skala 0 → statisch).
- *Aufwand/Streuung:* Eine Animation je Übung ist L und driftet; eine parametrierte Animation (Phasen, Dauer) für alle Übungen ist M.

**Minimale Umsetzung (Vorschlag)**
1. **Nur „nachher“, relativ, ein Tipp, überspringbar.** Auf der Ende-Seite unter „Spür kurz nach, wie es dir geht.“ drei gleich große, stille Wahlflächen: „ruhiger · wie vorher · unruhiger“ (Wörter statt Emojis; bei aktivierenden Übungen „wacher · wie vorher · müder“, aus `exercise.activating`). Kein Pflichtfeld: „Ende“/„Nochmal“ bleiben jederzeit tippbar, keine Erinnerung, kein Nachfragen. Die relative Antwort erfasst „vorher/nachher“ in einem Tipp – ohne Abfrage vor dem Start. Speicherort: Feld `feeling` am `SessionLogEntry` (Room-Migration, `exportSchema`), Enum per `name`, Standard `null` = nicht beantwortet. Aufwand S–M.
2. **Verlauf nur zeigen, nie rechnen.** Im Logbuch je Eintrag das Wort in `TERTIARY`-Grau („ruhiger“), im Wochenpunkt kein eigener Code (keine Ampel). Keine Summen, Durchschnitte, Trends, Pfeile, Empfehlungen, keine Sortierung „wirkt am besten“. Optionaler Schalter in den Einstellungen „Befinden am Ende abfragen“ (Standard an, weil ein Tipp; oder aus und beim ersten Ende einmalig angeboten – Nutzerentscheidung). Ein Satz in PRIVACY.md und `settings_data_intro`. Aufwand S.
3. **Animation „Ein Atemzug“ als eine parametrierte Komponente**, nicht je Übung: Torso-Silhouette (wie `VayuFigure`), Zwerchfell senkt sich/hebt sich, Lungenfläche füllt sich mit der Phase, daneben eine Herzschlag-Linie, deren Abstände beim Ausatmen weiter werden; Beschriftung in TEXT-07-Ton („Beim Ausatmen schlägt das Herz etwas langsamer – das ist normal und messbar“), Umschalter „Lehre: vāyus“ färbt dieselbe Figur mit prāṇa (ein) / apāna (aus) / samāna (Mitte) und blendet „Lehre, keine Studienlage“ ein. Ort: Seite „Atemlehre“ als erster Abschnitt „Ein Atemzug“ (statisch zwei Zustände, tippbar Ein/Aus) – Aufwand M; später optional auf der Detailseite unter „Wirkung“ als 3-Sekunden-Loop und in der Session hinter dem „Anleitung“-Expander – L. Nicht: Animation je Übung, „Stress fließt ab“-Bilder, Partikel, Farbverläufe als Wirkung.
4. **Reihenfolge:** erst 1+2 (klein, messbar nützlich, keine neue Seite), Gerätecheck mit TalkBack (drei Wahlflächen als RadioGroup, Live-Region für „gespeichert“), dann 3 auf der Atemlehre-Seite. Vorher in SECURITY.md unter SEC-LEGAL-02 festhalten: „Befinden = Selbstauskunft ohne Auswertung; keine Aggregation, keine Empfehlung“ – damit die Grenze dokumentiert ist, bevor jemand den naheliegenden Trend-Chart baut.

## Quellen

- AtemFlow: https://play.google.com/store/apps/details?id=atemflow.studio&hl=de · https://apps.apple.com/de/app/atemflow-studio/id6467029838 (Beschreibung, Versionsverlauf, Datenschutz-Label) · https://atemflow.de/ · https://atemflow.de/studio/ · https://atemflow.de/atemflow/
- Prana Breath (Play, Wiki): https://play.google.com/store/apps/details?id=com.abdula.pranabreath&hl=de · https://pranabreath.info/wiki/index.php?title=Guru_version · https://pranabreath.info/wiki/index.php?title=Data_tab · https://pranabreath.info/wiki/index.php?title=Progress_tab · https://pranabreath.info/wiki/index.php?title=Log_tab · https://pranabreath.info/wiki/index.php?title=Ratio_of_breath_cycle · https://reports.exodus-privacy.eu.org/en/reports/91060/
- Prana Breath: https://www.apkmirror.com/apk/oleksandr-albul/prana-breath-calm-meditate/ · https://www.casey-douglass.com/2019/07/app-review-prana-breath.html · https://onda-life.com/reviews/prana-breath · https://pranabreath.info/Guru_version · https://apps.apple.com/jp/app/id6478402874
- Breathwrk: https://screensdesign.com/showcase/breathwrk-breathing-exercises · https://apps.apple.com/us/app/breathwrk-breathing-exercises/id1481804500 · https://www.breathwrk.com/science · https://kimola.com/reports/unlock-insights-breathwrk-app-user-feedback-report-google-play-154030 · https://www.choosingtherapy.com/?p=18275
- Apple Mindfulness: https://support.apple.com/guide/watch/apd371dfe3d7/watchos · https://en.wikipedia.org/wiki/Mindfulness_(Apple)
- Calm: https://goodux.appcues.com/blog/calm-app-new-user-experience · https://screensdesign.com/showcase/calm · https://support.calm.com/hc/en-us/articles/9699990936731-How-to-Use-Check-Ins · https://mhealth.jmir.org/2021/4/e27106/ · https://www.choosingtherapy.com/calm-app-review/ · https://www.appbrain.com/app/com.calm.android
- Headspace: https://goodux.appcues.com/blog/headspaces-mindful-onboarding-sequence · https://help.headspace.com/hc/en-us/articles/215730567-How-does-the-run-streak-feature-work · https://builtformars.com/ux-bites/hiding-your-headspace-streaks · https://choosingtherapy.com/headspace-review
- Insight Timer: https://apps.apple.com/us/app/insight-timer-meditation-timer/id337472899 · https://www.appbrain.com/app/com.spotlightsix.zentimerlite2 · https://manassaloi.com/2020/05/15/meditation-app-streak.html · https://insighttimer.com/meditation-courses/journey-to-the-breath
- Othership: https://apps.apple.com/us/app/othership-guided-breathwork/id1590348936 · https://screensdesign.com/showcase/othership-guided-breathwork · https://adapty.io/paywall-library/othership/
- iBreathe: https://apps.apple.com/us/app/ibreathe-deep-breathing-exercises/id1296605806 · https://www.applevis.com/comment/146049
- Oak: https://tidbits.com/2022/09/23/meditate-for-free-with-the-oak-app/ · https://apps.apple.com/us/app/oak-meditation-breathing/id1210209691
- Paced Breathing: https://healthify.nz/paced-breathing-app · https://www.appbrain.com/app/com.apps.paced.breathing
- Breathly: https://github.com/mmazzarolo/breathly-app · https://f-droid.org/en/packages/com.mmazzarolo.breathly/ · https://apps.apple.com/us/app/breathly/id1454852966
- Breathe: relax & focus: https://apps.apple.com/us/app/-/id1506712199 · https://similarweb.com/app/google/com.havabee.breathe
- Sicherheit: https://wimhofmethod.com/safety-warnings · https://www.outsideonline.com/outdoor-adventure/exploration-survival/wim-hof-california-lawsuit · https://github.com/Koenkk/whb
- Atemkraft: `docs/STYLEGUIDE.md`, `docs/STORE.md`, `fastlane/metadata/android/de-DE/*`, `app/src/main/java/app/atemkraft/ui/**`, `app/src/main/res/values/strings.xml`, Store-Screenshots 1–6 (v2.7.0), Roborazzi-Bilder (Stand vor 2.7.0, nur unveränderte Screens).
