package kcanotify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.antest1.kcanotify.KcaApiData;
import com.antest1.kcanotify.KcaBattle;
import com.antest1.kcanotify.KcaDeckInfo;
import com.google.common.io.ByteStreams;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TestDeck {
    @Before
    public void setUp() {

    }

    @Test
    public void test() throws ParseException, IOException {
        JSONParser p = new JSONParser();

        InputStream in1 = this.getClass().getClassLoader().getResourceAsStream("api_start2");
        byte[] bytes1 = ByteStreams.toByteArray(in1);
        String data = new String(bytes1);
        JSONObject api_data1 = (JSONObject) ((JSONObject)(p.parse(data))).get("api_data");

        KcaApiData.getKcGameData(api_data1);
        in1.close();

        InputStream in2 = this.getClass().getClassLoader().getResourceAsStream("api_require_info");
        byte[] bytes2 = ByteStreams.toByteArray(in2);
        String data2 = new String(bytes2);
        JSONObject api_data2 = (JSONObject) ((JSONObject)(p.parse(data2))).get("api_data");

        KcaApiData.getSlotItemData(api_data2);
        in2.close();

        InputStream in3 = this.getClass().getClassLoader().getResourceAsStream("api_port");
        byte[] bytes3 = ByteStreams.toByteArray(in3);
        String data3 = new String(bytes3);
        in3.close();

        JSONObject api_data3 = (JSONObject) ((JSONObject)(p.parse(data3))).get("api_data");
        KcaApiData.getPortData(api_data3);

        JSONArray api_deckport = (JSONArray) api_data3.get("api_deck_port");
        Log.e("KCA", String.valueOf(KcaDeckInfo.getSeekValue(api_deckport, 0,  1)));

        int[] testAirPower = KcaDeckInfo.getAirPowerRange(api_deckport, 0);
        Log.e("KCA", String.format("AAC: %d - %d", testAirPower[0], testAirPower[1]));
    }

}
