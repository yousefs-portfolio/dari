package code.yousef.dari.shared.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import code.yousef.dari.shared.ui.theme.DariMotion

/**
 * Loading indicator with customizable appearance and animation
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Loading...",
    color: Color = MaterialTheme.colorScheme.primary,
    size: LoadingSize = LoadingSize.Medium
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (size) {
            LoadingSize.Small -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = color
            )
            LoadingSize.Medium -> CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = color
            )
            LoadingSize.Large -> CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp,
                color = color
            )
        }
        
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = when (size) {
                    LoadingSize.Small -> MaterialTheme.typography.bodySmall
                    LoadingSize.Medium -> MaterialTheme.typography.bodyMedium
                    LoadingSize.Large -> MaterialTheme.typography.bodyLarge
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Small loading indicator for inline use
 */
@Composable
fun LoadingIndicatorSmall(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = color
        )
    }
}

/**
 * Pulsing loading indicator for financial data
 */
@Composable
fun PulsingLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = DariMotion.AnimationSpecs.PulseAnimation,
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = DariMotion.AnimationSpecs.PulseAnimation,
        label = "alpha"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size((40 * scale).dp)
                .rotate(scale * 360f),
            strokeWidth = 3.dp,
            color = color.copy(alpha = alpha)
        )
    }
}

/**
 * Full screen loading overlay
 */
@Composable
fun FullScreenLoading(
    message: String = "Loading...",
    isVisible: Boolean = true,
    onDismiss: (() -> Unit)? = null
) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onDismiss != null) {
                        Modifier.clickable { onDismiss() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Semi-transparent background
            androidx.compose.foundation.background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading content
            Surface(
                modifier = Modifier.wrapContentSize(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                LoadingIndicator(
                    modifier = Modifier.padding(24.dp),
                    message = message,
                    size = LoadingSize.Large
                )
            }
        }
    }
}

/**
 * Skeleton loading placeholder for list items
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }
    
    Column(modifier = modifier) {
        repeat(lines) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == lines - 1) 0.7f else 1f)
                    .height(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        shape = MaterialTheme.shapes.small
                    )
            )
            if (index < lines - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Loading sizes
 */
enum class LoadingSize {
    Small,
    Medium, 
    Large
}