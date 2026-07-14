package com.srishti.pantrypulse

import VoiceWaveform
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.srishti.pantrypulse.Utilities.parseRelativeDateFromSpeech
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.datetime.LocalDate
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.setActive
import platform.Speech.*
import platform.AVFoundation.*
import platform.Foundation.*
import platform.UIKit.*
import kotlin.math.sqrt

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VoiceCommandOverlay(
    mode: String,
    onDateParsed: (LocalDate) -> Unit,
    onProductNameParsed: (String) -> Unit,
    onClose: () -> Unit
) {
    var speechText by remember { mutableStateOf("Ready to record voice...") }
    var isProcessing by remember { mutableStateOf(false) }
    var volumeIntensity by remember { mutableStateOf(0f) }

    val speechRecognizer = remember { SFSpeechRecognizer(NSLocale(localeIdentifier = "en-US")) }
    val audioEngine = remember { AVAudioEngine() }
    var recognitionRequest by remember { mutableStateOf<SFSpeechAudioBufferRecognitionRequest?>(null) }
    var recognitionTask by remember { mutableStateOf<SFSpeechRecognitionTask?>(null) }

    DisposableEffect(Unit) {
        // Request authorization and start voice input on iOS
        SFSpeechRecognizer.requestAuthorization { status ->
            if (status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                AVAudioSession.sharedInstance().requestRecordPermission { allowed ->
                    if (allowed) {
                        try {
                            isProcessing = true
                            speechText = "Listening... Speak now"

                            // Initialize capture graph on iOS AVAudioEngine
                            val audioSession = AVAudioSession.sharedInstance()
                            audioSession.setCategory(AVAudioSessionCategoryRecord, error = null)
                            audioSession.setActive(true, error = null)

                            val request = SFSpeechAudioBufferRecognitionRequest().apply {
                                shouldReportPartialResults = false
                            }
                            recognitionRequest = request

                            val inputNode = audioEngine.inputNode
                            val recordingFormat = inputNode.outputFormatForBus(0u)
                            inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
                                request.appendAudioPCMBuffer(buffer!!)

                                // Real-time RMS (amplitude) calculation from the audio buffer channel samples
                                val channelData = buffer?.floatChannelData?.get(0)
                                if (channelData != null) {
                                    val frameCount = buffer.frameLength.toInt()
                                    var sumOfSquares = 0f
                                    for (i in 0 until frameCount) {
                                        val sample = channelData[i]
                                        sumOfSquares += sample * sample
                                    }
                                    val rms = sqrt(sumOfSquares / frameCount)
                                    // Map normal speaking RMS range (~0.0 to 0.35) to 0.0 to 1.0 range
                                    val normalized = (rms * 3.0f).coerceIn(0f, 1f)

                                    // Update state safely on Main Thread for Jetpack Compose UI
                                    platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                                        volumeIntensity = normalized
                                    }
                                }
                            }

                            audioEngine.prepare()
                            audioEngine.startAndReturnError(null)

                            recognitionTask = speechRecognizer?.recognitionTaskWithRequest(request) { result, error ->
                                if (result != null) {
                                    val bestTranscription = result.bestTranscription.formattedString
                                    if (mode == "productName") {
                                        speechText = "Success! Set to \${bestTranscription}"
                                        onProductNameParsed(bestTranscription)
                                    } else {
                                        val resolvedDate = parseRelativeDateFromSpeech(bestTranscription, mode)
                                        speechText = "Success! Set to \${resolvedDate.formatToDeviceLocale()}"
                                        onDateParsed(resolvedDate)
                                    }
                                } else if (error != null) {
                                    speechText = "Error in recognition: \${error.localizedDescription}"
                                    isProcessing = false
                                }
                            }
                        } catch (e: Exception) {
                            speechText = "Failed to start speech recognition."
                            isProcessing = false
                        }
                    } else {
                        speechText = "Microphone permission denied."
                        isProcessing = false
                    }
                }
            } else {
                speechText = "Speech recognition permission denied."
                isProcessing = false
            }
        }

        onDispose {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
            recognitionRequest?.endAudio()
            recognitionTask?.cancel()
            isProcessing = false
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
                    containerColor = Color(0xFF0F172A) // Custom themed slate background
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
                        text = when (mode) {
                            "productName" -> "Voice Input: Product Name"
                            "expiryDate" -> "Voice Input: Expiry Date"
                            else -> "Voice Input: Buy Date"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // WhatsApp-style sound wave / voice frequency visualization
                    VoiceWaveform(isProcessing = isProcessing, volumeIntensity = volumeIntensity)

                    // Pulse mic indicator button representing voice activity
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = if (isProcessing) Color(0xFF00A884).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (isProcessing) Color(0xFF00A884) else Color.White.copy(alpha = 0.6f),
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