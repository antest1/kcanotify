package com.antest1.kcanotify;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaConstants.FAIRY_TOTAL_COUNT;
import static com.antest1.kcanotify.KcaConstants.KCA_API_PREF_FAIRY_CHANGED;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class KcaFairySelectActivity extends AppCompatActivity {
    Toolbar toolbar;
    private static Handler sHandler;
    static Gson gson = new Gson();
    GridView gv;

    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public KcaFairySelectActivity() {
        LocaleUtils.updateConfig(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_fairy);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.setting_menu_kand_title_fairy_select));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        List<Integer> fairy_id = new ArrayList<>();
        for(int i = 0; i<FAIRY_TOTAL_COUNT; i++) {
            int btnId = getId("noti_icon_".concat(String.valueOf(i)), R.mipmap.class);
            fairy_id.add(btnId);
        }

        final FairyAdapter adapter = new FairyAdapter(getApplicationContext(),
                R.layout.listview_fairy_item, fairy_id);

        String pref_value = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        if (pref_value.length() > 0) {
            adapter.setPrevActive(Integer.parseInt(pref_value));
        }

        gv = (GridView)findViewById(R.id.fairy_gridview);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setPreferences(getApplicationContext(), PREF_FAIRY_ICON, String.valueOf(position));
                if (KcaService.getServiceStatus()) {
                    JsonObject data = new JsonObject();
                    data.addProperty("id", position);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_PREF_FAIRY_CHANGED);
                    bundle.putString("data", data.toString());
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
                adapter.setPrevActive(position);
                gv.invalidateViews();
                if (MainActivity.kcafairybtn != null) {
                    String fairyPath = "noti_icon_".concat(String.valueOf(position));
                    int viewBitmapSmallId = getId(fairyPath.concat("_small"), R.mipmap.class);
                    MainActivity.kcafairybtn.setImageResource(viewBitmapSmallId);
                    MainActivity.kcafairybtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                            R.color.colorBtnText), PorterDuff.Mode.MULTIPLY);

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

class FairyAdapter extends BaseAdapter {
    Context context;
    int layout;
    List<Integer> fairy = new ArrayList<>();
    LayoutInflater inf;
    private int prevactive = -1;

    public FairyAdapter(Context context, int layout, List<Integer> data) {
        this.context = context;
        this.layout = layout;
        this.fairy = data;
        inf = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setPrevActive(int v) {
        prevactive = v;
    }

    @Override
    public int getCount() {
        return fairy.size();
    }

    @Override
    public Object getItem(int position) {
        return fairy.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView==null)
            convertView = inf.inflate(layout, null);
        ImageView iv = (ImageView) convertView.findViewById(R.id.setting_fairy_pic);
        iv.setImageResource(fairy.get(position));
        if (position == prevactive) iv.setBackground(ContextCompat.getDrawable(context, R.drawable.imagebtn_on));
        else iv.setBackground(ContextCompat.getDrawable(context, R.drawable.imagebtn_off));
        return convertView;
    }
}