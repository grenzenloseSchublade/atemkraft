# Atemkraft – Sicherheit und Datenschutz

Sicherheits-, Datenschutz-, Lieferketten- und Release-Regeln für Atemkraft (`app.atemkraft`). Zielgruppe: Maintainer und Claude. Gegenstück für Design, Text und Code: [STYLEGUIDE.md](STYLEGUIDE.md).

- **Stand:** 2026-10-01 (v1.5.1).
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
| Unterstützte Version | nur die jeweils neueste Release-Version (derzeit 1.5.x), keine Backports |
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

Atemkraft ist **offline-first**: kein Konto, kein Server, keine Telemetrie, keine Werbung. Alle Nutzerdaten bleiben auf dem Gerät. Einzige Ausnahme ist der optionale, per Tap ausgelöste Stimm-Download ([Netzwerk](#netzwerk)). Es gibt **keine Cloud-Anbindung**, auch kein Android-Cloud-Backup (Nutzerentscheidung 2026-10-05, Datenhoheit). Daten verlassen das Gerät nur auf ausdrücklichen Wunsch: als Export-Datei an einen selbst gewählten Ort oder per lokaler Gerät-zu-Gerät-Übertragung beim Handywechsel ([Speicherung](#speicherung)).

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-PRIV-01 | DARF NICHT | Analytics-, Crash-Reporting-, Werbe- oder Tracking-SDKs, Google Play Services oder Firebase einbinden, auch nicht transitiv. | Offline-Versprechen; F-Droid-Anti-Feature *Tracking*. | auto: `sec-deny-deps`, `apk-dex-deny` |
| SEC-PRIV-02 | MUSS | Einzige Netzverbindung der App ist der nutzerinitiierte Stimm-Download. Externe Links sind konstante `https://`-URLs, die der Browser öffnet. Jede neue Verbindung steht im selben Commit in der Endpunkt-Tabelle, im README und in der Datenschutzerklärung. | Transparenz; Play Data safety bleibt wahr. | auto: `sec-net-endpoints`; manuell: Review |
| SEC-PRIV-03 | MUSS | Keine Abflüsse auf dem Gerät: keine Gerätekennungen (Android ID, Werbe-ID, IMEI, Seriennummer), kein Logging in `app/src/main` (Ausnahme: hinter `BuildConfig.DEBUG`, ohne Nutzerdaten), keine Zwischenablage ohne Nutzeraktion, auf dem Sperrbildschirm nur generischer Status (nie Übungsnamen oder Logbuchdaten). | Logcat, Zwischenablage und Sperrbildschirm sind für Dritte sichtbar. | auto: `sec-device-leaks`; manuell: Review der Notifications |
| SEC-PRIV-04 | MUSS | Jedes persistierte Feld steht im Daten-Inventar (Zeile im selben Commit). Nutzer können ihre Daten in der App löschen, ohne Reste; Löschaktionen sind gegen Fehltipp geschützt (STYLEGUIDE MUSTER-05). | Datenminimierung und Nutzerkontrolle nachweisbar. | manuell: Review bei Diff an `data/local/*`, `SettingsRepository.kt` |
| SEC-PRIV-05 | MUSS | Texte zur Datennutzung (App, README, Store) sind wahr und vollständig („offline, außer optionalem Stimm-Download“). Eine Datenschutzerklärung liegt unter `docs/PRIVACY.md` und ist im Über-Screen verlinkt. | Play-Pflicht auch ohne Datenerhebung; DSGVO Art. 13. | auto: `sec-privacy-link`; manuell: Inhalt |
| SEC-PRIV-06 | MUSS | Freitext-Eingaben (heute nur die Befindens-Suche im Situationen-Tab) sind flüchtig: nur `rememberSaveable` (Activity-Zustand für Drehen und Prozess-Neustart), nie DataStore, Room, Datei oder Log; sie verfallen beim Schließen der Suche. Die Tastatur bekommt `autoCorrectEnabled = false` und `IME_FLAG_NO_PERSONALIZED_LEARNING` (über `InterceptPlatformTextInput` im `SearchField`), damit Tastatur-Apps Befindens-Wörter nicht lernen oder synchronisieren. | Eingaben zum Befinden sind gesundheitsnah; Drittanbieter-Tastaturen lernen und synchronisieren sonst Wörter. Das Flag ist eine Bitte an die Tastatur, kein Schutz vor einer bösartigen. | auto: JUnit `SituationsScreenTest` (EditorInfo des Feldes), `sec-device-leaks`; manuell: Review bei neuen Textfeldern |

**Daten-Inventar (v1.5.1)**

| Speicherort | Inhalt | Sensitivität | Gerät-zu-Gerät / Export | Löschweg |
|---|---|---|---|---|
| Room `log_entries` | Übung, Zeitpunkt, Dauer, Runden | gesundheitsnah, niedrig (Übungs-IDs können auf Erkrankungen hindeuten) | ja (ab Android 12) / nein | „Logbuch leeren“, App-Speicher löschen |
| Room `saved_patterns` | Name und Phasenlängen eigener Muster | niedrig | ja (ab Android 12) / ja (mit Anpassung) | einzeln löschen |
| DataStore `settings` | Einstellungen, Anpassungen pro Übung, Aufklapp-Zustand „Meine Muster“ (`saved_patterns_expanded`) | niedrig | ja (ab Android 12) / nur Anpassungen gespeicherter Muster | zurücksetzen pro Übung, App-Speicher löschen |
| `files/tts/` | Stimmmodelle (öffentliche Daten, bis ca. 130 MB) | keine | nein / nein | „Stimme löschen“ |
| `cache/` | Teil-Download | keine | nein / nein | beim nächsten Download-Versuch; Android leert `cache/` bei Speichermangel |
| Activity-Zustand (flüchtig, `rememberSaveable`) | Suchtext der Befindens-Suche (SEC-PRIV-06) | gesundheitsnah | nein / nein | Suche schließen (X, Zurück, „Alle zeigen“), App beenden |

Export-Datei (`atemkraft-muster-<datum>.json`, Format `atemkraft-muster` v1): Name, Phasenlängen, Zeitpunkt und Anpassung gespeicherter Muster; Ort wählt der Nutzer über das Storage Access Framework, die App behält keinen Zugriff. Der Import übernimmt nur, was die App selbst erzeugen könnte (Leitplanken des Generators, Stepper-Grenzen, Namen ohne Steuer-/Format-Zeichen, höchstens 512 KiB und 500 Muster). Gespeichert werden keine Messwerte, keine Freitexte (auch nicht die Befindens-Suche) und keine Identität. Logbuch-Daten werden vorsorglich wie Gesundheitsdaten behandelt.

<a id="berechtigungen"></a>
## Berechtigungen

Vollständige Allowlist für das gemergte Release-Manifest (`config/security/permissions.allow`):

| Berechtigung | Zweck |
|---|---|
| `INTERNET` | nur der optionale Stimm-Download |
| `VIBRATE` | Haptik bei Phasenwechseln |
| `WAKE_LOCK` | Meditations-Timer im Doze-Modus (`PARTIAL_WAKE_LOCK`) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Meditation läuft bei gesperrtem Bildschirm weiter |
| `POST_NOTIFICATIONS` | Status der Meditation; bei Ablehnung läuft der Timer weiter |
| `app.atemkraft.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | automatisch von androidx.core, signature-geschützt, nur intern |
| `<queries>` `TTS_SERVICE` | Sichtbarkeit der System-Sprachausgabe; sie erhält nur feste Ansagetexte |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-PERM-01 | MUSS | Das gemergte Release-Manifest enthält genau die Berechtigungen der Allowlist. Eine neue Berechtigung braucht im selben Commit Manifest-Kommentar, README-Eintrag und Allowlist-Zeile (bei Bibliotheken mit Herkunft). Verboten sind u. a. `QUERY_ALL_PACKAGES`, `AD_ID`, Speicher-, Standort-, Kamera-, Mikrofon-, Kontakt- und `REQUEST_INSTALL_PACKAGES`-Berechtigungen. | Minimalprinzip; Bibliotheken schleppen Berechtigungen ein. | auto: `apk-permissions`; Lint `QueryAllPackagesPermission` |
| SEC-PERM-02 | MUSS | Hintergrundarbeit minimal: Foreground Services mit kleinstem passendem Typ (Begründung im Manifest-Kommentar), Wakelocks nur `PARTIAL_WAKE_LOCK` mit Timeout und Freigabe bei Pause und Ende. Runtime-Berechtigungen werden im Kontext abgefragt. Bei jedem `targetSdk`-Bump werden FGS- und Behavior-Changes geprüft. | Akku, Play-FGS-Deklaration. | auto: `sec-wakelock`; manuell: Review |

<a id="netzwerk"></a>
## Netzwerk und Downloads

| Zweck | Ziel | Auslöser | Übertragene Daten | Empfänger |
|---|---|---|---|---|
| Stimm-Download | `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/<stimme>.tar.bz2` (Weiterleitung auf `release-assets.githubusercontent.com`) | nur expliziter Tap | IP-Adresse, User-Agent, Zeitpunkt, Dateiname | GitHub / Microsoft (USA) |
| Quellcode-Link | `https://github.com/grenzenloseSchublade/atemkraft` | Tap, öffnet externen Browser | – (kein App-Traffic) | – |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-NET-01 | MUSS | Nur HTTPS: Cleartext per `network_security_config.xml` verboten, nur System-Trust-Anchors. Keine eigenen `X509TrustManager`, `HostnameVerifier` oder `SSLSocketFactory`; kein Zertifikats-Pinning auf Fremd-Hosts (Integrität sichert SEC-NET-03). | Abhören und Manipulation; Fremd-Hosts rotieren Zertifikate. | auto: `sec-tls`, `apk-hardening`; Lint `TrustAllX509TrustManager`, `BadHostnameVerifier` |
| SEC-NET-02 | MUSS | Downloads nur nach explizitem Tap, nie automatisch oder im Hintergrund. Die URL besteht aus der Konstanten-Basis plus Stimm-ID aus dem Code-Katalog. Quelle und Größe sind vor dem Tap sichtbar. Eigener User-Agent ohne Gerätedaten, Timeouts ≤ 30 s. | F-Droid-Opt-in-Regel, DSGVO Art. 13; keine datengesteuerten Ziele. | auto: `sec-net-endpoints`, `sec-download`; JUnit `VoiceCatalogTest` |
| SEC-NET-03 | MUSS | Jede Stimme ist im Katalog mit SHA-256 und Größe gepinnt. Der Download wird beim Streamen gehasht und **vor** dem Entpacken verglichen; bei Abweichung wird er gelöscht. Ändert sich ein Asset upstream, ist das ein Katalog-Update im Code. | Modelle werden von nativem Code geparst. | auto: JUnit `VoiceCatalogTest`, `VoiceIntegrityTest` |
| SEC-NET-04 | MUSS | Archive werden in einen Staging-Ordner je Stimme entpackt. Jeder Zielpfad liegt darin, erlaubt sind nur reguläre Dateien und Ordner, Größe und Eintragszahl sind begrenzt. Installiert wird atomar (Staging-Ordner, dann Umbenennen); Reste werden beim App-Start entfernt. | Path Traversal, Archiv-Bomben, halbe Installationen. | auto: JUnit `VoiceIntegrityTest` |

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
| SEC-STORE-02 | MUSS | **Kein Cloud-Backup:** `android:allowBackup="false"` und in `data_extraction_rules.xml` schließt `cloud-backup` jede Domäne aus. Erlaubt ist nur `device-transfer` (lokal, ab Android 12; darunter schaltet `allowBackup="false"` auch sie ab); große oder neu ladbare Daten (`files/tts`) sind dort ausgeschlossen. Persistenz über eine Neuinstallation nur per Export-Datei (SEC-STORE-01); der Import behandelt die Datei als nicht vertrauenswürdig (Größe, Anzahl, Plausibilität begrenzt). | Datenhoheit: gesundheitsnahe Daten verlassen das Gerät nicht ohne ausdrückliche Wahl. | auto: `sec-backup`; Lint `DataExtractionRules`; JUnit `PatternBackupTest` |
| SEC-STORE-03 | MUSS | Room-Schemaänderungen nur mit expliziter `Migration` und Migrationstest, Schema exportiert nach `app/schemas/`; `fallbackToDestructiveMigration*` ist verboten. DataStore hat einen `corruptionHandler`. | Das Logbuch ist Nutzerhistorie. | auto: `sec-room`; JUnit Migrationstest |

<a id="lieferkette"></a>
## Lieferkette

**Herkunftsnachweis gebündelter Binärartefakte**

| Datei | SHA-256 | Herkunft | Enthaltene Komponenten |
|---|---|---|---|
| `app/libs/sherpa-onnx-1.13.6.aar` | `0012d9a28f15bd6fb966b62b70a75da3990512fdccce28b83098248ce4be1698` | Release `v1.13.6` von `k2-fsa/sherpa-onnx`, byte-identisch mit dem GitHub-Asset | sherpa-onnx (Apache-2.0), onnxruntime (MIT), espeak-ng (GPL-3.0-or-later) |
| `gradle/wrapper/gradle-wrapper.jar` | `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046` | offizieller Gradle-8.11.1-Wrapper | – |

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-SUP-01 | MUSS | Repositories nur `google()`, `mavenCentral()`, `gradlePluginPortal()` (nur Plugins), jeweils mit Content-Filter; `FAIL_ON_PROJECT_REPOS` bleibt. Jedes weitere Repo (z. B. JitPack) nur mit `includeGroup` und SHA-256-Pin in `gradle/verification-metadata.xml`. Kein `mavenLocal`, kein `http`. | Dependency Confusion, veränderliche Artefakte. | auto: `sec-repos` |
| SEC-SUP-02 | MUSS | Build-Eingaben sind gepinnt: `distributionSha256Sum` im Wrapper; eingecheckte Binärartefakte nur nach Allowlist, jedes mit `<datei>.sha256` und Eintrag im Herkunftsnachweis. Updates nur vom Upstream-Release mit Abgleich des veröffentlichten Digests. | Manipulierte Distribution oder Bibliothek. | auto: `sec-pinned-inputs`; CI `wrapper-validation` |
| SEC-SUP-03 | SOLL | Dependabot (`gradle`, `github-actions`, monatlich, gruppiert) und Dependabot-Alerts sind an. Gebündelte native Komponenten (sherpa-onnx, onnxruntime, espeak-ng) werden monatlich gegen GHSA/OSV abgeglichen; Security-Fixes in Netz-, Parser- und Native-Bibliotheken kommen innerhalb von 30 Tagen. | Bekannte Schwachstellen; das AAR erfasst Dependabot nicht. | auto: `sec-actions` (`dependabot.yml`), `check-github`; manuell: Checkliste Schritt 14 |

<a id="build-signatur"></a>
## Build und Signatur

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-BUILD-01 | MUSS | Ohne Release-Key entsteht nur eine unsignierte APK (`app-release-unsigned.apk`, Gradle-Warnung; F-Droid baut ebenfalls unsigniert und übernimmt die Entwickler-Signatur), nie stillschweigend eine debug-signierte. Debug-Signatur nur mit `-PallowDebugSignedRelease=true` für Testbuilds. Der öffentliche `app/debug.keystore` signiert nie ein verteiltes Artefakt. Jede APK/AAB wird vor der Veröffentlichung gegen den Fingerprint geprüft. | Nutzer, F-Droid und Obtainium verifizieren gegen den Fingerprint. | auto: `apk-cert` (unsigniert oder `CN=Android Debug` = Fehler) |
| SEC-BUILD-02 | MUSS | Release- und Upload-Key liegen außerhalb des Repo-Baums, je mit mindestens zwei verschlüsselten Offline-Backups. Passwörter stehen im Passwortmanager und für den Build nur in einer Properties-Datei außerhalb des Repo-Baums (`chmod 600`, Pfad per `ATEMKRAFT_KEYSTORE_PROPERTIES`), nie im Repo-Baum. Claude darf Keystores und Signatur-Passwörter weder lesen noch ausgeben (Deny-Regeln in `.claude/settings.json`). | Verlust heißt kein Update-Pfad; der Agent hat Shell-Zugriff. | auto: `sec-secrets`; manuell: Checkliste Schritt 15 |
| SEC-BUILD-03 | MUSS | Release-Härtung: `isMinifyEnabled = true`, nicht `debuggable`, kein `testOnly`, kein `usesCleartextTraffic`; `dependenciesInfo { includeInApk = false }`; nur genutzte Native Libs (`config/security/native-libs.allow`). | Angriffsfläche; der Dependency-Info-Block blockiert F-Droid. | auto: `apk-hardening`, `sec-gradle-flags` |
| SEC-BUILD-04 | MUSS | Releases nur aus einem frischen Checkout (`git worktree`) des annotierten Tag-Commits, gebaut nach Commit und Tag; die Revision in `META-INF/version-control-info.textproto` ist der Tag-Commit. | Nachweis, dass das Asset aus dem Quellstand stammt. | auto: `apk-provenance` |
| SEC-BUILD-05 | MUSS | `versionCode` steigt um genau 1. Veröffentlichte Assets werden nie ersetzt, eine Korrektur ist eine neue Patch-Version. Die Release-Notes enthalten die SHA-256 jeder APK und einen Verweis auf den Fingerprint. | Eindeutige, prüfbare Releases. | auto: CI `release-verify` |
| SEC-BUILD-06 | SOLL | Release-Builds sind mit JDK 17 reproduzierbar; CI baut jedes Release aus dem Tag nach und vergleicht mit dem Asset. | Voraussetzung für F-Droid mit Entwickler-Signatur. | auto: CI `rebuild-compare` |

**Notfallplan Release-Key:** Bei Kompromittierung neuen Key erzeugen, per `apksigner rotate --lineage` rotieren, neuen Fingerprint hier und im README veröffentlichen, Advisory anlegen. Bei Verlust ohne Backup bleibt nur eine neue `applicationId`; deshalb SEC-BUILD-02.

<a id="verteilung"></a>
## Verteilung: GitHub, Google Play, F-Droid

Grundsatz: **ein Signaturschlüssel für alle Kanäle.** So bleiben Installationen kanalübergreifend updatefähig, und niemand verliert beim Wechsel sein Logbuch.

| ID | Stufe | Regel | Warum | Prüfung |
|---|---|---|---|---|
| SEC-DIST-01 | MUSS | Play App Signing mit dem eigenen Release-Key (vor dem ersten Open-Testing- oder Produktions-Release hochladen) und separatem Upload-Key. F-Droid: reproduzierbarer Build mit `Binaries:` (GitHub-Asset) und `AllowedAPKSigningKeys: 75022def…1890`, ohne eingecheckte Binärartefakte im Build. | Ein Store-eigener Key wäre inkompatibel zu den GitHub-APKs; F-Droid Inclusion Policy. | manuell: Store-Einreichung; auto: `sec-pinned-inputs` |
| SEC-DIST-02 | MUSS | Plattformpflichten: `targetSdk` erfüllt die Play-Frist; Native Libs sind 16-KB-kompatibel (APK und AAB); Paketname und Zertifikat sind bei der Android-Entwicklerverifizierung registriert. | Play-Pflichten; Installierbarkeit auf zertifizierten Geräten. | auto: Lint `ExpiredTargetSdkVersion`, `apk-16kb`; manuell: Checkliste Schritt 10 |
| SEC-DIST-03 | MUSS | Store-Angaben sind wahr und kommen aus einer Quelle (`fastlane/metadata/android/de-DE/`). Play-Formulare (Data safety „keine Daten erhoben“, Werbe-ID „Nein“, Zielgruppe 18+, Health-Deklaration, FGS-Deklaration, Datenschutz-URL) und F-Droid-Anti-Features (z. B. *NonFreeNet* für den Stimm-Download) stimmen mit diesem Dokument überein. | Falsche Angaben führen zur Sperre. | auto: CI `release-verify` (Changelog vorhanden); manuell: Checkliste Schritte 10–11 |

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
| SEC-LEGAL-01 | DARF NICHT | Heil-, Therapie-, Linderungs- oder Präventionsversprechen in App, README, Store-Texten, Datenschutzerklärung oder Release-Notes. Wortliste: STYLEGUIDE TEXT-09, maschinenlesbar `config/health-claims.txt`; Ausnahmen (Verneinungen, Disclaimer) nur zeilengenau in `config/health-claims.allow`. | Sonst Medizinprodukt bzw. irreführende Werbung (HWG § 3); Play Health Policy. | auto: `ContentRulesTest`, `sec-health-claims`, CI `release-verify` |
| SEC-LEGAL-02 | MUSS | Die Store-Beschreibung enthält: „Kein Medizinprodukt; diagnostiziert, behandelt, heilt oder verhindert keine Erkrankung. Für medizinischen Rat wende dich an Fachpersonal.“ Mess-, Diagnose- oder Auswertungsfunktionen mit gesundheitlicher Bewertung kommen erst nach einer MDR-Neubewertung in diesem Dokument; vor einer Monetarisierung wird der EU Cyber Resilience Act neu bewertet. | Play Health Content Policy; MDR Rule 11; CRA-FOSS-Ausnahme gilt nur ohne Monetarisierung. | auto: `sec-store-disclaimer`; manuell: Review |
| SEC-LEGAL-03 | MUSS | Nur FLOSS-Abhängigkeiten. Jede gebündelte oder ladbare Komponente und Stimme hat eine Lizenz, die Weiterverbreitung erlaubt, und steht mit ihr in den Credits (App und README). Keine Stimmen aus urheberrechtlich geschütztem Material; NC-Lizenzen nur als deklarierte Ausnahme (Anti-Feature). Lizenztexte und NOTICE-Dateien liegen im APK; für GPL-Bestandteile ist der Upstream-Quellstand genannt. | Urheberrecht; Apache-2.0 § 4, GPL-3.0 § 6; F-Droid und Play. | auto: JUnit `VoiceCatalogTest` (Lizenzfeld), `apk-licenses`; manuell: Credits |

<a id="automatische-pruefung"></a>
## Automatische Prüfung

Soll-Setup. Gemeinsame Infrastruktur (`scripts/check.sh`, `lint {}`, `.github/workflows/ci.yml`) spezifiziert der STYLEGUIDE; hier stehen die Sicherheits-Checks. Statische Checks laufen mit Null-Toleranz ohne Baseline; Zeilen, die mit `//`, `/*` oder `*` beginnen, gelten als Kommentar.

```bash
scripts/check.sh                                     # ruft scripts/check-security.sh auf
scripts/verify-apk.sh <apk> [--tag vX.Y.Z] [--allow-debug-cert]
scripts/check-github.sh                              # Repo-Einstellungen per gh api
```

| Check | Werkzeug | Regel | Logik |
|---|---|---|---|
| `sec-deny-deps` | check-security | SEC-PRIV-01 | `firebase\|crashlytics\|play-services\|gms\|admob\|analytics\|sentry\|bugsnag\|appcenter\|facebook` in `gradle/libs.versions.toml`, `**/*.gradle.kts` → 0 |
| `sec-net-endpoints` | check-security | SEC-PRIV-02, SEC-NET-02 | `https?://` in `app/src/main/java` nur in `VoiceCatalog.kt`, `AboutScreen.kt`; `openConnection\(\|Socket\(\|okhttp\|retrofit\|ktor` nur in `VoiceModelManager.kt` |
| `sec-device-leaks` | check-security | SEC-PRIV-03 | `ANDROID_ID\|AdvertisingIdClient\|getDeviceId\|getImei\|Build\.SERIAL`, `\bLog\.[vdiwe]\(\|println\(\|printStackTrace\(\|Timber\.`, `ClipboardManager\|setPrimaryClip` in `app/src/main/java` → 0 |
| `sec-privacy-link` | check-security | SEC-PRIV-05 | `docs/PRIVACY.md` existiert; ihre URL steht in `AboutScreen.kt` |
| `sec-wakelock` | check-security | SEC-PERM-02 | `newWakeLock\(` nur mit `PARTIAL_WAKE_LOCK`; `\bacquire\(\s*\)` → 0 |
| `sec-tls` | check-security | SEC-NET-01 | `http://` in `app/src/main` (ohne `xmlns`/`schemas.android.com`) → 0; `X509TrustManager\|HostnameVerifier\|SSLSocketFactory\|setDefaultSSL` → 0; `network_security_config.xml` mit `cleartextTrafficPermitted="false"` existiert und ist im Manifest referenziert |
| `sec-download` | check-security | SEC-NET-02 | jede Datei mit `openConnection` setzt `connectTimeout`, `readTimeout` und `User-Agent`; `strings.xml` hat `settings_voice_source` mit „GitHub“ |
| `sec-components` | check-security | SEC-PLAT-01 | `<receiver` im Quell-Manifest → 0; jede Zeile mit `registerReceiver\(` enthält `RECEIVER_NOT_EXPORTED` |
| `sec-intents` | check-security | SEC-PLAT-02 | `intent\.(data\|extras)\|get\w*Extra\(` → 0; `<data ` im Manifest → 0; `FLAG_MUTABLE` → 0 außer mit `// mutable:`-Begründung |
| `sec-no-dyncode` | check-security | SEC-PLAT-03 | `WebView\|DexClassLoader\|PathClassLoader\|InMemoryDexClassLoader\|System\.load\(` → 0 |
| `sec-storage` | check-security | SEC-STORE-01 | `getExternal\w*Dir\|getExternalStorage\|MediaStore\|MODE_WORLD_` → 0 |
| `sec-backup` | check-security | SEC-STORE-02 | `allowBackup="false"`; `backup_rules.xml` (API < 31) und `cloud-backup` schließen alle Domänen aus; `device-transfer` schließt `files/tts` aus |
| `sec-room` | check-security | SEC-STORE-03 | `fallbackToDestructiveMigration` → 0; `exportSchema = true`; `app/schemas/` versioniert; `corruptionHandler` gesetzt |
| `sec-repos` | check-security | SEC-SUP-01 | `settings.gradle.kts`: `mavenLocal\|http://` → 0; jedes Repo hat `content {`; jede `includeGroup` eines Nicht-Standard-Repos hat einen `sha256`-Eintrag in `gradle/verification-metadata.xml` |
| `sec-pinned-inputs` | check-security | SEC-SUP-02, SEC-DIST-01, SEC-REPO-01 | `distributionSha256Sum=[0-9a-f]{64}`; `git ls-files '*.aar' '*.jar' '*.so' '*.dex' '*.jks' '*.keystore' '*.p12' '*.apk' '*.aab'` ⊆ Allowlist; `sha256sum -c` jeder `.sha256`; Hash steht identisch im Herkunftsnachweis |
| `sec-gradle-flags` | check-security | SEC-BUILD-03 | `includeInApk = false` in `app/build.gradle.kts` |
| `sec-secrets` | check-security | SEC-BUILD-02 | im Repo-Baum inkl. ungetrackter Dateien keine Zeile `^(store\|key)Password=`; `.claude/settings.json` verbietet `Read(**/keystore.properties)`, `Read(**/*.keystore)`, `Read(**/*.jks)` |
| `sec-gitignore` | check-security | SEC-REPO-01 | `git check-ignore -q` greift (per Repo-`.gitignore`) für `x.jks`, `x.p12`, `sub/keystore.properties`, `x.idsig`, `.claude/settings.local.json` |
| `sec-actions` | check-security | SEC-REPO-03, SEC-SUP-03 | jedes `uses:` in `.github/workflows/*.yml` endet auf `@[0-9a-f]{40}`; `permissions:` auf oberster Ebene; `persist-credentials: false`; `.github/dependabot.yml` hat `package-ecosystem` `gradle` und `github-actions` mit `interval: monthly` und `groups:` |
| `sec-health-claims` | check-security | SEC-LEGAL-01 | Wortliste `config/health-claims.txt` (ohne Groß-/Kleinschreibung, Wortgrenzen) in `README.md`, `fastlane/metadata/**`, `docs/PRIVACY.md`, `docs/release-notes/*.md` → 0, außer Zeilen aus `config/health-claims.allow` |
| `sec-store-disclaimer` | check-security | SEC-LEGAL-02 | `full_description.txt` enthält den Disclaimer-Satz aus SEC-LEGAL-02 wörtlich (fester String) |
| `apk-cert` | verify-apk | SEC-BUILD-01 | `apksigner verify --print-certs`: Signer = `75022def…1890`; unsigniert ist immer ein Fehler, `CN=Android Debug` ebenso (`--allow-debug-cert` nur für CI-Testbuilds) |
| `apk-permissions` | verify-apk | SEC-PERM-01 | `aapt2 dump permissions` = `config/security/permissions.allow` |
| `apk-exported` | verify-apk | SEC-PLAT-01 | Komponenten mit `exported=true` ⊆ `config/security/exported.allow` |
| `apk-hardening` | verify-apk | SEC-BUILD-03, SEC-NET-01 | kein `debuggable`, `testOnly`, `usesCleartextTraffic=true`; kein Signing-Block-ID `0x504b4453`; `lib/*/*.so` ⊆ `native-libs.allow` |
| `apk-16kb` | verify-apk | SEC-DIST-02 | `zipalign -c -P 16 4`; `readelf -lW`: jedes `LOAD`-Alignment ≥ 16384 |
| `apk-provenance` | verify-apk | SEC-BUILD-04 | mit `--tag`: Revision in `version-control-info.textproto` = Tag-Commit; `versionName` = Tag |
| `apk-licenses` | verify-apk | SEC-LEGAL-03 | Lizenztexte und NOTICE-Dateien unter `assets/licenses/` |
| `apk-dex-deny` | verify-apk | SEC-PRIV-01 | `dexdump`: keine Klassen unter `com/google/android/gms/`, `com/google/firebase/`, `crashlytics`, `/analytics/` |
| `check-github` | check-github | SEC-REPO-02, SEC-SUP-03 | `gh api`: Private Vulnerability Reporting, Secret Scanning, Push Protection, Dependabot-Alerts, Rulesets `main`/`v*` |
| `VoiceCatalogTest`, `VoiceIntegrityTest` | JUnit | SEC-NET-02, SEC-NET-03, SEC-NET-04, SEC-LEGAL-03 | Katalog: HTTPS-Basis, SHA-256 (64 Hex), Größe > 0, Lizenz mit Lizenztext. Integrität: Hash- und Größen-Abweichung, `../`-Pfad, Symlink- und Hardlink-Eintrag, Größen- und Eintragslimit |
| `release-verify` | CI (`on: release`) | SEC-BUILD-04, SEC-BUILD-05, SEC-BUILD-06, SEC-LEGAL-01, SEC-DIST-03 | Job `verify`: `verify-apk.sh --tag`, SHA-256 und Fingerprint im Release-Body, Body ohne Heilversprechen, Changelog vorhanden, `versionCode` = Vorgänger + 1. Job `rebuild-compare`: Tag mit JDK 17 bauen, `apksigcopier compare` gegen das Asset |

<a id="release-checkliste"></a>
## Release-Checkliste

1. `git status` leer, `main` gepusht, `scripts/check.sh` grün.
2. `versionCode` +1, `versionName` gesetzt; Release-Notes `docs/release-notes/vX.Y.Z.md` und Changelog `fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt` geschrieben (SEC-LEGAL-01).
3. Release-Commit `vX.Y.Z: …` und annotierter Tag `git tag -a vX.Y.Z` gepusht.
4. Im frischen Worktree des Tags mit JDK 17 bauen: `git worktree add ../atemkraft-rel vX.Y.Z`, dort mit `ATEMKRAFT_KEYSTORE_PROPERTIES=<pfad>` (`storeFile` absolut oder relativ zur Properties-Datei) `scripts/build.sh clean assembleRelease`; Signatur-Geheimnisse nach SEC-BUILD-02. Fehlt der Key, entsteht nur eine unsignierte APK (Warnung), die Schritt 5 abfängt.
5. `scripts/verify-apk.sh <apk> --tag vX.Y.Z` grün. Mindestens: `apksigner verify --print-certs` zeigt `75022def…1890`, **Abbruch bei unsignierter APK oder `CN=Android Debug`**.
6. Auf einem Gerät als Update über die Vorversion installieren: kein Signaturkonflikt, Logbuch bleibt, kurzer Test von Atem-Session, Meditation und Stimme.
7. APK als `atemkraft-vX.Y.Z.apk` benennen; Release-Body = Notes-Datei + Block mit SHA-256 und Fingerprint-Verweis.
8. `gh release create vX.Y.Z atemkraft-vX.Y.Z.apk --title … --notes-file <datei>`. Ein Asset wird nie ersetzt (SEC-BUILD-05).
9. CI `release-verify` grün, sonst manuell: Asset laden, Hash vergleichen.
10. Play (sobald aktiv): `scripts/build.sh bundleRelease`, 16-KB-Alignment per `bundletool dump config` prüfen, mit Upload-Key hochladen; Formulare gegen SEC-DIST-03 prüfen.
11. F-Droid (sobald aktiv): `rebuild-compare` grün oder lokal `apksigcopier compare` gegen einen Rebuild im sauberen Klon; Rezept (`Binaries:`, `AllowedAPKSigningKeys:`, Anti-Features) aktuell.
12. Worktree entfernen, lokale APK/AAB löschen.
13. Bei neuem AAR: Herkunftsnachweis und `.sha256` im selben Commit aktualisieren.
14. Monatlich: Dependabot-PRs und -Alerts sichten, native Komponenten gegen GHSA/OSV abgleichen, `scripts/check-github.sh` ausführen.
15. Jährlich: Release- und Upload-Keystore testweise aus dem Backup wiederherstellen.
