package com.dari.finance.camera

import platform.AVFoundation.*
import platform.Foundation.*
import platform.UIKit.*

/**
 * iOS implementation of camera permission handler
 * Uses AVFoundation authorization system
 */
class IOSCameraPermissionHandler : CameraPermissionHandler {

    override fun checkCameraPermission(): CameraPermissionStatus {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> CameraPermissionStatus.GRANTED
            AVAuthorizationStatusDenied -> CameraPermissionStatus.PERMANENTLY_DENIED
            AVAuthorizationStatusRestricted -> CameraPermissionStatus.PERMANENTLY_DENIED
            AVAuthorizationStatusNotDetermined -> CameraPermissionStatus.NOT_DETERMINED
            else -> CameraPermissionStatus.NOT_DETERMINED
        }
    }

    override fun requestCameraPermission(callback: (CameraPermissionStatus) -> Unit) {
        val currentStatus = checkCameraPermission()
        
        if (currentStatus == CameraPermissionStatus.GRANTED) {
            callback(CameraPermissionStatus.GRANTED)
            return
        }
        
        if (currentStatus == CameraPermissionStatus.PERMANENTLY_DENIED) {
            callback(CameraPermissionStatus.PERMANENTLY_DENIED)
            return
        }
        
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            NSOperationQueue.mainQueue.addOperationWithBlock {
                val status = if (granted) {
                    CameraPermissionStatus.GRANTED
                } else {
                    // Check if it's permanently denied or just denied
                    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                        AVAuthorizationStatusDenied -> CameraPermissionStatus.PERMANENTLY_DENIED
                        else -> CameraPermissionStatus.DENIED
                    }
                }
                callback(status)
            }
        }
    }

    override fun shouldShowPermissionRationale(): Boolean {
        // iOS doesn't have the concept of "shouldShowRequestPermissionRationale"
        // We can show rationale when status is NotDetermined
        return checkCameraPermission() == CameraPermissionStatus.NOT_DETERMINED
    }

    override fun openAppSettings() {
        if (UIApplication.sharedApplication.canOpenURL(NSURL(string = UIApplicationOpenSettingsURLString)!!)) {
            UIApplication.sharedApplication.openURL(
                NSURL(string = UIApplicationOpenSettingsURLString)!!
            )
        }
    }

    /**
     * Additional iOS-specific utilities
     */
    fun isCameraAvailable(): Boolean {
        return UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypeCamera)
    }

    fun getAvailableCameraDevices(): List<AVCaptureDevice> {
        return AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo).map { 
            it as AVCaptureDevice 
        }
    }

    fun hasFlashlight(): Boolean {
        val devices = getAvailableCameraDevices()
        return devices.any { device ->
            device.hasFlash() || device.hasTorch()
        }
    }

    fun getCameraPosition(device: AVCaptureDevice): CameraPosition {
        return when (device.position) {
            AVCaptureDevicePositionFront -> CameraPosition.FRONT
            AVCaptureDevicePositionBack -> CameraPosition.BACK
            else -> CameraPosition.UNSPECIFIED
        }
    }
}

enum class CameraPosition {
    FRONT,
    BACK, 
    UNSPECIFIED
}