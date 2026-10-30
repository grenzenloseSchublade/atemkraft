# Atemkraft – Kurzfassung für Agenten

Android-App (Kotlin, Compose, M3), deutsch, Ein-Personen-Projekt. Verbindlich: docs/STYLEGUIDE.md (Design, Text, Code),
docs/SECURITY.md (Daten, Lieferkette, Release), docs/PROCESS.md (Commit, Push, Release, Arbeitsweise). Diese Datei ist
die Kurzfassung; bei Widerspruch gilt der Guide, bei Widerspruch zum Eigentümer gilt der Eigentümer.

## Leitplanken (unverrückbar, STYLEGUIDE §1)
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
- Für jedes Layout gilt die Layout-Grammatik (STYLEGUIDE §4.5): Anatomie nachschlagen, nicht erfinden. Karten mit
  Aktionen nur über `CardHeader`; Maße benachbarter Elemente im Code vergleichen (ein Größentoken je Leiste).
- Screenshots nicht ansehen: Gates (WORTBRUCH, ABGESCHNITTEN, TIPPFLAECHE, ANATOMIE, SCROLLT) und Token-Maße im Code
  vergleichen.
- Texte: strings.xml (UI) oder src/data/ (Inhalte); Begriffe nach STYLEGUIDE §11.1; „…“-Anführungszeichen; Einheit mit
  geschütztem Leerzeichen; Sanskrit *kursiv*.
- Neue Verbindung, Berechtigung oder gespeichertes Feld: im selben Commit SECURITY.md + docs/PRIVACY.md nachziehen.

## Prüfen
- `scripts/check.sh` (alles wie CI) · `scripts/check.sh --fast` · `scripts/check-style.sh` (Sekunden) ·
  `scripts/check-style.sh --self-test` nach Check-Änderungen · nie `./gradlew` direkt, immer `scripts/build.sh`.
- Baseline nur senken (`config/style-baseline.txt`, jede Zeile mit S-ID); sec-* und Inhaltsregeln haben keine Baseline.

## Commit, Push, Release (PROCESS.md)
- Betreff: `fix: …`, `docs: …`, `build: …`, Release `vX.Y.Z: …`. Nie `feat:`. Deutsch, echte Umlaute, Warum im Text.
- Ändert ein Push ui/theme-Tokens oder öffentliche Komponenten, ändert er auch docs/STYLEGUIDE.md (PROZ-02).
- Vor Push: `scripts/check-style.sh --ci-range origin/main..HEAD`. Hooks: `git config core.hooksPath .githooks`.
- **Push, Tag, Release und Store-Upload nur nach ausdrücklicher Bestätigung des Eigentümers;** alte Releases nie ohne
  Zusage löschen (PROZ-04).
- Versionsbump (eigener Commit `vX.Y.Z:`): `scripts/bump-version.sh X.Y.Z`, dann Notes und Changelog füllen;
  `version-sync` prüft. Release-Ablauf: SECURITY.md → Release-Checkliste.

## Orte
src/domain (reines Kotlin) · src/data (Inhalte, Room, DataStore) · src/cue (Ton, Haptik, Stimme) · src/playback (Abläufe,
Service) · src/ui/{theme,components,navigation,<feature>} · config/ (Baselines, Allowlists, Fixtures) · docs/spec/ (Details
je Feature) · docs/DECISIONS.md (Warum etwas so ist).
