package com.antest1.kcanotify;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntegerRes;
import android.util.Log;

import com.google.common.collect.EvictingQueue;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
    public static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static int state = NONE;
    public static byte[] requestData = {};
    public static byte[] responseData = {};

    static String requestUri = "";
    static boolean gzipflag = false;
    static boolean chunkflag = false;
    static boolean isreadyflag = false;

    static int responseBodyLength = -1;

    private static Map<Integer, String> portToUri = new HashMap<Integer, String>();
    private static Map<Integer, Byte[]> portToRequestData = new HashMap<Integer, Byte[]>();
    private static Map<Integer, Byte[]> portToResponseData = new HashMap<Integer, Byte[]>();
    private static Map<Integer, Integer> portToResponseHeaderLength = new HashMap<Integer, Integer>();
    private static Map<Integer, Integer> portToLength = new HashMap<Integer, Integer>();
    private static Map<Integer, Boolean> portToGzipped = new HashMap<Integer, Boolean>();
    private static Map<Integer, String> portToResponseHeaderPart = new HashMap<Integer, String>();

    private static Queue<Integer> ignoreResponseList = EvictingQueue.create(8);

    public static void setHandler(Handler h) {
        handler = h;
    }

    // Called from native code
    private static int containsKcaServer(int type, byte[] source, byte[] target) {
        String saddrstr = new String(source);
        String taddrstr = new String(target);
        if (type == REQUEST && !kcaServers.contains(taddrstr)) {
            return 0;
        } else if (type == RESPONSE && !kcaServers.contains(saddrstr)) {
            return 0;
        }
        //Log.e("KCAV", String.format("containsKcaServer[%d] %s:%d => %s:%d", type, saddrstr, sport, taddrstr, tport));
        return 1;
    }

    // Called from native code
    private static void getDataFromNative(byte[] data, int size, int type, byte[] source, byte[] target, int sport, int tport) {
        try {
            String s = new String(data);
            String saddrstr = new String(source);
            String taddrstr = new String(target);
            //Log.e("KCAV", String.format("getDataFromNative[%d] %s:%d => %s:%d", type, saddrstr, sport, taddrstr, tport));

            String[] head_body = s.split("\r\n\r\n", 2);
            String head = head_body[0];

            //head = head.substring(0, Math.min(60, head.length()));
            if (type == REQUEST) {
                if (head.startsWith("GET") || head.startsWith("POST")) {
                    state = REQUEST;
                    isreadyflag = false;
                    String[] header = head.split("\r\n");
                    requestUri = header[0].split(" ")[1];
                    portToUri.put(sport, requestUri);
                    Log.e("KCAV", requestUri);
                    if (!checkKcRes(requestUri)) {
                        portToRequestData.put(sport, new Byte[]{});
                        portToResponseData.put(sport, new Byte[]{});
                        portToResponseHeaderPart.put(sport, "");
                        portToGzipped.put(sport, false);
                        portToLength.put(sport, 0);
                        if (ignoreResponseList.contains(sport)) {
                            ignoreResponseList.remove(sport);
                        }
                    } else {
                        if (!ignoreResponseList.contains(sport)) {
                            ignoreResponseList.add(sport);
                        }
                        Log.e("KCAV", ignoreResponseList.toString());
                    }
                }
                if (!ignoreResponseList.contains(sport)) {
                    Byte[] requestData = portToRequestData.get(sport);
                    portToRequestData.put(sport, ArrayUtils.toObject(Bytes.concat(ArrayUtils.toPrimitive(requestData), data)));
                }
            } else if (type == RESPONSE) {
                state = RESPONSE;
                if (ignoreResponseList.contains(tport)) {
                    Log.e("KCAV", portToUri.get(tport) + " ignored");
                    return;
                }
                if (portToResponseHeaderPart.get(tport).length() == 0) {
                    portToResponseData.put(tport, new Byte[]{});
                    String prevResponseHeaderPart = portToResponseHeaderPart.get(tport);
                    String responseDataStr = new String(data);
                    if (!responseDataStr.contains("\r\n\r\n")) {
                        portToResponseHeaderPart.put(tport, prevResponseHeaderPart.concat(responseDataStr));
                    } else {
                        portToResponseHeaderPart.put(tport, prevResponseHeaderPart.concat(responseDataStr.split("\r\n\r\n", 2)[0]));
                        portToResponseHeaderLength.put(tport, portToResponseHeaderPart.get(tport).length());
                        String[] headers = portToResponseHeaderPart.get(tport).split("\r\n");
                        for (String line : headers) {
                            if (line.startsWith("Content-Encoding: ")) {
                                portToGzipped.put(tport, line.contains("gzip"));
                                Log.e("KCA", String.valueOf(tport) + " gzip " + portToGzipped.get(tport));
                            } else if (line.startsWith("Transfer-Encoding")) {
                                if (line.contains("chunked")) {
                                    portToLength.put(tport, -1);
                                    responseBodyLength = -1;
                                }
                            } else if (line.startsWith("Content-Length")) {
                                portToLength.put(tport, Integer.parseInt(line.replaceAll("Content-Length: ", "").trim()));
                                responseBodyLength = Integer.parseInt(line.replaceAll("Content-Length: ", "").trim());
                            }
                        }
                    }
                }

                boolean chunkflag = (portToLength.get(tport) == -1);
                boolean gzipflag = portToGzipped.get(tport);
                Byte[] responsePrevData = portToResponseData.get(tport);
                portToResponseData.put(tport, ArrayUtils.toObject(Bytes.concat(ArrayUtils.toPrimitive(responsePrevData), data)));
                if (portToLength.get(tport) == -1 && isChunkEnd(ArrayUtils.toPrimitive(portToResponseData.get(tport)))) {
                    isreadyflag = true;
                } else if (portToResponseData.get(tport).length == portToResponseHeaderLength.get(tport) + 4 + portToLength.get(tport)) {
                    isreadyflag = true;
                }

                if (isreadyflag) {
                    String requestStr = new String(ArrayUtils.toPrimitive(portToRequestData.get(tport)));
                    String[] requestHeadBody = requestStr.split("\r\n\r\n", 2);
                    byte[] requestBody = new byte[]{};
                    if (requestHeadBody[1].length() > 0) {
                        requestBody = requestHeadBody[1].getBytes();
                    }
                    byte[] responseData = ArrayUtils.toPrimitive(portToResponseData.get(tport));
                    byte[] responseBody = Arrays.copyOfRange(responseData, portToResponseHeaderLength.get(tport) + 4, responseData.length);
                    Log.e("KCA", String.valueOf(responseData.length));
                    Log.e("KCA", String.valueOf(portToResponseHeaderPart.get(tport).length()));
                    Log.e("KCA", "====================================");
                    if (chunkflag) {
                        Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, 0, 15)));
                        Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, responseBody.length - 15, responseBody.length)));
                        responseBody = unchunkAllData(responseBody, gzipflag);
                    } else if (gzipflag) {
                        Log.e("KCA", "Ungzip " + String.valueOf(tport));
                        responseBody = gzipdecompress(responseBody);
                    }

                    //Log.e("KCA", String.valueOf(responseData.length));
                    String requestUri = portToUri.get(tport);
                    if (checkKcApi(requestUri)) {
                        KcaHandler k = new KcaHandler(handler, requestUri, requestBody, responseBody);
                        executorService.execute(k);
                    }
                    portToUri.remove(tport);
                    portToRequestData.remove(tport);
                    portToResponseData.remove(tport);
                    portToResponseHeaderLength.remove(tport);
                    portToLength.remove(tport);
                    portToGzipped.remove(tport);
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
            String responseDataStr = byteArrayToHex(responseData);
            if (responseDataStr.length() > 240) {
                error_data.addProperty("response", responseDataStr.substring(0, 240));
            } else {
                error_data.addProperty("response", responseDataStr);
            }
            KcaHandler k = new KcaHandler(handler, error_uri, empty_request.getBytes(), error_data.toString().getBytes());
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

    private static boolean checkKcRes(String uri) {
        boolean isKcsSwf = uri.contains("/kc") && uri.contains(".swf");
        boolean isKcaRes = uri.contains("/kcs/resources");
        boolean isKcsSound = uri.contains("/kcs/sound");
        //Log.e("KCA", uri + " " + String.valueOf(isKcaVer || isKcsApi));
        return (isKcsSwf || isKcaRes || isKcsSound);
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
