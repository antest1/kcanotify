/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.antest1.kcanotify.remote_capture.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.antest1.kcanotify.remote_capture.MitmAddon;

import java.util.HashSet;
import java.util.Set;

public class Prefs {
    public static final String IP_MODE_IPV4_ONLY = "ipv4";
    public static final String IP_MODE_IPV6_ONLY = "ipv6";
    public static final String IP_MODE_BOTH = "both";
    public static final String IP_MODE_DEFAULT = IP_MODE_IPV4_ONLY;

    public static final String BLOCK_QUIC_MODE_NEVER = "never";
    public static final String BLOCK_QUIC_MODE_ALWAYS = "always";
    public static final String BLOCK_QUIC_MODE_TO_DECRYPT = "to_decrypt";
    public static final String BLOCK_QUIC_MODE_DEFAULT = BLOCK_QUIC_MODE_NEVER;

    public static final String PREF_SOCKS5_PROXY_IP_KEY = "socks5_proxy_ip_address";
    public static final String PREF_SOCKS5_PROXY_PORT_KEY = "socks5_proxy_port";
    public static final String PREF_CAPTURE_INTERFACE = "capture_interface";
    public static final String PREF_TLS_DECRYPTION_KEY = "tls_decryption";
    public static final String PREF_IP_MODE = "ip_mode";
    public static final String PREF_SOCKS5_ENABLED_KEY = "socks5_enabled";
    public static final String PREF_SOCKS5_AUTH_ENABLED_KEY = "socks5_auth_enabled";
    public static final String PREF_SOCKS5_USERNAME_KEY = "socks5_username";
    public static final String PREF_SOCKS5_PASSWORD_KEY = "socks5_password";
    public static final String PREF_TLS_DECRYPTION_SETUP_DONE = "tls_decryption_setup_ok";
    public static final String PREF_CA_INSTALLATION_SKIPPED = "ca_install_skipped";
    public static final String PREF_BLOCK_QUIC = "block_quic_mode";
    public static final String PREF_AUTO_BLOCK_PRIVATE_DNS = "auto_block_private_dns";
    public static final String PREF_LOCKDOWN_VPN_NOTICE_SHOWN = "vpn_lockdown_notice";
    public static final String PREF_VPN_EXCEPTIONS = "vpn_exceptions";
    public static final String PREF_PORT_MAPPING = "port_mapping";
    public static final String PREF_PORT_MAPPING_ENABLED = "port_mapping_enabled";
    public static final String PREF_MITMPROXY_OPTS = "mitmproxy_opts";
    public static final String PREF_DNS_SERVER_V4 = "dns_v4";
    public static final String PREF_DNS_SERVER_V6 = "dns_v6";
    public static final String PREF_USE_SYSTEM_DNS = "system_dns";
    public static final String PREF_RESTART_ON_DISCONNECT = "restart_on_disconnect";
    public static final String PREF_IGNORED_MITM_VERSION = "ignored_mitm_version";

    public enum IpMode {
        IPV4_ONLY,
        IPV6_ONLY,
        BOTH,
    }

    public enum BlockQuicMode {
        NEVER,
        ALWAYS,
        TO_DECRYPT
    }

    public static IpMode getIPMode(String pref) {
        switch (pref) {
            case IP_MODE_IPV6_ONLY:     return IpMode.IPV6_ONLY;
            case IP_MODE_BOTH:          return IpMode.BOTH;
            default:                    return IpMode.IPV4_ONLY;
        }
    }

    public static BlockQuicMode getBlockQuicMode(String pref) {
        switch (pref) {
            case BLOCK_QUIC_MODE_ALWAYS:        return BlockQuicMode.ALWAYS;
            case BLOCK_QUIC_MODE_TO_DECRYPT:    return BlockQuicMode.TO_DECRYPT;
            default:                            return BlockQuicMode.NEVER;
        }
    }

    public static void setLockdownVpnNoticeShown(SharedPreferences p) {
        p.edit().putBoolean(PREF_LOCKDOWN_VPN_NOTICE_SHOWN, true).apply();
    }

    public static void setPortMappingEnabled(SharedPreferences p, boolean enabled) {
        p.edit().putBoolean(PREF_PORT_MAPPING_ENABLED, enabled).apply();
    }

