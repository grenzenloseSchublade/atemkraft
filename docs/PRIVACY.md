# Datenschutzerklärung Atemkraft

Für die Android-App Atemkraft (`app.atemkraft`), wie sie über dieses Repo und seine GitHub-Releases verteilt wird.

- **Stand:** 2026-10-07, geprüft gegen den Quellcode auf `main` an diesem Tag. Wo sich die veröffentlichte Version 1.5.1 davon unterscheidet, steht es dabei ([Stimm-Download](#netz), [Cloud-Backup](#wechsel), [Berechtigungen](#berechtigungen)); Befindens-Suche sowie Export und Import der Muster gibt es erst ab Version 1.6.0.
- **Anbieter:** grenzenloseSchublade, Maintainer von [github.com/grenzenloseSchublade/atemkraft](https://github.com/grenzenloseSchublade/atemkraft). Kontakt siehe [unten](#kontakt).

## Kurzfassung

Atemkraft erhebt keine Daten über dich. Es gibt kein Konto, keinen Server des Anbieters, keine Analyse, keine Werbung, keine Absturzberichte und kein Tracking; die App enthält keine solchen Bibliotheken und keine Google Play Services. Was du in der App tust, bleibt auf deinem Gerät, solange du es nicht selbst exportierst oder beim Handywechsel überträgst (bis einschließlich Version 1.5.1 kommt das Android-Backup hinzu, siehe [unten](#wechsel)). Die einzige Netzverbindung der App ist der Download einer Stimme, und den löst nur ein Tippen von dir aus.

## Was auf deinem Gerät gespeichert wird

Alles liegt im privaten Speicher der App, den andere Apps nicht lesen können. Der Anbieter erhält davon nichts.

| Ort | Inhalt | So löschst du es |
|---|---|---|
| Logbuch (Datenbank) | abgeschlossene Sitzungen: Übung bzw. Meditation, Zeitpunkt, Dauer, Runden | „Logbuch leeren“ im Logbuch; App-Speicher löschen |
| Eigene Muster (Datenbank) | Name, Phasenlängen und Art (aktivierend oder nicht) gespeicherter Muster, Zeitpunkt des Speicherns | einzeln löschen unter „Meine Muster“ |
| Einstellungen | Ton, Haptik, Hinweise (auch, ob du den Sicherheitshinweis vor intensiven Übungen bestätigt hast), Meditation, gewählte Stimme, Anpassungen pro Übung, ob „Meine Muster“ aufgeklappt ist | pro Übung zurücksetzen; App-Speicher löschen |
| Stimmen | heruntergeladene Stimmmodelle (öffentliche Dateien, Download je ca. 25–110 MB) | „Stimme löschen“ in den Einstellungen |
| Zwischenspeicher | ein laufender Download und beim Entpacken ein Zwischenordner | bei Abbruch oder Fehler löscht die App beides sofort; wird die App mittendrin beendet, beim nächsten Versuch. Den Download-Cache leert Android auch bei Speichermangel |

Die Befindens-Suche im Situationen-Tab (ab Version 1.6.0) wird nicht gespeichert: Der Suchtext bleibt nur erhalten, solange die Suche offen ist (auch beim Drehen oder wenn Android die App im Hintergrund beendet und du zurückkehrst), und ist weg, sobald du die Suche schließt. Die App bittet die Tastatur, deine Eingaben nicht zu lernen. Ob eine Tastatur-App dieser Bitte folgt, liegt bei ihr.

Das Logbuch kann Rückschlüsse auf dein Befinden zulassen, weil manche Übungen zu bestimmten Anlässen passen. Es steht deshalb nicht in der Export-Datei und ab Version 1.6.0 nicht im Cloud-Backup; mitgenommen wird es nur bei einer Übertragung beim Handywechsel, die du selbst startest (siehe [Handywechsel](#wechsel)). Beim Deinstallieren löscht Android alle Daten der App.

<a id="netz"></a>
## Stimm-Download – die einzige Netzverbindung

Wenn du in den Einstellungen bei einer Stimme auf „Laden“ tippst, lädt die App ein Archiv von GitHub:

| Punkt | Inhalt |
|---|---|
| Adresse | `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/<stimme>.tar.bz2`, von dort weitergeleitet zu `release-assets.githubusercontent.com` |
| Auslöser | nur dein Tippen auf „Laden“; nie automatisch oder im Hintergrund |
| Was GitHub dabei sieht | deine IP-Adresse, Zeitpunkt, die angefragte Datei und den User-Agent der App |
| User-Agent | ab Version 1.6.0 nur `Atemkraft/<Version>`. **Bis einschließlich Version 1.5.1** sendet die App den Standard von Android, der Android-Version und Gerätemodell enthält (z. B. `Dalvik/2.1.0 (Linux; U; Android 14; <Modell> Build/…)`). |
| Was nicht übertragen wird | keine Kennung, kein Konto, keine Daten aus der App |
| Empfänger | GitHub, Inc. (ein Unternehmen von Microsoft, USA). Für die Verarbeitung dort gilt das [GitHub General Privacy Statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement). Der Anbieter von Atemkraft hat keinen Zugriff auf diese Daten. |

Die Verbindung ist verschlüsselt (HTTPS). Die App prüft jedes Archiv gegen eine fest hinterlegte Prüfsumme, bevor sie es entpackt. Danach läuft die Sprachausgabe ohne Netz.

Links im Über-Screen (Quellcode, diese Erklärung, Lizenzen) öffnen deinen Browser; diese Aufrufe macht der Browser, nicht die App.

## Sprachausgabe des Systems

Hast du keine Stimme geladen oder gewählt, kann die gesprochene Meditationsanleitung die Sprachausgabe deines Geräts nutzen. Sie erhält nur die festen Ansagetexte der App, nie Daten aus Logbuch oder Einstellungen. Die App wählt dabei eine deutsche Stimme, die kein Netz braucht, sofern eine installiert ist. Ob die Sprachausgabe selbst eine Netzverbindung nutzt, hängt von der installierten Sprach-App ab, nicht von Atemkraft.

## Benachrichtigung

Während einer Meditation zeigt die App eine Benachrichtigung „Meditation läuft“ mit der verbleibenden bzw. vergangenen Zeit, auch auf dem Sperrbildschirm. Sie nennt keine Übungen und keine Logbuchdaten.

## Export und Import

Ab Version 1.6.0 kannst du unter *Einstellungen → Daten* („Muster exportieren“) deine gespeicherten Muster als Datei sichern (`atemkraft-muster-<datum>.json`). Sie enthält Name, Phasenlängen, Art, Zeitpunkt des Speicherns und die Anpassungen dieser Muster sowie den Zeitpunkt des Exports, kein Logbuch und keine Suchtexte. Den Speicherort wählst du selbst über die Dateiauswahl von Android; die App behält danach keinen Zugriff. Wohin die Datei weitergeht (z. B. in einen Cloud-Ordner), entscheidest du.

Mit „Muster importieren“ liest die App eine Datei, die du in der Dateiauswahl wählst, einmal ein und übernimmt daraus nur gültige Muster und deren Anpassungen. Auch hier behält sie keinen Zugriff auf die Datei.

<a id="wechsel"></a>
## Kein Cloud-Backup, Handywechsel

Die App ist vom Android-Cloud-Backup ausgeschlossen. Ab Android 12 kann die direkte Übertragung beim Handywechsel (Kabel oder WLAN-Direkt, z. B. Smart Switch) Logbuch, Muster und Einstellungen von Gerät zu Gerät mitnehmen, ohne Umweg über eine Cloud; ob sie das tut, hängt laut Android-Dokumentation vom Gerätehersteller ab. Stimmen lädst du danach neu. Bis Android 11 ist auch diese Übertragung abgeschaltet.

**Bis einschließlich Version 1.5.1** ist das Android-Backup noch erlaubt: Ist auf deinem Gerät die Sicherung eingeschaltet (meist in dein Google-Konto), kann Android Logbuch, Muster und Einstellungen dorthin sichern, Stimmen nicht. Auch die Übertragung beim Handywechsel funktioniert dort unter Android 11 und älter. Der Anbieter von Atemkraft hat auf diese Sicherung keinen Zugriff. Abschalten lässt sie sich in der Regel nur für das ganze Gerät, in den Sicherungs-Einstellungen von Android (Bezeichnung je nach Hersteller). Ob das Update auf Version 1.6.0 eine schon angelegte Sicherung entfernt, dokumentiert Android nicht; ansehen und löschen kannst du Sicherungen in deinem Google-Konto.

<a id="berechtigungen"></a>
## Berechtigungen

| Berechtigung | Wofür |
|---|---|
| Internet (`INTERNET`) | nur der Stimm-Download |
| Vibration (`VIBRATE`) | fühlbare Phasenwechsel und ein kurzer Tick beim Antippen des Kreises; beides mit dem Schalter „Vibration“ abschaltbar. **Bis einschließlich Version 1.5.1** vibriert der Tick auch bei ausgeschaltetem Schalter |
| Aktiv halten (`WAKE_LOCK`) | hält den Prozessor während einer laufenden Meditation wach, damit Gongs pünktlich kommen |
| Vordergrunddienst (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) | Meditation läuft bei ausgeschaltetem Bildschirm weiter |
| Benachrichtigungen (`POST_NOTIFICATIONS`) | Status der laufenden Meditation mit „Beenden“; ab Android 13 fragt die App beim Start danach. Lehnst du ab, läuft der Timer trotzdem |
| Sichtbarkeit der Sprachausgabe (`<queries>` für `TTS_SERVICE`) | die Sprachausgabe des Geräts finden |

Hinzu kommt `app.atemkraft.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, die Android-Bibliothek AndroidX automatisch einträgt; sie gilt nur innerhalb der App. Die App fragt weder Standort, Kontakte, Kamera, Mikrofon noch Speicher ab.

## Deine Rechte

Über die App verarbeitet der Anbieter keine Daten über dich; es gibt bei ihm also nichts, worüber er Auskunft geben, was er berichtigen oder löschen könnte. Schreibst du ihm über GitHub (siehe [Kontakt](#kontakt)), sieht er deinen GitHub-Namen und was du dort schreibst; ein Issue ist öffentlich. Deine Daten in der App verwaltest und löschst du selbst (siehe oben). Für Daten bei GitHub wende dich an GitHub. Du hast das Recht, dich bei einer Datenschutz-Aufsichtsbehörde zu beschweren (Art. 77 DSGVO).

<a id="kontakt"></a>
## Kontakt

Fragen zum Datenschutz: [Issue im Repo](https://github.com/grenzenloseSchublade/atemkraft/issues) eröffnen. Vertraulich, etwa bei einer Sicherheitslücke: [Private Vulnerability Reporting](https://github.com/grenzenloseSchublade/atemkraft/security/advisories/new), siehe [SECURITY.md](SECURITY.md#melden).

## Änderungen

Ändert sich etwas am Umgang mit Daten, steht das in dieser Datei mit neuem Stand-Datum; frühere Fassungen zeigt die Versionsgeschichte im Repo.
