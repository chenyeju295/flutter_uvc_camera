package com.sdk.usbvideo;



public interface IDeviceInfoCallback {
    void onDeviceInfo(int vid, int pid, int rev, String manufactor, String product, String serialno, String devicename, String customcode);
}