# Auswertung der Recherchen und Reviews

Stand: 2026-10-24, App-Version 2.7.1 (lokal). Ausgewertet werden der große Review vom 2026-10-23 (sechs Dimensionen, umgesetzt in v2.6.0), die [Store-Recherche](2026-10-23-store.md), der [UX-Vergleich mit 13 Apps](2026-10-24-ux-vergleich.md), die [Inhalte-Recherche](2026-10-24-inhalte-geschichte.md) und der [Guardrails-Audit](2026-10-24-guardrails.md). Ziel: aus fünf Berichten eine Reihenfolge machen und die Entscheidungen benennen, die beim Eigentümer liegen.

## 1. Gesamtbild

Alle Berichte kommen zum selben Schluss, jeder aus seiner Richtung: **Die Substanz stimmt, die Lücken liegen am Rand.**

- Der UX-Vergleich gibt 3,9 von 5. Inhalte 5 (in keiner der 13 Apps gibt es Evidenz-Etiketten je Übung, Sicherheitsdialoge, getrennte Yoga-Lehre, einen Kurven-Editor). Schwach sind nur Wiederkommen (3) und Store-Wirkung (3).
- Die Inhalte-Recherche findet keine falschen Inhalte mehr, aber eine fehlende Ebene: Einordnung und Geschichte. Die App zitiert die Haṭhayogapradīpikā überall, sagt aber nie, was das für ein Text ist.
- Der Guardrails-Audit erklärt, warum Layoutfehler wiederkehren: Regeln als Chronik, keine Anatomie, keine Kurzfassung. Das wird gerade behoben.
- Die Store-Recherche zeigt einen gangbaren Weg für Play und F-Droid; IzzyOnDroid fällt weg.

Die Leitlinie, die alles verbindet, steht in [RECHERCHE.md](../RECHERCHE.md): **Atem ist keine Zahl.** Die App folgt dem Gefühl, rechnet im Hintergrund, und alle Daten bleiben auf dem Gerät. Jede Empfehlung unten ist daran gemessen.

## 2. Befunde nach Themen

### A. Bedienung und Oberfläche

Erledigt seit dem Review: Suchfeld, weiße Köpfe, Filter-Chips, Eigene-Tab, Editor-Zeile, Kreationen-Karten. Übrig bleiben kleine Dinge, von denen drei an bestehenden Entscheidungen rütteln (siehe Abschnitt 3):

| Punkt | Quelle | Aufwand | Einschätzung |
|---|---|---|---|
| Willkommenskarte verspricht „ein paar Minuten“, öffnet aber 10 min | UX #4 | S | klar, machen: 3 min übergeben |
| Bei gewähltem Anlass fehlt ein sichtbarer Weg „Alle Übungen ›“ | UX #8 | S | machen, neben „Mehr: … ›“ |
| Dauer sitzt im Detail unten; Start-Knopf als Split „Starten · 10 min ▾“ | UX #10 | S–M | prüfen; Split-Button ist Nutzergeschmack, aber die Dauer bleibt auch unten erreichbar |
| Tagesmuster als erste Karte unter „Übungen“ lässt Erstnutzer stutzen | UX #6 | S | Entscheidung (widerspricht Vorschlag B) |
| Drei Speicher-Zeichen: Lesezeichen, Ring ◉, Tab-Icon | UX #7 | S | Entscheidung (◉ soll bleiben) |
| Tab-Name „Eigene“ unvollständig | UX #11 | S | Entscheidung, Vorschlag: lassen, Icon sprechender |
| Tab-Labels am A54 nur etwa 10 sp | UX #13, S-37 | M | später, mit der Tab-Leiste |

### B. Wiederkommen

Die einzige Dimension, in der fast alle anderen Apps mehr haben, und alles davon geht ohne Cloud:

| Punkt | Quelle | Aufwand | Einschätzung |
|---|---|---|---|
| „Zuletzt geübt ›“ unter „Verlauf“ in Eigene | UX #3 | S | machen |
| Lokale Erinnerung: Uhrzeit, Wochentage, Standard aus, keine Netzverbindung | UX #2 | M | machen; einziges Gewohnheits-Werkzeug ohne Druck; Play-Formular ergänzen |
| Statische App-Shortcuts (Long-Press auf das Icon) | UX #9 | S | machen, billigster Schnellstart |
| Export als eine Datei: Logbuch, Einstellungen, Kreationen | UX #18 | M | machen; heute geht das Logbuch bei Deinstallation verloren |
| Serie ausblendbar | UX, „nicht übernehmen“ | S | machen, passt zu „keine Zahl“ |

