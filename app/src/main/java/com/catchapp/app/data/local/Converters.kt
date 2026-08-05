package com.catchapp.app.data.local

import androidx.room.TypeConverter

/** Room can't persist enums or List<String> natively — these bridge them to columns. */
class Converters {

    @TypeConverter
    fun fromCaptureState(value: CaptureState): String = value.name

    @TypeConverter
    fun toCaptureState(value: String): CaptureState = CaptureState.valueOf(value)

    @TypeConverter
    fun fromCaptureKind(value: CaptureKind?): String? = value?.name

    @TypeConverter
    fun toCaptureKind(value: String?): CaptureKind? = value?.let { CaptureKind.valueOf(it) }

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_DELIMITER)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(LIST_DELIMITER)

    private companion object {
        // Unicode "unit separator" () — won't collide with real tag/name text,
        // unlike a comma which people will actually type.
        const val LIST_DELIMITER = ""
    }
}
