package code.yousef.dari.shared.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default implementation of ThemePreferences using in-memory storage
 * Platform-specific implementations should override this with proper persistence
 */
class ThemePreferencesImpl : ThemePreferences {
    
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _dynamicColorEnabled = MutableStateFlow(false)
    private val _highContrastEnabled = MutableStateFlow(false)
    
    override fun getThemeMode(): Flow<ThemeMode> = _themeMode.asStateFlow()
    
    override suspend fun setThemeMode(themeMode: ThemeMode) {
        _themeMode.value = themeMode
    }
    
    override fun getDynamicColorEnabled(): Flow<Boolean> = _dynamicColorEnabled.asStateFlow()
    
    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
    }
    
    override fun getHighContrastEnabled(): Flow<Boolean> = _highContrastEnabled.asStateFlow()
    
    override suspend fun setHighContrastEnabled(enabled: Boolean) {
        _highContrastEnabled.value = enabled
    }
}