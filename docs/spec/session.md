# Spezifikation: Session (Atem-Session und Meditation im Vollbild)

Regel: [STYLEGUIDE MUSTER-07](../STYLEGUIDE.md#muster). Hier steht, wie die laufende Sitzung genau gebaut ist. Entscheidungen und ihr Datum in [DECISIONS.md](../DECISIONS.md).

## Aufbau

- Rahmen `OverlayChrome` (Rand `SessionPadding`, `safeDrawing`): links Minimieren, rechts „Danach in die Stille“ (nur Atem-Session) und Ton (`SoundSwitch`, Kreis `Dimens.OverlayIconButton` 40). Für TalkBack ein eigener Bereich (`paneTitle` = Übungsname bzw. „Meditation“); die App darunter ist derweil nicht erreichbar (`inertWhen`).
- Gerüst `SessionRunningLayout`: Kopf, Kreis im freien Platz, Steuerung; was unter dem Kreis steht (Anleitungszeile, „Als Nächstes“), ist als `centerExtra` fest reserviert – der Kreis springt weder beim Wechsel der Zeilen noch beim Aus-/Einblenden.
- Kopf der Atem-Session: „Name · Runde x von y“ (`DotSeparatedFlow`, schon im Countdown; der Zusatz ist so breit wie „Runde y von y“, die Kopfhöhe bleibt fest). Im Kreis nur Phasenwort und Sekunden (`GlowText` in `SessionTextYellow`; Halten heißt nach seiner Höhe „Halten voll“/„Halten leer“, die Pause zwischen zwei Stufen nur „Halten“ – ebenso in „Als Nächstes“, Mini-Leiste und Ansage, TEXT-02), die Anleitungszeile (offenes Halten: „Tippen, wenn du weiteratmen willst“) fest darunter in `SessionNoteAmber`; Cyan-Fortschrittsbogen der Gesamtsitzung (`SessionRingStroke`).
- Steuerung nur über `SessionControlBar`: „Von vorne“ tonal (`TonalDeep`) mit `ic_reset`, Pause/Weiter groß in `SessionButtonCyan`, „Beenden“ tonal mit `ic_stop` in `error`; Kreise `SessionMainButton` 76 / `SessionSideButton` 56, Icons 32/28, Abstand `SessionControlGap` 40 (Maße des Mockups Variante B, fest, nicht mit der Schrift – Bedienung mit halb geschlossenen Augen). In der Vorbereitung nur „Beenden“, die anderen unsichtbar mit Platz statt ausgegraut; Knöpfe erscheinen nach der Vorbereitung (`Motion.ControlFade`). Text-Buttons am Ende `SessionPrimaryButton`/`SessionSecondaryButton`.
- Alles, was per Kreis-Tipp geht (Pause, Weiter), hat zusätzlich einen benannten Button; in `WAITING_FOR_USER` morpht der Hauptknopf zu „Weiter“. Kreis-Tipp über `CircleTapTarget` (Treffer nur bis zur Spur, `swallowWhenIdle` im Countdown; Fokusring `SessionButtonCyan`, Strich `FocusRing`, nur bei Tastatur/D-Pad), Rückmeldung `rememberTapFlash` (Haptik, Flash, Umschalten), `PauseFlash` nie größer als der Kreis.
- Pause sagt das Phasenwort an („Pausiert, Einatmen“), die Meditation als Zustand ihres Titels.

## Anzeige-Optionen

Bogen, Sekunden und „Als Nächstes“ sind in den Einstellungen abschaltbar („Anzeige der Atemübung“, `SessionDisplay`); der Countdown vor dem Start zählt immer. Die Meditation hat eigene Schalter (`MeditationDisplay`: Automatisch ausblenden, Fortschrittsring, Zeit im Kreis).

## Ruhe-Ansicht

Tipp neben den Kreis blendet alle Bedienelemente samt Status- und Navigationsleiste aus und wieder ein (`CalmHost`, `HideableSlot`, `HideSystemBars`; Kreis-Tipp bleibt Pause/Weiter). Mit „Automatisch ausblenden“ (Standard an) kommt sie 5 s nach der letzten Berührung (`AUTO_CALM_DELAY_MS`), nicht im Countdown. Pause und Ende blenden ein; mit TalkBack oder Switch Access wird nie ausgeblendet (`rememberAccessibilityActive`); eine Taste blendet zuerst nur ein. Der Overlay-Rand rechnet mit den Systemleisten auch, wenn sie fehlen: Nichts springt. Ausblenden über `Motion.CalmFade` 500 ms, bewusst länger als 300 ms.

## Danach in die Stille

Je Übung schnell umschaltbar: Icon-only-Schalter in der Kopfzeile des Overlays links neben dem Ton (`IconSwitch` mit `ic_meditation`, „Danach in die Stille“, An/Aus, aus in `TERTIARY`, gleiches Größentoken wie der Ton); nicht bei `Exercise.silenceAfterAllowed = false` (Atmen im Gehen) und nicht am Ende; blendet in der Ruhe-Ansicht mit der Kopfzeile aus. Der Wunsch gilt nur für diese Übung (`SessionUiState.silenceAfter` im `BreathSessionController`, bleibt bei „Von vorne“ und „Nochmal“); Startwert ist die Einstellung `SessionDisplay.silenceAfter` („Standard für neue Übungen“, Standard aus).

Endet eine Übung mit diesem Wunsch, geht es nahtlos in die laufende Meditation mit Dauer, Gong und Stimme wie zuletzt im Tab Meditation (`meditationSettings`); der Eintrag der Atemübung steht schon im Logbuch. Ohne den Wunsch bietet der Ende-Screen „In die Stille ›“ (`FinishedPanel.onIntoSilence`, derselbe Begriff wie Einstellung und Schalter). Der Übergang läuft im Ablauf, nicht in der Oberfläche: `SilenceAfterBreath` (`AppContainer`) hört auf `BreathSessionController.finished`, liest den Wunsch der Übung und startet die Meditation über dasselbe `MeditationController.start` wie der Tab; die Oberfläche übernimmt nur, ob das Overlay offen war (`PlaybackState.takeOverSilence` in `PlaybackHost.kt`). Im Hintergrund nur, solange der Foreground-Service der Atem-Session läuft (die Meditation übernimmt ihn; ein neuer Start aus dem Hintergrund wäre ab Android 12 verboten), sonst wird der Übergang verworfen. Nie über eine offene Meditation hinweg.

Die Benachrichtigungs-Erlaubnis fragt der erste Start einer Atemübung vor dem Start ab: Die Sitzung beginnt erst nach der Antwort, gleich welcher – kein Countdown unter dem Systemdialog, kein eigener Erklärdialog (wofür, sagt die Kanalbeschreibung in den Systemeinstellungen).

## App-weit, minimiert, Zurück

Beide Sitzungsarten laufen app-weit weiter (`MeditationController`, `BreathSessionController` im `AppContainer`, Foreground-Service `PlaybackService` mit Haltern je Ablauf über `PlaybackServiceLinks`, Partial-Wakelock während des Laufs), auch nach Wegwischen. Minimiert ist eine laufende Session immer als `MiniNowPlayingBar` sichtbar, am Ende mit „Geschafft“ (`session_done_title`). Zurück im Overlay minimiert, am Ende-Screen wirkt es wie „Ende“, auf einem Tab beendet es die App wie gewohnt. Es läuft höchstens ein Ablauf: Ein neuer Start ersetzt den laufenden ohne Rückfrage (`PlaybackState.startBreath`/`startMeditation`); „Danach in die Stille“ räumt einen stehenden Ende-Screen der Meditation weg.

## Ende

Beide Sitzungsarten gleich: „✓ Geschafft“ (`paneTitle` „Geschafft“), die Einladung „Spür kurz nach, wie es dir geht.“, darunter die Antwortreihe zum Befinden (siehe unten), Bilanz nur aus echten Werten (`sessionSummaryParts`; Meditation: Dauer; TalkBack hört die Dauer ausgeschrieben, „6 Minuten 24 Sekunden“), „Nochmal“/„Ende“ (`FinishedPanel`, bei Tastatur Fokus auf „Nochmal“), darunter „Im Logbuch ansehen ›“ (`AppTextButton` + `ForwardLinkText`, beendet wie „Ende“ und öffnet das Logbuch) und ohne Stille-Wunsch „In die Stille ›“.

## Befinden am Ende

Regel [SEC-LEGAL-04](../SECURITY.md#recht): Selbstauskunft ohne Auswertung. Unter der Einladung drei gleich breite, stille Wahlflächen („ruhiger · wie vorher · unruhiger“, nach Übungen der Familie „Aktivieren“ und sanft aktivierenden Mustern „wacher · wie vorher · müder“, `Feeling.scaleFor`), Anatomie „Antwortreihe“ in [§4.5](../STYLEGUIDE.md#anatomie). Ein Tipp, keine vorgewählt, eine zweite Wahl ersetzt die erste; „Nochmal“, „Ende“, Zurück und die Links gehen jederzeit, ohne Nachfrage. Keine Abfrage vor dem Start.

- Nur wenn ein Logbuch-Eintrag entstand: Die Atemübung zeigt den Ende-Screen nur am natürlichen Ende (Eintrag immer), die Meditation auch nach „Beenden“ ab einer Minute (Eintrag ebenfalls). Der Ablauf merkt sich die Id des Eintrags (`BreathSessionController.answerFeeling`, `MeditationController.answerFeeling`) und setzt die Antwort nachträglich (`LogbookDao.setFeeling`); die Wahl erscheint sofort.
- Einstellung „Befinden am Ende fragen“ (Einstellungen › Daten, DataStore `feeling_prompt`): Fehlt sie, erscheint die Reihe beim ersten regulären Ende einmal mit dem Satz „Nur auf diesem Gerät gespeichert. Abschalten kannst du die Frage in den Einstellungen.“; das Erscheinen setzt den Schalter auf an (`SettingsRepository.feelingOffered`). Aus: keine Reihe.
- Nach „Danach in die Stille“ fragt erst der Ende-Screen der Meditation; der Eintrag der Atemübung bleibt unbeantwortet.
- Im Logbuch steht das Wort gedämpft unter der Uhrzeit (`TERTIARY`, gesprochen „danach ruhiger“), ohne Farbe, Icon oder Summe; „Logbuch leeren“ löscht es mit.

## Logbuch und Ersetzen

Was von selbst zu Ende läuft, kommt immer ins Logbuch; früher Beendetes („Beenden“, „Stopp“ in der Benachrichtigung, ersetzt durch einen neuen Start) ab einer Minute aktiver Zeit (`SessionTiming.MIN_LOG_MS`), mit echter Dauer und ohne Wertung „abgebrochen“, darunter gar nicht. Die Meditation zeigt ab dieser Minute auch beim Beenden End-Gong und Ende-Screen (Timer wie offene Sitzung), darunter kehrt sie in die Auswahl zurück; die Atemübung schließt beim Beenden ohne Ende-Screen.

## Takt

`SessionTiming.FRAME_MS` 33 ms und `COUNTDOWN_SECONDS` 3 in `domain/SessionTiming.kt`, geteilt von Atem-Session und Meditation; Stoppuhr-Tick 200 ms im `MeditationController`. Kreis, Countdown und Ton leiten sich aus der Session-Uhr ab (MOTION-01); `Pacer.ScaleMin`/`ScaleMax` 0,42 / 1,0. Der Füllstand gleitet je Phase von `RuntimePhase.circle.from` nach `.to` (`CircleFractions`, stetig über die Grenzen, MOTION-03); im Countdown steht der Kreis am Start der ersten Phase, die Tonhöhe des Dauertons folgt demselben Füllstand. Nach Weiter setzt der Dauerton an der Tonhöhe fort, bei der die Pause begann (`PhaseAudio.elapsedMs`), nicht am Anfang der Phase.

## Audio-Fokus

Beide Sitzungsarten (`SessionAudioCoordinator`, `MeditationController`/`MeditationAudioCoordinator`) fordern transienten Fokus mit Ducken erst an, wenn etwas klingt, und nur, solange etwas klingen kann: nicht im Countdown (Atem: ab der ersten Phase; Meditation: mit Start-Gong oder erster Ansage), nicht im Modus „Aus“, nicht in der Pause, nicht stummgeschaltet (Stumm gibt ihn ab, wieder an fordert ihn neu an – in der Meditation mit dem nächsten Ton). Ein neuer Start und „Nochmal“ blenden einen noch klingenden Abschluss-Gong aus. Nimmt ein Anruf oder eine andere App den Fokus (`AudioFocusController`, Listener), schweigen Ton, Gong und Stimme; Ablauf und Haptik laufen weiter, nichts pausiert von selbst. Kommt der Fokus zurück, klingt es wieder. Lehnt das System den Antrag ab (etwa „Weiter“ während eines Anrufs), fragt jeder folgende Ton neu – nach dem Anruf klingt es von selbst wieder; nach dauerhaftem Verlust (fremde Musik) holt erst Pause/Weiter oder Stumm aus/an ihn zurück, damit die fremde Musik nicht ungefragt wieder geduckt wird. Fokus und Service enden nach dem Abschluss-Gong erst nach `gongTotalMs()` plus `ToneCuePlayer.GONG_END_RESERVE_MS` (500 ms, eine Zahl für beide, AUDIO-02).

**Kopfhörer abziehen** (Kabel oder Bluetooth, `ACTION_AUDIO_BECOMING_NOISY`) wirkt wie der Stummschalter: Ton, Gong und Stimme schweigen statt über den Lautsprecher weiterzuklingen, Ablauf und Vibration laufen weiter, nichts pausiert – wie beim Anruf. Der Schalter steht danach auf stumm; ein Tipp holt den Ton zurück. Der Empfänger (`UnplugWatcher`) ist nur registriert, solange die Übung bzw. Meditation läuft (Atem: Vorbereitung bis Ende; Meditation: Start bis zum Ausklang), nie im Manifest (SEC-PLAT-01), ohne Recht. Die Vorschau im Muster-Editor schaltet ebenso ihren Ton aus ([muster-editor.md](muster-editor.md)).

**Sprachmodell:** Die gewählte neuronale Stimme wird beim App-Start nur gemerkt; ihr Modell (rund 100 MB nativer Speicher) lädt erst `MeditationController.start` mit Sprachanleitung – die erste Ansage kommt frühestens 20 s nach dem Countdown und wartet sonst hinter dem Laden. Nach dem Ende, beim Abbruch und bei `onTrimMemory` ohne laufende Meditation wird es wieder freigegeben. Tab und Einstellungen binden nur das System-TTS an (`checkSpeech`).

## Prüfung

`ScreenshotTest` (`09_session_*`, `10_meditation_*` samt `*_ende_befinden*`, auch quer), `PlaybackFlowTest`, R-A11Y (TalkBack, Switch Access, D-Pad: Pause/Weiter ohne Kreis-Geste erreichbar), R-AUDIO.
