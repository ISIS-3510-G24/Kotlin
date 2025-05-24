package com.example.unimarket.data

import android.content.Context
import android.net.Uri
import androidx.collection.LruCache
import com.example.unimarket.data.daos.FindDao
import com.example.unimarket.data.daos.ImageCacheDao
import com.example.unimarket.data.daos.OrderDao
import com.example.unimarket.data.daos.PendingOpDao
import com.example.unimarket.data.daos.ProductDao
import com.example.unimarket.data.daos.UserReviewDao
import com.example.unimarket.data.daos.WishlistDao
import com.example.unimarket.data.entities.FindEntity
import com.example.unimarket.data.entities.ImageCacheEntity
import com.example.unimarket.data.entities.OrderEntity
import com.example.unimarket.data.entities.PendingOpEntity
import com.example.unimarket.data.entities.ProductEntity
import com.example.unimarket.data.entities.UserReviewEntity
import com.example.unimarket.data.entities.WishlistEntity
import com.example.unimarket.di.IoDispatcher
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

data class RatingStats(val average: Double, val count: Int)

class UniMarketRepository(
    private val appContext: Context,
    private val productDao: ProductDao,
    private val wishlistDao: WishlistDao,
    private val findDao: FindDao,
    private val orderDao: OrderDao,
    private val imageCacheDao: ImageCacheDao,
    private val pendingOpDao: PendingOpDao,
    private val userReviewDao: UserReviewDao,
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val storage: FirebaseStorage = Firebase.storage,
    private val gson: Gson = Gson(),
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getProducts(cacheTtlMs: Long): Flow<List<ProductEntity>> =
        productDao.observeAll()
            .flatMapLatest { cached ->
                flow {
                    val now = System.currentTimeMillis()
                    if (cached.isEmpty()) {
                        FileCacheManager.readCache(appContext, "finds")?.let { json ->
                            val backup: List<ProductEntity> = gson.fromJson(
                                json, object : TypeToken<List<ProductEntity>>() {}.type
                            )
                            emit(backup)
                        }
                    }
                    val oldest = cached.minOfOrNull { it.fetchedAt } ?: 0L
                    if (cached.isNotEmpty() && now - oldest < cacheTtlMs) {
                        emit(cached)
                    } else {
                        val snap = firestore.collection("Product").get().await()
                        val fresh = snap.documents.map { d ->
                            ProductEntity(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                description = d.getString("description") ?: "",
                                price = d.getDouble("price") ?: 0.0,
                                imageUrls = d.get("imageUrls") as? List<String> ?: emptyList(),
                                labels = d.get("labels") as? List<String> ?: emptyList(),
                                status = d.getString("status") ?: "",
                                majorID = d.getString("majorID") ?: "",
                                classId = d.getString("classID") ?: "",
                                sellerID = d.getString("sellerID") ?: "",
                                fetchedAt = now
                            )
                        }
                        productDao.insertAll(fresh)
                        emit(fresh)
                    }
                }.flowOn(ioDispatcher)
            }.flowOn(ioDispatcher)

    suspend fun toggleWishlist(userId: String, productId: String) = withContext(ioDispatcher) {
        // Check if the product is already in the wishlist
        val exists = wishlistDao.count(productId) > 0

        pendingOpDao.insert(
            PendingOpEntity(
                type = "WISHLIST",
                payload = gson.toJson(WishlistOpPayload(userId, productId, !exists)),
                createdAt = Date().time
            )
        )

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

    suspend fun uploadImage(localUri: String, remotePath: String) = withContext(ioDispatcher) {
        imageCacheDao.insert(
            ImageCacheEntity(
                localUri = localUri,
                remotePath = remotePath,
                state = "PENDING"
            )
        )

        val payload = UploadImagePayload(localUri = localUri, remotePath = remotePath)
        pendingOpDao.insert(
            PendingOpEntity(
                type = "UPLOAD_IMAGE",
                payload = gson.toJson(payload),
                createdAt = Date().time
            )
        )
    }

    fun observeImageCacheEntries() = imageCacheDao.observeAll().flowOn(ioDispatcher)

    fun getWishlistIds(): Flow<Set<String>> =
        wishlistDao.observeIds()
            .map { it.toSet() }
            .flowOn(ioDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getWishlistProducts(cacheTtlMs: Long): Flow<List<ProductEntity>> =
        getWishlistIds()
            .flatMapLatest { ids ->
                productDao.observeAll()
                    .map { list -> list.filter { it.id in ids } }
            }
            .flowOn(ioDispatcher)

    val maxKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxKb / 8
    private val productCache = object : LruCache<String, ProductEntity>(cacheSize) {
        override fun sizeOf(key: String, value: ProductEntity) = 1
    }

    fun getProductByIdCached(productId: String, cacheTtlMs: Long): Flow<ProductEntity> = flow {
        productCache[productId]?.let {
            emit(it)
            return@flow
        }
        val fromDb = productDao.getById(productId)
        if (fromDb != null) {
            productCache.put(productId, fromDb)
            emit(fromDb)
            return@flow
        }

        val remote = try {
            getProductById(productId, cacheTtlMs).first()
        } catch (e: Exception) {
            throw Exception("Could not fetch detail: ${e.message}")
        }

        productCache.put(productId, remote)
        productDao.insert(remote)
        emit(remote)
    }.flowOn(Dispatchers.IO)

    fun getProductById(productId: String, cacheTtlMs: Long): Flow<ProductEntity> = flow {
        val doc = firestore.collection("Product").document(productId).get().await()

        if (!doc.exists()) {
            throw Exception("Product not found")
        }

        val now = System.currentTimeMillis()
        emit(
            ProductEntity(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                price = doc.getDouble("price") ?: 0.0,
                imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                labels = doc.get("labels") as? List<String> ?: emptyList(),
                status = doc.getString("status") ?: "",
                majorID = doc.getString("majorID") ?: "",
                classId = doc.getString("classID") ?: "",
                sellerID = doc.getString("sellerID") ?: "",
                fetchedAt = now
            )
        )
    }.flowOn(Dispatchers.IO)

    fun observeOrders(): Flow<List<OrderEntity>> =
        orderDao.observeAll()
            .flowOn(ioDispatcher)

    suspend fun clearImageCacheEntry(entry: ImageCacheEntity) = with(ioDispatcher) {
        imageCacheDao.delete(entry)
    }

    suspend fun enqueuePublishProduct(payload: PublishProductPayload) =
        withContext(ioDispatcher) {
            val op = PendingOpEntity(
                type = "PUBLISH_PRODUCT",
                payload = gson.toJson(payload),
                createdAt = Date().time
            )
            pendingOpDao.insert(op)
        }

    suspend fun enqueuePublishWithImage(
        payload: PublishWithImagePayload,
    ) = withContext(ioDispatcher) {
        val op = PendingOpEntity(
            type = "PUBLISH_WITH_IMAGE",
            payload = gson.toJson(payload),
            createdAt = Date().time
        )
        pendingOpDao.insert(op)
    }

    suspend fun publishProductWithImage(
        payload: PublishProductPayload,
        localImageUri: String,
        online: Boolean,
    ) = withContext(ioDispatcher) {
        val remotePath = payload.imageUrls.first()

        if (online) {
            try {
                val ref = storage.reference.child(remotePath)
                ref.putFile(Uri.parse(localImageUri)).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                val map = mapOf<String, Any>(
                    "majorID" to payload.majorId,
                    "classId" to payload.classId,
                    "title" to payload.title,
                    "description" to payload.description,
                    "price" to payload.price,
                    "labels" to payload.labels,
                    "imageUrls" to listOf(downloadUrl),
                    "status" to payload.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("Product")
                    .add(map)
                    .await()
            } catch (e: Exception) {
                uploadImage(localImageUri, remotePath)
                enqueuePublishProduct(payload)
            }
        } else {
            uploadImage(localImageUri, remotePath)
            enqueuePublishProduct(payload)
        }
    }

    fun observeUserReviewsFor(uid: String) =
        userReviewDao.observeForUser(uid)

    fun observeReviewsByReviewer(reviewerId: String): Flow<List<UserReviewEntity>> =
        userReviewDao.observeByReviewer(reviewerId)

    fun hasReviewedOrder(reviewerId: String, orderId: String): Flow<Boolean> =
        userReviewDao.hasReviewedOrder(reviewerId, orderId)

    fun observeLatestReviewsFor(
        uid: String,
        limit: Int = 10,
    ) = userReviewDao.observeLatestReviewsFor(uid, limit)

    suspend fun syncRemoteReviewsFor(userId: String) = withContext(ioDispatcher) {
        val snaps = firestore.collection("User")
            .document(userId)
            .collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()

        val entities = snaps.documents.map { d ->
            UserReviewEntity(
                localId        = 0,
                orderId        = d.getString("orderId")               ?: "",
                targetUserId   = userId,
                reviewerUserId = d.getString("reviewerUserId")        ?: "",
                rating         = d.getLong("rating")?.toInt()         ?: 0,
                comment        = d.getString("comment")               ?: "",
                createdAt      = d.getTimestamp("createdAt")?.toDate()?.time
                    ?: 0L,
                status         = "SENT"
            )
        }

        userReviewDao.clearSentReviewsFor(userId)
        userReviewDao.insertAll(entities)
    }

    suspend fun postUserReview(
        reviewerId: String,
        targetId: String,
        orderId: String,
        rating: Int,
        comment: String,
    ) {
        val now = System.currentTimeMillis()

        val localId =
            userReviewDao.insert(
                UserReviewEntity(
                    orderId = orderId,
                    targetUserId = targetId,
                    reviewerUserId = reviewerId,
                    rating = rating,
                    comment = comment,
                    createdAt = now,
                    status = "PENDING"
                )
            )

        pendingOpDao.insert(
            PendingOpEntity(
                type = "USER_REVIEW",
                payload = gson.toJson(
                    mapOf(
                        "localId" to localId,
                        "targetUserId" to targetId,
                        "reviewerUserId" to reviewerId,
                        "orderId" to orderId,
                        "rating" to rating,
                        "comment" to comment,
                    )
                ),
                createdAt = now
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFinds(cacheTtlMs: Long): Flow<List<FindEntity>> =
        findDao.observeAll()
            .flatMapLatest { cached ->
                flow {
                    val now = System.currentTimeMillis()

                    if (cached.isEmpty()) {
                        FileCacheManager.readCache(appContext, "finds")?.let { json ->
                            val backup: List<FindEntity> = gson.fromJson(
                                json, object : TypeToken<List<FindEntity>>() {}.type
                            )
                            emit(backup)
                        }
                    }

                    val oldest = cached.minOfOrNull { it.fetchedAt } ?: 0L
                    if (cached.isNotEmpty() && now - oldest < cacheTtlMs) {
                        emit(cached)
                    } else {
                        val snap = firestore.collection("finds").get().await()
                        val fresh = snap.documents.map { d ->
                            FindEntity(
                                id = d.id,
                                title = d.getString("title") ?: "",
                                description = d.getString("description") ?: "",
                                image = d.get("image") as? List<String> ?: emptyList(),
                                labels = d.get("labels") as? List<String> ?: emptyList(),
                                major = d.getString("major") ?: "",
                                offerCount = d.getLong("offerCount")?.toString() ?: "0",
                                status = d.getString("status") ?: "",
                                fetchedAt = now
                            )
                        }
                        findDao.clear()
                        findDao.insertAll(fresh)
                        FileCacheManager.writeCache(appContext, "finds", gson.toJson(fresh))
                        emit(fresh)
                    }
                }.flowOn(ioDispatcher)
            }.flowOn(ioDispatcher)

    suspend fun refreshFinds() = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val snap = firestore.collection("finds").get().await()
        val fresh = snap.documents.map { d ->
            FindEntity(
                id = d.id,
                title = d.getString("title") ?: "",
                description = d.getString("description") ?: "",
                image = d.get("image") as? List<String> ?: emptyList(),
                labels = d.get("labels") as? List<String> ?: emptyList(),
                major = d.getString("major") ?: "",
                offerCount = d.getLong("offerCount")?.toString() ?: "0",
                status = d.getString("status") ?: "",
                fetchedAt = now
            )
        }
        findDao.clear()
        findDao.insertAll(fresh)
        FileCacheManager.writeCache(appContext, "finds", gson.toJson(fresh))
    }

    fun observeFindById(id: String): Flow<FindEntity?> =
        findDao.observeById(id)
            .flowOn(ioDispatcher)

    val findCache = object : LruCache<String, FindEntity>(100) {
        override fun sizeOf(key: String, value: FindEntity) = 1
    }

    fun getFindByIdCached(id: String, cacheTtlMs: Long): Flow<FindEntity> = flow {
        findCache[id]?.let {
            emit(it)
            return@flow
        }
        findDao.getById(id)?.let {
            findCache.put(id, it)
            emit(it)
            return@flow
        }
        val now = System.currentTimeMillis()
        val doc = firestore.collection("finds").document(id).get().await()
        if (!doc.exists()) throw Exception("Find not found")
        val fetched = FindEntity(
            id = doc.id,
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            image = doc.get("image") as? List<String> ?: emptyList(),
            labels = doc.get("labels") as? List<String> ?: emptyList(),
            major = doc.getString("major") ?: "",
            offerCount = doc.getLong("offerCount")?.toString() ?: "0",
            status = doc.getString("status") ?: "",
            fetchedAt = now
        )
        findCache.put(id, fetched)
        findDao.insert(fetched)
        emit(fetched)
    }.flowOn(ioDispatcher)

    fun getFindById(id: String): Flow<FindEntity> = flow {
        val doc = firestore.collection("finds").document(id).get().await()
        if (!doc.exists()) throw Exception("Find not found")
        val entity = FindEntity(
            id = doc.id,
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            image = doc.get("image") as? List<String> ?: emptyList(),
            labels = doc.get("labels") as? List<String> ?: emptyList(),
            major = doc.getString("major") ?: "",
            offerCount = doc.getLong("offerCount")?.toString() ?: "0",
            status = doc.getString("status") ?: "",
            fetchedAt = System.currentTimeMillis()
        )
        emit(entity)
    }.flowOn(ioDispatcher)

    suspend fun postUserReviewImmediate(
        reviewerId: String,
        targetId: String,
        orderId: String,
        rating: Int,
        comment: String,
    ) {
        firestore.collection("User")
            .document(targetId)
            .collection("reviews")
            .add(
                mapOf(
                    "orderId" to orderId,
                    "reviewerUserId" to reviewerId,
                    "rating" to rating,
                    "comment" to comment,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    fun observeUserRatingStats(uid: String): Flow<RatingStats> =
        combine(
            userReviewDao.averageRatingFor(uid).map { it ?: 0.0 },
            userReviewDao.countReviewsFor(uid)
        ) { avg, cnt ->
            RatingStats(avg, cnt)
        }

    suspend fun refreshUserReviews(uid: String) = withContext(ioDispatcher) {
        val snap = firestore
            .collection("User")
            .document(uid)
            .collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()

        val list = snap.documents.map { d->
            UserReviewEntity(
                localId = 0,
                orderId = d.getString("orderId")!!,
                targetUserId = uid,
                reviewerUserId = d.getString("reviewerUserId")!!,
                rating = (d.getLong("rating")!!).toInt(),
                comment = d.getString("comment")!!,
                createdAt = d.getTimestamp("createdAt")!!.toDate().time,
                status = "SENT"
            )
        }

        userReviewDao.clearSentReviewsFor(uid)
        userReviewDao.insertAll(list)
    }
}




