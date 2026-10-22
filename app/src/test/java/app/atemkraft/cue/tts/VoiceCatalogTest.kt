package app.atemkraft.cue.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Integrität des Stimmen-Katalogs: eindeutige Ids, gepinnte Prüfsummen, Lizenzen, Hörproben. */
class VoiceCatalogTest {

    @Test
    fun `Katalog ist eindeutig, gepinnt und laedt nur aus der offiziellen Quelle`() {
        listOf<(VoiceSpec) -> Any>({ it.id }, { it.sampleAsset }, { it.sha256 }).forEach { key ->
            val values = VoiceCatalog.all.map(key)
            assertEquals("doppelt: $values", values.size, values.toSet().size)
        }
        val hex64 = Regex("^[0-9a-f]{64}$")
        VoiceCatalog.all.forEach { spec ->
            assertTrue("${spec.id}: sha256 muss 64 Hex-Zeichen (klein) haben", hex64.matches(spec.sha256))
            assertTrue("${spec.id}: sizeBytes muss > 0 sein", spec.sizeBytes > 0L)
            assertTrue(
                spec.tarUrl,
                spec.tarUrl.startsWith("https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/") &&
                    spec.tarUrl.endsWith("${spec.dirName}.tar.bz2"),
            )
        }
    }

    @Test
    fun `jede Stimme nennt Lizenz und Rechteinhaber (SEC-LEGAL-03)`() {
        VoiceCatalog.all.forEach { spec ->
            assertTrue("${spec.id}: license fehlt", spec.license.isNotBlank())
            assertTrue("${spec.id}: rightsHolder fehlt", spec.rightsHolder.isNotBlank())
        }
        // Nicht kommerzielle Stimmen müssen das im Kurzlabel zeigen (Modellkarte von TigreGotico).
        listOf("miro", "dii").forEach { id ->
            assertTrue(id, VoiceCatalog.byId(id)!!.license.contains("NC"))
            assertTrue("$id: Kennzeichnung „nur nicht kommerziell“ fehlt", VoiceCatalog.byId(id)!!.nonCommercialOnly)
        }
        // Gegenprobe: Lizenzen ohne NC lösen den Hinweis nicht aus.
        VoiceCatalog.all.filterNot { "NC" in it.license }.forEach { spec ->
            assertTrue("${spec.id}: fälschlich als nicht kommerziell markiert", !spec.nonCommercialOnly)
        }
    }

    @Test
    fun `jede Hoerprobe gehoert zu einer Stimme und umgekehrt`() {
        val dir = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
        val files = File(dir, "voice_samples").listFiles().orEmpty().map { "voice_samples/${it.name}" }.toSet()
        assertEquals(VoiceCatalog.all.map { it.sampleAsset }.toSet(), files)
    }
}
