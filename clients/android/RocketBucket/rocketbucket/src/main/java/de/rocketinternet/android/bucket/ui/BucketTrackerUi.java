package de.rocketinternet.android.bucket.ui;

import android.app.Activity;
import android.graphics.PointF;
import android.os.SystemClock;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import de.rocketinternet.android.bucket.BucketsActivity;
import de.rocketinternet.android.bucket.R;

/**
 * Created by mohamed.elawadi on 12/04/16.
 */
public class BucketTrackerUi {
    public static void inject(Activity activity, final String url, final String apiKey) {
        //TODO we can make url part of singleton Network or bucket instance to avoid passing url multiple times
        ViewGroup viewGroup = (ViewGroup) activity.getWindow().getDecorView();

        Display display = activity.getWindowManager().getDefaultDisplay();
        int buttonSize = Math.min(display.getWidth(), display.getHeight()) / 8;

        View view = activity.findViewById(R.id.btn_bucket);
        if (view == null) {
            view = new View(activity);

            FrameLayout.LayoutParams layoutParams =
                    new FrameLayout.LayoutParams(buttonSize / 2, buttonSize, Gravity.CENTER_VERTICAL | Gravity.LEFT);
            layoutParams.leftMargin = buttonSize / 8;
            view.setId(R.id.btn_bucket);
            view.setLayoutParams(layoutParams);
            view.setBackgroundColor(ResourcesCompat.getColor(activity.getResources(), R.color.bucket_button_color, activity.getTheme()));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.getContext().startActivity(BucketsActivity.newIntent(view.getContext()));
                }
            });
            view.setOnTouchListener(new View.OnTouchListener() {
                long touchTime;
                PointF touchPos = new PointF();

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            touchTime = SystemClock.uptimeMillis();
                            touchPos.set(event.getX(), event.getY());
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float diffX = touchPos.x - event.getX();
                            float diffY = touchPos.y - event.getY();
                            view.setX(view.getX() - diffX);
                            view.setY(view.getY() - diffY);
                            break;
                    }
                    return SystemClock.uptimeMillis() - touchTime > 200;
                }
            });

            viewGroup.addView(view);
        }
    }
}
