package com.example.unimarket.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.example.unimarket.data.UniMarketDatabase
import com.example.unimarket.data.UniMarketRepository
import com.example.unimarket.data.daos.ImageCacheDao
import com.example.unimarket.data.daos.OrderDao
import com.example.unimarket.data.daos.PendingOpDao
import com.example.unimarket.data.daos.ProductDao
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
    @Provides fun provideOrderDao(db: UniMarketDatabase) = db.orderDao()
    @Provides fun provideImageCacheDao(db: UniMarketDatabase) = db.imageCacheDao()
    @Provides fun providePendingOpDao(db: UniMarketDatabase) = db.pendingOpDao()

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
        orderDao: OrderDao,
        imageCacheDao: ImageCacheDao,
        pendingOpDao: PendingOpDao,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        gson: Gson
    ): UniMarketRepository =
        UniMarketRepository(
            appContext     = c,
            productDao     = productDao,
            wishlistDao    = wishlistDao,
            orderDao       = orderDao,
            imageCacheDao  = imageCacheDao,
            pendingOpDao   = pendingOpDao,
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

    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}