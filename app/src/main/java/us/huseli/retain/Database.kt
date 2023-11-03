package us.huseli.retain

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import us.huseli.retain.dao.ChecklistItemDao
import us.huseli.retain.dao.ImageDao
import us.huseli.retain.dao.NoteDao
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.dataclasses.entities.DeletedNote
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.dataclasses.entities.Note
import java.util.concurrent.Executors

@androidx.room.Database(
    entities = [Note::class, ChecklistItem::class, Image::class, DeletedNote::class],
    exportSchema = false,
    version = 14,
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun imageDao(): ImageDao

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DatabaseEntryPoint {
        fun logger(): Logger
    }

    companion object {
        fun build(context: Context): Database {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DatabaseEntryPoint::class.java
            )
            val logger = hiltEntryPoint.logger()

            val builder = Room
                .databaseBuilder(context.applicationContext, Database::class.java, "db.sqlite3")
                .fallbackToDestructiveMigration()

            if (BuildConfig.DEBUG) {
                class Callback : QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        logger.log(
                            LogMessage(
                                level = Log.DEBUG,
                                tag = "Database<${System.identityHashCode(this)}>",
                                thread = Thread.currentThread().name,
                                message = "$sqlQuery, bindArgs=$bindArgs",
                            )
                        )
                    }
                }

                val executor = Executors.newSingleThreadExecutor()
                builder.setQueryCallback(Callback(), executor)
            }

            return builder.build()
        }
    }
}