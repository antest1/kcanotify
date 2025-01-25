package com.antest1.kcanotify.remote_capture.model;

import static com.antest1.kcanotify.KcaConstants.PREF_USE_TLS_DECRYPTION;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.remote_capture.Cidr;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CaptureSettings implements Serializable {
    public boolean socks5_enabled;
    public boolean socks5_allapps;
    public boolean tls_decryption;
    public String socks5_proxy_address;
    public int socks5_proxy_port;
    public String socks5_username;
    public String socks5_password;
    public List<String> socks5_bypass_addr;
    public Prefs.IpMode ip_mode;
    public Prefs.BlockQuicMode block_quic_mode;
    public boolean auto_block_private_dns;
    public String capture_interface;
    public int snaplen = 0;
    public String mitmproxy_opts;

    public CaptureSettings(Context ctx, SharedPreferences prefs) {
        // use existing socks5 preferences
        SharedPreferences shared_prefs = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        socks5_enabled = shared_prefs.getBoolean("socks5_enable", false);
        socks5_allapps = shared_prefs.getBoolean("socks5_allapps", false);
        socks5_proxy_address = shared_prefs.getString("socks5_address", "0.0.0.0");
        socks5_proxy_port = Integer.parseInt(shared_prefs.getString("socks5_port", "8080"));
        socks5_username = shared_prefs.getString("socks5_name", "");
        socks5_password = shared_prefs.getString("socks5_pass", "");
        tls_decryption = KcaUtils.getBooleanPreferences(ctx, PREF_USE_TLS_DECRYPTION);
        socks5_bypass_addr = new ArrayList<>();

        String bypass_list = shared_prefs.getString("bypass_address", "");
        if (!bypass_list.isEmpty()) {
            socks5_bypass_addr = Arrays.asList(bypass_list.split(","));
        }

        // TODO: move prefs to pref_settings.xml
        ip_mode = Prefs.getIPMode(prefs);
        capture_interface = Prefs.getCaptureInterface(prefs);
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
