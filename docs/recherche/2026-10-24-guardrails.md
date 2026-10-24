# Audit der Guardrails – Atemkraft, main @ ae3bab3 (v2.7.1), 2026-10-24

Geprüft (nur gelesen): `docs/STYLEGUIDE.md` (572 Zeilen, 80 Regeln: 72 MUSS, 2 DARF NICHT, 6 SOLL), `docs/SECURITY.md` (253 Zeilen, 36 Regeln: 31 MUSS, 3 DARF NICHT, 2 SOLL), `scripts/check_style.py` (68 Checks, davon 21 `sec-*`), `scripts/check.sh`, `.githooks/*`, `.github/workflows/*`, `config/*`, `.editorconfig`, `CONTRIBUTING.md`, `README.md`, Git-Log (200 Commits), Komponentenverzeichnis, Testverzeichnis. `scripts/check-style.sh` lief: 68 Checks, 0 Fehler, 14 Altlasten.

---

## 1 Was gut ist

1. **Jede Regel nennt ihre Prüfung** (`ID | Stufe | Regel | Warum | Prüfung`, STYLEGUIDE:11-12, SECURITY:7). Das ist selten und der eigentliche Wert des Systems: 47 Stil-Checks + 21 Sicherheits-Checks + Roborazzi-Gates + JUnit-Regeltests + `verify-apk` + `release-verify`, alle rückverfolgbar auf eine ID.
2. **Ein Prüf-Einstieg** (`scripts/check.sh`, check.sh:1-56; CI ruft genau dieselbe Datei, ci.yml:33). Lokal = CI. `--fast` für zwischendurch. Signierte APKs werden beiseitegelegt (check.sh:28-44) – durchdacht.
3. **Selbsttest der Checks** (`--self-test` mit `.pos/.neg`-Fixtures je Check, check_style.py:692-724), inkl. Gegenprobe, dass Stil-Marker keinen `sec-*`-Treffer verstecken (Z. 712-719). Ein Check, der nie anschlägt, beweist über das Positiv-Fixture, dass er funktioniert.
4. **Null-Toleranz dort, wo es zählt:** `sec-*` ohne Baseline und ohne Marker-Ausnahme (check_style.py:57, 85, 656-659); `health-claims` ohne Baseline (TEXT-09, content-baseline.txt:3). Ein Baseline-Eintrag für einen Sicherheits-Check ist selbst ein Fehler (Z. 656-659).
5. **Baseline darf nur sinken** (`baseline-stale`, check_style.py:654-662) – die Altlast schrumpft automatisch mit, keine stille Verwahrlosung. 14 Einträge, 2026-10-05 waren es Hunderte (S-29: 160 dp-Literale, 39 Alpha).
6. **Roborazzi-Gates als Testfehler** (`WORTBRUCH`, `ABGESCHNITTEN`, `TIPPFLAECHE`, `BESCHNITTEN`, `SCROLLT`, `KOMPAKT_FEHLT`, STYLEGUIDE:433) ersetzen das Ansehen von Bildern – genau die Nutzerregel „Maße im Code vergleichen, nicht am Bild“ (LAYOUT-04, Z. 208).
7. **Architektur ist maschinell gesichert:** `layer-import-*`, `domain-platform`, `ui-feature-import`, `ui-state-holder`, `screen-params` (check_style.py:560-580). Der Review vom 10/23-24 hat Zyklen aufgelöst und sofort den Check dafür geschrieben (Commits 1b8240c, 4f8225f, f7ede0b).
8. **Lieferkette ernsthaft:** Content-Filter je Repo, SHA-Pin für JitPack, `distributionSha256Sum`, Actions per 40-Zeichen-SHA, `persist-credentials: false`, Herkunftsnachweis mit Hash-Abgleich im Check (`sec-pinned-inputs`, check_style.py:405-433 prüft, dass jeder Hash wörtlich in SECURITY.md steht). `release-verify.yml` baut jeden Tag nach und vergleicht mit `apksigcopier`.
9. **Keystore außerhalb des Repos und Agent-Leseverbote** (`.claude/settings.json`, SEC-BUILD-02:142) – die Agenten-Realität ist in den Sicherheitsregeln angekommen.
10. **Hooks spiegeln den CI** (`.githooks/commit-msg`, `pre-push` → `--ci-range`, `core.hooksPath` ist gesetzt). Ein falscher Betreff fällt vor dem Push auf.
11. **Nutzerentscheidungen sind datiert und begründet** („Nutzer 2026-10-23“, Spalte Warum). Nichts ist willkürlich; man kann später nachlesen, warum.
12. **Begriffsliste §11.1** (Z. 382-403) als einzige Quelle für UI-Wörter – „Sitzung“ statt „Session“ usw.; wirkt bis in Store-Texte.
13. **Tests schlank** ist gelebt: 22 Dateien unter `app/src/test`, davon 7 Harness/Fakes; CODE-07 (Z. 417) schreibt die Nutzerentscheidung vom 10/22 fest.

---

## 2 Befunde

### HOCH

