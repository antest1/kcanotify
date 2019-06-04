package com.antest1.kcanotify;


import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.google.common.collect.EvictingQueue;
import com.google.common.primitives.Bytes;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.KCA_API_RESOURCE_URL;
import static com.antest1.kcanotify.KcaConstants.KCA_API_VPN_DATA_ERROR;
import static com.antest1.kcanotify.KcaUtils.byteArrayToHex;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.gzipdecompress;
import static com.antest1.kcanotify.KcaUtils.unchunkdata;

public class KcaVpnData {
    public static KcaDBHelper helper;
    public static Handler handler;

    private static final int NONE = 0;
    private static final int REQUEST = 1;
    private static final int RESPONSE = 2;

    // Full Server List: http://kancolle.wikia.com/wiki/Servers
    // 2017.10.18: Truk and Ringga Server was moved.
    private static String[] kcaServerPrefixList = {
            "203.104.209", // Yokosuka, Kure, Maizuru, Oominato Truk, Ringga, Kanoya, Iwagawa, Saikiman, Hashirajima, Android Server
            "125.6.189", // Shortland, Buin, Tawi-Tawi, Palau, Brunel, Hittokappuman, Paramushir, Sukumoman
            "125.6.184", // Sasebo
            "203.104.248"  // Rabaul
    };

    // ooi-based connector ips
    private static String[] kcaExtServiceList = {
            "104.27.146.101",
            "104.27.147.101",
            "120.194.85.200",
            "104.31.72.227",
            "104.31.73.227",
            "125.46.39.225"
    };
    private static String kcaDmmLoginServer = "202.6"; // DMM prefix

    private static String[] prefixCheckList = kcaServerPrefixList;

    public static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static int state = NONE;
    public static byte[] requestData = {};
    public static byte[] responseData = {};

    static boolean isRequestUriReady = false;
    static String requestUri = "";
    static boolean gzipflag = false;
    static boolean chunkflag = false;
    static boolean isreadyflag = false;

    static int responseBodyLength = -1;

    private static SparseArray<String> portToUri = new SparseArray<>();
    private static SparseArray<StringBuilder> portToRequestData = new SparseArray<>();
    private static SparseArray<Byte[]> portToResponseData = new SparseArray<>();
    private static SparseIntArray portToResponseHeaderLength = new SparseIntArray();
    private static SparseIntArray portToLength = new SparseIntArray();
    private static SparseBooleanArray portToGzipped = new SparseBooleanArray();
    private static SparseArray<String> portToResponseHeaderPart = new SparseArray<>();

    private static Queue<Integer> ignoreResponseList = EvictingQueue.create(32);

    public static void setHandler(Handler h) {
        handler = h;
    }

    public static void setExternalFilter(boolean use_ext) {
        if (use_ext) {
            prefixCheckList = Arrays.copyOf(kcaServerPrefixList, kcaServerPrefixList.length + kcaExtServiceList.length);
            System.arraycopy(kcaExtServiceList, 0, prefixCheckList, kcaServerPrefixList.length, kcaExtServiceList.length);
        } else {
            prefixCheckList = kcaServerPrefixList;
        }
        Log.e("KCA", Arrays.toString(prefixCheckList));
    }

    // Called from native code
    private static int containsKcaServer(int type, byte[] source, byte[] target) {
        String saddrstr = new String(source);
        String taddrstr = new String(target);
        if (type == REQUEST) {
            for (String prefix: prefixCheckList) {
                if (taddrstr.startsWith(prefix)) return 1;
            }
        } else if (type == RESPONSE) {
            for (String prefix: prefixCheckList) {
                if (saddrstr.startsWith(prefix)) return 1;
            }
        }
        //Log.e("KCAV", KcaUtils.format("containsKcaServer[%d] %s:%d => %s:%d", type, saddrstr, sport, taddrstr, tport));
        return 0;
    }

