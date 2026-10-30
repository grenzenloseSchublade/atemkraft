package app.atemkraft.cue.tts

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

/**
 * SEC-NET-03/-04: Das Stimmen-Archiv wird nur angenommen, wenn es dem gepinnten Stand entspricht und
 * sicher entpackt werden kann. Jede Ablehnung hinterlässt nichts – weder den Download noch den
 * Staging-Ordner noch eine Datei außerhalb davon.
 */
class VoiceArchiveSafetyTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val payload = "sherpa".repeat(100).toByteArray()

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `Download mit falscher Pruefsumme oder Uebergroesse wird abgelehnt und geloescht`() {
        val target = File(folder.root, "stimme.tar.bz2.part")
        val sha = sha256(payload)

        // Falscher Hash bei richtiger Größe.
        assertThrows(VoiceIntegrityException::class.java) {
            copyVerified(ByteArrayInputStream(payload), target, payload.size.toLong(), sha256(payload + 1))
        }
        assertFalse(target.exists())
        // Mehr Bytes als gepinnt: Abbruch, bevor der Rest geschrieben ist.
        assertThrows(VoiceIntegrityException::class.java) {
            copyVerified(ByteArrayInputStream(payload + payload), target, payload.size.toLong(), sha)
        }
        assertFalse(target.exists())
        // Gegenprobe: der gepinnte Stand kommt an.
        copyVerified(ByteArrayInputStream(payload), target, payload.size.toLong(), sha)
        assertArrayEquals(payload, target.readBytes())
    }

    @Test
    fun `Archiv mit Pfadausbruch, Symlink, zu vielen Eintraegen oder zu viel Inhalt wird ohne Reste abgelehnt`() {
        val archives = folder.newFolder("download")
        val base = folder.newFolder("tts")
        val staging = File(base, ".vits-piper-test.staging")

        fun rejected(archive: File, maxEntries: Int = MAX_ARCHIVE_ENTRIES, maxBytes: Long = MAX_ARCHIVE_BYTES) {
            assertThrows(VoiceIntegrityException::class.java) {
                extractTarBz2Safely(archive, staging, maxEntries = maxEntries, maxBytes = maxBytes)
            }
            // Weder der Staging-Ordner noch etwas daneben (ein Pfadausbruch landete in tts/).
            assertEquals("Reste nach ${archive.name}", emptyList<String>(), base.list()!!.toList())
        }

        // Je ein sauberer Eintrag vorweg: Die Ablehnung greift mitten im Entpacken, es gibt etwas aufzuräumen.
        rejected(tar(archives, "ausbruch", "ok.txt" to payload, "../ausbruch.txt" to payload))
        rejected(tar(archives, "link", "ok.txt" to payload, link = "modell.onnx" to "/etc/passwd"))
        rejected(tar(archives, "viele", *Array(4) { "datei-$it.txt" to payload }), maxEntries = 3)
        rejected(tar(archives, "bombe", "ok.txt" to payload, "gross.bin" to payload), maxBytes = payload.size + 10L)

        // Gegenprobe: ein sauberes Archiv in denselben Grenzen wird entpackt.
        extractTarBz2Safely(tar(archives, "sauber", *Array(3) { "dir/datei-$it.txt" to payload }), staging, maxEntries = 3)
        assertArrayEquals(payload, File(staging, "dir/datei-2.txt").readBytes())
    }

    /** Ein `.tar.bz2` in [dir] mit den Dateien [files] (Pfad → Inhalt) und optional einem Symlink [link] (Name → Ziel). */
    private fun tar(dir: File, name: String, vararg files: Pair<String, ByteArray>, link: Pair<String, String>? = null): File {
        val archive = File(dir, "$name.tar.bz2")
        TarArchiveOutputStream(BZip2CompressorOutputStream(archive.outputStream())).use { tar ->
            for ((path, bytes) in files) {
                tar.putArchiveEntry(TarArchiveEntry(path).apply { size = bytes.size.toLong() })
                tar.write(bytes)
                tar.closeArchiveEntry()
            }
            link?.let { (linkName, linkTarget) ->
                tar.putArchiveEntry(TarArchiveEntry(linkName, TarConstants.LF_SYMLINK).apply { this.linkName = linkTarget })
                tar.closeArchiveEntry()
            }
        }
        return archive
    }
}
