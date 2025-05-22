package com.example.unimarket.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.data.entities.FindEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FindDao {
    @Query("SELECT * FROM finds")
    fun observeAll(): Flow<List<FindEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<FindEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(find: FindEntity)

    @Query("DELETE FROM finds")
    suspend fun clear()

    @Query("SELECT * FROM finds WHERE id = :id")
    fun observeById(id: String): Flow<FindEntity?>

    @Query("SELECT * FROM finds WHERE id = :id")
    suspend fun getById(id: String): FindEntity?
}
