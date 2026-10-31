# Atemkraft – Styleguide

Normativer Design-, Text- und Code-Leitfaden für Maintainer und Claude: Jede Regel nennt ihre Prüfung. Stand: 2026-10-30, v2.10.0 und `main`. Sicherheit, Datenschutz, Build und Release regelt [SECURITY.md](SECURITY.md); Commit, Push, Release und Arbeitsweise [PROCESS.md](PROCESS.md); die Kurzfassung für Agenten steht in [CLAUDE.md](../CLAUDE.md). Wie ein Feature genau gebaut ist, steht in [docs/spec/](spec/); warum etwas so entschieden wurde, in [DECISIONS.md](DECISIONS.md).

**Inhalt:** [0 Geltung](#geltung) · [1 Leitplanken](#leitplanken) · [2 Farbe](#farbe) · [3 Typografie](#typografie) · [4 Layout](#layout) (4.5 [Anatomie der Bausteine](#anatomie)) · [5 Komponenten](#komponenten) · [6 Muster](#muster) · [7 Icons](#icons) · [8 Bewegung](#bewegung) · [9 Audio und Haptik](#audio) · [10 Barrierefreiheit](#a11y) · [11 Text und Ton](#text) · [12 Code](#code) · [13 Automatische Prüfung](#automatische-pruefung) · [Quellen](#quellen) · außerhalb: [BACKLOG.md](BACKLOG.md) (Offenes), [CHECKS.md](CHECKS.md) (Prüfwerkzeuge)

## <a id="geltung"></a>0 Geltung

**Einstieg nach Aufgabe:** erst die Leitplanken (§1), dann die Zeile unten, dazu immer der Katalog (§5.1) und die Spec des Features ([docs/spec/](spec/)).

| Du baust … | lies | baue mit |
|---|---|---|
| eine Karte | [Karte mit Aktionen](#anat-karte), KOMP-02, -03, FARBE-07 | `CardHeader`, `ExerciseCard`, `NavCard` |
| einen Abschnitt, eine Liste | [Abschnittskopf](#anat-kopf), §4.1, LAYOUT-02, MUSTER-03 | `SectionHeader`, `CollapsibleSection`, `GroupLabel` |
| eine Einstellung | [Zeile mit Steuerelement](#anat-steuerzeile), A11Y-03, LAYOUT-07, TEXT-15, [spec/einstellungen.md](spec/einstellungen.md) | `ToggleRow`, `Stepper`, `SegmentedChoiceRow` |
| ein Blatt | [Bottom Sheet](#anat-sheet), MUSTER-11 | `AppBottomSheet` |
| eine neue Seite | §4.3, LAYOUT-02, MUSTER-08, A11Y-07, [spec/navigation.md](spec/navigation.md) | `PushScaffold`, `TabListScaffold` |
| Start, Leiste unten | [Knopfleiste unten](#anat-leiste), MUSTER-01, LAYOUT-04 | `PinnedActionBar`, `StartSplitButton` |
| Chips, Suche, Filter | [Chip-Reihen](#anat-chips), [Suchpille](#anat-pille), MUSTER-09, -12 | `ChoiceChip`, `TonalChip`, `SearchField` |
| Farbe, Maß, Token | §2, §4.1, §4.2, FARBE-*, LAYOUT-01, PROZ-02 | `ui/theme/` |
| Text, Inhalt | §11, §11.1, R-TEXT | `BodyText`, `HintText`, `WholeWordText` |
| Sitzung, Ton, Bewegung, Übergang | MUSTER-07, §8, §9, PERF-01, [spec/session.md](spec/session.md) | `OverlayChrome`, `SessionControlBar` |

- **Pfade:** `src/` = `app/src/main/java/app/atemkraft/`, `res/` = `app/src/main/res/`.
- **Regelformat:** `ID | Stufe | Regel | Warum | Prüfung`. Eine Regelzelle sagt in höchstens 400 Zeichen, *was gilt* (Doku-Lint `rule-length` prüft die dritte Spalte jeder Zeile mit ID, auch die S-Zeilen in BACKLOG.md); die Spalte Warum gibt den Grund in einem Halbsatz (Richtwert 120 Zeichen, ungeprüft). Spezifikation gehört nach `docs/spec/` oder in die KDoc, Historie und Nutzerentscheidungen nach [DECISIONS.md](DECISIONS.md), Offenes nach [BACKLOG.md](BACKLOG.md).
- **IDs** sind stabil und werden nie neu vergeben. Gestrichen bzw. verschoben, aber gesperrt: META-01 → PROZ-02, LAYOUT-06 und KOMP-04 → LAYOUT-04, MUSTER-02 → MUSTER-01, TEXT-09 → SEC-LEGAL-01, CODE-08 → PROZ-01. Eine künftig gestrichene Regel kommt in diese Zeile.
- **Marker:** `// Abweichung <ID>: <Grund>` in derselben Zeile; für dekorative Icons, Linien und Flächen (ICON-02, FARBE-03) `// dekorativ: <Grund>`. Beide nehmen die Zeile aus den Stil-Checks, nie aus `sec-*`.
- **Prüfung:** `auto: <check>` verweist auf die Check-Tabelle ([CHECKS.md](CHECKS.md#checks), §13.3), `manuell: R-…` auf eine Checkliste in §13.4.

| Stufe | Bei Verstoß |
|---|---|
| **MUSS** / **DARF NICHT** | Mit `auto:` wird die CI rot, mit `manuell:` hält das Review an. Altlasten sind per Baseline mit S-ID eingefroren (§13.2). Marker `// Abweichung <ID>: <Grund>` nur, wo die Regel eine Ausnahme nennt. |
| **SOLL** | Abweichung nur mit Marker `// Abweichung <ID>: <Grund>` in derselben Zeile; ohne Marker warnt die Automatik. |

## <a id="leitplanken"></a>1 Leitplanken

Zwölf Sätze, die jeder Beitrag einhält; dieser Abschnitt ist maßgeblich, [CLAUDE.md](../CLAUDE.md) führt dieselben zwölf Punkte in Kurzform und gleicher Nummerierung. Die Regeln dahinter stehen in Klammern.

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

Leitplanke 1 hat keine eigene Fachregel weiter unten; sie gilt als:

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| PRIN-01 | MUSS | **Nur dunkel:** ein `darkColorScheme`, kein helles, `DayNight`- oder dynamisches Schema, kein Ordner `values-night*`; `enableEdgeToEdge` nur mit ausdrücklich dunklen Leisten. | Ein Farbsystem, auf dem die Kontrasttabelle (§2.1) beruht. | auto: `dark-only`, `dark-only-dirs` |
| PRIN-02 | MUSS | **Eine Neon-Fläche je Screen:** höchstens eine gefüllte Fläche in `primary`, `secondary`, `tertiary` oder `Neon*` (die Hauptaktion); alles andere tonal, umrandet oder als Text. Gefüllte Neon-Container setzen nur die Komponenten in `ui/components/`. | Eine Fläche führt den Blick, zwei konkurrieren. | auto: `neon-fill`; manuell: R-VISUAL |
| PRIN-03 | MUSS | **Nichts schwebt:** kein FAB, Toast oder Snackbar; Bestätigung und Fehler stehen im auslösenden Element (MUSTER-01, -04). Aktionen ohne Text tragen ihren Namen als `contentDescription` am Icon (ICON-02). | Schwebendes verdeckt Inhalt und ist weg, bevor TalkBack es liest. | auto: `no-fab-toast`, `cd-style` |

## <a id="farbe"></a>2 Farbe

### 2.1 Tokens ([Color.kt](../app/src/main/java/app/atemkraft/ui/theme/Color.kt), [Theme.kt](../app/src/main/java/app/atemkraft/ui/theme/Theme.kt))

Kontrast (WCAG 2.x) der Token-Farbe als Vordergrund gegen `background` #160F2E und Card #2E2150, Alpha vorher mit dem realen Grund gemischt.

| Token | Hex | M3-Rolle | Bedeutung | vs BG | vs Card |
|---|---|---|---|---|---|
| `NeonMagenta` | #F25CA2 | primary | Marke, Aktion, Auswahl; gewählter Tab (Symbol und Beschriftung) | 5,98 | 4,71 |
| `NeonCyan` | #34E0E8 | secondary | Verweise und Links im Inhalt (FARBE-04), Einatmen, eigene Muster (Rand, gespeichert), Meditation (Zeitring, Logbuch-Punkt), Fortschritt | 11,36 | 8,95 |
| `NeonYellow` | #FAD84A | tertiary | nur über `WarnAmber`; ΔE76 30,5 zu `FamilySympathetic` | 13,12 | 10,34 |
| `OnNeon` / `OnCyan` | #1E0A16 / #042A2C | onPrimary, onTertiary, onError / onSecondary | Inhalt auf Neon | 6,15 auf primary, 6,96 auf error | – |
| `DarkBackground` / `DarkSurface` | #160F2E / #211640 | background / surface, surfaceContainer | Screen-Grund (`window_background`) / Leisten (Elevation 3 dp → #321C48), als `TabBarContainer` Grund der Tab-Leiste | – | – |
| `DarkSurfaceVariant` | #2E2150 | surfaceVariant, surfaceContainerHighest | **Card-Fläche**, Mini-Bar | 1,27 | – |
| `SurfaceContainerHigh` / `Lowest` / `Low` | #281B48 / #120C26 / #1B1236 | surfaceContainerHigh/Lowest/Low | Dialog, Suchfeld-Pille (`SearchField`), ungewählte Filter-Chips (`TonalChip`) / nur implizit von M3 gelesen / ruhige Leer-Karte „Eigene Kreationen“ | – | – |
| `SecondaryContainer` | #5A4890 | secondaryContainer | Tonal-Buttons (Stepper, Reset); darauf nur `onSecondaryContainer` (6,32), farbige Beschriftung nie (`primary` 2,47; → `TonalDeep`) | 2,42 | 1,91 |
| `TonalDeep` | #3A2C60 | – | dunkle tonale Fläche für farbige Glyphen (≥ 4,5:1): gewählte Zeilen im Muster-Editor, Seitenknöpfe der `SessionControlBar`, Auswahl-Pille der Tab-Leiste (pinkes Symbol 3,99) | 1,50 | 1,18 |
| `SegmentSelected` | #715AAF | – | gewählte Fläche von Segment (`SegmentedChoiceRow`, 3,02 gegen ungewählt, Beschriftung 4,62), Anlass-Chip (`ChoiceChip`) und Filter-Chip (`TonalChip`, 2,82 gegen `surfaceContainerHigh`, darum Beschriftung von `SECONDARY` auf voll) | 3,31 | 2,61 |
| `OutlineVariantIndigo` | #453763 | outlineVariant | Trenner unter „Muster des Tages ›“ im Atmen-Tab, Feinstriche des Zeitrings; dekorativ | 1,73 | 1,37 |
| `DividerSubtle` | #F0E6FF α 0,08 | – | **einziger** Trenner in Karten und Expandern (KOMP-03); dekorativ | – | – |
| `OutlineIndigo` | #9A8FBF | outline | Rand von Switch, Chip (`ChoiceChip`, `LinkChip`), Segment, `AppOutlinedButton`, Namensfeld, ∞-Knopf des Zeitrings | 6,18 | 4,87 |
| `PrimaryContainer` / `OnPrimaryContainer` | #4E2560 / = DarkOnBackground | primaryContainer / onPrimaryContainer | nur implizit von M3 gelesen (FARBE-06); Text darauf 9,94 | – | – |
| `DarkOnBackground/OnSurface` | #F0E6FF | onBackground, onSurface | Primärtext, alle Köpfe (FARBE-07) | 15,30 | 12,05 |
| `DarkOnSurfaceVariant` | #C3B8DD | onSurfaceVariant | leiser Text (Dialog, Mini-Bar), „+ Halten“ im Editor | 9,83 | 7,74 |
| `FamilyVagal` / `Sympathetic` / `Balance` / `Functional` | #60A5FA / #F59E0B / #A78BFA / #34D399 | – | Familie „Beruhigen“ / „Aktivieren“ / „Ausgleichen“ / „Atem schulen“: Gruppenlabel, Kartenrand, Bereichs-Verweis | 7,23 / 8,56 / 6,76 / 9,57 | 5,70 / 6,75 / 5,32 / 7,54 |
| `EvidenceBest` | = NeonCyan | – | Studienlage „gut belegt“ | 11,36 | 8,95 |
| `TodayGold` | = FamilySympathetic #F59E0B | – | „heute“ und die Serie: Heute-Ring der Woche (Tab „Meins“ und Logbuch), „3 Tage in Folge“ (`StreakText`); dasselbe Gold wie die Kapitel in „Lernen“, nie als Gruppenlabel oder Rand (Ausnahme FARBE-04 wie der Kopf-Ring) | 8,56 | 6,75 |
| `EvidenceStudied` | #78B0CC | – | Studienlage „in Studien geprüft“; Chip-Text 4,60 auf Card | 7,78 | 6,13 |
| `EvidenceLittle` | #C8B8B4 | – | Studienlage „kaum untersucht“; Chip-Text 5,43 auf Card | 9,61 | 7,57 |
| `EvidenceCaution` | #FF6B8B | error | Vorsicht, Fehler, Bestätigungszustand (MUSTER-05); Leiste 5,54 | 6,77 | 5,33 |
| `WarnAmber` | = NeonYellow | – | Sicherheit, Intensität („intensiv“, ⚠-Zeile, Bediengrenze im Editor); Chip 7,02 auf Card | 13,12 | 10,34 |
| `BreathExhale` | #FF9E7A | – | Ausatmen in Rhythmusbildern, Phasen-Karte und Atemkurve (Koralle; ΔE76 ≥ 36 zu jeder Semantikfarbe); derselbe Ton im Kopf-Ring | 9,13 | 7,19 |
| `HeadingRing` | #F25CA2 → #FF9E7A → #A78BFA → #F59E0B → #34D399 | – | Kopf-Ring (FARBE-07): nur die Titel der vier Kapitelkarten in „Lernen“ (`NavCard`) über `headingColor(index)`, **ohne Bedeutung** | 5,98 / 9,13 / 6,76 / 8,56 / 9,57 | 4,71 / 7,19 / 5,32 / 6,75 / 7,54 |
| `VayuPrana` / `Apana` / `Samana` / `Udana` / `Vyana` | = NeonCyan / BreathExhale / NeonYellow / FamilyBalance / FamilyFunctional | – | die fünf *vāyus* im Bild der Atemlehre (`VayuFigure`) und seiner Liste, **nur dort**, jede Fläche mit Namen (FARBE-05); Gelb und Grün sind hier weder Warnung noch Familie ([spec/lernen.md](spec/lernen.md)) | 11,36 / 9,13 / 13,12 / 6,76 / 9,57 | 8,95 / 7,19 / 10,34 / 5,32 / 7,54 |
| `VayuFigureFill` / `VayuFigureOutline` | #241A47 / #8C7BC4 | – | Grundfläche (dekorativ) und Umriss der Piktogramm-Figur; Umriss ≥ 3:1 auf `surface` | – / – | – |
| `SynthCircleCenter` / `Edge` | #CB3C9A / #4A2080 (`ALPHA_CIRCLE_EDGE` 0,72) | – | Atemkreis-Verlauf | 4,07 / 1,36 | – |
| `SessionTextYellow` / `SessionNoteAmber` | #EBDCB0 / #F0E4C0 | – | Phase und Sekunden im Kreis / Anleitungszeile unter dem Kreis (α `SECONDARY`) | 13,48 / 7,56 (Phase auf Zentrum 3,31, großer Text) | – |
| `SessionTextGlow` | #160B22 | – | Textschatten, Label auf Cyan-Button | – | – |
| `SessionButtonCyan` | #6FB6BA | – | Session-Buttons (Pause/Weiter gefüllt, „Von vorne“); „Beenden“ steht in `error` | 7,95 | – |

ΔE76 der Pinktöne: primary ↔ `EvidenceCaution` 19,9; ein drittes Pink gibt es nicht, „Beenden“ nutzt `error`.

### 2.2 Alpha-Tokens

| Stufe | Token | Wert | min. Kontrast BG / Card | Verwendung |
|---|---|---|---|---|
| Primär | – | 1,0 | 15,30 / 12,05 | Titel, Labels, Werte |
| Fließtext | `BODY` | 0,8 | 10,00 / 8,26 | Kurzbeschreibung der Detailseite, Metazeile der Karte, Text der Detailseite, Glossar, Über, aufgeklappter Teaser (`ExpanderSection`) |
| Sekundär | `SECONDARY` | 0,7 | 7,92 / 6,68 | Untertitel, Beschreibung, Status, jeder zugeklappte Teaser, neutrale Chips (Umriss), Caret und Kopf-Aktionen, ungewählte Filter-Chips |
| Tertiär | `TERTIARY` | 0,55 | 5,27 / 4,69 (Leiste 4,78) | DOI/PMID, Zeitstempel, Wochentage, Session-Hilfszeilen, „✓ Fertig“, unselektierte Tabs, Ton aus, Zeitleiste und Linien „voll“/„leer“ der Atemkurve |
| Disabled | M3-intern | 0,38 | – | nur inaktive Bedienelemente |
| Flächen | `ALPHA_BORDER` · `CHIP_FILL` · `ALPHA_HEAT_LOW` / `_MID` · `ALPHA_CURVE_FILL` · `ALPHA_CIRCLE_EDGE` | 0,35 · 0,16 · 0,70 / 0,85 · 0,08 · 0,72 | `primary` 0,70: 3,50 / 3,01 | Kartenrand (`accentBorder`) · Fläche gefüllter Chips · Stufen der Logbuch-Wochenzeile (Zustandsgrafik; Tag ohne Übung = leerer Ring in `TERTIARY`, 4,70 auf Card) · Füllung unter der Atemkurve (dekorativ) · Rand des Atemkreis-Verlaufs |
| vāyu-Bild | `VayuAlpha(selected, idle, dimmed)`: `VayuFieldAlpha` · `VayuWholeAlpha` · `VayuAuraAlpha` · `VayuLineAlpha` · `VayuLabelAlpha`; `VAYU_PULSE_LOW` / `_HIGH` | Werte in `Color.kt` | Beschriftung voll 6,76 auf `surface` | Zustände gewählt / nichts gewählt / ein anderer gewählt, Pulsieren der gewählten Fläche ([spec/lernen.md](spec/lernen.md)) |

### 2.3 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| FARBE-01 | MUSS | Farben nur aus `MaterialTheme.colorScheme` oder aus `Color.kt` (Family\*, Evidence\*, Session\*, Synth\*, `BreathExhale`, `HeadingRing`, `Vayu*`, `TabBarContainer`). Kein `Color(0x…)` außerhalb `src/ui/theme/`. Hex in `res/` nur für `window_background`, den Launcher-Grund #1A0B2E (das Symbol bleibt unverändert) und `#FF000000`/`#FFFFFFFF` in einfarbigen `drawable/ic_*.xml`. | Eine Quelle der Wahrheit. | auto: `color-source` |
| FARBE-02 | MUSS | Alpha nur über die Tokens aus §2.2, auch nicht bedingt (`if (…) 0.8f else 0.6f`). | Freie Literale verwischen die Hierarchie. | auto: `alpha-literal` |
| FARBE-03 | MUSS | Kontrast nach Alpha-Mischen gegen den realen Grund: Text < 24 sp (< 18,66 sp Bold) ≥ 4,5:1; großer Text, Icons, Zustandsgrafik ≥ 3:1. Kritischer Punkt zählt (Kreiszentrum, gemischte Chip-Fläche, Leiste #321C48). Ausnahme: rein dekorative Linien/Flächen mit Marker `// dekorativ: <Grund>`. | WCAG 1.4.3 / 1.4.11. | auto: `ThemeContrastTest` (Tabellen §2.1, §2.2); manuell: R-A11Y |
| FARBE-04 | MUSS | Semantische Farben tragen nur die Bedeutung aus der Spalte „Bedeutung“ in §2.1; Aufklappen ist keine Aktion (nie `primary`). Einzige Ausnahme: der Bereichs-Verweis „Mehr: Beruhigen ›“ steht in der Familienfarbe. Semantische Farben auf einem Screen: ΔE76 ≥ 25; Kopf-Ring und `Vayu*` zählen nicht dazu. | Doppelt belegte Farben sagen nichts mehr. | manuell: R-VISUAL (ΔE) |
| FARBE-05 | MUSS | Farbe ist nie alleiniger Träger: Evidenz und Warnung mit Text (⚠-Zeile), Familie als Text oder im Gruppenlabel, Zustand (gewählt, aktiv, heute) mit ≥ 3:1 zur Umgebung oder einem Formmerkmal. Wie eine Auswahl aussieht, regelt A11Y-03. | WCAG 1.4.1 / 1.4.11. | manuell: R-A11Y |
| FARBE-06 | MUSS | Von M3 implizit gelesene Rollen sind gesetzt (`outline`, `error`, `onError`, `primaryContainer`). Jedes Token in `Color.kt`, `Dimens`, `Sizes`, `Motion` ist genutzt; seine KDoc stimmt. | Sonst M3-Baseline-Grau, tote Tokens. | auto: `theme-tokens` |
| FARBE-07 | MUSS | **Köpfe weiß:** Abschnitts-, Expander- und Kartentitel sowie Aufklapper (`SectionHeader`, `ExpanderSection`, `CardHeader`, `CardTitle`, `DisclosureToggle`) in `onSurface`, ohne Balken; Caret, Chevron, Zähler und Kopf-Aktionen gedimmt bzw. `onSurface`. Farbe eine Ebene darunter: `GroupLabel` (Familie), Kartenrand. Einzige Ausnahme: die Kapitelkarten in „Lernen“ (`NavCard`, `HeadingRing`). | Eine Kopf-Art liest sich ruhig; Farbe bleibt Bedeutung ([DECISIONS 2026-10-24](DECISIONS.md)). | auto: `head-color`; manuell: R-VISUAL |

## <a id="typografie"></a>3 Typografie

Quelle [Type.kt](../app/src/main/java/app/atemkraft/ui/theme/Type.kt): M3 `Typography()`, Roboto, **Größen sind Material-Standard** ohne eigenen Skalierfaktor (Leitplanke 5; DECISIONS 2026-10-19). Überschrieben sind nur Gewicht und Umbruch der sechs Überschrift-Rollen; die Systemschriftgröße wirkt zusätzlich.

| Rolle | sp (M3) | Gewicht | Zweck (normativ) |
|---|---|---|---|
| `headlineLarge` | 32/40 | Bold | derzeit ungenutzt |
| `headlineMedium` | 28/36 | Bold | Markenkopf „Atemkraft“ auf der Seite „Über“, `onBackground` wie jeder Kopf |
| `headlineSmall` | 24/32 | SemiBold | Titel der festen Kopfzeile aller Seiten (`ScreenHeader`, `PushHeader`), Abschluss-Titel, Phasenname im Kreis |
| `titleLarge` | 22/28 | SemiBold | `SectionHeader` (Titel und Anzahl-Suffix), `onSurface` |
| `titleMedium` | 16/24 | SemiBold | Karten- und Expander-Titel (`CardHeader`, `CardTitle`), Stepper-Wert |
| `titleSmall` | 14/20 | M3 (Medium) | Gruppenlabel (`GroupLabel`), Overlay-Titel, Mini-Bar-Status, Kennzahlen, Zeitring-Marken, gewählte Phase im Editor |
| `bodyMedium` | 14/20 | M3 | Einleitung der Push-Seiten (`ScreenSubtitle`), Suchfeld, Namensfeld, Kurzbeschreibung der Detailseite, Zeilen-Labels (`SubLabel`, `ToggleRow`, `Stepper`, Radioliste, Vorlagen), Phasen-Notiz, Name · Ort in der vāyu-Liste |
| `bodySmall` | 12/16 | M3 | Fließtext, Kartentext, Metazeile, Teaser, Dialogtext, Such-Status, Hinweise unter Kontrollen, Zitat, Datum, Footer, Taktangabe |
| `labelLarge` | 14/20 | M3 | Beschriftung der Tab-Leiste (`AppNavigationBar`, eine Stufe über der M3-Rolle `labelMedium`); implizit über M3: Beschriftung von Buttons, Segmenten, `LinkChip`, `ChoiceChip` |
| `labelMedium` | 12/16 | M3 | Eyebrow (Familie auf der Detailseite), Filter-Chips (`TonalChip`), `DisclosureToggle` (`onSurface`), „✓ Fertig“, „Aktiv“ (Stimme), Einheit unter der Zeit, Phasen-Beschriftung der Atemkurve, Suffix „· angepasst“ am Abschnittskopf, Beschriftung im vāyu-Bild (kursiv) |
| `labelSmall` | 11/16 | M3 | Info-Chips, DOI/PMID, Wochentage, Suffix „· angepasst“ in Knöpfen, Mini-Bar-Titel, Zeitring-Ziffern, Zeitleiste der Atemkurve |
| `displayMedium` / `displaySmall` | 45/52 / 36/44 | M3 | Zeit in der Mitte des Zeitrings (größter passender Stil, Rückfall bis `titleLarge`) / Countdown, Meditationszeit (`GlowText`) |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| TYPO-01 | MUSS | Schrift nur über `MaterialTheme.typography.<Rolle>`. Kein `.sp`, `fontSize`, `letterSpacing`, `lineHeight` oder `FontWeight` außerhalb `src/ui/theme/`; anderes Gewicht = neue Rolle. Kein Gewichtswechsel bei Auswahl. Ausnahmen mit Marker: `WholeWordText` und `SegmentedChoiceRow` verkleinern die übergebene Rolle; das gewählte Segment ist SemiBold ([DECISIONS 2026-10-20](DECISIONS.md)). | Skalierung an einer Stelle; Gewichtswechsel springt die Breite. | auto: `type-literal` |
| TYPO-02 | MUSS | Die Rolle folgt der Tabelle; eine neue Verwendung wird erst eingetragen, dann gebaut. | Lesbare Hierarchie. | manuell: R-CODE |
| TYPO-03 | MUSS | `headline*`- und `title*`-Rollen tragen in `Type.kt` `LineBreak.Heading` und `Hyphens.None`. **Keine Rolle schaltet Silbentrennung ein.** Ganze Wörter sichert das Layout (LAYOUT-03), nicht die Typografie. | Silbentrennung trennt auch Wörter, die ganz in die nächste Zeile passten; zu lange Wörter fängt das Layout ab. | manuell: R-CODE |

## <a id="layout"></a>4 Layout, Abstände, Formen

### 4.1 Abstände ([Dimens.kt](../app/src/main/java/app/atemkraft/ui/theme/Dimens.kt)), Raster 4 dp

Ränder und Karten-Innenabstand wie M3 (16 dp). Touch-Ziele hängen nie an diesen Werten: Eigene klickbare Flächen tragen `heightIn(min = MinTouchTarget)`; die Optik der Bedienelemente kommt aus §4.2. Keine Zwischenwerte, keine Summen „8 + 4“ am Aufrufort (LAYOUT-01, -02).

| Token | dp | Wann |
|---|---|---|
| `ScreenPadding` | 16 | Screen-Rand, Rand der Chip-Reihen und Blätter |
| `CardPadding` | 16 | Karten-Innenabstand |
| `ListGap` | 12 | **Karte ↔ Karte (immer)**, Listen, Button-Reihen (Start ↔ Reset), Rand einer Leiste bis zu ihrer ersten Tippfläche, Lupe → Text im Suchfeld |
| `HeaderGap` | 12 (= `ListGap`) | **Kopf → erster Inhalt**, unter jedem `SectionHeader` und `GroupLabel`: Der Kopf trägt unten keinen Abstand; Listen treffen ihn über `spacedBy(ListGap)`, Spalten über `CollapsibleSection` |
| `SectionGap` | 16 | senkrecht nur zwischen Blöcken **verschiedenen Typs** (Gruppenlabel ↔ Block darüber, oben auf jeder Push-Seite; auch innerhalb einer Karte: Titelzeile ↔ Grafik ↔ Text); waagerecht Name ↔ Schalter in `ToggleRow` |
| `SectionHeaderTop` | 24 | über einem `SectionHeader` |
| `ScreenTopSub` / `ScreenBottom` | 8 / 24 | oben über der festen Kopfzeile / unten auf allen Scroll-Screens |
| `TabBarGripClearance` | 48 (= `MinTouchTarget`) | unten auf jeder Seite der Tiefe statt `ScreenBottom`: Platz für die Tippfläche des Griffs der Tab-Leiste ([spec/navigation.md](spec/navigation.md)) |
| `GapSmall` | 8 | Titel → Aktionen/Zeichen, Abschnittstitel → Teaser bzw. Intro, Label → Steuerung/Hinweis, Steuerzeile → Steuerzeile (jede genau `MinTouchTarget` hoch), Chips nebeneinander, Feld → Chip-Reihe, Werte → Kurve, Rand über der Tippfläche eines tippbaren Kopfs |
| `GapTiny` | 4 | Titel → Metazeile, Icon ↔ Text in einer Zeile, Hinweis unter einer Zeile |
| `GapHairline` | 2 | Auswahlzeile ↔ Auswahlzeile einer `selectableGroup` mit Fläche (`selectedRow`), enge Divider, Rest des Überstands einer Kopf-Aktion |
| `ChipPadding` | 12 | waagerechtes Polster der Anlass- und Filter-Chips (`ChoiceChip`, `TonalChip`) |
| `MinTouchTarget` | 48 | Mindesthöhe eigener klickbarer Flächen, Höhe der Kopfzeile in der Karte |
| `BorderThin` / `BorderChip` | 1 / 1,5 | Kartenrand (`accentBorder`), Rand von `AppOutlinedButton`, Info-Chips und ∞-Knopf / Rand von `LinkChip` und `ChoiceChip` in `outline` |

Feature-Maße mit Wert und Zweck in der KDoc von `Dimens`: `SessionPadding`, `SessionRingStroke`, `SessionMainButton`/`SessionSideButton`, `SessionMainIcon`/`SessionSideIcon`, `SessionControlGap`, `OverlayIconButton`, `SessionCircleMin`/`WidthShare`/`HeightShare`, `DurationPickerMin`, `ContentMaxWidth`, `PinnedBarElevation`, `FocusRing`, `BorderSection`, `ScrollEdgeFade`.

### 4.2 Größen ([Sizes.kt](../app/src/main/java/app/atemkraft/ui/theme/Sizes.kt))

**Bedienelemente haben Material-3-Größe.** Die **Tippfläche** ist `Dimens.MinTouchTarget` 48 (A11Y-05). Größen von Bedienelementen setzen nur die Komponenten in `src/ui/components/`, an M3-Elementen nur als `heightIn(min = …)` (LAYOUT-04); Zeichen- und Grafikmaße (Kurve, Punkte, Icons in Feature-Zeichnungen) dürfen auch Feature-Dateien setzen. Ob Optik und Tippfläche stimmen, prüfen die Gates (`TIPPFLAECHE`, `BESCHNITTEN`, `ANATOMIE`).

| Token | dp (M3) | Wofür |
|---|---|---|
| `ButtonHeight` | 40 | Mindesthöhe von Buttons, Segmenten, `DisclosureToggle`, `ChoiceChip` (Anlass, eine Antwort) |
| `ChipHeight` | 32 | `LinkChip`, `TonalChip` (Filter, mehrere) |
| `ButtonMinWidth` | 58 | M3-Mindestbreite; feste Breite des Reset im `StartSplitButton` (`ResetWidth`) |
| `IconButtonSize` | 40 | Kreis von `AppIconButton`, `AppIconToggle`, `SoundSwitch`, Stepper-Knöpfen |
| `SwitchWidth` | 52 | M3-Switch; der Hinweis einer `ToggleRow` endet davor |
| `IconDefault` / `IconInButton` / `IconSmall` | 24 / 18 / 16 | Icon in Tippfläche 48 / Icon neben Beschriftung / Zeichen auf Karten neben `label*`-Text |
| `TouchOverhang` / `CircleOverhang` | 12 / 4 | Überstand der Tippfläche 48 über ein `IconDefault`-Icon bzw. einen `IconButtonSize`-Kreis; um ihn melden `flushStart`/`flushEnd` bzw. der Stepper sich schmaler |

Feature- und Zeichenmaße mit Wert und Zweck in der KDoc von `Sizes`: `TabBarGripWidth`/`TabBarGripHeight`, `TabBarSwipeDistance`/`TabBarSwipeVelocity`/`TabBarSwipeBand`/`TabBarFallbackTravel`, `SegmentIconReserve`, `StepperValueMinWidth`, `ProgressInline`/`ProgressStroke`, `WeekDot`, `FamilyDot`, `PauseFlashSize`, `Curve*`, `MiniCurve*`, `RhythmCurve*`, `VayuFigureMaxWidth`.

Switch und RadioButton stehen in M3-Größe. `AppTextButton` trägt `shapes.large` (20 dp = halbe `ButtonHeight`): mehrzeilig schneidet keine große Rundung Glyphen an. Eigene Maße außerhalb von `Sizes`: `NavigationBar` (M3 1.4: Höhe mindestens 80, wächst mit der Beschriftung; Indikator 64 × 32), die Icons von `OverlayChrome` und die `SessionControlBar` (Bedienung mit halb geschlossenen Augen, `Dimens.Session*`). Kartenrand nur über `accentBorder(color)`; gestrichelter Rand über `dashedBorder(color, shape, width = BorderThin, dash = GapTiny)` (`CardBorder.kt`).

### 4.3 Formen und Screen-Gerüst

| Shape | Radius | Wofür |
|---|---|---|
| `extraSmall` / `small` / `medium` / `large` / `extraLarge` | 8 / 12 / 16 / 20 / 28 dp | Info-Chip / `ChoiceChip`, gewählte Listenzeile / Card, Leer-Karte / `AppTextButton`, `AppOutlinedButton` / Dialog |
| `CircleShape` | – | Punkte, Buttons, Pillen (`SearchField`, `TonalChip`) |

| Typ | Container | oben | Kopf | unten |
|---|---|---|---|---|
| Tab | `TabListScaffold(header, top = 0)`: `Surface(background)` → Kopf-Slot und `LazyColumn(weight(1f), spacedBy(ListGap))`, horizontal `ScreenPadding` als `contentPadding` (nicht als Modifier: bündige Icon-Buttons ragen in den Rand); kein Untertitel. **Der erste Eintrag ist Inhalt, nie ein Abschnittskopf** – der Seitentitel benennt schon (Atmen: Suchfeld, Meins: Woche, Lernen: Kapitelkarte); Push-Seiten beginnen dagegen mit ihrem ersten `SectionHeader` (`headerTop = 0`) | `ScreenTopSub` (im `ScreenHeader`) | `ScreenHeader`: Titel `headlineSmall`, rechts höchstens eine Icon-Aktion (`SettingsAction`). **Rahmen fest, Inhalt scrollt:** Kopf und Leisten scrollen nie; keine Linie unter dem Kopf und über der Tab-Leiste, auch nicht beim Scrollen – das Violett grenzt ab (Eigentümer 2026-10-31). Überschriften in `StickyHeader` kleben oben (`StickyHeaderOverlay`, beide Gerüste). Tabs: Atmen, Meins, Meditation (eigenes Gerüst, LAYOUT-08), Lernen ([spec/](spec/)) | `ScreenBottom` als `contentPadding` (`bottom`) |
| Push | `PushScaffold(onBack, title \| titleContent, action, bottomBar)`: `PushHeader` (fest), `Column(weight(1f)).verticalScroll` mit Rand `ScreenPadding` (in einer `Box` mit der Schicht der klebenden Überschriften), darunter fest optional `bottomBar` (`PinnedActionBar`). Einleitung als `ScreenSubtitle` (Atemlehre, Pranayama, Über). Logbuch: `TabListScaffold` mit `PushHeader`, `top = SectionGap`; Muster-Editor: eigenes Gerüst | `ScreenTopSub`, dann `SectionGap` bis zum ersten Inhalt (erster `SectionHeader` mit `headerTop = 0`) | `PushHeader`: `BackButton` und Titel `headlineSmall` (Laufschrift statt Umbruch), rechts höchstens eine `action` (Detail: Markierung bzw. Stift; Editor: ◉ „In „Meins“ speichern“) | `TabBarGripClearance` (Logbuch als `bottom`): Am Ende liegt nichts unter dem Griff der Tab-Leiste |
| Vollbild-Session | `OverlayChrome` → Inhalt mit `SessionPadding` ([spec/session.md](spec/session.md)) | `safeDrawing` | Minimieren links, rechts „Danach in die Stille“ (nur Atem-Session) und Ton | – |

### 4.4 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| LAYOUT-01 | MUSS | Abstände nur über `Dimens`, Größen über `Sizes`, Formen über `MaterialTheme.shapes.*` oder `CircleShape`. Keine `.dp`-Literale (außer `0.dp`), kein `RoundedCornerShape`/`CutCornerShape`, kein `Dp.Hairline` außerhalb `src/ui/theme/`. Nie eckige Schnitte. | Ein Raster, eine Quelle ([DECISIONS 2026-10-03](DECISIONS.md)). | auto: `layout-literal` |
| LAYOUT-02 | MUSS | Screens folgen dem Gerüst §4.3 und der Abstandsskala §4.1: Karte ↔ Karte immer `ListGap`, `SectionGap` nur zwischen Blöcken verschiedenen Typs, Kopf → Inhalt `HeaderGap`. Insets nur über `Scaffold`-`innerPadding` oder `safeDrawingPadding()`, keine festen Leistenhöhen. | Nachgebaute Kopf-Blöcke, drei Kartenabstände; Edge-to-edge ab API 35. | auto: `ScreenshotTest` (`ERSTER_KOPF`: Tab beginnt mit Inhalt); manuell: R-VISUAL |
| LAYOUT-03 | MUSS | **Kein Wort bricht um, kein Text wird gekürzt** – auf 360–412 dp bei fontScale 1,0–2,0, quer und Tablet; keine Zeile beginnt im Wort oder mit Satzzeichen (`badBreakAt`); keine runde Form schneidet Glyphen an. Mittel in der Reihenfolge von §4.5 „Platz für Text“. Verboten: Ellipse auf Inhalt (Ausnahme `MarqueeText`), feste Höhe für Text, `weight(fill = false)` neben Chips, Orientierungssperre. | Halbe Wörter und gekürzte Hinweise sind unlesbar ([DECISIONS 2026-10-03](DECISIONS.md)); WCAG 1.4.4, 1.4.10. | auto: `ScreenshotTest` (`WORTBRUCH`, `ABGESCHNITTEN`, `CHIP_UMBRUCH`, `ZUGESCHNITTEN`), `weight-nofill`, `platform-override` |
| LAYOUT-04 | MUSS | **Bedienelemente:** sichtbare Größe nur aus `Sizes`, gesetzt nur in `src/ui/components/`; M3-Steuerelemente nur dort, M3-Größenkonstanten nur in `src/ui/theme/`. An M3-Elementen nur `heightIn(min)`, eigene feste Größe nur `minimumInteractiveComponentSize().size(x)`, kein `scale`/`graphicsLayer` auf Bedienbarem, gleiche Höhe nie über `IntrinsicSize.Min`. **Ein Größentoken je Leiste** (§4.5). | `height` nahm die 48-dp-Reservierung, `scale` den Hit-Test ([DECISIONS 2026-10-22](DECISIONS.md)). | auto: `m3-size-const`, `control-direct`, `control-scale`, `touch-order`, `layout-literal`, `ScreenshotTest` (`TIPPFLAECHE`) |
| LAYOUT-05 | MUSS | **Bedienelemente bleiben ganz.** Kein Bedienelement wird beschnitten oder aus dem Bild geschoben: Abschneiden darf nur eine Scroll-Fläche in ihrer Richtung – senkrecht die Seite, waagerecht einzig die Chip-Reihen (`ChoiceChipRow`, `FilterChipRow`); Beschriftung und Icon bleiben im sichtbaren Behälter, bei fontScale 2,0 wächst er mit. | Kleinere Tokens und feste Höhen machen genau das wahrscheinlich; LAYOUT-03 prüft nur Text. | auto: `ScreenshotTest` (`BESCHNITTEN`, `AUSSERHALB`) |
| LAYOUT-07 | SOLL | **Steuerung beim Label, Glyph bündig:** Die Steuerung einer Zeile steht als rechte Spalte, ihre *sichtbare* Kante an der Inhaltskante, gebaut nach §4.5 „Bündigkeit“ und „Zeile mit Steuerelement“ (kein `offset`, kein clippender Behälter, Hinweis endet vor dem Schalter). Feste Endspalten (`TrailingRow`) halten in jedem Zustand ihre Breite ([spec/einstellungen.md](spec/einstellungen.md)). | Weit abgesetzte Steuerung liest sich als fremde Zeile ([DECISIONS 2026-10-05](DECISIONS.md)). | auto: `ScreenshotTest` (`ANATOMIE`); manuell: R-VISUAL |
| LAYOUT-08 | MUSS | **Meditations-Tab ohne Scrollen.** Kopf, Zeitring, „Klang & Stimme“ und Start passen bis fontScale 1,3 auf jedem Gerät der Matrix in die Höhe über Tab-Leiste und Systemleisten, in jedem Zustand; der Zeitring nimmt den Rest (`FillViewportColumn`, Mindestkante `DurationPickerMin`/`timeDialMinSide`). Nur bei fontScale 2,0 darf die Seite als Notlösung scrollen – abgeschnitten wird nie. | Eine ruhige Seite, alles auf einen Blick; ohne Leisten im Test wurde der Platz um rund 150 dp überschätzt. | auto: `ScreenshotTest` (`SCROLLT` an `03_meditation_fein`), `DeviceFitTest` (`03_meditation`, `RING`) |

### <a id="anatomie"></a>4.5 Anatomie der Bausteine (Layout-Grammatik)

Für jeden wiederkehrenden Baustein gilt eine verbindliche Anatomie: Anordnung, Position und Größenverhältnisse werden nachgeschlagen, nicht erfunden (PROZ-07). Die Anatomie lebt an **einer** Stelle im Code – Kartenköpfe (auch Expander und Einstellungskarten) nur über `CardHeader`, Abschnittsköpfe über `SectionHeader`/`GroupLabel` und deren gemeinsame `TitleRow`, Blätter über `AppBottomSheet` –, und das Gate `ANATOMIE` im `ScreenshotTest` misst sie an jedem Bild nach (Test-Tags `CARD_HEADER_TAG`, `TITLE_ROW_TAG`, `TITLE_ROW_TITLE_TAG`, `TITLE_ROW_ACTIONS_TAG`, `TITLE_ROW_INDICATOR_TAG`, `CONTROL_ROW_TAG`). Statisch wacht `anatomy-overlay`: Ein Overlay (`align(Alignment.TopEnd)`) gibt es außerhalb von `CardHeader` nicht mehr. Feature-Anatomien, die nur an einer Stelle vorkommen: Antwortreihe am Ende → [spec/session.md](spec/session.md#antwortreihe), Kurvenkarte des Editors → [spec/muster-editor.md](spec/muster-editor.md#kurvenkarte), Rhythmuskurve und Leiste der Detailseite → [spec/detail.md](spec/detail.md#rhythmuskurve).

#### <a id="anat-regeln"></a>Allgemeine Positionsregeln

| Regel | Was gilt | Prüfung |
|---|---|---|
| **Bündigkeit** | Glyphen stehen bündig mit der Textkante, nie die Tippfläche: Icon-Buttons über `flushEnd`/`flushStart` (Überstand `TouchOverhang` 12), Knopf-Kreise über `CircleOverhang` 4, Text-Buttons reichen ihr Polster über `flushStart(inCard)` in den Rand. In Karten höchstens `CardPadding` Überstand, damit die Tippfläche nicht über den Kartenrand ragt (die Karte clippt). Kein `offset`: Nachbarn rücken mit. | `ANATOMIE` (Glyph bündig ± 1 dp), `TIPPFLAECHE`, `BESCHNITTEN` |
| **Vertikale Mitte auf der ersten Textzeile** | Aktionen, Zeichen (Atemkreis, Chevron, Caret) und Status stehen mittig zur **ersten** Titelzeile (`rememberFirstLineHeight(style)`, gemessen – ab Android 14 wächst große Schrift nichtlinear), nicht mittig zum ganzen Block; bricht der Titel um oder rutschen Chips darunter, bleiben sie oben. | `ANATOMIE` (Glyph-Mitte ± 4 dp) |
| **Größenverhältnisse** | Icon 24 (`IconDefault`) in Tippfläche 48 (`MinTouchTarget`); Kreis-Knopf 40 (`IconButtonSize`) in 48; Icon neben Text 18 (`IconInButton`); Zeichen auf Karten 16 (`IconSmall`). Titel `titleMedium` zu Metazeile/Kurztext `bodySmall`, Abschnittskopf `titleLarge` zu Gruppenlabel `titleSmall`; Chip-Höhen 40 (Anlass, eine Antwort) / 32 (Filter, mehrere). Nachbarn in einer Leiste (waagerechte Reihe gleichrangiger Bedienelemente) teilen **ein** Größentoken und sind gleich hoch; hervorgehoben wird durch Füllung oder Farbe (LAYOUT-04). Einzige Ausnahme: die `SessionControlBar` (Pause 76, Seitenknöpfe 56; Bedienung mit halb geschlossenen Augen). | `m3-size-const`, `control-direct`, `TIPPFLAECHE`; Tokenvergleich im Code |
| **Platz für Text** | Passt ein Text nicht (LAYOUT-03), in dieser Reihenfolge: (1) Layout gibt Platz (`TitleWithChips`, `AdaptiveButtonRow`, `InlineOrBelow`, Reserven), (2) `WholeWordText` verkleinert bis 70 % (Chips, Tabs bis 85 %, dann Layoutwechsel), (3) Text umformulieren. | `WORTBRUCH`, `ABGESCHNITTEN`, `CHIP_UMBRUCH` |
| **Nichts springt** | Wo ein Zustand wechselt (Reset erscheint, „· angepasst“, Vorschau läuft, X im Suchfeld, Startpunkt-Name), hält eine Reserve den Platz des breitesten Inhalts (`widestOf`, `rememberWidestTextWidth`, `leadingReserve`/`endReserve`, Platzhalter mit gleicher Breite); Höhe und Lage der Nachbarn ändern sich nicht. | `ScreenshotTest` (`chipLayout`, `KOMPAKT_FEHLT`); R-VISUAL |
| **Overlay nur über der Titelzeile** | Eine Aktion liegt nie frei über Fließtext oder in einer Ecke „irgendwo“: Sie steht auf der Titelzeile (`CardHeader`) oder im Kopf (`SectionHeader`, `GroupLabel`, `ScreenHeader`, `PushHeader`), ihre 48-dp-Tippfläche ragt nach unten in den Abstand bzw. über die Metazeile, der Baustein wird dadurch nicht höher. | `anatomy-overlay`, `ANATOMIE` |

#### <a id="anat-karte"></a>Karte mit Aktionen (`ExerciseCard`, `PatternCard`, `DailyPatternCard`, `WelcomeCard`, `NavCard`; Kopf von `ExpanderSection`/`ExpanderCard` und `SettingsCard`)

```
┌ Card(onClick?) · Rand accentBorder(Bedeutung) · Polster CardPadding ──────────────────┐
│ Titel titleMedium [Chip] [Chip]          ·····GapSmall·····  (◌)  [⊕ 48][⊗ 48] ←Glyph bündig │
│   ↑ hält rechts MinTouchTarget·n − TouchOverhang + GapSmall frei      ↑ Mitte = 1. Titelzeile │
│ Metazeile bodySmall BODY ········································ bis zur Inhaltskante ─────→ │
│ Kurztext bodySmall SECONDARY ····································· läuft unter den Aktionen ─→ │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

- **Komponente:** `CardHeader(title, titleColor, chips, indicator, actionCount, heading, marks, actions)` als erste Zeile der Kartenspalte; der Inhalt darunter ist Geschwister in voller Breite. Niemand baut diese Zeile nach. Expander: Titel und Caret als `indicator` mittig auf der ersten Titelzeile, Teaser darunter in voller Breite, `marks` für Sanskrit; Einstellungskarte: nur Titel mit `heading`.
- **Titelzeile** (`TitleWithChips`) hält rechts die Breite der Aktionen frei: `MinTouchTarget × n − TouchOverhang + GapSmall`. **Fließtext und Metazeile laufen darunter durch** bis zur Inhaltskante.
- **Aktions-Glyphen** stehen mittig auf der ersten Titelzeile (`centeredOverflow(titleLine / 2)`) und bündig mit der Inhaltskante (`flushEnd(inCard = true)`); die Tippfläche ragt oben ins Polster und unten über die Metazeile – die **Karte wird durch Aktionen nicht höher**. Höchstens zwei Aktionen, zerstörende außen (`ConfirmIconButton` rechts vom Stift); Tint `onSurface` · `SECONDARY`, Zustandsakzent gefüllt in `secondary` (◉ „In „Meins“ speichern“). **Zeichen** (`indicator`, nicht klickbar: Atemkreis „tut mir gut“ `IconSmall`, `ForwardChevron`, „öffnet im Browser“) stehen in einer Box von Titelzeilenhöhe am Zeilenende.
- **Prüfung:** `ANATOMIE` – (1) Glyph über keinem Text, Tippfläche nicht über dem Titel, (2) mit Aktionen: mehrzeiliger Text reicht bis zur Inhaltskante, (3) Glyph-Mitte in der ersten Titelzeile ± 4 dp, (4) Glyph bündig ± 1 dp; `TIPPFLAECHE` 48 dp; `anatomy-overlay`.

#### <a id="anat-kopf"></a>Abschnittskopf (`SectionHeader`, `CollapsibleSection`, `GroupLabel`)

```
 SectionHeaderTop 24 (bzw. GapSmall über der Tippfläche, wenn tippbar; top = 0: keiner, als erstes Element einer Fläche mit eigenem Rand oder wo darüber schon genug steht)
 Titel titleLarge onSurface · Status SECONDARY  ⌄(Caret, Mitte 1. Zeile)        [+ 48] ←Glyph bündig
 Teaser bodySmall SECONDARY (ganzer Satz)                                      ↑ ragt ≤ HeaderGap − 2 nach unten
 HeaderGap 12
 ┌ erster Inhalt ┐
```

- **Komponente:** `TitleRow` (intern in `SectionHeader.kt`) trägt Zeile, Test-Tag und Aktion für `SectionHeader` und `GroupLabel`; niemand baut sie nach. Titel links (`weight(1f)`), **genau eine** Aktion rechts als 48-dp-`AppIconButton` in `onSurface`, Glyph bündig mit der Inhaltskante (`flushEnd`), Mitte auf der ersten Titelzeile; Caret bleibt am Titel, die Tippfläche des Auf-/Zuklappens endet vor der Aktion (zwei Bedienelemente, kein Button im Button).
- Kopf → Inhalt `HeaderGap` (Listen `spacedBy(ListGap)`, Spalten `CollapsibleSection`); Abschnitt → Abschnitt `SectionHeaderTop`/`SectionGap`. Der Kopf trägt unten keinen Abstand; die Tippfläche der Aktion ragt höchstens `HeaderGap − GapHairline` hinein (`centeredOverflow` mit Grenze), sonst wird der Kopf höher.
- `GroupLabel`: `titleSmall` in der Familienfarbe, `top = SectionGap` (in Listen abzüglich `ListGap`), Aktion wie oben (✕ „Alle zeigen“).
- Tippbare Köpfe (`expanded`, `onClick`): ganze Titelspalte samt Teaser ≥ 48 dp, Chevron statt Caret bei `onClick`.
- `leading` (nur mit `onClick`, nur „Muster des Tages ›“): Zeichen in einer Box von Titelzeilenhöhe, Mitte auf der ersten Titelzeile, `GapSmall` zum Titel; dekorativ, Teil der einen Tippfläche, keine eigene Ansage.
- **Prüfung:** `ANATOMIE` (3, 4, tippbare Zeile ≥ 48), `semantics-heading`, `head-color`.

#### <a id="anat-steuerzeile"></a>Zeile mit Steuerelement (`ToggleRow`, `Stepper`, `CompactStepper`, Einstellungs-Zeilen `TrailingRow`)

```
 Label bodyMedium (weight 1f) ···················· SectionGap ···· [Switch 52]  ≥ 48 dp hoch, Mitte
 Hinweis bodySmall SECONDARY ─── endet vor dem Schalter (end = SwitchWidth + Abstand)
 Label ·········································· (−)  48  (+)   Kreis von „+“ bündig (CircleOverhang)
```

- Label links mit `weight(1f)`, Steuerelement rechts, Mindesthöhe `MinTouchTarget`, vertikal zentriert; ganze Zeile ist die Tippfläche (`toggleable`), Switch und Radio als reine Anzeige in M3-Größe.
- Glyph bzw. Kreis bündig: Icon-Buttons `flushEnd(inCard = true)`, Stepper-Kreis um `CircleOverhang` eingerückt. Feste Endspalten halten in jedem Zustand ihre Breite ([spec/einstellungen.md](spec/einstellungen.md)).
- **Prüfung:** `ANATOMIE` (Steuerzeile ≥ 48 dp), `switch-unlabeled`, `TIPPFLAECHE`.

#### <a id="anat-chips"></a>Chip-Reihen (`ChoiceChipRow` Anlässe, `FilterChipRow` Filter)

```
 [Stress 40][Kurzatmig][Einschlafen]      ← Anlass: ChoiceChip 40 dp, shapes.small, Rand, eine Antwort
 [Fokus][Wach werden][Erschöpft]▒▒        ← höchstens zwei Zeilen, GapSmall, sonst gemeinsam seitlich scrollend
 (Bis 3 min)(Ohne Halten)(Aus dem Yoga)   ← Filter: TonalChip 32 dp, Pille, tonal, mehrere; eine Zeile
```

- Anlass-Chips 40 dp mit Rand (eine Antwort), Filter-Chips 32 dp tonal (mehrere); Abstand `GapSmall` nebeneinander und untereinander (sichtbar); Reihe bündig mit `ScreenPadding`/`CardPadding`, die Scroll-Fläche reicht bis an den Bildschirmrand und läuft über `ScrollEdgeFade` aus. Anlässe höchstens zwei Zeilen, Filter genau eine. Chips messen in jedem Zustand gleich; nie Umbruch im Chip ([spec/suche.md](spec/suche.md), [spec/atmen-tab.md](spec/atmen-tab.md)).
- Ausnahme `FilterChipFlow` (Wochentage der Erinnerung in einer Karte): dieselben `TonalChip`s brechen zwischen ganzen Chips um statt seitlich zu scrollen – alle sieben bleiben sichtbar, keine Kartenkante schneidet einen an.
- **Prüfung:** `ScreenshotTest` (`chipLayout`, `chipRows`, `filterRow`, `CHIP_UMBRUCH`), `no-check-icon`.

#### <a id="anat-leiste"></a>Knopfleiste unten (`StartBar`: `PinnedActionBar`, Zeile `SectionHeader`, `StartSplitButton`)

```
 ┌ PinnedActionBar surface · Elevation 3 · Rand ScreenPadding × ListGap ─────────────────┐
 │ Anpassen · 10 min ›  bzw.  Dauer · 5 min ›   SectionHeader(onClick, top = 0), öffnet ein Blatt │
 │ [ Starten · ca. 5 min ····· füllt den Rest ·····] ListGap [↻ 40]   (Reset nur bei Abweichung)│
 │ knapp in der Höhe:  10 min ›  ListGap  [ Starten · ca. 5 min ··· Rest ···] [↻ 40]            │
 └──────────────────────────────────────────────────────────────────────────────────────────┘
```

- Feste Breiten für Stepper und Reset, Start füllt den Rest; Start und Reset gleich hoch über dieselbe Mindesthöhe (`ButtonHeight`), nie `IntrinsicSize.Min` – wird „Starten“ zweizeilig, wächst der Reset um dieselben Zeilen (`SlidingReset(lines)`). Die Zeilen entscheidet `StartSplitButton` in einer Komposition aus der Restbreite (Reset fest `ResetWidth`), nicht per Rückmeldung aus der Messphase: Beide stehen vom ersten Frame an gleich hoch; solange der Reset hinausgleitet, zählt seine Breite mit. Bei Platzmangel `InlineOrBelow`: Steuerung rechtsbündig darunter, entschieden mit Reserven.
- **Ein Bauteil:** Detailseite und Muster-Editor zeigen dieselbe `StartBar` – oben genau eine Zeile, die ein Blatt öffnet („Anpassen · 10 min ›“ bzw. „Dauer · 5 min ›“, `SectionHeader` mit `onClick`, `top = 0`), darunter der Start; erst die Einstellung, dann die Aktion, kein runder Einzelknopf (wirkt wie ein FAB, PRIN-02). Knapp in der Höhe (Höhe, die die Seite der Leiste anbietet, ÷ Schriftgröße unter `StartBarCompactBelow` 450: 360 × 640 bei Schrift 1,3, quer) und wenn beides nebeneinander passt, steht alles in einer Zeile – links nur „10 min ›“ (dieselbe Schaltfläche, Ansage „Anpassen, 10 Minuten“, dasselbe Blatt), rechts der Start, der dann nur „Starten“ sagt, wenn „Starten · ca. 5 min“ nicht in eine Zeile passt; das entscheidet die Leiste, nicht der Aufrufer. Meditation: nur der `StartSplitButton`; in der Tiefe schwebt über der Leiste der Griff der Tab-Leiste (unten), nichts überlappt. Aufbau: [spec/detail.md](spec/detail.md#anatomie-leiste).
- **Prüfung:** `ScreenshotTest` (`TIPPFLAECHE`, `ANATOMIE`, `08_detail_quer`, `08_detail_tiefe*` mit `GRIFF`; Leiste einzeilig bis 1,3 im `DeviceFitTest`).

#### <a id="anat-sheet"></a>Bottom Sheet (`AppBottomSheet`: `SoundSheet`, `AdjustSheet`, `TemplateSheet`, `DailyPatternSheet`)

```
 ModalBottomSheet · surface · ganz geöffnet · ScreenPadding 16 seitlich · ScreenBottom 24 unten
                         ▬▬                      ← Griff M3 (32 × 4 mittig in 48 dp), stumm für TalkBack
 Titel titleLarge onSurface                      ← SectionHeader(top = 0), TalkBack beginnt hier
 GapSmall · Intro bodySmall SECONDARY            ← optional, Teaser des Titels, ≤ 95 Zeichen
 HeaderGap 12 · SubLabel bodyMedium              ← Gruppe
 GapSmall · Steuerzeile (Segmente, Stepper) 48   ← je GapSmall; ⌄ Aufklapper onSurface
 [ Karte ]                                       ← nur Objekte, die man öffnet oder startet
```

- **Komponente:** `AppBottomSheet(title, onDismiss, intro, content)` – Fläche `surface`, eigener stummer Griff (M3 `BottomSheetDefaults.DragHandle`; der Titel folgt ohne weiteren Abstand), scrollende Spalte, Titel als `SectionHeader` mit `top = 0`, Intro als dessen Teaser, Kopf → Inhalt `HeaderGap`. Kein Blatt ruft `ModalBottomSheet` oder `rememberModalBottomSheetState` selbst auf; den Zustand hält das Gerüst. Schließen per Ziehen: Schwellen und Einrasten des M3-Blatts (56 dp bzw. 125 dp/s), dazu ein kleiner Impuls, sobald das Blatt beim Loslassen schließen würde, und das Gegenstück beim Zurückziehen (`gestureThreshold`; Schwelle aus dem M3-Zustand `targetValue`) – nur solange ein Finger darauf liegt, nicht bei Zurück, Tipp daneben oder `closeThen`; gilt für jedes Blatt, keines sperrt das Schließen.
- **Erst schließen, dann handeln:** Was woandershin führt oder etwas Großes öffnet (Karte → Detailseite, „Selbst gestalten“ → Editor, „Stimme laden ›“, eine Wahl, nach der der Fokus wandert), ruft `closeThen { … }` aus `AppBottomSheetScope`: Das Blatt gleitet sichtbar hinaus, die Aktion läuft erst nach seinem Abbau (Ablauf in der KDoc). Wischen, Tipp daneben und Zurück schließen; Steuerungen, die sofort gelten (Stepper, Schalter, ◉, ↻), lassen das Blatt offen.
- **Steuerungen ohne Karte, eine Textkante:** `Stepper`, `ToggleRow`, `SegmentedChoiceRow` und Auswahlzeilen stehen direkt auf dem Blatt (das Blatt ist schon die Fläche); Karten nur für Objekte, die man öffnet oder startet (`PatternCard`, `ExerciseCard`, `NavCard`). Titel, Intro, `SubLabel` und Zeilen-Labels beginnen bei `ScreenPadding`; Flächen (`selectedRow`) dürfen in den Rand ragen, Text nicht (`bleed`).
- **Gliederung nur über `SubLabel` und Abstand:** Label → Steuerung, Steuerung → nächste Gruppe und Steuerzeile → Steuerzeile `GapSmall` (der 48-dp-Rand zählt sichtbar mit); Auswahlzeilen mit Fläche (`selectedRow`) untereinander `GapHairline`; jede Stepper-Zeile genau `MinTouchTarget` hoch, ohne eigenes Polster; Karte ↔ Karte `ListGap`; keine Trenner zwischen Steuerzeilen, keine zweite Kopf-Ebene in `titleLarge`. Zeilen-Labels `bodyMedium` `onSurface`; die Einheit steht beim Wert („5 s“), nicht in Klammern am Label.
- **Farbe:** pink nur Auswahl und Aktion (FARBE-04: Switch an, Start); Verweise woandershin in `secondary` („Stimme laden ›“); Aufklapper (`DisclosureToggle`) in `onSurface`, Caret gedimmt, mittig auf der ersten Textzeile. **Intro** nur, wenn die Wahl eine Folge hat, die man nicht sieht (Startpunkt ersetzt den Ablauf, das Tagesmuster speichert man über ◉); ganze Sätze, höchstens 95 Zeichen, immer als Teaser des Titels.
- **Prüfung:** `sheet-direct` (auch `rememberModalBottomSheetState` nur im Gerüst), `ScreenshotTest` (`*_offen`: `WORTBRUCH`, `TIPPFLAECHE`, `ANATOMIE` der Steuerzeilen), R-A11Y (Fokus beim Öffnen auf dem Titel, Zurück schließt), R-VISUAL (Textkante, keine Karte um Steuerungen).

#### <a id="anat-pille"></a>Suchpille, Tab-Leiste, Overlay-Kopf

| Baustein | Anatomie | Prüfung |
|---|---|---|
| **Suchpille** (`SearchField`) | Pille `CircleShape`, Höhe ≥ 48, Fläche `surfaceContainerHigh`, ohne Rand; Lupe `IconDefault` links mit `CardPadding`, `ListGap` zum Text `bodyMedium`; X als `AppIconButton` nur mit Eingabe, Kreis `CardPadding` vor dem Ende (Tippfläche ragt um `CircleOverhang` darüber), Platzhalter hält seine Breite. | `ANATOMIE` (Steuerzeile ≥ 48), `TIPPFLAECHE` |
| **Tab-Leiste** (`AppNavigationBar`, `HideableTabBar`, `TabBarToggle`) | M3 `NavigationBar` mindestens 80 hoch, Beschriftung `labelLarge` (bis 85 %, dann nur Icons, Name am Icon), keine Linie darüber, Auswahl: Pille `TonalDeep`, Icon und Beschriftung `primary`. In der Tiefe: Griff `TabBarGripWidth` × `TabBarGripHeight` (`CircleShape`, `onSurface` `SECONDARY`) mittig über der Oberkante der untersten Leiste, `GapSmall` darüber; Tippfläche 48 × 48 **ganz über der Kante**, sie überdeckt nichts in der Leiste; Seiten halten unten `TabBarGripClearance` frei. Wischen öffnet und schließt, die Leiste folgt dabei dem Finger und rastet mit `Motion.TabBar` ein (Strecke `TabBarSwipeDistance` oder Tempo `TabBarSwipeVelocity`, ein Impuls an der Schwelle, das Gegenstück beim Zurückziehen); mit Bedienhilfe steht sie in der Tiefe offen: [spec/navigation.md](spec/navigation.md). | `TabNavigationTest` (Wischen, Tipp), `ScreenshotTest` (`TabFrame`, `08_detail_tiefe*`: `ANATOMIE` „Griff über …“) |
| **Overlay-Kopf** (`OverlayChrome`) | Rand `safeDrawing` und Systemleisten (Inhalt darunter mit `SessionPadding`); links Minimieren (M3 `IconButton`, Kreis 40), rechts Stille-Schalter und Ton als `OverlayIconButton` 40, alle in Tippfläche 48; kein sichtbarer Titel – `title` ist der `paneTitle`, der Übungsname steht im Inhalt; blendet in der Ruhe-Ansicht aus, Platz bleibt. | `ScreenshotTest` (`09_*`, `10_*`), `pane-title` |

## <a id="komponenten"></a>5 Komponenten

### 5.1 Katalog

Auswahl für den Bau: welche Komponente wofür, was sie nicht ist (→ Alternative) und ihr A11y-Kern. Bauweise, Parameter und Maße stehen in der KDoc, Feature-Einzelheiten in [docs/spec/](spec/), Geometrie in §4.5. Jede öffentliche `fun` in `ui/components/` kommt hier vor (`catalog-sync`).

| Komponente (Datei) | Wofür | Nicht für → | A11y-Kern |
|---|---|---|---|
| `ScreenHeader` / `SettingsAction` / `TabListScaffold` / `ScreenSubtitle` (`ScreenHeader.kt`) | fester Kopf und Gerüst der Tabs und des Logbuchs, rechts höchstens das Zahnrad; Einleitung einer Push-Seite (`bodyMedium` `SECONDARY`) | Push-Seiten → `PushHeader`; Untertitel im Tab-Kopf; Meditations-Tab (eigenes Gerüst) | Titel `heading()` und `paneTitle` (A11Y-07) |
| `PushHeader` / `PushScaffold` / `BackButton` | fester Kopf jeder Push-Seite (`BackButton` mit AutoMirrored-Pfeil, Titel oder `titleContent`, höchstens eine `action`) und ihr Gerüst mit Scroll-Spalte und optional `bottomBar` (§4.3) | Tabs → `ScreenHeader`; Seiten mit Liste → `TabListScaffold`; Muster-Editor | Titel `heading()`, `paneTitle` an der `HeaderBar`; Pfeil cd `action_back` |
| `SectionHeader` / `CollapsibleSection` / `GroupLabel` / `headingColor` (`SectionHeader.kt`) | Abschnittskopf `titleLarge` mit Teaser, Status, `expanded` (Caret, MUSTER-03), `onClick` (Chevron; mit `leading` ein dekoratives Zeichen vor dem Titel, nur „Muster des Tages ›“: `ic_breath_curve`) und genau einer `trailingAction`; `CollapsibleSection` setzt Kopf → Inhalt zentral; `GroupLabel` farbiges Label darunter; `headingColor` Kopf-Ring der `NavCard` (§4.5) | Kartentitel → `CardHeader`; Aktionen für eine Karte; Meditations-Tab (LAYOUT-08) | `heading()`; aufklappbar `Role.Button` + `stateDescription`; mit `onClick` eine Schaltfläche aus Titel, Status (`statusDescription`) und Teaser |
| `StickyHeader` / `Sticky` / `stickyItem` / `declareSticky` / `StickyHeaderOverlay` / `pinHeaders` (`StickyHeaders.kt`) | klebende Überschriften (Eigentümer 2026-10-31): `StickyHeader(Sticky(key, level)) { Kopf }` hüllt eine Überschrift; sie scrollt, bis sie oben ankommt, und bleibt dort als neu aufgebaute Kopie stehen (Breite und Lage des Originals; keine aufgezeichneten Pixel – die verwirft Android, sobald das Original aus dem Bild ist). In einer Liste zusätzlich anmelden: `stickyItem(sticky) { Kopf }` (eigener Eintrag) oder `declareSticky(sticky, Kopf)` vor dem Eintrag, in dem der Kopf steht – so klebt er auch nach Rückkehr oder Sprung. `CollapsibleSection(sticky = …)`; ein `CollapsibleSection` ohne `sticky` beendet den klebenden Abschnitt darüber. Ebene 2 hängt unter Ebene 1, die nächste Überschrift derselben oder höheren Ebene schiebt sie hinaus; Stapel über 30 % des sichtbaren Bereichs: nur die tiefste Ebene. Lage je Bild aus dem Scrollstand, ohne Dauer: keine Animation im Sinn von MOTION-02/-04. Im Einsatz: Atmen (Abschnitt + Gruppe), Logbuch, Begriffe, Atemlehre, Pranayama | Köpfe mit Aktion (✕, „+“, Caret: oben stünde ein Knopf ohne Wirkung), Kartentitel (Einstellungen), kurze Seiten | Kopie ohne Semantik (`clearAndSetSemantics`, Abweichung A11Y-02; die echten Überschriften bleiben in der Liste); Wischen scrollt, ein Tipp fällt nicht durch |
| `SubLabel` / `Labeled` (`SubLabel.kt`) | Unter-Überschrift in Karten, Abschnitten und Blättern; `Labeled` setzt sie über einen Block | Abschnittsköpfe → `SectionHeader` | – |
| `AppBottomSheet` | Gerüst jedes Blatts (MUSTER-11, §4.5 „Bottom Sheet“): `title`, `intro`, Inhalt in `AppBottomSheetScope` mit `closeThen` | Dialoge mit Bestätigen, lange Listen, Inhalte; Karten um Steuerungen | TalkBack beginnt beim Titel, Griff stumm, Schließen über Hintergrund und Zurück |
| `CardHeader` / `TitleWithChips` | Titelzeile jeder Karte mit Chips, Zeichen oder bis zu zwei Aktionen (§4.5 „Karte mit Aktionen“), auch Kopf von Expander und Einstellungskarte; Test-Tags für `ANATOMIE`; `TitleWithChips` lässt Chips bei Platzmangel unter den Titel rutschen | Abschnittsköpfe → `SectionHeader`; Nachbau mit `align(TopEnd)` | Titel über `TitleWithChips`; Aktionen mit Namen am Icon |
| `ExerciseCard` / `PatternCard` (`ExerciseCards.kt`) | Karte einer Übung (Rand Familienfarbe, Metazeile Takt · Dauer) bzw. eines generierten Musters (Rand `secondary`, bis zu zwei Aktionen) (§4.5) | Verweise → `NavCard`; mehr als zwei Aktionen | `Card(onClick)`; Aktionen mit Tippfläche 48 und Namen |
| `ExerciseRow` / `ExerciseRows` / `ExerciseCurve` (`ExerciseRow.kt`) | schlanke Zeile einer Übung: vorn `ExerciseCurve` (`MiniCurve`, ohne Takt ein Familienpunkt; ab Schrift 1,5 keins), Name `titleSmall`, Hinweis („gestern Abend · 10 min“, sonst „ca. 10 min“), ⚠-Chip, am Ende `trailing` (Chevron, ◉ oder Stift und Löschen); `ExerciseRows` stellt mehrere in eine Karte mit `DividerSubtle` („Zuletzt geübt“, Seiten „Eigene Kreationen“ und „Tut mir gut“) | Beschreibung und Phasen zeigen → `ExerciseCard` | ganze Zeile `Role.Button` ≥ 48 dp, ein Fokus-Stopp; `hintDescription` schreibt Kürzel aus |
| `CardPager` / `pagerPageCount` (`CardPager.kt`) | Karteikarten: eine Karte je Seite (86 % der Inhaltsbreite, die nächste ragt herein), einrastendes Wischen, bis an die Bildschirmkanten; alle Karten gleich hoch (stille Probe misst jede, mit `sameHeightAs: CardSet` auch die eines anderen Pagers, zwischen denen man umschaltet); darunter „2 / 7“ (TalkBack „Karte 2 von 7“) und rechts optional „Alle 4 ›“, dann ist die letzte Karte „Alle ansehen ›“ (Tab „Meins“) | viele gleichrangige Einträge zum Durchsehen → Liste; Auswahl → `SegmentedChoiceRow` | Seiten über die Scroll-Aktionen des Pagers erreichbar; `PagerState` hält der Aufrufer (übersteht Tab-Wechsel) |
| `NavCard` | Karte, die woandershin führt (Kapitel in „Lernen“, Über, Quellcode, Datenschutz, „Zuletzt: <Name>“ im Tab „Meins“; `external`) | Verweise im Fließtext → `ForwardLinkText` | Titel `heading()`, `Role.Button`; extern „Link öffnen“; `hintDescription` spricht einen Hinweis mit Kürzeln ausgeschrieben (A11Y-06) |
| `ExpanderSection` / `ExpanderCard` (`ExpanderSection.kt`) | aufklappbarer Abschnitt: Titel über `CardHeader`, Teaser immer ganz, Detail; als Karte mit optionalem `header` | Einstellungen → `DisclosureToggle`; Listen gleichartiger Abschnitte → `CollapsibleSection`; nie ein `SectionHeader` direkt darüber | `heading()`, `Role.Button`, `stateDescription`; ohne Detail `mergeDescendants` |
| `DisclosureToggle` | Aufklapper in Karten und Blättern („Phasen anpassen“), Text `onSurface`, über die volle Breite tippbar, `note` als Status („· angepasst“) | Inhalt mit Teaser → `ExpanderSection`; Aktion oder Verweis; nie `primary` | `stateDescription` inkl. „angepasst“ |
| `AppIconButton` (`Controls.kt`) | jede Icon-only-Aktion: Kreis `IconButtonSize`, Icon `IconDefault`, Tippfläche 48 | Aktion mit Text → `AppTextButton`; Zustand → `AppIconToggle` | cd am `Icon` (ICON-02) |
| `AppIconToggle` (`Controls.kt`) | Icon-only-Umschalter mit Zustand (◉ „In „Meins“ speichern“), gefüllt oder leer | Ton → `SoundSwitch`; Einstellung → `ToggleRow` | `Role.Checkbox`, `stateDescription`, `liveRegion` nur für Wechsel von außen; `announce = false`, wenn ein anderer Knopf ansagt (A11Y-04) |
| `AppTextButton` / `AppOutlinedButton` (`Controls.kt`) | Text-Aktion ohne Fläche (`shapes.large`) / gleichrangige Aktionen nebeneinander, Rand `outline`, optional `dashed` oder `compact` | Primäraktion → `StartSplitButton`; Auswahl → `SegmentedChoiceRow`; Session → `SessionSecondaryButton` | Text = Name; kurze Beschriftung mit vollem Namen über `contentDescription` |
| `ToggleRow` (`Controls.kt`) | Ein/Aus-Einstellung: Label, Switch, Hinweis endet vor dem Switch (§4.5) | Auswahl aus mehreren → `SegmentedChoiceRow` | ganze Zeile `toggleable(Role.Switch)`, Name = Label (A11Y-03) |
| `SoundSwitch` / `IconSwitch` (`SoundSwitch.kt`) | Icon-only-Schalter: Lautsprecher bzw. beliebiges Icon („Danach in die Stille“); aus in `TERTIARY` | Zustand mit Fläche oder Text → `AppIconToggle`, `ToggleRow` | Name am `Icon`, `Role.Switch`, `stateDescription` „An“/„Aus“ |
| `Stepper` / `CompactStepper` (`Stepper.kt`) | ganzzahliger Wert in festem Bereich mit Label (Kreis bündig, `widestValues` gegen Springen) bzw. ohne sichtbares Label; `valueText`/`spokenValue` für Werte ohne Zahl (Uhrzeit) | Freitext | Knöpfe „Label, Erhöhen“, am Anschlag Fokus auf den Gegenknopf; Wert als Live-Region mit Einheit |
| `SegmentedChoiceRow` / `choiceGroupHeading` | Einzelwahl aus 2–4 kurzen Optionen (mehr als Raster), **nie ein Häkchen**, gewählt `SegmentSelected` + SemiBold; zu eng → bis 85 %, dann Radioliste | mehr als 4 Optionen in einer Reihe | `groupLabel` als Gruppenname, `choiceGroupHeading()`; Radioliste `selectableGroup()`, `Role.RadioButton` |
| `Chip` / `EvidenceChip` / `CautionChip` / `InfoChip` / `CharacterChip` (`Chips.kt`) | nicht klickbare Info-Pillen in `TitleWithChips` oder der Familienzeile: Studienlage (nur Detailseite), „nur gesund & ausgeruht“, „zum Nachlesen“, Charakter; Reihenfolge Studienlage → Vorsicht → zum Nachlesen; einzeilig bis `CHIP_MIN_SCALE` | Auswahl → `SegmentedChoiceRow` | Text Pflicht; `EvidenceChip` spricht „Studienlage: …“ |
| `LinkChip` / `ExerciseChips` (`LinkChip.kt`) | antippbarer Verweis auf eine Übung, sichtbar `ChipHeight`, Tippfläche 48 | Info → `Chip`; Aktion ohne Ziel → `AppTextButton` | Text = Name, Rolle Button |
| `ChoiceChip` / `ChoiceChipRow` / `TonalChip` / `FilterChipRow` / `FilterChipFlow` (`ChoiceChip.kt`) | Anlass-Chips (40 dp, Rand, eine Antwort) und Filter-Chips (32 dp, tonal, mehrere) in Reihen (§4.5 „Chip-Reihen“); `FilterChipFlow` bricht zwischen Chips um | Einzelwahl aus 2–4 → `SegmentedChoiceRow`; Verweise → `LinkChip` | `Role.Checkbox` über `toggleable`; Reihe `selectableGroup` mit Namen, Fokusfolge = Datenfolge |
| `BreathMarkIcon` / `PersonalMarkIndicator` / `PersonalMarkToggle` (`PersonalMark.kt`) | das eine Zeichen „zu mir“, der Atemkreis ◉ (ICON-03): „Tut mir gut“ (Detailseite, `ExerciseCard`) und „In „Meins“ speichern“ | Herz, Stern, Lesezeichen; Sortieren; Tab-Symbol „Meins“ | cd und `stateDescription` je Bedeutung; gesetzt ≥ 4,5:1 |
| `SearchField` | einzeiliges Suchfeld als Pille mit einem `TextFieldState` des Aufrufers (§4.5, [spec/suche.md](spec/suche.md)) | gespeicherte Eingaben; mehrzeiliger Text | X cd „Eingabe löschen“; Platzhalter als Name |
| `AdaptiveButtonRow` / `InlineOrBelow` | Button-Reihe, die vom gleich breiten Nebeneinander bis zum Untereinander ausweicht / zwei Teile nebeneinander, sonst `end` rechtsbündig darunter, entschieden über Reserven | Split-Button → `StartSplitButton`; Button-Reihen → `AdaptiveButtonRow` | Lesereihenfolge bleibt; Reserven ohne Semantik |
| `StartBar` / `StartSplitButton` / `SlidingReset` | `StartBar`: die Startleiste einer Übung (Detailseite, Muster-Editor) – Zeile „… · Wert ›“ zum Blatt und darunter der Start, knapp in der Höhe beides in einer Zeile („10 min ›“ neben „Starten“); `StartSplitButton`: **jeder** Start einer konfigurierbaren Session (MUSTER-01, §4.5 „Knopfleiste unten“); Reset, der nur bei Abweichung hereingleitet | andere Aktionspaare; Vorschau → `CurvePreviewControls` | Reset Icon-only, cd `adjust_reset`, Fokus → Start, wenn er verschwindet (`onFocusHandoff`) |
| `PinnedActionBar` | gepinnte Start-Leiste unter dem Scroll-Inhalt (§4.5); meldet ihre Oberkante dem `TabBarState` (Griff, Wischen) | Aktionen im Inhalt; mehr als eine Zeile über dem Start | Inhalt `StartSplitButton`, darüber höchstens ein `SectionHeader` mit `onClick` |
| `ConfirmIconButton` / `rememberArmedConfirm` (`ConfirmAction.kt`) | zerstörende Aktion zweistufig im selben Element (MUSTER-05) | Abbruch eines Downloads; sofort umkehrbare Aktionen | Name mit Gegenstand, Bestätigung „‚X‘ wirklich löschen?“, `liveRegion` am Knopf; in Listen `key(id)` |
| `SafetyDialog` / `rememberSafetyGate` | Sicherheitsbestätigung vor intensiven Übungen und das Gate je Übung (`SafetySettings.acknowledgedIds`, MUSTER-06) | Infos ohne Risiko | nicht außen schließbar; Warn-Icon dekorativ |
| `WholeWordText` / `badBreakAt` / `breaksInsideWord` (`WholeWordText.kt`) / `MarqueeText` | Titel, Namen, Beschriftungen, die nie im Wort brechen: verkleinert schrittweise; die Umbruch-Regel teilt sich der `ScreenshotTest`; `MarqueeText` als Laufschrift für einen einzeiligen Titel (Seitentitel, Name im Editor) | Fließtext → umformulieren; Laufschrift nicht in Listen, Karten, Abschnittsköpfen | wie `Text`; Laufschrift liest TalkBack ganz |
| `BodyText` / `HintText` / `SecondaryText` / `WarnText` / `CardTitle` / `Bullet` (`TextBlocks.kt`) | Textrollen außerhalb der Köpfe nach §3 und §2.2: Fließtext, Hinweis, Sekundärtext, Warnzeile „⚠ …“ in `WarnAmber`, Kartentitel ohne Aktionen, Aufzählungspunkt | Köpfe → `SectionHeader`; Einleitung → `ScreenSubtitle` | `CardTitle` ist `heading()`; Bullet-Glyph stumm |
| `ForwardChevron` / `ForwardLinkText` (`ExpandCaret.kt`) | Chevron „führt weiter“ hinter einem Verweis; Verweistext mit Chevron in einem `AppTextButton` | externe Links (`ic_open_in_new`) | dekorativ, den Namen trägt die Zeile |
| `BreathingCircle` / `breathingDiscDiameter` | Atem-Pacer (`fraction` 0…1, Verhältnis klein : groß 0,42) und sein Durchmesser für Nachbarn | dekorative Kreise | Aufrufer setzt Name, `onClickLabel`, Zustand, Fokus (A11Y-07) |
| `OverlayChrome` / `SessionRunningLayout` / `CalmHost` | Rahmen jedes Vollbild-Overlays (Minimieren, Stille, Ton) / Gerüst laufender Sessions mit fest reserviertem `centerExtra` / Ruhe-Ansicht beider Overlays ([spec/session.md](spec/session.md)) | Push-Screens; eigene Tipp-Logik je Overlay | `title` als `paneTitle`; nie ausblenden, solange eine Bedienungshilfe läuft |
| `SessionControls.kt`: `GlowText`, `SessionControlBar`, `SessionPrimaryButton`, `SessionSecondaryButton`, `FinishedPanel`, `rememberTapFlash`; `PauseFlash`; `CircleTapTarget` | Text, Buttons und Tap-Rückmeldung **nur** in Vollbild-Sessions; Ende-Screen mit Antwortreihe ([spec/session.md](spec/session.md#antwortreihe)) | normale Screens, stehender Status | `FinishedPanel`: `paneTitle` „Geschafft“, Fokus auf „Nochmal“; `PauseFlash` dekorativ; `CircleTapTarget` mit Namen und `onClickLabel` |
| `MiniNowPlayingBar` | minimierte laufende Session über der Tab-Leiste | andere Meldungen | ≥ 48 dp, ein Fokus-Stopp, `Role.Button` |
| `RhythmCurve` / `SessionAdjustControls` | Bausteine der Detailseite: ein Atemzug mit Punkt im echten Takt (in Karteikarten ruhend, `lineReserve` hält Zeilen frei); Dauer und Intervalle auf dem Blatt „Anpassen“, Zustand `SessionAdjustState` in `ui/detail/` ([spec/detail.md](spec/detail.md)) | Bearbeiten → `BreathCurveEditor`; Übungen ohne Takt; Stufen-Übungen ohne „Phasen anpassen“ | Kurve ein Knoten mit den Phasen in Worten, ohne Live-Region; „· angepasst“ im Toggle-Zustand |
| `MiniCurve` / `drawLevelSegments` / `breathSideColor` (`MiniCurve.kt`) | kleine statische Kurve eines Atemzugs ohne Zahlen und Punkt (Startpunkt-Sheet; über `ExerciseCurve` in Zeilen und Kacheln des Tabs „Meins“); `drawLevelSegments` zeichnet die Linie für `MiniCurve` und `RhythmCurve`; `breathSideColor` ist die eine Farbregel der Atemkurven (FARBE-04), auch für `phaseColor` des Editors | große Kurve mit Griffen → `BreathCurveEditor`; laufender Punkt → `RhythmCurve` | dekorativ, ohne Semantik; Name bzw. Teaser daneben trägt die Bedeutung |
| `AppNavigationBar` / `AppTabBar` / `HideableTabBar` / `TabBarToggle` (`ui/navigation/`) | Tab-Leiste, in der Tiefe einklappbar mit Griff und Wischen (§4.5, [spec/navigation.md](spec/navigation.md)) | Push-Ziele | `labelLarge` in jedem Zustand gleich, bis 85 %, dann Icons mit Namen; Auswahl tonal |

**Helfer ohne eigene Optik** (Bauweise in der KDoc): Messen und Reserven `widestOf`, `rememberWidestTextWidth`, `rememberWidestAnnotatedWidth` (Breiten prozessweit in `TextWidths` je Text, Stil und Schrift-Umgebung; vorab messen nur darüber, Reserve-Texte ohne Farben), `rememberFirstLineHeight` (§4.5) · Kanten und Ränder `flushEnd`/`flushStart` (§4.5 „Bündigkeit“), `accentBorder`, `dashedBorder` (`CardBorder.kt`), `selectedRow` (Auswahl ohne Häkchen) · Sichtbarkeit `HideableSlot`, `inertWhen`, `FillViewportColumn` (LAYOUT-08), `HideSystemBars`, `rememberAccessibilityActive` (`ConfirmAction.kt`) · Text `DotSeparatedFlow`, `sanskritText` (TEXT-14), `ReferenceItem`, `PhaseMark`/`HoldDash`, `ExpandCaret`/`expandedStateText` · Daten in Worten `color`/`title` (`BreathingFamily`), `label` (`EvidenceLevel`) in `FamilyPresentation.kt`, `sessionSummaryParts` (`SummaryPart`), `spokenDuration`, `durationClockText`, `feelingText`/`FeelingAsk` (SEC-LEGAL-04).

**Feature-Bausteine außerhalb von `ui/components/`:** `BreathCurveEditor`/`BreathCurveHeights`/`CurvePreviewControls` → [spec/muster-editor.md](spec/muster-editor.md) · `TimeDial`/`DurationPickerState` → [spec/meditation.md](spec/meditation.md) · `VayuFigure`/`VayusContent` → [spec/lernen.md](spec/lernen.md) · Wrapper ohne eigene Regeln `MiniSessionBar`, `SessionOverlay`, `MiniMeditationBar`, `MeditationOverlay`.

### 5.2 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| KOMP-01 | MUSS | Gibt es eine Komponente in §5.1, wird sie genutzt; kein Inline-Nachbau. Wird ein Muster zum zweiten Mal gebraucht, wandert es nach `src/ui/components/`; jede öffentliche `fun` dort steht im Katalog. Geteilt heißt gleiches Verhalten: Parameter steuern Daten und Inhalt, nicht ob eine Funktion an einem Ort fehlt. | Konsistenz; Nachbauten laufen auseinander ([DECISIONS 2026-10-03](DECISIONS.md)), Schalter je Ort ebenso (Wochenkalender, [2026-10-31](DECISIONS.md)). | auto: `catalog-sync`; manuell: R-CODE |
| KOMP-02 | MUSS | Klickbare Karten sind `Card(onClick)`, nie `Card(Modifier.clickable)`. Aktionen einer Karte stehen nur auf ihrer Titelzeile (`CardHeader`, §4.5): höchstens zwei, zerstörende außen, bestimmen nie die Kartenhöhe; ein `AppIconToggle` zeigt seinen Zustand gefüllt in `secondary`. | Ripple und Rolle stimmen; keine verschachtelten Ziele; eine Bausprache statt dreier Ausnahmen. | auto: `card-style`, `anatomy-overlay`, `ScreenshotTest` (`ANATOMIE`) |
| KOMP-03 | MUSS | Karte: Default-Farbe, `shapes.medium`, `CardPadding`, Titel `titleMedium`, Text `bodySmall` nach §4.5 (Metazeile `BODY`, Kurztext `SECONDARY`); Rand (`accentBorder`) nur bei navigierenden Inhaltskarten. Trenner in Karten und Expandern nur `HorizontalDivider(color = DividerSubtle)`. Ausnahmen mit Marker `// Abweichung KOMP-03:`: Marken-Trenner über der Tab-Leiste, Bereichs-Trenner unter „Muster des Tages ›“. | Eine Trennlinie für alle Karten ([DECISIONS 2026-10-03](DECISIONS.md)). | auto: `card-style`; manuell: R-VISUAL |

## <a id="muster"></a>6 Muster

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| MUSTER-01 | MUSS | **Split-Button und Rückmeldung im Element:** Start (`primary`) in voller Restbreite, Reset (tonal, Icon-only) gleitet nur ein, wenn ein Wert vom Default abweicht (`StartSplitButton`); kein Bestätigungsdialog. Bestätigung ist ein Zustandswechsel des auslösenden Elements (◉ leer → gefüllt, `stateDescription`, `liveRegion = Polite`); Abweichung als Suffix „· angepasst“, nie als Badge oder loser Text. | Eine Fläche statt zwei; Reset nur, wenn er etwas bewirkt; TalkBack erfährt den Erfolg. | manuell: R-VISUAL, R-A11Y; auto nur „kein Toast, keine Snackbar“: `no-fab-toast` |
| MUSTER-03 | MUSS | **Ausklappen und Übersicht.** `ExpanderSection`: Teaser immer ganz, Detail darunter, Höhe animiert. Lange Listen gleichartiger Abschnitte werden zur Übersicht aus `SectionHeader` mit `expanded` und Teaser, Standard zu, Zustand per `rememberSaveable` (Beispiel Tab „Meins“: [spec/eigene-tab.md](spec/eigene-tab.md)). Ein Suchfeld nur als Zusatz und nur nach MUSTER-09. | Kontext bleibt sichtbar; Sprünge irritieren. | manuell: R-VISUAL; auto nur der Teaser-Text: `ContentRulesTest teaser` |
| MUSTER-04 | SOLL | **Leer und Fehler.** Leer: „Noch keine …“ plus sanfte Einladung, ohne Illustration oder Extra-Button (Ausnahmen: Suche ohne Treffer „Nichts gefunden“ mit „Alle zeigen“; Leer-Karte „Eigene Kreationen“ mit dem Textverweis „Muster gestalten ›“). Fehler inline im Element: was passiert ist und was du tun kannst; kein `e.message` in der UI. | Nutzer wissen, was zu tun ist; Exception-Texte sind technisch. | auto: `exception-message-ui`; manuell: R-TEXT |
| MUSTER-05 | MUSS | **Zerstörende Aktion** (Logbuch, gespeicherte Muster, Stimmen): zweistufig im selben Element über `ConfirmIconButton` bzw. `rememberArmedConfirm` – erster Tipp morpht in den Bestätigungszustand (`error`, cd „‚X‘ wirklich löschen?“, `liveRegion`), zweiter löscht; Rückfall nach 4 s, nicht bei Fokus oder aktiver Bedienungshilfe. Kein Dialog, keine Snackbar. | Schutz vor Fehltipp ohne Dialog (SEC-PRIV-04). | manuell: R-A11Y |
| MUSTER-06 | MUSS | **Sicherheitsdialog** nur für Übungen mit `caution` (Wim Hof, Feueratmung) und intensive eigene Muster: M3 `AlertDialog`, Warn-Icon, Hinweisliste scrollbar, „Abbrechen“ links, „Verstanden, starten“ rechts, nicht außen schließbar; **je Übung** bis zu ihrer ersten Bestätigung (`SafetySettings.acknowledgedIds`), mit „Sicherheitshinweis immer zeigen“ (Standard an) jedes Mal. | Bewusste Bestätigung; eine Bestätigung bei der Feueratmung sagte nichts über Wim Hof. | auto: `PatternEditorSafetyTest` (Editor, Detailseite), `ContentRulesTest` (`intense-caution`), `DailyPatternGoldenTest` (nie intensiv); manuell: R-A11Y |
| MUSTER-07 | MUSS | **Session:** Steuerung nur `SessionControlBar` (Icon-only, feste Kreise), alles per Kreis-Tipp auch als Button; eine laufende Sitzung bleibt app-weit als `MiniNowPlayingBar` sichtbar (`PlaybackService`); Overlay mit `paneTitle`, App darunter inert; keine Ruhe-Ansicht bei aktiver Bedienungshilfe; Bilanz aus echten Werten. Ende, Logbuch, „Danach in die Stille“: [spec/session.md](spec/session.md). | Bedienbar ohne Kreis-Geste; eine laufende Session ist nie unsichtbar. | auto: `ScreenshotTest` (`09_*`, `10_*`), `PlaybackFlowTest`; manuell: R-A11Y, R-AUDIO |
| MUSTER-08 | MUSS | **Tabs und Zurück:** Feste Köpfe (`PushHeader`, `ScreenHeader`); Tipp auf den markierten Tab führt zu dessen Startseite, ein anderer Tab stellt seinen Stapel wieder her, markiert ist immer der Tab des Stapels; die Tab-Leiste steht auf den vier Tabs immer und ist in der Tiefe eingeklappt (Griff und Wischen, keine Scroll-Messung). Details: [spec/navigation.md](spec/navigation.md). | Aus den Einstellungen kam man nur über den gescrollten Zurück-Pfeil heraus. | auto: `TabNavigationTest`; manuell: R-VISUAL |
| MUSTER-09 | MUSS | **Suche** nur, wo sie über eine Übersicht hinaus hilft (Block „Wie fühlst du dich?“ im Tab „Atmen“): gewählt ist ein Anlass, eine Suche, ein Bereich oder nichts; Chips verrutschen nie; Trefferstatus als Live-Region; ohne Treffer „Nichts gefunden“, nie eine geratene Empfehlung; Suchlogik rein in `src/domain/`, kuratierte Laien-Begriffe; Eingabe: SEC-PRIV-06. Aufbau: [spec/suche.md](spec/suche.md). | Die Suche soll halten, was sie verspricht; „müde“ darf nie bei Wim Hof landen. | auto: `ContentRulesTest health-claims`, `ScreenshotTest` (`01_atmen_suche_*`, `01_atmen_anlass_offen`, `01_atmen_bereich`); manuell: R-TEXT, R-A11Y |
| MUSTER-10 | MUSS | **Muster-Editor:** Ein eigenes Muster ist eine freie Phasenfolge (1–30 s je Phase, 2–12 Phasen, mindestens ein Ein und ein Aus); Ungültiges lässt sich nicht einstellen, immer ist eine Phase gewählt, „intensiv“ warnt (MUSTER-06); alles Bedienbare steht bis fontScale 1,3 ohne Scrollen da. Speichern über ◉, „Starten“ probiert ohne Speichern. Aufbau: [spec/muster-editor.md](spec/muster-editor.md). | Frei gestalten statt Wunsch-Generator; Werkzeugleiste und Ablauf halten alles ohne Ziehen bedienbar. | auto: `FreePatternTest`, `PatternEditorSafetyTest`, `DeviceFitTest` (`KOMPAKT_FEHLT`), `ScreenshotTest` (`14_*`); manuell: R-A11Y |
| MUSTER-11 | MUSS | **Bottom Sheet** nur für kurze Nebenoptionen einer Seite, deren Hauptelement ruhig stehen bleiben soll (die Blätter in §4.5 „Bottom Sheet“); sofort wirksam, kein Bestätigen; Auslöser ist ein Element der Seite, das die aktuelle Wahl zeigt. Nicht für Inhalte, lange Listen oder Eingaben mit Bestätigen. Gerüst nur über `AppBottomSheet`, Inhalt nach derselben Anatomie. | Ein Blatt legt sich darüber, ohne das Layout zu ändern; ein Gerüst statt vier Kopien. | auto: `sheet-direct`, `ScreenshotTest` (`*_offen`); manuell: R-A11Y, R-VISUAL |
| MUSTER-12 | MUSS | **Filterreihe** über der Übungsliste nur im Grundzustand: eine Zeile `TonalChip`s, nur Filter quer zu den Familien („Bis 3 min“, „Ohne Halten“, „Aus dem Yoga“; ein Filter teilt die Übungen sinnvoll, höchstens fünf, nie Studienlage), UND-verknüpft, Familien ohne Treffer fallen weg, das Tagesmuster darüber bleibt, Trefferstatus als Live-Region. Details: [spec/atmen-tab.md](spec/atmen-tab.md). | Wer wenig Zeit hat oder nicht die Luft anhalten mag, sucht quer zu den Familien. | auto: `ExerciseFilterTest`, `ScreenshotTest` (`01_atmen_filter`); manuell: R-A11Y |
| MUSTER-13 | MUSS | **Erster Start: eine Karte, kein Onboarding.** Die Willkommenskarte („Neu hier?“, Einstieg, Verweis auf die Resonanz-Atmung, Gesundheitshinweis) steht oben im Atmen-Tab, bis sie weggeklickt ist (DataStore); kein Dialog, keine zweite Seite, kein Fokus beim Öffnen. Details: [spec/atmen-tab.md](spec/atmen-tab.md). | Einsteiger fanden weder Einstieg noch Gesundheitshinweis; ein Wizard widerspräche dem ruhigen Charakter. | manuell: R-TEXT, R-A11Y; auto nur die Darstellung: `ScreenshotTest` (`01_atmen_willkommen`) |

## <a id="icons"></a>7 Icons

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| ICON-01 | MUSS | Icons aus `material-icons-core`, Stil `Filled` (`AutoMirrored` für Richtungen), oder eigener Vektor `res/drawable/ic_<name>.xml` (24-dp-Viewport, einfarbig). Kein `material-icons-extended`, kein `Outlined`/`Rounded`/`Sharp`/`TwoTone`. Größen nur `IconDefault`/`IconInButton`/`IconSmall`, in der `SessionControlBar` `Dimens.Session*Icon`. Bediensymbole („−“, „+“, „›“) sind Icons, keine Text-Glyphen. | APK-Größe, eine Linie, RTL, TalkBack liest Glyphen vor. | auto: `icon-source`, `layout-literal` |
| ICON-02 | MUSS | Icon-only-Elemente setzen `contentDescription` **am `Icon`** über `R.string.cd_*`/`action_*`, benannt nach der Aktion ohne Typwort. `contentDescription = null` nur bei dekorativem Icon oder Text im selben Knoten, mit `// dekorativ: <Grund>`. | Ein Muster statt zwei; prüfbar. | auto: `cd-style` |
| ICON-03 | MUSS | **Ein Zeichen, eine Bedeutung.** Der Atemkreis ◉ (`BreathMarkIcon`) ist das einzige Zeichen für „zu mir“: „Tut mir gut“ und „In „Meins“ speichern“. Tab-Symbole bezeichnen nur ihren Ort (Inventar unten). Kein Lesezeichen, Herz oder Stern. | Drei Speicher-Zeichen für zwei Bedeutungen ließen raten, was wohin gehört ([DECISIONS 2026-10-24](DECISIONS.md)). | manuell: R-VISUAL |

**Inventar der Zeichen (ICON-03):**

| Zeichen | Quelle | steht | bedeutet |
|---|---|---|---|
| Atemkreis ◉ | `BreathMarkIcon` (`ic_breath_mark` / `_filled`) | Detailseite (Kopf), `ExerciseCard`, Tagesmuster, Muster-Editor | „Tut mir gut“, „In „Meins“ speichern“ |
| Tab-Symbole | Atmen `ic_breathe` (Material „Air“), Meins `Icons.Filled.Person` (nur hier), Meditation `ic_meditation` (Material „Timer“), Lernen `ic_learn` (Material „Menu Book“) | Tab-Leiste; `ic_meditation` auch am Schalter „Danach in die Stille“ | Ort; Stille danach |
| Atemkurve | `ic_breath_curve` (eigener Vektor) | nur vor „Muster des Tages ›“ | „Muster: ansehen und selbst gestalten“ |

## <a id="bewegung"></a>8 Bewegung

**Tokens:** Dauern und Animations-Specs nur aus `Motion.*` ([Motion.kt](../app/src/main/java/app/atemkraft/ui/theme/Motion.kt), Wert und Zweck je Token in der KDoc); Übergänge der Navigation: [spec/navigation.md](spec/navigation.md).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| MOTION-01 | MUSS | Kreis, Countdown und Ton leiten sich aus der Session-Uhr ab, nie aus einer Animations-API. Nach Pause setzt auch der durchgehende Ton an der aktuellen Position fort. | Synchron, Pause friert alles ein, „Animationen entfernen“ bricht nichts. | manuell: R-CODE, R-AUDIO |
| MOTION-02 | DARF NICHT | Nichts blinkt schneller als 3 Hz; keine rotgesättigte Fläche mit Helligkeitswechsel; Pacer-Phasen < 0,5 s. Dekorative Endlos-Animationen über Compose-APIs (stehen bei Animator-Skala 0), Alpha-Änderung ≤ 0,1; Tap-Flash bei 0 statisch. Ausnahmen, bei Skala 0 still: Pulsieren der gewählten vāyu-Fläche (`VayuPulse`, nur nach Antippen), Rhythmus-Punkt der Detailseite (`RhythmCurve`, ohne Pulsieren). | WCAG 2.3.1 / 2.2.2, ruhig. | manuell: R-VISUAL |
| MOTION-03 | SOLL | Phasenübergänge am Kreis sind stetig: Endskala einer Phase = Startskala der nächsten (`CircleFractions` setzt `RuntimePhase.circle`; die Vorbereitung zeigt den Start der ersten Phase); Stufen (Viloma) über `Phase.level`, der Kreis wächst stufenweise, der durchgehende Ton folgt demselben Füllstand. | Sprünge wirken wie ein Blitz. | manuell: R-VISUAL |
| MOTION-04 | SOLL | Dauern und Easing aus `Motion.*`; neue UI-Bewegung ≤ 300 ms, Standard-Easing oder Default-Spring, kein Bounce. | Zentral, ruhig. | auto: `motion-literal` |

## <a id="audio"></a>9 Audio und Haptik

**Ist-Werte** (Frequenzen, Hüllkurven, Lautstärken, Sprache, Haptik, Defaults): [spec/session.md, Klang und Haptik](spec/session.md#klang); Code in `src/cue/`.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| AUDIO-01 | MUSS | Töne werden offline synthetisiert; neue Audio-Assets nur mit freier Lizenz (in den Credits). Kein Text in App, Store, README oder Release-Notes nennt eine Frequenz oder verspricht etwas mit ihr (keine „Heil“-Frequenzen); die Wechseltöne ([Ist-Werte](spec/session.md#klang)) sind nur Klang. Jeder Ton hat Attack ≥ 8 ms, Release ≥ 60 ms, Spitze ≤ 0,95 FS. | Offline, F-Droid, evidenz-ehrlich, keine Klicks. | manuell: R-CODE, R-AUDIO |
| AUDIO-02 | MUSS | Ausgabe über `USAGE_MEDIA`. Audio-Fokus nur, wenn etwas hörbar ist, in der Pause abgegeben, Fokusverlust per Listener behandelt. Der Gong klingt vollständig aus; Fokus/Service enden erst nach `gongTotalMs()` plus Reserve. | Fremde Musik bleibt sonst geduckt; abgeschnittener Gong. | manuell: R-AUDIO |
| AUDIO-03 | MUSS | Sprache schweigt bei Medienlautstärke 0, bricht vor dem End-Gong ab, überlappt nicht (`QUEUE_ADD`). | Ruhe. | manuell: R-AUDIO |
| AUDIO-04 | MUSS | Audio und Haptik hängen an den app-weiten Abläufen (`MeditationController`, `BreathSessionController`) bzw. am Service, nie an der Composition; Player kommen aus dem `AppContainer`, nie aus `remember { … }`. Beide Sitzungsarten laufen mit dem Foreground-Service `PlaybackService` (`mediaPlayback`, Partial-Wakelock). | Sperrbildschirm; Android 17 stummt Hintergrund-Audio ohne FGS. | auto: `player-in-composable`; manuell: R-AUDIO |
| AUDIO-05 | MUSS | Jedes Session-Ereignis ist auch hörbar oder fühlbar. Session-Vibration folgt dem App-Schalter „Vibration“ als `USAGE_ALARM`. Ausnahme: Rasten von Zeitring und Atemkurve sind Bedien-Rückmeldung (`USAGE_TOUCH`, Android regelt an/aus). Phasen unterscheiden sich in der Dauer. UI-Haptik nur ohne sichtbare Rückmeldung (Kreis-Tipp) und an Rasten; Knöpfe mit Ripple vibrieren nicht. | Bedienbar mit geschlossenen Augen; Touch-Ticks verwirft Samsung ([DECISIONS 2026-10-08](DECISIONS.md)). | manuell: R-AUDIO |

## <a id="a11y"></a>10 Barrierefreiheit

Ziel: WCAG 2.2 AA (nach WCAG2ICT) plus Android-Richtlinien.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| A11Y-01 | MUSS | Jedes Bedienelement hat Name, Rolle und Zustand: klickbare Nicht-M3-Elemente mit `role` und, wenn nötig, `onClickLabel`; Icon-Umschalter mit `Role.Switch` und `stateDescription`. Screen-Titel, `SectionHeader`, Kartentitel tragen `heading()`, zentral in der Komponente. | WCAG 4.1.2, 1.3.1. | auto: `semantics-required`, `semantics-heading`; manuell: R-A11Y |
| A11Y-02 | MUSS | Semantik nicht zerstören: kein `clearAndSetSemantics` auf Klickbarem, außer `onClick(label)`, `role`, `disabled()` werden neu gesetzt (Marker `// Abweichung A11Y-02:`); eine Container-cd verdeckt keine dynamischen Kind-Texte. | Sonst fehlen Klick, Rolle oder Restzeit. | auto: `a11y-ratchet`, `ScreenshotTest` (`OHNE_KLICK`); manuell: R-A11Y |
| A11Y-03 | MUSS | **Auswahl ohne Häkchen**, Zustand für TalkBack: Schalterzeilen als ganze Zeile `toggleable(role = Role.Switch)` mit `Switch(onCheckedChange = null)`, Zeilentext = Name; Einzelwahl über `SegmentedChoiceRow` (`Role.RadioButton`, `selectableGroup()`, verknüpftes Gruppenlabel); Chips über `ChoiceChip`/`TonalChip`, nie fette Schrift, gleiches Maß in jedem Zustand. Aussehen: Katalog §5.1. | WCAG 1.4.1, 4.1.2; fette Schrift änderte die Breite ([DECISIONS 2026-10-20, -23](DECISIONS.md)). | auto: `switch-unlabeled`, `no-check-icon`; manuell: R-A11Y |
| A11Y-04 | MUSS | Zustandswechsel ohne Fokuswechsel (Phase, Pause, Ende, Stepper-Wert, ◉ Speichern, Download, Trefferzahl) haben genau eine `liveRegion = Polite` an einem nicht zusammengeführten Knoten; eine Ansage je Tipp. Sekündlich tickende Werte nicht. | WCAG 4.1.3. | manuell: R-A11Y |
| A11Y-05 | MUSS | Tippflächen ≥ 48 × 48 dp **als Platz im Layout**, unabhängig von der Optik (M3 oder `minimumInteractiveComponentSize()`); die Hit-Test-Erweiterung von Compose zählt nicht. `LocalMinimumInteractiveComponentSize` nie ändern. Ausnahme: Link mitten im Fließtext (WCAG 2.5.8 „Inline“). | Reserviert das Layout weniger, ragen Nachbarn hinein. | auto: `ScreenshotTest` (`TIPPFLAECHE`), `platform-override` |
| A11Y-06 | SOLL | Dekorative Glyphen (· • › ⚠ ✓) sind keine Fokus-Stopps; Listeneinträge sind ein Stopp. Namen tragen Kontext („Stimme Thorsten anhören“); Zeiten und Einheiten ausgeschrieben („4 Sekunden“). | Weniger Rauschen, WCAG 2.4.6. | manuell: R-A11Y |
| A11Y-07 | MUSS | Overlays und Screens setzen `paneTitle` (über `HeaderBar` bzw. `OverlayChrome`); ausblendbare Bedienelemente bleiben sichtbar, solange ein Bedienungshilfe-Dienst läuft; verschwindet ein fokussiertes Element, setzt `FocusRequester` den Fokus. Elemente mit `indication = null` zeigen eigenen Fokus ≥ 3:1 (Marker `// Abweichung A11Y-07:`). Der Atemkreis hat keine Ripple. | WCAG 2.4.3, 2.4.7; D-Pad, Switch Access. | auto: `a11y-ratchet`, `pane-title`; manuell: R-A11Y |

## <a id="text"></a>11 Text und Ton

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| TEXT-01 | MUSS | UI-Chrome inkl. Hilfstechnik-Texten steht in `res/values/strings.xml`; Fachinhalte (Übungen, Situationen samt Suchbegriffen, Refs, Cues, Glossar) als Kotlin-Daten in `src/data/`. Mengen über `<plurals>`; keine fest eingetragenen Zählungen oder Inhaltsnamen in UI-Strings (Ausnahme mit Marker: Namen statischer App-Shortcuts). | Prüfbar; bricht nicht bei Inhaltsänderung. | auto: `ui-literal`, `lintDebug PluralsCandidate`; manuell: R-TEXT |
| TEXT-02 | MUSS | Ein Begriff pro Sache nach der Begriffsliste (§11.1), in UI und Store. Phasen-Labels eindeutig („Halten (voll)“ ≠ „Halten (leer)“). | Anzeige, TalkBack und Ansage unterscheiden sonst nicht. | manuell: R-TEXT |
| TEXT-03 | MUSS | Einsprachig Deutsch: nur `res/values/`; Datum und Zahlen über `java.time` + `DateTimeFormatter` mit `AppLocale` (`de`, `ui/components/`); kein `SimpleDateFormat`, kein `Locale.getDefault()`/`Locale.GERMAN` in `ui/`. Build mit `localeFilters += "de"`, Manifest mit `locales_config.xml`. | Sonst Mischsprache und gemischte Formate. | auto: `locale`, `locale-config` |
| TEXT-04 | MUSS | Anrede „du“, klein. Übungen laden ein statt anzuweisen („Wenn du magst …“). Sicherheit klar und knapp („Bei Schwindel sofort aufhören.“). | Nutzerpräferenz; Sicherheit braucht Klarheit. | manuell: R-TEXT |
| TEXT-05 | MUSS | Laiensprache in Situationen, Suchbegriffen, Labels und Tags: kein „Schub“, „Crash“, „vagal“, „Sympathikus“, „Hormese“, „CO₂-Toleranz“. Fachbegriffe nur im Detail; jeder steht im Glossar. | Verständlich, kein Krankheitsjargon. | manuell: R-TEXT |
| TEXT-06 | MUSS | **Teaser** (`effect`, erste `caution`): ein geschlossener Satz mit finitem Verb, endet auf „.“, ≤ 95 Zeichen. **Detail** (`effectDetail`) beginnt nicht mit dem Teaser, vertieft denselben Punkt, SOLL ≤ 700 Zeichen. Keine Baseline. | Nichts darf abgeschnitten wirken. | auto: `ContentRulesTest teaser`; manuell: R-TEXT (finites Verb, Detaillänge) |
| TEXT-07 | MUSS | Evidenz ehrlich: Schwache Evidenz steht schon im Teaser. **Jede eingebaute Übung trägt genau eine Studienlage** (`EvidenceLevel`, Stufen in §11.1), sichtbar nur auf der Detailseite; hochstufen nur mit neuer Quelle in `Refs.kt`; Teaser, Detail und Stufe widersprechen sich nicht. Generierte Muster haben keine Studienlage. Kein Filter und kein Karten-Chip nach Studienlage. | Kernversprechen der App; ein Etikett nur für die beste Übung ließ die übrigen offen. | manuell: R-TEXT |
| TEXT-08 | MUSS | Jeder sicherheitsrelevante Satz aus dem Detail steht auch in `cautions`, als Bedingung → Handlung („Nur im Sitzen oder Liegen – nie im Wasser.“). Keine Therapie-Anweisungen. | Sichtbar ohne Aufklappen. | manuell: R-TEXT |
| TEXT-10 | MUSS | Zeichen: nur „…“ und ‚…‘, Apostroph ’, nie ASCII-`"` oder `”`; Gedankenstrich –, Minus −, Auslassung …; Dezimalkomma; Einheit mit geschütztem Leerzeichen (`4 s`, `93 %`, `n = 40`) – auch in README, Store, Release-Notes und PRIVACY; „ca.“ statt „~“; Trennpunkt „ · “ nur über `DOT_SEPARATOR`/`splitAtDots()`: keine Zeile beginnt oder endet mit „·“. Ausnahme mit Marker: Zeichenlisten im Code. | Deutsche Typografie; Zeichen gehen im Build sonst verloren. | auto: `typo-chars-strings`, `-src`, `-content`, `ContentRulesTest typography` (`quotes`, `decimal-comma`, `dot-separator`, `number-unit`), `ScreenshotTest` (`badBreakAt`) |
| TEXT-11 | MUSS | Store-Texte (`fastlane/metadata/android/de-DE/`): `title` ≤ 30, `short_description` ≤ 80, `full_description` ≤ 4000, `changelogs/<versionCode>.txt` ≤ 500 Zeichen; sie folgen TEXT-04, -07, §11.1 und SEC-LEGAL-01/-02 (Disclaimer wörtlich). Changelog: Nutzen in Laiensprache, eine Zeile pro Änderung, keine IDs, keine Superlative. | Store-Grenzen und -Policy. | auto: `ContentRulesTest store-lengths`, `health-claims`; manuell: R-STORE |
| TEXT-12 | MUSS | Jede genannte Studie steht in [Refs.kt](../app/src/main/java/app/atemkraft/data/Refs.kt): `Nachname I. et al. (Jahr): Originaltitel. Journal Band(Heft):Seiten. <deutsche Einordnung>.`, Kennung `doi:10.… · PMID n`. Im Text „(Name Jahr)“. Jede Referenz wird genutzt. | Nachprüfbarkeit. | manuell: R-TEXT |
| TEXT-13 | MUSS | Yoga-Lehre steht getrennt von der Studienlage: Abschnitt „Aus der Yoga-Lehre“ auf der Detailseite (Übungen yogischen Ursprungs, generierte Muster) und die Seiten „Atemlehre“ und „Pranayama“; immer zuschreibend („gilt in der Yoga-Lehre als …“), nie als Wirkung; Verse deutsch zitiert; Teaser nach TEXT-06, Studien nach TEXT-12. Aufbau: [spec/lernen.md](spec/lernen.md). | Tradition darf erklären, aber nicht wie Evidenz aussehen. | auto: `ContentRulesTest teaser` (`lore-teaser`), `health-claims`; manuell: R-TEXT |
| TEXT-14 | MUSS | **Sanskrit klein, kursiv, mit Längenzeichen** (IAST): Fachwörter der Lehre zwischen Sternchen („*prāṇa*“), kursiv gesetzt über `sanskritText`. Aufrecht bleiben Eigennamen (Übungen, Techniken, Personen, Schulen, Texttitel); geht es um die Wortbedeutung, ist der Name ein Wort. Eingedeutschtes normal, Nutzernamen wörtlich (`marks = false`). Beispiele: [spec/lernen.md](spec/lernen.md). | Einheitlich und nachschlagbar; Kursiv trennt Fachwort von Eigenname. | manuell: R-TEXT |
| TEXT-15 | MUSS | **Einstellungs-Texte nach einem Muster** ([spec/einstellungen.md](spec/einstellungen.md)): je Karte und je Schalter höchstens ein Satz, wofür sie gilt bzw. was er bewirkt; ein Hinweis wiederholt weder Titel noch Segmente; Langes nur im Aufklapper; was zwei Karten teilen, steht einmal, die zweite verweist. | Hinweise, die den Titel wiederholen, machen das Menü länger, nicht klarer. | manuell: R-TEXT; auto nur die Darstellung: `ScreenshotTest` (`05_einstellungen_daten`) |
| TEXT-16 | MUSS | **Die App zählt, sie bewertet nicht:** Sitzungen, Minuten und Tage in Folge sind nüchterne Angaben („3 Tage in Folge · 45 min in den letzten 7 Tagen“), die Serie ist abschaltbar („Serie zeigen“); keine Ziele, kein Soll, keine Vergleiche, keine Trends mit Urteil, keine Auswertung mit gesundheitlicher Bewertung (SEC-LEGAL-02). Befinden am Ende: nur das Wort am Eintrag, nie gerechnet (SEC-LEGAL-04). | Kein Medizinprodukt, kein Leistungsdruck – Atem ist keine Zahl. | manuell: R-TEXT |

### 11.1 Mikrotexte und Begriffsliste

**Buttons** im Infinitiv, 1–2 Wörter („Starten“, „Speichern“; Ausnahmen „Verstanden, starten“ und „Nochmal“/„Ende“ am Ende-Screen). **Status** klein, unaufdringlich („· angepasst“, „lädt … 42 %“). **Datenschutz-Hinweis** in der App nach [SECURITY.md, Prinzipien](SECURITY.md#prinzipien).

**Studienlage** (TEXT-07, `EvidenceLevel`), genau eine Stufe je eingebauter Übung: „gut belegt“ = Meta-Analyse oder mehrere unabhängige RCTs zur Anwendung · „in Studien geprüft“ = ein RCT oder mehrere kleine Studien, dünn oder nur für eine Gruppe · „kaum untersucht“ = Mechanismus plausibel.

| Verwenden | Nicht verwenden |
|---|---|
| Sitzung | Session |
| Übung (Oberbegriff), Atemtechnik (Fachtext), Muster (generiert/eigene) | Programm, Technik als Oberbegriff |
| Programm (nur Kapitel „Programme & Gewohnheiten“), Chip „zum Nachlesen“ an seinen Karten | Chip „Programm“ |
| Bereich (die vier Familien in UI und Store: „Mehr: …“, „17 Atemübungen in vier Bereichen“) | Gruppe, Familie (nur Code) |
| Eigenes Muster (selbst zusammengestellt), Eigene Kreationen (Liste im Tab „Meins“), Tab „Meins“, Verlauf (Kopf mit Wochenpunkten, führt zum Logbuch) | Custom, Vorlage, Konfigurator, Meine Muster (nur in Code und Doku) |
| Letzte 7 Tage (heute und die sechs davor, wie die Wochenpunkte: „45 min in den letzten 7 Tagen“; als Gruppe im Logbuch die übrigen dieser Tage nach „Heute“ und „Gestern“) | diese Woche (zählte mal rollend, mal ab Montag) |
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
| CODE-01 | MUSS | **Schichten:** Jede Schicht importiert nur, was die Tabelle „Schichten“ unten erlaubt; `domain/` ist reines Kotlin (keine JVM-APIs, KMP-fähig; offene Fälle in [IOS_PLAN.md](IOS_PLAN.md) §4); Controller ohne UI liegen in `playback/`. Ausnahmen stehen nur in der Tabelle und in `check_style.py`. | Testbarkeit, iOS-Plan, Aufteilung nach Features. | auto: `layer-import-*`, `ui-feature-import`, `domain-platform` |
| CODE-02 | MUSS | Screens (`XScreen`, `XOverlay`) bekommen Werte und `on…`-Lambdas, nie Repository, ViewModel, Controller oder `CoroutineScope`; höchstens 8 Parameter (ohne `modifier`), sonst `XUiState` und `XActions`. Zustand halten nur die vier State-Holder-Arten unter der Tabelle „Schichten“ unten. | Testbar, vorschaubar; kein ViewModel je Push-Ziel ([DECISIONS 2026-10-24](DECISIONS.md)). | auto: `ui-state-holder`, `screen-params`, ktlint `compose:vm-injection-check`; manuell: R-CODE |
| CODE-03 | MUSS | Flows mit `collectAsStateWithLifecycle`; Eingaben und offene Dialoge mit `rememberSaveable`; Timer und Audio im `viewModelScope` oder app-weiten Controller; `CancellationException` nie schlucken. | Lifecycle, Rotation, strukturierte Nebenläufigkeit. | auto: `lintDebug`; manuell: R-CODE |
| CODE-04 | MUSS | Öffentliche Composables in `ui/components/`: `modifier: Modifier = Modifier` als erster optionaler Parameter, am Root angewendet (außer ohne eigenen Layout-Knoten: `AppBottomSheet`, `HideSystemBars`); Reihenfolge Pflicht, `modifier`, optional, Content-Lambda; KDoc; privates `@Preview` in `AtemkraftTheme`. | Wiederverwendbar, sichtbar. | auto: ktlint `compose:modifier-*`, `param-order-check`, `preview-public-check` |
| CODE-05 | MUSS | Navigation nur über `@Serializable`-Routen in `ui/navigation/Routes.kt`; unauflösbare Route → `popBackStack()`. Persistierte Enums über `name`, nie `ordinal`. Logik hängt nie an Anzeigetexten. | Kein leerer Screen, keine stille Datenverschiebung. | auto: `ordinal-persist`; manuell: R-CODE |
| CODE-06 | MUSS | ktlint (`intellij_idea`, ohne Zeilenlimit, keine Wildcards, keine voll qualifizierten Namen). Kommentare und KDoc deutsch, erklären das Warum; `TODO` nennt eine Backlog-ID (`// TODO(S-12)`). | Diff-Ruhe, Nachverfolgbarkeit. | auto: `ktlintCheck`, `todo-unlinked` |
| CODE-07 | SOLL | Tests nur für den Kern: Daten überleben Migration und Export/Import, Muster des Tages unverändert, Sicherheitsgrenzen der freien Folge, Gates (Wortbruch, Abschneiden, Tippfläche, Anatomie), ehrliche Texte, Navigations-Grundregel. Neue Tests nur, wo ein Fehler Daten, Sicherheit oder diese Gates trifft; Namen als deutscher Satz in Backticks. | Wenige Tests, die etwas bedeuten, statt einer langsamen Suite ([DECISIONS 2026-10-22](DECISIONS.md)). | auto: `testDebugUnitTest` (nur, dass sie grün sind); manuell: R-CODE (neuer Test nur für Daten, Sicherheit, Gates) |
| CODE-09 | MUSS | **Ablauf fehlertolerant:** Ein unbehandelter Fehler im Ablauf einer Sitzung (Atemübung, Meditation) beendet sie wie Stopp bzw. Abbruch, gibt den `PlaybackService` frei und geht an `UncaughtErrors`; keine Sitzung bleibt laufend stehen. Abbrechen ist kein Fehler (CODE-03). | Eine hängende Sitzung hielte Service, Wakelock und Benachrichtigung ohne Ende. | auto: `PlaybackFlowTest` (Fehler im Ablauf) |
| PERF-01 | SOLL | **Leistung.** Kein I/O und kein `runBlocking` auf dem Main-Thread (Room, DataStore, Dateien, Netz über `suspend`/`Flow`). Endlos-Animationen lesen die Zeit nur im Draw und laufen nur sichtbar (`RhythmCurve` ruht unter dem Vollbild). Keine Arbeit pro Frame in der Komposition: Werte je Frame liest erst Layout oder Draw, Teures steht in `remember(key)`, Reserve-Breiten in `TextWidths`. | Ruckelfreies Bild auf 120 Hz, Akku in langen Sitzungen; Referenz: [Messwerte](recherche/2026-10-30-iso25010.md#messwerte). | auto: `perf-main`; manuell: R-PERF |

**Schichten (CODE-01):** Die Checks `layer-import-*`, `ui-feature-import` und `domain-platform` setzen die Tabelle um; ihre Ausnahmen stehen dort und hier.

| Schicht | importiert aus `app.atemkraft` nur | Ausnahme |
|---|---|---|
| `domain/` | `domain/`; keine JVM-APIs (KMP-fähig) | `java.text.Normalizer` (IOS_PLAN §4); `javaClass` in `AppScopes` (Marker, S-41) |
| `data/` | nichts aus `ui/`, `cue/`, `playback/` | – |
| `cue/` | `cue/`, `domain/`, `BuildConfig`, `R` | – |
| `playback/` | nichts aus `ui/` (Controller ohne UI) | – |
| `ui/components/` | `ui/components/`, `ui/theme/`, `domain/`, `R` | `LocalTabBarState`, `TabBarState` aus `ui/navigation/` |
| `ui/<feature>/` | aus `ui/` nur `theme`, `components`, `navigation` und sich selbst | `own/` → `ui/log/LogHistory` |
| `data/`, `playback/`, `ui/` | aus dem Wurzelpaket nur `BuildConfig` und `R` | – |

**State-Holder (CODE-02):** `XViewModel` nur für ab App-Start Gesammeltes · `XDestination` für Push-Ziele mit eigener Logik · `GraphState` / `PlaybackState` im Wurzelpaket · `rememberX`-Holder für Zustand mit Activity-Bezug.

## <a id="automatische-pruefung"></a>13 Automatische Prüfung

Grundsatz: wenige, wartungsarme Werkzeuge; Altlasten über eine Baseline mit S-ID eingefroren. Wie jedes Werkzeug im Einzelnen arbeitet (Optionen, Gerätematrix, Referenzbilder, CI), steht in [CHECKS.md](CHECKS.md#werkzeuge); die Sicherheitsprüfungen außerhalb von `check_style.py` (`verify-apk`, `release-verify`, `check-github`) in [SECURITY.md, Automatische Prüfung](SECURITY.md#automatische-pruefung).

### 13.1 Ablauf

| Werkzeug | Aufgabe |
|---|---|
| `scripts/check.sh` | **Einziger Einstieg**, lokal und im CI: `check-style.sh --self-test` und Prüfung → `scripts/build.sh ktlintCheck verifyRoborazziDebug lintDebug assembleRelease -PunsignedRelease=true` → `verify-apk` auf die Prüf-APKs; `--fast` ohne Lint, Release-Build und `verify-apk`. Nie `./gradlew` direkt. Hooks in `.githooks/`: `commit-msg`, `pre-push` (`--ci-range`). |
| `scripts/check-style.sh` | Stil-, Doku- und `sec-*`-Checks aus `scripts/check_style.py`, jeder mit Fixtures in `config/style-fixtures/`. Kommentarzeilen (außer bei `todo-unlinked`) und Zeilen mit Marker `// Abweichung <ID>:` bzw. `// dekorativ:` zählen nicht, außer bei `sec-*`. `--self-test`, `--list`, `--update-check-table`, `--update-baseline`, `--ci-range A..B`. |
| `scripts/bump-version.sh` | Versionsbump an einer Stelle (PROZ-05); `version-sync` prüft. |
| ktlint, Android Lint | Format und Compose-Regeln (`.editorconfig`), Lint als Gate (`warningsAsErrors`); beide ohne Baseline. |
| JUnit-Tests | `ContentRulesTest` (ehrliche Texte, ohne Baseline), Daten-, Ablauf- und Navigationstests, `ThemeContrastTest` (§2.1, §2.2). |
| Screenshot-Gates | `ScreenshotTest` (360 dp bei Schrift 1,0 und 2,0), `DeviceFitTest` (Gerätehöhen), `TabMatrixTest` (A54 und Tablet, Referenzbilder pixelgenau). Jeder Befund ist ein Fehler: Text `WORTBRUCH`, `GETRENNT`, `ABGESCHNITTEN`, `CHIP_UMBRUCH`, `ZUGESCHNITTEN`; Bedienelemente `TIPPFLAECHE`, `OHNE_KLICK`, `BESCHNITTEN`, `AUSSERHALB`; `ANATOMIE` (§4.5), `SCROLLT` (LAYOUT-08), `KOMPAKT_FEHLT` (MUSTER-10). Gewollte Änderung: Referenzbilder neu aufnehmen (PROCESS, Checkliste eines Pushes). |
| CI | `.github/workflows/ci.yml`: Job `check` (`check.sh`), Job `conventions` (`--ci-range` über alle Commits); Dependabot monatlich. |

### 13.2 Baseline

`config/style-baseline.txt` (`check⇥datei⇥anzahl⇥S-ID`) friert bekannte MUSS-Verstöße ein; jede Zeile nennt den Backlog-Punkt, der sie behebt (`baseline-ids`). Mehr Treffer als Baseline → Fehler. Weniger → Fehler „Baseline senken“ (`baseline-stale`, PROZ-02). SOLL-Checks warnen nur. Keine Baseline haben `sec-*` (ein Eintrag ist selbst ein Fehler) und die Inhaltsregeln des `ContentRulesTest`. Eine bewusste Ausnahme gehört nicht in die Baseline, sondern als Marker `// Abweichung <ID>: <Grund>` an die Zeile.

### 13.3 Checks

Jede Regel nennt ihre Checks in der Spalte Prüfung. Die aus dem Code erzeugte Tabelle Check → Regel → Scope → Stufe steht in [CHECKS.md](CHECKS.md#checks) (`check-style.sh --list`; `check-table` hält sie aktuell), die Prüfungen außerhalb von `check_style.py` (Tests, Gates, Lint) [darunter](CHECKS.md#weitere).

### 13.4 Manuelle Checklisten

| Liste | Wann | Schritte |
|---|---|---|
| R-A11Y | UI-Änderung; vor Release Session, Meditation, Einstellungen | TalkBack: Name, Rolle, Zustand, Überschriften; Phase, Pause, Ende, Restzeit hörbar; keine Glyphen, Einheiten ausgeschrieben · Voice Access, Switch Access, D-Pad: alles erreichbar, Fokus sichtbar und sinnvoll · MUSTER-05 mit TalkBack: angesagt, kein Rückfall · Systemsprache Englisch: Texte bleiben deutsch · Accessibility Scanner ohne neue Funde |
| R-VISUAL | UI-Änderung | **Agent:** Gates grün (`ScreenshotTest`, `DeviceFitTest`, `TabMatrixTest`), neue Screens und Zustände als Szene aufnehmen, Referenzbilder bei gewollter Änderung neu aufnehmen (§13.1), Token-Maße benachbarter Elemente im Code vergleichen (§4.5) – Bilder werden nicht angesehen (PROZ-07). **Mensch am Gerät (A54):** Köpfe weiß, Gruppenlabels in Familienfarbe, Kapitelkarten reihum · Querformat, Tablet ohne Beschnitt · heller Systemmodus: Statusleisten-Icons hell · Rotation mit offenem Dialog · „Animationen entfernen“: Pacer läuft, Shimmer steht · eine Neon-Fläche, Farben in ihrer Rolle, Gerüst nach §4 |
| R-TEXT | Inhaltsänderung | Teaser vollständig, derselbe Punkt wie das Detail, Studienlage-Stufe passend (TEXT-07) · keine Heilaussage, Begriffsliste, Laiensprache, zählt statt bewertet (TEXT-16) · Sicherheitsrelevantes in `cautions`, Studien mit Originaltitel in `Refs.kt` · Leer- und Fehlertexte mit nächstem Schritt |
| R-AUDIO | Änderung in `src/cue/` oder Session-Abläufen | alle Lautstärkestufen ohne Klicks, Gong klingt aus · stumm: fremde Musik nicht geduckt; Pause gibt Fokus ab · Vibration aus: kein Tick; System-Tippvibration aus, App-Schalter an: Tick spürbar · Bildschirm aus, Home-Taste: Ton läuft, nach Fortsetzen synchron · Kopfhörer abziehen (Kabel, Bluetooth) in Übung, Meditation und Editor-Vorschau: kein Ton aus dem Lautsprecher, Ablauf und Vibration laufen weiter, Ton-Schalter auf stumm bzw. aus |
| R-CODE | Code-Änderung | CODE-01 … -03, -05; KOMP-01; TYPO-02; MOTION-01; AUDIO-01; Layout nach §4.5 · ändert sich das Sicherungsformat oder das Room-Schema: neue Fixture bzw. Migrationsfall mit Daten der Vorversion (PROZ-08) |
| R-PERF | vor Release; Änderung an Start, Listen, Editor, Sitzung oder Abhängigkeiten | Release-Build am A54, Werte mit Datum und Version zur letzten Messung legen ([Qualitätsbericht](recherche/2026-10-30-iso25010.md#messwerte)) · Baseline-Profil vorher übersetzen: `adb install-multiple <apk> <dm>` oder nach dem ersten Start `adb shell cmd package compile -f -m speed-profile app.atemkraft` ([STORE.md](STORE.md)) · Kaltstart: `adb shell am start -S -W app.atemkraft/.MainActivity`, `TotalTime` als Median aus 3 · Bildraten: `adb shell dumpsys gfxinfo app.atemkraft reset`, dann im Atmen-Tab scrollen bzw. den Editor über „Muster des Tages“ → „Selbst gestalten“ und über „Meins“ → „+“ öffnen (je 1.–3. Öffnen mit eigenem `reset`, Wege abwechselnd, ≥ 1 s nach Blatt bzw. Tab tippen), danach `dumpsys gfxinfo app.atemkraft` (Frames, Ruckler-Anteil, p50/p90/p99) · Speicher: `adb shell dumpsys meminfo app.atemkraft` nach dem Start mit und ohne installierte Stimme (PSS, Native Heap) |
| R-STORE | Store-Update | Screenshots aus Release-Build, dunkel, Demo-Statusleiste, ohne echte Logbuchdaten · Feature-Grafik 1024 × 500 ohne Alpha, Icon 512 × 512 aus der Palette · Texte nach TEXT-11 ([STORE.md](STORE.md)) |
| Vor Release | jedes Release (Design-Teil des Qualitätszyklus PROZ-09) | R-A11Y, R-VISUAL (Mensch), R-AUDIO und R-PERF auf echtem Gerät, bei Store-Update R-STORE · Changelog nach TEXT-11 · Backlog und Baseline aktuell. Einstieg und Reihenfolge: [PROCESS.md, Checkliste eines Releases](PROCESS.md#release) |

## <a id="quellen"></a>Quellen

RFC 2119 · [WCAG 2.2](https://www.w3.org/TR/WCAG22/) · [WCAG2ICT](https://www.w3.org/TR/wcag2ict-22/) · [Compose Accessibility](https://developer.android.com/develop/ui/compose/accessibility/key-steps) · [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) · [Edge-to-edge](https://developer.android.com/develop/ui/compose/system/setup-e2e) · [Android 17 Background-Audio](https://developer.android.com/about/versions/17/changes/bg-audio) · [Per-App-Sprache](https://developer.android.com/guide/topics/resources/app-languages) · [ktlint](https://pinterest.github.io/ktlint/) · [compose-rules](https://github.com/mrmans0n/compose-rules) · EU MDR 2017/745 (Erwägungsgrund 19, Art. 2 Nr. 12) · [Play Health Content](https://support.google.com/googleplay/android-developer/answer/14738291) · [Play-Listing](https://support.google.com/googleplay/android-developer/answer/9866151) · [F-Droid-Metadaten](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/) · interne Audits an `2a1cb20` (Kontrast nach WCAG 2.x, ΔE nach CIE76) · [Guardrails-Audit 2026-10-24](recherche/2026-10-24-guardrails.md).
