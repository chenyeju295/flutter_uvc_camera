package com.chenyeju

import com.google.gson.Gson

private class FeatureAccessor(
    val jsonKey: String,
    val set: (CameraUVC, Int) -> Unit,
    val getAsInt: (CameraUVC) -> Int?,
    val getForJson: (CameraUVC) -> Any?,
    val reset: (CameraUVC) -> Unit,
)

private fun intFeature(
    jsonKey: String,
    setter: (CameraUVC, Int) -> Unit,
    getter: (CameraUVC) -> Int?,
    resetter: (CameraUVC) -> Unit,
) = FeatureAccessor(
    jsonKey = jsonKey,
    set = setter,
    getAsInt = getter,
    getForJson = getter,
    reset = resetter,
)

private val featureRegistry: Map<String, FeatureAccessor> = linkedMapOf(
    "autoexposure" to FeatureAccessor(
        "autoExposure",
        set = { c, v -> c.setAutoExposure(v > 0) },
        getAsInt = { c -> if (c.getAutoExposure() == true) 1 else 0 },
        getForJson = { c -> c.getAutoExposure() == true },
        reset = { c -> c.setAutoExposure(true) },
    ),
    "exposuremode" to intFeature("exposureMode", CameraUVC::setExposureMode, CameraUVC::getExposureMode, CameraUVC::resetExposureMode),
    "exposurepriority" to intFeature("exposurePriority", CameraUVC::setExposurePriority, CameraUVC::getExposurePriority, CameraUVC::resetExposurePriority),
    "exposure" to intFeature("exposure", CameraUVC::setExposure, CameraUVC::getExposure, CameraUVC::resetExposure),
    "autofocus" to FeatureAccessor(
        "autoFocus",
        set = { c, v -> c.setAutoFocus(v > 0) },
        getAsInt = { c -> if (c.getAutoFocus() == true) 1 else 0 },
        getForJson = { c -> c.getAutoFocus() == true },
        reset = { c -> c.resetAutoFocus() },
    ),
    "autowhitebalance" to FeatureAccessor(
        "autoWhiteBalance",
        set = { c, v -> c.setAutoWhiteBalance(v > 0) },
        getAsInt = { c -> if (c.getAutoWhiteBalance() == true) 1 else 0 },
        getForJson = { c -> c.getAutoWhiteBalance() == true },
        reset = { c -> c.setAutoWhiteBalance(true) },
    ),
    "zoom" to intFeature("zoom", CameraUVC::setZoom, CameraUVC::getZoom, CameraUVC::resetZoom),
    "brightness" to intFeature("brightness", CameraUVC::setBrightness, CameraUVC::getBrightness, CameraUVC::resetBrightness),
    "contrast" to intFeature("contrast", CameraUVC::setContrast, CameraUVC::getContrast, CameraUVC::resetContrast),
    "saturation" to intFeature("saturation", CameraUVC::setSaturation, CameraUVC::getSaturation, CameraUVC::resetSaturation),
    "sharpness" to intFeature("sharpness", CameraUVC::setSharpness, CameraUVC::getSharpness, CameraUVC::resetSharpness),
    "gain" to intFeature("gain", CameraUVC::setGain, CameraUVC::getGain, CameraUVC::resetGain),
    "gamma" to intFeature("gamma", CameraUVC::setGamma, CameraUVC::getGamma, CameraUVC::resetGamma),
    "hue" to intFeature("hue", CameraUVC::setHue, CameraUVC::getHue, CameraUVC::resetHue),
)

class CameraFeaturesManager {
    companion object {
        private val gson = Gson()
    }

    fun applyCameraFeature(camera: CameraUVC?, feature: String, value: Int): Boolean {
        if (camera == null) return false
        val accessor = featureRegistry[feature.lowercase()] ?: return false
        accessor.set(camera, value)
        return true
    }

    fun resetCameraFeature(camera: CameraUVC?, feature: String): Boolean {
        if (camera == null) return false
        val accessor = featureRegistry[feature.lowercase()] ?: return false
        accessor.reset(camera)
        return true
    }

    fun getCameraFeature(camera: CameraUVC?, feature: String): Int? {
        if (camera == null) return null
        val accessor = featureRegistry[feature.lowercase()] ?: return null
        return accessor.getAsInt(camera)
    }

    fun getAllCameraFeatures(camera: CameraUVC?): String {
        if (camera == null) return "{}"
        val result = linkedMapOf<String, Any?>()
        for ((_, accessor) in featureRegistry) {
            result[accessor.jsonKey] = accessor.getForJson(camera)
        }
        return gson.toJson(result)
    }
}
