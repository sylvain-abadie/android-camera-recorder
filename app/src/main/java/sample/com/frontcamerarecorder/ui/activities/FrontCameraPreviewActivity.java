package sample.com.frontcamerarecorder.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;

import sample.com.frontcamerarecorder.R;
import sample.com.frontcamerarecorder.controllers.CameraController;
import sample.com.frontcamerarecorder.controllers.ffmpegvideogenerator.FFMpegVideoGenerator;
import sample.com.frontcamerarecorder.ui.views.FrontCameraSurfaceView;

public class FrontCameraPreviewActivity extends AppCompatActivity implements CameraController.CameraRecordListener, FFMpegVideoGenerator.VideoGeneratorListener {
    public final static String TAG = "FrontCameraPreviewAct";

    private final static int CAMERA_PERMISSION_REQUEST_CODE = 50;

    private FrontCameraSurfaceView mPreview;
    private FrameLayout mPreviewFrame;
    private FloatingActionButton fab;
    private Animation hideCameraFab;
    private CameraController mCameraController;
    private ProgressBar mPbProcessing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_camera_preview);

        fab = (FloatingActionButton) findViewById(R.id.btn_record);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.startAnimation(hideCameraFab);
            }
        });

        hideCameraFab = AnimationUtils.loadAnimation(getApplication(), R.anim.fade_out);
        hideCameraFab.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mCameraController.record();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mPbProcessing = (ProgressBar) findViewById(R.id.pb_processing);
        mPreviewFrame = (FrameLayout) findViewById(R.id.fl_camera_preview);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    this.initCamera();

                } else {
                    // TODO : display an error view
                }
            }
        }
    }

    @Override
    protected void onResume() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initCamera();
        }
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraController != null) {
            mCameraController.release();
            mCameraController = null;
        }
    }

    private void initCamera() {
        if (mCameraController == null) {
            mCameraController = new CameraController(this);
            mCameraController.setCameraRecordListener(this);

            if (mCameraController.getCamera() == null) {
                Toast.makeText(this, R.string.camera_not_available, Toast.LENGTH_SHORT).show();
                // TODO : display an error view
            } else if (mPreview == null) {
                mPreview = new FrontCameraSurfaceView(this, mCameraController.getCamera(), CameraController.getFrontCameraInfo());
                mPreviewFrame.addView(mPreview);
            } else {
                // handle the onResume after background properly
                mPreview.setCamera(mCameraController.getCamera());
            }
        } else {
            mCameraController.getCamera();
        }
    }


    @Override
    public void onCameraRecordSuccess(final File file) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fab.setVisibility(View.GONE);
                mCameraController.release();
                mPreviewFrame.removeAllViews();
                mCameraController = null;
                mPbProcessing.setVisibility(View.VISIBLE);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "start");
                FFMpegVideoGenerator generator = new FFMpegVideoGenerator(FrontCameraPreviewActivity.this.getApplication());
                generator.setVideoGeneratorListener(FrontCameraPreviewActivity.this);
                generator.convert(file);
            }
        }).start();

    }

    @Override
    public void onCameraRecordFailure() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FrontCameraPreviewActivity.this, R.string.camera_not_available, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onVideoGenerated(String message, File generatedFile) {
        Intent intent = new Intent(FrontCameraPreviewActivity.this, VideoPreviewActivity.class);
        intent.putExtra(VideoPreviewActivity.VIDEO_PATH, generatedFile.getAbsolutePath());
        startActivity(intent);
        recreate();
    }

    @Override
    public void onVideoGeneratedError(String message) {
        Log.e(TAG, message);
    }
}
