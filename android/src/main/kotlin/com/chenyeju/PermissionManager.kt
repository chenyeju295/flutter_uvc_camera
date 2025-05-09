package com.chenyeju

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker

/**
 * Manages camera permissions
 */
class PermissionManager {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1230
        
        /**
         * Check if camera and storage permissions are granted
         */
        fun hasRequiredPermissions(context: Context): Boolean {
            val hasCameraPermission = PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            )
            val hasStoragePermission = PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            return hasCameraPermission == PermissionChecker.PERMISSION_GRANTED
                && hasStoragePermission == PermissionChecker.PERMISSION_GRANTED
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
            
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        
        /**
         * Check if permission result is successful
         */
        fun isPermissionGranted(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
            if (requestCode != PERMISSION_REQUEST_CODE) {
                return false
            }
            
            return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
        
        /**
         * Get permission request code
         */
        fun getPermissionRequestCode() = PERMISSION_REQUEST_CODE
    }
} 