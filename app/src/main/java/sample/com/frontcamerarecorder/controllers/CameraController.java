package sample.com.frontcamerarecorder.controllers;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * Created by Sylvain on 26/11/2016.
 */

public class CameraController {
    private final static String TAG = "CameraHelper";

    public interface CameraRecordListener {
        void onSuccess(File file);
        void onFailure();
    }

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording;
    private Context mContext;

    private static Camera GetFrontCameraInstance() {
        Camera c = null;
        int cameraId = GetCameraId();
        try {
            c = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return c; // returns null if camera is unavailable
    }

    private static int GetCameraId() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int count = Camera.getNumberOfCameras();

        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return -1;
    }


    public CameraController(Context ctx){
        this.mContext = ctx;
        mCamera = GetFrontCameraInstance();
        isRecording = false;
    }

    public void record(int duration,final CameraRecordListener handler){
        final File output = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        if (prepareVideoRecorder(output)) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.start();

            // inform the user that recording has started
            isRecording = true;
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            if (isRecording) {
                                // stop recording and release camera
                                mMediaRecorder.stop();  // stop the recording
                                releaseMediaRecorder(); // release the MediaRecorder object
                                mCamera.lock();         // take camera access back from MediaRecorder
                                isRecording = false;
                                handler.onSuccess(output);
                            }
                        }
                    },
                    duration);

        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            handler.onFailure();
            // inform user
        }

    }

    public void release(){
        this.releaseMediaRecorder();
        this.releaseCamera();
    }

    private boolean prepareVideoRecorder(File output) {

        mMediaRecorder = new MediaRecorder();

        // store the quality profile required
        CamcorderProfile profile = CamcorderProfile.get(CameraController.GetCameraId(), CamcorderProfile.QUALITY_HIGH);

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setOutputFormat(profile.fileFormat);
        mMediaRecorder.setVideoEncoder(profile.videoCodec);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(output.toString());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mContext.getFilesDir().getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mContext.getFilesDir().getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    public Camera getCamera() {
        return mCamera;
    }
}
