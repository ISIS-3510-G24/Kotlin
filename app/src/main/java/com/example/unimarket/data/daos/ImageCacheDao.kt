package com.example.unimarket.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.unimarket.data.entities.ImageCacheEntity

@Dao
interface ImageCacheDao {
    @Query("SELECT * FROM image_cache")
    suspend fun getAll(): List<ImageCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ImageCacheEntity)

    @Update
    suspend fun update(item: ImageCacheEntity)

    @Delete
    suspend fun delete(item: ImageCacheEntity)
}