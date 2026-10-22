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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room-Migrationen gegen die eingecheckten Schemas in app/schemas/ (SEC-STORE-03). Ohne Cloud-Backup
 * ist eine fehlerhafte Migration nicht zu retten: Logbuch und gespeicherte Muster müssen jeden
 * Versionssprung bis zur aktuellen Version überstehen, und die App muss sie danach lesen.
 *
 * 2.json stammt aus v1.3.1 (letzter Stand vor „Meine Muster“), 3.json aus v1.6.8 (letzter Stand
 * vor dem Muster-Editor). Schema v1 gab es nur in v0.2, das nie öffentlich war.
 * Eigene [Application], damit der App-Start nicht nebenher die echte Datenbank öffnet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AtemkraftDatabase::class.java)

    @Test
    fun `Logbuch aus Version 2 uebersteht alle Migrationen`() = runBlocking {
        helper.createDatabase(DB_NAME, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO log_entries
                    (exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                VALUES
                    ('resonanz', 'Resonanzatmung', 'DOWNREGULATE', 1700000000000, 300000, 30, 'BREATHING'),
                    ('meditation', 'Meditation', NULL, 1700000600000, 600000, 0, 'MEDITATION')
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, LATEST, true, *AtemkraftDatabase.MIGRATIONS).close()

        withApp { room ->
            val entries = room.logbookDao().all().first().sortedBy { it.startedAtEpochMs }
            assertEquals(listOf("resonanz", "meditation"), entries.map { it.exerciseId })
            assertEquals(listOf(300000L, 600000L), entries.map { it.durationMs })
            assertEquals(listOf("BREATHING", "MEDITATION"), entries.map { it.kind })
        }
    }

    @Test
    fun `gespeicherte Muster aus Version 3 ueberstehen alle Migrationen als Tagesmuster`() = runBlocking {
        helper.createDatabase(DB_NAME, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO saved_patterns
                    (name, inhale, holdFull, exhale, holdEmpty, activating, createdAtEpochMs)
                VALUES ('Muster 4·2·6', 4.0, 2.0, 6.0, NULL, 0, 1759000000000)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO log_entries
                    (exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                VALUES ('muster-1', 'Muster 4·2·6', 'BALANCE', 1700000000000, 300000, 25, 'BREATHING')
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, LATEST, true, *AtemkraftDatabase.MIGRATIONS).close()

        withApp { room ->
            val row = room.savedPatternDao().snapshot().single()
            assertEquals("Muster 4·2·6", row.name)
            assertEquals(1759000000000L, row.createdAtEpochMs)
            assertEquals(PatternOrigin.SAVED_DAILY, row.patternOrigin())
            assertNull(row.minutes)
            assertNull(row.phases)
            assertEquals(listOf("muster-1"), room.logbookDao().all().first().map { it.exerciseId })
        }
    }

    /** Öffnet die migrierte Datei wie [AtemkraftDatabase.build], nur mit Abfragen im Testthread. */
    private suspend fun withApp(block: suspend (AtemkraftDatabase) -> Unit) {
        val room = Room.databaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AtemkraftDatabase::class.java, DB_NAME)
            .addMigrations(*AtemkraftDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            block(room)
        } finally {
            room.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"

        /** Aktuelle Schemaversion (`@Database(version = …)`). */
        const val LATEST = 5
    }
}
