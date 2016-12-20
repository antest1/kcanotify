package com.antest1.kcanotify;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public class KcaProxyServer {
	private static boolean is_on;
	private static HttpProxyServer proxyServer;
	private static final int PORT = 24110;
	public static Context cntx;
	public static Handler handler;
	private static final String KCA_HOST = "Host: 203.104.209.7";
	private static final String KC_APP_REFERER = "Referer: app:/AppMain.swf/";
	private static final String KCANOTIFY_REFERER = "Referer: app:/KCA/";
	private static final String HTTP_CONTENT_ENCODING = HttpHeaders.Names.CONTENT_ENCODING;

	public static int delays = 20;
	public static void incDelays() {
		if (delays < 50) {
			delays += 10;
		}
	}

	public static void decDelays() {
		if (delays > 20) {
			delays -= 10;
		}
	}

	public static ExecutorService executorService = Executors.newScheduledThreadPool(15);

	public KcaProxyServer() {
		proxyServer = null;
	}

	public static void start(Handler h) {
		handler = h;
		HttpFiltersSource filtersSource = getFiltersSource();
		proxyServer = DefaultHttpProxyServer.bootstrap().withPort(PORT).withAllowLocalOnly(false)
				.withConnectTimeout(120000).withFiltersSource(filtersSource).withName("FilterProxy").start();
		// //Log.e("KCA", "Start");
	}

	public static void stop() {
		handler = null;
		if (is_on()) {
			proxyServer.abort();
			proxyServer = null;
		}
		// //Log.e("KCA", "Stop");
	}

	public static boolean is_on() {
		return proxyServer != null;
	}

	private static HttpFiltersSource getFiltersSource() {
		return new HttpFiltersSourceAdapter() {

			@Override
			public int getMaximumResponseBufferSizeInBytes() {
				return 128 * 1024 * 1024;
			}

			@Override
			public HttpFilters filterRequest(HttpRequest originalRequest) {

				return new HttpFiltersAdapter(originalRequest) {
					boolean isKcsApi = false;
					boolean isKcsRes = false;
					boolean isKcaRes = false;
					boolean isKcaVer = false;
					boolean isKcaApiS2 = false;
					boolean is_kca = false;

					String currentUrl = "";

					@Override
					public HttpResponse proxyToServerRequest(HttpObject httpObject) {
						if (httpObject instanceof HttpRequest) {
							is_kca = false;
							HttpRequest request = (HttpRequest) httpObject;
							String requestUri = request.getUri();

							if(request.headers().contains(HttpHeaders.Names.ACCEPT_ENCODING)) {
								String acceptEncodingData = request.headers().get(HttpHeaders.Names.ACCEPT_ENCODING).trim();
								if (acceptEncodingData.endsWith(",")) {
									acceptEncodingData = acceptEncodingData.concat(" sdch");
									request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, acceptEncodingData);
								}
							}

							Log.e("KCA", "Request " + request);
							Log.e("KCA", request.toString());
							String[] requestData = request.toString().split("\n");

							boolean isKcRequest = false;
							isKcaVer = requestUri.startsWith("/kca/version");
							isKcsApi = requestUri.startsWith("/kcsapi/api_");
							isKcsRes = requestUri.startsWith("/kcs/resources");
							isKcaRes = requestUri.startsWith("/kca/resources");
							isKcaApiS2 = requestUri.startsWith("/kcanotify/kca_api_start2.php");
							for (int i = 0; i < requestData.length; i++) {
								if (requestData[i].startsWith(KC_APP_REFERER) || requestData[i].startsWith(KCA_HOST)
										|| requestData[i].startsWith(KCANOTIFY_REFERER)) {
									isKcRequest = true;
									break;
								}
							}

							if (isKcRequest && (isKcaVer || isKcsApi || isKcsRes || isKcaRes || isKcaApiS2)) {
								// Log.e("KCA", "Request " + requestUri);
								// //Log.e("KCA", request.getUri());
								is_kca = true;
								currentUrl = requestUri;
								try {
									Thread.sleep(delays);
									//Log.e("KCA", "Delay " + String.valueOf(delays));
									//Log.e("KCA", "Request " + requestUri);
								} catch(InterruptedException ex) {
									Thread.currentThread().interrupt();
								}
							}
						}
						return null;
					}
					
					@Override
					public HttpObject serverToProxyResponse(HttpObject httpObject) {
						boolean gzipped = false;

						if (httpObject instanceof FullHttpResponse) {

							FullHttpResponse response = (FullHttpResponse) httpObject;


							//Log.e("KCA", "Response " + currentUrl + " " +
							//String.valueOf(contentBuf.readableBytes()));

							if (response.headers().contains(HTTP_CONTENT_ENCODING)) {
								if (response.headers().get(HTTP_CONTENT_ENCODING).startsWith("gzip")) {
									gzipped = true;
								}
							}
							
							if (is_kca) {


								if (isKcsApi || isKcaVer || isKcaApiS2) {
									ByteBuf contentBuf = response.content();
									Log.e("KCA", "Response " + currentUrl + " " + String.valueOf(contentBuf.readableBytes()));
									byte[] bytes = new byte[contentBuf.readableBytes()];
									int readerIndex = contentBuf.readerIndex();
									contentBuf.getBytes(readerIndex, bytes);

									KcaHandler k = new KcaHandler(handler, currentUrl, bytes, gzipped);
									executorService.execute(k);

									response = null;
									contentBuf = null;
									bytes = null;
								}
							}

						}

						return httpObject;

					}

					@Override
					public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
						ChannelPipeline pipeline = serverCtx.pipeline();
						if (pipeline.get("inflater") != null) {
							// Log.e("KCA", "remove inflater");
							pipeline.remove("inflater");
						}
						/*
						 * if (pipeline.get("aggregator") != null) {
						 * //Log.e("KCA", "remove aggregator");
						 * pipeline.remove("aggregator"); }
						 */
						super.proxyToServerConnectionSucceeded(serverCtx);
					}

				};
			}

		};
	}

}
