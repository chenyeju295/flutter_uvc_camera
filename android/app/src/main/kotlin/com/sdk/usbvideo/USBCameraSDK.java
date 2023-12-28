package com.sdk.usbvideo;

import android.view.Surface;

public class USBCameraSDK {
    static {
        System.loadLibrary("usbvideo1.6");
    }

    public static native int openCamera(final int fd, int format, int w, int h, int keythread, int decodecmode);
    public static native void closeCamera();
    public static native void setPreviewSurface(final Surface surface, int w, int h, int displaymode);
    public static native void setPreviewCallback(final IPreviewCallback cb);
    public static native void setPictureCallback(final IPictureCallback cb);
    public static native void takePicture();
    public static native int getPUPropertyInfo(final long ctrlId, int[] info);
    public static native int getPUPropertyValue(final long ctrlId);
    public static native int setPUPropertyValue(final long ctrlId, final int value);
    public static native int getITPropertyInfo(final long ctrlId, int[] info);
    public static native int getITPropertyValue(final long ctrlId);
    public static native int setITPropertyValue(final long ctrlId, final int value);
    public static native void dspRegW(int addr, byte[] pval, int len);
    public static native void dspRegR(int addr, byte[] pval, int len);
    public static native void sensorRegW(int addr, byte[] pval, int len);
    public static native void sensorRegR(int addr, byte[] pval, int len);
    public static native void setKeyCallback(final IKeyCallback cb, int keyAddr, int interval);
    public static native void getDeviceInfo(final IDeviceInfoCallback cb, int addr1, int addr2);
    public static native int setButtonCallback(final IButtonCallback callback);
}
