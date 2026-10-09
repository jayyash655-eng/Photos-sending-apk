package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DownloadHistoryScreen
import com.example.ui.screens.FilePreviewScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.VisitorGalleryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScreenState
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VaultAppRoot()
            }
        }
    }
}

@Composable
fun VaultAppRoot(
    viewModel: VaultViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val loggedInVisitor by viewModel.loggedInVisitor.collectAsStateWithLifecycle()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()
    val adminTab by viewModel.adminTab.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val visitorItems by viewModel.filteredVisitorItems.collectAsStateWithLifecycle()

    val selectedVaultItem by viewModel.selectedVaultItem.collectAsStateWithLifecycle()
    val pdfBitmap by viewModel.pdfPageBitmap.collectAsStateWithLifecycle()
    val pdfCurrentPage by viewModel.pdfCurrentPage.collectAsStateWithLifecycle()
    val pdfTotalPages by viewModel.pdfTotalPages.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()

    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allVaultItems by viewModel.allVaultItems.collectAsStateWithLifecycle()
    val dashboardMetrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val downloadRecords by viewModel.filteredDownloadRecords.collectAsStateWithLifecycle()
    val trackerUserFilter by viewModel.trackerUserFilter.collectAsStateWithLifecycle()
    val trackerTypeFilter by viewModel.trackerTypeFilter.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when (currentScreen) {
            ScreenState.LOGIN -> {
                LoginScreen(
                    onVisitorLogin = { user, pass ->
                        viewModel.loginVisitor(user, pass) { success, err ->
                            if (!success) {
                                loginErrorMessage = err
                            } else {
                                loginErrorMessage = null
                            }
                        }
                    },
                    onAdminLogin = { pass ->
                        viewModel.loginAdmin(pass) { success, err ->
                            if (!success) {
                                loginErrorMessage = err
                            } else {
                                loginErrorMessage = null
                            }
                        }
                    },
                    errorMessage = loginErrorMessage,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenState.VISITOR_GALLERY -> {
                loggedInVisitor?.let { visitor ->
                    VisitorGalleryScreen(
                        user = visitor,
                        items = visitorItems,
                        searchQuery = searchQuery,
                        typeFilter = typeFilter,
                        isDownloading = isDownloading,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterChange = { viewModel.setTypeFilter(it) },
                        onItemClick = { viewModel.openItemPreview(it) },
                        onDownloadItem = { viewModel.downloadItem(it) },
                        onLogout = { viewModel.logoutVisitor() },
                        onOpenDownloadHistory = { viewModel.navigateTo(ScreenState.DOWNLOAD_HISTORY) },
                        modifier = Modifier.padding(innerPadding)
                    )
                } ?: run {
                    viewModel.navigateTo(ScreenState.LOGIN)
                }
            }

            ScreenState.FILE_PREVIEW -> {
                selectedVaultItem?.let { item ->
                    FilePreviewScreen(
                        item = item,
                        pdfBitmap = pdfBitmap,
                        currentPage = pdfCurrentPage,
                        totalPages = pdfTotalPages,
                        isDownloading = isDownloading,
                        onBack = { viewModel.closePreview() },
                        onNextPage = { viewModel.nextPdfPage() },
                        onPrevPage = { viewModel.previousPdfPage() },
                        onDownload = { viewModel.downloadItem(it) },
                        modifier = Modifier.padding(innerPadding)
                    )
                } ?: run {
                    viewModel.closePreview()
                }
            }

            ScreenState.ADMIN_DASHBOARD -> {
                if (isAdminLoggedIn) {
                    AdminDashboardScreen(
                        currentTab = adminTab,
                        metrics = dashboardMetrics,
                        downloadRecords = downloadRecords,
                        users = allUsers,
                        vaultItems = allVaultItems,
                        trackerUserFilter = trackerUserFilter,
                        trackerTypeFilter = trackerTypeFilter,
                        onTabSelected = { viewModel.setAdminTab(it) },
                        onTrackerUserFilterChange = { viewModel.setTrackerUserFilter(it) },
                        onTrackerTypeFilterChange = { viewModel.setTrackerTypeFilter(it) },
                        onAddUser = { user, pass, name, tier, notes, active, onOk, onErr ->
                            viewModel.addAuthorizedUser(user, pass, name, tier, notes, active, onOk, onErr)
                        },
                        onUpdateUser = { updatedUser, onOk ->
                            viewModel.updateAuthorizedUser(updatedUser, onOk)
                        },
                        onDeleteUser = { viewModel.deleteAuthorizedUser(it) },
                        onUploadUri = { uri, title -> viewModel.uploadFileFromUri(uri, title) },
                        onCreateSampleFile = { title, type, cat -> viewModel.createAdminSampleFile(title, type, cat) },
                        onDeleteVaultItem = { viewModel.deleteVaultItem(it) },
                        onPreviewVaultItem = { viewModel.openItemPreview(it) },
                        onUpdateAdminPasscode = { newPass, onOk, onErr ->
                            viewModel.updateAdminPasscode(newPass, onOk, onErr)
                        },
                        onClearLogs = { viewModel.clearAllDownloadLogs() },
                        onLogout = { viewModel.logoutAdmin() },
                        onOpenDownloadHistory = { viewModel.navigateTo(ScreenState.DOWNLOAD_HISTORY) },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    viewModel.navigateTo(ScreenState.LOGIN)
                }
            }

            ScreenState.DOWNLOAD_HISTORY -> {
                DownloadHistoryScreen(
                    downloadRecords = downloadRecords,
                    onBack = {
                        if (isAdminLoggedIn) {
                            viewModel.navigateTo(ScreenState.ADMIN_DASHBOARD)
                        } else if (loggedInVisitor != null) {
                            viewModel.navigateTo(ScreenState.VISITOR_GALLERY)
                        } else {
                            viewModel.navigateTo(ScreenState.LOGIN)
                        }
                    },
                    onClearLogs = { viewModel.clearAllDownloadLogs() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
