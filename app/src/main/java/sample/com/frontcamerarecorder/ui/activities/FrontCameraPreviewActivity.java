package sample.com.frontcamerarecorder.ui.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;

import sample.com.frontcamerarecorder.Constants;
import sample.com.frontcamerarecorder.R;
import sample.com.frontcamerarecorder.controllers.CameraController;
import sample.com.frontcamerarecorder.controllers.CameraRecordListener;
import sample.com.frontcamerarecorder.ui.views.FrontCameraView;

public class FrontCameraPreviewActivity extends AppCompatActivity {
    public final static String TAG = "FrontCameraPreviewAct";

    private final static int CAMERA_PERMISSION_REQUEST_CODE = 50;

    private Camera mCamera;
    private FrontCameraView mPreview;
    private FrameLayout mPreviewFrame;
    private FloatingActionButton fab;
    private Animation hideCameraFab;
    private CameraController mCameraController;

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
                mCameraController.record(Constants.VIDEO_DURATION, new CameraRecordListener() {
                    @Override
                    public void onSuccess(File file) {
                        mCameraController.release();
                        Toast.makeText(FrontCameraPreviewActivity.this, "success", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure() {
                        // TODO : display an error view
                        Toast.makeText(FrontCameraPreviewActivity.this, R.string.camera_not_available, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mPreviewFrame = (FrameLayout) findViewById(R.id.camera_preview);

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
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraController.release();
    }

    private void initCamera() {
        mCameraController = new CameraController(this);
        if (mCameraController.getCamera() == null) {
            Toast.makeText(this, R.string.camera_not_available, Toast.LENGTH_SHORT).show();
            // TODO : display an error view
        } else {
            mPreview = new FrontCameraView(this, mCameraController.getCamera());
            mPreviewFrame.addView(mPreview);
        }
    }


}
