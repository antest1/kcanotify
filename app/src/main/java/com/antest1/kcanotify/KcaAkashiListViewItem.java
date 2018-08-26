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
    private String equipName = "";
    private String equipSupport = "";
    private String equipMaterials = "";
    private String equipScrews = "";

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

        List<String> material_data = new ArrayList<>();
        List<String> screw_data = new ArrayList<>();
        if (ship.size() > 1) {
            for (int i = 0; i < ship.size(); i++) {
                material_data.clear();
                screw_data.clear();
                String text = KcaUtils.format("[%d] %s\n", i+1, ship.get(i));
                material_data.add(material1[i]);
                material_data.add(material2[i]);
                material_data.add(material3[i]);
                screw_data.add(screw1[i]);
                screw_data.add(screw2[i]);
                screw_data.add(screw3[i]);
                equipSupport = equipSupport.concat(text);
                equipMaterials = equipMaterials.concat(joinStr(material_data, "/")).concat("\n");
                equipScrews = equipScrews.concat(joinStr(screw_data, "/")).concat("\n");
            }
            equipMaterials = equipMaterials.trim();
            equipScrews = equipScrews.trim();
            equipSupport = equipSupport.trim();
        } else {
            material_data.add(material1[0]);
            material_data.add(material2[0]);
            material_data.add(material3[0]);
            screw_data.add(screw1[0]);
            screw_data.add(screw2[0]);
            screw_data.add(screw3[0]);
            equipSupport = ship.get(0);
            equipMaterials = equipMaterials.concat(joinStr(material_data, "/"));
            equipScrews = equipScrews.concat(joinStr(screw_data, "/"));
        }
    }

    private String setMaterialScrewString(String s, boolean ignore) {
        if (Integer.parseInt(s) == -1) return "?";
        else if (ignore && Integer.parseInt(s) == 0) return "x";
        else return s;
    }

}
