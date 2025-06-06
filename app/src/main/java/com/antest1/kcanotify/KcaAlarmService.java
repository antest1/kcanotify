package com.antest1.kcanotify;

import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.common.collect.EvictingQueue;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.isExpeditionDataLoaded;
import static com.antest1.kcanotify.KcaApiData.loadSimpleExpeditionInfoFromStorage;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.ERROR_TYPE_NOTI;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_NOTICOUNT_CHANGED;
import static com.antest1.kcanotify.KcaConstants.KCA_API_UPDATE_FRONTVIEW;
import static com.antest1.kcanotify.KcaConstants.NOTI_AKASHI;
import static com.antest1.kcanotify.KcaConstants.NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.NOTI_MORALE;
import static com.antest1.kcanotify.KcaConstants.NOTI_SOUND_KIND_MUTE;
import static com.antest1.kcanotify.KcaConstants.NOTI_UPDATE;
import static com.antest1.kcanotify.KcaConstants.PREF_ALARM_DELAY;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_AKASHI;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_DOCK;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_EXP;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_MORALE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_NOTIFYATSVCOFF;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_RINGTONE;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_NOTI_SOUND_KIND;
import static com.antest1.kcanotify.KcaConstants.NOTI_SOUND_KIND_MIXED;
import static com.antest1.kcanotify.KcaConstants.NOTI_SOUND_KIND_NORMAL;
import static com.antest1.kcanotify.KcaConstants.NOTI_SOUND_KIND_VIBRATE;
import static com.antest1.kcanotify.KcaUtils.checkContentUri;
import static com.antest1.kcanotify.KcaUtils.createBuilder;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getUriFromContent;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getKcIntent;
import static com.antest1.kcanotify.KcaUtils.getNotificationId;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setSoundSetting;

public class KcaAlarmService extends BaseService {
    public static final int TYPE_EXPEDITION = 1;
    public static final int TYPE_DOCKING = 2;
    public static final int TYPE_UPDATE = 3;
    public static final int TYPE_MORALE = 4;
    public static final int TYPE_AKASHI = 5;

    public static final int NOTI_ICON_SIZE = 128;

    public static final String ALARM_CHANNEL_ID = "noti_alarm_channel";
    public static final String ALARM_CHANNEL_NAME = "Kcanotify Notification";

    public static final int EXP_CANCEL_FLAG = 32;
    public static long ALARM_DELAY = 61000;
    public static Set<Integer> alarm_set = new HashSet<>();
    private static Queue<String> alarmChannelList = EvictingQueue.create(4);

    public static final String ACTION_PREFIX = "action_";
    public static final String CLICK_ACTION = "action_click_";
    public static final String DELETE_ACTION = "action_delete_";
    public static final String UPDATE_ACTION = "action_update_";
    public static final String REFRESH_CHANNEL = "refresh_channel";

    AudioManager mAudioManager;
    KcaDBHelper dbHelper;
    NotificationManager notificationManager;
    public static Handler sHandler = null;
    Bundle bundle;
    Message sMsg;

    public static void setAlarmDelay(int val) {
        ALARM_DELAY = val * 1000;
    }

