package com.example.unimarket.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.data.entities.UserReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserReviewDao {
    @Query(
        """
        SELECT * FROM user_reviews
        WHERE targetUserId = :uid
        ORDER BY createdAt DESC
    """
    )
    fun observeForUser(uid: String): Flow<List<UserReviewEntity>>

    @Query(
        """
        SELECT * FROM user_reviews
        WHERE reviewerUserId = :reviewer
    """
    )
    fun observeByReviewer(reviewer: String): Flow<List<UserReviewEntity>>

    @Query(
        """
    SELECT EXISTS(
      SELECT 1 FROM user_reviews 
       WHERE reviewerUserId = :reviewer 
         AND orderId   = :orderId
    )
  """
    )
    fun hasReviewedOrder(reviewer: String, orderId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: UserReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<UserReviewEntity>)

    @Query("""
    DELETE FROM user_reviews 
     WHERE targetUserId = :uid 
       AND status = 'SENT'
  """)
    suspend fun clearSentReviewsFor(uid: String)

    @Query("UPDATE user_reviews SET status = :status WHERE localId = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query(
        """
    SELECT * 
    FROM user_reviews 
    WHERE targetUserId = :uid AND status = 'SENT'
    ORDER BY createdAt DESC 
    LIMIT :limit
  """
    )
    fun observeLatestReviewsFor(uid: String, limit: Int = 10): Flow<List<UserReviewEntity>>

    @Query(
        """
    SELECT COUNT(*) 
      FROM user_reviews 
     WHERE targetUserId = :uid 
       AND status = 'SENT'
  """
    )
    fun countReviewsFor(uid: String): Flow<Int>

    @Query(
        """
    SELECT AVG(rating*1.0) 
      FROM user_reviews 
     WHERE targetUserId = :uid 
       AND status = 'SENT'
  """
    )
    fun averageRatingFor(uid: String): Flow<Double?>

    @Query("DELETE FROM user_reviews WHERE targetUserId = :uid")
    suspend fun deleteAllForUser(uid: String)
}

