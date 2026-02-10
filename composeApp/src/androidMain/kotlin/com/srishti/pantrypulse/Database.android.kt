package com.srishti.pantrypulse

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.srishti.pantrypulse.db.PantryDatabase

    fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<PantryDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("pantry_items.db")
        return Room.databaseBuilder<PantryDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
