package com.antest1.kcanotify.mitm;

import java.io.Serializable;

public class MitmAPI {
    public static final String PACKAGE_NAME = "com.antest1.kcanotify.mitm";

    public static final String MITM_SERVICE = PACKAGE_NAME + ".MitmService";
    public static final int MSG_ERROR = -1;
    public static final int MSG_START_MITM = 1;
    public static final int MSG_GET_CA_CERTIFICATE = 2;
    public static final int MSG_STOP_MITM = 3;
    public static final int MSG_DISABLE_DOZE = 4;
    public static final String MITM_CONFIG = "mitm_config";
    public static final String CERTIFICATE_RESULT = "certificate";
    public static final String SSLKEYLOG_RESULT = "sslkeylog";

    public static final class MitmConfig implements Serializable {
        public int proxyPort;              // the SOCKS5 port to use to accept mitm-ed connections
        public boolean transparentMode;    // true to use transparent proxy mode, false to use SOCKS5 proxy mode
        public boolean sslInsecure;        // true to disable upstream certificate check
        public boolean dumpMasterSecrets;  // true to enable the TLS master secrets dump messages (similar to SSLKEYLOG)
        public boolean shortPayload;       // if true, only the initial portion of the payload will be sent
        public String proxyAuth;           // SOCKS5 proxy authentication, "user:pass"
        public String additionalOptions;   // provide additional options to mitmproxy
    }
}