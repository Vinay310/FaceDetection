package com.net.facedetection

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Processor
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import java.io.IOException


class TextRecognitionActivity : AppCompatActivity() {
    private var mCameraSource: CameraSource? = null
    private var mTextRecognizer: TextRecognizer? = null
    private var mSurfaceView: SurfaceView? = null
    private var mTextView: TextView? = null
    private val RC_HANDLE_CAMERA_PERM: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_text_recognition)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mSurfaceView =findViewById(R.id.surfaceView)
        mTextView = findViewById(R.id.textView)

        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startTextRecognizer()
        } else {
            askCameraPermission()
        }
    }
    private fun startTextRecognizer() {
        mTextRecognizer = TextRecognizer.Builder(applicationContext).build()

        if (!mTextRecognizer!!.isOperational) {
            Toast.makeText(
                applicationContext,
                "Oops ! Not able to start the text recognizer ...",
                Toast.LENGTH_LONG
            ).show()
        } else {
            mCameraSource = mTextRecognizer?.let {
                CameraSource.Builder(applicationContext, it)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(15.0f)
                    .setAutoFocusEnabled(true)
                    .build()
            }

            mSurfaceView!!.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            mCameraSource!!.start(mSurfaceView!!.holder)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        askCameraPermission()
                    }
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mCameraSource!!.stop()
                }
            })

            mTextRecognizer!!.setProcessor(object :Processor<TextBlock?> {
                override
                fun receiveDetections(detections: Detector.Detections<TextBlock?>) {
                    val items: SparseArray<TextBlock?> = detections.detectedItems
                    val stringBuilder = StringBuilder()
                    for (i in 0 until items.size()) {
                        val item = items.valueAt(i)
                        if (item != null) {
                            stringBuilder.append(item.value + " ")
                        }
                    }

                    val fullText = stringBuilder.toString()
                    val handler = Handler(Looper.getMainLooper())
                    handler.post { mTextView!!.text = fullText }
                }

                override fun release() {
                    TODO("Not yet implemented")
                }
            })
        }
    }
    private fun askCameraPermission() {
        val permissions = arrayOf(android.Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.CAMERA
            )
        ) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTextRecognizer()
            return
        }
    }
}