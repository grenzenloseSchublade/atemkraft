# Spezifikation: Detailseite einer Übung

Regeln: Anatomie in [STYLEGUIDE §4.5](../STYLEGUIDE.md#anatomie) (Knopfleiste unten, Bottom Sheet), MUSTER-11 (Bottom Sheet), MUSTER-10 (Phasen eigener Muster nur im Editor). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/detail/`.

Gerüst: `PushScaffold` – Kopf mit Name und Markierung bzw. Stift fest, Inhalt scrollt, unten bei getakteten Übungen die gepinnte Start-Leiste (`PinnedActionBar`).

## Kopf und Rhythmus (oben)

Unter dem Kopf: Familie mit Chips, Kurzbeschreibung, Dauer- und Charakter-Chip, bei intensiven Übungen die ⚠-Zeile; darunter die Expander (Anleitung, Wirkung, Aus der Yoga-Lehre, Hinweise, Quellen).

## Anpassen und Start (unten)

Gepinnt: `StartSplitButton` („Starten · ca. 10 min“, Reset nach einer Anpassung). Die Karte „Anpassen“ (`SessionAdjustCard`: Dauer, bei eingebauten Übungen und beim Muster des Tages auch Phasen hinter „Phasen anpassen“) steht am Ende des scrollenden Inhalts.
