package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FintechGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBackupLoading by remember { mutableStateOf(false) }
    var backupStatusText by remember { mutableStateOf("") }
    var lastOperationResult by remember { mutableStateOf("") }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importCsvFromUri(context, uri) { result ->
                lastOperationResult = result
            }
        }
    }

    val dbRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreSqliteDatabaseFromUri(context, uri) { result ->
                lastOperationResult = result
            }
        }
    }

    val authPrefs = remember { context.getSharedPreferences("finance_tracker_auth_prefs", Context.MODE_PRIVATE) }
    var userEmail by remember { mutableStateOf(authPrefs.getString("active_user_email", "guest@financetracker.local") ?: "guest@financetracker.local") }
    var userName by remember { mutableStateOf(authPrefs.getString("active_user_name", "Valued Member") ?: "Valued Member") }
    var authProvider by remember { mutableStateOf(authPrefs.getString("active_auth_provider", "EMAIL") ?: "EMAIL") }
    var is2FaEnabled by remember { mutableStateOf(authPrefs.getBoolean("2fa_${userEmail.lowercase().trim()}", false)) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("data_management_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 0: User Profile & Security Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("user_account_profile_card")
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "User Profile",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column {
                                Text(userName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Text(userEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = authProvider,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Two-Factor Authentication (2FA)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Require SMS / Authenticator OTP on login", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = is2FaEnabled,
                            onCheckedChange = { checked ->
                                is2FaEnabled = checked
                                authPrefs.edit().putBoolean("2fa_${userEmail.lowercase().trim()}", checked).apply()
                                Toast.makeText(context, if (checked) "2FA Enabled for $userEmail" else "2FA Disabled", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("toggle_2fa_switch")
                        )
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Data Management",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DATA MANAGEMENT & BACKUPS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Manage local Room database snapshots and file imports",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (lastOperationResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (lastOperationResult.startsWith("Success") || lastOperationResult.contains("restored successfully"))
                                    FintechGreen.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = lastOperationResult,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Database Backup & AES-256 Passphrase Encryption
        item {
            var backupPassphrase by remember { mutableStateOf("") }
            var restorePassphrase by remember { mutableStateOf("") }
            var selectedBackupToRestore by remember { mutableStateOf<com.example.data.database.DatabaseEncryptionBackupManager.BackupMetadata?>(null) }
            var backupsList by remember { mutableStateOf(viewModel.getEncryptedBackupsList()) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("encrypted_database_backup_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "AES-256 ENCRYPTED DATABASE BACKUP & RECOVERY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Encrypt your existing Room database with a custom passphrase and save an encrypted backup file (.enc) to local device storage for recovery.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Encryption Passphrase") },
                        placeholder = { Text("Enter strong passphrase (min 4 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_input")
                    )

                    if (isBackupLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(backupStatusText.ifEmpty { "Encrypting & saving backup..." }, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (backupPassphrase.length < 4) {
                                    lastOperationResult = "Error: Passphrase must be at least 4 characters long"
                                    return@Button
                                }
                                isBackupLoading = true
                                viewModel.exportEncryptedDatabaseBackup(backupPassphrase) { success, msg ->
                                    isBackupLoading = false
                                    lastOperationResult = if (success) "Encrypted backup created successfully: $msg" else "Error: $msg"
                                    backupsList = viewModel.getEncryptedBackupsList()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("create_encrypted_backup_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Encrypted Backup to Local Storage")
                        }
                    }

                    if (backupsList.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "LOCAL ENCRYPTED BACKUP FILES (${backupsList.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        backupsList.forEach { bk ->
                            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                            val dateStr = dateFormat.format(java.util.Date(bk.timestamp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bk.fileName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("$dateStr • ${bk.sizeBytes / 1024} KB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    OutlinedButton(
                                        onClick = { selectedBackupToRestore = bk },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("restore_file_btn_${bk.fileName}")
                                    ) {
                                        Text("Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedBackupToRestore != null) {
                val bk = selectedBackupToRestore!!
                AlertDialog(
                    onDismissRequest = { selectedBackupToRestore = null },
                    title = { Text("Restore Database Backup") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Decrypting file: ${bk.fileName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Enter the passphrase used when creating this backup:", fontSize = 11.sp)
                            OutlinedTextField(
                                value = restorePassphrase,
                                onValueChange = { restorePassphrase = it },
                                label = { Text("Passphrase") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("restore_passphrase_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val file = java.io.File(bk.filePath)
                                viewModel.restoreEncryptedDatabaseBackup(file, restorePassphrase) { success, msg ->
                                    lastOperationResult = msg
                                    selectedBackupToRestore = null
                                    restorePassphrase = ""
                                }
                            },
                            modifier = Modifier.testTag("confirm_restore_btn")
                        ) {
                            Text("Decrypt & Restore")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedBackupToRestore = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        // Section 2: CSV Data Migration & Parsing Utility
        item {
            var rawCsvInput by remember { mutableStateOf("") }
            var parseResultMsg by remember { mutableStateOf("") }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("csv_data_migration_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CSV FINANCIAL DATA MIGRATION & IMPORT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Migrate transaction data from external banking apps or spreadsheets directly into your Room SQLite database.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                rawCsvInput = """Date,Amount,Merchant,Category,Notes
2026-07-01,45.50,Whole Foods,Groceries,Weekly grocery run
2026-07-03,12.00,Starbucks,Coffee,Latte & muffin
2026-07-10,85.00,Shell Station,Transport,Gas refill
2026-07-15,120.00,Electric Utility,Bills,Monthly power bill"""
                            }
                        ) {
                            Text("Insert Sample CSV Template", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { csvImportLauncher.launch("text/*") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick File", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = rawCsvInput,
                        onValueChange = { rawCsvInput = it },
                        label = { Text("CSV Text Data (Paste or Pick)") },
                        placeholder = { Text("Date,Amount,Merchant,Category,Notes") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("csv_raw_text_input")
                    )

                    if (parseResultMsg.isNotEmpty()) {
                        Surface(
                            color = if (parseResultMsg.startsWith("Error") || parseResultMsg.contains("empty")) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = parseResultMsg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (rawCsvInput.isNotBlank()) {
                                viewModel.importExpensesFromCsv(rawCsvInput) { success, count, msg ->
                                    parseResultMsg = msg
                                    if (success) {
                                        rawCsvInput = ""
                                    }
                                }
                            } else {
                                parseResultMsg = "Please enter or paste CSV text first."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_csv_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parse & Import CSV to Database")
                    }
                }
            }
        }


        // Section 3: CSV Report Export
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EXPORT TRANSACTIONS TO CSV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Download/share formatted CSV files for accounting, tax reporting, or external spreadsheet analysis.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.exportReportToCsv(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Transaction History (CSV)")
                    }
                }
            }
        }
    }
}
