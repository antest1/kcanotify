package com.antest1.kcanotify;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.getExpeditionInfo;
import static com.antest1.kcanotify.KcaApiData.getQuestTrackInfo;
import static com.antest1.kcanotify.KcaApiData.getShipTypeAbbr;
import static com.antest1.kcanotify.KcaApiData.isQuestTrackable;
import static com.antest1.kcanotify.KcaApiData.kcQuestInfoData;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class KcaQuestListAdpater extends BaseAdapter {
    private List<JsonObject> listViewItemList = new ArrayList<>();
    private KcaQuestViewService service;
    private Context application_context;
    private KcaQuestTracker questTracker;
    public static final float PROGRESS_1 = 0.5f;
    public static final float PROGRESS_2 = 0.8f;

    public KcaQuestListAdpater(KcaQuestViewService svc, KcaQuestTracker qt) {
        service = svc;
        application_context = svc.getApplicationContext();
        questTracker = qt;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(application_context, service.getBaseContext(), id);
    }

    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.listview_quest_item, parent, false);
            ViewHolder holder = new ViewHolder();

            holder.quest_category = v.findViewById(R.id.quest_category);
            holder.quest_type = v.findViewById(R.id.quest_type);
            holder.quest_name = v.findViewById(R.id.quest_name);
            holder.quest_desc = v.findViewById(R.id.quest_desc);
            holder.quest_progress = v.findViewById(R.id.quest_progress);
            holder.quest_progress_track = v.findViewById(R.id.quest_progress_track);
            holder.quest_desc_full = v.findViewById(R.id.quest_desc_full);
            v.setTag(holder);
        }

        JsonObject item = listViewItemList.get(position);
        Log.e("KCA", String.valueOf(position) + " " + item.toString() );
        final ViewHolder holder = (ViewHolder) v.getTag();

        if (!item.has("api_no")) {
            v.setVisibility(View.INVISIBLE);
            return v;
        } else {
            v.setVisibility(View.VISIBLE);
        }

        String api_no = item.get("api_no").getAsString();
        int api_category = item.get("api_category").getAsInt();
        int api_type = item.get("api_type").getAsInt();
        int api_label_type = item.get("api_label_type").getAsInt();
        int api_progress = item.get("api_progress_flag").getAsInt();
        int api_state = item.get("api_state").getAsInt();

        String api_title = KcaUtils.format("[%s] %s", api_no, item.get("api_title").getAsString());
        String api_detail = item.get("api_detail").getAsString();

        if (kcQuestInfoData.has(api_no)) {
            String code = kcQuestInfoData.getAsJsonObject(api_no).get("code").getAsString();
            String name = kcQuestInfoData.getAsJsonObject(api_no).get("name").getAsString();
            api_title = KcaUtils.format("[%s] %s", code, name);
            api_detail = kcQuestInfoData.getAsJsonObject(api_no).get("desc").getAsString();
            if (kcQuestInfoData.getAsJsonObject(api_no).has("memo")) { // Temporary
                String memo = kcQuestInfoData.getAsJsonObject(api_no).get("memo").getAsString();
                api_detail = api_detail.concat(" (").concat(memo).concat(")");
            }
        }

        holder.quest_category.setText(getStringWithLocale(getId(KcaUtils.format("quest_category_%d", api_category), R.string.class)));
        holder.quest_category.setBackgroundColor(getQuestCategoryColor(api_category));

        holder.quest_type.setText(getQuestLabelString(api_label_type));
        holder.quest_type.setBackgroundColor(getQuestLabelColor(api_label_type));

        holder.quest_name.setText(api_title);
        holder.quest_desc.setText(api_detail);

        String finalApiTitle = api_title;
        String finalApiDetail = api_detail;
        holder.quest_desc_full.setOnClickListener(v1 ->
                service.setAndShowPopup(finalApiTitle, finalApiDetail));

        if (api_progress != 0) {
            holder.quest_progress
                    .setText(getStringWithLocale(getId(KcaUtils.format("quest_progress_%d", api_progress), R.string.class)));
            holder.quest_progress.setBackgroundColor(ContextCompat.getColor(application_context,
                            getId(KcaUtils.format("colorQuestProgress%d", api_progress), R.color.class)));
            holder.quest_progress.setVisibility(View.VISIBLE);
        } else {
            holder.quest_progress.setVisibility(View.GONE);
        }

        if (isQuestTrackable(api_no)) {
            boolean noshowflag = false;
            questTracker.test();
            List<String> trackinfo_list = new ArrayList<>();
            String trackinfo_text = "";
            JsonObject questTrackInfo = getQuestTrackInfo(api_no);
            if (questTrackInfo != null) {
                JsonArray trackData = questTracker.getQuestTrackInfo(api_no);
                JsonArray trackCond = questTrackInfo.getAsJsonArray("cond");

                if (trackData.size() == 1) {
                    JsonArray updatevalue = new JsonArray();
                    if (api_progress == 1 && trackData.get(0).getAsFloat() < trackCond.get(0).getAsFloat() * PROGRESS_1) {
                        updatevalue.add((int) (Math.ceil(trackCond.get(0).getAsFloat() * PROGRESS_1)));
                    } else if (api_progress == 2) {
                        int calculated_value = (int) Math.ceil(trackCond.get(0).getAsFloat() * PROGRESS_2);
                        if (calculated_value >= trackCond.get(0).getAsFloat()) {
                            updatevalue.add(trackCond.get(0).getAsInt() - 1);
                        } else if (trackData.get(0).getAsFloat() < trackCond.get(0).getAsFloat() * PROGRESS_2) {
                            updatevalue.add(calculated_value);
                        }
                    } else if (api_state == 3) {
                        updatevalue.add(trackCond.get(0).getAsInt());
                    }
                    if (updatevalue.size() > 0) {
                        questTracker.updateQuestTrackValueWithId(api_no, updatevalue);
                        trackData = questTracker.getQuestTrackInfo(api_no);
                    }
                }

                for (int n = 0; n < trackData.size(); n++) {
                    int cond = trackCond.get(n).getAsInt() - KcaQuestTracker.getInitialCondValue(api_no);
                    int val = trackData.get(n).getAsInt() - KcaQuestTracker.getInitialCondValue(api_no);
                    Log.e("KCA-QV", api_no + " " + String.valueOf(val) + " " + String.valueOf(cond));
                    trackinfo_list.add(KcaUtils.format("%d/%d", Math.min(val, cond), cond));
                }
                if (trackinfo_list.size() > 0) {
                    trackinfo_text = joinStr(trackinfo_list, ", ");
                } else {
                    noshowflag = true;
                }

                holder.quest_progress_track.setText(trackinfo_text);
                holder.quest_progress_track.setVisibility(View.VISIBLE);
            }
            if (noshowflag) {
                holder.quest_progress_track.setVisibility(View.GONE);
            }
        } else {
            holder.quest_progress_track.setVisibility(View.GONE);
        }

        holder.quest_name.setTextColor(ContextCompat.getColor(application_context,
                        getId(KcaUtils.format("colorQuestState%d", api_state), R.color.class)));
        return v;
    }

    private static class ViewHolder {
        TextView quest_category, quest_type;
        TextView quest_name, quest_progress, quest_progress_track;
        TextView quest_desc;
        ImageView quest_desc_full;
    }

    public void setListViewItemList(JsonArray ship_list, int filter) {
        Type listType = new TypeToken<List<JsonObject>>() {}.getType();
        listViewItemList = new Gson().fromJson(ship_list, listType);
        if (filter != -1) {
            listViewItemList = new ArrayList<>(Collections2.filter(listViewItemList, new Predicate<JsonObject>() {
                @Override
                public boolean apply(JsonObject input) {
                    int category = input.get("api_category").getAsInt();
                    if (filter == 7) {
                        return (category == 1 || category == 5 || category == 7);
                    } else if (filter == 2) {
                        return (category == 2 || category == 8 || category == 9);
                    } else {
                        return (category == filter);
                    }
                }
            }));
        }
        if (listViewItemList.size() > 5 && listViewItemList.size() % 5 != 0) {
            int dummyCount = 5 - listViewItemList.size() % 5;
            for (int i = 0 ; i < dummyCount; i++) {
                listViewItemList.add(new JsonObject());
            }
        }
    }

    public int getQuestCategoryColor(int category) {
        return ContextCompat.getColor(application_context, KcaUtils.getId(KcaUtils.format("colorQuestCategory%d", category), R.color.class));
    }

    public String getQuestLabelString(int label) {
        if (label > 100 && label < 120) {
            String format = getStringWithLocale(R.string.quest_label_type_100);
            return KcaUtils.format(format, label % 100);
        } else {
            return getStringWithLocale(getId(KcaUtils.format("quest_label_type_%d", label), R.string.class));
        }
    }

    public int getQuestLabelColor(int label) {
        if (label > 100 && label < 120) label = 100;
        return ContextCompat.getColor(application_context, KcaUtils.getId(KcaUtils.format("colorQuestLabel%d", label), R.color.class));
    }
}
