package com.example.unimarket.data

import android.content.Context
import androidx.collection.LruCache
import com.example.unimarket.data.daos.ImageCacheDao
import com.example.unimarket.data.daos.OrderDao
import com.example.unimarket.data.daos.PendingOpDao
import com.example.unimarket.data.daos.ProductDao
import com.example.unimarket.data.daos.WishlistDao
import com.example.unimarket.data.entities.ImageCacheEntity
import com.example.unimarket.data.entities.OrderEntity
import com.example.unimarket.data.entities.PendingOpEntity
import com.example.unimarket.data.entities.ProductEntity
import com.example.unimarket.data.entities.WishlistEntity
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class UniMarketRepository(
    private val appContext: Context,
    private val productDao: ProductDao,
    private val wishlistDao: WishlistDao,
    private val orderDao: OrderDao,
    private val imageCacheDao: ImageCacheDao,
    private val pendingOpDao: PendingOpDao,
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val storage: FirebaseStorage = Firebase.storage,
    private val gson: Gson = Gson(),
) {
    fun getProducts(cacheTtlMs: Long): Flow<List<ProductEntity>> = flow {
        val now = System.currentTimeMillis()

        val roomList = productDao.getAll()
        if (roomList.isEmpty()) {
            FileCacheManager.readCache(appContext)?.let { json ->
                val backup: List<ProductEntity> = gson.fromJson(
                    json,
                    object : TypeToken<List<ProductEntity>>() {}.type
                )
                emit(backup)
            }
        }

        val cached = productDao.getAll()
        val oldestFetch = cached.minOfOrNull { it.fetchedAt } ?: 0L
        if (cached.isNotEmpty() && now - oldestFetch < cacheTtlMs) {
            emit(cached)
        } else {
            val snapshot = firestore.collection("Product").get().await()
            val fresh = snapshot.documents.map { doc ->
                ProductEntity(
                    id          = doc.id,
                    title       = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    price       = doc.getDouble("price") ?: 0.0,
                    imageUrls   = doc.get("imageUrls") as? List<String> ?: emptyList(),
                    labels      = doc.get("labels")    as? List<String> ?: emptyList(),
                    status      = doc.getString("status") ?: "",
                    fetchedAt   = now
                )
            }
            productDao.insertAll(fresh)
            FileCacheManager.writeCache(appContext, gson.toJson(fresh))
            emit(fresh)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun toggleWishlist(userId: String, productId: String) = withContext(Dispatchers.IO) {
        // Check if the product is already in the wishlist
        val exists = wishlistDao.count(productId) > 0

        // Insert pending operation
        val payload = WishlistOpPayload(userId, productId, !exists)
        pendingOpDao.insert(
            PendingOpEntity(
                type = "WISHLIST",
                payload = gson.toJson(payload),
                createdAt = Date().time
            )
        )

        // Update the local database
        if (exists) wishlistDao.delete(WishlistEntity(productId, 0))
        else wishlistDao.insert(WishlistEntity(productId, Date().time))
    }

    suspend fun markUnavailable(productId: String) = withContext(Dispatchers.IO) {
        val payload = MarkUnavailablePayload(productId)
        pendingOpDao.insert(
            PendingOpEntity(
                type = "MARK_UNAVAILABLE",
                payload = gson.toJson(payload),
                createdAt = Date().time
            )
        )
    }

    suspend fun createOrder(order: OrderEntity, hashConfirm: String) = withContext(Dispatchers.IO) {
        val payload = CreateOrderPayload(
            buyerId = order.buyerId,
            sellerId = order.sellerId,
            hashConfirm = hashConfirm,
            price = order.price,
            productId = order.productId,
            status = order.status
        )
        pendingOpDao.insert(
            PendingOpEntity(
                type = "CREATE_ORDER",
                payload = gson.toJson(payload),
                createdAt = Date().time
            )
        )
        orderDao.insert(order)
    }

    suspend fun uploadImage(localUri: String, remotePath: String) = withContext(Dispatchers.IO) {
        imageCacheDao.insert(
            ImageCacheEntity(
                localUri = localUri,
                remotePath = remotePath,
                state = "PENDING"
            )
        )

        val payload = UploadImagePayload(localUri, remotePath)
        pendingOpDao.insert(
            PendingOpEntity(
                type = "UPLOAD_IMAGE",
                payload = gson.toJson(payload),
                createdAt = Date().time
            )
        )
    }

    fun getWishlistIds(): Flow<Set<String>> = flow {
        wishlistDao.getAll().let { list ->
            emit(list.map { it.productId }.toSet())
        }
    }.flowOn(Dispatchers.IO)

    val maxKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxKb / 8
    private val productCache = object : LruCache<String, ProductEntity>(cacheSize) {
        override fun sizeOf(key: String, value: ProductEntity) = 1
    }

    fun getProductByIdCached(productId: String, cacheTtlMs: Long): Flow<ProductEntity> = flow {
        productCache[productId]?.let {
            emit(it)
        } ?: run {
            val entity = getProductById(productId, cacheTtlMs).first()
            productCache.put(productId, entity)
            emit(entity)
        }
    }.flowOn(Dispatchers.IO)

    fun getProductById(productId: String, cacheTtlMs: Long): Flow<ProductEntity> = flow {
        val doc = firestore.collection("Product").document(productId).get().await()
        val now = System.currentTimeMillis()
        emit(
            ProductEntity(
                id = doc.id,
                title = doc.getString("title")!!,
                description = doc.getString("description")!!,
                price = doc.getDouble("price")!!,
                imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                labels = doc.get("labels") as? List<String> ?: emptyList(),
                status = doc.getString("status")!!,
                fetchedAt = now
            )
        )
    }.flowOn(Dispatchers.IO)
}