### C. Session

| Punkt | Quelle | Aufwand | Einschätzung |
|---|---|---|---|
| Sprachansage der Phasen (Stimmen sind da) | UX #5 | M–L | lohnt, aber nach B und D; Augen-zu-Nutzung |
| Dauerton-Ausblendung 15 ms, Skalensprünge | S-36, S-11 | S | machen |
| Bildschirm atmet mit (Helligkeit) bei Schlafmustern | Prana Breath | S | später, nett |
| „Atemzüge/min“ in der Bilanz als Fakt | Prana Breath | S | nein: eine Zahl mehr auf der ersten Ebene |

### D. Befinden und „Was passiert“ (Idee des Eigentümers)

Der UX-Vergleich prüft die Idee gegen AtemFlow, Prana Breath und Calm und kommt zu einem Minimum, das ich übernehme:

1. **Nur „nachher“, relativ, ein Tipp, überspringbar:** unter „Spür kurz nach, wie es dir geht.“ drei stille Wahlflächen „ruhiger · wie vorher · unruhiger“ (bei aktivierenden Übungen „wacher · wie vorher · müder“). Keine Abfrage vor dem Start, kein Pflichtfeld. Feld am Logbuch-Eintrag, lokal.
2. **Zeigen, nie rechnen:** im Logbuch das Wort je Eintrag; keine Summen, Trends, Pfeile, Durchschnitte, Empfehlungen, Ampeln. Schalter in den Einstellungen. Satz in PRIVACY.md.
3. **Vorher festhalten** in SECURITY.md unter SEC-LEGAL-02: Befinden ist Selbstauskunft ohne Auswertung, keine Aggregation, keine Empfehlung. Damit ist die Grenze dokumentiert, bevor jemand den naheliegenden Trend-Chart baut.
4. **Animation „Ein Atemzug“** als eine parametrierte Komponente auf der Atemlehre-Seite: Zwerchfell, Lunge, Herzschlag-Abstände beim Ausatmen weiter; Umschalter „Lehre: vāyus“ färbt dieselbe Figur und blendet „Lehre, keine Studienlage“ ein. Später Detailseite und Session. Nicht: Animation je Übung, Wirkungsbilder, Partikel.

Warum das kein Widerspruch zu „keinen Emoji-Check-in übernehmen“ ist: Calm fragt absolut, mit Emojis, und wertet aus. Wir fragen relativ, mit Wörtern, lokal, und werten nicht aus. Die Differenz „vorher/nachher“ steckt in einem Tipp.

### E. Inhalte

| Punkt | Quelle | Aufwand | Einschätzung |
|---|---|---|---|
| Kapitel „Geschichte des Atmens“: Indien, Andere Wege, Der Westen, Heute (Top 8 der Recherche) | Inhalte | L (Text) | machen; erst Texte schreiben und freigeben, dann einbauen |
| Kapitel „Körper“: Atemmechanik, CO₂ als Atemantrieb, Vagus und Baroreflex, Atem und Gefühl | Inhalte P1/P2/P4/P6 | M (Text) | machen; die einzige echte Wissenslücke |
| Karte „Grenzen einer App, wann Fachleute“ | Inhalte E2 | S | machen, bündelt Verstreutes |
| Programme-Chip „Programm“ → „zum Nachlesen“ | UX #15 | S | machen |
| Kurzbeschreibungen der 17 Übungen kürzen; Detailtexte über 700 Zeichen | Review, S-21 | M (Text) | später, mit Freigabe |
| Store-Langtext: „fragt, wie du dich fühlst“ stimmt erst mit D | UX #14 | S | mit D anpassen |

### F. Store

| Punkt | Quelle | Aufwand | Einschätzung |
|---|---|---|---|
| Screenshots mit Bildunterschrift je Bild | UX #1 | S | machen, vor dem ersten Store-Upload |
| Icon eigenständiger (atmende Kontur statt drei Ringe) | UX #17 | M | Entscheidung, nicht dringend |
| Play-Konto, Signaturschlüssel vor dem ersten AAB, 14 Tage Test, F-Droid-Antrag | Store | Prozess | beim Eigentümer; Anleitung in STORE.md |
| Store-Titel, Kontakt-E-Mail, Keystore-Backups | Store | Entscheidung | beim Eigentümer |

### G. Guardrails

