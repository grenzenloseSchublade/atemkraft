package app.atemkraft.cue.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integrität des Stimmen-Katalogs: eindeutige Ids, korrekte Ableitungen aus dem Piper-Schema. */
class VoiceCatalogTest {

    @Test
    fun `alle Ids und Sample-Assets sind eindeutig`() {
        val ids = VoiceCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val samples = VoiceCatalog.all.map { it.sampleAsset }
        assertEquals(samples.size, samples.toSet().size)
    }

    @Test
    fun `onnxName folgt dem Piper-Schema (dirName ohne Praefix + onnx)`() {
        val thorsten = VoiceCatalog.byId("thorsten")!!
        assertEquals("de_DE-thorsten-high.onnx", thorsten.onnxName)
        VoiceCatalog.all.forEach { spec ->
            assertTrue(spec.onnxName.endsWith(".onnx"))
            assertTrue(!spec.onnxName.startsWith("vits-piper-"))
        }
    }

    @Test
    fun `tarUrl zeigt auf die offizielle tts-models-Release`() {
        VoiceCatalog.all.forEach { spec ->
            assertTrue(
                spec.tarUrl,
                spec.tarUrl.startsWith(
                    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/",
                ) && spec.tarUrl.endsWith("${spec.dirName}.tar.bz2"),
            )
        }
    }

    @Test
    fun `byId liefert null fuer Unbekanntes`() {
        assertEquals(null, VoiceCatalog.byId("gibtsnicht"))
        assertEquals(null, VoiceCatalog.byId(null))
    }
}
