package app.atemkraft.data

import app.atemkraft.cue.tts.VoiceCatalog
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Lizenzen und Datenschutz bleiben beieinander (SEC-LEGAL-03, SEC-PRIV-05): Was die App in
 * „Über → Lizenzen“ zeigt, steht auch in THIRD_PARTY_LICENSES.md; jeder Lizenztext in der APK
 * ist dort genannt; die Datenschutzerklärung existiert und ist im Über-Screen verlinkt.
 */
class LicenseInventoryTest {

    private val inventory by lazy { repoFile("THIRD_PARTY_LICENSES.md").readText() }

    @Test
    fun `jede Stimme steht mit Lizenz in THIRD_PARTY_LICENSES`() {
        VoiceCatalog.all.forEach { spec ->
            assertTrue("${spec.displayName} fehlt", inventory.contains("| ${spec.displayName} |"))
            assertTrue("${spec.dirName} fehlt", inventory.contains(spec.dirName))
        }
    }

    @Test
    fun `jeder Baustein der App-Liste steht in THIRD_PARTY_LICENSES`() {
        ThirdParty.components.forEach { c ->
            // Erstes Wort des Namens genügt („sherpa-onnx 1.13.6“ → „sherpa-onnx“).
            val key = c.name.substringBefore(' ').trimEnd(',')
            assertTrue("${c.name}: „$key“ fehlt", inventory.contains(key))
            assertTrue("${c.name}: Lizenz leer", c.license.isNotBlank() && c.holder.isNotBlank())
        }
    }

    @Test
    fun `jeder Lizenztext in der APK ist genannt und nicht leer`() {
        val dir = moduleFile("src/main/assets/licenses")
        val files = dir.listFiles().orEmpty().filter { it.isFile }
        assertTrue("keine Lizenztexte unter assets/licenses", files.size >= 5)
        listOf("Apache-2.0.txt", "GPL-3.0.txt", "NOTICE.txt").forEach { name ->
            assertTrue("$name fehlt", files.any { it.name == name })
        }
        files.forEach { f ->
            assertTrue("${f.name} ist leer", f.length() > 0)
            assertTrue("${f.name} fehlt in THIRD_PARTY_LICENSES.md", inventory.contains(f.name))
        }
    }

    @Test
    fun `Datenschutzerklaerung existiert und ist im Ueber-Screen verlinkt`() {
        assertTrue(repoFile("docs/PRIVACY.md").isFile)
        val about = moduleFile("src/main/java/app/atemkraft/ui/about/AboutScreen.kt").readText()
        assertTrue(
            "PRIVACY.md-Link fehlt im Über-Screen",
            about.contains("https://github.com/grenzenloseSchublade/atemkraft/blob/main/docs/PRIVACY.md"),
        )
        assertTrue(
            "THIRD_PARTY_LICENSES-Link fehlt im Über-Screen",
            about.contains("https://github.com/grenzenloseSchublade/atemkraft/blob/main/THIRD_PARTY_LICENSES.md"),
        )
    }

    private fun repoFile(path: String): File = listOf(File("../$path"), File(path)).firstOrNull { it.exists() } ?: File("../$path")

    private fun moduleFile(path: String): File = listOf(File(path), File("app/$path")).first { it.exists() }
}
