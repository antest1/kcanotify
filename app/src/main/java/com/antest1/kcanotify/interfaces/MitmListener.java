package com.antest1.kcanotify.interfaces;

import androidx.annotation.Nullable;

public interface MitmListener {
    // NOTE: for fragments, this may be called when their context is null
    void onMitmGetCaCertificateResult(@Nullable String ca_pem);

    void onMitmServiceConnect();
    void onMitmServiceDisconnect();
}