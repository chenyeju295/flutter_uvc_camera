package com.chenyeju.flutter_uvc_camera.usbvideo;



public interface IDeviceInfoCallback {
    void onDeviceInfo(int vid, int pid, int rev, String manufactor, String product, String serialno, String devicename, String customcode);
}