# Voice-Cloning für Atemkraft — Anleitung

**Ziel:** Die gesprochene Meditations-Anleitung in **deiner eigenen Stimme** — ohne alle Sätze
selbst einsprechen zu müssen. Du nimmst eine **kurze Referenz** auf (~30–60 s), klonst die
Stimme **einmalig am PC** und renderst damit die 12 festen Ansage-Sätze der App vor. Alles
läuft **lokal** (keine Cloud, keine Konten).

> Stand der Recherche: August 2026, alle Quellen am Ende. Empfohlener Weg: **OpenVoice v2**
> (MIT-Lizenz, läuft auf CPU). Alternative mit höchster Klon-Qualität: **OmniVoice**
> (⚠ Gewichte nicht-kommerziell). Live-Klonen **auf dem Handy** ist Stand heute nicht
> praktikabel — Begründung unten.

---

## Überblick: die Pipeline

```
1. Referenz aufnehmen        deine Stimme, ~30–60 s, ruhig gesprochen
        │
2. Piper (de_DE-thorsten)    erzeugt die 12 Sätze als deutsche WAVs (Quell-Sprache)
        │
3. OpenVoice v2 Converter    überträgt DEINE Klangfarbe auf die Piper-WAVs (sprachunabhängig)
        │
4. Nachschliff (ffmpeg)      Pegel angleichen, Stille trimmen
        │
5. In die App                als Audio-Cues abspielen (kleiner Code-Anbau, s. u.)
```

Warum dieser Umweg über Piper? OpenVoice v2 besteht aus einem Basis-TTS (MeloTTS — **kann
kein Deutsch**) und einem **ToneColorConverter**, der die Klangfarbe eines Referenz-Sprechers
auf *beliebige* Sprachaufnahmen überträgt — sprachunabhängig, Deutsch ist im offiziellen
Cross-Lingual-Demo (`demo_part2.ipynb`) explizit dabei. Unsere App nutzt Piper/Thorsten ohnehin
schon als deutsche Stimme → Thorsten spricht den Text, der Converter macht daraus *deine* Stimme.

## Die 12 Sätze, die geklont werden

Das sind die festen Ansagen aus `app/src/main/java/app/atemkraft/data/MeditationCues.kt`
(im Skript unten bereits enthalten):

1. Lass deinen Körper im Sitz zur Ruhe kommen.
2. Nimm den Atem wahr, so wie er gerade ist.
3. Spüre, wie die Luft ein- und wieder ausströmt.
4. Es gibt gerade nichts zu tun.
5. Wenn die Gedanken abschweifen, ist das in Ordnung.
6. Komm sanft zurück zum Atem.
7. Lass die Schultern weich werden.
8. Spüre, wo dein Körper den Boden berührt.
9. Gedanken dürfen kommen und gehen.
10. Du musst ihnen nicht folgen.
11. Kehre ruhig zu diesem Atemzug zurück.
12. Was auch immer da ist, lass es da sein.

## Rechtliches & Fairness (kurz)

- **Nur die eigene Stimme klonen** — oder mit ausdrücklicher Einwilligung der Person.
- OpenVoice v2 (Code + Checkpoints): **MIT** — frei, auch kommerziell.
- Der Converter bettet standardmäßig ein **unhörbares Wasserzeichen** ein (wavmark). Für den
  Privatgebrauch unkritisch; abschaltbar (siehe Schritt 4).
- Piper-Nachfolger `piper1-gpl` ist **GPL-3.0** — für das *lokale Erzeugen* von WAVs egal
  (GPL greift erst beim Weiterverteilen des Programms, nicht der Audio-Ausgaben).

---

## Schritt 1 — Referenzaufnahme

**Was aufnehmen:** ~30–60 Sekunden **ruhiges, natürliches Sprechen** — ideal im selben Ton,
den die Anleitung haben soll (langsam, entspannt). Inhalt egal; sprich z. B. einfach die
12 Sätze oben einmal durch, das trifft die Zielprosodie am besten.

**Qualitäts-Checkliste** (aus den OpenVoice-Docs):
- ruhiger Raum, **kein Hintergrundrauschen/Musik**
- nur **eine** Stimme, keine langen Pausen
- Handy-Sprachmemo reicht; WAV oder MP3, mono

Speichere die Datei z. B. als `meine_stimme.wav`.

## Schritt 2 — PC-Setup (Linux, einmalig)

Python 3.9 (Conda empfohlen). **GPU nicht nötig** — alles läuft auf CPU.

```bash
conda create -n openvoice python=3.9 && conda activate openvoice
git clone https://github.com/myshell-ai/OpenVoice.git
cd OpenVoice && pip install -e .

# Checkpoints: der offizielle S3-Link liefert derzeit HTTP 403 (Issues #381/#390).
# Funktionierender offizieller Hugging-Face-Mirror (MIT):
pip install -U "huggingface_hub[cli]"
huggingface-cli download myshell-ai/OpenVoiceV2 --local-dir checkpoints_v2
```

