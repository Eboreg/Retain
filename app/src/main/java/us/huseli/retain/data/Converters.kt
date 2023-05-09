package us.huseli.retain.data

import androidx.room.TypeConverter
import java.time.Instant
import java.util.UUID

object Converters {
    @TypeConverter
    @JvmStatic
    fun instantToLong(value: Instant): Long = value.epochSecond

    @TypeConverter
    @JvmStatic
    fun longToInstant(value: Long): Instant = Instant.ofEpochSecond(value)

    @TypeConverter
    @JvmStatic
    fun uuidToString(value: UUID): String = value.toString()

    @TypeConverter
    @JvmStatic
    fun stringToUuid(value: String): UUID = UUID.fromString(value)
}