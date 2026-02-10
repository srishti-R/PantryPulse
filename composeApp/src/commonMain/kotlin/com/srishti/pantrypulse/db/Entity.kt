package com.srishti.pantrypulse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.srishti.pantrypulse.model.Category
import kotlinx.datetime.LocalDate


@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: Category,
    val expiryDate: LocalDate?,
    val buyDate: LocalDate?
)