Hinweise:
- Die MeloTTS-Schritte aus der offiziellen USAGE.md (`pip install …MeloTTS…`,
  `python -m unidic download`) **weglassen** — wir brauchen MeloTTS nicht.
- Gebraucht werden `checkpoints_v2/converter/{config.json,checkpoint.pth}`.

Piper (deutsche Quell-Audios) dazu:

```bash
pip install piper-tts
python3 -m piper.download_voices de_DE-thorsten-high
```

## Schritt 3 — Deutsche Quell-WAVs erzeugen (Piper)

```bash
mkdir -p cues_piper cues_cloned
python3 - <<'PY'
import subprocess
CUES = [
    "Lass deinen Körper im Sitz zur Ruhe kommen.",
    "Nimm den Atem wahr, so wie er gerade ist.",
    "Spüre, wie die Luft ein- und wieder ausströmt.",
    "Es gibt gerade nichts zu tun.",
    "Wenn die Gedanken abschweifen, ist das in Ordnung.",
    "Komm sanft zurück zum Atem.",
    "Lass die Schultern weich werden.",
    "Spüre, wo dein Körper den Boden berührt.",
    "Gedanken dürfen kommen und gehen.",
    "Du musst ihnen nicht folgen.",
    "Kehre ruhig zu diesem Atemzug zurück.",
    "Was auch immer da ist, lass es da sein.",
]
for i, text in enumerate(CUES, 1):
    subprocess.run(
        ["python3", "-m", "piper", "-m", "de_DE-thorsten-high",
         "-f", f"cues_piper/cue_{i:02d}.wav", "--", text],
        check=True,
    )
    print(f"cue_{i:02d}.wav ✓")
PY
```

Tipp: Piper kennt `--length-scale` (z. B. `1.1` = ~10 % langsamer) — für einen noch
ruhigeren Meditations-Ton lohnt ein Versuch.

## Schritt 4 — Klangfarbe übertragen (OpenVoice)

```bash
python3 - <<'PY'
import glob, os, torch
from openvoice import se_extractor
from openvoice.api import ToneColorConverter

device = "cuda:0" if torch.cuda.is_available() else "cpu"
conv = ToneColorConverter("checkpoints_v2/converter/config.json", device=device)
# Wasserzeichen für den Privatgebrauch ok; sonst: ToneColorConverter(..., enable_watermark=False)
conv.load_ckpt("checkpoints_v2/converter/checkpoint.pth")

# Ziel-Klangfarbe: EINMAL aus deiner Referenz extrahieren.
target_se, _ = se_extractor.get_se("meine_stimme.wav", conv, vad=True)

for src in sorted(glob.glob("cues_piper/cue_*.wav")):
    # Quell-Klangfarbe je Datei (Thorsten) — wie im offiziellen Cross-Lingual-Demo.
    source_se, _ = se_extractor.get_se(src, conv, vad=True)
    out = os.path.join("cues_cloned", os.path.basename(src))
    conv.convert(audio_src_path=src, src_se=source_se, tgt_se=target_se,
                 output_path=out, message="@MyShell")
    print(out, "✓")
PY
```

Laufzeit auf CPU: die Embedding-Extraktion (Whisper-VAD) ist der langsamste Teil;
die Konvertierung selbst dauert Sekunden pro Satz. **Höre die Ergebnisse an** — falls die
Klangfarbe zu schwach durchkommt, hilft eine längere/sauberere Referenzaufnahme am meisten.

## Schritt 5 — Nachschliff (empfohlen)

Pegel vereinheitlichen und Stille an den Rändern kürzen:

```bash
for f in cues_cloned/cue_*.wav; do
  ffmpeg -y -loglevel error -i "$f" \
    -af "silenceremove=start_periods=1:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse,loudnorm=I=-20:TP=-2" \
    -ar 22050 -ac 1 "final/$(basename "$f")"
done
```

(`-20 LUFS` ist bewusst leise/ruhig; Ordner `final/` vorher anlegen.)

## Schritt 6 — In die App bringen

Die App ist dafür schon gut vorbereitet: Sprach-Engines stecken hinter dem gemeinsamen
Interface **`cue/SpeechEngine.kt`** (`available/ensureInit/speak/stop/release`), und der
`MeditationAudioCoordinator` wählt die aktive Engine. Der Anbau ist deshalb klein:

1. **Neue Engine `CueAudioSpeechGuide : SpeechEngine`** (≈100 Zeilen): hält eine Zuordnung
   *Cue-Text → Audiodatei*, spielt sie per `MediaPlayer`/`AudioTrack` ab (USAGE_MEDIA, wie
   `VoiceSamplePlayer`), respektiert Medienlautstärke 0.
2. **Dateiablage:** `filesDir/voice_custom/cue_01.wav …` — per einfachem **Import-Button** in
   den Einstellungen (SAF-Ordnerwahl) vom PC übertragen (`adb push` geht fürs Erste auch:
   `adb push final/. /sdcard/Download/atemkraft_voice/` + Import aus Download).
3. **Koordinator:** Engine-Wahl erweitern: eigene Stimme (falls importiert) → Piper-Katalog →
   System-TTS.
