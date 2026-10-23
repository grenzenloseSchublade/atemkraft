# Veröffentlichung in Google Play und F-Droid

Stand: 2026-10-23, App-Version 2.5.0. Dieses Dokument beschreibt, wie Atemkraft in die Stores kommt: was im Repo vorbereitet sein muss, welche Konten und Formulare nötig sind und in welcher Reihenfolge man vorgeht. Die verbindlichen Regeln (Signatur, Lizenzen, Gesundheitsaussagen) stehen in [SECURITY.md](SECURITY.md), Abschnitte *Build und Signatur*, *Verteilung* und *Gesundheit, Recht und Lizenzen*; die Schritte jedes einzelnen Releases in dessen *Release-Checkliste*. Hier steht der einmalige Weg in die Stores und das, was sich danach je Release ändert.

Quellen sind die offiziellen Hilfeseiten von Google, F-Droid und IzzyOnDroid (Links im Text, abgerufen am 2026-10-23). Store-Regeln ändern sich; vor jedem Schritt die verlinkte Seite noch einmal lesen. Keine Rechtsberatung.

## Überblick

| Kanal | Status | Wer baut und signiert | Dauer bis online | Hauptaufwand |
|---|---|---|---|---|
| GitHub-Releases | aktiv bis v1.6.8, seitdem pausiert | wir | sofort | Push, Tag, Release |
| Google Play | geplant | wir (AAB), Google verteilt | etwa 4–6 Wochen | Konto, 14 Tage geschlossener Test mit 12 Testern, Formulare, Grafiken |
| F-Droid (Hauptrepo) | geplant | F-Droid baut aus dem Tag nach, veröffentlicht unsere signierte APK | Wochen bis Monate | Bibliothek von JitPack statt `app/libs`, reproduzierbarer Build, Rezept in fdroiddata |
| IzzyOnDroid | **zurückgestellt** | wir (GitHub-APK) | Tage | Ausschlussregel für KI-generierten Code, APK über 30 MB |

**Strategie:** ein Signaturschlüssel und eine `applicationId` (`app.atemkraft`) für alle Kanäle. Nutzer können so zwischen GitHub, Play und F-Droid wechseln, ohne zu deinstallieren. Das ist wichtig, weil es bewusst kein Cloud-Backup gibt: Eine Deinstallation löscht Logbuch und Muster (außer sie wurden exportiert). Außerdem muss bei der Android-Entwicklerverifizierung nur ein Fingerprint registriert werden.

## Offene Entscheidungen

Vor dem Start muss der Nutzer entscheiden:

