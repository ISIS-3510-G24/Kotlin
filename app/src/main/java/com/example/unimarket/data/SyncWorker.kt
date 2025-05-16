package com.example.unimarket.data

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val db = UniMarketDatabase.getInstance(appContext)
    private val pendingDao = db.pendingOpDao()
    private val imageDao = db.imageCacheDao()

    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val ops = pendingDao.getAll()
        for (op in ops) {
            when (op.type) {
                "WISHLIST" -> {
                    try {
                        val p = gson.fromJson(op.payload, WishlistOpPayload::class.java)
                        val docRef = firestore.collection("User")
                            .document(p.userId)
                            .collection("wishlist")
                            .document(p.productId)

                        if (p.add) {
                            docRef.set(mapOf("addedAt" to FieldValue.serverTimestamp())).await()
                        } else {
                            docRef.delete().await()
                        }
                        pendingDao.delete(op)
                    } catch (e: Exception) {
                        return Result.retry()
                    }
                }

                "MARK_UNAVAILABLE" -> {
                    try {
                        val p = gson.fromJson(op.payload, MarkUnavailablePayload::class.java)
                        firestore.collection("Product")
                            .document(p.productId)
                            .update("status", "Unavailable")
                            .await()
                        pendingDao.delete(op)
                    } catch (e: Exception) {
                        return Result.retry()
                    }
                }

                "CREATE_ORDER" -> {
                    try {
                        val p = gson.fromJson(op.payload, CreateOrderPayload::class.java)
                        firestore.collection("orders")
                            .add(
                                mapOf(
                                    "buyerID" to p.buyerId,
                                    "sellerID" to p.sellerId,
                                    "hashConfirm" to p.hashConfirm,
                                    "orderDate" to FieldValue.serverTimestamp(),
                                    "price" to p.price,
                                    "productID" to p.productId,
                                    "status" to p.status
                                )
                            )
                            .await()
                        pendingDao.delete(op)
                    } catch (e: Exception) {
                        return Result.retry()
                    }
                }

                "UPLOAD_IMAGE" -> {
                    val p = gson.fromJson(op.payload, UploadImagePayload::class.java)
                    val uri = Uri.parse(p.localUri)
                    try {
                        val ref = storage.reference.child(p.remotePath)
                        ref.putFile(uri).await()

                        val downloadUrl = ref.downloadUrl.await().toString()

                        imageDao.updateEntry(
                            localUri = p.localUri,
                            remotePath = p.remotePath,
                            state = "SUCCESS",
                            downloadUrl = downloadUrl
                        )
                    } catch (e: Exception) {
                        imageDao.updateEntry(
                            localUri = p.localUri,
                            remotePath = p.remotePath,
                            state = "FAILED",
                            downloadUrl = null
                        )
                    } finally {
                        pendingDao.delete(op)
                    }
                }

                "PUBLISH_PRODUCT" -> {
                    try {
                        val p = gson.fromJson(op.payload, PublishProductPayload::class.java)
                        val map = mapOf<String, Any>(
                            "majorID" to p.majorId,
                            "classId" to p.classId,
                            "title" to p.title,
                            "description" to p.description,
                            "price" to p.price,
                            "labels" to p.labels,
                            "imageUrls" to p.imageUrls,
                            "status" to p.status,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        firestore.collection("Product")
                            .add(map)
                            .await()
                        pendingDao.delete(op)
                    } catch (e: Exception) {
                        return Result.retry()
                    }
                }

                "PUBLISH_WITH_IMAGE" -> {
                    try {
                        val p = gson.fromJson(op.payload, PublishWithImagePayload::class.java)
                        val ref = storage.reference.child(p.remotePath)
                        ref.putFile(Uri.parse(p.localImageUri)).await()
                        val downloadUrl = ref.downloadUrl.await().toString()
                        val data = mapOf<String, Any>(
                            "majorID" to p.majorId,
                            "classId" to p.classId,
                            "title" to p.title,
                            "description" to p.description,
                            "price" to p.price,
                            "labels" to p.labels,
                            "imageUrls" to listOf(downloadUrl),
                            "status" to p.status,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        firestore.collection("Product")
                            .add(data)
                            .await()
                        pendingDao.delete(op)
                    } catch (_: Exception) {
                        return Result.retry()
                    }
                }

                else -> pendingDao.delete(op)
            }
        }
        return Result.success()
    }
}
