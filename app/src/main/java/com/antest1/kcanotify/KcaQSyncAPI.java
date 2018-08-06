package com.antest1.kcanotify;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface KcaQSyncAPI {
    String USER_AGENT = KcaUtils.format("Kcanotify/%s ", BuildConfig.VERSION_NAME);

    @POST("/api/read")
    Call<String> read (
            @HeaderMap Map<String, String> headers,
            @Query(value = "data") String data
    );

    @POST("/api/write")
    Call<String> write (
            @HeaderMap Map<String, String> headers,
            @Query(value = "data") String data
    );
}
