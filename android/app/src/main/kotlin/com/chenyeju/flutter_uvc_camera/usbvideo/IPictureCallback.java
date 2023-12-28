package com.chenyeju.flutter_uvc_camera.usbvideo;

public interface IPictureCallback {
    void onPictureFrame(byte [] pbuf, int size);
}
