# Atemkraft – minimale App, kaum Reflection-Abhängigkeiten.
# Standardregeln aus proguard-android-optimize.txt reichen für den App-Code.

# sherpa-onnx: Die Klassen in com.k2fsa.sherpa.onnx enthalten native Methoden und Felder, die
# per JNI vom C++-Code gelesen/aufgerufen werden (OfflineTts, OfflineTts*ModelConfig, GeneratedAudio,
# Callbacks). R8 darf sie NICHT umbenennen oder entfernen – sonst bricht die neuronale Sprachausgabe.
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class com.k2fsa.sherpa.onnx.** { *; }
