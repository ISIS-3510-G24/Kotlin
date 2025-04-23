package com.example.unimarket.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.data.entities.WishlistEntity

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist")
    suspend fun getAll(): List<WishlistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistEntity)

    @Delete
    suspend fun delete(item: WishlistEntity)
}