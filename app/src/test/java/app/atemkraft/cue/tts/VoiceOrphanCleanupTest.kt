package app.atemkraft.cue.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

/**
 * Aufräumen von Stimmordnern, die zu keiner Katalog-Stimme mehr gehören (GLaDOS bis 1.5.1):
 * nur passende Unterordner von `files/tts`, Symlinks werden nie verfolgt.
 */
class VoiceOrphanCleanupTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun dirWithModel(parent: File, name: String): File = File(parent, name).apply {
        File(this, "espeak-ng-data/de").mkdirs()
        File(this, "tokens.txt").writeText("a 1")
        File(this, "model.onnx").writeBytes(ByteArray(16))
    }

    @Test
    fun `verwaister Stimmordner und sein Staging-Ordner werden geloescht, Katalog-Stimmen bleiben`() {
        val base = tmp.newFolder("tts")
        val thorsten = VoiceCatalog.byId("thorsten")!!
        val known = dirWithModel(base, thorsten.dirName)
        val knownStaging = dirWithModel(base, ".${thorsten.dirName}.staging")
        dirWithModel(base, "vits-piper-de_DE-glados-high")
        dirWithModel(base, ".vits-piper-de_DE-glados-high.staging")

        val deleted = deleteOrphanVoiceDirs(base, VoiceCatalog.all)

        assertEquals(
            setOf("vits-piper-de_DE-glados-high", ".vits-piper-de_DE-glados-high.staging"),
            deleted.toSet(),
        )
        assertTrue(File(known, "tokens.txt").isFile)
        assertTrue(knownStaging.isDirectory)
        assertEquals(setOf(known.name, knownStaging.name), base.list()!!.toSet())
    }

    @Test
    fun `fremde Namen und Dateien bleiben unangetastet`() {
        val base = tmp.newFolder("tts")
        val other = dirWithModel(base, "eigener-ordner")
        val file = File(base, "vits-piper-de_DE-x-high").apply { writeText("keine Stimme") }

        assertEquals(emptyList<String>(), deleteOrphanVoiceDirs(base, VoiceCatalog.all))
        assertTrue(other.isDirectory)
        assertTrue(file.isFile)
    }

    @Test
    fun `Symlinks werden nicht verfolgt, ihr Ziel bleibt erhalten`() {
        val base = tmp.newFolder("tts")
        val outside = tmp.newFolder("draussen")
        val precious = File(outside, "wichtig.txt").apply { writeText("bleibt") }

        // Ein Link mit Stimmnamen direkt in tts: wird weder verfolgt noch gelöscht.
        val topLink = File(base, "vits-piper-de_DE-link-high")
        Files.createSymbolicLink(topLink.toPath(), outside.toPath())
        // Ein Link innerhalb eines verwaisten Ordners: nur der Link verschwindet.
        val orphan = dirWithModel(base, "vits-piper-de_DE-glados-high")
        Files.createSymbolicLink(File(orphan, "espeak-ng-data/aussen").toPath(), outside.toPath())

        val deleted = deleteOrphanVoiceDirs(base, VoiceCatalog.all)

        assertEquals(listOf("vits-piper-de_DE-glados-high"), deleted)
        assertFalse(orphan.exists())
        assertTrue(Files.isSymbolicLink(topLink.toPath()))
        assertEquals("bleibt", precious.readText())
    }

    @Test
    fun `fehlendes tts-Verzeichnis ist kein Fehler`() {
        assertEquals(emptyList<String>(), deleteOrphanVoiceDirs(File(tmp.root, "gibtsnicht"), VoiceCatalog.all))
    }
}
