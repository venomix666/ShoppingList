package com.example.shoppinglist

import androidx.room.Dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemSuggestionDao {

    @Query("""
        SELECT * FROM item_suggestions
        WHERE name LIKE '%' || :query || '%'
        ORDER BY name
        LIMIT 10
    """)
    fun search(query: String): Flow<List<ItemSuggestionEntity>>

    @Query("SELECT * FROM item_suggestions")
    fun getAll(): Flow<List<ItemSuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemSuggestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemSuggestionEntity)
}