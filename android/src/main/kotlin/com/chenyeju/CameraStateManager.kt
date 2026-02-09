package com.chenyeju

import com.chenyeju.VideoStreamHandler

/**
 * Camera state management for UVC cameras
 */
class CameraStateManager(private val videoStreamHandler: VideoStreamHandler) {

    enum class CameraState {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING,
        ERROR
    }

    private var currentState = CameraState.CLOSED
    
    /**
     * Update camera state and notify Flutter
     */
    fun updateState(state: CameraState, message: String? = null) {
        currentState = state
        val stateName = when (state) {
            CameraState.CLOSED -> "CLOSED"
            CameraState.OPENING -> "OPENING"
            CameraState.OPENED -> "OPENED"
            CameraState.CLOSING -> "CLOSING"
            CameraState.ERROR -> "ERROR"
        }
        
        val data = if (message != null) {
            mapOf("msg" to message)
        } else {
            null
        }
        
        videoStreamHandler.sendState(stateName, data)
    }

    /**
     * Check if current camera state allows operation
     */
    fun checkStateForOperation(operation: String): Boolean {
        return when (operation) {
            "open" -> currentState == CameraState.CLOSED
            "close" -> currentState == CameraState.OPENED
            "capture" -> currentState == CameraState.OPENED
            "stream" -> currentState == CameraState.OPENED
            else -> false
        }
    }

    /**
     * Get current camera state
     */
    fun getCurrentState(): CameraState = currentState
    
    /**
     * Check if camera is in error state
     */
    fun isError() = currentState == CameraState.ERROR
    
    /**
     * Check if camera is opened
     */
    fun isOpened() = currentState == CameraState.OPENED
} 