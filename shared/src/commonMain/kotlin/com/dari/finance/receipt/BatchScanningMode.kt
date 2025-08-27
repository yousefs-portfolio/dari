package com.dari.finance.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Batch scanning mode for capturing multiple receipts quickly
 */
@Composable
fun BatchScanningScreen(
    scanningState: BatchScanningState,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
    onCaptureReceipt: () -> Unit,
    onProcessBatch: (List<CapturedReceipt>) -> Unit,
    onClearBatch: () -> Unit,
    onRetakeReceipt: (String) -> Unit,
    onDeleteReceipt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Batch scanning header
        BatchScanningHeader(
            state = scanningState,
            onStartScanning = onStartScanning,
            onStopScanning = onStopScanning,
            onProcessBatch = { onProcessBatch(scanningState.capturedReceipts) },
            onClearBatch = onClearBatch,
            modifier = Modifier.fillMaxWidth()
        )
        
        when {
            scanningState.isScanning -> {
                // Active scanning interface
                ActiveScanningInterface(
                    capturedCount = scanningState.capturedReceipts.size,
                    onCaptureReceipt = onCaptureReceipt,
                    modifier = Modifier.weight(1f)
                )
            }
            
            scanningState.capturedReceipts.isNotEmpty() -> {
                // Review captured receipts
                CapturedReceiptsReview(
                    receipts = scanningState.capturedReceipts,
                    onRetakeReceipt = onRetakeReceipt,
                    onDeleteReceipt = onDeleteReceipt,
                    modifier = Modifier.weight(1f)
                )
            }
            
            else -> {
                // Empty state
                BatchScanningEmptyState(
                    onStartScanning = onStartScanning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BatchScanningHeader(
    state: BatchScanningState,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit,
    onProcessBatch: () -> Unit,
    onClearBatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Batch Scanning",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = when {
                            state.isScanning -> "Scanning active - Tap to capture"
                            state.capturedReceipts.isNotEmpty() -> "${state.capturedReceipts.size} receipts captured"
                            else -> "Capture multiple receipts quickly"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isScanning) {
                        OutlinedButton(
                            onClick = onStopScanning,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop")
                        }
                    } else {
                        if (state.capturedReceipts.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onClearBatch,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Clear")
                            }
                            
                            Button(onClick = onProcessBatch) {
                                Text("Process All")
                            }
                        } else {
                            Button(onClick = onStartScanning) {
                                Text("Start Scanning")
                            }
                        }
                    }
                }
            }
            
            // Progress indicator
            if (state.capturedReceipts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                BatchProgressIndicator(
                    capturedCount = state.capturedReceipts.size,
                    processedCount = state.capturedReceipts.count { it.isProcessed },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ActiveScanningInterface(
    capturedCount: Int,
    onCaptureReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Capture count
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "$capturedCount receipts captured",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Capture instructions
        Text(
            text = "Position receipt in frame and tap the capture button",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Large capture button
        Button(
            onClick = onCaptureReceipt,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture Receipt",
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick tips
        BatchScanningTips()
    }
}

@Composable
private fun CapturedReceiptsReview(
    receipts: List<CapturedReceipt>,
    onRetakeReceipt: (String) -> Unit,
    onDeleteReceipt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(receipts) { receipt ->
            CapturedReceiptCard(
                receipt = receipt,
                onRetake = { onRetakeReceipt(receipt.id) },
                onDelete = { onDeleteReceipt(receipt.id) }
            )
        }
        
        item {
            BatchScanningTips()
        }
    }
}

@Composable
private fun CapturedReceiptCard(
    receipt: CapturedReceipt,
    onRetake: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Receipt thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Receipt info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Receipt ${receipts.indexOf(receipt) + 1}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = formatCaptureTime(receipt.capturedAt),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Processing status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        receipt.isProcessed -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Processed",
                                fontSize = 12.sp,
                                color = Color.Green
                            )
                        }
                        
                        receipt.processingFailed -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Failed",
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }
                        
                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Processing...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onRetake) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Retake",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchScanningEmptyState(
    onStartScanning: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Batch Receipt Scanning",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Quickly capture multiple receipts in one session",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onStartScanning,
            modifier = Modifier.size(width = 200.dp, height = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Scanning")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        BatchScanningTips()
    }
}

