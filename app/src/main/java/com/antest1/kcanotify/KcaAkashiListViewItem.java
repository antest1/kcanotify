package com.antest1.kcanotify;

import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.getItemTranslation;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.removeKai;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.joinStr;

public class KcaAkashiListViewItem {
    private int equipId;
    private JsonObject equipImprovementData;
    private int equipIconMipmap;
    private String equipName;
    private String equipSupport;
    private String equipMaterials;
    private String equipScrews;

    public int getEquipId() { return equipId; }

    public JsonObject getEquipImprovementData() { return equipImprovementData; }

    public int getEquipIconMipmap() {
        return equipIconMipmap;
    }

    public String getEquipName() {
        return equipName;
    }

    public String getEquipSupport() {
        return equipSupport;
    }

    public String getEquipMaterials() {
        return equipMaterials;
    }

    public String getEquipScrews() {
        return equipScrews;
    }

    public void setEquipDataById(int id) {
        JsonObject kcItemData = getKcItemStatusById(id, "type,name");

        String kcItemName = getItemTranslation(kcItemData.get("name").getAsString());
        int type = kcItemData.getAsJsonArray("type").get(3).getAsInt();
        int typeres = 0;
        try {
            typeres = getId(KcaUtils.format("item_%d", type), R.mipmap.class);
        } catch (Exception e) {
            typeres = R.mipmap.item_0;
        }

        equipId = id;
        equipIconMipmap = typeres;
        equipName = kcItemName;
    }

    public void setEquipImprovementData(JsonObject data) {
        equipImprovementData = data;
    }

    // 0: sun ~ 6: sat
    public void setEquipImprovementElement(int day, boolean checked) {
        JsonArray data = equipImprovementData.getAsJsonArray("improvement");
        boolean convert_exception = equipImprovementData.has("convert_exception");
        String[] material1 = new String[2];
        String[] material2 = new String[2];
        String[] material3 = new String[2];
        String[] screw1 = new String[2];
        String[] screw2 = new String[2];
        String[] screw3 = new String[2];

        int count = 0;
        List<String> screw = new ArrayList<String>();
        List<String> material = new ArrayList<String>();
        List<String> ship = new ArrayList<String>();

        for (int i = 0; i < data.size(); i++) {
            JsonArray req = data.get(i).getAsJsonObject().getAsJsonArray("req");
            List<String> shiplist = new ArrayList<String>();
            for (int j = 0; j < req.size(); j++) {
                JsonArray reqitem = req.get(j).getAsJsonArray();
                if (reqitem.size() == 2 && reqitem.get(0).getAsJsonArray().get(day).getAsBoolean()) {
                    JsonElement supportInfo = reqitem.get(1);
                    if (supportInfo.isJsonArray()) {
                        int[] filtered = removeKai(supportInfo.getAsJsonArray(), convert_exception);
                        for(int k = 0; k < filtered.length; k++) {
                            JsonObject kcShipData = getKcShipDataById(filtered[k], "name");
                            shiplist.add(getShipTranslation(kcShipData.get("name").getAsString(), false));
                        }
                    } else {
                        shiplist.add("-");
                    }
                }
            }
            if (shiplist.size() > 0) {
                ship.add(joinStr(shiplist,"/"));

                JsonArray resource = data.get(i).getAsJsonObject().getAsJsonArray("resource");
                JsonArray improv1 = resource.get(1).getAsJsonArray();
                JsonArray improv2 = resource.get(2).getAsJsonArray();
                JsonArray improv3 = resource.get(3).getAsJsonArray();
                if (checked) {
                    material1[count] = setMaterialScrewString(improv1.get(1).getAsString(), false);
                    material2[count] = setMaterialScrewString(improv2.get(1).getAsString(), false);
                    material3[count] = setMaterialScrewString(improv3.get(1).getAsString(), true);
                    screw1[count] = setMaterialScrewString(improv1.get(3).getAsString(), false);
                    screw2[count] = setMaterialScrewString(improv2.get(3).getAsString(), false);
                    screw3[count] = setMaterialScrewString(improv3.get(3).getAsString(), true);
                } else {
                    material1[count] = setMaterialScrewString(improv1.get(0).getAsString(), false);
                    material2[count] = setMaterialScrewString(improv2.get(0).getAsString(), false);
                    material3[count] = setMaterialScrewString(improv3.get(0).getAsString(), true);
                    screw1[count] = setMaterialScrewString(improv1.get(2).getAsString(), false);
                    screw2[count] = setMaterialScrewString(improv2.get(2).getAsString(), false);
                    screw3[count] = setMaterialScrewString(improv3.get(2).getAsString(), true);
                }
                count += 1;
            }
        }
        if (count == 2) {
            if (!material1[0].equals(material1[1])) material.add(material1[0].concat("or").concat(material1[1]));
            else material.add(material1[0]);
            if (!material2[0].equals(material2[1])) material.add(material2[0].concat("or").concat(material2[1]));
            else material.add(material2[0]);
            if (!material3[0].equals(material3[1])) material.add(material3[0].concat("or").concat(material3[1]));
            else material.add(material3[0]);
            if (!screw1[0].equals(screw1[1])) screw.add(screw1[0].concat("or").concat(screw1[1]));
            else screw.add(screw1[0]);
            if (!screw2[0].equals(screw2[1])) screw.add(screw2[0].concat("or").concat(screw2[1]));
            else screw.add(screw2[0]);
            if (!screw3[0].equals(screw3[1])) screw.add(screw3[0].concat("or").concat(screw3[1]));
            else screw.add(screw3[0]);
        } else {
            material.add(material1[0]);
            material.add(material2[0]);
            material.add(material3[0]);
            screw.add(screw1[0]);
            screw.add(screw2[0]);
            screw.add(screw3[0]);
        }
        equipSupport = joinStr(ship, " or ");
        equipMaterials = joinStr(material, "/");
        equipScrews = joinStr(screw, "/");
    }

    private String setMaterialScrewString(String s, boolean ignore) {
        if (Integer.parseInt(s) == -1) return "?";
        else if (ignore && Integer.parseInt(s) == 0) return "-";
        else return s;
    }

}
