package com.antest1.kcanotify;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.KCA_API_VPN_DATA_ERROR;
import static com.antest1.kcanotify.KcaUtils.byteArrayToHex;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.gzipdecompress;
import static com.antest1.kcanotify.KcaUtils.unchunkdata;

public class KcaVpnData {
    public static Handler handler;

    private static final int NONE = 0;
    private static final int REQUEST = 1;
    private static final int RESPONSE = 2;

    private static String[] kcaServerList = {
            "203.104.209.71",   // Yokosuka
            "203.104.209.87",   // Kure
            "125.6.184.16",     // Sasebo
            "125.6.187.205",    // Maizuru
            "125.6.187.229",    // Ominato
            "125.6.187.253",    // Truk
            "125.6.188.25",     // Ringga
            "203.104.248.135",  // Rabaul
            "125.6.189.7",      // Shortland
            "125.6.189.39",     // Buin
            "125.6.189.71",     // Tawi-Tawi
            "125.6.189.103",    // Palau
            "125.6.189.135",    // Brunei
            "125.6.189.167",    // Hittokappuman
            "125.6.189.215",    // Paramushir
            "125.6.189.247",    // Sukumoman
            "203.104.209.23",   // Kanoya
            "203.104.209.39",   // Iwagawa
            "203.104.209.55",   // Saikiman
            "203.104.209.102",  // Hashirajima

            "203.104.209.7"     // Kancolle Android Server
    };
    private static List<String> kcaServers = new ArrayList<String>(Arrays.asList(kcaServerList));
    public static ExecutorService executorService = Executors.newCachedThreadPool();

    public static int state = NONE;
    public static byte[] requestData = {};
    public static byte[] responseData = {};

    static String requestUri = "";
    static boolean kcdataflag = false;
    static boolean gzipflag = false;
    static boolean chunkflag = false;
    static boolean isreadyflag = false;

    static String responseHeaderPart = "";
    static int responseBodyLength = -1;

    public static void setHandler(Handler h) {
        handler = h;
    }

    // Called from native code
    private static int containsKcaServer(byte[] source, byte[] target) {
        String saddrstr = new String(source);
        String taddrstr = new String(target);
        if (kcaServers.contains(saddrstr) || kcaServers.contains(taddrstr)) {
            return 1;
        } else {
            return 0;
        }
    }

    // Called from native code
    private static void getDataFromNative(byte[] data, int size, int type, byte[] source, byte[] target) {
        try {
            //byte[] slicedData = Arrays.copyOfRange(data, 0, size);

            String s = new String(data);
            String saddrstr = new String(source);
            String taddrstr = new String(target);

            String[] head_body = s.split("\r\n\r\n", 2);
            String head = head_body[0];


            //head = head.substring(0, Math.min(60, head.length()));
            if (type == REQUEST) {
                if (head.startsWith("GET") || head.startsWith("POST")) {
                    state = REQUEST;
                    requestData = new byte[]{};
                    responseData = new byte[]{};
                    responseHeaderPart = "";
                    String[] header = head.split("\r\n");
                    kcdataflag = checkKcApi(header[0]);
                    requestUri = header[0].split(" ")[1];
                    isreadyflag = false;
                    gzipflag = false;
                    chunkflag = false;
                }
                requestData = Bytes.concat(requestData, data);
            } else if (type == RESPONSE) {
                state = RESPONSE;
                if (responseHeaderPart.length() == 0) {
                    responseData = new byte[]{};
                    String responseDataStr = new String(data);
                    responseHeaderPart = responseDataStr.split("\r\n\r\n")[0];
                    String[] headers = responseHeaderPart.split("\r\n");
                    for (String line : headers) {
                        if (line.startsWith("Content-Encoding: ")) {
                            if (line.contains("gzip")) gzipflag = true;
                            else gzipflag = false;
                        } else if (line.startsWith("Transfer-Encoding")) {
                            if (line.contains("chunked")) {
                                chunkflag = true;
                                responseBodyLength = -1;
                            } else chunkflag = false;
                        } else if (line.startsWith("Content-Length")) {
                            chunkflag = false;
                            responseBodyLength = Integer.parseInt(line.replaceAll("Content-Length: ", "").trim());
                        }
                    }
                }
                responseData = Bytes.concat(responseData, data);
                if (chunkflag && isChunkEnd(responseData)) {
                    isreadyflag = true;
                } else if (responseData.length == responseHeaderPart.length() + 4 + responseBodyLength) {
                    isreadyflag = true;
                }

                if (isreadyflag) {
                    String requestStr = new String(requestData);
                    String[] requestHeadBody = requestStr.split("\r\n\r\n", 2);
                    byte[] requestBody = new byte[]{};
                    if (requestHeadBody[1].length() > 0) {
                        requestBody = requestHeadBody[1].getBytes();
                    }
                    Log.e("KCA", String.valueOf(responseData.length));
                    Log.e("KCA", String.valueOf(responseHeaderPart.length()));
                    Log.e("KCA", "====================================");
                    byte[] responseBody = Arrays.copyOfRange(responseData, responseHeaderPart.length() + 4, responseData.length);
                    if (chunkflag) {
                        Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, 0, 15)));
                        Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, responseBody.length - 15, responseBody.length)));
                        responseBody = unchunkAllData(responseBody, gzipflag);
                    } else if (gzipflag) {
                        responseBody = gzipdecompress(responseBody);
                    }

                    //Log.e("KCA", String.valueOf(responseData.length));
                    if(checkKcApi(requestUri)) {
                        KcaHandler k = new KcaHandler(handler, requestUri, requestBody, responseBody, false);
                        executorService.execute(k);
                    }
                    requestUri = "";
                    responseData = new byte[]{};
                    requestData = new byte[]{};
                    isreadyflag = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String error_uri = KCA_API_VPN_DATA_ERROR;
            String empty_request = "";
            JsonObject error_data = new JsonObject();
            error_data.addProperty("error", getStringFromException(e));
            error_data.addProperty("uri", requestUri);
            error_data.addProperty("request", byteArrayToHex(requestData));
            error_data.addProperty("response", byteArrayToHex(responseData));
            KcaHandler k = new KcaHandler(handler, error_uri, empty_request.getBytes(), error_data.toString().getBytes(), false);
            executorService.execute(k);
            Log.e("KCA", getStringFromException(e));
        }
    }

    private static boolean checkKcApi(String uri) {
        boolean isKcaVer = uri.contains("/kca/version");
        boolean isKcsApi = uri.contains("/kcsapi/api_");
        //Log.e("KCA", uri + " " + String.valueOf(isKcaVer || isKcsApi));
        return (isKcaVer || isKcsApi);
    }

    private static byte[] unchunkAllData(byte[] data, boolean gzipped) throws IOException {
        byte[] rawdata = unchunkdata(data);
        if (gzipped) rawdata = gzipdecompress(rawdata);
        //Log.e("KCA", new String(rawdata));
        return rawdata;
    }

    private static boolean isChunkEnd(byte[] data) {
        int dataLength = data.length;
        if (dataLength < 5) return false;
        byte[] chunkEndSignal = {48, 13, 10, 13, 10};
        byte[] endChunk = Arrays.copyOfRange(data, dataLength - 5, dataLength);
        return Arrays.equals(endChunk, chunkEndSignal);
    }
}
