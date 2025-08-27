package code.yousef.dari.shared.data.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.Flow

/**
 * Theme preferences for managing user's theme settings
 */
interface ThemePreferences {
    /**
     * Get the current theme mode
     * @return Flow of ThemeMode
     */
    fun getThemeMode(): Flow<ThemeMode>
    
    /**
     * Set the theme mode
     * @param themeMode The theme mode to set
     */
    suspend fun setThemeMode(themeMode: ThemeMode)
    
    /**
     * Get dynamic color preference
     * @return Flow of Boolean indicating if dynamic color is enabled
     */
    fun getDynamicColorEnabled(): Flow<Boolean>
    
    /**
     * Set dynamic color preference
     * @param enabled Whether dynamic color should be enabled
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    
    /**
     * Get high contrast preference
     * @return Flow of Boolean indicating if high contrast is enabled
     */
    fun getHighContrastEnabled(): Flow<Boolean>
    
    /**
     * Set high contrast preference
     * @param enabled Whether high contrast should be enabled
     */
    suspend fun setHighContrastEnabled(enabled: Boolean)
}

/**
 * Theme mode options
 */
enum class ThemeMode {
    SYSTEM,  // Follow system setting
    LIGHT,   // Always light theme
    DARK     // Always dark theme
}

/**
 * Theme state for UI consumption
 */
data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val highContrastEnabled: Boolean = false
)

/**
 * Composable to collect theme state
 */
@Composable
fun collectThemeState(themePreferences: ThemePreferences): ThemeState {
    val themeMode by themePreferences.getThemeMode().collectAsState(ThemeMode.SYSTEM)
    val dynamicColorEnabled by themePreferences.getDynamicColorEnabled().collectAsState(false)
    val highContrastEnabled by themePreferences.getHighContrastEnabled().collectAsState(false)
    
    return ThemeState(
        themeMode = themeMode,
        dynamicColorEnabled = dynamicColorEnabled,
        highContrastEnabled = highContrastEnabled
    )
}