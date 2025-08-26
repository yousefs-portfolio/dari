package code.yousef.dari.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Font families for the app
// These will be loaded from resources when font files are added
@Composable
expect fun arabicFontFamily(): FontFamily

@Composable  
expect fun englishFontFamily(): FontFamily

// Default font families
object AppFonts {
    @Composable
    fun getDefaultFontFamily(): FontFamily {
        return FontFamily.Default
    }
    
    @Composable
    fun getArabicFontFamily(): FontFamily {
        return try {
            arabicFontFamily()
        } catch (e: Exception) {
            FontFamily.Default
        }
    }
    
    @Composable
    fun getEnglishFontFamily(): FontFamily {
        return try {
            englishFontFamily()
        } catch (e: Exception) {
            FontFamily.Default
        }
    }
}