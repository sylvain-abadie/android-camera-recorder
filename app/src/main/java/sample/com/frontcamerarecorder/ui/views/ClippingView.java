package sample.com.frontcamerarecorder.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import sample.com.frontcamerarecorder.Constants;

/**
 * Created by Sylvain on 26/11/2016.
 */

public class ClippingView extends View {

    private Rect rect1 = new Rect();
    private Rect rect2 = new Rect();
    private Paint clippingColor = new Paint(Color.BLACK);

    public ClippingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        int width = getWidth();
        int height = getHeight();
        int desiredWidth = (int) (height * Constants.PREVIEW_ASPECT_RATIO);
        desiredWidth /= 2;

        rect1.set(
                0,
                0,
                (width / 2) - desiredWidth,
                height);

        rect2.set(
                (width / 2) + desiredWidth,
                0,
                width,
                height);

        canvas.drawRect(rect1, clippingColor);
        canvas.drawRect(rect2, clippingColor);
    }
}
