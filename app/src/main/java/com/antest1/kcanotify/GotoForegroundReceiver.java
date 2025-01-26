package com.antest1.kcanotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GotoForegroundReceiver extends BroadcastReceiver {
    boolean is_front = true;

    public boolean checkForeground() {
        return is_front;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null)
            is_front = bundle.getBoolean("is_front", false);
    }
}
