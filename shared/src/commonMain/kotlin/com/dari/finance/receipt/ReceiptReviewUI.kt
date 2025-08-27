package com.dari.finance.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Receipt review screen for verifying OCR results
 */
@Composable
fun ReceiptReviewScreen(
    receipt: ProcessedReceipt,
    onConfirm: (ProcessedReceipt) -> Unit,
    onEdit: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editedReceipt by remember { mutableStateOf(receipt) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with confidence indicator
        ReceiptReviewHeader(
            receipt = editedReceipt,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Receipt preview and details
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ReceiptDetailsSection(
                        receipt = editedReceipt,
                        onFieldEdit = { field, value ->
                            editedReceipt = when (field) {
                                "merchant" -> editedReceipt.copy(merchantName = value)
                                "amount" -> editedReceipt.copy(totalAmount = value.toDoubleOrNull() ?: editedReceipt.totalAmount)
                                "date" -> editedReceipt.copy(transactionDate = value)
                                else -> editedReceipt
                            }
                        }
                    )
                }
                
                if (editedReceipt.items.isNotEmpty()) {
                    item {
                        ItemsReviewSection(
                            items = editedReceipt.items,
                            onItemsChange = { newItems ->
                                editedReceipt = editedReceipt.copy(items = newItems)
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        ReviewActionButtons(
            onConfirm = { onConfirm(editedReceipt) },
            onEdit = { showEditDialog = true },
            onReject = onReject,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    // Edit dialog
    if (showEditDialog) {
        ReceiptEditDialog(
            receipt = editedReceipt,
            onSave = { updatedReceipt ->
                editedReceipt = updatedReceipt
                showEditDialog = false
            },
            onCancel = { showEditDialog = false }
        )
    }
}

@Composable
private fun ReceiptReviewHeader(
    receipt: ProcessedReceipt,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                receipt.confidence > 0.9f -> MaterialTheme.colorScheme.primaryContainer
                receipt.confidence > 0.7f -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Receipt Review",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                ConfidenceIndicator(
                    confidence = receipt.confidence,
                    modifier = Modifier
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please verify the extracted information is correct",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val confidenceText = "${(confidence * 100).toInt()}%"
    val confidenceColor = when {
        confidence > 0.9f -> Color.Green
        confidence > 0.7f -> Color.Orange
        else -> Color.Red
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = confidenceColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    confidence > 0.9f -> Icons.Default.CheckCircle
                    confidence > 0.7f -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = "Confidence",
                tint = confidenceColor,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = confidenceText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = confidenceColor
            )
        }
    }
}

@Composable
private fun ReceiptDetailsSection(
    receipt: ProcessedReceipt,
    onFieldEdit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Receipt Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        EditableReceiptField(
            label = "Merchant",
            value = receipt.merchantName,
            onValueChange = { onFieldEdit("merchant", it) },
            icon = Icons.Default.Store
        )
        
        EditableReceiptField(
            label = "Amount",
            value = receipt.totalAmount.toString(),
            onValueChange = { onFieldEdit("amount", it) },
            icon = Icons.Default.AttachMoney
        )
        
        EditableReceiptField(
            label = "Date",
            value = receipt.transactionDate,
            onValueChange = { onFieldEdit("date", it) },
            icon = Icons.Default.CalendarToday
        )
    }
}

@Composable
private fun EditableReceiptField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        editValue = value
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(
                    onClick = { 
                        if (isEditing) {
                            onValueChange(editValue)
                            isEditing = false
                        } else {
                            isEditing = true
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isEditing) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Text(
                    text = value.ifBlank { "Not detected" },
                    fontSize = 16.sp,
                    color = if (value.isBlank()) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun ItemsReviewSection(
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var editableItems by remember { mutableStateOf(items.toMutableList()) }
    
    LaunchedEffect(items) {
        editableItems = items.toMutableList()
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Items (${editableItems.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            TextButton(
                onClick = {
                    editableItems.add("")
                    onItemsChange(editableItems)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item")
            }
        }
        
        editableItems.forEachIndexed { index, item ->
            ItemReviewCard(
                item = item,
                onItemChange = { newItem ->
                    editableItems[index] = newItem
                    onItemsChange(editableItems)
                },
                onRemove = {
                    editableItems.removeAt(index)
                    onItemsChange(editableItems)
                }
            )
        }
    }
}

@Composable
private fun ItemReviewCard(
    item: String,
    onItemChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(item.isBlank()) }
    var editValue by remember { mutableStateOf(item) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    placeholder = { Text("Enter item name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                IconButton(
                    onClick = {
                        onItemChange(editValue)
                        isEditing = false
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                }
            } else {
                Text(
                    text = item.ifBlank { "Empty item" },
                    modifier = Modifier.weight(1f),
                    color = if (item.isBlank()) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                IconButton(
                    onClick = { isEditing = true }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
            
            IconButton(
                onClick = onRemove
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ReviewActionButtons(
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reject")
        }
        
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit")
        }
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Confirm")
        }
    }
}

@Composable
private fun ReceiptEditDialog(
    receipt: ProcessedReceipt,
    onSave: (ProcessedReceipt) -> Unit,
    onCancel: () -> Unit
) {
    var editedReceipt by remember { mutableStateOf(receipt) }
    
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Edit Receipt") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editedReceipt.merchantName,
                    onValueChange = { editedReceipt = editedReceipt.copy(merchantName = it) },
                    label = { Text("Merchant") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = editedReceipt.totalAmount.toString(),
                    onValueChange = { 
                        val amount = it.toDoubleOrNull()
                        if (amount != null) {
                            editedReceipt = editedReceipt.copy(totalAmount = amount)
                        }
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = editedReceipt.transactionDate,
                    onValueChange = { editedReceipt = editedReceipt.copy(transactionDate = it) },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(editedReceipt) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}