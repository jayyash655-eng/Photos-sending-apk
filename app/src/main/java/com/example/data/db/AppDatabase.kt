package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.AdminSetting
import com.example.data.model.AuthorizedUser
import com.example.data.model.DownloadRecord
import com.example.data.model.VaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM authorized_users ORDER BY displayName ASC")
    fun getAllUsersFlow(): Flow<List<AuthorizedUser>>

    @Query("SELECT * FROM authorized_users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): AuthorizedUser?

    @Query("SELECT * FROM authorized_users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): AuthorizedUser?

    @Query("SELECT * FROM authorized_users WHERE LOWER(username) = LOWER(:username) AND password = :password LIMIT 1")
    suspend fun authenticate(username: String, password: String): AuthorizedUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: AuthorizedUser): Long

    @Update
    suspend fun updateUser(user: AuthorizedUser)

    @Delete
    suspend fun deleteUser(user: AuthorizedUser)

    @Query("SELECT COUNT(*) FROM authorized_users")
    fun getUserCount(): Flow<Int>
}

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun getAllItemsFlow(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): VaultItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem): Long

    @Update
    suspend fun updateItem(item: VaultItem)

    @Delete
    suspend fun deleteItem(item: VaultItem)

    @Query("SELECT COUNT(*) FROM vault_items")
    fun getItemCount(): Flow<Int>
}

@Dao
interface DownloadRecordDao {
    @Query("SELECT * FROM download_records ORDER BY timestamp DESC")
    fun getAllDownloadsFlow(): Flow<List<DownloadRecord>>

    @Query("SELECT * FROM download_records WHERE LOWER(username) = LOWER(:username) ORDER BY timestamp DESC")
    fun getDownloadsForUserFlow(username: String): Flow<List<DownloadRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DownloadRecord): Long

    @Query("DELETE FROM download_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM download_records")
    fun getTotalDownloadCount(): Flow<Int>
}

@Dao
interface AdminSettingDao {
    @Query("SELECT * FROM admin_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AdminSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AdminSetting)
}

@Database(
    entities = [
        AuthorizedUser::class,
        VaultItem::class,
        DownloadRecord::class,
        AdminSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun downloadRecordDao(): DownloadRecordDao
    abstract fun adminSettingDao(): AdminSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_vault.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
