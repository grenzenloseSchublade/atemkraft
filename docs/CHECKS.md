# Atemkraft – Prüfwerkzeuge

Wie die automatischen Prüfungen arbeiten und welcher Check welcher Regel dient. Die Regeln und ihre Prüfspalten stehen im [Styleguide](STYLEGUIDE.md) (Kurzfassung der Werkzeuge in §13, manuelle Checklisten R-… in §13.4), in [SECURITY.md](SECURITY.md#automatische-pruefung) und [PROCESS.md](PROCESS.md). Diese Datei ist Nachschlagewerk, kein Regelwerk: Hier entsteht keine neue Pflicht.

## <a id="werkzeuge"></a>Werkzeuge im Einzelnen

| Werkzeug | Datei | Aufgabe |
|---|---|---|
| `scripts/check.sh` | – | **einziger Einstieg**, lokal und im CI: Stil- und Sicherheits-Checks (`--self-test`, dann Prüfung) → `scripts/build.sh ktlintCheck verifyRoborazziDebug lintDebug assembleRelease -PunsignedRelease=true` (`verifyRoborazziDebug` = `testDebugUnitTest` mit Vergleich der Referenzbilder) → `scripts/verify-apk.sh --allow-unsigned` auf die Prüf-APKs. Der Release-Build bleibt unsigniert; signierte `app-*-release.apk` legt das Skript beiseite und stellt sie wieder hin. `--fast` ohne `lintDebug`, Release-Build und `verify-apk`. Nie `./gradlew` direkt. Git-Hooks in `.githooks/` (`git config core.hooksPath .githooks`): `commit-msg` → `check-style.sh --commit-msg`, `pre-push` → `check-style.sh --ci-range` über die neuen Commits. |
| `scripts/check-style.sh` | `scripts/check_style.py`, `config/style-baseline.txt`, `config/style-fixtures/<check>.{pos,neg}` | Checks der [Tabelle unten](#checks) in Python 3: zeilenweise oder (ml) über die ganze Datei; Sonder-Checks als Funktionen. Ignoriert Kommentarzeilen (außer `todo-unlinked`) und Zeilen mit Marker `// Abweichung <ID>:` / `// dekorativ:`. `--self-test` prüft jeden Check gegen seine Fixtures (Sonder-Checks über ihre Fixture-Funktion oder einen Dateibaum aus `=== pfad`-Abschnitten in einem leeren Repo); ein Check ohne Fixture ist ein Fehler, `--list` druckt die [Check-Tabelle](#checks), `--update-check-table` schreibt sie in diese Datei, `--update-baseline` schreibt den Ist-Stand mit den vorhandenen S-IDs, `--ci-range A..B` prüft `commit-msg` je Commit und `guide-sync` über den ganzen Bereich. Die `sec-*`-Checks laufen im selben Skript ohne Baseline und ohne Marker-Ausnahme. |
| `scripts/bump-version.sh` | `app/build.gradle.kts`, `docs/release-notes/`, `fastlane/…/changelogs/`, Stand-Zeilen | Versionsbump an einer Stelle (PROZ-05); `version-sync` prüft, dass alles übereinstimmt. |
| ktlint 1.8.0 + compose-rules 0.6.8 | `.editorconfig`, Plugin `org.jlleitschuh.gradle.ktlint` 14.2.0, `.git-blame-ignore-revs` | Format, Compose-Regeln; in `*Screen.kt` ist `modifier-missing-check` aus. Ohne Baseline: Jeder Fund ist ein Fehler. |
| Android Lint | `lint {}`: `abortOnError`, `warningsAsErrors` (aus: `GradleDependency`, `AndroidGradlePluginVersion`, `NewerVersionAvailable`, `OldTargetApi`, `ChromeOsAbiSupport`); `app/lint.xml` | Manifest, Ressourcen, Compose-Lint. |
| JUnit-Regeltests | `ContentRulesTest`; `config/health-claims.{txt,allow}` | Ehrliche Texte ([unten](#weitere)), ohne Baseline. `config/` ist Eingabe der Test-Tasks. |
| Screenshot-Gates | `ScreenshotTest`, `DeviceFitTest` und `TabMatrixTest` (Robolectric + Roborazzi) auf `ScreenshotHarness` (Aufnahme, `TabFrame`, Gates aus `ControlFindings`, `TextClipFindings`, `AnatomyFindings`); `ui/TestMatrix.kt`; SDK 35 | **Breiten-Matrix** (`ScreenshotTest`): 360 dp bei fontScale 1,0 und 2,0; **Gerätehöhen-Matrix** (`DeviceFitTest`): 360 × 640 bei 1,3, A54 bei 1,1 und 1,3 (Editor ohne Tab-Leiste, Meditations-Tab mit); **Tab-Matrix** (`TabMatrixTest`, `TestMatrix.TAB_CONFIGS`): A54 hoch und quer bei 1,1, Tablet 800 × 1280 hoch und quer bei 1,0 – die vier Tab-Startseiten, laufende Atemübung und Meditation. Tabs mit den Leisten der App (`TabFrame`). Läuft in `testDebugUnitTest` und **schlägt bei jedem Befund fehl**: Text (`WORTBRUCH`, `GETRENNT`, `ABGESCHNITTEN`, `CHIP_UMBRUCH`, `ZUGESCHNITTEN`), Bedienelemente (`TIPPFLAECHE`, `OHNE_KLICK`, `BESCHNITTEN`, `AUSSERHALB`), Layout-Grammatik (`ANATOMIE`, STYLEGUIDE §4.5), `SCROLLT` (LAYOUT-08), `KOMPAKT_FEHLT` (MUSTER-10). Die Szenen stehen in den Tests selbst. Bilder: `scripts/build.sh recordRoborazziDebug` → `app/build/outputs/roborazzi/<gerät>/fs<n>/`, je Bild eine `.tsv` mit Befunden und Maßen. **Referenzbilder** (A-12): die 24 Bilder der Tab-Matrix liegen in halber Auflösung im Repo unter `app/src/test/screenshots/<gerät>/fs<n>/` (rund 1,7 MB); `check.sh` vergleicht sie pixelgenau, jede optische Abweichung und jedes fehlende Bild ist ein Fehler (Vergleichsbilder `*_compare.png` unter `app/build/outputs/roborazzi/`, im CI als Artefakt). Nur Grundzustände in Gerätegröße, weil die hohen Listenbilder rund 30 MB wären und sich mit jedem Inhaltstext änderten. **Gewollte Änderung:** `rm -rf app/src/test/screenshots && scripts/build.sh recordRoborazziDebug` nimmt alle Referenzbilder neu auf (das Löschen räumt umbenannte Szenen ab); die neuen Bilder gehören in denselben Commit wie die UI-Änderung. |
| CI | `.github/workflows/ci.yml`, `.github/dependabot.yml` | Push auf `main` und PR, `permissions: contents: read`, Actions per SHA, `persist-credentials: false`. Job `check`: `check.sh` (bei Fehler `.tsv`, Testbericht und Lint-Bericht als Artefakt). Job `conventions`: `check-style.sh --ci-range` über alle Commits des Pushes/PRs. Dependabot monatlich, Betreff „build: …“; Gradle gruppiert, Major-Sprünge von AGP und Gradle von Hand. |

## <a id="checks"></a>Checks

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
| `control-scale` | LAYOUT-04 | src/ui/*.kt, src/MainActivity.kt ohne src/ui/theme/* | MUSS |
| `touch-order` | LAYOUT-04 | src/ui/*.kt | MUSS |
| `control-direct` | LAYOUT-04 | src/ui/*.kt, src/MainActivity.kt ohne src/ui/components/* | MUSS |
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
| `sheet-direct` | MUSTER-11 | src/ui/*.kt ohne src/ui/components/AppBottomSheet.kt | MUSS |
| `catalog-sync` | KOMP-01 | src/ui/components/*.kt | MUSS |
| `rule-length` | §0 | docs/STYLEGUIDE.md, docs/SECURITY.md, docs/PROCESS.md, docs/BACKLOG.md | MUSS |
| `check-table` | §13.3 | `docs/CHECKS.md` | MUSS |
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
| `perf-main` | PERF-01 | java/*.kt | SOLL |
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

## <a id="weitere"></a>Prüfungen außerhalb von `check_style.py`

| Prüfung | Was | Regel |
|---|---|---|
| `ContentRulesTest` | `teaser` (Fälle `teaser`, `first-caution`, `lore-teaser`: ganzer Satz ≤ 95 Zeichen, Detail beginnt nicht mit dem Teaser, Lehre zuschreibend), `health-claims` (App-Inhalte, `strings.xml`, README, PRIVACY, Release-Notes, `fastlane/**` je Zeile; Disclaimer wörtlich), `store-lengths`, `typography` (`decimal-comma`, `quotes`, `dot-separator`, `number-unit` in Inhalten, `strings.xml`, Quellen und öffentlichen Texten, `phase-names`), `cross-references`, `intense-caution` (eingebaut und nach den Phasen intensiv ⇒ `caution`, Hinweise nennen Wasser); alle ohne Baseline | TEXT-06, -10, -11, -13, MUSTER-03, -06, SEC-LEGAL-01/-02 |
| `ScreenshotTest`, `DeviceFitTest` | Gates aus der [Werkzeug-Tabelle](#werkzeuge); dazu `chipLayout`/`chipRows`/`filterRow` (MUSTER-09, -12), `RING` (LAYOUT-08), `editorFit` (MUSTER-10) | LAYOUT-03, -04, -05, -07, -08, A11Y-02, -05, §4.5 |
| `TabNavigationTest` | Re-Tap auf den markierten Tab führt zur Tab-Startseite; markiert bleibt der Tab des Stapels; Leiste fest nur auf den Startseiten | MUSTER-08 |
| `DatabaseMigrationTest`, `PatternBackupTest`, `DailyPatternGoldenTest`, `FreePatternTest`, `PatternEditorSafetyTest`, `PlaybackFlowTest` | Room-Migrationen (auch Schema 5 mit Daten); Export/Import-Rundlauf und alte Exportdateien als Golden-Fixtures (auch mit BOM); Muster des Tages unverändert (Hashes), in den Erzeugungsregeln und nie intensiv, auch als freie Folge, mit der „Selbst gestalten“ den Editor öffnet (`toFreePattern`); Grenzen der freien Folge; Sicherheitsdialog je Übung wie in der App (Editor und Detailseite, mit und ohne „immer zeigen“); Abläufe beider Sitzungsarten (offene Meditation, Wim Hof mit offenem Halten, abgezogener Kopfhörer, Fehler im Ablauf), „Letzte 7 Tage“ an einem Montag | SEC-STORE-01 … -03, PROZ-08, MUSTER-06, -07, -10, CODE-09, TEXT-16 |
| `VoiceArchiveSafetyTest` | Stimmen-Archiv: falscher Hash, Übergröße, `../`, Symlink, zu viele Einträge, zu viel Inhalt – abgelehnt, Staging-Ordner gelöscht, nichts daneben | SEC-NET-03, -04 |
| `ThemeContrastTest` | Kontrastwerte der Tabellen STYLEGUIDE §2.1 und §2.2 aus den Tokens nachgerechnet: Text ≥ 4,5, großer Text und Grafik ≥ 3, Abweichung zur Tabelle ≤ 0,05 | FARBE-03 |
| `ExerciseFilterTest` | jeder Filter trifft mindestens 3 und nicht alle Übungen, „Bis 3 min“ wie am Startknopf, UND, leere Familien fallen weg | MUSTER-12 |
| ktlint / Lint / Tests | `ktlintCheck` (+ compose-rules), `lintDebug`, `testDebugUnitTest` | TEXT-01, CODE-02 … -04, -06, -07 |
| `verify-apk`, `release-verify` (mit `verify-release-body`), `check-github`, `sec-*` | [SECURITY.md, Automatische Prüfung](SECURITY.md#automatische-pruefung) | SEC-* |
