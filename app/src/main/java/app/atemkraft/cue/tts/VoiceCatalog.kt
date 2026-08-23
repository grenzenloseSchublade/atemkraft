package app.atemkraft.cue.tts

/** Geschlecht/Charakter einer Stimme – steuert Icon/Label im UI. */
enum class VoiceGender { MALE, FEMALE, SPECIAL }

/**
 * Beschreibung einer neuronalen deutschen Stimme (Piper via sherpa-onnx). Alle Modelle stammen
 * aus der offiziellen sherpa-onnx `tts-models`-Release und sind frei lizenziert (Piper MIT,
 * Stimmen je nach Sprecher). [onnxName] und [tarUrl] werden aus [dirName] abgeleitet (Piper-Schema).
 */
data class VoiceSpec(
    val id: String,
    val displayName: String,
    val gender: VoiceGender,
    /** Kurzes Qualitäts-/Charakter-Label fürs UI (deutsch). */
    val qualityLabel: String,
    /** Ordnername im Tarball (= Zielordner). */
    val dirName: String,
    /** Ungefähre Download-Größe in MB (für den UI-Hinweis). */
    val approxMb: Int,
    /** Asset-Pfad des Vorhör-Clips (funktioniert vor dem Download). */
    val sampleAsset: String,
) {
    /** Dateiname des ONNX-Modells im Ordner (Piper: dirName ohne „vits-piper-" + .onnx). */
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
        VoiceSpec("thorsten", "Thorsten", VoiceGender.MALE, "Studio · sehr klar",
            "vits-piper-de_DE-thorsten-high", 110, "voice_samples/thorsten.mp3"),
        VoiceSpec("miro", "Miro", VoiceGender.MALE, "Studio",
            "vits-piper-de_DE-miro-high", 64, "voice_samples/miro.mp3"),
        VoiceSpec("dii", "Dii", VoiceGender.MALE, "Studio",
            "vits-piper-de_DE-dii-high", 64, "voice_samples/dii.mp3"),
        VoiceSpec("kerstin", "Kerstin", VoiceGender.FEMALE, "Einfach",
            "vits-piper-de_DE-kerstin-low", 63, "voice_samples/kerstin.mp3"),
        VoiceSpec("ramona", "Ramona", VoiceGender.FEMALE, "Einfach",
            "vits-piper-de_DE-ramona-low", 63, "voice_samples/ramona.mp3"),
        VoiceSpec("eva", "Eva", VoiceGender.FEMALE, "Einfach · kompakt",
            "vits-piper-de_DE-eva_k-x_low", 25, "voice_samples/eva.mp3"),
        VoiceSpec("glados", "GLaDOS", VoiceGender.SPECIAL, "Roboter · Spaß",
            "vits-piper-de_DE-glados-high", 110, "voice_samples/glados.mp3"),
    )

    fun byId(id: String?): VoiceSpec? = all.firstOrNull { it.id == id }
}
