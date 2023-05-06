package us.huseli.retain.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import us.huseli.retain.BuildConfig
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import java.util.concurrent.Executors

@androidx.room.Database(
    entities = [Note::class, ChecklistItem::class],
    exportSchema = false,
    version = 4,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object : LoggingObject {
        @Volatile
        private var INSTANCE: Database? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE note ADD COLUMN noteShowChecked INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): Database {
            return INSTANCE ?: synchronized(this) {
                val builder = Room
                    .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .addMigrations(MIGRATION_3_4)

                if (BuildConfig.DEBUG) {
                    class Callback : QueryCallback {
                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                            log("$sqlQuery, bindArgs=$bindArgs")
                        }
                    }

                    val executor = Executors.newSingleThreadExecutor()
                    builder.setQueryCallback(Callback(), executor)
                }

                val instance = builder.build()

                INSTANCE = instance
                instance
            }
        }
    }
}