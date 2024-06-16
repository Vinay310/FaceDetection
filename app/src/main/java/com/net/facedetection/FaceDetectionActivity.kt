package com.net.facedetection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.net.facedetection.ui.theme.FaceDetectionTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects


class FaceDetectionActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppContent()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppContent() {
    val context = LocalContext.current
    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        context.packageName + ".provider", file
    )
    var capturedImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
            capturedImageUri = uri
        }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            val permissionResult =
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            if (permissionResult == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(uri)
            } else {
                // Request a permission
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }) {
            Text(text = "Captured Image From Camera")
        }
       Button(onClick = {
           val intent = Intent(context, TextRecognitionActivity::class.java)
           context.startActivity(intent)
       }) {
           Text(text = "Text Recognition")
       } 
    }
    if (capturedImageUri.toString().isNotEmpty()) {
        Image(
            modifier = Modifier.padding(16.dp, 8.dp),
            painter = rememberAsyncImagePainter(capturedImageUri),
            contentDescription = null
        )
        detectFace(convertImageUriToBitmap(capturedImageUri,context),context)
    }

}

fun convertImageUriToBitmap(imageUri: Uri?, context: Context): Bitmap? {
    return try {
        val contentResolver = context.contentResolver
        // Convert the URI to a Bitmap object
        val bitmap = contentResolver.openInputStream(imageUri!!)?.use {
            BitmapFactory.decodeStream(it)
        }

        // Return the Bitmap object
        bitmap
    } catch (e: NullPointerException) {
        // Handle the exception gracefully, e.g., show an error message
        Log.e("TAG", "Error converting image URI to bitmap: $e")
        null
    }
}

@Composable
fun NavigateToTextRecognition() {
   
}

fun detectFace(bitmap: Bitmap?, context: Context) {
    val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build()
    val detector = FaceDetection.getClient(highAccuracyOpts)
    val image = bitmap?.let { InputImage.fromBitmap(it, 0) }
    val result = image?.let {
        detector.process(it).addOnSuccessListener { faces ->
            var resultText = ""
            var i = 1
            for (face in faces) {
                resultText =
                    "Face Number $i" + "\nSmile : ${face.smilingProbability?.times(100)}" + "" +
                            "\n Left Eye Open ${face.leftEyeOpenProbability?.times(100)}"
                "\n Right Eye Open ${face.rightEyeOpenProbability?.times(100)}"
                i++
            }
            if (faces.isEmpty()) {
                Toast.makeText(context, "No Face detected", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, resultText, Toast.LENGTH_LONG).show()
            }

        }.addOnFailureListener { e ->
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }
}

fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH:mm:ss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
    return image
}

@RequiresApi(Build.VERSION_CODES.P)
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FaceDetectionTheme {
        AppContent()
    }
}