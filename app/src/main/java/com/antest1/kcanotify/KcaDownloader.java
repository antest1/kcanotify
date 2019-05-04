package com.antest1.kcanotify;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface KcaDownloader {
    @Headers({
            "Accept: application/json",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Cache-control: no-cache, no-store, must-revalidate",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/v.php")
    Call<String> getRecentVersion();

    @Headers({
            "Accept: application/json",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Cache-Control: no-cache, no-store, must-revalidate",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/list.php")
    Call<String> getResourceList();

    @Headers({
            "Accept: application/octet-stream",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Cache-Control: no-cache, no-store, must-revalidate",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/kca_api_start2.php?")
    Call<String> getGameData(@Query("v") String v);
}
