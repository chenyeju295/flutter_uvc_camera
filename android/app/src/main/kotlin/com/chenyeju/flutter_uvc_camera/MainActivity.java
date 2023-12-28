package com.chenyeju.flutter_uvc_camera;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chenyeju.flutter_uvc_camera.usbvideo.IButtonCallback;
import com.chenyeju.flutter_uvc_camera.usbvideo.IDeviceInfoCallback;
import com.chenyeju.flutter_uvc_camera.usbvideo.IKeyCallback;
import com.chenyeju.flutter_uvc_camera.usbvideo.IPictureCallback;
import com.chenyeju.flutter_uvc_camera.usbvideo.USBCameraSDK;
import com.chenyeju.flutter_uvc_camera.uvc_camera.CustomTextureView;
import com.chenyeju.flutter_uvc_camera.uvc_camera.PictureSaver;
import com.chenyeju.flutter_uvc_camera.uvc_camera.PictureSaverCallback;
import com.chenyeju.flutter_uvc_camera.uvc_camera.PictureSaverInfo;
import com.chenyeju.flutter_uvc_camera.uvc_camera.Thumbnail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.flutter.embedding.android.FlutterActivity;

public class MainActivity extends AppCompatActivity {
    // 存储路径
    public static final String DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    // LED灯曝光稳定延时时间, 单位毫秒
    private static final int CTRLLED_DELAY_TIME = 5000;

    // 视频支持的格式，目前只支持MJPEG
    public static final int VIDEOFORMAT_MJPG = 0;

    // 影像预览格式及分辨率
    private int previewFormat = VIDEOFORMAT_MJPG;
    private int previewW;
    private int previewH;

    // 解码模式
    private int mDecodecMode = 0;   // 0: libjpeg mode; 1: libjpeg-turbo mode (0通用; 1效率高，但某些设备不适配)

    // 显示模式
    private int mDisplayMode = 0;   // 0: normal mode; 1: gpu mode (0通用; 1效率高，但某些设备不适配)

    // 选项
    private int m_choosed_option;
    private int m_dsp_adr;
    private byte m_min_val;
    private byte m_max_val;
    private byte m_adr;

    // 是否预览标记
    private boolean isPreview = false;

    // 其他变量
    private ContentResolver mContentResolver;
    PictureSaver mPictureSaver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setOptionValue(10);
        m_adr = m_min_val;

