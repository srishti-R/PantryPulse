package com.srishti.pantrypulse

import PermissionGate
import VoiceWaveform
import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.srishti.pantrypulse.Utilities.parseRelativeDateFromSpeech
import kotlinx.datetime.LocalDate

@Composable
actual fun VoiceCommandOverlay(
    mode: String,
    onDateParsed: (LocalDate) -> Unit,
    onProductNameParsed: (String) -> Unit,
    onClose: () -> Unit
) {
    PermissionGate(Manifest.permission.RECORD_AUDIO) {
        VoiceCommandOverlayContent(mode, onDateParsed, onProductNameParsed, onClose)
    }
}

@Composable
fun VoiceCommandOverlayContent(
    mode: String,
    onDateParsed: (LocalDate) -> Unit,
    onProductNameParsed: (String) -> Unit,
    onClose: () -> Unit
) {
    if (mode.isEmpty()) return
    val context = LocalContext.current
    var speechText by remember { mutableStateOf("Ready to record voice...") }
    var isProcessing by remember { mutableStateOf(false) }
    var volumeIntensity by remember { mutableStateOf(0f) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                speechText = "Listening... Speak now"
                isProcessing = true
            }

            override fun onBeginningOfSpeech() {
                isProcessing = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB usually ranges between -2f and 10f+; map to 0.0f - 1.0f range dynamically
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                volumeIntensity = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                speechText = "Processing voice command..."
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_CLIENT -> "Client error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission missing."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No voice matched."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer busy."
                    SpeechRecognizer.ERROR_SERVER -> "Server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
                    else -> "Voice recognition failed."
                }
                speechText = errorMessage
                isProcessing = false
                volumeIntensity = 0f
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heardText = matches?.firstOrNull()
                if (!heardText.isNullOrBlank()) {
                    if (mode == "productName") {
                        speechText = $$"Success! Set to ${heardText}"
                        onProductNameParsed(heardText)
                    } else {
                        val resolvedDate = parseRelativeDateFromSpeech(heardText, mode)
                        speechText = $$"Success! Set to ${resolvedDate.formatToDeviceLocale()}"
                        onDateParsed(resolvedDate)
                    }
                } else {
                    speechText = "No clear command heard."
                }
                isProcessing = false
                volumeIntensity = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)
        try {
            speechRecognizer.startListening(intent)
            isProcessing = true
            speechText = "Listening..."
        } catch (_: Exception) {
            speechText = "System Voice Search is unavailable on this device."
            isProcessing = false
        }

        onDispose {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false) {}, // Prevent click propagation to outer dim area
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Drawer drag handle indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    )

                    Text(
                        text = if (mode == "expiryDate") "Voice Input: Expiry Date" else "Voice Input: Buy Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    VoiceWaveform(isProcessing = isProcessing, volumeIntensity = volumeIntensity)

                    // Pulse mic indicator button representing voice activity
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = if (isProcessing) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (isProcessing) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = speechText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

