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
- Android SDK mit Plattform 36; Pfad in `local.properties` (`sdk.dir=…`, nicht eingecheckt) oder über `ANDROID_HOME`
- Python 3 (nur Standardbibliothek) für die Stil-Checks
- Ein Gerät oder Emulator ab Android 8.0 (API 26)

```bash
scripts/build.sh                 # Debug-APK bauen (sucht ein JDK und setzt JAVA_HOME)
scripts/build.sh installDebug    # bauen und auf Gerät/Emulator installieren
```

`scripts/build.sh` reicht alle Argumente an `./gradlew` durch. Für eine Release-APK brauchst du keinen Schlüssel: Ohne Release-Keystore entsteht eine unsignierte APK. Signiert werden offizielle Releases nur vom Maintainer.

## Prüfen

Vor jedem Pull Request:

```bash
scripts/check.sh          # Stil-Checks, ktlint, Unit-Tests, Android Lint – muss grün sein
scripts/check.sh --fast   # dasselbe ohne Android Lint, für zwischendurch
scripts/build.sh ktlintFormat   # Formatierung automatisch korrigieren
```

`scripts/check.sh` ist der einzige Prüf-Einstieg, lokal und im CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)). Dazu gehört ein Screenshot-Test, der alle Screens auf drei Breiten und vier Schriftgrößen rendert und fehlschlägt, wenn ein Wort umbricht, ein Text abgeschnitten wird oder eine Tippfläche kleiner als 48 dp ist. Die Bilder erzeugst du mit `scripts/build.sh recordRoborazziDebug`; sie liegen dann unter `app/build/outputs/roborazzi/`.

Altlasten sind in Baselines unter `config/` eingefroren. Eine Baseline darf nur sinken, nie wachsen.

## Regeln

Die verbindlichen Regeln stehen in zwei Dokumenten. Jede Regel hat eine ID und nennt, wie sie geprüft wird.

- [docs/STYLEGUIDE.md](docs/STYLEGUIDE.md): Design, Text und Ton, Barrierefreiheit, Code. Die Kurz-Checkliste am Anfang deckt die meisten Änderungen ab.
- [docs/SECURITY.md](docs/SECURITY.md): Datenschutz, Berechtigungen, Netzwerk, Lieferkette, Release.

Das Wichtigste in Kürze:

- **Kein Tracking:** keine Analyse-, Absturz-, Werbe- oder Tracking-Bibliotheken, keine Google Play Services, kein Firebase, auch nicht über Abhängigkeiten (SEC-PRIV-01).
- **Offline:** Die einzige Netzverbindung ist der Stimm-Download, den die Person selbst antippt. Eine neue Verbindung, eine neue Berechtigung oder ein neues gespeichertes Feld braucht im selben Commit die Einträge in SECURITY.md, README und [Datenschutzerklärung](docs/PRIVACY.md) (SEC-PRIV-02, SEC-PRIV-04, SEC-PERM-01).
- **Nur freie Software:** Neue Abhängigkeiten brauchen eine FLOSS-Lizenz und einen Eintrag in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) (SEC-LEGAL-03).
- **Oberfläche:** nur dunkles Design, vorhandene Komponenten aus `ui/components/`, Farben und Maße als Tokens aus `ui/theme/`, kein Wortumbruch und keine gekürzten Texte, Tippflächen mindestens 48 dp.
- **Texte:** Deutsch, Anrede „du“, Laiensprache, deutsche Anführungszeichen „…“. Begriffe nach der Begriffsliste (STYLEGUIDE §11.1, z. B. „Sitzung“ statt „Session“).

## Inhalte: evidenzehrlich

Atemkraft ist eine Wellness-App, kein Medizinprodukt. Für Übungstexte, Situationen und alle öffentlichen Texte gilt:

- **Keine Heil-, Therapie- oder Präventionsversprechen** („heilt“, „lindert“, „beugt vor“, „nachweislich“ …). Die vollständige Wortliste steht in [`config/health-claims.txt`](config/health-claims.txt); der `ContentRulesTest` prüft die App-Inhalte dagegen.
- **Studienlage ehrlich:** Schwache Belege (Einzelstudie, kleine Stichprobe, indirekt) stehen schon im ersten Satz („in einer Studie“, „kaum untersucht“).
- **Jede genannte Studie steht in [`Refs.kt`](app/src/main/java/app/atemkraft/data/Refs.kt)** im Format `Nachname I. et al. (Jahr): Originaltitel. Journal Band(Heft):Seiten. <deutsche Einordnung>.` mit DOI oder PMID. Im Text steht sie als „(Name Jahr)“. Bitte nur Quellen, die du selbst geöffnet und gelesen hast.
- **Sicherheit sichtbar:** Jeder sicherheitsrelevante Satz steht auch in `cautions`, als Bedingung und Handlung („Bei Schwindel sofort aufhören.“).

### Eine neue Übung hinzufügen

In [`data/BuiltInExercises.kt`](app/src/main/java/app/atemkraft/data/BuiltInExercises.kt) ein `Exercise` definieren und in `all` aufnehmen. Pflichtfelder sind `id`, `name`, `family`, `shortDescription` und `effect`:

```kotlin
private val meine = Exercise(
    id = "meine-uebung",
    name = "Meine Übung",
    family = BreathingFamily.DOWNREGULATE,
    shortDescription = "4 s ein, 6 s aus, ohne Halten.",
    effect = "Ein langer Ausatem lädt zum Herunterfahren ein.",
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

Danach `scripts/check.sh`: Der `ContentRulesTest` prüft u. a. Teaser-Länge, Anführungszeichen, Anrede, Quellenformat und die Wortliste, der Screenshot-Test die neue Detailseite.

## Commits und Pull Requests

- **Commit-Betreff** (CODE-08, im CI geprüft): `fix: …`, `docs: …` oder `build: …`, Releases `vX.Y.Z: …`. Deutsch, mit echten Umlauten, als kurze Zusammenfassung. Beispiel: `fix: Tipp-Tick am Kreis folgt dem Schalter „Vibration“`.
- **Commit-Text** erklärt das Warum, nicht nur das Was.
- Ändert ein Commit ein Token in `ui/theme/` oder die Signatur einer öffentlichen Funktion in `ui/components/`, ändert er auch den Styleguide (META-01).
- Bei sichtbaren Änderungen bitte Screenshots vorher und nachher in den Pull Request.
- Mit dem Einreichen stimmst du zu, dass dein Beitrag unter der [MIT-Lizenz](LICENSE) des Projekts steht.

## Fragen

Für Fragen ein Issue eröffnen. Vertrauliches (z. B. Sicherheitslücken) über [Private Vulnerability Reporting](https://github.com/grenzenloseSchublade/atemkraft/security/advisories/new).
