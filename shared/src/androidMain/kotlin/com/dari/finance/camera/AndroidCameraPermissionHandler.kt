package com.dari.finance.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Android implementation of camera permission handler
 * Uses Android's runtime permission system
 */
class AndroidCameraPermissionHandler(
    private val context: Context,
    private val activity: ComponentActivity? = null
) : CameraPermissionHandler {

    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingCallback: ((CameraPermissionStatus) -> Unit)? = null

    init {
        setupPermissionLauncher()
    }

    private fun setupPermissionLauncher() {
        activity?.let { activity ->
            permissionLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                val status = if (isGranted) {
                    CameraPermissionStatus.GRANTED
                } else {
                    if (shouldShowPermissionRationale()) {
                        CameraPermissionStatus.DENIED
                    } else {
                        CameraPermissionStatus.PERMANENTLY_DENIED
                    }
                }
                pendingCallback?.invoke(status)
                pendingCallback = null
            }
        }
    }

    override fun checkCameraPermission(): CameraPermissionStatus {
        return when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> CameraPermissionStatus.GRANTED
            PackageManager.PERMISSION_DENIED -> {
                if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.CAMERA
                )) {
                    CameraPermissionStatus.DENIED
                } else {
                    // Either first time or permanently denied
                    if (isFirstTimeRequest()) {
                        CameraPermissionStatus.NOT_DETERMINED
                    } else {
                        CameraPermissionStatus.PERMANENTLY_DENIED
                    }
                }
            }
            else -> CameraPermissionStatus.NOT_DETERMINED
        }
    }

    override fun requestCameraPermission(callback: (CameraPermissionStatus) -> Unit) {
        pendingCallback = callback
        
        val currentStatus = checkCameraPermission()
        if (currentStatus == CameraPermissionStatus.GRANTED) {
            callback(CameraPermissionStatus.GRANTED)
            return
        }
        
        if (currentStatus == CameraPermissionStatus.PERMANENTLY_DENIED) {
            callback(CameraPermissionStatus.PERMANENTLY_DENIED)
            return
        }
        
        permissionLauncher?.launch(Manifest.permission.CAMERA) ?: run {
            // Fallback if no activity context
            callback(CameraPermissionStatus.DENIED)
        }
    }

    override fun shouldShowPermissionRationale(): Boolean {
        return activity?.let { activity ->
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.CAMERA
            )
        } ?: false
    }

    override fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Ignore if settings cannot be opened
            }
        }
    }

    private fun isFirstTimeRequest(): Boolean {
        // Check if we've never requested this permission before
        // This can be tracked using SharedPreferences
        val prefs = context.getSharedPreferences("camera_permissions", Context.MODE_PRIVATE)
        val hasRequestedBefore = prefs.getBoolean("has_requested_camera", false)
        
        if (!hasRequestedBefore) {
            prefs.edit().putBoolean("has_requested_camera", true).apply()
            return true
        }
        
        return false
    }

    /**
     * Additional Android-specific utilities
     */
    fun isCameraHardwareAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    fun getCameraCount(): Int {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            cameraManager.cameraIdList.size
        } catch (e: Exception) {
            0
        }
    }

    fun hasFlashlight(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }
}