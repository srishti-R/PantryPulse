package com.srishti.pantrypulse.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters

@Database(entities = [PantryItem::class], version = 1)
@TypeConverters(Converters::class)
@ConstructedBy(PantryDatabaseConstructor::class)
abstract class PantryDatabase: RoomDatabase() {
    abstract fun getDao(): PantryDao
}

@Suppress("KotlinNoActualForExpect")
expect object PantryDatabaseConstructor : RoomDatabaseConstructor<PantryDatabase> {
    override fun initialize(): PantryDatabase
}