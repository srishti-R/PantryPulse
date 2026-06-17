package com.srishti.pantrypulse.db


import Category
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: Category, // Strongly typed Category enum for ultimate M3/SQLite type safety!
    val expiryDate: LocalDate?,
    val buyDate: LocalDate? = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val consumed: Boolean = false,
    val isRemindEnabled: Boolean = true
)