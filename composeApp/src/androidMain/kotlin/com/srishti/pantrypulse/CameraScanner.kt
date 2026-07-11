package com.srishti.pantrypulse

import CameraPermissionGate
import Category
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.srishti.pantrypulse.util.OcrDateParser
import kotlinx.datetime.LocalDate
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
actual fun CameraScanner(
    onDateDetected: (LocalDate?, LocalDate?, String, Category?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    mode: String
) {
    CameraPermissionGate {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
        val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

        // Clean up camera executor and unbind camera provider when composable is disposed
        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            onDispose {
                cameraExecutor.shutdown()
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = modifier,
            update = { previewView ->
                // Note: If ProcessCameraProvider does not resolve, ensure you have the following dependency in build.gradle.kts:
                // implementation(libs.camerax.lifecycle)
                //
                // If the compiler says "cannot access class com.google.common.util.concurrent.ListenableFuture":
                //
                // This is a common Kotlin Multiplatform (KMP) dependency visibility issue.
                // Since 'CameraScanner' is imported/compiled in your UI/App module, the dependency MUST be
                // exposed transitively using 'api' instead of 'implementation' in your shared module's build.gradle.kts:
                //
                // Change:
                // implementation("com.google.guava:listenablefuture:1.0")
                // To:
                // api("com.google.guava:listenablefuture:1.0")
                //
                // (Alternatively, you can also add 'implementation("com.google.guava:listenablefuture:1.0")'
                // directly to the app-level module's build.gradle.kts so the compiler can access it during compilation of AddItemScreen.)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context.applicationContext)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    // Capture starting time to allow 3 seconds of settling/adjusting delay
                    val startTime = System.currentTimeMillis()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed < 3000) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            textRecognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    val detectedText = visionText.text
                                    // Parse dates and details safely using the OcrDateParser helper utility
                                    val parsedResult = OcrDateParser.parse(detectedText, mode)
                                    if (parsedResult != null) {
                                        onDateDetected(parsedResult.expiryDate, parsedResult.buyDate, parsedResult.productName, parsedResult.category)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
    }
}