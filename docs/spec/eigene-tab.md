# Spezifikation: Tab „Eigene“ und Logbuch

Regeln: [STYLEGUIDE MUSTER-03](../STYLEGUIDE.md#muster) (Ausklappen und Übersicht), MUSTER-08 (Tabs und Zurück), TEXT-16 (zählt, bewertet nicht); Anatomie der Köpfe und Karten in [§4.5](../STYLEGUIDE.md#anatomie). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/own/OwnPatternsScreen.kt`, `ui/log/`.

## Drei gleiche Köpfe (`SectionHeader`, weiß)

1. **„Verlauf“** mit Chevron – die ganze Kopfzeile ist der Button zur Logbuch-Seite (`onClick`, `onClickLabel` „Logbuch öffnen“). Darunter **ohne Karte** die Wochenpunkte wie im Logbuch (`LogHistory` aus `ui/log/`, einzige erlaubte Feature-Abhängigkeit, CODE-01) und die Zeile „3 Tage in Folge · 45 min diese Woche“ – eine nüchterne Zählung ohne Ziel, Soll oder Urteil (TEXT-16); mit „Serie zeigen“ aus (Einstellungen › Daten) ohne die Tage in Folge, die Minuten bleiben; ohne Sitzung nur der Satz „Noch keine Sitzung …“. Darunter, wenn es einen Logbuch-Eintrag gibt, dessen Übung noch existiert (eingebaute Übung oder vorhandenes gespeichertes Muster; nicht Meditation, Tagesmuster oder Entwurf – dieselbe Regel wie im Logbuch), der Verweis **„Zuletzt: <Name> ›“** zur Detailseite der neuesten solchen Übung (`ForwardLinkText` im bündigen `AppTextButton` in `secondary`). Ein `HintText` darunter sagt, wo man das Logbuch leert (Einstellungen › Daten).
2. **„Tut mir gut · 4“** mit Caret: die persönlich markierten eingebauten Übungen als `ExerciseCard(marked = true)`; ohne Markierung erscheint der Abschnitt nicht.
3. **„Eigene Kreationen · 5“** mit Caret und „+“ rechts („Neues Muster“, `AppIconButton` in `onSurface` als `trailingAction` neben dem Caret – zwei getrennte Bedienelemente): gespeicherte Tagesmuster und eigene Muster als `SavedPatternCard` (→ `PatternCard(actionCount = 2)`: Stift „Muster <Name> bearbeiten“ und `ConfirmIconButton` „Muster <Name> löschen“, beide `onSurface` · `SECONDARY`, auf der Titelzeile; die Karte wird nicht höher, der Tipp auf die Karte öffnet die Detailseite). Ohne eigene Kreationen bleibt derselbe Kopf mit „+“ stehen, darunter eine ruhige Karte (`Surface` `surfaceContainerLow`, `shapes.medium`, `CardPadding`) mit dem Satz `own_empty` und dem Verweis „Muster gestalten ›“ (`ForwardLinkText` im bündigen `AppTextButton` in `primary`, Rolle Button, Tippfläche 48 dp) – kein großer Knopf.

Beide Abschnitte sind `CollapsibleSection`s mit der Anzahl als Status im Kopf (`statusDescription` in Worten: „4 Übungen“), einklappbar erst, wenn beide gefüllt sind – ein einzelner steht offen ohne Caret. Beide starten offen; die Wahl gilt per `rememberSaveable` nur für die Sitzung.

Nicht mehr: ein Abschnitt „Logbuch“ mit Kennzahlen, letzten Sitzungen und „Alle Sitzungen ›“ (eine gekürzte Kopie der Logbuch-Seite); ein Suchfeld kommt nur als Zusatz zur Übersicht und nur nach MUSTER-09.

## Logbuch (`LogbookRoute`, `LogbookScreen`)

- Push-Seite (`TabListScaffold` mit `PushHeader`, `top = SectionGap`), erreichbar über den Kopf „Verlauf“ und über „Im Logbuch ansehen ›“ am Ende jeder Übung (Atem-Session und Meditation; beendet wie „Ende“); gehört zum Stapel des Tabs, aus dem es geöffnet wurde. Kein Tab-Kopf trägt einen Kalender.
- Einträge nach Tagen und Monaten gruppiert (`CollapsibleSection` ohne Caret für den Abstand Kopf → Inhalt); ein Eintrag ist ein Fokus-Stopp, Dauer sichtbar „6:05 min“ (`durationClockText`), gesprochen „6 Minuten 5 Sekunden“ (`spokenDuration`); Familie über Randfarbe und Tagespunkt (`WeekDot` 18, Heute-Ring `SynthTrack`).
- Ein Eintrag, dessen Übung es noch gibt (eingebaute Übung, vorhandenes gespeichertes Muster; nicht Meditation, Tagesmuster oder Entwurf), öffnet ihre Detailseite (`clickable` mit `Role.Button`, `onClickLabel` „Übung öffnen“) – „das von gestern Abend nochmal“.
- Die Serie („N Tage in Folge“, erst ab zwei Tagen) fehlt mit „Serie zeigen“ aus, die Minuten bleiben (DataStore `show_streak`, Standard an).
- Was ins Logbuch kommt, steht in [session.md](session.md) („Logbuch und Ersetzen“). „Logbuch leeren“ gibt es nur in den Einstellungen › Daten (Papierkorb der Zeile „Logbuch“, zweistufig, MUSTER-05); die Karte „Daten“ zeigt nur die Zahl der Sitzungen, keine Navigationszeile.

## Prüfung

`ScreenshotTest` (`13_eigene`, `13_eigene_kreationen`, `13_eigene_leer`, `04_logbuch`), `TabNavigationTest`, Gate ANATOMIE (Köpfe mit Caret und „+“, Kreationen-Karten), manuell R-A11Y.
