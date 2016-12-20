package com.antest1.kcanotify;

/**
 * Created by alias on 2016-12-18.
 */

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(
        resToastText = R.string.error_text,
        mailTo = "kcanotify@gmail.com"
)

public class KcaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
    }
}
