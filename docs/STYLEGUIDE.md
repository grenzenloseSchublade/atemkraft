# Atemkraft – Styleguide

Normativer Design-, Text- und Code-Leitfaden für Maintainer und Claude: Jede Regel nennt ihre Prüfung. Stand: v2.9.0 und `main`, 2026-10-24. Sicherheit, Datenschutz, Build und Release regelt [SECURITY.md](SECURITY.md); Commit, Push, Release und Arbeitsweise [PROCESS.md](PROCESS.md); die Kurzfassung für Agenten steht in [CLAUDE.md](../CLAUDE.md). Wie ein Feature genau gebaut ist, steht in [docs/spec/](spec/); warum etwas so entschieden wurde, in [DECISIONS.md](DECISIONS.md).

**Inhalt:** [0 Geltung](#geltung) · [1 Leitplanken](#leitplanken) · [2 Farbe](#farbe) · [3 Typografie](#typografie) · [4 Layout](#layout) (4.5 [Anatomie der Bausteine](#anatomie)) · [5 Komponenten](#komponenten) · [6 Muster](#muster) · [7 Icons](#icons) · [8 Bewegung](#bewegung) · [9 Audio und Haptik](#audio) · [10 Barrierefreiheit](#a11y) · [11 Text und Ton](#text) · [12 Code](#code) · [13 Automatische Prüfung](#automatische-pruefung) · [14 Backlog](#backlog) · [Quellen](#quellen)

## <a id="geltung"></a>0 Geltung

- **Pfade:** `src/` = `app/src/main/java/app/atemkraft/`, `res/` = `app/src/main/res/`.
- **Regelformat:** `ID | Stufe | Regel | Warum | Prüfung`. Eine Regelzelle sagt in höchstens 400 Zeichen, *was gilt* (Doku-Lint `rule-length` ab 600 Zeichen); die Spalte Warum gibt den Grund in einem Halbsatz. Spezifikation gehört nach `docs/spec/` oder in die KDoc, Historie und Nutzerentscheidungen nach [DECISIONS.md](DECISIONS.md), Offenes in den [Backlog](#backlog).
- **IDs** sind stabil und werden nie neu vergeben; eine gestrichene oder verschobene Regel bleibt als „entfällt → Ziel“ stehen.
- **Prüfung:** `auto: <check>` verweist auf §13.3, `manuell: R-…` auf eine Checkliste in §13.4.

| Stufe | Bei Verstoß |
|---|---|
| **MUSS** / **DARF NICHT** | CI rot. Altlasten sind per Baseline mit S-ID eingefroren (§13.2). Marker `// Abweichung <ID>: <Grund>` nur, wo die Regel eine Ausnahme nennt. |
| **SOLL** | Abweichung nur mit Marker `// Abweichung <ID>: <Grund>` in derselben Zeile; ohne Marker warnt die Automatik. |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| META-01 | – | entfällt → PROZ-02 ([PROCESS.md](PROCESS.md)): Guide-Pflicht je Push, Baseline sinkt im selben Commit. | Prozessregel, keine Designregel. | auto: `guide-sync`, `baseline-stale` |

## <a id="leitplanken"></a>1 Leitplanken

Zwölf Sätze, die jeder Beitrag einhält; sie stehen wortgleich in [CLAUDE.md](../CLAUDE.md). Die Regeln dahinter stehen in Klammern.

1. **Nur dunkel, ruhig, minimal:** ein `darkColorScheme`, pro Screen eine gefüllte Neon-Fläche, Aktionen Icon-only mit `contentDescription`, nichts schwebt (kein FAB, Toast, Snackbar). (PRIN-01…03)
2. **Köpfe weiß, Farbe trägt Bedeutung:** Abschnitts-, Expander- und Kartentitel in `onSurface`; Farbe nur bei Gruppenlabel, Kartenrand, Familie, Studienlage, Warnung, Atemphase, eigenem Muster. (FARBE-04, -07)
3. **Kein Wort bricht um, nichts wird gekürzt, nichts beschnitten** – auf 360–412 dp bei fontScale 1,0–2,0; Tippflächen 48 dp als Layoutplatz. (LAYOUT-03, -05, A11Y-05)
4. **Tokens statt Literale, Komponenten statt Nachbau:** Farbe, Alpha, Schrift, Maß, Form, Dauer aus `ui/theme/`; M3-Steuerelemente nur über die Wrapper in `ui/components/`; jedes Layout folgt seiner Anatomie (§4.5); Auswahl ohne Häkchen. (FARBE-01/-02, TYPO-01, LAYOUT-01/-04, KOMP-01, A11Y-03)
5. **Material-Schrift und M3-Größen ohne eigenen Skalierfaktor.** (§3, §4.2)
6. **Barrierefrei nach WCAG 2.2 AA:** Name, Rolle, Zustand, Überschrift, `paneTitle`, eine Live-Region je Zustandswechsel. (A11Y-01…07)
7. **Evidenz-ehrlich, Laiensprache, „du“:** kein Heil-, Therapie- oder Präventionsversprechen in irgendeinem Text; Teaser ist ein ganzer Satz; jede Studie in `Refs.kt`. (TEXT-04…08, -12, SEC-LEGAL-01/-02)
8. **Die App zählt, sie bewertet nicht:** keine Ziele, kein Soll, keine Auswertung mit gesundheitlichem Urteil. (TEXT-16, SEC-LEGAL-02, -04)
9. **Offline und lokal:** kein Tracking, keine Cloud, kein Auto-Backup; Daten verlassen das Gerät nur per Export oder Gerät-zu-Gerät; einzige Netzverbindung der angetippte Stimm-Download. (SEC-PRIV-01…03, SEC-STORE-02)
10. **Lieferkette gepinnt, ein Schlüssel außerhalb des Repos:** nur FLOSS, SHA-Pins, Actions per SHA; Keystore und Passwörter liest kein Agent. (SEC-SUP-*, SEC-BUILD-02, SEC-LEGAL-03)
11. **Jede Regel hat eine ID und eine Prüfung;** Altlasten nur in der Baseline mit S-ID, Ausnahmen nur mit Marker. (§0, §13.2)
12. **Wenige, aussagekräftige Tests** für Daten, Sicherheit und die Gates – kein Test je Befund. (CODE-07)

## <a id="farbe"></a>2 Farbe

### 2.1 Tokens ([Color.kt](../app/src/main/java/app/atemkraft/ui/theme/Color.kt), [Theme.kt](../app/src/main/java/app/atemkraft/ui/theme/Theme.kt))

Kontrast (WCAG 2.x) der Token-Farbe als Vordergrund gegen `background` #160F2E und Card #2E2150, Alpha vorher mit dem realen Grund gemischt.

| Token | Hex | M3-Rolle | Bedeutung | vs BG | vs Card |
|---|---|---|---|---|---|
| `NeonMagenta` | #F25CA2 | primary | Marke, Aktion, Auswahl | 5,98 | 4,71 |
| `NeonCyan` | #34E0E8 | secondary | Verweise und Links im Inhalt (FARBE-04), Einatmen, eigene Muster (Rand, gespeichert, Eyebrow „MUSTER DES TAGES“), Meditation (Zeitring, Logbuch-Punkt), Fortschritt | 11,36 | 8,95 |
| `NeonYellow` | #FAD84A | tertiary | nur über `WarnAmber`; ΔE76 30,5 zu `FamilySympathetic` | 13,12 | 10,34 |
| `OnNeon` / `OnCyan` | #1E0A16 / #042A2C | onPrimary, onTertiary, onError / onSecondary | Inhalt auf Neon | 6,15 auf primary, 6,96 auf error | – |
| `DarkBackground` / `DarkSurface` | #160F2E / #211640 | background / surface, surfaceContainer | Screen-Grund (`window_background`) / Leisten (Elevation 3 dp → #321C48) | – | – |
| `DarkSurfaceVariant` | #2E2150 | surfaceVariant, surfaceContainerHighest | **Card-Fläche**, Mini-Bar | 1,27 | – |
| `SurfaceContainerHigh` / `Lowest` / `Low` | #281B48 / #120C26 / #1B1236 | surfaceContainerHigh/Lowest/Low | Dialog, Suchfeld-Pille (`SearchField`), ungewählte Filter-Chips (`TonalChip`) / nur implizit von M3 gelesen / ruhige Leer-Karte „Eigene Kreationen“ | – | – |
| `SecondaryContainer` | #3A2C60 | secondaryContainer | Tonal-Buttons, Auswahl-Pille der Tab-Leiste (Icon und Beschriftung darauf `primary`, 4,00) | 1,50 | 1,18 |
| `SegmentSelected` | #715AAF | – | Fläche des gewählten Segments (`SegmentedChoiceRow`, 3,02 gegen das ungewählte, Beschriftung 4,62 darauf), des gewählten Anlass-Chips (`ChoiceChip`) und Filter-Chips (`TonalChip`, 2,82 gegen `surfaceContainerHigh`, Beschriftung von `SECONDARY` auf voll) | 3,31 | 2,61 |
| `OutlineVariantIndigo` | #453763 | outlineVariant | Haarlinie unter dem Kopf, Feinstriche des Zeitrings; dekorativ | 1,73 | 1,37 |
| `DividerSubtle` | #F0E6FF α 0,08 | – | **einziger** Trenner in Karten und Expandern (KOMP-03); dekorativ | – | – |
| `OutlineIndigo` | #9A8FBF | outline | Rand von Switch, Chip (`ChoiceChip`, `LinkChip`), Segment, `AppOutlinedButton`, Namensfeld, ∞-Knopf des Zeitrings | 6,18 | 4,87 |
| `PrimaryContainer` / `OnPrimaryContainer` | #4E2560 / = DarkOnBackground | primaryContainer / onPrimaryContainer | nur implizit von M3 gelesen (FARBE-06); Text darauf 9,94 | – | – |
| `DarkOnBackground/OnSurface` | #F0E6FF | onBackground, onSurface | Primärtext, alle Köpfe (FARBE-07) | 15,30 | 12,05 |
| `DarkOnSurfaceVariant` | #C3B8DD | onSurfaceVariant | leiser Text (Dialog, Mini-Bar), „+ Halten“ im Editor | 9,83 | 7,74 |
| `FamilyVagal` / `Sympathetic` / `Balance` / `Functional` | #60A5FA / #F59E0B / #A78BFA / #34D399 | – | Familie „Beruhigen“ / „Aktivieren“ / „Ausgleichen“ / „Atem schulen“: Gruppenlabel, Kartenrand, Bereichs-Verweis | 7,23 / 8,56 / 6,76 / 9,57 | 5,70 / 6,75 / 5,32 / 7,54 |
| `EvidenceBest` / `SynthTrack` | = NeonCyan | – | Studienlage „gut belegt“ / Heute-Ring im Logbuch | 11,36 | 8,95 |
| `EvidenceStudied` | #78B0CC | – | Studienlage „in Studien geprüft“; Chip-Text 4,60 auf Card | 7,78 | 6,13 |
| `EvidenceLittle` | #C8B8B4 | – | Studienlage „kaum untersucht“; Chip-Text 5,43 auf Card | 9,61 | 7,57 |
| `EvidenceCaution` | #FF6B8B | error | Vorsicht, Fehler, Bestätigungszustand (MUSTER-05); Leiste 5,54 | 6,77 | 5,33 |
| `WarnAmber` | = NeonYellow | – | Sicherheit, Intensität („intensiv“, ⚠-Zeile, Bediengrenze im Editor); Chip 7,02 auf Card | 13,12 | 10,34 |
| `BreathExhale` | #FF9E7A | – | Ausatmen in Rhythmusbildern, Phasen-Karte und Atemkurve (Koralle; ΔE76 ≥ 36 zu jeder Semantikfarbe); derselbe Ton im Kopf-Ring | 9,13 | 7,19 |
| `HeadingRing` | #F25CA2 → #FF9E7A → #A78BFA → #F59E0B → #34D399 | – | Kopf-Ring (FARBE-07): nur die Titel der vier Kapitelkarten in „Lernen“ (`NavCard`) über `headingColor(index)`, **ohne Bedeutung** | 5,98 / 9,13 / 6,76 / 8,56 / 9,57 | 4,71 / 7,19 / 5,32 / 6,75 / 7,54 |
| `VayuPrana` / `Apana` / `Samana` / `Udana` / `Vyana` | = NeonCyan / BreathExhale / NeonYellow / FamilyBalance / FamilyFunctional | – | die fünf *vāyus* im Bild der Atemlehre (`VayuFigure`) und seiner Liste, **nur dort**, jede Fläche mit Namen (FARBE-05); Gelb und Grün sind hier weder Warnung noch Familie ([spec/lernen.md](spec/lernen.md)) | 11,36 / 9,13 / 13,12 / 6,76 / 9,57 | 8,95 / 7,19 / 10,34 / 5,32 / 7,54 |
| `VayuFigureFill` / `VayuFigureOutline` | #241A47 / #8C7BC4 | – | Grundfläche (dekorativ) und Umriss der Piktogramm-Figur; Umriss ≥ 3:1 auf `surface` | – / – | – |
| `SynthCircleCenter` / `Edge` | #CB3C9A / #4A2080 (`ALPHA_CIRCLE_EDGE` 0,72) | – | Atemkreis-Verlauf | 4,07 / 1,44 | – |
| `SessionTextYellow` / `SessionNoteAmber` | #EBDCB0 / #F0E4C0 | – | Phase und Sekunden im Kreis / Anleitungszeile unter dem Kreis (α `SECONDARY`) | 13,48 / 7,50 (Phase auf Zentrum 3,31, großer Text) | – |
| `SessionTextGlow` | #160B22 | – | Textschatten, Label auf Cyan-Button | – | – |
| `SessionButtonCyan` / `Pink` | #6FB6BA / #FF8FB0 | – | Session-Buttons, „Beenden“ | 7,95 / 8,59 | – |

ΔE76 der Pinktöne: primary ↔ `EvidenceCaution` 19,9, primary ↔ `SessionButtonPink` 23,0, `EvidenceCaution` ↔ `SessionButtonPink` 18,1 (S-18).

### 2.2 Alpha-Tokens

| Stufe | Token | Wert | min. Kontrast BG / Card | Verwendung |
|---|---|---|---|---|
| Primär | – | 1,0 | 15,30 / 12,05 | Titel, Labels, Werte |
| Fließtext | `BODY` | 0,8 | 10,00 / 8,26 | Kurzbeschreibung, Metazeile der Karte, Text der Detailseite, Glossar, Über, aufgeklappter Teaser (`ExpanderSection`) |
| Sekundär | `SECONDARY` | 0,7 | 7,92 / 6,68 | Untertitel, Beschreibung, Status, jeder zugeklappte Teaser, neutrale Chips (Umriss), Caret und Kopf-Aktionen, ungewählte Filter-Chips |
| Tertiär | `TERTIARY` | 0,55 | 5,27 / 4,69 (Leiste 4,78) | DOI/PMID, Zeitstempel, Wochentage, Session-Hilfszeilen, „✓ Fertig“, unselektierte Tabs, Ton aus, Zeitleiste und Linien „voll“/„leer“ der Atemkurve |
| Disabled | M3-intern | 0,38 | – | nur inaktive Bedienelemente |
| Flächen | `ALPHA_BORDER` · `CHIP_FILL` · `ALPHA_HEAT_LOW` / `_MID` · `ALPHA_CURVE_FILL` · `ALPHA_CIRCLE_EDGE` · `ALPHA_TRACK` / `ALPHA_DISABLED_BORDER` | 0,35 · 0,16 · 0,70 / 0,85 · 0,08 · 0,72 · 0,10 / 0,4 | `primary` 0,70: 3,01 / 3,50 | Kartenrand (`accentBorder`) · Fläche gefüllter Chips · Stufen der Logbuch-Wochenzeile (Zustandsgrafik; Tag ohne Übung = leerer Ring in `TERTIARY`, 4,70 auf Card) · Füllung unter der Atemkurve (dekorativ) · Rand des Atemkreis-Verlaufs · Kreis-Track, inaktiver Rand |
| vāyu-Bild | `VayuAlpha(selected, idle, dimmed)`: `VayuFieldAlpha` · `VayuWholeAlpha` · `VayuAuraAlpha` · `VayuLineAlpha` · `VayuLabelAlpha`; `VAYU_PULSE_LOW` / `_HIGH` | 0,95/0,55/0,10 · 0,32/0,10/0,04 · 0,90/0,35/0,08 · 1/0,55/0 · 1/1/0,30; 0,55 / 0,95 | Beschriftung voll 6,76 auf `surface` | Zustände im vāyu-Bild: gewählt / nichts gewählt / ein anderer gewählt; Pulsieren der gewählten Fläche ([spec/lernen.md](spec/lernen.md)) |

### 2.3 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| FARBE-01 | MUSS | Farben nur aus `MaterialTheme.colorScheme` oder aus `Color.kt` (Family\*, Evidence\*, Session\*, Synth\*, `BreathExhale`, `HeadingRing`, `Vayu*`). Kein `Color(0x…)` außerhalb `src/ui/theme/`. Hex in `res/` nur für `window_background`, den Launcher-Grund #1A0B2E (Abweichung: das Symbol bleibt unverändert, Eigentümer 2026-10-30) und `#FF000000`/`#FFFFFFFF` in einfarbigen `drawable/ic_*.xml`. | Eine Quelle der Wahrheit. | auto: `color-source` |
| FARBE-02 | MUSS | Alpha nur über die Tokens aus §2.2, auch nicht bedingt (`if (…) 0.8f else 0.6f`). | Freie Literale verwischen die Hierarchie. | auto: `alpha-literal` |
| FARBE-03 | MUSS | Kontrast nach Alpha-Mischen gegen den realen Grund: Text < 24 sp (< 18,66 sp Bold) ≥ 4,5:1; großer Text, Icons, Zustandsgrafik ≥ 3:1. Kritischer Punkt zählt (Kreiszentrum, gemischte Chip-Fläche, Leiste #321C48). Ausnahme: rein dekorative Linien/Flächen mit Marker `// dekorativ: <Grund>`. | WCAG 1.4.3 / 1.4.11. | manuell: R-A11Y |
| FARBE-04 | MUSS | Semantische Farben tragen nur ihre Bedeutung (Spalte Bedeutung in §2.1): Studienlage, `EvidenceCaution`/`error`, `WarnAmber`, `Family*`, `primary` (Marke/Aktion/Auswahl), `secondary` (Links im Inhalt, Einatmen, eigenes Muster), `BreathExhale` (Ausatmen). Einzige Ausnahme: der Bereichs-Verweis „Mehr: Beruhigen ›“ steht in der Familienfarbe. Semantische Farben auf einem Screen: ΔE76 ≥ 25; Kopf-Ring und `Vayu*` zählen nicht dazu. | Doppelt belegte Farben sagen nichts mehr. | manuell: R-VISUAL (ΔE) |
| FARBE-05 | MUSS | Farbe ist nie alleiniger Träger: Evidenz und Warnung mit Text (⚠-Zeile), Familie als Text oder im Gruppenlabel, Zustand (gewählt, aktiv, heute) mit ≥ 3:1 zur Umgebung oder einem Formmerkmal. Wie eine Auswahl aussieht, regelt A11Y-03. | WCAG 1.4.1 / 1.4.11. | manuell: R-A11Y |
| FARBE-06 | MUSS | Von M3 implizit gelesene Rollen sind gesetzt (`outline`, `error`, `onError`, `primaryContainer`). Jedes Token in `Color.kt`, `Dimens`, `Sizes`, `Motion` ist genutzt; seine KDoc stimmt. | Sonst M3-Baseline-Grau, tote Tokens. | auto: `theme-tokens` |
| FARBE-07 | MUSS | **Köpfe weiß.** Abschnittsköpfe (`SectionHeader`, `CollapsibleSection`), Expander-Titel (`ExpanderSection`, `ExpanderCard`) und Kartentitel (`CardTitle`, `CardHeader`) stehen in `onSurface`, ohne Balken; Caret, Chevron, Zähler und Kopf-Aktionen gedimmt bzw. `onSurface`. Farbe eine Ebene darunter: `GroupLabel` (Familie), Eyebrow des Tagesmusters (`secondary`), Kartenrand. Einzige Ausnahme: die Titel der vier Kapitelkarten in „Lernen“ (`NavCard`) reihum durch `HeadingRing`. | Eine Kopf-Art liest sich ruhig; Farbe sagt dann nur noch etwas, wo sie etwas bedeutet (DECISIONS 2026-10-24). | auto: `head-color`; manuell: R-VISUAL |

## <a id="typografie"></a>3 Typografie

Quelle [Type.kt](../app/src/main/java/app/atemkraft/ui/theme/Type.kt): M3 `Typography()`, Roboto, **Größen sind Material-Standard** ohne eigenen Skalierfaktor (Leitplanke 5; DECISIONS 2026-10-19). Überschrieben sind nur Gewicht und Umbruch der sechs Überschrift-Rollen; die Systemschriftgröße wirkt zusätzlich.

| Rolle | sp (M3) | Gewicht | Zweck (normativ) |
|---|---|---|---|
| `headlineLarge` | 32/40 | Bold | derzeit ungenutzt |
| `headlineMedium` | 28/36 | Bold | Markenkopf „Über“ |
| `headlineSmall` | 24/32 | SemiBold | Titel der festen Kopfzeile aller Seiten (`ScreenHeader`, `PushHeader`), Abschluss-Titel, Phasenname im Kreis |
| `titleLarge` | 22/28 | SemiBold | `SectionHeader` (Titel und Anzahl-Suffix), `onSurface` |
| `titleMedium` | 16/24 | SemiBold | Karten- und Expander-Titel (`CardHeader`, `CardTitle`), Stepper-Wert |
| `titleSmall` | 14/20 | M3 (Medium) | Gruppenlabel (`GroupLabel`), Stepper-Label, Overlay-Titel, Mini-Bar-Status, Kennzahlen, Zeitring-Marken, gewählte Phase im Editor |
| `bodyMedium` | 14/20 | M3 | Einleitung der Push-Seiten (`ScreenSubtitle`), Suchfeld, Namensfeld, Kurzbeschreibung der Detailseite, Zeilen-Labels (`SubLabel`, `ToggleRow`, Radioliste, Vorlagen), Phasen-Notiz, Name · Ort in der vāyu-Liste |
| `bodySmall` | 12/16 | M3 | Fließtext, Kartentext, Metazeile, Teaser, Dialogtext, Such-Status, Hinweise unter Kontrollen, Zitat, Datum, Footer, Taktangabe |
| `labelLarge` | 14/20 | M3 | nur implizit über M3: Beschriftung von Buttons, Segmenten, `LinkChip`, `ChoiceChip` |
| `labelMedium` | 12/16 | M3 | Eyebrow (Familie auf der Detailseite), Filter-Chips (`TonalChip`), `DisclosureToggle`, „✓ Fertig“, „Aktiv“ (Stimme), Einheit unter der Zeit, Phasen-Beschriftung der Atemkurve, Suffix „· angepasst“ am Abschnittskopf, Beschriftung im vāyu-Bild (kursiv) |
| `labelSmall` | 11/16 | M3 | Info-Chips, DOI/PMID, Wochentage, Eyebrow „MUSTER DES TAGES“ (Spielart `typography.eyebrow`: Laufweite 0,08 em, Großbuchstaben per `uppercase()`, TalkBack hört den Originaltext), Suffix „· angepasst“ in Knöpfen, Mini-Bar-Titel, Zeitring-Ziffern, Zeitleiste der Atemkurve |
| `displayMedium` / `displaySmall` | 45/52 / 36/44 | M3 | Zeit in der Mitte des Zeitrings (größter passender Stil, Rückfall bis `titleLarge`) / Countdown, Meditationszeit (`GlowText`) |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| TYPO-01 | MUSS | Schrift nur über `MaterialTheme.typography.<Rolle>`. Kein `.sp`, `fontSize`, `letterSpacing`, `lineHeight` oder `FontWeight` außerhalb `src/ui/theme/`; anderes Gewicht = neue Rolle. Kein Gewichtswechsel bei Auswahl. | Skalierung an einer Stelle; Gewichtswechsel springt die Breite. | auto: `type-literal` |
| TYPO-02 | MUSS | Die Rolle folgt der Tabelle; eine neue Verwendung wird erst eingetragen, dann gebaut. | Lesbare Hierarchie. | manuell: R-CODE |
| TYPO-03 | MUSS | `headline*`- und `title*`-Rollen tragen in `Type.kt` `LineBreak.Heading` und `Hyphens.None`. **Keine Rolle schaltet Silbentrennung ein.** Ganze Wörter sichert das Layout (LAYOUT-03), nicht die Typografie. | Silbentrennung trennt auch Wörter, die ganz in die nächste Zeile passen würden; ohne sie bricht Compose zu lange Wörter an beliebiger Stelle. | manuell: R-CODE |

## <a id="layout"></a>4 Layout, Abstände, Formen

### 4.1 Abstände ([Dimens.kt](../app/src/main/java/app/atemkraft/ui/theme/Dimens.kt)), Raster 4 dp

Ränder und Karten-Innenabstand wie M3 (16 dp). Touch-Ziele hängen nie an diesen Werten: Eigene klickbare Flächen tragen `heightIn(min = MinTouchTarget)`; die Optik der Bedienelemente kommt aus §4.2. Wann welcher Abstand gilt, sagt die Abstandsskala in §4.5.

| Token | dp | Rolle |
|---|---|---|
| `ScreenPadding` | 16 | horizontaler Screen-Rand |
| `CardPadding` | 16 | Karten-Innenabstand |
| `ListGap` | 12 | **Karte ↔ Karte (immer)**, Listen, Button-Reihen, Lupe → Text im Suchfeld |
| `SessionPadding` | 24 | Rand der Vollbild-Sessions |
| `ScreenTopSub` / `ScreenBottom` | 8 / 24 | oben über der festen Kopfzeile / unten alle Scroll-Screens |
| `SectionGap` / `SectionHeaderTop` | 16 / 24 | nur zwischen Blöcken **verschiedenen Typs**, oben auf jeder Push-Seite / über `SectionHeader`; auch der Abstand Name ↔ Schalter in `ToggleRow` |
| `HeaderGap` | 12 (= `ListGap`) | **Kopf → erster Inhalt**, unter jedem `SectionHeader` und `GroupLabel` gleich: Der Kopf trägt unten keinen Abstand; Listen treffen ihn über `spacedBy(ListGap)`, Spalten über `CollapsibleSection` |
| `ChipPadding` | 12 | waagerechtes Polster der Anlass-Chips (`ChoiceChip`) und Filter-Chips (`TonalChip`) |
| `ScrollEdgeFade` | 32 | Verlauf in Transparenz an der Seite einer seitlich scrollenden Chip-Reihe mit weiterem Inhalt |
| `GapSmall` / `GapTiny` / `GapHairline` | 8 / 4 / 2 | Label → Steuerung / Hint, Titel → Aktionen, Chips nebeneinander / Icon ↔ Text, Titel → Metazeile, Eyebrow → Titel / enge Divider, Rest des Überstands einer Kopf-Aktion |
| `MinTouchTarget` | 48 | Mindesthöhe eigener klickbarer Flächen, Höhe der Kopfzeile in der Karte |
| `PinnedBarElevation` | 3 | tonale Erhöhung der gepinnten Start-Leiste (`PinnedActionBar`) |
| `FocusRing` | 2 | Fokusring um Atemkreis und Meditationsring (`CircleTapTarget`; Raster-Ausnahme) |
| `BorderThin` | 1 | Kartenrand (`accentBorder`, `ALPHA_BORDER`), Rand von `AppOutlinedButton`, umrandeten Info-Chips und ∞-Knopf |
| `BorderChip` | 1,5 | Rand der `LinkChip` in `outline` |
| `SessionRingStroke` | 3 | Spur und Fortschrittsbogen um den Atemkreis (Raster-Ausnahme) |
| `SessionMainButton` / `SessionSideButton` | 76 / 56 | Kreise der `SessionControlBar`: Pause/Weiter / „Von vorne“, „Beenden“; fest, selbst Tippfläche |
| `SessionMainIcon` / `SessionSideIcon` | 32 / 28 | Icons in diesen Kreisen |
| `SessionControlGap` | 40 | Abstand der Knöpfe in der `SessionControlBar` |
| `OverlayIconButton` | 40 | Kreis der Kopfleisten-Knöpfe im Session-Overlay |
| `DurationPickerMin` | 200 | Mindestkante des Zeitrings; bei großer Schrift `timeDialMinSide` ([spec/meditation.md](spec/meditation.md)) |
| `ContentMaxWidth` | 600 | Höchstbreite der Inhalte auf Tablets und Foldables, mittig (`MainActivity`) |
| `SessionCircleMin` · `SessionCircleWidthShare` · `SessionCircleHeightShare` | 96 · 0,9 · 0,62 | Session-Kreis (`SessionRunningLayout`): kleinste Kante · Höchstanteil an der Spaltenbreite · an der Höhe im Hochformat |

### 4.2 Größen ([Sizes.kt](../app/src/main/java/app/atemkraft/ui/theme/Sizes.kt))

**Bedienelemente haben Material-3-Größe.** Die **Tippfläche** ist `Dimens.MinTouchTarget` 48 (A11Y-05). Gesetzt werden die Tokens nur in `src/ui/components/`, an M3-Elementen nur als `heightIn(min = …)` (LAYOUT-04). Ob Optik und Tippfläche stimmen, prüfen die Gates (`TIPPFLAECHE`, `BESCHNITTEN`, `ANATOMIE`).

| Token | dp (M3) | Verwendung |
|---|---|---|
| `ButtonHeight` | 40 | Mindesthöhe: Start und Reset, `Session*Button`, `AppTextButton`, `AppOutlinedButton`, `DisclosureToggle`, Segmente, `ChoiceChip` |
| `ChipHeight` | 32 | `LinkChip`, `TonalChip` (Filter-Chips) |
| `SwitchWidth` | 52 | Breite des M3-Switch; der Hinweis einer `ToggleRow` endet um diese Breite plus Abstand vor der Kante |
| `IconButtonSize` | 40 | Kreis von `AppIconButton`, `AppIconToggle`, `SoundSwitch`, Stepper-Knöpfen, `StartPlayButton` |
| `TabBarHandleWidth` · `TabBarHandleHeight` · `TabBarHandleShape` | 56 · 18 · oben gerundet | Lasche des Knopfs `TabBarToggle` ([spec/navigation.md](spec/navigation.md)) |
| `IconDefault` | 24 | Icon in `AppIconButton`, Expander-Caret, Mini-Leiste, Lupe |
| `IconInButton` | 18 | Icon neben Beschriftung: Reset, `DisclosureToggle`-Caret, `ForwardChevron` |
| `SegmentIconReserve` | 26 | Platz, den M3-`SegmentedButton` (1.3) für das Häkchen reserviert; `SegmentedChoiceRow` gibt ihn der Beschriftung zurück (an M3 1.3 gebunden) |
| `IconSmall` | 16 | Info-Icon „Anleitung“, Atemkreis „tut mir gut“ auf Karten, Stift im Editor-Kopf, „öffnet im Browser“ |
| `ProgressInline` / `ProgressStroke` | 20 / 2 | Lade-Kreis in der Stimmenliste |
| `WeekDot` · `FamilyDot` | 18 · 10 | Tagespunkt der 7-Tage-Übersicht · Familienpunkt vor jedem Logbuch-Eintrag |
| `PauseFlashSize` | 150 | Pause-/Play-Symbol, das beim Antippen des Atemkreises aufblinkt (`PauseFlash`); nie größer als der Kreis |
| `TouchOverhang` | 12 | (48 − `IconDefault`) / 2: Überstand der Tippfläche über das Icon; um ihn melden `flushStart`/`flushEnd` einen Icon-Button schmaler |
| `CircleOverhang` | 4 | (48 − `IconButtonSize`) / 2: Überstand der Tippfläche über den Knopf-Kreis (Stepper, Kopfzeile in der Karte) |
| `StepperValueMinWidth` | 48 | Wertfeld des Steppers (kein Touch-Ziel) |
| `CurveSpan` · `CurveHandle` · `CurveHandleSelected` · `CurveSpanMin` | 88 · 18 · 22 · 48 | Atemkurve: Abstand „voll“/„leer“ · Griff · gewählter Griff · kleinster Abstand (= `MinTouchTarget`) |
| `MiniCurveWidth` · `MiniCurveHeight` | 64 · 24 | Mini-Kurve je Vorlage im Startpunkt-Sheet (dekorativ) |
| `VayuFigureMaxWidth` | 280 | Höchstbreite des vāyu-Bilds (Seitenverhältnis 352 : 450) |
| `CurveLabelMinSpacing` · `CurveTickMinSpacing` | 30 · 6 | Atemkurve: Mindestabstand der Zeitleisten-Zahlen · feine Striche ab diesem Abstand (Schwellen, kein Layout-Abstand) |
| `CurveStroke` · `CurveStrokeSelected` · `CurveHoldStroke` · `CurveDash` · `CurveDot` | 4 · 6 · 3 · 6 · 14 | Atemkurve: Striche, Strichlänge Halten, Punkt der Vorschau |

Switch und RadioButton stehen in M3-Größe. `AppTextButton` trägt `shapes.large` (20 dp = halbe `ButtonHeight`): mehrzeilig schneidet keine große Rundung Glyphen an. Eigene Maße außerhalb von `Sizes`: `NavigationBar` (M3 1.3.1: Höhe 80, Indikator 64 × 32, S-37), die Icons von `OverlayChrome` und die `SessionControlBar` (Bedienung mit halb geschlossenen Augen, `Dimens.Session*`). Kartenrand nur über `accentBorder(color)`; gestrichelter Rand über `dashedBorder(color, shape, width = BorderThin, dash = GapTiny)` (`CardBorder.kt`).

### 4.3 Formen und Screen-Gerüst

| Shape | Radius | Wofür |
|---|---|---|
| `extraSmall` / `small` / `medium` / `large` / `extraLarge` | 8 / 12 / 16 / 20 / 28 dp | Info-Chip / `ChoiceChip`, gewählte Listenzeile / Card, Leer-Karte / `AppTextButton`, `AppOutlinedButton` / Dialog |
| `CircleShape` | – | Punkte, Buttons, Pillen (`SearchField`, `TonalChip`) |

| Typ | Container | oben | Kopf | unten |
|---|---|---|---|---|
| Tab | `TabListScaffold(header, top = 0)`: `Surface(background)` → Kopf-Slot (bekommt `scrolled`) und `LazyColumn(weight(1f), spacedBy(ListGap))`, horizontal `ScreenPadding` als `contentPadding` (nicht als Modifier: bündige Icon-Buttons ragen in den Rand); kein Untertitel | `ScreenTopSub` (im `ScreenHeader`) | `ScreenHeader`: Titel `headlineSmall`, rechts höchstens eine Icon-Aktion (`SettingsAction`). **Rahmen fest, Inhalt scrollt:** Kopf und Leisten scrollen nie; Haarlinie (`outlineVariant`) erst mit `scrolled`. Tabs: Atmen, Eigene, Meditation (eigenes Gerüst, LAYOUT-08), Lernen ([spec/](spec/)) | `ScreenBottom` als `contentPadding` |
| Push | `PushScaffold(onBack, title \| titleContent, action, bottomBar)`: `PushHeader` (fest), `Column(weight(1f)).verticalScroll` mit Rand `ScreenPadding`, darunter fest optional `bottomBar` (`PinnedActionBar`). Einleitung als `ScreenSubtitle` (Atemlehre, Pranayama, Über). Logbuch: `TabListScaffold` mit `PushHeader`, `top = SectionGap`; Muster-Editor: eigenes Gerüst | `ScreenTopSub`, dann `SectionGap` bis zum ersten Inhalt (erster `SectionHeader` mit `headerTop = 0`) | `PushHeader`: `BackButton` und Titel `headlineSmall` (Laufschrift statt Umbruch), rechts höchstens eine `action` (Detail: Markierung bzw. Stift; Editor: ◉ „In Eigene speichern“) | `ScreenBottom` |
| Vollbild-Session | `OverlayChrome` → Inhalt mit `SessionPadding` ([spec/session.md](spec/session.md)) | `safeDrawing` | Minimieren links, rechts „Danach in die Stille“ (nur Atem-Session) und Ton | – |

### 4.4 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| LAYOUT-01 | MUSS | Abstände nur über `Dimens`, Größen über `Sizes`, Formen über `MaterialTheme.shapes.*` oder `CircleShape`. Keine `.dp`-Literale (außer `0.dp`), kein `RoundedCornerShape`/`CutCornerShape`, kein `Dp.Hairline` außerhalb `src/ui/theme/`. Nie eckige Schnitte. | 160 Literale, 36 davon neben dem Raster; zwei Chip-Radien. | auto: `layout-literal` |
| LAYOUT-02 | MUSS | Screens folgen dem Gerüst §4.3 und der Abstandsskala §4.5: Karte ↔ Karte immer `ListGap`, `SectionGap` nur zwischen Blöcken verschiedenen Typs, Kopf → Inhalt `HeaderGap`. Insets nur über `Scaffold`-`innerPadding` oder `safeDrawingPadding()`, keine festen Leistenhöhen. | Nachgebaute Kopf-Blöcke, drei Kartenabstände; Edge-to-edge ab API 35. | manuell: R-VISUAL |
| LAYOUT-03 | MUSS | **Kein Wort bricht um, kein Text wird gekürzt** – auf 360–412 dp bei fontScale 1,0–2,0, quer und Tablet; keine Zeile beginnt im Wort oder mit Satzzeichen (`badBreakAt`); keine runde Form schneidet Glyphen an. Mittel in dieser Reihenfolge: (1) Layout gibt Platz (`TitleWithChips`, `AdaptiveButtonRow`, `InlineOrBelow`, Reserven), (2) `WholeWordText` verkleinert bis 70 % (Chips, Tabs 85 %, dann Layoutwechsel), (3) Text umformulieren. Verboten: Ellipse auf Inhalt (Ausnahme `MarqueeText`), feste Höhe für Text, `weight(fill = false)` neben Chips, Orientierungssperre. | Nutzerbefund A54: „Resonanz-Atmun / g“, gekürzte Sicherheitshinweise; WCAG 1.4.4, 1.4.10. | auto: `ScreenshotTest` (`WORTBRUCH`, `ABGESCHNITTEN`, `CHIP_UMBRUCH`, `ZUGESCHNITTEN`), `weight-nofill`, `platform-override` |
| LAYOUT-04 | MUSS | **Bedienelemente.** Sichtbare Größe nur aus `Sizes`, gesetzt nur in `src/ui/components/`; M3-Größenkonstanten nur in `src/ui/theme/`; M3-Steuerelemente (`Button`, `IconButton`, `FilterChip`, `Switch` …) nur in `components/`, Screens nutzen die Wrapper (§5.1). Höhe an M3-Elementen nur `heightIn(min)`; eigene feste Größe nur `minimumInteractiveComponentSize().size(x)`; kein `scale`/`graphicsLayer` auf Bedienbarem; gleiche Höhe nie über `IntrinsicSize.Min`. **Ein Größentoken je Leiste:** Nachbarn sind gleich hoch, hervorgehoben wird durch Füllung oder Farbe. | `height` nahm dem Reset die 48-dp-Reservierung, `scale` den Hit-Test; Einzelkorrekturen laufen auseinander (DECISIONS 2026-10-22). | auto: `m3-size-const`, `control-direct`, `control-scale`, `touch-order`, `layout-literal`, `ScreenshotTest` (`TIPPFLAECHE`) |
| LAYOUT-05 | MUSS | **Bedienelemente bleiben ganz.** Kein Bedienelement wird beschnitten oder aus dem Bild geschoben: Abschneiden darf nur eine Scroll-Fläche in ihrer Richtung – senkrecht die Seite, waagerecht einzig die Chip-Reihen (`ChoiceChipRow`, `FilterChipRow`); Beschriftung und Icon bleiben im sichtbaren Behälter, bei fontScale 2,0 wächst er mit. | Kleinere Tokens und feste Höhen machen genau das wahrscheinlich; LAYOUT-03 prüft nur Text. | auto: `ScreenshotTest` (`BESCHNITTEN`, `AUSSERHALB`) |
| LAYOUT-06 | – | entfällt → LAYOUT-04 (Bauregeln für Bedienelemente). | Ein Gegenstand, eine Regel. | auto: `control-scale`, `touch-order` |
| LAYOUT-07 | SOLL | **Steuerung beim Label, Glyph bündig.** In Karten und Einstellungen steht die Steuerung als rechte Spalte, ihre *sichtbare* Kante an der Inhaltskante: Überstand der Tippfläche über `flushEnd`/`flushStart` (`TouchOverhang`) bzw. `CircleOverhang` vom Polster abziehen, kein `offset`; nie in einem clippenden Behälter. Feste Endspalten (`TrailingRow`) halten ihre Breite in jedem Zustand ([spec/einstellungen.md](spec/einstellungen.md)). Ein Hinweis endet vor dem Schalter seiner Zeile. | Nutzerbefund „weit versetzt“: 100–130 dp zwischen Label und Stepper. | auto: `ScreenshotTest` (`ANATOMIE`); manuell: R-VISUAL |
| LAYOUT-08 | MUSS | **Meditations-Tab ohne Scrollen.** Kopf, Zeitring, „Klang & Stimme“ und Start passen bis fontScale 1,3 auf jedem Gerät der Matrix in die Höhe über Tab-Leiste und Systemleisten, in jedem Zustand; der Zeitring nimmt den Rest (`FillViewportColumn`, Mindestkante `DurationPickerMin`/`timeDialMinSide`). Nur bei fontScale 2,0 darf die Seite als Notlösung scrollen – abgeschnitten wird nie. | Eine ruhige Seite, alles auf einen Blick; ohne Leisten im Test wurde der Platz um rund 150 dp überschätzt. | auto: `ScreenshotTest` (`SCROLLT` an `03_meditation_fein`), `DeviceFitTest` (`03_meditation`, `RING`) |

### <a id="anatomie"></a>4.5 Anatomie der Bausteine (Layout-Grammatik)

Für jeden wiederkehrenden Baustein gilt eine verbindliche Anatomie: Anordnung, Position und Größenverhältnisse werden nachgeschlagen, nicht erfunden (PROZ-07). Die Anatomie lebt an **einer** Stelle im Code – Karten mit Aktionen nur über `CardHeader`, Abschnittsköpfe über `SectionHeader`/`GroupLabel` –, und das Gate `ANATOMIE` im `ScreenshotTest` misst sie an jedem Bild nach (Test-Tags `CARD_HEADER_TAG`, `TITLE_ROW_TAG`, `TITLE_ROW_TITLE_TAG`, `TITLE_ROW_ACTIONS_TAG`, `TITLE_ROW_INDICATOR_TAG`, `CONTROL_ROW_TAG`). Statisch wacht `anatomy-overlay`: Ein Overlay (`align(Alignment.TopEnd)`) gibt es außerhalb von `CardHeader` nicht mehr.

#### Allgemeine Positionsregeln

| Regel | Was gilt | Prüfung |
|---|---|---|
| **Abstandsskala** | `GapTiny` 4: Titel → Metazeile, Eyebrow → Titel, Icon ↔ Text in einer Zeile · `GapSmall` 8: Titel → Aktionen/Zeichen, Chips nebeneinander, Label → Steuerung/Hinweis, Feld → Chip-Reihe, Werte → Kurve · `ListGap` 12: Karte ↔ Karte, Listen, Button-Reihen · `HeaderGap` 12: Kopf → erster Inhalt (der Kopf selbst trägt unten nichts) · `SectionGap` 16: nur zwischen Blöcken verschiedenen Typs, Gruppenlabel ↔ Block darüber · `SectionHeaderTop` 24: über einem `SectionHeader` · `CardPadding` 16: Karten-Innenabstand · `ScreenPadding` 16: Screen-Rand, Rand der Chip-Reihen und Blätter. Keine Zwischenwerte, keine Summen „8 + 4“ am Aufrufort. | `layout-literal`; R-VISUAL |
| **Bündigkeit** | Glyphen stehen bündig mit der Textkante, nie die Tippfläche: Icon-Buttons über `flushEnd`/`flushStart` (Überstand `TouchOverhang` 12), Knopf-Kreise über `CircleOverhang` 4, Text-Buttons reichen ihr Polster über `flushStart(inCard)` in den Rand. In Karten höchstens `CardPadding` Überstand, damit die Tippfläche nicht über den Kartenrand ragt (die Karte clippt). Kein `offset`: Nachbarn rücken mit. | `ANATOMIE` (Glyph bündig ± 1 dp), `TIPPFLAECHE`, `BESCHNITTEN` |
| **Vertikale Mitte auf der ersten Textzeile** | Aktionen, Zeichen (Atemkreis, Chevron, Caret) und Status stehen mittig zur **ersten** Titelzeile (`rememberFirstLineHeight(style)`, gemessen – ab Android 14 wächst große Schrift nichtlinear), nicht mittig zum ganzen Block; bricht der Titel um oder rutschen Chips darunter, bleiben sie oben. | `ANATOMIE` (Glyph-Mitte ± 4 dp) |
| **Größenverhältnisse** | Icon 24 (`IconDefault`) in Tippfläche 48 (`MinTouchTarget`); Kreis-Knopf 40 (`IconButtonSize`) in 48; Icon neben Text 18 (`IconInButton`); Zeichen auf Karten 16 (`IconSmall`). Titel `titleMedium` zu Metazeile/Kurztext `bodySmall`, Abschnittskopf `titleLarge` zu Gruppenlabel `titleSmall`; Chip-Höhen 40 (Anlass, eine Antwort) / 32 (Filter, mehrere). Nachbarn in einer Leiste teilen **ein** Größentoken (LAYOUT-04). | `m3-size-const`, `control-direct`, `TIPPFLAECHE`; Tokenvergleich im Code |
| **Nichts springt** | Wo ein Zustand wechselt (Reset erscheint, „· angepasst“, Vorschau läuft, X im Suchfeld, Startpunkt-Name), hält eine Reserve den Platz des breitesten Inhalts (`widestOf`, `rememberWidestTextWidth`, `leadingReserve`/`endReserve`, Platzhalter mit gleicher Breite); Höhe und Lage der Nachbarn ändern sich nicht. | `ScreenshotTest` (`chipLayout`, `KOMPAKT_FEHLT`); R-VISUAL |
| **Overlay nur über der Titelzeile** | Eine Aktion liegt nie frei über Fließtext oder in einer Ecke „irgendwo“: Sie steht auf der Titelzeile (`CardHeader`) oder im Kopf (`SectionHeader`, `GroupLabel`, `ScreenHeader`, `PushHeader`), ihre 48-dp-Tippfläche ragt nach unten in den Abstand bzw. über die Metazeile, der Baustein wird dadurch nicht höher. | `anatomy-overlay`, `ANATOMIE` |

#### Karte mit Aktionen (`ExerciseCard`, `PatternCard` → `SavedPatternCard`, `DailyPatternCard`, `WelcomeCard`, `NavCard`)

```
┌ Card(onClick?) · Rand accentBorder(Bedeutung) · Polster CardPadding ──────────────────┐
│ Titel titleMedium [Chip] [Chip]          ·····GapSmall·····  (◌)  [⊕ 48][⊗ 48] ←Glyph bündig │
│   ↑ hält rechts MinTouchTarget·n − TouchOverhang + GapSmall frei      ↑ Mitte = 1. Titelzeile │
│ Metazeile bodySmall BODY ········································ bis zur Inhaltskante ─────→ │
│ Kurztext bodySmall SECONDARY ····································· läuft unter den Aktionen ─→ │
└───────────────────────────────────────────────────────────────────────────────────────────┘
mit Eyebrow (Tagesmuster): Polster oben CardPadding − CircleOverhang
│ EYEBROW labelSmall secondary  (Zeile MinTouchTarget hoch)             [↻ 48][◉ 48] │
│ Titel titleMedium [Chip]   (volle Breite)                                            │
│ Musterzeile ···                                                                      │
```

- **Komponente:** `CardHeader(title, titleColor, eyebrow, chips, indicator, actionCount, actions)` als erste Zeile der Kartenspalte; der Inhalt darunter ist Geschwister in voller Breite. Niemand baut diese Zeile nach.
- **Titelzeile** (`TitleWithChips`) hält rechts die Breite der Aktionen frei: `MinTouchTarget × n − TouchOverhang + GapSmall`. **Fließtext und Metazeile laufen darunter durch** bis zur Inhaltskante.
- **Aktions-Glyphen** stehen mittig auf der ersten Titelzeile (`centeredOverflow(titleLine / 2)`) und bündig mit der Inhaltskante (`flushEnd(inCard = true)`); die Tippfläche ragt oben ins Polster und unten über die Metazeile – die **Karte wird durch Aktionen nicht höher**. Höchstens zwei Aktionen, zerstörende außen (`ConfirmIconButton` rechts vom Stift); Tint `onSurface` · `SECONDARY`, Zustandsakzent gefüllt in `secondary` (◉ „In Eigene speichern“).
- **Zeichen** (`indicator`, nicht klickbar: Atemkreis „tut mir gut“ `IconSmall`, `ForwardChevron`, „öffnet im Browser“) stehen in einer Box von Titelzeilenhöhe am Zeilenende.
- **Kopfzeile in der Karte** (`eyebrow`): Zeile `MinTouchTarget` hoch, Eyebrow links `CenterStart`, Aktionen rechts; Titel darunter in voller Breite; das Kartenpolster oben um `CircleOverhang` kleiner, damit der sichtbare Kreis `CardPadding` vom Rand steht.
- **Prüfung:** `ANATOMIE` – (1) Glyph über keinem Text, Tippfläche nicht über dem Titel, (2) mehrzeiliger Text reicht bis zur Inhaltskante, (3) Glyph-Mitte in der ersten Titelzeile ± 4 dp, (4) Glyph bündig ± 1 dp; `TIPPFLAECHE` 48 dp; `anatomy-overlay`.

#### Abschnittskopf (`SectionHeader`, `CollapsibleSection`, `GroupLabel`)

```
 SectionHeaderTop 24 (bzw. GapSmall über der Tippfläche, wenn aufklappbar)
 Titel titleLarge onSurface · Status SECONDARY  ⌄(Caret, Mitte 1. Zeile)        [+ 48] ←Glyph bündig
 Teaser bodySmall SECONDARY (ganzer Satz)                                      ↑ ragt ≤ HeaderGap − 2 nach unten
 HeaderGap 12
 ┌ erster Inhalt ┐
```

- Titel links (`weight(1f)`), **genau eine** Aktion rechts als 48-dp-`AppIconButton` in `onSurface`, Glyph bündig mit der Inhaltskante (`flushEnd`), Mitte auf der ersten Titelzeile; Caret bleibt am Titel, die Tippfläche des Auf-/Zuklappens endet vor der Aktion (zwei Bedienelemente, kein Button im Button).
- Kopf → Inhalt `HeaderGap` (Listen `spacedBy(ListGap)`, Spalten `CollapsibleSection`); Abschnitt → Abschnitt `SectionHeaderTop`/`SectionGap`. Der Kopf trägt unten keinen Abstand; die Tippfläche der Aktion ragt höchstens `HeaderGap − GapHairline` hinein (`centeredOverflow` mit Grenze), sonst wird der Kopf höher.
- `GroupLabel`: `titleSmall` in der Familienfarbe, `top = SectionGap` (in Listen abzüglich `ListGap`), Aktion wie oben (✕ „Alle zeigen“).
- Tippbare Köpfe (`expanded`, `onClick`): ganze Titelspalte samt Teaser ≥ 48 dp, Chevron statt Caret bei `onClick`.
- **Prüfung:** `ANATOMIE` (3, 4, tippbare Zeile ≥ 48), `semantics-heading`, `head-color`.

#### Zeile mit Steuerelement (`ToggleRow`, `Stepper`, `CompactStepper`, Einstellungs-Zeilen `TrailingRow`)

```
 Label bodyMedium (weight 1f) ···················· SectionGap ···· [Switch 52]  ≥ 48 dp hoch, Mitte
 Hinweis bodySmall SECONDARY ─── endet vor dem Schalter (end = SwitchWidth + Abstand)
 Label ·········································· (−)  48  (+)   Kreis von „+“ bündig (CircleOverhang)
```

- Label links mit `weight(1f)`, Steuerelement rechts, Mindesthöhe `MinTouchTarget`, vertikal zentriert; ganze Zeile ist die Tippfläche (`toggleable`), Switch und Radio als reine Anzeige in M3-Größe.
- Glyph bzw. Kreis bündig: Icon-Buttons `flushEnd(inCard = true)`, Stepper-Kreis um `CircleOverhang` eingerückt. Feste Endspalten halten in jedem Zustand ihre Breite ([spec/einstellungen.md](spec/einstellungen.md)).
- **Prüfung:** `ANATOMIE` (Steuerzeile ≥ 48 dp), `switch-unlabeled`, `TIPPFLAECHE`.

#### Chip-Reihen (`ChoiceChipRow` Anlässe, `FilterChipRow` Filter)

```
 [Stress 40][Kurzatmig][Einschlafen]      ← Anlass: ChoiceChip 40 dp, shapes.small, Rand, eine Antwort
 [Fokus][Wach werden][Erschöpft]▒▒        ← höchstens zwei Zeilen, GapSmall, sonst gemeinsam seitlich scrollend
 (Bis 3 min)(Ohne Halten)(Aus dem Yoga)   ← Filter: TonalChip 32 dp, Pille, tonal, mehrere; eine Zeile
```

- Anlass-Chips 40 dp mit Rand (eine Antwort), Filter-Chips 32 dp tonal (mehrere); Abstand `GapSmall` nebeneinander und untereinander (sichtbar); Reihe bündig mit `ScreenPadding`/`CardPadding`, die Scroll-Fläche reicht bis an den Bildschirmrand und läuft über `ScrollEdgeFade` aus. Anlässe höchstens zwei Zeilen, Filter genau eine. Chips messen in jedem Zustand gleich; nie Umbruch im Chip ([spec/suche.md](spec/suche.md), [spec/atmen-tab.md](spec/atmen-tab.md)).
- Ausnahme `FilterChipFlow` (Wochentage der Erinnerung in einer Karte): dieselben `TonalChip`s brechen zwischen ganzen Chips um statt seitlich zu scrollen – alle sieben bleiben sichtbar, keine Kartenkante schneidet einen an.
- **Prüfung:** `ScreenshotTest` (`chipLayout`, `chipRows`, `filterRow`, `CHIP_UMBRUCH`), `no-check-icon`.

#### Antwortreihe (`FinishedPanel` mit `FeelingAsk`: Befinden am Ende)

```
 ✓ Geschafft  labelMedium TERTIARY
 GapSmall
 Spür kurz nach, wie es dir geht.   ← Titel headlineSmall, zentriert
 ListGap 12
 [ ruhiger | wie vorher | unruhiger ]  ← SegmentedChoiceRow: drei gleich breite Segmente, ButtonHeight 40, Tippfläche 48, ohne Häkchen
 GapSmall                              (nur beim ersten Mal:)
 Nur auf diesem Gerät gespeichert. …   ← bodySmall TERTIARY, zentriert
 GapSmall · Bilanz · Links · SessionPadding · [Nochmal][Ende]
```

- Keine neue Optik: Die Reihe ist eine `SegmentedChoiceRow` (Einzelwahl aus drei, Fläche `SegmentSelected` + SemiBold, bei Platzmangel gemeinsam bis 85 % kleiner, dann Radioliste); keine vorgewählt, eine Wahl ersetzt die vorige, nie abwählen, nie Pflicht. Breite wie die Knopfreihe darunter (volle Breite im `SessionPadding`). Bei aktivierenden Übungen „wacher · wie vorher · müder“.
- Erscheint nur, wenn ein Logbuch-Eintrag entstand (`feelingOptions` nicht leer) und die Einstellung es will; der Hinweis des ersten Mals bleibt für diesen Ende-Screen stehen. Passt der Ende-Screen nicht in die Höhe (Schrift 2,0), scrollt er; mittig bleibt er, solange alles passt.
- **Prüfung:** `ScreenshotTest` (`09_session_ende_befinden*`, `10_meditation_ende_befinden`: `WORTBRUCH`, `TIPPFLAECHE`, `BESCHNITTEN`), `no-check-icon`; manuell R-A11Y (Gruppe „Befinden“, Auswahl angesagt).

#### Kurvenkarte des Editors (`CurveCard`, `BreathCurveEditor`, `CurvePreviewControls`)

```
 Box · 16 s je Atemzug · 3,8/min          [intensiv]   ← Werte-Zeile, eine Textzeile, nicht klickbar
 GapSmall
 ┌ CurveCard surface · ohne Polster ────────────────────┐
 │  voll ────────────────○──────────                     │  Höhe aus BreathCurveHeights (min CurveSpanMin … CurveSpan)
 │      ╱       ╲ · · · ╲                                │
 │  leer ─●──────────────────●─── 0  5  10 s             │
 │ (▶) ← Play bündig links an der Zeitachse   Ton (🔊) → │  PreviewRow genau MinTouchTarget hoch
 └──────────────────────────────────────────────────────┘
```

- Werte über der Kurve (nicht klickbar, Reserven gegen Springen), Kurve, am unteren Kartenrand Play bündig links am Beginn der Zeitachse (`flushStart`, `CardPadding`) und Ton bündig rechts (`flushEnd`, `GapSmall` wie das Ende der letzten Zahl); Mindesthöhe aus `BreathCurveHeights` ([spec/muster-editor.md](spec/muster-editor.md)).
- **Prüfung:** `DeviceFitTest` (`KOMPAKT_FEHLT`), `ScreenshotTest` (`14_muster_editor_*`).

#### Knopfleiste unten (`PinnedActionBar`: `StartSplitButton`; Editor: `CompactStepper`, `SlidingReset`, `StartPlayButton`)

```
 ┌ PinnedActionBar surface · Elevation 3 · Rand ScreenPadding × ListGap ─────────────────┐
 │ [ Starten · ca. 5 min ····· füllt den Rest ·····] GapSmall [↻ 40]   (Reset nur bei Abweichung)│
 │ (−) 5 min (+) ·································· [↻ 40] [▶ 40]   ← Editor: ein Token, 40 dp │
 └──────────────────────────────────────────────────────────────────────────────────────────┘
```

- Feste Breiten für Stepper und Reset, Start füllt den Rest; Start und Reset gleich hoch über dieselbe Mindesthöhe (`ButtonHeight`), nie `IntrinsicSize.Min`; Session-Steuerung 76/56 dp fest. Bei Platzmangel `InlineOrBelow`: Steuerung rechtsbündig darunter, entschieden mit Reserven.
- **Prüfung:** `ScreenshotTest` (`TIPPFLAECHE`, Leiste einzeilig bis 1,3 im `DeviceFitTest`).

#### Suchpille, Tab-Leiste, Bottom Sheet, Overlay-Kopf

| Baustein | Anatomie | Prüfung |
|---|---|---|
| **Suchpille** (`SearchField`) | Pille `CircleShape`, Höhe ≥ 48, Fläche `surfaceContainerHigh`, ohne Rand; Lupe `IconDefault` links mit `CardPadding`, `ListGap` zum Text `bodyMedium`; X als `AppIconButton` nur mit Eingabe, Kreis `CardPadding` vor dem Ende (Tippfläche ragt um `CircleOverhang` darüber), Platzhalter hält seine Breite. | `ANATOMIE` (Steuerzeile ≥ 48), `TIPPFLAECHE` |
| **Tab-Leiste** (`AppNavigationBar`, `TabBarToggle`) | M3 `NavigationBar` 80 hoch, Trennlinie `BorderThin` darüber, Auswahl tonal; Lasche 56 × 18 mittig auf der Oberkante der untersten Leiste, Tippfläche 56 × 48, drei Viertel über der Kante. | `TabNavigationTest`, `ScreenshotTest` (`TabFrame`) |
| **Bottom Sheet** (`TemplateSheet`, `SoundSheet`) | ganz geöffnet, Fläche `surface`, eigener Griff, Titel als `SectionHeader` mit `top = 0`, Kopf → Inhalt `HeaderGap`, seitlich `ScreenPadding`, unten `ScreenBottom` über den Systemleisten; sofort wirksam. | `ScreenshotTest` (`*_offen`), R-A11Y |
| **Overlay-Kopf** (`OverlayChrome`) | Rand `SessionPadding`; links Minimieren, rechts Stille-Schalter und Ton, alle als Kreise `OverlayIconButton` 40 in Tippfläche 48, ein Token; Titel `titleSmall` dazwischen; blendet in der Ruhe-Ansicht aus, Platz bleibt. | `ScreenshotTest` (`09_*`, `10_*`), `pane-title` |

## <a id="komponenten"></a>5 Komponenten

### 5.1 Katalog (`src/ui/components/`, `src/ui/navigation/`, `src/ui/session/`, `src/ui/meditation/`, `src/ui/detail/`)

Je Zeile Nutzen, Nicht-für und A11y-Pflicht; wie etwas im Detail gebaut ist, steht in der KDoc und in [docs/spec/](spec/). Jede öffentliche `fun` in `ui/components/` kommt hier vor (`catalog-sync`).

| Komponente | Nutzen für | Nicht für | A11y-Pflicht |
|---|---|---|---|
| `CardHeader` (`CardHeader.kt`) | Titelzeile jeder Karte mit Chips, Zeichen oder bis zu zwei Aktionen – die Anatomie „Karte mit Aktionen“ (§4.5) an einer Stelle; mit `eyebrow` die Kopfzeile in der Karte (Tagesmuster). Test-Tags `CARD_HEADER_TAG`, `TITLE_ROW_*`, `CONTROL_ROW_TAG` für das Gate `ANATOMIE` | Abschnittsköpfe (→ `SectionHeader`); Nachbau mit `align(TopEnd)` | Titel über `TitleWithChips` (Name wörtlich); Aktionen tragen ihren Namen am Icon |
| `AppIconButton` (`Controls.kt`) | jede Icon-only-Aktion: Kreis `IconButtonSize`, Icon `IconDefault`, Tippfläche 48; `colors` wie M3 (Stepper gefüllt) | Aktionen mit Text (→ `AppTextButton`); Zustand (→ `AppIconToggle`) | cd am `Icon` (ICON-02), Rolle Button |
| `AppIconToggle` (`Controls.kt`) | Icon-only-Umschalter mit Zustand (◉ „In Eigene speichern“, `PersonalMarkToggle`); Zustand zeigt das Icon (gefüllt/leer) | Ton (→ `SoundSwitch`); Einstellungen (→ `ToggleRow`) | cd am `Icon`, `Role.Checkbox`, `stateDescription`, `liveRegion = Polite` nur für Wechsel von außen; `announce = false`, wenn ein anderer Knopf den Wechsel schon ansagt (A11Y-04) |
| `AppTextButton` (`Controls.kt`) | Text-Aktion ohne Fläche (Laden, Wählen, Dialog), Mindesthöhe `ButtonHeight`, `shapes.large` | Primäraktion (→ `StartSplitButton`); gleichrangige Aktionen (→ `AppOutlinedButton`) | Text = Name |
| `AppOutlinedButton` | gleichrangige Aktionen nebeneinander (Exportieren · Importieren in `AdaptiveButtonRow`); Rand `outline` 1 dp, Beschriftung `primary`, mit `color`/`borderColor`/`dashed` für Bedeutung (Editor „+ Ein / + Halten / + Aus“), `compact` mit Polster `ListGap` | Auswahl (→ `SegmentedChoiceRow`); Session (→ `SessionSecondaryButton`) | Text = Name; kurze Beschriftung mit vollem Namen über `contentDescription` |
| `BackButton` | Zurück-Pfeil im `PushHeader` (AutoMirrored, über `AppIconButton`, Hülle `flushStart()`) | Overlays (dort Minimieren); im Scroll-Inhalt | cd `action_back` |
| `BreathingCircle` / `breathingDiscDiameter` | Atem-Pacer (`fraction` 0…1 → Skala 0,42…1,0) und sein Durchmesser für Nachbarn | dekorative Kreise | Aufrufer setzt Name, `onClickLabel`, Zustand, Fokus (A11Y-07) |
| `Chip` / `EvidenceChip` / `CautionChip` / `InfoChip` / `CharacterChip` (`Chips.kt`) | nicht klickbare Info-Pillen, `shapes.extraSmall`, Text `labelSmall` einzeilig (verkleinert bis `CHIP_MIN_SCALE`): Studienlage (gefüllt, nur Detailseite), „nur gesund & ausgeruht“ (Umriss `EvidenceCaution`), „zum Nachlesen“ (Programme, Umriss neutral), Charakter („intensiv“ gefüllt `WarnAmber`; „ruhig“/„sanft aktivierend“ neutral); Reihenfolge Studienlage → Vorsicht → zum Nachlesen; immer in `TitleWithChips` oder der Familienzeile | Auswahl (→ `SegmentedChoiceRow`) | Text Pflicht; `EvidenceChip` liest TalkBack als „Studienlage: …“ (`spokenText`) |
| `LinkChip` / `ExerciseChips` (`LinkChip.kt`) | antippbarer Verweis auf eine Übung (Atemlehre, Pranayama); M3 `SuggestionChip`, sichtbar `ChipHeight`, Tippfläche 48, Beschriftung einzeilig bis 85 %, sonst zweizeilig zwischen den Wörtern; `ExerciseChips(ids, exercises, onOpenExercise)` für mehrere | Info ohne Aktion (→ `Chip`); Aktion ohne Ziel-Inhalt (→ `AppTextButton`) | Text = Name, Rolle Button |
| `ChoiceChip` / `ChoiceChipRow` / `TonalChip` / `FilterChipRow` / `FilterChipFlow` (`ChoiceChip.kt`) | Anlass-Chips (40 dp, Rand, eine Antwort, höchstens zwei Zeilen) und Filter-Chips (32 dp, tonal, mehrere, eine Zeile); Reihen scrollen bei Platzmangel seitlich mit Verlauf; Test-Tags `CHOICE_CHIPS_TAG`, `FILTER_CHIPS_TAG`. Anatomie §4.5, Details [spec/suche.md](spec/suche.md), [spec/atmen-tab.md](spec/atmen-tab.md) | Einzelwahl aus 2–4 Optionen (→ `SegmentedChoiceRow`); Verweise (→ `LinkChip`) | `Role.Checkbox` über `toggleable`; Reihe `selectableGroup` mit Namen und Traversal-Gruppe, Fokusfolge = Datenfolge |
| `BreathMarkIcon` / `PersonalMarkIndicator` / `PersonalMarkToggle` (`PersonalMark.kt`) | das eine Zeichen „zu mir“, der Atemkreis ◉ (ICON-03): `BreathMarkIcon(filled)` als Icon jedes solchen Schalters – Ring in `onSurface` · `SECONDARY`, gesetzt gefüllt in `secondary` –; persönliche Markierung „tut mir gut“ als Schalter im `PushHeader` der Detailseite (eingebaute Übungen) und Zeichen `IconSmall` auf `ExerciseCard` (nur gesetzt sichtbar); „In Eigene speichern“ am Tagesmuster und im Muster-Editor; beides gesammelt im Tab „Eigene“ | Herz, Stern, Lesezeichen; Sortieren oder eigene Gruppe; das Tab-Symbol „Eigene“ (Person) | cd „Tut mir gut“ bzw. „In Eigene speichern“; Schalter mit `stateDescription` „Markiert“/„Nicht markiert“ bzw. „In Eigene gespeichert“/„Nicht gespeichert“ |
| `DisclosureToggle` | Aufklapper in Karten („Wie funktioniert das?“, „Stimmen zeigen“), Caret `IconInButton`, tippbar über die volle Breite; `enabled = false`, solange der Inhalt offen bleiben muss; `note` („· angepasst“) als Status nach der Anatomie „Titel · Status“ (`TitleWithStatus`), `noteVisible = false` hält seinen Platz | Inhalt mit Teaser (→ `ExpanderSection`) | `stateDescription` „Ausgeklappt“/„Eingeklappt“ inkl. „angepasst“ |
| `ExpanderSection` / `ExpanderCard` (`ExpanderSection.kt`) | aufklappbarer Abschnitt mit Titel `titleMedium` `onSurface`, Teaser (immer ganz) und Detail; ohne Detail schlichter Abschnitt ohne Caret; `ExpanderCard(title, appetizer, initiallyExpanded, header, content)` als Karte mit optionalem `header` (Rhythmusbild) | Einstellungen (→ `DisclosureToggle`); Listen gleichartiger Abschnitte (→ `CollapsibleSection`); nie ein `SectionHeader` direkt über einer einzelnen `ExpanderSection` | `heading()`, `Role.Button`, `stateDescription`; ohne Detail `mergeDescendants` |
| `NavCard` | Karte, die woandershin führt (Kapitel in „Lernen“ mit `titleColor = headingColor(index)`, „Über Atemkraft“, Quellcode, Datenschutz): `CardHeader` mit `ForwardChevron` bzw. „öffnet im Browser“ (`external`) als Zeichen, `HintText` darunter; `Card(onClick)`, ganze Karte Tippfläche | Verweise im Fließtext (→ `ForwardLinkText`) | Titel `heading()`, `Role.Button` per `semantics`; extern „Link öffnen“ |
| `ExerciseCard` / `PatternCard` (`ExerciseCards.kt`) | Übungskarte (Titel mit seltenen Chips, Zeichen „tut mir gut“, Metazeile Takt · Dauer, Kurzbeschreibung, Rand Familienfarbe) und Karte eines generierten Musters (Titel mit `CharacterChip`, Musterzeile, Rand `secondary`, `actionCount` Aktionen auf der Titelzeile; mit `eyebrow` die Kopfzeile des Tagesmusters). Anatomie §4.5 | Verweis-Karten (→ `NavCard`); mehr als zwei Aktionen | Aktionen mit eigener Tippfläche 48 und Namen; Karte `Card(onClick)` |
| `AdaptiveButtonRow` | Button-Reihe: gleich breit → nach Inhalt → Primär oben + Rest → untereinander; `primaryFirst = false` ohne Rangfolge; `leading`/`leadingStacked`/`aboveInline` für eine kurze Beschriftung davor | Aktionspaare mit Split-Button (→ `StartSplitButton`) | Lesereihenfolge bleibt; Beschriftung einzeilig |
| `InlineOrBelow` | zwei Teile einer Zeile nebeneinander, sonst `end` rechtsbündig darunter; `leadingReserve`/`endReserve` halten die Entscheidung für den breitesten Inhalt fest | Button-Reihen (→ `AdaptiveButtonRow`) | Reserven ohne Semantik und Fokus |
| `widestOf` / `rememberWidestTextWidth` / `rememberWidestAnnotatedWidth` (`ReserveMeasure.kt`) | Platz für den breitesten möglichen Inhalt (LAYOUT-03, „nichts springt“): unsichtbar messen statt `alpha(0f)` von Hand | – | Messinhalt ohne Semantik |
| `rememberFirstLineHeight` (`TextBlocks.kt`) | Höhe der ersten Textzeile einer Rolle, gemessen – für Aktionen und Zeichen mittig auf der ersten Zeile (§4.5) | `lineHeight` in dp rechnen (ab Android 14 falsch) | – |
| `DotSeparatedFlow` | Teile mit Trennpunkt, gierig je Zeile, zentriert; der Punkt nur zwischen Teilen einer Zeile (Session-Kopf, Bilanz) | fertige Texte (→ `splitAtDots`, TEXT-10) | – |
| `PhaseMark` / `HoldDash` (`PhaseMark.kt`) | Marke einer Atemphase in Phasenfarbe (Halten gestrichelt); Farbe der freien Folge über `phaseColor` | große Kurve (→ `BreathCurveEditor`) | dekorativ, Wert im Text daneben |
| `SlidingReset` | Zurücksetzen, das nur bei Abweichung hereingleitet (Split-Button, Editor-Leiste `compact`); `onFocusHandoff` gibt den Fokus an den Nachbarn | – | cd „Zurücksetzen“ am Icon |
| `selectedRow` (`SelectedRow.kt`) | Auswahl einer Listenzeile ohne Häkchen: gewählt mit Fläche und dünnem Rand (Vorlagen, Ablauf, vāyu-Liste) | Einzelwahl in Reihen (→ `SegmentedChoiceRow`) | `selectable`/`toggleable` dazu |
| `MiniNowPlayingBar` | minimierte laufende Session über der NavigationBar; Titel über `WholeWordText`, Status einzeilig | andere Meldungen | ≥ 48 dp, ein Fokus-Stopp, `Role.Button` per `semantics` |
| `OverlayChrome` | Rahmen jedes Vollbild-Overlays: Minimieren, Stille-Schalter, Ton; `chromeVisible` für die Ruhe-Ansicht ([spec/session.md](spec/session.md)) | Push-Screens | `title` als `paneTitle`; Schalter `Role.Switch` mit `stateDescription`, ohne Live-Region |
| `SoundSwitch` / `IconSwitch` (`SoundSwitch.kt`) | Icon-only-Schalter: Lautsprecher (Overlays, Vorschau des Editors) bzw. beliebiges Icon mit Zustand (`ic_meditation` „Danach in die Stille“); aus in `TERTIARY`, Kreis `IconButtonSize` bzw. `OverlayIconButton` | Zustände mit Fläche oder Text (→ `AppIconToggle`, `ToggleRow`) | Name am `Icon`, `Role.Switch`, `stateDescription` „An“/„Aus“ |
| `ReferenceItem` | eine Quelle (Zitat, DOI/PMID) | – | Kennung ≥ 4,5:1 |
| `ScreenHeader` / `SettingsAction` / `TabListScaffold` / `ScreenSubtitle` (`ScreenHeader.kt`) | fester Kopf der Tabs (dieselbe Zeile wie `PushHeader`, rechts höchstens eine Aktion: das Zahnrad `SettingsAction`); Gerüst der Tabs mit Liste und des Logbuchs; Einleitung einer Push-Seite (`bodyMedium` `SECONDARY`) | Push-Screens (→ `PushHeader`); Untertitel im Tab-Kopf; Meditations-Tab (eigenes Gerüst) | Titel `heading()` und `paneTitle` (A11Y-07); Aktion cd am `Icon` |
| `SearchField` | einzeiliges Suchfeld als Pille mit einem `TextFieldState` des Aufrufers (Anatomie §4.5, [spec/suche.md](spec/suche.md)) | gespeicherte Eingaben; mehrzeiliger Text; ein zweiter Textzustand | X cd „Eingabe löschen“; Platzhalter als Name des Felds |
| `PushHeader` / `PushScaffold` (`PushHeader.kt`) | fester Kopf jeder Push-Seite (`BackButton`, Titel oder `titleContent`, rechts höchstens eine `action`, Icon bündig) und das Gerüst mit Scroll-Spalte, `SectionGap` oben, optional `bottomBar`; `contentModifier` für Sprungziele | Tab-Screens (→ `ScreenHeader`); Seiten mit Liste (→ `TabListScaffold`); Muster-Editor (eigenes Gerüst) | Titel `heading()`; `paneTitle` an der `HeaderBar`; Pfeil cd `action_back` |
| `SectionHeader` / `CollapsibleSection` / `GroupLabel` / `headingColor` (`SectionHeader.kt`) | Abschnittskopf `titleLarge` `onSurface` mit optionalem Teaser, Status (`statusDescription`, `smallStatus`), `expanded` (Caret, MUSTER-03), `onClick` (Chevron, öffnet etwas) und genau einer `trailingAction`; `CollapsibleSection` setzt Kopf → Inhalt `HeaderGap` zentral und animiert nur den Inhalt; `GroupLabel` das kleine farbige Label eine Ebene darunter; `headingColor(index)` der Kopf-Ring für `NavCard`. Anatomie §4.5 | Kartentitel (→ `CardHeader`); Aktionen für eine Karte; Meditations-Tab (LAYOUT-08) | `heading()`; aufklappbar `Role.Button` und `stateDescription`; mit `onClick` eine Schaltfläche aus Titel und Teaser (Punkte als Kommas) |
| `SubLabel` / `Labeled` (`SubLabel.kt`) | Unter-Überschrift in Karten und Abschnitten; `Labeled(label) { … }` setzt sie über einen Block | Abschnittsköpfe | – |
| `SegmentedChoiceRow` / `choiceGroupHeading` | Einzelwahl aus 2–4 kurzen Optionen, mehr als Raster mit `columns`; Segmente `ButtonHeight`, **nie ein Häkchen**, Auswahl `SegmentSelected` + SemiBold; Beschriftungen gemeinsam bis 85 % verkleinert, dann Radioliste | > 4 Optionen in einer Reihe | `groupLabel` als Name des Gruppen-Knotens, sichtbare Überschrift mit `choiceGroupHeading()`; Radioliste `selectableGroup()`, `Role.RadioButton` |
| `SessionAdjustCard` (`ui/detail/`) / `SessionAdjustState` | Dauer und Intervalle vor dem Start; Logik ohne Darstellung (Vorgaben aus `AdjustDefaults.of`, `modified`, `reset()`, `applyStored`, saveable je Übung) | Stufen-Übungen ohne „Phasen anpassen“ (`phasesAdjustable`) | „· angepasst“ im Toggle-Zustand |
| `Stepper` / `CompactStepper` (`Stepper.kt`) | ganzzahliger Wert in festem Bereich mit Label (rechte Spalte, Kreis bündig; mit `horizontal = 0.dp` in einer Karte mit Polster ragt der Rand von „+“ hinein) bzw. ohne sichtbares Label für knappe Zeilen (`valueText`, `flushEnd`); `valueText`/`spokenValue` für Werte, die keine Zahl zeigen (Uhrzeit der Erinnerung „08:15“, gesprochen „8:15 Uhr“) | Freitext | Knöpfe „Label, Erhöhen“, am Anschlag Fokus auf den Gegenknopf; Wert als Live-Region mit Einheit (`valueUnit`, `spokenSuffix`); `announce` steuert sie |
| `BreathCurveEditor` / `BreathCurveHeights` / `CurvePreviewControls` (`ui/patterns/curve/`) | Hauptbedienung des Muster-Editors: freie Phasenfolge als Kurve mit Griffen, Vorschau-Punkt, Höhenspanne ([spec/muster-editor.md](spec/muster-editor.md)) | Phasen eingebauter Übungen; Anzeige in der Sitzung | ein Knoten mit `contentDescription` der Folge; Ziehen nie Pflicht |
| `TimeDial` / `DurationPickerState` (`ui/meditation/`) | Zeitring wie ein Küchenwecker (1–60 min, ∞, Feinwahl) und seine Logik ohne Darstellung ([spec/meditation.md](spec/meditation.md)) | Werte über 60 min; Dauer in Karten (→ `Stepper`) | Schieberegler mit `setProgress`, `stateDescription`; ∞ als `Role.Switch` |
| `WholeWordText` / `badBreakAt` / `breaksInsideWord` (`WholeWordText.kt`) | Titel, Namen, Beschriftungen, die nie im Wort brechen: verkleinert schrittweise (`minScale`, `sharedScale`, `singleLine`, `onDoesNotFit`); die Umbruch-Regel teilt sich der `ScreenshotTest` | Fließtext (→ umformulieren) | wie `Text` |
| `MarqueeText` | einzeiliger Text als Laufschrift, wenn er nicht passt (Seitentitel, Name im Editor); `MARQUEE_TEXT_TAG` | Listen, Karten, Abschnittsköpfe (dort umbrechen) | TalkBack liest den ganzen Text |
| `sanskritText` (`SanskritText.kt`) | Sanskrit kursiv (TEXT-14) aus „*laṅghana*“; zentral in `WholeWordText`, `BodyText`, `Bullet`, Teasern, `ForwardLinkText`; Nutzernamen ohne (`marks = false`) | eigene Auszeichnungszeichen; Kursiv als Betonung | TalkBack und Wortbruch-Prüfung sehen den Text ohne Sternchen |
| `VayuFigure` / `VayusContent` (`ui/atemlehre/VayuFigure.kt`) | Abschnitt „Die fünf *vāyus*“ der Atemlehre: Bild als `Canvas`, Liste als Einzelwahl ([spec/lernen.md](spec/lernen.md)) | Anatomie, Wirkung; Bilder anderer Lehren ohne Quelle | Bild ein Knoten (`Role.Image`), zugänglicher Weg ist die Liste (`selectableGroup`, `Role.RadioButton`) |
| `SessionRunningLayout` | Gerüst laufender Sessions: Kopf, Kreis im freien Platz, Steuerung; `centerExtra` fest reserviert; `chromeVisible = false` blendet über `HideableSlot` aus | andere Screens | der Kreis springt nie |
| `CalmHost` / `rememberAccessibilityActive` (`CalmHost.kt`) | Ruhe-Ansicht beider Overlays (Tipp auf freie Fläche, automatisch nach 5 s, `HideSystemBars`); ob ein Bedienungshilfe-Dienst läuft | eigene Tipp-Logik je Overlay | nie ausblenden, solange TalkBack oder Switch Access läuft; in Pause und am Ende alles sichtbar |
| `HideableSlot` / `inertWhen` (`HideableSlot.kt`) | sanft ausblenden, Platz bleibt, ausgeblendet nicht antippbar und nicht im TalkBack-Baum; `Modifier.inertWhen(hidden)` sperrt Semantik und Fokus (auch die App unter dem Session-Overlay) | dauerhaft unsichtbare Elemente | nie bei aktiver Bedienungshilfe ausblenden |
| `FillViewportColumn` (`FillViewportColumn.kt`) / `HideSystemBars` (`SystemBars.kt`) | Spalte, die den sichtbaren Bereich füllt und erst darüber hinaus scrollt (Meditations-Tab, LAYOUT-08); Status- und Navigationsleiste in der Ruhe-Ansicht ausblenden | Listen (→ `LazyColumn`) | – |
| `AppNavigationBar` / `AppTabBar` / `HideableTabBar` / `TabBarToggle` (`ui/navigation/`) | Haupt-Tabs unten, auch im `ScreenshotHarness`; ein- und ausklappbare Leiste mit Lasche ([spec/navigation.md](spec/navigation.md)) | Push-Ziele | eine Beschriftungsgröße für alle Tabs (TYPO-01), verkleinert bis 85 %, dann nur Icons mit Namen; Auswahl tonal (PRIN-02) |
| `TitleWithChips` | Titel mit nachgestellten Chips (Karten); Chips rutschen bei Platzmangel unter den Titel; `titleModifier` für den Titeltext | Abschnittsköpfe (→ `SectionHeader`) | Titel-Rolle und Farbe vom Aufrufer; keine Chips → `null` |
| `StartSplitButton` / `StartPlayButton` | **jeder** Start einer konfigurierbaren Session (Start in Restbreite, Reset gleitet ein, gleiche Höhe über `ButtonHeight`; Start und Dauer bei Platzmangel in zwei Zeilen ohne Punkt) bzw. runder Start-Knopf in knappen Leisten (Editor: gefüllter Kreis `primary` in `IconButtonSize`) | andere Aktionspaare; Vorschau (→ `CurvePreviewControls`) | Reset Icon-only, cd `adjust_reset`, Fokus → Start, wenn Reset verschwindet; `StartPlayButton` mit vollem Wortlaut |
| `ToggleRow` (`Controls.kt`) | Ein/Aus-Einstellung: Label, Switch in derselben Zeile, Hinweis darunter endet vor dem Switch (Anatomie §4.5) | Auswahl aus mehreren (→ `SegmentedChoiceRow`) | ganze Zeile `toggleable(Role.Switch)`, Name = Label (A11Y-03) |
| `SessionControls.kt`: `GlowText`, `SessionControlBar`, `SessionPrimaryButton`, `SessionSecondaryButton`, `FinishedPanel`, `rememberTapFlash`; `PauseFlash`; `CircleTapTarget` | Text, Buttons und Tap-Rückmeldung **nur** in Vollbild-Sessions ([spec/session.md](spec/session.md)) | normale Screens, stehender Status | `FinishedPanel`: `paneTitle` „Geschafft“, Fokus auf „Nochmal“, Antwortreihe als `SegmentedChoiceRow` mit Gruppenname „Befinden“, scrollt bei großer Schrift statt abzuschneiden; `PauseFlash` dekorativ (`maxSize` Pflicht); `CircleTapTarget` mit Namen, `Role.Button`, `onClickLabel` |
| `SafetyDialog` / `rememberSafetyGate` | Sicherheitsbestätigung vor intensiven Übungen (Hinweisliste scrollt, `Bullet`); das Gate je Übung (`SafetySettings.acknowledgedIds`, MUSTER-06) | Infos ohne Risiko | nicht außen schließbar; Warn-Icon dekorativ |
| `BodyText` / `HintText` / `SecondaryText` / `WarnText` / `CardTitle` / `Bullet` (`TextBlocks.kt`) | Textrollen außerhalb der Köpfe, Farbe aus `LocalContentColor`: Fließtext `bodySmall` `BODY` (Kurzbeschreibung `bodyMedium`); Hinweis `SECONDARY` über `WholeWordText`; Sekundärtext mit normalem Umbruch (Kurz- und Musterzeile, Teaser, Trefferzahl); Warnzeile „⚠ …“ in `WarnAmber`; Kartentitel `titleMedium` (Glossar, Einstellungen); Aufzählungspunkt | Köpfe (→ `SectionHeader`); Einleitung (→ `ScreenSubtitle`) | `CardTitle` ist `heading()` (`semantics-heading`); Bullet-Glyph `invisibleToUser()` |
| `ConfirmIconButton` / `rememberArmedConfirm` (`ConfirmAction.kt`) | zerstörende Aktion zweistufig im selben Element (MUSTER-05): Papierkorb `SECONDARY` → `ic_delete_forever` in `error`, Rückfall nach 4 s, nicht bei Fokus oder aktiver Bedienungshilfe | Abbruch eines Downloads; sofort umkehrbare Aktionen | cd mit Namen, bestätigt „‚X‘ wirklich löschen?“; `liveRegion = Polite` am Knopf; in Listen `key(id)` |
| `PinnedActionBar` | gepinnte Start-Leiste unter dem Scroll-Inhalt (`surface`, `PinnedBarElevation`, Rand `ScreenPadding` × `ListGap`); meldet ihre Oberkante dem `TabBarState` (Anatomie §4.5) | Aktionen mitten im Inhalt | Inhalt `StartSplitButton` |
| `ForwardChevron` / `ForwardLinkText` / `ExpandCaret` / `expandedStateText` (`ExpandCaret.kt`) | Chevron „führt weiter“ (`IconInButton`, AutoMirrored) hinter einem Verweis; Verweistext mit Chevron an der Endkante in einem `AppTextButton`; Caret aufklappbarer Köpfe; Zustandstext „Ausgeklappt“/„Eingeklappt“ | externe Links (Symbol `ic_open_in_new`) | dekorativ, den Namen trägt die Zeile |
| `accentBorder` / `dashedBorder` (`CardBorder.kt`) | Kartenrand `BorderThin` mit `ALPHA_BORDER` in der Akzentfarbe; gestrichelter Rand (Halten, „Phase hinzufügen“) | eigene `border(…)`-Aufrufe | dekorativ |
| `flushEnd` / `flushStart` (`FlushEdge.kt`) | bündige Kante für Bedienelemente mit unsichtbarem Rand (§4.5 Bündigkeit): meldet sich um `TouchOverhang` schmaler, in Karten höchstens `CardPadding` | `offset` | Tippfläche bleibt 48 dp |
| `color` / `title` (auf `BreathingFamily`), `label` (auf `EvidenceLevel`) (`FamilyPresentation.kt`) | Familienfarbe und -name, Etikett der Studienlage – einmal für Karten, Köpfe, Chips | eigene `when`-Zuordnungen | – |
| `sessionSummaryParts` (`SummaryPart`) / `spokenDuration` / `durationClockText` (`SessionSummaryText.kt`, `domain/`) | Bilanz einer Sitzung aus echten Werten; Dauer sichtbar „6:05 min“, gesprochen „6 Minuten 5 Sekunden“ | eigene Formatierung | jeder Teil trägt seine gesprochene Form (`SummaryPart.spoken`), `FinishedPanel` sagt sie an |
| `feelingText` / `FeelingAsk` (`FeelingText.kt`) | Wort eines Befindens („ruhiger“, „wie vorher“, „wacher“ …) für Antwortreihe und Logbuch; `FeelingAsk` = Frage des Ende-Screens (`onAnswer`, einmal `intro`) | Farbe, Icon, Zahl oder Summe zum Befinden (SEC-LEGAL-04) | Logbuch: gesprochen „danach ruhiger“ |

Feature-Wrapper ohne eigene Regeln: `MiniSessionBar`, `SessionOverlay` (`ui/session/`), `MiniMeditationBar`, `MeditationOverlay` (`ui/meditation/`). Entfallen: `SelectChip` (2026-10-19), `AccentBar` (2026-10-24), siehe [DECISIONS.md](DECISIONS.md).

### 5.2 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| KOMP-01 | MUSS | Gibt es eine Komponente in §5.1, wird sie genutzt; kein Inline-Nachbau. Wird ein Muster zum zweiten Mal gebraucht, wandert es nach `src/ui/components/`; jede öffentliche `fun` dort steht im Katalog. | Konsistenz; 15 Duplikate im Audit. | auto: `catalog-sync`; manuell: R-CODE |
| KOMP-02 | MUSS | Klickbare Karten sind `Card(onClick)`, nie `Card(Modifier.clickable)`. Aktionen einer Karte stehen nur auf ihrer Titelzeile (`CardHeader`, §4.5): höchstens zwei, zerstörende außen, bestimmen nie die Kartenhöhe; ein `AppIconToggle` zeigt seinen Zustand gefüllt in `secondary`. Eine Karte, die einen Abschnitt vertritt (Tagesmuster), darf eine Kopfzeile mit Eyebrow und Abschnittsaktionen tragen. | Ripple und Rolle stimmen; keine verschachtelten Ziele; eine Bausprache statt dreier Ausnahmen. | auto: `card-style`, `anatomy-overlay`, `ScreenshotTest` (`ANATOMIE`) |
| KOMP-03 | MUSS | Karte: Default-Farbe, `shapes.medium`, `CardPadding`, Titel `titleMedium`, Text `bodySmall` `SECONDARY`; Rand (`accentBorder`) nur bei navigierenden Inhaltskarten. Trenner in Karten und Expandern nur `HorizontalDivider(color = DividerSubtle)`. Ausnahmen mit Marker `// Abweichung KOMP-03:` – der Marken-Trenner über der Tab-Leiste. | Vorher drei Trenner-Varianten. | auto: `card-style`; manuell: R-VISUAL |
| KOMP-04 | – | entfällt → LAYOUT-04 (M3-Steuerelemente nur in `components/`). | Ein Gegenstand, eine Regel. | auto: `control-direct` |

## <a id="muster"></a>6 Muster

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| MUSTER-01 | MUSS | **Split-Button und Rückmeldung im Element.** Start (`primary`) in voller Restbreite, Reset (tonal, Icon-only) gleitet nur ein, wenn ein Wert vom Default abweicht; gleiche Höhe über `ButtonHeight`, nie `IntrinsicSize.Min`; kein Bestätigungsdialog. Bestätigung ist ein Zustandswechsel des auslösenden Elements (◉ leer → gefüllt mit `stateDescription`, lesbar ≥ 4,5:1, `liveRegion = Polite`); Abweichung als Suffix „· angepasst“ am Element, nie als Badge oder loser Text. | Eine Fläche statt zwei; Reset nur, wenn er etwas bewirkt; TalkBack erfährt den Erfolg. | auto: `no-fab-toast`; manuell: R-VISUAL, R-A11Y |
| MUSTER-02 | – | entfällt → MUSTER-01 (Rückmeldung und Reset im Element). | doppelt | auto: `no-fab-toast` |
| MUSTER-03 | MUSS | **Ausklappen und Übersicht.** `ExpanderSection`: Teaser immer ganz, Detail darunter, Höhe animiert. Lange Listen gleichartiger Abschnitte werden zur Übersicht aus `SectionHeader` mit `expanded` und Teaser, Standard zu, Zustand per `rememberSaveable`. Der Tab „Eigene“ hat drei gleiche Köpfe (Verlauf, Tut mir gut, Eigene Kreationen; [spec/eigene-tab.md](spec/eigene-tab.md)). Ein Suchfeld nur als Zusatz und nur nach MUSTER-09. | Kontext bleibt sichtbar; Sprünge irritieren. | auto: `ContentRulesTest teaser`; manuell: R-VISUAL |
| MUSTER-04 | SOLL | **Leer und Fehler.** Leer: „Noch keine …“ plus sanfte Einladung, ohne Illustration oder Extra-Button (Ausnahmen: Suche ohne Treffer „Nichts gefunden“ mit „Alle zeigen“; Leer-Karte „Eigene Kreationen“ mit dem Textverweis „Muster gestalten ›“). Fehler inline im Element: was passiert ist und was du tun kannst; kein `e.message` in der UI. | Nutzer wissen, was zu tun ist; Exception-Texte sind technisch. | auto: `exception-message-ui`; manuell: R-TEXT |
| MUSTER-05 | MUSS | **Zerstörende Aktion** (Logbuch, gespeicherte Muster, Stimmen): zweistufig im selben Element über `ConfirmIconButton` bzw. `rememberArmedConfirm` – erster Tipp morpht in den Bestätigungszustand (`error`, cd „‚X‘ wirklich löschen?“, `liveRegion`), zweiter löscht; Rückfall nach 4 s, nicht bei Fokus oder aktiver Bedienungshilfe. Kein Dialog, keine Snackbar. | Schutz vor Fehltipp ohne Dialog (SEC-PRIV-04). | manuell: R-A11Y |
| MUSTER-06 | MUSS | **Sicherheitsdialog** nur für Übungen mit `caution` (Wim Hof, Feueratmung) und intensive eigene Muster: M3 `AlertDialog`, Warn-Icon, Hinweisliste scrollbar, „Abbrechen“ links, „Verstanden, starten“ rechts, nicht außen schließbar; **je Übung** bis zu ihrer ersten Bestätigung (`SafetySettings.acknowledgedIds`), mit „Sicherheitshinweis immer zeigen“ (Standard an) jedes Mal. | Bewusste Bestätigung; eine Bestätigung bei der Feueratmung sagte nichts über Wim Hof. | auto: `PatternEditorSafetyTest`; manuell: R-A11Y |
| MUSTER-07 | MUSS | **Session.** Steuerung nur `SessionControlBar` (Icon-only, feste Kreise), alles per Kreis-Tipp auch als Button; laufende Sitzung minimiert immer als `MiniNowPlayingBar`, app-weit (`PlaybackService`); Overlay mit `paneTitle`, App darunter inert; Ruhe-Ansicht blendet aus, nie mit TalkBack; „Danach in die Stille“ je Übung umschaltbar; Ende „✓ Geschafft“ mit Bilanz aus echten Werten, „Nochmal“/„Ende“, „Im Logbuch ansehen ›“; Logbuch ab einer Minute aktiver Zeit, ein neuer Start ersetzt den laufenden. Details: [spec/session.md](spec/session.md). | Bedienbar ohne Kreis-Geste; eine laufende Session ist nie unsichtbar. | auto: `ScreenshotTest` (`09_*`, `10_*`), `PlaybackFlowTest`; manuell: R-A11Y, R-AUDIO |
| MUSTER-08 | MUSS | **Tabs und Zurück.** Feste Köpfe (`PushHeader`, `ScreenHeader`); Tipp auf den markierten Tab führt zu dessen Startseite, ein anderer Tab stellt seinen Stapel wieder her, markiert ist immer der Tab des Stapels (`tabOnStack`); Logbuch als Push-Seite über „Verlauf“ und „Im Logbuch ansehen ›“; Tab-Leiste auf den vier Tabs immer, in der Tiefe eingeklappt mit Lasche `TabBarToggle`, keine Scroll-Messung. Details: [spec/navigation.md](spec/navigation.md). | Aus den Einstellungen kam man nur über den gescrollten Zurück-Pfeil heraus. | auto: `TabNavigationTest`; manuell: R-VISUAL |
| MUSTER-09 | MUSS | **Suche** nur, wo sie über eine Übersicht hinaus hilft (Befinden im Tab „Atmen“): Block „Wie fühlst du dich?“ aus Suchpille und sechs Anlass-Chips ohne Kasten; gewählt ist ein Anlass, eine Suche oder ein Bereich – oder nichts; Chips verrutschen nie; Trefferstatus als entprellte Live-Region; ohne Treffer „Nichts gefunden“, nie eine geratene Empfehlung; Suchlogik rein in `src/domain/` mit kuratierten Laien-Begriffen, ohne Embeddings oder Tippfehler-Suche; Eingabe flüchtig (SEC-PRIV-06). Details: [spec/suche.md](spec/suche.md). | Die Suche soll halten, was sie verspricht; „müde“ darf nie bei Wim Hof landen. | auto: `ContentRulesTest health-claims`, `ScreenshotTest` (`01_atmen_suche_*`, `01_atmen_anlass_offen`, `01_atmen_bereich`); manuell: R-TEXT, R-A11Y |
| MUSTER-10 | MUSS | **Muster-Editor:** ein eigenes Muster ist eine freie Phasenfolge (1–30 s, 2–12 Phasen, mindestens ein Ein und ein Aus); Kopf mit Name und Startpunkt, Werte, Kurvenkarte mit Vorschau, Werkzeugleiste und Leiste unten stehen ohne Scrollen da (bis fontScale 1,3), es ist immer eine Phase gewählt, eine ungültige Folge lässt sich nicht einstellen, „intensiv“ warnt (MUSTER-06); Speichern über ◉ „In Eigene speichern“, „Starten“ probiert ohne Speichern. Details: [spec/muster-editor.md](spec/muster-editor.md). | Frei gestalten statt Wunsch-Generator; Werkzeugleiste und Ablauf halten alles ohne Ziehen bedienbar. | auto: `FreePatternTest`, `PatternEditorSafetyTest`, `DeviceFitTest` (`KOMPAKT_FEHLT`), `ScreenshotTest` (`14_*`); manuell: R-A11Y |
| MUSTER-11 | MUSS | **Bottom Sheet** nur für kurze Nebenoptionen einer Seite, deren Hauptelement ruhig stehen bleiben soll (Startpunkt im Editor, Klang & Stimme in der Meditation); sofort wirksam, kein Bestätigen; Auslöser ist ein Element der Seite, das die aktuelle Wahl zeigt. Nicht für Inhalte, lange Listen oder Eingaben mit Bestätigen. Anatomie §4.5; Inhalt [spec/meditation.md](spec/meditation.md). | Ein Blatt legt sich darüber, ohne das Layout zu ändern (S-40). | auto: `ScreenshotTest` (`*_offen`); manuell: R-A11Y |
| MUSTER-12 | MUSS | **Filterreihe** über der Übungsliste nur im Grundzustand: eine Zeile `TonalChip`s, nur Filter quer zu den Familien („Bis 3 min“, „Ohne Halten“, „Aus dem Yoga“; ein Filter teilt die Übungen sinnvoll, höchstens fünf, nie Studienlage), UND-verknüpft, Familien ohne Treffer fallen weg, das Tagesmuster darüber bleibt, Trefferstatus als Live-Region. Details: [spec/atmen-tab.md](spec/atmen-tab.md). | Wer wenig Zeit hat oder nicht die Luft anhalten mag, sucht quer zu den Familien. | auto: `ExerciseFilterTest`, `ScreenshotTest` (`01_atmen_filter`); manuell: R-A11Y |
| MUSTER-13 | MUSS | **Erster Start: eine Karte, kein Onboarding.** Die Willkommenskarte („Neu hier?“, Einstieg, Verweis auf die Resonanz-Atmung, Gesundheitshinweis) steht oben im Atmen-Tab, bis sie weggeklickt ist (DataStore); kein Dialog, keine zweite Seite, kein Fokus beim Öffnen. Details: [spec/atmen-tab.md](spec/atmen-tab.md). | Einsteiger fanden weder Einstieg noch Gesundheitshinweis; ein Wizard widerspräche dem ruhigen Charakter. | auto: `ScreenshotTest` (`01_atmen_willkommen`); manuell: R-TEXT, R-A11Y |

## <a id="icons"></a>7 Icons

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| ICON-01 | MUSS | Icons aus `material-icons-core`, Stil `Filled` (`AutoMirrored` für Richtungen), oder eigener Vektor `res/drawable/ic_<name>.xml` (24-dp-Viewport, einfarbig). Kein `material-icons-extended`, kein `Outlined`/`Rounded`/`Sharp`/`TwoTone`. Größen nur `IconDefault`/`IconInButton`/`IconSmall`. Bediensymbole („−“, „+“, „›“) sind Icons, keine Text-Glyphen. | APK-Größe, eine Linie, RTL, TalkBack liest Glyphen vor. | auto: `icon-source`, `layout-literal` |
| ICON-02 | MUSS | Icon-only-Elemente setzen `contentDescription` **am `Icon`** über `R.string.cd_*`/`action_*`, benannt nach der Aktion ohne Typwort. `contentDescription = null` nur bei dekorativem Icon oder Text im selben Knoten, mit `// dekorativ: <Grund>`. | Ein Muster statt zwei; prüfbar. | auto: `cd-style` |
| ICON-03 | MUSS | **Ein Zeichen, eine Bedeutung.** Der Atemkreis ◉ (`BreathMarkIcon`, `ic_breath_mark`/`_filled`) ist das einzige Zeichen für „zu mir“: „Tut mir gut“ und „In Eigene speichern“. Tab-Symbole bezeichnen Orte: Atmen `ic_breathe` (Material „Air“), Eigene `Icons.Filled.Person` (nur dort), Meditation `ic_meditation` („Timer“), Lernen `ic_learn` („Menu Book“). Kein Lesezeichen, Herz oder Stern. | Drei Speicher-Zeichen für zwei Bedeutungen ließen raten, was wohin gehört (DECISIONS 2026-10-24). | manuell: R-VISUAL |

## <a id="bewegung"></a>8 Bewegung

**Tokens** (`src/ui/theme/Motion.kt`): `TapFlashHold` 220 ms, `TapFlashFadeStep` × `TapFlashFadeSteps` 34 ms × 16, `TapFlashStatic` 500 ms (bei Animator-Skala 0), `NavPush` 300 ms Slide + Fade (einmal in `pushComposable<T>()`), `NavTab` 180 ms Fade, `CalmFade` 500 ms (Ruhe-Ansicht), `ControlFade` 300 ms (Session-Knöpfe nach der Vorbereitung), `VayuFlow` 2400 / `VayuFlowSelected` 1100 ms (Strömungslinien), `VayuPulse` 2600 ms (`EaseInOutSine`), `SearchStatusSettle` 600 ms (Trefferzahl der Suche), `Disclosure` (Default-Spring für Größe: `animateContentSize`, `expand…`/`shrink…`) mit `Reveal` (Default-Spring für `fadeIn`/`fadeOut`), `AmbientShimmer` `tween(8000, EaseInOutSine)` reverse (Schein des Atemkreises, Glow-α ≤ 0,16).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| MOTION-01 | MUSS | Kreis, Countdown und Ton leiten sich aus der Session-Uhr ab, nie aus einer Animations-API. Nach Pause setzt auch der durchgehende Ton an der aktuellen Position fort. | Synchron, Pause friert alles ein, „Animationen entfernen“ bricht nichts. | manuell: R-CODE, R-AUDIO |
| MOTION-02 | DARF NICHT | Nichts blinkt schneller als 3 Hz; keine rotgesättigte Fläche mit Helligkeitswechsel; Pacer-Phasen < 0,5 s. Dekorative Endlos-Animationen laufen über Compose-APIs (stoppen bei Animator-Skala 0) mit Alpha-Änderung ≤ 0,1. Tap-Flash zeigt bei 0 statisch. Ausnahme vāyu-Bild: gewählte Fläche pulsiert 0,55 ↔ 0,95 in 2,6 s, nur nach Antippen, bei Skala 0 still. | WCAG 2.3.1 / 2.2.2, ruhig. | manuell: R-VISUAL |
| MOTION-03 | SOLL | Phasenübergänge am Kreis sind stetig: Endskala einer Phase = Startskala der nächsten (`CircleFractions` setzt `RuntimePhase.circle`; die Vorbereitung zeigt den Start der ersten Phase); Stufen (Viloma) über `Phase.level`, der Kreis wächst stufenweise, der durchgehende Ton folgt demselben Füllstand. | Sprünge wirken wie ein Blitz. | manuell: R-VISUAL |
| MOTION-04 | SOLL | Dauern und Easing aus `Motion.*`; neue UI-Bewegung ≤ 300 ms, Standard-Easing oder Default-Spring, kein Bounce. | Zentral, ruhig. | auto: `motion-literal` |

## <a id="audio"></a>9 Audio und Haptik

**Ist-Werte** (`src/cue/`): Synthese 44,1 kHz PCM16 mono zur Laufzeit. Wechselton 528/396/440 Hz (Ein/Aus/Halten), 180 ms, Attack 12, Release 60 ms. Gong Teiltöne 330/331,6/894/1698 Hz, „Lang“ 11,5 s + 1,5 s Stille, „Kurz“ 6 + 1,5 s. Durchgehend 196 ↔ 294 Hz, Tonhöhe nach dem Füllstand des Kreises, Ein- und Ausblende 60 ms. Lautstärke Ton/Gong 0,9/1,4/1,85, Dauerton 1,2/2,0/3,2. Sprache System-TTS de-DE (Rate 0,85, Pitch 0,9) oder Piper (Speed 0,9). Haptik-Raste: Primitive `TICK` 0,6 bzw. `CLICK` 1,0, sonst `EFFECT_TICK`/`EFFECT_HEAVY_CLICK`, sonst One-Shot 30/60 ms; gedrosselt (Tick frühestens 40 ms nach einem Tick, 80 ms nach einem Klick). Ein/Aus 110 ms, Halten 70 ms. Fokus `GAIN_TRANSIENT_MAY_DUCK`, `USAGE_MEDIA`. Defaults: Töne, Haptik an, Zäsur MEDIUM, Lautstärke MEDIUM, Gong lang.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| AUDIO-01 | MUSS | Töne werden offline synthetisiert; neue Audio-Assets nur mit freier Lizenz (in den Credits). Frequenzen gleichstufig begründet, keine Solfeggio- oder „Heil“-Frequenzen. Jeder Ton hat Attack ≥ 8 ms, Release ≥ 60 ms, Spitze ≤ 0,95 FS. | Offline, F-Droid, evidenz-ehrlich, keine Klicks. | manuell: R-CODE, R-AUDIO |
| AUDIO-02 | MUSS | Ausgabe über `USAGE_MEDIA`. Audio-Fokus nur, wenn etwas hörbar ist, in der Pause abgegeben, Fokusverlust per Listener behandelt. Der Gong klingt vollständig aus; Fokus/Service enden erst nach `gongTotalMs()` plus Reserve. | Fremde Musik bleibt sonst geduckt; abgeschnittener Gong. | manuell: R-AUDIO |
| AUDIO-03 | MUSS | Sprache schweigt bei Medienlautstärke 0, bricht vor dem End-Gong ab, überlappt nicht (`QUEUE_ADD`). | Ruhe. | manuell: R-AUDIO |
| AUDIO-04 | MUSS | Audio und Haptik hängen an den app-weiten Abläufen (`MeditationController`, `BreathSessionController`) bzw. am Service, nie an der Composition; Player kommen aus dem `AppContainer`, nie aus `remember { … }`. Beide Sitzungsarten laufen mit dem Foreground-Service `PlaybackService` (`mediaPlayback`, Partial-Wakelock). | Sperrbildschirm; Android 17 stummt Hintergrund-Audio ohne FGS. | auto: `player-in-composable`; manuell: R-AUDIO |
| AUDIO-05 | MUSS | Jedes Session-Ereignis ist auch hörbar oder fühlbar. Session-Vibration folgt dem App-Schalter „Vibration“ als `USAGE_ALARM` (ab API 33 `VibrationAttributes`). Ausnahme: Rasten von Zeitring und Atemkurve sind Bedien-Rückmeldung (`USAGE_TOUCH`, Android regelt an/aus). Phasen unterscheiden sich in der Dauer. UI-Haptik nur ohne sichtbare Rückmeldung (Kreis-Tipp) und an Rasten; Knöpfe mit Ripple vibrieren nicht. | Bedienbar mit geschlossenen Augen; Samsung verwirft Touch-Ticks bei ausgeschalteter System-Tippvibration. | manuell: R-AUDIO |

## <a id="a11y"></a>10 Barrierefreiheit

Ziel: WCAG 2.2 AA (nach WCAG2ICT) plus Android-Richtlinien.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| A11Y-01 | MUSS | Jedes Bedienelement hat Name, Rolle und Zustand: klickbare Nicht-M3-Elemente mit `role` und, wenn nötig, `onClickLabel`; Icon-Umschalter mit `Role.Switch` und `stateDescription`. Screen-Titel, `SectionHeader`, Kartentitel tragen `heading()`, zentral in der Komponente. | WCAG 4.1.2, 1.3.1. | auto: `semantics-required`, `semantics-heading`; manuell: R-A11Y |
| A11Y-02 | MUSS | Semantik nicht zerstören: kein `clearAndSetSemantics` auf Klickbarem, außer `onClick(label)`, `role`, `disabled()` werden neu gesetzt (Marker `// Abweichung A11Y-02:`); eine Container-cd verdeckt keine dynamischen Kind-Texte. | Sonst fehlen Klick, Rolle oder Restzeit. | auto: `a11y-ratchet`, `ScreenshotTest` (`OHNE_KLICK`); manuell: R-A11Y |
| A11Y-03 | MUSS | **Auswahl ohne Häkchen.** Schalterzeilen: ganze Zeile `toggleable(role = Role.Switch)`, `Switch(onCheckedChange = null)`, Zeilentext = Name. Einzelwahl: `SegmentedChoiceRow` (Fläche `SegmentSelected` ≥ 3:1 + fette Beschriftung), `Role.RadioButton`, `selectableGroup()`, verknüpftes Gruppenlabel. Chips: Fläche `SegmentSelected` plus Rand (`ChoiceChip`) bzw. volle Beschriftung (`TonalChip`), nie fette Schrift oder Häkchen, misst in jedem Zustand gleich. | WCAG 1.4.1, 4.1.2; fette Schrift änderte die Breite (DECISIONS 2026-10-20/23). | auto: `switch-unlabeled`, `no-check-icon`; manuell: R-A11Y |
| A11Y-04 | MUSS | Zustandswechsel ohne Fokuswechsel (Phase, Pause, Ende, Stepper-Wert, ◉ Speichern, Download, Trefferzahl) haben genau eine `liveRegion = Polite` an einem nicht zusammengeführten Knoten; eine Ansage je Tipp. Sekündlich tickende Werte nicht. | WCAG 4.1.3. | manuell: R-A11Y |
| A11Y-05 | MUSS | Tippflächen ≥ 48 × 48 dp **als Platz im Layout**, unabhängig von der Optik (M3 oder `minimumInteractiveComponentSize()`); die Hit-Test-Erweiterung von Compose zählt nicht. `LocalMinimumInteractiveComponentSize` nie ändern. Ausnahme: Link mitten im Fließtext (WCAG 2.5.8 „Inline“). | Reserviert das Layout weniger, ragen Nachbarn hinein. | auto: `ScreenshotTest` (`TIPPFLAECHE`), `platform-override` |
| A11Y-06 | SOLL | Dekorative Glyphen (· • › ⚠ ✓) sind keine Fokus-Stopps; Listeneinträge sind ein Stopp. Namen tragen Kontext („Stimme Thorsten anhören“); Zeiten und Einheiten ausgeschrieben („4 Sekunden“). | Weniger Rauschen, WCAG 2.4.6. | manuell: R-A11Y |
| A11Y-07 | MUSS | Overlays und Screens setzen `paneTitle` (über `HeaderBar` bzw. `OverlayChrome`); ausblendbare Bedienelemente bleiben sichtbar, solange ein Bedienungshilfe-Dienst läuft; verschwindet ein fokussiertes Element, setzt `FocusRequester` den Fokus. Elemente mit `indication = null` zeigen eigenen Fokus ≥ 3:1 (Marker `// Abweichung A11Y-07:`). Der Atemkreis hat keine Ripple. | WCAG 2.4.3, 2.4.7; D-Pad, Switch Access. | auto: `a11y-ratchet`, `pane-title`; manuell: R-A11Y |

## <a id="text"></a>11 Text und Ton

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| TEXT-01 | MUSS | UI-Chrome inkl. Hilfstechnik-Texten steht in `res/values/strings.xml`; Fachinhalte (Übungen, Situationen samt Suchbegriffen, Refs, Cues, Glossar) als Kotlin-Daten in `src/data/`. Mengen über `<plurals>`; keine fest eingetragenen Zählungen oder Inhaltsnamen in UI-Strings. | Prüfbar; bricht nicht bei Inhaltsänderung. | auto: `ui-literal`, `lintDebug PluralsCandidate`; manuell: R-TEXT |
| TEXT-02 | MUSS | Ein Begriff pro Sache nach der Begriffsliste (§11.1), in UI und Store. Phasen-Labels eindeutig („Halten (voll)“ ≠ „Halten (leer)“). | Anzeige, TalkBack und Ansage unterscheiden sonst nicht. | manuell: R-TEXT |
| TEXT-03 | MUSS | Einsprachig Deutsch: nur `res/values/`; Datum und Zahlen über `java.time` + `DateTimeFormatter` mit `AppLocale` (`de`, `ui/components/`); kein `SimpleDateFormat`, kein `Locale.getDefault()`/`Locale.GERMAN` in `ui/`. Build mit `localeFilters += "de"`, Manifest mit `locales_config.xml`. | Sonst Mischsprache und gemischte Formate. | auto: `locale`, `locale-config` |
| TEXT-04 | MUSS | Anrede „du“, klein. Übungen laden ein statt anzuweisen („Wenn du magst …“). Sicherheit klar und knapp („Bei Schwindel sofort aufhören.“). | Nutzerpräferenz; Sicherheit braucht Klarheit. | manuell: R-TEXT |
| TEXT-05 | MUSS | Laiensprache in Situationen, Suchbegriffen, Labels und Tags: kein „Schub“, „Crash“, „vagal“, „Sympathikus“, „Hormese“, „CO₂-Toleranz“. Fachbegriffe nur im Detail; jeder steht im Glossar. | Verständlich, kein Krankheitsjargon. | auto: `ContentRulesTest glossary-terms`; manuell: R-TEXT |
| TEXT-06 | MUSS | **Teaser** (`effect`, erste `caution`): ein geschlossener Satz mit finitem Verb, endet auf „.“, ≤ 95 Zeichen. **Detail** (`effectDetail`) beginnt nicht mit dem Teaser, vertieft denselben Punkt, SOLL ≤ 700 Zeichen. Keine Baseline. | Nichts darf abgeschnitten wirken. | auto: `ContentRulesTest teaser`; manuell: R-TEXT (finites Verb, Detaillänge) |
| TEXT-07 | MUSS | Evidenz ehrlich: Schwache Evidenz steht schon im Teaser. **Jede eingebaute Übung trägt genau eine Studienlage** (`EvidenceLevel`, nur auf der Detailseite): „gut belegt“ (Meta-Analyse oder mehrere unabhängige RCTs zur Anwendung), „in Studien geprüft“ (ein RCT oder mehrere kleine Studien, dünn oder nur für eine Gruppe), „kaum untersucht“ (Mechanismus plausibel). Hochstufen nur mit neuer Quelle in `Refs.kt`; Teaser, Detail und Stufe widersprechen sich nicht. Generierte Muster haben keine Studienlage. Kein Filter und kein Karten-Chip nach Studienlage. | Kernversprechen der App; ein Etikett nur für die beste Übung ließ die übrigen offen. | manuell: R-TEXT |
| TEXT-08 | MUSS | Jeder sicherheitsrelevante Satz aus dem Detail steht auch in `cautions`, als Bedingung → Handlung („Nur im Sitzen oder Liegen – nie im Wasser.“). Keine Therapie-Anweisungen. | Sichtbar ohne Aufklappen. | manuell: R-TEXT |
| TEXT-09 | – | entfällt → SEC-LEGAL-01 ([SECURITY.md](SECURITY.md#recht)): keine Heil-, Therapie- oder Präventionsversprechen in App, README, Store, Release-Notes, PRIVACY; Wortliste `config/health-claims.txt`, Ausnahmen `config/health-claims.allow`, keine Baseline. | Eine Regel, eine ID. | auto: `ContentRulesTest health-claims` |
| TEXT-10 | MUSS | Zeichen: nur „…“ und ‚…‘, Apostroph ’, nie ASCII-`"` oder `”`; Gedankenstrich –, Minus −, Auslassung …; Dezimalkomma; Einheit mit geschütztem Leerzeichen (`4 s`, `93 %`, `n = 40`) – auch in README, Store, Release-Notes und PRIVACY; „ca.“ statt „~“; Trennpunkt „ · “ nur über `DOT_SEPARATOR`/`splitAtDots()`: keine Zeile beginnt oder endet mit „·“. | Deutsche Typografie; Zeichen gehen im Build sonst verloren. | auto: `typo-chars`, `ContentRulesTest typography` (`quotes`, `decimal-comma`, `dot-separator`, `number-unit`), `ScreenshotTest` (`badBreakAt`) |
| TEXT-11 | MUSS | Store-Texte (`fastlane/metadata/android/de-DE/`): `title` ≤ 30, `short_description` ≤ 80, `full_description` ≤ 4000, `changelogs/<versionCode>.txt` ≤ 500 Zeichen; sie folgen TEXT-04, -07, §11.1 und SEC-LEGAL-01/-02 (Disclaimer wörtlich). Changelog: Nutzen in Laiensprache, eine Zeile pro Änderung, keine IDs, keine Superlative. | Store-Grenzen und -Policy. | auto: `ContentRulesTest store-lengths`, `health-claims`; manuell: R-STORE |
| TEXT-12 | MUSS | Jede genannte Studie steht in [Refs.kt](../app/src/main/java/app/atemkraft/data/Refs.kt): `Nachname I. et al. (Jahr): Originaltitel. Journal Band(Heft):Seiten. <deutsche Einordnung>.`, Kennung `doi:10.… · PMID n`. Im Text „(Name Jahr)“. Jede Referenz wird genutzt. | Nachprüfbarkeit. | manuell: R-TEXT |
| TEXT-13 | MUSS | Yoga-Lehre steht getrennt von der Studienlage: Abschnitt „Aus der Yoga-Lehre“ auf der Detailseite (Übungen yogischen Ursprungs, generierte Muster) und die Seiten „Atemlehre“ und „Pranayama“; immer zuschreibend („gilt in der Yoga-Lehre als …“), nie als Wirkung; Verse deutsch zitiert; Teaser nach TEXT-06, Studien nach TEXT-12. Aufbau: [spec/lernen.md](spec/lernen.md). | Tradition darf erklären, aber nicht wie Evidenz aussehen. | auto: `ContentRulesTest teaser` (`lore-teaser`), `health-claims`; manuell: R-TEXT |
| TEXT-14 | MUSS | **Sanskrit klein, kursiv, mit Längenzeichen** (IAST): Fachwörter der Lehre zwischen Sternchen („*prāṇa*“), die UI setzt sie über `sanskritText` kursiv. Aufrecht bleiben Eigennamen: Übungen und Techniken (Nadi Shodhana, Ujjayi, Viloma), Personen, Schulen, Texttitel; geht es um die Wortbedeutung, ist der Name ein Wort (klein, kursiv). Eingedeutschtes (Yoga, Ayurveda) normal. Nutzernamen stehen wörtlich (`marks = false`). Schreibweisen: [spec/lernen.md](spec/lernen.md). | Einheitlich und nachschlagbar; Kursiv trennt Fachwort von Eigenname. | manuell: R-TEXT |
| TEXT-15 | MUSS | **Einstellungs-Texte nach einem Muster:** Kartentitel neutral (`CardTitle`), darunter höchstens ein Satz, wofür die Karte gilt; Schalter-Name als Substantiv, darunter höchstens ein Satz zur Wirkung; Auswahlreihe mit `SubLabel` darüber und `HintText` darunter, der die Segmente nicht wiederholt; Langes nur im Aufklapper; nichts doppelt – was zwei Karten teilen, steht einmal, die zweite verweist. Details: [spec/einstellungen.md](spec/einstellungen.md). | Hinweise, die den Titel wiederholen, machen das Menü länger, nicht klarer. | auto: `ScreenshotTest` (`05_einstellungen_daten`); manuell: R-TEXT |
| TEXT-16 | MUSS | **Die App zählt, sie bewertet nicht:** Sitzungen, Minuten und Tage in Folge sind nüchterne Angaben („3 Tage in Folge · 45 min diese Woche“), die Serie ist abschaltbar („Serie zeigen“); keine Ziele, kein Soll, keine Vergleiche, keine Trends mit Urteil, keine Auswertung mit gesundheitlicher Bewertung (SEC-LEGAL-02). Befinden am Ende: nur das Wort am Eintrag, nie gerechnet (SEC-LEGAL-04). | Kein Medizinprodukt, kein Leistungsdruck – Atem ist keine Zahl. | manuell: R-TEXT |

### 11.1 Mikrotexte und Begriffsliste

**Buttons** im Infinitiv, 1–2 Wörter („Starten“, „Speichern“; Ausnahmen „Verstanden, starten“ und „Nochmal“/„Ende“ am Ende-Screen). **Status** klein, unaufdringlich („· angepasst“, „lädt … 42 %“). **Datenschutz-Hinweis** in der App nach [SECURITY.md, Prinzipien](SECURITY.md#prinzipien).

| Verwenden | Nicht verwenden |
|---|---|
| Sitzung | Session |
| Übung (Oberbegriff), Atemtechnik (Fachtext), Muster (generiert/eigene) | Programm, Technik als Oberbegriff |
| Programm (nur Kapitel „Programme & Gewohnheiten“), Chip „zum Nachlesen“ an seinen Karten | Chip „Programm“ |
| Bereich (die vier Familien in UI und Store: „Mehr: …“, „17 Atemübungen in vier Bereichen“) | Gruppe, Familie (nur Code) |
| Eigenes Muster (selbst zusammengestellt), Eigene Kreationen (Liste im Tab „Eigene“), Tab „Eigene“, Verlauf (Kopf mit Wochenpunkten, führt zum Logbuch) | Custom, Vorlage, Konfigurator, Meine Muster (nur in Code und Doku) |
| Atemkurve (Bild eines Atemzugs im Editor), Griff, Startpunkt (Ausgangsfolgen im Editor), Ablauf (Liste der Phasen), Phase hinzufügen, intensiv | Graph, Diagramm, Feinjustieren, Zufallsmuster, Sequenz |
| Halten voll / Halten leer (Session, Karten, Muster-Editor; zwischen zwei Stufen und am Stepper für beide nur „Halten“) | Halten oben/unten |
| Laden, wird geladen | Download |
| Sprachanleitung, Stimme | Sprach-Stimme, Stimmmodell (nur Credits) |
| Beenden (laufende Sitzung abbrechen) | Ende (nur als Knopf am Ende-Screen) |
| Einatmen/Ausatmen/Halten (Labels), Einatem/Ausatem (Substantiv) | – |
| beruhigend / aktivierend | vagal / sympathisch |
| wenn du dich gesund und ausgeruht fühlst; Etikett „nur gesund & ausgeruht“ | stabile Phase, Schub, Crash |
| bewusster Belastungsreiz (Fachwort Hormese nur im Detail) | hormetischer Stressor |
| mit Quellen, „in Studien“, „in einer Studie (Name Jahr)“ | evidenzbasiert, nachweislich, in Sekunden, Notbremse |
| Akuter Stress | Panik (Situation, Suchbegriff) |
| Atempause (Substantiv), halten (Verb), „Pause“ (Notiz bei Viloma) | Retention, Atemanhalten, Halte-Pause |
| Runde (gezählte Wiederholung), Atemzug (einmal ein und aus), Phase (Einatmen, Halten, Ausatmen) | Zyklus (nur in Code-Kommentaren), Intervall für Phasenlängen |
| 6 Atemzüge pro Minute, danach im selben Text kurz 6/min | Atemzüge/Minute, Züge pro Minute |
| Klang (Karte mit Lautstärke und Gong), Ton (Tonart beim Atmen: Aus, Töne, Dauerton) | Ton für beides |
| Danach in die Stille (Einstellung, Schalter und Ende-Screen gleich) | Übergang, Nachmeditation |
| ca. (Anleitung, UI), etwa (Fließtext) | ~ |

## <a id="code"></a>12 Code (Compose und Kotlin)

Logging, Speicherung, Netzwerk und Abhängigkeiten regelt [SECURITY.md](SECURITY.md); Commit und Push [PROCESS.md](PROCESS.md).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| CODE-01 | MUSS | Schichten: `domain/` reines Kotlin (keine JVM-APIs, KMP-fähig; offene Fälle in [IOS_PLAN.md](IOS_PLAN.md) §4), importiert nur `domain/`; `data/` nichts aus `ui/`, `cue/`, `playback/`; `cue/` nur `domain/` (+ `BuildConfig`/`R`); Controller ohne UI in `playback/`; vom Wurzelpaket sehen die Schichten nur `BuildConfig` und `R`; `ui/components/` nur `ui/theme/`, `domain/`, `R` (Ausnahme `LocalTabBarState`); `ui/<feature>/` aus `ui/` nur `theme`, `components`, `navigation`, sich selbst (Ausnahme `own/` → `LogHistory`). | Testbarkeit, iOS-Plan, Aufteilung nach Features. | auto: `layer-import-*`, `ui-feature-import`, `domain-platform` |
| CODE-02 | MUSS | Screens (`XScreen`, `XOverlay`) bekommen Werte und `on…`-Lambdas, nie Repository, ViewModel, Controller oder `CoroutineScope`; höchstens 8 Parameter (ohne `modifier`), sonst `XUiState` und `XActions`. Zustand halten genau vier Arten von State-Holdern: `XViewModel` (nur ab App-Start Gesammeltes), `XDestination` (Push-Ziele mit eigener Logik), `GraphState`/`PlaybackState` im Wurzelpaket, `rememberX`-Holder mit Activity-Bezug. | Testbar, vorschaubar; kein ViewModel je Push-Ziel (DECISIONS 2026-10-24). | auto: `ui-state-holder`, `screen-params`, ktlint `compose:vm-injection-check`; manuell: R-CODE |
| CODE-03 | MUSS | Flows mit `collectAsStateWithLifecycle`; Eingaben und offene Dialoge mit `rememberSaveable`; Timer und Audio im `viewModelScope` oder app-weiten Controller; `CancellationException` nie schlucken. | Lifecycle, Rotation, strukturierte Nebenläufigkeit. | auto: `lintDebug`; manuell: R-CODE |
| CODE-04 | MUSS | Öffentliche Composables in `ui/components/`: `modifier: Modifier = Modifier` als erster optionaler Parameter, am Root angewendet; Reihenfolge Pflicht, `modifier`, optional, Content-Lambda; KDoc; privates `@Preview` in `AtemkraftTheme`. | Wiederverwendbar, sichtbar. | auto: ktlint `compose:modifier-*`, `param-order-check`, `preview-public-check` |
| CODE-05 | MUSS | Navigation nur über `@Serializable`-Routen in `ui/navigation/Routes.kt`; unauflösbare Route → `popBackStack()`. Persistierte Enums über `name`, nie `ordinal`. Logik hängt nie an Anzeigetexten. | Kein leerer Screen, keine stille Datenverschiebung. | auto: `ordinal-persist`; manuell: R-CODE |
| CODE-06 | MUSS | ktlint (`intellij_idea`, ohne Zeilenlimit, keine Wildcards, keine voll qualifizierten Namen). Kommentare und KDoc deutsch, erklären das Warum; `TODO` nennt eine Backlog-ID (`// TODO(S-12)`). | Diff-Ruhe, Nachverfolgbarkeit. | auto: `ktlintCheck`, `todo-unlinked` |
| CODE-07 | SOLL | Tests nur für den Kern: Daten überleben Migration und Export/Import, Muster des Tages unverändert, Sicherheitsgrenzen der freien Folge, Gates (Wortbruch, Abschneiden, Tippfläche, Anatomie), ehrliche Texte, Navigations-Grundregel. Neue Tests nur, wo ein Fehler Daten, Sicherheit oder diese Gates trifft; Namen als deutscher Satz in Backticks. | „Kürze ab und schmeiße eher raus als zu behalten“ (DECISIONS 2026-10-22). | auto: `testDebugUnitTest` |
| CODE-08 | – | entfällt → PROZ-01 ([PROCESS.md](PROCESS.md)): Commit-Betreff `fix:`/`docs:`/`build:`/`vX.Y.Z:`. | Prozessregel. | auto: `commit-msg` |

## <a id="automatische-pruefung"></a>13 Automatische Prüfung

Grundsatz: wenige, wartungsarme Werkzeuge; Altlasten über eine Baseline mit S-ID eingefroren. Die Sicherheitsschritte und ihren Ist-Stand führt allein [SECURITY.md, Automatische Prüfung](SECURITY.md#automatische-pruefung); hier steht nur ihre Stelle im Ablauf.

### 13.1 Ablauf

| Werkzeug | Datei | Aufgabe |
|---|---|---|
| `scripts/check.sh` | – | **einziger Einstieg**, lokal und im CI: Stil- und Sicherheits-Checks (`--self-test`, dann Prüfung) → `scripts/build.sh ktlintCheck verifyRoborazziDebug lintDebug assembleRelease -PunsignedRelease=true` (`verifyRoborazziDebug` = `testDebugUnitTest` mit Vergleich der Referenzbilder) → `scripts/verify-apk.sh --allow-unsigned` auf die Prüf-APKs. Der Release-Build bleibt unsigniert; signierte `app-*-release.apk` legt das Skript beiseite und stellt sie wieder hin. `--fast` ohne `lintDebug`, Release-Build und `verify-apk`. Nie `./gradlew` direkt. Git-Hooks in `.githooks/` (`git config core.hooksPath .githooks`): `commit-msg` → `check-style.sh --commit-msg`, `pre-push` → `check-style.sh --ci-range` über die neuen Commits. |
| `scripts/check-style.sh` | `scripts/check_style.py`, `config/style-baseline.txt`, `config/style-fixtures/<check>.{pos,neg}` | Checks aus §13.3 in Python 3: zeilenweise oder (ml) über die ganze Datei; Sonder-Checks als Funktionen. Ignoriert Kommentarzeilen (außer `todo-unlinked`) und Zeilen mit Marker `// Abweichung <ID>:` / `// dekorativ:`. `--self-test` prüft jedes Muster gegen seine Fixtures (Sonder-Checks über ihre Fixture-Funktion), `--list` druckt die Check-Tabelle (§13.3), `--update-check-table` schreibt sie in diesen Guide, `--update-baseline` schreibt den Ist-Stand mit den vorhandenen S-IDs, `--ci-range A..B` prüft `commit-msg` je Commit und `guide-sync` über den ganzen Bereich. Die `sec-*`-Checks laufen im selben Skript ohne Baseline und ohne Marker-Ausnahme. |
| `scripts/bump-version.sh` | `app/build.gradle.kts`, `docs/release-notes/`, `fastlane/…/changelogs/`, Stand-Zeilen | Versionsbump an einer Stelle (PROZ-05); `version-sync` prüft, dass alles übereinstimmt. |
| ktlint 1.8.0 + compose-rules 0.6.8 | `.editorconfig`, Plugin `org.jlleitschuh.gradle.ktlint` 14.2.0, `.git-blame-ignore-revs` | Format, Compose-Regeln; in `*Screen.kt` ist `modifier-missing-check` aus. Ohne Baseline: Jeder Fund ist ein Fehler. |
| Android Lint | `lint {}`: `abortOnError`, `warningsAsErrors` (aus: `GradleDependency`, `AndroidGradlePluginVersion`, `NewerVersionAvailable`, `OldTargetApi`, `ChromeOsAbiSupport`); `app/lint.xml` | Manifest, Ressourcen, Compose-Lint. |
| JUnit-Regeltests | `ContentRulesTest`; `config/health-claims.{txt,allow}` | Ehrliche Texte (§13.3), ohne Baseline. `config/` ist Eingabe der Test-Tasks. |
| Screenshot-Gates | `ScreenshotTest`, `DeviceFitTest` und `TabMatrixTest` (Robolectric + Roborazzi) auf `ScreenshotHarness` (Aufnahme, `TabFrame`, Gates aus `ControlFindings`, `TextClipFindings`, `AnatomyFindings`); `ui/TestMatrix.kt`; SDK 35 | **Breiten-Matrix** (`ScreenshotTest`): 360 dp bei fontScale 1,0 und 2,0; **Gerätehöhen-Matrix** (`DeviceFitTest`): 360 × 640 bei 1,3, A54 bei 1,1 und 1,3 (Editor ohne Tab-Leiste, Meditations-Tab mit); **Tab-Matrix** (`TabMatrixTest`, `TestMatrix.TAB_CONFIGS`): A54 hoch und quer bei 1,1, Tablet 800 × 1280 hoch und quer bei 1,0 – die vier Tab-Startseiten, laufende Atemübung und Meditation. Tabs mit den Leisten der App (`TabFrame`). Läuft in `testDebugUnitTest` und **schlägt bei jedem Befund fehl**: Text (`WORTBRUCH`, `GETRENNT`, `ABGESCHNITTEN`, `CHIP_UMBRUCH`, `ZUGESCHNITTEN`), Bedienelemente (`TIPPFLAECHE`, `OHNE_KLICK`, `BESCHNITTEN`, `AUSSERHALB`), Layout-Grammatik (`ANATOMIE`, §4.5), `SCROLLT` (LAYOUT-08), `KOMPAKT_FEHLT` (MUSTER-10). Die Szenen stehen in den Tests selbst. Bilder: `scripts/build.sh recordRoborazziDebug` → `app/build/outputs/roborazzi/<gerät>/fs<n>/`, je Bild eine `.tsv` mit Befunden und Maßen. **Referenzbilder** (A-12): die 24 Bilder der Tab-Matrix liegen in halber Auflösung im Repo unter `app/src/test/screenshots/<gerät>/fs<n>/` (rund 1,7 MB); `check.sh` vergleicht sie pixelgenau, jede optische Abweichung und jedes fehlende Bild ist ein Fehler (Vergleichsbilder `*_compare.png` unter `app/build/outputs/roborazzi/`, im CI als Artefakt). Nur Grundzustände in Gerätegröße, weil die hohen Listenbilder rund 30 MB wären und sich mit jedem Inhaltstext änderten. **Gewollte Änderung:** `rm -rf app/src/test/screenshots && scripts/build.sh recordRoborazziDebug` nimmt alle Referenzbilder neu auf (das Löschen räumt umbenannte Szenen ab); die neuen Bilder gehören in denselben Commit wie die UI-Änderung. |
| CI | `.github/workflows/ci.yml`, `.github/dependabot.yml` | Push auf `main` und PR, `permissions: contents: read`, Actions per SHA, `persist-credentials: false`. Job `check`: `check.sh` (bei Fehler `.tsv`, Testbericht und Lint-Bericht als Artefakt). Job `conventions`: `check-style.sh --ci-range` über alle Commits des Pushes/PRs. Dependabot monatlich, Betreff „build: …“; Gradle gruppiert, Major-Sprünge von AGP und Gradle von Hand. |

### 13.2 Baseline

`config/style-baseline.txt` (`check⇥datei⇥anzahl⇥S-ID`) friert bekannte MUSS-Verstöße ein; jede Zeile nennt den Backlog-Punkt, der sie behebt (`baseline-ids`). Mehr Treffer als Baseline → Fehler. Weniger → Fehler „Baseline senken“ (`baseline-stale`, PROZ-02). SOLL-Checks warnen nur. Keine Baseline haben `sec-*` (ein Eintrag ist selbst ein Fehler) und die Inhaltsregeln des `ContentRulesTest`. Eine bewusste Ausnahme gehört nicht in die Baseline, sondern als Marker `// Abweichung <ID>: <Grund>` an die Zeile.

### 13.3 Checks

Erzeugt aus `scripts/check-style.sh --list` (`check-table` meldet Abweichungen; `--update-check-table` schreibt neu). Die Muster stehen im Code und in den Fixtures `config/style-fixtures/`; ein Check ohne Ist-Treffer beweist über sein Positiv-Fixture, dass er funktioniert.

<!-- check-table:start -->
| Check | Regel | Scope | Stufe |
|---|---|---|---|
| `color-source` | FARBE-01 | src/*.kt ohne src/ui/theme/* | MUSS |
| `color-source-res` | FARBE-01 | res/*.xml ohne res/values*/colors.xml, res/drawable*/ic_launcher_*, res/mipmap*/*, res/drawable*/ic_*.xml | MUSS |
| `color-source-icons` | FARBE-01 | res/drawable*/ic_*.xml ohne res/drawable*/ic_launcher_* | MUSS |
| `alpha-literal` | FARBE-02 | src/ui/*.kt, src/MainActivity.kt ohne src/ui/theme/* | MUSS |
| `theme-tokens` | FARBE-06 | `ui/theme/` | MUSS |
| `type-literal` | TYPO-01 | src/*.kt ohne src/ui/theme/* | MUSS |
| `layout-literal` | LAYOUT-01 | src/*.kt ohne src/ui/theme/* | MUSS |
| `weight-nofill` | LAYOUT-03 | src/ui/*.kt | MUSS |
| `m3-size-const` | LAYOUT-04 | src/*.kt ohne src/ui/theme/* | MUSS |
| `control-scale` | LAYOUT-06 | src/ui/*.kt, src/MainActivity.kt ohne src/ui/theme/* | MUSS |
| `touch-order` | LAYOUT-06 | src/ui/*.kt | MUSS |
| `control-direct` | KOMP-04 | src/ui/*.kt, src/MainActivity.kt ohne src/ui/components/* | MUSS |
| `platform-override` | LAYOUT-03 | src/*.kt, app/src/main/AndroidManifest.xml | MUSS |
| `dark-only` | PRIN-01 | src/*.kt, res/*.xml | MUSS |
| `dark-only-dirs` | PRIN-01 | `res/values-night*/` | MUSS |
| `neon-fill` | PRIN-02 | src/ui/*.kt, src/MainActivity.kt ohne src/ui/components/* | MUSS |
| `no-fab-toast` | PRIN-03 | src/*.kt | MUSS |
| `card-style` | KOMP-02 | src/ui/*.kt, src/MainActivity.kt | MUSS |
| `exception-message-ui` | MUSTER-04 | src/ui/*.kt, src/cue/tts/*.kt | SOLL |
| `icon-source` | ICON-01 | *.kts, gradle/*.toml, src/*.kt | MUSS |
| `cd-style` | ICON-02 | src/*.kt | MUSS |
| `motion-literal` | MOTION-04 | src/ui/*.kt, src/MainActivity.kt ohne src/ui/theme/* | SOLL |
| `motion-literal-delay` | MOTION-04 | src/ui/components/*.kt | SOLL |
| `player-in-composable` | AUDIO-04 | src/ui/*.kt | MUSS |
| `semantics-required` | A11Y-01 | src/ui/*.kt | MUSS |
| `semantics-heading` | A11Y-01 | `ui/components/` (SectionHeader, PushHeader, TextBlocks) | MUSS |
| `pane-title` | A11Y-07 | `ui/components/` (PushHeader, ScreenHeader, OverlayChrome) | MUSS |
| `a11y-ratchet` | A11Y-02 | src/ui/*.kt | MUSS |
| `switch-unlabeled` | A11Y-03 | src/ui/*.kt | MUSS |
| `ui-literal` | TEXT-01 | src/ui/*.kt | MUSS |
| `head-color` | FARBE-07 | src/ui/*.kt ohne src/ui/components/SectionHeader.kt, src/ui/components/TextBlocks.kt, src/ui/components/CardHeader.kt, src/ui/components/ExpanderSection.kt, src/ui/components/NavCard.kt, src/ui/home/*.kt | MUSS |
| `no-check-icon` | A11Y-03 | src/ui/*.kt | MUSS |
| `anatomy-overlay` | KOMP-02 | src/ui/*.kt ohne src/ui/components/CardHeader.kt | MUSS |
| `catalog-sync` | KOMP-01 | src/ui/components/*.kt | MUSS |
| `rule-length` | §0 | docs/STYLEGUIDE.md, docs/SECURITY.md, docs/PROCESS.md | MUSS |
| `check-table` | §13.3 | `docs/STYLEGUIDE.md` | MUSS |
| `version-sync` | PROZ-05 | `app/build.gradle.kts`, Stand-Zeilen, `docs/release-notes/`, `fastlane/…/changelogs/` | MUSS |
| `locale` | TEXT-03 | src/ui/*.kt | MUSS |
| `locale-config` | TEXT-03 |  | MUSS |
| `typo-chars-strings` | TEXT-10 | res/values*/strings.xml | MUSS |
| `typo-chars-src` | TEXT-10 | src/*.kt | MUSS |
| `typo-chars-content` | TEXT-10 | src/data/*.kt, src/domain/*.kt | MUSS |
| `layer-import-domain` | CODE-01 | src/domain/*.kt | MUSS |
| `domain-platform` | CODE-01 | src/domain/*.kt | MUSS |
| `layer-import-data` | CODE-01 | src/data/*.kt | MUSS |
| `layer-import-cue` | CODE-01 | src/cue/*.kt | MUSS |
| `layer-import-components` | CODE-01 | src/ui/components/*.kt | MUSS |
| `layer-import-playback` | CODE-01 | src/playback/*.kt | MUSS |
| `layer-import-root` | CODE-01 | src/data/*.kt, src/playback/*.kt, src/ui/*.kt | MUSS |
| `ui-state-holder` | CODE-02 | src/ui/*.kt ohne src/ui/*Destination.kt, src/ui/*ViewModel.kt, src/ui/settings/PatternTransfer.kt | MUSS |
| `screen-params` | CODE-02 | src/ui/*.kt | MUSS |
| `ui-feature-import` | CODE-01 | src/ui/*.kt | MUSS |
| `ordinal-persist` | CODE-05 | src/data/*.kt | MUSS |
| `todo-unlinked` | CODE-06 | src/*.kt | MUSS |
| `sec-deny-deps` | SEC-PRIV-01 | gradle/libs.versions.toml, *.gradle.kts | MUSS, ohne Baseline |
| `sec-net-endpoints` | SEC-PRIV-02 | java/*.kt, java/*.java ohne src/domain/VoiceCatalog.kt, src/ui/about/AboutScreen.kt | MUSS, ohne Baseline |
| `sec-net-client` | SEC-NET-02 | java/*.kt, java/*.java ohne src/cue/tts/VoiceModelManager.kt | MUSS, ohne Baseline |
| `sec-device-leaks` | SEC-PRIV-03 | java/*.kt, java/*.java | MUSS, ohne Baseline |
| `sec-privacy-link` | SEC-PRIV-05 | `docs/PRIVACY.md`, `ui/about/AboutScreen.kt` | MUSS, ohne Baseline |
| `sec-wakelock` | SEC-PERM-02 | java/*.kt, java/*.java | MUSS, ohne Baseline |
| `sec-tls` | SEC-NET-01 | app/src/main/*.kt, app/src/main/*.java, app/src/main/*.xml ohne app/src/main/assets/licenses/* | MUSS, ohne Baseline |
| `sec-tls-config` | SEC-NET-01 | `res/xml/network_security_config.xml`, Manifest | MUSS, ohne Baseline |
| `sec-download` | SEC-NET-02 | `java/` | MUSS, ohne Baseline |
| `sec-components` | SEC-PLAT-01 | java/*.kt, java/*.java, app/src/main/AndroidManifest.xml | MUSS, ohne Baseline |
| `sec-intents` | SEC-PLAT-02 | java/*.kt, java/*.java, app/src/main/AndroidManifest.xml | MUSS, ohne Baseline |
| `sec-no-dyncode` | SEC-PLAT-03 | java/*.kt, java/*.java, res/*.xml | MUSS, ohne Baseline |
| `sec-storage` | SEC-STORE-01 | java/*.kt, java/*.java | MUSS, ohne Baseline |
| `sec-backup` | SEC-STORE-02 | Manifest, `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | MUSS, ohne Baseline |
| `sec-room` | SEC-STORE-03 | `java/`, `app/schemas/` | MUSS, ohne Baseline |
| `sec-repos` | SEC-SUP-01 | `settings.gradle.kts`, `gradle/verification-metadata.xml` | MUSS, ohne Baseline |
| `sec-pinned-inputs` | SEC-SUP-02 | Wrapper, Binärdateien, `.sha256`, `verification-metadata.xml`, SECURITY.md | MUSS, ohne Baseline |
| `sec-gradle-flags` | SEC-BUILD-03 | `app/build.gradle.kts` | MUSS, ohne Baseline |
| `sec-secrets` | SEC-BUILD-02 | alle Dateien | MUSS, ohne Baseline |
| `sec-gitignore` | SEC-REPO-01 | `.gitignore` | MUSS, ohne Baseline |
| `sec-actions` | SEC-REPO-03 | `.github/workflows/`, `dependabot.yml` | MUSS, ohne Baseline |
| `baseline-stale` / `baseline-ids` | PROZ-02, §13.2 | `config/style-baseline.txt` | MUSS |
| `guide-sync` (CI) | PROZ-02 | Push-Bereich | MUSS |
| `commit-msg` (CI, Hook) | PROZ-01 | Push-Bereich, `.githooks/commit-msg` | MUSS |
<!-- check-table:end -->

Weitere Prüfungen außerhalb von `check_style.py`:

| Prüfung | Was | Regel |
|---|---|---|
| `ContentRulesTest` | `teaser` (Fälle `teaser`, `first-caution`, `lore-teaser`: ganzer Satz ≤ 95 Zeichen, Detail beginnt nicht mit dem Teaser, Lehre zuschreibend), `health-claims` (App-Inhalte, `strings.xml`, README, PRIVACY, Release-Notes, `fastlane/**` je Zeile; Disclaimer wörtlich), `store-lengths`, `typography` (`decimal-comma`, `quotes`, `dot-separator`, `number-unit` in Inhalten, `strings.xml`, Quellen und öffentlichen Texten, `phase-names`), `cross-references`, `glossary-terms`; alle ohne Baseline | TEXT-05, -06, -10, -11, -13, MUSTER-03, SEC-LEGAL-01/-02 |
| `ScreenshotTest`, `DeviceFitTest` | Gates aus §13.1; dazu `chipLayout`/`chipRows`/`filterRow` (MUSTER-09, -12), `RING` (LAYOUT-08), `editorFit` (MUSTER-10) | LAYOUT-03, -04, -05, -07, -08, A11Y-02, -05, §4.5 |
| `TabNavigationTest` | Re-Tap auf den markierten Tab führt zur Tab-Startseite; markiert bleibt der Tab des Stapels; Leiste fest nur auf den Startseiten | MUSTER-08 |
| `DatabaseMigrationTest`, `PatternBackupTest`, `DailyPatternGoldenTest`, `FreePatternTest`, `PatternEditorSafetyTest`, `PlaybackFlowTest` | Room-Migrationen; Export/Import-Rundlauf; Muster des Tages unverändert (Hashes); Grenzen der freien Folge; Sicherheitsdialog je Übung; Abläufe beider Sitzungsarten | SEC-STORE-01 … -03, MUSTER-06, -07, -10 |
| `ExerciseFilterTest` | jeder Filter trifft mindestens 3 und nicht alle Übungen, „Bis 3 min“ wie am Startknopf, UND, leere Familien fallen weg | MUSTER-12 |
| ktlint / Lint / Tests | `ktlintCheck` (+ compose-rules), `lintDebug`, `testDebugUnitTest` | TEXT-01, CODE-02 … -04, -06, -07 |
| `verify-apk`, `release-verify` (mit `verify-release-body`), `check-github`, `sec-*` | [SECURITY.md, Automatische Prüfung](SECURITY.md#automatische-pruefung) | SEC-* |

### 13.4 Manuelle Checklisten

| Liste | Wann | Schritte |
|---|---|---|
| R-A11Y | UI-Änderung; vor Release Session, Meditation, Einstellungen | TalkBack: Name, Rolle, Zustand, Überschriften; Phase, Pause, Ende, Restzeit hörbar; keine Glyphen, Einheiten ausgeschrieben · Voice Access, Switch Access, D-Pad: alles erreichbar, Fokus sichtbar und sinnvoll · MUSTER-05 mit TalkBack: angesagt, kein Rückfall · Systemsprache Englisch: Texte bleiben deutsch · Accessibility Scanner ohne neue Funde |
| R-VISUAL | UI-Änderung | **Agent:** Gates grün (`ScreenshotTest`, `DeviceFitTest`, `TabMatrixTest`), neue Screens und Zustände als Szene aufnehmen, Referenzbilder bei gewollter Änderung neu aufnehmen (§13.1), Token-Maße benachbarter Elemente im Code vergleichen (§4.5) – Bilder werden nicht angesehen (PROZ-07). **Mensch am Gerät (A54):** Köpfe weiß, Gruppenlabels in Familienfarbe, Kapitelkarten reihum · Querformat, Tablet ohne Beschnitt · heller Systemmodus: Statusleisten-Icons hell · Rotation mit offenem Dialog · „Animationen entfernen“: Pacer läuft, Shimmer steht · eine Neon-Fläche, Farben in ihrer Rolle, Gerüst nach §4 |
| R-TEXT | Inhaltsänderung | Teaser vollständig, derselbe Punkt wie das Detail, Studienlage-Stufe passend (TEXT-07) · keine Heilaussage, Begriffsliste, Laiensprache, zählt statt bewertet (TEXT-16) · Sicherheitsrelevantes in `cautions`, Studien mit Originaltitel in `Refs.kt` · Leer- und Fehlertexte mit nächstem Schritt |
| R-AUDIO | Änderung in `src/cue/` oder Session-Abläufen | alle Lautstärkestufen ohne Klicks, Gong klingt aus · stumm: fremde Musik nicht geduckt; Pause gibt Fokus ab · Vibration aus: kein Tick; System-Tippvibration aus, App-Schalter an: Tick spürbar · Bildschirm aus, Home-Taste: Ton läuft, nach Fortsetzen synchron |
| R-CODE | Code-Änderung | CODE-01 … -03, -05; KOMP-01; TYPO-02; MOTION-01; AUDIO-01; Layout nach §4.5 |
| R-STORE | Store-Update | Screenshots aus Release-Build, dunkel, Demo-Statusleiste, ohne echte Logbuchdaten · Feature-Grafik 1024 × 500 ohne Alpha, Icon 512 × 512 aus der Palette · Texte nach TEXT-11 ([STORE.md](STORE.md)) |
| Vor Release | jedes Release (Design-Teil) | `check.sh` grün · R-A11Y, R-VISUAL (Mensch), R-AUDIO auf echtem Gerät, bei Store-Update R-STORE · Changelog nach TEXT-11 · Backlog und Baseline aktuell. Ablauf: [PROCESS.md](PROCESS.md), [SECURITY.md, Release-Checkliste](SECURITY.md#release-checkliste) |

## <a id="backlog"></a>14 Backlog

Nur Offenes; Erledigtes steht in [DECISIONS.md](DECISIONS.md). Sicherheitsbefunde werden nicht öffentlich geführt ([SECURITY.md](SECURITY.md#melden)). Reihenfolge = Priorität. Aufwand **S** < 1 h · **M** ≤ 1 Tag · **L** > 1 Tag.

### Automatisierung

| ID | Aufgabe | Aufwand |
|---|---|---|
| A-14 | Check `icon-size` (ICON-01, LAYOUT-04): `Icon(` ohne Größen-Token außerhalb von `AppIconButton`; regulär nicht ohne Fehlalarm, erst mit Parser | S |
| A-10 | später: Compose-UI-Semantiktests (`isHeading`, `hasClickAction`, `enableAccessibilityChecks()`) | L |

### Design und Code

| ID | Regel | Befund (Ort) | Aufwand |
|---|---|---|---|
| S-18 | FARBE-04, -06 | drei Pinktöne ΔE < 25 (Entscheidung: `SessionButtonPink` aus `error` oder `EvidenceCaution` Richtung Koralle); `SecondaryContainer` 1,18 auf Card (Option #5A4890) | S |
| S-23 | TEXT-12 | `respeRate` (Patent, kein Studienzitat) wird genutzt: „Mehr zum Dauerton“ in den Einstellungen und `settings_sound_hint` nennen RESPeRATE. Entscheidung offen: als Quelle des Prinzips behalten oder samt Satz streichen | S |
| S-25 | KOMP-01 | About-Titel `headlineLarge` in `primary` | S |
| S-32 | CODE-04, -06 | Komponenten ohne `modifier`; 0 Previews bei mehreren Komponenten; 67 Kommentarzeilen mit ASCII-Schlusszeichen | M |
| S-33 | ICON-02, TYPO-01 | cd 5× per `semantics` am Button; Gewichtswechsel in der NavigationBar | S |
| S-37 | LAYOUT-04, TYPO-02 | `NavigationBar` folgt der Schrift nicht (Höhe 80, Indikator 64 × 32 fest in M3 1.3.1); Label 9,9 sp am A54 eher zu klein. Entscheidung offen: eigene Leiste oder größere Label-Rolle | M |
| S-38 | LAYOUT-03 | „Atmen → Ton“ fällt auf dem A54 (1,1) trotz kleinerer Segmente auf die Radioliste zurück | S |
| S-41 | CODE-01, -02 | Bewusst offen (Architektur-Review 2026-10-24): gemeinsamer Ablauf-Kern für `BreathSessionController` und `MeditationController` (erst mit einem dritten Ablauf); lange Composables aufteilen (`ExerciseDetailScreen`, `SessionScreen.ActiveContent`, `TimeDial`, `HomeScreen`, `BreathCurveEditor`); `SubcomposeLayout` in `SectionHeader` → `Layout` mit Intrinsics; `java.text.Normalizer` und `javaClass` als expect/actual für KMP | L |
| S-39 | MUSTER-09 | nur auf Zuruf: Embeddings für die Befindens-Suche (erst bei Hunderten freier Inhalte sinnvoll) | L |
| S-42 | §4.5 | Weitere Bausteine in `CardHeader`/Anatomie überführen: `SettingsCard`-Titel, `ExpanderCard`-Kopf, `LogEntryCard`; `SectionHeader` und `GroupLabel` auf eine gemeinsame `TitleRow` ziehen | M |

## <a id="quellen"></a>Quellen

RFC 2119 · [WCAG 2.2](https://www.w3.org/TR/WCAG22/) · [WCAG2ICT](https://www.w3.org/TR/wcag2ict-22/) · [Compose Accessibility](https://developer.android.com/develop/ui/compose/accessibility/key-steps) · [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) · [Edge-to-edge](https://developer.android.com/develop/ui/compose/system/setup-e2e) · [Android 17 Background-Audio](https://developer.android.com/about/versions/17/changes/bg-audio) · [Per-App-Sprache](https://developer.android.com/guide/topics/resources/app-languages) · [ktlint](https://pinterest.github.io/ktlint/) · [compose-rules](https://github.com/mrmans0n/compose-rules) · EU MDR 2017/745 (Erwägungsgrund 19, Art. 2 Nr. 12) · [Play Health Content](https://support.google.com/googleplay/android-developer/answer/14738291) · [Play-Listing](https://support.google.com/googleplay/android-developer/answer/9866151) · [F-Droid-Metadaten](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/) · interne Audits an `2a1cb20` (Kontrast nach WCAG 2.x, ΔE nach CIE76) · [Guardrails-Audit 2026-10-24](recherche/2026-10-24-guardrails.md).