    // Called from native code
    private static void getDataFromNative(byte[] data, int size, int type, byte[] source, byte[] target, int sport, int tport) {
        try {
            String s = new String(data);
            String saddrstr = new String(source);
            String taddrstr = new String(target);
            Log.e("KCAV", KcaUtils.format("getDataFromNative[%d] %s:%d => %s:%d", type, saddrstr, sport, taddrstr, tport));

            if (type == REQUEST) {
                if (s.startsWith("GET") || s.startsWith("POST")) {
                    isRequestUriReady = false;
                    state = REQUEST;
                    portToRequestData.put(sport, new StringBuilder());
                }
                if (portToRequestData.get(sport) == null) return;
                else portToRequestData.get(sport).append(s);

                if(!isRequestUriReady && portToRequestData.get(sport).toString().contains("HTTP/1.1")) {
                    isRequestUriReady = true;
                    String[] head_line = portToRequestData.get(sport).toString().split("\r\n");
                    requestUri = head_line[0].split(" ")[1];
                    portToUri.put(sport, requestUri);
                    Log.e("KCAV", requestUri);
                    if (!checkKcRes(requestUri)) {
                        portToResponseData.put(sport, new Byte[]{});
                        portToResponseHeaderPart.put(sport, "");
                        portToGzipped.put(sport, false);
                        portToLength.put(sport, 0);
                        if (ignoreResponseList.contains(sport)) {
                            ignoreResponseList.remove(sport);
                        }
                    } else {
                        //KcaHandler k = new KcaHandler(handler, KCA_API_RESOURCE_URL, (requestUri + " " + sport).getBytes(), new byte[]{});
                        //executorService.execute(k);
                        if (!ignoreResponseList.contains(sport)) {
                            ignoreResponseList.add(sport);
                        }
                        Log.e("KCAV", ignoreResponseList.toString());
                    }
                }
            } else if (type == RESPONSE) {
                state = RESPONSE;
                if (ignoreResponseList.contains(tport)) {
                    Log.e("KCAV", portToUri.get(tport) + " ignored");
                    return;
                }
                if (portToResponseHeaderPart.indexOfKey(tport) >= 0 && portToResponseHeaderPart.get(tport).length() == 0) {
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
                Byte[] empty = {};
                Byte[] responsePrevData = portToResponseData.get(tport, empty);
                portToResponseData.put(tport, ArrayUtils.toObject(Bytes.concat(ArrayUtils.toPrimitive(responsePrevData), data)));
                if (portToLength.get(tport) == -1 && isChunkEnd(ArrayUtils.toPrimitive(portToResponseData.get(tport)))) {
                    isreadyflag = true;
                } else if (portToResponseData.get(tport).length == portToResponseHeaderLength.get(tport) + 4 + portToLength.get(tport)) {
                    isreadyflag = true;
                }

                if (isreadyflag) {
                    String requestStr = portToRequestData.get(tport).toString();
                    String[] requestHeadBody = requestStr.split("\r\n\r\n", 2);
                    if (requestHeadBody.length > 1) {
                        byte[] requestBody = new byte[]{};
                        if (requestHeadBody[1].length() > 0) {
                            requestBody = requestHeadBody[1].getBytes();
                        }
                        byte[] responseData = ArrayUtils.toPrimitive(portToResponseData.get(tport));
                        byte[] responseBody = Arrays.copyOfRange(responseData, portToResponseHeaderLength.get(tport) + 4, responseData.length);
                        //Log.e("KCA", String.valueOf(responseData.length));
                        //Log.e("KCA", String.valueOf(portToResponseHeaderPart.get(tport).length()));
                        //Log.e("KCA", "====================================");
                        if (chunkflag) {
                            //Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, 0, 15)));
                            //Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, responseBody.length - 15, responseBody.length)));
                            responseBody = unchunkAllData(responseBody, gzipflag);
                        } else if (gzipflag) {
                            //Log.e("KCA", "Ungzip " + String.valueOf(tport));
                            responseBody = gzipdecompress(responseBody);
                        }

                        //Log.e("KCA", String.valueOf(responseData.length));
                        String requestUri = portToUri.get(tport);
                        if (checkKcApi(requestUri)) {
                            KcaHandler k = new KcaHandler(handler, requestUri, requestBody, responseBody);
                            executorService.execute(k);
                        }
                        portToUri.delete(tport);
                        portToRequestData.delete(tport);
                        portToResponseData.delete(tport);
                        portToResponseHeaderLength.delete(tport);
                        portToLength.delete(tport);
                        portToGzipped.delete(tport);
                        isreadyflag = false;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            String error_uri = KCA_API_VPN_DATA_ERROR;
            String empty_request = "";
            JsonObject error_data = new JsonObject();
            error_data.addProperty("error", getStringFromException(e));
            error_data.addProperty("uri", requestUri);
            error_data.addProperty("request", requestData.toString());
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
        boolean isKcaRes = uri.contains("/kc") && uri.contains("/resources");
        boolean isKcsSound = uri.contains("/kcs/sound");
        boolean isKcsWorld = uri.contains("/api_world/get_id/");
        boolean isKcs2Res = uri.contains("/kcs2/img/");
        //Log.e("KCA", uri + " " + String.valueOf(isKcaVer || isKcsApi));
        return (isKcsSwf || isKcaRes || isKcsSound || isKcsWorld | isKcs2Res);
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
