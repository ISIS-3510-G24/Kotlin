package com.example.unimarket.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import coil.ImageLoader
import coil.memory.MemoryCache
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.data.daos.FindDao
import com.example.unimarket.data.daos.ImageCacheDao
import com.example.unimarket.data.daos.OrderDao
import com.example.unimarket.data.daos.PendingOpDao
import com.example.unimarket.data.daos.ProductDao
import com.example.unimarket.data.daos.UserReviewDao
import com.example.unimarket.data.daos.WishlistDao
import com.example.unimarket.utils.ConnectivityObserver
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun providedDatabase(@ApplicationContext c: Context): UniMarketDatabase =
        Room.databaseBuilder(c, UniMarketDatabase::class.java, "unimarket.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProductDao(db: UniMarketDatabase) = db.productDao()
    @Provides fun provideWishlistDao(db: UniMarketDatabase) = db.wishlistDao()
    @Provides fun provideFindDao(db: UniMarketDatabase) = db.findDao()
    @Provides fun provideOrderDao(db: UniMarketDatabase) = db.orderDao()
    @Provides fun provideImageCacheDao(db: UniMarketDatabase) = db.imageCacheDao()
    @Provides fun providePendingOpDao(db: UniMarketDatabase) = db.pendingOpDao()
    @Provides fun provideUserReviewDao(db: UniMarketDatabase) = db.userReviewDao()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideRepository(
        @ApplicationContext c: Context,
        productDao: ProductDao,
        wishlistDao: WishlistDao,
        findDao: FindDao,
        orderDao: OrderDao,
        imageCacheDao: ImageCacheDao,
        pendingOpDao: PendingOpDao,
        userReviewDao: UserReviewDao,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        gson: Gson
    ): UniMarketRepository =
        UniMarketRepository(
            appContext     = c,
            productDao     = productDao,
            wishlistDao    = wishlistDao,
            orderDao       = orderDao,
            findDao        = findDao,
            imageCacheDao  = imageCacheDao,
            pendingOpDao   = pendingOpDao,
            userReviewDao  = userReviewDao,
            firestore      = firestore,
            storage        = storage,
            gson           = gson,
            ioDispatcher = Dispatchers.IO
    )

    @Provides @Singleton
    fun provideWorkManager(@ApplicationContext c: Context): WorkManager =
        WorkManager.getInstance(c)

    @Provides @Singleton
    fun provideConnectivityObserver(@ApplicationContext c: Context): ConnectivityObserver =
        ConnectivityObserver(c)

    @Provides @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext c: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(c)

    @Provides @Singleton
    fun provideCrashlytics(@ApplicationContext c: Context): FirebaseCrashlytics =
        FirebaseCrashlytics.getInstance()

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebasePerformance(): FirebasePerformance =
        FirebasePerformance.getInstance()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Provides @Singleton
    fun provideImageLoader(@ApplicationContext ctx: Context): ImageLoader =
        ImageLoader.Builder(ctx)
            .memoryCache {
                MemoryCache.Builder(ctx)
                    .maxSizePercent(0.10)
                    .build()
            }
            .dispatcher(Dispatchers.IO.limitedParallelism(2))
            .build()
}