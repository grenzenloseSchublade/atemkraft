# Spezifikation: Detailseite einer Übung

Regeln: Anatomie in [STYLEGUIDE §4.5](../STYLEGUIDE.md#anatomie) (Knopfleiste unten, Bottom Sheet), MUSTER-11 (Bottom Sheet), MUSTER-10 (Phasen eigener Muster nur im Editor). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/detail/`.

Gerüst: `PushScaffold` – Kopf mit Name und Markierung bzw. Stift fest, Inhalt scrollt, unten bei getakteten Übungen die gepinnte Start-Leiste (`PinnedActionBar`).

## Kopf und Rhythmus (oben)

Unter dem Kopf: Familie mit Chips, Kurzbeschreibung, die Rhythmuskurve, Dauer- und Charakter-Chip, bei intensiven Übungen die ⚠-Zeile; darunter die Expander (Anleitung, Wirkung, Aus der Yoga-Lehre, Hinweise, Quellen).

**Rhythmuskurve** (`RhythmCurve`, Anatomie in [STYLEGUIDE §4.5](../STYLEGUIDE.md#anatomie)): ein Atemzug der Übung als dünne Linie über die volle Breite (x = Zeit, y = Lungenfüllung), Farben wie die Kurve des Muster-Editors – Einatmen und Nachatmen `secondary`, Ausatmen `BreathExhale`, Halten gestrichelt in der Farbe seiner Höhe. Darunter die Phasen als Zeile („Einatmen 4 s · Halten voll 7 s · Ausatmen 8 s“), beim Muster des Tages seine Musterzeile (`PatternTexts.cardLineFor`). Sie steht nach der Kurzbeschreibung und vor Dauer und Charakter, in derselben Reihenfolge wie die Metazeile der Karte (Takt · Dauer).

- **Daten:** `Exercise.breathRhythm()` aus der angepassten Übung (`exercise.adjusted(adjust.config())`): Ändert jemand die Phasen unter „Phasen anpassen“, folgt die Kurve. Füllstände stetig wie beim Atemkreis (`CircleFractions`): Doppel-Einatmen (Seufzer) steigt erst bis 0,8, dann bis voll; Stufen (Viloma, Dirgha) zeigen ihre Stufen aus `Phase.level`. Gleiche Phasen hintereinander stehen in der Zeile einmal mit Anzahl („3 × Einatmen 2 s“).
- **Sonderfälle:** Nadi Shodhana zeigt eine Hälfte (ein, aus), die Seiten nennt die Anleitung. Wim Hof zeigt nur die Atemfolge, mit der jede Runde beginnt, und sagt das („Zu Beginn jeder Runde: …“), auch bei einer gewählten Runde; das Halten bis zum Atemreiz hat keine Dauer und erscheint nicht. Keine Kurve bei Programmen ohne Takt, bei offenen Phasen im ersten Teil, bei Phasen unter 1 s (Feueratmung) und bei freiem Atmen.
- **Bewegung:** Ein kleiner Punkt (`RhythmCurveDot` 8) läuft im echten Takt der Übung endlos über die Kurve, ohne Ton (Eigentümer 2026-10-30: „Punkt läuft endlos!“; Ausnahme in MOTION-02): Er zeigt den Takt, den man mitatmen kann. Er trägt die Farbe der Phase, auf der er gerade läuft – Einatmen `secondary`, Ausatmen `BreathExhale`, Halten in der Farbe seiner Höhe –, aus derselben Funktion wie die Linie (`stepColor`); an jeder Grenze blendet sie über `Motion.RhythmDotColor` (300 ms, je zur Hälfte davor und danach) weich in die nächste. Kein Pulsieren, keine Fläche. Die Größe bleibt 8 dp: Zum 2-dp-Strich steht der Punkt 4 : 1, deutlicher als der Vorschaupunkt des Editors (14 zu 4), und liest sich als Perle auf der eigenen Linie auch ohne Weiß. Eine Anpassung der Phasen ergibt einen neuen Rhythmus und startet ihn neu. Bei „Animationen entfernen“ (Animator-Skala 0) fehlt er ganz. Unter einer Session oder Meditation im Vollbild (`overlayOpen`; die Seite bleibt darunter komponiert) fehlt er ebenfalls und seine Bildschleife steht, sonst hielte er die ganze Meditation lang die volle Bildrate; danach beginnt er neu.
- **TalkBack:** Die Kurve ist ein Knoten „Atemkurve: Einatmen 4 Sekunden, Halten voll 7 Sekunden, Ausatmen 8 Sekunden“ (Wim Hof: „Atemkurve zu Beginn jeder Runde: …“), ohne Live-Region; die Zeile darunter ist für TalkBack unsichtbar.

## Anpassen und Start (unten)

Gepinnt in der `PinnedActionBar`, nur bei getakteten Übungen: oben die Zeile „Anpassen · 10 min ›“, darunter der `StartSplitButton` („Starten · ca. 10 min“, Reset nach einer Anpassung). Die Anpassung hat genau einen Ort, das Blatt; am Ende des scrollenden Inhalts steht keine Karte mehr. Programme ohne getakteten Ablauf (`guided = false`) haben weder Leiste noch Blatt, nur den Hinweis am Ende des Inhalts.

**Zeile „Anpassen ›“** (`AdjustRow`): `SectionHeader` mit `onClick` wie „Klang & Stimme“ in der Meditation ([meditation.md](meditation.md)), seit 2026-10-30 möglichst einzeilig (Eigentümer: quer und bei großer Schrift mehr Inhalt): Titel `titleLarge` mit dem Status dahinter (`status`, klein über `smallStatus` in `labelMedium` wie „Ablauf · 4 von 12 Phasen“), „Anpassen · 10 min“ bzw. „Anpassen · 4 Runden“, nach einer Anpassung „Anpassen · 6 Runden · angepasst“ (`SessionAdjustState.modified`), rechts `ForwardChevron`. Klein passt der Status hoch bei 360 dp und Schrift 1,0 auch angepasst dahinter (in `titleLarge` rutschte „· 6 Runden · angepasst“ schon dort darunter); bei großer Schrift rutscht er laut `SectionHeader` darunter (ohne führenden Punkt), dann nicht höher als der frühere Teaser. Die ganze Zeile ist der Button (≥ 48 dp); TalkBack liest sie als eine Schaltfläche „Anpassen, 6 Runden, angepasst“ bzw. „Anpassen, 10 Minuten“ (Minuten ausgeschrieben über `statusDescription`, A11Y-06) mit der Aktion „Anpassen öffnen“. Keine eigene Live-Region: Den neuen Wert sagt der Stepper im Blatt an, das Zurücksetzen der Start-Knopf (Fokus auf Start). Sie bleibt `titleLarge`, obwohl sie in der Leiste steht: Jeder Kopf, der ein Blatt öffnet, hat diese eine Kopf-Art („Klang & Stimme“, „Muster des Tages ›“), und einzeilig spart `titleMedium` bei Schrift 1,0 nichts – die Zeile steht ohnehin auf der Mindest-Tippfläche 48 dp (Zeile 28 + 2 × 8 Polster = 44 < 48); erst bei großer Schrift wären es wenige dp. Klein ist nur der Status.

**Blatt „Anpassen“** (`AdjustSheet`, MUSTER-11): Gerüst wie `SoundSheet` – ganz geöffnet, eigener stummer Griff, Titel als `SectionHeader` mit `top = 0`, Kopf → Inhalt `HeaderGap`, TalkBack beginnt beim Titel, der Inhalt scrollt bei großer Schrift. Darin die `SessionAdjustCard`: Dauer- bzw. Runden-Stepper, bei eingebauten Übungen und beim Muster des Tages dahinter „Phasen anpassen“ mit den Phasen-Steppern; gespeicherte und eigene Muster nur die Dauer (Phasen ändert der Stift im Editor, MUSTER-10). Jede Änderung gilt sofort und wird wie bisher gespeichert (`persistAdjustments`), ein Bestätigen gibt es nicht. Schließen per Wischen, Tipp daneben oder Zurück; offen überlebt das Blatt Drehen (`rememberSaveable`, wie der Aufklapper der Phasen). Der Reset bleibt am `StartSplitButton`.

**Platz:** Der feste Rahmen wächst nur um die Zeile – einzeilig bei Schrift 1,0 um 56 dp (vorher mit Teaser 76 dp). Quer bleibt dem scrollenden Inhalt zwischen Kopf und Leiste (`SICHT` der Szene `08_detail_quer`, gemessen im App-Rahmen: Statusleiste 24 dp, 3-Tasten-Navigation unten 48 dp, ungünstiger Fall):

| Quer | Schrift | vorher (Titel + Teaser) | nachher (eine Zeile) |
|---|---|---|---|
| 360 dp hoch | 1,0 | 84 dp | 104 dp |
| 360 dp hoch | 2,0 | 46 dp | 86 dp |
| A54 (384 dp) | 1,0 | 108 dp | 128 dp |
| A54 (384 dp) | 1,1 | 105 dp | 128 dp |
| A54 (384 dp) | 2,0 | 70 dp | 110 dp |

Bei 2,0 fast doppelt so viel wie vorher, gut zwei Textzeilen statt einer. Abgeschnitten wird nichts, der Inhalt scrollt. Die Szene prüft im Test nur 360 dp bei 2,0; die übrigen Werte sind einmalig mit erweiterter Matrix gemessen (2026-10-30), noch mit dem Status in `titleLarge`. Der kleine Status ändert quer nichts: Bei 360 dp und 2,0 bleiben es 86 dp (nachgemessen); bei 1,0 passte er schon groß dahinter.

Hoch (360 × 640 im App-Rahmen ohne Tab-Leiste, gleiche Messung, einmalig 2026-10-30): „Status groß“ ist die erste einzeilige Fassung mit dem Status in `titleLarge`, „Status klein“ die heutige mit `smallStatus`. 4-7-8 angepasst ist der längste Status („· 6 Runden · angepasst“), Resonanz unangepasst der kurze („· 10 min“):

| Hoch, 360 dp | Schrift | vorher (Titel + Teaser) | Status groß | Status klein |
|---|---|---|---|---|
| 4-7-8 angepasst | 1,0 | 364 dp | 360 dp | 384 dp |
| 4-7-8 angepasst | 2,0 | 288 dp | 238 dp | 296 dp |
| Resonanz | 1,0 | 364 dp | 384 dp | 384 dp |
| Resonanz | 2,0 | 326 dp | 366 dp | 366 dp |

Groß rutschte der lange Status hoch schon bei 1,0 unter den Titel und brach bei 2,0 an den Punkten in weitere Zeilen in Titelgröße – 50 dp weniger als mit Teaser. Klein steht er bei 1,0 dahinter und kostet bei 2,0 darunter weniger als der Teaser.

**Prüfung:** `ScreenshotTest` – `08_detail_*` (jede Seite, 4-7-8 mit gespeicherter Anpassung), `08_detail_quer` (4-7-8 quer im App-Rahmen ohne Tab-Leiste, 360 dp hoch, Schrift 2,0; misst die sichtbare Höhe als `SICHT`) und `08_detail_anpassen_offen` (Blatt offen mit Phasen, ganzer Bildschirm, Schrift 1,0 und 2,0; die Zeile ist eine Schaltfläche „Anpassen, 6 Runden, angepasst“ mit „Anpassen öffnen“); Gates `WORTBRUCH`, `ABGESCHNITTEN`, `TIPPFLAECHE`, `BESCHNITTEN`, `ANATOMIE`. Manuell R-A11Y: Fokus beim Öffnen auf dem Titel, Zurück schließt.
