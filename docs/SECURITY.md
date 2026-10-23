# Atemkraft – Sicherheit und Datenschutz

Sicherheits-, Datenschutz-, Lieferketten- und Release-Regeln für Atemkraft (`app.atemkraft`). Zielgruppe: Maintainer und Claude. Gegenstück für Design, Text und Code: [STYLEGUIDE.md](STYLEGUIDE.md).

- **Stand:** 2026-10-24 (v2.5.0 und `main`; statische Sicherheits-Checks seit 2026-10-19 aktiv, `verify-apk` und der Tag-Nachbau `release-verify` seit 2026-10-24).
- **Schlüsselwörter:** MUSS / DARF NICHT / SOLL im Sinne von RFC 2119.
- **Regelformat** (gemeinsam mit STYLEGUIDE): `ID | Stufe | Regel | Warum | Prüfung`. `auto:` nennt Werkzeug und Check (siehe [Automatische Prüfung](#automatische-pruefung)), `manuell:` nennt Review oder Checklisten-Schritt. In der Check-Tabelle stehen die vollen Regel-IDs.
- **Grundlage:** OWASP MASVS v2.1.0 (L1 + Privacy), Android Security Checklist, Google-Play-Richtlinien, F-Droid Inclusion Policy.

<a id="melden"></a>
## Sicherheitslücke melden

**Bitte keine öffentlichen Issues für Sicherheitslücken.** Melde sie über GitHub Private Vulnerability Reporting: *Security* → *Advisories* → **Report a vulnerability**. Ist das Formular nicht erreichbar, eröffne ein Issue ohne technische Details und bitte um Kontakt. Reports in English are welcome.

| Punkt | Zusage |
|---|---|
| Inhalt der Meldung | betroffene Version, Gerät/Android-Version, Schritte zum Nachstellen, Auswirkung, ggf. Fix-Idee |
| Eingangsbestätigung | in der Regel innerhalb von 7 Tagen (Ein-Personen-Projekt) |
| Ersteinschätzung | innerhalb von 30 Tagen |
| Fix und Offenlegung | koordiniert; Ziel ist ein Fix innerhalb von 90 Tagen, danach ein GitHub Security Advisory mit Nennung (auf Wunsch) |
| Unterstützte Version | nur die jeweils neueste Release-Version (derzeit 2.5.x), keine Backports |
| Nicht im Scope | gerootete oder kompromittierte Geräte, Reverse Engineering, fehlende Obfuskation oder Tamper-Erkennung (die App ist Open Source und enthält keine Geheimnisse) |

### Echtheit einer APK prüfen

Alle offiziellen Releases sind mit diesem Zertifikat signiert, auf allen Kanälen dasselbe:

| Feld | Wert |
|---|---|
| Subject | `CN=grenzenloseSchublade, O=Atemkraft` (RSA 4096) |
| Zertifikat SHA-256 | `75:02:2D:EF:E6:5A:A2:4E:22:3F:3E:79:88:0F:09:F4:23:45:67:57:00:2F:F0:B3:3B:A2:AE:29:B9:F4:18:90` |
| ohne Trennzeichen | `75022defe65aa24e223f3e79880f09f423456757002ff0b33ba2ae29b9f41890` |

Prüfen mit `apksigner verify --print-certs atemkraft-vX.Y.Z.apk`, AppVerifier oder Obtainium. Die SHA-256 jeder APK steht in den Release-Notes. Eine APK mit anderem Zertifikat, insbesondere `CN=Android Debug`, ist kein offizielles Release.

<a id="prinzipien"></a>
## Prinzipien und Daten-Inventar

Atemkraft ist **offline-first**: kein Konto, kein Server, keine Telemetrie, keine Werbung. Alle Nutzerdaten bleiben auf dem Gerät. Einzige Ausnahme ist der optionale, per Tap ausgelöste Stimm-Download ([Netzwerk](#netzwerk)). Es gibt **keine Cloud-Anbindung**, auch kein Android-Cloud-Backup (Nutzerentscheidung 2026-10-05, Datenhoheit). Daten verlassen das Gerät nur auf ausdrücklichen Wunsch: als Export-Datei an einen selbst gewählten Ort oder per lokaler Gerät-zu-Gerät-Übertragung beim Handywechsel ([Speicherung](#speicherung)). Für Nutzer zusammengefasst in der [Datenschutzerklärung](PRIVACY.md).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-PRIV-01 | DARF NICHT | Analytics-, Crash-Reporting-, Werbe- oder Tracking-SDKs, Google Play Services oder Firebase einbinden, auch nicht transitiv. | Offline-Versprechen; F-Droid-Anti-Feature *Tracking*. | auto: `sec-deny-deps`, `apk-dex-deny` |
| SEC-PRIV-02 | MUSS | Einzige Netzverbindung der App ist der nutzerinitiierte Stimm-Download. Externe Links sind konstante `https://`-URLs, die der Browser öffnet. Jede neue Verbindung steht im selben Commit in der Endpunkt-Tabelle, im README und in der Datenschutzerklärung. | Transparenz; Play Data safety bleibt wahr. | auto: `sec-net-endpoints`; manuell: Review |
| SEC-PRIV-03 | MUSS | Keine Abflüsse auf dem Gerät: keine Gerätekennungen (Android ID, Werbe-ID, IMEI, Seriennummer), kein Logging in `app/src/main` (Ausnahme: hinter `BuildConfig.DEBUG`, ohne Nutzerdaten), keine Zwischenablage ohne Nutzeraktion, auf dem Sperrbildschirm nur generischer Status (nie Übungsnamen oder Logbuchdaten). | Logcat, Zwischenablage und Sperrbildschirm sind für Dritte sichtbar. | auto: `sec-device-leaks`; manuell: Review der Notifications |
| SEC-PRIV-04 | MUSS | Jedes persistierte Feld steht im Daten-Inventar (Zeile im selben Commit). Nutzer können ihre Daten in der App löschen, ohne Reste; Löschaktionen sind gegen Fehltipp geschützt (STYLEGUIDE MUSTER-05). | Datenminimierung und Nutzerkontrolle nachweisbar. | manuell: Review bei Diff an `data/local/*`, `SettingsRepository.kt` |
| SEC-PRIV-05 | MUSS | Texte zur Datennutzung (App, README, Store) sind wahr und vollständig („offline, außer optionalem Stimm-Download“). Eine Datenschutzerklärung liegt unter `docs/PRIVACY.md` und ist im Über-Screen verlinkt. | Play-Pflicht auch ohne Datenerhebung; DSGVO Art. 13. | auto: `sec-privacy-link`; manuell: Inhalt |
| SEC-PRIV-06 | MUSS | Freitext-Eingaben zum Befinden (die Befindens-Suche im Tab „Atmen“) sind flüchtig: nur `rememberSaveable` (Activity-Zustand für Drehen und Prozess-Neustart), nie DataStore, Room, Datei oder Log; sie verfallen beim Leeren der Suche (X oder Zurück). **Einzige Ausnahme:** der selbst gewählte Name eines eigenen Musters (Namensfeld im Muster-Editor) – er wird bewusst gespeichert (Room `saved_patterns.name`) und exportiert, bereinigt wie beim Import (`PatternBackup.cleanName`, keine Steuer- und Formatzeichen), höchstens 80 Zeichen, nie im Log. Jedes Freitextfeld bekommt `autoCorrectEnabled = false` und `IME_FLAG_NO_PERSONALIZED_LEARNING` (über `InterceptPlatformTextInput`: `SearchField`, `NoPersonalizedLearning` am Namensfeld), damit Tastatur-Apps die Wörter nicht lernen oder synchronisieren. | Eingaben zum Befinden sind gesundheitsnah; Drittanbieter-Tastaturen lernen und synchronisieren sonst Wörter. Das Flag ist eine Bitte an die Tastatur, kein Schutz vor einer bösartigen. | auto: `sec-device-leaks`; manuell: Review bei neuen Textfeldern (EditorInfo des Feldes) |

**Daten-Inventar (ab v1.6.0, Stand `main`)**

| Speicherort | Inhalt | Sensitivität | Gerät-zu-Gerät / Export | Löschweg |
|---|---|---|---|---|
| Room `log_entries` | Art (Atemübung/Meditation), Übung, Zeitpunkt, Dauer, Runden | gesundheitsnah, niedrig (Übungs-IDs können auf Erkrankungen hindeuten) | je nach Hersteller (ab Android 12) / nein | *Einstellungen → Daten → „Logbuch leeren“* (zweistufig), App-Speicher löschen |
| Room `saved_patterns` | Name (frei wählbar, höchstens 80 Zeichen, SEC-PRIV-06), Phasenlängen, Art (`activating`: aktivierend ja/nein), Herkunft (`origin`), Dauer (`minutes`), Phasenfolge eigener Muster (`phases`, z. B. `in:4,hold:1,out:6`) und Speicherzeitpunkt gespeicherter und eigener Muster | niedrig | je nach Hersteller (ab Android 12) / ja (mit Anpassung) | einzeln löschen |
| DataStore `settings` | Einstellungen (inkl. bestätigter Sicherheitshinweis `safety_acknowledged` und Anzeige während Atemübung und Meditation, z. B. `session_auto_calm`, `session_silence_after`, `med_show_time`), Anpassungen pro Übung, Markierungen „Tut mir gut“ (`marked_exercises`), ob die Benachrichtigungs-Erlaubnis schon erfragt wurde (`notification_asked`), Zeitpunkt eines Zurücksetzens nach beschädigter Datei (`settings_reset_at`) und ob es gemeldet ist (`settings_reset_seen_at`) | niedrig | je nach Hersteller (ab Android 12) / nur Anpassungen gespeicherter Muster | zurücksetzen pro Übung, App-Speicher löschen |
| `files/tts/` | Stimmmodelle (öffentliche Daten, bis ca. 130 MB), beim Entpacken ein Zwischenordner `.<stimme>.staging` | keine | nein / nein | „Stimme löschen“ |
| `files/tts/.download/` | Teil-Download (bis 2.5.0 in `cache/`; dort liegende Reste löscht die App beim Start) | keine | nein / nein | bei Abbruch oder Fehler sofort; nach Prozess-Ende beim nächsten App-Start |
| Activity-Zustand (flüchtig, `rememberSaveable`) | Suchtext der Befindens-Suche (SEC-PRIV-06) | gesundheitsnah | nein / nein | Suche schließen (X, Zurück, „Alle zeigen“), App beenden |

Export-Datei (`atemkraft-muster-<datum>.json`, Format `atemkraft-muster` v1): Name, Phasenlängen, Art, Herkunft (`origin`: Muster des Tages oder eigenes Muster), Vorgabedauer (`minutes`), Phasenfolge eigener Muster (`phases`), Speicherzeitpunkt und Anpassung gespeicherter Muster sowie der Exportzeitpunkt; `origin`, `minutes` und `phases` sind optional, ältere Versionen überlesen sie (bei `phases` tragen die Phasenlängen deren Zusammenfassung); Ort wählt der Nutzer über das Storage Access Framework, die App behält keinen Zugriff. Der Import übernimmt nur, was die App selbst erzeugen könnte (Leitplanken des Generators, bei einer Phasenfolge stattdessen deren Regeln: 2–12 Phasen, je 1–30 s, mindestens ein Ein und ein Aus, nur bei eigenen Mustern; Stepper-Grenzen, Dauer 1–30 min, Namen ohne Steuer-/Format-Zeichen, höchstens 512 KiB und 500 Muster). Gespeichert werden keine Messwerte, keine Freitexte außer dem selbst gewählten Namen eigener Muster (höchstens 80 Zeichen, bereinigt wie beim Import; nicht die Befindens-Suche) und keine Identität. Logbuch-Daten werden vorsorglich wie Gesundheitsdaten behandelt.

**Deinstallieren:** Die App setzt `android:hasFragileUserData="true"` (auf `main` seit 2026-10-24, also ab der Version nach 2.5.0; wirksam ab Android 10). Android fragt dann beim Deinstallieren, ob die App-Daten behalten werden sollen; bejaht man, bleiben sie im App-Speicher des Geräts (nicht in einer Cloud) und sind nach einer Neuinstallation mit derselben Signatur wieder da. Ohne diese Wahl löscht Android alles.

<a id="berechtigungen"></a>
## Berechtigungen

Vollständige Allowlist für das gemergte Release-Manifest (`config/security/permissions.allow`):

| Berechtigung | Zweck |
|---|---|
| `INTERNET` | nur der optionale Stimm-Download |
| `VIBRATE` | Haptik bei Phasenwechseln und Tipp-Tick am Kreis (beides hinter dem Schalter „Vibration“); Rasten des Zeitrings als Berührungs-Feedback (`USAGE_TOUCH`, folgt der Android-Einstellung) |
| `WAKE_LOCK` | Meditations-Timer im Doze-Modus (`PARTIAL_WAKE_LOCK`) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Meditation läuft bei gesperrtem Bildschirm weiter |
| `POST_NOTIFICATIONS` | Status der Meditation; einmal erfragt beim ersten Meditationsstart, bei Ablehnung läuft der Timer weiter |
| `app.atemkraft.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | automatisch von androidx.core, signature-geschützt, nur intern |
| `<queries>` `TTS_SERVICE` | Sichtbarkeit der System-Sprachausgabe; sie erhält nur feste Ansagetexte |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-PERM-01 | MUSS | Das gemergte Release-Manifest enthält genau die Berechtigungen der Allowlist. Eine neue Berechtigung braucht im selben Commit Manifest-Kommentar, Zeile in der Berechtigungstabelle der Datenschutzerklärung und Allowlist-Zeile (bei Bibliotheken mit Herkunft). Verboten sind u. a. `QUERY_ALL_PACKAGES`, `AD_ID`, Speicher-, Standort-, Kamera-, Mikrofon-, Kontakt- und `REQUEST_INSTALL_PACKAGES`-Berechtigungen. | Minimalprinzip; Bibliotheken schleppen Berechtigungen ein. | auto: `apk-permissions`; Lint `QueryAllPackagesPermission` |
| SEC-PERM-02 | MUSS | Hintergrundarbeit minimal: Foreground Services mit kleinstem passendem Typ (Begründung im Manifest-Kommentar), Wakelocks nur `PARTIAL_WAKE_LOCK` mit Timeout und Freigabe bei Pause und Ende. Runtime-Berechtigungen werden im Kontext abgefragt. Bei jedem `targetSdk`-Bump werden FGS- und Behavior-Changes geprüft. | Akku, Play-FGS-Deklaration. | auto: `sec-wakelock`; manuell: Review |

<a id="netzwerk"></a>
## Netzwerk und Downloads

| Zweck | Ziel | Auslöser | Übertragene Daten | Empfänger |
|---|---|---|---|---|
| Stimm-Download | `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/<stimme>.tar.bz2` (Weiterleitung auf `release-assets.githubusercontent.com`) | nur expliziter Tap | IP-Adresse, User-Agent `Atemkraft/<version>` (bis v1.5.1 der Android-Standard mit Android-Version und Gerätemodell), Zeitpunkt, Dateiname | GitHub / Microsoft (USA) |
| Links im Über-Screen | `https://github.com/grenzenloseSchublade/atemkraft` sowie dort `blob/main/docs/PRIVACY.md` und `blob/main/THIRD_PARTY_LICENSES.md` | Tap, öffnet externen Browser | – (kein App-Traffic) | – |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-NET-01 | MUSS | Nur HTTPS: Cleartext per `network_security_config.xml` verboten (auch für Android 8, wo er sonst noch erlaubt ist), nur System-Trust-Anchors. Keine eigenen `X509TrustManager`, `HostnameVerifier` oder `SSLSocketFactory`; kein Zertifikats-Pinning auf Fremd-Hosts (Integrität sichert SEC-NET-03). | Abhören und Manipulation; Fremd-Hosts rotieren Zertifikate. | auto: `sec-tls`, `sec-tls-config`, `apk-hardening`; Lint `TrustAllX509TrustManager`, `BadHostnameVerifier` |
| SEC-NET-02 | MUSS | Downloads nur nach explizitem Tap, nie automatisch oder im Hintergrund. Die URL besteht aus der Konstanten-Basis plus Stimm-ID aus dem Code-Katalog. Quelle und Größe sind vor dem Tap sichtbar. Eigener User-Agent ohne Gerätedaten, Timeouts ≤ 30 s. | F-Droid-Opt-in-Regel, DSGVO Art. 13; keine datengesteuerten Ziele. | auto: `sec-net-endpoints`, `sec-net-client`, `sec-download`; manuell: Katalog-Review (HTTPS-Basis, SHA-256, Größe); Quelle vor dem Tap (seit 2026-10-19 im Hinweis über dem Stimmen-Katalog: „Stimmen kommen von GitHub (sherpa-onnx).“; dazu *Über → Lizenzen*) |
| SEC-NET-03 | MUSS | Jede Stimme ist im Katalog mit SHA-256 und Größe gepinnt. Der Download wird beim Streamen gehasht und **vor** dem Entpacken verglichen; bei Abweichung wird er gelöscht. Ändert sich ein Asset upstream, ist das ein Katalog-Update im Code. | Modelle werden von nativem Code geparst. | manuell: Review von Katalog-Updates und `VoiceModelManager` |
| SEC-NET-04 | MUSS | Archive werden in einen Staging-Ordner je Stimme entpackt. Jeder Zielpfad liegt darin, erlaubt sind nur reguläre Dateien und Ordner, Größe und Eintragszahl sind begrenzt (höchstens 1000 Einträge und 300 MB entpackt, gezählt beim Schreiben; die Archive des Katalogs haben rund 400 Einträge und höchstens etwa 130 MB). Installiert wird atomar (Staging-Ordner, dann Umbenennen); Reste werden beim App-Start entfernt. | Path Traversal, Archiv-Bomben, halbe Installationen. Den Inhalt sichert schon der Hash-Pin (SEC-NET-03); die Grenzen greifen, falls er je versagt. | manuell: Review von `VoiceModelManager` (`extractTarBz2Safely`, `MAX_ARCHIVE_*`) |

<a id="plattform"></a>
## Plattform-Interaktion

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-PLAT-01 | MUSS | `android:exported` ist immer explizit. Exportiert sind nur `MainActivity` (Launcher) und geschützte Bibliotheks-Komponenten aus `config/security/exported.allow`. Keine eigenen Manifest-Receiver; dynamische Receiver nur mit `RECEIVER_NOT_EXPORTED`. | Kleine IPC-Angriffsfläche. | auto: `apk-exported`, `sec-components`; Lint `ExportedService`, `ExportedReceiver` |
| SEC-PLAT-02 | MUSS | Die App wertet keine Intent-Daten oder Extras von außen aus und hat keine `<data>`-Intent-Filter; Deep-Links nur nach Review mit Allowlist-Validierung jedes Parameters. PendingIntents sind explizit und `FLAG_IMMUTABLE`. | Fremde Apps erreichen die exportierte Activity; Intent-Hijacking. | auto: `sec-intents`; Lint `UnspecifiedImmutableFlag`; manuell: Review bei Diff an Navigation |
| SEC-PLAT-03 | DARF NICHT | WebView, JavaScript-Bridges oder dynamisch geladener Code (`DexClassLoader`, `System.load` aus beschreibbaren Pfaden). Heruntergeladene Modelle sind Daten, nie Code. | Play-Policy gegen Code-Download; F-Droid. | auto: `sec-no-dyncode`; Lint `SetJavaScriptEnabled` |

<a id="speicherung"></a>
## Speicherung und Backup

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-STORE-01 | MUSS | Persistente Daten nur im app-internen Speicher (Room, DataStore, `filesDir`, `cacheDir`); kein externer Speicher, kein `MediaStore`, kein `MODE_WORLD_*`. Ein Export läuft nur über das Storage Access Framework. | App-Sandbox, keine Speicher-Berechtigung. | auto: `sec-storage`; Lint `WorldReadableFiles`, `SdCardPath` |
| SEC-STORE-02 | MUSS | **Kein Cloud-Backup:** `android:allowBackup="false"` und in `data_extraction_rules.xml` schließt `cloud-backup` jede Domäne aus. Erlaubt ist nur `device-transfer` (lokal, ab Android 12; darunter schaltet `allowBackup="false"` auch sie ab). Ist-Stand: Laut [Android-Doku](https://developer.android.com/guide/topics/manifest/application-element#allowbackup) schaltet `allowBackup="false"` die Übertragung auch ab Android 12 ab, nur auf Geräten mancher Hersteller nicht; ob `device-transfer` greift, hängt also vom Gerät ab, und Nutzertexte sagen „je nach Hersteller“; große oder neu ladbare Daten (`files/tts`) sind dort ausgeschlossen. Persistenz über eine Neuinstallation nur per Export-Datei (SEC-STORE-01); der Import behandelt die Datei als nicht vertrauenswürdig (Größe, Anzahl, Plausibilität begrenzt). | Datenhoheit: gesundheitsnahe Daten verlassen das Gerät nicht ohne ausdrückliche Wahl. | auto: `sec-backup`; Lint `DataExtractionRules`; JUnit `PatternBackupTest` |
| SEC-STORE-03 | MUSS | Room-Schemaänderungen nur mit expliziter `Migration` und Migrationstest, Schema exportiert nach `app/schemas/` (ab v2; v1 gab es nur in der nie veröffentlichten v0.2); `fallbackToDestructiveMigration*` ist verboten. DataStore hat einen `corruptionHandler` (beschädigte Datei → Standardwerte plus Marke `settings_reset_at`, kein Absturz); Lesefehler liefern Standardwerte. Enums stehen unter ihrem Namen, nie als `ordinal`. | Das Logbuch ist Nutzerhistorie; ohne Cloud-Backup wäre „App-Speicher löschen“ die einzige Rettung. | auto: `sec-room`, `ordinal-persist`; JUnit `DatabaseMigrationTest`; manuell: Review des `corruptionHandler` |

<a id="lieferkette"></a>
## Lieferkette

**Herkunftsnachweis gepinnter Binärartefakte**

| Artefakt | SHA-256 | Herkunft | Enthaltene Komponenten |
|---|---|---|---|
| `com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.6` (AAR, von JitPack; gepinnt in `gradle/verification-metadata.xml`) | `0012d9a28f15bd6fb966b62b70a75da3990512fdccce28b83098248ce4be1698` | Release `v1.13.6` von `k2-fsa/sherpa-onnx`: JitPack installiert laut `jitpack.yml` von sherpa-onnx das AAR aus dem GitHub-Release unverändert; byte-identisch mit dem GitHub-Asset (verglichen 2026-10-23 und 2026-10-24). Bis 2026-10-24 lag dieselbe Datei als `app/libs/sherpa-onnx-1.13.6.aar` im Repo | sherpa-onnx (Apache-2.0), onnxruntime 1.27.1 (MIT), espeak-ng (GPL-3.0-or-later), piper-phonemize (MIT), OpenFst/Kaldi-Bausteine (Apache-2.0), Eigen (MPL-2.0), hclust-cpp und Darts-clone (BSD-2-Clause), KISS FFT (BSD-3-Clause), Supertonic-Teile und nlohmann/json (MIT); vollständig mit Commits in [THIRD_PARTY_LICENSES.md](../THIRD_PARTY_LICENSES.md). In die APK kommen daraus nur `libsherpa-onnx-jni.so` und `libonnxruntime.so` (byte-gleich, ohne erneutes Strippen); die C- und C++-API (`libsherpa-onnx-c-api.so`, `-cxx-api.so`) lädt niemand und ist ausgeschlossen (`app/build.gradle.kts`, `packaging`) |
| `gradle/wrapper/gradle-wrapper.jar` | `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98` | offizieller Gradle-9.8.2-Wrapper | – |

Im Repo liegen keine Bibliotheken als Datei (der F-Droid-Scanner lehnt `.aar`/`.jar`/`.so` im Quellbaum ab). Neben jeder eingecheckten Binärdatei liegt `<datei>.sha256` (prüfbar mit `sha256sum -c` im selben Ordner); erlaubte Binärdateien stehen in `config/security/binaries.allow`. Bibliotheken aus Repositories außerhalb von Google und Maven Central pinnt die **gezielte** Gradle Dependency Verification in `gradle/verification-metadata.xml` (AAR und POM; Abweichung = Build-Abbruch). Alle übrigen Gruppen sind dort per `trust`-Regel ausgenommen: Eine volle Verifikation aller Abhängigkeiten pflegt Dependabot nicht mit, jeder Update-PR fiele durch, und Google Maven und Maven Central liefern unveränderliche Releases. Die Gradle-Distribution ist per `distributionSha256Sum` gepinnt (`21bd5da2…89607`, veröffentlichter Digest von `gradle-9.8.2-bin.zip`).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-SUP-01 | MUSS | Repositories nur `google()`, `mavenCentral()`, `gradlePluginPortal()` (nur Plugins), jeweils mit Content-Filter (Google nur `com.android.*`, `com.google.*`, `androidx.*`; die anderen nie diese Android-Gruppen); `FAIL_ON_PROJECT_REPOS` bleibt. Jedes weitere Repo nur in `exclusiveContent` mit `includeGroup` und SHA-256-Pin je Gruppe in `gradle/verification-metadata.xml` – derzeit allein JitPack für `com.github.k2-fsa.sherpa-onnx`. Kein `mavenLocal`, kein `http`. | Dependency Confusion, veränderliche Artefakte. | auto: `sec-repos` |
| SEC-SUP-02 | MUSS | Build-Eingaben sind gepinnt: `distributionSha256Sum` im Wrapper; eingecheckte Binärartefakte nur nach Allowlist, jedes mit `<datei>.sha256` und Eintrag im Herkunftsnachweis; keine Bibliotheken als Datei im Repo. Bibliotheken aus Nicht-Standard-Repos mit SHA-256 in `gradle/verification-metadata.xml` und demselben Hash im Herkunftsnachweis. Updates nur vom Upstream-Release mit Abgleich des veröffentlichten Digests. | Manipulierte Distribution oder Bibliothek. | auto: `sec-pinned-inputs`; CI `wrapper-validation` |
| SEC-SUP-03 | SOLL | Dependabot (`gradle`, `github-actions`, monatlich, gruppiert) und Dependabot-Alerts sind an. Gebündelte native Komponenten (sherpa-onnx, onnxruntime, espeak-ng) werden monatlich gegen GHSA/OSV abgeglichen; Security-Fixes in Netz-, Parser- und Native-Bibliotheken kommen innerhalb von 30 Tagen. | Bekannte Schwachstellen; die Native-Bausteine im sherpa-onnx-AAR kennt Dependabot nicht, und ein Versionssprung von sherpa-onnx scheitert an der Prüfsumme, bis sie von Hand nachgezogen ist (Checkliste Schritt 13). | auto: `sec-actions` (`dependabot.yml`), `check-github`; manuell: Checkliste Schritt 14 |

<a id="build-signatur"></a>
## Build und Signatur

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-BUILD-01 | MUSS | Ohne Release-Key entsteht nur eine unsignierte APK (`app-release-unsigned.apk`, Gradle-Warnung; F-Droid baut ebenfalls unsigniert und übernimmt die Entwickler-Signatur), nie stillschweigend eine debug-signierte. Debug-Signatur nur mit `-PallowDebugSignedRelease=true` für Testbuilds. Der öffentliche `app/debug.keystore` signiert nie ein verteiltes Artefakt. Jede APK/AAB wird vor der Veröffentlichung gegen den Fingerprint geprüft. | Nutzer, F-Droid und Obtainium verifizieren gegen den Fingerprint. | auto: `apk-cert` (unsigniert oder `CN=Android Debug` = Fehler) |
| SEC-BUILD-02 | MUSS | Release- und Upload-Key liegen außerhalb des Repo-Baums, je mit mindestens zwei verschlüsselten Offline-Backups. Passwörter stehen im Passwortmanager und für den Build nur in einer Properties-Datei außerhalb des Repo-Baums (`chmod 600`; Standard `~/.config/atemkraft/keystore.properties` mit dem Keystore daneben, sonst Pfad per `ATEMKRAFT_KEYSTORE_PROPERTIES`), nie im Repo-Baum. Claude darf Keystores und Signatur-Passwörter weder lesen noch ausgeben (Deny-Regeln in `.claude/settings.json`). Ist-Stand 2026-10-24: Keystore und Properties liegen unter `~/.config/atemkraft/` (Ordner 700, Dateien 600). | Verlust heißt kein Update-Pfad; der Agent hat Shell-Zugriff. | auto: `sec-secrets`; manuell: Checkliste Schritt 15 |
| SEC-BUILD-03 | MUSS | Release-Härtung: `isMinifyEnabled = true`, nicht `debuggable`, kein `testOnly`, kein `usesCleartextTraffic`; `dependenciesInfo { includeInApk = false }`; nur genutzte Native Libs (`config/security/native-libs.allow`; ohne die C- und C++-API von sherpa-onnx). | Angriffsfläche; der Dependency-Info-Block blockiert F-Droid. | auto: `apk-hardening`, `sec-gradle-flags` |
| SEC-BUILD-04 | MUSS | Releases nur aus einem frischen Klon (`git clone`, **kein** `git worktree`: darin findet AGP kein Git und schreibt keine Revision, der Nachbau aus einem Klon wiche dann ab) des annotierten Tag-Commits, gebaut nach Commit und Tag; die Revision in `META-INF/version-control-info.textproto` ist der Tag-Commit. | Nachweis, dass das Asset aus dem Quellstand stammt. | auto: `apk-provenance` |
| SEC-BUILD-05 | MUSS | `versionCode` steigt um genau 1. Je Release entstehen drei APKs mit **demselben** `versionCode` (ABI-Splits, `app/build.gradle.kts`): die Universal-APK `atemkraft-vX.Y.Z.apk` (gegen sie vergleicht F-Droid über `Binaries:`, sie bleibt der Standard-Download) und `atemkraft-vX.Y.Z-arm64-v8a.apk` sowie `atemkraft-vX.Y.Z-armeabi-v7a.apk` als kleinere Downloads; dazu das App Bundle für Play (eigener Aufruf `bundleRelease`, ohne Splits). Kein ABI-Versatz im `versionCode`: Play erhält das Bundle und teilt selbst, und ein Wechsel zwischen Universal- und ABI-APK derselben Version ist mit gleicher Signatur ein normales Drüberinstallieren. Veröffentlichte Assets werden nie ersetzt, eine Korrektur ist eine neue Patch-Version. Die Release-Notes enthalten die SHA-256 jeder APK und einen Verweis auf den Fingerprint. | Eindeutige, prüfbare Releases. | auto: CI `release-verify` (Nachbau und Vergleich je APK); manuell: Checkliste Schritte 2, 7 |
| SEC-BUILD-06 | SOLL | Release-Builds sind mit JDK 17 reproduzierbar (`scripts/build.sh` bricht Release-Aufgaben mit anderem JDK ab); Native Libs werden unverändert aus dem AAR übernommen (`keepDebugSymbols`, kein NDK-abhängiges Strippen). CI baut jedes Release aus dem Tag nach und vergleicht mit dem Asset. Geprüft 2026-10-24: zwei saubere Builds (ohne Build-Cache, mit 2 und mit allen Gradle-Workern) ergaben bitgleiche APKs samt `assets/dexopt/baseline.prof`; das Baseline-Profil bleibt deshalb aktiv. | Voraussetzung für F-Droid mit Entwickler-Signatur. | auto: CI `release-verify` (Job `rebuild-compare`) |

**Notfallplan Release-Key:** Bei Kompromittierung neuen Key erzeugen, per `apksigner rotate --lineage` rotieren, neuen Fingerprint hier und im README veröffentlichen, Advisory anlegen. Bei Verlust ohne Backup bleibt nur eine neue `applicationId`; deshalb SEC-BUILD-02.

<a id="verteilung"></a>
## Verteilung: GitHub, Google Play, F-Droid

Grundsatz: **ein Signaturschlüssel für alle Kanäle.** So bleiben Installationen kanalübergreifend updatefähig, und niemand verliert beim Wechsel sein Logbuch.

Der einmalige Weg in die Stores (Konten, Formulare, Grafiken, F-Droid-Rezept, Reihenfolge) steht in [STORE.md](STORE.md).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-DIST-01 | MUSS | Play App Signing mit dem eigenen Release-Key (vor dem ersten Open-Testing- oder Produktions-Release hochladen) und separatem Upload-Key. F-Droid baut selbst aus dem Tag nach; Bibliotheken nur aus erlaubten Repositories (Maven Central, Google, JitPack für sherpa-onnx), keine Binärdateien im Quellbaum. Mit `Binaries:` (Universal-APK des GitHub-Release), `AllowedAPKSigningKeys: 75022def…1890` und bitgleichem Nachbau (SEC-BUILD-06) veröffentlicht F-Droid unsere signierte APK; `Binaries:` allein ersetzt den Build aus Quellen nicht. Das vorgebaute AAR von JitPack ist ein Rest-Risiko im Review (Präzedenzfall SherpaTTS, [STORE.md](STORE.md)). | Ein Store-eigener Key wäre inkompatibel zu den GitHub-APKs; F-Droid Inclusion Policy. | manuell: Store-Einreichung; auto: `sec-pinned-inputs` |
| SEC-DIST-02 | MUSS | Plattformpflichten: `targetSdk` erfüllt die Play-Frist; Native Libs sind 16-KB-kompatibel (APK und AAB); Paketname und Zertifikat sind bei der Android-Entwicklerverifizierung registriert. | Play-Pflichten; Installierbarkeit auf zertifizierten Geräten. | auto: Lint `ExpiredTargetSdkVersion`, `apk-16kb`; manuell: Checkliste Schritt 10 |
| SEC-DIST-03 | MUSS | Store-Angaben sind wahr und kommen aus einer Quelle (`fastlane/metadata/android/de-DE/`). Play-Formulare (Data safety „keine Daten erhoben“, Werbe-ID „Nein“, Zielgruppe 18+, Health-Deklaration, FGS-Deklaration, Datenschutz-URL) und F-Droid-Anti-Features stimmen mit diesem Dokument überein: *NonFreeAssets* (Hörproben von Miro und Dii unter CC BY-NC-ND 4.0 liegen in der APK, SEC-LEGAL-03) und *NonFreeNet* (optionaler Stimm-Download von GitHub; ob nötig, entscheidet der F-Droid-Prüfer). | Falsche Angaben führen zur Sperre. | auto: CI `release-verify` (Changelog vorhanden); manuell: Checkliste Schritte 10–11 |

<a id="repo"></a>
## Repo und GitHub

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-REPO-01 | MUSS | Nie im Repo: Keystores (außer `app/debug.keystore`), Passwörter, `keystore.properties`, `local.properties`, APK/AAB/`.idsig`, lokale Agent-Einstellungen. `.gitignore` deckt das per Muster ab (`*.jks`, `*.keystore`, `*.p12`, `**/keystore.properties`, `*.idsig`, `.claude/settings.local.json`). | Secret Scanning erkennt binäre Keystores nicht zuverlässig. | auto: `sec-gitignore`, `sec-pinned-inputs` |
| SEC-REPO-02 | MUSS | GitHub: 2FA, Secret Scanning, Push Protection und Private Vulnerability Reporting sind an. Rulesets: `main` ohne Force-Push und Löschen, Tags `v*` unveränderlich. | Account-Übernahme, Leaks, unveränderliche Release-Referenzen. | auto (lokal): `check-github` |
| SEC-REPO-03 | MUSS | CI-Workflows: `permissions: contents: read` als Default, Actions per 40-stelligem Commit-SHA gepinnt, `persist-credentials: false`. Keine Release-Signatur und keine Keystore-Secrets in CI. | Kompromittierte Actions, Key-Leck. | auto: `sec-actions` |

<a id="recht"></a>
## Gesundheit, Recht und Lizenzen

Atemkraft ist eine Wellness-App und **kein Medizinprodukt** (MDR 2017/745, Erwägungsgrund 19). Die Zweckbestimmung ergibt sich aus allen Texten: App, README, Store und Release-Notes. Ton und Wortwahl regelt [STYLEGUIDE.md, Text und Ton](STYLEGUIDE.md#text). Rechtliche Einordnungen sind keine Rechtsberatung.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-LEGAL-01 | DARF NICHT | Heil-, Therapie-, Linderungs- oder Präventionsversprechen in App, README, Store-Texten, Datenschutzerklärung oder Release-Notes. Wortliste: STYLEGUIDE TEXT-09, maschinenlesbar `config/health-claims.txt`; Ausnahmen (Verneinungen, Disclaimer) nur zeilengenau in `config/health-claims.allow`. | Sonst Medizinprodukt bzw. irreführende Werbung (HWG § 3); Play Health Policy. | auto: `ContentRulesTest health-claims` (Inhalte, `strings.xml` und die öffentlichen Texte), CI `release-verify` |
| SEC-LEGAL-02 | MUSS | Die Store-Beschreibung enthält: „Kein Medizinprodukt; diagnostiziert, behandelt, heilt oder verhindert keine Erkrankung. Für medizinischen Rat wende dich an Fachpersonal.“ Mess-, Diagnose- oder Auswertungsfunktionen mit gesundheitlicher Bewertung kommen erst nach einer MDR-Neubewertung in diesem Dokument; vor einer Monetarisierung wird der EU Cyber Resilience Act neu bewertet. | Play Health Content Policy; MDR Rule 11; CRA-FOSS-Ausnahme gilt nur ohne Monetarisierung. | auto: `ContentRulesTest health-claims` (Disclaimer wörtlich); manuell: Review |
| SEC-LEGAL-03 | MUSS | Nur FLOSS-Abhängigkeiten. Jede gebündelte oder ladbare Komponente und Stimme hat eine Lizenz, die Weiterverbreitung erlaubt, und steht mit ihr in den Credits (App und THIRD_PARTY_LICENSES.md; README, Store-Texte und PRIVACY.md verweisen darauf, ohne Stimmen einzeln aufzuzählen). Keine Stimmen aus urheberrechtlich geschütztem Material; NC-Lizenzen nur als deklarierte Ausnahme (Anti-Feature). Lizenztexte und NOTICE-Dateien liegen im APK; für GPL-Bestandteile ist der Upstream-Quellstand genannt. | Urheberrecht; Apache-2.0 § 4, GPL-3.0 § 6; F-Droid und Play. | auto: `apk-licenses`; manuell: Credits, Lizenz je Stimme im Katalog |

**Ist-Stand SEC-LEGAL-03 (2026-10-08, ergänzt 2026-10-24):** Lizenzliste in [THIRD_PARTY_LICENSES.md](../THIRD_PARTY_LICENSES.md), Lizenztexte und NOTICE in der APK unter `assets/licenses/`, Liste in der App unter *Über → Lizenzen*, Lizenz je Stimme im Katalog. GLaDOS ist seit 1.6.0 entfernt (Nutzerentscheidung 2026-10-08: keine Lizenzangabe, laut Autor aus Spieldateien von Valve trainiert), samt Hörprobe; ein schon geladenes Modell löscht die App beim Start (`deleteOrphanVoiceDirs`), eine gespeicherte Wahl verwirft sie. **Deklarierte Ausnahme NC:** Miro und Dii stehen unter CC BY-NC-ND 4.0 (nur nicht kommerziell). Nutzerentscheidung 2026-10-08: behalten, weil Atemkraft kostenlos und werbefrei ist; bei jeder kommerziellen Nutzung (Preis, Werbung, Spenden-Paywall, Verkauf) neu prüfen. Stimmenauswahl und Lizenzliste in der App zeigen „nur nicht kommerziell“ (`VoiceSpec.nonCommercialOnly`, abgeleitet aus der Lizenz); ihre Hörproben liegen in der APK. Für F-Droid bzw. IzzyOnDroid ist das Anti-Feature *NonFreeAssets* zu deklarieren (Entscheidung bestätigt 2026-10-24: Miro und Dii bleiben). **Basismodelle:** Kerstin ist aus „ryan“ feinjustiert (Trainingsdaten CC BY-NC-SA 4.0), Thorsten aus „lessac“ (Blizzard-2013-Daten unter einer Forschungslizenz); ob das auf die Stimme durchschlägt, ist offen. App und Lizenzliste nennen es seit 2026-10-24 offen in der Kurzlizenz („Daten CC0; Basismodell: Daten CC BY-NC-SA“ bzw. „…: Daten mit Forschungslizenz“); als „nur nicht kommerziell“ gekennzeichnet sind nur Stimmen, deren eigene Lizenz NC ist (`nonCommercialOnly` liest den Teil vor „;“). **Lizenz der APK (Entscheidung 2026-10-24):** Der Quelltext steht unter MIT; die APK als Ganzes wird unter GPL-3.0-or-later weitergegeben, weil sie eSpeak NG statisch gelinkt enthält (THIRD_PARTY_LICENSES.md, „GPL und die APK“). So steht es in Store-Texten und im F-Droid-Rezept (`License: GPL-3.0-or-later`, im Merge Request erläutert); die Quellen nennt jedes Release (GPL-3.0 § 6, Checkliste Schritt 7). GPL-3-Apps in Google Play sind verbreitet, rechtlich aber nicht unumstritten; das ist bewusst in Kauf genommen. Stimmen stehen nur in `VoiceCatalog.kt` und THIRD_PARTY_LICENSES.md (manuell abgeglichen); eine Stimme hinzufügen oder entfernen heißt: Katalog, Lizenzliste, Hörprobe.

<a id="automatische-pruefung"></a>
## Automatische Prüfung

Gemeinsame Infrastruktur (`scripts/check.sh`, `lint {}`, `.github/workflows/ci.yml`) spezifiziert der STYLEGUIDE; hier stehen die Sicherheits-Checks. Die statischen Checks (`sec-*`, Werkzeug **check-style**) stehen seit 2026-10-19 in `scripts/check_style.py` und laufen bei jedem `scripts/check.sh`, also auch im CI: gleiche Engine und Fixtures (`config/style-fixtures/sec-*.{pos,neg}`, `--self-test`) wie die Stil-Checks, aber mit Null-Toleranz ohne Baseline – ein Baseline-Eintrag für einen `sec-*`-Check ist selbst ein Fehler. Zeilen, die mit `//`, `/*` oder `*` beginnen, gelten als Kommentar; die Stil-Marker `// dekorativ:` und `// Abweichung <ID>:` heben einen `sec-*`-Treffer nicht auf (`--self-test` prüft das mit einer Gegenprobe je Fixture). Die Prüfung öffentlicher Texte auf Heilversprechen und den Store-Disclaimer übernimmt der JUnit-Test `ContentRulesTest` (`health-claims`, samt Disclaimer), weil er Wortliste und Ausnahmen schon für die App-Inhalte liest. `scripts/verify-apk.sh` läuft in `scripts/check.sh` gegen die unsignierten Prüf-APKs (mit `--allow-unsigned`) und in der Release-Checkliste gegen die signierten; der Workflow `.github/workflows/release-verify.yml` baut jeden Tag nach. Soll-Setup, noch nicht vorhanden: `check-github` sowie die mit **offen** markierten Teile.

```bash
scripts/check.sh                                     # Stil- und sec-*-Checks, ktlint, Tests, Lint
scripts/check-style.sh                               # nur Stil- und sec-*-Checks
scripts/verify-apk.sh <apk>… [--tag vX.Y.Z] [--allow-debug-cert] [--allow-unsigned]
scripts/check-github.sh                              # Soll: Repo-Einstellungen per gh api
```

| Check | Werkzeug | Regel | Logik |
|---|---|---|---|
| `sec-deny-deps` | check-style | SEC-PRIV-01 | `firebase\|crashlytics\|play-services\|\bgms\b\|admob\|analytics\|sentry\|bugsnag\|appcenter\|facebook` (ohne Groß-/Kleinschreibung) in `gradle/libs.versions.toml`, `**/*.gradle.kts` → 0 |
| `sec-net-endpoints` | check-style | SEC-PRIV-02, SEC-NET-02 | `https?://` in `app/src/main/java` nur in `VoiceCatalog.kt`, `AboutScreen.kt` |
| `sec-net-client` | check-style | SEC-NET-02 | `openConnection\(\|\bSocket\(\|\bokhttp\|\bretrofit\|\bktor\b` in `app/src/main/java` nur in `VoiceModelManager.kt` (Wortgrenzen, sonst trifft „Faktor“) |
| `sec-device-leaks` | check-style | SEC-PRIV-03 | `ANDROID_ID\|AdvertisingIdClient\|getDeviceId\|getImei\|Build\.SERIAL`, `\bLog\.[vdiwe]\(\|println\(\|printStackTrace\(\|Timber\.`, `ClipboardManager\|setPrimaryClip` in `app/src/main/java` → 0 |
| `sec-privacy-link` | check-style | SEC-PRIV-05 | `docs/PRIVACY.md` existiert; ihre URL steht in `AboutScreen.kt` |
| `sec-wakelock` | check-style | SEC-PERM-02 | `newWakeLock\(` nur mit `PARTIAL_WAKE_LOCK`; `\bacquire\(\s*\)` → 0 |
| `sec-tls` | check-style | SEC-NET-01 | `http://` in `app/src/main` (`*.kt`, `*.java`, `*.xml`; ohne `schemas.android.com` und ohne die wortgetreuen Lizenztexte unter `assets/licenses/`) → 0; `X509TrustManager\|HostnameVerifier\|SSLSocketFactory\|setDefaultSSL` → 0 |
| `sec-tls-config` | check-style | SEC-NET-01 | `res/xml/network_security_config.xml` mit `cleartextTrafficPermitted="false"` existiert und ist im Manifest referenziert |
| `sec-download` | check-style | SEC-NET-02 | jede Datei mit `openConnection` setzt `connectTimeout`, `readTimeout` und `"User-Agent"`. Die sichtbare Quelle vor dem Tap prüft nur das Review (es gibt keinen eigenen String dafür) |
| `sec-components` | check-style | SEC-PLAT-01 | `<receiver` im Quell-Manifest → 0; jede Zeile mit `registerReceiver\(` enthält `RECEIVER_NOT_EXPORTED` |
| `sec-intents` | check-style | SEC-PLAT-02 | `intent\.(data\|extras)\|get\w*Extra\(` → 0; `<data ` im Manifest → 0; `FLAG_MUTABLE` → 0 außer mit `// mutable:`-Begründung |
| `sec-no-dyncode` | check-style | SEC-PLAT-03 | `WebView\|DexClassLoader\|PathClassLoader\|InMemoryDexClassLoader\|System\.load\(` → 0 |
| `sec-storage` | check-style | SEC-STORE-01 | `getExternal\w*Dir\|getExternalStorage\|MediaStore\|MODE_WORLD_` → 0 |
| `sec-backup` | check-style | SEC-STORE-02 | `allowBackup="false"`; `backup_rules.xml` (API < 31) und `cloud-backup` schließen alle Domänen aus; `device-transfer` schließt `files/tts` aus |
| `sec-room` | check-style | SEC-STORE-03 | `fallbackToDestructiveMigration` → 0; jede `@Database` mit `exportSchema = true`, und `app/schemas/<paket>.<Klasse>/<version>.json` liegt im Repo; jede Datei mit `preferencesDataStore(` setzt `corruptionHandler` |
| `sec-repos` | check-style | SEC-SUP-01 | `settings.gradle.kts`: `mavenLocal\|http://` → 0; `FAIL_ON_PROJECT_REPOS` gesetzt; `google()`, `mavenCentral()`, `gradlePluginPortal()` ohne Block (also ohne Content-Filter) → 0; gibt es ein Nicht-Standard-Repo (`maven(…)`/`maven {…}`), dann je eines in `exclusiveContent` und jede `includeGroup` mit `sha256`-Eintrag in `gradle/verification-metadata.xml` |
| `sec-pinned-inputs` | check-style | SEC-SUP-02, SEC-DIST-01, SEC-REPO-01 | `distributionSha256Sum=[0-9a-f]{64}`; getrackte und neue `*.aar`, `*.jar`, `*.so`, `*.dex`, `*.jks`, `*.keystore`, `*.p12`, `*.apk`, `*.aab` ⊆ `config/security/binaries.allow`, dort mit `sha256` markierte haben eine `.sha256`-Datei; jede `.sha256` stimmt mit der Datei überein, und ihr Hash steht identisch im Herkunftsnachweis; ebenso jede in `gradle/verification-metadata.xml` gepinnte AAR/JAR |
| `sec-gradle-flags` | check-style | SEC-BUILD-03 | `includeInApk = false` in `app/build.gradle.kts` |
| `sec-secrets` | check-style | SEC-BUILD-02 | in getrackten und neuen, nicht ignorierten Dateien keine Zeile `^(store\|key)Password\s*[=:]`. Ignorierte Dateien (z. B. `keystore.properties` im Root) prüft der Check nicht; dass sie außerhalb des Repo-Baums liegen, sichert Checkliste Schritt 15. Die Deny-Regeln für `Read(**/keystore.properties)`, `Read(**/*.keystore)`, `Read(**/*.jks)` und `Read(~/.config/atemkraft/**)` stehen in `.claude/settings.json` (im Repo) |
| `sec-gitignore` | check-style | SEC-REPO-01 | `git check-ignore -q` greift (per Repo-`.gitignore`) für `x.jks`, `x.p12`, `sub/keystore.properties`, `x.idsig`, `.claude/settings.local.json` |
| `sec-actions` | check-style | SEC-REPO-03, SEC-SUP-03 | jedes `uses:` in `.github/workflows/*.yml` endet auf `@[0-9a-f]{40}`; `permissions:` auf oberster Ebene; `persist-credentials: false`; `.github/dependabot.yml` hat `package-ecosystem` `gradle` und `github-actions` mit `interval: monthly` und `groups:` |
| `health-claims` (öffentliche Texte) | JUnit `ContentRulesTest` | SEC-LEGAL-01 | Wortliste `config/health-claims.txt` (ohne Groß-/Kleinschreibung, Wortgrenzen) in `README.md`, `fastlane/metadata/**`, `docs/PRIVACY.md`, `docs/release-notes/*.md` → 0, außer Zeilen aus `config/health-claims.allow`; je Zeile ein Datensatz `datei:zeile`, ohne Baseline |
| `health-claims` (Disclaimer) | JUnit `ContentRulesTest` | SEC-LEGAL-02 | `full_description.txt` enthält den Disclaimer-Satz aus SEC-LEGAL-02 wörtlich (fester String) |
| `apk-cert` | verify-apk | SEC-BUILD-01 | `apksigner verify --print-certs`: Signer = `75022def…1890`; unsigniert ist ein Fehler (außer mit `--allow-unsigned` für Prüf-Builds in `check.sh` und im Tag-Nachbau), `CN=Android Debug` ebenso (`--allow-debug-cert` nur für CI-Testbuilds) |
| `apk-permissions` | verify-apk | SEC-PERM-01 | `aapt2 dump permissions` = `config/security/permissions.allow` |
| `apk-exported` | verify-apk | SEC-PLAT-01 | Komponenten mit `exported=true` ⊆ `config/security/exported.allow` |
| `apk-hardening` | verify-apk | SEC-BUILD-03, SEC-NET-01 | kein `debuggable`, `testOnly`, `usesCleartextTraffic=true` (`aapt2 dump xmltree`); kein Signing-Block-ID `0x504b4453`; `lib/*/*.so` ⊆ `native-libs.allow` |
| `apk-16kb` | verify-apk | SEC-DIST-02 | `zipalign -c -P 16 4`; ELF-Programmköpfe: jedes `LOAD`-Alignment ≥ 16384 |
| `apk-provenance` | verify-apk | SEC-BUILD-04 | mit `--tag`: Revision in `version-control-info.textproto` = Tag-Commit; `versionName` = Tag |
| `apk-licenses` | verify-apk | SEC-LEGAL-03 | jede Datei aus `app/src/main/assets/licenses/` liegt unter `assets/licenses/` in der APK |
| `apk-dex-deny` | verify-apk | SEC-PRIV-01 | `classes*.dex`: keine Typen `Lcom/google/android/gms/`, `Lcom/google/firebase/`, `L…/analytics/`, kein `crashlytics` |
| `check-github` | check-github | SEC-REPO-02, SEC-SUP-03 | `gh api`: Private Vulnerability Reporting, Secret Scanning, Push Protection, Dependabot-Alerts, Rulesets `main`/`v*` |
| `release-verify` | CI `.github/workflows/release-verify.yml` (Tag-Push `v*`, Release veröffentlicht, von Hand mit Tag) | SEC-BUILD-04, SEC-BUILD-05, SEC-BUILD-06 | Job `rebuild-compare`: Tag mit JDK 17 ohne Build-Cache unsigniert bauen, `verify-apk.sh --allow-unsigned --tag` auf den Nachbau, SHA-256 in die Job-Zusammenfassung; gibt es das Release schon: Assets `atemkraft-vX.Y.Z*.apk` laden, `verify-apk.sh --tag`, `apksigcopier compare --unsigned` je APK gegen den Nachbau. **Offen:** SHA-256 und Fingerprint im Release-Body, Body ohne Heilversprechen, Changelog vorhanden, `versionCode` = Vorgänger + 1 (bis dahin Checkliste Schritte 2, 7) |

<a id="release-checkliste"></a>
## Release-Checkliste

**Ist-Stand (2026-10-24):** Die statischen `sec-*`-Checks und `verify-apk` (an den unsignierten Prüf-APKs) laufen in `scripts/check.sh` (Schritt 1); der Workflow `release-verify` baut jeden Tag nach. `scripts/check-github.sh` ist Soll-Setup; Schritt 14 nennt den manuellen Ersatz. Bis v1.5.1 waren die Tags nicht annotiert, und der Release-Commit enthielt die Versionsänderung selbst.

1. `git status` leer, `main` gepusht, `scripts/check.sh` grün.
2. `versionCode` +1, `versionName` gesetzt; Release-Notes `docs/release-notes/vX.Y.Z.md` und Changelog `fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt` geschrieben (SEC-LEGAL-01). Die Notes-Datei enthält nur die Änderungen; SHA-256 und Fingerprint kommen in Schritt 7 dazu. Versionsangaben nachziehen: „Unterstützte Version“ in diesem Dokument, der Stand in `THIRD_PARTY_LICENSES.md` und Markierungen „ab Version X.Y.Z“ in der Datenschutzerklärung. Inhalte nachziehen: `fastlane/metadata/android/de-DE/full_description.txt` (ausführliche Funktionsliste, höchstens 4000 Zeichen), die kurze Funktionsliste im README (je Bereich ein Satz, ohne Versionsmarkierungen; Einzelheiten stehen im Store-Text, Berechtigungen in der Datenschutzerklärung, Toolchain in CONTRIBUTING), Datenschutzerklärung samt Berechtigungstabelle gegen den Code, bei sichtbaren Änderungen die zwei README-Bilder `docs/screenshot-atmen.png` und `docs/screenshot-sitzung.png` (aus den Roborazzi-Bildern `kompakt360/fs100` von `recordRoborazziDebug`: `01_atmen` oben 720 × 1560 px zugeschnitten und auf 432 × 936 px verkleinert, `09_session_einatmen` ganz auf 432 px Breite; beide auf 256 Farben reduziert).
3. Release-Commit `vX.Y.Z: …` und annotierter Tag `git tag -a vX.Y.Z` gepusht.
4. Im frischen Klon des Tags mit JDK 17 bauen: `git clone --branch vX.Y.Z . ../atemkraft-rel` (kein `git worktree`, SEC-BUILD-04), dort mit `ATEMKRAFT_KEYSTORE_PROPERTIES=<pfad>` (`storeFile` absolut oder relativ zur Properties-Datei) `scripts/build.sh clean assembleRelease` (drei APKs: `app-universal-release.apk`, `app-arm64-v8a-release.apk`, `app-armeabi-v7a-release.apk`) und für Play getrennt `scripts/build.sh bundleRelease`; Signatur-Geheimnisse nach SEC-BUILD-02. Fehlt der Key, entstehen nur unsignierte APKs (Warnung), die Schritt 5 abfängt.
5. `scripts/verify-apk.sh app/build/outputs/apk/release/*-release.apk --tag vX.Y.Z` grün (alle drei APKs). Es prüft unter anderem, dass `apksigner verify --print-certs` `75022def…1890` zeigt, und **bricht bei unsignierter APK oder `CN=Android Debug` ab**. Von Hand: `$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs <apk>` muss `Signer #1 certificate DN: CN=grenzenloseSchublade, O=Atemkraft` und `Signer #1 certificate SHA-256 digest: 75022defe65aa24e223f3e79880f09f423456757002ff0b33ba2ae29b9f41890` zeigen.
6. Auf einem Gerät als Update über die Vorversion installieren: kein Signaturkonflikt, Logbuch bleibt, kurzer Test von Atem-Session, Meditation und Stimme. Die neuronale Stimme einmal in der Release-APK anhören: Der Release-Build in `check.sh` prüft R8 und Shrinking nur beim Bauen, einen JNI-Bruch zur Laufzeit findet erst das Gerät.
7. APKs benennen (SEC-BUILD-05): `app-universal-release.apk` → `atemkraft-vX.Y.Z.apk`, `app-arm64-v8a-release.apk` → `atemkraft-vX.Y.Z-arm64-v8a.apk`, `app-armeabi-v7a-release.apk` → `atemkraft-vX.Y.Z-armeabi-v7a.apk`. Release-Body als eigene Datei außerhalb des Repo-Baums = Notes-Datei + Block mit SHA-256 je APK (`sha256sum atemkraft-vX.Y.Z*.apk`, Zeile „SHA-256 der APK:“ wie bei v1.5.1, dazu ein Satz: Die Universal-APK passt auf jedes Gerät, die beiden anderen sind kleiner und nur für 64- bzw. 32-Bit-ARM) und Verweis auf den Fingerprint ([Echtheit einer APK prüfen](#echtheit-einer-apk-prüfen)), dazu ein Satz zu Quellcode und Lizenzen neben der APK (GPL-3.0 § 6): Quellcode dieses Releases ist der Tag `vX.Y.Z`, der Quellstand der Native-Bibliotheken steht in [THIRD_PARTY_LICENSES.md](../THIRD_PARTY_LICENSES.md#quellcode).
8. `gh release create vX.Y.Z atemkraft-vX.Y.Z.apk atemkraft-vX.Y.Z-arm64-v8a.apk atemkraft-vX.Y.Z-armeabi-v7a.apk --title … --notes-file <datei>`. Ein Asset wird nie ersetzt (SEC-BUILD-05).
9. CI `release-verify` grün (startet beim Veröffentlichen des Releases; vergleicht den Nachbau mit jedem Asset). Sonst manuell: Assets mit `gh release download vX.Y.Z` laden, `sha256sum` mit dem Release-Body vergleichen, `scripts/verify-apk.sh --tag vX.Y.Z` darauf.
10. Play (sobald aktiv): `scripts/build.sh bundleRelease`, 16-KB-Alignment per `bundletool dump config` prüfen, mit Upload-Key hochladen; Formulare gegen SEC-DIST-03 prüfen.
11. F-Droid (sobald aktiv): `rebuild-compare` grün oder lokal `apksigcopier compare --unsigned atemkraft-vX.Y.Z.apk app-universal-release-unsigned.apk` gegen einen Rebuild im sauberen Klon; Rezept (`Binaries:`, `output:`, `AllowedAPKSigningKeys:`, Anti-Features) aktuell.
12. Klon `../atemkraft-rel` entfernen, lokale APK/AAB löschen.
13. Bei neuem sherpa-onnx-Release: Version in `gradle/libs.versions.toml`, beide Prüfsummen in `gradle/verification-metadata.xml` (AAR vorher mit dem GitHub-Release-Asset vergleichen) und den Herkunftsnachweis im selben Commit aktualisieren; mit `readelf -dW` und `strings` erneut prüfen, dass die JNI-Lib nur `libonnxruntime.so` braucht und welche Klassen sie per JNI anspricht (`app/proguard-rules.pro`).
14. Monatlich: Dependabot-PRs und -Alerts sichten, native Komponenten gegen GHSA/OSV abgleichen, `scripts/check-github.sh` ausführen.
15. Jährlich: Release- und Upload-Keystore testweise aus dem Backup wiederherstellen.