| # | Ort | Problem | Fix |
|---|---|---|---|
| H1 | STYLEGUIDE:92 (FARBE-04), :376 (TEXT-15), :261 (TimeDial) | **Widerspruch zu FARBE-07.** FARBE-04 sagt „Abschnittsköpfe … gehen reihum durch den Kopf-Ring (FARBE-07 …), außer im Atmen-Tab (Familienfarben)“; TEXT-15 sagt „Titel (`CardTitle`, Kopf reihum nach FARBE-07)“. FARBE-07 (Z. 95) sagt seit 2026-10-24 das Gegenteil: Köpfe weiß, Ring nur für `NavCard` in „Lernen“. TimeDial: „oben bündig unter dem Untertitel“ – Untertitel sind entfallen (Z. 197, 245, 249). | Drei Stellen auf den Stand von FARBE-07 bringen; Historie („bis 2026-10-24 …“) aus den Regelzellen in einen Entscheidungs-Log auslagern (siehe §3). |
| H2 | check_style.py:744-765 (`ci_range`), ci.yml:44-61, Git-Log | **`guide-sync` prüft je Commit statt je Push-Bereich.** Alle 7 Trailer der letzten 200 Commits lauten gleich: „Guide: n/a – Styleguide im Folgecommit … nachgezogen“ (95c3853, 7602ae2, 12e98e3, ab966b2, 5005540, 5c4f745, 983b0f0). Die Regel META-01 (Z. 21) wird formal erfüllt, inhaltlich umgangen; sie erzeugt nur Reibung bei einem sauberen Branch-Workflow (Code-Commits, dann ein Doku-Commit). | `ci_range`: Treffer nur, wenn im **gesamten Bereich** `docs/STYLEGUIDE.md` unberührt bleibt. Trailer `Guide: n/a` nur noch für echte Ausnahmen (z. B. reine Umbenennung). Aufwand S. META-01 entsprechend umformulieren („im selben Push“). |
| H3 | config/style-baseline.txt:14-16, STYLEGUIDE:364 (TEXT-03), :293 (KOMP-03), :467 (a11y-ratchet) | **Baseline als Dauer-Ausnahme statt Altlast.** `locale-config` (build.gradle.kts, Manifest) und `locale` (LogbookScreen 2) stehen seit Einführung in der Baseline; TEXT-03 fordert als MUSS `localeFilters`/`localeConfig` – nie umgesetzt, S-28 (Z. 552) offen seit 10/05. KOMP-03 nennt als „Ausnahme … Marken-Trenner (Baseline)“ – eine bewusste Ausnahme gehört in einen Marker, nicht in die Altlasten-Datei. `a11y-ratchet` friert `clearAndSetSemantics`/`indication = null` ein, die A11Y-07 (Z. 356) ausdrücklich erlaubt (eigener Fokusring). Die Baseline ist damit teils Schlupfloch: Sie verdeckt, dass eine MUSS-Regel offen ist, und sie verdeckt erlaubte Fälle, die ein Marker erklären würde. | (a) Jede Baseline-Zeile trägt eine S-ID wie `content-baseline.txt` (Format `check⇥datei⇥anzahl⇥S-nn`); Zeile ohne S-ID = Fehler. (b) Erlaubte Fälle → `// Abweichung <ID>: <Grund>`. (c) TEXT-03: entweder umsetzen (S-28, Aufwand S) oder Regel auf den Ist-Stand kürzen. |
| H4 | Repo-Wurzel, `.claude/` | **Keine Kurzfassung für Agenten.** Es gibt weder `CLAUDE.md` noch `AGENTS.md` (nur `.claude/settings.json` mit Deny-Regeln, `.claude/worktrees/`). Die Prozessregeln, die jeden Agenten-Lauf prägen – Push/Tag/Release nur nach Bestätigung, Release-Politik „nur lokal versionieren“, Analyse vor Umsetzung, Screenshots nicht ansehen, Versionsbump-Dateiliste, `check-style --ci-range` vor Push, Worktree-Arbeit – stehen nur im Gedächtnis des Assistenten. Ein Agent muss heute 572 + 253 Zeilen lesen, um die zehn Regeln zu finden, die er wirklich braucht. | `CLAUDE.md` (≤ 60 Zeilen, Entwurf in §4) mit Leitplanken, Prozess und Verweisen. |
| H5 | STYLEGUIDE:422, :434, :510 (A-13) vs SECURITY:5, :188, :236 | **Status doppelt gepflegt und auseinandergelaufen.** STYLEGUIDE §13-Einleitung: „offen sind APK-, GitHub- und Release-Prüfung (A-13)“; CI-Zeile: „APK- und Release-Prüfung laut SECURITY folgen mit A-13“. SECURITY sagt korrekt: `verify-apk` und `release-verify` seit 2026-10-24 aktiv (check.sh:50-53, release-verify.yml existiert). Offen ist nur noch `scripts/check-github.sh` (fehlt) und die Release-Body-Prüfung (SECURITY:231 „Offen“). | Ist-Stand nur in SECURITY; STYLEGUIDE verweist. A-13 auf den Rest kürzen. |

### MITTEL