    private boolean isExpAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_EXP);
    }

    private boolean isDockAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_DOCK);
    }

    private boolean isMoraleAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_MORALE);
    }

    private boolean isAkashiAlarmEnabled() {
        return getBooleanPreferences(getApplicationContext(), PREF_KCA_NOTI_AKASHI);
    }

    public static int getAlarmCount() {
        return alarm_set.size();
    }

    public static void clearAlarmCount() {
        alarm_set.clear();
    }

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        setDefaultGameData();
        loadTranslationData(getApplicationContext());

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String delay_value = getStringPreferences(getApplicationContext(), PREF_ALARM_DELAY);
        if (delay_value.length() > 0) setAlarmDelay(Integer.parseInt(delay_value));
        createAlarmChannel();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("KCA", "KcaAlarmService Called: " + String.valueOf(startId));
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.e("KCA-N", "Action: ".concat(action));
            Log.e("KCA-N", "B> " + alarm_set.toString());
            if (action.startsWith(REFRESH_CHANNEL)) {
                String uri = intent.getStringExtra("uri");
                Log.e("KCA-A", REFRESH_CHANNEL + " recv: " + uri);
                createAlarmChannel(uri);
            } else if (action.startsWith(ACTION_PREFIX)) {
                if (action.startsWith(CLICK_ACTION)) {
                    Intent kcintent = getKcIntent(getApplicationContext());
                    if (kcintent != null) {
                        kcintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent pintent = PendingIntent.getActivity(getApplicationContext(), 0, kcintent, PendingIntent.FLAG_IMMUTABLE);
                        try {
                            pintent.send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (action.startsWith(UPDATE_ACTION)) {
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                String[] action_list = action.split("_");
                if (action_list.length == 3) {
                    int nid = Integer.parseInt(action_list[2]);
                    alarm_set.remove(nid);
                    notificationManager.cancel(nid);
                }
            }
            Log.e("KCA-N", "A> " + alarm_set.toString());
        } else if (intent != null && intent.getStringExtra("data") != null) {
            String extra_data = intent.getStringExtra("data");
            if (extra_data != null) {
                JsonObject data = JsonParser.parseString(extra_data).getAsJsonObject();
                int type = data.get("type").getAsInt();
                if (type == TYPE_UPDATE) {
                    int utype = data.get("utype").getAsInt();
                    String version = data.get("version").getAsString();
                    int nid = getNotificationId(NOTI_UPDATE, utype);
                    show_notification(nid, createUpdateNotification(utype, version, nid));
                    alarm_set.add(nid);

                } else if (getBooleanPreferences(getApplication(), PREF_KCA_NOTI_NOTIFYATSVCOFF) || KcaService.getServiceStatus()) {
                    if (type == TYPE_EXPEDITION) {
                        int idx = data.get("idx").getAsInt();
                        KcaExpedition2.clearMissionData(idx);
                        if (isExpAlarmEnabled()) {
                            if (!isExpeditionDataLoaded())
                                loadSimpleExpeditionInfoFromStorage(getApplicationContext());
                            int mission_no = data.get("mission_no").getAsInt();
                            String mission_name = KcaApiData.getExpeditionName(mission_no);
                            String kantai_name = data.get("kantai_name").getAsString();
                            boolean cancelFlag = data.get("cancel_flag").getAsBoolean();
                            boolean caFlag = data.get("ca_flag").getAsBoolean();
                            // if (caFlag) idx = idx | EXP_CANCEL_FLAG;
                            int nid = getNotificationId(NOTI_EXP, idx);
                            show_notification(nid, createExpeditionNotification(mission_no, mission_name, kantai_name, cancelFlag, caFlag, nid));
                            alarm_set.add(nid);
                        }
                    } else if (type == TYPE_DOCKING) {
                        int dockId = data.get("dock_id").getAsInt();
                        KcaDocking.setCompleteTime(dockId, -1);
                        KcaDocking.setShipId(dockId, 0);
                        if (isDockAlarmEnabled()) {
                            int shipId = data.get("ship_id").getAsInt();
                            String shipName = "";
                            if (shipId != -1) {
                                JsonObject kcShipData = KcaApiData.getKcShipDataById(shipId, "name");
                                if (kcShipData != null) {
                                    String kcShipName = kcShipData.get("name").getAsString();
                                    shipName = getShipTranslation(kcShipName, shipId, false);
                                }
                            }
                            int nid = getNotificationId(NOTI_DOCK, dockId);
                            show_notification(nid, createDockingNotification(dockId, shipName, nid));
                            alarm_set.add(nid);
                        }
                    } else if (type == TYPE_MORALE) {
                        int idx = data.get("idx").getAsInt();
                        if(isMoraleAlarmEnabled()) {
                            int nid = getNotificationId(NOTI_MORALE, idx);
                            String kantai_name = data.get("kantai_name").getAsString();
                            show_notification(nid, createMoraleNotification(idx, kantai_name, nid));
                            alarm_set.add(nid);
                        }
                    } else if (type == TYPE_AKASHI) {
                        if (isAkashiAlarmEnabled()) {
                            int nid = getNotificationId(NOTI_AKASHI, 0);
                            if (KcaAkashiRepairInfo.getAkashiInAnyFlagship()) {
                                show_notification(nid, createAkashiRepairNotification(nid));
                                alarm_set.add(nid);
                            }
                        }
                    }
                }
            }
        }

        Log.e("KCA", "Noti Count: " + alarm_set.size());
        if (sHandler != null) {
            bundle = new Bundle();
            bundle.putString("url", KCA_API_PREF_NOTICOUNT_CHANGED);
            bundle.putString("data", "");
            sMsg = sHandler.obtainMessage();
            sMsg.setData(bundle);
            sHandler.sendMessage(sMsg);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static String createAlarmId(String uri, String sound_kind) {
        if (NOTI_SOUND_KIND_VIBRATE.equals(sound_kind) || NOTI_SOUND_KIND_MUTE.equals(sound_kind)) {
            return KcaUtils.format("%s_%s", ALARM_CHANNEL_ID, sound_kind);
        } else {
            return KcaUtils.format("%s_%s_%s", ALARM_CHANNEL_ID, String.valueOf(uri.hashCode()), sound_kind);
        }
    }

    private void createAlarmChannel(String uri) {
        Log.e("KCA-A", "recv: " + uri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String soundKind = getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_SOUND_KIND);
            List<NotificationChannel> channels = notificationManager.getNotificationChannels();
            for (NotificationChannel nc: channels) {
                if(nc.getId().startsWith(ALARM_CHANNEL_ID)) {
                    notificationManager.deleteNotificationChannel(nc.getId());
                }
            }
            alarmChannelList.clear();

            boolean isSound = soundKind.equals(NOTI_SOUND_KIND_MIXED) || soundKind.equals(NOTI_SOUND_KIND_NORMAL);
            boolean isVibrate = soundKind.equals(NOTI_SOUND_KIND_MIXED) || soundKind.equals(NOTI_SOUND_KIND_VIBRATE);

            String channel_name = createAlarmId(uri, soundKind);
            alarmChannelList.add(channel_name);
            NotificationChannel channel = new NotificationChannel(alarmChannelList.peek(),
                    getString(R.string.notification_appinfo_title), NotificationManager.IMPORTANCE_HIGH);

            if (isSound && uri.length() > 0) {
                AudioAttributes.Builder attrs = new AudioAttributes.Builder();
                attrs.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
                attrs.setUsage(AudioAttributes.USAGE_NOTIFICATION);

                try {
                    Uri content_uri = getUriFromContent(Uri.parse(uri));
                    if (DEFAULT_NOTIFICATION_URI.equals(content_uri)) {
                        channel.setSound(DEFAULT_NOTIFICATION_URI, attrs.build());
                    } else if (checkContentUri(getApplicationContext(), content_uri)) {
                        channel.setSound(content_uri, attrs.build());
                    } else {
                        channel.setSound(DEFAULT_NOTIFICATION_URI, attrs.build());
                    }
                } catch (Exception e) {
                    channel.setSound(null, null);
                    dbHelper.recordErrorLog(ERROR_TYPE_NOTI, "create_channel", null, null, getStringFromException(e));
                }
            } else {
                channel.setSound(null, null);
            }

            channel.enableVibration(isVibrate);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createAlarmChannel() {
        createAlarmChannel(getStringPreferences(getApplicationContext(), PREF_KCA_NOTI_RINGTONE));
    }

    private Notification createExpeditionNotification(int missionNo, String missionName, String kantaiName, boolean cancelFlag, boolean caFlag, int nid) {
        PendingIntent contentPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(CLICK_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(DELETE_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = "";
        String content = "";
        String missionNoStr = KcaExpedition2.getExpeditionStr(missionNo);
        if (cancelFlag) {
            title = KcaUtils.format(getString(R.string.kca_noti_title_exp_canceled), missionNoStr, missionName);
            content = KcaUtils.format(getString(R.string.kca_noti_content_exp_canceled), kantaiName, missionNoStr);
        } else {
            title = KcaUtils.format(getString(R.string.kca_noti_title_exp_finished), missionNoStr, missionName);
            if (caFlag)
                content = KcaUtils.format(getString(R.string.kca_noti_content_exp_finished_canceled), kantaiName, missionNoStr);
            else
                content = KcaUtils.format(getString(R.string.kca_noti_content_exp_finished_normal), kantaiName, missionNoStr);

        }

        Bitmap expBitmap = KcaUtils.decodeSampledBitmapFromResource(getResources(),  R.mipmap.expedition_notify_bigicon, NOTI_ICON_SIZE, NOTI_ICON_SIZE);
        NotificationCompat.Builder builder = createBuilder(getApplicationContext(), alarmChannelList.peek())
                .setSmallIcon(R.mipmap.expedition_notify_icon)
                .setLargeIcon(expBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setTicker(title)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(contentPendingIntent);

        builder = setSoundSetting(getApplicationContext(), mAudioManager, builder);
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

    private Notification createDockingNotification(int dockId, String shipName, int nid) {
        PendingIntent contentPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(CLICK_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(DELETE_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = KcaUtils.format(getString(R.string.kca_noti_title_dock_finished), dockId + 1);
        String content = "";
        if (shipName.length() > 0) {
            content = KcaUtils.format(getString(R.string.kca_noti_content_dock_finished), dockId + 1, shipName);
        } else {
            content = KcaUtils.format(getString(R.string.kca_noti_content_dock_finished_nodata), dockId + 1);
        }

        Bitmap dockBitmap = KcaUtils.decodeSampledBitmapFromResource(getResources(),  R.mipmap.docking_notify_bigicon, NOTI_ICON_SIZE, NOTI_ICON_SIZE);
        NotificationCompat.Builder builder = createBuilder(getApplicationContext(), alarmChannelList.peek())
                .setSmallIcon(R.mipmap.docking_notify_icon)
                .setLargeIcon(dockBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setTicker(title)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(contentPendingIntent);

        builder = setSoundSetting(getApplicationContext(), mAudioManager, builder);
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

    private Notification createMoraleNotification(int idx, String kantaiName, int nid) {
        PendingIntent contentPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class)
                        .setAction(CLICK_ACTION.concat(String.valueOf(nid))),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class).setAction(DELETE_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = KcaUtils.format(getString(R.string.kca_noti_title_morale_recovered), idx + 1);
        String content = KcaUtils.format(getString(R.string.kca_noti_content_morale_recovered), kantaiName);

        Bitmap moraleBitmap = KcaUtils.decodeSampledBitmapFromResource(getResources(),  R.mipmap.morale_notify_bigicon, NOTI_ICON_SIZE, NOTI_ICON_SIZE);
        NotificationCompat.Builder builder = createBuilder(getApplicationContext(), alarmChannelList.peek())
                .setSmallIcon(R.mipmap.morale_notify_icon)
                .setLargeIcon(moraleBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setTicker(title)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(contentPendingIntent);

        builder = setSoundSetting(getApplicationContext(), mAudioManager, builder);
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

    private Notification createAkashiRepairNotification(int nid) {
        PendingIntent contentPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class)
                        .setAction(CLICK_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class)
                        .setAction(DELETE_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = getString(R.string.kca_noti_title_akashirepair_recovered);
        String content = getString(R.string.kca_noti_content_akashirepair_recovered);

        Bitmap akashiRepairBitmap = KcaUtils.decodeSampledBitmapFromResource(getResources(),  R.mipmap.docking_akashi_notify_bigicon, NOTI_ICON_SIZE, NOTI_ICON_SIZE);
        NotificationCompat.Builder builder = createBuilder(getApplicationContext(), alarmChannelList.peek())
                .setSmallIcon(R.mipmap.docking_notify_icon)
                .setLargeIcon(akashiRepairBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setTicker(title)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(contentPendingIntent);

        builder = setSoundSetting(getApplicationContext(), mAudioManager, builder);
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

    private Notification createUpdateNotification(int type, String version, int nid) {
        PendingIntent contentPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class)
                        .setAction(UPDATE_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deletePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, KcaAlarmService.class)
                        .setAction(DELETE_ACTION.concat(String.valueOf(nid))),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int title_text_id;
        switch(type) {
            case 1:
                title_text_id = R.string.ma_hasdataupdate;
                break;
            default:
                title_text_id = R.string.ma_hasupdate;
                break;
        }
        String title = getString(title_text_id).replace("(%s)", "").trim();
        String content = version;

        Bitmap updateBitmap = KcaUtils.decodeSampledBitmapFromResource(getResources(),
                getId("ic_update_" + String.valueOf(type), R.mipmap.class), NOTI_ICON_SIZE, NOTI_ICON_SIZE);
        NotificationCompat.Builder builder = createBuilder(getApplicationContext(), alarmChannelList.peek())
                .setSmallIcon(R.mipmap.ic_stat_notify_1)
                .setLargeIcon(updateBitmap)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setTicker(title)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(contentPendingIntent);

        builder = setSoundSetting(getApplicationContext(), mAudioManager, builder);
        Notification Notifi = builder.build();
        Notifi.flags = Notification.FLAG_AUTO_CANCEL;
        return Notifi;
    }

    private void show_notification(int id, Notification notification) {
        try {
            notificationManager.notify(id, notification);
        } catch (Exception e) {
            if (dbHelper != null) {
                dbHelper.recordErrorLog(ERROR_TYPE_NOTI, "alarm_error", "", "", getStringFromException(e));
            }
        }

    }

    private int setDefaultGameData() {
        return KcaUtils.setDefaultGameData(getApplicationContext(), dbHelper);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
