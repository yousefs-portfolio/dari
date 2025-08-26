package code.yousef.dari.shared.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import code.yousef.dari.shared.ui.theme.DariTheme

// Common preview annotations for consistent theming
@Preview(name = "Light Theme")
@Composable
fun LightPreview(content: @Composable () -> Unit) {
    DariTheme(darkTheme = false) {
        content()
    }
}

@Preview(name = "Dark Theme")
@Composable
fun DarkPreview(content: @Composable () -> Unit) {
    DariTheme(darkTheme = true) {
        content()
    }
}

// Device-specific previews
@Preview(name = "Phone", device = "spec:width=360dp,height=640dp,dpi=480")
@Preview(name = "Tablet", device = "spec:width=1280dp,height=800dp,dpi=480")
annotation class DevicePreviews

// RTL support preview
@Preview(name = "RTL", locale = "ar")
annotation class RtlPreview

// Font scale previews
@Preview(name = "Large Font", fontScale = 1.5f)
annotation class LargeFontPreview

@Preview(name = "Small Font", fontScale = 0.85f)
annotation class SmallFontPreview

// Combined preview annotations
@LightPreview
@DarkPreview
@DevicePreviews
annotation class ComprehensivePreviews

@LightPreview
@DarkPreview
@RtlPreview
annotation class LocalizedPreviews