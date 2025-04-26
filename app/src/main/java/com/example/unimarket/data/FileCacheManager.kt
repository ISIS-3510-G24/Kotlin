package com.example.unimarket.data

import android.content.Context

object FileCacheManager {
    private const val FILENMAME = "product_cache.json"

    fun writeCache(context: Context, json: String) {
        context.openFileOutput(FILENMAME, Context.MODE_PRIVATE)
            .use { it.write(json.toByteArray()) }
    }

    fun readCache(context: Context): String? =
        try {
            context.openFileInput(FILENMAME).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
}