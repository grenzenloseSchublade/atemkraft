# Atemkraft auf iOS – Recherche und Plan

Stand: 05.10.2026 · Status: **Plan, nicht umgesetzt** · Bezug: Repo-Stand `30caf66`

Ziel: **dieselbe App, wie sie heute auf Android ist** (gleiche Funktionen, gleiches Aussehen) auf dem iPhone – ohne eigenen Mac, zunächst ohne eigenes iPhone, ohne Cloud-Anbindung der App.

Legende: **[sicher]** offizielle Quelle geprüft · **[mittel]** Sekundärquelle oder abgeleitet · **[unsicher]** Schätzung/ungeprüft. Aufwände in Personentagen (PT) sind Schätzungen für eine Person mit Kotlin-Erfahrung **[unsicher]**.

---

## 1. Ergebnis in Kürze

- **Weg: Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP).** CMP ist auf iOS seit 1.8.0 (Mai 2025) stabil, aktuell 1.12.1 (22.09.2026) [sicher]. Die Oberfläche ist derselbe Kotlin-Code und wird von Compose selbst gezeichnet – die App sieht auf dem iPhone aus wie auf Android.
- **Wiederverwendung ca. 70–75 %** des heutigen Codes (11.850 Zeilen, 98 Dateien) [mittel]. Neu für iOS: Ton-Ausgabe, Sprachausgabe, Vibration, Hintergrundbetrieb, Datei-Export, App-Einstieg.
- **Ohne Mac machbar:** GitHub-Actions-macOS-Runner sind für öffentliche Repos kostenlos und unbegrenzt [sicher]. Bauen, Testen, Signieren und Hochladen laufen dort; entwickelt wird weiter unter Linux.
- **Pflicht-Update:** Seit 28.04.2026 nimmt App Store Connect nur Builds mit Xcode 26 / iOS-26-SDK an [sicher] → Kotlin 2.1 auf mindestens 2.2.21, empfohlen 2.4.20 [sicher].
- **Kosten:** bis zum Machbarkeits-Test 0 €; für Test-Verteilung und Store 99 €/Jahr (Apple-Konto) + gebrauchtes iPhone.

---

## 2. Test- und Debug-Builds für Testnutzer

Kurz: **Ja – mit dem Apple-Konto über TestFlight, komplett ohne Mac.** Ohne Konto gibt es nur eine Browser-Vorschau.

| Weg | Wer kann testen | Voraussetzung | Ton/Haptik/Hintergrund | Kosten |
|---|---|---|---|---|
| **Simulator im Browser (Appetize)** | jeder mit Link, ohne iPhone | Simulator-Build aus der CI, kein Apple-Konto | **nein** – Appetize gibt auf iOS keinen Ton aus [sicher] | Free: 30 min/Monat, 2 Geräte; Starter 59 $/Monat [mittel] |
| **TestFlight intern** | bis 100 Personen, die als Nutzer in App Store Connect eingetragen sind | Apple-Konto (99 €/Jahr) | ja (echtes iPhone) | im Konto enthalten |
| **TestFlight extern** | bis 10.000 Personen per E-Mail oder **öffentlichem Link** | Apple-Konto; der erste externe Build braucht eine kurze Beta-Review | ja | im Konto enthalten |
| **Ad-hoc-/Development-Build** | bis 100 registrierte Geräte pro Jahr (Geräte-ID nötig) | Apple-Konto; Installation unter Linux per USB mit `pymobiledevice3` | ja | im Konto enthalten |
| Ohne Konto (Personal Team) | nur eigene Geräte, 7 Tage gültig, max. 3 Geräte | **braucht Xcode/Mac** → für uns praktisch nicht nutzbar [sicher] | – | 0 € |

Details TestFlight [sicher]:
- Installation über die TestFlight-App auf dem iPhone – ohne Mac, ohne Kabel, ohne Geräte-ID.
- Builds laufen nach **90 Tagen** ab; danach neuen Build hochladen.
- Feedback und Absturzberichte der Tester (mit Kommentar und Screenshot) stehen in App Store Connect im Browser, 120 Tage herunterladbar. Kein Tracking-SDK nötig.

