package com.srishti.pantrypulse
import Category
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.srishti.pantrypulse.util.OcrDateParser
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDate
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIView
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraScanner(
    onDateDetected: (LocalDate?, LocalDate?, String, Category?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    mode: String
) {
    val captureSession = remember { AVCaptureSession() }

    // Stop AV capture session running when composable is disposed
    androidx.compose.runtime.DisposableEffect(captureSession) {
        onDispose {
            if (captureSession.isRunning()) {
                captureSession.stopRunning()
            }
        }
    }

    UIKitView(
        factory = {
            val previewView = UIView()
            val videoLayer = AVCaptureVideoPreviewLayer(session = captureSession).apply {
                videoGravity = AVLayerVideoGravityResizeAspectFill
            }
            previewView.layer.addSublayer(videoLayer)

            // Set up native iOS AVCaptureSession + Apple Vision Framework text recognition
            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: AVCaptureDevice()
            val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as? AVCaptureDeviceInput
            if (input != null && captureSession.canAddInput(input)) {
                captureSession.addInput(input)
            }

            val startTime = NSDate.date().timeIntervalSince1970()

            val output = AVCaptureVideoDataOutput().apply {
                setSampleBufferDelegate(object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
                    override fun captureOutput(
                        output: AVCaptureOutput,
                        didOutputSampleBuffer: CMSampleBufferRef?,
                        fromConnection: AVCaptureConnection
                    ) {
                        if (didOutputSampleBuffer == null) return

                        val elapsed = NSDate.date().timeIntervalSince1970() - startTime
                        if (elapsed < 3.0) return

                        // Extract native pixel buffer safely to avoid pointer errors with CMSampleBufferRef constructors
                        val pixelBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return

                        // Execute Vision OCR request on native CVPixelBufferRef frames
                        // Positional constructors are preferred in Kotlin Native to guarantee interop match for initWithCVPixelBuffer:options:
                        val requestHandler = VNImageRequestHandler(pixelBuffer, emptyMap<Any?, Any?>())
                        val request = VNRecognizeTextRequest { request, error ->
                            val results = request?.results as? List<VNRecognizedTextObservation>
                            val scannedString = results?.joinToString("\\n") { (it.topCandidates(1u).firstOrNull() as? VNRecognizedText)?.string ?: "" }
                            if (!scannedString.isNullOrBlank()) {
                                val parsed = OcrDateParser.parse(scannedString, mode)
                                if (parsed != null) {
                                    dispatch_async(dispatch_get_main_queue()) {
                                        onDateDetected(parsed.expiryDate, parsed.buyDate, parsed.productName, parsed.category)
                                    }
                                }
                            }
                        }
                        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
                        requestHandler.performRequests(listOf(request), null)
                    }
                }, queue = dispatch_get_main_queue())
            }

            if (captureSession.canAddOutput(output)) {
                captureSession.addOutput(output)
            }

            captureSession.startRunning()
            previewView
        },
        modifier = modifier
    )
}