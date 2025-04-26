// SyncWorker.kt
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // DAOs
    private val pendingDao = UniMarketDatabase
        .getInstance(appContext)
        .pendingOpDao()
    private val imageDao = UniMarketDatabase
        .getInstance(appContext)
        .imageCacheDao()

    // Firebase
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    // JSON serializer
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val ops = pendingDao.getAll()
        for (op in ops) {
            when (op.type) {
                "WISHLIST" -> {
                    try {
                        val p = gson.fromJson(op.payload, WishlistOpPayload::class.java)
                        val doc = firestore.collection("User")
                            .document(p.userId)
                            .collection("wishlist")
                            .document(p.productId)
                        if (p.add) doc.set(mapOf("addedAt" to FieldValue.serverTimestamp())).await()
                        else doc.delete().await()

                        pendingDao.delete(op)
                    } catch (_: Exception) {
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
                    } catch (_: Exception) {
                        return Result.retry()
                    }
                }

                "CREATE_ORDER" -> {
                    try {
                        val p = gson.fromJson(op.payload, CreateOrderPayload::class.java)
                        firestore.collection("orders")
                            .add(mapOf(
                                "buyerID"     to p.buyerId,
                                "sellerID"    to p.sellerId,
                                "hashConfirm" to p.hashConfirm,
                                "orderDate"   to FieldValue.serverTimestamp(),
                                "price"       to p.price,
                                "productID"   to p.productId,
                                "status"      to p.status
                            ))
                            .await()

                        pendingDao.delete(op)
                    } catch (_: Exception) {
                        return Result.retry()
                    }
                }

                "UPLOAD_IMAGE" -> {
                    val p = gson.fromJson(op.payload, UploadImagePayload::class.java)
                    val uri = Uri.parse(p.localUri)
                    try {
                        storage.reference.child(p.remotePath)
                            .putFile(uri)
                            .await()
                        imageDao.observeAll().first()
                            .filter { it.localUri == p.localUri && it.remotePath == p.remotePath }
                            .forEach { img -> imageDao.update(img.copy(state = "SUCCESS")) }
                    } catch (e: Exception) {
                        imageDao.observeAll().first()
                            .filter { it.localUri == p.localUri && it.remotePath == p.remotePath }
                            .forEach { img -> imageDao.update(img.copy(state = "FAILED")) }
                        pendingDao.delete(op)
                        continue
                    }
                    pendingDao.delete(op)
                }
            }
        }
        return Result.success()
    }
}
