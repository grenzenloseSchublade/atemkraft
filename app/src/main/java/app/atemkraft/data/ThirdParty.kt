package app.atemkraft.data

/**
 * Ein Fremdbaustein in der App mit Lizenz und Rechteinhaber, für die Liste „Lizenzen“ im
 * Über-Screen. Quelle der Wahrheit samt Versionen, Belegen und Volltexten ist
 * THIRD_PARTY_LICENSES.md im Repo; die Lizenztexte liegen der App unter `assets/licenses/` bei.
 */
data class ThirdPartyComponent(val name: String, val license: String, val holder: String)

/**
 * Alles, was in der APK steckt: Gradle-Abhängigkeiten (`releaseRuntimeClasspath`) und die
 * Native-Bibliotheken aus dem sherpa-onnx-AAR (statisch gelinkte Teile laut Build-Skripten von
 * sherpa-onnx v1.13.6 und Zeichenketten in `libsherpa-onnx-jni.so`). Die Stimmen stehen im
 * Stimmen-Katalog (`VoiceCatalog`), nicht hier.
 */
object ThirdParty {
    val components: List<ThirdPartyComponent> = listOf(
        ThirdPartyComponent("sherpa-onnx 1.13.6", "Apache-2.0", "Xiaomi Corporation, k2-fsa"),
        ThirdPartyComponent("eSpeak NG (in sherpa-onnx)", "GPL-3.0 oder neuer", "Jonathan Duddington, Reece H. Dunn und weitere"),
        ThirdPartyComponent("piper-phonemize (in sherpa-onnx)", "MIT", "Michael Hansen"),
        ThirdPartyComponent("ONNX Runtime 1.27.1", "MIT", "Microsoft Corporation"),
        ThirdPartyComponent(
            "OpenFst, kaldifst, kaldi-decoder, kaldi-native-fbank, simple-sentencepiece (in sherpa-onnx)",
            "Apache-2.0",
            "Google (OpenFst), k2-fsa und weitere",
        ),
        ThirdPartyComponent("Eigen (in sherpa-onnx)", "MPL-2.0", "Eigen-Projekt"),
        ThirdPartyComponent("hclust-cpp (in sherpa-onnx)", "BSD-2-Clause", "Daniel Müllner, Christoph Dalitz"),
        ThirdPartyComponent("JSON for Modern C++ (in sherpa-onnx)", "MIT", "Niels Lohmann"),
        ThirdPartyComponent("uni-algo (in piper-phonemize)", "Unlicense oder MIT", "uni-algo-Projekt"),
        ThirdPartyComponent("AndroidX, Jetpack Compose, Material Icons", "Apache-2.0", "The Android Open Source Project, Google"),
        ThirdPartyComponent("Kotlin, kotlinx.coroutines, kotlinx.serialization", "Apache-2.0", "JetBrains"),
        ThirdPartyComponent("Apache Commons Compress, IO, Lang, Codec", "Apache-2.0", "The Apache Software Foundation"),
        ThirdPartyComponent("Okio", "Apache-2.0", "Square"),
        ThirdPartyComponent("Guava ListenableFuture", "Apache-2.0", "Google"),
    )
}
