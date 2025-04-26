package com.example.unimarket.data

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.unimarket.data.entities.PendingOpEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = UniMarketDatabase.getInstance(appContext)
    private val pendingDao = db.pendingOpDao()
    private val imageDao   = db.imageCacheDao()

    private val firestore = Firebase.firestore
    private val storage   = Firebase.storage
    private val gson      = Gson()

    override suspend fun doWork(): Result {
        val ops: List<PendingOpEntity> = pendingDao.getAll()
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
                                    "buyerID"     to p.buyerId,
                                    "sellerID"    to p.sellerId,
                                    "hashConfirm" to p.hashConfirm,
                                    "orderDate"   to FieldValue.serverTimestamp(),
                                    "price"       to p.price,
                                    "productID"   to p.productId,
                                    "status"      to p.status
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
                            localUri    = p.localUri,
                            remotePath  = p.remotePath,
                            state       = "SUCCESS",
                            downloadUrl = downloadUrl
                        )
                    } catch (e: Exception) {
                        imageDao.updateEntry(
                            localUri    = p.localUri,
                            remotePath  = p.remotePath,
                            state       = "FAILED",
                            downloadUrl = null
                        )
                    } finally {
                        pendingDao.delete(op)
                    }
                }

                else -> {
                    pendingDao.delete(op)
                }
            }
        }
        return Result.success()
    }
}
