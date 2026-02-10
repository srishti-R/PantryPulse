package com.srishti.pantrypulse.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Insert
    suspend fun insert(item: PantryItem)

    @Query("SELECT count(*) FROM pantry_items")
    suspend fun count(): Int

    @Query("SELECT * FROM pantry_items")
    fun getAllAsFlow(): Flow<List<PantryItem>>
}