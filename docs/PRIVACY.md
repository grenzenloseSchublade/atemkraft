# Datenschutzerklärung Atemkraft

Für die Android-App Atemkraft (`app.atemkraft`), wie sie über dieses Repo und seine GitHub-Releases verteilt wird.

- **Stand:** 2026-10-07, geprüft gegen den Quellcode auf `main` an diesem Tag. Wo sich die veröffentlichte Version 1.5.1 davon unterscheidet, steht es dabei ([Stimm-Download](#netz)).
- **Anbieter:** grenzenloseSchublade, Maintainer von [github.com/grenzenloseSchublade/atemkraft](https://github.com/grenzenloseSchublade/atemkraft). Kontakt siehe [unten](#kontakt).

## Kurzfassung

Atemkraft erhebt keine Daten über dich. Es gibt kein Konto, keinen Server des Anbieters, keine Analyse, keine Werbung, keine Absturzberichte und kein Tracking; die App enthält keine solchen Bibliotheken und keine Google Play Services. Was du in der App tust, bleibt auf deinem Gerät. Die einzige Netzverbindung der App ist der Download einer Stimme, und den löst nur ein Tippen von dir aus.

## Was auf deinem Gerät gespeichert wird

Alles liegt im privaten Speicher der App, den andere Apps nicht lesen können. Der Anbieter erhält davon nichts.

| Ort | Inhalt | So löschst du es |
|---|---|---|
| Logbuch (Datenbank) | abgeschlossene Sitzungen: Übung bzw. Meditation, Zeitpunkt, Dauer, Runden | „Logbuch leeren“ im Logbuch; App-Speicher löschen |
| Eigene Muster (Datenbank) | Name und Phasenlängen gespeicherter Muster, Zeitpunkt des Speicherns | einzeln löschen unter „Meine Muster“ |
| Einstellungen | Ton, Haptik, Hinweise, Meditation, gewählte Stimme, Anpassungen pro Übung, ob „Meine Muster“ aufgeklappt ist | pro Übung zurücksetzen; App-Speicher löschen |
| Stimmen | heruntergeladene Stimmmodelle (öffentliche Dateien, Download je ca. 25–110 MB) | „Stimme löschen“ in den Einstellungen |
| Zwischenspeicher | ein abgebrochener oder laufender Download | wird beim nächsten Versuch ersetzt; Android leert ihn bei Speichermangel |

Die Befindens-Suche im Situationen-Tab wird nicht gespeichert: Der Suchtext lebt nur, solange die Suche offen ist, und die App bittet die Tastatur, deine Eingaben nicht zu lernen. Ob eine Tastatur-App dieser Bitte folgt, liegt bei ihr.

Das Logbuch kann Rückschlüsse auf dein Befinden zulassen, weil manche Übungen zu bestimmten Anlässen passen. Die App gibt es deshalb nur auf deinen ausdrücklichen Wunsch weiter (siehe [Handywechsel](#wechsel)). Beim Deinstallieren löscht Android alle Daten der App.

<a id="netz"></a>
## Stimm-Download – die einzige Netzverbindung

Wenn du in den Einstellungen bei einer Stimme auf „Laden“ tippst, lädt die App ein Archiv von GitHub:

| Punkt | Inhalt |
|---|---|
| Adresse | `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/<stimme>.tar.bz2`, von dort weitergeleitet zu `release-assets.githubusercontent.com` |
| Auslöser | nur dein Tippen auf „Laden“; nie automatisch oder im Hintergrund |
| Was GitHub dabei sieht | deine IP-Adresse, Zeitpunkt, die angefragte Datei und den User-Agent der App |
| User-Agent | ab der nächsten Version nur `Atemkraft/<Version>`. **Bis einschließlich Version 1.5.1** sendet die App den Standard von Android, der Android-Version und Gerätemodell enthält (z. B. `Dalvik/2.1.0 (Linux; U; Android 14; <Modell> Build/…)`). |
| Was nicht übertragen wird | keine Kennung, kein Konto, keine Daten aus der App |
| Empfänger | GitHub, Inc. (ein Unternehmen von Microsoft, USA). Für die Verarbeitung dort gilt das [GitHub General Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement). Der Anbieter von Atemkraft hat keinen Zugriff auf diese Daten. |

Die Verbindung ist verschlüsselt (HTTPS). Die App prüft jedes Archiv gegen eine fest hinterlegte Prüfsumme, bevor sie es entpackt. Danach läuft die Sprachausgabe ohne Netz.

Links im Über-Screen (Quellcode, diese Erklärung, Lizenzen) öffnen deinen Browser; diese Aufrufe macht der Browser, nicht die App.

## Sprachausgabe des Systems

Hast du keine Stimme geladen, kann die gesprochene Meditationsanleitung die Sprachausgabe deines Geräts nutzen. Sie erhält nur die festen Ansagetexte der App, nie Daten aus Logbuch oder Einstellungen. Ob diese Sprachausgabe selbst eine Netzverbindung nutzt, hängt von der installierten Sprach-App ab, nicht von Atemkraft.

## Benachrichtigung

Während einer Meditation zeigt die App eine Benachrichtigung „Meditation läuft“ mit der verbleibenden bzw. vergangenen Zeit, auch auf dem Sperrbildschirm. Sie nennt keine Übungen und keine Logbuchdaten.

## Export-Datei

Unter *Einstellungen → Daten* („Muster exportieren“) kannst du deine gespeicherten Muster als Datei sichern (`atemkraft-muster-<datum>.json`). Sie enthält Name, Phasenlängen, Zeitpunkt des Speicherns und die Anpassungen dieser Muster, kein Logbuch und keine Suchtexte. Den Speicherort wählst du selbst über die Dateiauswahl von Android; die App behält danach keinen Zugriff. Wohin die Datei weitergeht (z. B. in einen Cloud-Ordner), entscheidest du.

<a id="wechsel"></a>
## Kein Cloud-Backup, Handywechsel

Die App ist vom Android-Cloud-Backup ausgeschlossen. Ab Android 12 kann die direkte Übertragung beim Handywechsel (Kabel oder WLAN-Direkt, z. B. Smart Switch) Logbuch, Muster und Einstellungen von Gerät zu Gerät mitnehmen, ohne Umweg über eine Cloud; Stimmen lädst du danach neu. Bis Android 11 ist auch diese Übertragung abgeschaltet.

## Berechtigungen

| Berechtigung | Wofür |
|---|---|
| Internet (`INTERNET`) | nur der Stimm-Download |
| Vibration (`VIBRATE`) | fühlbare Phasenwechsel, abschaltbar |
| Aktiv halten (`WAKE_LOCK`) | hält den Prozessor während einer laufenden Meditation wach, damit Gongs pünktlich kommen |
| Vordergrunddienst (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) | Meditation läuft bei ausgeschaltetem Bildschirm weiter |
| Benachrichtigungen (`POST_NOTIFICATIONS`) | Status der laufenden Meditation; lehnst du ab, läuft der Timer trotzdem |
| Sichtbarkeit der Sprachausgabe (`<queries>` für `TTS_SERVICE`) | die Sprachausgabe des Geräts finden |

Hinzu kommt `app.atemkraft.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, die Android-Bibliothek AndroidX automatisch einträgt; sie gilt nur innerhalb der App. Die App fragt weder Standort, Kontakte, Kamera, Mikrofon noch Speicher ab.

## Deine Rechte

Da der Anbieter keine Daten über dich verarbeitet, gibt es bei ihm nichts, worüber er Auskunft geben, was er berichtigen oder löschen könnte. Deine Daten in der App verwaltest und löschst du selbst (siehe oben). Für Daten bei GitHub wende dich an GitHub. Du hast das Recht, dich bei einer Datenschutz-Aufsichtsbehörde zu beschweren (Art. 77 DSGVO).

<a id="kontakt"></a>
## Kontakt

Fragen zum Datenschutz: [Issue im Repo](https://github.com/grenzenloseSchublade/atemkraft/issues) eröffnen. Vertraulich, etwa bei einer Sicherheitslücke: [Private Vulnerability Reporting](https://github.com/grenzenloseSchublade/atemkraft/security/advisories/new), siehe [SECURITY.md](SECURITY.md#melden).

## Änderungen

Ändert sich etwas am Umgang mit Daten, steht das in dieser Datei mit neuem Stand-Datum; frühere Fassungen zeigt die Versionsgeschichte im Repo.
