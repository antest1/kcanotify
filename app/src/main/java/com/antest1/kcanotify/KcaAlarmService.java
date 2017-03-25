package com.antest1.kcanotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.isExpeditionDataLoaded;
import static com.antest1.kcanotify.KcaApiData.isGameDataLoaded;
import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_S2_CACHE_FILENAME;
import static com.antest1.kcanotify.KcaConstants.KCA_API_UPDATE_FRONTVIEW;
import static com.antest1.kcanotify.KcaConstants.NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_RINGTONE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_SOUND_KIND;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getKcIntent;
import static com.antest1.kcanotify.KcaUtils.getNotificationId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.readCacheData;

public class KcaAlarmService extends Service {
    public static final int TYPE_EXPEDITION = 1;
    public static final int TYPE_DOCKING = 2;

    public static final int EXP_CANCEL_FLAG = 8;
    public static final long ALARM_DELAY = 61000;

    AudioManager mAudioManager;
    NotificationManager notificationManager;
    Bitmap expBitmap, dockBitmap = null;
    public static Handler sHandler = null;
    Bundle bundle;
    Message sMsg;

    private boolean isExpAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_EXP);
    }

    private boolean isDockAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_DOCK);
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        expBitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.expedition_notify_bigicon)).getBitmap();
        dockBitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.docking_notify_bigicon)).getBitmap();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA", "KcaAlarmService Called: " + String.valueOf(startId));
        loadTranslationData(getAssets(), getApplicationContext());
        if (intent != null) {
            JsonObject data = new JsonParser().parse(intent.getStringExtra("data")).getAsJsonObject();
            int type = data.get("type").getAsInt();
            String locale = LocaleUtils.getLocaleCode(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE));
            if (type == TYPE_EXPEDITION && isExpAlarmEnabled()) {
                int idx = data.get("idx").getAsInt();
                KcaExpedition2.clearMissionData(idx);
                if (isExpAlarmEnabled()) {
                    if (!isExpeditionDataLoaded()) loadSimpleExpeditionInfoFromAssets(getAssets());
                    int mission_no = data.get("mission_no").getAsInt();
                    String mission_name = KcaApiData.getExpeditionName(mission_no, locale);
                    String kantai_name = data.get("kantai_name").getAsString();
                    boolean cancelFlag = data.get("cancel_flag").getAsBoolean();
                    boolean caFlag = data.get("ca_flag").getAsBoolean();
                    if (caFlag) idx = idx | EXP_CANCEL_FLAG;
                    notificationManager.notify(getNotificationId(NOTI_EXP, idx), createExpeditionNotification(mission_no, mission_name, kantai_name, cancelFlag, caFlag));
                }
            } else if (type == TYPE_DOCKING) {
                int dockId = data.get("dock_id").getAsInt();
                KcaDocking.setCompleteTime(dockId, -1);
                KcaDocking.setShipId(dockId, 0);
                if(isDockAlarmEnabled()) {
                    int shipId = data.get("ship_id").getAsInt();
                    String shipName = "";
                    if (shipId != -1) {
                        if (!isGameDataLoaded()) {
                            JsonObject cachedData = readCacheData(getApplicationContext(), KCANOTIFY_S2_CACHE_FILENAME);
                            KcaApiData.getKcGameData(cachedData.getAsJsonObject("api_data"));
                        }
                        JsonObject kcShipData = KcaApiData.getKcShipDataById(shipId, "name");
                        shipName = getShipTranslation(kcShipData.get("name").getAsString(), false);
                    }
                    notificationManager.notify(getNotificationId(NOTI_DOCK, dockId), createDockingNotification(dockId, shipName));
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (expBitmap != null) {
            expBitmap.recycle();
            expBitmap = null;
        }
        if (dockBitmap != null) {
            dockBitmap.recycle();
            dockBitmap = null;
        }
        super.onDestroy();
    }

    private Notification createExpeditionNotification(int missionNo, String missionName, String kantaiName, boolean cancelFlag, boolean caFlag) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, getKcIntent(getApplicationContext()),
                PendingIntent.FLAG_UPDATE_CURRENT);
        String title = "";
        String content = "";
        if (cancelFlag) {
            title = String.format(getStringWithLocale(R.string.kca_noti_title_exp_canceled), missionNo, missionName);
            content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_canceled), kantaiName, missionNo);
        } else {
            title = String.format(getStringWithLocale(R.string.kca_noti_title_exp_finished), missionNo, missionName);
            if (caFlag)
                content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_finished_canceled), kantaiName, missionNo);
            else
                content = String.format(getStringWithLocale(R.string.kca_noti_content_exp_finished_normal), kantaiName, missionNo);

        }
        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.expedition_notify_icon)
                .setLargeIcon(expBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
                .setContentIntent(pendingIntent);

        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                if (soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                builder.setSound(Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
            } else if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_vibrate))) {
            if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_mute))) {
            builder.setDefaults(0);
        }

        Notification Notifi = builder.build();
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;

        if (sHandler != null) {
            bundle = new Bundle();
            bundle.putString("url", KCA_API_UPDATE_FRONTVIEW);
            bundle.putString("data", "");
            sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }
        return Notifi;
    }

    private Notification createDockingNotification(int dockId, String shipName) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, getKcIntent(getApplicationContext()),
                PendingIntent.FLAG_UPDATE_CURRENT);
        String title = String.format(getStringWithLocale(R.string.kca_noti_title_dock_finished), dockId + 1);
        String content = "";
        if (shipName.length() > 0) {
            content = String.format(getStringWithLocale(R.string.kca_noti_content_dock_finished), dockId + 1, shipName);
        } else {
            content = String.format(getStringWithLocale(R.string.kca_noti_content_dock_finished_nodata), dockId + 1);
        }

        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.docking_notify_icon)
                .setLargeIcon(dockBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
                .setContentIntent(pendingIntent);
        String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
        if (soundKind.equals(getString(R.string.sound_kind_value_normal)) || soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                if (soundKind.equals(getString(R.string.sound_kind_value_mixed))) {
                    builder.setDefaults(Notification.DEFAULT_VIBRATE);
                }
                builder.setSound(Uri.parse(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE)));
            } else if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_vibrate))) {
            if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else {
                builder.setDefaults(0);
            }
        }
        if (soundKind.equals(getString(R.string.sound_kind_value_mute))) {
            builder.setDefaults(0);
        }

        Notification Notifi = builder.build();
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;

        if (sHandler != null) {
            bundle = new Bundle();
            bundle.putString("url", KCA_API_UPDATE_FRONTVIEW);
            bundle.putString("data", "");
            sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
            // send Handler to setFrontView
        }
        return Notifi;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
