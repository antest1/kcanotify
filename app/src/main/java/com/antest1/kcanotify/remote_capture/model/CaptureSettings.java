package com.antest1.kcanotify.remote_capture.model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CaptureSettings implements Serializable {
    public boolean socks5_enabled;
    public boolean tls_decryption;
    public String socks5_proxy_address;
    public int socks5_proxy_port;
    public String socks5_username;
    public String socks5_password;
    public Prefs.IpMode ip_mode;
    public Prefs.BlockQuicMode block_quic_mode;
    public boolean auto_block_private_dns;
    public String capture_interface;
    public int snaplen = 0;
    public String mitmproxy_opts;

    public CaptureSettings(Context ctx, SharedPreferences prefs) {
        socks5_enabled = Prefs.getSocks5Enabled(prefs);
        socks5_proxy_address = Prefs.getSocks5ProxyHost(prefs);
        socks5_proxy_port = Prefs.getSocks5ProxyPort(prefs);
        socks5_username = Prefs.isSocks5AuthEnabled(prefs) ? Prefs.getSocks5Username(prefs) : "";
        socks5_password = Prefs.isSocks5AuthEnabled(prefs) ? Prefs.getSocks5Password(prefs) : "";
        ip_mode = Prefs.getIPMode(prefs);
        capture_interface = Prefs.getCaptureInterface(prefs);
        tls_decryption = Prefs.getTlsDecryptionEnabled(prefs);
        block_quic_mode = Prefs.getBlockQuicMode(prefs);
        auto_block_private_dns = Prefs.isPrivateDnsBlockingEnabled(prefs);
        mitmproxy_opts = Prefs.getMitmproxyOpts(prefs);
    }

    private static String getString(Intent intent, String key, String def_value) {
        String val = intent.getStringExtra(key);
        return (val != null) ? val : def_value;
    }

    // get a integer value from the bundle. The value may be represented as an int or as a string.
    private static int getInt(Intent intent, String key, int def_value) {
        Bundle bundle = intent.getExtras();

        String s = bundle.getString(key);
        if(s != null)
            return Integer.parseInt(s);
        return bundle.getInt(key, def_value);
    }

    // get a boolean value from the bundle. The value may be represented as a bool or as a string.
    private static boolean getBool(Intent intent, String key, boolean def_value) {
        Bundle bundle = intent.getExtras();

        String s = bundle.getString(key);
        if(s != null)
            return Boolean.parseBoolean(s);
        return bundle.getBoolean(key, def_value);
    }

    // get a list of comma-separated strings from the bundle
    private static List<String> getStringList(Intent intent, String key) {
        List<String> rv;

        String s = intent.getStringExtra(key);
        if(s != null) {
            if (s.indexOf(',') < 0) {
                rv = new ArrayList<>();
                rv.add(s);
            } else {
                String[] arr = s.split(",");
                rv = Arrays.asList(arr);
            }
        } else
            rv = new ArrayList<>();

        return rv;
    }

}
