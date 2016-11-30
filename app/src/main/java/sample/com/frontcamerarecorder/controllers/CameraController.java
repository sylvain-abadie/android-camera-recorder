package sample.com.frontcamerarecorder.controllers;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import sample.com.frontcamerarecorder.Constants;

/**
 * Created by Sylvain on 26/11/2016.
 */

public class CameraController {
    private final static String TAG = "CameraController";


    public interface CameraRecordListener {
        void onCameraRecordSuccess(File file);
        void onCameraRecordFailure();
    }

    private CameraRecordListener mCameraRecordListener;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording;
    private Context mContext;

    private static Camera GetFrontCameraInstance() {
        Camera c = null;
        int cameraId = GetFrontCameraId();
        try {
            c = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return c; // returns null if camera is unavailable
    }

    private static int GetFrontCameraId() {
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

    public static Camera.CameraInfo getFrontCameraInfo(){
        Camera.CameraInfo info = new Camera.CameraInfo();
        int count = Camera.getNumberOfCameras();

        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return info;
            }
        }
        return info;
    }

    public CameraController(Context ctx){
        this.mContext = ctx;
        mCamera = GetFrontCameraInstance();
        isRecording = false;
    }

    public void setCameraRecordListener(CameraRecordListener cameraRecordListener){
        this.mCameraRecordListener = cameraRecordListener;
    }

    class RecordedFileObserver extends FileObserver {
        private File output;
        public RecordedFileObserver(File output, int mask) {
            super(output.getAbsolutePath(), mask);
            this.output  = output;
        }

        public void onEvent(int event, String path) {
            if(event == FileObserver.CLOSE_WRITE){
             if(mCameraRecordListener!=null){
                 mCameraRecordListener.onCameraRecordSuccess(output);
             }
            }
        }
    }

    public void record(){

        final File output = getOutputMediaFile();
        if (prepareVideoRecorder(output)) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
                        if (isRecording) {

                            RecordedFileObserver fb = new RecordedFileObserver(output, FileObserver.CLOSE_WRITE);
                            fb.startWatching();

                            // stop recording and release camera
                            mMediaRecorder.stop();  // stop the recording
                            releaseMediaRecorder(); // release the MediaRecorder object
                            mCamera.lock();         // take camera access back from MediaRecorder
                            isRecording = false;
                        }
                    }else{
                        if(mCameraRecordListener!=null){
                            mCameraRecordListener.onCameraRecordFailure();
                        }
                    }
                }
            });
            mMediaRecorder.start();
            isRecording = true;

        } else {
            releaseMediaRecorder();
            if(mCameraRecordListener!=null){
                mCameraRecordListener.onCameraRecordFailure();
            }
        }

    }

    public void release(){
        this.releaseMediaRecorder();
        this.releaseCamera();
    }

    private boolean prepareVideoRecorder(File output) {

        mMediaRecorder = new MediaRecorder();

        // store the quality profile required
        CamcorderProfile profile = CamcorderProfile.get(CameraController.GetFrontCameraId(), CamcorderProfile.QUALITY_HIGH);

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
        // Set the duration
        mMediaRecorder.setMaxDuration(Constants.VIDEO_DURATION);

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

    private File getOutputMediaFile() {
        return new File(mContext.getFilesDir().getPath() + File.separator + Constants.VIDEO_TEMP_NAME);
    }


    public Camera getCamera() {
        return mCamera;
    }
}
