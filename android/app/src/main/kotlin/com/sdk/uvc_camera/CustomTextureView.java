package com.sdk.uvc_camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

public class CustomTextureView extends TextureView {
    private double mAspectRatio;
    private boolean mIsFullscreen;

    public CustomTextureView(Context context) {
        this(context, null);
    }

    public CustomTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAspectRatio = 0.0;
        mIsFullscreen = true;
    }

    ///
    /// 按比例显示
    ///
    public void setAspectRatio(int w, int h) {

        double ratio = w / (double)h;
        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
        }
        mIsFullscreen = false;
    }


    ///
    /// 全屏显示
    ///
    public void setFullScreen() {
        mAspectRatio = 0.0;
        requestLayout();
        mIsFullscreen = true;
    }

    ///
    /// 是否是全屏状态
    ///
    public boolean isFullScreen() {
        return mIsFullscreen;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int previewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int previewHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (mAspectRatio > 0.0) {

            // Get the padding of the border background.
            int hPadding = getPaddingLeft() + getPaddingRight();
            int vPadding = getPaddingTop() + getPaddingBottom();

            // Resize the preview frame with correct aspect ratio.
            previewWidth -= hPadding;
            previewHeight -= vPadding;

            double ratio = previewWidth / (double)previewHeight;
            int newW, newH;
            if (ratio < mAspectRatio) {
                newW = previewWidth;
                newH = (int)(newW / mAspectRatio);
            } else {
                newH = previewHeight;
                newW = (int)(newH * mAspectRatio);
            }

            previewWidth = newW;
            previewHeight = newH;

            // Add the padding of the border.
            previewWidth += hPadding;
            previewHeight += vPadding;

        }

        // Ask children to follow the new preview dimension.
        super.onMeasure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
    }

    public View getView() {
        return this;
    }
}
