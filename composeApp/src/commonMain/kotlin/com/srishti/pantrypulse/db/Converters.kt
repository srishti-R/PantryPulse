
import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate

/**
 * Robust TypeConverters indicating how Room database should save complex enum classes and LocalDateTime
 * objects into normal persistent SQL types.
 */
class Converters {
    @TypeConverter
    fun fromCategory(category: Category): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(value: String): Category {
        return try {
            Category.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Category.NA
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
}