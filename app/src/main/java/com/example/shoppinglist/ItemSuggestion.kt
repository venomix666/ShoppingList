package com.example.shoppinglist

import androidx.room.*

@Entity(tableName = "item_suggestions")
data class ItemSuggestionEntity(
    @PrimaryKey val name: String,
    val categoryId: Long? = null
)
