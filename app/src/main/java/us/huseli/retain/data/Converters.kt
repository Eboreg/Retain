package us.huseli.retain.data

import androidx.room.TypeConverter
import java.util.Date
import java.util.UUID

object Converters {
    @TypeConverter
    @JvmStatic
    fun dateToLong(value: Date): Long = value.time

    @TypeConverter
    @JvmStatic
    fun longToDate(value: Long): Date = Date(value)

    @TypeConverter
    @JvmStatic
    fun uuidToString(value: UUID): String = value.toString()

    @TypeConverter
    @JvmStatic
    fun stringToUuid(value: String): UUID = UUID.fromString(value)
}