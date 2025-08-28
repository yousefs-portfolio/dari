package code.yousef.dari.shared.ui.platform

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

/**
 * Android-specific file picker implementation
 */
@Composable
actual fun FilePicker(
    fileTypes: List<String>,
    allowMultipleSelection: Boolean,
    onFilesSelected: (List<String>) -> Unit,
    onCancelled: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = if (allowMultipleSelection) {
            ActivityResultContracts.GetMultipleContents()
        } else {
            ActivityResultContracts.GetContent()
        }
    ) { result ->
        when (result) {
            is List<*> -> {
                val uris = result.filterIsInstance<Uri>()
                if (uris.isNotEmpty()) {
                    onFilesSelected(uris.map { it.toString() })
                } else {
                    onCancelled()
                }
            }
            is Uri -> {
                onFilesSelected(listOf(result.toString()))
            }
            null -> {
                onCancelled()
            }
            else -> {
                onCancelled()
            }
        }
    }
    
    LaunchedEffect(fileTypes, allowMultipleSelection) {
        val mimeType = when {
            fileTypes.isEmpty() -> "*/*"
            fileTypes.size == 1 -> fileTypes.first()
            else -> "*/*" // For multiple types, we'll use generic and filter later
        }
        
        launcher.launch(mimeType)
    }
}

/**
 * Android-specific image picker
 */
@Composable
actual fun ImagePicker(
    allowMultipleSelection: Boolean,
    onImagesSelected: (List<String>) -> Unit,
    onCancelled: () -> Unit
) {
    FilePicker(
        fileTypes = listOf("image/*"),
        allowMultipleSelection = allowMultipleSelection,
        onFilesSelected = onImagesSelected,
        onCancelled = onCancelled
    )
}

/**
 * Android-specific camera capture
 */
@Composable
actual fun CameraCapture(
    onImageCaptured: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // TODO: Save bitmap to file and return path
            // For now, return a placeholder
            onImageCaptured("camera_capture_${System.currentTimeMillis()}")
        } else {
            onCancelled()
        }
    }
    
    LaunchedEffect(Unit) {
        launcher.launch(null)
    }
}

/**
 * Android document picker with specific intent
 */
@Composable
fun AndroidDocumentPicker(
    mimeTypes: Array<String>,
    allowMultiple: Boolean = false,
    onDocumentsSelected: (List<Uri>) -> Unit,
    onCancelled: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onDocumentsSelected(uris)
        } else {
            onCancelled()
        }
    }
    
    LaunchedEffect(mimeTypes) {
        launcher.launch(mimeTypes)
    }
}

/**
 * Android receipt scanner with camera and gallery options
 */
@Composable
fun AndroidReceiptScanner(
    onReceiptSelected: (String) -> Unit,
    onCancelled: () -> Unit
) {
    var showOptions by remember { mutableStateOf(true) }
    
    if (showOptions) {
        // TODO: Show options dialog for camera vs gallery
        // For now, default to gallery
        showOptions = false
        ImagePicker(
            allowMultipleSelection = false,
            onImagesSelected = { images ->
                images.firstOrNull()?.let { onReceiptSelected(it) }
            },
            onCancelled = onCancelled
        )
    }
}

/**
 * Android bank statement importer
 */
@Composable
fun AndroidBankStatementImporter(
    onStatementsSelected: (List<String>) -> Unit,
    onCancelled: () -> Unit
) {
    FilePicker(
        fileTypes = listOf("application/pdf", "text/csv", "application/vnd.ms-excel"),
        allowMultipleSelection = true,
        onFilesSelected = onStatementsSelected,
        onCancelled = onCancelled
    )
}