| # | Ort | Problem | Fix |
|---|---|---|---|
| M1 | STYLEGUIDE:306-311, :92, :95, :228, :260 | **Regelzellen als Chroniken.** MUSTER-10 ist eine Zeile mit 15 009 Zeichen, MUSTER-09 7 559, MUSTER-07 5 638, BreathCurveEditor 6 069, FARBE-04 ≈ 2 300. Sie mischen Regel, Komponenten-Spezifikation, Entscheidungshistorie („bis 2026-10-23 …“, „Nachtrag 4/5/6/7“) und Begründung. Nicht prüfbar, nicht lesbar, nicht diffbar; ein Agent liest 15 000 Zeichen, um „Phasen 1–30 s, 2–12 Phasen“ zu finden. | Regelsatz ≤ ~400 Zeichen (was gilt), Spezifikation in `docs/spec/<feature>.md` oder in die KDoc der Composable (Editor, Suche, Session), Historie in einen Entscheidungs-Log. Siehe §3. |
| M2 | STYLEGUIDE:292 (KOMP-02) | Regel besteht aus drei benannten Ausnahmen (Zustandsakzent, Kopfzeile in der Karte, Kreationen-Karte). Jede neue Karte wird eine vierte. | Verallgemeinern: „Klickbare Karte `Card(onClick)`; Aktionen nur als Overlay in der obersten Zeile, höchstens zwei, bestimmen nie die Kartenhöhe; eine Karte, die einen Abschnitt vertritt, darf eine Kopfzeile tragen.“ |
| M3 | CONTRIBUTING.md:56, :58, :121 | **Veraltet:** „Screenshot-Test … auf drei Breiten und vier Schriftgrößen“ (Ist: 360 dp bei 1,0/2,0 plus Gerätehöhen-Matrix, STYLEGUIDE:433); „signierte `app-release.apk`“ (Ist: drei `app-*-release.apk`, SEC-BUILD-05); Releases-Absatz nennt eine APK (Ist: drei + AAB). | Zahlen raus, auf STYLEGUIDE §13.1 / SECURITY-Checkliste verweisen. Grundsatz: CONTRIBUTING beschreibt *wie*, nicht *was genau* – das steht einmal in den Guides. |
| M4 | STYLEGUIDE:433 | Bildliste veraltet: nennt `02_situationen_aufgeklappt`, `02_situationen_suche_treffer`; die Tests kennen `01_atmen_suche_treffer`, `01_atmen_anlass_offen`, `01_atmen_bereich`, `13_eigene_leer`, `15_lernen`, `16_pranayama_offen`, `17_programme` (nicht im Guide). | Liste streichen (die Tests sind die Quelle) oder Check `scene-sync` (Szenen-Namen aus `ScreenshotScenes.kt` ↔ Guide). |
| M5 | STYLEGUIDE §5.1 vs `ui/components/` | **Katalog ↔ Code:** öffentliche Composables/Helfer ohne Katalogzeile: `FillViewportColumn`, `HideSystemBars` (SystemBars.kt), `flushStart`/`flushEnd` (FlushEdge.kt), `accentBorder`/`dashedBorder` (CardBorder.kt), `sessionSummaryText`, `IconSwitch`, `SettingsAction`, `Labeled`, `ExerciseChips`. Alle in Prosa erwähnt, aber nicht als Eintrag – KOMP-01 („gibt es eine Komponente in §5.1 …“) greift für sie nicht. | Check `catalog-sync`: jeder nicht-private `fun` in `ui/components/` kommt als `` `Name` `` im STYLEGUIDE vor (S). |
| M6 | STYLEGUIDE §14, :186, :323 | **Backlog ist Chronik.** Komplett erledigt, aber stehen geblieben: S-02, S-04, S-05, S-06 (bis auf R-A11Y), S-08, S-09, S-10, S-13, S-14, S-26, S-39, S-40 – zwölf Einträge, teils mit mehreren ~~durchgestrichenen~~ Sätzen. „Geplant“-Tokens in §4.2 (`AccentBarWidth` – Balken ist entfernt, tot; `PauseFlashSize`, `ContentMaxWidth` seit 10/05) und §8 (`Disclosure`/`Reveal`, `AmbientShimmer`). A-13 halb erledigt (H5). | Erledigtes löschen (Git hat die Historie) oder in `docs/DECISIONS.md`. Backlog = nur Offenes, je Eintrag ein Satz + Aufwand. „Geplant“-Tokens als S-Einträge oder streichen. |
| M7 | config/content-baseline.txt:6-7, STYLEGUIDE:367, :545 | **Teaser-Regel MUSS mit Dauer-Baseline:** `muster-des-tages.effect` 102 Z. und `wim-hof.cautions[0]` 97 Z. (> 95) seit 10/08 eingefroren (S-21). Zwei Sätze kürzen kostet 10 Minuten; die Baseline ist teurer als die Behebung. | Texte kürzen, Baseline leeren, `content-baseline.txt` abschaffen (dann gilt wie bei `health-claims`: keine Baseline). |
| M8 | SECURITY:239 (Checkliste Schritt 2), Commit ae3bab3 | **Versionsbump ohne Check und ohne Skript.** Schritt 2 nennt ~8 Pflegestellen; v2.7.1 berührte 9 Dateien von Hand (`build.gradle.kts`, THIRD_PARTY, PRIVACY, SECURITY, STORE, STYLEGUIDE, RECHERCHE, release-notes, changelog). „Stand: v2.7.1“ steht in STYLEGUIDE:4, SECURITY:5 und :21 („2.7.x“), PRIVACY:5, THIRD_PARTY:5, STORE:3. Nichts prüft, dass sie übereinstimmen oder dass `changelogs/<versionCode>.txt` und `release-notes/vX.Y.Z.md` existieren. | Check `version-sync` (S) + `scripts/bump-version.sh` (M), siehe §5. |
| M9 | STYLEGUIDE:446-483 vs check_style.py | **Regex doppelt dokumentiert, driftet:** §13.3 wiederholt die Muster. Z. 455 `control-direct` nennt `…|IconButton|FilterChip|…`, der Code (check_style.py:512-513) enthält zusätzlich `ElevatedButton`, `FilledIconButton`, `OutlinedIconButton`, `InputChip`. Jede Check-Änderung muss zweimal geschrieben werden. | Spalte „Muster“ streichen; Tabelle = `Check | Regel | Scope | Stufe`. Die Fixtures sind die Spezifikation. Optional: Tabelle aus `CHECKS` generieren (`--list`). |
| M10 | STYLEGUIDE:370 (TEXT-09) ↔ SECURITY:179 (SEC-LEGAL-01); :372 (TEXT-11) ↔ SECURITY:161 (SEC-DIST-03); :21 (META-01) ↔ SECURITY:44-47 („im selben Commit“) | Dieselbe Regel zweimal mit zwei IDs. TEXT-09 und SEC-LEGAL-01 sind wortgleich bis auf den Scope. | Eine ID behält die Regel, die andere wird „→ siehe SEC-LEGAL-01“ (IDs bleiben, Regel steht einmal). |
| M11 | SECURITY:159 (SEC-DIST-01), CONTRIBUTING:118-122, Gedächtnis „Release-Politik 10/19“ | SEC-DIST-01 ist ein MUSS für etwas, das es nicht gibt (Play App Signing, Upload-Key) – ein Plan, kein Ist-Zustand, Prüfung „manuell: Store-Einreichung“. Die aktuelle Release-Politik (nicht veröffentlichen, nur Tag + Gerät; später Store nach Bestätigung) steht nirgends im Repo; CONTRIBUTING suggeriert laufende GitHub-Releases. | SEC-DIST-01 → Stufe „bei Store-Start“ oder nach STORE.md. Release-Politik in den Prozess-Teil (§3) schreiben: „Veröffentlichen nur auf ausdrücklichen Zuruf; Tag lokal.“ |
| M12 | STYLEGUIDE:495 (R-VISUAL) vs :208 (LAYOUT-04) vs Gedächtnis 10/22 | R-VISUAL verlangt „Bilder des betroffenen Screens auf A54 1,1 und 360 dp 2,0 ansehen“; LAYOUT-04 und die Nutzerregel sagen „Maße im Code vergleichen, nicht am Bild“. Für den Menschen am Gerät ist R-VISUAL richtig, für den Agenten falsch – das steht nirgends. | In der Checkliste trennen: „Mensch am Gerät“ vs. „Agent: Gates + Tokenvergleich“. In CLAUDE.md: „Screenshots nicht ansehen; Gates und Tokens vergleichen.“ |
| M13 | STYLEGUIDE:302 (MUSTER-03), Gedächtnis „Atem ist keine Zahl“ | Der Tab „Eigene“ zeigt „3 Tage in Folge · 45 min diese Woche“ – eine Serien-Kennzahl. Die ungeschriebene Regel „Atem ist keine Zahl / keine Datenschutz-KPI“ und SEC-LEGAL-02 (keine Auswertung mit gesundheitlicher Bewertung) stehen dazu in Spannung; nichts im Guide entscheidet, wo die Grenze liegt (Zählen ja, Bewerten nein?). | Regel schreiben (Vorschlag: „Die App zählt, sie bewertet nicht: keine Ziele, kein Soll, keine Vergleiche, keine Trends mit Urteil; Serien nur als nüchterne Angabe“) und als Leitplanke führen. |

