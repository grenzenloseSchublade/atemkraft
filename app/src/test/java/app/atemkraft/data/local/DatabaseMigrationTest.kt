package app.atemkraft.data.local

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
 * 2.json stammt aus v1.3.1 (letzter Stand vor „Meine Muster“), 3.json erzeugt der aktuelle Build.
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

        val room = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            AtemkraftDatabase::class.java,
            DB_NAME,
        ).addMigrations(*AtemkraftDatabase.MIGRATIONS).allowMainThreadQueries().build()
        try {
            val entries = room.logbookDao().all().first()
            assertEquals(listOf("box-4-4-4-4"), entries.map { it.exerciseId })
        } finally {
            room.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
