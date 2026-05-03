package com.example.shoppinglist

import androidx.room.*

@Entity(
    tableName = "shopping_items",
    indices = [
        Index("shareId"),
        Index("categoryId")
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey
    val id: String,
    val shareId: String,
    val name: String,
    val categoryId: Long? = null,
    val picked: Boolean = false
)
