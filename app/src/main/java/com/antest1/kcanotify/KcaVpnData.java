package com.antest1.kcanotify;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaUtils.byteArrayToHex;
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
    static byte[] datapart;
    static boolean gzipflag = false;
    static boolean chunkflag = false;
    static boolean isreadyflag = false;

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
    private static void getDataFromNative(byte[] data, int size, int type, byte[] source, byte[] target) throws IOException {
        //byte[] slicedData = Arrays.copyOfRange(data, 0, size);

        String s = new String(data);
        String saddrstr = new String(source);
        String taddrstr = new String(target);

        //Log.e("KCA", saddrstr + " -> " + taddrstr);
        //Log.e("KCA", String.valueOf(type) + " / "  + String.valueOf(data.length));
        String[] head_body = s.split("\r\n\r\n", 2);
        String head = head_body[0];
        /*
        if (type == REQUEST && head.startsWith("POST") && head_body.length > 1) body = head_body[1];
        if (type == RESPONSE && head.startsWith("HTTP")) body = head_body[1];
        */

        //head = head.substring(0, Math.min(60, head.length()));
        if (type == REQUEST) {
            if (head.startsWith("GET") || head.startsWith("POST")) {
                state = REQUEST;
                String[] header = head.split("\r\n");
                kcdataflag = checkKcApi(header[0]);
                requestUri = header[0].split(" ")[1];
                if(head.startsWith("POST")) {
                    if (head.length() == data.length) { // No data
                        requestData = new byte[]{};
                    } else {
                        int head_size = head.length() + 4;
                        requestData = Arrays.copyOfRange(data, head_size, data.length);
                    }
                } else {
                    requestData = new byte[]{};
                }
                responseData = new byte[]{};

                isreadyflag = false;
                gzipflag = false;
                chunkflag = false;
            }
            else {
                requestData = Bytes.concat(requestData, data);
            }

        } else if (type == RESPONSE) {
            state = RESPONSE;
            if (!kcdataflag) {
                // No Process
            } else {
                if (head.startsWith("HTTP")) {
                    String[] header = head.split("\r\n");
                    for (String line : header) {
                        if (line.startsWith("Content-Encoding: ")) {
                            if (line.contains("gzip")) gzipflag = true;
                        } else if (line.startsWith("Transfer-Encoding")) {
                            if (line.contains("chunked")) chunkflag = true;
                        }
                    }
                    datapart = Arrays.copyOfRange(data, head.length() + 4, data.length);
                } else {
                    datapart = data;
                }

                if (gzipflag) {
                    if (chunkflag) {
                        responseData = Bytes.concat(responseData, datapart);
                        if (isChunkEnd(datapart)) {
                            isreadyflag = true;
                            responseData = unchunkAllData(gzipflag);
                        }
                    } else {
                        isreadyflag = true;
                        responseData = gzipdecompress(datapart);
                    }
                } else {
                    isreadyflag = true;
                    responseData = datapart;
                }
                if(isreadyflag) {
                    //Log.e("KCA", String.valueOf(responseData.length));
                    KcaHandler k = new KcaHandler(handler, requestUri, requestData, responseData, false);
                    executorService.execute(k);
                    isreadyflag = false;
                }
            }


        }
    }

    private static boolean checkKcApi(String uri) {
        boolean isKcaVer = uri.contains("/kca/version");
        boolean isKcsApi = uri.contains("/kcsapi/api_");
        //Log.e("KCA", uri + " " + String.valueOf(isKcaVer || isKcsApi));
        return (isKcaVer || isKcsApi);
    }

    private static byte[] unchunkAllData(boolean gzipped) throws IOException {
        byte[] rawdata = unchunkdata(responseData);
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
