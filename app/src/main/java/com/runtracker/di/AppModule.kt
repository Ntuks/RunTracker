package com.runtracker.di

import android.content.Context
import androidx.room.Room
import com.runtracker.db.RunsDatabase
import com.runtracker.utils.Constants.RUNS_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRunsDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        RunsDatabase::class.java,
        RUNS_DATABASE_NAME
    ).build()

    @Singleton
    @Provides
    fun provideRunDAO(db: RunsDatabase) = db.getRunDao()
}