package com.example.cameraxgl

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// Based partly on example at: https://developer.android.com/codelabs/camerax-getting-started
// CameraX to OpenGL based on info at https://stackoverflow.com/questions/56163568/how-to-bind-preview-and-texture-in-camerax

class MainActivity : AppCompatActivity() {
    private var permissions = arrayOf(Manifest.permission.CAMERA)
    private var surfaceTexture: SurfaceTexture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openglview = OpenGLView(this) {
            Log.d("CAMERAXGL", "Starting camera")
            surfaceTexture = it
            if (!startCamera()) {
                ActivityCompat.requestPermissions(this, permissions, 0)
            }
        }
        setContentView(openglview)
    }

    private fun checkPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            AlertDialog.Builder(this).setPositiveButton("OK", null)
                .setMessage("Will not work as camera permission not granted").show()
        }
    }

    private fun startCamera(): Boolean {
        Log.d("CAMERAXGL", "startCamera()")
        if (checkPermissions()) {
            Log.d("CAMERAXGL", "startCamera() ready to go")
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    val provider: (SurfaceRequest) -> Unit = { request ->
                        val resolution = request.resolution
                        surfaceTexture?.apply {

                            setDefaultBufferSize(resolution.width, resolution.height)
                            val surface = Surface(this)
                            request.provideSurface(
                                surface,
                                ContextCompat.getMainExecutor(this@MainActivity.baseContext))
                                { }

                        }
                    }
                    it.setSurfaceProvider(provider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                } catch (e: Exception) {
                    Log.e("CAMERAXGL", e.stackTraceToString())
                }
            }, ContextCompat.getMainExecutor(this))
            return true
        } else {
            return false
        }
    }
}