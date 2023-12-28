package com.chenyeju.flutter_uvc_camera.uvc_camera;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class PictureSaver extends Thread {

    // 队列长度
    private static final int QUEUE_LIMIT = 6;

    private ArrayList<PictureSaverInfo> mQueue;
    private boolean mStop;

    public PictureSaverCallback mPictureSaverCallback;

    ///
    /// 构造函数
    ///
    public PictureSaver(PictureSaverCallback cb) {   // 主线程调用
        mPictureSaverCallback = cb;
        mQueue = new ArrayList<PictureSaverInfo>();
        start();
    }

    ///
    /// 添加图像数据及参数到队列
    ///
    public void addImage(PictureSaverInfo param) {
        synchronized (this) {
            while (mQueue.size() >= QUEUE_LIMIT) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore.
                }
            }
            mQueue.add(param);
            notifyAll();
        }
    }

    ///
    /// 线程循环处理
    ///
    @Override
    public void run() {
        while (true) {
            PictureSaverInfo r;
            synchronized (this) {
                if (mQueue.isEmpty()) {
                    notifyAll();  // 通知主线程，可以添加数据

                    // 退出保存线程
                    if (mStop) break;

                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                    continue;
                }

                // 获取队列中的项
                r = mQueue.get(0);
            }

            // 存储
            storeImage(r);

            synchronized(this) {
                mQueue.remove(0);
                notifyAll();  // 通知主线程，有空了
            }
        }
    }

    ///
    /// 保存Bitmap
    ///
    public boolean writeBitmap(Bitmap bmp, String path)
    {
        try {
            File file = new File(path);
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean writeJpeg(byte[] pbuf, int size, String path)
    {
        try {
            File file = new File(path);
            FileOutputStream out = new FileOutputStream(file);
            out.write(pbuf, 0, size);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    ///
    /// 存储
    ///
    private void storeImage(PictureSaverInfo r) {

        int msg;
        if (!writeJpeg(r.data, r.size, r.pathname)) {
            msg = PictureSaverCallback.MESSAGE_STOREIMAGE_ERR;
        } else {
            msg =PictureSaverCallback.MESSAGE_STOREIMAGE_SUCCESS;
        }
        if (mPictureSaverCallback!=null) {
            mPictureSaverCallback.onMessage(msg, r);
        }
    }

    ///
    /// 主线程调用，等待队列空
    ///
    public void waitDone() {
        synchronized (this) {
            while (!mQueue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore.
                }
            }
        }
    }

    // 主线程调用，退出保存线程
    public void finish() {
        waitDone();
        synchronized (this) {
            mStop = true;
            notifyAll();
        }
        try {
            join();
        } catch (InterruptedException ex) {
            // ignore.
        }
    }
}
