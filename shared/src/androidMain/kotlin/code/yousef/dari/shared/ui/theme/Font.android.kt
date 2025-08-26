package code.yousef.dari.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
actual fun arabicFontFamily(): FontFamily {
    // TODO: Load Arabic font when font files are added
    // return FontFamily(Font(SharedRes.fonts.cairo_regular))
    return FontFamily.Default
}

@Composable
actual fun englishFontFamily(): FontFamily {
    // TODO: Load English font when font files are added
    // return FontFamily(Font(SharedRes.fonts.roboto_regular))
    return FontFamily.Default
}