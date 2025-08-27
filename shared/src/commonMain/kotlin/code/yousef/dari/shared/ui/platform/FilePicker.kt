package code.yousef.dari.shared.ui.platform

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic file picker interface
 */
@Composable
expect fun FilePicker(
    fileTypes: List<String> = emptyList(),
    allowMultipleSelection: Boolean = false,
    onFilesSelected: (List<String>) -> Unit,
    onCancelled: () -> Unit
)

/**
 * Platform-agnostic image picker
 */
@Composable
expect fun ImagePicker(
    allowMultipleSelection: Boolean = false,
    onImagesSelected: (List<String>) -> Unit,
    onCancelled: () -> Unit
)

/**
 * Platform-agnostic camera capture
 */
@Composable
expect fun CameraCapture(
    onImageCaptured: (String) -> Unit,
    onCancelled: () -> Unit
)

/**
 * Common file types for financial apps
 */
object FinancialFileTypes {
    const val PDF = "application/pdf"
    const val IMAGE_JPEG = "image/jpeg"
    const val IMAGE_PNG = "image/png"
    const val CSV = "text/csv"
    const val EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val JSON = "application/json"
    
    val RECEIPTS = listOf(IMAGE_JPEG, IMAGE_PNG, PDF)
    val DOCUMENTS = listOf(PDF, CSV, EXCEL)
    val IMAGES = listOf(IMAGE_JPEG, IMAGE_PNG)
    val EXPORTS = listOf(PDF, CSV, EXCEL, JSON)
}

/**
 * Receipt scanner with file picker
 */
@Composable
fun ReceiptFilePicker(
    onReceiptSelected: (String) -> Unit,
    onCancelled: () -> Unit
) {
    FilePicker(
        fileTypes = FinancialFileTypes.RECEIPTS,
        allowMultipleSelection = false,
        onFilesSelected = { files ->
            files.firstOrNull()?.let { onReceiptSelected(it) }
        },
        onCancelled = onCancelled
    )
}

/**
 * Document import for financial statements
 */
@Composable
fun FinancialDocumentPicker(
    allowMultiple: Boolean = false,
    onDocumentsSelected: (List<String>) -> Unit,
    onCancelled: () -> Unit
) {
    FilePicker(
        fileTypes = FinancialFileTypes.DOCUMENTS,
        allowMultipleSelection = allowMultiple,
        onFilesSelected = onDocumentsSelected,
        onCancelled = onCancelled
    )
}