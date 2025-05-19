package com.example.unimarket.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.data.entities.UserReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserReviewDao {
    @Query("""
        SELECT * FROM user_reviews
        WHERE targetUserId = :uid
        ORDER BY createdAt DESC
    """)
    fun observeForUser(uid: String): Flow<List<UserReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: UserReviewEntity)

    @Query("UPDATE user_reviews SET status = :status WHERE localId = :id")
    suspend fun updateStatus(id: Long, status: String)
}