@Composable
private fun BatchProgressIndicator(
    capturedCount: Int,
    processedCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Progress",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "$processedCount / $capturedCount processed",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = if (capturedCount > 0) processedCount.toFloat() / capturedCount.toFloat() else 0f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BatchScanningTips(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Tips for best results",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val tips = listOf(
                "Ensure good lighting for clearer text",
                "Keep receipts flat and aligned",
                "Capture one receipt at a time",
                "Check each capture before moving to the next"
            )
            
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )
                    
                    Text(
                        text = tip,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun formatCaptureTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        else -> java.text.SimpleDateFormat("HH:mm").format(java.util.Date(timestamp))
    }
}

// State management for batch scanning
data class BatchScanningState(
    val isScanning: Boolean = false,
    val capturedReceipts: List<CapturedReceipt> = emptyList(),
    val isProcessingBatch: Boolean = false,
    val processingProgress: Float = 0.0f
)

data class CapturedReceipt(
    val id: String,
    val imageBytes: ByteArray,
    val capturedAt: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false,
    val processingFailed: Boolean = false,
    val processedResult: ProcessedReceipt? = null
)

/**
 * ViewModel for managing batch scanning state
 */
class BatchScanningViewModel {
    private var _state = MutableState(BatchScanningState())
    val state: State<BatchScanningState> = _state
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    fun startScanning() {
        _state.value = _state.value.copy(isScanning = true)
    }
    
    fun stopScanning() {
        _state.value = _state.value.copy(isScanning = false)
    }
    
    fun addCapturedReceipt(imageBytes: ByteArray) {
        val newReceipt = CapturedReceipt(
            id = "receipt_${System.currentTimeMillis()}",
            imageBytes = imageBytes
        )
        
        val updatedReceipts = _state.value.capturedReceipts + newReceipt
        _state.value = _state.value.copy(capturedReceipts = updatedReceipts)
    }
    
    fun removeReceipt(receiptId: String) {
        val updatedReceipts = _state.value.capturedReceipts.filter { it.id != receiptId }
        _state.value = _state.value.copy(capturedReceipts = updatedReceipts)
    }
    
    fun clearBatch() {
        _state.value = _state.value.copy(
            capturedReceipts = emptyList(),
            isScanning = false
        )
    }
    
    fun processBatch(
        receipts: List<CapturedReceipt>,
        onComplete: (List<ProcessedReceipt>) -> Unit
    ) {
        coroutineScope.launch {
            _state.value = _state.value.copy(isProcessingBatch = true)
            
            val processedResults = mutableListOf<ProcessedReceipt>()
            
            receipts.forEachIndexed { index, receipt ->
                try {
                    // Simulate OCR processing
                    delay(2000) // Simulated processing time
                    
                    val processedReceipt = ProcessedReceipt(
                        id = receipt.id,
                        merchantName = "Sample Merchant",
                        totalAmount = 25.99,
                        transactionDate = "2023-12-01",
                        items = listOf("Item 1", "Item 2"),
                        confidence = 0.85f
                    )
                    
                    processedResults.add(processedReceipt)
                    
                    // Update progress
                    val progress = (index + 1).toFloat() / receipts.size
                    _state.value = _state.value.copy(processingProgress = progress)
                    
                } catch (e: Exception) {
                    // Mark as failed
                    val updatedReceipts = _state.value.capturedReceipts.map { capturedReceipt ->
                        if (capturedReceipt.id == receipt.id) {
                            capturedReceipt.copy(processingFailed = true)
                        } else {
                            capturedReceipt
                        }
                    }
                    _state.value = _state.value.copy(capturedReceipts = updatedReceipts)
                }
            }
            
            _state.value = _state.value.copy(
                isProcessingBatch = false,
                processingProgress = 0.0f
            )
            
            onComplete(processedResults)
        }
    }
}