package com.sarahmizzi.fyp.utils;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * Created by Sarah on 09-Feb-16.
 * Refer to Kore Remote on Android.
 */
public class RepeatListener implements View.OnTouchListener {
    private static final String TAG = RepeatListener.class.getSimpleName();

    private static Handler repeatHandler = new Handler();

    private int initialInterval;
    private final int repeatInterval;
    private final View.OnClickListener clickListener;

    private Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            if (downView.isShown()) {
                if (repeatInterval >= 0) {
                    repeatHandler.postDelayed(this, repeatInterval);
                }
                clickListener.onClick(downView);
            }
        }
    };

    /**
     * Animations for down/up
     */
    private Animation animDown;
    private Animation animUp;

    private View downView;

    private Context context;

    /**
     * Constructor for a repeat listener
     */
    public RepeatListener(int initialInterval, int repeatInterval, View.OnClickListener clickListener) {
        this(initialInterval, repeatInterval, clickListener, null, null, null);
    }

    public RepeatListener(int initialInterval, int repeatInterval, View.OnClickListener clickListener,
                          Animation animDown, Animation animUp) {
        this(initialInterval, repeatInterval, clickListener, animUp, animDown, null);
    }

    /**
     * Constructor for a repeat listener, with animation and vibration
     */
    public RepeatListener(int initialInterval, int repeatInterval, View.OnClickListener clickListener,
                          Animation animDown, Animation animUp, Context context) {
        this.initialInterval = initialInterval;
        this.repeatInterval = repeatInterval;
        this.clickListener = clickListener;

        this.animDown = animDown;
        this.animUp = animUp;

        this.context = context;
    }

    /**
     * Handle touch events.
     */
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                UIUtils.handleVibration(context);
                repeatHandler.removeCallbacks(handlerRunnable);
                if (initialInterval >= 0) {
                    repeatHandler.postDelayed(handlerRunnable, initialInterval);
                }
                downView = view;

                if (animDown != null) {
                    animDown.setFillAfter(true);
                    view.startAnimation(animDown);
                }
                break;
            case MotionEvent.ACTION_UP:
                clickListener.onClick(view);
                view.playSoundEffect(SoundEffectConstants.CLICK);
                // Fallthrough
            case MotionEvent.ACTION_CANCEL:
                repeatHandler.removeCallbacks(handlerRunnable);
                downView = null;

                if (animUp != null) {
                    view.startAnimation(animUp);
                }
                break;
        }

        return !((view instanceof Button) || (view instanceof ImageButton));
    }
}
