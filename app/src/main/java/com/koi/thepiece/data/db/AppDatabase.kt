package com.koi.thepiece.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CardEntity::class,
        DeckEntity::class,
        DeckCardEntity::class
               ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun deckDao(): DeckDao
}
