package com.antest1.kcanotify;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

public class KcaRequest extends AsyncTask<String, Void, String> {
    public static final String TIMEOUT_CODE = "timeout";
    public static final String ERROR_CODE = "erroroccured";
    @Override
    protected String doInBackground(String... params) {
        String content = "";
        try {
            for (int i=0; i<3; i++) {
                Log.e("KCA", "KcaRequest Request");
                content = Request(params[0], params[1], params[2], params[3]);
                if (!content.equals(TIMEOUT_CODE) && !content.equals(ERROR_CODE)) break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (content.equals(ERROR_CODE)) {
            Log.e("KCA", "KcaRequest Error: "+params[0]);
        } else {
            Log.e("KCA", "KcaRequest Responsed "+String.valueOf(content.length()));
        }
        return content;
    }

    public byte[] gzipcompress(String value) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutStream = new GZIPOutputStream(
                new BufferedOutputStream(byteArrayOutputStream));
        gzipOutStream.write(value.getBytes());
        gzipOutStream.finish();
        gzipOutStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static String gzipdecompress(byte[] bytes) throws Exception {
        String data = "";
        if(isCompressed(bytes)) {
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                data += line + "\n";
            }
        } else {
            data = new String(bytes);
        }
        return data;
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public String Request(String uri, String method, String header_str, String data) throws Exception {

        URL url = null;
        String[] header = header_str.split("\r\n");
        for (int i=0; i<header.length; i++) {
            String key = header[i].split(": ")[0];
            String value = header[i].split(": ")[1];
            if (key.startsWith(HttpHeaders.Names.HOST)) {
                url = new URL("http://" + value + uri);
                break;
            }
        }

        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setConnectTimeout(15000);
            http.setReadTimeout(2000);
            http.setDoInput(true);

            for (int i = 0; i < header.length; i++) {
                String key = header[i].split(": ")[0];
                String value = header[i].split(": ")[1];
                if (key.startsWith(HttpHeaders.Names.USER_AGENT)) {
                    http.setRequestProperty(key, String.format("Kca/%s ", BuildConfig.VERSION_NAME) + value);
                } else if (!key.startsWith(HttpHeaders.Names.VIA)) {
                    http.setRequestProperty(key, value);
                }
            }

            if (method == HttpMethod.POST.name()) {
                http.setDoOutput(true);
                OutputStream outStream = http.getOutputStream();
                outStream.write(data.getBytes());
                outStream.flush();
                outStream.close();
            }

            int code = http.getResponseCode();

            JsonObject responseData = new JsonObject();
            String responseHeaderString = "";
            Map<String, List<String>> map = http.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if(entry.getKey() != null && !entry.getKey().startsWith("X-Android") && !entry.getKey().startsWith("Via")) {
                    responseHeaderString += String.format("%s: %s\r\n", entry.getKey(), joinStr(entry.getValue(), "; "));
                }
            }
            responseData.addProperty("status", String.valueOf(code));
            responseData.addProperty("header", responseHeaderString);

            InputStream is = http.getInputStream();
            byte[] bytes = ByteStreams.toByteArray(is);
            String bytesData = Base64.encodeToString(bytes, Base64.DEFAULT);
            //Log.e("KCA", bytesData);
            responseData.addProperty("data", bytesData);

            return responseData.toString();
        } catch(java.net.SocketTimeoutException e) {
            return TIMEOUT_CODE;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_CODE;
        }
    }

    private String joinStr(List<String> list, String delim) {
        String resultStr = "";
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            resultStr = resultStr.concat(list.get(i));
            resultStr = resultStr.concat(delim);
        }
        resultStr = resultStr.concat(list.get(i));
        return resultStr;
    }

}