### NIEDRIG

| # | Ort | Problem | Fix |
|---|---|---|---|
| N1 | SECURITY:194, :230, :251; STYLEGUIDE:510 | `scripts/check-github.sh` an vier Stellen referenziert, Datei fehlt (als „Soll“ gekennzeichnet). | Einmal als offen führen (A-13), sonst nur verweisen. Oder schreiben (M). |
| N2 | STYLEGUIDE:23 | Kurz-Checkliste nennt 20 IDs in einem Absatz – als Einstieg zu dicht. | Wird durch die Leitplanken (§3) ersetzt. |
| N3 | STYLEGUIDE §0 Z. 11 „Den Ist-Stand führt nur der Backlog“ | Tatsächlich steht Ist-Stand in jeder Regelzelle („seit 2026-10-24 …“). Die Regel über die Regeln wird nicht eingehalten. | Mit M1 lösen: Ist-Stand raus aus Regelzellen. |
| N4 | STYLEGUIDE:256 | `~~SelectChip~~ entfällt (2026-10-19)` – korrekt nach §0 („bleibt als entfällt“), aber für Komponenten unnötig: KOMP-01 verweist auf Vorhandenes. | Streichen; §0-Regel „ID nie neu vergeben“ auf Regel-IDs begrenzen. |
| N5 | check_style.py:234 `SCREEN_PARAMS_MAX = 8`, STYLEGUIDE:412 „ab 9 Parametern“, :476 „mehr als 8“ | Konsistent, aber dreimal formuliert. | Eine Formulierung („höchstens 8“). |
| N6 | ci.yml:44-61 | Job `conventions` läuft auch bei `push` auf `main` ohne PR; bei Force-Push oder neuem Branch `BASE = HEAD~1` – nur der letzte Commit wird geprüft. Harmlos, aber nicht dokumentiert. | Kommentar; oder `fetch-depth: 0` + `origin/main..HEAD`. |
| N7 | .githooks/pre-push:14 | Ohne Upstream `origin/main..HEAD` – gut; aber ohne `origin` (frischer Klon) bricht der Hook mit Git-Fehler ab statt mit Hinweis. | `git rev-parse --verify origin/main` prüfen, sonst Hinweis. |
| N8 | STYLEGUIDE:430 | Toolchain-Historie („compose-rules 0.6.x lief erst mit neuerem Gradle …“) in der Ablauf-Tabelle. | In Entscheidungs-Log oder streichen (steht im Commit). |

### Zu a) Vollständigkeit: Regel und Check je Thema

