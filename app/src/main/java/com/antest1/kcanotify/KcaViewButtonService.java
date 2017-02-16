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

import java.util.Calendar;

import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_NODE;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_DATA;

public class KcaViewButtonService extends Service {
    public static final int FAIRY_NOTIFICATION_ID = 10118;
    public static final String RETURN_FAIRY_ACTION = "return_fairy_action";
    public static final String REMOVE_FAIRY_ACTION = "remove_fairy_action";

    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver battleinfo_receiver;
    private BroadcastReceiver battlehdmg_receiver;
    private BroadcastReceiver battlenode_receiver;
    private BroadcastReceiver buttontop_receiver;
    private View mView;
    private WindowManager mManager;
    private Handler mHandler;
    private Vibrator vibrator;
    private ImageView viewbutton;
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
        mHandler = new Handler();
        startService(new Intent(getBaseContext(), KcaBattleViewService.class));
        broadcaster = LocalBroadcastManager.getInstance(this);
        battleinfo_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(KCA_MSG_DATA);
                currentApiData = new JsonParser().parse(s).getAsJsonObject();
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
        viewbutton.getDrawable().clearColorFilter();
        viewbutton.setOnTouchListener(mViewTouchListener);
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
        if(intent != null && intent.getAction() != null && intent.getAction().equals(RETURN_FAIRY_ACTION)) {
            mView.setVisibility(View.VISIBLE);
        }
        notificationManager.cancel(FAIRY_NOTIFICATION_ID);
        Log.e("KCA-V", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mManager.removeView(mView);
        notificationManager.cancel(FAIRY_NOTIFICATION_ID);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battleinfo_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlenode_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlehdmg_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(buttontop_receiver);
        stopService(new Intent(getBaseContext(), KcaBattleViewService.class));
        super.onDestroy();
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private static final int LONG_CLICK_DURATION = 800;

        private long startClickTime;
        private long recentClickTime;
        private boolean longPressTriggered = false;
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
                        startService(new Intent(getBaseContext(), KcaBattleViewService.class));
                    }
                    int[] locations = new int[2];
                    mView.getLocationOnScreen(locations);
                    int xx = locations[0];
                    int yy = locations[1];
                    Log.e("KCA", String.format("Coord: %d %d", xx, yy));
                    longPressTriggered = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;
                    mManager.updateViewLayout(mView, mParams);
                    if(Math.abs(x) > 20 || Math.abs(y) > 20) {
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
                        .setSmallIcon(R.mipmap.noti_icon4)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.noti_icon3))
                        .setContentTitle(getStringWithLocale(R.string.fairy_hidden_notification_title))
                        .setContentText(getStringWithLocale(R.string.fairy_hidden_notification_text))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(getStringWithLocale(R.string.fairy_hidden_notification_bigtext)))
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOngoing(true)
                        .addAction(new NotificationCompat.Action(R.mipmap.noti_icon4,
                                getStringWithLocale(R.string.fairy_hidden_notification_action_return), returnPendingIntent))
                        .addAction(new NotificationCompat.Action(R.mipmap.ic_cancel,
                                getStringWithLocale(R.string.fairy_hidden_notification_action_remove), removePendingIntent));

        notificationManager.notify(FAIRY_NOTIFICATION_ID, notificationBuilder.build());
    }
}