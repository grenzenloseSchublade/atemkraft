# Atemkraft – Prozess

Regeln für Commit, Push, Release, Qualitätszyklus und die Arbeitsweise von Maintainer und Agenten. Kurzfassung in [CLAUDE.md](../CLAUDE.md); Design-, Text- und Code-Regeln im [Styleguide](STYLEGUIDE.md), Sicherheit und Release-Ablauf in [SECURITY.md](SECURITY.md). Regelformat wie dort: `ID | Stufe | Regel | Warum | Prüfung`.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| PROZ-01 | MUSS | Commit-Betreff `fix:`, `docs:`, `build:` oder `vX.Y.Z:` plus Zusammenfassung, deutsch mit echten Umlauten; nie `feat:`, kein Scope. Der Text erklärt das Warum, nicht nur das Was. | Ist-Stil; der CI-Job `conventions` lehnt anderes ab. | auto: `commit-msg` (Hook, CI) |
| PROZ-02 | MUSS | Ändert ein **Push** ein Token in `src/ui/theme/**` oder die Signatur einer öffentlichen `fun` in `src/ui/components/**`, ändert er irgendwo im selben Push auch `docs/STYLEGUIDE.md`; der Trailer `Guide: n/a – <Grund>` bleibt echten Ausnahmen (reine Umbenennung). Ein behobener Backlog-Punkt und seine Baseline-Einträge fallen im selben Commit weg. | Guide und Baselines driften sonst; je Commit geprüft erzwang die Regel nur Alibi-Trailer. | auto: `guide-sync` (range-weit), `baseline-stale` |
| PROZ-03 | MUSS | Vor jedem Push ist `scripts/check.sh` grün (oder `--fast` lokal und `check.sh` im CI); die Hooks sind aktiv (`git config core.hooksPath .githooks`, Wirkung: STYLEGUIDE §13.1). | Lokal = CI; ein falscher Betreff ist vor dem Push noch ohne Force-Push zu ändern. | auto: CI-Job `check` (erst nach dem Push), Hooks nur für Betreff und `guide-sync`; manuell: Checkliste eines Pushes |
| PROZ-04 | MUSS | **Push, Tag, Release und Store-Upload nur nach ausdrücklicher Bestätigung des Eigentümers.** Veröffentlicht wird als GitHub-Release, Ziel Google Play und F-Droid ([DECISIONS 2026-10-24](DECISIONS.md)). Alte Releases werden nie ohne Zusage gelöscht. | Ein Push ist öffentlich und ein Release unveränderlich; die Entscheidung liegt beim Eigentümer. | manuell (Agent) |
| PROZ-05 | MUSS | Der Versionsbump ist ein eigener Commit `vX.Y.Z: …` über `scripts/bump-version.sh X.Y.Z`: `versionCode` +1 und `versionName` in `app/build.gradle.kts`, `docs/release-notes/vX.Y.Z.md`, `fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt`, die Stand-Zeilen von STYLEGUIDE, SECURITY, PRIVACY, THIRD_PARTY_LICENSES und STORE. | Von Hand bleibt eine Datei zurück ([DECISIONS 2026-10-24](DECISIONS.md)). | auto: `version-sync` |
| PROZ-06 | MUSS | Release-Ablauf = [Release-Checkliste](SECURITY.md#release-checkliste): frischer Klon des Tags, JDK 17, `verify-apk`, Gerätetest, Assets und Release-Body; der Workflow `release-verify` baut den Tag nach. | Reproduzierbar, nachprüfbar. | auto: `release-verify`; manuell: Checkliste |
| PROZ-07 | MUSS | **Agenten-Arbeitsweise:** Analyse mit Befunden (Datei:Zeile; bei UI ein Entwurf) vor der Umsetzung, Arbeit im Worktree, nichts über den Auftrag hinaus; Layouts nach der Anatomie in STYLEGUIDE §4.5; Screenshots nicht ansehen, es gelten Gates und Token-Maße im Code (R-VISUAL); Tests nach CODE-07, Schlüssel nach SEC-BUILD-02. Vorrang: Eigentümer (auch seine Memory-Regeln), dann Guides, dann Code. | Agenten bauten Best-Practice-Layouts statt der Hausregeln. | manuell |
| PROZ-08 | MUSS | **Jeder Formatwechsel bekommt eine Fixture:** Ändert ein Commit, was die Sicherung schreibt (`PatternBackup`, auch ein neues optionales Feld), legt er eine Datei im neuen Format unter `app/src/test/resources/backup/` ab und liest sie im `PatternBackupTest` ein; ein neues Room-Schema bekommt im `DatabaseMigrationTest` einen Fall mit Daten aus der Vorversion. | Nur eine Datei im damaligen Format zeigt, dass alte Sicherungen lesbar bleiben. | manuell: R-CODE; auto halten `PatternBackupTest`/`DatabaseMigrationTest` nur die vorhandenen lesbar |
| PROZ-09 | MUSS | **Qualitätszyklus:** Vor jedem Release ein Kurzcheck – die neun Fragen der [Tabelle unten](#iso25010) je ein Satz, dazu R-PERF am Gerät; Ergebnis als Abschnitt „Kurzcheck vX.Y.Z“ im jüngsten Qualitätsbericht (`docs/recherche/`), Befunde ins Backlog. Vor jeder Major-Version (X.0.0) ein ausführlicher Bericht wie [2026-10-30](recherche/2026-10-30-iso25010.md), mit Vergleich zur Vorversion. | Ohne Vorher-Wert sieht niemand, ob ein Release schlechter wurde. | manuell: Checkliste eines Releases (Schritt 2) |

## Checkliste eines Pushes

1. `scripts/check.sh` grün (oder `--fast` und CI abwarten).
2. `scripts/check-style.sh --ci-range origin/main..HEAD` ohne Fehler (Betreffs, `guide-sync` über den ganzen Bereich).
3. Baselines nur gesunken; neue Ausnahmen als Marker `// Abweichung <ID>: <Grund>`.
4. Ändert der Push die Optik gewollt, liegen die neu aufgenommenen Referenzbilder im selben Commit (`rm -rf app/src/test/screenshots && scripts/build.sh recordRoborazziDebug`, STYLEGUIDE §13.1); sonst schlägt `verifyRoborazziDebug` in `check.sh` fehl. Schlägt es ohne gewollte Änderung fehl, ist die Abweichung ein Befund, kein Anlass zum Neuaufnehmen.
5. Bestätigung des Eigentümers für den Push.

## <a id="release"></a>Checkliste eines Releases

Einziger Einstieg in ein Release; die Schritte im Einzelnen stehen in der [Release-Checkliste](SECURITY.md#release-checkliste).

1. `scripts/bump-version.sh X.Y.Z`, Release-Notes und Changelog füllen (Laiensprache, TEXT-11), `scripts/check.sh` grün.
2. Qualitätszyklus (PROZ-09): Kurzcheck mit den neun Fragen, R-PERF und die Gerätelisten „Vor Release“ (STYLEGUIDE §13.4); vor einer Major-Version der ausführliche Bericht.
3. Commit `vX.Y.Z: …`; dann die Release-Checkliste ab Schritt 3 – Tag, Nachbau, `verify-apk`, Gerätetest, Release.
4. Bestätigung des Eigentümers für Push, Tag und Release; Store-Upload nach [STORE.md](STORE.md).

## <a id="iso25010"></a>Qualitätsmerkmale nach ISO/IEC 25010:2023

Welche Regeln welchem Merkmal dienen, womit es am stärksten geprüft wird, welche Frage der Kurzcheck vor jedem Release stellt (PROZ-09) und wo das Merkmal dünn ist. Jede aktive Regel steht mindestens in einer Zeile; „*“ heißt alle IDs der Familie. Bewertung und Belege: [Qualitätsbericht 2026-10-30](recherche/2026-10-30-iso25010.md).

| Merkmal | Regeln | stärkste Prüfung | Frage vor jedem Release | Lücke → Backlog |
|---|---|---|---|---|
| Funktionale Eignung | MUSTER-07, -09, -10, -12, MOTION-01, -03, AUDIO-03, CODE-05, TEXT-07, -12, -13, -16 | `PlaybackFlowTest`, `FreePatternTest`, `ExerciseFilterTest`, `ContentRulesTest` | Tut jede geänderte Funktion, was Regel und Spec sagen – auch die Sonderabläufe (Wim Hof, Viloma, offene Meditation)? | – |
| Leistungseffizienz | PERF-01, AUDIO-04, SEC-PERM-02, CODE-03, SEC-BUILD-03, ICON-01 | `perf-main`, R-PERF mit [Messwerten](recherche/2026-10-30-iso25010.md#messwerte) | Sind Kaltstart, Ruckler-Anteil und Speicher am A54 nicht schlechter als bei der letzten Messung? | **dünn:** S-49 |
| Kompatibilität | AUDIO-02, -05, PROZ-08, SEC-STORE-02, -03, SEC-PLAT-02, LAYOUT-02 | `PatternBackupTest` (Golden-Fixtures), `DatabaseMigrationTest` | Lesen sich alte Sicherungen und Datenbanken, und verhält sich der Ton mit anderen Apps, Anruf und Kopfhörer wie in R-AUDIO? | **dünn:** S-56 |
| Interaktionsfähigkeit | PRIN-*, FARBE-*, TYPO-*, LAYOUT-*, KOMP-*, MUSTER-01, -03, -04, -05, -08, -11, -13, ICON-*, MOTION-04, A11Y-*, TEXT-01, -02, -04, -05, -06, -10, -11, -14, -15 | Gates `WORTBRUCH`, `ABGESCHNITTEN`, `TIPPFLAECHE`, `ANATOMIE`; `ThemeContrastTest` | Sind alle Gates grün, und ist R-A11Y für jeden geänderten Screen durchgegangen? | S-44, A-10, S-54, S-60, S-61 |
| Zuverlässigkeit | CODE-09, CODE-03, CODE-07, SEC-STORE-02, -03, SEC-NET-04, PROZ-08, AUDIO-02, -04, MUSTER-04, -07 | `PlaybackFlowTest` (Fehler im Ablauf), `DatabaseMigrationTest`, `VoiceArchiveSafetyTest` | Übersteht jede Sitzung Fehler, Prozesstod und Fokusverlust ohne hängenden Service und ohne Datenverlust? | **dünn:** Fokusverlust, Prozesstod und Service-Ende nur über R-AUDIO am Gerät; automatisch nur der Fehler im Ablauf (CODE-09) |
| Security | SEC-PRIV-*, SEC-PERM-*, SEC-NET-*, SEC-PLAT-*, SEC-STORE-*, SEC-SUP-*, SEC-BUILD-*, SEC-DIST-*, SEC-REPO-*, SEC-LEGAL-03, PROZ-04 | `sec-*` ohne Baseline, `verify-apk`, `release-verify`, `check-github` | Sind `sec-*`, `verify-apk` und `check-github` grün, und steht jede neue Verbindung, Berechtigung und jedes neue Feld in SECURITY und PRIVACY? | S-43, S-52, S-53, A-22 |
| Wartbarkeit | CODE-01, -02, -04, -06, -07, KOMP-01, FARBE-06, LAYOUT-01, TYPO-01, MOTION-04, TEXT-01, PROZ-01, -02, -03, -05, -07, -09 | Schicht-Checks, `catalog-sync`, `theme-tokens`, `--self-test` | Ist die Baseline leer bzw. gesunken, und hat jede neue Komponente ihre Katalogzeile, jeder neue Check seine Fixtures? | S-41, S-55 |
| Flexibilität | LAYOUT-03, -05, -08, TEXT-03, CODE-01, SEC-BUILD-01, -04, -05, -06, SEC-DIST-*, PROZ-06 | `TabMatrixTest` (A54 und Tablet, hoch und quer), `release-verify` | Passt jede geänderte Seite quer, auf dem Tablet und bei Schrift 2,0, und baut der Tag bitgleich nach? | **dünn:** S-54, S-56, S-57, S-58 |
| Safety | MUSTER-06, -10, -13, TEXT-04, -07, -08, -16, MOTION-02, AUDIO-01, SEC-LEGAL-01, -02, -04 | `PatternEditorSafetyTest`, `ContentRulesTest` (`health-claims`, `intense-caution`), `DailyPatternGoldenTest` | Hat jede neue oder geänderte Übung ihre Hinweise in `cautions`, und verspricht kein Text etwas Gesundheitliches? | S-51 |
