package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.getShipTypeAbbr;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_HP_FORMAT;
import static com.antest1.kcanotify.KcaUtils.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

public class KcaFleetViewListItem extends FrameLayout {
    private static final String TAG = "FleetViewItem";

    private final FrameLayout container;
    private final TextView tv_name;
//    private final TextView tv_condmark;
    private final TextView tv_stype;
    private final TextView tv_lv;
    private final TextView tv_exp;
    private final TextView tv_hp;
    private final TextView tv_cond;

    private final SharedPreferences pref;

    private static final String[] HP_FORMAT_LIST = {
            "HP %1$d/%2$d",         // HP 35/37
            "HP %1$d/%2$d +%3$s %4$s",  // HP 35/37 +2 3:24
            "HP %1$d+%3$s/%2$d %4$s",   // HP 35+2/37 3:24
            "HP %1$d/%2$d +%3$s",   // HP 35/37 +2
            "HP %1$d+%3$s/%2$d",    // HP 35+2/37
            "HP %1$d/%2$d %4$s"     // HP 35/37 3:24
    };
    private int hp_format_id = 1;

    private ShipInfo info;

    private boolean isAkashiActive = false;

    public KcaFleetViewListItem(Context context) {
        this(context, null);
    }

    public KcaFleetViewListItem(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KcaFleetViewListItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_fleet_list_item, this);

//        container = findViewById(R.id.container);
        container = this;
        tv_name = findViewById(R.id.name);
//        tv_condmark = findViewById(R.id.condmark);
        tv_stype = findViewById(R.id.stype);
        tv_lv = findViewById(R.id.lv);
        tv_exp = findViewById(R.id.exp);
        tv_hp = findViewById(R.id.hp);
        tv_cond = findViewById(R.id.cond);

        pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
        updateHPFormatId(pref);
    }

    public void setContent(int ship_id) {
        setContent(getShipInfoById(ship_id));
    }

    public void setContent(ShipInfo info) {
        this.info = info;

        Context appContext = getContext().getApplicationContext();

        tv_name.setText(getShipTranslation(info.name, info.mst_ship_id, false));
        tv_stype.setText(getShipTypeAbbr(info.stype));
        tv_stype.setBackgroundColor(ContextCompat.getColor(appContext, R.color.transparent));
        tv_stype.setTextColor(ContextCompat.getColor(appContext, R.color.colorAccent));

        if (info.sally_area > 0) {
            int colorRes = KcaUtils.getId("colorStatSallyArea".concat(String.valueOf(info.sally_area)), R.color.class);
            tv_stype.setBackgroundColor(ContextCompat.getColor(appContext, colorRes));
            tv_stype.getBackground().setAlpha(192);
            tv_stype.setTextColor(ContextCompat.getColor(appContext, R.color.white));
        }

        tv_lv.setText(makeLvString(info.lv));
        tv_exp.setText(makeExpString(info.exp));
        setHPText();

        int cond = info.cond;
        tv_cond.setText(makeCondString(cond));
        if (cond > 49) {
            tv_cond.setBackgroundColor(ContextCompat.getColor(appContext, R.color.colorFleetShipKira));
            tv_cond.setTextColor(ContextCompat.getColor(appContext, R.color.colorPrimaryDark));
        } else if (cond / 10 >= 4) {
            tv_cond.setBackgroundColor(ContextCompat.getColor(appContext, R.color.colorFleetInfoBtn));
            tv_cond.setTextColor(ContextCompat.getColor(appContext, R.color.white));
        } else if (cond / 10 >= 3) {
            tv_cond.setBackgroundColor(ContextCompat.getColor(appContext, R.color.colorFleetInfoBtn));
            tv_cond.setTextColor(ContextCompat.getColor(appContext, R.color.colorFleetShipFatigue1));
        } else if (cond / 10 == 2) {
            tv_cond.setBackgroundColor(ContextCompat.getColor(appContext, R.color.colorFleetShipFatigue1));
            tv_cond.setTextColor(ContextCompat.getColor(appContext, R.color.white));
        } else {
            tv_cond.setBackgroundColor(ContextCompat.getColor(appContext, R.color.colorFleetShipFatigue2));
            tv_cond.setTextColor(ContextCompat.getColor(appContext, R.color.white));
        }

        int now_hp = info.now_hp, max_hp = info.max_hp;
        if (now_hp * 4 <= max_hp) {
            container.setBackgroundColor(ContextCompat.getColor(appContext, R.color.colorFleetWarning));
            tv_hp.setTextColor(ContextCompat.getColor(appContext, R.color.colorHeavyDmgState));
        } else if (now_hp * 2 <= max_hp) {
            container.setBackgroundColor(Color.TRANSPARENT);
            tv_hp.setTextColor(ContextCompat.getColor(appContext, R.color.colorModerateDmgState));
        } else if (now_hp * 4 <= max_hp * 3) {
            container.setBackgroundColor(Color.TRANSPARENT);
            tv_hp.setTextColor(ContextCompat.getColor(appContext, R.color.colorLightDmgState));
        } else if (now_hp != max_hp) {
            container.setBackgroundColor(Color.TRANSPARENT);
            tv_hp.setTextColor(ContextCompat.getColor(appContext, R.color.colorNormalState));
        } else {
            container.setBackgroundColor(Color.TRANSPARENT);
            tv_hp.setTextColor(ContextCompat.getColor(appContext, R.color.colorFullState));
        }

        if (KcaDocking.checkShipInDock(info.ship_id)) {
            container.setBackgroundColor(ContextCompat.getColor(appContext, R.color.colorFleetInRepair));
        }

        container.setVisibility(View.VISIBLE);
    }

    public void setAkashiTimer(boolean isActive) {
        if (!isActive && !isAkashiActive) return; // inactive -> inactive, no need to update
        isAkashiActive = isActive;
        if (info != null) setHPText();
    }

    private void setHPText() {
        int now = info.now_hp, max = info.max_hp;
        int lv = info.lv, stype = info.stype;
        int damage = max - now;
        if (isAkashiActive && !KcaDocking.checkShipInDock(info.ship_id) && damage > 0 && now * 2 > max) {

            int elapsed = KcaAkashiRepairInfo.getAkashiElapsedTimeInSecond();
            int repaired = Math.min(damage, KcaDocking.getRepairedHp(lv, stype, elapsed));
            int next = (repaired < damage) ? KcaDocking.getNextRepair(lv, stype, elapsed) : 0;

            String str = format(
                    HP_FORMAT_LIST[hp_format_id],
                    now, max, repaired,
                    getTimeStr(next, true)
            );
            tv_hp.setText(str);
        } else {

            tv_hp.setText(format(HP_FORMAT_LIST[0], now, max));
        }
    }

    public ShipInfo getShipInfo() {
        return info;
    }

    // region static
    private static String makeLvString(int lv) {
        return format("Lv %d", lv);
    }

    private static String makeExpString(int exp) {
        return format("next: %d", exp);
    }

    private static String makeCondString(int cond) {
        return format("%d", cond);
    }

    private static ShipInfo getShipInfoById(int ship_id) {
        JsonObject userData = KcaApiData.getUserShipDataById(ship_id, "id,ship_id,lv,exp,nowhp,maxhp,cond,sally_area");
        int mst_ship_id = userData.get("ship_id").getAsInt();
        JsonObject kcData = KcaApiData.getKcShipDataById(mst_ship_id, "name,stype");

        return new ShipInfo(
                userData.get("id").getAsInt(),
                mst_ship_id,
                kcData == null ? "" : kcData.get("name").getAsString(),
                kcData == null ? 0 : kcData.get("stype").getAsInt(),
                userData.get("lv").getAsInt(),
                userData.getAsJsonArray("exp").get(1).getAsInt(),
                userData.get("nowhp").getAsInt(), userData.get("maxhp").getAsInt(),
                userData.get("cond").getAsInt(),
                userData.has("sally_area") ? userData.get("sally_area").getAsInt() : 0
        );
    }
    // endregion

    // region preferences
    private void updateHPFormatId(SharedPreferences pref) {
        try {
            hp_format_id = Integer.parseInt(pref.getString(PREF_KCA_HP_FORMAT, "1"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final OnSharedPreferenceChangeListener onSharedPreferenceChanged = (pref, key) -> {
        if (!key.equals(PREF_KCA_HP_FORMAT)) return;
        updateHPFormatId(pref);
    };

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        pref.registerOnSharedPreferenceChangeListener(onSharedPreferenceChanged);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pref.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChanged);
    }
    // endregion

    // region saved instance state
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        return new SavedState(superState, info, isAkashiActive);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState state0 = (SavedState) state;
        super.onRestoreInstanceState(state0.getSuperState());

        isAkashiActive = state0.isAkashiActive;
        setContent(state0.info);
    }

    public static final class ShipInfo implements Parcelable {
        final int ship_id;
        final int mst_ship_id;
        final String name;
        final int stype;
        final int lv, exp;
        final int now_hp, max_hp;
        final int cond;
        final int sally_area;

        public ShipInfo(
                int ship_id, int mst_ship_id, String name, int stype,
                int lv, int exp,
                int now_hp, int max_hp, int cond,
                int sally_area
        ) {

            this.ship_id = ship_id;
            this.mst_ship_id = mst_ship_id;
            this.name = name;
            this.stype = stype;
            this.lv = lv;
            this.exp = exp;
            this.now_hp = now_hp;
            this.max_hp = max_hp;
            this.cond = cond;
            this.sally_area = sally_area;
        }

        public ShipInfo(Parcel source) {

            this.ship_id = source.readInt();
            this.mst_ship_id = source.readInt();
            this.name = source.readString();
            this.stype = source.readInt();
            this.lv = source.readInt();
            this.exp = source.readInt();
            this.now_hp = source.readInt();
            this.max_hp = source.readInt();
            this.cond = source.readInt();
            this.sally_area = source.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

            dest.writeInt(ship_id);
            dest.writeInt(mst_ship_id);
            dest.writeString(name);
            dest.writeInt(stype);
            dest.writeInt(lv);
            dest.writeInt(exp);
            dest.writeInt(now_hp);
            dest.writeInt(max_hp);
            dest.writeInt(cond);
            dest.writeInt(sally_area);
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<ShipInfo> CREATOR = new Parcelable.Creator<ShipInfo>() {
            public ShipInfo createFromParcel(Parcel in) {
                return new ShipInfo(in);
            }

            public ShipInfo[] newArray(int size) {
                return new ShipInfo[size];
            }
        };
    }

    private static class SavedState extends View.BaseSavedState {
        final ShipInfo info;
        final boolean isAkashiActive;

        public SavedState(
                Parcelable superState,
                ShipInfo info,
                boolean isAkashiActive
        ) {
            super(superState);

            this.info = info;
            this.isAkashiActive = isAkashiActive;
        }

        public SavedState(Parcel source) {
            super(source);

            info = new ShipInfo(source);
            isAkashiActive = source.readInt() == 1; // to avoid using read/writeBoolean
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);

            info.writeToParcel(out, flags);
            out.writeInt(isAkashiActive ? 1 : 0); // to avoid using read/writeBoolean
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    // endregion
}