1. **Release-Politik wieder öffnen.** Seit 2026-10-19 wird nur lokal versioniert; v1.7.0 bis v2.5.0 sind weder gepusht noch getaggt. F-Droid baut ausschließlich aus öffentlichen Tags, Play braucht das zwar nicht, aber Quelltext und Store-Version sollen übereinstimmen (GPL-Bestandteile, siehe unten). Ohne Push und Tag geht nur Play.
2. **IzzyOnDroid.** Die Aufnahmeregeln lehnen Apps ab, deren Code ganz oder teilweise mit generativer KI entstanden ist („Vibe-coded apps will be rejected“, [Inclusion Policy, Abschnitt AI](https://izzyondroid.org/docs/general/AppInclusionPolicy/)). Atemkraft wird offen mit Claude entwickelt (Co-Authored-By in den Commits). Verschweigen widerspräche deren Regeln. Empfehlung: IzzyOnDroid nicht einreichen; wer die GitHub-APKs automatisch aktualisieren will, nutzt [Obtainium](https://obtainium.imranr.dev/).
3. **Lizenzangabe.** Der Quelltext steht unter MIT. Die APK enthält über sherpa-onnx statisch gelinktes eSpeak NG (GPL-3.0-or-later) und ist als Ganzes GPL-3.0. Das steht so in [THIRD_PARTY_LICENSES.md](../THIRD_PARTY_LICENSES.md) und muss in Store-Text und F-Droid-Rezept genauso ehrlich stehen. GPL-3-Apps in Play sind verbreitet (Signal, Termux), juristisch aber nicht unumstritten; das ist eine bewusste Entscheidung, die in SEC-LEGAL-03 festgehalten wird.
4. **Nicht-freie Stimmen.** Miro und Dii stehen unter CC BY-NC-ND 4.0, ihre Hörproben liegen in der APK. In F-Droid führt das zum Warnhinweis *NonFreeAssets*. Alternativ beide Stimmen samt Hörproben entfernen. Kerstin beruht auf einem Basismodell mit CC-BY-NC-SA-Trainingsdaten, Thorsten auf einem mit Forschungslizenz; die Kennzeichnung in der App ist dafür zu ergänzen.
5. **Öffentliche Kontaktdaten.** Ein privates Play-Konto zeigt **gesetzlichen Namen, Land und Entwickler-E-Mail** öffentlich an ([Hilfe 13628312](https://support.google.com/googleplay/android-developer/answer/13628312)). Für Store und Datenschutzerklärung eine eigene E-Mail-Adresse oder einen Alias nur für die App anlegen, nicht die Uni- oder Privatadresse.

## Im Repo vorbereiten

Alles, was beide Stores aus dem Repo lesen, liegt unter `fastlane/metadata/android/` (Fastlane-Struktur). F-Droid liest es automatisch; in Play lädt man dieselben Dateien von Hand oder per `fastlane supply` hoch. Eine Quelle für alle Kanäle (SEC-DIST-03).

### Texte

| Datei | Grenze | Stand |
|---|---|---|
| `de-DE/title.txt` | 30 Zeichen (Play) | vorhanden |
| `de-DE/short_description.txt` | 80 Zeichen | vorhanden (71); sollte Nutzen statt Funktionsliste sagen |
| `de-DE/full_description.txt` | 4000 Zeichen | vorhanden (3988, fast voll); enthält den Disclaimer aus SEC-LEGAL-02 wörtlich |
| `de-DE/changelogs/<versionCode>.txt` | 500 Zeichen | je Release |
| `en-US/title.txt`, `short_description.txt`, `full_description.txt` | wie oben | **fehlt**; F-Droid fällt für alle anderen Sprachen auf en-US zurück. Kurz halten und sagen, dass die App nur Deutsch spricht |

### Grafiken (fehlen alle)

| Datei | Format |
|---|---|
| `de-DE/images/icon.png` | 512 × 512 px, 32-Bit-PNG mit Alpha, höchstens 1 MB. Aus dem adaptiven Launcher-Icon rendern |
| `de-DE/images/featureGraphic.png` | 1024 × 500 px, JPEG oder 24-Bit-PNG **ohne** Alpha. Play-Pflicht, F-Droid zeigt es als Kopfbild |
| `de-DE/images/phoneScreenshots/1.png` … | Play: mindestens 2, höchstens 8; Seiten 320–3840 px, Seitenverhältnis höchstens 2:1, ohne Alpha. Für Empfehlungen in Play mindestens 4 im Hochformat ab 1080 × 1920 px |

Die Roborazzi-Bilder (720 × 1560 px) sind dafür zu klein und zu schmal. Besser echte Screenshots vom Gerät oder Roborazzi in höherer Auflösung mit Seitenverhältnis 9:16 rendern. `en-US/images/` kann auf dieselben Dateien verweisen (Kopie).

### Build

- **sherpa-onnx von JitPack statt aus `app/libs/`.** Der F-Droid-Scanner lehnt `.aar`-Dateien im Quellbaum ab. JitPack liefert dieselbe Datei byte-identisch (SHA-256 am 2026-10-23 verglichen) und steht auf der Positivliste des Scanners; die TTS-App SherpaTTS ist so in F-Droid. Einbinden als AAR-Koordinate (`com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.6@aar`), JitPack in `settings.gradle.kts` per `content {}` auf `com.github.k2-fsa` beschränken, Prüfsumme per Gradle Dependency Verification (`gradle/verification-metadata.xml`) statt `.sha256`-Datei pinnen. SEC-SUP-02 und `config/security/binaries.allow` anpassen.
- **Reproduzierbarer Build** (SEC-BUILD-06). F-Droid baut den Tag nach und veröffentlicht unsere APK nur, wenn sie bis auf die Signatur gleich ist. Bekannte Stolpersteine: Baseline-Profile (`assets/dexopt/baseline.prof`) können je CPU-Kernzahl abweichen; R8 und Ressourcen-Shrinking mehrfach bauen und vergleichen; immer JDK 17 (`scripts/build.sh` soll bei anderem JDK abbrechen, nicht nur warnen). Werkzeuge: `apksigcopier compare`, `diffoscope`. Der CI-Job `rebuild-compare` sollte vor der Einreichung grün sein.
- **App Bundle für Play:** `scripts/build.sh bundleRelease`. Play erzeugt daraus pro Gerät passende APKs; die 56-MB-APK fällt dort nicht ins Gewicht.
- **ABI-Splits für GitHub** (optional): eine APK je Architektur plus Universal-APK; versionCode-Schema dann in SEC-BUILD-05 festlegen.

### Dokumente

- **Datenschutzerklärung** (`docs/PRIVACY.md`): Play verlangt eine öffentliche, nicht geofence-geschützte URL (kein PDF), die auch in der App verlinkt ist. Die GitHub-Ansicht genügt; besser ein stabiler Link (GitHub Pages oder Tag statt `main`). Ergänzen: Verteilung über Play und F-Droid, Hinweis, dass Google bzw. F-Droid als Store-Betreiber eigene Daten verarbeiten, Kontakt-E-Mail, Anbietername wie im Store. Historische Hinweise auf Versionen bis 1.5.1 in einen Anhang.
- **README**, Abschnitt *Installieren*: „nicht im Play Store“ ersetzen durch die Bezugsquellen und den Hinweis, dass alle dieselbe Signatur tragen.
- **SECURITY.md**: SEC-DIST-01 präzisieren (F-Droid braucht einen Build aus Quellen, `Binaries:` allein reicht nicht); fehlende Prüfskripte (`verify-apk.sh`, Allowlists) anlegen oder die Verweise streichen.

## Google Play

### 1. Konto anlegen

- Privates Konto („Personal“), **25 US$ einmalig**, ab 18 ([Hilfe 6112435](https://support.google.com/googleplay/android-developer/answer/6112435)).
- Identität: Google-Payments-Profil verknüpfen, amtlichen Lichtbildausweis hochladen (Name muss exakt passen), E-Mail und Telefon per Code bestätigen ([10841920](https://support.google.com/googleplay/android-developer/answer/10841920)).
- Geräteprüfung: Mit der Play-Console-App auf einem echten, nicht gerooteten Android-Telefon bestätigen ([14316361](https://support.google.com/googleplay/android-developer/answer/14316361)). Das A54 reicht.
- Entwicklername darf „grenzenloseSchublade“ sein; der gesetzliche Name erscheint trotzdem im Entwicklerprofil.
- **DSA-Händlerstatus:** Die Console fragt, ob man als Händler im Sinne des Digital Services Act auftritt. Eine kostenlose App ohne Werbung, Käufe oder Spenden spricht für „kein Händler“. Bei jeder Monetarisierung neu prüfen (dann wären Adresse und Telefon öffentlich). Den Dialog in der Console selbst lesen.

Konto früh anlegen: Die Prüfungen dauern einige Tage.

### 2. App anlegen und Signatur festlegen

**Vor dem ersten Upload:** Neue Apps bekommen sonst automatisch einen von Google erzeugten Signaturschlüssel, und unsere GitHub- und F-Droid-APKs passen nicht mehr dazu.

1. App anlegen, Standardsprache Deutsch.
2. *Play App Signing → Change the app signing key → Provide a copy of your app signing key*: unseren Release-Schlüssel (Fingerprint `75022def…1890`) mit dem PEPK-Tool verschlüsselt hochladen ([9842756](https://support.google.com/googleplay/android-developer/answer/9842756)). Das geht nur, bevor ein Release in Open Testing oder Produktion ausgerollt ist; am sichersten vor dem allerersten AAB.
3. Einen **separaten Upload-Schlüssel** erzeugen und registrieren. Geht er verloren, kann Google ihn zurücksetzen; den App-Signaturschlüssel nicht.

Der Upload-Schlüssel liegt wie der Release-Schlüssel außerhalb des Repos (SEC-BUILD-02) und kommt in den Notfallplan.

### 3. Formulare unter „App-Inhalte“

| Formular | Angabe für Atemkraft |
|---|---|
| Datenschutzerklärung | URL der PRIVACY.md (siehe oben) |
| Datensicherheit | „Keine Daten erhoben, keine Daten geteilt.“ Daten, die das Gerät nicht verlassen, zählen nicht. Der freiwillige Stimmen-Download geht an GitHub; wir erhalten nichts, er ist in der Datenschutzerklärung offengelegt (SEC-DIST-03) |
| Werbe-ID | Nein |
| Werbung | Keine Werbung |
| App-Zugriff | Alle Funktionen ohne Anmeldung |
| Altersfreigabe (IARC) | Fragebogen ehrlich ausfüllen; erwartbar USK 0 / PEGI 3 |
| Zielgruppe | 18+ (Atemanhalten und intensive Übungen sind für Kinder ungeeignet; unter 13 griffe die Families-Policy) |
| Health-Apps-Erklärung | Pflicht für alle Apps. Kategorie *Health and fitness → Stress Management, Relaxation, Mental Acuity*; **nicht** „Medical“ oder „Medical Device“ ([14738291](https://support.google.com/googleplay/android-developer/answer/14738291)) |
| Foreground-Service | Typ `mediaPlayback` begründen, **mit Video** ([13392821](https://support.google.com/googleplay/android-developer/answer/13392821)): 30–60 s, Session starten, Ton läuft, Bildschirm aus, Gong und Phasentöne laufen weiter, Benachrichtigung sichtbar. Als nicht gelistetes Video hochladen |
| Nachrichten, Behörden, Finanzen | jeweils Nein |

Kategorie im Store: *Gesundheit & Fitness*.

### 4. Geschlossener Test (Pflicht für neue private Konten)

- Mindestens **12 Tester**, die **14 Tage am Stück** im geschlossenen Test angemeldet sind ([14151465](https://support.google.com/googleplay/android-developer/answer/14151465)). Interner Test zählt nicht.
- Lieber 15–20 Tester einladen, die die App wirklich öffnen; fällt die Zahl unter 12, beginnen die 14 Tage nach Berichten neu.
- Tester über eine Google-Gruppe oder eine E-Mail-Liste einladen. Feedback mitschreiben: Der Antrag fragt danach, wie Tester gewonnen wurden, was sie rückgemeldet haben und was geändert wurde.

### 5. Produktion

- Antrag auf Produktionszugang stellen; Prüfung meist bis 7 Tage.
- Danach Produktions-Release; die erste Prüfung kann noch einmal einige Tage dauern.
- Jedes weitere Release: `bundleRelease`, 16-KB-Ausrichtung prüfen (`bundletool dump config`), mit Upload-Schlüssel hochladen, Changelog aus `changelogs/<versionCode>.txt` (Release-Checkliste Schritt 10).

## F-Droid

### Voraussetzungen

- Nur freie Software, keine Tracker, keine Play-Dienste (erfüllt, SEC-PRIV-01). Nicht-funktionale Assets dürfen unfreier sein, müssen aber als Anti-Feature deklariert werden ([Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/)).
- Abhängigkeiten aus vertrauenswürdigen Repos (Maven Central, Google, JitPack) oder aus Quellen gebaut; keine Binärdateien im Repo. Deshalb JitPack statt `app/libs/` (siehe oben).
- Öffentlicher, getaggter Quellstand (`v<versionName>`) und Fastlane-Metadaten im Repo.
- Etwa drei Monate aktive Entwicklung werden erwartet; Atemkraft ist seit 2026-07 in Arbeit und seit 2026-08-24 öffentlich.

### Signatur: unsere APK, nicht die von F-Droid

Standardmäßig signiert F-Droid mit eigenem Schlüssel; dann wäre die F-Droid-Version nicht mit Play und GitHub kompatibel. Mit reproduzierbarem Build und den Feldern `Binaries:` und `AllowedAPKSigningKeys:` baut F-Droid aus dem Tag nach, vergleicht mit unserer GitHub-APK und veröffentlicht **unsere** signierte APK. Weicht der Nachbau ab, überspringt F-Droid diese Version ([Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/)). Deshalb muss jede Version reproduzierbar sein (SEC-BUILD-06).

### Ablauf

1. GitHub-Release mit APK für den Tag erstellen (Release-Checkliste).
2. `gitlab.com/fdroid/fdroiddata` forken, Branch `app.atemkraft`, Datei `metadata/app.atemkraft.yml` anlegen.
3. Lokal im fdroidserver-Container prüfen: `fdroid readmeta`, `fdroid rewritemeta app.atemkraft`, `fdroid lint app.atemkraft`, `fdroid build app.atemkraft`.
4. Pushen, CI abwarten, Merge Request „New App: Atemkraft“ öffnen, Rückfragen zügig beantworten ([Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)).
5. Nach dem Merge erscheint die App in etwa 24–48 Stunden. Bis zum Merge vergehen bei neuen Apps oft Wochen bis Monate (ehrenamtliche Prüfer).
6. Danach erkennt F-Droid neue Tags selbst (`UpdateCheckMode: Tags`); Updates erscheinen meist innerhalb von 1–2 Wochen.

Skizze des Rezepts (ungetestet, im Merge Request anpassen):

```yaml
Categories:
  - Sports & Health
License: GPL-3.0-or-later   # APK als Ganzes wegen eSpeak NG; Quelltext MIT, im MR erläutern
SourceCode: https://github.com/grenzenloseSchublade/atemkraft
IssueTracker: https://github.com/grenzenloseSchublade/atemkraft/issues
Changelog: https://github.com/grenzenloseSchublade/atemkraft/releases

AntiFeatures:
  NonFreeAssets:
    de: Zwei Stimmen (Miro, Dii) und ihre Hörproben stehen unter CC BY-NC-ND 4.0.
  NonFreeNet:
    de: Lädt auf Wunsch Stimmen von GitHub herunter.

RepoType: git
Repo: https://github.com/grenzenloseSchublade/atemkraft.git
Binaries: https://github.com/grenzenloseSchublade/atemkraft/releases/download/v%v/atemkraft-v%v.apk

Builds:
  - versionName: 2.5.0
    versionCode: 37
    commit: v2.5.0
    subdir: app
    gradle:
      - yes

AllowedAPKSigningKeys: 75022defe65aa24e223f3e79880f09f423456757002ff0b33ba2ae29b9f41890

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 2.5.0
CurrentVersionCode: 37
```

Ob *NonFreeNet* für einen optionalen Download von GitHub nötig ist, entscheidet der Prüfer; die TTS-App SherpaTTS trägt es für ihren Download von Hugging Face.

## Android-Entwicklerverifizierung

Google verlangt schrittweise, dass auch Apps außerhalb von Play einem verifizierten Entwickler zugeordnet sind ([developer-verification](https://developer.android.com/developer-verification)): seit 2026-09-30 in Brasilien, Indonesien, Singapur und Thailand, **weltweit ab 2027** (genaues Datum offen). Unregistrierte Apps lassen sich dann nur noch über einen umständlichen „Advanced flow“ installieren.

- Mit dem Play-Konto ist das Konto bereits verifiziert.
- Paketname `app.atemkraft` mit dem Fingerprint des Signaturschlüssels registrieren. Mit Play App Signing geschieht das automatisch; in der Console prüfen. Weil GitHub, Play und F-Droid dieselbe Signatur tragen, sind damit alle Kanäle abgedeckt.
- Würde F-Droid doch mit eigenem Schlüssel signieren, müsste dessen Fingerprint zusätzlich eingetragen werden.

## Reihenfolge

1. Entscheidungen oben treffen (Release-Politik, IzzyOnDroid, Lizenzangabe, Stimmen, Kontakt-E-Mail).
2. Play-Konto anlegen (läuft parallel, dauert Tage).
3. Repo vorbereiten: Grafiken, en-US-Texte, JitPack statt AAR, reproduzierbarer Build, Datenschutzerklärung, README, SECURITY.md; Ergebnisse des großen Reviews einarbeiten.
4. Release v2.x.y nach der Release-Checkliste: Push, annotierter Tag, GitHub-Release mit APK.
5. Play: App anlegen, **zuerst** den eigenen Signaturschlüssel hochladen, Upload-Schlüssel anlegen, Formulare, FGS-Video, AAB in den geschlossenen Test.
6. 14 Tage geschlossener Test mit 15–20 Testern; parallel den F-Droid-Merge-Request einreichen (dessen Wartezeit überlappt).
7. Antrag auf Produktionszugang, dann Produktions-Release.
8. Registrierung bei der Entwicklerverifizierung prüfen.

## Je Release danach

Die Release-Checkliste in SECURITY.md gilt weiter. Zusätzlich:

- Changelog `fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt` (höchstens 500 Zeichen) wird in Play und F-Droid angezeigt.
- Play: AAB hochladen (Schritt 10 der Checkliste); F-Droid holt den neuen Tag selbst und veröffentlicht nur, wenn der Nachbau übereinstimmt. Schlägt der Vergleich fehl, Ursache beheben und eine neue Patch-Version taggen; ein Asset wird nie ersetzt (SEC-BUILD-05).
- Bei geänderten Bildschirmen die Store-Screenshots erneuern.
