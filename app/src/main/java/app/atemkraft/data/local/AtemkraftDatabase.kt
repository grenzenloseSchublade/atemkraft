package app.atemkraft.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LogEntryEntity::class], version = 1, exportSchema = false)
abstract class AtemkraftDatabase : RoomDatabase() {

    abstract fun logbookDao(): LogbookDao

    companion object {
        fun build(context: Context): AtemkraftDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AtemkraftDatabase::class.java,
                "atemkraft.db",
            ).build()
    }
}
