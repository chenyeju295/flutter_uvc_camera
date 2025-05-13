package com.chenyeju

import com.google.gson.Gson
import io.flutter.plugin.common.MethodChannel

/**
 * Exposes advanced camera features from CustomCameraUVC
 */
class CameraFeaturesManager(
    private val channel: MethodChannel
) {
    /**
     * Apply camera feature to CustomCameraUVC
     */
    fun applyCameraFeature(camera: CameraUVC?, feature: String, value: Int): Boolean {
        if (camera == null) return false
        
        when (feature.lowercase()) {
            "autofocus" -> camera.setAutoFocus(value > 0)
            "autowhitebalance" -> camera.setAutoWhiteBalance(value > 0)
            "zoom" -> camera.setZoom(value)
            "brightness" -> camera.setBrightness(value)
            "contrast" -> camera.setContrast(value)
            "saturation" -> camera.setSaturation(value)
            "sharpness" -> camera.setSharpness(value)
            "gain" -> camera.setGain(value)
            "gamma" -> camera.setGamma(value)
            "hue" -> camera.setHue(value)
            else -> return false
        }
        return true
    }
    
    /**
     * Reset camera feature
     */
    fun resetCameraFeature(camera: CameraUVC?, feature: String): Boolean {
        if (camera == null) return false
        
        when (feature.lowercase()) {
            "autofocus" -> camera.resetAutoFocus()
            "zoom" -> camera.resetZoom()
            "brightness" -> camera.resetBrightness()
            "contrast" -> camera.resetContrast()
            "saturation" -> camera.resetSaturation()
            "sharpness" -> camera.resetSharpness()
            "gain" -> camera.resetGain()
            "gamma" -> camera.resetGamma()
            "hue" -> camera.resetHue()
            else -> return false
        }
        return true
    }
    
    /**
     * Get camera feature value
     */
    fun getCameraFeature(camera: CameraUVC?, feature: String): Int? {
        if (camera == null) return null
        
        return when (feature.lowercase()) {
            "autofocus" -> if (camera.getAutoFocus() == true) 1 else 0
            "autowhitebalance" -> if (camera.getAutoWhiteBalance() == true) 1 else 0
            "zoom" -> camera.getZoom()
            "brightness" -> camera.getBrightness()
            "contrast" -> camera.getContrast()
            "saturation" -> camera.getSaturation()
            "sharpness" -> camera.getSharpness()
            "gain" -> camera.getGain()
            "gamma" -> camera.getGamma()
            "hue" -> camera.getHue()
            else -> null
        }
    }
    
    /**
     * Get all camera features as JSON
     */
    fun getAllCameraFeatures(camera: CameraUVC?): String {
        if (camera == null) return "{}"
        
        val features = mapOf(
            "autoFocus" to (camera.getAutoFocus() == true),
            "autoWhiteBalance" to (camera.getAutoWhiteBalance() == true),
            "zoom" to camera.getZoom(),
            "brightness" to camera.getBrightness(),
            "contrast" to camera.getContrast(),
            "saturation" to camera.getSaturation(),
            "sharpness" to camera.getSharpness(),
            "gain" to camera.getGain(),
            "gamma" to camera.getGamma(),
            "hue" to camera.getHue()
        )
        
        return Gson().toJson(features)
    }
} 