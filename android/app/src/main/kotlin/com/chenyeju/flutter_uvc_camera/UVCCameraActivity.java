//package com.chenyeju.flutter_uvc_camera;
//
//import android.hardware.usb.UsbDevice;
//import android.net.Uri;
//import android.os.Bundle;
//import android.view.TextureView;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ImageView;
//import android.widget.Spinner;
//
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.lgh.uvccamera.UVCCameraProxy;
//import com.lgh.uvccamera.callback.ConnectCallback;
//import com.lgh.uvccamera.callback.PhotographCallback;
//import com.lgh.uvccamera.callback.PictureCallback;
//import com.lgh.uvccamera.callback.PreviewCallback;
//
//import org.jetbrains.annotations.NotNull;
//
//import java.util.List;
//
//public class UVCCameraActivity extends AppCompatActivity{
//    private static TextureView mTextureView;
//
//    private UVCCameraProxy mUVCCamera;
//    private Spinner mSpinner;
//
//    private String path1;
//    private ImageView mImageView1;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        initView();
//        initUVCCamera();
//    }
//
//
//    private void initView() {
//        mTextureView = findViewById(R.id.texture_container);
//        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });
//    }
//
//    private void initUVCCamera() {
//        mUVCCamera = new UVCCameraProxy(this);
//
//        mUVCCamera.setPreviewTexture(mTextureView);
////        mUVCCamera.setPreviewSurface(mSurfaceView);
////        mUVCCamera.registerReceiver();
//
//        mUVCCamera.setConnectCallback(new ConnectCallback() {
//            @Override
//            public void onAttached(UsbDevice usbDevice) {
//                mUVCCamera.requestPermission(usbDevice);
//            }
//
//            @Override
//            public void onGranted(UsbDevice usbDevice, boolean granted) {
//                if (granted) {
//                    mUVCCamera.connectDevice(usbDevice);
//                }
//            }
//
//            @Override
//            public void onConnected(UsbDevice usbDevice) {
//                mUVCCamera.openCamera();
//            }
//
//            @Override
//            public void onCameraOpened() {
////                showAllPreviewSizes();
//                mUVCCamera.setPreviewSize(640, 480);
//                mUVCCamera.startPreview();
//            }
//
//            @Override
//            public void onDetached(UsbDevice usbDevice) {
//                mUVCCamera.closeCamera();
//            }
//        });
//
//        mUVCCamera.setPhotographCallback(new PhotographCallback() {
//            @Override
//            public void onPhotographClick() {
//                mUVCCamera.takePicture();
////                mUVCCamera.takePicture("test.jpg");
//            }
//        });
//
//        mUVCCamera.setPreviewCallback(new PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] yuv) {
//
//            }
//        });
//
//        mUVCCamera.setPictureTakenCallback(new PictureCallback() {
//            @Override
//            public void onPictureTaken(String path) {
//                path1 = path;
//                mImageView1.setImageURI(null);
//                mImageView1.setImageURI(Uri.parse(path));
//            }
//        });
//    }
//
//    @NotNull
//    public static View getCameraViewContainer() {
//        return  mTextureView.getRootView();
//    }
//}
