package com.srishti.pantrypulse

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate

@Composable
expect fun VoiceCommandOverlay(
    mode: String,
    onDateParsed: (LocalDate) -> Unit,
    onProductNameParsed: (String) -> Unit = {},
    onClose: () -> Unit
)