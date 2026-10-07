# Atemkraft – Lizenzen und Drittkomponenten

Was in der App steckt, was sie auf Wunsch lädt und unter welchen Bedingungen. Gegenstück in der App: *Einstellungen → Über Atemkraft → Lizenzen*.

- **Stand:** 2026-10-07, App-Version 1.5.1 (`versionCode` 17).
- **Belege:** Lizenzangaben aus Maven-POMs, `LICENSE`/`COPYING`-Dateien der Upstream-Repos am genannten Tag oder Commit und den Modellkarten der Stimmen. Der Inhalt der APK ist mit `unzip -l` an der Release-APK v1.5.1 geprüft, die gebündelten Bibliotheken über die Build-Skripte von sherpa-onnx v1.13.6 und Zeichenketten in `libsherpa-onnx-jni.so`.
- **Keine Rechtsberatung.** Die Einordnungen unten geben Lizenztexte und veröffentlichte Auffassungen wieder.

## Kurzfassung

| Teil | Lizenz | Bedeutung |
|---|---|---|
| Quellcode von Atemkraft (dieses Repo) | MIT ([LICENSE](LICENSE)) | frei nutzbar, auch kommerziell, mit Copyright-Hinweis |
| Verteilte APK | enthält Bausteine unter Apache-2.0, MIT, BSD-2-Clause, MPL-2.0, Unlicense und **eSpeak NG unter GPL-3.0-or-later** | siehe [GPL und die APK](#gpl) |
| Stimmmodelle | je Stimme verschieden, teils nur nicht kommerziell, eine ohne Lizenzangabe | **nicht in der APK**; die App lädt sie auf Wunsch direkt von GitHub ([Stimmen](#stimmen)) |

<a id="gpl"></a>
### GPL und die APK

eSpeak NG ist über piper-phonemize statisch in `libsherpa-onnx-jni.so` gelinkt (Zeichenketten wie `Wrong version of espeak-ng-data` und `phondata` in der Bibliothek). Die Free Software Foundation vertritt die Auffassung, dass ein Programm, das mit einer GPL-Bibliothek gelinkt ist, mit ihr ein kombiniertes Werk bildet, für das als Ganzes die GPL gilt – gleich ob statisch oder dynamisch gelinkt ([GPL-FAQ „IfLibraryIsGPL“](https://www.gnu.org/licenses/gpl-faq.html#IfLibraryIsGPL), [„GPLStaticVsDynamic“](https://www.gnu.org/licenses/gpl-faq.html#GPLStaticVsDynamic)). Nach dieser Auffassung gilt für die **Weitergabe der APK** die GPL-3.0; die übrigen Lizenzen (MIT, Apache-2.0, BSD-2-Clause, MPL-2.0, Unlicense) sind laut FSF mit ihr vereinbar ([Lizenzliste der FSF](https://www.gnu.org/licenses/license-list.html)). Der Quellcode von Atemkraft bleibt davon unberührt MIT-lizenziert und darf auch ohne die GPL-Teile weiterverwendet werden.

Was das praktisch heißt (GPL-3.0 §§ 4–6): Wer die APK weitergibt, gibt die Lizenztexte mit (sie liegen in der APK unter `assets/licenses/`) und macht den vollständigen Quellcode zugänglich. Dazu gehören dieses Repo und der Quellstand von sherpa-onnx samt den gebündelten Bibliotheken, siehe [Quellcode der Native-Bibliotheken](#quellcode). Wer nur Links weitergibt, bleibt nach § 6 d selbst dafür verantwortlich, dass der Quellcode erreichbar bleibt.

## 1 In der APK: Kotlin- und Java-Bibliotheken

Ermittelt mit `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`. Alle stehen unter Apache-2.0; den Lizenztext enthält [`Apache-2.0.txt`](app/src/main/assets/licenses/Apache-2.0.txt), die Hinweise nach Apache-2.0 § 4 d [`NOTICE.txt`](app/src/main/assets/licenses/NOTICE.txt).

| Komponente | Version | Lizenz | Rechteinhaber / Quelle | Hinweis |
|---|---|---|---|---|
| AndroidX Core, Activity, Lifecycle, Navigation, Room, SQLite, DataStore, SavedState, Startup, Emoji2, Profile Installer, Tracing, Collection, Annotation, Arch Core, Concurrent Futures, Graphics Path u. a. | Core 1.15.0, Activity 1.9.3, Lifecycle 2.8.7, Navigation 2.8.5, Room 2.6.1, SQLite 2.4.0, DataStore 1.1.1 | Apache-2.0 | The Android Open Source Project ([androidx/androidx](https://github.com/androidx/androidx)) | bringt die Native-Bibliotheken `libandroidx.graphics.path.so` (Graphics Path) und `libdatastore_shared_counter.so` (DataStore) mit |
| Jetpack Compose (UI, Foundation, Animation, Runtime, Material 3, Material Icons Core, Ripple) | BOM 2025.01.00: UI 1.7.6, Material 3 1.3.1 | Apache-2.0 | The Android Open Source Project | – |
| Kotlin Standardbibliothek | 2.1.0 (jdk7/jdk8-Teile 1.8.22, Parcelize-Runtime 1.9.22) | Apache-2.0 | JetBrains s.r.o. ([JetBrains/kotlin](https://github.com/JetBrains/kotlin)) | NOTICE |
| kotlinx.coroutines | 1.7.3 | Apache-2.0 | JetBrains s.r.o. | NOTICE |
| kotlinx.serialization | 1.7.3 | Apache-2.0 | JetBrains s.r.o. | NOTICE |
| JetBrains Annotations | 23.0.0 | Apache-2.0 | JetBrains s.r.o. | nur Annotationen |
| Apache Commons Compress | 1.27.1 | Apache-2.0 | The Apache Software Foundation | entpackt die Stimm-Archive; NOTICE |
| Apache Commons IO, Lang, Codec | 2.16.1, 3.16.0, 1.17.1 | Apache-2.0 | The Apache Software Foundation | Abhängigkeiten von Commons Compress; NOTICE |
| Okio | 3.4.0 | Apache-2.0 | Square, Inc. | Abhängigkeit von DataStore |
| Guava ListenableFuture | 1.0 | Apache-2.0 | Google | Abhängigkeit von AndroidX |

## 2 In der APK: Native Bibliotheken aus sherpa-onnx

Quelle: `app/libs/sherpa-onnx-1.13.6.aar`, byte-gleich mit dem Asset der Release [v1.13.6 von k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.6) (SHA-256 `0012d9a2…1698`, siehe [SECURITY.md](docs/SECURITY.md#lieferkette)). Die APK enthält daraus `libsherpa-onnx-jni.so`, `libsherpa-onnx-c-api.so`, `libsherpa-onnx-cxx-api.so` und `libonnxruntime.so` für `arm64-v8a` und `armeabi-v7a`. Versionen und Commits der gebündelten Bibliotheken stammen aus `cmake/*.cmake` und `build-android-arm64-v8a.sh` am Tag v1.13.6.

| Komponente | Version / Commit | Lizenz | Rechteinhaber / Quelle | Lizenztext in der APK |
|---|---|---|---|---|
| sherpa-onnx | 1.13.6 | Apache-2.0 | Xiaomi Corporation und Mitwirkende ([k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)) | `Apache-2.0.txt` |
| **eSpeak NG** (Fork für Piper) | Commit [`ed530aa`](https://github.com/csukuangfj/espeak-ng/tree/ed530aa113046142eb5115cf2fc9157854d0ffe1) von csukuangfj/espeak-ng (Fork von rhasspy/espeak-ng) | **GPL-3.0-or-later**; enthaltene Unicode-Daten unter der Unicode-Lizenz (`COPYING.UCD`) | Jonathan Duddington, Reece H. Dunn und Mitwirkende | `GPL-3.0.txt`, `espeak-ng-COPYING.UCD.txt` |
| piper-phonemize (Fork) | Commit [`f3ff95a`](https://github.com/csukuangfj/piper-phonemize/tree/f3ff95afc03640bc1399e113e83361192a2fafb4) von csukuangfj/piper-phonemize | MIT | Michael Hansen | `piper-phonemize-LICENSE.txt` |
| uni-algo (in piper-phonemize) | wie piper-phonemize | Unlicense oder MIT | uni-algo-Projekt | `uni-algo-LICENSE.txt` |
| ONNX Runtime | 1.27.1 (Binärpaket aus [csukuangfj/onnxruntime-libs](https://github.com/csukuangfj/onnxruntime-libs/releases/tag/v1.27.1), Quelle [microsoft/onnxruntime v1.27.1](https://github.com/microsoft/onnxruntime/tree/v1.27.1)) | MIT; enthaltene Drittbausteine laut `ThirdPartyNotices.txt` | Microsoft Corporation | `onnxruntime-LICENSE.txt`, `onnxruntime-ThirdPartyNotices.txt` |
| OpenFst (Fork) | [csukuangfj/openfst v1.8.5-2026-07-09](https://github.com/csukuangfj/openfst/tree/v1.8.5-2026-07-09) | Apache-2.0 | Google und Mitwirkende | `Apache-2.0.txt` |
| kaldifst | 1.8.0 ([k2-fsa/kaldifst](https://github.com/k2-fsa/kaldifst/tree/v1.8.0), über kaldi-decoder) | Apache-2.0 | k2-fsa und Mitwirkende | `Apache-2.0.txt` |
| kaldi-decoder | [0.3.0](https://github.com/k2-fsa/kaldi-decoder/tree/v0.3.0) | Apache-2.0 | k2-fsa und Mitwirkende | `Apache-2.0.txt` |
| kaldi-native-fbank | [1.22.3](https://github.com/csukuangfj/kaldi-native-fbank/tree/v1.22.3) | Apache-2.0 | k2-fsa und Mitwirkende | `Apache-2.0.txt` |
| simple-sentencepiece | [0.7](https://github.com/pkufool/simple-sentencepiece/tree/v0.7) | Apache-2.0 | pkufool und Mitwirkende | `Apache-2.0.txt` |
| Eigen (Header-Bibliothek, über kaldi-decoder) | laut `cmake/eigen.cmake` von sherpa-onnx v1.13.6 | MPL-2.0 | Eigen-Projekt ([libeigen/eigen](https://gitlab.com/libeigen/eigen)) | `MPL-2.0.txt`; Quellcode unter dem Link (MPL-2.0 § 3.2) |
| hclust-cpp (fastcluster) | [csukuangfj/hclust-cpp 2026-02-25](https://github.com/csukuangfj/hclust-cpp/tree/2026-02-25) | BSD-2-Clause | Daniel Müllner, Christoph Dalitz | `hclust-cpp-LICENSE.txt` |
| JSON for Modern C++ (Header-Bibliothek) | [3.12.0](https://github.com/nlohmann/json/tree/v3.12.0) | MIT | Niels Lohmann | `nlohmann-json-LICENSE.txt` |

Die sherpa-onnx-Bibliothek bringt auch Spracherkennung und Sprecher-Diarisierung mit; Atemkraft nutzt davon nur die Sprachsynthese.

<a id="quellcode"></a>
### Quellcode der Native-Bibliotheken

Der vollständige Quellstand des AAR ist der Tag [`v1.13.6` von k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.6) mit dem Build-Skript [`build-android-arm64-v8a.sh`](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.6/build-android-arm64-v8a.sh) (bzw. `build-android-armv7-eabi.sh`). Die Build-Skripte unter [`cmake/`](https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.6/cmake) laden die Bibliotheken der Tabelle oben mit fester URL und SHA-256, darunter eSpeak NG ([Quellarchiv des Commits `ed530aa`](https://github.com/csukuangfj/espeak-ng/archive/ed530aa113046142eb5115cf2fc9157854d0ffe1.zip)) und piper-phonemize ([Quellarchiv des Commits `f3ff95a`](https://github.com/csukuangfj/piper-phonemize/archive/f3ff95afc03640bc1399e113e83361192a2fafb4.zip)).

## 3 In der APK: eigene Ressourcen

| Teil | Herkunft | Lizenz |
|---|---|---|
| Symbole `Icons.*` | Material Icons aus `material-icons-core` (Google) | Apache-2.0 |
| `ic_bookmark`, `ic_bookmark_filled`, `ic_remove`, `ic_reset`, `ic_meditation` (Material „Timer“), `ic_sound_on`/`ic_sound_off` (Material „Volume Up/Off“) | Pfade aus den Material Icons/Symbols von Google | Apache-2.0 |
| App-Symbol (`ic_launcher_foreground`) | eigene Gestaltung (konzentrische Atemringe) | MIT wie die App |
| Töne und Gong | zur Laufzeit im Code synthetisiert (`ToneCuePlayer`, `ContinuousTonePlayer`); keine Audiodateien | MIT wie die App |
| Schriften | keine gebündelt; die App nutzt die Systemschrift | – |
| Hörproben `assets/voice_samples/*.mp3` (7 Dateien, je 2–4 s) | mit der jeweiligen Stimme erzeugt (Abtastrate passt zum Modell: 16 kHz bei „low“/„x_low“, 22,05 kHz bei „high“) | siehe [Stimmen](#stimmen). Ob eine Modelllizenz auch für damit erzeugtes Audio gilt, ist rechtlich nicht eindeutig; die Proben sind deshalb vorsorglich mit der Lizenz und Namensnennung ihrer Stimme aufgeführt. |
| Texte, Übungen, Quellenangaben | eigene Inhalte; Studien sind zitiert (`data/Refs.kt`), nicht übernommen | MIT wie die App |

<a id="stimmen"></a>
## 4 Stimmen (nicht in der APK)

Die App lädt eine Stimme nur nach ausdrücklichem Tippen aus der Release [`tts-models` von k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models) (Größe und SHA-256 in `VoiceCatalog.kt` gepinnt). Atemkraft verbreitet die Modelle nicht selbst. Jedes Archiv enthält neben dem Modell `espeak-ng-data` (Sprachdaten von eSpeak NG, GPL-3.0-or-later) und die Modellkarte bzw. ein README des Herausgebers.

| Stimme | Archiv | Lizenz | Rechteinhaber / Quelle | Hinweis |
|---|---|---|---|---|
| Thorsten | `vits-piper-de_DE-thorsten-high` | Trainingsdaten CC0 1.0; für das Modell selbst nennt die Karte keine eigene Lizenz | Thorsten Müller ([Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice)); Modell aus [rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices/blob/main/de/de_DE/thorsten/high/MODEL_CARD) (Repo-Metadaten: MIT) | laut Modellkarte feinjustiert aus der englischen Stimme „lessac“, deren Trainingsdaten (Blizzard Challenge 2013) unter einer [Forschungslizenz](https://www.cstr.ed.ac.uk/projects/blizzard/2013/lessac_blizzard2013/license.html) stehen |
| Miro | `vits-piper-de_DE-miro-high` | **CC BY-NC-ND 4.0** laut Rechteinhaber ([Modellkarte](https://huggingface.co/OpenVoiceOS/pipertts_de-DE_miro), so seit 2026-08-10; vorher ohne Lizenzangabe); das Archiv von sherpa-onnx nennt CC BY-NC-SA 4.0 | TigreGotico Lda ([tigregotico.pt](https://tigregotico.pt)); Stimme einer realen Person | **nur nicht kommerziell**; Namensnennung TigreGotico Lda; keine Bearbeitung, keine abgeleiteten Stimmen |
| Dii | `vits-piper-de_DE-dii-high` | wie Miro ([Modellkarte](https://huggingface.co/OpenVoiceOS/pipertts_de-DE_dii)) | TigreGotico Lda; Stimme einer realen Person | wie Miro; weibliche Stimme |
| Kerstin | `vits-piper-de_DE-kerstin-low` | Trainingsdaten CC0 1.0; keine eigene Modelllizenz genannt | Datensatz [rhasspy/dataset-voice-kerstin](https://github.com/rhasspy/dataset-voice-kerstin); Modell aus [rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices/blob/main/de/de_DE/kerstin/low/MODEL_CARD) | laut Modellkarte feinjustiert aus der englischen Stimme „ryan“, deren Trainingsdaten unter CC BY-NC-SA 4.0 stehen |
| Ramona | `vits-piper-de_DE-ramona-low` | Trainingsdaten unter der M-AILABS-Lizenz (BSD-artig: Weitergabe und kommerzielle Nutzung mit Copyright-Hinweis) | M-AILABS Speech Dataset, Aufnahmen von LibriVox (gemeinfrei); Modell aus [rhasspy/piper-voices](https://huggingface.co/rhasspy/piper-voices/blob/main/de/de_DE/ramona/low/MODEL_CARD) | von Grund auf trainiert; die Website des Datensatzes (caito.de) ist offline, Lizenztext über das [Internet Archive](https://web.archive.org/web/2023/https://www.caito.de/2019/01/03/the-m-ailabs-speech-dataset/) |
| Eva | `vits-piper-de_DE-eva_k-x_low` | wie Ramona | wie Ramona ([Modellkarte](https://huggingface.co/rhasspy/piper-voices/blob/main/de/de_DE/eva_k/x_low/MODEL_CARD)) | von Grund auf trainiert |
| GLaDOS | `vits-piper-de_DE-glados-high` | **keine Lizenzangabe** | [systemofapwne/piper-de-glados](https://huggingface.co/systemofapwne/piper-de-glados) | laut Autor aus den deutschen Sprachdateien von „Portal“ und „Portal 2“ trainiert, die er als geistiges Eigentum von Valve bezeichnet; Name und Figur gehören Valve. Die Rechtslage der Weitergabe ist ungeklärt. |

Copyright-Hinweis der M-AILABS-Lizenz: „Copyright (c) 2017-2019 by the original creators @ M-AILABS“.

## 5 Lizenztexte

Die Volltexte liegen im Repo unter [`app/src/main/assets/licenses/`](app/src/main/assets/licenses/) und damit in jeder APK unter `assets/licenses/`:

| Datei | Inhalt |
|---|---|
| `Apache-2.0.txt` | Apache License 2.0 (aus sherpa-onnx v1.13.6) |
| `NOTICE.txt` | NOTICE-Hinweise nach Apache-2.0 § 4 d: Apache Commons Compress, IO, Lang, Codec; Kotlin; kotlinx.coroutines; kotlinx.serialization |
| `GPL-3.0.txt` | GNU General Public License 3 (aus eSpeak NG, Commit `ed530aa`) |
| `espeak-ng-COPYING.UCD.txt` | Unicode-Lizenz der Zeichendaten in eSpeak NG |
| `MPL-2.0.txt` | Mozilla Public License 2.0 (aus Eigen 5.0.1) |
| `onnxruntime-LICENSE.txt`, `onnxruntime-ThirdPartyNotices.txt` | MIT-Lizenz und Drittanbieter-Hinweise von ONNX Runtime v1.27.1 |
| `piper-phonemize-LICENSE.txt` | MIT-Lizenz von piper-phonemize |
| `uni-algo-LICENSE.txt` | Unlicense/MIT von uni-algo |
| `hclust-cpp-LICENSE.txt` | BSD-2-Clause von hclust-cpp |
| `nlohmann-json-LICENSE.txt` | MIT-Lizenz von JSON for Modern C++ |

Lizenztexte der Stimmen: [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode), [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode), [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/legalcode).

## 6 Pflege

Bei jeder neuen Abhängigkeit, neuem AAR oder neuer Stimme im selben Commit: Tabelle hier, Liste in `data/ThirdParty.kt` bzw. `license`/`rightsHolder` in `VoiceCatalog.kt` und, falls nötig, ein Lizenztext unter `assets/licenses/` (SEC-LEGAL-03). `LicenseInventoryTest` prüft, dass App-Liste, Stimmen-Katalog und Lizenztexte hier genannt sind.