| Thema | Regel | Check | Bewertung |
|---|---|---|---|
| Wortbruch / Abschneiden | LAYOUT-03, -05 | `ScreenshotTest`-Gates, `weight-nofill`, `platform-override` | ✓ beides, stark |
| Tippflächen 48 dp | A11Y-05, LAYOUT-06 | `TIPPFLAECHE`, `touch-order`, `control-scale` | ✓ beides |
| `paneTitle` | A11Y-07 | `pane-title` | ✓ beides (Check ist nur eine Dateiexistenz-Prüfung, aber ausreichend) |
| Chips/Segmente ohne Häkchen | A11Y-03, FARBE-05, §5.1 | **keiner** (nur R-VISUAL) | Regel ohne Check → `no-check-icon` (S) |
| Köpfe weiß | FARBE-07 | **keiner** (R-VISUAL) | Regel ohne Check → `head-color` (S) |
| guide-sync-Trailer | META-01 | `guide-sync` | Check falsch geschnitten (H2) |
| 8-Parameter-Regel | CODE-02 | `screen-params` | ✓ beides |
| Zyklen / Schichten | CODE-01 | `layer-import-*`, `ui-feature-import`, `domain-platform` | ✓ beides, stark |
| health-claims | TEXT-09, SEC-LEGAL-01/-02 | `ContentRulesTest health-claims` ohne Baseline | ✓ beides, doppelt normiert (M10) |
| Versionsbump | Checkliste Schritt 2 (Prozess) | **keiner** | Prozess ohne Check (M8) |
| Reproduzierbarkeit | SEC-BUILD-04/-06 | `release-verify` (nur bei Tag), `apk-provenance` | ✓ beides; lokal nicht ohne Tag prüfbar |
| Icon-only / nichts schwebt | PRIN-03 | `no-fab-toast`, `control-direct` | ✓ teilweise (Icon-only selbst nicht prüfbar) |
| Material-Schrift ohne Skalierung | §3, TYPO-01 | `type-literal`, `m3-size-const` | ✓ beides |
| Keine Cloud / kein Auto-Backup | SEC-STORE-02 | `sec-backup` | ✓ beides |
| Teaser-Regel | TEXT-06, MUSTER-03 | `ContentRulesTest teaser` – mit Baseline | Check entwertet (M7) |
| number-unit | TEXT-10 | `typography number-unit` nur `strings.xml` + `src/data/` | Lücke: README, fastlane, PRIVACY, Release-Notes (S) |
| Commit-Präfix | CODE-08 | `commit-msg` Hook + CI | ✓ beides |
| Push/Tag/Release nur nach Bestätigung | **keine** | – | nur Gedächtnis (H4) |
| Release-Politik (nicht veröffentlichen) | **keine** | – | nur Gedächtnis (M11) |
| Tests schlank | CODE-07 SOLL | – | Regel ohne Check – akzeptabel (Review-Regel) |
| Screenshots nicht ansehen | LAYOUT-04 halb | Gates | widersprüchlich zu R-VISUAL (M12) |
| Analyse/Canvas vor Umsetzung | **keine** | – | nur Gedächtnis (H4) |
| Keystore außerhalb | SEC-BUILD-02 | `sec-secrets`, `sec-gitignore`, Deny-Regeln | ✓ beides |
| „Atem ist keine Zahl“ | **keine** | – | nur Gedächtnis, in Spannung zu MUSTER-03 (M13) |
| SEC-LEGAL-02 (keine Bewertung) | ✓ | manuell | Regel ohne Check – akzeptabel |
| Katalog ↔ Komponenten | KOMP-01 | **keiner** | Regel ohne Check (M5) |

### Zu d) Wirksamkeit

- **Nie anschlagend:** 50 von 68 Checks haben 0 Treffer und 0 Baseline. Das ist bei Regressions-Wächtern erwartbar und durch den Selbsttest gedeckt. Kein Befund – aber ein Argument, die Check-Tabelle nicht länger als nötig zu machen.
- **Ständig kostend:** `guide-sync` (7 Trailer, H2). `baseline-stale` schlägt bei jeder Behebung an – gewollt.
- **Entwertet:** `locale`, `locale-config`, `card-style` (AppNavigationBar), `a11y-ratchet` über Baseline (H3); `teaser` über content-baseline (M7).
- **Baseline als Schlupfloch:** Ja, in genau den vier Fällen oben – nicht als Mechanismus (sinken-nur ist richtig), sondern weil Einträge ohne S-ID und ohne Frist nicht zwischen „Altlast“ und „Ausnahme“ unterscheiden.
- **Handarbeit ohne Check:** Versionsbump (M8), Köpfe-Farbe, Häkchen, Katalog-Sync, number-unit in öffentlichen Texten, Szenen-Liste (M4), Release-Body (SECURITY:231 „Offen“).

---

## 3 Neuordnung in drei Ebenen

Grundsatz: **Leitplanken** sind wenige, stabile Sätze, die ein Agent in einer Minute liest. **Regeln** sind viele, prüfbar, mit ID. **Prozess** regelt Commit, Push, Release und die Arbeitsweise. Spezifikationen (wie der Editor genau gebaut ist) und Historie (warum es so wurde) sind weder Regel noch Leitplanke und bekommen eigene Orte.

### 3.1 Leitplanken (12, je ein Satz) – `CLAUDE.md` oben und STYLEGUIDE §1

