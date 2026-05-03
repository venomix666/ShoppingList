package com.example.shoppinglist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    // Lists
    @Query("SELECT * FROM shopping_lists")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE shareId = :shareId")
    fun getList(shareId: String): ShoppingListEntity?

    @Insert
    suspend fun insertList(list: ShoppingListEntity): Unit

    @Delete
    suspend fun deleteList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists")
    suspend fun clearLists()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLists(lists: List<ShoppingListEntity>)

    @Transaction
    suspend fun replaceAllLists(lists: List<ShoppingListEntity>) {
        clearLists()
        insertAllLists(lists)
    }

    @Query("DELETE FROM shopping_items WHERE shareId = :shareId")
    suspend fun clearItems(shareId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<ShoppingItemEntity>)

    // Categories
    @Query("SELECT * FROM categories")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    // Items filtered by list
    //@Query("SELECT * FROM shopping_items WHERE shareId = :shareId")
    //fun getItemsForList(shareId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE shareId = :shareId")
    fun observeItems(shareId: String): Flow<List<ShoppingItemEntity>>


    @Insert
    suspend fun insertItem(item: ShoppingItemEntity): Unit

    @Delete
    suspend fun deleteItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET picked = :picked WHERE shareId = :shareId")
    suspend fun setPicked(shareId: String, picked: Boolean)

    @Transaction
    suspend fun replaceItemsForList(shareId: String, items: List<ShoppingItemEntity>) {
        clearItems(shareId)
        insertAllItems(items)
    }

}