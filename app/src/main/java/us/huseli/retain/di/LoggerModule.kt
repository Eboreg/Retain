package us.huseli.retain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.huseli.retain.Logger
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object LoggerModule {
    @Provides
    @Singleton
    fun provideLogger(): Logger = Logger()
}
