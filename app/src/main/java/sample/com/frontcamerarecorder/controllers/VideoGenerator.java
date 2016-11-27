package sample.com.frontcamerarecorder.controllers;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;

/**
 * Created by Sylvain on 27/11/2016.
 */

public class VideoGenerator {

    private static final String TAG = "VideoGenerator";
    private Context mContext;
    private boolean mReadyToConvert = false;

    public interface VideoGeneratorListener{
        void onVideoGenerated(String message, File generatedFile);
        void onVideoGeneratedError(String message);
    }

    public VideoGenerator(Context ctx){
        this.mContext = ctx;
        FFmpeg ffmpeg = FFmpeg.getInstance(ctx);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {
                    mReadyToConvert = true;
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            Log.e(TAG,"not supported");
            // Handle if FFmpeg is not supported by device
        }
    }

    public void fixMetaData(final File inputFile, final VideoGeneratorListener handler){
        FFmpeg ffmpeg = FFmpeg.getInstance(this.mContext);

        String filesDirPath = this.mContext.getFilesDir().getAbsolutePath();
        String c = "-i "+inputFile.getAbsolutePath()+ " -metadata:s:v rotate=270 -codec copy " +filesDirPath+"/met"+inputFile.getName();

        String[] cmd = c.split(" ");
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onProgress(String message) {
                }

                @Override
                public void onFailure(String message) {
                    Log.e(TAG,"Failure");
                }

                @Override
                public void onSuccess(String message) {
                    Log.e(TAG,"Success");
                }

                @Override
                public void onFinish() {
                    File generated = new File(mContext.getFilesDir().getAbsolutePath()+"/met"+inputFile.getName());
                    convertVideo(generated,handler);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG,"FFmpeg already running");
            // Handle if FFmpeg is already running
        }
    }

    public void convertVideo(final File inputFile, final VideoGeneratorListener handler ){
        FFmpeg ffmpeg = FFmpeg.getInstance(this.mContext);

        String filesDirPath = this.mContext.getFilesDir().getAbsolutePath();
        //String c = "-i "+inputFile.getAbsolutePath()+ " -filter:v crop=100:100 -c:a copy " +filesDirPath+"/gen.mp4";
        // Working : -metadata:s:v rotate=270 -codec copy
        String c = "-i "+inputFile.getAbsolutePath()+ " -vf crop=1/5*in_h:in_h -preset ultrafast -strict -2 " +filesDirPath+"/gen"+inputFile.getName();

        String[] cmd = c.split(" ");
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onProgress(String message) {
                    Log.e(TAG,message);
                }

                @Override
                public void onFailure(String message) {
                    handler.onVideoGeneratedError(message);
                }

                @Override
                public void onSuccess(String message) {
                    File generated = new File(mContext.getFilesDir().getAbsolutePath()+"/gen"+inputFile.getName());
                    Log.e(TAG, message);
                    handler.onVideoGenerated(message, generated);
                }

                @Override
                public void onFinish() {
                    Log.e(TAG, "FInish");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG,"FFmpeg already running");
            // Handle if FFmpeg is already running
        }
    }

}
