package com.dari.finance.receipt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Receipt preview gallery for viewing and managing receipt images
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceiptPreviewGallery(
    receipts: List<ReceiptWithImage>,
    selectedReceipts: Set<String>,
    onReceiptSelect: (String) -> Unit,
    onReceiptClick: (ReceiptWithImage) -> Unit,
    onDeleteReceipts: (List<String>) -> Unit,
    onShareReceipts: (List<String>) -> Unit,
    onBulkProcess: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(GalleryViewMode.GRID) }
    var showSelectionMode by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(ReceiptSortBy.DATE_DESC) }
    var filterBy by remember { mutableStateOf(ReceiptFilter.ALL) }
    
    val sortedAndFilteredReceipts = remember(receipts, sortBy, filterBy) {
        receipts
            .filter { receipt ->
                when (filterBy) {
                    ReceiptFilter.ALL -> true
                    ReceiptFilter.PROCESSED -> receipt.processedReceipt != null
                    ReceiptFilter.UNPROCESSED -> receipt.processedReceipt == null
                    ReceiptFilter.HIGH_CONFIDENCE -> receipt.processedReceipt?.confidence ?: 0f > 0.8f
                    ReceiptFilter.LOW_CONFIDENCE -> receipt.processedReceipt?.confidence ?: 0f <= 0.8f
                }
            }
            .sortedWith { a, b ->
                when (sortBy) {
                    ReceiptSortBy.DATE_DESC -> b.metadata.capturedAt.compareTo(a.metadata.capturedAt)
                    ReceiptSortBy.DATE_ASC -> a.metadata.capturedAt.compareTo(b.metadata.capturedAt)
                    ReceiptSortBy.AMOUNT_DESC -> (b.processedReceipt?.totalAmount ?: 0.0).compareTo(a.processedReceipt?.totalAmount ?: 0.0)
                    ReceiptSortBy.AMOUNT_ASC -> (a.processedReceipt?.totalAmount ?: 0.0).compareTo(b.processedReceipt?.totalAmount ?: 0.0)
                    ReceiptSortBy.MERCHANT -> (a.processedReceipt?.merchantName ?: "").compareTo(b.processedReceipt?.merchantName ?: "")
                    ReceiptSortBy.CONFIDENCE_DESC -> (b.processedReceipt?.confidence ?: 0f).compareTo(a.processedReceipt?.confidence ?: 0f)
                }
            }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Gallery header with controls
        GalleryHeader(
            totalReceipts = receipts.size,
            filteredReceipts = sortedAndFilteredReceipts.size,
            selectedCount = selectedReceipts.size,
            viewMode = viewMode,
            sortBy = sortBy,
            filterBy = filterBy,
            showSelectionMode = showSelectionMode,
            onViewModeChange = { viewMode = it },
            onSortChange = { sortBy = it },
            onFilterChange = { filterBy = it },
            onSelectionModeToggle = { 
                showSelectionMode = it
                if (!it) {
                    // Clear selections when exiting selection mode
                    selectedReceipts.forEach { onReceiptSelect(it) }
                }
            }
        )
        
        // Selection actions bar
        if (showSelectionMode && selectedReceipts.isNotEmpty()) {
            SelectionActionsBar(
                selectedCount = selectedReceipts.size,
                onDelete = { onDeleteReceipts(selectedReceipts.toList()) },
                onShare = { onShareReceipts(selectedReceipts.toList()) },
                onBulkProcess = { onBulkProcess(selectedReceipts.toList()) }
            )
        }
        
        // Receipt grid/list
        when (viewMode) {
            GalleryViewMode.GRID -> {
                ReceiptGrid(
                    receipts = sortedAndFilteredReceipts,
                    selectedReceipts = selectedReceipts,
                    showSelectionMode = showSelectionMode,
                    onReceiptSelect = onReceiptSelect,
                    onReceiptClick = onReceiptClick,
                    modifier = Modifier.weight(1f)
                )
            }
            GalleryViewMode.LIST -> {
                ReceiptList(
                    receipts = sortedAndFilteredReceipts,
                    selectedReceipts = selectedReceipts,
                    showSelectionMode = showSelectionMode,
                    onReceiptSelect = onReceiptSelect,
                    onReceiptClick = onReceiptClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GalleryHeader(
    totalReceipts: Int,
    filteredReceipts: Int,
    selectedCount: Int,
    viewMode: GalleryViewMode,
    sortBy: ReceiptSortBy,
    filterBy: ReceiptFilter,
    showSelectionMode: Boolean,
    onViewModeChange: (GalleryViewMode) -> Unit,
    onSortChange: (ReceiptSortBy) -> Unit,
    onFilterChange: (ReceiptFilter) -> Unit,
    onSelectionModeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                        text = if (showSelectionMode && selectedCount > 0) {
                            "$selectedCount selected"
                        } else {
                            "Receipts ($filteredReceipts)"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (filteredReceipts != totalReceipts) {
                        Text(
                            text = "$filteredReceipts of $totalReceipts receipts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Selection mode toggle
                    IconButton(
                        onClick = { onSelectionModeToggle(!showSelectionMode) }
                    ) {
                        Icon(
                            imageVector = if (showSelectionMode) Icons.Default.Close else Icons.Default.Checklist,
                            contentDescription = if (showSelectionMode) "Exit selection" else "Select mode",
                            tint = if (showSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // View mode toggle
                    IconButton(
                        onClick = {
                            onViewModeChange(
                                if (viewMode == GalleryViewMode.GRID) GalleryViewMode.LIST else GalleryViewMode.GRID
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == GalleryViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view mode"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Filter and sort chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sort chip
                FilterChip(
                    onClick = { /* Show sort options */ },
                    label = { Text(sortBy.displayName, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    selected = false
                )
                
                // Filter chip
                FilterChip(
                    onClick = { /* Show filter options */ },
                    label = { Text(filterBy.displayName, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    selected = filterBy != ReceiptFilter.ALL
                )
            }
        }
    }
}

@Composable
private fun SelectionActionsBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onBulkProcess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = onBulkProcess
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Process")
            }
            
            TextButton(
                onClick = onShare
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share")
            }
            
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

@Composable
private fun ReceiptGrid(
    receipts: List<ReceiptWithImage>,
    selectedReceipts: Set<String>,
    showSelectionMode: Boolean,
    onReceiptSelect: (String) -> Unit,
    onReceiptClick: (ReceiptWithImage) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(receipts) { receipt ->
            ReceiptGridItem(
                receipt = receipt,
                isSelected = selectedReceipts.contains(receipt.metadata.id),
                showSelectionMode = showSelectionMode,
                onSelect = { onReceiptSelect(receipt.metadata.id) },
                onClick = { onReceiptClick(receipt) }
            )
        }
    }
}

@Composable
private fun ReceiptList(
    receipts: List<ReceiptWithImage>,
    selectedReceipts: Set<String>,
    showSelectionMode: Boolean,
    onReceiptSelect: (String) -> Unit,
    onReceiptClick: (ReceiptWithImage) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(receipts) { receipt ->
            ReceiptListItem(
                receipt = receipt,
                isSelected = selectedReceipts.contains(receipt.metadata.id),
                showSelectionMode = showSelectionMode,
                onSelect = { onReceiptSelect(receipt.metadata.id) },
                onClick = { onReceiptClick(receipt) }
            )
        }
    }
}

@Composable
private fun ReceiptGridItem(
    receipt: ReceiptWithImage,
    isSelected: Boolean,
    showSelectionMode: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .clickable {
                if (showSelectionMode) {
                    onSelect()
                } else {
                    onClick()
                }
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Receipt image placeholder
            ReceiptImagePlaceholder(
                receipt = receipt,
                modifier = Modifier.fillMaxSize()
            )
            
            // Selection checkbox
            if (showSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                )
            }
            
            // Receipt info overlay
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = receipt.processedReceipt?.merchantName ?: "Unknown",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = receipt.processedReceipt?.totalAmount?.toString()?.let { "$$it" } ?: "No amount",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    if (receipt.processedReceipt != null) {
                        ConfidenceBar(
                            confidence = receipt.processedReceipt.confidence,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptListItem(
    receipt: ReceiptWithImage,
    isSelected: Boolean,
    showSelectionMode: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (showSelectionMode) {
                    onSelect()
                } else {
                    onClick()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            ReceiptImagePlaceholder(
                receipt = receipt,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Receipt details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = receipt.processedReceipt?.merchantName ?: "Unknown Merchant",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = receipt.processedReceipt?.totalAmount?.toString()?.let { "$$it" } ?: "Amount not detected",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Text(
                    text = formatDate(receipt.metadata.capturedAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                if (receipt.processedReceipt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ConfidenceBar(
                        confidence = receipt.processedReceipt.confidence,
                        modifier = Modifier
                            .width(100.dp)
                            .height(3.dp)
                    )
                }
            }
            
            // Selection checkbox or status indicator
            if (showSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() }
                )
            } else {
                ProcessingStatusIndicator(
                    status = receipt.metadata.processingStatus
                )
            }
        }
    }
}

@Composable
private fun ReceiptImagePlaceholder(
    receipt: ReceiptWithImage,
    modifier: Modifier = Modifier
) {
    // In a real implementation, this would load and display the actual receipt image
    // For now, we'll show a placeholder
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(8.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = "Receipt",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfidenceBar(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val confidenceColor = when {
        confidence > 0.8f -> Color.Green
        confidence > 0.6f -> Color.Orange
        else -> Color.Red
    }
    
    Box(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(confidence)
                .background(confidenceColor, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun ProcessingStatusIndicator(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        ProcessingStatus.PENDING -> Icons.Default.HourglassEmpty to Color.Gray
        ProcessingStatus.PROCESSING -> Icons.Default.AutoAwesome to Color.Blue
        ProcessingStatus.COMPLETED -> Icons.Default.CheckCircle to Color.Green
        ProcessingStatus.FAILED -> Icons.Default.Error to Color.Red
        ProcessingStatus.REQUIRES_MANUAL_REVIEW -> Icons.Default.Warning to Color.Orange
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = color,
        modifier = modifier.size(20.dp)
    )
}

private fun formatDate(timestamp: Long): String {
    // Simplified date formatting - in real implementation would use proper date formatter
    return java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(timestamp))
}

// Data classes and enums
data class ReceiptWithImage(
    val metadata: ReceiptMetadata,
    val imageBytes: ByteArray,
    val thumbnailBytes: ByteArray,
    val processedReceipt: ProcessedReceipt?
)

enum class GalleryViewMode {
    GRID, LIST
}

enum class ReceiptSortBy(val displayName: String) {
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    AMOUNT_DESC("Amount (High)"),
    AMOUNT_ASC("Amount (Low)"),
    MERCHANT("Merchant"),
    CONFIDENCE_DESC("Confidence")
}

enum class ReceiptFilter(val displayName: String) {
    ALL("All"),
    PROCESSED("Processed"),
    UNPROCESSED("Unprocessed"),
    HIGH_CONFIDENCE("High Confidence"),
    LOW_CONFIDENCE("Low Confidence")
}