Debuggen ohne Mac [mittel]:
- Live-Logs und Absturzberichte eines per USB angeschlossenen iPhones unter Linux mit `pymobiledevice3` (iOS 17+).
- Apple-eigenes MetricKit liefert Absturzdiagnosen beim nächsten Start; die App kann sie als Datei über das Teilen-Menü exportieren (ohne Cloud).
- Für hartnäckige Fälle: Cloud-Mac für 24 h mieten (Scaleway M2 ca. 4–5 €/Tag) [mittel].

Android-Testbuilds gibt es bereits (GitHub-Releases, signiertes APK); Play-Store-„Internal testing“ wäre der Android-Gegenpart zu TestFlight.

---

## 3. Werkzeugkette ohne Mac

| Schritt | Wo | Wie |
|---|---|---|
| Entwickeln, Logik- und UI-Tests | Linux | gemeinsamer Code mit `commonTest`; zusätzlich ein Desktop-Ziel (`jvm()`), damit Compose-UI-Tests und Roborazzi-Bilder unter Linux laufen |
| iOS-Code typprüfen | Linux (Bonus) | `compileKotlinIosArm64` (klib, seit Kotlin 2.2.20 stabil); KSP-Schritte (Room) laufen dort nicht [mittel] |
| Bauen, Simulator-Tests, Screenshots | GitHub Actions `macos-26` (öffentliches Repo: 0 €) | Xcode **26.4.1 fest pinnen** (passend zu Kotlin 2.4.x); Ziele nur `iosArm64` + `iosSimulatorArm64`; Simulator-Kette boot → install → launch → screenshot; Gradle- und `~/.konan`-Cache |
| Xcode-Projekt | Repo | Projekt aus dem KMP-Wizard einchecken **oder** XcodeGen auf dem Runner (`brew install xcodegen`); Einbindung per `embedAndSignAppleFrameworkForXcode` |
| Signieren | CI | App-Store-Connect-API-Key (Rolle *App Manager* + Zertifikatszugriff, **kein Admin**) in einer GitHub-Environment `ios-release` mit **Pflicht-Freigabe**; Jobs nur per Tag oder manuell, nie für fremde Pull Requests. Signierung per `fastlane match` (empfohlen) oder per `openssl`-CSR unter Linux |
| Hochladen | CI | `fastlane pilot` → TestFlight |
| Ersatz-CI | Codemagic | 500 kostenlose macOS-Minuten/Monat [sicher] |
| Xcode Cloud | – | braucht zur Einrichtung einmal Xcode → nur mit gemietetem Mac sinnvoll [sicher] |

---

## 4. Was geteilt wird – was iOS-eigen ist

| Block | geteilt (grob) | Nötig / iOS-eigen |
|---|---|---|
| domain (Logik, Generator, Suche) | ~95 % | `java.time` → kotlinx-datetime; `java.text.Normalizer` → doistx-normalize |
| data + Inhaltskatalog | ~90 % | DataStore KMP; Zeitfunktionen ersetzen |
| Datenbank (Room) | ~80 % | Room 2.8 oder Room 3.0 (KMP); **vorher** Schema-Export und Migrationstests |
| Theme + Komponenten | ~95 % | `LocalConfiguration` ersetzen (u. a. `WholeWordText`, Regel „nie Wortumbruch“); eigene Icon-Vektoren |
| Screens | ~85–90 % | `R.string` → Compose Resources (182 Strings, 11 Plurals); Zurück-Gesten; iPhone braucht sichtbare Schließen-Knöpfe |
| Session/Meditation-Logik | ~80 % | Monotone Uhr plattformneutral; Verhalten im Gerätesleep am iPhone prüfen [unsicher] |
| Ton-Synthese (Mathematik) | ~100 % | reines Kotlin |
| Ton-Wiedergabe, Haptik, Systemstimme | 0 % | AVAudioEngine/AVAudioSession, UIImpactFeedbackGenerator/Core Haptics, AVSpeechSynthesizer |
| Piper/sherpa-onnx | ~30 % (Katalog) | für den App Store vorerst blockiert (Lizenz, §6) |
| Meditations-Dienst (Foreground-Service) | 0 % | iOS: Hintergrund-Audio, „Now Playing“, Benachrichtigung bzw. AlarmKit fürs Ende |
| Export/Import | ~50 % | iOS-Dateiauswahl (FileKit oder UIDocumentPicker) |
| Tests | ~10 Dateien nach `commonTest` | Robolectric/Roborazzi bleiben Android; iOS-Screenshots in der CI (Roborazzi iOS experimentell) |

