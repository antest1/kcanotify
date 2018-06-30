package com.antest1.kcanotify;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import retrofit2.Call;

public class KcaRequestThread extends Thread {
    private Call<String> call;
    private StringBuilder output = new StringBuilder();
    String url;

    public KcaRequestThread(Call<String> call) {
        this.call = call;
        output = new StringBuilder();
    }

    public void run() {
        try {
            String result = call.execute().body();
            output.append(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getResult() {
        return output.toString();
    }

}
