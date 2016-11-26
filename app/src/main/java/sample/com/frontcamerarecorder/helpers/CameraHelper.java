package sample.com.frontcamerarecorder.helpers;

import android.hardware.Camera;

/**
 * Created by Sylvain on 26/11/2016.
 */

public class CameraHelper {

    public static Camera GetFrontCameraInstance() {
        Camera c = null;  // object that use
        Camera.CameraInfo info = new Camera.CameraInfo();
        int count = Camera.getNumberOfCameras();

        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    c = Camera.open(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return c; // returns null if camera is unavailable
    }


}
