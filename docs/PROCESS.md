# Atemkraft – Prozess

Regeln für Commit, Push, Release und die Arbeitsweise von Maintainer und Agenten. Kurzfassung in [CLAUDE.md](../CLAUDE.md); Design-, Text- und Code-Regeln im [Styleguide](STYLEGUIDE.md), Sicherheit und Release-Ablauf in [SECURITY.md](SECURITY.md). Regelformat wie dort: `ID | Stufe | Regel | Warum | Prüfung`.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| PROZ-01 | MUSS | Commit-Betreff `fix:`, `docs:`, `build:` oder `vX.Y.Z:` plus Zusammenfassung, deutsch mit echten Umlauten; nie `feat:`, kein Scope. Der Text erklärt das Warum, nicht nur das Was. (früher CODE-08) | Ist-Stil; der CI-Job `conventions` lehnt anderes ab. | auto: `commit-msg` (Hook, CI) |
| PROZ-02 | MUSS | Ändert ein **Push** ein Token in `src/ui/theme/**` oder die Signatur einer öffentlichen `fun` in `src/ui/components/**`, ändert er irgendwo im selben Push auch `docs/STYLEGUIDE.md`; der Trailer `Guide: n/a – <Grund>` bleibt echten Ausnahmen (reine Umbenennung). Ein behobener Backlog-Punkt und seine Baseline-Einträge fallen im selben Commit weg. (früher META-01) | Guide und Baselines driften sonst; je Commit geprüft erzwang die Regel nur Alibi-Trailer. | auto: `guide-sync` (range-weit), `baseline-stale` |
| PROZ-03 | MUSS | Vor jedem Push ist `scripts/check.sh` grün (oder `--fast` lokal und `check.sh` im CI); die Hooks sind aktiv (`git config core.hooksPath .githooks`): `commit-msg` prüft den Betreff, `pre-push` Betreffs und `guide-sync` über die neuen Commits. | Lokal = CI; ein falscher Betreff ist vor dem Push noch ohne Force-Push zu ändern. | Hook, CI |
| PROZ-04 | MUSS | **Push, Tag, Release und Store-Upload nur nach ausdrücklicher Bestätigung des Eigentümers.** Seit 2026-10-24 wird wieder veröffentlicht (GitHub-Release je Version, v2.6.0 ist öffentlich; Ziel Google Play und F-Droid). Alte Releases werden nie ohne Zusage gelöscht. | Ein Push ist öffentlich und ein Release unveränderlich; die Entscheidung liegt beim Eigentümer. | manuell (Agent) |
| PROZ-05 | MUSS | Der Versionsbump ist ein eigener Commit `vX.Y.Z: …` über `scripts/bump-version.sh X.Y.Z`: `versionCode` +1 und `versionName` in `app/build.gradle.kts`, `docs/release-notes/vX.Y.Z.md`, `fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt`, die Stand-Zeilen von STYLEGUIDE, SECURITY, PRIVACY, THIRD_PARTY_LICENSES und STORE. | v2.7.1 berührte neun Dateien von Hand; nichts prüfte, dass sie übereinstimmen. | auto: `version-sync` |
| PROZ-06 | MUSS | Release-Ablauf = [Release-Checkliste](SECURITY.md#release-checkliste): frischer Klon des Tags, JDK 17, `verify-apk`, Gerätetest, Assets und Release-Body; der Workflow `release-verify` baut den Tag nach. | Reproduzierbar, nachprüfbar. | auto: `release-verify`; manuell: Checkliste |
| PROZ-07 | MUSS | **Agenten-Arbeitsweise:** Analyse mit Befunden (Datei:Zeile; bei UI ein Entwurf) vor der Umsetzung; Arbeit im Worktree; für jedes Layout gilt die Layout-Grammatik (STYLEGUIDE §4.5: Anatomie nachschlagen, nicht erfinden); Screenshots werden nicht angesehen – die Gates und die Token-Maße im Code gelten; wenige Tests (CODE-07); Keystore-Pfade und Passwörter werden nie gelesen; Memory-Regeln des Eigentümers haben Vorrang vor den Guides, die Guides vor dem Code. | Zwei Iterationen gingen daneben, weil Agenten Best-Practice-Layouts statt der Hausregeln bauten. | manuell |

## Checkliste eines Pushes

1. `scripts/check.sh` grün (oder `--fast` und CI abwarten).
2. `scripts/check-style.sh --ci-range origin/main..HEAD` ohne Fehler (Betreffs, `guide-sync` über den ganzen Bereich).
3. Baselines nur gesunken; neue Ausnahmen als Marker `// Abweichung <ID>: <Grund>`.
4. Ändert der Push die Optik gewollt, liegen die neu aufgenommenen Referenzbilder im selben Commit (`rm -rf app/src/test/screenshots && scripts/build.sh recordRoborazziDebug`, STYLEGUIDE §13.1); sonst schlägt `verifyRoborazziDebug` in `check.sh` fehl. Schlägt es ohne gewollte Änderung fehl, ist die Abweichung ein Befund, kein Anlass zum Neuaufnehmen.
5. Bestätigung des Eigentümers für den Push.

## Checkliste eines Releases

1. `scripts/bump-version.sh X.Y.Z`, Release-Notes und Changelog füllen (Laiensprache, TEXT-11), `scripts/check.sh` grün.
2. Commit `vX.Y.Z: …`; dann die [Release-Checkliste](SECURITY.md#release-checkliste) ab Schritt 3 – Tag, Nachbau, `verify-apk`, Gerätetest, Release.
3. Bestätigung des Eigentümers für Push, Tag und Release; Store-Upload nach [STORE.md](STORE.md).
