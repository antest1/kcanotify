package com.antest1.kcanotify;


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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.KCA_API_VPN_DATA_ERROR;
import static com.antest1.kcanotify.KcaUtils.byteArrayToHex;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.gzipdecompress;
import static com.antest1.kcanotify.KcaUtils.unchunkdata;

public class KcaVpnData {
    private static final String kcHostPostfix = ".kancolle-server.com";

    public static KcaDBHelper helper;
    public static Handler handler;

    public static final int NONE = 0;
    public static final int REQUEST = 1;
    public static final int RESPONSE = 2;
    public static final int HEADER_INSPECT_SIZE = 256;

    // ooi-based connector ips
    private static String[] kcaExtServiceList = {
            "104.27.146.101",
            "104.27.147.101",
            "120.194.85.200",
            "104.31.72.227",
            "104.31.73.227",
            "125.46.39.225"
    };

    private static Set<String> prefixCheckList = new HashSet<>();
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

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
            try {
                String[] ooi_addresses = {"ooi.moe", "cn.kcwiki.org", "kancolle.su"};
                for (String ooi: ooi_addresses) {
                    String[] ooi_ip = KcaUtils.getIpAddress(ooi);
                    prefixCheckList.addAll(Arrays.asList(ooi_ip));
                }
            } catch (Exception e) {
                for (String ooi: kcaExtServiceList) {
                    String[] ooi_ip = KcaUtils.getIpAddress(ooi);
                    prefixCheckList.addAll(Arrays.asList(ooi_ip));
                }
            }
        }
        Log.e("KCA", prefixCheckList.toString());
    }

    // Called from native code
    public static void registerKcaServer(byte[] host, byte[] addr) {
        String hoststr = new String(host);
        String addrstr = new String(addr);
        Log.e("KCAV", "registerKcaServer "+ hoststr + ": " + addrstr + " - " + hoststr.contains(kcHostPostfix));
        if (hoststr.contains(kcHostPostfix)) {
            prefixCheckList.add(addrstr);
        }
    }

    // Called from native code
    public static int containsKcaServer(int type, byte[] source, byte[] target, byte[] head) {
        String saddrstr = new String(source);
        String taddrstr = new String(target);

        if (type == REQUEST) {
            if (prefixCheckList.contains(taddrstr)) {
                return 1;
            } else if (head != null) { // detect kcs host from request header (as backup)
                String headstr = (new String(head)).split("\r\n\r\n")[0];
                if (headstr.contains("Host: ") && headstr.contains(kcHostPostfix + "\r\n")) {
                    Log.e("KCAV", "kcs detected: " + taddrstr);
                    prefixCheckList.add(taddrstr);
                    return 1;
                }
            }
            for (String prefix: prefixCheckList) {
                if (taddrstr.startsWith(prefix)) return 1;
            }
        } else if (type == RESPONSE) {
            if (prefixCheckList.contains(saddrstr)) return 1;
            for (String prefix: prefixCheckList) {
                if (saddrstr.startsWith(prefix)) return 1;
            }
        }
        return 0;
    }

    // Called from native code
    public static void getDataFromNative(byte[] data, int size, int type, byte[] source, byte[] target, int sport, int tport) {
        try {
            if (source != null && target != null) {
                String saddrstr = new String(source);
                String taddrstr = new String(target);
                Log.e("KCAV", KcaUtils.format("getDataFromNative[%d] %s:%d => %s:%d", type, saddrstr, sport, taddrstr, tport));
            } else {
                Log.e("KCAV", KcaUtils.format("getDataFromNative[%d] MitM %d => %d", type, sport, tport));
            }

            if (type == REQUEST) {
                String requestDataStr = new String(data);
                if (requestDataStr.startsWith("GET") || requestDataStr.startsWith("POST")) {
                    isRequestUriReady = false;
                    state = REQUEST;
                    portToRequestData.put(sport, new StringBuilder());
                }
                if (portToRequestData.get(sport) == null) return;
                else portToRequestData.get(sport).append(requestDataStr);

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
                            String lineLower = line.toLowerCase();
                            if (lineLower.startsWith("content-encoding: ")) {
                                portToGzipped.put(tport, line.contains("gzip"));
                                //Log.e("KCA", String.valueOf(tport) + " gzip " + portToGzipped.get(tport));
                            }
                            if (lineLower.startsWith("transfer-encoding")) {
                                if (line.contains("chunked")) {
                                    portToLength.put(tport, -1);
                                    responseBodyLength = -1;
                                }
                            } else if (lineLower.startsWith("content-length")) {
                                int length = Integer.parseInt(lineLower.replace("content-length: ", "").trim());
                                responseBodyLength = length;
                                portToLength.put(tport, length);
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
                        if (chunkflag && responseBody.length > 0) {
                            //Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, 0, 15)));
                            //Log.e("KCA", byteArrayToHex(Arrays.copyOfRange(responseBody, responseBody.length - 15, responseBody.length)));
                            responseBody = unchunkAllData(responseBody, gzipflag);
                        } else if (gzipflag && responseBody.length > 0) {
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
        if (uri.contains("/kca/version") || uri.contains("/kcs2/version")) return true;
        if (uri.contains("/kcsapi/api_")) return true;

        return false;
    }

    private static boolean checkKcRes(String uri) {
        if (uri.contains("/api_world/get_id/")) return true;
        if (uri.contains("/gadget_html5/") || uri.contains("/kcscontents/")) return true;
        if (uri.contains("/kcs2/index.php") || uri.contains("favicon.ico")) return true;
        if (uri.contains("/kcs2/js/") || uri.contains("/kcs2/css/") || uri.contains("/kcs2/resources/")
                || uri.contains("/kcs/sound") || uri.contains("/kcs2/img")) return true;
        return false;
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
