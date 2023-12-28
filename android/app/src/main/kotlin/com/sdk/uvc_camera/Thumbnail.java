package com.sdk.uvc_camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;

import java.io.IOException;

public class Thumbnail {
    ///
    /// 计算缩略尺寸
    ///
    public static int calcImSampleSize(int imageW, int layoutW)
    {
        int ratio = (int)Math.ceil((double)imageW/layoutW);
        int inSampleSize = Integer.highestOneBit(ratio);
        return inSampleSize;
    }

    ///
    /// 创建缩略图
    ///
    public static Bitmap createThumbnailBitmap(String path, int imSampleSize)
    {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = imSampleSize;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    ///
    /// 创建方形缩略图
    ///
    public static Bitmap createSquareBitmap(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int r = Math.min(w, h);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        Bitmap newBitmap = Bitmap.createBitmap(r, r, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(newBitmap);
        canvas.drawRect(0,0,r,r, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect srcRect = new Rect((w-r)/2,(h-r)/2,(w-r)/2+r,(h-r)/2+r);
        Rect dstRect = new Rect(0,0,r,r);

        canvas.drawBitmap(bmp, srcRect, dstRect, paint);
        return newBitmap;
    }

    ///
    /// 从视频创建缩略位图
    ///
    public static Bitmap createVideoThumbnail(String filePath, int targetWidth) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(-1);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException | IOException ex) {
                // Ignore failures while cleaning up.
            }
        }
        if (bitmap == null) return null;

        // Scale down the bitmap if it is bigger than we need.
        float scale = 1;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int neww, newh;
        if (w > h) {
            newh = targetWidth;
            neww = w * newh / h;
        } else {
            neww = targetWidth;
            newh = h * neww / w;
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, neww, newh, true);
        return bitmap;
    }
}
