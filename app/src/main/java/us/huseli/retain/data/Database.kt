package us.huseli.retain.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import us.huseli.retain.BuildConfig
import us.huseli.retain.Logger
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import java.util.concurrent.Executors

@androidx.room.Database(
    entities = [Note::class, ChecklistItem::class],
    exportSchema = false,
    version = 5,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DatabaseEntryPoint {
        fun logger(): Logger
    }

    companion object : LoggingObject {
        override var logger: Logger? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE note ADD COLUMN noteShowChecked INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun build(context: Context): Database {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DatabaseEntryPoint::class.java
            )
            this.logger = hiltEntryPoint.logger()
            // val logger = hiltEntryPoint.logger()

            val builder = Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigrationFrom(1, 2)
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()

            if (BuildConfig.DEBUG) {
                class Callback : QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        log("$sqlQuery, bindArgs=$bindArgs", Log.DEBUG)
                    }
                }

                val executor = Executors.newSingleThreadExecutor()
                builder.setQueryCallback(Callback(), executor)
            }

            return builder.build()
        }
    }
}