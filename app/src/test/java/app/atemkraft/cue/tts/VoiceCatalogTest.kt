package app.atemkraft.cue.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integrität des Stimmen-Katalogs: eindeutige Ids, gepinnte Prüfsummen, Ableitungen aus dem Piper-Schema. */
class VoiceCatalogTest {

    @Test
    fun `alle Ids und Sample-Assets sind eindeutig`() {
        val ids = VoiceCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val samples = VoiceCatalog.all.map { it.sampleAsset }
        assertEquals(samples.size, samples.toSet().size)
    }

    @Test
    fun `jeder Eintrag hat gepinnte Groesse und SHA-256`() {
        val hex64 = Regex("^[0-9a-f]{64}$")
        VoiceCatalog.all.forEach { spec ->
            assertTrue("${spec.id}: sha256 muss 64 Hex-Zeichen (klein) haben", hex64.matches(spec.sha256))
            assertTrue("${spec.id}: sizeBytes muss > 0 sein", spec.sizeBytes > 0L)
            assertTrue("${spec.id}: approxMb muss > 0 sein", spec.approxMb > 0)
        }
        val hashes = VoiceCatalog.all.map { it.sha256 }
        assertEquals("Prüfsummen doppelt – Copy-Paste-Fehler?", hashes.size, hashes.toSet().size)
    }

    @Test
    fun `approxMb ist die abgerundete MiB-Groesse`() {
        assertEquals(110, VoiceCatalog.byId("thorsten")!!.approxMb)
        assertEquals(25, VoiceCatalog.byId("eva")!!.approxMb)
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
