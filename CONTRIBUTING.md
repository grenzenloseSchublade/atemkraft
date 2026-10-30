# Mitmachen bei Atemkraft

Schön, dass du helfen willst. Atemkraft ist ein Ein-Personen-Projekt in der Freizeit; Antworten können ein paar Tage dauern. Jeder Beitrag hilft, auch ein kurzer Fehlerbericht.

Für alle Beiträge gilt der [Verhaltenskodex](CODE_OF_CONDUCT.md).

## Wie du helfen kannst

- **Testen und Fehler melden:** Neues Issue mit der Vorlage [„Fehler melden“](https://github.com/grenzenloseSchublade/atemkraft/issues/new/choose). Wichtig sind Gerät, Android-Version, App-Version und die Schritte bis zum Fehler.
- **Ideen vorschlagen:** Vorlage „Idee vorschlagen“. Beschreibe, was du erreichen willst, nicht nur die Lösung.
- **Texte und Quellen prüfen:** Unklare Formulierungen, zu starke Wirkaussagen oder eine Studie, die etwas anderes sagt als die App, sind wertvolle Hinweise.
- **Code beitragen:** Pull Request gegen `main`, siehe unten. Bei größeren Änderungen bitte vorher ein Issue, damit keine Arbeit umsonst ist.

**Bitte nie Gesundheitsdaten posten:** keine Diagnosen, Medikamente oder Logbuch-Auszüge in Issues, Screenshots oder Logs. Issues sind öffentlich.

**Sicherheitslücken nicht öffentlich melden**, sondern über GitHub Private Vulnerability Reporting, siehe [SECURITY.md](docs/SECURITY.md#melden).

## Entwicklungsumgebung

- JDK 17 (CI und Release-Builds nutzen Temurin 17)
- Android SDK mit Plattform 37; Pfad in `local.properties` (`sdk.dir=…`, nicht eingecheckt) oder über `ANDROID_HOME`
- Python 3 (nur Standardbibliothek) für die Stil-Checks
- Ein Gerät oder Emulator ab Android 8.0 (API 26)

```bash
scripts/build.sh                 # Debug-APK bauen (sucht JDK 17 und setzt JAVA_HOME)
scripts/build.sh installDebug    # bauen und auf Gerät/Emulator installieren
```

`scripts/build.sh` reicht alle Argumente an `./gradlew` durch und nimmt bevorzugt JDK 17. Für eine Release-APK brauchst du keinen Schlüssel: Ohne Release-Keystore entsteht eine unsignierte APK; eine selbst gebaute Release-APK lässt sich nicht über die offizielle installieren, weil die Signatur abweicht. Signiert werden offizielle Releases nur vom Maintainer.

### Aufbau

Toolchain: Gradle 9.8.2, Android Gradle Plugin 9.4.1 (mit eingebautem Kotlin), Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00) mit Material 3, compileSdk 37, targetSdk 36, minSdk 26.

```
domain/   reines Modell: Phase, Segment, Exercise, Zeitplan, Muster-Generator, Befindens-Suche
data/     Übungen, Situationen und Quellen (Refs.kt), Room (Logbuch, Muster), DataStore (Einstellungen)
cue/      Töne, Gong, Vibration, Sprachausgabe (System oder Piper-Stimmen über sherpa-onnx)
playback/ Abläufe ohne UI: Sitzungsstatus, Meditations-Controller, Foreground-Service, Audio-Koordinatoren
ui/       theme/, components/, navigation/ und je Feature ein Paket
```

## Prüfen

Vor jedem Pull Request:

```bash
scripts/check.sh          # Stil- und Sicherheits-Checks, ktlint, Unit- und Screenshot-Tests, Android Lint, Release-Build – muss grün sein
scripts/check.sh --fast   # dasselbe ohne Android Lint und Release-Build, für zwischendurch
scripts/build.sh ktlintFormat   # Formatierung automatisch korrigieren
```

`scripts/check.sh` ist der einzige Prüf-Einstieg, lokal und im CI; was genau läuft, steht im [Styleguide §13](docs/STYLEGUIDE.md#automatische-pruefung). Dazu gehören Screenshot-Gates, die alle Screens rendern und fehlschlagen, wenn ein Wort umbricht, ein Text abgeschnitten wird, eine Tippfläche kleiner als 48 dp ist oder ein Baustein von seiner Anatomie abweicht (Styleguide §4.5). Bilder erzeugst du mit `scripts/build.sh recordRoborazziDebug` (unter `app/build/outputs/roborazzi/`). Die Referenzbilder der Tab-Matrix liegen unter `app/src/test/screenshots/` und werden bei jedem `check.sh` pixelgenau verglichen; ändert sich die Optik gewollt, nimm sie mit `rm -rf app/src/test/screenshots && scripts/build.sh recordRoborazziDebug` neu auf und committe sie mit.

Altlasten der Stil-Checks sind in `config/style-baseline.txt` eingefroren, jede Zeile mit dem Backlog-Punkt, der sie behebt. Die Baseline darf nur sinken, nie wachsen; eine bewusste Ausnahme ist ein Marker `// Abweichung <ID>: <Grund>` an der Zeile.

## Regeln

Die verbindlichen Regeln stehen in drei Dokumenten. Jede Regel hat eine ID und nennt, wie sie geprüft wird.

- [docs/STYLEGUIDE.md](docs/STYLEGUIDE.md): Design, Text und Ton, Barrierefreiheit, Code. Die zwölf Leitplanken in §1 decken die meisten Änderungen ab; §4.5 sagt, wie jeder Baustein gebaut ist.
- [docs/SECURITY.md](docs/SECURITY.md): Datenschutz, Berechtigungen, Netzwerk, Lieferkette, Release.
- [docs/PROCESS.md](docs/PROCESS.md): Commit, Push, Release und Arbeitsweise. Die Kurzfassung für Agenten ist [CLAUDE.md](CLAUDE.md).

Das Wichtigste in Kürze:

- **Kein Tracking:** keine Analyse-, Absturz-, Werbe- oder Tracking-Bibliotheken, keine Google Play Services, kein Firebase, auch nicht über Abhängigkeiten (SEC-PRIV-01).
- **Offline:** Die einzige Netzverbindung ist der Stimm-Download, den die Person selbst antippt. Eine neue Verbindung, eine neue Berechtigung oder ein neues gespeichertes Feld braucht im selben Commit die Einträge in SECURITY.md und [Datenschutzerklärung](docs/PRIVACY.md), eine neue Verbindung auch den Satz im README (SEC-PRIV-02, SEC-PRIV-04, SEC-PERM-01).
- **Nur freie Software:** Neue Abhängigkeiten brauchen eine FLOSS-Lizenz und einen Eintrag in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) (SEC-LEGAL-03).
- **Oberfläche:** nur dunkles Design, vorhandene Komponenten aus `ui/components/` (Karten mit Aktionen über `CardHeader`), Farben und Maße als Tokens aus `ui/theme/`, kein Wortumbruch und keine gekürzten Texte, Tippflächen mindestens 48 dp, Auswahl ohne Häkchen.
- **Texte:** Deutsch, Anrede „du“, Laiensprache, deutsche Anführungszeichen „…“. Begriffe nach der Begriffsliste (STYLEGUIDE §11.1, z. B. „Sitzung“ statt „Session“). Die App zählt, sie bewertet nicht (TEXT-16).

## Inhalte: evidenzehrlich

Atemkraft ist eine Wellness-App, kein Medizinprodukt. Für Übungstexte, Situationen und alle öffentlichen Texte gilt:

- **Keine Heil-, Therapie- oder Präventionsversprechen** („heilt“, „lindert“, „beugt vor“, „nachweislich“ …). Die vollständige Wortliste steht in [`config/health-claims.txt`](config/health-claims.txt); der `ContentRulesTest` prüft App-Inhalte, README, Store-Texte, Release-Notes und Datenschutzerklärung dagegen (SEC-LEGAL-01).
- **Studienlage ehrlich:** Schwache Belege stehen schon im ersten Satz („in einer Studie“, „kaum untersucht“); jede eingebaute Übung trägt genau eine Studienlage (TEXT-07).
- **Jede genannte Studie steht in [`Refs.kt`](app/src/main/java/app/atemkraft/data/Refs.kt)** im Format `Nachname I. et al. (Jahr): Originaltitel. Journal Band(Heft):Seiten. <deutsche Einordnung>.` mit DOI oder PMID. Im Text steht sie als „(Name Jahr)“. Bitte nur Quellen, die du selbst geöffnet und gelesen hast.
- **Sicherheit sichtbar:** Jeder sicherheitsrelevante Satz steht auch in `cautions`, als Bedingung und Handlung („Bei Schwindel sofort aufhören.“).

### Eine neue Übung hinzufügen

In [`data/BuiltInExercises.kt`](app/src/main/java/app/atemkraft/data/BuiltInExercises.kt) ein `Exercise` definieren und in `all` aufnehmen. Pflichtfelder sind `id`, `name`, `family`, `shortDescription`, `effect` und `evidence`:

```kotlin
private val meine = Exercise(
    id = "meine-uebung",
    name = "Meine Übung",
    family = BreathingFamily.DOWNREGULATE,
    shortDescription = "4 s ein, 6 s aus, ohne Halten.",
    effect = "Ein langer Ausatem lädt zum Herunterfahren ein.",
    evidence = EvidenceLevel.LITTLE_STUDIED,
    cautions = listOf("Bei Schwindel sofort aufhören."),
    references = listOf(Refs.zaccaro2018),
    rounds = 1,
    segments = listOf(
        Segment(
            repeat = 30,
            phases = listOf(
                Phase(PhaseType.INHALE, secs(4.0)),
                Phase(PhaseType.EXHALE, secs(6.0)),
            ),
        ),
    ),
)
```

Danach `scripts/check.sh`: Der `ContentRulesTest` prüft Teaser-Länge, Typografie (Anführungszeichen, Dezimalkomma, Trennpunkt, Zahl und Einheit) und die Wortliste ohne Baseline, der Screenshot-Test die neue Detailseite; Anrede und Quellenformat prüft das Review.

## Commits und Pull Requests

- **Commit-Betreff** (PROZ-01, im CI geprüft): `fix: …`, `docs: …` oder `build: …`, Releases `vX.Y.Z: …`. Deutsch, mit echten Umlauten, als kurze Zusammenfassung. Beispiel: `fix: Tipp-Tick am Kreis folgt dem Schalter „Vibration“`.
- **Git-Hooks** (empfohlen, einmal je Klon): `git config core.hooksPath .githooks`. Dann prüft `commit-msg` den Betreff schon beim Commit und `pre-push` die Betreffs und die Styleguide-Pflicht über alle neuen Commits wie der CI-Job „conventions“; ein falscher Betreff lässt sich so noch ohne Force-Push ändern.
- **Commit-Text** erklärt das Warum, nicht nur das Was.
- Ändert ein Push ein Token in `ui/theme/` oder die Signatur einer öffentlichen Funktion in `ui/components/`, ändert er irgendwo im selben Push auch den Styleguide (PROZ-02) – ein eigener Doku-Commit genügt.
- Bei sichtbaren Änderungen bitte Screenshots vorher und nachher in den Pull Request.
- Mit dem Einreichen stimmst du zu, dass dein Beitrag unter der [MIT-Lizenz](LICENSE) des Projekts steht.

## Releases

Releases baut, signiert und veröffentlicht nur der Maintainer. Der Ablauf steht in [PROCESS.md](docs/PROCESS.md) und der [Release-Checkliste](docs/SECURITY.md#release-checkliste): `scripts/bump-version.sh X.Y.Z` setzt Version, Release-Notes, Changelog und Stand-Zeilen; dann Tag, Build aus einem frischen Checkout des Tags, Signaturprüfung, GitHub-Release mit den drei APKs (universal, arm64-v8a, armeabi-v7a) und den Notes samt SHA-256. Für Release-Notes und Store-Texte gelten dieselben Textregeln wie in der App (STYLEGUIDE TEXT-11, SEC-LEGAL-01).

## Fragen

Für Fragen ein Issue eröffnen. Vertrauliches (z. B. Sicherheitslücken) über [Private Vulnerability Reporting](https://github.com/grenzenloseSchublade/atemkraft/security/advisories/new).