---

## 5. Plan

| Stufe | Inhalt | PT | Kosten | Ergebnis / Entscheidungspunkt |
|---|---|---|---|---|
| **S. Machbarkeits-Test** | Separater Branch: Meditations-Tab (Timer, Chips, Gong-Logik) als KMP-Ableger; Desktop-Variante unter Linux; iOS-Simulator-Build + Screenshots auf GitHub-Mac; optional Appetize-Link | 3–5 | 0 € | Sieht es gleich aus? Halten LAYOUT-03 und Tippflächen? Bauzeit erträglich? **Nur bei Ja weiter** |
| **0. Android modernisieren** (nützt sofort) | Kotlin 2.4.20 + KSP2; Room-Schema-Export + Migrationstest, dann Room 2.8/3.0; plattformneutrale Zeit-/Text-APIs; Compose Resources; eigene Icons; Haptik hinter Interface; F-Droid-Testbuild | 12–18 | 0 € | Android verhält sich unverändert, alle Prüfungen grün |
| **1. Gemeinsames Modul** | Code nach `commonMain`/`androidMain`; Tests nach `commonTest`; Schnittstellen für Audio, Stimme, Dateien, Datum, Hintergrund | 4–6 | 0 € | Android baut aus `:shared`, iOS kompiliert in der CI |
| **2. iOS-Gerüst + Test-Verteilung** | Apple-Konto; Signierung per API-Key; CI-Pipeline mit iOS-Screenshots; erster **TestFlight-Build** (Oberfläche, Logbuch, Muster, noch ohne Ton) | 4–6 | 99 €/Jahr + iPhone | Testnutzer können installieren (§2) |
| **3. iOS-Technik** | Töne und Gong, Hintergrund + Sperrbildschirm, Systemstimme, Vibration (Vordergrund), Export, Backup-Ausschluss, Display anlassen, Ende per Benachrichtigung/AlarmKit | 15–25 | – | Atem-Session und Meditation laufen auf dem iPhone |
| **4. Feinschliff + Store** | Textgröße mit `TEXT_SCALE` und Dynamic Type, VoiceOver-Prüfung, Privacy Manifest, Datenschutz- und Support-Seite, Store-Texte ohne Heilversprechen, Alters- und DSA-Angaben, Apple-Prüfung | 5–8 | – | iOS v1 im App Store |
| **5. optional** | Piper auf iOS (vorgerenderte Ansagen oder sherpa-onnx 2.0), Live Activity, AltStore PAL (EU) | 5–15 | – | |

**Summe Stufen 0–4: ca. 40–60 PT** (plus Machbarkeits-Test). Die Spanne ist breit, weil Ton und Hintergrundbetrieb auf iOS Neuland sind.

Test-iPhone: gebrauchtes **iPhone 12 oder 13** (iOS 27 bestätigt) [mittel]; iPhone 11/SE 2 könnten 2027 herausfallen [unsicher]. Am Gerät zu prüfen: Hintergrund-Audio und Sperrbildschirm, Unterbrechungen (Anruf), Stummschalter, Bluetooth-Latenz, Haptik, VoiceOver, Textgröße, Display-Timeout.

---

## 6. Wo es nicht ganz „dieselbe App“ wird

