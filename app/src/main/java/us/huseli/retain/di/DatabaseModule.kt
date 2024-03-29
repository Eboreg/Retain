package us.huseli.retain.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.huseli.retain.dao.ChecklistItemDao
import us.huseli.retain.Database
import us.huseli.retain.dao.ImageDao
import us.huseli.retain.dao.NoteDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): Database = Database.build(context)

    @Provides
    fun provideNoteDao(database: Database): NoteDao = database.noteDao()

    @Provides
    fun provideChecklistItemDao(database: Database): ChecklistItemDao = database.checklistItemDao()

    @Provides
    fun provideImageDao(database: Database): ImageDao = database.imageDao()
}
