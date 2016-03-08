package com.sarahmizzi.fyp.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.widget.ImageView;

import com.sarahmizzi.fyp.R;

/**
 * Created by Sarah on 09-Feb-16.
 */
public class UIUtils {
    public static final int initialButtonRepeatInterval = 400; // ms
    public static final int buttonRepeatInterval = 80; // ms
    public static final int buttonVibrationDuration = 50; //ms

    public static void handleVibration(Context context) {
        if(context == null) return;

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (!vibrator.hasVibrator()) return;

        //Check if we should vibrate
        boolean vibrateOnPress = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(Settings.KEY_PREF_VIBRATE_REMOTE_BUTTONS,
                        Settings.DEFAULT_PREF_VIBRATE_REMOTE_BUTTONS);
        if (vibrateOnPress) {
            vibrator.vibrate(UIUtils.buttonVibrationDuration);
        }
    }
}
