package com.antest1.kcanotify.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaConstants;
import com.antest1.kcanotify.KcaDBHelper;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.LocaleUtils;
import com.antest1.kcanotify.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.antest1.kcanotify.KcaApiData.findRemodelLv;
import static com.antest1.kcanotify.KcaApiData.getKcItemStatusById;
import static com.antest1.kcanotify.KcaApiData.getKcShipDataById;
import static com.antest1.kcanotify.KcaApiData.getShipTranslation;
import static com.antest1.kcanotify.KcaApiData.getSlotItemTranslation;
import static com.antest1.kcanotify.KcaApiData.removeKai;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.joinStr;

/**
 * Business logic for {@link com.antest1.kcanotify.AkashiDetailActivity}.
 *
 * <p>Contains all of the improvement-data computation and text formatting. Only the view binding
 * remains in the activity.
 */
public class AkashiDetailViewModel extends AndroidViewModel {
    private static JsonArray requiredItemTranslation;

    private final KcaDBHelper dbHelper;
    private final Context localizedContext;
    private JsonObject itemImprovementData;
    private JsonObject useritemCount = new JsonObject();

    public AkashiDetailViewModel(@NonNull Application application) {
        super(application);
        dbHelper = new KcaDBHelper(application, null, KcaConstants.KCANOTIFY_DB_VERSION);
        localizedContext = LocaleUtils.getLocalizedContext(application);
    }

    public static void setRequiredItemTranslation(JsonArray data) {
        requiredItemTranslation = data;
    }

