package com.example.unimarket.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.unimarket.data.daos.ImageCacheDao
import com.example.unimarket.data.daos.OrderDao
import com.example.unimarket.data.daos.PendingOpDao
import com.example.unimarket.data.daos.ProductDao
import com.example.unimarket.data.daos.UserReviewDao
import com.example.unimarket.data.daos.WishlistDao
import com.example.unimarket.data.entities.ImageCacheEntity
import com.example.unimarket.data.entities.OrderEntity
import com.example.unimarket.data.entities.PendingOpEntity
import com.example.unimarket.data.entities.ProductEntity
import com.example.unimarket.data.entities.UserReviewEntity
import com.example.unimarket.data.entities.WishlistEntity

@Database(
    entities = [
        ProductEntity::class,
        WishlistEntity::class,
        OrderEntity::class,
        ImageCacheEntity::class,
        PendingOpEntity::class,
        UserReviewEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UniMarketDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun orderDao(): OrderDao
    abstract fun imageCacheDao(): ImageCacheDao
    abstract fun pendingOpDao(): PendingOpDao
    abstract fun userReviewDao(): UserReviewDao

    companion object {
        @Volatile private var INSTANCE: UniMarketDatabase? = null

        fun getInstance(context: Context): UniMarketDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, UniMarketDatabase::class.java, "unimarket.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}