package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "authorized_users",
    indices = [Index(value = ["username"], unique = true)]
)
data class AuthorizedUser(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val password: String,
    val displayName: String,
    val permissionTier: String = "ALL", // "ALL", "IMAGES_ONLY", "PDFS_ONLY"
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val fileType: String, // "IMAGE" or "PDF"
    val fileName: String,
    val localFilePath: String,
    val fileSizeBytes: Long = 0,
    val mimeType: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_records")
data class DownloadRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val username: String,
    val displayName: String,
    val fileId: Long,
    val fileName: String,
    val fileTitle: String,
    val fileType: String, // "IMAGE" or "PDF"
    val fileSizeBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetSavedPath: String = ""
)

@Entity(tableName = "admin_settings")
data class AdminSetting(
    @PrimaryKey
    val key: String,
    val value: String
)
