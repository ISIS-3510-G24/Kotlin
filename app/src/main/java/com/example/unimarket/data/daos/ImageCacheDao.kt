package com.example.unimarket.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.data.entities.ImageCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageCacheDao {
    @Query("SELECT * FROM image_cache")
    fun observeAll(): Flow<List<ImageCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ImageCacheEntity)

    @Query("""
    UPDATE image_cache
      SET state = :state,
          downloadUrl = :downloadUrl
    WHERE localUri = :localUri AND remotePath = :remotePath
  """)
    suspend fun updateEntry(
        localUri: String,
        remotePath: String,
        state: String,
        downloadUrl: String?
    )

    @Delete
    suspend fun delete(item: ImageCacheEntity)
}