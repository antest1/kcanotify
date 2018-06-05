package com.antest1.kcanotify;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.gson.JsonArray;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaAlarmService.ALARM_DELAY;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_KDOCKDATA;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_NDOCKDATA;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_TIMER_WIDGET_STATE;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getTimeStr;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaTimerWidget extends AppWidgetProvider {
    private static final int UPDATE_INTERVAL = 500;

    private static PendingIntent updateIntent;
    private static AlarmManager alarmManager;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    public String getStringWithLocale(Context context, int id) {
        return KcaUtils.getStringWithLocale(context.getApplicationContext(), context, id);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        int layout_id = R.layout.widget_timer;
        for (int i = 0; i < appWidgetIds.length; i++) {
            int widgetId = appWidgetIds[i];
            RemoteViews views = new RemoteViews(context.getPackageName(), layout_id);
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    public void setWidget(Context context, AppWidgetManager manager, int widgetId, int status) {
        int layout_id = R.layout.widget_timer;
        int[] menu_list = { R.id.menu_1 }; //, R.id.menu_2, R.id.menu_3 };
        int[] menu_text_list = { R.string.viewmenu_excheck };
        String fairyId = "noti_icon_".concat(getStringPreferences(context, PREF_FAIRY_ICON));
        int fairy_bitmap = getId(fairyId.concat("_small"), R.mipmap.class);

        RemoteViews views = new RemoteViews(context.getPackageName(), layout_id);
        views.setImageViewResource(R.id.open_icon, fairy_bitmap);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.open_icon, openIntent);

        for (int i = 0; i < menu_list.length; i++) {
            int color = status == i + 1 ? R.color.colorAccent : R.color.white;
            views.setTextColor(menu_list[i], ContextCompat.getColor(context, color));
            views.setTextViewText(menu_list[i], getStringWithLocale(context, menu_text_list[i]));
        }

        List<AbstractMap.SimpleEntry<String, String>> data = getTimerData(context, status);
        for (int i = 0; i < data.size(); i++) {
            int name = KcaUtils.getId(KcaUtils.format("item%d_name", i+1), R.id.class);
            int time = KcaUtils.getId(KcaUtils.format("item%d_time", i+1), R.id.class);
            AbstractMap.SimpleEntry<String, String> item = data.get(i);
            String name_value = item.getKey();
            String time_value = item.getValue();
            views.setTextViewText(name, name_value);
            views.setTextViewText(time, time_value);
            if (time_value.contains(":") && Integer.parseInt(time_value.replace(":", "")) == 0) {
                views.setTextColor(time, ContextCompat.getColor(context, R.color.colorWidgetAlert));
            } else {
                views.setTextColor(time, ContextCompat.getColor(context, R.color.white));
            }
        }
        manager.updateAppWidget(widgetId, views);
    }

    public List<AbstractMap.SimpleEntry<String, String>> getTimerData(Context context, int status) {
        List<AbstractMap.SimpleEntry<String, String>> entries = new ArrayList<>();
        KcaDBHelper dbHelper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
        switch (status) {
            case 1:
                String name_format = getStringWithLocale(context, R.string.widget_timer_fleetname);
                JsonArray deckport = dbHelper.getJsonArrayValue(DB_KEY_DECKPORT);
                if (deckport == null) {
                    for (int i = 0; i < 4; i++) entries.add(new AbstractMap.SimpleEntry<>("no data", ""));
                } else {
                    for (int i = 1; i < deckport.size(); i++) {
                        JsonArray mission = deckport.get(i).getAsJsonObject().getAsJsonArray("api_mission");
                        if (mission.get(0).getAsInt() == 1) {
                            int mission_no = mission.get(1).getAsInt();
                            String expedition_name = KcaUtils.format(name_format, i+1).concat(" ").concat(KcaExpedition2.getExpeditionHeader(mission_no).trim());
                            String arrive_time = getLeftTimeStr(mission.get(2).getAsLong());
                            entries.add(new AbstractMap.SimpleEntry<>(expedition_name, arrive_time));
                        } else {
                            String expedition_name = KcaUtils.format(name_format, i+1);
                            entries.add(new AbstractMap.SimpleEntry<>(expedition_name, "-"));
                        }
                    }
                    entries.add(new AbstractMap.SimpleEntry<>("", ""));
                }
                break;
            case 2:
                JsonArray ndock = dbHelper.getJsonArrayValue(DB_KEY_NDOCKDATA);
                break;
            case 3:
                JsonArray kdock = dbHelper.getJsonArrayValue(DB_KEY_KDOCKDATA);
                break;
            default:
                break;
        }
        dbHelper.close();
        return entries;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        Log.e("KCA", String.valueOf(action));
        int[] appWidgetIds =  manager.getAppWidgetIds(new ComponentName(context, getClass()));

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            clearAlarm();
            long firstTime = System.currentTimeMillis() + UPDATE_INTERVAL;
            updateIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC, firstTime, updateIntent);

            for (int id: appWidgetIds) {
                setWidget(context, manager, id, getWidgetState(context, id));
            }
        }

        if (AppWidgetManager.ACTION_APPWIDGET_DISABLED.equals(action)) {
            clearAlarm();
        }
    }


    public void addWidgetState(Context context, int id) {
        String current_pref = getStringPreferences(context, PREF_TIMER_WIDGET_STATE);
        setPreferences(context, PREF_TIMER_WIDGET_STATE, KcaUtils.format("%s%d,%d|", current_pref, id, 0));
    }

    public void removeWidgetState(Context context, int id) {
        String pref = getStringPreferences(context, PREF_TIMER_WIDGET_STATE);
        int state = getWidgetState(context, id);
        pref = pref.replace(KcaUtils.format("%d,%d|", id, state), "");
        setPreferences(context, PREF_TIMER_WIDGET_STATE, pref);
    }

    public int getWidgetState(Context context, int id) {
        String id_str = String.valueOf(id);
        String pref = getStringPreferences(context, PREF_TIMER_WIDGET_STATE);
        String[] prefs = pref.split("|");
        for (String p: prefs) {
            if (p.contains(id_str)) {
                String[] data = p.split(",");
                return Integer.parseInt(data[1]);
            }
        }
        return 1;
    }

    public static String getLeftTimeStr(long target_time) {
        if (target_time == -1) return "";
        else {
            int left_time = (int) (target_time - System.currentTimeMillis() - ALARM_DELAY) / 1000;
            if (left_time < 0) left_time = 0;
            return getTimeStr(left_time);
        }
    }

    public void clearAlarm()  {
        if(alarmManager != null && updateIntent != null) {
            updateIntent.cancel();
            alarmManager.cancel(updateIntent);
        }
    }

}
