package com.antest1.kcanotify;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.FAIRY_REVERSE_LIST;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_NODE;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_DATA;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_LIST;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_VIEW_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;

public class KcaViewButtonService extends Service {
    public static final int FAIRY_NOTIFICATION_ID = 10118;
    public static final String RETURN_FAIRY_ACTION = "return_fairy_action";
    public static final String REMOVE_FAIRY_ACTION = "remove_fairy_action";
    public static final String SHOW_BATTLE_INFO = "show_battle_info";
    public static final String SHOW_QUEST_INFO = "show_quest_info";
    public static final String REFRESH_BATTLEVIEW_ACTION = "refresh_battleview";
    public static final String SHOW_BATTLEVIEW_ACTION = "show_battleview";
    public static final String REFRESH_QUESTVIEW_ACTION = "refresh_questview";
    public static final String SHOW_QUESTVIEW_ACTION = "show_questview";

    public static final int BATTLE_MODE = 1;
    public static final int QUEST_MODE = 2;

    private boolean hidden = false;
    private int status = -1;
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver battleinfo_receiver;
    private BroadcastReceiver battlehdmg_receiver;
    private BroadcastReceiver battlenode_receiver;
    private BroadcastReceiver questlist_receiver;
    private View mView;
    private WindowManager mManager;
    private Handler mHandler;
    private Vibrator vibrator;
    private ImageView viewbutton;
    public int viewBitmapId = 0;
    public int viewBitmapSmallId = 0;
    WindowManager.LayoutParams mParams;
    NotificationManagerCompat notificationManager;
    public static JsonObject currentApiData;
    public static int type;
    public static int clickcount;

    public static JsonObject getCurrentApiData() {
        return currentApiData;
    }

