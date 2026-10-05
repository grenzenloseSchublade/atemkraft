# Atemkraft – Styleguide

Normativer Design-, Text- und Code-Leitfaden für Maintainer und Claude. Er dient als Leitplanke und als Grundlage der automatischen Vereinheitlichung: Jede Regel nennt ihre Prüfung.
Stand: v1.5.1 + Fix-Durchgang (Basis `2a1cb20`), 2026-10-01. Sicherheit, Datenschutz, Build und Release regelt [SECURITY.md](SECURITY.md).

**Inhalt:** [0 Geltung](#geltung) · [1 Prinzipien](#prinzipien) · [2 Farbe](#farbe) · [3 Typografie](#typografie) · [4 Layout](#layout) · [5 Komponenten](#komponenten) · [6 Muster](#muster) · [7 Icons](#icons) · [8 Bewegung](#bewegung) · [9 Audio und Haptik](#audio) · [10 Barrierefreiheit](#a11y) · [11 Text und Ton](#text) · [12 Code](#code) · [13 Automatische Prüfung](#automatische-pruefung) · [14 Backlog](#backlog) · [Quellen](#quellen)

## <a id="geltung"></a>0 Geltung

- **Pfade:** `src/` = `app/src/main/java/app/atemkraft/`, `res/` = `app/src/main/res/`. `Datei:Zeile` bezieht sich auf den Commit, der diese Fassung einführt.
- **Regelformat:** `ID | Stufe | Regel | Warum | Prüfung`. IDs sind ab dieser Fassung stabil und werden nie neu vergeben; eine gestrichene Regel bleibt als „entfällt → Ziel“ stehen. Den Ist-Stand führt nur der [Backlog](#backlog).
- **Prüfung:** `auto: <check>` verweist auf §13 (noch nicht eingerichtet, Block A). `manuell: R-…` verweist auf eine Checkliste in §13.4.

| Stufe | Bei Verstoß |
|---|---|
| **MUSS** / **DARF NICHT** | CI rot. Altlasten sind per Baseline eingefroren (§13.2). Marker `// Abweichung <ID>: <Grund>` nur, wo die Regel eine Ausnahme nennt. |
| **SOLL** | Abweichung nur mit Marker `// Abweichung <ID>: <Grund>` in derselben Zeile; ohne Marker warnt die Automatik. |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| META-01 | MUSS | Fügt ein Commit ein Token in `src/ui/theme/**` hinzu oder entfernt eines, oder ändert er die Signatur einer öffentlichen `fun` in `src/ui/components/**`, ändert er auch diesen Guide (sonst Trailer `Guide: n/a` mit Grund). Ein behobener Backlog-Punkt und seine Baseline-Einträge fallen im selben Commit weg. | Guide und Baselines driften sonst. | auto: `guide-sync`, `baseline-stale` |

**Kurz-Checkliste neuer Screen / Änderung:** Tokens statt Literale (FARBE-01, LAYOUT-01, TYPO-01, MOTION-04) · vorhandene Komponente, neue Kontrollen integriert, Bedienelemente nur über die Wrapper aus §5.1 (KOMP-01, -04, PRIN-03, LAYOUT-04) · Name, Rolle, Zustand, Überschrift, sichtbarer Fokus (A11Y-01, -07) · kein Text in fester Höhe, fontScale 2,0 und Querformat geprüft (LAYOUT-03) · Kontrast nach Alpha-Mischen (FARBE-03) · Texte in `strings.xml`/`src/data/`, „…“, Laiensprache, ohne Heilversprechen (TEXT-01, -05, -09, -10) · Löschen zweistufig im Element (MUSTER-05) · `scripts/check.sh` grün.

## <a id="prinzipien"></a>1 Prinzipien

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| PRIN-01 | MUSS | **Nur dunkel.** Ein `darkColorScheme`; kein `lightColorScheme`, kein `dynamicColor`, kein `Light`/`DayNight`-Fenster-Theme, kein `values-night/`. `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), SystemBarStyle.dark(TRANSPARENT))`. | Abendnutzung; Neon-Palette fällt auf Hell durch AA; mit `auto` sind Statusleisten-Icons im hellen Systemmodus unsichtbar. | auto: `dark-only` |
| PRIN-02 | MUSS | **Ruhig.** Pro Screen höchstens eine gefüllte Neon-Fläche (in der Regel Start). Neon sonst nur als Text-, Linien- oder Icon-Akzent. In der Session ist der Atemkreis die einzige gesättigte Fläche; Buttons dort sind gedeckt. | Nutzung mit halb geschlossenen Augen. | auto: `neon-fill`; manuell: R-VISUAL |
| PRIN-03 | MUSS | **Minimal und integriert.** Aktionen mit eindeutigem Symbol (Zurück, Löschen, Ton, Zurücksetzen, Info, Anhören, Minimieren, Speichern als Lesezeichen, Neu generieren) sind Icon-only mit `contentDescription`. Neue Kontrollen kommen in ein bestehendes Hauptelement (Split-Button, Label-Morph, Zustandswechsel). Keine FABs, Toasts, Snackbars, keine frei schwebenden Buttons. | Nutzerpräferenz. | auto: `no-fab-toast`; manuell: R-VISUAL |

## <a id="farbe"></a>2 Farbe

### 2.1 Tokens ([Color.kt](../app/src/main/java/app/atemkraft/ui/theme/Color.kt), [Theme.kt](../app/src/main/java/app/atemkraft/ui/theme/Theme.kt))

Kontrast (WCAG 2.x) der Token-Farbe als Vordergrund gegen `background` #160F2E und Card #2E2150, Alpha vorher mit dem realen Grund gemischt.

| Token | Hex | M3-Rolle | Bedeutung | vs BG | vs Card |
|---|---|---|---|---|---|
| `NeonMagenta` | #F25CA2 | primary | Marke, Aktion, Auswahl, Links | 5,98 | 4,71 |
| `NeonCyan` | #34E0E8 | secondary | Struktur-Header, eigene Muster, Meditation | 11,36 | 8,95 |
| `NeonYellow` | #F9C80E | tertiary | nur über `WarnAmber` | 11,64 | 9,17 |
| `OnNeon` / `OnCyan` | #1E0A16 / #042A2C | onPrimary, onTertiary, *Soll* onError / onSecondary | Inhalt auf Neon | 6,15 auf primary, 6,96 auf error | – |
| `DarkBackground` / `DarkSurface` | #160F2E / #211640 | background / surface, surfaceContainer | Screen-Grund (`window_background`) / Leisten (Elevation 3 dp → #321C48) | – | – |
| `DarkSurfaceVariant` | #2E2150 | surfaceVariant, surfaceContainerHighest | **Card-Fläche**, Mini-Bar | 1,27 | – |
| `SurfaceContainerHigh` / `Lowest` / `Low` | #281B48 / #120C26 / #1B1236 | surfaceContainerHigh/Lowest/Low | Dialog / nur implizit von M3 gelesen | – | – |
| `SecondaryContainer` | #3A2C60 | secondaryContainer | Tonal-Buttons, gewähltes Segment | 1,50 | 1,18 |
| `OutlineVariantIndigo` | #453763 | outlineVariant | **einziger** Trenner, dekorativ | 1,73 | 1,37 |
| *Soll* `OutlineIndigo` | #9A8FBF | outline | Rand von Switch, Chip, Segment (heute M3-Grau #938F99) | 6,18 | 4,87 |
| `DarkOnBackground/OnSurface` | #F0E6FF | onBackground, onSurface | Primärtext | 15,30 | 12,05 |
| `DarkOnSurfaceVariant` | #C3B8DD | onSurfaceVariant | leiser Text (Dialog, Mini-Bar) | 9,83 | 7,74 |
| `FamilyVagal` / `Sympathetic` / `Balance` / `Functional` | #60A5FA / #F59E0B / #A78BFA / #34D399 | – | Familie A „Herunterregeln“ / B „Hochregeln“ / C „Balancieren“ / D „Funktionell“ | 7,23 / 8,56 / 6,76 / 9,57 | 5,70 / 6,75 / 5,32 / 7,54 |
| `EvidenceBest` / `SynthTrack` | = NeonCyan | – | „am besten belegt“ / Kreis-Bahn, Fortschritts- und Heute-Ring | 11,36 | 8,95 |
| `EvidenceCaution` | #FF6B8B | *Soll* error | Vorsicht, Fehler, Bestätigungszustand (MUSTER-05); Leiste 5,54 | 6,77 | 5,33 |
| `WarnAmber` | = NeonYellow | – | Sicherheit, Intensität | 11,64 | 9,17 |
| `SynthCircleCenter` / `Edge` | #CB3C9A / #4A2080 (α 0,72) | – | Atemkreis-Verlauf | 4,07 / 1,44 | – |
| `SessionTextYellow` / `SessionNoteAmber` | #EBDCB0 / #F0E4C0 | – | Phase und Sekunden / Phasen-Notiz im Kreis | 13,48 / 14,51 (Zentrum 3,31 / 3,56) | – |
| `SessionTextGlow` | #160B22 | – | Textschatten, Label auf Cyan-Button | – | – |
| `SessionButtonCyan` / `Pink` | #6FB6BA / #FF8FB0 | – | Session-Buttons, „Beenden“ | 7,95 / 8,59 | – |
| `SynthText`, `SynthGlow`, `PauseGlyphCreme` | #FFEAF7, #2A0A2E, #FFF3D6 | – | ungenutzt (Backlog) | – | – |

ΔE76 der Pinktöne: primary ↔ `EvidenceCaution` 19,9, primary ↔ `SessionButtonPink` 23,0, `EvidenceCaution` ↔ `SessionButtonPink` 18,1.

### 2.2 Alpha-Tokens

| Stufe | Token | Wert | min. Kontrast BG / Card | Verwendung |
|---|---|---|---|---|
| Primär | – | 1,0 | 15,30 / 12,05 | Titel, Labels, Werte |
| Fließtext | `BODY` (*neu*) | 0,8 | 10,01 / 8,24 | Detail- und Glossartext |
| Sekundär | `SECONDARY` | 0,7 | 7,88 / 6,66 | Untertitel, Beschreibung, Status |
| Tertiär | `TERTIARY` | *Soll* 0,55 (Ist 0,5) | 5,28 / 4,70 (Ist 4,57 / 4,14 ✗) | DOI/PMID, Zeitstempel, Wochentage |
| Disabled | M3-intern | 0,38 | – | nur inaktive Bedienelemente |
| Flächen | `ALPHA_CHIP` / `ALPHA_BORDER` / `ALPHA_TRACK` / `ALPHA_DISABLED_BORDER` (*neu*) | 0,10 / 0,35 / 0,10 / 0,4 | – | Chip-Fläche, Kartenrand, Kreis-Track, inaktiver Rand |

### 2.3 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| FARBE-01 | MUSS | Farben nur aus `MaterialTheme.colorScheme` (für Rollen mit M3-Entsprechung) oder aus `Color.kt` (Family\*, Evidence\*, Session\*, Synth\*). Kein `Color(0x…)` außerhalb `src/ui/theme/`. Hex in `res/` nur für `window_background`, Launcher und `#FF000000`/`#FFFFFFFF` in einfarbigen `drawable/ic_*.xml`. | Eine Quelle der Wahrheit. | auto: `color-source` |
| FARBE-02 | MUSS | Alpha nur über die Tokens aus §2.2, auch nicht bedingt (`if (…) 0.8f else 0.6f`). | 39 freie Literale haben die Hierarchie verwischt. | auto: `alpha-literal` |
| FARBE-03 | MUSS | Kontrast nach Alpha-Mischen gegen den realen Grund: Text < 24 sp (< 18,66 sp Bold) ≥ 4,5:1, großer Text, Icons, Zustandsgrafik ≥ 3:1. Kritischer Punkt zählt (Kreiszentrum, gemischte Chip-Fläche, Leiste #321C48). Ausgenommen: rein dekorative Linien/Flächen mit Marker `// dekorativ: <Grund>`. | WCAG 1.4.3 / 1.4.11. | auto: `ThemeContrastTest`; manuell: R-A11Y |
| FARBE-04 | MUSS | Semantische Farben tragen nur ihre Bedeutung: `EvidenceBest` Evidenz, `EvidenceCaution` Einschränkung und `error`, `WarnAmber` Sicherheit/Intensität, `Family*` Familie, `primary` Marke/Aktion/Auswahl. „ruhig“/„aktivierend“ in Familienfarbe oder neutral. Semantische Farben auf demselben Screen: ΔE76 ≥ 25. | Heute doppelt belegte Farben, drei kaum unterscheidbare Pinktöne. | auto: `ThemeContrastTest` (ΔE); manuell: R-VISUAL |
| FARBE-05 | MUSS | Farbe ist nie alleiniger Träger: Evidenz und Warnung mit Text, Familie als Text oder in der Semantik, Auswahl mit Häkchen oder Kontur. Zustand (gewählt, aktiv, heute) hat ≥ 3:1 zur Umgebung oder ein integriertes Formmerkmal. | WCAG 1.4.1 / 1.4.11. | manuell: R-A11Y |
| FARBE-06 | MUSS | Von M3 implizit gelesene Rollen sind gesetzt (`outline`, `error`, `onError`, `primaryContainer`). Jedes Token in `Color.kt`, `Dimens`, `Sizes`, `Motion` ist genutzt (außerhalb seiner Datei referenziert oder in `Theme.kt` zugewiesen); seine KDoc stimmt. | Sonst M3-Baseline-Grau, tote Tokens, falsche KDoc. | auto: `theme-tokens` |

## <a id="typografie"></a>3 Typografie

Quelle [Type.kt](../app/src/main/java/app/atemkraft/ui/theme/Type.kt): M3 `Typography()`, Roboto, fünf Rollen überschrieben.

Größen in sp (Schrift/Zeile), **effektiv nach `TEXT_SCALE` = 0,75** in `Type.kt`: Ein Faktor skaliert alle Rollen zentral (Nutzerentscheidung 2026-10-05; vorher 1,0, dann 0,7 – am Gerät „ein kleines bisschen zu klein“). Die Systemschriftgröße wirkt zusätzlich. Fließtext liegt damit unter 12 sp: Lesbarkeit der kleinsten Rollen (`labelSmall`, `bodySmall`) bei jeder Änderung am Gerät prüfen (R-VISUAL).

| Rolle | sp | Gewicht | Zweck (normativ) |
|---|---|---|---|
| `headlineLarge` | 25,5/30 | Bold | Titel von Tab-Screens |
| `headlineMedium` | 21/27 | Bold | Titel von Push-Screens, Abschluss-Titel, Phasenname im Kreis |
| `headlineSmall` | 18/24 | SemiBold | `SectionHeader` |
| `titleLarge` | 16,5/21 | SemiBold | Karten- und Expander-Titel, Stepper-Wert |
| `titleMedium` | 12/18 | SemiBold | Stepper-Label, Overlay-Titel, Mini-Bar-Status, Kennzahlen |
| `bodyLarge` | 12/18 | M3 | Screen-Untertitel, Kurzbeschreibung, Zeilen-Labels, Phasen-Notiz |
| `bodyMedium` | 10,5/15 | M3 | Fließtext, Kartentext, Teaser, Dialogtext |
| `bodySmall` | 9/12 | M3 | Hinweise unter Kontrollen, Zitat, Datum, Footer |
| `labelLarge` | 10,5/15 | M3 | Eyebrow, Buttons |
| `labelMedium` | 9/12 | M3 | Status-Suffix „· angepasst“, Mini-Bar-Titel, Nav-Label |
| `labelSmall` | 8,25/12 | M3 | Chips, DOI/PMID, Wochentage |
| `displaySmall` | 27/33 | M3 | Countdown, Meditationszeit (`GlowText`) |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| TYPO-01 | MUSS | Schrift nur über `MaterialTheme.typography.<Rolle>`. Kein `.sp`, `fontSize`, `letterSpacing`, `lineHeight` oder `FontWeight` außerhalb `src/ui/theme/`; anderes Gewicht = neue Rolle. Kein Gewichtswechsel bei Auswahl. | Skalierung an einer Stelle; Gewichtswechsel springt die Breite. | auto: `type-literal` |
| TYPO-02 | MUSS | Die Rolle folgt der Tabelle; eine neue Verwendung wird erst eingetragen, dann gebaut. | Lesbare Hierarchie. | manuell: R-CODE |
| TYPO-03 | MUSS | `headline*`- und `title*`-Rollen tragen in `Type.kt` `LineBreak.Heading` (gleichmäßige Zeilen) und `Hyphens.None`. **Keine Rolle schaltet Silbentrennung ein.** | Silbentrennung trennt auch Wörter, die ganz in die nächste Zeile passen würden („Wach & energie-geladen werden“ schon bei fontScale 1,0); ohne sie bricht Compose zu lange Wörter dagegen an beliebiger Stelle („Resonanz-Atmun / g“). Ganze Wörter sichert deshalb das Layout (LAYOUT-03), nicht die Typografie. | auto: `TypographyTest` |

## <a id="layout"></a>4 Layout, Abstände, Formen

### 4.1 Abstände ([Dimens.kt](../app/src/main/java/app/atemkraft/ui/theme/Dimens.kt)), Raster 4 dp

Seit `TEXT_SCALE` 0,75 eine Stufe kompakter (Nutzerentscheidung 2026-10-05), damit Luft und Schrift im Verhältnis bleiben; Wert vor der Umstellung in Klammern. Streu-Literale für Abstände gibt es in `src/ui/` nicht mehr (LAYOUT-01). Touch-Ziele hängen nie an diesen Werten: Eigene klickbare Flächen tragen `heightIn(min = MinTouchTarget)`; die Optik der Bedienelemente kommt aus §4.2.

| Token | dp | Rolle |
|---|---|---|
| `ScreenPadding` | 16 (20) | horizontaler Screen-Rand |
| `CardPadding` | 12 (16) | Karten-Innenabstand |
| `ListGap` | 8 (12) | **Karte ↔ Karte (immer)**, Listen, Button-Reihen |
| `SessionPadding` | 20 (24) | Rand der Vollbild-Sessions |
| `ScreenTop` / `ScreenTopSub` / `ScreenBottom` | 16 / 8 / 16 (20 / 12 / 24) | oben Tab-Screen / oben Push-Screen / unten alle Scroll-Screens |
| `SectionGap` / `SectionHeaderTop` | 12 / 20 (16 / 28) | nur zwischen Blöcken **verschiedenen Typs** / über `SectionHeader` |
| `GapSmall` / `GapTiny` / `GapHairline` | 8 / 4 / 2 | Label → Steuerung / Hint, Icon ↔ Text / Eyebrow → Titel, enge Divider |
| `MinTouchTarget` | 48 | Mindesthöhe eigener klickbarer Flächen |

### 4.2 Größen ([Sizes.kt](../app/src/main/java/app/atemkraft/ui/theme/Sizes.kt))

**Bedienelemente folgen der Schrift** (Nutzerentscheidung 2026-10-05: mitwachsend, ganze dp, vorerst ohne 4-dp-Raster). Sichtbare Größe = M3-Wert × k, auf ganze dp gerundet; `k = controlScale(fontScale)` = `TEXT_SCALE` × Systemschrift, begrenzt auf 0,8 … 1,0. So stehen Bedienelement und Beschriftung im M3-Verhältnis (Button 40 dp zu `labelLarge` 14 sp), bei großer Systemschrift erreichen sie wieder die M3-Größe statt neben dem Text zu verschwinden. Die **Tippfläche wird nie skaliert**: `Dimens.MinTouchTarget` 48 (A11Y-05). Gesetzt werden die Tokens nur in `src/ui/components/` (LAYOUT-04), an M3-Elementen nur als `heightIn(min = …)` (LAYOUT-06).

| Systemschrift | 1,0 | 1,1 (A54) | 1,3 | 2,0 |
|---|---|---|---|---|
| k | 0,8 | 0,825 | 0,975 | 1,0 |

| Token | M3 | dp bei 1,0 / 1,1 / 1,3 / 2,0 | Verwendung |
|---|---|---|---|
| `ControlScale` | 1 | k | `scaledLayout` für Switch (`ToggleRow`) und Radio (`SegmentedChoiceRow`) |
| `ButtonHeight` | 40 | 32 / 33 / 39 / 40 | Mindesthöhe: Start und Reset, `Session*Button`, `AppTextButton`, `DisclosureToggle`, Segmente |
| `ChipHeight` | 32 | 26 / 26 / 31 / 32 | `SelectChip` |
| `IconButtonSize` | 40 | 32 / 33 / 39 / 40 | Kreis von `AppIconButton` und den Stepper-Knöpfen |
| `IconDefault` | 24 | 19 / 20 / 23 / 24 | Icon in `AppIconButton` (Zurück, Zahnrad, Anhören, Löschen), Expander-Caret, Mini-Leiste |
| `IconInButton` | 18 | 14 / 15 / 18 / 18 | Icon neben Beschriftung: Reset, Segment-Häkchen, `DisclosureToggle`-Caret |
| `IconSmall` | 16 | 13 / 13 / 16 / 16 | Info-Icon „Anleitung“ in der Session |
| `ProgressInline` | 20 | 16 / 17 / 20 / 20 | Lade-Kreis in der Stimmenliste |
| `StepperValueMinWidth` | 48 | 38 / 40 / 47 / 48 | Wertfeld des Steppers (kein Touch-Ziel) |

Nicht skaliert: `NavigationBar` (M3 1.3.1: Höhe 80, Indikator 64 × 32 fest, S-37) und die Icons von `OverlayChrome` in der laufenden Sitzung (bewusst M3, Bedienung mit halb geschlossenen Augen). Die Mini-Leiste hat keine eigene Höhe mehr (`MinTouchTarget`, vorher 60 dp mit Leerraum).

*Geplant* (S-29, heute noch Literale): `BorderThin` / `ProgressStroke` / `RingStroke` 1 / 2 / 2,5 dp · `PauseFlashSize` 150 · `PinnedBarElevation` 3 · `ContentMaxWidth` 600 · `DotSmall` / `DotLarge` / `AccentBarWidth` 10 / 18 / 4 dp (Balkenhöhe folgt `headlineSmall`) · `CircleMaxWidthFraction` / `HeightFraction` 0,9 / 0,62.

### 4.3 Formen und Screen-Gerüst

| Shape | Radius | Wofür |
|---|---|---|
| `extraSmall` / `small` / `medium` / `extraLarge` | 8 / 12 / 16 / 28 dp | Info-Chip / FilterChip / Card / Dialog (`large` 20 dp ungenutzt) |
| `CircleShape` | – | Punkte, Balken, Buttons |

| Typ | Container | oben | Kopf | unten |
|---|---|---|---|---|
| Tab | `Surface(background)` → `LazyColumn(spacedBy(ListGap))`, horizontal `ScreenPadding` | `ScreenTop` | `headlineLarge` + Untertitel `bodyLarge` `SECONDARY`, optional Icon-Aktion rechts | `ScreenBottom` |
| Push | `Surface(background)` → `Column` aus `PushHeader` (fest) und `Column(Modifier.weight(1f)).verticalScroll`, ggf. gepinnte Startleiste | `ScreenTopSub` (im `PushHeader`) | `PushHeader`: Pfeil und Titel `headlineMedium` fest (Einstellungen, Glossar) oder nur der Pfeil fest, wenn der Kopf zum Inhalt gehört (Über: Markenkopf; Detail: Eyebrow `labelLarge` + `TitleWithChips`) | `ScreenBottom` |
| Vollbild-Session | `OverlayChrome` → Inhalt mit `SessionPadding` | `safeDrawing` | Minimieren links, Ton rechts | – |

### 4.4 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| LAYOUT-01 | MUSS | Abstände nur über `Dimens`, Größen über `Sizes`, Formen über `MaterialTheme.shapes.*` oder `CircleShape`. Keine `.dp`-Literale (außer `0.dp`) und kein `RoundedCornerShape`/`CutCornerShape` außerhalb `src/ui/theme/`. Nie eckige Schnitte. | 160 Literale, 36 davon nicht auf dem Raster; zwei Chip-Radien. | auto: `layout-literal` |
| LAYOUT-02 | MUSS | Screens folgen dem Gerüst §4.3: Karte ↔ Karte immer `ListGap`, `SectionGap` nur zwischen Blöcken verschiedenen Typs. Insets nur über `Scaffold`-`innerPadding` oder `safeDrawingPadding()`, keine festen Leistenhöhen. | 8 nachgebaute Kopf-Blöcke, drei Kartenabstände; Edge-to-edge ab API 35 erzwungen. | manuell: R-VISUAL |
| LAYOUT-03 | MUSS | **Kein Wort bricht um, kein Text wird gekürzt** – auf 360–412 dp Breite bei fontScale 1,0–2,0 (nichtlinear wie Android 14+), Querformat und Tablet ebenso. Eine Zeile beginnt nie mitten im Wort, mit Bindestrich oder Satzzeichen (`badBreakAt`). Mittel, in dieser Reihenfolge: (1) Layout gibt Platz – Titel mit Chips nur über `TitleWithChips`, Button-Reihen nur über `AdaptiveButtonRow`, Einfachauswahl über `SegmentedChoiceRow` (fällt auf Radioliste zurück), Text + Aktion in einer `FlowRow`, Sessions über `SessionRunningLayout`; (2) Titel, Namen und Beschriftungen über `WholeWordText` (verkleinert bis 70 %, Tabs bis 50 %, gleichrangige Texte mit gemeinsamer Größe); (3) Fließtext so formulieren, dass kein Wort die Zeile sprengt. Verboten: `maxLines` + Ellipse auf Inhaltstext, feste `height`/`size` für Text-Container (`heightIn(min)` erlaubt), `weight(…, fill = false)` für Text neben Chips, Orientierungssperre. | Nutzerbefund auf dem Galaxy A54 (384 dp, fontScale 1,1): „Resonanz-Atmun / g“, „Beende / n“, gekürzte Sicherheitshinweise („…Fahren oder Stehen …“ ohne „Ohnmachtsgefahr“). WCAG 1.4.4, 1.4.10, 1.3.4. | auto: `ScreenshotTest` (Testfehler bei jedem Befund), `weight-nofill`, `platform-override`; manuell: R-VISUAL |
| LAYOUT-04 | MUSS | **Optik folgt der Schrift, zentral.** Die sichtbare Größe jedes Bedienelements (Höhe von Button, Chip, Segment; Kreis des Icon-Buttons; Icons; Switch; Radio; Inline-Spinner) kommt nur aus `Sizes` (§4.2). Die Tokens setzen nur die Komponenten in `src/ui/components/`; am Aufrufort keine Größe, kein `scale`, keine Einzelkorrektur. M3-Größenkonstanten (`ButtonDefaults.MinHeight` …) nur in `src/ui/theme/`. | `TEXT_SCALE` 0,75 hat nur die Schrift verkleinert: Bedienelemente wirkten 1,33-mal zu groß (Button 40 dp neben 11,6 sp, Session-Buttons 48). Einzelkorrekturen laufen auseinander. | auto: `SizesTest` (Optik/Schrift je Token bei fs 1,0 / 1,1 / 1,3 / 2,0, Ober- und Untergrenze), `m3-size-const`, `control-direct`, `layout-literal`; manuell: R-VISUAL |
| LAYOUT-05 | MUSS | **Bedienelemente bleiben ganz.** Kein Bedienelement wird beschnitten oder aus dem Bild geschoben: waagerecht schneidet nie ein Elternteil ab, senkrecht nur eine Scroll-Fläche; Beschriftung und Icon bleiben im sichtbaren Behälter, bei fontScale 2,0 wächst er mit. | Kleinere Tokens und feste Höhen machen genau das wahrscheinlich; LAYOUT-03 prüft nur Text. | auto: `ScreenshotTest` (`BESCHNITTEN`, `AUSSERHALB`) |
| LAYOUT-06 | MUSS | **Bauregeln für Bedienelemente.** (1) Höhe an M3-Elementen nur `heightIn(min = Sizes.…)`, nie `height`/`size`/`requiredSize`. (2) Eigene feste Größe nur als `minimumInteractiveComponentSize().size(x)`, in dieser Reihenfolge. (3) Kein `Modifier.scale` und kein `graphicsLayer { scaleX/scaleY }` auf Bedienbarem; Switch und Radio werden über `scaledLayout(Sizes.ControlScale)` verkleinert, die Tippfläche trägt ein äußerer `toggleable`-/`selectable`-Knoten. (4) Gleiche Höhe nebeneinander nie über `IntrinsicSize.Min` oder feste Constraints, sondern über gleiche Mindesthöhe, gleiches Polster und gleich hohen Inhalt. | `height` nahm dem Reset die 48-dp-Reservierung (58 × 40); `scale` verkleinert den Hit-Test mit (Switch 39 × 36); `IntrinsicSize.Min` und `Constraints.fixed` messen die 48-dp-Tippfläche mit und blähen die Optik auf 48 (Session-Buttons). | auto: `control-scale`, `touch-order`, `m3-size-const`, `ControlTouchTest` (Tap-Injektion), `ScreenshotTest` (`TIPPFLAECHE`) |
| LAYOUT-07 | SOLL | **Steuerung beim Label.** In Karten und Einstellungen steht die Steuerung als rechte Spalte, ihre *sichtbare* Kante bündig an der Inhaltskante (unsichtbaren Touch-Rand vom Polster abziehen, kein `offset`; umgesetzt für Stepper und Schalter, Icon-only-Buttons am Zeilenende offen: S-38). Im freien Fluss (Meditations-Tab) folgt sie direkt dem Label (`Stepper(inline = true)`), linksbündig wie die Chips. Ein Schalter steht in der Zeile seines Namens, der Hinweis darunter (`ToggleRow`). | Nutzerbefund „weit versetzt“: 100–130 dp zwischen Label und Stepper, Switch neben dem Hinweis statt neben „Sprachanleitung“. | manuell: R-VISUAL |

## <a id="komponenten"></a>5 Komponenten

### 5.1 Katalog (`src/ui/components/`, `src/ui/session/`)

| Komponente | Nutzen für | Nicht für | A11y-Pflicht |
|---|---|---|---|
| `AppIconButton` | jede Icon-only-Aktion: Kreis `IconButtonSize` (Ripple nur dort), Icon `IconDefault`, Tippfläche 48; `colors` wie M3 (Stepper: gefüllt) | Aktionen mit Text (→ `AppTextButton`); Aktionen mit Zustand (→ `AppIconToggle`) | cd am `Icon` (ICON-02), Rolle Button |
| `AppIconToggle` | Icon-only-Umschalter mit Zustand (Muster speichern ↔ entfernen); Optik und Tippfläche wie `AppIconButton`, den Zustand zeigt das Icon (gefüllt/leer) | Ton in Overlays (→ `OverlayChrome`, `Role.Switch`); Einstellungen (→ `ToggleRow`) | cd am `Icon`, `Role.Checkbox`, `stateDescription`, `liveRegion = Polite` |
| `AppTextButton` | Text-Aktion ohne Fläche (Laden, Wählen, Speichern, Dialog), Mindesthöhe `ButtonHeight` | Primäraktion (→ `StartSplitButton`) | Text = Name |
| `BackButton` | Zurück-Pfeil im `PushHeader` (AutoMirrored, über `AppIconButton`; Versatz −(48 − `IconDefault`)/2 bündig) | Overlays (dort Minimieren); direkt im Scroll-Inhalt (scrollt aus dem Bild) | cd `action_back` |
| `BreathingCircle` | Atem-Pacer (`fraction` 0…1 → Skala 0,42…1,0) | dekorative Kreise | Aufrufer setzt Name, `onClickLabel`, Zustand, Fokus (A11Y-07) |
| `Chip` / `TagChip` / `InfoChip` | nicht klickbare Info-Pillen | Auswahl (→ `SelectChip`) | Text Pflicht; einzige Info-Chip-Implementierung |
| `DisclosureToggle` | Einstellungen in Karten aufklappen (Caret `ExpandCaret` in `IconInButton`) | Inhalt mit Teaser | `stateDescription` „Ausgeklappt“/„Eingeklappt“ inkl. „angepasst“ |
| `ExpanderSection` | Titel, Teaser (immer ganz), Detail; ohne Detail ein schlichter Abschnitt ohne Caret | Einstellungen; aufklappbare Abschnitte einer Liste (→ `SectionHeader` mit `expanded`) | `heading()`, `Role.Button`, `stateDescription` „Ausgeklappt“/„Eingeklappt“ (nur aufklappbar) |
| `AdaptiveButtonRow` | Reihe gleichrangiger Buttons: gleich breit → nach Inhalt → Primär oben + Rest darunter → untereinander | Aktionspaare mit Split-Button (→ `StartSplitButton`) | Lesereihenfolge bleibt; Beschriftung einzeilig |
| `MiniNowPlayingBar` | minimierte laufende Session über der NavigationBar | andere Meldungen | ≥ 48 dp, `maxLines = 1`, ein Fokus-Stopp |
| `OverlayChrome` | Rahmen jedes Vollbild-Overlays | Push-Screens | Ton als `Role.Switch` + `stateDescription`, `paneTitle` |
| `ReferenceItem` | eine Quelle (Zitat, DOI/PMID) | – | Kennung ≥ 4,5:1 |
| `ScreenHeader` | Kopf der Haupt-Tabs: Titel, optional Untertitel und Zahnrad (nur Tabs mit eigenen Einstellungen: Atmen, Meditation) | Push-Screens (→ `PushHeader`) | Titel `heading()`; Zahnrad cd `settings_title` |
| `PushHeader` | fester Kopf jeder Push-Seite über der Scroll-Fläche: `BackButton` und optional Titel (`headlineMedium` über `WholeWordText`, Zeile `heightIn(min = 48)`); ohne eigene Fläche und Erhöhung, bringt `ScreenPadding` und `ScreenTopSub` selbst mit; Titel folgt dem Pfeil im Abstand des Touch-Überstands | Tab-Screens (→ `ScreenHeader`); Köpfe, die zum Inhalt gehören (Markenkopf, Titel mit Chips → dann ohne `title`) | Titel `heading()`; Pfeil cd `action_back`, Tippfläche 48 |
| `SectionHeader` | Abschnittskopf mit Akzentbalken; optional `teaser` (ganzer Satz unter dem Titel) und `trailingAction` (ein `AppIconButton` für den ganzen Abschnitt, z. B. „Muster neu generieren“), rechts unten bündig, sodass die 48-dp-Tippfläche im Platz des Kopfes liegt. Mit `expanded` aufklappbar (MUSTER-03): Caret am Titel (`ExpandCaret`), ganze Zeile samt Teaser ist die Tippfläche (≥ 48 dp), den Inhalt zeigt der Aufrufer darunter in einer `Column` mit `animateContentSize` (Situationen-Tab) | Kartentitel; Aktionen, die nur eine Karte betreffen (→ KOMP-02); `trailingAction` zusammen mit `expanded` | `heading()` am Titel; aufklappbar zusätzlich `Role.Button`, `stateDescription` „Ausgeklappt“/„Eingeklappt“ |
| `SubLabel` | Unter-Überschrift in Karten und Abschnitten (eine Ebene unter `SectionHeader`) | Abschnittsköpfe | – |
| `SegmentedChoiceRow` | Einzelwahl, 2–4 kurze Optionen (`modifier`); Segmente `ButtonHeight`, Häkchen `IconInButton`; passt eine Beschriftung nicht ins Segment, Radioliste (Radio per `scaledLayout`, Zeile 48 dp) | > 4 Optionen | Gruppenlabel verknüpft; Radioliste mit `selectableGroup()`, `Role.RadioButton` |
| `SelectChip` | Einzelwahl aus vielen Presets (`ChipHeight`, `modifier`) | Info | Häkchen, `Role.RadioButton`, `selectableGroup()` |
| `SessionAdjustCard` | Dauer und Intervalle vor dem Start | – | „· angepasst“ im Toggle-Zustand |
| `Stepper` | ganzzahliger Wert in festem Bereich; Knöpfe `AppIconButton` (gefüllt, Icons „−“/„+“), Wert `StepperValueMinWidth`; Karten: rechte Spalte, Kreis bündig; `inline = true`: direkt hinter dem Label (LAYOUT-07) | Freitext | Knöpfe mit Klick, Rolle, Disabled und Name „Label, Erhöhen“; Wert als Live-Region; Einheit ausgeschrieben |
| `WholeWordText` | Titel, Namen, Beschriftungen, die nie im Wort brechen dürfen; verkleinert schrittweise (`minScale`; gemeinsame Größe über `sharedScale` + `onSharedScaleTooBig`) | Fließtext (→ umformulieren) | wie `Text` |
| `SessionRunningLayout` | Gerüst laufender Sessions: Kopf, Kreis im freien Platz, Steuerung | andere Screens | Kopf liegt über dem Kreis, nicht davor |
| `AppNavigationBar` | Haupt-Tabs unten | Push-Ziele | eine Beschriftungsgröße für alle Tabs |
| `TitleWithChips` | Titel mit nachgestellten Chips (Karten, Detail-Kopf); Chips rutschen bei Platzmangel unter den Titel | Abschnittsköpfe (→ `SectionHeader`) | Titel-Rolle und Farbe vom Aufrufer; keine Chips → `null`, kein leerer Slot |
| `StartSplitButton` | **jeder** Start einer konfigurierbaren Session; Start und Reset gleich hoch (MUSTER-01) | andere Aktionspaare | Reset Icon-only, cd `adjust_reset`; Fokus → Start, wenn Reset verschwindet |
| `ToggleRow` | Ein/Aus-Einstellung: Label, Switch in derselben Zeile, optional Hinweis darunter; Switch per `scaledLayout`, sichtbar bündig | Auswahl aus mehreren (→ `SegmentedChoiceRow`) | ganze Zeile `toggleable(Role.Switch)`, Name = Label (A11Y-03) |
| `SessionControls.kt` (`GlowText`, `Session*Button`, `FinishedPanel`, `rememberTapFlash`), `PauseFlash` | Text, Buttons, Tap-Rückmeldung **nur** in Vollbild-Sessions | normale Screens, stehender Status | `FinishedPanel` sagt das Ende an; `PauseFlash` dekorativ |
| `SafetyDialog` | Sicherheitsbestätigung vor intensiven Übungen | Infos ohne Risiko | nicht außen schließbar, scrollbar |

Feature-Wrapper ohne eigene Regeln: `MiniSessionBar`, `SessionOverlay`. **Geplant** (ersetzen Duplikate): `PinnedActionBar`, `Bullet`, `CardTitle`/`Hint` (öffentlich). Die Wrapper `AppIconButton`, `AppIconToggle`, `AppTextButton`, `ToggleRow` und `scaledLayout` (intern) liegen in `Controls.kt`.

### 5.2 Regeln

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| KOMP-01 | MUSS | Gibt es eine Komponente in §5.1, wird sie genutzt; kein Inline-Nachbau. Wird ein Muster zum zweiten Mal gebraucht, wandert es nach `src/ui/components/`. | Konsistenz; 15 Duplikate im Audit. | manuell: R-CODE |
| KOMP-02 | MUSS | Klickbare Karten: `Card(onClick = …)`, nie `Card(Modifier.clickable)`. Darin höchstens eine weitere Aktion, integriert als trailing `AppIconButton` (Tint `SECONDARY`) in der obersten Inhaltszeile, ohne eigene Zeile oder Fläche. Darf die Kartenhöhe nicht bestimmen: dann als Overlay in der Ecke (`Box` mit Inhalt, Ende-Padding `MinTouchTarget`, Aktion `align(TopEnd)` mit voller Tippfläche). Ausnahme **Zustandsakzent**: Ein `AppIconToggle` (Lesezeichen im „Muster des Tages“) zeigt „gespeichert“ gefüllt in `secondary` statt `SECONDARY`; erneutes Tippen nimmt das Muster wieder heraus (sofort umkehrbar, daher nicht zweistufig wie MUSTER-05). | Ripple und Rolle stimmen; keine verschachtelten Ziele. | auto: `card-style`; manuell: R-VISUAL |
| KOMP-03 | MUSS | Karte: Default-Farbe, `shapes.medium`, `CardPadding`, Titel `titleLarge`, Text `bodyMedium` `SECONDARY`; Rand (`BorderThin`, `ALPHA_BORDER`, Akzentfarbe) nur bei navigierenden Inhaltskarten. Trenner nur `HorizontalDivider()` mit Default-Farbe; Ausnahme Marken-Trenner über der NavigationBar (markiert). | Heute drei Trenner-Varianten. | auto: `card-style`; manuell: R-VISUAL |
| KOMP-04 | MUSS | M3-Steuerelemente (`Button`, `TextButton`, `FilledTonalButton`, `OutlinedButton`, `IconButton`, `FilledTonalIconButton`, `FilterChip`, `SegmentedButton`, `Switch`, `RadioButton`, `Checkbox`, `Slider` …) stehen nur in `src/ui/components/`; Screens nutzen die Wrapper aus §5.1. | Nur so greift LAYOUT-04 an einer Stelle; vorher 14 direkte Aufrufe in 6 Dateien mit eigenen Größen. | auto: `control-direct` |

## <a id="muster"></a>6 Muster

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| MUSTER-01 | MUSS | **Split-Button.** Start (`Button`, primary) in voller Restbreite; Reset (`FilledTonalButton`, `ic_reset`, Icon-only) gleitet nur ein, wenn ein Wert vom Default abweicht; kein Bestätigungsdialog. Gleiche Höhe über dieselbe Mindesthöhe (`Sizes.ButtonHeight`), dasselbe senkrechte Polster und ein Icon-Feld so hoch wie die Textzeile – nie feste Höhe, nie `IntrinsicSize.Min` (LAYOUT-06). Sitzt in der gepinnten Leiste (`surface`, Elevation 3 dp). | Eine Fläche statt zwei; Reset nur, wenn er etwas bewirkt. | manuell: R-VISUAL |
| MUSTER-02 | MUSS | **Rückmeldung im Element.** Bestätigung als Zustandswechsel des auslösenden Elements („Speichern“ → „✓ Gespeichert“; Icon-only: `AppIconToggle`, leeres → gefülltes Lesezeichen mit `stateDescription` „Gespeichert“), lesbar (≥ 4,5:1, nicht disabled) und als `liveRegion = Polite` angesagt. Abweichung vom Default als Suffix „· angepasst“ (`labelMedium` `SECONDARY`) am Element und in dessen `stateDescription`; kein Badge, kein loses Geschwister-`Text`. | Nichts frei Schwebendes; TalkBack erfährt den Erfolg. | auto: `no-fab-toast`; manuell: R-A11Y |
| MUSTER-03 | MUSS | **Ausklappen.** `ExpanderSection`: Teaser steht immer ganz da (nie gekürzt, LAYOUT-03), Detail erscheint darunter (TEXT-06). Jedes Ein-/Ausklappen animiert die Höhe (`animateContentSize`/`AnimatedVisibility`). Lange Listen gleichartiger Abschnitte werden zur **Übersicht**: `SectionHeader` mit `expanded` und Teaser, Standard zu, Zustand per `rememberSaveable` (Situationen-Tab als Befindens-Übersicht; selbst Angelegtes wie „Meine Muster“ bleibt offen). Kein Suchfeld für eine Handvoll fester Kategorien. | Kontext bleibt sichtbar; Sprünge irritieren. Situationen-Tab a54 fs110 von rund 2540 auf 895 dp, ohne Tastatur, Treffer-null-Fälle oder Tag-Pflege. | auto: `ContentRulesTest teaser`, `SituationsScreenTest`; manuell: R-VISUAL |
| MUSTER-04 | SOLL | **Leer und Fehler.** Leer: „Noch keine …“ plus sanfte Einladung, ohne Illustration oder Extra-Button. Fehler inline im Element: was passiert ist und was du tun kannst; kein `e.message` in der UI. | Nutzer wissen, was zu tun ist; Exception-Texte sind technisch und oft englisch. | auto: `exception-message-ui`; manuell: R-TEXT |
| MUSTER-05 | MUSS | **Zerstörende Aktion** (Logbuch, gespeicherte Muster, Stimmen): zweistufig im selben Element. Erster Tipp morpht in den Bestätigungszustand (Label „Wirklich leeren?“ bzw. Bestätigungs-Icon mit cd „‚X‘ wirklich löschen?“, Farbe `error`, `liveRegion`), zweiter Tipp löscht. Rückfall nach 4 s, nicht solange Screenreader/Switch Access aktiv ist oder das Element Fokus hat. Kein Dialog, keine Snackbar. | Schutz vor Fehltipp ohne Dialog (SEC-PRIV-04). | manuell: R-A11Y |
| MUSTER-06 | MUSS | **Sicherheitsdialog** nur für `EvidenceTag.CAUTION`: M3 `AlertDialog`, Warn-Icon (cd null), Hinweisliste scrollbar, „Abbrechen“ links, „Verstanden, starten“ rechts, `dismissOnClickOutside = false`; erscheint bis zur ersten Bestätigung, danach in den Einstellungen abschaltbar. | Bewusste Bestätigung vor intensiven Übungen. | manuell: R-A11Y |
| MUSTER-07 | MUSS | **Session.** Buttons nur `SessionPrimary/Secondary/StopButton`. Alles, was per Kreis-Tap geht (Pause, Weiter), hat zusätzlich einen benannten Button; in `WAITING_FOR_USER` morpht der Primärbutton zu „Weiter“. Minimiert ist eine laufende Session immer als `MiniNowPlayingBar` sichtbar; Zurück im Overlay minimiert. | Bedienbar ohne Kreis-Geste (TalkBack, Switch Access); eine laufende Session ist nie unsichtbar. | manuell: R-A11Y |
| MUSTER-08 | MUSS | **Tabs und Zurück.** Push-Seiten haben einen festen Kopf (`PushHeader`); der Zurück-Pfeil scrollt nie aus dem Bild. Ein Tipp auf den markierten Tab führt zu dessen Startseite (`tapTab`: `popBackStack(route, inclusive = false)`, sonst `switchTab`), für alle vier Tabs. Ein anderer Tab stellt seinen gemerkten Stapel wieder her (`switchTab`, `restoreState`). Die Einstellungen bleiben eine zentrale Route. | Nutzerbefund: Aus den Einstellungen kam man nur über den Zurück-Pfeil heraus, und der war nach dem Sprung zur Meditations-Karte schon aus dem Bild gescrollt; der markierte Tab reagierte nicht. Der feste Kopf kostet beim Scrollen rund 56 dp – bewusst Platz gegen Orientierung. | auto: `TabNavigationTest`; manuell: R-VISUAL |

## <a id="icons"></a>7 Icons

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| ICON-01 | MUSS | Icons aus `material-icons-core`, Stil `Filled` (`AutoMirrored` für Richtungen), oder eigener Vektor `res/drawable/ic_<name>.xml` (24-dp-Viewport, einfarbig). Kein `material-icons-extended`, kein `Outlined`/`Rounded`/`Sharp`/`TwoTone`. Größen nur `IconDefault`/`IconInButton`/`IconSmall`. Bediensymbole („−“, „+“, „›“, „→“) sind Icons, keine Text-Glyphen. | APK-Größe, eine Linie, RTL, TalkBack liest Glyphen vor. | auto: `icon-source`, `layout-literal` |
| ICON-02 | MUSS | Icon-only-Elemente setzen `contentDescription` **am `Icon`** über `R.string.cd_*`/`action_*`, benannt nach der Aktion ohne Typwort. `contentDescription = null` nur bei dekorativem Icon oder Text im selben Knoten, mit `// dekorativ: <Grund>`. | Ein Muster statt zwei; prüfbar. | auto: `cd-style` |

## <a id="bewegung"></a>8 Bewegung

**Tokens** (*neu*: `src/ui/theme/Motion.kt`, Werte = Ist): `NavPush` `tween(300)` Slide + Fade · `NavTab` `tween(180)` Fade · `Disclosure`/`Reveal` Default-Spring · `AmbientShimmer` `tween(8000, EaseInOutSine)` reverse, Glow-α ≤ 0,16 · `TapFlashHold`/`TapFlashFade` 220 ms / 16 × 34 ms · `PacerFrame`/`ClockTick` 33 / 200 ms · `Pacer.ScaleMin`/`ScaleMax` 0,42 / 1,0.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| MOTION-01 | MUSS | Kreis, Countdown und Ton leiten sich aus der Session-Uhr ab, nie aus einer Animations-API. Nach Pause setzt auch der durchgehende Ton an der aktuellen Position fort. | Synchron, Pause friert alles ein, „Animationen entfernen“ bricht nichts. | manuell: R-CODE, R-AUDIO |
| MOTION-02 | DARF NICHT | Nichts blinkt schneller als 3 Hz; keine rotgesättigte Fläche mit Helligkeitswechsel; Pacer-Phasen < 0,5 s. Dekorative Endlos-Animationen laufen über Compose-APIs (stoppen bei Animator-Skala 0) mit effektiver Alpha-Änderung ≤ 0,1. Tap-Flash darf die Skala umgehen, zeigt bei 0 aber statisch (≈ 500 ms). | WCAG 2.3.1 / 2.2.2, ruhig. | auto: `ContentRulesTest phase-min-duration`; manuell: R-VISUAL |
| MOTION-03 | SOLL | Phasenübergänge am Kreis sind stetig: Endskala einer Phase (inkl. Vorbereitung) = Startskala der nächsten. | Sprünge wirken wie ein Blitz. | auto: `FractionContinuityTest` |
| MOTION-04 | SOLL | Dauern und Easing aus `Motion.*`; neue UI-Bewegung ≤ 300 ms, Standard-Easing oder Default-Spring, kein Bounce. | Zentral, ruhig. | auto: `motion-literal` |

## <a id="audio"></a>9 Audio und Haptik

**Ist-Werte** (`src/cue/`): Synthese 44,1 kHz PCM16 mono zur Laufzeit. Wechselton 528/396/440 Hz (Ein/Aus/Halten), 180 ms, Attack 12, Release 60 ms. Gong Teiltöne 330/331,6/894/1698 Hz, „Lang“ 11,5 s + 1,5 s Stille, „Kurz“ 6 + 1,5 s. Durchgehend 196 ↔ 294 Hz. Lautstärke Ton/Gong 0,9/1,4/1,85, Dauerton 1,2/2,0/3,2. Sprache System-TTS de-DE (Rate 0,85, Pitch 0,9) oder Piper (Speed 0,9). Haptik Tick 25 ms, Ein/Aus 110 ms, Halten 70 ms. Fokus `GAIN_TRANSIENT_MAY_DUCK`, `USAGE_MEDIA`. Defaults: Wechseltöne, Haptik an, Zäsur MEDIUM, Lautstärke LOUD, Gong lang.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| AUDIO-01 | MUSS | Töne werden offline synthetisiert; neue Audio-Assets nur mit freier Lizenz (in den Credits). Frequenzen gleichstufig begründet, keine Solfeggio- oder „Heil“-Frequenzen. Jeder Ton hat Attack ≥ 8 ms, Release ≥ 60 ms, Spitze ≤ 0,95 FS bei lautester Stufe. | Offline, F-Droid, evidenz-ehrlich, keine Klicks. | auto: `ToneEnvelopeTest`; manuell: R-CODE |
| AUDIO-02 | MUSS | Ausgabe über `USAGE_MEDIA`. Audio-Fokus nur, wenn tatsächlich etwas hörbar ist, in der Pause abgegeben, Fokusverlust per Listener behandelt. Der Gong klingt vollständig aus; Fokus/Service enden erst nach `gongTotalMs()` plus einheitlicher Reserve. | Fremde Musik bleibt sonst geduckt; abgeschnittener Gong. | manuell: R-AUDIO |
| AUDIO-03 | MUSS | Sprache schweigt bei Medienlautstärke 0, bricht vor dem End-Gong ab, überlappt nicht (`QUEUE_ADD`). | Ruhe. | manuell: R-AUDIO |
| AUDIO-04 | MUSS | Audio und Haptik hängen am `viewModelScope` bzw. Service, nie an der Composition; Player kommen aus dem `AppContainer`, nie aus `remember { … }`. Sessions mit Ton laufen mit Foreground-Service `mediaPlayback`. | Sperrbildschirm; Android 17 stummt Hintergrund-Audio ohne FGS. | auto: `player-in-composable`; manuell: R-AUDIO |
| AUDIO-05 | MUSS | Jedes Session-Ereignis (Phase, Pause, Ende) ist auch hörbar oder fühlbar. Jede Vibration folgt dem App-Schalter „Vibration“. Phasen-Haptik `USAGE_ALARM` (ab API 33 `VibrationAttributes`), Tap-Tick `USAGE_TOUCH`. Phasen unterscheiden sich in der Dauer, nicht nur in der Amplitude. | Bedienbar mit geschlossenen Augen; Nutzerkontrolle. | manuell: R-AUDIO |

## <a id="a11y"></a>10 Barrierefreiheit

Ziel: WCAG 2.2 AA (nach WCAG2ICT) plus Android-Richtlinien.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| A11Y-01 | MUSS | Jedes Bedienelement hat Name, Rolle und Zustand: klickbare Nicht-M3-Elemente mit `role` und, wenn nötig, `onClickLabel`; Icon-Umschalter (Ton) mit `Role.Switch` und `stateDescription`. Screen-Titel, `SectionHeader`, Kartentitel tragen `heading()`, zentral in der Komponente. | WCAG 4.1.2, 1.3.1. | auto: `semantics-required`; manuell: R-A11Y |
| A11Y-02 | MUSS | Semantik nicht zerstören: kein `clearAndSetSemantics` auf Klickbarem, außer `onClick(label)`, `role`, `disabled()` werden neu gesetzt; eine Container-cd verdeckt keine dynamischen Kind-Texte (Zeit, Phase). | Sonst fehlen Klick, Rolle oder Restzeit. | auto: `a11y-ratchet`, `ScreenshotTest` (`OHNE_KLICK`); manuell: R-A11Y |
| A11Y-03 | MUSS | Schalterzeilen: ganze Zeile `toggleable(role = Role.Switch)`, `Switch(onCheckedChange = null)`, Zeilentext = Name. Einzelwahl: `SegmentedChoiceRow` oder `SelectChip` mit Häkchen, `Role.RadioButton`, `selectableGroup()`, verknüpftes Gruppenlabel. | WCAG 1.4.1, 4.1.2. | auto: `switch-unlabeled`; manuell: R-A11Y |
| A11Y-04 | MUSS | Zustandswechsel ohne Fokuswechsel (Phase, Pause, Ende, Stepper-Wert, „✓ Gespeichert“, Download fertig/Fehler, MUSTER-05) haben genau eine `liveRegion = Polite` an einem nicht zusammengeführten Knoten. Sekündlich tickende Werte nicht. | WCAG 4.1.3. | manuell: R-A11Y |
| A11Y-05 | MUSS | Tippflächen ≥ 48 × 48 dp **als Platz im Layout**, unabhängig von der Optik (M3 oder `minimumInteractiveComponentSize()`, LAYOUT-06); die Hit-Test-Erweiterung von Compose zählt nicht. `LocalMinimumInteractiveComponentSize` nie ändern. Ausnahme: Link mitten im Fließtext (WCAG 2.5.8 „Inline“). | Android-Richtlinie, WCAG 2.5.8. Compose vergrößert nur den Hit-Test auf 48 – reserviert das Layout weniger, ragen Nachbarn hinein (Reset 58 × 40). | auto: `ScreenshotTest` (`TIPPFLAECHE`), `ControlTouchTest` (±23 dp trifft, ±25 dp nicht), `platform-override` |
| A11Y-06 | SOLL | Dekorative Glyphen (· • › ⚠ ✓) sind keine Fokus-Stopps; Listeneinträge sind ein Stopp. Namen tragen Kontext („Stimme Thorsten anhören“); Zeiten und Einheiten ausgeschrieben („4 Sekunden“, nicht „(s)“ oder „3:05“). | Weniger Rauschen, WCAG 2.4.6. | manuell: R-A11Y |
| A11Y-07 | MUSS | Overlays und Screens setzen `paneTitle`; verschwindet ein fokussiertes Element, setzt `FocusRequester` den Fokus. Elemente mit `indication = null` zeigen eigenen Fokus (≥ 3:1). Der Atemkreis hat keine Ripple. | WCAG 2.4.3, 2.4.7; D-Pad, Switch Access. | auto: `a11y-ratchet`; manuell: R-A11Y |

## <a id="text"></a>11 Text und Ton

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| TEXT-01 | MUSS | UI-Chrome inkl. Hilfstechnik-Texten (`contentDescription`, `stateDescription`, `onClickLabel`, Wochentage) steht in `res/values/strings.xml`; Fachinhalte (Übungen, Situationen, Refs, Cues, Glossar) als Kotlin-Daten in `src/data/`. Mengen über `<plurals>`; keine fest eingetragenen Zählungen oder Inhaltsnamen in UI-Strings. | Prüfbar; bricht nicht bei Inhaltsänderung. | auto: `ui-literal`, `lintDebug PluralsCandidate`; manuell: R-TEXT |
| TEXT-02 | MUSS | Ein Begriff pro Sache nach der Begriffsliste (§11.1), in UI und Store. Phasen-Labels eindeutig („Halten (voll)“ ≠ „Halten (leer)“). | Anzeige, TalkBack und Ansage unterscheiden sonst nicht. | auto: `ContentRulesTest phase-labels-distinct`; manuell: R-TEXT |
| TEXT-03 | MUSS | Einsprachig Deutsch: nur `res/values/`, `localeFilters += "de"`, `locales_config.xml` mit nur `de`, App-Locale `de`. Datum und Zahlen über `java.time` + `DateTimeFormatter` mit `AppLocale`; kein `SimpleDateFormat`, kein `Locale.getDefault()` in `ui/`. | Sonst Mischsprache und gemischte Formate. | auto: `locale` |
| TEXT-04 | MUSS | Anrede „du“, klein. Übungen laden ein statt anzuweisen („Wenn du magst …“). Sicherheit klar und knapp („Bei Schwindel sofort aufhören.“). | Nutzerpräferenz; Sicherheit braucht Klarheit. | auto: `ContentRulesTest formal-address`; manuell: R-TEXT |
| TEXT-05 | MUSS | Laiensprache in Situationen, Labels und Tags: kein „Schub“, „Crash“, „stabile Phase“, „vagal“, „sympathisch“, „(Para-)Sympathikus“, „Hormese“, „CO₂-Toleranz“. Fachbegriffe nur im Detail; jeder steht im Glossar. | Verständlich, kein Krankheitsjargon. | auto: `ContentRulesTest jargon` |
| TEXT-06 | MUSS | **Teaser** (`effect`, erste `caution` als Teaser): ein geschlossener Satz mit finitem Verb, endet auf „.“, ≤ 95 Zeichen (subjektloser Telegrammstil erlaubt). **Detail** (`effectDetail`) beginnt nicht mit dem Teaser, vertieft denselben Punkt, SOLL ≤ 700 Zeichen. | Nichts darf abgeschnitten wirken. | auto: `ContentRulesTest teaser`, `detail-length` (Warnung); manuell: R-TEXT |
| TEXT-07 | MUSS | Evidenz ehrlich: Schwache Evidenz (Einzelstudie, indirekt, unverblindet) steht schon im Teaser („in einer Studie“, „kaum untersucht“). Teaser, Detail, Ref-Einordnung und `EvidenceTag` widersprechen sich nicht; `BEST_EVIDENCE` nur bei Meta-Analyse oder repliziertem RCT in `Refs.kt`. Ehrlichkeit zeigen, nicht ankündigen (kein „Ehrlich:“). | Kernversprechen der App. | manuell: R-TEXT |
| TEXT-08 | MUSS | Jeder sicherheitsrelevante Satz aus dem Detail steht auch in `cautions`, als Bedingung → Handlung („Nur im Sitzen oder Liegen – nie im Wasser.“). Keine Therapie-Anweisungen. | Sichtbar ohne Aufklappen. | manuell: R-TEXT |
| TEXT-09 | DARF NICHT | **Heil-, Therapie- oder Präventionsversprechen** in Inhalten, `strings.xml`, `README.md`, `fastlane/metadata/**`, Release-Notes und `docs/PRIVACY.md`. Einzige Wortliste: `config/health-claims.txt` (z. B. „heilt“, „lindert“, „Therapie“, „beugt … vor“, „Prävention“, „nachweislich“, „in Sekunden“, „Notbremse“); Ausnahmen (Disclaimer, belegte Verneinungen) nur in `config/health-claims.allow`. Beide Dokumente und beide Prüfungen nutzen diese Dateien. Keine Baseline. Erlaubt: Entspannung, Wohlbefinden, neutral referierte Studienlage. | Kein Medizinprodukt (MDR Art. 2 Nr. 12), HWG § 3, Play-Policy; siehe [SECURITY.md, Recht](SECURITY.md#recht). | auto: `ContentRulesTest health-claims` (Inhalte, `strings.xml`), `sec-health-claims` (README, Store, Release-Notes, PRIVACY; [SECURITY.md](SECURITY.md#automatische-pruefung)); manuell: R-TEXT |
| TEXT-10 | MUSS | Zeichen: Anführungszeichen nur „…“ (U+201E/U+201C), halbe ‚…‘, Apostroph ’ – nie ASCII-`"` oder `”` (aapt2 entfernt es, in Kotlin beendet es den String). Gedankenstrich – (U+2013), nie —; Minus −; Auslassung …. Dezimalkomma; Einheit mit Leerzeichen (`4 s`, `0,1 Hz`, `93 %`); „ca.“ statt „~“ in der UI; „✓“ als Präfix. | Deutsche Typografie; Zeichen gehen im Build sonst verloren. | auto: `typo-chars`, `ContentRulesTest quotes`, `decimal-comma` |
| TEXT-11 | MUSS | Store-Texte (`fastlane/metadata/android/de-DE/`, Quelle für Play, F-Droid und GitHub-Releases): `title` ≤ 30, `short_description` ≤ 80, `full_description` ≤ 4000, `changelogs/<versionCode>.txt` ≤ 500 Zeichen. Sie folgen TEXT-04, -07, -09 und §11.1; `full_description` enthält den Disclaimer aus SECURITY.md wörtlich. Changelog: Nutzen in Laiensprache, eine Zeile pro Änderung, keine internen IDs, keine Superlative. | Store-Grenzen und -Policy. | auto: `ContentRulesTest store-lengths`; `sec-health-claims`, `sec-store-disclaimer` ([SECURITY.md](SECURITY.md#automatische-pruefung)); manuell: R-STORE |
| TEXT-12 | MUSS | Jede genannte Studie steht in [Refs.kt](../app/src/main/java/app/atemkraft/data/Refs.kt): `Nachname I. et al. (Jahr): Originaltitel. Journal Band(Heft):Seiten. <deutsche Einordnung>.`, Kennung `doi:10.… · PMID n`. Im Text „(Name Jahr)“. Jede Referenz wird genutzt. | Nachprüfbarkeit. | auto: `ContentRulesTest ref-format`, `ref-unused`; manuell: R-TEXT |

### 11.1 Mikrotexte und Begriffsliste

**Buttons** im Infinitiv, 1–2 Wörter („Starten“, „Speichern“; Ausnahme „Verstanden, starten“). **Status** klein, unaufdringlich („· angepasst“, „lädt … 42 %“). **Datenschutz-Hinweis** in der App nach [SECURITY.md, Prinzipien](SECURITY.md#prinzipien).

| Verwenden | Nicht verwenden |
|---|---|
| Sitzung | Session |
| Übung (Oberbegriff), Atemtechnik (Fachtext), Muster (generiert/eigene) | Programm, Technik als Oberbegriff |
| Laden, wird geladen | Download |
| Sprachanleitung, Stimme | Sprach-Stimme, Stimmmodell (nur Credits) |
| Beenden | Ende |
| Einatmen/Ausatmen/Halten (Labels), Einatem/Ausatem (Substantiv) | – |
| beruhigend / aktivierend | vagal / sympathisch |
| wenn du dich gesund und ausgeruht fühlst | stabile Phase, Schub, Crash |
| Atempause | Retention |

## <a id="code"></a>12 Code (Compose und Kotlin)

Logging, Speicherung, Netzwerk und Abhängigkeiten regelt [SECURITY.md](SECURITY.md) ([Prinzipien](SECURITY.md#prinzipien), [Speicherung](SECURITY.md#speicherung), [Netzwerk](SECURITY.md#netzwerk), [Lieferkette](SECURITY.md#lieferkette)).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| CODE-01 | MUSS | Schichten: `domain/` reines Kotlin; `data/` importiert nichts aus `ui/`; Nicht-UI-Controller liegen außerhalb `ui/`; `ui/components/` hängt nur von `ui/theme/`, `domain/` und `R` ab. | Testbarkeit. | auto: `layer-import` |
| CODE-02 | MUSS | Screens bekommen Werte und `on…`-Lambdas, nie Repository oder ViewModel; ab ≈ 8 Parametern ein `XUiState` oder fachliches Objekt. | Testbar, vorschaubar. | auto: ktlint `compose:vm-injection-check`, `vm-forwarding-check`; manuell: R-CODE |
| CODE-03 | MUSS | Flows mit `collectAsStateWithLifecycle`; Eingaben und offene Dialoge mit `rememberSaveable`; Timer und Audio im `viewModelScope` oder app-weiten Controller; `CancellationException` nie schlucken (`runCatching` nur mit erneutem Werfen). | Lifecycle, Rotation, strukturierte Nebenläufigkeit. | auto: `lintDebug` (`StateFlowValueCalledInComposition`, `CoroutineCreationDuringComposition`); manuell: R-CODE |
| CODE-04 | MUSS | Öffentliche Composables in `ui/components/`: `modifier: Modifier = Modifier` als erster optionaler Parameter, am Root angewendet; Reihenfolge Pflicht, `modifier`, optional, Content-Lambda; KDoc; privates `@Preview` in `AtemkraftTheme`. | Wiederverwendbar, sichtbar. | auto: ktlint `compose:modifier-*`, `param-order-check`, `preview-public-check` |
| CODE-05 | MUSS | Navigation nur über `@Serializable`-Routen in `Routes.kt`; unauflösbare Route → `popBackStack()`. Persistierte Enums über `name`, nie `ordinal`. Logik hängt nie an Anzeigetexten. | Kein leerer Screen, keine stille Datenverschiebung. | auto: `ordinal-persist`; manuell: R-CODE |
| CODE-06 | MUSS | ktlint (`intellij_idea`, ohne Zeilenlimit, keine Wildcards, keine voll qualifizierten Namen). Kommentare und KDoc deutsch, erklären das Warum; `TODO` nennt eine Backlog-ID (`// TODO(S-12)`). | Diff-Ruhe, Nachverfolgbarkeit. | auto: `ktlintCheck`, `todo-unlinked` |
| CODE-07 | SOLL | Unit-Tests für Domain- und Timing-Logik, Namen als deutscher Satz in Backticks; ViewModels mit `kotlinx-coroutines-test`. | Ist: kein VM-Test. | auto: `testDebugUnitTest` |
| CODE-08 | MUSS | Commit-Betreff `vX.Y.Z: <Zusammenfassung>` für Releases, sonst `build:`/`docs:`/`fix:` + Zusammenfassung, deutsch mit echten Umlauten. | Ist-Stil. | auto: `commit-msg` |

## <a id="automatische-pruefung"></a>13 Automatische Prüfung

Eingerichtet; offen sind nur die Sicherheitsschritte (warten auf die Veröffentlichung von SECURITY.md, A-13) und Referenzbilder (A-12). Grundsatz: wenige, wartungsarme Werkzeuge; Altlasten über Baselines eingefroren. Die Sicherheitsschritte definiert [SECURITY.md, Automatische Prüfung](SECURITY.md#automatische-pruefung); hier steht nur ihre Stelle im Ablauf.

### 13.1 Ablauf

| Werkzeug | Datei | Aufgabe |
|---|---|---|
| `scripts/check.sh` | – | **einziger Einstieg**, lokal und im CI: Stil-Checks (`--self-test`, dann Prüfung) → `check-security.sh`, sobald vorhanden (A-13) → `scripts/build.sh ktlintCheck testDebugUnitTest lintDebug`. `--fast` ohne `lintDebug`. Nie `./gradlew` direkt. Optional `.githooks/pre-push` → `check.sh --fast`. |
| `scripts/check-style.sh` | `scripts/check_style.py`, `config/style-baseline.txt`, `config/style-fixtures/<check>.{pos,neg}` | Checks aus §13.3 in Python 3 (im Skript teils in Unter-Checks aufgeteilt, z. B. `color-source-res`, `typo-chars-strings`). Zeilenweise, (ml) über die ganze Datei; `ui-literal` über die ganze Anweisung, damit Umformatieren keinen Fund „behebt“. Ignoriert Kommentarzeilen (außer bei `todo-unlinked`) und Zeilen mit Marker `// Abweichung <ID>:` / `// dekorativ:`. `--self-test` prüft jedes Muster gegen seine Fixtures, `--update-baseline` schreibt den Ist-Stand (meldet Anstiege), `--ci-range A..B` prüft nur `guide-sync` und `commit-msg`. |
| ktlint 1.8.0 + compose-rules 0.5.8 | `.editorconfig`, Plugin `org.jlleitschuh.gradle.ktlint` 14.2.0, `config/ktlint-baseline.xml`, `.git-blame-ignore-revs` | Format, Compose-Regeln; in `*Screen.kt` ist `modifier-missing-check` aus. compose-rules 0.6.x ist gegen eine neuere Kotlin-Laufzeit gebaut und bricht im ktlint-Worker ab (`NoSuchMethodError`); Update erst mit neuerem Gradle. Bekannte Compose-Funde in der Baseline (S-32). |
| Android Lint | `lint {}`: `abortOnError`, `warningsAsErrors` (aus: `GradleDependency`, `AndroidGradlePluginVersion`, `NewerVersionAvailable`, `ChromeOsAbiSupport`); `app/lint.xml` (`ObsoleteSdkInt` nur für `mipmap-anydpi-v26`, ohne Qualifier findet aapt die adaptiven Icons nicht) | Manifest, Ressourcen, Compose-Lint. |
| JUnit-Regeltests | `ContentRulesTest`, `ThemeContrastTest`, `FractionContinuityTest`, `ToneEnvelopeTest`, `TypographyTest`, `SizesTest`, `ControlTouchTest` (Robolectric); `config/health-claims.{txt,allow}`, `config/content-baseline.txt`, `config/contrast-baseline.txt` | Inhalte, Kontrast, Pacer, Hüllkurven, Umbruch-Stile (§13.3). `config/` ist Eingabe der Test-Tasks: Eine reine Baseline-Änderung löst die Tests neu aus. |
| Screenshot-Test | `ScreenshotTest` (Robolectric + Roborazzi), `scripts/screenshot-sheet.py` | Alle Screens, Session und Meditation in Gerätehöhe, Navigationsleiste; 360 / 384 (Galaxy A54) / 412 dp × fontScale 1,0 / 1,1 / 1,3 / 2,0 über die echte Plattform-Skalierung (nichtlinear ab Android 14, am A54 auf 1 dp genau nachgemessen); Detailseiten aller Übungen auf A54 1,1 und 360 dp ab 1,3. Läuft in `testDebugUnitTest` (≈ 70 s) und **schlägt bei jedem Befund fehl**: Text (`WORTBRUCH`, `GETRENNT`, `ABGESCHNITTEN`, Regel `badBreakAt`) und Bedienelemente (`ControlFindings`: `TIPPFLAECHE`, `OHNE_KLICK`, `BESCHNITTEN`, `AUSSERHALB`; erkannt über Klick-Semantik oder clickable-/toggleable-/selectable-Modifier, nur platzierte Knoten). Zustände mit sichtbarem Reset (`03_meditation_reset`, `08_detail_angepasst`); Situationen eingeklappt und eine aufgeklappt (`02_situationen`, `02_situationen_aufgeklappt`). Bilder: `./gradlew recordRoborazziDebug` → `app/build/outputs/roborazzi/<gerät>/fs<n>/`, je Bild eine `.tsv`. Endlos-Animationen eingefroren (`InfiniteAnimationPolicy`). |
| CI | `.github/workflows/ci.yml`, `.github/dependabot.yml` | Push auf `main` und PR, `permissions: contents: read`, Actions per SHA gepinnt, `persist-credentials: false`. Job `check`: `wrapper-validation` → `setup-java` 17 → `setup-gradle` → `check.sh` (bei Fehler die Screenshot-Befunde als Artefakt). Job `conventions`: `check-style.sh --ci-range` über alle Commits des Pushes/PRs. Sicherheitsschritte laut SECURITY folgen mit A-13. Dependabot monatlich, gruppiert, `gradle` und `github-actions`. |

### 13.2 Baselines

`style-baseline.txt` (`check⇥datei⇥anzahl`), `content-baseline.txt` und `contrast-baseline.txt` (`Testfall | Datensatz bzw. Token-Paar | S-nn`) frieren bekannte MUSS-Verstöße ein. Mehr Treffer als Baseline → Fehler. Weniger → Fehler „Baseline senken“ (`baseline-stale`, META-01). SOLL-Checks warnen nur. `health-claims` hat keine Baseline. Bei Einführung werden alle bekannten Treffer mit Backlog-ID eingetragen, damit der CI ab dem ersten Lauf grün ist.

### 13.3 Checks

Muster vereinfacht, `\|` = Alternation. Ein Check ohne Ist-Treffer muss sein Positiv-Fixture treffen. Bekannte Treffer stehen nur in den Baselines (§13.2), nicht hier.

| Check | Muster / Mechanismus | Scope | Regel |
|---|---|---|---|
| `color-source` | `src/` ohne `ui/theme/`: `Color\(0x`, `\b(NeonMagenta\|NeonCyan\|DarkSurface\|DarkBackground\|OnNeon)\b` (ohne Importe); `res/` ohne `values*/colors.xml`, `drawable/ic_launcher_*`: `#[0-9A-Fa-f]{6,8}`, in `drawable/ic_*.xml` nur `#FF000000`/`#FFFFFFFF` | `src/`, `res/` | FARBE-01 |
| `alpha-literal` | `alpha\s*=\s*(?:if\s*\([^)]*\)[^,)\n]*?)?\d*\.\d+f?` | `src/ui/`, `MainActivity.kt` ohne `ui/theme/` | FARBE-02 |
| `theme-tokens` | `Theme.kt` enthält `outline\s*=`, `error\s*=`, `onError\s*=`, `primaryContainer\s*=`; jedes `val`/`const val` aus `Color.kt`, `Dimens.kt`, `Sizes`, `Motion` ist außerhalb seiner Datei referenziert oder in `Theme.kt` zugewiesen | `src/` | FARBE-06 |
| `type-literal` | `\b\d+(\.\d+)?f?\.sp\b\|FontWeight\.\|(fontSize\|letterSpacing\|lineHeight)\s*=` | `src/` ohne `ui/theme/`, Importe | TYPO-01 |
| `layout-literal` | `\b([1-9]\d*\|\d*\.\d+)f?\.dp\b`; `(RoundedCorner\|CutCorner)Shape\(` | `src/` ohne `ui/theme/` | LAYOUT-01, ICON-01 |
| `weight-nofill` | `weight\([^)]*fill\s*=\s*false` | `src/ui/` | LAYOUT-03 |
| `m3-size-const` | `ButtonDefaults\.(MinHeight\|MinWidth\|IconSize)\|FilterChipDefaults\.Height\|SwitchDefaults\.IconSize\|SegmentedButtonDefaults\.IconSize` | `src/` ohne `ui/theme/` | LAYOUT-04 |
| `control-scale` | `\.scale\(\|\bscale[XY]\s*=` | `src/ui/`, `MainActivity.kt` ohne `ui/theme/`, `components/Controls.kt` (`scaledLayout`) | LAYOUT-06 |
| `touch-order` (ml) | `\.(size\|height\|width\|required…)\(…\)\s*\.minimumInteractiveComponentSize\(\)` | `src/ui/` | LAYOUT-06 |
| `control-direct` | `(?<![\w.])(Button\|TextButton\|…\|IconButton\|FilterChip\|SegmentedButton\|Switch\|RadioButton\|Checkbox\|Slider)\(` | `src/ui/`, `MainActivity.kt` ohne `ui/components/` | KOMP-04 |
| `platform-override` | `screenOrientation\|requestedOrientation`; `LocalMinimumInteractiveComponentSize\s+provides` | Manifest, `src/` | LAYOUT-03, A11Y-05 |
| `dark-only` | `Theme\.Material\.Light\|DayNight\|lightColorScheme\|dynamic(Dark\|Light)ColorScheme\|enableEdgeToEdge\(\s*\)`; Existenz `res/values-night/` | `src/`, `res/` | PRIN-01 |
| `neon-fill` | Datei mit > 1 `(?<![A-Za-z])Button\(` oder `containerColor\s*=\s*(…colorScheme\.(primary\|secondary\|tertiary)\|Neon\w+)` | `src/ui/` ohne `components/`, `MainActivity.kt` | PRIN-02 |
| `no-fab-toast` | `FloatingActionButton\|Toast\.\|Snackbar` | `src/` | PRIN-03, MUSTER-02 |
| `card-style` (ml) | `Card\((?:(?!\)\s*\{)[\s\S])*?\.clickable`; `HorizontalDivider\((?:[^()]\|\([^()]*\))*?color\s*=` | `src/ui/`, `MainActivity.kt` | KOMP-02, -03 |
| `exception-message-ui` (SOLL) | `\b(e\|it\|t\|ex\|err\|error\|throwable)\.message\b` | `src/ui/`, `src/cue/tts/` | MUSTER-04 |
| `icon-source` | `material-icons-extended` (in `*.kts`, `gradle/*.toml`); `Icons\.(Outlined\|Rounded\|Sharp\|TwoTone)\.`; (ml) `Text\(\s*(text\s*=\s*)?"\s*[−+›→‹←]\s*"` | Build, `src/` | ICON-01 |
| `cd-style` | `IconButton\([^)]*semantics\s*\{\s*contentDescription`; `contentDescription\s*=\s*null(?!.*//\s*dekorativ:)` | `src/` | ICON-02 |
| `motion-literal` (SOLL) | `tween\(\s*(durationMillis\s*=\s*)?\d`; `val\s+\w*(dur\|Duration\|Ms\|Millis)\w*\s*=\s*\d`; in `components/` `delay\(\s*\d` | `src/ui/`, `MainActivity.kt` ohne `ui/theme/` | MOTION-04 |
| `player-in-composable` | `remember\s*\{\s*(Haptic\|ToneCue\|Continuous)\w*Player\(` | `src/ui/` | AUDIO-04 |
| `semantics-required` | `\.clickable\s*(\((?![^)]*role\s*=)\|\{)` in `src/ui/`; `heading()` fehlt in `SectionHeader.kt`, `ScreenHeader.kt` oder `PushHeader.kt` (künftig `CardTitle`) | `src/ui/` | A11Y-01 |
| `a11y-ratchet` (Review) | `clearAndSetSemantics` (ohne Importe); `indication\s*=\s*null` | `src/ui/` | A11Y-02, -07 |
| `switch-unlabeled` (ml) | `\bSwitch\((?:[^()]\|\([^()]*\))*?onCheckedChange\s*=\s*(?!null)` | `src/ui/` | A11Y-03 |
| `ui-literal` (perl) | in Zeilen mit `Text(`, `contentDescription =`, `stateDescription =`, `onClickLabel =`, `listOf(`: String-Literal mit `\p{L}` nach Entfernen von `$name`/`${…}` | `src/ui/` | TEXT-01 |
| `locale` | `Locale\.(getDefault\|GERMAN\|GERMANY)\b\|SimpleDateFormat` in `src/ui/`; `build.gradle.kts` ohne `localeFilters` mit `"de"`; Manifest ohne `android:localeConfig` | `src/ui/`, Build, Manifest | TEXT-03 |
| `typo-chars` | `„[^“<]*"` und `\\'` in `strings.xml`; `\\"` in `src/`; `”` in `src/`, `res/`; `—` in `strings.xml`, `src/data/`, `src/domain/` | `res/values*/`, `src/` | TEXT-10 |
| `layer-import` | `import app\.atemkraft\.(ui\|data)` in `domain/`; `import app\.atemkraft\.ui` in `data/`; `^import app\.atemkraft\.ui\.(?!theme\|components)` in `ui/components/` | `src/domain/`, `src/data/`, `src/ui/components/` | CODE-01 |
| `ordinal-persist` | `\.ordinal\b` | `src/data/` | CODE-05 |
| `todo-unlinked` | `//\s*TODO(?!\((S\|A)-\d+\))`, läuft ohne Kommentarfilter | `src/` | CODE-06 |
| `guide-sync` (CI) | Diff ändert `^\s*(const\s+)?val\s` in `src/ui/theme/**` oder öffentliche `^\s*fun\s` in `src/ui/components/**`, ohne `docs/STYLEGUIDE.md` und ohne Trailer `Guide: n/a` | Push-Bereich | META-01 |
| `baseline-stale` | Baseline-Eintrag ohne Treffer (alle Baselines, §13.2) | `config/*-baseline.txt` | META-01 |
| `commit-msg` (CI) | `^(v\d+\.\d+\.\d+\|build\|docs\|fix): \S` | Push-Bereich | CODE-08 |
| `ContentRulesTest` | Fälle `teaser`, `first-caution`, `detail-length` (SOLL, nur Ausgabe), `health-claims` (nur Ausgabe bis S-08 behoben, dann scharf ohne Baseline), `jargon`, `formal-address`, `decimal-comma` (`\d\.\d` außerhalb Kennungen), `quotes`, `ref-format`, `ref-unused`, `phase-min-duration`, `phase-labels-distinct`, `store-lengths` (ab `fastlane/`); Baseline `config/content-baseline.txt`. Finites Verb und Evidenzhinweis im Teaser prüft nur R-TEXT | `BuiltInExercises`, `Situations`, `Refs`, `RandomPatternGenerator.forSeed(0..6)`, `strings.xml`; `store-lengths`: `fastlane/**` (README, Store-Texte, Release-Notes und PRIVACY auf Heilversprechen: `sec-health-claims`) | TEXT-02, -04 … -06, -09 … -12, MUSTER-03, MOTION-02 |
| `TypographyTest` | jede `headline*`/`title*`-Rolle: `LineBreak.Heading`, `Hyphens.None`; keine Rolle mit `Hyphens.Auto` | Theme | TYPO-03 |
| `SizesTest` | jedes Token aus `ControlSize` bei fs 1,0 / 1,1 / 1,3 / 2,0: Optik/Schrift (`labelLarge` gegen M3) ≤ 1,1 × M3-Verhältnis, ≥ 0,9 × M3 × min(1, Schrift), ≥ 80 % M3; k monoton, bei 2,0 = 1 | Theme | LAYOUT-04 |
| `ControlTouchTest` | Tap-Injektion bei fs 1,0 / 1,1 / 2,0: `AppIconButton`, `AppIconToggle`, Zurück-Pfeil im `PushHeader` (mit Titel), Stepper-Knöpfe, `SelectChip`, `AppTextButton`, Segment, Reset ±23 dp trifft, ±25 dp nicht; `ToggleRow` über die ganze Zeile; Naht zweier Icon-Buttons; Start = Reset und Session-Buttons sichtbar `ButtonHeight`; Stepper und `ToggleRow` im TalkBack-Baum mit Name, Rolle, Zustand, Disabled und Wert-Live-Region; Selbsttest von `ControlFindings` | Komponenten | A11Y-01 … -05, LAYOUT-06, MUSTER-01, -08 |
| `DailyPatternCardTest` | „Muster des Tages“ (A54-Größe): Lesezeichen mit `Role.Checkbox`, `stateDescription`, Live-Region, schaltet hin und zurück ohne die Karte zu starten; 48-dp-Tippfläche bündig oben rechts in der Karte; Karte startet, Refresh im Abschnittskopf (Rolle Button, `heading()` am Titel); „ca. 4 min“ mit geschützten Leerzeichen | Startseite | KOMP-02, A11Y-01, -04, -05, MUSTER-02, TEXT-10 |
| `SituationsScreenTest` | Situationen-Tab als Befindens-Übersicht (A54-Größe): Abschnitte starten zu (Titel und Begründung da, keine Übung); ganze Kopfzeile ist `heading()` und `Role.Button`, klappt per Tipp auf und zu, `stateDescription` „Ausgeklappt“/„Eingeklappt“; Übungskarte startet die Übung, die Kopfzeile nicht | Situationen | MUSTER-03, A11Y-01, -04 |
| `TabNavigationTest` | NavHost mit den Routen der App und den echten Screens Einstellungen, Glossar, Über (A54-Größe): Re-Tap auf den markierten Tab führt aus Einstellungen, Glossar und Detail zur Tab-Startseite (alle vier Tabs), auf der Startseite ändert er nichts; Tab-Wechsel behält den Stapel; Zurück-Pfeil steht nach dem Sprung zur Meditations-Karte und am Ende des Glossars (360 × 640) im Bild und führt zurück; System-Zurück | Navigation | MUSTER-08 |
| `ThemeContrastTest` | WCAG-Kontrast aller Token-Paare (Vordergrund, Alpha, Hintergrund-Kette inkl. Leiste mit Tonal-Elevation 3 dp, Mindestwert); ΔE76 ≥ 25 der Semantikfarben und Pinktöne; Baseline `config/contrast-baseline.txt` | Theme | FARBE-03, -04 |
| `FractionContinuityTest` | Skala Phasenende = nächster Phasenanfang inkl. Vorbereitung, alle Built-ins und `forSeed(0..6)` (`circleFraction` in `ui/session/BreathingFraction.kt`); bekannte Sprünge in `knownJumps` mit S-ID | Session | MOTION-03 |
| `ToneEnvelopeTest` | Attack ≥ 8 ms, Release ≥ 60 ms, Spitze ≤ 0,95 FS je Stufe für Wechselton, Gong (erzeugte PCM-Daten, Pegel über 3,5 ms, bis zum 90-%-Punkt), Dauerton (über seine Konstanten) | `src/cue/` | AUDIO-01 |
| ktlint / Lint / Tests | `ktlintCheck` (+ compose-rules), `lintDebug`, `testDebugUnitTest` | Gradle | TEXT-01, CODE-02 … -04, -06, -07 |

### 13.4 Manuelle Checklisten

| Liste | Wann | Schritte |
|---|---|---|
| R-A11Y | UI-Änderung; vor Release Session, Meditation, Einstellungen | TalkBack: Name, Rolle, Zustand, Überschriften; Phase, Pause, Ende, Restzeit hörbar; keine Glyphen, Einheiten ausgeschrieben · Voice Access, Switch Access, D-Pad: alles erreichbar (auch „Weiter“), Fokus sichtbar (auch am Kreis) und sinnvoll · MUSTER-05 mit TalkBack: angesagt, kein Rückfall · Systemsprache Englisch: Texte bleiben deutsch · Accessibility Scanner auf allen 8 Screens ohne neue Funde |
| R-VISUAL | UI-Änderung | `ScreenshotTest` grün; neue Screens und Zustände dort aufnehmen · Bilder des betroffenen Screens auf A54 1,1 und 360 dp 2,0 ansehen (Layout, nicht nur Text) · Querformat, Tablet: kein Beschnitt · heller Systemmodus: Statusleisten-Icons hell · Rotation mit offenem Dialog: Zustand bleibt · „Animationen entfernen“: Pacer läuft, Shimmer steht, Flash sichtbar · eine Neon-Fläche, Farben in ihrer Rolle, Icon-only wo klar, nichts frei schwebend, Gerüst nach §4 |
| R-TEXT | Inhaltsänderung | Teaser vollständig, derselbe Punkt wie das Detail, Evidenzstärke und Tag passend · keine Heilaussage, Begriffsliste, Laiensprache · Sicherheitsrelevantes in `cautions`, Studien mit Originaltitel in `Refs.kt` · Leer- und Fehlertexte mit nächstem Schritt |
| R-AUDIO | Änderung in `src/cue/` oder Session-Abläufen | alle Lautstärkestufen ohne Klicks, Gong klingt aus · stumm: fremde Musik nicht geduckt; Pause gibt Fokus ab; Anruf nachvollziehbar · Vibration aus: auch kein Tick; Lautlos/„Bitte nicht stören“: Phasen-Haptik spürbar; kein veralteter `vibrate`-Overload (ab API 33 `VibrationAttributes`) · Bildschirm aus, Home-Taste: Ton läuft, nach Fortsetzen synchron |
| R-CODE | Code-Änderung | CODE-01 … -03, -05; KOMP-01; TYPO-02; MOTION-01; AUDIO-01 |
| R-STORE | Store-Update | Screenshots aus Release-Build, dunkel, Demo-Statusleiste, ohne echte Logbuchdaten (2–8, 320–3840 px, ≤ 2:1) · Feature-Grafik 1024 × 500 ohne Alpha, Icon 512 × 512 aus der Palette · Texte nach TEXT-11 |
| Vor Release | jedes Release (Design-Teil) | `check.sh` grün · R-A11Y, R-VISUAL, R-AUDIO auf echtem Gerät, bei Store-Update R-STORE · Changelog nach TEXT-11 · Backlog und Baselines aktuell. Signatur, APK-Prüfung, Store-Formulare: [SECURITY.md, Release-Checkliste](SECURITY.md#release-checkliste) |

## <a id="backlog"></a>14 Backlog

Nur Design- und Code-Qualität. Sicherheitsbefunde werden nicht öffentlich geführt (siehe [SECURITY.md](SECURITY.md#melden), Meldeweg). Reihenfolge = Priorität, behoben wird erst, wenn ein Punkt dran ist. Aufwand **S** < 1 h · **M** ≤ 1 Tag · **L** > 1 Tag. Orte relativ zu `src/`/`res/`.

### Block A – Automatisierung (zuerst)

| ID | Aufgabe | Aufwand |
|---|---|---|
| A-13 | Sicherheitsschritte nach SECURITY: `check-security.sh`, `verify-apk.sh`, `check-github.sh`, CI-Schritte und `release-verify.yml`; erst mit Veröffentlichung von SECURITY.md (sonst zeigt ein öffentlicher CI-Lauf offene Befunde) | M |
| A-12 | Referenzbilder (Viewport-Höhe, klein) im Repo und `verifyRoborazziDebug` in CI, damit auch rein optische Änderungen auffallen; Querformat und Tablet in die Matrix. Textprüfung steht bereits (§13.1). | S |
| A-10 | später: Compose-UI-Semantiktests (`isHeading`, `hasClickAction`, `enableAccessibilityChecks()`) | L |
| A-14 | Check `icon-size` (ICON-01, LAYOUT-04): `Icon(` ohne Größen-Token außerhalb von `AppIconButton`-Inhalten. Als regulärer Ausdruck nicht ohne Fehlalarm (Token in verschachtelten Argumenten, Slot-Inhalte) – erst mit Parser | S |

### P1 – sichtbare Fehler, blockierte Funktion, Inhaltsrisiko

| ID | Regel | Befund (Ort) | Aufwand |
|---|---|---|---|
| S-02 | PRIN-01 | Rest-Ordner `values-night/` (`colors.xml`, `window_background` identisch mit `values/colors.xml`): Datei löschen | S |
| S-03 | FARBE-03 | Phasen-Notiz auf Kreiszentrum 3,56:1, Kommentar `Color.kt:53` behauptet Fix; Zentrum #A82F80 (Notiz 4,90, Phase 4,55) | S |
| S-04 | A11Y-01 … -03 | ~~4 Switches ohne Namen; Stepper `clearAndSetSemantics` ohne Klick/Rolle, Wert nicht angesagt~~ (erledigt 2026-10-05: `ToggleRow`, Stepper-Knöpfe als `AppIconButton`, Wert als Live-Region); offen: Kartentitel ohne `heading()` (künftig `CardTitle`; `SectionHeader` erledigt 2026-10-05) | S |
| S-05 | MUSTER-05 | Logbuch leeren, Muster löschen, Stimme löschen ohne zweistufige Bestätigung; „Leeren“ als `TextButton` in eigener Zeile (`LogbookScreen.kt:107`) | S |
| S-06 | A11Y-02, -07, MUSTER-07 | Atemkreis: cd verdeckt Zeit-Texte, kein `onClickLabel`, „Weiter“ nur per Kreis, kein sichtbarer Fokus (`SessionScreen.kt:177-236`, `MeditationScreen.kt:367-374`) | M |
| S-07 | LAYOUT-03 | Nur noch ungeprüft: Querformat und Tablet (nicht in der Matrix, A-12). Bei fontScale 2,0 auf 360 dp stehen im Logbuch Dauer und Runden unter dem Namen mit Einzug (FlowRow + `Alignment.End`) – unschön, nicht falsch | S |
| S-08 | TEXT-09, -07 | Overclaims abbauen; `ContentRulesTest health-claims` findet: `physiological-sigh.shortDescription` „in Sekunden“, „Notbremse“; `Situations` ACUTE_STRESS „in Sekunden“; `strings:settings_sound_hint` „nachweislich“. Danach `health-claims` scharf schalten | M |
| S-09 | TEXT-08 | Sicherheitsrelevantes nur im Detail (Box, Zwerchfell, Resonanz; `BuiltInExercises.kt:44, 213, 382`) | S |
| S-10 | TEXT-07 | `BEST_EVIDENCE` beim physiologischen Seufzer trotz „nur indirekt belegt“; Lippenbremse (Meta-Analyse) ohne Tag | S |
| S-11 | MOTION-03 | Skalensprünge (`FractionContinuityTest`): Vorbereitung → Einatmen bzw. → Ausatmen (Feueratmung); `INHALE` → `INHALE_TOP_UP` (Seufzer); Feueratmung `INHALE` → `REST` und `REST` → `EXHALE`; Wim Hof Ausatmen → Ausatmen „ganz“ und Halten voll → Einatmen (neue Runde) | S |
| S-36 | AUDIO-01 | Dauerton blendet bei Pause, Stumm und Ende in 15 ms aus, nötig ≥ 60 ms (`ContinuousTonePlayer.GAIN_FADE_SECONDS` gilt fürs Ein- und Ausblenden; `ToneEnvelopeTest` meldet die Behebung) | S |
| S-12 | AUDIO-02 | Fokus trotz Stumm; kein Fokus-Listener; Reserve 300 vs. 500 ms; Cue-Thread blockiert 480 ms; Gong-Vorhören ohne Fokus | M |
| S-13 | AUDIO-04 | Atem-Session ohne Foreground-Service; Android 17 stummt Hintergrund-Audio | L |
| S-14 | CODE-05 | Enums per `ordinal` persistiert (`SettingsRepository.kt:120-202`), Migration auf `name` | M |

### P2 – Kontrast, Barrierefreiheit, Konsistenz

| ID | Regel | Befund (Ort) | Aufwand |
|---|---|---|---|
| S-15 | FARBE-03, -05 | Text < 4,5:1 auf Card (`ThemeContrastTest`): `TERTIARY` 4,16, Caution-TagChip 4,15, InfoChip 3,23, Wochentage 4,16; WeekRow-Punkte α 0,12 / 0,4 nur 1,40 / 1,85. Heute-Ring nicht in der Semantik; Familie nur über Randfarbe/Log-Punkt | S |
| S-16 | A11Y-01, -03, -04, -07 | `SelectChip` ohne Häkchen (Rolle Checkbox), `SegmentedChoiceRow`-Label nicht verknüpft; Mute ohne `Role.Switch`, kein `paneTitle`; Ende, Pause, Download-Status nicht angesagt; Fokusverlust bei Reset/Löschen | M |
| S-17 | MUSTER-01, -02, -06 | ~~„✓ Gespeichert“ disabled (3,00:1) ohne Live-Region~~ (erledigt 2026-10-05: Lesezeichen als `AppIconToggle`, gefüllt in `secondary`, `stateDescription` + Live-Region); „· angepasst“ als loses Geschwister (`SessionAdjustCard.kt:66`); ~~Reset mit fester Höhe 40 dp~~ (erledigt 2026-10-05, 48-dp-Tippfläche, gleiche Höhe wie Start); `SafetyDialog` ohne `verticalScroll` | S |
| S-18 | FARBE-04, -06, KOMP-01 | `CharacterChip`: „aktivierend“ in `WarnAmber` (ΔE 23,9 zu `FamilySympathetic`, steht neben Familie B), „ruhig“ in `EvidenceBest`, eigener Nachbau; drei Pinktöne ΔE < 25 (primary↔EvidenceCaution 19,9, primary↔SessionButtonPink 23,0, EvidenceCaution↔SessionButtonPink 18,1; Entscheidung: `SessionButtonPink` aus `error` oder `EvidenceCaution` Richtung Koralle); `outline`/`error`/`onError`/`primaryContainer` fehlen; `SecondaryContainer` 1,18 auf Card (Option #5A4890) | S |
| S-19 | AUDIO-01, -04, -05, MOTION-01, -02 | 528/396 Hz sind Solfeggio-Frequenzen; Tap-Tick ignoriert „Vibration“ und nutzt `USAGE_ALARM`; veralteter Vibrations-Overload; `HapticPlayer` in der Composable; Dauerton-Glide startet nach Fortsetzen bei f0; Tap-Flash fadet bei Animator-Skala 0 | M |
| S-20 | TEXT-01, -05 | Hartkodierte TalkBack-Strings (~~`DisclosureToggle.kt:36`, `ExpanderSection.kt:59`, `SessionScreen.kt:182`~~ erledigt 2026-10-05, `expandedStateText` mit `state_expanded`/`state_collapsed`; offen `LogbookScreen.kt:146`); Glossar im UI-File → `src/data/Glossary.kt`, Pflichtbegriffe ergänzen (RCT, Meta-Analyse, Hyperventilation, COPD …); `settings_safety_hint` nennt Übungen fest | S |
| S-21 | TEXT-06, -07 | Teaser `muster-des-tages.effect` 102 Z. (> 95); erste caution Wim Hof 97, Buteyko 109; ohne finites Verb `BuiltInExercises.kt:34, 445, 468`; ohne Evidenzhinweis `:106, 139, 374, 498`; Detail > 700: Cyclic Sighing 752, Physiological Sigh 709, Zwerchfell 748, Wim Hof 840 | M |
| S-22 | TEXT-02, -05, -10 | Jargon in Situationen und Familien-Labels (`BuiltInExercises.kt:275-295`, `strings.xml:17-24`); `phase_hold_full` = `phase_hold_empty`; Wording („Session“, Lädt/lädt, „Aktiv ✓“, `action_end`/`action_stop`, „~“/„ca.“, Apostroph `strings.xml:118`) | S |
| S-23 | TEXT-12 | „(Bowler 1998)“ und „(Vergleichsstudie 2025)“ in `4-7-8.effectDetail` ohne Quelle; `shetty2019`, `compare2025`, `nadiShodhana2024`, `nadiBhramari2023` unvollständig; `respeRate` (Patent, kein Studienzitat) und `fincham2023` ungenutzt; 8 deutsche Titel-Paraphrasen | M |
| S-24 | CODE-03, -05 | `remember` statt `rememberSaveable` (`MeditationScreen.kt:160`, `ExerciseDetailScreen.kt:82`); unauflösbare `exerciseId` → leerer Screen (`MainActivity.kt:365`); Logik hängt an „Tagesmuster“ (`SavedPatternsRepository.kt:42`) | S |
| S-25 | KOMP-01, LAYOUT-02 | Duplikate → geplante Komponenten (§5.1; ~~`ToggleRow`~~, ~~`PushHeader`~~ erledigt 2026-10-05); ~~Stepper außerhalb der Karte eingerückt~~ (erledigt 2026-10-05, bündig); About-Titel `headlineLarge` in primary | M |
| S-26 | KOMP-02, A11Y-01 | ~~`Card(Modifier.clickable)` 4×; `DailyPatternCard` mit zwei beschrifteten `TextButton`s~~ (erledigt 2026-10-05: alle Karten `Card(onClick)`, „Neu generieren“ als Refresh-Icon im `SectionHeader`, Speichern als Lesezeichen in der Kartenecke; Karte a54 fs110 115 → 67 dp); offen: `clickable` ohne Rolle 5× (Baseline `semantics-required`) | S |
| S-27 | A11Y-06 | Glyphen als Fokus-Stopps, `LogEntryCard` 4 Stopps, „3:05“; Stepper-cd „Einatmen (s): 4“; Stimmenliste ohne Namen | M |
| S-28 | TEXT-03 | Keine `localeFilters`/`localeConfig`; `Locale.getDefault()` neben `Locale.GERMAN`; `"%d:%02d"` doppelt | S |
| S-37 | LAYOUT-04, TYPO-02 | `NavigationBar` folgt der Schrift nicht: Höhe 80 und Indikator 64 × 32 sind in M3 1.3.1 fest, nur das Icon wäre steuerbar; eher ist das Label (`labelMedium`, 9,9 sp am A54) zu klein. Entscheidung offen: eigene Leiste oder größere Label-Rolle | M |
| S-39 | MUSTER-03, PRIN-03 | ~~Situationen-Tab: Lupe als Tab-Symbol, aber keine Suche; Liste rund 3,5 Bildschirmhöhen lang~~ (erledigt 2026-10-05: Befindens-Übersicht, Situationen als aufklappbare `SectionHeader`, Standard zu; Lupe bleibt, weil der Tab jetzt „Finden nach Befinden“ ist). Freitext-Suche (kuratierte Begriffe, ohne Tippfehler-Suche und ohne Embeddings) nur auf Zuruf | S |

### P3 – Hygiene und Tokens

| ID | Regel | Befund (Ort) | Aufwand |
|---|---|---|---|
| S-29 | LAYOUT-01, FARBE-02, MOTION-04 | Literale abbauen (~~Größen der Bedienelemente~~ erledigt 2026-10-05, `Sizes.kt`): 160 `.dp` (36 off-grid), 39 Alpha, 2 Shapes, Dauern inline; Tokens aus §2.2, §4 und `Motion.kt` anlegen; Kartenabstand 12/16/6+6; Transition-Zuweisung 4× dupliziert | L |
| S-30 | FARBE-01, -06, KOMP-03 | Ungenutzte Tokens; KDoc-Drift (`PauseFlash` nutzt `SessionButtonCyan`; Dauerton „Quarte“ statt Quinte, „150 ms“, „bewusst dezent“); Launcher #1A0B2E nicht aus der Palette; drei Trenner-Varianten | S |
| S-31 | CODE-01, -02 | `AppContainer.kt:8` → `ui.meditation`, `Chips.kt:17` → `ui.home`; `SettingsScreen` 27 Parameter, 13 Flows in der Root | M |
| S-32 | CODE-04, -06 | 13 Komponenten ohne `modifier`, `GlowText`-Reihenfolge, 0 Previews; Imports und voll qualifizierte Namen (A-03); 67 Kommentarzeilen mit ASCII-Schlusszeichen | M |
| S-33 | ICON-01, -02, TYPO-01 | Glyph „›“ als Text (~~„−“/„+“ im Stepper~~ erledigt 2026-10-05, Icons), zwei Bullet-Varianten, `Icons.Outlined.Info`; cd 5× per `semantics` am Button, 11 `null` ohne Marker; Gewichtswechsel in der NavigationBar | S |
| S-34 | MUSTER-03, -04 | `SessionAdjustCard` springt ohne `animateContentSize`; `log_empty` ohne Einladung, Fehlertexte ohne nächsten Schritt; `VoiceDownloadState.Failed` trägt freien Text (`VoiceModelManager.kt:158, 170`, `e.message`), die UI zeigt nur „Fehler – erneut versuchen“ → Grund-Enum (Integrität, Größe, Netz, Archiv) mit Texten in `strings.xml` | S |
| S-38 | LAYOUT-03, -07 | „Atmen → Ton“ fällt auf dem A54 (1,1) trotz kleinerer Segmente auf die Radioliste zurück; in der Stimmenliste springt die Anhören-Spalte je nach Zustand (Aktiv, Laden, Wählen); trailing `AppIconButton`s (Gong- und Stimmen-Vorhören, Löschen, Abbrechen, Muster löschen) stehen um ihren Touch-Rand eingerückt (am A54 ≈ 17 dp bis zum Glyph), Schalter und Stepper daneben bündig – Lösung ohne `offset` braucht eine gemeinsame Endspalte | S |

## <a id="quellen"></a>Quellen

RFC 2119 · [WCAG 2.2](https://www.w3.org/TR/WCAG22/) · [WCAG2ICT](https://www.w3.org/TR/wcag2ict-22/) · [Compose Accessibility](https://developer.android.com/develop/ui/compose/accessibility/key-steps) · [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) · [Edge-to-edge](https://developer.android.com/develop/ui/compose/system/setup-e2e) · [Android 17 Background-Audio](https://developer.android.com/about/versions/17/changes/bg-audio) · [Per-App-Sprache](https://developer.android.com/guide/topics/resources/app-languages) · [ktlint](https://pinterest.github.io/ktlint/) · [compose-rules](https://github.com/mrmans0n/compose-rules) · EU MDR 2017/745 (Erwägungsgrund 19, Art. 2 Nr. 12) · [Play Health Content](https://support.google.com/googleplay/android-developer/answer/14738291) · [Play-Listing](https://support.google.com/googleplay/android-developer/answer/9866151) · [F-Droid-Metadaten](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/) · interne Audits an `2a1cb20` (Kontrast nach WCAG 2.x, ΔE nach CIE76).
