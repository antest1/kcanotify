package com.antest1.kcanotify;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

public class KcaHandler implements Runnable {
	public Handler handler;

	private static final String HTTP_CONTENT_ENCODING = HttpHeaders.Names.CONTENT_ENCODING;
	
	public static final int BufferSize = 4096;
	
	public static Map<String, Boolean> flag = new HashMap<String, Boolean>();
	public static Map<String, JSONObject> data = new HashMap<String, JSONObject>();

	String url;
	FullHttpResponse response;

	boolean gzipped;
	byte[] requestBytes;
	byte[] responseBytes;
	
	public KcaHandler(Handler h, String u, byte[] b1, byte[] b2, boolean gz){
		handler = h;
		url = u;
		requestBytes = b1;
		responseBytes = b2;
		gzipped = gz;
	}
	
	public void run(){

		String data, reqData = "";
		try {
			if (gzipped) {
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				GZIPInputStream gzipInStream;
				gzipInStream = new GZIPInputStream(new BufferedInputStream(new ByteArrayInputStream(responseBytes)));

				int size;
				byte[] buffer = new byte[BufferSize];
				while ((size = gzipInStream.read(buffer)) > 0) {
					outStream.write(buffer, 0, size);
				}
				outStream.flush();
				outStream.close();
				data = new String(outStream.toByteArray());
			} else {
				data = new String(responseBytes);
				reqData = new String(requestBytes);
			}

			//parseJSON(url, data);
			Bundle bundle = new Bundle();
			bundle.putString("url", url.replace("/kcsapi", ""));
			bundle.putString("request", reqData);
			bundle.putString("data", data.replace("svdata=", ""));

			Message msg = handler.obtainMessage();
			msg.setData(bundle);
			
			handler.sendMessage(msg);
			
			bundle = null;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handler = null;
		responseBytes = null;

		//Log.e("KCA", "RefCnt: "+String.valueOf(contentBuf.refCnt()));
		Log.e("KCA", "Data Processed: "+url);
	}
	
	public static int parseJSON(String url, String resp) {
		String key = url.replace("/kcsapi", "");
		String resp_data_json = resp.replace("svdata=", "");
		JSONParser jsonParser = new JSONParser();
		JSONObject jsonSvData;
		try {
			jsonSvData = (JSONObject) jsonParser.parse(resp_data_json);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 1;
		} 
		return 0;
	}

}
