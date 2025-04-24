package com.example.unimarket.data

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class UniMarketRepository(
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
        val cached = productDao.getAll()
        if (cached.isNotEmpty() && now - (cached.minOf { it.fetchedAt }) < cacheTtlMs) {
            emit(cached)
        } else {
            val snapshot = firestore.collection("Product").get().await()
            val fresh = snapshot.documents.map { doc ->
                ProductEntity(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                    labels = doc.get("labels") as? List<String> ?: emptyList(),
                    status = doc.getString("status") ?: "",
                    fetchedAt = now
                )
            }
            productDao.insertAll(fresh)
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
        imageCacheDao.insert(ImageCacheEntity(
            localUri = localUri,
            remotePath = remotePath,
            state = "PENDING"
        ))

        val payload = UploadImagePayload(localUri, remotePath)
        pendingOpDao.insert(
            PendingOpEntity(
                type = "UPLOAD_IMAGE",
                payload = gson.toJson(payload),
                createdAt = Date().time
            )
        )
    }

}