package com.antest1.kcanotify;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
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
import android.widget.RelativeLayout;
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
    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver battleinfo_receiver;
    private BroadcastReceiver battlehdmg_receiver;
    private BroadcastReceiver battlenode_receiver;
    private View mView;
    private WindowManager mManager;
    private boolean isViewEnabled;

    private ImageView viewbutton;
    WindowManager.LayoutParams mParams;

    public static JsonObject currentApiData;
    public static int clickcount;

    public static JsonObject getCurrentApiData() {
        return currentApiData;
    }

    public static int getClickCount() {
        return clickcount;
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
        isViewEnabled = false;
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
                //Log.e("KCA", "KCA_MSG_BATTLE_INFO Received: \n".concat(s));
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
                //Log.e("KCA", "KCA_MSG_BATTLE_NODE Received: \n".concat(s));
            }
        };
        battlehdmg_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(KCA_MSG_DATA);
                Intent intent_send = new Intent(KCA_MSG_BATTLE_VIEW_HDMG);
                intent_send.putExtra(KCA_MSG_DATA, "");
                if(viewbutton != null) {
                    viewbutton.getDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(),
                            R.color.colorHeavyDmgStateWarn), PorterDuff.Mode.MULTIPLY);
                }
                broadcaster.sendBroadcast(intent_send);
                //Log.e("KCA", "KCA_MSG_BATTLE_HDMG Received: \n".concat(s));
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver((battleinfo_receiver), new IntentFilter(KCA_MSG_BATTLE_INFO));
        LocalBroadcastManager.getInstance(this).registerReceiver((battlenode_receiver), new IntentFilter(KCA_MSG_BATTLE_NODE));
        LocalBroadcastManager.getInstance(this).registerReceiver((battlehdmg_receiver), new IntentFilter(KCA_MSG_BATTLE_HDMG));
        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.view_button, null);

        viewbutton = (ImageView) mView.findViewById(R.id.viewbutton);
        viewbutton.setOnTouchListener(mViewTouchListener);
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.TOP | Gravity.LEFT;

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
    }

    @Override
    public void onDestroy() {
        mManager.removeViewImmediate(mView);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battleinfo_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlenode_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlehdmg_receiver);
        stopService(new Intent(getBaseContext(), KcaBattleViewService.class));
        super.onDestroy();
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
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
                    break;

                case MotionEvent.ACTION_UP:
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
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;
                    mManager.updateViewLayout(mView, mParams);
                    break;
            }
            return true;
        }
    };
}