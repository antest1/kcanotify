package com.antest1.kcanotify;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.antest1.kcanotify.KcaAlarmService.ALARM_DELAY;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.getUserShipDataById;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_DECKPORT;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_KDOCKDATA;
import static com.antest1.kcanotify.KcaConstants.DB_KEY_NDOCKDATA;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_SHOW_CONSTRSHIP_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_TIMER_WIDGET_STATE;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.getTimeStr;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaTimerWidget extends AppWidgetProvider {
    private static final int UPDATE_INTERVAL = 500;

    public static final int STATE_EXPD = 1;
    public static final int STATE_DOCK = 2;
    public static final int STATE_CONSTR = 3;

    public static final String WIDGET_MENU_CHANGE_FORMAT = "com.antest1.kcanotify.widget.menuchange";
    public static final String WIDGET_DATA_UPDATE = "com.antest1.kcanotify.widget.dataupdate";

    private static JsonObject widgetData = new JsonObject();
    private static ScheduledExecutorService executor;

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
        int[] currentAppWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
        if (currentAppWidgetIds.length > 0 && executor == null) {
            setExecutor(context, appWidgetManager);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    public void setWidget(Context context, AppWidgetManager manager, int widgetId, int status) {
        int layout_id = R.layout.widget_timer;
        int[] menu_list = { R.id.menu_1, R.id.menu_2, R.id.menu_3 };
        int[] menu_text_list = { R.string.viewmenu_excheck, R.string.viewmenu_docking, R.string.viewmenu_construction };

        RemoteViews views = new RemoteViews(context.getPackageName(), layout_id);
        for (int i = 0; i < menu_list.length; i++) {
            int color = status == i + 1 ? R.color.colorAccent : R.color.white;
            views.setTextColor(menu_list[i], ContextCompat.getColor(context, color));
            views.setTextViewText(menu_list[i], getStringWithLocale(context, menu_text_list[i]));

            Intent changeIntent = new Intent(context, KcaTimerWidget.class);
            changeIntent.setAction(KcaUtils.format(WIDGET_MENU_CHANGE_FORMAT+"%d", i+1));
            changeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            changeIntent.putExtra("menu", i + 1);

            PendingIntent changePendingIntent = PendingIntent.getBroadcast(context, widgetId+10, changeIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(menu_list[i], changePendingIntent);
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
        KcaDBHelper dbHelper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);

        List<AbstractMap.SimpleEntry<String, String>> entries = new ArrayList<>();
        switch (status) {
            case STATE_EXPD:
                String name_format = getStringWithLocale(context, R.string.fleet_format);
                if (!widgetData.has("deckport") || widgetData.get("deckport").isJsonNull()) {
                    for (int i = 0; i < 4; i++) entries.add(new AbstractMap.SimpleEntry<>("no data", ""));
                } else {
                    JsonArray deckport = widgetData.getAsJsonArray("deckport");
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
                        int prev_size = entries.size();
                        for (int i = 0; i < 3 - prev_size; i++) {
                            entries.add(new AbstractMap.SimpleEntry<>("CLOSED", ""));
                        }
                        entries.add(new AbstractMap.SimpleEntry<>("", ""));
                    }
                }
                break;
            case STATE_DOCK:
                if (!widgetData.has("ndock") || widgetData.get("ndock").isJsonNull()) {
                    for (int i = 0; i < 4; i++) entries.add(new AbstractMap.SimpleEntry<>("no data", ""));
                } else {
                    JsonArray ndock = widgetData.getAsJsonArray("ndock");
                    if (ndock == null) {
                        for (int i = 0; i < 4; i++)
                            entries.add(new AbstractMap.SimpleEntry<>("no data", ""));
                    } else {
                        for (int i = 0; i < ndock.size(); i++) {
                            JsonObject item = ndock.get(i).getAsJsonObject();
                            if (item != null && item.get("api_state").getAsInt() != -1) {
                                int ship_id = item.get("api_ship_id").getAsInt();
                                if (ship_id > 0) {
                                    String ship_name = "";
                                    JsonObject shipData = getUserShipDataById(ship_id, "ship_id");
                                    if (shipData != null) {
                                        JsonObject kcShipData = KcaApiData.getKcShipDataById(shipData.get("ship_id").getAsInt(), "name");
                                        if (kcShipData != null) {
                                            ship_name = getShipTranslation(kcShipData.get("name").getAsString(), ship_id, false);
                                        }
                                    }
                                    entries.add(new AbstractMap.SimpleEntry<>(ship_name, getLeftTimeStr(item.get("api_complete_time").getAsLong())));
                                } else {
                                    entries.add(new AbstractMap.SimpleEntry<>("-", ""));
                                }
                            } else {
                                entries.add(new AbstractMap.SimpleEntry<>("CLOSED", ""));
                            }
                        }
                    }
                }
                break;
            case STATE_CONSTR:
                if (!widgetData.has("kdock") || widgetData.get("kdock").isJsonNull()) {
                    for (int i = 0; i < 4; i++) entries.add(new AbstractMap.SimpleEntry<>("no data", ""));
                } else {
                    JsonArray kdock = widgetData.getAsJsonArray("kdock");
                    boolean show_shipname = getBooleanPreferences(context, PREF_SHOW_CONSTRSHIP_NAME);
                    for (int i = 0; i < kdock.size(); i++) {
                        JsonObject item = kdock.get(i).getAsJsonObject();
                        if (item.get("api_state").getAsInt() != -1) {
                            int ship_id = item.get("api_created_ship_id").getAsInt();
                            if (ship_id > 0) {
                                JsonObject shipdata = KcaApiData.getKcShipDataById(item.get("api_created_ship_id").getAsInt(), "name");
                                String shipname = show_shipname ? KcaApiData.getShipTranslation(shipdata.get("name").getAsString(), ship_id, false) : "？？？";
                                entries.add(new AbstractMap.SimpleEntry<>(shipname, getConstrLeftTimeStr(item.get("api_complete_time").getAsLong())));
                            } else {
                                entries.add(new AbstractMap.SimpleEntry<>("-", ""));
                            }
                        } else {
                            entries.add(new AbstractMap.SimpleEntry<>("CLOSED", ""));
                        }
                    }
                }
                break;
            default:
                break;
        }
        return entries;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        int[] appWidgetIds =  manager.getAppWidgetIds(new ComponentName(context, getClass()));
        JsonObject state;

        try {
            state = JsonParser.parseString(getStringPreferences(context, PREF_TIMER_WIDGET_STATE)).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            state = new JsonObject();
        }

        if (action != null && action.startsWith(WIDGET_MENU_CHANGE_FORMAT)) {
            int target_widgetid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (target_widgetid > 0) {
                int menu = intent.getIntExtra("menu", 1);
                state.addProperty(String.valueOf(target_widgetid), menu);
                setWidget(context, manager, target_widgetid, menu);
            }
            setPreferences(context, PREF_TIMER_WIDGET_STATE, state.toString());
        }

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            if (!widgetData.has("deckport")) {
                updateData(context);
            }

            if (appWidgetIds.length > 0 && executor == null) {
                setExecutor(context, manager);
            }
        }

        if (WIDGET_DATA_UPDATE.equals(action)) {
            updateData(context);
            if (appWidgetIds.length > 0 && executor == null) {
                setExecutor(context, manager);
            }
        }
    }

    private void setExecutor(Context context, AppWidgetManager manager) {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (!widgetData.has("deckport")) {
                updateData(context);
            }

            int[] appWidgetIds =  manager.getAppWidgetIds(new ComponentName(context, getClass()));
            JsonObject state;
            try {
                state = JsonParser.parseString(getStringPreferences(context, PREF_TIMER_WIDGET_STATE)).getAsJsonObject();
            } catch (JsonSyntaxException | IllegalStateException e) {
                state = new JsonObject();
            }
            Log.e("KCA", Arrays.toString(appWidgetIds));

            for (int id: appWidgetIds) {
                String key = String.valueOf(id);
                int menu = 1;
                if (state.has(key)) menu = state.get(key).getAsInt();
                else state.addProperty(key, menu);
                setWidget(context, manager, id, menu);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static String getLeftTimeStr(long target_time) {
        if (target_time == -1) return "";
        else {
            int left_time = (int) (target_time - System.currentTimeMillis() - ALARM_DELAY) / 1000;
            if (left_time < 0) left_time = 0;
            return getTimeStr(left_time);
        }
    }

    public static String getConstrLeftTimeStr(long complete_time) {
        if (complete_time <= 0) return "Completed!";
        else {
            int left_time = (int) (complete_time - System.currentTimeMillis()) / 1000;
            if (left_time < 0) return "Completed!";

            int sec, min, hour;
            sec = left_time;
            min = sec / 60;
            hour = min / 60;
            sec = sec % 60;
            min = min % 60;

            return KcaUtils.format("%02d:%02d:%02d", hour, min, sec);
        }
    }

    private void updateData(Context context) {
        KcaDBHelper dbHelper = new KcaDBHelper(context, null, KCANOTIFY_DB_VERSION);
        JsonObject new_data = new JsonObject();
        new_data.add("deckport", dbHelper.getJsonArrayValue(DB_KEY_DECKPORT));
        new_data.add("ndock", dbHelper.getJsonArrayValue(DB_KEY_NDOCKDATA));
        new_data.add("kdock", dbHelper.getJsonArrayValue(DB_KEY_KDOCKDATA));
        widgetData = new_data;
        dbHelper.close();
    }
}
