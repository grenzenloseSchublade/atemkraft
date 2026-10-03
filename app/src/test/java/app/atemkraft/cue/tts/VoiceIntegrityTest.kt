package app.atemkraft.cue.tts

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Download-Prüfung (Größe + SHA-256) und sicheres Entpacken der Stimm-Archive. */
class VoiceIntegrityTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val payload = ByteArray(200_000) { (it % 251).toByte() }
    private val payloadSha = sha256(payload)

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun copy(bytes: ByteArray, size: Long, sha: String): ByteArray {
        val out = ByteArrayOutputStream()
        copyVerified(ByteArrayInputStream(bytes), out, size, sha)
        return out.toByteArray()
    }

    @Test
    fun `passender Download wird vollstaendig durchgereicht`() {
        assertArrayEquals(payload, copy(payload, payload.size.toLong(), payloadSha))
    }

    @Test
    fun `Pruefsumme ist case-insensitiv`() {
        assertArrayEquals(payload, copy(payload, payload.size.toLong(), payloadSha.uppercase()))
    }

    @Test
    fun `abweichende Pruefsumme wird abgelehnt`() {
        val tampered = payload.copyOf().also { it[1000] = (it[1000] + 1).toByte() }
        assertThrows(VoiceIntegrityException::class.java) {
            copy(tampered, payload.size.toLong(), payloadSha)
        }
    }

    @Test
    fun `zu grosser Download bricht frueh ab`() {
        val out = ByteArrayOutputStream()
        assertThrows(VoiceIntegrityException::class.java) {
            copyVerified(ByteArrayInputStream(payload), out, 1000L, payloadSha)
        }
        // Abbruch, bevor die Datei über die erwartete Größe hinaus wächst
        assertTrue(out.size() <= 1000)
    }

    @Test
    fun `zu kurzer Download wird abgelehnt`() {
        assertThrows(VoiceIntegrityException::class.java) {
            copy(payload.copyOf(1000), payload.size.toLong(), payloadSha)
        }
    }

    // --- Entpacken ---

    private fun tarBz2(build: (TarArchiveOutputStream) -> Unit): File {
        val f = tmp.newFile()
        TarArchiveOutputStream(BZip2CompressorOutputStream(f.outputStream())).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            build(tar)
        }
        return f
    }

    private fun TarArchiveOutputStream.file(name: String, content: String) {
        val bytes = content.toByteArray()
        putArchiveEntry(TarArchiveEntry(name).apply { size = bytes.size.toLong() })
        write(bytes)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.link(name: String, target: String, flag: Byte) {
        putArchiveEntry(TarArchiveEntry(name, flag).apply { linkName = target })
        closeArchiveEntry()
    }

    @Test
    fun `normales Archiv wird entpackt`() {
        val archive = tarBz2 { tar ->
            tar.putArchiveEntry(TarArchiveEntry("voice/"))
            tar.closeArchiveEntry()
            tar.file("voice/tokens.txt", "a 1")
        }
        val target = tmp.newFolder("out")
        extractTarBz2Safely(archive, target)
        assertEquals("a 1", File(target, "voice/tokens.txt").readText())
    }

    @Test
    fun `Symlink-Eintrag wird abgelehnt`() {
        val archive = tarBz2 { it.link("voice/evil", "/etc/passwd", TarConstants.LF_SYMLINK) }
        val target = tmp.newFolder("out")
        assertThrows(VoiceIntegrityException::class.java) { extractTarBz2Safely(archive, target) }
        assertFalse(File(target, "voice/evil").exists())
    }

    @Test
    fun `Hardlink-Eintrag wird abgelehnt`() {
        val archive = tarBz2 { it.link("voice/evil", "../../secret", TarConstants.LF_LINK) }
        val target = tmp.newFolder("out")
        assertThrows(VoiceIntegrityException::class.java) { extractTarBz2Safely(archive, target) }
        assertFalse(File(target, "voice/evil").exists())
    }

    @Test
    fun `Pfad ausserhalb des Ziels wird abgelehnt (Zip-Slip)`() {
        val archive = tarBz2 { it.file("../escaped.txt", "x") }
        val target = tmp.newFolder("out")
        assertThrows(IOException::class.java) { extractTarBz2Safely(archive, target) }
        assertFalse(File(target.parentFile, "escaped.txt").exists())
    }
}
