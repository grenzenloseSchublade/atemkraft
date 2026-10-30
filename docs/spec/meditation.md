# Spezifikation: Tab „Meditation“ (Zeitring und Klang & Stimme)

Regeln: [STYLEGUIDE LAYOUT-08](../STYLEGUIDE.md#layout) (ohne Scrollen), MUSTER-11 (Bottom Sheet); die laufende Meditation steht in [session.md](session.md). Entscheidungen in [DECISIONS.md](../DECISIONS.md). Code: `ui/meditation/`.

## Seite

Eigenes Gerüst ohne Liste: `ScreenHeader`, darunter `FillViewportColumn` mit dem Zeitring (`DurationPicker` → `TimeDial`), der Zeile „Klang & Stimme“ und der `PinnedActionBar` mit dem `StartSplitButton`. Kopf, Zeitring, „Klang & Stimme“ und Start passen bis fontScale 1,3 auf jedem Gerät der Matrix in die Höhe über Tab-Leiste und Systemleisten – in jedem Zustand des Tabs; der Zeitring nimmt den Rest (Mindestkante `DurationPickerMin` 200; bei großer Schrift `timeDialMinSide`: so groß, dass Zahl und Einheit im kleinsten Stil innerhalb der Spur stehen). Nur bei fontScale 2,0 darf die Seite als Notlösung scrollen – abgeschnitten wird nie. Die Auswahl der Tab-Leiste ist tonal, der Zeitring bei ∞ Cyan statt Pink: Pink bleibt dem Start (PRIN-02).

## Zeitring (`TimeDial`)

- Dauer wie ein Küchenwecker: eine Runde = 60 min, Raster 1 min, 0 oben, im Uhrzeigersinn; Bogen `secondary` bis zum Knopf, ∞-Marke oben (Ring und Marke voll in `secondary`); Zifferblatt mit Minutenstrichen (`OutlineVariantIndigo`, dekorativ) und 5er-Zahlen, die Zahl unter dem Knopf in `secondary`, dazwischen ein kleines Label am Knopf; der ∞-Knopf mit Rand `BorderThin` in `outline` (fest, wächst nicht mit dem Ring). Tipp auf die Zeit in der Mitte öffnet die Feinwahl (− / + je 1 min, `AppIconButton`); die Zeit steht im größten passenden Stil (`displayMedium` … Rückfall bis `titleLarge`, `fitNumberStyle`/`measureReadout`), die Einheit `labelMedium` darunter.
- Füllt das größte Quadrat im gegebenen Platz, ausgerichtet nach `alignment` (Standard mittig; der Meditations-Tab oben bündig unter dem Kopf); der Tab gibt ihm den ganzen Platz zwischen Kopf und der Zeile „Klang & Stimme“ – der Ring behält so immer seine volle Größe.
- Ziehen rastet an 5er-Marken magnetisch ein (`snapMinutes`: ab ±0,8 min, Lösen ab ±1,3 min, `crossesMajor`), im Uhrzeigersinn über 12 Uhr hinaus in ∞, zurück wieder auf 60; gegen den Uhrzeigersinn hält es an der 1 (`DialDrag` gegen den Sprung über 12 Uhr). Ein Tipp aufs Band setzt den gerundeten Wert (ohne Fang). Rasten über `onTick(major)` → `HapticPlayer.dialTick` (Berührungs-Vibration ohne App-Schalter, AUDIO-05).
- TalkBack: Schieberegler (`progressBarRangeInfo` + `setProgress`, `Modifier.durationSlider`), `stateDescription` „12 Minuten“ / „offene Sitzung“; ∞-Marke als Schalter (`Modifier.freeMark`, `Role.Switch`, „Offene Sitzung“, „An“/„Aus“), Mitte („Feinwahl“) mit Namen und Klick.
- Nicht dafür: Werte über 60 min; Dauer in Karten (→ `Stepper`).

## Logik (`DurationPickerState`, `DurationScale.kt`, `DurationReadout.kt`)

`DurationPickerState`: `minutes` 1..60 (geklemmt, alte 90 → 60; Grenzen `MEDITATION_MIN/MAX_MINUTES` in `domain`), `free` (∞), `fineAdjust`; `select`/`selectFree`/`step`/`toggleFineAdjust`, saveable (`rememberDurationPickerState`). Darstellungen ändern ihn nur über `selectWithTick`/`stepWithTick`/`selectFreeWithTick` (Raste-Regel: jede Änderung, deutlich bei erreichter oder überfahrener 5er-Marke und bei ∞). Umrechnung als reine Funktionen in `DurationScale.kt` (`DialScale` samt `exactMinutesAt`, `DialDrag`, `snapMinutes`; für die nicht ausgelieferte Variante F `RulerScale`, Vorrat Stand v1.6.5). Geteilte Teile in `DurationReadout.kt`: `DurationReadout`, Texte, TalkBack-Regler, ∞-Marke. Variante F (wischbare Skala) wäre eine neue Composable `TimeRuler(state, onTick, modifier)` mit derselben Signatur wie `TimeDial`; getauscht wird nur der Aufruf in `DurationPicker`.

## „Klang & Stimme“ (MUSTER-11, `SoundSheet`)

Eine Zeile als `SectionHeader` mit `onClick`: Titel, darunter die aktuelle Wahl als Teaser („Gong Start & Ende · ohne Stimme“), rechts `ForwardChevron`; die ganze Zeile ist der Button (≥ 48 dp), TalkBack liest sie als eine Schaltfläche mit Kommas statt Punkten. Sie öffnet das Blatt „Klang & Stimme“ über `AppBottomSheet` (Anatomie „Bottom Sheet“, STYLEGUIDE §4.5). Schließen per Wischen, Tipp daneben oder System-Zurück; der Offen-Zustand überlebt Drehen (`rememberSaveable`). Sofort wirksam, kein Bestätigen; die Seite ändert sich dabei nicht, der Zeitring behält seine Größe.

Inhalt: Gong (Aus / Start & Ende / „alle 5 min“), bei „alle … min“ direkt darunter der Gong-Abstand (3/5/10/15 min, `SegmentedChoiceRow`, app-weit gespeichert – nur hier, die Einstellungen führen ihn nicht), dann „Sprachanleitung“ (`ToggleRow`); kann das Gerät kein Deutsch sprechen, steht unter dem Hinweis „Stimme laden ›“ (`ForwardLinkText` in `secondary`), das erst das Blatt schließt (`closeThen`) und dann die Einstellungen an der Meditations-Karte mit aufgeklapptem Stimmen-Katalog öffnet (`SettingsRoute(focusMeditation = true, showVoices = true)`) – kein ausgegrauter Schalter als Sackgasse.

## Prüfung

`ScreenshotTest` (`03_meditation_fein` mit `SCROLLT` bei 1,0 und `RING`; `03_meditation_klang_offen` ganzer Bildschirm samt Blatt), `DeviceFitTest` (`03_meditation`: 360 × 640 bei 1,3, A54 bei 1,1 und 1,3; Kante des Rings als `RING`), manuell R-A11Y (Fokus beim Öffnen auf dem Titel, Zurück schließt), R-VISUAL (andere Geräte).