1. **Piper-Stimmen:** espeak-ng (GPL-3.0) ist statisch in sherpa-onnx gelinkt [sicher]; GPLv3 gilt als unvereinbar mit den App-Store-Bedingungen (FSF; Fälle GNU Go 2010, VLC 2011) [mittel]. Auf iOS zunächst **Systemstimme**. Auswege: Ansagen am Entwicklerrechner mit Piper vorab rendern und als Audio mitliefern [mittel] oder sherpa-onnx 2.0 ohne espeak-ng (Issue #3731, kein Termin). Stimmmodell Thorsten ist CC0 [sicher].
2. **Vibration** nur bei offener App – Core Haptics stoppt im Hintergrund [sicher].
3. **Stille Meditation im Hintergrund:** iOS hält Apps nur bei hörbarem Audio wach (Guideline 2.5.4) [sicher]. „Stille abspielen“ ist eine Grauzone mit Ablehnungsrisiko [mittel]. Für ein zuverlässiges Ende: AlarmKit (iOS 26) oder lokale Benachrichtigung.
4. **Keine Cloud:** `isExcludedFromBackup` ist laut Apple nur ein Hinweis [sicher]. Garantiert nur mit gerätegebundenem Schlüssel (`…ThisDeviceOnly` im Schlüsselbund) – dann sind Daten bei Gerätewechsel weg, nur der Export rettet sie [sicher]. Plattform-Krypto statt SQLCipher (Export-Compliance) [mittel].
5. **Optik:** Material-3-Look wie auf Android (gewollt); kein iOS-„Liquid Glass“. Material3 ist in CMP derzeit nur als Alpha veröffentlicht [mittel].

Weitere Risiken: Navigation für Multiplatform erst 2.10.0-beta01 [sicher]; ob F-Droid ein KMP-Projekt mit iOS-Zielen baut, ist ungeprüft – vor Stufe 0 mit `fdroidserver` testen; Guideline 4.3 („simple timers“ gesättigt) – im Review Muster des Tages, eigene Muster und „keine Datenerhebung“ hervorheben [sicher]; Frühjahr 2027 voraussichtlich Xcode 27 Pflicht.

---

## 7. Kosten Jahr 1

| Posten | Kosten |
|---|---|
| Machbarkeits-Test, Stufen 0–1, CI (öffentliches Repo) | 0 € |
| Apple Developer Program (Einzelperson; **Klarname** erscheint als Verkäufer; keine Gebührenbefreiung für Einzelpersonen) | 99 USD, in DE voraussichtlich 99 €/Jahr [mittel] |
| Gebrauchtes iPhone 12/13 | ca. 150–300 € [unsicher] |
| Optional Cloud-Mac zur Fehlersuche | ca. 4–5 € pro 24 h [mittel] |
| TestFlight, Datenschutzseite (z. B. GitHub Pages), Apple-Gebühren für kostenlose App in der EU | 0 € [sicher] |

---

## 8. Entscheidungen

Jetzt:
1. Machbarkeits-Test (0 €) starten?
2. Stufe 0 unabhängig von iOS angehen (nützt Android sofort)?

Ab Stufe 2:
3. Apple-Konto als Einzelperson (Klarname im Store) oder als Verein?
4. DSA-Status „Trader“ (öffentliche Adresse/Telefon) oder „kein Trader“ – ggf. rechtlich prüfen.
5. Sprachausgabe auf iOS: Systemstimme, vorgerenderte Piper-Ansagen oder auf sherpa-onnx 2.0 warten?
6. Lizenzhinweis Android: GPL-Bestandteil (espeak-ng) korrekt ausweisen.
7. Stille Meditation im Hintergrund: nur mit Ton/Display, Stille-Audio (Review-Risiko) oder AlarmKit/Benachrichtigung fürs Ende?
8. Haptik nur im Vordergrund akzeptieren und in der App erklären?
9. Kein Backup: Flag (keine Garantie) oder gerätegebundene Verschlüsselung (Daten weg bei Gerätewechsel)?
10. Versionen: aktuelles CMP mit Material3-Alpha oder ältere stabile Linie?
11. Room: erst 2.8 oder direkt Room 3.0?
12. AltStore PAL als Zusatzkanal in der EU?
13. Testnutzer: TestFlight intern, extern per öffentlichem Link, oder Appetize-Vorschau für Leute ohne iPhone?

---

## 9. Quellen (Auswahl, Stand in Klammern)

Compose Multiplatform/Kotlin: [CMP 1.8.0 stabil](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/) (05/2025) · [What's new 1.12](https://kotlinlang.org/docs/multiplatform/whats-new-compose-112.html) (09/2026) · [Kompatibilität Kotlin/Xcode](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) (10/2026) · [Mac-Pflicht für Apple-Binaries](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html) (10/2026) · [Direct Integration Xcode](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html) · [iOS Accessibility](https://kotlinlang.org/docs/multiplatform/compose-ios-accessibility.html) · [Navigation](https://kotlinlang.org/docs/multiplatform/compose-navigation.html) · [Resources](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html) · [Compose-Tests](https://kotlinlang.org/docs/multiplatform/compose-test.html)

Jetpack/Bibliotheken: [Room KMP](https://developer.android.com/kotlin/multiplatform/room) · [Room 3](https://developer.android.com/jetpack/androidx/releases/room3) · [DataStore KMP](https://developer.android.com/kotlin/multiplatform/datastore) · [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime/releases) · [doistx-normalize](https://github.com/Doist/doistx-normalize) · [FileKit](https://github.com/vinceglb/FileKit/releases) · [Roborazzi](https://github.com/takahirom/roborazzi)

Sprache/Lizenz: [sherpa-onnx Releases](https://github.com/k2-fsa/sherpa-onnx/releases) · [Issue #3731](https://github.com/k2-fsa/sherpa-onnx/issues/3731) · [Thorsten CC0](https://huggingface.co/rhasspy/piper-voices/blob/main/de/de_DE/thorsten/high/MODEL_CARD) · [FSF zu App Store](https://www.fsf.org/news/2010-05-app-store-compliance)

Apple: [Upcoming Requirements (Xcode 26)](https://developer.apple.com/news/upcoming-requirements/) · [TestFlight](https://developer.apple.com/testflight/) · [Tester-Feedback](https://developer.apple.com/help/app-store-connect/test-a-beta-version/view-tester-feedback) · [Mitgliedschaften](https://developer.apple.com/support/compare-memberships/) · [Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) · [AVAudioSession playback](https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct/playback) · [Core Haptics im Hintergrund](https://developer.apple.com/documentation/corehaptics/chhapticengine/stoppedreason/applicationsuspended) · [iCloud-Backup optimieren](https://developer.apple.com/documentation/foundation/optimizing-your-app-s-data-for-icloud-backup) · [DTS zu ThisDeviceOnly](https://developer.apple.com/forums/thread/788360) · [Privacy Manifest](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files) · [Apps in der EU](https://developer.apple.com/support/apps-in-the-eu/) · [AlarmKit WWDC25](https://developer.apple.com/videos/play/wwdc2025/230/) · [Xcode Cloud](https://developer.apple.com/xcode-cloud/get-started)

CI/Test: [GitHub-Runner](https://docs.github.com/en/actions/reference/runners/github-hosted-runners) · [Actions-Abrechnung](https://docs.github.com/en/billing/concepts/product-billing/github-actions) · [macos-26 GA](https://github.blog/changelog/2026-02-26-macos-26-is-now-generally-available-for-github-hosted-runners/) · [Codemagic Preise](https://codemagic.io/pricing/) · [Scaleway Apple Silicon](https://www.scaleway.com/en/pricing/apple-silicon/) · [Appetize Preise](https://appetize.io/pricing) · [Appetize ohne iOS-Ton](https://support.appetize.io/does-appetize.io-support-audio-including-sound-or-microphone) · [pymobiledevice3](https://github.com/doronz88/pymobiledevice3) · [fastlane TestFlight](https://docs.fastlane.tools/actions/testflight/)

Vollständige Recherche-Rohdaten (alle Kernaussagen mit Sicherheitsstufe) lagen in der Session vor; dieses Dokument enthält die geprüfte Essenz.
