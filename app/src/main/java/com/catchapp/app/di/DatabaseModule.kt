package com.catchapp.app.di

import android.content.Context
import androidx.room.Room
import com.catchapp.app.data.local.CaptureDao
import com.catchapp.app.data.local.CatchDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCatchDatabase(@ApplicationContext context: Context): CatchDatabase =
        Room.databaseBuilder(context, CatchDatabase::class.java, "catch.db").build()

    @Provides
    fun provideCaptureDao(database: CatchDatabase): CaptureDao = database.captureDao()
}
