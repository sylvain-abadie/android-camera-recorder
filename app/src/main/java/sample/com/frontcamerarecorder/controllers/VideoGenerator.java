package sample.com.frontcamerarecorder.controllers;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.util.Arrays;

import sample.com.frontcamerarecorder.Constants;
import sample.com.frontcamerarecorder.utils.FileUtils;

/**
 * Created by Sylvain on 27/11/2016.
 */

public class VideoGenerator {

    private static final String TAG = "VideoGenerator";
    private Context mContext;
    private boolean mReadyToConvert = false;
    private String mWorkingDir;

    public interface VideoGeneratorListener {
        void onVideoGenerated(String message, File generatedFile);

        void onVideoGeneratedError(String message);
    }

    public VideoGenerator(Context ctx) {
        this.mContext = ctx;
        FFmpeg ffmpeg = FFmpeg.getInstance(ctx);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                    mReadyToConvert = true;
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            Log.e(TAG, "not supported");
        }
        mWorkingDir = mContext.getFilesDir() + File.separator + "tmp";
    }


    public void convert(final File inputFile, final VideoGeneratorListener handler) {
        fixMetaData(inputFile, handler);
    }


    private void fixMetaData(final File inputFile, final VideoGeneratorListener handler) {
        FFmpeg ffmpeg = FFmpeg.getInstance(this.mContext);

        String filesDirPath = this.mContext.getFilesDir().getAbsolutePath();
        String c = "-y -i " + inputFile.getAbsolutePath() + " -metadata:s:v rotate=270 -codec copy "
                + filesDirPath + File.separator + "met" + inputFile.getName();

        String[] cmd = c.split(" ");
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onProgress(String message) {
                }

                @Override
                public void onFailure(String message) {
                    handler.onVideoGeneratedError(message);
                }

                @Override
                public void onSuccess(String message) {
                    File generated = new File(mContext.getFilesDir().getAbsolutePath() +
                            File.separator + "met" + inputFile.getName());
                    cropVideo(generated, handler);
                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, "FFmpeg already running");
        }
    }

    private void cropVideo(final File inputFile, final VideoGeneratorListener handler) {
        FFmpeg ffmpeg = FFmpeg.getInstance(this.mContext);

        String filesDirPath = this.mContext.getFilesDir().getAbsolutePath();

        String c = "-y -i " + inputFile.getAbsolutePath() + " -vf crop=" + "" +
                Constants.VIDEO_ASPECT_RATIO + "*in_h:in_h -preset ultrafast -strict -2 " +
                filesDirPath + File.separator + "crop" + inputFile.getName();

        String[] cmd = c.split(" ");
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onProgress(String message) {
                    Log.e(TAG, message);
                }

                @Override
                public void onFailure(String message) {
                    handler.onVideoGeneratedError(message);
                }

                @Override
                public void onSuccess(String message) {
                    File generated = new File(mContext.getFilesDir().getAbsolutePath() +
                            File.separator + "crop" + inputFile.getName());
                    // handler.onVideoGenerated(message, generated);
                    splitIntoImages(generated, handler);
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "FInish");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, "FFmpeg already running");
        }
    }

    public void splitIntoImages(final File inputFile, final VideoGeneratorListener handler) {
        FFmpeg ffmpeg = FFmpeg.getInstance(this.mContext);

        final String filesDirPath = this.mContext.getFilesDir().getAbsolutePath();

        File tempDir = new File(filesDirPath + File.separator + "tmp");

        if (tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }
        tempDir.mkdir();


        String c = "-y -i " + inputFile.getAbsolutePath() +
                " -strict experimental -r 30 -qscale 1 -f image2 -vcodec mjpeg " +
                filesDirPath + File.separator + "tmp" + File.separator + "%03d.jpg";

        String[] cmd = c.split(" ");
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onProgress(String message) {
                    Log.e(TAG, message);
                }

                @Override
                public void onFailure(String message) {
                    handler.onVideoGeneratedError(message);
                }

                @Override
                public void onSuccess(String message) {
                    File imagesDirectory = new File(filesDirPath + File.separator + "tmp");
                    reverseImagesOrder(imagesDirectory, handler);
                    assembleVideo(imagesDirectory, handler);
                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, "FFmpeg already running");
        }
    }

    private void reverseImagesOrder(final File inputDirectory, final VideoGeneratorListener handler) {
        File[] files = inputDirectory.listFiles();
        Arrays.sort(files);
        int nbImages = files.length;
        if (nbImages <= 2) {
            handler.onVideoGeneratedError("Not enough images generated");
        }
        // start from before the last image and duplicate all the images in reverse order
        for (int i = nbImages - 2; i > 0; i--) {
            File img = files[i];
            if (img.exists()) {
                String copiedImg = inputDirectory.getAbsolutePath() + File.separator +
                        String.format("%03d", 2 * nbImages - i - 1) + ".jpg";
                Log.d(TAG, copiedImg);
                FileUtils.copyAndRenameInDirectory(img.getAbsolutePath(), copiedImg);
            } else {
                Log.e(TAG, "file not found : " + img.getAbsolutePath());
            }
        }
    }

    public void assembleVideo(final File inputDirectory, final VideoGeneratorListener handler) {
        FFmpeg ffmpeg = FFmpeg.getInstance(this.mContext);
        File containingFolder = new File(inputDirectory.getAbsolutePath() + File.separator + "generated");
        if (!containingFolder.exists()) {
            containingFolder.mkdir();
        }
        final File assembledVideo = new File(containingFolder.getAbsolutePath() + File.separator + "final.mp4");

        String c = "-y -f image2 -i " + inputDirectory.getAbsolutePath() + File.separator +
                "%03d.jpg -r 30 -vcodec mpeg4 -b:v 2100k " +
                assembledVideo.getAbsolutePath();

        String[] cmd = c.split(" ");
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onProgress(String message) {
                    Log.e(TAG, message);
                }

                @Override
                public void onFailure(String message) {
                    handler.onVideoGeneratedError(message);
                }

                @Override
                public void onSuccess(String message) {
                    handler.onVideoGenerated(message, assembledVideo);
                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, "FFmpeg already running");
        }
    }

}
