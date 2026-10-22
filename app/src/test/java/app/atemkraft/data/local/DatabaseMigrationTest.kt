package app.atemkraft.data.local

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.atemkraft.domain.PatternOrigin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room-Migrationen gegen die eingecheckten Schemas in app/schemas/ (SEC-STORE-03). Ohne Cloud-Backup
 * ist eine fehlerhafte Migration nicht zu retten: Das Logbuch muss jeden Versionssprung überstehen.
 *
 * 2.json stammt aus v1.3.1 (letzter Stand vor „Meine Muster“), 3.json aus v1.6.8 (letzter Stand
 * vor dem Muster-Editor), 4.json und 5.json (gleiches Schema, nur Ausgleich früher
 * Entwicklungsstände von v4) erzeugt der aktuelle Build.
 * Schema v1 gab es nur in v0.2, das nie öffentlich war; dafür gibt es keinen Fall.
 * Eigene [Application], damit der App-Start nicht nebenher die echte Datenbank öffnet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AtemkraftDatabase::class.java)

    @Test
    fun `2 nach 3 behält das Logbuch und legt Meine Muster an`() {
        helper.createDatabase(DB_NAME, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO log_entries
                    (id, exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                VALUES
                    (1, 'resonanz', 'Resonanzatmung', 'DOWNREGULATE', 1700000000000, 300000, 30, 'BREATHING'),
                    (2, 'meditation', 'Meditation', NULL, 1700000600000, 600000, 0, 'MEDITATION')
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 3, true, *AtemkraftDatabase.MIGRATIONS).use { db ->
            db.query("SELECT exerciseId, family, durationMs, kind FROM log_entries ORDER BY id").use { c ->
                assertEquals(2, c.count)
                c.moveToFirst()
                assertEquals("resonanz", c.getString(0))
                assertEquals("DOWNREGULATE", c.getString(1))
                assertEquals(300000L, c.getLong(2))
                assertEquals("BREATHING", c.getString(3))
                c.moveToNext()
                assertEquals("meditation", c.getString(0))
                assertTrue(c.isNull(1))
                assertEquals("MEDITATION", c.getString(3))
            }
            db.query("SELECT COUNT(*) FROM saved_patterns").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }
    }

    @Test
    fun `3 nach 4 behält gespeicherte Muster als Tagesmuster ohne Dauer und Folge und das Logbuch`() {
        helper.createDatabase(DB_NAME, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO log_entries
                    (id, exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                VALUES (1, 'muster-1', 'Muster 4·2·6', 'BALANCE', 1700000000000, 300000, 25, 'BREATHING')
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO saved_patterns
                    (id, name, inhale, holdFull, exhale, holdEmpty, activating, createdAtEpochMs)
                VALUES
                    (1, 'Muster 4·2·6', 4.0, 2.0, 6.0, NULL, 0, 1759000000000),
                    (2, 'Muster 4,5·3', 4.5, NULL, 3.0, NULL, 1, 1759500000000)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 4, true, *AtemkraftDatabase.MIGRATIONS).use { db ->
            db.query(
                "SELECT id, name, inhale, holdFull, activating, createdAtEpochMs, origin, minutes, phases FROM saved_patterns ORDER BY id",
            ).use { c ->
                assertEquals(2, c.count)
                c.moveToFirst()
                assertEquals(1L, c.getLong(0))
                assertEquals("Muster 4·2·6", c.getString(1))
                assertEquals(4.0, c.getDouble(2), 0.0)
                assertEquals(2.0, c.getDouble(3), 0.0)
                assertEquals(0, c.getInt(4))
                assertEquals(1759000000000L, c.getLong(5))
                assertEquals("daily", c.getString(6))
                assertTrue(c.isNull(7))
                assertTrue(c.isNull(8))
                c.moveToNext()
                assertEquals("Muster 4,5·3", c.getString(1))
                assertEquals(4.5, c.getDouble(2), 0.0)
                assertTrue(c.isNull(3))
                assertEquals(1, c.getInt(4))
                assertEquals("daily", c.getString(6))
                assertTrue(c.isNull(7))
                assertTrue(c.isNull(8))
            }
            db.query("SELECT exerciseId, exerciseName, durationMs, kind FROM log_entries").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("muster-1", c.getString(0))
                assertEquals("Muster 4·2·6", c.getString(1))
                assertEquals(300000L, c.getLong(2))
                assertEquals("BREATHING", c.getString(3))
            }
        }
    }

    @Test
    fun `2 nach 4 in einem Zug behält das Logbuch und nimmt freie Folgen auf`() {
        helper.createDatabase(DB_NAME, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO log_entries
                    (id, exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                VALUES (1, 'resonanz', 'Resonanzatmung', 'DOWNREGULATE', 1700000000000, 300000, 30, 'BREATHING')
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 4, true, *AtemkraftDatabase.MIGRATIONS).use { db ->
            db.query("SELECT exerciseId, durationMs FROM log_entries").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("resonanz", c.getString(0))
                assertEquals(300000L, c.getLong(1))
            }
            db.query("SELECT COUNT(*) FROM saved_patterns").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
            // Die Spalte für die Phasenfolge ist da: Ein eigenes Muster mit 12 Phasen passt hinein.
            db.execSQL(
                """
                INSERT INTO saved_patterns
                    (name, inhale, holdFull, exhale, holdEmpty, activating, createdAtEpochMs, origin, minutes, phases)
                VALUES ('Zwölf', 8.0, 4.0, 10.0, 2.0, 0, 1761000000000, 'custom', 10, '$TWELVE_PHASES')
                """.trimIndent(),
            )
            db.query("SELECT origin, minutes, phases FROM saved_patterns").use { c ->
                c.moveToFirst()
                assertEquals("custom", c.getString(0))
                assertEquals(10, c.getInt(1))
                assertEquals(TWELVE_PHASES, c.getString(2))
            }
        }
    }

    @Test
    fun `die App liest eine freie Folge aus einer migrierten Datenbank`() = runBlocking {
        helper.createDatabase(DB_NAME, 3).close()
        helper.runMigrationsAndValidate(DB_NAME, 4, true, *AtemkraftDatabase.MIGRATIONS).use { db ->
            db.execSQL(
                """
                INSERT INTO saved_patterns
                    (name, inhale, holdFull, exhale, holdEmpty, activating, createdAtEpochMs, origin, minutes, phases)
                VALUES ('Zwölf', 8.0, 4.0, 10.0, 2.0, 0, 1761000000000, 'custom', 10, '$TWELVE_PHASES')
                """.trimIndent(),
            )
        }

        val room = openWithAppBuilder()
        try {
            val row = room.savedPatternDao().snapshot().single()
            assertEquals(TWELVE_PHASES, row.phases)
            assertEquals(12, row.freePattern()!!.phases.size)
            assertEquals(TWELVE_PHASES, row.freePattern()!!.toCompact())
        } finally {
            room.close()
        }
    }

    @Test
    fun `die App öffnet eine Datenbank von 3 und liest gespeicherte Muster als Tagesmuster`() = runBlocking {
        helper.createDatabase(DB_NAME, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO saved_patterns
                    (name, inhale, holdFull, exhale, holdEmpty, activating, createdAtEpochMs)
                VALUES ('Muster 4·2·6', 4.0, 2.0, 6.0, NULL, 0, 1759000000000)
                """.trimIndent(),
            )
        }

        val room = openWithAppBuilder()
        try {
            val rows = room.savedPatternDao().snapshot()
            assertEquals(listOf("Muster 4·2·6"), rows.map { it.name })
            assertEquals(SavedPatternEntity.ORIGIN_DAILY, rows.single().origin)
            assertNull(rows.single().minutes)
            assertNull(rows.single().phases)
            assertEquals(PatternOrigin.SAVED_DAILY, rows.single().patternOrigin())

            // Nach der Migration lässt sich das Muster wie ein eigenes bearbeiten.
            val changed = room.savedPatternDao().updateCustom(rows.single().id, "Abend", 4.0, 1.0, 6.0, null, false, 12, "in:4,hold:1,out:6")
            assertEquals(1, changed)
            val updated = room.savedPatternDao().byId(rows.single().id)!!
            assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, updated.origin)
            assertEquals(12, updated.minutes)
            assertEquals("in:4,hold:1,out:6", updated.phases)
            assertEquals(1759000000000L, updated.createdAtEpochMs)
        } finally {
            room.close()
        }
    }

    @Test
    fun `die App öffnet eine migrierte Datenbank und liest das Logbuch`() = runBlocking {
        helper.createDatabase(DB_NAME, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO log_entries
                    (exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                VALUES ('box-4-4-4-4', 'Box-Atmung', 'BALANCE', 1700000000000, 240000, 15, 'BREATHING')
                """.trimIndent(),
            )
        }

        val room = openWithAppBuilder()
        try {
            val entries = room.logbookDao().all().first()
            assertEquals(listOf("box-4-4-4-4"), entries.map { it.exerciseId })
        } finally {
            room.close()
        }
    }

    @Test
    fun `fruehes v4 ohne Folge-Spalte bekommt sie mit 5, Muster und Logbuch bleiben`() = runBlocking {
        // Früher Entwicklungsstand von v4: origin und minutes, aber noch kein phases.
        helper.createDatabase(DB_NAME, 3).use { db ->
            db.execSQL("ALTER TABLE saved_patterns ADD COLUMN origin TEXT NOT NULL DEFAULT 'daily'")
            db.execSQL("ALTER TABLE saved_patterns ADD COLUMN minutes INTEGER")
            db.execSQL(
                """
                INSERT INTO saved_patterns
                    (name, inhale, holdFull, exhale, holdEmpty, activating, createdAtEpochMs, origin, minutes)
                VALUES ('Abend', 4.0, 1.0, 6.0, NULL, 0, 1761000000000, 'custom', 12)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO log_entries
                    (exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                VALUES ('box-4-4-4-4', 'Box-Atmung', 'BALANCE', 1700000000000, 240000, 15, 'BREATHING')
                """.trimIndent(),
            )
            db.execSQL("PRAGMA user_version = 4")
        }

        helper.runMigrationsAndValidate(DB_NAME, 5, true, *AtemkraftDatabase.MIGRATIONS).use { db ->
            db.query("SELECT name, origin, minutes, phases FROM saved_patterns").use { c ->
                c.moveToFirst()
                assertEquals("Abend", c.getString(0))
                assertEquals("custom", c.getString(1))
                assertEquals(12, c.getInt(2))
                assertTrue(c.isNull(3))
            }
        }
        val room = openWithAppBuilder()
        try {
            assertEquals(listOf("box-4-4-4-4"), room.logbookDao().all().first().map { it.exerciseId })
            assertEquals(listOf("Abend"), room.savedPatternDao().snapshot().map { it.name })
        } finally {
            room.close()
        }
    }

    @Test
    fun `4 mit Folge-Spalte nach 5 aendert nichts`() {
        helper.createDatabase(DB_NAME, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO saved_patterns
                    (name, inhale, holdFull, exhale, holdEmpty, activating, createdAtEpochMs, origin, minutes, phases)
                VALUES ('Zwölf', 8.0, 4.0, 10.0, 2.0, 0, 1761000000000, 'custom', 10, '$TWELVE_PHASES')
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, 5, true, *AtemkraftDatabase.MIGRATIONS).use { db ->
            db.query("SELECT phases FROM saved_patterns").use { c ->
                c.moveToFirst()
                assertEquals(TWELVE_PHASES, c.getString(0))
            }
        }
    }

    /** Wie [AtemkraftDatabase.build], nur mit Testdatei und Abfragen im Testthread. */
    private fun openWithAppBuilder(): AtemkraftDatabase = Room.databaseBuilder(
        ApplicationProvider.getApplicationContext<Application>(),
        AtemkraftDatabase::class.java,
        DB_NAME,
    ).addMigrations(*AtemkraftDatabase.MIGRATIONS).allowMainThreadQueries().build()

    private companion object {
        const val DB_NAME = "migration-test.db"

        /** Zwölf Phasen (Höchstzahl), mit Stufen beim Ein- und Ausatmen. */
        const val TWELVE_PHASES = "in:3,hold:1,in:3,hold:1,out:4,hold:2,in:2,in:2,out:3,hold:1,out:3,hold:5"
    }
}
