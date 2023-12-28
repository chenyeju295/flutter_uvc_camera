package com.sdk.usbvideo;

public interface IPictureCallback {
    void onPictureFrame(byte [] pbuf, int size);
}