1. **Nur dunkel, ruhig, minimal:** ein `darkColorScheme`, pro Screen eine gefüllte Neon-Fläche, Aktionen Icon-only mit `contentDescription`, nichts schwebt (kein FAB, Toast, Snackbar). → PRIN-01..03
2. **Köpfe weiß, Farbe trägt Bedeutung:** Abschnitts-, Expander- und Kartentitel in `onSurface`; Farbe nur bei Gruppenlabel, Kartenrand, Familie, Studienlage, Warnung, Atemphase. → FARBE-04, -07
3. **Kein Wort bricht um, nichts wird gekürzt, nichts beschnitten** – auf 360–412 dp bei fontScale 1,0–2,0; Tippflächen 48 dp als Layoutplatz. → LAYOUT-03, -05, A11Y-05
4. **Tokens statt Literale, Komponenten statt Nachbau:** Farbe, Alpha, Schrift, Maß, Form, Dauer aus `ui/theme/`; M3-Steuerelemente nur über die Wrapper in `ui/components/`; Auswahl ohne Häkchen. → FARBE-01/-02, TYPO-01, LAYOUT-01/-04/-06, KOMP-01/-04
5. **Material-Schrift und M3-Größen ohne eigenen Skalierfaktor.** → §3, §4.2
6. **Barrierefrei nach WCAG 2.2 AA:** Name, Rolle, Zustand, Überschrift, `paneTitle`, eine Live-Region je Zustandswechsel. → A11Y-01..07
7. **Evidenz-ehrlich, Laiensprache, „du“:** kein Heil-, Therapie- oder Präventionsversprechen in irgendeinem Text; Teaser ist ein ganzer Satz; jede Studie in `Refs.kt`. → TEXT-04..09, -12, SEC-LEGAL-01/-02
8. **Die App zählt, sie bewertet nicht:** keine Ziele, kein Soll, keine Auswertung mit gesundheitlichem Urteil. → SEC-LEGAL-02 + neu (M13)
9. **Offline und lokal:** kein Tracking, keine Cloud, kein Auto-Backup; Daten verlassen das Gerät nur per Export oder Gerät-zu-Gerät; einzige Netzverbindung der angetippte Stimm-Download. → SEC-PRIV-01..03, SEC-STORE-02
10. **Lieferkette gepinnt, ein Schlüssel außerhalb des Repos:** nur FLOSS, SHA-Pins, Actions per SHA; Keystore und Passwörter liest kein Agent. → SEC-SUP-*, SEC-BUILD-02, SEC-LEGAL-03
11. **Jede Regel hat eine ID und eine Prüfung;** Altlasten nur in der Baseline mit S-ID, Ausnahmen nur mit Marker. → META-01, §13.2
12. **Wenige, aussagekräftige Tests** für Daten, Sicherheit und die Gates – kein Test je Befund. → CODE-07

### 3.2 Regeln (IDs behalten)

**Ort:** `docs/STYLEGUIDE.md` (Design, Text, Code) und `docs/SECURITY.md` (Sicherheit, Recht) wie heute – aber jede Regelzelle ≤ ~400 Zeichen: *was gilt*, ein Halbsatz *warum*, die Prüfung. Alles Weitere wandert:

- **Spezifikation** (wie etwas genau gebaut ist) → `docs/spec/session.md` (aus MUSTER-07), `docs/spec/suche.md` (MUSTER-09), `docs/spec/muster-editor.md` (MUSTER-10 + BreathCurveEditor), `docs/spec/atmen-tab.md` (MUSTER-12/-13, FeelingBlock), oder in die KDoc der jeweiligen Composable. Der Katalog §5.1 bleibt, aber je Zeile Nutzen / Nicht für / A11y-Pflicht, keine Historie.
- **Historie und Nutzerentscheidungen** → `docs/DECISIONS.md` (Datum, Entscheidung, Grund, betroffene IDs). Dorthin alle „bis 2026-10-24 …“, „Nachtrag 4…7“, „Nutzer 2026-10-23: …“, erledigte Backlog-Einträge, „Geplant“-Listen. Git hat den Diff; die Datei hat den Grund.
- **Check-Tabelle §13.3** → `Check | Regel | Scope | Stufe`, ohne Regex (M9); idealerweise aus `check_style.py --list` erzeugt.

**Streichliste / Zusammenlegungen (Vorschlag):**

| Maßnahme | IDs | Begründung |
|---|---|---|
| Streichen (→ „entfällt → Ziel“) | TEXT-09 → SEC-LEGAL-01; TEXT-11 Disclaimer-Teil → SEC-DIST-03; MUSTER-02 → in MUSTER-01 („Rückmeldung und Reset im Element“) | doppelt (M10) |
| Zusammenlegen | LAYOUT-04 + LAYOUT-06 + KOMP-04 → **„Bedienelemente“** (Größe aus `Sizes`, nur in `components/`, `heightIn(min)`, kein `scale`, kein `IntrinsicSize`) | drei Regeln, ein Gegenstand |
| Zusammenlegen | FARBE-05 (Auswahl-Merkmale) → A11Y-03; FARBE-05 behält nur „Farbe nie alleiniger Träger“ | Überschneidung |
| Kürzen | FARBE-04 → zwei Sätze; die Zuordnung Farbe → Bedeutung steht schon in §2.1 (Spalte Bedeutung) | 2 300 Zeichen → 300 |
| Kürzen | FARBE-07, MUSTER-03, -07, -08, -09, -10, -11, -12, -13, TEXT-07, TEXT-14, TEXT-15, LAYOUT-07, LAYOUT-08, CODE-01, CODE-02 | Spezifikation/Historie auslagern (M1) |
| Verallgemeinern | KOMP-02 | drei Ausnahmen → eine Regel (M2) |
| Nach Prozess verschieben | META-01, CODE-08 | sind keine Design-/Code-Regeln |
| Stufe ändern | SEC-DIST-01 → „bei Store-Start“ (oder nach STORE.md) | kein Ist-Zustand (M11) |
| Neu | **TEXT-16** „Die App zählt, sie bewertet nicht“ (M13) | ungeschriebene Nutzerregel |
| Neu | **PROZ-01…05** (siehe 3.3) | ungeschriebene Nutzerregeln |
| Baseline | `style-baseline.txt` mit S-ID je Zeile; `content-baseline.txt` abschaffen (M7, H3) | Altlast ≠ Ausnahme |
| Backlog | nur Offenes: S-03, S-07, S-11, S-12, S-15 (Rest), S-16 (Rest), S-17 (Rest), S-18 (Rest), S-19 (Rest), S-21, S-22 (Rest), S-23 (respeRate), S-24 (Rest), S-25 (About-Titel), S-27 (Rest), S-28, S-29, S-30, S-32, S-33 (Rest), S-34, S-36, S-37, S-38, S-41, A-10, A-12, A-13 (Rest), A-14 | zwölf erledigte raus (M6) |