    /* Prefs with defaults */
    public static boolean getTlsDecryptionEnabled(SharedPreferences p) { return(p.getBoolean(PREF_TLS_DECRYPTION_KEY, false)); }
    public static boolean getSocks5Enabled(SharedPreferences p)     { return(p.getBoolean(PREF_SOCKS5_ENABLED_KEY, false)); }
    public static String getSocks5ProxyHost(SharedPreferences p)    { return(p.getString(PREF_SOCKS5_PROXY_IP_KEY, "0.0.0.0")); }
    public static int getSocks5ProxyPort(SharedPreferences p)       { return(Integer.parseInt(p.getString(Prefs.PREF_SOCKS5_PROXY_PORT_KEY, "8080"))); }
    public static boolean isSocks5AuthEnabled(SharedPreferences p)  { return(p.getBoolean(PREF_SOCKS5_AUTH_ENABLED_KEY, false)); }
    public static String getSocks5Username(SharedPreferences p)     { return(p.getString(PREF_SOCKS5_USERNAME_KEY, "")); }
    public static String getSocks5Password(SharedPreferences p)     { return(p.getString(PREF_SOCKS5_PASSWORD_KEY, "")); }
    public static IpMode getIPMode(SharedPreferences p)          { return(getIPMode(p.getString(PREF_IP_MODE, IP_MODE_DEFAULT))); }
    public static BlockQuicMode getBlockQuicMode(SharedPreferences p) { return(getBlockQuicMode(p.getString(PREF_BLOCK_QUIC, BLOCK_QUIC_MODE_DEFAULT))); }
    public static String getCaptureInterface(SharedPreferences p) { return(p.getString(PREF_CAPTURE_INTERFACE, "@inet")); }
    public static boolean restartOnDisconnect(SharedPreferences p)        { return(p.getBoolean(PREF_RESTART_ON_DISCONNECT, false)); }
    public static boolean isTLSDecryptionSetupDone(SharedPreferences p)     { return(p.getBoolean(PREF_TLS_DECRYPTION_SETUP_DONE, false)); }
    public static boolean isPrivateDnsBlockingEnabled(SharedPreferences p) { return(p.getBoolean(PREF_AUTO_BLOCK_PRIVATE_DNS, true)); }
    public static boolean lockdownVpnNoticeShown(SharedPreferences p)      { return(p.getBoolean(PREF_LOCKDOWN_VPN_NOTICE_SHOWN, false)); }
    public static String getMitmproxyOpts(SharedPreferences p)    { return(p.getString(PREF_MITMPROXY_OPTS, "")); }
    public static boolean isPortMappingEnabled(SharedPreferences p) { return(p.getBoolean(PREF_PORT_MAPPING_ENABLED, true)); }
    public static boolean useSystemDns(SharedPreferences p)     { return(p.getBoolean(PREF_USE_SYSTEM_DNS, true)); }
    public static String getDnsServerV4(SharedPreferences p)    { return(p.getString(PREF_DNS_SERVER_V4, "1.1.1.1")); }
    public static String getDnsServerV6(SharedPreferences p)    { return(p.getString(PREF_DNS_SERVER_V6, "2606:4700:4700::1111")); }
    public static boolean isIgnoredMitmVersion(SharedPreferences p, String v) { return p.getString(PREF_IGNORED_MITM_VERSION, "").equals(v); }

    // Gets a StringSet from the prefs
    // The preference should either be a StringSet or a String
    // An empty set is returned as the default value
    @SuppressLint("MutatingSharedPrefs")
    public static @NonNull Set<String> getStringSet(SharedPreferences p, String key) {
        Set<String> rv = null;

        try {
            rv = p.getStringSet(key, null);
        } catch (ClassCastException e) {
            // retry with string
            String s = p.getString(key, "");

            if (!s.isEmpty()) {
                rv = new HashSet<>();
                rv.add(s);
            }
        }

        if (rv == null)
            rv = new HashSet<>();

        return rv;
    }

    public static String asString(Context ctx) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);

        // NOTE: possibly sensitive info like the collector IP address not shown
        return "TLSDecryption: " + getTlsDecryptionEnabled(p) +
                "\nTLSSetupOk: " + isTLSDecryptionSetupDone(p) +
                "\nCAInstallSkipped: " + MitmAddon.isCAInstallationSkipped(ctx) +
                "\nBlockQuic: " + getBlockQuicMode(p) +
                "\nSocks5: " + getSocks5Enabled(p) +
                "\nBlockPrivateDns: " + isPrivateDnsBlockingEnabled(p) +
                "\nCaptureInterface: " + getCaptureInterface(p) +
                "\nIpMode: " + getIPMode(p);
    }
}
