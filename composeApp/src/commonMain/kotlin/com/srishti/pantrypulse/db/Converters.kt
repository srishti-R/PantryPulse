package com.srishti.pantrypulse.db

import androidx.room.TypeConverter
import com.srishti.pantrypulse.model.Category
import kotlinx.datetime.LocalDate

class Converters {
    // For LocalDate
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    // For Category (assuming it's an Enum)
    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)
}