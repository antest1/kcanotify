package com.antest1.kcanotify;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface KcaResourceInfoDownloader {
    @Headers({
            "Accept: application/json",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/temp/icon_info.json")
    Call<String> getIconInfo();

    @Headers({
            "Accept: application/octet-stream",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",

    })
    @GET("/kcanotify/kca_api_start2.php?")
    Call<String> getGameData(@Query("v") String v);
}
