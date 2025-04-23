package com.example.unimarket.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_cache")
data class ImageCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localUri: String,
    val remotePath: String,
    val state: String
)
