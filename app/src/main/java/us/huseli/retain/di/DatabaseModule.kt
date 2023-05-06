package us.huseli.retain.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.huseli.retain.data.Database
import us.huseli.retain.data.NoteDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): Database {
        return Database.getDatabase(context)
    }

    @Provides
    fun provideNoteDao(database: Database): NoteDao {
        return database.noteDao()
    }
}