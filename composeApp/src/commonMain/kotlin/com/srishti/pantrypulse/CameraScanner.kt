package com.srishti.pantrypulse
import Category
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate

/**
 * Expected interface or composable for platform-native Camera & OCR Scanner.
 * Expect-Actual design pattern lets us bind to the native camera systems on Android and iOS.
 */
@Composable
expect fun CameraScanner(
    onDateDetected: (LocalDate?, LocalDate?, String, Category?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    mode: String = "productName"
)