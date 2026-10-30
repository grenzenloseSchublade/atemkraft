# Spezifikation: Einstellungen

Regeln: [STYLEGUIDE TEXT-15](../STYLEGUIDE.md#text) (Texte nach einem Muster), LAYOUT-07 (Steuerung beim Label); Anatomie „Zeile mit Steuerelement“ in [§4.5](../STYLEGUIDE.md#anatomie). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/settings/` (`SettingsScreen`, `SettingsColumns.kt`, `PatternTransfer.kt`).

## Texte nach einem Muster (TEXT-15)

- **Karte:** Titel (`CardTitle`, `onSurface`), direkt darunter höchstens ein Satz, wofür die Karte gilt (`SettingsCard(intro)`), nur wo er etwas sagt, das die Zeilen nicht sagen („Gilt für Atemübungen und Meditation.“).
- **Schalter** (`ToggleRow`): Name sagt, was geschaltet wird, als Substantiv oder kurze Wendung („Vibration“, „Nächste Phase“, „Sicherheitshinweis immer zeigen“); darunter höchstens ein erklärender Satz zur Wirkung – was sich ändert, wann es nützt, bei Bedarf „Aus: …“ –, kein Hinweis, wenn der Name alles sagt (Fortschrittsring der Meditation).
- **Auswahlreihe** (`SegmentedChoiceRow`): Unterüberschrift (`SubLabel`) darüber, darunter ein `HintText` zur gewählten Option (Ton) oder zur Wahl allgemein; er wiederholt nicht die Segmentbeschriftungen.
- **Aktionen** (Export/Import): Unterüberschrift darüber, Satz darunter.
- Langes, Quellen, Rechtliches nur im Aufklapper (`DisclosureToggle`), dessen Text nicht mit der Frage des Umschalters beginnt.
- **Nichts doppelt:** Was zwei Karten teilen, steht einmal erklärt, die zweite verweist („Wie bei der Atemübung.“); kein Hinweis wiederholt Titel, Kartensatz oder Beschriftung. Begriffe nach §11.1, Orte als Tab „Meditation“, Startwerte als solche benennen („Danach in die Stille“: nur der Startwert, in der Übung oben umschaltbar).

## Zeilen und Endspalten (LAYOUT-07)

- Ein Schalter steht in der Zeile seines Namens, der Hinweis darunter in der Textspalte: Er endet vor dem Schalter, statt unter ihn zu laufen (`ToggleRow`: Hinweis mit `padding(end = SwitchWidth + Abstand)`). Switch in M3-Größe, sichtbar bündig; die ganze Zeile `toggleable(Role.Switch)`, ≥ 48 dp.
- **Feste Endspalten** (`TrailingRow` in `SettingsColumns.kt`): Zeilen mit Vorhören und Aktionen haben rechts drei Fächer – Vorhören (48 dp), Zustand (Breite gemessen an der breitesten Beschriftung „Laden“/„Wählen“/„✓ Aktiv“ samt Text-Button-Polster, `rememberWidestTextWidth`) und Icon-Spalte (`flushEnd(inCard = true)`). Leere Fächer behalten ihre Breite: Alle Play-Knöpfe der Stimmenliste stehen untereinander, ebenso Löschen und Abbrechen der Stimmen und der Papierkorb der Zeile „Logbuch“. Passt der Text daneben nicht ohne Wortbruch, stehen die Fächer unter dem Text.
- Das Gong-Vorhören („Gong-Nachhall“) steht in der Reihe der Auswahl: Kurz/Lang belegen zwei Drittel, das Play mittig im freien Drittel – alle Segmente der Karten „Klang“ und „Atemübung“ sind ein Drittel breit. Es hält Audio-Fokus wie eine Session (`playback/GongPreview`, AUDIO-02), weitere Taps während des Gongs zählen nicht.
- Stepper (`Stepper`): Label links mit `weight(1f)`, Knöpfe als rechte Spalte, der sichtbare Kreis von „+“ bündig an der Inhaltskante (Polster rechts um `CircleOverhang` kleiner).

## Aufbau und Wege

- Atemlehre und Glossar stehen seit 2026-10-19 nicht mehr in den Einstellungen (Tab „Lernen“); „Über Atemkraft“ als `NavCard`.
- Karte „Erinnerung“ (vor „Daten“): Schalter „Ans Atmen erinnern“ (`ToggleRow`, aus ab Werk); an: `Stepper` „Uhrzeit“ in Viertelstunden (sichtbar „08:15“, gesprochen „8:15 Uhr“, ohne eigenes Polster in der Karte) und „Tage“ als `FilterChipFlow` aus `TonalChip`s „Mo“ … „So“ (gesprochen ganz, der letzte gewählte Tag bleibt). Beim Einschalten fragt Android ab 13 einmal nach der Benachrichtigung; abgelehnt oder in Android aus, sagt der Hinweis das statt der Beschreibung. Planung `playback/ReminderScheduler` (JobScheduler, Fenster 15 min, nichts nach drei Stunden Verspätung), Ausführung `ReminderJobService`, neu geplant bei jedem App-Start und jeder Änderung (`AppContainer`).
- Karte „Daten“: Unterüberschrift „Sicherung“, Exportieren · Importieren (`AppOutlinedButton` in `AdaptiveButtonRow`; Datei `atemkraft-sicherung-<datum>.json` mit Kreationen, Logbuch samt Befinden, „Tut mir gut“ und Einstellungen ohne Gerätespezifisches, `PatternTransferService`). Nach dem Wählen einer Datei erst eine Vorschau in der Karte („In der Datei“: Teile mit „ · “, Satz zum Zusammenführen, bei Einstellungen der Schalter „Einstellungen ersetzen“, Standard aus), dann „Übernehmen“ · „Abbrechen“ (gleichrangig); erst „Übernehmen“ schreibt, Erfolg als Label von „Importieren“ (MUSTER-02). Die Vorschau lebt nur im Speicher des Bildschirms (nach dem Drehen neu wählen); „Beim Handywechsel?“ als `DisclosureToggle`, Zeile „Logbuch“ mit der Zahl der Sitzungen und dem Papierkorb (zweistufig, MUSTER-05), darunter die Schalter „Serie zeigen“ (`ToggleRow`, Standard an; aus: Verlauf und Logbuch nennen keine Tage in Folge) und „Befinden am Ende fragen“ (an, solange die Reihe noch nie angeboten wurde; [session.md](session.md), „Befinden am Ende“).
- Stimmen-Katalog im `DisclosureToggle` „Stimmen zeigen“ (gesperrt während eines Downloads); Fehler inline mit nächstem Schritt (`VoiceFailure` NETWORK/STORAGE/DAMAGED/OTHER, MUSTER-04); der Katalog rät vorab zum WLAN.
- Ansage und Fokus der Stimmen-Zeile (`VoiceRow`): Der Untertitel ist die eine Live-Region der Zeile und sagt jeden Zustandswechsel einmal an – Start („lädt …“), Entpacken, „geladen“, Fehler, Abbrechen –, ohne Prozentwerte (den Fortschritt liest TalkBack am Kreis). Verschwinden Laden, Abbrechen oder Löschen unter dem Fokus (Tastatur, D-Pad, laufender Bedienungshilfe-Dienst), geht er auf den Knopf, der an ihre Stelle tritt: Abbrechen beim Laden, Laden nach Abbrechen, Löschen oder Fehler (A11Y-04, A11Y-07).
- `SettingsRoute(focusMeditation, showVoices)` öffnet die Seite an der Meditations-Karte mit aufgeklapptem Katalog („Stimme laden ›“ aus dem Bottom Sheet der Meditation).

## Prüfung

`ScreenshotTest` (`05_einstellungen_daten` mit allen längsten Varianten, `05_einstellungen_erinnerung` mit Uhrzeit und Tagen; Gates samt ANATOMIE für `ToggleRow` und `Stepper`), manuell R-TEXT, R-A11Y (Download-Status, Fokus beim Löschen).
