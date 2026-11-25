# Spezifikation: Session (Atem-Session und Meditation im Vollbild)

Regel: [STYLEGUIDE MUSTER-07](../STYLEGUIDE.md#muster). Hier steht, wie die laufende Sitzung genau gebaut ist. Entscheidungen und ihr Datum in [DECISIONS.md](../DECISIONS.md).

## Aufbau

- Rahmen `OverlayChrome` (Rand `safeDrawing` und Systemleisten, der Inhalt darunter mit `SessionPadding`): links Minimieren, rechts „Danach in die Stille“ (nur Atem-Session) und Ton (`SoundSwitch`, Kreis `Dimens.OverlayIconButton` 40). Für TalkBack ein eigener Bereich (`paneTitle` = Übungsname bzw. „Meditation“); die App darunter ist derweil nicht erreichbar (`inertWhen`).
- Gerüst `SessionRunningLayout`: Kopf, Kreis im freien Platz, Steuerung; was unter dem Kreis steht (Anleitungszeile, „Als Nächstes“), ist als `centerExtra` fest reserviert – der Kreis springt weder beim Wechsel der Zeilen noch beim Aus-/Einblenden.
- Kopf der Atem-Session: „Name · Runde x von y“ (`DotSeparatedFlow`, schon im Countdown; der Zusatz ist so breit wie „Runde y von y“, die Kopfhöhe bleibt fest). Im Kreis nur Phasenwort und Sekunden (`GlowText` in `SessionTextYellow`; Halten heißt nach seiner Höhe „Halten voll“/„Halten leer“, die Pause zwischen zwei Stufen nur „Halten“ – ebenso in „Als Nächstes“, Mini-Leiste und Ansage, TEXT-02), die Anleitungszeile (offenes Halten: „Tippen, wenn du weiteratmen willst“) fest darunter in `SessionNoteAmber`; Cyan-Fortschrittsbogen der Gesamtsitzung (`SessionRingStroke`).
- Steuerung nur über `SessionControlBar`: „Von vorne“ tonal (`TonalDeep`) mit `ic_reset`, Pause/Weiter groß in `SessionButtonCyan`, „Beenden“ tonal mit `ic_stop` in `error`; Kreise `SessionMainButton` 76 / `SessionSideButton` 56, Icons 32/28, Abstand `SessionControlGap` 40 (Maße des Mockups Variante B, fest, nicht mit der Schrift – Bedienung mit halb geschlossenen Augen). In der Vorbereitung nur „Beenden“, die anderen unsichtbar mit Platz statt ausgegraut; Knöpfe erscheinen nach der Vorbereitung (`Motion.ControlFade`). Text-Buttons am Ende `SessionPrimaryButton`/`SessionSecondaryButton`.
- Alles, was per Kreis-Tipp geht (Pause, Weiter), hat zusätzlich einen benannten Button; in `WAITING_FOR_USER` morpht der Hauptknopf zu „Weiter“. Kreis-Tipp über `CircleTapTarget` (Treffer nur bis zur Spur, `swallowWhenIdle` im Countdown; Fokusring `SessionButtonCyan`, Strich `FocusRing`, nur bei Tastatur/D-Pad), Rückmeldung `rememberTapFlash` (Haptik, Flash, Umschalten), `PauseFlash` nie größer als der Kreis.
- Pause sagt das Phasenwort an („Pausiert, Einatmen“), die Meditation als Zustand ihres Titels.

## Anzeige-Optionen

Bogen, Sekunden, „Als Nächstes“ und der Nachklang am Ende sind in den Einstellungen abschaltbar („Anzeige der Atemübung“, `SessionDisplay`); der Countdown vor dem Start zählt immer. Die Meditation hat eigene Schalter (`MeditationDisplay`: Automatisch ausblenden, Fortschrittsring, Zeit im Kreis).

## Ruhe-Ansicht

Tipp neben den Kreis blendet alle Bedienelemente samt Status- und Navigationsleiste aus und wieder ein (`CalmHost`, `HideableSlot`, `HideSystemBars`; Kreis-Tipp bleibt Pause/Weiter). Mit „Automatisch ausblenden“ (Standard an) kommt sie 5 s nach der letzten Berührung (`AUTO_CALM_DELAY_MS`), nicht im Countdown. Pause und Ende blenden ein; mit TalkBack oder Switch Access wird nie ausgeblendet (`rememberAccessibilityActive`); eine Taste blendet zuerst nur ein. Der Overlay-Rand rechnet mit den Systemleisten auch, wenn sie fehlen: Nichts springt. Ausblenden über `Motion.CalmFade` 500 ms, bewusst länger als 300 ms.

## Danach in die Stille

Je Übung schnell umschaltbar: Icon-only-Schalter in der Kopfzeile des Overlays links neben dem Ton (`IconSwitch` mit `ic_meditation`, „Danach in die Stille“, An/Aus, aus in `TERTIARY`, gleiches Größentoken wie der Ton); nicht bei `Exercise.silenceAfterAllowed = false` (Atmen im Gehen) und nicht am Ende; blendet in der Ruhe-Ansicht mit der Kopfzeile aus. Der Wunsch gilt nur für diese Übung (`SessionUiState.silenceAfter` im `BreathSessionController`, bleibt bei „Von vorne“ und „Nochmal“). Startwert ist die Wahl „Danach Stille“ im Blatt „Anpassen“ der Detailseite (`SessionConfig.silenceMinutes`: 0 = aus, sonst an mit dieser Dauer; Eigentümer 2026-11-24, [detail.md](detail.md#anpassen-und-start-unten)); wo kein Blatt war (Muster-Editor), die Einstellung `SessionDisplay.silenceAfter` (Standard aus). Der Schalter ändert nur An/Aus, die Dauer aus dem Blatt bleibt (`SessionUiState.silenceMinutes`).

Endet eine Übung mit diesem Wunsch, geht es nahtlos in die laufende Meditation mit Gong und Stimme wie zuletzt im Tab Meditation (`meditationSettings`) und der Dauer aus dem Blatt als feste Zeit – ohne Wahl dort mit der Dauer (oder offenen Sitzung) des Tabs; der Eintrag der Atemübung steht schon im Logbuch. **Ein Bogen, nicht zwei Abläufe** (Eigentümer 2026-11-25): Die Stille beginnt ohne Countdown, schon im Lauf (`MeditationEntry`); klingt der Abschluss-Gong der Atemübung noch (`BreathSessionController.finishGongRemainingMs`), ist er ihr Beginn und sie schlägt keinen eigenen Start-Gong (`ON_BREATH_GONG`), sonst ihren eigenen, wenn eingestellt (`HANDOVER`, z. B. ohne Ton in der Übung). Zwei Gongs klingen nie übereinander. Das Overlay der Meditation blendet dabei auf ruhigem Grund in `Motion.CalmFade` ein (`SilenceFadeIn` in `PlaybackHost.kt`), der Atemkreis verlischt ohne harten Schnitt; TalkBack hört den neuen Bereich „Meditation“ und ihren Zustand am Titel. „Nochmal“ in der Stille startet wieder mit Countdown. Derselbe Weg andersherum: „Vorher atmen“ im Tab Meditation ([meditation.md](meditation.md)) startet die Resonanz-Atmung mit diesem Wunsch. Der Übergang läuft im Ablauf, nicht in der Oberfläche: `SilenceAfterBreath` (`AppContainer`) hört auf `BreathSessionController.finished`, liest den Wunsch der Übung und startet die Meditation über dasselbe `MeditationController.start` wie der Tab; die Oberfläche übernimmt nur, ob das Overlay offen war (`PlaybackState.takeOverSilence` in `PlaybackHost.kt`). Im Hintergrund nur, solange der Foreground-Service der Atem-Session läuft (die Meditation übernimmt ihn; ein neuer Start aus dem Hintergrund wäre ab Android 12 verboten), sonst wird der Übergang verworfen. Nie über eine offene Meditation hinweg.

Die Benachrichtigungs-Erlaubnis fragt die App erst, wenn die erste Sitzung zu Ende ist und ihr Overlay schließt („Ende“ am Ende-Screen oder „Beenden“; Eigentümer 2026-11-25) – nie vor dem ersten Atemzug, einmal, danach nie wieder ungefragt (auch nach Ablehnung; der Schalter der Erinnerung fragt selbst). Kein eigener Erklärdialog (wofür, sagt die Kanalbeschreibung in den Systemeinstellungen). Die erste Sitzung läuft ohne die Erlaubnis: Der Foreground-Service darf ab Android 13 auch ohne sie laufen, nur seine Benachrichtigung steht dann nicht in der Leiste (Android führt die App weiter in der Liste aktiver Apps); Ton, Vibration, Bildschirm aus und der Übergang in die Stille laufen wie sonst.

## App-weit, minimiert, Zurück

Beide Sitzungsarten laufen app-weit weiter (`MeditationController`, `BreathSessionController` im `AppContainer`, Foreground-Service `PlaybackService` mit Haltern je Ablauf über `PlaybackServiceLinks`, Partial-Wakelock während des Laufs), auch nach Wegwischen. Minimiert ist eine laufende Session immer als `MiniNowPlayingBar` sichtbar, am Ende mit „Geschafft“ (`session_done_title`). Zurück im Overlay minimiert, am Ende-Screen wirkt es wie „Ende“, auf einem Tab beendet es die App wie gewohnt. Es läuft höchstens ein Ablauf: Ein neuer Start ersetzt den laufenden ohne Rückfrage (`PlaybackState.startBreath`/`startMeditation`); „Danach in die Stille“ räumt einen stehenden Ende-Screen der Meditation weg.

**Benachrichtigung und Kopfhörer-Taste:** Die Dauer-Benachrichtigung (`PlaybackService`) trägt neben „Beenden“ davor „Pause“ bzw. „Weiter“, solange der laufende Ablauf das auch im Overlay kann (Atemübung und Meditation im Lauf bzw. pausiert; nicht im Countdown, nicht im offenen Halten). Dieselbe Steuerung hat eine Plattform-`MediaSession` (keine Bibliothek): Die Taste am Kopfhörer pausiert und setzt fort, im Countdown und offenen Halten bewirkt sie nichts. Die Benachrichtigung bleibt die ruhige Status-Zeile ohne Media-Player-Ansicht; die Metadaten der Session sind so generisch wie ihr Titel („Atemübung läuft“, „Meditation läuft“), nie der Name der Übung (SEC-PRIV-03). Ohne laufenden Ablauf ist die Session inaktiv; eine verwaiste Benachrichtigung räumt jede ihrer Aktionen weg. Im Moment des Übergangs in die Stille gilt sie der Meditation.

## Ende

**Nachklang** (nur Atemübung, Eigentümer 2026-11-25, `AfterglowContent`): Kommt eine Übung von selbst zu Ende, atmet der Kreis erst noch `Motion.Afterglow` (18 s) still aus und erlischt – ohne Zahl, Kopf und Steuerung ausgeblendet, ihr Platz bleibt (dasselbe Gerüst wie die Ruhe-Ansicht, der Kreis steht, wo er stand). Erst dann der Ende-Screen. Ein Tipp irgendwo überspringt ihn (TalkBack: „Nachklang“, Aktion „überspringen“, Live-Region). Die Zeit läuft ab dem Ende auf der Session-Uhr (`SessionUiState.finishedAtMs`, MOTION-01): Wer das Overlay später öffnet, sieht den Rest oder gleich den Ende-Screen. Nicht mit „Danach in die Stille“ (das Ende mündet dort in die Stille), nicht nach „Beenden“; Logbuch, Befinden und „Nochmal“ wie bisher. Bei Animator-Skala 0 steht der Kreis still, nur `Motion.AfterglowStill` (5 s). Abschaltbar: „Nachklang“ in „Anzeige der Atemübung“ (`SessionDisplay.afterglow`, Standard an).

**Nur das Nötigste** (Eigentümer 2026-11-25), beide Sitzungsarten gleich (`FinishedPanel`): „✓ Geschafft“ (`paneTitle` „Geschafft“), die Einladung „Spür kurz nach, wie es dir geht.“, darunter die Antwortreihe zum Befinden (siehe unten), dann ◉ „Tut mir gut“ (`PersonalMarkToggle`, `EndMark`: dieselbe Markierung wie auf der Detailseite, nach derselben Regel – eingebaute Übungen, gespeicherte Muster und die Meditation, nicht das Muster des Tages; nach „Atmen → Stille“ für die geübte Übung, nach einer Meditation allein für die Meditation), dann „Nochmal“/„Ende“ (bei Tastatur Fokus auf „Nochmal“). Keine Bilanz (die Dauer steht im Logbuch; „Atem ist keine Zahl“), kein „Im Logbuch ansehen ›“, kein „In die Stille ›“ (die Stille wählt man vorher im Blatt „Anpassen“ oder mit dem Schalter in der Übung).

```
 ✓ Geschafft                          labelMedium TERTIARY
 GapSmall
 Spür kurz nach, wie es dir geht.     headlineSmall, zentriert
 ListGap
 [ ruhiger | wie vorher | unruhiger ] SegmentedChoiceRow (nur mit Logbuch-Eintrag und Einstellung)
 GapSmall
              ◉                       PersonalMarkToggle 48 dp, „Tut mir gut“ + Zustand (nur markierbar)
 SessionPadding
 [ Nochmal ]  [ Ende ]                AdaptiveButtonRow
```

TalkBack-Reihenfolge: Bereich „Geschafft“, Titel, Gruppe „Befinden“, „Tut mir gut, nicht markiert“, Nochmal, Ende.

## Befinden am Ende

Regel [SEC-LEGAL-04](../SECURITY.md#recht): Selbstauskunft ohne Auswertung. Unter der Einladung drei gleich breite, stille Wahlflächen („ruhiger · wie vorher · unruhiger“, nach Übungen der Familie „Aktivieren“ und sanft aktivierenden Mustern „wacher · wie vorher · müder“, `Feeling.scaleFor`), Anatomie „Antwortreihe“ [unten](#antwortreihe). Ein Tipp, keine vorgewählt, eine zweite Wahl ersetzt die erste; „Nochmal“, „Ende“, ◉ und Zurück gehen jederzeit, ohne Nachfrage. Keine Abfrage vor dem Start.

- Nur wenn ein Logbuch-Eintrag entstand: Die Atemübung zeigt den Ende-Screen nur am natürlichen Ende (Eintrag immer), die Meditation auch nach „Beenden“ ab einer Minute (Eintrag ebenfalls). Der Ablauf merkt sich die Id des Eintrags (`BreathSessionController.answerFeeling`, `MeditationController.answerFeeling`) und setzt die Antwort nachträglich (`LogbookDao.setFeeling`); die Wahl erscheint sofort.
- Einstellung „Befinden am Ende fragen“ (Einstellungen › Daten, DataStore `feeling_prompt`): Fehlt sie, erscheint die Reihe beim ersten regulären Ende einmal mit dem Satz „Nur auf diesem Gerät gespeichert. Abschalten kannst du die Frage in den Einstellungen.“; das Erscheinen setzt den Schalter auf an (`SettingsRepository.feelingOffered`). Aus: keine Reihe.
- Nach „Danach in die Stille“ fragt erst der Ende-Screen der Stille, und die Antwort gilt dem einen Eintrag „Atmen → Stille“ (siehe unten).
- Im Logbuch steht das Wort gedämpft unter der Uhrzeit (`TERTIARY`, gesprochen „danach ruhiger“), ohne Farbe, Icon oder Summe; „Logbuch leeren“ löscht es mit.

### <a id="antwortreihe"></a>Anatomie der Antwortreihe (`FinishedPanel` mit `FeelingAsk`)

```
 ✓ Geschafft  labelMedium TERTIARY
 GapSmall
 Spür kurz nach, wie es dir geht.   ← Titel headlineSmall, zentriert
 ListGap 12
 [ ruhiger | wie vorher | unruhiger ]  ← SegmentedChoiceRow: drei gleich breite Segmente, ButtonHeight 40, Tippfläche 48, ohne Häkchen
 GapSmall                              (nur beim ersten Mal:)
 Nur auf diesem Gerät gespeichert. …   ← bodySmall TERTIARY, zentriert
 GapSmall · ◉ · SessionPadding · [Nochmal][Ende]
```

- Keine neue Optik: Die Reihe ist eine `SegmentedChoiceRow` (Einzelwahl aus drei, Fläche `SegmentSelected` + SemiBold, bei Platzmangel gemeinsam bis 85 % kleiner, dann Radioliste); keine vorgewählt, eine Wahl ersetzt die vorige, nie abwählen, nie Pflicht. Breite wie die Knopfreihe darunter (volle Breite im `SessionPadding`). Bei aktivierenden Übungen „wacher · wie vorher · müder“.
- Erscheint nur, wenn ein Logbuch-Eintrag entstand (`feelingOptions` nicht leer) und die Einstellung es will; der Hinweis des ersten Mals bleibt für diesen Ende-Screen stehen. Passt der Ende-Screen nicht in die Höhe (Schrift 2,0), scrollt er; mittig bleibt er, solange alles passt.
- **Prüfung:** `ScreenshotTest` (`09_session_ende_befinden*`, `10_meditation_ende_befinden`: `WORTBRUCH`, `TIPPFLAECHE`, `BESCHNITTEN`), `no-check-icon`; manuell R-A11Y (Gruppe „Befinden“, Auswahl angesagt).

## Logbuch und Ersetzen

**Atmen → Stille ist ein Eintrag** (Eigentümer 2026-11-25): Die Atemübung schreibt ihren Eintrag am Ende wie immer; die Stille danach („Danach in die Stille“, „Vorher atmen“) schreibt keinen zweiten, sondern hängt sich an ihn (`LoggedBreath`, `LogbookDao.addSilence`: `durationMs` wächst um die Stille, `silenceMs` hält ihren Anteil; Room-Schema 8). Es gilt dieselbe Minuten-Regel: Endet die Stille unter einer Minute, bleibt der Eintrag der Atemübung, wie er war; ab einer Minute zählt die tatsächliche Stille. Fehlt der Eintrag der Atemübung (z. B. Logbuch inzwischen geleert) oder kommt er nicht binnen 5 s, schreibt die Stille wie bisher ihren eigenen Eintrag „Meditation“. Befinden und ◉ am Ende-Screen der Stille gelten der geübten Übung (`MeditationUiState.afterExerciseId`). Im Logbuch „Resonanz-Atmung → Stille“ ([eigene-tab.md](eigene-tab.md)).

Was von selbst zu Ende läuft, kommt immer ins Logbuch; früher Beendetes („Beenden“, „Stopp“ in der Benachrichtigung, ersetzt durch einen neuen Start) ab einer Minute aktiver Zeit (`SessionTiming.MIN_LOG_MS`), mit echter Dauer und ohne Wertung „abgebrochen“, darunter gar nicht. Die Meditation zeigt ab dieser Minute auch beim Beenden End-Gong und Ende-Screen (Timer wie offene Sitzung), darunter kehrt sie in die Auswahl zurück; die Atemübung schließt beim Beenden ohne Ende-Screen.

## Takt

`SessionTiming.FRAME_MS` 33 ms und `COUNTDOWN_SECONDS` 3 in `domain/SessionTiming.kt`, geteilt von Atem-Session und Meditation; Stoppuhr-Tick 200 ms im `MeditationController`. Kreis, Countdown und Ton leiten sich aus der Session-Uhr ab (MOTION-01). Der Kreis wächst von 0,42 seiner vollen Größe bis zur vollen (`MIN_SCALE` = 0,42 × `MAX_SCALE`, `MAX_SCALE` 0,97 des Außenradius in `BreathingCircle.kt`). Der Füllstand gleitet je Phase von `RuntimePhase.circle.from` nach `.to` (`CircleFractions`, stetig über die Grenzen, MOTION-03); im Countdown steht der Kreis am Start der ersten Phase, die Tonhöhe des Dauertons folgt demselben Füllstand. Nach Weiter setzt der Dauerton an der Tonhöhe fort, bei der die Pause begann (`PhaseAudio.elapsedMs`), nicht am Anfang der Phase.

## <a id="klang"></a>Klang und Haptik (Ist-Werte, `src/cue/`)

Regeln AUDIO-01 … -05 in [STYLEGUIDE §9](../STYLEGUIDE.md#audio). Synthese 44,1 kHz PCM16 mono zur Laufzeit. Wechselton 528/396/440 Hz (Ein/Aus/Halten), 180 ms, Attack 12, Release 60 ms. Gong Teiltöne 330/331,6/894/1698 Hz, „Lang“ 11,5 s + 1,5 s Stille, „Kurz“ 6 + 1,5 s. Durchgehend 196 ↔ 294 Hz, Tonhöhe nach dem Füllstand des Kreises, Ein- und Ausblende 60 ms. Lautstärke Ton/Gong 0,9/1,4/1,85, Dauerton 1,2/2,0/3,2. Sprache System-TTS de-DE (Rate 0,85, Pitch 0,9) oder Piper (Speed 0,9). Haptik-Raste: Primitive `TICK` 0,6 bzw. `CLICK` 1,0, sonst `EFFECT_TICK`/`EFFECT_HEAVY_CLICK`, sonst One-Shot 30/60 ms; gedrosselt (Tick frühestens 40 ms nach einem Tick, 80 ms nach einem Klick). Ein/Aus 110 ms, Halten 70 ms. Session-Vibration als `USAGE_ALARM` (ab API 33 über `VibrationAttributes`), Rasten als `USAGE_TOUCH`. Fokus `GAIN_TRANSIENT_MAY_DUCK`, `USAGE_MEDIA`. Defaults: Töne, Haptik an, Zäsur MEDIUM, Lautstärke MEDIUM, Gong lang.

**Vibration als Welle** (Einstellung „Vibration als Welle“ unter „Vibration“, aus ab Werk, Eigentümer 2026-11-25: erst Option): Statt eines Impulses je Wechsel folgt die Stärke dem Füllstand des Kreises (`BreathWave`, `HapticPlayer.wave`): Einatmen schwillt an, Ausatmen klingt ab, stetig über die Phasengrenzen; Halten ist still, markiert vom Halte-Impuls (70 ms), das Ende vom Doppel-Muster. Wellenform in Schritten von 50 ms (höchstens 400 je Phase), Stärke 30 bei leerer bis 200 bei voller Lunge, als `USAGE_ALARM` wie jede Session-Vibration, auch bei Bildschirm aus. Beim Fortsetzen geht die Welle dort weiter, wo der Kreis steht; Pause, Abbruch und Vorbereitung halten sie sofort an. Nur mit Stärkeregelung des Motors (`hasAmplitudeControl`), sonst bleiben die Impulse – Primitive-Folgen sind kein Ersatz, ihre Höchstzahl je Auftrag ist nicht abfragbar.

**Phasen ansagen** (Einstellung „Phasen ansagen“, aus ab Werk; `PhaseVoice`, `PhaseWord`): Zu Beginn jeder Phase sagt eine Stimme „ein“, „noch ein“ (Nachatmen), „aus“, „halten“ oder „locker“ (Pause) – mit der im Tab Meditation gewählten Stimme (Piper, offline nach dem Download), sonst dem System-TTS; ohne Stimme bleibt alles wie ohne Ansage. Im Modus „Töne“ ersetzt das Wort den Wechselton; Phasen unter 1,5 s (`PhaseWord.MIN_SPOKEN_MS`: Feueratmung, kurzes Nachatmen) behalten ihren Ton, weil sich Worte dort stauen und hinter dem Atem herliefen. Der Dauerton läuft unter der Stimme weiter, „Ton aus“ schaltet nur die Töne ab, nicht die Ansage. Jede Ansage verdrängt die vorige (eine verspätete – Modell lädt noch – spricht nie in die nächste Phase), beim Fortsetzen mitten in einer Phase kommt keine. Sie schweigt in der Pause, beim Stummschalten, bei Fokusverlust und vor dem Abschluss-Gong (AUDIO-03). Das Modell der Stimme lädt in der Vorbereitung und wird am Ende freigegeben; Fokus fordert die Ansage selbst an wie ein Ton.

## Audio-Fokus

Beide Sitzungsarten (`SessionAudioCoordinator`, `MeditationController`/`MeditationAudioCoordinator`) fordern transienten Fokus mit Ducken erst an, wenn etwas klingt, und nur, solange etwas klingen kann: nicht im Countdown (Atem: ab der ersten Phase; Meditation: mit Start-Gong oder erster Ansage), nicht im Modus „Aus“, nicht in der Pause, nicht stummgeschaltet (Stumm gibt ihn ab, wieder an fordert ihn neu an – in der Meditation mit dem nächsten Ton). Ein neuer Start und „Nochmal“ blenden einen noch klingenden Abschluss-Gong aus. Nimmt ein Anruf oder eine andere App den Fokus (`AudioFocusController`, Listener), schweigen Ton, Gong und Stimme; Ablauf und Haptik laufen weiter, nichts pausiert von selbst. Kommt der Fokus zurück, klingt es wieder. Lehnt das System den Antrag ab (etwa „Weiter“ während eines Anrufs), fragt jeder folgende Ton neu – nach dem Anruf klingt es von selbst wieder; nach dauerhaftem Verlust (fremde Musik) holt erst Pause/Weiter oder Stumm aus/an ihn zurück, damit die fremde Musik nicht ungefragt wieder geduckt wird. Fokus und Service enden nach dem Abschluss-Gong erst nach `gongTotalMs()` plus `ToneCuePlayer.GONG_END_RESERVE_MS` (500 ms, eine Zahl für beide, AUDIO-02).

**Kopfhörer abziehen** (Kabel oder Bluetooth, `ACTION_AUDIO_BECOMING_NOISY`) wirkt wie der Stummschalter: Ton, Gong und Stimme schweigen statt über den Lautsprecher weiterzuklingen, Ablauf und Vibration laufen weiter, nichts pausiert – wie beim Anruf. Der Schalter steht danach auf stumm; ein Tipp holt den Ton zurück. Der Empfänger (`UnplugWatcher`) ist nur registriert, solange die Übung bzw. Meditation läuft (Atem: Vorbereitung bis Ende; Meditation: Start bis zum Ausklang), nie im Manifest (SEC-PLAT-01), ohne Recht. Die Vorschau im Muster-Editor schaltet ebenso ihren Ton aus ([muster-editor.md](muster-editor.md)).

**Sprachmodell:** Die gewählte neuronale Stimme wird beim App-Start nur gemerkt; ihr Modell (rund 100 MB nativer Speicher) lädt erst `MeditationController.start` mit Sprachanleitung – die erste Ansage kommt frühestens 20 s nach dem Countdown und wartet sonst hinter dem Laden. Nach dem Ende, beim Abbruch und bei `onTrimMemory` ohne laufende Meditation wird es wieder freigegeben. Tab und Einstellungen binden nur das System-TTS an (`checkSpeech`).

## Prüfung

`ScreenshotTest` (`09_session_*`, `10_meditation_*` samt `*_ende_befinden*`, auch quer), `PlaybackFlowTest`, R-A11Y (TalkBack, Switch Access, D-Pad: Pause/Weiter ohne Kreis-Geste erreichbar), R-AUDIO.
