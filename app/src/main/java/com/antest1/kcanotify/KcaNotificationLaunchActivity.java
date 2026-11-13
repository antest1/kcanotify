package com.antest1.kcanotify;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Opens the game (or the main screen) when an alarm notification is tapped.
 * Notifications start this activity directly: an activity opened from a notification may start
 * other activities, while a service started from a notification may not.
 */
public class KcaNotificationLaunchActivity extends Activity {
    public static final String EXTRA_NID = "nid";
    public static final String EXTRA_TARGET = "target";
    public static final String TARGET_GAME = "game";
    public static final String TARGET_MAIN = "main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) handleIntent(getIntent());
        finish();
    }

    private void handleIntent(Intent intent) {
        Intent target = TARGET_MAIN.equals(intent.getStringExtra(EXTRA_TARGET))
                ? new Intent(this, MainActivity.class)
                : KcaUtils.getKcIntent(this);
        if (target != null) {
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(target);
            } catch (ActivityNotFoundException e) {
                Log.w("KCA", "cannot open " + target + ": " + e.getMessage());
            }
        }

        int nid = intent.getIntExtra(EXTRA_NID, -1);
        if (nid != -1) {
            // same bookkeeping as dismissing the notification, after the target is open so that
            // a refused service start cannot keep it from opening
            try {
                startService(new Intent(this, KcaAlarmService.class)
                        .setAction(KcaAlarmService.DELETE_ACTION.concat(String.valueOf(nid))));
            } catch (IllegalStateException e) {
                Log.w("KCA", "cannot clear notification " + nid + ": " + e.getMessage());
            }
        }
    }
}
