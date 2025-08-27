package code.yousef.dari.shared.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

/**
 * Motion and animation specifications for the Dari app
 * Following Material Design 3 motion principles
 */
object DariMotion {
    
    // Duration constants
    object Duration {
        const val INSTANT = 0
        const val QUICK = 100
        const val FAST = 200
        const val NORMAL = 300
        const val SLOW = 400
        const val SLOWER = 500
        const val SLOWEST = 700
    }
    
    // Easing curves
    object Easing {
        val Standard = FastOutSlowInEasing
        val Accelerate = LinearOutSlowInEasing
        val Decelerate = LinearEasing
        val Emphasized = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    }
    
    // Animation specs
    object AnimationSpecs {
        // Standard animations
        val Standard = tween<Float>(Duration.NORMAL, easing = Easing.Standard)
        val Quick = tween<Float>(Duration.QUICK, easing = Easing.Standard)
        val Fast = tween<Float>(Duration.FAST, easing = Easing.Standard)
        val Slow = tween<Float>(Duration.SLOW, easing = Easing.Standard)
        
        // Emphasized animations for important UI changes
        val Emphasized = tween<Float>(Duration.SLOW, easing = Easing.Emphasized)
        
        // Spring animations for bouncy effects
        val SpringStandard = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
        
        val SpringBouncy = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
        
        // Infinite animations
        val PulseAnimation = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    }
    
    // Financial-specific animations
    object Financial {
        // Balance change animations
        val BalanceUpdate = tween<Float>(Duration.SLOW, easing = Easing.Emphasized)
        
        // Transaction animations
        val TransactionAdd = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
        
        // Chart animations
        val ChartAnimation = tween<Float>(Duration.SLOWER, easing = Easing.Emphasized)
        
        // Progress bar animations
        val ProgressAnimation = tween<Float>(Duration.SLOW, easing = Easing.Standard)
        
        // Goal completion animation
        val GoalCompletion = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    }
    
    // Screen transitions
    object ScreenTransitions {
        // Default enter/exit for navigation
        val DefaultEnter: EnterTransition = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(Duration.NORMAL, easing = Easing.Standard)
        ) + fadeIn(animationSpec = tween(Duration.NORMAL))
        
        val DefaultExit: ExitTransition = slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(Duration.NORMAL, easing = Easing.Standard)
        ) + fadeOut(animationSpec = tween(Duration.NORMAL))
        
        val DefaultPopEnter: EnterTransition = slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = tween(Duration.NORMAL, easing = Easing.Standard)
        ) + fadeIn(animationSpec = tween(Duration.NORMAL))
        
        val DefaultPopExit: ExitTransition = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(Duration.NORMAL, easing = Easing.Standard)
        ) + fadeOut(animationSpec = tween(Duration.NORMAL))
        
        // Bottom sheet transitions
        val BottomSheetEnter: EnterTransition = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(Duration.NORMAL, easing = Easing.Standard)
        ) + fadeIn(animationSpec = tween(Duration.NORMAL))
        
        val BottomSheetExit: ExitTransition = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Duration.NORMAL, easing = Easing.Standard)
        ) + fadeOut(animationSpec = tween(Duration.NORMAL))
        
        // Modal/Dialog transitions
        val ModalEnter: EnterTransition = scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(Duration.NORMAL, easing = Easing.Emphasized)
        ) + fadeIn(animationSpec = tween(Duration.NORMAL))
        
        val ModalExit: ExitTransition = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(Duration.FAST, easing = Easing.Accelerate)
        ) + fadeOut(animationSpec = tween(Duration.FAST))
        
        // Tab transitions
        val TabEnter: EnterTransition = fadeIn(
            animationSpec = tween(Duration.FAST, easing = Easing.Standard)
        )
        
        val TabExit: ExitTransition = fadeOut(
            animationSpec = tween(Duration.FAST, easing = Easing.Standard)
        )
    }
}

/**
 * Provides motion specifications for different UI components
 */
object ComponentMotion {
    // Button press animations
    const val BUTTON_PRESS_SCALE = 0.95f
    val BUTTON_PRESS_DURATION = DariMotion.Duration.QUICK
    
    // Card hover/press animations
    const val CARD_HOVER_ELEVATION = 8f
    const val CARD_PRESS_SCALE = 0.98f
    
    // FAB animations
    const val FAB_SCALE_FACTOR = 1.2f
    val FAB_ANIMATION_SPEC = DariMotion.AnimationSpecs.SpringBouncy
    
    // Loading indicator
    val LOADING_ROTATION_DURATION = 1000
    val LOADING_PULSE_DURATION = 1200
    
    // Swipe-to-refresh
    const val REFRESH_TRIGGER_DISTANCE = 80f
    val REFRESH_ANIMATION_DURATION = DariMotion.Duration.NORMAL
}