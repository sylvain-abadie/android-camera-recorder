package sample.com.frontcamerarecorder.controllers;

import java.io.File;

import sample.com.frontcamerarecorder.controllers.ffmpegvideogenerator.FFMpegVideoGenerator;

/**
 * Created by Sylvain on 30/11/2016.
 */

public interface VideoGenerator {

    String TMP_DIR = "tmp";

    String OUTPUT_DIR = "gen";

    String FINAL_VIDEO_NAME = "generated.mp4";


    interface VideoGeneratorListener {

        void onVideoGenerated(String message, File generatedFile);

        void onVideoGeneratedError(String message);

    }

    void setVideoGeneratorListener(VideoGeneratorListener listener);

    void convert(final File inputFile);

}
