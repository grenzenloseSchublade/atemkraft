package app.atemkraft.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LogEntryEntity::class], version = 2, exportSchema = false)
abstract class AtemkraftDatabase : RoomDatabase() {

    abstract fun logbookDao(): LogbookDao

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

        fun build(context: Context): AtemkraftDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AtemkraftDatabase::class.java,
                "atemkraft.db",
            ).addMigrations(MIGRATION_1_2).build()
    }
}
