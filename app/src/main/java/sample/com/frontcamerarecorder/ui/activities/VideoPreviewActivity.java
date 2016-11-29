
package sample.com.frontcamerarecorder.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import sample.com.frontcamerarecorder.R;

public class VideoPreviewActivity extends Activity {
    public static final String TAG = "VideoPreviewActivity";
    public static final String VIDEO_PATH = "VIDEO_PATH";

    String mVideoPath;
    float mVideoRatio;

    SurfaceView mSurfaceView1;
    SurfaceHolder mSurfaceHolder1;
    FloatingActionButton mFabShareVideo;

    boolean mCreated = false;
    boolean mIsPlaying = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_video_preview);
        View decorView = getWindow().getDecorView();
        mVideoPath = getIntent().getStringExtra(VIDEO_PATH);

        // Hide the status bar.
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        mSurfaceView1 = (SurfaceView) findViewById(R.id.sf_video_preview);

        mSurfaceHolder1 = mSurfaceView1.getHolder();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mVideoPath);
        int width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        mVideoRatio = (float) width / (float) height;
        retriever.release();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthToSet = (int) (metrics.heightPixels * mVideoRatio);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthToSet, metrics.heightPixels);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        mSurfaceView1.setLayoutParams(params);

        mSurfaceHolder1.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.v(TAG, "surfaceChanged format=" + format + ", width=" + width + ", height="
                        + height);
                if (mVideoPath != null) {
                    mCreated = createStreamingMediaPlayer(mVideoPath);
                    setPlayingStreamingMediaPlayer(true);
                }

            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                setSurface(holder.getSurface());

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.v(TAG, "surfaceDestroyed");
            }

        });

        mFabShareVideo = (FloatingActionButton) findViewById(R.id.btn_share);
        mFabShareVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, mVideoPath);
                shareIntent.setType("video/mp4");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
            }
        });

    }

    void create() {
        if (!mCreated) {
            Surface s = mSurfaceHolder1.getSurface();
            Log.i("@@@", "setting surface " + s);
            setSurface(s);
        } else {
            mIsPlaying = true;
            setPlayingStreamingMediaPlayer(true);
        }
    }

    /**
     * Called when the activity is about to be paused.
     */
    @Override
    protected void onPause() {
        mIsPlaying = false;
        setPlayingStreamingMediaPlayer(mIsPlaying);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        create();

    }

    /**
     * Called when the activity is about to be destroyed.
     */
    @Override
    protected void onDestroy() {
        shutdown();
        mCreated = false;
        super.onDestroy();
    }

    /**
     * Native methods, implemented in jni folder
     */
    public static native boolean createStreamingMediaPlayer(String filename);

    public static native boolean createStreamingMediaPlayerFromAssets(AssetManager assetMgr, String filename);

    public static native void setPlayingStreamingMediaPlayer(boolean isPlaying);

    public static native void shutdown();

    public static native void setSurface(Surface surface);

    public static native void rewindStreamingMediaPlayer();

    /** Load jni .so on initialization */
    static {
        System.loadLibrary("native-codec-jni");
    }


}