        initViews();
        requestPermission();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        mPictureSaver = new PictureSaver(mPictureSaverCallback);
        mContentResolver = getContentResolver();
    }

    @Override
    protected void onDestroy() {
        // 停止保存图像
        if (mPictureSaver != null) {
            mPictureSaver.finish();
            mPictureSaver = null;
        }

        // 取消消息接收器
        unregisterReceiver(mUsbReceiver);

        // 退出时关闭摄像头
        closeCamera();

        super.onDestroy();
    }



    ///
    /// 初始化控件
    ///
    private CustomTextureView mTextureView;
    private ImageView mImageViewThumb;
    private Button mBtnPreview;
    private Button mBtnSnapshot;
    private Button mBtnOption;
    private Button mBtnRead;
    private Button mBtnInfo;
    private ProgressBar mProgressBar;
    private TextView mTextViewReg;

    private void initViews()
    {
        mTextureView = (CustomTextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mTextureView.setAspectRatio(previewW, previewH);    // 注意：gpu 显示模式时，有些设备不支持onMeasure接口，需要屏蔽掉
        //mTextureView.setFullScreen();

        mImageViewThumb = (ImageView) findViewById(R.id.imageview_thumb);
        mImageViewThumb.setOnClickListener(mClickListener);

        mBtnPreview = (Button) findViewById(R.id.button_preview);
        mBtnPreview.setOnClickListener(mClickListener);

        mBtnSnapshot = (Button) findViewById(R.id.button_snapshot);
        mBtnSnapshot.setOnClickListener(mClickListener);


        mBtnRead = (Button) findViewById(R.id.button_read);
        mBtnRead.setOnClickListener(mClickListener);

        mBtnInfo = (Button) findViewById(R.id.button_info);
        mBtnInfo.setOnClickListener(mClickListener);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setMax(4);

        mTextViewReg = (TextView) findViewById(R.id.textViewReg);
    }

    ///
    /// surface 回调处理
    ///
    Surface mSurface;
    TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            mSurface = new Surface(surface);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            mSurface = null;
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    ///
    /// 点击事件处理
    ///
    int a = 0;
    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.imageview_thumb:
                    break;

                case R.id.button_preview:
                    if (!isPreview) {
                        openCamera();
                    } else {
                        closeCamera();
                    }
                    break;

                case R.id.button_snapshot:
                    snapshot();
                    break;

                case R.id.button_read:
                    read4bytes();
                    break;

                case R.id.button_info:
                    readInfo();
                    break;
            }
        }
    };

    ///
    /// 打开摄像头
    ///
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbConnection;

    private void openCamera()
    {
        // 这里判定相机、存储、录音等权限
        if (!mHasPermission) {
            Toast.makeText(this, "请先获取其他相关权限", Toast.LENGTH_SHORT).show();
            return;
        }

        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

        // 枚举USB设备
        final HashMap<String, UsbDevice> devMap = mUsbManager.getDeviceList();
        final List<UsbDevice> devList = new ArrayList<UsbDevice>();
        devList.addAll(devMap.values());

        mUsbDevice = null;
        for (int i=0; i<devList.size(); i++) {
            if (devList.get(i).getDeviceClass() == 239) {   // 239表示图像设备，这里获取第一个图像设备，也可以通过vendorId之类获取指定设备
                mUsbDevice = devList.get(i);
                break;
            }
        }

        if (mUsbDevice==null) {
            Toast.makeText(this, "没有摄像头设备", Toast.LENGTH_SHORT).show();
            return;
        }

        // 请求权限
        if (mUsbManager.hasPermission(mUsbDevice)) {
            mHandler.sendEmptyMessage(MSG_GET_USB_PERMISSION);
        } else {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(mUsbDevice, permissionIntent);
        }
    }

    ///
    /// 关闭摄像头
    ///
    private void closeCamera()
    {
        if (isPreview) {
            USBCameraSDK.setPreviewSurface(null, previewW, previewH, mDisplayMode);
            USBCameraSDK.closeCamera();
            isPreview = false;
        }

        mHandler.removeMessages(MSG_DO_SNAPSHOT);
        mHandler.removeMessages(MSG_KEY_PROCESS);


        if (mUsbConnection!=null) {
            mUsbConnection.close();
            mUsbConnection = null;
        }

        isPreview = false;
        mBtnPreview.setText("预览");
    }

    ///
    /// USB设备消息接收器
    ///
    private static final String ACTION_USB_PERMISSION = "REQUEST_USB_PERMISSION";

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    mHandler.sendEmptyMessage(MSG_GET_USB_PERMISSION);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Toast.makeText(MainActivity.this, "Usb device attached", Toast.LENGTH_SHORT).show();
                //mHandler.sendEmptyMessage(MSG_OPENDEVICE);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(MainActivity.this, "Usb device detached", Toast.LENGTH_SHORT).show();
                mHandler.sendEmptyMessage(MSG_CLOSEDEVICE);
            }
        }
    };

    ///
    /// 获取到USB设备权限后，继续USB设备打开操作
    ///
    private void openCameraContinue()
    {

        mUsbConnection = mUsbManager.openDevice(mUsbDevice);
        if (mUsbConnection == null) {
            Toast.makeText(this, "打开USB设备错误", Toast.LENGTH_SHORT).show();
            return;
        }
        int fd = mUsbConnection.getFileDescriptor();

        // 该函数需要在openCamera前调用
        USBCameraSDK.setPreviewSurface(mSurface, previewW, previewH, mDisplayMode);


        if (USBCameraSDK.openCamera(fd, previewFormat, previewW, previewH, m_key_enable, mDecodecMode) < 0) {
            mUsbConnection.close();
            mUsbConnection = null;
            Toast.makeText(this, "打开摄像头错误", Toast.LENGTH_SHORT).show();
            return;
        }

        if (m_key_enable > 0) {
            enableKeyFunction();
        }

        // (方式一，通过UVC返回的button数据方式)
        USBCameraSDK.setButtonCallback(new IButtonCallback() {
            @Override
            public void onButton(final int button, final int state) {


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Toast toast = Toast.makeText(MainActivity.this, "onButton(button=" + button + "; " +
                                "state=" + state + ")", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
                if (state == 1) {
                    mHandler.sendEmptyMessage(MSG_KEY_PROCESS);
                }
            }
        });

        isPreview = true;

        mBtnPreview.setText("停止");
    }

    ///
    /// 请求相机，录音，存储等权限
    ///
    private static int REQUEST_PERMISSION = 100;

    private String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private List<String> mNoPermissionList = new ArrayList<>();
    private boolean mHasPermission = false;

    private void requestPermission() {

        mHasPermission = false;
        mNoPermissionList.clear();


        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mNoPermissionList.add(permissions[i]);
            }
        }

        if (mNoPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
        } else {
            afterGetPermissions();
        }
    }

    ///
    /// 权限请求回调处理函数
    ///
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        boolean PermissionDeined = false;

        if (requestCode == REQUEST_PERMISSION) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    PermissionDeined = true;
                    break;
                }
            }
        }
        if (!PermissionDeined) {
            afterGetPermissions();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    ///
    /// 设置权限标记
    ///
    private void afterGetPermissions() {
        mHasPermission = true;
    }

    ///
    /// 消息处理
    ///
    private static final int MSG_GET_USB_PERMISSION = 1;
    private static final int MSG_STOREIMAGE_ERR = 2;
    private static final int MSG_STOREIMAGE_SUCCESS = 3;
    private static final int MSG_DO_SNAPSHOT = 4;
    private static final int MSG_SHOW_DEVINFO = 5;
    private static final int MSG_KEY_PROCESS = 6;
    private static final int MSG_CLOSEDEVICE = 7;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {

                case MSG_GET_USB_PERMISSION:    // 获取到USB权限，继续打开摄像头
                    openCameraContinue();
                    break;


                case MSG_STOREIMAGE_ERR:        // 存储错误
                    snapshot_release(1);
                    break;

                case MSG_STOREIMAGE_SUCCESS:    // 存储成功

                    // 图片信息插入媒体库
                    PictureSaverInfo info = (PictureSaverInfo)msg.obj;
                    Uri uri = insertImageToMediaStore(mContentResolver, info.title, info.pathname, info.dateTaken, info.size, info.w, info.h);
                    if (uri == null) {
                        Toast.makeText(MainActivity.this, "图片插入媒体库错误", Toast.LENGTH_SHORT).show();
                        snapshot_release(1);
                        return;
                    }
                    sendBroadcast(new Intent(Camera.ACTION_NEW_PICTURE, uri));
                    sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));

                    // 判定当前图片编号
                    if (m_adr == m_max_val) {    // 所有图片保存完成
                        mProgressBar.setProgress(6);

                        // 创建最后一张的缩略图
                        int minW = info.w < info.h ? info.w : info.h;
                        int layoutW = (int)getResources().getDimension(R.dimen.toolbar_item_w);
                        int inSampleSize = Thumbnail.calcImSampleSize(minW, layoutW);
                        Bitmap thumbnail1 = Thumbnail.createThumbnailBitmap(info.pathname, inSampleSize);
                        Bitmap thumbnail2 = Thumbnail.createSquareBitmap(thumbnail1);
                        if (thumbnail1 == null) {
                            Toast.makeText(MainActivity.this, "创建缩略图错误", Toast.LENGTH_SHORT).show();
                            snapshot_release(1);
                            return;
                        }
                        mImageViewThumb.setImageBitmap(thumbnail2);

                        // 完成
                        snapshot_release(0);

                    } else {
                        int val = m_adr-m_min_val+1;
                        mProgressBar.setProgress(val);

                        // 控制灯光
                        m_adr += 1;
                        ctrlLed(m_adr, (byte)0xff);

                        // 延时一段时间后开始截图
                        mHandler.sendEmptyMessageDelayed(MSG_DO_SNAPSHOT, CTRLLED_DELAY_TIME);
                    }
                    break;

                case MSG_DO_SNAPSHOT:       // 继续拍照动作
                    snapshot_continue();
                    break;

                case MSG_SHOW_DEVINFO:      // 显示设备信息
                    showDevInfoWindow(mBtnInfo, (DevInfo)msg.obj);
                    break;

                case MSG_KEY_PROCESS:       // 处理按键
                    processKey();
                    break;

                case MSG_CLOSEDEVICE:
                    closeCamera();
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    };

    ///
    /// 拍照
    ///

    PictureSaverCallback mPictureSaverCallback = new PictureSaverCallback() {
        @Override
        public void onMessage(int msg, PictureSaverInfo info) {
            switch (msg) {
                case PictureSaverCallback.MESSAGE_STOREIMAGE_ERR:
                    mHandler.sendEmptyMessage(MSG_STOREIMAGE_ERR);
                    break;
                case PictureSaverCallback.MESSAGE_STOREIMAGE_SUCCESS:
                    mHandler.obtainMessage(MSG_STOREIMAGE_SUCCESS, info).sendToTarget();
                    break;
                default:
                    break;
            }
        }
    };





    ///
    /// 插入图像到MediaStore媒体库
    ///
    public Uri insertImageToMediaStore(ContentResolver resolver, String title, String path, long dateTaken, int size, int w, int h)
    {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.ImageColumns.DATA, path);
        values.put(MediaStore.Images.ImageColumns.SIZE, size);
        values.put(MediaStore.Images.ImageColumns.WIDTH, w);
        values.put(MediaStore.Images.ImageColumns.HEIGHT, h);

        Uri uri = null;
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th) {

        }
        return uri;
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// 拍照
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///
    /// 生成文件名
    ///
    public String generateTitle(long dateTaken, int index)
    {
        Date date = new Date(dateTaken);
        SimpleDateFormat format = new SimpleDateFormat("'IMG'_yyyyMMddHHmmss_");
        String title = format.format(date) + Integer.toHexString(index);

        return title;
    }

    ///
    /// LED控制
    ///
    void ctrlLed(byte adr, byte val)
    {
        byte [] pdat = new byte[4];
        pdat[0] = 0x0;
        pdat[1] = 0x78;
        pdat[2] = adr;
        pdat[3] = val;
        USBCameraSDK.dspRegW(m_dsp_adr, pdat, 4);
    }

    ///
    /// 开始拍照功能
    ///
    private void snapshot()
    {
        if (!isPreview)
            return;

        // 设置标记，禁用按键
        mProgressBar.setProgress(0);
        mBtnSnapshot.setEnabled(false);

        // 设置回调函数
        USBCameraSDK.setPictureCallback(mPictureCallback);

        // 控制灯光
        m_adr = m_min_val;
        ctrlLed(m_adr, (byte)0xff);

        // 延时一段时间后开始截图
        mHandler.sendEmptyMessageDelayed(MSG_DO_SNAPSHOT, CTRLLED_DELAY_TIME);

    }

    ///
    /// 继续拍照功能
    ///
    void snapshot_continue()
    {
        // 触发拍照
        USBCameraSDK.takePicture();
    }

    ///
    /// 存储完成，释放资源
    ///
    void snapshot_release(int err)
    {
        ctrlLed(m_adr, (byte)0);

        if (err > 0) {
            Toast.makeText(MainActivity.this, "存储图片错误", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "存储图片成功", Toast.LENGTH_SHORT).show();
        }
        USBCameraSDK.setPictureCallback(null);
        mBtnSnapshot.setEnabled(true);

        if (m_key_enable > 0) {
            processKeyFinished();
        }
    }

    ///
    /// 拍照回调函数
    ///
    IPictureCallback mPictureCallback = new IPictureCallback() {
        @Override
        public void onPictureFrame(byte [] pbuf, int size) {
            // 设置参数
            PictureSaverInfo param = new PictureSaverInfo();
            param.dateTaken =  System.currentTimeMillis();
            param.title = generateTitle(param.dateTaken, m_adr);
            param.name = param.title + ".jpg";
            param.pathname = DCIM + "/" + param.name;
            param.w = previewW;
            param.h = previewH;
            param.data = pbuf;
            param.index = m_adr - m_min_val;
            param.size = size;

            // 添加数据到存储线程
            mPictureSaver.addImage(param);  // 注意：这里也可以直接存储图片，不需要用PictureSaver通过缓冲线程来存储
        }
    };

    ///
    /// 显示选项菜单
    ///
    PopupWindow mOptionWindow;

    RadioButton mBtnOption0;
    RadioButton mBtnOption1;
    RadioButton mBtnOption2;
    RadioButton mBtnOption3;
    RadioButton mBtnOption4;
    RadioButton mBtnOption5;
    RadioButton mBtnOption6;
    RadioButton mBtnOption7;
    RadioButton mBtnOption8;
    RadioButton mBtnOption9;
    RadioButton mBtnOption10;
    RadioButton mBtnOption11;


    ///
    /// 设置选中项
    ///
    void checkedButton(int choosed) {
        mBtnOption0.setChecked(false);
        mBtnOption1.setChecked(false);
        mBtnOption2.setChecked(false);
        mBtnOption3.setChecked(false);
        mBtnOption4.setChecked(false);
        mBtnOption5.setChecked(false);
        mBtnOption6.setChecked(false);
        mBtnOption7.setChecked(false);
        mBtnOption8.setChecked(false);
        mBtnOption9.setChecked(false);
        mBtnOption10.setChecked(false);
        mBtnOption11.setChecked(false);

        switch (choosed) {
            case 0: mBtnOption0.setChecked(true); break;
            case 1: mBtnOption1.setChecked(true); break;
            case 2: mBtnOption2.setChecked(true); break;
            case 3: mBtnOption3.setChecked(true); break;
            case 4: mBtnOption4.setChecked(true); break;
            case 5: mBtnOption5.setChecked(true); break;
            case 6: mBtnOption6.setChecked(true); break;
            case 7: mBtnOption7.setChecked(true); break;
            case 8: mBtnOption8.setChecked(true); break;
            case 9: mBtnOption9.setChecked(true); break;
            case 10: mBtnOption10.setChecked(true); break;
            case 11: mBtnOption11.setChecked(true); break;
        }
    }

    ///
    /// 选项菜单事件
    ///


    ///
    /// 根据选项设定参数
    ///
    void setOptionValue(int option) {
        m_choosed_option = option;

        switch (option) {
            case 0:
                previewW = 3264;
                previewH = 2160;
                m_dsp_adr = 0xd816;
                m_min_val = 0x10;
                m_max_val = 0x17;
                break;

            case 1:
                previewW = 3264;
                previewH = 2160;
                m_dsp_adr = 0xd816;
                m_min_val = 0x10;
                m_max_val = 0x15;
                break;

            case 2:
                previewW = 3264;
                previewH = 2160;
                m_dsp_adr = 0xd816;
                m_min_val = 0x10;
                m_max_val = 0x14;
                break;

            case 3:
                previewW = 2592;
                previewH = 1944;
                m_dsp_adr = 0xd816;
                m_min_val = 0x10;
                m_max_val = 0x14;
                break;

            case 4:
                previewW = 3264;
                previewH = 2160;
                m_dsp_adr = 0xd70c;
                m_min_val = 0x10;
                m_max_val = 0x14;
                break;

            case 5:
                previewW = 3264;
                previewH = 2448;
                m_dsp_adr = 0xd7df;
                m_min_val = 0x10;
                m_max_val = 0x15;
                break;

            case 6:
                previewW = 2592;
                previewH = 1944;
                m_dsp_adr = 0xc6d9;
                m_min_val = 0x10;
                m_max_val = 0x15;
                break;

            case 7:
                previewW = 1920;
                previewH = 1080;
                m_dsp_adr = 0xd55b;
                m_min_val = 0x10;
                m_max_val = 0x13;
                break;

            case 8:
                previewW = 1280;
                previewH = 960;
                m_dsp_adr = 0xd55b;
                m_min_val = 0x10;
                m_max_val = 0x13;
                break;

            case 9:
                previewW = 1280;
                previewH = 720;
                m_dsp_adr = 0xd55b;
                m_min_val = 0x10;
                m_max_val = 0x13;
                break;

            case 10:
                previewW = 640;
                previewH = 480;
                m_dsp_adr = 0xd55b;
                m_min_val = 0x10;
                m_max_val = 0x13;
                break;

            case 11:
                previewW = 640;
                previewH = 480;
                m_dsp_adr = 0xd55b;
                m_min_val = 0x10;
                m_max_val = 0x12;
                break;
        }
    }

    ///
    /// 读4字节数据
    ///
    void read4bytes()
    {
        if (!isPreview)
            return;

        byte [] pcmd = new byte[4];
        pcmd[0] = 0x01;
        pcmd[1] = 0x78;
        pcmd[2] = 0x79;
        pcmd[3] = 0x00;
        USBCameraSDK.dspRegW(m_dsp_adr, pcmd, 4);
        try {
            Thread.sleep(100);
        } catch (Exception ex) {

        }
        byte [] pdat = new byte[4];
        USBCameraSDK.dspRegR(m_dsp_adr+1, pdat, 4);

        String str = String.format("%02x %02x %02x %02x",(pdat[0]&0xff),(pdat[1]&0xff),(pdat[2]&0xff),(pdat[3]&0xff));
        mTextViewReg.setText(str);
    }

    ///
    /// 读4字节数据
    ///
    void readInfo()
    {
        // for 64K flash, the address is 0xEE00, 0xFE00
        // for 128K flash, the address is 0xAE00, 0x1FE00
        USBCameraSDK.getDeviceInfo(mDeviceInfoCallback, 0xAE00, 0x1FE00);
    }

    ///
    /// 设备信息类
    ///
    class DevInfo {
        public int vid;
        public int pid;
        public int rev;
        public String manufactor;
        public String product;
        public String serialno;
        public String devicename;
        public String customcode;
    }

    ///
    /// 设备消息回调
    ///
    IDeviceInfoCallback mDeviceInfoCallback = new IDeviceInfoCallback() {
        @Override
        public void onDeviceInfo(int vid, int pid, int rev, String manufactor, String product, String serialno, String devicename, String customcode) {
            DevInfo info = new DevInfo();
            info.vid = vid;
            info.pid = pid;
            info.rev = rev;
            info.manufactor = manufactor;
            info.product = product;
            info.serialno = serialno;
            info.devicename = devicename;
            info.customcode = customcode;

            Message msg = mHandler.obtainMessage();
            msg.what = MSG_SHOW_DEVINFO;
            msg.obj = info;
            msg.sendToTarget();
        }
    };

    ///
    /// 显示设备信息菜单
    ///
    PopupWindow mDevInfoWindow;

    TextView mTextViewVid;
    TextView mTextViewPid;
    TextView mTextViewRev;
    TextView mTextViewManufactor;
    TextView mTextViewProduct;
    TextView mTextViewSerialNo;
    TextView mTextViewDeviceName;
    TextView mTextViewCustomCode;


    void showDevInfoWindow(View view, DevInfo info)
    {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(
                R.layout.layout_info, null);

        mTextViewVid = (TextView) layout.findViewById(R.id.textView_vid);
        mTextViewPid = (TextView) layout.findViewById(R.id.textView_pid);
        mTextViewRev = (TextView) layout.findViewById(R.id.textView_rev);
        mTextViewManufactor = (TextView) layout.findViewById(R.id.textView_manufacture);
        mTextViewProduct = (TextView) layout.findViewById(R.id.textView_product);
        mTextViewSerialNo = (TextView) layout.findViewById(R.id.textView_serialno);
        mTextViewDeviceName = (TextView) layout.findViewById(R.id.textView_devicename);
        mTextViewCustomCode = (TextView) layout.findViewById(R.id.textView_customcode);

        mTextViewVid.setText(String.format("0x%04x", info.vid));
        mTextViewPid.setText(String.format("0x%04x", info.pid));
        mTextViewRev.setText(String.format("0x%04x", info.rev));
        mTextViewManufactor.setText(info.manufactor);
        mTextViewProduct.setText(info.product);
        mTextViewSerialNo.setText(info.serialno);
        mTextViewDeviceName.setText(info.devicename);
        mTextViewCustomCode.setText(info.customcode);

        layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int h = layout.getMeasuredHeight();

        mDevInfoWindow = new PopupWindow(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        mDevInfoWindow.setContentView(layout);
        mDevInfoWindow.setFocusable(true);
        mDevInfoWindow.setOutsideTouchable(true);

        int [] location= new int[2];
        mProgressBar.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1] - h -10;

        mDevInfoWindow.showAtLocation(view, Gravity.CENTER_HORIZONTAL|Gravity.TOP, 0, y);




    }

    ///
    /// 按键寄存器地址
    ///
    int m_key_adr = 0xd72a; // 不同机种的地址会不同，和供应商联系以确定地址
    int m_key_processed = 1;
    int m_key_enable = 0;   // 禁止通过线程读取按键状态(拍照按键用setButtonCallback的方式，其他特殊按键才需要用读寄存器的方式，且需要firmware配合)

    ///
    /// 启用按键回调
    ///
    void enableKeyFunction()
    {
        // 读取按键状态间隔时间， 最小为100ms（小于100时，内部强制设为100）
        USBCameraSDK.setKeyCallback(mKeyCallback, m_key_adr, 200);
    }

    ///
    /// 回调处理
    ///
    IKeyCallback mKeyCallback = new IKeyCallback() {
        @Override
        public void onKeyStatus(int state) {
            if (state == 1) {
                if (m_key_processed > 0) {   // 等待上一个操作处理完成，再继续进行处理，否则丢弃
                    m_key_processed = 0;

                    mHandler.sendEmptyMessage(MSG_KEY_PROCESS);
                }
            }
        }
    };

    ///
    /// 清空按键状态
    ///
    void clearKey() {
        byte [] pdat = new byte[1];
        pdat[0] = 0;
        USBCameraSDK.dspRegW(m_key_adr, pdat, 1);
    }

    ///
    /// 处理按键
    ///
    void processKey() {

        // 拍照
        snapshot();
    }

    ///
    /// 按键处理完成
    ///
    void processKeyFinished() {
        // 清空底层按键状态
        clearKey();
        // 重置处理标记
        m_key_processed = 1;
    }
}
