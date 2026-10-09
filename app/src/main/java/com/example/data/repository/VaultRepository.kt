package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.AdminSetting
import com.example.data.model.AuthorizedUser
import com.example.data.model.DownloadRecord
import com.example.data.model.VaultItem
import com.example.data.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class VaultRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val vaultItemDao = db.vaultItemDao()
    private val downloadRecordDao = db.downloadRecordDao()
    private val adminSettingDao = db.adminSettingDao()

    val allUsersFlow: Flow<List<AuthorizedUser>> = userDao.getAllUsersFlow()
    val allVaultItemsFlow: Flow<List<VaultItem>> = vaultItemDao.getAllItemsFlow()
    val allDownloadRecordsFlow: Flow<List<DownloadRecord>> = downloadRecordDao.getAllDownloadsFlow()

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        // 1. Initialize Admin Passcode if not present
        val existingPasscode = adminSettingDao.getSetting("admin_passcode")
        if (existingPasscode == null) {
            adminSettingDao.setSetting(AdminSetting("admin_passcode", "admin123"))
        }

        // 2. Pre-seed authorized users if database has none
        val userCount = userDao.getUserCount().first()
        if (userCount == 0) {
            val initialUsers = listOf(
                AuthorizedUser(
                    username = "alice",
                    password = "alicepassword",
                    displayName = "Alice Walker",
                    permissionTier = "ALL",
                    isActive = true,
                    notes = "Executive Partner - Full access to all vault contents"
                ),
                AuthorizedUser(
                    username = "bob",
                    password = "bobpassword",
                    displayName = "Bob Miller",
                    permissionTier = "PDFS_ONLY",
                    isActive = true,
                    notes = "External Financial Auditor - PDF documents access only"
                ),
                AuthorizedUser(
                    username = "sarah",
                    password = "sarahpassword",
                    displayName = "Sarah Jenkins",
                    permissionTier = "IMAGES_ONLY",
                    isActive = true,
                    notes = "Brand & Creative Lead - Image assets only"
                )
            )
            initialUsers.forEach { userDao.insertUser(it) }
        }

        // 3. Pre-seed Vault Items if empty
        val itemCount = vaultItemDao.getItemCount().first()
        if (itemCount == 0) {
            val sampleItems = FileUtils.createInitialSeedFiles(context)
            sampleItems.forEach { vaultItemDao.insertItem(it) }
        }
    }

    suspend fun authenticateVisitor(username: String, password: String): AuthorizedUser? = withContext(Dispatchers.IO) {
        val user = userDao.authenticate(username.trim(), password.trim())
        if (user != null && user.isActive) {
            user
        } else {
            null
        }
    }

    suspend fun verifyAdminPasscode(passcode: String): Boolean = withContext(Dispatchers.IO) {
        val stored = adminSettingDao.getSetting("admin_passcode")?.value ?: "admin123"
        stored == passcode.trim()
    }

    suspend fun updateAdminPasscode(newPasscode: String): Boolean = withContext(Dispatchers.IO) {
        if (newPasscode.isBlank()) return@withContext false
        adminSettingDao.setSetting(AdminSetting("admin_passcode", newPasscode.trim()))
        true
    }

    suspend fun getAdminPasscode(): String = withContext(Dispatchers.IO) {
        adminSettingDao.getSetting("admin_passcode")?.value ?: "admin123"
    }

    // User management (Admin only)
    suspend fun addUser(user: AuthorizedUser): Long = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: AuthorizedUser) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: AuthorizedUser) = withContext(Dispatchers.IO) {
        userDao.deleteUser(user)
    }

    // Vault items management
    suspend fun addVaultItem(item: VaultItem): Long = withContext(Dispatchers.IO) {
        vaultItemDao.insertItem(item)
    }

    suspend fun deleteVaultItem(item: VaultItem) = withContext(Dispatchers.IO) {
        vaultItemDao.deleteItem(item)
        // Clean up file if present in vault_files
        try {
            val file = File(item.localFilePath)
            if (file.exists() && file.parentFile?.name == "vault_files") {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun uploadFromUri(uri: Uri, customTitle: String?): VaultItem? = withContext(Dispatchers.IO) {
        val item = FileUtils.copyUriToVault(context, uri, customTitle)
        if (item != null) {
            val id = vaultItemDao.insertItem(item)
            item.copy(id = id)
        } else {
            null
        }
    }

    suspend fun createAdminSampleFile(title: String, type: String, category: String): VaultItem = withContext(Dispatchers.IO) {
        val item = FileUtils.createAdminSampleFile(context, title, type, category)
        val id = vaultItemDao.insertItem(item)
        item.copy(id = id)
    }

    // Download & Track
    suspend fun executeDownloadAndTrack(
        user: AuthorizedUser,
        item: VaultItem
    ): Pair<DownloadRecord, File> = withContext(Dispatchers.IO) {
        val (destFile, _) = FileUtils.downloadVaultItemToDevice(context, item)

        val record = DownloadRecord(
            userId = user.id,
            username = user.username,
            displayName = user.displayName,
            fileId = item.id,
            fileName = item.fileName,
            fileTitle = item.title,
            fileType = item.fileType,
            fileSizeBytes = item.fileSizeBytes,
            timestamp = System.currentTimeMillis(),
            targetSavedPath = destFile.absolutePath
        )

        val recordId = downloadRecordDao.insertRecord(record)
        Pair(record.copy(id = recordId), destFile)
    }

    suspend fun clearDownloadRecords() = withContext(Dispatchers.IO) {
        downloadRecordDao.clearAll()
    }
}
