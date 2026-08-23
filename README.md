# Atemkraft

Android-App für evidenzbasiertes Atmen und stille Meditation: **16 Atemtechniken** mit
getakteten Sessions (mitatmender Kreis, Ton, Vibration), ein **Meditations-Timer**
(Timer/Frei, Gongs, gesprochene Anleitung) und ein **Logbuch**. Synthwave-Design, dark-only.

**Privatsphäre:** ohne Konto, ohne Werbung, ohne Tracking, ohne Analytics. Die App läuft
vollständig offline. Einzige Netznutzung: der **optionale, ausdrücklich angestoßene
Download der neuronalen Sprachstimmen** (Piper-Modelle von der offiziellen
sherpa-onnx-Release auf GitHub) — danach läuft auch die Sprachausgabe offline.

### Berechtigungen (alle mit Zweck)

| Berechtigung | Zweck |
|---|---|
| `INTERNET` | Nur für den optionalen Stimmen-Download (Einstellungen → Meditation) |
| `VIBRATE` | Fühlbare Phasenwechsel in Atem-Sessions (abschaltbar) |
| `FOREGROUND_SERVICE` + `_MEDIA_PLAYBACK` | Meditation läuft zuverlässig bei ausgeschaltetem Bildschirm |
| `POST_NOTIFICATIONS` | Dezente Dauer-Notification der laufenden Meditation (mit „Beenden") |
| `WAKE_LOCK` | Hält die CPU während einer laufenden Meditation wach (Gongs pünktlich trotz Doze) |

## Funktionsumfang

- **Atmen:** 16 Techniken in vier Familien (A Herunterregeln/vagal · B Hochregeln/
  sympathisch · C Balancieren/Programme · D Funktionell), je mit Anleitung, Wirkung,
  Sicherheitshinweisen und verifizierten Quellen (`data/Refs.kt`). Sessions minimierbar
  zur „Now-Playing"-Leiste.
- **Meditation:** Timer (5–90 min + frei einstellbar) oder Stoppuhr; Gongs
  (Aus / Start & Ende / alle X min), natürlicher Klangschalen-Ausklang (Kurz/Lang);
  optionale gesprochene Anleitung mit **neuronalen deutschen Stimmen** (Piper via
  sherpa-onnx, Katalog mit Vorhören vor dem Download); Foreground-Service + Wakelock.
- **Logbuch:** abgeschlossene Sitzungen mit Statistik (Serie, Minuten).
- **Situationen:** Einstiege nach Anlass („Was passt gerade?").

## Architektur

```
domain/   Reines Modell: Phase, PhaseType, PhaseDuration, Segment, Exercise + buildTimeline()
data/     ExerciseRepository · SettingsRepository (DataStore) · LogbookRepository (Room) · AppContainer
cue/      ToneCuePlayer (Gong/Cues) · ContinuousTonePlayer · SpeechGuide (System-TTS-Fallback)
          PiperSpeechGuide (sherpa-onnx) · tts/ (VoiceCatalog, VoiceModelManager, Samples)
ui/       theme/ · components/ (geteilt) · home/ · situations/ · session/ · meditation/
          log/ · settings/ · detail/ · glossary/ · about/
```

- Atem-Session: `SessionViewModel` (monotone Uhr, fortlaufender Phasen-Anker → driftfrei).
- Meditation: app-weiter `MeditationController` (überlebt Navigation) + Foreground-Service.
- Sprach-Engines hinter gemeinsamer API; neuronale Modelle werden nach `filesDir/tts`
  geladen (vom Backup ausgeschlossen, siehe `res/xml/`).

## Eine neue Übung hinzufügen

In `data/BuiltInExercises.kt` ein `Exercise` definieren und in `all` aufnehmen:

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

Voraussetzungen: JDK 21, Android SDK (Plattform 36) unter `~/Android/Sdk`
(Pfad in `local.properties`, nicht eingecheckt).

```bash
./gradlew assembleDebug          # Debug-APK
./gradlew installDebug           # bauen + installieren
# oder komfortabel (sucht JDK, setzt JAVA_HOME):
scripts/build.sh installDebug
```

Signiert wird derzeit mit `app/debug.keystore` (stabile Signatur über Container-Builds;
F-Droid signiert selbst). Toolchain: Gradle 8.11.1, AGP 8.9.1, Kotlin 2.1.0,
Compose (BOM 2025.01.00), Material 3, compileSdk/targetSdk 36, minSdk 26.

**Hinweis Verteilung:** Wegen der vorkompilierten sherpa-onnx-Native-Libs (`app/libs/*.aar`)
ist die App nicht für das Haupt-f-droid.org-Repo geeignet (build-from-source-Regel);
IzzyOnDroid oder Direkt-APK sind passende Kanäle. Alle Komponenten sind FOSS
(sherpa-onnx Apache-2.0, Piper MIT, ONNX Runtime MIT, Stimme „Thorsten" CC0) —
Credits im Über-Screen.

## Ideen / Backlog

Eigene Übungen per Editor, Zufalls-Atemmuster-Generator, Voice-Cloning (OmniVoice),
Wear OS, Widgets.
