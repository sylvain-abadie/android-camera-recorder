package sample.com.frontcamerarecorder.ui.views;

/**
 * Created by Sylvain on 26/11/2016.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

/**
 * A basic Camera preview class
 */
public class FrontCameraSurfaceView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "FrontCameraView";

    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private List<Camera.Size> mSupportedVideoSizes;

    private Camera.Size mPreviewSize;
    private int mDisplayOrientation;
    private Camera.CameraInfo mCameraInfo;

    public FrontCameraSurfaceView(Activity context, Camera camera, Camera.CameraInfo cameraInfo) {
        super(context);
        mCamera = camera;
        mCameraInfo = cameraInfo;
        mDisplayOrientation = context.getWindowManager().getDefaultDisplay().getRotation();

        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        mSupportedVideoSizes = mCamera.getParameters().getSupportedVideoSizes();

        for (Camera.Size str : mSupportedPreviewSizes)
            Log.e(TAG, str.width + "/" + str.height);
        this.setSurfaceTextureListener(this);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        mPreviewSize = getOptimalPreviewSize(width, height);

        // height of the preview is the actual height
        if (mPreviewSize.height >= mPreviewSize.width) {
            setMeasuredDimension(mPreviewSize.width, mPreviewSize.height);
        }
        // height of the preview is the actual width
        else {
            float ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
            int actualWidth = (int) (height * ratio);
            setMeasuredDimension(actualWidth, height);
        }
    }


    private Camera.Size getOptimalPreviewSize(int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        List<Camera.Size> videoSizes;
        if (mSupportedVideoSizes != null) {
            videoSizes = mSupportedVideoSizes;
        } else {
            videoSizes = mSupportedPreviewSizes;
        }
        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = h;

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff && mSupportedPreviewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && mSupportedPreviewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);

            int degrees = 0;
            switch (mDisplayOrientation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            int result;
            if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (mCameraInfo.orientation + degrees) % 360;
                result = (360 - result) % 360; // compensate the mirror
            } else { // back-facing
                result = (mCameraInfo.orientation - degrees + 360) % 360;
            }
            mCamera.setDisplayOrientation(result);
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();

        } catch (IOException ioe) {
            Log.d(TAG, "Error");
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void setCamera(Camera camera) {
        this.mCamera = camera;
    }
}