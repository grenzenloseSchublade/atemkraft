package app.atemkraft.cue.tts

/** Geschlecht/Charakter einer Stimme – steuert Icon/Label im UI. */
enum class VoiceGender { MALE, FEMALE, SPECIAL }

/**
 * Beschreibung einer neuronalen deutschen Stimme (Piper via sherpa-onnx). Alle Modelle stammen
 * aus der offiziellen sherpa-onnx `tts-models`-Release und sind frei lizenziert (Piper MIT,
 * Stimmen je nach Sprecher). [onnxName] und [tarUrl] werden aus [dirName] abgeleitet (Piper-Schema).
 *
 * [sizeBytes] und [sha256] sind auf das Release-Archiv **gepinnt**: [VoiceModelManager] bricht
 * ab, wenn der Download davon abweicht. Wird ein Modell upstream neu hochgeladen, schlägt der
 * Download fehl, bis die Werte hier aktualisiert sind (`curl -L <tarUrl> | sha256sum`).
 */
data class VoiceSpec(
    val id: String,
    val displayName: String,
    val gender: VoiceGender,
    /** Kurzes Qualitäts-/Charakter-Label fürs UI (deutsch). */
    val qualityLabel: String,
    /** Ordnername im Tarball (= Zielordner). */
    val dirName: String,
    /** Asset-Pfad des Vorhör-Clips (funktioniert vor dem Download). */
    val sampleAsset: String,
    /** Exakte Größe des Archivs in Bytes (gepinnt). */
    val sizeBytes: Long,
    /** SHA-256 des Archivs, 64 Hex-Zeichen klein (gepinnt). */
    val sha256: String,
) {
    /** Download-Größe in MiB, abgerundet (für den UI-Hinweis). */
    val approxMb: Int get() = (sizeBytes / (1024L * 1024L)).toInt()

    /** Dateiname des ONNX-Modells im Ordner (Piper: dirName ohne „vits-piper-“ + .onnx). */
    val onnxName: String get() = dirName.removePrefix("vits-piper-") + ".onnx"

    val tarUrl: String get() = "$RELEASE_BASE/$dirName.tar.bz2"

    companion object {
        private const val RELEASE_BASE =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
    }
}

/** Kuratierter Katalog freier deutscher Stimmen (beste zuerst). */
object VoiceCatalog {
    val all: List<VoiceSpec> = listOf(
        VoiceSpec(
            "thorsten", "Thorsten", VoiceGender.MALE, "Studio · sehr klar",
            "vits-piper-de_DE-thorsten-high", "voice_samples/thorsten.mp3",
            sizeBytes = 115_591_546L,
            sha256 = "dd4ed1b0d42c30a1a4862fc2b243e8044d52b8889c9ff3d1e99e92028888bc4a",
        ),
        VoiceSpec(
            "miro", "Miro", VoiceGender.MALE, "Studio",
            "vits-piper-de_DE-miro-high", "voice_samples/miro.mp3",
            sizeBytes = 67_160_790L,
            sha256 = "8226bb45d3684286981a55b3e0937e373f37673036a7eee27fc51def2d8c926d",
        ),
        VoiceSpec(
            "dii", "Dii", VoiceGender.MALE, "Studio",
            "vits-piper-de_DE-dii-high", "voice_samples/dii.mp3",
            sizeBytes = 67_200_058L,
            sha256 = "64d46d39afb0f53a49871f5415259ffe474f443696148930b34a003612b69110",
        ),
        VoiceSpec(
            "kerstin", "Kerstin", VoiceGender.FEMALE, "Einfach",
            "vits-piper-de_DE-kerstin-low", "voice_samples/kerstin.mp3",
            sizeBytes = 67_107_768L,
            sha256 = "4bfe121d5f690a3b87acecd7ae1e70b48f4589ce6ae9f45a1809e6eb19fad97b",
        ),
        VoiceSpec(
            "ramona", "Ramona", VoiceGender.FEMALE, "Einfach",
            "vits-piper-de_DE-ramona-low", "voice_samples/ramona.mp3",
            sizeBytes = 67_084_795L,
            sha256 = "79f985c10f86c4d58205b519abc488aa65eb713d54665ff729880a02e2625a42",
        ),
        VoiceSpec(
            "eva", "Eva", VoiceGender.FEMALE, "Einfach · kompakt",
            "vits-piper-de_DE-eva_k-x_low", "voice_samples/eva.mp3",
            sizeBytes = 26_521_242L,
            sha256 = "daec8658456d8e5ec714972f2e5a71f8a84d58b974db396f4eaeace0df06455a",
        ),
        VoiceSpec(
            "glados", "GLaDOS", VoiceGender.SPECIAL, "Roboter · Spaß",
            "vits-piper-de_DE-glados-high", "voice_samples/glados.mp3",
            sizeBytes = 115_594_308L,
            sha256 = "77a041d0f7a0a6a27bf6e260aa5de0d17ad0772d46a815e83f0594bd498291f6",
        ),
    )

    fun byId(id: String?): VoiceSpec? = all.firstOrNull { it.id == id }
}
