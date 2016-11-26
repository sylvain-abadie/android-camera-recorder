package sample.com.frontcamerarecorder.ui.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import sample.com.frontcamerarecorder.Constants;
import sample.com.frontcamerarecorder.R;
import sample.com.frontcamerarecorder.helpers.CameraHelper;
import sample.com.frontcamerarecorder.ui.views.FrontCameraView;

@SuppressWarnings("deprecation")
public class FrontCameraPreviewActivity extends AppCompatActivity {
    public final static String TAG = "FrontCameraPreviewAct";

    private final static int CAMERA_PERMISSION_REQUEST_CODE = 50;

    private Camera mCamera;
    private FrontCameraView mPreview;
    private FrameLayout mPreviewLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_camera_preview);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initCamera();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    this.initCamera();

                } else {
                    // TODO : display an error view

                }
            }
        }
    }


    private void initCamera() {
        mCamera = CameraHelper.GetFrontCameraInstance();
        if (mCamera == null) {
            Log.d(TAG, "can't access camera");
            // TODO : display an error view
        } else {
            mPreview = new FrontCameraView(this, mCamera);
            mPreviewLayout.addView(mPreview);

            this.initBorderClipping();
        }
    }

    private void initBorderClipping() {
        ViewTreeObserver vto = mPreviewLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPreviewLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int width = mPreviewLayout.getMeasuredWidth();
                int height = mPreviewLayout.getMeasuredHeight();
                int desiredWidth = (int) (height * Constants.PREVIEW_ASPECT_RATIO);
                desiredWidth /= 2;

                Rect rect = new Rect(
                        (width / 2) - desiredWidth,
                        0,
                        (width / 2) + desiredWidth,
                        height
                );
                mPreviewLayout.setClipBounds(rect);
            }
        });
    }

}
