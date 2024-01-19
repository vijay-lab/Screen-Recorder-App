package com.kmmc.media_record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File  // Import for File
import android.widget.Toast  // Import for Toast
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.media.projection.MediaProjectionManager

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.kmmc.screenrecorder/screenrecord"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startRecording" -> {
                    if (checkPermissions()) {
                        startRecording()
                    } else {
                        requestPermissions()
                    }
                    result.success(null)
                }
                "stopRecording" -> {
                    stopRecording()
                    result.success(null)
                }
                "openFileManager" -> {
                    openFileManager()
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return audioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSIONS_REQUEST_CODE)
    }

    private fun startRecording() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val screenCaptureIntent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(screenCaptureIntent, SCREEN_RECORD_REQUEST_CODE)
    }

    private fun stopRecording() {
        val serviceIntent = Intent(this, ScreenRecordService::class.java)
        serviceIntent.action = ScreenRecordService.ACTION_STOP
        startService(serviceIntent)
    }

    private fun openFileManager() {
        val recordingDirectory = File(getExternalFilesDir(null), "support/recording")
        val uri = Uri.parse(recordingDirectory.absolutePath)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(Intent.createChooser(intent, "Open folder"))
        } catch (e: Exception) {
            // Handle the situation where a file manager is not installed
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenRecordService::class.java)
            serviceIntent.action = ScreenRecordService.ACTION_START
            serviceIntent.putExtras(data)
            startService(serviceIntent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        }
    }

    companion object {
        private const val SCREEN_RECORD_REQUEST_CODE = 1000
        private const val PERMISSIONS_REQUEST_CODE = 2000
    }
}
