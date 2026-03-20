package com.chenyeju

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker

/**
 * Manages camera permissions
 */
class PermissionManager {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1230
        private const val AUDIO_PERMISSION_REQUEST_CODE = 1231
        
        /**
         * Check if camera and storage permissions are granted
         */
        fun hasRequiredPermissions(context: Context): Boolean {
            return hasCameraAndStoragePermissions(context) && hasAudioPermission(context)
        }

        /**
         * Check camera + storage permissions for preview/photo only.
         */
        fun hasCameraAndStoragePermissions(context: Context): Boolean {
            val hasCameraPermission = PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            )

            val hasStoragePermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                PermissionChecker.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PermissionChecker.PERMISSION_GRANTED
            } else {
                true
            }

            return hasCameraPermission == PermissionChecker.PERMISSION_GRANTED && hasStoragePermission
        }

        /**
         * Check audio permission for streaming/recording only.
         */
        fun hasAudioPermission(context: Context): Boolean {
            return PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED
        }
        
        /**
         * Request camera and storage permissions
         * @return true if permissions already granted, false if request was made
         */
        fun requestPermissionsIfNeeded(activity: Activity?): Boolean {
            if (activity == null) {
                return false
            }
            
            if (hasRequiredPermissions(activity)) {
                return true
            }

            val permissions = ArrayList<String>()
            permissions.add(Manifest.permission.CAMERA)
            permissions.add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }

        /**
         * Request camera + storage permissions only.
         */
        fun requestCameraAndStoragePermissionsIfNeeded(activity: Activity?): Boolean {
            if (activity == null) {
                return false
            }
            if (hasCameraAndStoragePermissions(activity)) {
                return true
            }
            val permissions = ArrayList<String>()
            permissions.add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }

        /**
         * Request audio permission only.
         */
        fun requestAudioPermissionIfNeeded(activity: Activity?): Boolean {
            if (activity == null) {
                return false
            }
            if (hasAudioPermission(activity)) {
                return true
            }
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
            return false
        }
        
        /**
         * Check if permission result is successful
         */
        fun isPermissionGranted(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
            if (requestCode != PERMISSION_REQUEST_CODE && requestCode != AUDIO_PERMISSION_REQUEST_CODE) {
                return false
            }
            
            return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
        
        /**
         * Get permission request code
         */
        fun getPermissionRequestCode() = PERMISSION_REQUEST_CODE

        fun getAudioPermissionRequestCode() = AUDIO_PERMISSION_REQUEST_CODE
    }
} 
