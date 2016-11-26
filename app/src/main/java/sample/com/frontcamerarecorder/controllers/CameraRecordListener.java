package sample.com.frontcamerarecorder.controllers;

import java.io.File;

/**
 * Created by Sylvain on 26/11/2016.
 */

public interface CameraRecordListener {
    void onSuccess(File file);
    void onFailure();
}