    public static int getClickCount() {
        return clickcount;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        clickcount = 0;
        hidden = false;
        status = -1;
        mHandler = new Handler();
        broadcaster = LocalBroadcastManager.getInstance(this);
        battleinfo_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(KCA_MSG_DATA);
                currentApiData = new JsonParser().parse(s).getAsJsonObject();
                KcaBattleViewService.setApiData(currentApiData);
                Intent intent_send = new Intent(KCA_MSG_BATTLE_VIEW_REFRESH);
                intent_send.putExtra(KCA_MSG_DATA, s);
                broadcaster.sendBroadcast(intent_send);
                Log.e("KCA", "KCA_MSG_BATTLE_INFO Received: \n".concat(s));
            }
        };
        battlenode_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(KCA_MSG_DATA);
                currentApiData = new JsonParser().parse(s).getAsJsonObject();
                KcaBattleViewService.setApiData(currentApiData);
                Intent intent_send = new Intent(KCA_MSG_BATTLE_VIEW_REFRESH);
                intent_send.putExtra(KCA_MSG_DATA, s);
                broadcaster.sendBroadcast(intent_send);
                Log.e("KCA", "KCA_MSG_BATTLE_NODE Received: \n".concat(s));
            }
        };
        battlehdmg_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(KCA_MSG_DATA);
                Intent intent_send = new Intent(KCA_MSG_BATTLE_VIEW_HDMG);
                intent_send.putExtra(KCA_MSG_DATA, s);
                if (s.contains("1")) {
                    ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(),
                            R.color.colorHeavyDmgStateWarn), PorterDuff.Mode.MULTIPLY);
                } else {
                    ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().clearColorFilter();
                }
                broadcaster.sendBroadcast(intent_send);
                Log.e("KCA", "KCA_MSG_BATTLE_HDMG Received");
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver((battleinfo_receiver), new IntentFilter(KCA_MSG_BATTLE_INFO));
        LocalBroadcastManager.getInstance(this).registerReceiver((battlenode_receiver), new IntentFilter(KCA_MSG_BATTLE_NODE));
        LocalBroadcastManager.getInstance(this).registerReceiver((battlehdmg_receiver), new IntentFilter(KCA_MSG_BATTLE_HDMG));
        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        mView = mInflater.inflate(R.layout.view_button, null);
        viewbutton = (ImageView) mView.findViewById(R.id.viewbutton);
        String fairyId = "noti_icon_".concat(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
        viewBitmapId = getId(fairyId, R.mipmap.class);
        viewBitmapSmallId = getId(fairyId.concat("_small"), R.mipmap.class);
        viewbutton.setImageResource(viewBitmapId);
        viewbutton.getDrawable().clearColorFilter();
        viewbutton.setOnTouchListener(mViewTouchListener);

        int fairyIdValue = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON));
        int index = Arrays.binarySearch(FAIRY_REVERSE_LIST, fairyIdValue);
        if (index >= 0) viewbutton.setScaleX(-1.0f);
        else viewbutton.setScaleX(1.0f);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width, height;
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int vw = mView.getMeasuredWidth();
        int vh = mView.getMeasuredHeight();

        mParams.y = height - vh;
        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);
        mView.setVisibility(View.VISIBLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(REMOVE_FAIRY_ACTION)) {
                notificationManager.cancel(FAIRY_NOTIFICATION_ID);
            }
            if (intent.getAction().equals(RETURN_FAIRY_ACTION)) {
                hidden = false;
            }
            if (intent.getAction().equals(SHOW_BATTLE_INFO)) {
                status = BATTLE_MODE;
                Intent qintent = new Intent(getBaseContext(), KcaBattleViewService.class);
                qintent.setAction(REFRESH_BATTLEVIEW_ACTION);
                startService(qintent);
            }
            if (intent.getAction().equals(SHOW_QUEST_INFO)) {
                status = QUEST_MODE;
                Intent qintent = new Intent(getBaseContext(), KcaQuestViewService.class);
                qintent.setAction(REFRESH_QUESTVIEW_ACTION);
                startService(qintent);
            }
        }
        if (!hidden) {
            mView.setVisibility(View.VISIBLE);
            notificationManager.cancel(FAIRY_NOTIFICATION_ID);
        }
        Log.e("KCA-V", String.format("onStartCommand %d", status));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mManager.removeView(mView);
        notificationManager.cancel(FAIRY_NOTIFICATION_ID);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battleinfo_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlenode_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlehdmg_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(questlist_receiver);
        if (status == BATTLE_MODE) {
            stopService(new Intent(getBaseContext(), KcaBattleViewService.class));
        } else if (status == QUEST_MODE) {
            stopService(new Intent(getBaseContext(), KcaQuestViewService.class));
        }
        status = -1;
        super.onDestroy();
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private static final int LONG_CLICK_DURATION = 800;

        private long startClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = mParams.x;
                    mViewY = mParams.y;
                    startClickTime = Calendar.getInstance().getTimeInMillis();
                    mHandler.postDelayed(mRunnable, LONG_CLICK_DURATION);
                    break;

                case MotionEvent.ACTION_UP:
                    Log.e("KCA", "Callback Canceled");
                    mHandler.removeCallbacks(mRunnable);
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                    if (clickDuration < MAX_CLICK_DURATION) {
                        clickcount += 1;
                        if (status == BATTLE_MODE) {
                            Intent qintent = new Intent(getBaseContext(), KcaBattleViewService.class);
                            qintent.setAction(SHOW_BATTLEVIEW_ACTION);
                            startService(qintent);
                        } else if (status == QUEST_MODE) {
                            Intent qintent = new Intent(getBaseContext(), KcaQuestViewService.class);
                            qintent.setAction(SHOW_QUESTVIEW_ACTION);
                            startService(qintent);
                        }
                    }
                    int[] locations = new int[2];
                    mView.getLocationOnScreen(locations);
                    int xx = locations[0];
                    int yy = locations[1];
                    Log.e("KCA", String.format("Coord: %d %d", xx, yy));
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;
                    mManager.updateViewLayout(mView, mParams);
                    if (Math.abs(x) > 20 || Math.abs(y) > 20) {
                        Log.e("KCA", "Callback Canceled");
                        mHandler.removeCallbacks(mRunnable);
                    }
                    break;
            }
            return true;
        }
    };


    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            hidden = true;
            vibrator.vibrate(100);
            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.viewbutton_hide), Toast.LENGTH_LONG).show();
            mView.setVisibility(View.GONE);
            displayNotification(getApplicationContext());
        }
    };

    public void displayNotification(Context context) {

        Intent returnIntent = new Intent(this, KcaViewButtonService.class)
                .setAction(RETURN_FAIRY_ACTION);

        Intent removeIntent = new Intent(this, KcaViewButtonService.class)
                .setAction(REMOVE_FAIRY_ACTION);

        PendingIntent returnPendingIntent = PendingIntent.getService(context, 0,
                returnIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent removePendingIntent = PendingIntent.getService(context, 0,
                removeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(viewBitmapSmallId)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), viewBitmapId))
                        .setContentTitle(getStringWithLocale(R.string.fairy_hidden_notification_title))
                        .setContentText(getStringWithLocale(R.string.fairy_hidden_notification_text))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(getStringWithLocale(R.string.fairy_hidden_notification_bigtext)))
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOngoing(true)
                        .addAction(new NotificationCompat.Action(viewBitmapSmallId,
                                getStringWithLocale(R.string.fairy_hidden_notification_action_return), returnPendingIntent))
                        .addAction(new NotificationCompat.Action(R.mipmap.ic_cancel,
                                getStringWithLocale(R.string.fairy_hidden_notification_action_remove), removePendingIntent));

        notificationManager.notify(FAIRY_NOTIFICATION_ID, notificationBuilder.build());
    }
}