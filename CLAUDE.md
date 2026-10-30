# Atemkraft – Kurzfassung für Agenten

Android-App (Kotlin, Compose, M3), deutsch, Ein-Personen-Projekt. Verbindlich: docs/STYLEGUIDE.md (Design, Text, Code),
docs/SECURITY.md (Daten, Lieferkette, Release), docs/PROCESS.md (Commit, Push, Release, Arbeitsweise, Qualitätszyklus).
Diese Datei fasst nur zusammen und verweist; bei Widerspruch gilt der Guide, bei Widerspruch zum Eigentümer gilt der
Eigentümer.

## Leitplanken (Kurzform von STYLEGUIDE §1, gleiche Nummern)
1. Nur dunkel, ruhig, minimal: eine Neon-Fläche je Screen, Icon-only mit contentDescription, kein FAB/Toast/Snackbar.
2. Köpfe weiß; Farbe nur, wo sie etwas bedeutet (Familie, Studienlage, Warnung, Atemphase, eigenes Muster).
3. Kein Wort bricht um, nichts wird gekürzt oder beschnitten (360–412 dp, fontScale 1,0–2,0); Tippflächen 48 dp.
4. Tokens statt Literale (ui/theme/), Komponenten statt Nachbau (ui/components/); Anatomie §4.5; Auswahl ohne Häkchen.
5. Material-Schrift und M3-Größen ohne eigenen Skalierfaktor.
6. Barrierefrei: Name, Rolle, Zustand, Überschrift, paneTitle, eine Live-Region je Zustandswechsel.
7. Evidenz-ehrlich, Laiensprache, „du“: kein Heil-/Therapie-/Präventionsversprechen; Teaser = ganzer Satz ≤ 95 Zeichen.
8. Die App zählt, sie bewertet nicht: keine Ziele, kein Soll, keine gesundheitliche Auswertung.
9. Offline und lokal: kein Tracking, keine Cloud, kein Auto-Backup; nur Export/Import; Netz nur Stimm-Download per Tap.
10. Lieferkette gepinnt, nur FLOSS; Keystore und Passwörter liegen außerhalb des Repos – nie lesen.
11. Jede Regel hat ID und Prüfung; Altlasten nur in der Baseline mit S-ID, Ausnahmen nur mit `// Abweichung <ID>:`.
12. Wenige aussagekräftige Tests (Daten, Sicherheit, Gates) – kein Test je Befund.

## Arbeitsweise (PROZ-07)
- Erst Analyse mit Befunden (Datei:Zeile), bei UI ein Entwurf; dann umsetzen, im Worktree, nichts über den Auftrag hinaus.
- Vorhandene Komponente suchen (STYLEGUIDE §5.1); Layout nach der Anatomie §4.5, Karten mit Aktionen nur über
  `CardHeader`; Maße benachbarter Elemente im Code vergleichen (ein Größentoken je Leiste, LAYOUT-04).
- Screenshots nicht ansehen: Gates (WORTBRUCH, ABGESCHNITTEN, TIPPFLAECHE, ANATOMIE, SCROLLT) und Token-Maße (R-VISUAL).
- Texte: strings.xml (UI) oder src/data/ (Inhalte); Begriffe nach §11.1; „…“; Einheit mit geschütztem Leerzeichen;
  Sanskrit *kursiv* (TEXT-01, -02, -10, -14).
- Neue Verbindung, Berechtigung oder gespeichertes Feld: im selben Commit SECURITY.md + docs/PRIVACY.md (SEC-PRIV-02,
  SEC-PERM-01, SEC-PRIV-04).

## Prüfen
- `scripts/check.sh` (alles wie CI) · `scripts/check.sh --fast` · `scripts/check-style.sh` (Sekunden) ·
  `scripts/check-style.sh --self-test` nach Check-Änderungen · nie `./gradlew` direkt, immer `scripts/build.sh`.
- Baseline nur senken (`config/style-baseline.txt`, jede Zeile mit S-ID); sec-* und Inhaltsregeln haben keine Baseline.

## Commit, Push, Release (PROCESS.md)
- Betreff `fix:`, `docs:`, `build:`, Release `vX.Y.Z:`; nie `feat:`; deutsch, echte Umlaute, Warum im Text (PROZ-01).
- Ändert ein Push ui/theme-Tokens oder öffentliche Komponenten, ändert er auch docs/STYLEGUIDE.md (PROZ-02).
- Vor Push: `scripts/check-style.sh --ci-range origin/main..HEAD`; Hooks: `git config core.hooksPath .githooks` (PROZ-03).
- **Push, Tag, Release und Store-Upload nur nach ausdrücklicher Bestätigung des Eigentümers;** alte Releases nie ohne
  Zusage löschen (PROZ-04).
- Release: Einstieg PROCESS „Checkliste eines Releases“ – `scripts/bump-version.sh X.Y.Z` (PROZ-05), Kurzcheck und
  R-PERF (PROZ-09), dann SECURITY.md → Release-Checkliste.

## Orte
src/domain (reines Kotlin) · src/data (Inhalte, Room, DataStore) · src/cue (Ton, Haptik, Stimme) · src/playback (Abläufe,
Service) · src/ui/{theme,components,navigation,<feature>} · config/ (Baselines, Allowlists, Fixtures) · docs/spec/ (Details
je Feature) · docs/DECISIONS.md (Warum etwas so ist) · STYLEGUIDE §14 (Backlog).