### 3.3 Prozess – `docs/PROCESS.md` (oder CONTRIBUTING-Abschnitt „Prozess“), Kurzform in `CLAUDE.md`

| ID | Regel | Prüfung |
|---|---|---|
| PROZ-01 | Commit-Betreff `fix:`/`docs:`/`build:`/`vX.Y.Z:`, deutsch, nie `feat:`; Text erklärt das Warum. (= CODE-08) | `commit-msg` Hook + CI |
| PROZ-02 | Ändert ein **Push** Theme-Tokens oder öffentliche Komponenten, ändert er auch den STYLEGUIDE; Baseline sinkt im selben Commit. (= META-01, range-weit) | `guide-sync`, `baseline-stale` |
| PROZ-03 | Vor jedem Push `scripts/check.sh` grün (oder `--fast` + CI); Hooks aktiv (`core.hooksPath .githooks`). | Hook, CI |
| PROZ-04 | **Push, Tag, Release, Store-Upload nur nach ausdrücklicher Bestätigung des Eigentümers.** Release-Politik (Stand 10/19): Versionen lokal taggen und auf dem Gerät prüfen; nicht veröffentlichen; alte Releases nie ohne Zusage löschen. | manuell (Agent) |
| PROZ-05 | Versionsbump ist ein eigener Commit `vX.Y.Z: …` und berührt: `app/build.gradle.kts` (versionCode +1, versionName), `docs/release-notes/vX.Y.Z.md`, `fastlane/…/changelogs/<versionCode>.txt`, Stand-Zeilen in STYLEGUIDE, SECURITY (2×), PRIVACY, THIRD_PARTY_LICENSES, STORE. | `version-sync` (neu), später `bump-version.sh` |
| PROZ-06 | Release-Ablauf = SECURITY Release-Checkliste (frischer Klon, JDK 17, `verify-apk`, Gerätetest). | `release-verify` |
| PROZ-07 | **Agenten-Arbeitsweise:** Analyse (Befunde mit Datei:Zeile, bei UI ein Entwurf/Canvas) vor Umsetzung; Arbeit im Worktree; keine Screenshots ansehen – Gates und Tokens vergleichen; wenige Tests; Keystore-Pfade nie lesen; Memory-Regeln des Eigentümers haben Vorrang vor Guides, Guides vor Code. | manuell |

---

## 4 Entwurf `CLAUDE.md` (≤ 60 Zeilen)

```markdown
# Atemkraft – Kurzfassung für Agenten

Android-App (Kotlin, Compose, M3), deutsch, Ein-Personen-Projekt. Verbindlich: docs/STYLEGUIDE.md (Design, Text, Code),
docs/SECURITY.md (Daten, Lieferkette, Release). Diese Datei ist die Kurzfassung; bei Widerspruch gilt der Guide,
bei Widerspruch zum Eigentümer gilt der Eigentümer.

## Leitplanken (unverrückbar)
1. Nur dunkel, ruhig, minimal: eine Neon-Fläche je Screen, Icon-only mit contentDescription, kein FAB/Toast/Snackbar.
2. Köpfe weiß; Farbe nur, wo sie etwas bedeutet (Familie, Studienlage, Warnung, Atemphase, eigenes Muster).
3. Kein Wort bricht um, nichts wird gekürzt oder beschnitten (360–412 dp, fontScale 1,0–2,0); Tippflächen 48 dp.
4. Tokens statt Literale (ui/theme/), Komponenten statt Nachbau (ui/components/); Auswahl ohne Häkchen.
5. Material-Schrift und M3-Größen ohne eigenen Skalierfaktor.
6. Barrierefrei: Name, Rolle, Zustand, Überschrift, paneTitle, eine Live-Region je Zustandswechsel.
7. Evidenz-ehrlich, Laiensprache, „du“: kein Heil-/Therapie-/Präventionsversprechen; Teaser = ganzer Satz ≤ 95 Zeichen.
8. Die App zählt, sie bewertet nicht: keine Ziele, kein Soll, keine gesundheitliche Auswertung.
9. Offline und lokal: kein Tracking, keine Cloud, kein Auto-Backup; nur Export/Import; Netz nur Stimm-Download per Tap.
10. Lieferkette gepinnt, nur FLOSS; Keystore und Passwörter liegen außerhalb des Repos – nie lesen.
11. Jede Regel hat ID und Prüfung; Altlasten nur in der Baseline mit S-ID, Ausnahmen nur mit `// Abweichung <ID>:`.
12. Wenige aussagekräftige Tests (Daten, Sicherheit, Gates) – kein Test je Befund.

## Arbeitsweise
- Erst Analyse mit Befunden (Datei:Zeile), bei UI ein Entwurf; dann umsetzen. Nichts gold-platen.
- Arbeit im Worktree. Vorhandene Komponente suchen (STYLEGUIDE §5.1), bevor etwas Neues entsteht.
- Screenshots nicht ansehen: Gates (WORTBRUCH, ABGESCHNITTEN, TIPPFLAECHE, SCROLLT) und Token-Maße im Code vergleichen.
- Texte: strings.xml (UI) oder src/data/ (Inhalte); Begriffe nach STYLEGUIDE §11.1; „…“-Anführungszeichen; Einheit mit
  geschütztem Leerzeichen; Sanskrit *kursiv*.
- Neue Verbindung, Berechtigung oder gespeichertes Feld: im selben Commit SECURITY.md + docs/PRIVACY.md nachziehen.

