package com.dari.finance.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Manual adjustment controls for image enhancement
 */
@Composable
fun ManualAdjustmentControls(
    adjustmentState: ImageAdjustmentState,
    onAdjustmentChanged: (ImageAdjustmentState) -> Unit,
    onApplyAdjustments: () -> Unit,
    onResetAdjustments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.8f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Adjust Image",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Row {
                TextButton(
                    onClick = onResetAdjustments,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Reset")
                }
                
                Button(
                    onClick = onApplyAdjustments,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Apply")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Brightness control
        AdjustmentSlider(
            label = "Brightness",
            value = adjustmentState.brightness,
            onValueChange = { value ->
                onAdjustmentChanged(adjustmentState.copy(brightness = value))
            },
            valueRange = -100f..100f,
            icon = Icons.Default.Brightness6
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Contrast control
        AdjustmentSlider(
            label = "Contrast",
            value = adjustmentState.contrast,
            onValueChange = { value ->
                onAdjustmentChanged(adjustmentState.copy(contrast = value))
            },
            valueRange = 0.5f..3.0f,
            icon = Icons.Default.Tune
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Sharpness control
        AdjustmentSlider(
            label = "Sharpness",
            value = adjustmentState.sharpness,
            onValueChange = { value ->
                onAdjustmentChanged(adjustmentState.copy(sharpness = value))
            },
            valueRange = 0.0f..1.0f,
            icon = Icons.Default.FilterCenterFocus
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Gamma control
        AdjustmentSlider(
            label = "Gamma",
            value = adjustmentState.gamma,
            onValueChange = { value ->
                onAdjustmentChanged(adjustmentState.copy(gamma = value))
            },
            valueRange = 0.3f..3.0f,
            icon = Icons.Default.Exposure
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick presets
        QuickPresetButtons(
            onPresetSelected = onAdjustmentChanged
        )
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = formatSliderValue(value),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.width(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickPresetButtons(
    onPresetSelected: (ImageAdjustmentState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Quick Presets",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton(
                text = "Receipt",
                onClick = {
                    onPresetSelected(ImageAdjustmentState.RECEIPT_PRESET)
                },
                modifier = Modifier.weight(1f)
            )
            
            PresetButton(
                text = "Document",
                onClick = {
                    onPresetSelected(ImageAdjustmentState.DOCUMENT_PRESET)
                },
                modifier = Modifier.weight(1f)
            )
            
            PresetButton(
                text = "Low Light",
                onClick = {
                    onPresetSelected(ImageAdjustmentState.LOW_LIGHT_PRESET)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = null,
            width = 1.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

private fun formatSliderValue(value: Float): String {
    return when {
        value == value.toInt().toFloat() -> value.toInt().toString()
        else -> String.format("%.1f", value)
    }
}

/**
 * Real-time adjustment preview overlay
 */
@Composable
fun AdjustmentPreviewOverlay(
    isVisible: Boolean,
    adjustmentState: ImageAdjustmentState,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Preview frame with adjustment indicators
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Preview",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AdjustmentIndicator(
                        label = "B",
                        value = adjustmentState.brightness,
                        range = -100f..100f
                    )
                    
                    AdjustmentIndicator(
                        label = "C",
                        value = adjustmentState.contrast,
                        range = 0.5f..3.0f
                    )
                    
                    AdjustmentIndicator(
                        label = "S",
                        value = adjustmentState.sharpness,
                        range = 0.0f..1.0f
                    )
                    
                    AdjustmentIndicator(
                        label = "G",
                        value = adjustmentState.gamma,
                        range = 0.3f..3.0f
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustmentIndicator(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.width(12.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color.Gray, RoundedCornerShape(2.dp))
        ) {
            val progress = (value - range.start) / (range.endInclusive - range.start)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(
                        if (progress > 0.5f) MaterialTheme.colorScheme.primary 
                        else Color.White,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/**
 * Image adjustment state
 */
data class ImageAdjustmentState(
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val sharpness: Float = 0.2f,
    val gamma: Float = 1.0f
) {
    companion object {
        val DEFAULT = ImageAdjustmentState()
        
        val RECEIPT_PRESET = ImageAdjustmentState(
            brightness = 10f,
            contrast = 1.3f,
            sharpness = 0.3f,
            gamma = 0.8f
        )
        
        val DOCUMENT_PRESET = ImageAdjustmentState(
            brightness = 5f,
            contrast = 1.4f,
            sharpness = 0.4f,
            gamma = 0.9f
        )
        
        val LOW_LIGHT_PRESET = ImageAdjustmentState(
            brightness = 30f,
            contrast = 1.5f,
            sharpness = 0.2f,
            gamma = 0.7f
        )
    }
}