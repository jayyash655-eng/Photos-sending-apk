package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthorizedUser
import com.example.data.model.DownloadRecord
import com.example.data.model.VaultItem
import com.example.data.util.FileUtils
import com.example.ui.viewmodel.AdminTab
import com.example.ui.viewmodel.DashboardMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentTab: AdminTab,
    metrics: DashboardMetrics,
    downloadRecords: List<DownloadRecord>,
    users: List<AuthorizedUser>,
    vaultItems: List<VaultItem>,
    trackerUserFilter: String,
    trackerTypeFilter: String,
    onTabSelected: (AdminTab) -> Unit,
    onTrackerUserFilterChange: (String) -> Unit,
    onTrackerTypeFilterChange: (String) -> Unit,
    onAddUser: (username: String, pass: String, name: String, tier: String, notes: String, active: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onUpdateUser: (AuthorizedUser, onSuccess: () -> Unit) -> Unit,
    onDeleteUser: (AuthorizedUser) -> Unit,
    onUploadUri: (Uri, String?) -> Unit,
    onCreateSampleFile: (title: String, type: String, category: String) -> Unit,
    onDeleteVaultItem: (VaultItem) -> Unit,
    onPreviewVaultItem: (VaultItem) -> Unit,
    onUpdateAdminPasscode: (String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onClearLogs: () -> Unit,
    onLogout: () -> Unit,
    onOpenDownloadHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onLogout() }

    // Dialog states
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<AuthorizedUser?>(null) }
    var showUploadModal by remember { mutableStateOf(false) }
    var showCreateSampleModal by remember { mutableStateOf(false) }
    var showClearLogsConfirm by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<AuthorizedUser?>(null) }
    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }

    // Photo picker launcher (zero-permission, complies with Google Play policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadUri(uri, null)
        }
    }

    // Document picker launcher for PDFs
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadUri(uri, null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Control Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Authorized Roster & Download Audit Log",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenDownloadHistory,
                        modifier = Modifier.testTag("admin_open_history_logs")
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Full Download History Screen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Exit to login",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AdminTab.DOWNLOAD_TRACKER,
                    onClick = { onTabSelected(AdminTab.DOWNLOAD_TRACKER) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Downloads") },
                    label = { Text("Audit Log") },
                    modifier = Modifier.testTag("nav_downloads")
                )
                NavigationBarItem(
                    selected = currentTab == AdminTab.USERS_LIST,
                    onClick = { onTabSelected(AdminTab.USERS_LIST) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Users") },
                    label = { Text("User Roster") },
                    modifier = Modifier.testTag("nav_users")
                )
                NavigationBarItem(
                    selected = currentTab == AdminTab.FILES_LIST,
                    onClick = { onTabSelected(AdminTab.FILES_LIST) },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Files") },
                    label = { Text("Vault Files") },
                    modifier = Modifier.testTag("nav_files")
                )
                NavigationBarItem(
                    selected = currentTab == AdminTab.SETTINGS,
                    onClick = { onTabSelected(AdminTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            when (currentTab) {
                AdminTab.USERS_LIST -> {
                    FloatingActionButton(
                        onClick = { showAddUserDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("fab_add_user")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Authorized User")
                    }
                }
                AdminTab.FILES_LIST -> {
                    FloatingActionButton(
                        onClick = { showUploadModal = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("fab_upload_file")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Upload File")
                    }
                }
                else -> {}
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AdminTab.DOWNLOAD_TRACKER -> {
                    DownloadTrackerContent(
                        metrics = metrics,
                        records = downloadRecords,
                        userFilter = trackerUserFilter,
                        typeFilter = trackerTypeFilter,
                        onUserFilterChange = onTrackerUserFilterChange,
                        onTypeFilterChange = onTrackerTypeFilterChange,
                        onClearLogs = { showClearLogsConfirm = true }
                    )
                }
                AdminTab.USERS_LIST -> {
                    UserRosterContent(
                        users = users,
                        onEditUser = { editingUser = it },
                        onDeleteUser = { userToDelete = it }
                    )
                }
                AdminTab.FILES_LIST -> {
                    VaultFilesContent(
                        items = vaultItems,
                        onUploadClick = { showUploadModal = true },
                        onPreview = onPreviewVaultItem,
                        onDelete = { itemToDelete = it }
                    )
                }
                AdminTab.SETTINGS -> {
                    AdminSettingsContent(
                        metrics = metrics,
                        onUpdatePasscode = onUpdateAdminPasscode,
                        onLogout = onLogout
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddUserDialog) {
        UserFormDialog(
            title = "Add Authorized Visitor",
            initialUser = null,
            onDismiss = { showAddUserDialog = false },
            onConfirm = { username, pass, name, tier, notes, active, onError ->
                onAddUser(username, pass, name, tier, notes, active, {
                    showAddUserDialog = false
                }, onError)
            }
        )
    }

    if (editingUser != null) {
        UserFormDialog(
            title = "Edit Authorized Visitor",
            initialUser = editingUser,
            onDismiss = { editingUser = null },
            onConfirm = { username, pass, name, tier, notes, active, _ ->
                val updated = editingUser!!.copy(
                    username = username,
                    password = pass,
                    displayName = name,
                    permissionTier = tier,
                    notes = notes,
                    isActive = active
                )
                onUpdateUser(updated) {
                    editingUser = null
                }
            }
        )
    }

    if (showUploadModal) {
        UploadOptionDialog(
            onDismiss = { showUploadModal = false },
            onPickImage = {
                showUploadModal = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickPdf = {
                showUploadModal = false
                documentPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
            },
            onCreateSample = {
                showUploadModal = false
                showCreateSampleModal = true
            }
        )
    }

    if (showCreateSampleModal) {
        CreateSampleFileDialog(
            onDismiss = { showCreateSampleModal = false },
            onCreate = { title, type, category ->
                onCreateSampleFile(title, type, category)
                showCreateSampleModal = false
            }
        )
    }

    if (showClearLogsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogsConfirm = false },
            title = { Text("Clear All Download Logs?") },
            text = { Text("This will permanently remove the download tracking history. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearLogs()
                        showClearLogsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete Authorized User?") },
            text = { Text("Remove '${userToDelete!!.displayName}' (@${userToDelete!!.username}) from authorized list? They will no longer be able to log in.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(userToDelete!!)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Vault Item?") },
            text = { Text("Remove '${itemToDelete!!.title}' permanently from the vault?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVaultItem(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// TAB 1: Download Tracker / Audit Log
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadTrackerContent(
    metrics: DashboardMetrics,
    records: List<DownloadRecord>,
    userFilter: String,
    typeFilter: String,
    onUserFilterChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // KPI Summary Cards
        item {
            Text(
                text = "Tracking Overview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricCard(
                    title = "Total Downloads",
                    value = metrics.totalDownloads.toString(),
                    subtitle = "${metrics.downloadsLast24h} in last 24h",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Visitors",
                    value = "${metrics.uniqueUsersDownloaded}/${metrics.totalAuthorizedUsers}",
                    subtitle = "Authorized accounts",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricCard(
                    title = "Vault Assets",
                    value = metrics.totalVaultFiles.toString(),
                    subtitle = "Images & PDFs",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Most Downloaded",
                    value = if (metrics.mostDownloadedCount > 0) "${metrics.mostDownloadedCount}x" else "None",
                    subtitle = metrics.mostDownloadedFileName,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search & Filter controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download Audit Trail",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        if (records.isNotEmpty()) {
                            TextButton(onClick = onClearLogs) {
                                Text("Clear Logs", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userFilter,
                        onValueChange = onUserFilterChange,
                        placeholder = { Text("Filter by user name or username...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (userFilter.isNotBlank()) {
                                IconButton(onClick = { onUserFilterChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tracker_search_filter")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = typeFilter == "ALL",
                            onClick = { onTypeFilterChange("ALL") },
                            label = { Text("All Types") }
                        )
                        FilterChip(
                            selected = typeFilter == "IMAGE",
                            onClick = { onTypeFilterChange("IMAGE") },
                            label = { Text("Images") }
                        )
                        FilterChip(
                            selected = typeFilter == "PDF",
                            onClick = { onTypeFilterChange("PDF") },
                            label = { Text("PDFs") }
                        )
                    }
                }
            }
        }

        // Download Log Entries
        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No download events recorded yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Whenever an authorized visitor downloads a file, it will appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                DownloadRecordCard(record = record)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DownloadRecordCard(record: DownloadRecord) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US).format(Date(record.timestamp))
    val isPdf = record.fileType == "PDF"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.displayName.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = record.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPdf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = record.fileType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "@${record.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Downloaded: ${record.fileTitle}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = FileUtils.formatFileSize(record.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// TAB 2: User Management ("written in my list. Only I can edit list")
@Composable
private fun UserRosterContent(
    users: List<AuthorizedUser>,
    onEditUser: (AuthorizedUser) -> Unit,
    onDeleteUser: (AuthorizedUser) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Authorized Visitor List",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Only the administrator can view and edit these credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(users, key = { it.id }) { user ->
            AuthorizedUserCard(
                user = user,
                onEdit = { onEditUser(user) },
                onDelete = { onDeleteUser(user) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB
        }
    }
}

@Composable
private fun AuthorizedUserCard(
    user: AuthorizedUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_card_${user.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Login Username: ${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active / Inactive pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (user.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (user.isActive) "Active" else "Disabled",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (user.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password row with toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Password: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (passwordVisible) user.password else "••••••••",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Permission tier & notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permission: ${
                        when (user.permissionTier) {
                            "IMAGES_ONLY" -> "Images Only"
                            "PDFS_ONLY" -> "PDFs Only"
                            else -> "All Files"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_user_${user.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_user_${user.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (user.notes.isNotBlank()) {
                Text(
                    text = user.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// TAB 3: Vault Content Manager ("files I uploaded")
@Composable
private fun VaultFilesContent(
    items: List<VaultItem>,
    onUploadClick: () -> Unit,
    onPreview: (VaultItem) -> Unit,
    onDelete: (VaultItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vault Repository Assets",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Upload images and PDFs for authorized visitors to download.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onUploadClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_upload_new_file")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload")
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No files uploaded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap 'Upload' to add real images or PDFs from your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                AdminVaultItemCard(
                    item = item,
                    onPreview = { onPreview(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun AdminVaultItemCard(
    item: VaultItem,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val isPdf = item.fileType == "PDF"
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.createdAt))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isPdf) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                    contentDescription = null,
                    tint = if (isPdf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = FileUtils.formatFileSize(item.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onPreview) {
                Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// TAB 4: Admin Settings
@Composable
private fun AdminSettingsContent(
    metrics: DashboardMetrics,
    onUpdatePasscode: (String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onLogout: () -> Unit
) {
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf<String?>(null) }
    var passcodeSuccess by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Administrator Settings",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Change Admin Passcode Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Master Admin Passcode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Update the passcode required to enter this administrator control center.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPasscode,
                    onValueChange = { newPasscode = it; passcodeError = null; passcodeSuccess = false },
                    label = { Text("New Admin Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = { confirmPasscode = it; passcodeError = null; passcodeSuccess = false },
                    label = { Text("Confirm New Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (passcodeError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = passcodeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (passcodeSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Admin passcode updated successfully!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newPasscode.isBlank()) {
                            passcodeError = "Passcode cannot be blank."
                        } else if (newPasscode != confirmPasscode) {
                            passcodeError = "Passcodes do not match."
                        } else {
                            onUpdatePasscode(newPasscode, {
                                passcodeSuccess = true
                                newPasscode = ""
                                confirmPasscode = ""
                            }, { err ->
                                passcodeError = err
                            })
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Passcode")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Diagnostic Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Vault Integrity Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Authorized Users", style = MaterialTheme.typography.bodySmall)
                    Text("${metrics.totalAuthorizedUsers}", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Files in Protected Vault", style = MaterialTheme.typography.bodySmall)
                    Text("${metrics.totalVaultFiles}", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recorded Download Logs", style = MaterialTheme.typography.bodySmall)
                    Text("${metrics.totalDownloads}", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out Administrator")
        }
    }
}

// DIALOG: Add / Edit Authorized User
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormDialog(
    title: String,
    initialUser: AuthorizedUser?,
    onDismiss: () -> Unit,
    onConfirm: (username: String, pass: String, name: String, tier: String, notes: String, active: Boolean, onError: (String) -> Unit) -> Unit
) {
    var username by remember { mutableStateOf(initialUser?.username ?: "") }
    var password by remember { mutableStateOf(initialUser?.password ?: "") }
    var displayName by remember { mutableStateOf(initialUser?.displayName ?: "") }
    var permissionTier by remember { mutableStateOf(initialUser?.permissionTier ?: "ALL") }
    var notes by remember { mutableStateOf(initialUser?.notes ?: "") }
    var isActive by remember { mutableStateOf(initialUser?.isActive ?: true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var tierDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorText = null },
                    label = { Text("Username (Unique ID)") },
                    placeholder = { Text("e.g. client_alex") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_username_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = null },
                    label = { Text("Password") },
                    placeholder = { Text("Enter user password") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Alex Johnson") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Tier selector
                ExposedDropdownMenuBox(
                    expanded = tierDropdownExpanded,
                    onExpandedChange = { tierDropdownExpanded = !tierDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = when (permissionTier) {
                            "IMAGES_ONLY" -> "Images Only"
                            "PDFS_ONLY" -> "PDFs Only"
                            else -> "All Files (Images & PDFs)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Permission Tier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tierDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = tierDropdownExpanded,
                        onDismissRequest = { tierDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Files (Images & PDFs)") },
                            onClick = {
                                permissionTier = "ALL"
                                tierDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Images Only") },
                            onClick = {
                                permissionTier = "IMAGES_ONLY"
                                tierDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("PDFs Only") },
                            onClick = {
                                permissionTier = "PDFS_ONLY"
                                tierDropdownExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Admin Notes (Optional)") },
                    placeholder = { Text("e.g. Marketing Lead") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Active", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(username, password, displayName, permissionTier, notes, isActive) { err ->
                        errorText = err
                    }
                },
                modifier = Modifier.testTag("dialog_user_confirm")
            ) {
                Text("Save to List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// DIALOG: Upload Options (Photo Picker vs PDF Document Picker vs Generator)
@Composable
private fun UploadOptionDialog(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onPickPdf: () -> Unit,
    onCreateSample: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload to Protected Vault", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select file type to upload from your device storage:")

                Button(
                    onClick = onPickImage,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_pick_image")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Image (Photo Picker)")
                }

                Button(
                    onClick = onPickPdf,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_pick_pdf")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload PDF (Document Picker)")
                }

                OutlinedButton(
                    onClick = onCreateSample,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_create_sample")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Custom Vault Document/Image")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// DIALOG: Create Custom Vault Document or Graphic
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSampleFileDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, type: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("PDF") }
    var category by remember { mutableStateOf("Financial") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Vault Document / Graphic", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document / Image Title") },
                    placeholder = { Text("e.g. Q4 Audit Summary") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("File Type:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = fileType == "PDF",
                        onClick = { fileType = "PDF" },
                        label = { Text("PDF Document") }
                    )
                    FilterChip(
                        selected = fileType == "IMAGE",
                        onClick = { fileType = "IMAGE" },
                        label = { Text("Image Graphic") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("Financial, Security, Legal, Design") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, fileType, category.ifBlank { "General" })
                    }
                }
            ) {
                Text("Generate & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
