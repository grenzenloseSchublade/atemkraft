# Spezifikation: Detailseite einer Übung

Regeln: Anatomie in [STYLEGUIDE §4.5](../STYLEGUIDE.md#anatomie) (Knopfleiste unten, Bottom Sheet), MUSTER-11 (Bottom Sheet), MUSTER-10 (Phasen eigener Muster nur im Editor). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/detail/`.

Gerüst: `PushScaffold` – Kopf mit Name und Markierung bzw. Stift fest, Inhalt scrollt, unten bei getakteten Übungen die gepinnte Start-Leiste (`PinnedActionBar`).

## Kopf und Rhythmus (oben)

Unter dem Kopf: Familie mit Chips, Kurzbeschreibung, Dauer- und Charakter-Chip, bei intensiven Übungen die ⚠-Zeile; darunter die Expander (Anleitung, Wirkung, Aus der Yoga-Lehre, Hinweise, Quellen).

## Anpassen und Start (unten)

Gepinnt in der `PinnedActionBar`, nur bei getakteten Übungen: oben die Zeile „Anpassen ›“, darunter der `StartSplitButton` („Starten · ca. 10 min“, Reset nach einer Anpassung). Die Anpassung hat genau einen Ort, das Blatt; am Ende des scrollenden Inhalts steht keine Karte mehr. Programme ohne getakteten Ablauf (`guided = false`) haben weder Leiste noch Blatt, nur den Hinweis am Ende des Inhalts.

**Zeile „Anpassen ›“** (`AdjustRow`): `SectionHeader` mit `onClick` wie „Klang & Stimme“ in der Meditation ([meditation.md](meditation.md)) – Titel `titleLarge`, darunter als Teaser die aktuelle Dauer bzw. Runden („10 min“, „4 Runden“), nach einer Anpassung mit „· angepasst“ (`SessionAdjustState.modified`), rechts `ForwardChevron`. Die ganze Zeile ist der Button (≥ 48 dp); TalkBack liest sie als eine Schaltfläche „Anpassen, 6 Runden, angepasst“ mit der Aktion „Anpassen öffnen“. Keine eigene Live-Region: Den neuen Wert sagt der Stepper im Blatt an, das Zurücksetzen der Start-Knopf (Fokus auf Start). Sie bleibt `titleLarge`, obwohl sie in der Leiste steht: Jeder Kopf, der ein Blatt öffnet, hat diese eine Kopf-Art („Klang & Stimme“, „Muster des Tages ›“); `titleMedium` spart nur 4 dp (Zeile 24 statt 28), die Höhe machen Polster und Teaser.

**Blatt „Anpassen“** (`AdjustSheet`, MUSTER-11): Gerüst wie `SoundSheet` – ganz geöffnet, eigener stummer Griff, Titel als `SectionHeader` mit `top = 0`, Kopf → Inhalt `HeaderGap`, TalkBack beginnt beim Titel, der Inhalt scrollt bei großer Schrift. Darin die `SessionAdjustCard`: Dauer- bzw. Runden-Stepper, bei eingebauten Übungen und beim Muster des Tages dahinter „Phasen anpassen“ mit den Phasen-Steppern; gespeicherte und eigene Muster nur die Dauer (Phasen ändert der Stift im Editor, MUSTER-10). Jede Änderung gilt sofort und wird wie bisher gespeichert (`persistAdjustments`), ein Bestätigen gibt es nicht. Schließen per Wischen, Tipp daneben oder Zurück; offen überlebt das Blatt Drehen (`rememberSaveable`, wie der Aufklapper der Phasen). Der Reset bleibt am `StartSplitButton`.

**Platz:** Der feste Rahmen wächst nur um die Zeile – bei Schrift 1,0 um 76 dp, bei 2,0 um rund 110 dp. Quer bei 360 dp Höhe und Schrift 2,0 bleiben dem Inhalt zwischen Kopf und Leiste knapp 120 dp (A54 quer, 384 dp: rund 140 dp); er scrollt, abgeschnitten wird nichts.

**Prüfung:** `ScreenshotTest` – `08_detail_*` (jede Seite, 4-7-8 mit gespeicherter Anpassung), `08_detail_quer` (4-7-8 quer, 360 dp hoch, Schrift 2,0) und `08_detail_anpassen_offen` (Blatt offen mit Phasen, ganzer Bildschirm, Schrift 1,0 und 2,0; die Zeile ist eine Schaltfläche mit „Anpassen öffnen“); Gates `WORTBRUCH`, `ABGESCHNITTEN`, `TIPPFLAECHE`, `BESCHNITTEN`, `ANATOMIE`. Manuell R-A11Y: Fokus beim Öffnen auf dem Titel, Zurück schließt.
