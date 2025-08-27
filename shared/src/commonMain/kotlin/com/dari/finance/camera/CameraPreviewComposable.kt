package com.dari.finance.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Camera preview composable with receipt scanning overlay
 */
@Composable
fun CameraPreviewScreen(
    cameraManager: CameraManager,
    onImageCaptured: (ByteArray) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cameraState by remember { mutableStateOf(cameraManager.getCameraState()) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var showCaptureAnimation by remember { mutableStateOf(false) }
    
    // Update camera state
    LaunchedEffect(cameraManager) {
        while (true) {
            cameraState = cameraManager.getCameraState()
            delay(100) // Update every 100ms
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview (platform-specific implementation)
        PlatformCameraPreview(
            cameraManager = cameraManager,
            modifier = Modifier.fillMaxSize()
        )
        
        // Receipt scanning overlay
        ReceiptScanningOverlay(
            modifier = Modifier.fillMaxSize()
        )
        
        // Top controls
        TopCameraControls(
            onNavigateBack = onNavigateBack,
            onToggleFlashlight = { 
                cameraManager.toggleFlashlight(!cameraState.isFlashlightOn)
            },
            isFlashlightOn = cameraState.isFlashlightOn,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        )
        
        // Bottom controls
        BottomCameraControls(
            onCapture = {
                if (!cameraState.isCapturing) {
                    captureImage(cameraManager, onImageCaptured) { error ->
                        captureError = error
                    }
                }
            },
            isCapturing = cameraState.isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.BottomCenter)
        )
        
        // Capture animation
        if (showCaptureAnimation) {
            CaptureAnimation(
                onAnimationComplete = { showCaptureAnimation = false },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Error display
        captureError?.let { error ->
            ErrorSnackbar(
                message = error,
                onDismiss = { captureError = null },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun TopCameraControls(
    onNavigateBack: () -> Unit,
    onToggleFlashlight: () -> Unit,
    isFlashlightOn: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        // Title
        Text(
            text = "Scan Receipt",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Flashlight toggle
        IconButton(
            onClick = onToggleFlashlight,
            modifier = Modifier
                .background(
                    if (isFlashlightOn) Color.Yellow.copy(alpha = 0.8f) 
                    else Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = if (isFlashlightOn) "Turn off flash" else "Turn on flash",
                tint = if (isFlashlightOn) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun BottomCameraControls(
    onCapture: () -> Unit,
    isCapturing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Instructions
        Text(
            text = "Position receipt within the frame and tap capture",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
                .fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Capture button
        Button(
            onClick = onCapture,
            enabled = !isCapturing,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                disabledContainerColor = Color.Gray
            )
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Gray
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ReceiptScanningOverlay(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Receipt frame overlay
        ReceiptFrameOverlay(
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Corner guides
        CornerGuides(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ReceiptFrameOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(280.dp)
            .height(360.dp)
    ) {
        // Semi-transparent border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .padding(4.dp)
        ) {
            // Inner frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
private fun CornerGuides(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(280.dp, 360.dp)) {
        val cornerSize = 24.dp
        val strokeWidth = 3.dp
        
        // Top-left corner
        Canvas(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.TopStart)
        ) {
            // Implementation would draw corner guides
        }
        
        // Top-right corner
        Canvas(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.TopEnd)
        ) {
            // Implementation would draw corner guides
        }
        
        // Bottom-left corner
        Canvas(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.BottomStart)
        ) {
            // Implementation would draw corner guides
        }
        
        // Bottom-right corner
        Canvas(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.BottomEnd)
        ) {
            // Implementation would draw corner guides
        }
    }
}

@Composable
private fun CaptureAnimation(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var alpha by remember { mutableStateOf(1.0f) }
    
    LaunchedEffect(Unit) {
        // Flash animation
        alpha = 0.0f
        delay(150)
        alpha = 1.0f
        delay(100)
        onAnimationComplete()
    }
    
    Box(
        modifier = modifier.background(Color.White.copy(alpha = alpha))
    )
}

@Composable
private fun ErrorSnackbar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
expect fun PlatformCameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier
)

@Composable
expect fun Canvas(
    modifier: Modifier,
    onDraw: () -> Unit = {}
)

private fun captureImage(
    cameraManager: CameraManager,
    onImageCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    val result = cameraManager.captureImage { captureResult ->
        when (captureResult) {
            is CameraResult.Success -> onImageCaptured(captureResult.data)
            is CameraResult.Error -> onError(captureResult.message)
        }
    }
    
    if (result is CameraResult.Error) {
        onError(result.message)
    }
}