Läuft als eigenes Paket: CLAUDE.md, PROCESS.md, Styleguide mit Leitplanken und Anatomie der Bausteine, CardHeader, Gate ANATOMIE, neue Checks. Danach gilt: Jeder Baustein hat eine Anatomie, eine Komponente, eine Prüfung.

### H. Bewusst nicht

Onboarding-Wizard, Lungentest, Konto, Paywall; Streak-Druck, Levels; Emoji-Check-in mit Auswertung; Soundscape-Bibliotheken; Gesundheitstests mit Trend (Kontrollpause); Cloud-Sync; Verhältnis-Editor; Hell-Theme und englische Oberfläche erst, wenn der Store es verlangt.

## 3. Widersprüche, die eine Entscheidung brauchen

1. **Tagesmuster als erste Karte.** Vorschlag B setzt es als erste Karte unter „Übungen“; der UX-Vergleich sagt, ein Erstnutzer sieht so ein Muster ohne Zweck vor den kuratierten Übungen. Empfehlung: Tagesmuster hinter die erste Gruppe „Beruhigen“ oder ans Ende; die Kopfzeile in der Karte bleibt.
2. **Speicher-Zeichen.** Eigentümer: ◉ bleibt. UX: drei Zeichen für zwei Bedeutungen. Empfehlung: ◉ bleibt für „Tut mir gut“, das Lesezeichen am Tagesmuster wird zum selben ◉ (Speichern heißt „in Eigene“), das Tab-Icon „Eigene“ bekommt ein eigenes Zeichen (Person oder Kreis). Dann ist ◉ das eine Zeichen für „zu mir“.
3. **Befinden trotz „keine Zahl“.** Aufgelöst durch die Minimalform: Wörter, relativ, keine Auswertung.
4. **Tab-Name „Eigene“.** Empfehlung: behalten, Icon ändern (siehe 2).
5. **Dauer im Start-Knopf als Split.** Der Eigentümer mag Split-Buttons; das Detail hat die Dauer unten. Empfehlung: prüfen, ob „Starten · 10 min ▾“ auf 360 dp bei Schrift 1,3 trägt; sonst lassen.

## 4. Empfohlene Reihenfolge

| Release | Inhalt | Warum in dieser Reihenfolge |
|---|---|---|
| **2.8.0** | Guardrails-Paket; dann die kleinen Dinge aus A, B, C, E, F: Willkommenslink 3 min, „Alle Übungen ›“, „Zuletzt geübt ›“, App-Shortcuts, Serie ausblendbar, Programme-Chip, Session-Härten, Store-Screenshots mit Bildunterschrift, Entscheidungen 1, 2, 4 aus Abschnitt 3 | alles klein, hebt Wiederkommen und Store-Wirkung je einen Punkt; danach Push, Tag, GitHub-Release, Beginn des Store-Wegs |
| **2.9.0** | Befinden (D 1–3) mit SECURITY/PRIVACY; lokale Erinnerung; Export als eine Datei | die drei Funktionen, die „Datenschutz als Argument“ sichtbar machen |
| **2.10.0** | Kapitel „Geschichte des Atmens“ und „Körper“ (Texte zuerst, Freigabe, dann Einbau), Karte „Grenzen einer App“ | Text-Arbeit, kann parallel zu 2.9 vorbereitet werden |
| **3.0.0** | Animation „Ein Atemzug“ auf der Atemlehre-Seite; später Detail und Session; Sprachansage der Phasen | die beiden größeren Bauteile, nach allem Kleinen |
| parallel | Play-Konto, Signaturschlüssel, geschlossener Test, F-Droid-Antrag | dauert Wochen, hängt nicht am Code |
| später | Reihen mit Start, Tab-Leisten-Labels, Icon, Kurzbeschreibungen, Englisch | nach Rückmeldungen aus dem Test |

## 5. Entscheidungen beim Eigentümer

1. Tagesmuster: erste Karte lassen oder hinter „Beruhigen“ (Empfehlung: dahinter)?
2. Speicher-Zeichen: ◉ auch fürs Tagesmuster, Tab-Icon neu (Empfehlung: ja)?
3. Befinden am Ende: Standard an oder aus und beim ersten Ende einmal angeboten (Empfehlung: einmal angeboten)?
4. Lokale Erinnerung: in 2.9.0 (Empfehlung: ja)?
5. Store: Titel mit Zusatz, Kontakt-E-Mail anlegen, Play-Konto jetzt starten?
6. Icon neu zeichnen (Empfehlung: nicht vor dem ersten Test)?
7. Sprachansage der Phasen: in 3.0 (Empfehlung: ja, nach Befinden und Erinnerung)?
