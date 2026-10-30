# Atemkraft – Backlog

Offene Arbeit zu den Regeln aus [STYLEGUIDE.md](STYLEGUIDE.md), [SECURITY.md](SECURITY.md) und [PROCESS.md](PROCESS.md). Die IDs `S-n` und `A-n` nennen die Baseline (`config/style-baseline.txt`, STYLEGUIDE §13.2) und `// TODO(S-n)` im Code (CODE-06). Regelzellen der S-Zeilen prüft `rule-length` wie im Styleguide (≤ 400 Zeichen).

Nur Offenes; Erledigtes steht in [DECISIONS.md](DECISIONS.md). Sicherheitsbefunde werden nicht öffentlich geführt ([SECURITY.md](SECURITY.md#melden)). Reihenfolge = Priorität. Aufwand **S** < 1 h · **M** ≤ 1 Tag · **L** > 1 Tag. IDs werden nie neu vergeben; die nächste freie ist S-64 bzw. A-23.

### Qualität (Maßnahmen aus dem [Qualitätsbericht 2026-10-30](recherche/2026-10-30-iso25010.md), [ISO-Zuordnung](PROCESS.md#iso25010))

| ID | Regel | Befund und Maßnahme | Aufwand |
|---|---|---|---|
| S-43 | SEC-REPO-02 | **Nur der Eigentümer:** Rulesets anlegen – `main` ohne Force-Push und Löschen, Tags `refs/tags/v*` ohne Update und Löschen; danach `scripts/check-github.sh` grün (heute 4× „fehlt“). | S |
| S-44 | A11Y-01 … -07, FARBE-05 | **Nur der Eigentümer (Gerät):** R-A11Y am A54 (TalkBack, Switch Access, D-Pad, Accessibility Scanner, Kontrast) mit Protokoll `docs/recherche/<datum>-a11y.md` (Screen × Prüfpunkt × ok/Befund); die Release-Checkliste verweist darauf. | M |
| S-49 | PERF-01 | Rest der Referenzmessung (R-PERF): `meminfo` ohne installierte Stimme (Anteil des Sprachmodells), `gfxinfo` in der laufenden Sitzung, Editor über Blatt „Muster des Tages“ und über „Eigene“ → „+“ nach `closeThen`, Vorladen und Baseline-Profil (2026-10-30) nachmessen ([spec/muster-editor.md](spec/muster-editor.md), „Öffnen ohne Ruckeln“). | S |
| S-51 | MUSTER-06, TEXT-08 | Abschnitt „Gesundheitliche Sicherheit“: Gefahr (Ohnmacht, Wasser, Hypokapnie, Pressen, Schwangerschaft) → Maßnahme → Prüfung; Herleitung der Intensitäts-Schwellen (Halten voll > 10 s, leer > 8 s, > 20 Atemzüge/min) in DECISIONS. | M |
| S-52 | SEC-BUILD-04 | SSH-Signatur mindestens für Release-Tags; `git tag -v` in `verify-release-body`. | S |
| S-53 | SEC-SUP-02 | Dependency Verification auf Gradle-Plugins ausweiten, langfristig auf alle Gruppen. | M |
| S-54 | LAYOUT-03, -05 | Querformat bei Schrift 2,0: Mindesthöhe des Inhalts (z. B. ≥ 120 dp, Detailseite heute 94 dp) im `ScreenshotTest` prüfen; Kopf und Leiste mitscrollen lassen oder einklappen. | M |
| S-55 | CODE-02 | Lange Composables aufteilen (`SettingsScreen` nach Karten, `ExerciseDetailScreen` in Abschnitte), dazu Check `composable-length` (> 120 Zeilen) mit Baseline; überschneidet sich mit S-41. | M |
| S-56 | – (Kompatibilität) | minSdk 26–32 absichern: kritische Robolectric-Tests mit `@Config(sdk = [26, 35])`, einmal ein Smoke-Test im Emulator – oder minSdk bewusst anheben. | M |
| S-57 | PROZ-04, SEC-BUILD-04 | **Push und Tag nur nach Bestätigung:** Tag v2.10.0 mit grünem `release-verify`, F-Droid-Probebau im fdroidserver-Container. | S/M |
| S-58 | LAYOUT-03, -05 | Splitscreen (320 × 640) und Tablet bei Schrift 2,0 in die `TestMatrix` (nur Gates); R-VISUAL bekommt einen Splitscreen-Schritt. | S |
| S-60 | – (Interaktion) | Laut-Denken-Test mit 3–5 Personen: erste Übung, eigenes Muster, Logbuch finden. | M |
| S-61 | MUSTER-05 | Zweistufiges Löschen (`rememberArmedConfirm`) im Compose-Test mit Uhr +4 s und mit Bedienungshilfe. | S |

Kleinere Punkte ohne ID (Gong-Import auf wählbare Werte, `withinCreationRules` im `DailyPatternGoldenTest`, Fall für den `corruptionHandler` u. a.) führt der Bericht unter „Niedrig, bei Gelegenheit“.

### Automatisierung

| ID | Aufgabe | Aufwand |
|---|---|---|
| A-10 | vorgezogen (Qualitätsbericht): Compose-Semantiktests – `enableAccessibilityChecks()` im `ScreenshotHarness`, Live-Regionen je Szene zählen (A11Y-04), Glyphen als eigene Fokus-Stopps melden (A11Y-06) | M |
| A-22 | monatlicher OSV-Abgleich der nativen Komponenten (sherpa-onnx, onnxruntime, espeak-ng) aus einer kleinen SBOM-Liste (SEC-SUP-03) | M |
| A-14 | Check `icon-size` (ICON-01, LAYOUT-04): `Icon(` ohne Größen-Token außerhalb von `AppIconButton`; regulär nicht ohne Fehlalarm, erst mit Parser | S |

### Design und Code

| ID | Regel | Befund (Ort) | Aufwand |
|---|---|---|---|
| S-23 | TEXT-12 | `respeRate` (Patent, kein Studienzitat) wird genutzt: „Mehr zum Dauerton“ in den Einstellungen und `settings_sound_hint` nennen RESPeRATE. Entscheidung offen: als Quelle des Prinzips behalten oder samt Satz streichen | S |
| S-41 | CODE-01, -02 | Bewusst offen (Architektur-Review): gemeinsamer Ablauf-Kern für `BreathSessionController` und `MeditationController` (erst mit einem dritten Ablauf); lange Composables aufteilen (S-55, dazu `SessionScreen.ActiveContent`, `TimeDial`, `HomeScreen`, `BreathCurveEditor`); `SubcomposeLayout` in `SectionHeader` → `Layout` mit Intrinsics; `java.text.Normalizer` und `javaClass` als expect/actual für KMP | L |
| S-62 | MUSTER-09 | nur auf Zuruf: Embeddings für die Befindens-Suche (erst bei Hunderten freier Inhalte sinnvoll) | L |
| S-63 | FARBE-02 | Freie Alpha-Konstanten außerhalb von `ui/theme/` (`TAB_DIVIDER_ALPHA` in `AppNavigationBar`, `AURA_LAYER` in `VayuFigure`, Glow-Alpha in `BreathingCircle`) als Tokens in `Color.kt` und §2.2 führen; `alpha-literal` findet benannte Konstanten nicht. | S |
| S-42 | STYLEGUIDE §4.5 | Rest: `LogEntryRow` (Logbuch) ist eine Listenzeile (Familienpunkt vorn, `titleSmall`, Dauer-Spalte) ohne passende Slots in `CardHeader`; erst mit einem Zeilen-Baustein überführen | S |
