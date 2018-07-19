package com.antest1.kcanotify;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.zxing.integration.android.IntentIntegrator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.antest1.kcanotify.KcaApiData.kcQuestInfoData;

public final class KcaQuestCode {
    public final static String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String convert_to_code(String key) {
        return convert_to_code(key, null, true);
    }

    public static String convert_to_code(String key_, JsonArray count, boolean active) {
        String text = "";
        int key = Integer.parseInt(key_);
        int key_high = key / chars.length();
        int key_low = key % chars.length();

        StringBuilder sb = new StringBuilder();
        sb.append(chars.charAt(key_high));
        sb.append(chars.charAt(key_low));

        if (count != null) {
            for (int i = 0; i < count.size(); i++) {
                if (count.get(i).getAsInt() >= chars.length()) {
                    if (key == 214) {
                        sb.append("=");
                    } else if (key == 221) {
                        sb.append("=");
                        sb.append(chars.charAt(count.get(i).getAsInt() % chars.length()));
                    }
                } else {
                    sb.append(chars.charAt(count.get(i).getAsInt()));
                }
            }
        }

        String value = sb.toString();
        String value_active = active ? "1" : "0";
        value = value_active.concat(value);
        String size = String.valueOf(value.length());
        return size.concat(value);
    }

    public static JsonArray decode_code(String code) {
        JsonArray code_list = new JsonArray();
        int key = 0;
        while (key < code.length()) {
            StringBuilder sb = new StringBuilder();
            int size = Integer.parseInt(code.substring(key, key+1));
            boolean active = Integer.parseInt(code.substring(key+1, key+2)) > 0;
            key += 2;
            for (int i = 0; i < size - 1; i++) {
                sb.append(code.charAt(key+i));
            }
            char[] sb_result = sb.toString().trim().toCharArray();
            String quest_code = String.valueOf(chars.indexOf(sb_result[0]) * chars.length() + chars.indexOf(sb_result[1]));
            JsonArray quest_count = new JsonArray();
            for (int i = 2; i < sb_result.length; i++) {
                if (sb_result[i] == '=') {
                    if (quest_code.equals("214")) {
                        quest_count.add(chars.length());
                    } else if (quest_code.equals("221")) {
                        int count_low = chars.indexOf(sb_result[i+1]);
                        quest_count.add(chars.length() + count_low);
                        break;
                    }
                } else {
                    quest_count.add(chars.indexOf(sb_result[i]));
                }
            }
            JsonObject item = new JsonObject();
            item.addProperty("code", quest_code);
            item.add("cond", quest_count);
            item.addProperty("active", active);
            code_list.add(item);
            key += (size - 1);
        }
        return code_list;
    }

    public static boolean validate_code(String code) {
        boolean valid = true;
        int key = 0;
        try {
            while (key < code.length()) {
                int size = Integer.parseInt(code.substring(key, key+1));
                key += (1 + size);
            }
            return key == code.length();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFormattedCodeInfo(String code, boolean mode, KcaQuestTracker qt) {
        StringBuilder data = new StringBuilder();
        if (KcaQuestCode.validate_code(code)) {
            JsonArray decoded_result = KcaQuestCode.decode_code(code);
            for (int i = 0; i < decoded_result.size(); i++) {
                JsonObject item = decoded_result.get(i).getAsJsonObject();
                String key = item.get("code").getAsString();
                boolean active = item.get("active").getAsBoolean();
                JsonObject quest_data = kcQuestInfoData.getAsJsonObject(key);
                String quest_code = quest_data.get("code").getAsString();
                String quest_name = quest_data.get("name").getAsString();
                JsonArray quest_cond = item.getAsJsonArray("cond");
                if (quest_cond.size() > 0) {
                    List<String> quest_cond_list = new ArrayList<>();
                    JsonArray quest_trackinfo = qt.getQuestTrackInfo(key);

                    for (int j = 0; j < quest_cond.size(); j++) {
                        String v = quest_cond.get(j).getAsString();
                        String u = "";
                        if (!mode) {
                            if (quest_trackinfo.size() > 0) {
                                u = quest_trackinfo.get(j).getAsString() + "+";
                            } else {
                                u = "0+";
                            }
                        }
                        quest_cond_list.add(u + v);
                    }
                    String active_str = active ? "" : "*";
                    String quest_cond_str = KcaUtils.joinStr(quest_cond_list, ", ");
                    data.append(KcaUtils.format("[%s%s] %s (%s)\n", quest_code, active_str, quest_name, quest_cond_str));
                } else {
                    data.append(KcaUtils.format("[%s] %s\n", quest_code, quest_name));
                }
            }
        }
        return data.toString().trim();
    }
}
