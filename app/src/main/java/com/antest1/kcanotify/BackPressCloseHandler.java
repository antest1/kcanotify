package com.antest1.kcanotify;

import android.app.Activity;
import android.widget.Toast;

public class BackPressCloseHandler {
    private static final int INTERVAL = 1500;
    long pressedTime = 0;
    Toast toast;

    private Activity activity;

    public BackPressCloseHandler(Activity context) {
        this.activity = context;
    }

    public void onBackPressed() {
        if (System.currentTimeMillis() > pressedTime + INTERVAL) {
            pressedTime = System.currentTimeMillis();
            showMessage();
            return;
        }
        if (System.currentTimeMillis() <= pressedTime + INTERVAL) {
            activity.finish();
            toast.cancel();
        }
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(activity.getApplication(), activity.getBaseContext(), id);
    }

    public void showMessage() {
        toast = Toast.makeText(activity, getStringWithLocale(R.string.backpress_msg), Toast.LENGTH_SHORT);
        toast.show();
    }
}