## Prüfen
- `scripts/check.sh` (alles wie CI) · `scripts/check.sh --fast` · `scripts/check-style.sh` (Sekunden) ·
  `scripts/check-style.sh --self-test` nach Check-Änderungen · nie `./gradlew` direkt, immer `scripts/build.sh`.
- Baseline nur senken (`config/style-baseline.txt`); sec-* und health-claims haben keine Baseline.

## Commit, Push, Release
- Betreff: `fix: …`, `docs: …`, `build: …`, Release `vX.Y.Z: …`. Nie `feat:`. Deutsch, echte Umlaute, Warum im Text.
- Ändert ein Push ui/theme-Tokens oder öffentliche Komponenten, ändert er auch docs/STYLEGUIDE.md (META-01).
- Vor Push: `scripts/check-style.sh --ci-range origin/main..HEAD`. Hooks: `git config core.hooksPath .githooks`.
- **Push, Tag, Release, Store-Upload nur nach ausdrücklicher Bestätigung.** Derzeit: Versionen lokal taggen, nicht
  veröffentlichen; alte Releases nie löschen.
- Versionsbump (eigener Commit `vX.Y.Z:`): app/build.gradle.kts, docs/release-notes/vX.Y.Z.md,
  fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt, Stand-Zeilen in STYLEGUIDE, SECURITY, PRIVACY,
  THIRD_PARTY_LICENSES.md, STORE.md. Release-Ablauf: SECURITY.md → Release-Checkliste.

## Orte
src/domain (reines Kotlin) · src/data (Inhalte, Room, DataStore) · src/cue (Ton, Haptik, Stimme) · src/playback (Abläufe,
Service) · src/ui/{theme,components,navigation,<feature>} · config/ (Baselines, Allowlists, Fixtures) · docs/spec/ (Details).
```

(Zeilen: 54.)

---

## 5 Neue und umgebaute Checks

| Check | Was | Regel | Aufwand |
|---|---|---|---|
| `guide-sync` (Umbau) | Range-weit statt je Commit: Fehler nur, wenn im ganzen Bereich Tokens/Komponenten geändert und STYLEGUIDE nirgends berührt | META-01/PROZ-02 | **S** |
| `baseline-ids` (Umbau) | `style-baseline.txt` vierte Spalte S-ID Pflicht; Zeile ohne ID = Fehler; `--update-baseline` übernimmt IDs | §13.2 | **S** |
| `version-sync` (neu) | `versionName` aus `build.gradle.kts` = „Stand: v…“ in STYLEGUIDE:4, SECURITY:5, PRIVACY:5, THIRD_PARTY:5, STORE:3; `changelogs/<versionCode>.txt` und `release-notes/v<versionName>.md` existieren; nur bei Betreff `vX.Y.Z:` scharf (sonst Warnung) | PROZ-05 | **S** |
| `head-color` (neu) | `SectionHeader(`, `CardTitle(`, `ExpanderSection(`, `ExpanderCard(`, `GroupLabel` ohne Familienkontext mit `color =`/`titleColor =` außerhalb `NavCard`/`GroupLabel` → Treffer | FARBE-07 | **S** |
| `no-check-icon` (neu) | in `ui/components/`: `SegmentedButton(`/`FilterChip(`/`SuggestionChip(` mit `icon =` oder `Icons.Filled.Check` → Treffer | A11Y-03, FARBE-05 | **S** |
| `catalog-sync` (neu) | jeder nicht-private `fun`/`val` auf oberster Ebene in `ui/components/*.kt` kommt als `` `Name` `` in STYLEGUIDE vor | KOMP-01 | **S** |
| `scene-sync` (neu) oder Liste streichen | Szenen-Namen in `ScreenshotScenes.kt` ↔ Guide; einfacher: Bildliste aus §13.1 entfernen | §13.1 | **XS/S** |
| `number-unit` (Erweiterung) | `ContentRulesTest typography number-unit` auch über README, fastlane, PRIVACY, Release-Notes (wie `health-claims`) | TEXT-10 | **S** |
| `teaser` (Umbau) | Baseline leeren (zwei Texte kürzen), `content-baseline.txt` entfernen | TEXT-06 | **S** |
| `rule-length` (neu, Doku-Lint) | Regelzeile in STYLEGUIDE/SECURITY > 600 Zeichen → Warnung; verhindert Rückfall in Chroniken | §0 | **S** |
| `check-table` (Umbau) | §13.3 aus `check_style.py --list` erzeugen oder beim Self-Test abgleichen (Name, Regel, Stufe) | M9 | **S** |
| `bump-version.sh` (neu) | Skript setzt versionCode/Name, legt Notes und Changelog an, ersetzt Stand-Zeilen; `version-sync` prüft | PROZ-05 | **M** |
| `check-github.sh` (neu) | wie SECURITY:230 beschrieben (`gh api`: PVR, Secret Scanning, Push Protection, Rulesets) | SEC-REPO-02 | **M** |
| `release-verify` (Erweiterung) | Release-Body: SHA-256 je APK, Fingerprint, health-claims, Changelog vorhanden, versionCode = Vorgänger + 1 | SEC-BUILD-05 | **M** |
| `icon-size` (A-14) | erst mit Parser, bleibt Backlog | ICON-01 | L |
| `locale` / TEXT-03 | entweder umsetzen (S-28: `localeFilters`, `locales_config.xml`, zwei `Locale`-Aufrufe) und Baseline leeren, oder Regel kürzen | TEXT-03 | **S** |

Reihenfolge-Empfehlung: H2 (`guide-sync`) und `version-sync` zuerst (sparen bei jedem Push/Release), dann H1/M1 (Dokument entschlacken, Widersprüche raus), dann `CLAUDE.md`, dann die kleinen Checks.
