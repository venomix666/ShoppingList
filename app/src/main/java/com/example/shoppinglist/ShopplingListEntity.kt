package com.example.shoppinglist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey
    val shareId: String = "",
    val name: String = "",
    val createdAt: Long = 0L
)
