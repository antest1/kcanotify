package com.antest1.kcanotify;


import android.os.Handler;
import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.ArrayUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.KcaConstants.KCA_API_VPN_DATA_ERROR;
import static com.antest1.kcanotify.KcaUtils.byteArrayToHex;
import static com.antest1.kcanotify.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.KcaUtils.gzipdecompress;

public class KcaVpnData {
    private static final String kcHostPostfix = ".kancolle-server.com";
    public static Handler handler;

    public static final int REQUEST = 1;
    public static final int RESPONSE = 2;

    private static final String HTTP_HEADER_END = "\r\n\r\n";

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
    private static final Map<Long, String> requestUriMap = new HashMap<>();
    private static final Map<Long, Byte[]> requestDataMap = new HashMap<>();

    public static void setHandler(Handler h) {
        handler = h;
    }

    public static void initVpnDataQueue() {
        requestUriMap.clear();
        requestDataMap.clear();
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
        Log.e("KCAD", "registerKcaServer "+ hoststr + ": " + addrstr + " - " + hoststr.contains(kcHostPostfix));
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
                String headstr = (new String(head)).split(HTTP_HEADER_END)[0];
                if (headstr.contains("Host: ") && headstr.contains(kcHostPostfix + "\r\n")) {
                    Log.e("KCAD", "kcs detected: " + taddrstr);
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
    public static void getDataFromNative(byte[] data, int size, int type, byte[] source, byte[] target, int sport, int tport, long tstamp) {
        String requestUri = "";
        byte[] requestData = new byte[]{};
        byte[] responseData = new byte[]{};

        try {
            if (source != null && target != null) {
                String saddrstr = new String(source);
                String taddrstr = new String(target);
                Log.e("KCAV", KcaUtils.format("getDataFromNative[%d] %s:%d => %s:%d (%d)", type, saddrstr, sport, taddrstr, tport, size));
            } else {
                Log.e("KCAV", KcaUtils.format("getDataFromNative[%d] %d MitM %d => %d (%d)", type, tstamp, sport, tport, size));
            }

            if (type == REQUEST) {
                requestData = data;
                String requestDataStr = new String(requestData);
                if (requestDataStr.contains(HTTP_HEADER_END)) {
                    String[] requestHeaderBody = requestDataStr.split(HTTP_HEADER_END, 2);
                    String[] requestHeaders = requestHeaderBody[0].split("\r\n");
                    requestUri = getApiUriPath(requestHeaders[0].split(" ")[1]);
                    if (checkKcVersion(requestUri) || checkKcApi(requestUri)) {
                        byte[] requestBody = requestHeaderBody[1].getBytes();
                        requestUriMap.put(tstamp, requestUri);
                        requestDataMap.put(tstamp, ArrayUtils.toObject(requestBody));
                    }
                }
            } else if (type == RESPONSE) {
                responseData = data;
                String responseDataStr = new String(data);
                if (responseDataStr.contains(HTTP_HEADER_END)) {
                    String[] responseHeaderBody = responseDataStr.split(HTTP_HEADER_END, 2);
                    String[] responseHeaders = responseHeaderBody[0].split("\r\n");
                    int responseBodyIndex = responseHeaderBody[0].length() + 4;

                    String contentType = "(none)";
                    boolean isGzipped = false;
                    for (String line : responseHeaders) {
                        String lineLower = line.toLowerCase();
                        if (lineLower.startsWith("content-type")) {
                            contentType = lineLower.replace("content-type:", "").trim();
                        }
                        if (lineLower.startsWith("content-encoding")) {
                            isGzipped = lineLower.contains("gzip");
                        }
                    }

                    // use only text/plain api responses (do not process resources)
                    byte[] responseBody = new byte[]{};
                    if (contentType.equals("text/plain")) {
                        responseBody = Arrays.copyOfRange(data, responseBodyIndex, data.length);
                        if (responseBody.length > 0 && isGzipped) {
                            responseBody = decompressGzipData(responseBody);
                        }
                    }

                    if (requestUriMap.containsKey(tstamp)) {
                        requestUri = requestUriMap.get(tstamp);
                        Log.e("KCAV", KcaUtils.format("%d %s", tstamp, requestUri));
                        byte[] requestBody = ArrayUtils.toPrimitive(requestDataMap.get(tstamp));
                        if (requestBody == null) requestBody = new byte[]{};

                        if (checkKcVersion(requestUri) || checkKcApi(requestUri)) {
                            KcaHandler k = new KcaHandler(
                                    handler, requestUri, requestBody, responseBody, false);
                            executorService.execute(k);
                            requestUriMap.remove(tstamp);
                            requestDataMap.remove(tstamp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String error_uri = KCA_API_VPN_DATA_ERROR;
            String empty_request = "";
            JsonObject error_data = new JsonObject();
            error_data.addProperty("error", getStringFromException(e));
            error_data.addProperty("uri", requestUri);
            error_data.addProperty("request", new String(requestData));
            String responseDataStr = byteArrayToHex(responseData);
            if (responseDataStr.length() > 240) {
                error_data.addProperty("response", responseDataStr.substring(0, 240));
            } else {
                error_data.addProperty("response", responseDataStr);
            }
            KcaHandler k = new KcaHandler(
                    handler, error_uri,
                    empty_request.getBytes(),error_data.toString().getBytes(), false);
            executorService.execute(k);
            Log.e("KCA", getStringFromException(e));
        }
    }

    private static boolean checkKcVersion(String uri) {
        return uri != null && (uri.contains("/kca/version") || uri.contains("/kcs2/version"));
    }

    private static boolean checkKcApi(String uri) {
        return uri != null && uri.contains("/kcsapi/api_");
    }

    private static String getApiUriPath(String uri) {
        if (uri.startsWith("http")) {
            try {
                return (new URL(uri)).getPath();
            } catch (MalformedURLException e) {
                return uri;
            }
        } else {
            return uri;
        }
    }

    private static byte[] decompressGzipData(byte[] data) {
        try {
            return gzipdecompress(data);
        } catch (Exception e) {
            String hex = byteArrayToHex(data);
            if (hex.length() > 256) hex = hex.substring(0, 256);
            Log.e("KCA", "data: " + hex);
            Log.e("KCA", getStringFromException(e));
            return data;
        }
    }
}
