package app.atemkraft.data

import app.atemkraft.cue.tts.VoiceCatalog
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * Lizenzen und Datenschutz bleiben beieinander (SEC-LEGAL-03, SEC-PRIV-05): Was die App in
 * „Über → Lizenzen“ zeigt, steht auch in THIRD_PARTY_LICENSES.md; jeder Lizenztext in der APK
 * ist dort genannt, ebenso jede Native-Bibliothek aus `app/libs`; die Datenschutzerklärung
 * existiert und ist im Über-Screen verlinkt.
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
    fun `THIRD_PARTY_LICENSES nennt die Zahl der Hoerproben und keine entfernte Stimme`() {
        val n = moduleFile("src/main/assets/voice_samples").listFiles().orEmpty().count { it.isFile }
        assertTrue("Hörproben-Zahl $n fehlt", inventory.contains("($n Dateien"))
        assertTrue("GLaDOS steht noch in der Liste", !inventory.contains("glados", ignoreCase = true))
    }

    /**
     * Einzige Quelle der Stimmen sind `VoiceCatalog.kt` und THIRD_PARTY_LICENSES.md: README,
     * Store-Texte, Datenschutzerklärung, Release-Notes und App-Texte nennen weder Stimmen beim
     * Namen noch ihre Anzahl. Eine Stimme hinzufügen oder entfernen heißt dann nur: Katalog,
     * Lizenzliste, Hörprobe. (Der historische Hinweis „GLaDOS entfernt“ ist erlaubt, weil GLaDOS
     * nicht mehr im Katalog steht.)
     */
    @Test
    fun `Stimmen stehen nur im Katalog und in der Lizenzliste`() {
        val docs = listOf(repoFile("README.md"), repoFile("docs/PRIVACY.md"), moduleFile("src/main/res/values/strings.xml")) +
            repoFile("docs/release-notes").listFiles().orEmpty().filter { it.name.endsWith(".md") } +
            repoFile("fastlane/metadata").walkTopDown().filter { it.isFile && it.name.endsWith(".txt") }.toList()
        val count = Regex("""(?<![\p{L}\d])(\d+|zwei|drei|vier|fünf|sechs|sieben|acht|neun|zehn)\s+(\p{L}+\s+)?Stimmen""", RegexOption.IGNORE_CASE)
        val findings = docs.filter { it.isFile }.flatMap { f ->
            val text = f.readText()
            VoiceCatalog.all.mapNotNull { spec ->
                Regex("""(?<![\p{L}\d])${Regex.escape(spec.displayName)}(?![\p{L}\d])""").find(text)?.let { "${f.name}: „${it.value}“" }
            } + listOfNotNull(count.find(text)?.let { "${f.name}: „${it.value}“" })
        }
        assertTrue("Stimmen außerhalb von Katalog und Lizenzliste:\n" + findings.joinToString("\n"), findings.isEmpty())
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
    fun `jede Native-Bibliothek aus app libs ist genannt`() {
        val aars = moduleFile("libs").listFiles().orEmpty().filter { it.name.endsWith(".aar") }
        assertTrue("kein AAR unter app/libs", aars.isNotEmpty())
        aars.forEach { aar ->
            assertTrue("${aar.name} fehlt in THIRD_PARTY_LICENSES.md", inventory.contains(aar.name))
            val libs = ZipFile(aar).use { zip ->
                zip.entries().asSequence().map { it.name }.filter { it.startsWith("jni/") && it.endsWith(".so") }
                    .map { it.substringAfterLast('/') }.toSet()
            }
            assertTrue("${aar.name} ohne Native-Bibliotheken", libs.isNotEmpty())
            libs.forEach { lib -> assertTrue("$lib aus ${aar.name} fehlt in THIRD_PARTY_LICENSES.md", inventory.contains(lib)) }
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