4. **Einstellungen:** im Stimmen-Katalog eine Zeile „Eigene Stimme (importiert)" mit
   Vorhören/Löschen — die `VoiceRow`-Bausteine existieren bereits.

> Diesen App-Anbau setze ich auf Zuruf um — die Schritte 1–5 kannst du unabhängig davon
> schon durchspielen und die WAVs anhören.

---

## Alternative: OmniVoice (höchste Klon-Qualität) — mit Lizenz-Haken

[OmniVoice](https://github.com/k2-fsa/OmniVoice) (k2-fsa, 03/2026) klont zero-shot aus nur
**3–10 s** Referenz (Transkript optional, Whisper transkribiert automatisch), Deutsch ist mit
~22.000 h Trainingsdaten Top-Tier. Erst im Browser testen:
<https://huggingface.co/spaces/k2-fsa/OmniVoice>

```bash
pip install omnivoice          # Python ≥ 3.10; GPU stark empfohlen (fp16: 40x Echtzeit)
omnivoice-infer --model k2-fsa/OmniVoice \
  --text "Komm sanft zurück zum Atem." \
  --ref_audio meine_stimme.wav --output cue.wav
```

**⚠ Wichtig:** Der *Code* ist Apache-2.0, die **Modell-Gewichte sind CC-BY-NC
(nicht-kommerziell)** — für rein private Cue-WAVs in Ordnung, für eine kommerziell
vertriebene App nicht. Außerdem: ~2,4 GB Modell, ohne GPU sehr langsam.

## Warum (noch) kein Live-Klonen auf dem Handy?

Geprüft am 2026-08-23:
- **sherpa-onnx hat keine OmniVoice-Unterstützung** (Code-Suche: 0 Treffer; Feature-Requests
  #3486/#3651 unbeantwortet bzw. vom Ersteller selbst geschlossen: „too big for edge").
- Community-ONNX-Exports existieren, aber: auf einem Desktop-**i9** liegt der Real-Time-Factor
  bei **2,8–6,7** (int8-hq) — ein Handy wäre nochmals ~10× langsamer; ein 5-s-Satz bräuchte
  Minuten. RAM-Bedarf ≥ 2 GB.
- Das einzige zero-shot-Cloning, das sherpa-onnx heute kann (**ZipVoice**), spricht nur
  Chinesisch/Englisch.

→ Deshalb: **am PC klonen, WAVs mitnehmen.** Sollte sherpa-onnx später ein kleines
Cloning-Modell mit Deutsch bekommen, passt es dank `SpeechEngine`-Interface direkt in die App.

## Lizenz-Übersicht

| Baustein | Lizenz | Nutzung hier |
|---|---|---|
| OpenVoice v2 (Code + Checkpoints) | MIT | Klangfarben-Übertragung ✓ |
| Piper `de_DE-thorsten-high` (Modell) | Stimme „Thorsten": CC0 | deutsche Quell-Audios ✓ |
| piper1-gpl (CLI) | GPL-3.0 | nur lokal ausführen ✓ (nichts davon wird verteilt) |
| OmniVoice Code / **Gewichte** | Apache-2.0 / **CC-BY-NC** | optional, nur privat |
| Eigene WAV-Ergebnisse | deine Aufnahme + CC0/MIT-Kette | frei nutzbar |

## Quellen

- OpenVoice: [Repo](https://github.com/myshell-ai/OpenVoice) ·
  [USAGE.md](https://github.com/myshell-ai/OpenVoice/blob/main/docs/USAGE.md) ·
  [QA.md](https://github.com/myshell-ai/OpenVoice/blob/main/docs/QA.md) ·
  [Cross-Lingual-Demo (inkl. Deutsch)](https://github.com/myshell-ai/OpenVoice/blob/main/demo_part2.ipynb) ·
  [Checkpoints (HF, MIT)](https://huggingface.co/myshell-ai/OpenVoiceV2) ·
  [S3-403-Issue #381](https://github.com/myshell-ai/OpenVoice/issues/381)
- Piper: [piper1-gpl](https://github.com/OHF-Voice/piper1-gpl) ·
  [CLI-Doku](https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/CLI.md) ·
  [Stimmen (HF)](https://huggingface.co/rhasspy/piper-voices)
- OmniVoice: [Repo](https://github.com/k2-fsa/OmniVoice) ·
  [HF-Modell + Lizenzhinweis](https://huggingface.co/k2-fsa/OmniVoice) ·
  [Sprachen (Deutsch: Zeile 193)](https://github.com/k2-fsa/OmniVoice/blob/master/docs/languages.md) ·
  [Browser-Demo](https://huggingface.co/spaces/k2-fsa/OmniVoice) ·
  [sherpa-onnx #3486](https://github.com/k2-fsa/sherpa-onnx/issues/3486) /
  [#3651](https://github.com/k2-fsa/sherpa-onnx/issues/3651) ·
  [Community-ONNX + CPU-RTF](https://github.com/AFun9/Omnivoice-onnx)
