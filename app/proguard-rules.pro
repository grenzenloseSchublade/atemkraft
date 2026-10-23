# Atemkraft – minimale App, kaum Reflection-Abhängigkeiten.
# Standardregeln aus proguard-android-optimize.txt reichen für den App-Code.

# sherpa-onnx: Atemkraft nutzt nur die Sprachsynthese. Deren C++-Code liest per JNI Felder der
# Konfigurationsklassen nach Namen und Typsignatur (OfflineTtsConfig → OfflineTtsModelConfig →
# OfflineTts*ModelConfig), ruft die nativen Methoden von OfflineTts und GeneratedAudio über ihre
# Java-Namen auf und erzeugt GeneratedAudio über den Konstruktor (float[], int); GenerationConfig
# liest generateWithConfig. Diese Klassen darf R8 weder umbenennen noch kürzen – sonst bricht die
# neuronale Stimme erst zur Laufzeit. Ermittelt aus den Klassennamen in libsherpa-onnx-jni.so
# (`strings`); bei einem neuen sherpa-onnx-Release dort erneut prüfen. Spracherkennung,
# Diarisierung usw. bleiben ungenutzt und fallen weg.
-keep class com.k2fsa.sherpa.onnx.OfflineTts** { *; }
-keep class com.k2fsa.sherpa.onnx.GeneratedAudio { *; }
-keep class com.k2fsa.sherpa.onnx.GenerationConfig { *; }
