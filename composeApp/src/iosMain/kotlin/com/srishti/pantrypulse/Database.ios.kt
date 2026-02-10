package com.srishti.pantrypulse

import androidx.room.Room
import androidx.room.RoomDatabase
import com.srishti.pantrypulse.db.PantryDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

    fun getDatabaseBuilder(): RoomDatabase.Builder<PantryDatabase> {
        val dbFilePath = documentDirectory() + "/pantry_items.db"
        return Room.databaseBuilder<PantryDatabase>(
            name = dbFilePath,
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }
