package com.blockit.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.blockit.ui.components.FocusAlertDialog
import com.blockit.ui.components.FocusButton
import com.blockit.ui.components.FocusOutlinedButton
import com.blockit.ui.components.FocusTopBar
import com.blockit.ui.theme.FocusShapes
import com.blockit.ui.theme.FocusSpacing
import com.blockit.ui.theme.FocusTheme
import com.blockit.ui.theme.FocusTypography

@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = FocusTheme.colors

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it, context.contentResolver) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pendingRestoreUri = it }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FocusTopBar(
                title = "Backup and restore",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(FocusSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.lg)
        ) {
            // Local file storage info card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.divider, FocusShapes.medium),
                shape = FocusShapes.medium,
                color = colors.surface
            ) {
                Row(
                    modifier = Modifier.padding(FocusSpacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(FocusSpacing.md))
                    Column {
                        Text(
                            text = "Local storage files",
                            style = FocusTypography.bodyBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.xs))
                        Text(
                            text = "Backups are exported directly as standard JSON files to your device storage. They never leave your phone.",
                            style = FocusTypography.caption,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Export Section Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.divider, FocusShapes.medium),
                shape = FocusShapes.medium,
                color = colors.surface
            ) {
                Column(modifier = Modifier.padding(FocusSpacing.md)) {
                    Text(
                        text = "Export settings",
                        style = FocusTypography.bodyBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.xs))
                    Text(
                        text = "Saves all blocking profiles, targeted app packages, website domains, recurring routines, and strict mode preferences into a JSON file.",
                        style = FocusTypography.caption,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.md))

                    FocusButton(
                        text = if (uiState.isExporting) "Exporting..." else "Export to JSON file",
                        leadingIcon = if (!uiState.isExporting) Icons.Rounded.FileDownload else null,
                        onClick = {
                            val defaultFileName = "focusshield_backup_${System.currentTimeMillis() / 1000}.json"
                            exportLauncher.launch(defaultFileName)
                        },
                        enabled = !uiState.isExporting && !uiState.isImporting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Restore Section Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.divider, FocusShapes.medium),
                shape = FocusShapes.medium,
                color = colors.surface
            ) {
                Column(modifier = Modifier.padding(FocusSpacing.md)) {
                    Text(
                        text = "Restore settings",
                        style = FocusTypography.bodyBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.xs))
                    Text(
                        text = "Load a previously exported FocusShield JSON backup. This replaces existing profiles with the backup contents and re-arms schedule alarms.",
                        style = FocusTypography.caption,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(FocusSpacing.md))

                    FocusOutlinedButton(
                        text = if (uiState.isImporting) "Restoring..." else "Restore from JSON file",
                        leadingIcon = if (!uiState.isImporting) Icons.Rounded.FileUpload else null,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        enabled = !uiState.isExporting && !uiState.isImporting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Privacy note
            Row(
                modifier = Modifier.padding(horizontal = FocusSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(FocusSpacing.sm))
                Text(
                    text = "Usage statistics are kept strictly in local logs and are not included in rule exports.",
                    style = FocusTypography.caption,
                    color = colors.textSecondary
                )
            }
        }
    }

    // Confirmation dialog before restore
    if (pendingRestoreUri != null) {
        FocusAlertDialog(
            title = "Restore backup?",
            text = "Restoring will replace all current profiles, blocked apps, domains, and schedules with the data in the selected backup. This cannot be undone.",
            confirmText = "Overwrite and restore",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                val uri = pendingRestoreUri!!
                pendingRestoreUri = null
                viewModel.importBackup(uri, context.contentResolver)
            },
            onDismiss = { pendingRestoreUri = null }
        )
    }
}
