package code.yousef.dari.shared.ui.platform

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android-specific share sheet implementation using Intent.ACTION_SEND
 */
@Composable
actual fun ShareSheet(
    content: ShareContent,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(content) {
        try {
            val intent = when (content) {
                is ShareContent.Text -> createTextShareIntent(content)
                is ShareContent.File -> createFileShareIntent(context, content)
                is ShareContent.Image -> createImageShareIntent(context, content)
                is ShareContent.Url -> createUrlShareIntent(content)
                is ShareContent.Multiple -> createMultipleShareIntent(context, content)
            }
            
            val shareIntent = Intent.createChooser(intent, "Share via")
            context.startActivity(shareIntent)
            onShared()
        } catch (e: Exception) {
            onCancelled()
        }
    }
}

private fun createTextShareIntent(content: ShareContent.Text): Intent {
    return Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content.text)
        content.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
    }
}

private fun createFileShareIntent(context: android.content.Context, content: ShareContent.File): Intent {
    val file = File(content.filePath)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    
    return Intent().apply {
        action = Intent.ACTION_SEND
        type = content.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        content.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun createImageShareIntent(context: android.content.Context, content: ShareContent.Image): Intent {
    val file = File(content.imagePath)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    
    return Intent().apply {
        action = Intent.ACTION_SEND
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        content.caption?.let { putExtra(Intent.EXTRA_TEXT, it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun createUrlShareIntent(content: ShareContent.Url): Intent {
    return Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content.url)
        content.title?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
    }
}

private fun createMultipleShareIntent(context: android.content.Context, content: ShareContent.Multiple): Intent {
    val uris = mutableListOf<Uri>()
    var textContent = ""
    
    content.items.forEach { item ->
        when (item) {
            is ShareContent.File -> {
                val file = File(item.filePath)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                uris.add(uri)
            }
            is ShareContent.Image -> {
                val file = File(item.imagePath)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                uris.add(uri)
            }
            is ShareContent.Text -> {
                textContent += "${item.text}\n\n"
            }
            is ShareContent.Url -> {
                textContent += "${item.url}\n"
            }
            else -> {} // Handle nested Multiple if needed
        }
    }
    
    return Intent().apply {
        if (uris.isNotEmpty() && textContent.isNotEmpty()) {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_TEXT, textContent.trim())
        } else if (uris.isNotEmpty()) {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        } else {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textContent.trim())
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/**
 * Android-specific financial report sharing with email pre-configuration
 */
@Composable
fun AndroidShareFinancialReportToEmail(
    reportPath: String,
    recipientEmail: String,
    subject: String,
    messageBody: String,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(reportPath) {
        try {
            val file = File(reportPath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val emailIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/pdf"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, messageBody)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(emailIntent, "Send via Email")
            context.startActivity(chooserIntent)
            onShared()
        } catch (e: Exception) {
            onCancelled()
        }
    }
}

/**
 * Android-specific backup sharing to cloud services
 */
@Composable
fun AndroidShareBackupToCloud(
    backupFilePath: String,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(backupFilePath) {
        try {
            val file = File(backupFilePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val cloudIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Dari Finance Backup")
                putExtra(Intent.EXTRA_TEXT, "Financial data backup from Dari Finance app")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                // Specify cloud storage apps
                setPackage("com.google.android.apps.docs") // Google Drive
                // or setPackage("com.dropbox.android") // Dropbox
                // or setPackage("com.microsoft.office.outlook") // OneDrive
            }
            
            try {
                context.startActivity(cloudIntent)
            } catch (e: Exception) {
                // If specific app not found, show all options
                val genericIntent = Intent.createChooser(
                    cloudIntent.apply { setPackage(null) },
                    "Save Backup to Cloud"
                )
                context.startActivity(genericIntent)
            }
            
            onShared()
        } catch (e: Exception) {
            onCancelled()
        }
    }
}

/**
 * Android-specific receipt sharing with OCR data
 */
@Composable
fun AndroidShareReceiptWithData(
    receiptImagePath: String,
    extractedData: String,
    onShared: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(receiptImagePath, extractedData) {
        try {
            val file = File(receiptImagePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Receipt Data:\n$extractedData")
                putExtra(Intent.EXTRA_SUBJECT, "Receipt from Dari Finance")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Share Receipt")
            context.startActivity(chooserIntent)
            onShared()
        } catch (e: Exception) {
            onCancelled()
        }
    }
}