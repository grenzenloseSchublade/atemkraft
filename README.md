# Atemkraft

Minimale Android-App, die Atemübungen begleitet: sie benennt die aktuelle Phase
(Einatmen / Ausatmen / Halten), zeigt die Dauer an und unterstützt visuell mit einem
mitatmenden Kreis sowie optional mit Ton und Vibration.

Die App ist offline, ohne Tracking und ohne Permissions (kein Internet, keine
Vibrations-Permission, kein Konto). Die konkreten Atemübungen werden in
`app/src/main/java/app/atemkraft/data/BuiltInExercises.kt` als Daten gepflegt –
neue Übungen erfordern keine Änderung an UI oder Ablauflogik.

## Stand

v0.2: **14 Atemtechniken** aus der Vorlage `docs/F3_Atmung.html`, sortiert nach den
vier Familien (A Herunterregeln/vagal · B Hochregeln/sympathisch · C Balancieren/
Programme · D Funktionell). Protokolle und Quellen sind durch Recherche bestätigt bzw.
korrigiert (siehe `data/Refs.kt`).

- **Getaktet** (Atemkreis-Session): Resonanz-Atmung, Cyclic Sighing, Physiological Sigh,
  4-7-8, Bhramari, Zwerchfellatmung, Wim Hof, Feueratmung (Kapalabhati), Box-Atmung,
  Nadi Shodhana, Lippenbremse.
- **Programm/Gewohnheit** (Infoseite, kein Takt): Sudarshan Kriya (SKY), Buteyko,
  Nasenatmung.

Jede Übung hat eine Detailseite mit Beschreibung, Anleitung, Wirkung, Sicherheitshinweisen
und den verifizierten Quellen. Startscreen ist nach Familien gruppiert und farbcodiert,
mit Tags (am besten belegt / nur stabile Phase).

## Architektur

```
domain/   Reines Modell: Phase, PhaseType, PhaseDuration, Segment, Exercise + buildTimeline()
data/     ExerciseRepository (BuiltInExercises) · SettingsRepository (DataStore)
cue/      CueEvent · ToneCuePlayer (synthetisierte Töne via AudioTrack)
ui/       theme/ · home/HomeScreen · session/SessionViewModel + SessionScreen + BreathingCircle
```

Das Datenmodell ist bewusst flexibel: feste Phasen, Halten bei voller/leerer Lunge,
mehrere Runden, Per-Runde-Anpassung (`Exercise.perRound`, z. B. Wim-Hof-Retention) und
offene Holds (`OpenEnded` / `UntilUrge`, durch Tippen beendet).

Der Timer läuft im `SessionViewModel` (`viewModelScope`), ist an die Monotonuhr
verankert (driftfrei) und übersteht Rotation. Der Atemkreis wird direkt aus dem
Zustand abgeleitet, damit Kreis, Countdown und Cues synchron bleiben und die Pause
alles gemeinsam einfriert.

## Eine neue Übung hinzufügen

In `BuiltInExercises.kt` ein `Exercise` definieren und in `all` aufnehmen, z. B.:

```kotlin
private val meine = Exercise(
    id = "meine-uebung",
    name = "Meine Übung",
    description = "Kurzbeschreibung",
    rounds = 5,
    segments = listOf(
        Segment(
            phases = listOf(
                Phase(PhaseType.INHALE, secs(4.0)),
                Phase(PhaseType.HOLD_FULL, secs(2.0)),
                Phase(PhaseType.EXHALE, secs(6.0)),
            ),
        ),
    ),
)
```

## Build & Installation

Voraussetzungen: JDK 21, Android SDK (Plattform 36) unter `~/Android/Sdk`.
Der Pfad steht in `local.properties` (nicht eingecheckt).

```bash
# Debug-APK bauen
./gradlew assembleDebug

# Auf angeschlossenem Gerät/Emulator installieren
./gradlew installDebug
# oder
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Auf diesem Entwicklungs-Host zeigt `java` auf ein JRE ohne `javac`, weshalb
`./gradlew` direkt scheitert. Bequemer ist daher das mitgelieferte Skript, das
ein vollständiges JDK sucht, `JAVA_HOME` setzt und den Gradle-Cache wiederverwendet:

```bash
scripts/build.sh                 # = assembleDebug
scripts/build.sh installDebug    # bauen + installieren
```

Signiert wird mit dem mitgelieferten `app/debug.keystore` (stabile Signatur über
Container-Builds). Toolchain: Gradle 8.11.1, AGP 8.9.1, Kotlin 2.1.0, Compose
(BOM 2025.01.00), Material 3, compileSdk/targetSdk 36, minSdk 26.

## Geplant (nicht in v0.1)

Sprachansage (TTS), Session-Historie/Statistik, eigene Übungen per Editor + JSON-Assets,
Wear OS, Hintergrund-Audio bei ausgeschaltetem Bildschirm.
