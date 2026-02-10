package com.srishti.pantrypulse.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class Database {
    fun getRoomDatabase(
        builder: RoomDatabase.Builder<PantryDatabase>
    ): PantryDatabase {
        return builder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

}