    public void loadUserItems() {
        useritemCount = new JsonObject();
        JsonArray useitemData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_USEITEMS);
        if (useitemData != null) {
            for (int n = 0; n < useitemData.size(); n++) {
                JsonObject item = useitemData.get(n).getAsJsonObject();
                String key = item.get("api_id").getAsString();
                String value = item.get("api_count").getAsString();
                useritemCount.addProperty(key, value);
            }
        }
    }

    /**
     * Returns the improvement information as a JSON object, or {@code null} when the intent data
     * was not valid JSON.
     */
    public JsonObject parseImprovementData(String itemImprovmetInfo) {
        if (itemImprovmetInfo == null || itemImprovmetInfo.isEmpty()) return null;
        itemImprovementData = JsonParser.parseString(itemImprovmetInfo).getAsJsonObject();
        return itemImprovementData;
    }

    public JsonObject getItemImprovementData() {
        return itemImprovementData;
    }

    /**
     * Returns one material count (6 = development material, 7 = improvement screw).
     */
    public String getMaterialCount(int index) {
        JsonArray materialData = dbHelper.getJsonArrayValue(KcaConstants.DB_KEY_MATERIALS);
        if (materialData == null) return null;
        JsonElement value = materialData.get(index);
        return value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("api_value").getAsString();
    }

    public String getItemName(int itemId) {
        JsonObject kcItemData = getKcItemStatusById(itemId, "type,name");
        return getSlotItemTranslation(kcItemData.get("name").getAsString());
    }

    /**
     * Formats the "improved count per star" summary shown next to the item name.
     */
    public String getImprovedCountText(int itemId) {
        List<Integer> status = getImprovedCount(String.valueOf(itemId));
        List<String> statusText = new ArrayList<>();
        for (int i = 0; i < status.size(); i++) {
            if (status.get(i) > 0) {
                int val = status.get(i);
                statusText.add(KcaUtils.format("★%s: %d", i == 10 ? "max" : String.valueOf(i), val));
            }
        }
        return statusText.isEmpty() ? "-" : KcaUtils.joinStr(statusText, ", ");
    }

    /**
     * Produces the material / screw / equipment strings for one improvement slot.
     */
    public String[] getMseString(JsonArray array, JsonObject itemCount) {
        String[] valueList = new String[7];
        for (int i = 0; i < array.size(); i++) {
            valueList[i] = array.get(i).getAsString();
            if (i >= 4 && valueList[i].equals("0")) {
                valueList[i] = "-";
            } else if (valueList[i].equals("-1")) {
                valueList[i] = "?";
            }
        }

        if (!valueList[4].equals("-")) {
            int count = 0;
            if (itemCount.has(valueList[4])) count = itemCount.get(valueList[4]).getAsInt();
            String equipName = getSlotItemTranslation(getKcItemStatusById(Integer.parseInt(valueList[4]), "name")
                    .get("name").getAsString());
            valueList[4] = equipName;
            valueList[6] = String.valueOf(count);
        }

        String materialData = valueList[0].concat(" / ").concat(valueList[1]);
        String screwData = valueList[2].concat(" / ").concat(valueList[3]);
        String equipData;
        if (valueList[4].equals("-")) {
            equipData = valueList[4];
        } else {
            equipData = KcaUtils.format("%sx%s (%s)", valueList[4], valueList[5], valueList[6]);
        }
        return new String[]{materialData, screwData, equipData};
    }

    public JsonObject getItemCount(Set<String> equipId) {
        JsonArray userEquipmentData = dbHelper.getItemData();
        JsonObject countResult = new JsonObject();
        for (String e : equipId) {
            countResult.addProperty(e, 0);
        }

        for (JsonElement data : userEquipmentData) {
            JsonObject equip = data.getAsJsonObject();
            JsonObject value = JsonParser.parseString(equip.get("value").getAsString()).getAsJsonObject();
            String slotitemId = value.get("api_slotitem_id").getAsString();
            int level = value.get("api_level").getAsInt();
            if (level == 0 && equipId.contains(slotitemId)) {
                countResult.addProperty(slotitemId, countResult.get(slotitemId).getAsInt() + 1);
            }
        }
        return countResult;
    }

    public JsonObject buildItemCountForResources(JsonArray resources) {
        Set<String> totalItems = new HashSet<>();
        for (int j = 1; j <= 3; j++) {
            int item = resources.get(j).getAsJsonArray().get(4).getAsInt();
            if (item != 0) totalItems.add(String.valueOf(item));
        }
        return getItemCount(totalItems);
    }

    /**
     * Builds the required-item lines for a slot.
     */
    public String getRequiredItemsText(JsonArray requireItems, String prefix, String currentText) {
        List<String> requireItemsStr = new ArrayList<>();
        requireItemsStr.add(currentText);
        for (int k = 0; k < requireItems.size(); k++) {
            JsonArray rItem = requireItems.get(k).getAsJsonArray();
            int requireItemCount = rItem.get(1).getAsInt();
            JsonObject itemInfo = getRequiredItemData(rItem.get(0).getAsInt());
            String requireItemName = getRequiredItemName(itemInfo.getAsJsonObject("name"));
            String useitemId = itemInfo.get("useitem_id").getAsString();
            int useritemCountView = 0;
            if (useritemCount.has(useitemId)) {
                useritemCountView = useritemCount.get(useitemId).getAsInt();
            }
            requireItemsStr.add(KcaUtils.format("%s%sx%d (%d)",
                    prefix, requireItemName, requireItemCount, useritemCountView));
        }
        return joinStr(requireItemsStr, "\n");
    }

    /**
     * Builds the required-item lines for the second improvement slot.
     */
    public String getRequiredItems2Text(JsonArray requireItem2, String currentText) {
        List<String> requireItemsStr = new ArrayList<>();
        requireItemsStr.add(currentText);

        if (!requireItem2.isEmpty() && requireItem2.size() % 2 == 0) {
            int requireItem2Cnt = requireItem2.size() / 2;
            for (int k = 0; k < requireItem2Cnt; k++) {
                int item2From = requireItem2.get(k * 2).getAsInt();
                JsonArray item2ReqItems = requireItem2.get(k * 2 + 1).getAsJsonArray();
                for (int n = 0; n < item2ReqItems.size(); n++) {
                    JsonArray rItem = item2ReqItems.get(n).getAsJsonArray();
                    int requireItemCount = rItem.get(1).getAsInt();
                    JsonObject itemInfo = getRequiredItemData(rItem.get(0).getAsInt());
                    String requireItemName = getRequiredItemName(itemInfo.getAsJsonObject("name"));
                    String useitemId = itemInfo.get("useitem_id").getAsString();

                    int useritemCountView;
                    if (useritemCount.has(useitemId)) {
                        useritemCountView = useritemCount.get(useitemId).getAsInt();
                    } else {
                        useritemCountView = getUserItemCount(useitemId);
                    }
                    requireItemsStr.add(KcaUtils.format("[★%d] %sx%d (%d)",
                            item2From, requireItemName, requireItemCount, useritemCountView));
                }
            }
        }

        return joinStr(requireItemsStr, "\n");
    }

    public String getConcatString(JsonArray array) {
        List<String> list = new ArrayList<>();
        for (JsonElement e : array) {
            list.add(e.getAsString());
        }
        return joinStr(list, " / ");
    }

    public String getSupportString(JsonArray array, boolean exception) {
        String supportString = "";
        for (int i = 0; i < array.size(); i++) {
            List<String> daylist = new ArrayList<>();
            JsonArray item = array.get(i).getAsJsonArray();
            if (item.get(1).isJsonArray()) {
                JsonArray day = item.get(0).getAsJsonArray();
                for (int j = 0; j < day.size(); j++) {
                    if (day.get(j).getAsBoolean()) {
                        daylist.add(localizedContext.getString(getId("akashi_term_day_".concat(String.valueOf(j)), R.string.class)));
                    }
                }

                String daytext;
                if (daylist.isEmpty()) {
                    daytext = localizedContext.getString(R.string.akashi_term_day_none);
                } else {
                    daytext = joinStr(daylist, ",");
                }

                int[] shiplist = removeKai(item.get(1).getAsJsonArray(), exception);
                for (int j = 0; j < shiplist.length; j++) {
                    JsonObject kcShipData = getKcShipDataById(shiplist[j], "name");
                    String shipname = getShipTranslation(kcShipData.get("name").getAsString(), shiplist[j], false);
                    supportString = supportString.concat(KcaUtils.format("%s(%s)", shipname, daytext)).concat("\n");
                }
            } else {
                supportString = "-";
            }
        }
        return supportString.trim();
    }

    /**
     * Builds the "default equipped on" text.
     */
    public String getDefaultEquippedText(JsonArray itemDefaultEquippedOn) {
        if (itemDefaultEquippedOn == null) return "-";

        List<String> shipList = new ArrayList<>();
        List<String> shipNameList = new ArrayList<>();
        JsonObject shipName = new JsonObject();
        for (JsonElement element : itemDefaultEquippedOn) {
            int shipId = element.getAsInt();
            shipList.add(String.valueOf(shipId));
            JsonObject kcShipData = getKcShipDataById(shipId, "name");
            String translatedName = getShipTranslation(kcShipData.get("name").getAsString(), shipId, false);
            shipName.addProperty(String.valueOf(shipId), translatedName);
        }
        if (shipList.size() > 0) {
            JsonObject shipRemovelLv = findRemodelLv(joinStr(shipList, ","));
            for (int i = 0; i < shipList.size(); i++) {
                String id = shipList.get(i);
                shipNameList.add(KcaUtils.format("%s(%d)", shipName.get(id).getAsString(), shipRemovelLv.get(id).getAsInt()));
            }
            return joinStr(shipNameList, " / ");
        }
        return "-";
    }

    public String getUpgradeText(JsonObject data) {
        JsonElement upgrade = data.get("upgrade");
        if (!upgrade.isJsonArray()) return null;
        int upgradeItemId = upgrade.getAsJsonArray().get(0).getAsInt();
        int upgradeItemStar = upgrade.getAsJsonArray().get(1).getAsInt();
        String upgradeItemName = getSlotItemTranslation(getKcItemStatusById(upgradeItemId, "name").get("name").getAsString());
        String upgradeStarString = "";
        if (upgradeItemStar > 0) {
            upgradeStarString = upgradeStarString.concat(" ").concat("★").concat(String.valueOf(upgradeItemStar));
        }
        return "→ ".concat(upgradeItemName.concat(upgradeStarString));
    }

    private JsonObject getRequiredItemData(int index) {
        if (index >= requiredItemTranslation.size()) {
            return requiredItemTranslation.get(0).getAsJsonObject();
        } else {
            return requiredItemTranslation.get(index).getAsJsonObject();
        }
    }

    private String getRequiredItemName(JsonObject data) {
        String locale = LocaleUtils.getResourceLocaleCode();
        if (data.has(locale)) {
            return data.get(locale).getAsString();
        } else {
            return data.get("jp").getAsString();
        }
    }

    private List<Integer> getImprovedCount(String equipId) {
        JsonArray userEquipmentData = dbHelper.getItemData();
        List<Integer> count = new ArrayList<>(11);
        for (int i = 0; i <= 10; i++) count.add(0);

        for (JsonElement data : userEquipmentData) {
            JsonObject equip = data.getAsJsonObject();
            JsonObject value = JsonParser.parseString(equip.get("value").getAsString()).getAsJsonObject();
            String slotitemId = value.get("api_slotitem_id").getAsString();
            int level = value.get("api_level").getAsInt();
            if (equipId.equals(slotitemId)) {
                count.set(level, count.get(level) + 1);
            }
        }
        return count;
    }

    private int getUserItemCount(String useritemId) {
        String key = switch (useritemId) {
            case "66" -> "145";
            case "67" -> "146";
            default -> null;
        };

        if (key != null) {
            Set<String> totalItems = new HashSet<>();
            totalItems.add(key);
            JsonObject result = getItemCount(totalItems);
            if (result.has(key)) return result.get(key).getAsInt();
        }
        return 0;
    }
}
