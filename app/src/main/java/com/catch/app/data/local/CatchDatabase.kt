package com.catch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CaptureEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CatchDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
}
