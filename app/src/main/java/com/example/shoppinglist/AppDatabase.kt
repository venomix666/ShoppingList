package com.example.shoppinglist

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ShoppingItemEntity::class,
        ShoppingListEntity::class,
        CategoryEntity::class,
        ItemSuggestionEntity::class
    ],
    version = 22   // increment version
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemSuggestionDao(): ItemSuggestionDao
}