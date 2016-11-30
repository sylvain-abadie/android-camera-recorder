package sample.com.frontcamerarecorder.controllers.ffmpegvideogenerator;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Locale;

import sample.com.frontcamerarecorder.Constants;
import sample.com.frontcamerarecorder.controllers.VideoGenerator;
import sample.com.frontcamerarecorder.utils.FileUtils;

/**
 * Created by Sylvain on 27/11/2016.
 */

public class FFMpegVideoGenerator extends Thread implements VideoGenerator {

    private static final String TAG = "VideoGenerator";
    private static final String TEMP_IMG_DIR = "imgs";
    private static final String METADATA_PREFIX = "met_";
    private static final String CROP_PREFIX = "crop_";

    private WeakReference<VideoGeneratorListener> mWeakListener;

    private String mWorkingDir;
    private String mFinalOutputDir;
    private FFmpeg mFFmpeg;

    public FFMpegVideoGenerator(Context ctx) {
        mFFmpeg = FFmpeg.getInstance(ctx);
        mWorkingDir = ctx.getCacheDir() + File.separator + TMP_DIR;
        mFinalOutputDir = ctx.getCacheDir() + File.separator + OUTPUT_DIR;
    }

    @Override
    public void setVideoGeneratorListener(VideoGeneratorListener listener) {
        mWeakListener = new WeakReference<>(listener);
    }

    @Override
    public void convert(final File inputFile) {
        FileUtils.createDirIfNeeded(mWorkingDir);
        FileUtils.createDirIfNeeded(mFinalOutputDir);

        try {
            mFFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onSuccess() {
                    fixMetaData(inputFile);
                }

            });
        } catch (FFmpegNotSupportedException e) {
            Log.e(TAG, "not supported");
        }
    }


    private void fixMetaData(final File inputFile) {
        Log.d(TAG, "fixeMetaData");

        String c = "-y -i " + inputFile.getAbsolutePath() + " -metadata:s:v rotate=270 -codec copy "
                + mWorkingDir + File.separator + METADATA_PREFIX + inputFile.getName();

        String[] cmd = c.split(" ");
        try {
            mFFmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, message);
                    dispatchError(message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, message);
                    File generated = new File(mWorkingDir +
                            File.separator + METADATA_PREFIX + inputFile.getName());
                    cropVideo(generated);
                }

            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            dispatchError(e.getMessage());
        }
    }

    private void cropVideo(final File inputFile) {
        String c = "-y -i " + inputFile.getAbsolutePath() + " -vf crop=" +
                Constants.VIDEO_ASPECT_RATIO + "*in_h:in_h -preset ultrafast " +
                mWorkingDir + File.separator + CROP_PREFIX + inputFile.getName();

        String[] cmd = c.split(" ");
        try {
            mFFmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, message);
                    dispatchError(message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, message);
                    File generated = new File(mWorkingDir +
                            File.separator + CROP_PREFIX + inputFile.getName());
                    splitIntoImages(generated);
                }

            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            dispatchError(e.getMessage());
        }
    }

    private void splitIntoImages(final File inputFile) {
        Log.d(TAG, "splitIntoImages");

        final File tempImgsDir = new File(mWorkingDir + File.separator + TEMP_IMG_DIR);

        if (tempImgsDir.exists()) {
            FileUtils.deleteDirectory(tempImgsDir);
        }
        tempImgsDir.mkdir();

        String c = "-y -i " + inputFile.getAbsolutePath() +
                " -strict experimental -r 30 -qscale 1 -f image2 -vcodec mjpeg " +
                tempImgsDir.getAbsolutePath() + File.separator + "%03d.jpg";

        String[] cmd = c.split(" ");
        try {
            mFFmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, message);
                    dispatchError(message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, message);
                    reverseImagesOrder(tempImgsDir);
                    assembleVideo(tempImgsDir);
                }

            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            dispatchError(e.getMessage());
        }
    }

    private void reverseImagesOrder(final File inputDirectory) {
        File[] files = inputDirectory.listFiles();
        Arrays.sort(files);
        int nbImages = files.length;
        if (nbImages <= 2) {
            dispatchError("Not enough images generated");
        }
        // start from before the last image and duplicate all the images in reverse order
        for (int i = nbImages - 2; i > 0; i--) {
            File img = files[i];
            if (img.exists()) {
                String copiedImg = inputDirectory.getAbsolutePath() + File.separator +
                        String.format(Locale.ENGLISH, "%03d", 2 * nbImages - i - 1) + ".jpg";
                Log.d(TAG, copiedImg);
                FileUtils.copyAndRenameInDirectory(img.getAbsolutePath(), copiedImg);
            } else {
                Log.e(TAG, "file not found : " + img.getAbsolutePath());
            }
        }
    }

    private void assembleVideo(final File inputDirectory) {
        Log.d(TAG, "assembleVideo");
        File containingFolder = new File(mFinalOutputDir);

        final File assembledVideo = new File(containingFolder.getAbsolutePath() + File.separator + FINAL_VIDEO_NAME);

        String c = "-y -f image2 -i " + inputDirectory.getAbsolutePath() + File.separator +
                "%03d.jpg -r 30 -vcodec mpeg4 -b:v 2100k " +
                assembledVideo.getAbsolutePath();

        String[] cmd = c.split(" ");
        try {
            mFFmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, message);
                    dispatchError(message);
                }

                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, message);
                    dispatchSuccess(message, assembledVideo);
                }

            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            dispatchError(e.getMessage());
        }
    }

    private void dispatchSuccess(String message, File file) {
        if (mWeakListener != null && mWeakListener.get() != null) {
            mWeakListener.get().onVideoGenerated(message, file);
        }
    }

    private void dispatchError(String message) {
        if (mWeakListener != null && mWeakListener.get() != null) {
            mWeakListener.get().onVideoGeneratedError(message);
        }
    }
}
