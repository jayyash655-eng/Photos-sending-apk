package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuthorizedUser
import com.example.data.model.DownloadRecord
import com.example.data.model.VaultItem
import com.example.data.repository.VaultRepository
import com.example.data.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class ScreenState {
    LOGIN,
    VISITOR_GALLERY,
    FILE_PREVIEW,
    ADMIN_DASHBOARD,
    DOWNLOAD_HISTORY
}

enum class AdminTab {
    DOWNLOAD_TRACKER,
    USERS_LIST,
    FILES_LIST,
    SETTINGS
}

data class DashboardMetrics(
    val totalDownloads: Int = 0,
    val uniqueUsersDownloaded: Int = 0,
    val totalVaultFiles: Int = 0,
    val totalAuthorizedUsers: Int = 0,
    val mostDownloadedFileName: String = "None yet",
    val mostDownloadedCount: Int = 0,
    val downloadsLast24h: Int = 0
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(application)

    private val _currentScreen = MutableStateFlow(ScreenState.LOGIN)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _loggedInVisitor = MutableStateFlow<AuthorizedUser?>(null)
    val loggedInVisitor: StateFlow<AuthorizedUser?> = _loggedInVisitor.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminTab.DOWNLOAD_TRACKER)
    val adminTab: StateFlow<AdminTab> = _adminTab.asStateFlow()

    // Visitor gallery search & filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL") // "ALL", "IMAGE", "PDF"
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    // Preview
    private val _selectedVaultItem = MutableStateFlow<VaultItem?>(null)
    val selectedVaultItem: StateFlow<VaultItem?> = _selectedVaultItem.asStateFlow()

    private val _pdfPageBitmap = MutableStateFlow<Bitmap?>(null)
    val pdfPageBitmap: StateFlow<Bitmap?> = _pdfPageBitmap.asStateFlow()

    private val _pdfCurrentPage = MutableStateFlow(0)
    val pdfCurrentPage: StateFlow<Int> = _pdfCurrentPage.asStateFlow()

    private val _pdfTotalPages = MutableStateFlow(1)
    val pdfTotalPages: StateFlow<Int> = _pdfTotalPages.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    // Feedback
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Tracking filter
    private val _trackerUserFilter = MutableStateFlow("")
    val trackerUserFilter: StateFlow<String> = _trackerUserFilter.asStateFlow()

    private val _trackerTypeFilter = MutableStateFlow("ALL")
    val trackerTypeFilter: StateFlow<String> = _trackerTypeFilter.asStateFlow()

    // Raw flows from repository
    val allUsers: StateFlow<List<AuthorizedUser>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVaultItems: StateFlow<List<VaultItem>> = repository.allVaultItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDownloadRecords: StateFlow<List<DownloadRecord>> = repository.allDownloadRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered gallery items for logged in visitor based on permissions + search + category filter
    val filteredVisitorItems: StateFlow<List<VaultItem>> = combine(
        allVaultItems,
        _loggedInVisitor,
        _searchQuery,
        _typeFilter
    ) { items, visitor, query, filter ->
        if (visitor == null) return@combine emptyList()

        items.filter { item ->
            // 1. Check user permission tier
            val tierAllowed = when (visitor.permissionTier) {
                "IMAGES_ONLY" -> item.fileType == "IMAGE"
                "PDFS_ONLY" -> item.fileType == "PDF"
                else -> true // "ALL"
            }
            if (!tierAllowed) return@filter false

            // 2. Check UI filter
            val filterMatch = when (filter) {
                "IMAGE" -> item.fileType == "IMAGE"
                "PDF" -> item.fileType == "PDF"
                else -> true
            }
            if (!filterMatch) return@filter false

            // 3. Check search query
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                item.title.lowercase().contains(q) ||
                        item.description.lowercase().contains(q) ||
                        item.category.lowercase().contains(q) ||
                        item.fileName.lowercase().contains(q)
            } else {
                true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered download logs for Admin Tracker
    val filteredDownloadRecords: StateFlow<List<DownloadRecord>> = combine(
        allDownloadRecords,
        _trackerUserFilter,
        _trackerTypeFilter
    ) { records, userFilter, typeFilter ->
        records.filter { record ->
            val matchUser = if (userFilter.isBlank()) true else {
                record.username.contains(userFilter.trim(), ignoreCase = true) ||
                        record.displayName.contains(userFilter.trim(), ignoreCase = true)
            }
            val matchType = when (typeFilter) {
                "IMAGE" -> record.fileType == "IMAGE"
                "PDF" -> record.fileType == "PDF"
                else -> true
            }
            matchUser && matchType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allDownloadRecords,
        allVaultItems,
        allUsers
    ) { downloads, items, users ->
        val totalDownloads = downloads.size
        val uniqueUsers = downloads.map { it.username.lowercase() }.distinct().size
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val last24hCount = downloads.count { it.timestamp >= oneDayAgo }

        val topFileEntry = downloads.groupBy { it.fileTitle }
            .maxByOrNull { it.value.size }

        DashboardMetrics(
            totalDownloads = totalDownloads,
            uniqueUsersDownloaded = uniqueUsers,
            totalVaultFiles = items.size,
            totalAuthorizedUsers = users.size,
            mostDownloadedFileName = topFileEntry?.key ?: "None yet",
            mostDownloadedCount = topFileEntry?.value?.size ?: 0,
            downloadsLast24h = last24hCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()
        }
    }

    // Navigation
    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun setAdminTab(tab: AdminTab) {
        _adminTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: String) {
        _typeFilter.value = filter
    }

    fun setTrackerUserFilter(filter: String) {
        _trackerUserFilter.value = filter
    }

    fun setTrackerTypeFilter(filter: String) {
        _trackerTypeFilter.value = filter
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    // Visitor Authentication
    fun loginVisitor(username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || pass.isBlank()) {
                onResult(false, "Please enter both username and password.")
                return@launch
            }
            val user = repository.authenticateVisitor(username, pass)
            if (user != null) {
                if (!user.isActive) {
                    onResult(false, "Account is disabled. Contact administrator.")
                } else {
                    _loggedInVisitor.value = user
                    _currentScreen.value = ScreenState.VISITOR_GALLERY
                    onResult(true, null)
                }
            } else {
                onResult(false, "Invalid credentials or user not in authorized list.")
            }
        }
    }

    fun logoutVisitor() {
        _loggedInVisitor.value = null
        _selectedVaultItem.value = null
        _searchQuery.value = ""
        _typeFilter.value = "ALL"
        _currentScreen.value = ScreenState.LOGIN
    }

    // Admin Authentication
    fun loginAdmin(passcode: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (passcode.isBlank()) {
                onResult(false, "Enter admin passcode.")
                return@launch
            }
            val valid = repository.verifyAdminPasscode(passcode)
            if (valid) {
                _isAdminLoggedIn.value = true
                _currentScreen.value = ScreenState.ADMIN_DASHBOARD
                onResult(true, null)
            } else {
                onResult(false, "Incorrect admin passcode.")
            }
        }
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
        _currentScreen.value = ScreenState.LOGIN
    }

    // Item Selection & Preview
    fun openItemPreview(item: VaultItem) {
        _selectedVaultItem.value = item
        _pdfCurrentPage.value = 0
        _currentScreen.value = ScreenState.FILE_PREVIEW

        if (item.fileType == "PDF") {
            loadPdfPage(item, 0)
        } else {
            _pdfPageBitmap.value = null
        }
    }

    fun closePreview() {
        _selectedVaultItem.value = null
        _pdfPageBitmap.value = null
        if (_isAdminLoggedIn.value) {
            _currentScreen.value = ScreenState.ADMIN_DASHBOARD
        } else if (_loggedInVisitor.value != null) {
            _currentScreen.value = ScreenState.VISITOR_GALLERY
        } else {
            _currentScreen.value = ScreenState.LOGIN
        }
    }

    fun loadPdfPage(item: VaultItem, page: Int) {
        viewModelScope.launch {
            val file = File(item.localFilePath)
            if (file.exists()) {
                val (bmp, totalPages) = FileUtils.renderPdfPageToBitmap(file, page)
                _pdfPageBitmap.value = bmp
                _pdfCurrentPage.value = page.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                _pdfTotalPages.value = totalPages
            }
        }
    }

    fun nextPdfPage() {
        val item = _selectedVaultItem.value ?: return
        if (_pdfCurrentPage.value < _pdfTotalPages.value - 1) {
            loadPdfPage(item, _pdfCurrentPage.value + 1)
        }
    }

    fun previousPdfPage() {
        val item = _selectedVaultItem.value ?: return
        if (_pdfCurrentPage.value > 0) {
            loadPdfPage(item, _pdfCurrentPage.value - 1)
        }
    }

    // Secure Download with Audit Logging
    fun downloadItem(item: VaultItem) {
        val visitor = _loggedInVisitor.value
        if (visitor == null && !_isAdminLoggedIn.value) {
            showSnackbar("Please login to download files.")
            return
        }

        val userForLogging = visitor ?: AuthorizedUser(
            id = -1,
            username = "admin",
            password = "",
            displayName = "Administrator",
            permissionTier = "ALL"
        )

        viewModelScope.launch {
            _isDownloading.value = true
            try {
                val (record, destFile) = repository.executeDownloadAndTrack(userForLogging, item)
                _isDownloading.value = false
                showSnackbar("Downloaded: ${record.fileName} (${FileUtils.formatFileSize(record.fileSizeBytes)})")
            } catch (e: Exception) {
                _isDownloading.value = false
                showSnackbar("Download failed: ${e.message}")
            }
        }
    }

    // Admin Operations: User Management
    fun addAuthorizedUser(
        username: String,
        password: String,
        displayName: String,
        permissionTier: String,
        notes: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedUser = username.trim()
            val trimmedPass = password.trim()
            if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
                onError("Username and password cannot be empty.")
                return@launch
            }
            val existing = allUsers.value.any { it.username.equals(trimmedUser, ignoreCase = true) }
            if (existing) {
                onError("Username '$trimmedUser' already exists in the list.")
                return@launch
            }

            val newUser = AuthorizedUser(
                username = trimmedUser,
                password = trimmedPass,
                displayName = displayName.ifBlank { trimmedUser },
                permissionTier = permissionTier,
                notes = notes,
                isActive = isActive
            )
            repository.addUser(newUser)
            showSnackbar("Added user '${newUser.displayName}' to authorized list")
            onSuccess()
        }
    }

    fun updateAuthorizedUser(
        user: AuthorizedUser,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateUser(user)
            showSnackbar("Updated user '${user.displayName}'")
            onSuccess()
        }
    }

    fun deleteAuthorizedUser(user: AuthorizedUser) {
        viewModelScope.launch {
            repository.deleteUser(user)
            showSnackbar("Removed user '${user.displayName}' from list")
        }
    }

    // Admin Operations: File Management
    fun uploadFileFromUri(uri: Uri, customTitle: String?) {
        viewModelScope.launch {
            val item = repository.uploadFromUri(uri, customTitle)
            if (item != null) {
                showSnackbar("Uploaded '${item.title}' successfully!")
            } else {
                showSnackbar("Failed to import file. Please try another.")
            }
        }
    }

    fun createAdminSampleFile(title: String, type: String, category: String) {
        viewModelScope.launch {
            val item = repository.createAdminSampleFile(title, type, category)
            showSnackbar("Created sample ${item.fileType} '${item.title}'")
        }
    }

    fun deleteVaultItem(item: VaultItem) {
        viewModelScope.launch {
            repository.deleteVaultItem(item)
            showSnackbar("Deleted '${item.title}' from vault")
        }
    }

    // Admin Operations: Passcode & Logs
    fun updateAdminPasscode(newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (newPass.trim().length < 4) {
                onError("Passcode must be at least 4 characters.")
                return@launch
            }
            repository.updateAdminPasscode(newPass)
            showSnackbar("Admin passcode updated successfully.")
            onSuccess()
        }
    }

    fun clearAllDownloadLogs() {
        viewModelScope.launch {
            repository.clearDownloadRecords()
            showSnackbar("All download audit logs cleared.")
        }
    }
}
