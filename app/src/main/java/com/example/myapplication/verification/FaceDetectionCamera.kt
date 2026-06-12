package com.example.myapplication.verification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun FaceDetectionCamera(
    onImageCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var faceDetected by remember { mutableStateOf(false) }
    var faceRect by remember { mutableStateOf<Rect?>(null) }
    var isFaceValid by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("Position your face in the frame") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(
                            imageProxy,
                            faceDetector,
                            onFaceDetected = { face, bitmap ->
                                faceDetected = true
                                faceRect = face.boundingBox
                                capturedBitmap = bitmap
                                
                                val validation = validateFace(face)
                                isFaceValid = validation.isValid
                                feedbackMessage = validation.message
                            },
                            onNoFace = {
                                faceDetected = false
                                faceRect = null
                                isFaceValid = false
                                feedbackMessage = "No face detected"
                            }
                        )
                    }
                }
            
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                android.util.Log.e("FaceCamera", "Camera binding failed", e)
            }
            
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            cameraExecutor.shutdown()
            faceDetector.close()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Face Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            faceRect?.let { rect ->
                val color = if (isFaceValid) Color.Green else Color.Yellow
                drawRect(
                    color = color,
                    topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                    size = Size(rect.width().toFloat(), rect.height().toFloat()),
                    style = Stroke(width = 8f)
                )
            }
        }
        
        // Feedback Message
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFaceValid) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        ) {
            Text(
                text = feedbackMessage,
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // Capture Button
        if (isFaceValid) {
            Button(
                onClick = {
                    capturedBitmap?.let { bitmap ->
                        onImageCaptured(bitmap)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Text("Capture Selfie")
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImage(
    imageProxy: ImageProxy,
    faceDetector: com.google.mlkit.vision.face.FaceDetector,
    onFaceDetected: (Face, Bitmap) -> Unit,
    onNoFace: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val bitmap = imageProxyToBitmap(imageProxy)
                    onFaceDetected(face, bitmap)
                } else {
                    onNoFace()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FaceDetection", "Face detection failed", e)
                onNoFace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val yBuffer = imageProxy.planes[0].buffer
    val uBuffer = imageProxy.planes[1].buffer
    val vBuffer = imageProxy.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun validateFace(face: Face): FaceValidation {
    val leftEyeOpen = face.leftEyeOpenProbability ?: 0f
    val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
    val headY = face.headEulerAngleY
    val headZ = face.headEulerAngleZ
    
    return when {
        leftEyeOpen < 0.5f || rightEyeOpen < 0.5f -> 
            FaceValidation(false, "Please open your eyes")
        headY < -15 || headY > 15 -> 
            FaceValidation(false, "Look straight at the camera")
        headZ < -10 || headZ > 10 -> 
            FaceValidation(false, "Keep your head straight")
        else -> 
            FaceValidation(true, "Perfect! Ready to capture")
    }
}

data class FaceValidation(
    val isValid: Boolean,
    val message: String
)
