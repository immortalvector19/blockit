package com.blockit.ui.screens.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockit.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class BackupViewModel(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportBackup(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val jsonString = backupRepository.createBackupJson()
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Could not open output stream for selected file.")

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Backup exported successfully to local file.",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Export failed: ${e.localizedMessage ?: "Unknown error"}",
                    isError = true
                )
            }
        }
    }

    fun importBackup(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, message = null)
            try {
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                } ?: throw IllegalStateException("Could not open input stream from selected file.")

                val result = backupRepository.restoreBackupJson(jsonString)
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        message = "Backup restored successfully ($count profiles imported).",
                        isError = false
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Invalid backup file"
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        message = "Restore failed: $errorMsg",
                        isError = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = "Restore failed: ${e.localizedMessage ?: "Unknown error"}",
                    isError = true
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    class Factory(private val backupRepository: BackupRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupViewModel(backupRepository) as T
        }
    }
}
