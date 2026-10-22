package app.atemkraft.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Logbuch und gespeicherte Muster. Jede Schemaänderung braucht eine explizite [Migration], das
 * exportierte Schema unter app/schemas/ und einen Fall in DatabaseMigrationTest (SEC-STORE-03).
 */
@Database(entities = [LogEntryEntity::class, SavedPatternEntity::class], version = 5, exportSchema = true)
abstract class AtemkraftDatabase : RoomDatabase() {

    abstract fun logbookDao(): LogbookDao

    abstract fun savedPatternDao(): SavedPatternDao

    companion object {
        /**
         * v1 → v2: `kind`-Spalte ergänzen (Atemübung/Meditation) und `family` nullbar machen
         * (Meditation hat keine Familie). SQLite kann eine Spalte nicht direkt auf NULL ändern,
         * daher wird die Tabelle neu aufgebaut; bestehende Zeilen bleiben Atemübungen.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE log_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId TEXT NOT NULL,
                        exerciseName TEXT NOT NULL,
                        family TEXT,
                        startedAtEpochMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        roundsCompleted INTEGER NOT NULL,
                        kind TEXT NOT NULL DEFAULT 'BREATHING'
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO log_entries_new
                        (id, exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, kind)
                    SELECT id, exerciseId, exerciseName, family, startedAtEpochMs, durationMs, roundsCompleted, 'BREATHING'
                    FROM log_entries
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE log_entries")
                db.execSQL("ALTER TABLE log_entries_new RENAME TO log_entries")
            }
        }

        /** v2 → v3: Tabelle für gespeicherte generierte Atemmuster. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE saved_patterns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        inhale REAL NOT NULL,
                        holdFull REAL,
                        exhale REAL NOT NULL,
                        holdEmpty REAL,
                        activating INTEGER NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v3 → v4: Herkunft (`origin`), Vorgabedauer (`minutes`) und freie Phasenfolge (`phases`)
         * gespeicherter Muster für den Muster-Editor. Rein additiv: Alle bisherigen Zeilen sind
         * gespeicherte Tagesmuster (`'daily'`) ohne eigene Dauer (NULL ≈ 5 min) und ohne Folge
         * (NULL = die vier Felder gelten).
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_patterns ADD COLUMN origin TEXT NOT NULL DEFAULT 'daily'")
                db.execSQL("ALTER TABLE saved_patterns ADD COLUMN minutes INTEGER")
                db.execSQL("ALTER TABLE saved_patterns ADD COLUMN phases TEXT")
            }
        }

        /**
         * v4 → v5: kein neues Feld, nur Ausgleich für unveröffentlichte Entwicklungsstände. Ein
         * früher Stand von v4 hatte `origin` und `minutes`, aber noch nicht `phases`; v4 mit
         * `phases` hätte denselben Versionsstand mit anderem Schema, Room bräche beim Öffnen ab.
         * Ergänzt daher jede fehlende Spalte von [MIGRATION_3_4] und lässt vorhandene in Ruhe –
         * Logbuch und gespeicherte Muster bleiben. Von v3 aus (alle Releases) ändert sie nichts.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = buildSet {
                    db.query("PRAGMA table_info(saved_patterns)").use { c ->
                        val name = c.getColumnIndexOrThrow("name")
                        while (c.moveToNext()) add(c.getString(name))
                    }
                }
                if ("origin" !in columns) db.execSQL("ALTER TABLE saved_patterns ADD COLUMN origin TEXT NOT NULL DEFAULT 'daily'")
                if ("minutes" !in columns) db.execSQL("ALTER TABLE saved_patterns ADD COLUMN minutes INTEGER")
                if ("phases" !in columns) db.execSQL("ALTER TABLE saved_patterns ADD COLUMN phases TEXT")
            }
        }

        /** Alle Migrationen in Reihenfolge; App und Migrationstest nutzen dieselbe Liste. */
        internal val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        fun build(context: Context): AtemkraftDatabase = Room.databaseBuilder(
            context.applicationContext,
            AtemkraftDatabase::class.java,
            "atemkraft.db",
        ).addMigrations(*MIGRATIONS).build()
    }
}
