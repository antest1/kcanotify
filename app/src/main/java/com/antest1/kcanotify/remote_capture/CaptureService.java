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
 * Copyright 2020-24 - Emanuele Faranda
 */

package com.antest1.kcanotify.remote_capture;

import static com.antest1.kcanotify.KcaConstants.DMMLOGIN_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.GOTO_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.KC_WV_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_PACKAGE_ALLOW;
import static com.antest1.kcanotify.KcaConstants.PREF_USE_TLS_DECRYPTION;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.antest1.kcanotify.KcaApplication;
import com.antest1.kcanotify.KcaUtils;
import com.antest1.kcanotify.R;
import com.antest1.kcanotify.remote_capture.model.AppDescriptor;
import com.antest1.kcanotify.remote_capture.model.BlacklistDescriptor;
import com.antest1.kcanotify.remote_capture.model.CaptureSettings;
import com.antest1.kcanotify.remote_capture.model.ConnectionDescriptor;
import com.antest1.kcanotify.remote_capture.model.ConnectionUpdate;
import com.antest1.kcanotify.remote_capture.model.MatchList;
import com.antest1.kcanotify.remote_capture.model.PortMapping;
import com.antest1.kcanotify.remote_capture.model.Prefs;
import com.antest1.kcanotify.remote_capture.model.CaptureStats;
import com.antest1.kcanotify.remote_capture.pcap_dump.PktsPcapDumper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CaptureService extends VpnService implements Runnable {
    private static final String TAG = "CaptureService";
    private static final int VPN_MTU = 10000;
    private static CaptureService INSTANCE;
    private static boolean HAS_ERROR = false;
    final ReentrantLock mLock = new ReentrantLock();
    final Condition mCaptureStopped = mLock.newCondition();
    private ParcelFileDescriptor mParcelFileDescriptor;
    private boolean mIsAlwaysOnVPN;
    private boolean mRevoked;
    private SharedPreferences mPrefs;
    private CaptureSettings mSettings;
    private Handler mHandler;
    private Thread mCaptureThread;
    private Thread mBlacklistsUpdateThread;
    private Thread mConnUpdateThread;
    private Thread mDumperThread;
    private MitmReceiver mMitmReceiver;
    private final LinkedBlockingDeque<Pair<ConnectionDescriptor[], ConnectionUpdate[]>> mPendingUpdates = new LinkedBlockingDeque<>(32);
    private LinkedBlockingDeque<byte[]> mDumpQueue;
    private String vpn_ipv4;
    private String vpn_dns;
    private String dns_server;
    private long last_bytes;
    private int last_connections;
    private int[] mAppFilterUids;
    private com.antest1.kcanotify.remote_capture.interfaces.PcapDumper mDumper;
    private ConnectionsRegister conn_reg;
    private Uri mPcapUri;
    private String mPcapFname;
    private long mMonitoredNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private AppsResolver mNativeAppsResolver; // can only be accessed by native code to avoid concurrency issues
    private boolean mMalwareDetectionEnabled;
    private boolean mBlacklistsUpdateRequested;
    private boolean mFirewallEnabled;
    private boolean mBlockPrivateDns;
    private boolean mDnsEncrypted;
    private boolean mStrictDnsNoticeShown;
    private boolean mQueueFull;
    private boolean mStopping;
    private MatchList mDecryptionList;
    private SparseArray<String> mIfIndexToName;
    private boolean mSocks5Enabled;
    private String mSocks5Address;
    private int mSocks5Port;
    private String mSocks5Auth;
    private static final MutableLiveData<CaptureStats> lastStats = new MutableLiveData<>();
    private static final MutableLiveData<ServiceStatus> serviceStatus = new MutableLiveData<>();
    private boolean mLowMemory;
    private BroadcastReceiver mNewAppsInstallReceiver;
    private Utils.PrivateDnsMode mPrivateDnsMode;

    /* The maximum connections to log into the ConnectionsRegister. Older connections are dropped.
     * Max estimated memory usage: less than 4 MB (+8 MB with payload mode minimal). */
    public static final int CONNECTIONS_LOG_SIZE = 8192;

    /* The IP address of the virtual network interface */
    public static final String VPN_IP_ADDRESS = "10.215.173.1";
    public static final String VPN_IP6_ADDRESS = "fd00:2:fd00:1:fd00:1:fd00:1";

    /* The DNS server IP address to use to internally analyze the DNS requests.
     * It must be in the same subnet of the VPN network interface.
     * After the analysis, requests will be routed to the primary DNS server. */
    public static final String VPN_VIRTUAL_DNS_SERVER = "10.215.173.2";

    public enum ServiceStatus {
        STOPPED,
        STARTED
    }

    static {
        /* Load native library */
        try {
            System.loadLibrary("capture");
            CaptureService.initPlatformInfo(Utils.getAppVersionString(), Utils.getDeviceModel(), Utils.getOsVersion());
        } catch (UnsatisfiedLinkError e) {
            // This should only happen while running tests
            //e.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base.createConfigurationContext(Utils.getLocalizedConfig(base)));
    }

    @Override
    public void onCreate() {
        Log.d(CaptureService.TAG, "onCreate");
        AppsResolver.clearMappedApps();
        mNativeAppsResolver = new AppsResolver(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings = new CaptureSettings(this, mPrefs); // initialize to prevent NULL pointer exceptions in methods (e.g. isRootCapture)

        INSTANCE = this;
        super.onCreate();
    }

    private int abortStart() {
        stopService();
        updateServiceStatus(ServiceStatus.STOPPED);
        return START_NOT_STICKY;
    }

    private static boolean alwaysOnVpnErrorLogged = false;

    // Android does not provide a reliable API to track the always-on VPN state
    // This function tries to detect but may fail to do so
    private boolean isAlwaysOnVpnDetected() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return isAlwaysOn();

        try {
            String always_on_vpn_app = Settings.Secure.getString(getContentResolver(), "always_on_vpn_app");
            return always_on_vpn_app.equals(getPackageName());
        } catch (Exception e) {
            if (!alwaysOnVpnErrorLogged) {
                Log.w(TAG, "Querying the always-on VPN state failed: " + e);
                alwaysOnVpnErrorLogged = true;
            }
            return false;
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        mStopping = false;

        // NOTE: onStartCommand may be called when the capture is already running, e.g. if the user
        // turns on the always-on VPN while the capture is running in root mode
        if (mCaptureThread != null) {
            // Restarting the capture requires calling stopAndJoinThreads, which is blocking.
            // Choosing not to support this right now.
            Log.e(TAG, "Restarting the capture is not supported");
            return abortStart();
        }

        mHandler = new Handler(Looper.getMainLooper());

        Log.d(CaptureService.TAG, "onStartCommand");

        // NOTE: a null intent may be delivered due to START_STICKY
        // It can be simulated by starting the capture, putting PCAPdroid in the background and then running:
        //  adb shell ps | grep remote_capture | awk '{print $2}' | xargs adb shell run-as com.antest1.kcanotify.remote_capture.debug kill
        CaptureSettings settings = ((intent == null) ? null : Utils.getSerializableExtra(intent, "settings", CaptureSettings.class));
        if (settings == null) {
            // Use the settings from mPrefs

            // An Intent without extras is delivered in case of always on VPN
            // https://developer.android.com/guide/topics/connectivity/vpn#always-on
            mIsAlwaysOnVPN = (intent != null);
            Log.i(CaptureService.TAG, "Missing capture settings, using SharedPrefs");
        } else {
            // Use the provided settings
            mSettings = settings;
            mIsAlwaysOnVPN = false;
        }

        mIsAlwaysOnVPN |= isAlwaysOnVpnDetected();

        Log.d(TAG, "alwaysOn? " + mIsAlwaysOnVPN);
        if (mIsAlwaysOnVPN) {
            mSettings.root_capture = false;
            mSettings.input_pcap_path = null;
        }

        if (mSettings.readFromPcap()) {
            // Disable incompatible settings
            mSettings.dump_mode = Prefs.DumpMode.NONE;
            mSettings.app_filter.clear();
            mSettings.socks5_enabled = false;
            mSettings.tls_decryption = false;
            mSettings.root_capture = false;
            mSettings.auto_block_private_dns = false;
            mSettings.capture_interface = mSettings.input_pcap_path;
        }

        mSettings.tls_decryption = KcaUtils.getBooleanPreferences(getApplicationContext(), PREF_USE_TLS_DECRYPTION);
        mSettings.dump_mode = Prefs.DumpMode.PCAP_FILE;
        mSettings.full_payload = true;

        // Retrieve DNS server
        String fallbackDnsV4 = Prefs.getDnsServerV4(mPrefs);
        dns_server = fallbackDnsV4;
        mBlockPrivateDns = false;
        mStrictDnsNoticeShown = false;
        mDnsEncrypted = false;
        setPrivateDnsBlocked(false);

        // Map network interfaces
        mIfIndexToName = new SparseArray<>();

        Enumeration<NetworkInterface> ifaces = Utils.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();

            Log.d(TAG, "ifidx " + iface.getIndex() + " -> " + iface.getName());
            mIfIndexToName.put(iface.getIndex(), iface.getName());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);
            Network net = cm.getActiveNetwork();

            if (net != null) {
                handleLinkProperties(cm.getLinkProperties(net));

                if (Prefs.useSystemDns(mPrefs) || mSettings.root_capture) {
                    dns_server = Utils.getDnsServer(cm, net);
                    if (dns_server == null)
                        dns_server = fallbackDnsV4;
                    else {
                        mMonitoredNetwork = net.getNetworkHandle();
                        registerNetworkCallbacks();
                    }
                } else
                    dns_server = fallbackDnsV4;
            }
        }

        vpn_dns = VPN_VIRTUAL_DNS_SERVER;
        vpn_ipv4 = VPN_IP_ADDRESS;
        last_bytes = 0;
        last_connections = 0;
        mLowMemory = false;
        conn_reg = new ConnectionsRegister(this, CONNECTIONS_LOG_SIZE);
        mDumper = null;
        mDumpQueue = null;
        mPendingUpdates.clear();
        mPcapFname = null;
        HAS_ERROR = false;


        // Max memory usage = (JAVA_PCAP_BUFFER_SIZE * 64) = 32 MB
        mDumper = new PktsPcapDumper();
        mDumpQueue = new LinkedBlockingDeque<>(64);

        try {
            mDumper.startDumper();
        } catch (IOException | SecurityException e) {
            reportError(e.getLocalizedMessage());
            e.printStackTrace();
            mDumper = null;
            return abortStart();
        }

        mSocks5Address = "";
        mSocks5Enabled = mSettings.socks5_enabled || mSettings.tls_decryption;
        if (mSocks5Enabled) {
            if (mSettings.tls_decryption) {
                // Built-in decryption
                mSocks5Address = "127.0.0.1";
                mSocks5Port = MitmReceiver.TLS_DECRYPTION_PROXY_PORT;
                mSocks5Auth = Utils.genRandomString(8) + ":" + Utils.genRandomString(8);

                mMitmReceiver = new MitmReceiver(this, mSettings, mSocks5Auth);
                try {
                    if (!mMitmReceiver.start())
                        return abortStart();
                } catch (IOException e) {
                    e.printStackTrace();
                    return abortStart();
                }
            } else {
                // SOCKS5 proxy
                mSocks5Address = mSettings.socks5_proxy_address;
                mSocks5Port = mSettings.socks5_proxy_port;

                if (!mSettings.socks5_username.isEmpty() && !mSettings.socks5_password.isEmpty())
                    mSocks5Auth = mSettings.socks5_username + ":" + mSettings.socks5_password;
                else
                    mSocks5Auth = null;
            }
        }

        if (mSettings.tls_decryption) {
            mDecryptionList = KcaApplication.getInstance().getDecryptionList();
            Log.d(TAG, "mDecryptionList: " + mDecryptionList.getSize());
        } else {
            mDecryptionList = null;
        }

        mAppFilterUids = new int[0];

        if(!mSettings.root_capture && !mSettings.readFromPcap()) {
            Log.i(TAG, "Using DNS server " + dns_server);

            // VPN
            /* In order to see the DNS packets into the VPN we must set an internal address as the DNS
             * server. */
            Builder builder = new Builder()
                    .setMtu(VPN_MTU);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                builder.setMetered(false);

            if (getIPv4Enabled() == 1) {
                builder.addAddress(vpn_ipv4, 30)
                        .addRoute("0.0.0.0", 1)
                        .addRoute("128.0.0.0", 1)
                        .addDnsServer(vpn_dns);
            }

            if (getIPv6Enabled() == 1) {
                builder.addAddress(VPN_IP6_ADDRESS, 128);

                // Route unicast IPv6 addresses
                builder.addRoute("2000::", 3);
                builder.addRoute("fc00::", 7);

                try {
                    builder.addDnsServer(InetAddress.getByName(Prefs.getDnsServerV6(mPrefs)));
                } catch (UnknownHostException | IllegalArgumentException e) {
                    Log.w(TAG, "Could not set IPv6 DNS server");
                }
            }

            // only allow kc-related and specified apps
            try {
                SharedPreferences prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);
                boolean socks5_enable = prefs.getBoolean("socks5_enable", false);
                boolean socks5_allapps = prefs.getBoolean("socks5_allapps", false);

                if (!socks5_enable || !socks5_allapps) {
                    builder.addAllowedApplication(KC_PACKAGE_NAME);
                    builder.addAllowedApplication(KC_WV_PACKAGE_NAME);
                    builder.addAllowedApplication(GOTO_PACKAGE_NAME);
                    if (socks5_enable) builder.addAllowedApplication(DMMLOGIN_PACKAGE_NAME);

                    JsonArray allowed_apps = JsonParser.parseString(KcaUtils.getStringPreferences(
                            getApplicationContext(), PREF_PACKAGE_ALLOW)).getAsJsonArray();
                    for (JsonElement pkg : allowed_apps) {
                        builder.addAllowedApplication(pkg.getAsString());
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if (Prefs.isPortMappingEnabled(mPrefs)) {
                PortMapping portMap = new PortMapping(this);
                Iterator<PortMapping.PortMap> it = portMap.iter();
                while (it.hasNext()) {
                    PortMapping.PortMap mapping = it.next();
                    addPortMapping(mapping.ipproto, mapping.orig_port, mapping.redirect_port, mapping.redirect_ip);
                }
            }

            try {
                mParcelFileDescriptor = builder.setSession(getString(R.string.app_vpn_name)).establish();
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                e.printStackTrace();
                // Utils.showToast(this, R.string.vpn_setup_failed);
                Toast.makeText(getApplicationContext(), "vpn_setup_failed", Toast.LENGTH_LONG).show();
                return abortStart();
            }
        }

        mConnUpdateThread = new Thread(this::connUpdateWork, "UpdateListener");
        mConnUpdateThread.start();

        if(mDumper != null) {
            mDumperThread = new Thread(this::dumpWork, "DumperThread");
            mDumperThread.start();
        }

        // Start the native capture thread
        mQueueFull = false;
        mCaptureThread = new Thread(this, "PacketCapture");
        mCaptureThread.start();

        // If the service is killed (e.g. due to low memory), then restart it with a NULL intent
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        Log.d(CaptureService.TAG, "onRevoke");
        mRevoked = true;
        stopService();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        Log.d(CaptureService.TAG, "onDestroy");

        // Do not nullify INSTANCE to allow its settings and the connections register to be accessible
        // after the capture is stopped
        //INSTANCE = null;

        unregisterNetworkCallbacks();

        if(mCaptureThread != null)
            mCaptureThread.interrupt();
        if(mBlacklistsUpdateThread != null)
            mBlacklistsUpdateThread.interrupt();

        if(mNewAppsInstallReceiver != null) {
            unregisterReceiver(mNewAppsInstallReceiver);
            mNewAppsInstallReceiver = null;
        }

        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void registerNetworkCallbacks() {
        if(mNetworkCallback != null)
            return;

        String fallbackDns = Prefs.getDnsServerV4(mPrefs);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "onLost " + network);

                // If the network goes offline we roll back to the fallback DNS server to
                // avoid possibly using a private IP DNS server not reachable anymore
                if(network.getNetworkHandle() == mMonitoredNetwork) {
                    Log.i(TAG, "Main network " + network + " lost, using fallback DNS " + fallbackDns);
                    dns_server = fallbackDns;
                    mMonitoredNetwork = 0;
                    unregisterNetworkCallbacks();

                    // change native
                    setDnsServer(dns_server);
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                Log.d(TAG, "onLinkPropertiesChanged " + network);

                if(network.getNetworkHandle() == mMonitoredNetwork)
                    handleLinkProperties(linkProperties);
            }
        };

        try {
            Log.d(TAG, "registerNetworkCallback");
            cm.registerNetworkCallback(
                    new NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                    mNetworkCallback);
        } catch (SecurityException e) {
            // this is a bug in Android 11 - https://issuetracker.google.com/issues/175055271?pli=1
            e.printStackTrace();

            Log.w(TAG, "registerNetworkCallback failed, DNS server detection disabled");
            dns_server = fallbackDns;
            mNetworkCallback = null;
        }
    }

    private void unregisterNetworkCallbacks() {
        if(mNetworkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Service.CONNECTIVITY_SERVICE);

            try {
                Log.d(TAG, "unregisterNetworkCallback");
                cm.unregisterNetworkCallback(mNetworkCallback);
            } catch(IllegalArgumentException e) {
                Log.w(TAG, "unregisterNetworkCallback failed: " + e);
            }

            mNetworkCallback = null;
        }
    }

    private void handleLinkProperties(LinkProperties linkProperties) {
        if(linkProperties == null)
            return;

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            mPrivateDnsMode = Utils.getPrivateDnsMode(linkProperties);
            Log.i(TAG, "Private DNS: " + mPrivateDnsMode);

            if(mSettings.readFromPcap()) {
                mDnsEncrypted = false;
                setPrivateDnsBlocked(false);
            } else if(!mSettings.root_capture && mSettings.auto_block_private_dns) {
                mDnsEncrypted = mPrivateDnsMode.equals(Utils.PrivateDnsMode.STRICT);
                boolean opportunistic_mode = mPrivateDnsMode.equals(Utils.PrivateDnsMode.OPPORTUNISTIC);

                /* Private DNS can be in one of these modes:
                 *  1. Off
                 *  2. Automatic (default): also called "opportunistic", only use it if not blocked
                 *  3. Strict: private DNS is enforced, Internet unavailable if blocked. User must set a specific DNS server.
                 * When in opportunistic mode, PCAPdroid will block private DNS connections to force the use of plain-text
                 * DNS queries, which can be extracted by PCAPdroid. */
                if (mBlockPrivateDns != opportunistic_mode) {
                    mBlockPrivateDns = opportunistic_mode;
                    setPrivateDnsBlocked(mBlockPrivateDns);
                }
            } else {
                // in root capture we don't block private DNS requests in opportunistic mode
                mDnsEncrypted = !mPrivateDnsMode.equals(Utils.PrivateDnsMode.DISABLED);
                setPrivateDnsBlocked(false);
            }

            if(mDnsEncrypted && !mStrictDnsNoticeShown) {
                mStrictDnsNoticeShown = true;
                // Utils.showToastLong(this, R.string.private_dns_message_notice);
            }
        }
    }

    private void signalServicesTermination() {
        mPendingUpdates.offer(new Pair<>(null, null));
        stopPcapDump();
    }

    // NOTE: do not call this on the main thread, otherwise it will be an ANR
    private void stopAndJoinThreads() {
        signalServicesTermination();

        Log.d(TAG, "Joining threads...");

        while((mConnUpdateThread != null) && (mConnUpdateThread.isAlive())) {
            try {
                Log.d(TAG, "Joining conn update thread...");
                mConnUpdateThread.join();
            } catch (InterruptedException ignored) {
                mPendingUpdates.offer(new Pair<>(null, null));
            }
        }
        mConnUpdateThread = null;

        while((mDumperThread != null) && (mDumperThread.isAlive())) {
            try {
                Log.d(TAG, "Joining dumper thread...");
                mDumperThread.join();
            } catch (InterruptedException ignored) {
                stopPcapDump();
            }
        }
        mDumperThread = null;
        mDumper = null;

        if(mMitmReceiver != null) {
            try {
                mMitmReceiver.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMitmReceiver = null;
        }
    }

    /* Stops the running Service. The SERVICE_STATUS_STOPPED notification is sent asynchronously
     * when mCaptureThread terminates. */
    @SuppressWarnings("deprecation")
    public static void stopService() {
        CaptureService captureService = INSTANCE;
        Log.d(TAG, "stopService called (instance? " + (captureService != null) + ")");

        if(captureService == null)
            return;

        captureService.mStopping = true;
        stopPacketLoop();

        captureService.stopSelf();
    }

    /* Check if the VPN service was launched */
    public static boolean isServiceActive() {
        return((INSTANCE != null) &&
                (INSTANCE.mCaptureThread != null));
    }

    public static MitmReceiver.Status getMitmProxyStatus() {
        if((INSTANCE == null) || (INSTANCE.mMitmReceiver == null))
            return MitmReceiver.Status.NOT_STARTED;

        return INSTANCE.mMitmReceiver.getProxyStatus();
    }

    public static boolean isLowMemory() {
        return((INSTANCE != null) && (INSTANCE.mLowMemory));
    }

    public static boolean isAlwaysOnVPN() {
        return((INSTANCE != null) && INSTANCE.mIsAlwaysOnVPN);
    }

    public static boolean checkAlwaysOnVpnActivated() {
        CaptureService instance = INSTANCE;
        if (instance == null)
            return false;

        if (!instance.mIsAlwaysOnVPN && instance.isAlwaysOnVpnDetected()) {
            Log.i(TAG, "Always-on VPN was activated");
            instance.mIsAlwaysOnVPN = true;
            return true;
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean isLockdownVPN() {
        return ((INSTANCE != null) && INSTANCE.isLockdownEnabled());
    }

    private String getIfname(int ifidx) {
        if(ifidx <= 0)
            return "";

        String rv = mIfIndexToName.get(ifidx);
        if(rv != null)
            return rv;

        // Not found, try to retrieve it
        NetworkInterface iface = null;
        try {
            iface = NetworkInterface.getByIndex(ifidx);
        } catch (SocketException ignored) {}
        rv = (iface != null) ? iface.getName() : "";

        // store it even if not found, to avoid looking up it again
        mIfIndexToName.put(ifidx, rv);
        return rv;
    }

    public static Set<String> getAppFilter() {
        return((INSTANCE != null) ? INSTANCE.mSettings.app_filter : null);
    }

    public static Uri getPcapUri() {
        return ((INSTANCE != null) ? INSTANCE.mPcapUri : null);
    }

    public static String getPcapFname() {
        return ((INSTANCE != null) ? INSTANCE.mPcapFname : null);
    }

    public static boolean isUserDefinedPcapUri() {
        return (INSTANCE == null || !INSTANCE.mSettings.pcap_uri.isEmpty());
    }

    public static long getBytes() {
        return((INSTANCE != null) ? INSTANCE.last_bytes : 0);
    }

    public static String getCollectorAddress() {
        return((INSTANCE != null) ? INSTANCE.mSettings.collector_address : "");
    }

    public static int getCollectorPort() {
        return((INSTANCE != null) ? INSTANCE.mSettings.collector_port : 0);
    }

    public static int getHTTPServerPort() {
        return((INSTANCE != null) ? INSTANCE.mSettings.http_server_port : 0);
    }

    public static Prefs.DumpMode getDumpMode() {
        return((INSTANCE != null) ? INSTANCE.mSettings.dump_mode : Prefs.DumpMode.NONE);
    }

    public static String getDNSServer() {
        return((INSTANCE != null) ? INSTANCE.getDnsServer() : "");
    }

    public static boolean isDNSEncrypted() {
        return((INSTANCE != null) && INSTANCE.mDnsEncrypted);
    }

    public static @NonNull CaptureService requireInstance() {
        CaptureService inst = INSTANCE;
        assert(inst != null);
        return(inst);
    }

    public static @Nullable ConnectionsRegister getConnsRegister() {
        return((INSTANCE != null) ? INSTANCE.conn_reg : null);
    }

    public static @NonNull ConnectionsRegister requireConnsRegister() {
        ConnectionsRegister reg = getConnsRegister();

        assert(reg != null);

        return reg;
    }

    public static boolean isCapturingAsRoot() {
        return((INSTANCE != null) &&
                (INSTANCE.isRootCapture() == 1));
    }

    public static boolean isDecryptingTLS() {
        return((INSTANCE != null) &&
                (INSTANCE.isTlsDecryptionEnabled() == 1));
    }

    public static boolean isReadingFromPcapFile() {
        return((INSTANCE != null) &&
                (INSTANCE.isPcapFileCapture() == 1));
    }

    public static boolean isIPv6Enabled() {
        return((INSTANCE != null) &&
                (INSTANCE.getIPv6Enabled() == 1));
    }

    public static boolean isDecryptionListEnabled() {
        return(INSTANCE != null && (INSTANCE.mDecryptionList != null));
    }

    public static Prefs.PayloadMode getCurPayloadMode() {
        if(INSTANCE == null)
            return Prefs.PayloadMode.MINIMAL;

        return INSTANCE.mSettings.full_payload ? Prefs.PayloadMode.FULL : Prefs.PayloadMode.MINIMAL;
    }

    public static void requestBlacklistsUpdate() {
        if(INSTANCE != null) {
            INSTANCE.mBlacklistsUpdateRequested = true;

            // Wake the update thread to run the blacklist thread
            INSTANCE.mPendingUpdates.offer(new Pair<>(new ConnectionDescriptor[0], new ConnectionUpdate[0]));
        }
    }

    public static String getInterfaceName(int ifidx) {
        String ifname = null;

        if(INSTANCE != null)
            ifname = INSTANCE.getIfname(ifidx);
        return (ifname != null) ? ifname : "";
    }



    // Inside the mCaptureThread
    @Override
    public void run() {
        if(mParcelFileDescriptor != null) {
            int fd = mParcelFileDescriptor.getFd();
            int fd_setsize = getFdSetSize();

            if((fd > 0) && (fd < fd_setsize)) {
                Log.d(TAG, "VPN fd: " + fd + " - FD_SETSIZE: " + fd_setsize);
                runPacketLoop(fd, this, Build.VERSION.SDK_INT);

                // if always-on VPN is stopped, it's not an always-on anymore
                mIsAlwaysOnVPN = false;
            } else
                Log.e(TAG, "Invalid VPN fd: " + fd);
        }

        // Important: the fd must be closed to properly terminate the VPN
        if(mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mParcelFileDescriptor = null;
        }

        // NOTE: join the threads here instead in onDestroy to avoid ANR
        stopAndJoinThreads();

        stopService();

        mLock.lock();
        mCaptureThread = null;
        mCaptureStopped.signalAll();
        mLock.unlock();

        // Notify
        mHandler.post(() -> {
            updateServiceStatus(ServiceStatus.STOPPED);
        });
    }

    private void connUpdateWork() {
        while(true) {
            Pair<ConnectionDescriptor[], ConnectionUpdate[]> item;
            try {
                item = mPendingUpdates.take();
            } catch (InterruptedException e) {
                continue;
            }

            if(item.first == null) { // termination request
                Log.i(TAG, "Connection update thread exit requested");
                break;
            }

            ConnectionDescriptor[] new_conns = item.first;
            ConnectionUpdate[] conns_updates = item.second;

            if(!mLowMemory)
                checkAvailableHeap();

            // synchronize the conn_reg to ensure that newConnections and connectionsUpdates run atomically
            // thus preventing the ConnectionsAdapter from interleaving other operations
            synchronized (conn_reg) {
                if(new_conns.length > 0)
                    conn_reg.newConnections(new_conns);

                if(conns_updates.length > 0)
                    conn_reg.connectionsUpdates(conns_updates);
            }
        }
    }

    private void dumpWork() {
        while(true) {
            byte[] data;
            try {
                data = mDumpQueue.take();
            } catch (InterruptedException e) {
                continue;
            }

            if(data.length == 0) // termination request
                break;

            try {
                mDumper.dumpData(data);
            } catch (IOException e) {
                // Stop the capture
                e.printStackTrace();
                reportError(e.getLocalizedMessage());
                mHandler.post(CaptureService::stopPacketLoop);
                break;
            }
        }

        try {
            mDumper.stopDumper();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAvailableHeap() {
        // This does not account per-app jvm limits
        long availableHeap = Utils.getAvailableHeap();

        if(availableHeap <= Utils.LOW_HEAP_THRESHOLD) {
            Log.w(TAG, "Detected low HEAP memory: " + Utils.formatBytes(availableHeap));
            handleLowMemory();
        }
    }

    // NOTE: this is only called on low system memory (e.g. obtained via getMemoryInfo). The app
    // may still run out of heap memory, whose monitoring requires polling (see checkAvailableHeap)
    @Override
    @SuppressWarnings("deprecation")
    public void onTrimMemory(int level) {
        // NOTE: most trim levels are not available anymore since API 34
        String lvlStr = Utils.trimlvl2str(level);
        boolean lowMemory = (level != TRIM_MEMORY_UI_HIDDEN) && (level >= TRIM_MEMORY_RUNNING_LOW);
        boolean critical = lowMemory && (level >= TRIM_MEMORY_COMPLETE);

        Log.d(TAG, "onTrimMemory: " + lvlStr + " - low=" + lowMemory + ", critical=" + critical);

        if(critical && !mLowMemory)
            handleLowMemory();
    }

    private void handleLowMemory() {
        Log.w(TAG, "handleLowMemory called");
        mLowMemory = true;
        boolean fullPayload = getCurPayloadMode() == Prefs.PayloadMode.FULL;

        if(fullPayload) {
            Log.w(TAG, "Disabling full payload");

            // Disable full payload for new connections
            mSettings.full_payload = false;
            setPayloadMode(Prefs.PayloadMode.NONE.ordinal());

            // Release memory for existing connections
            if(conn_reg != null) {
                conn_reg.releasePayloadMemory();

                // *possibly* call the gc
                System.gc();

                Log.i(TAG, "Memory stats full payload release:\n" + Utils.getMemoryStats(this));
            }

        } else {
            // TODO lower memory consumption (e.g. reduce connections register size)
            Log.w(TAG, "low memory detected, expect crashes");
        }
    }

    /* The following methods are called from native code */

    public String getVpnIPv4() {
        return(vpn_ipv4);
    }

    public String getVpnDns() {
        return(vpn_dns);
    }

    public String getDnsServer() {
        return(dns_server);
    }

    public String getIpv6DnsServer() { return(Prefs.getDnsServerV6(mPrefs)); }

    public int getSocks5Enabled() { return mSocks5Enabled ? 1 : 0; }

    public String getSocks5ProxyAddress() {  return(mSocks5Address); }

    public int getSocks5ProxyPort() {  return(mSocks5Port);  }

    public String getSocks5ProxyAuth() {  return(mSocks5Auth);  }

    public int getIPv4Enabled() { return((mSettings.ip_mode != Prefs.IpMode.IPV6_ONLY) ? 1 : 0); }

    public int getIPv6Enabled() { return((mSettings.ip_mode != Prefs.IpMode.IPV4_ONLY) ? 1 : 0); }

    public int isVpnCapture() { return (isRootCapture() | isPcapFileCapture()) == 1 ? 0 : 1; }

    public int isRootCapture() { return(mSettings.root_capture ? 1 : 0); }

    public int isPcapFileCapture() { return(mSettings.readFromPcap() ? 1 : 0); }

    public int isTlsDecryptionEnabled() { return mSettings.tls_decryption ? 1 : 0; }

    public int malwareDetectionEnabled() { return(mMalwareDetectionEnabled ? 1 : 0); }

    public int firewallEnabled() { return(mFirewallEnabled ? 1 : 0); }

    public int dumpExtensionsEnabled() { return(mSettings.dump_extensions ? 1 : 0); }

    public int isPcapngEnabled() { return(mSettings.pcapng_format ? 1 : 0); }

    public int[] getAppFilterUids() { return(mAppFilterUids); }

    public int getMitmAddonUid() {
        return MitmAddon.getUid(this);
    }

    public String getCaptureInterface() { return(mSettings.capture_interface); }

    public int getSnaplen() {  return mSettings.snaplen; }

    public int getMaxPktsPerFlow() {  return mSettings.max_pkts_per_flow; }

    public int getMaxDumpSize() {  return mSettings.max_dump_size; }

    public int getPayloadMode() { return getCurPayloadMode().ordinal(); }

    public int getVpnMTU()      { return VPN_MTU; }

    public int getBlockQuickMode() { return mSettings.block_quic_mode.ordinal(); }

    // returns 1 if dumpPcapData should be called
    public int pcapDumpEnabled() {
        return((mSettings.dump_mode != Prefs.DumpMode.NONE) ? 1 : 0);
    }

    public String getPcapDumperBpf() { return((mDumper != null) ? mDumper.getBpf() : ""); }

    @Override
    public boolean protect(int socket) {
        // Do not call protect in root mode
        if(mSettings.root_capture)
            return true;

        return super.protect(socket);
    }

    // from NetGuard
    @TargetApi(Build.VERSION_CODES.Q)
    public int getUidQ(int protocol, String saddr, int sport, String daddr, int dport) {
        if (protocol != 6 /* TCP */ && protocol != 17 /* UDP */)
            return Utils.UID_UNKNOWN;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null)
            return Utils.UID_UNKNOWN;

        InetSocketAddress local = new InetSocketAddress(saddr, sport);
        InetSocketAddress remote = new InetSocketAddress(daddr, dport);

        Log.d(TAG, "Get uid local=" + local + " remote=" + remote);
        return cm.getConnectionOwnerUid(protocol, local, remote);
    }

    public void updateConnections(ConnectionDescriptor[] new_conns, ConnectionUpdate[] conns_updates) {
        if(mQueueFull)
            // if the queue is full, stop receiving updates to avoid inconsistent incr_ids
            return;

        // Put the update into a queue to avoid performing much work on the capture thread.
        // This will be processed by mConnUpdateThread.
        if(!mPendingUpdates.offer(new Pair<>(new_conns, conns_updates))) {
            Log.e(TAG, "The updates queue is full, this should never happen!");
            mQueueFull = true;
            mHandler.post(CaptureService::stopPacketLoop);
        }
    }

    // called from native
    public void sendStatsDump(CaptureStats stats) {
        //Log.d(TAG, "sendStatsDump");

        last_bytes = stats.bytes_sent + stats.bytes_rcvd;
        last_connections = stats.tot_conns;

        // notify the observers
        lastStats.postValue(stats);
    }

    // called from native
    private void sendServiceStatus(String cur_status) {
        updateServiceStatus(cur_status.equals("started") ? ServiceStatus.STARTED : ServiceStatus.STOPPED);
    }

    private void updateServiceStatus(ServiceStatus cur_status) {
        // notify the observers
        // NOTE: new subscribers will receive the STOPPED status right after their registration
        serviceStatus.postValue(cur_status);

        if(cur_status == ServiceStatus.STARTED) {
            if(mDecryptionList != null) reloadDecryptionList();
        } else if (cur_status == ServiceStatus.STOPPED) {
            if (mRevoked && Prefs.restartOnDisconnect(mPrefs) && !mIsAlwaysOnVPN && (isVpnCapture() == 1)) {
                /*
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Log.i(TAG, "VPN disconnected, starting reconnect service");

                    final Intent intent = new Intent(this, VpnReconnectService.class);
                    ContextCompat.startForegroundService(this, intent);
                }*/
            }
        }
    }

    // NOTE: to be invoked only by the native code
    public String getApplicationByUid(int uid) {
        AppDescriptor dsc = mNativeAppsResolver.getAppByUid(uid, 0);

        if(dsc == null)
            return "";

        return dsc.getName();
    }

    public String getPackageNameByUid(int uid) {
        AppDescriptor dsc = mNativeAppsResolver.getAppByUid(uid, 0);

        if(dsc == null)
            return "";

        return dsc.getPackageName();
    }

    public void loadUidMapping(int uid, String package_name, String app_name) {
        if (uid < 0)
            return;

        AppDescriptor dsc = mNativeAppsResolver.getAppByUid(uid, 0);

        if ((dsc == null) || !dsc.getPackageName().equals(package_name)) {
            // This uid corresponds to a different app than the one on the Pcapng
            AppsResolver.addMappedApp(uid, package_name, app_name);
        }
    }

    // dummy
    public void notifyBlacklistsLoaded(Blacklists.NativeBlacklistStatus[] loaded_blacklists) {
        // this is invoked from the packet capture thread. Use the handler to save time.
    }

    // dummy
    public BlacklistDescriptor[] getBlacklistsInfo() {
        BlacklistDescriptor[] blsinfo = new BlacklistDescriptor[0];
        return blsinfo;
    }

    // dummy
    public String getCountryCode(String host) {
        return "";
    }


    /* Exports a PCAP data chunk */
    public void dumpPcapData(byte[] data) {
        if((mDumper != null) && (data.length > 0)) {
            while(true) {
                try {
                    // wait until the queue has space to insert the data. If the queue is full, we
                    // will experience slow-downs/drops but this is expected
                    mDumpQueue.put(data);
                    break;
                } catch (InterruptedException e) {
                    // retry
                }
            }
        }
    }

    public void stopPcapDump() {
        if((mDumpQueue != null) && (mDumperThread != null) && (mDumperThread.isAlive()))
            mDumpQueue.offer(new byte[0]);
    }

    public void reportError(String msg) {
        HAS_ERROR = true;

        mHandler.post(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }

    public String getWorkingDir() {
        return getCacheDir().getAbsolutePath();
    }
    public String getPersistentDir() { return getFilesDir().getAbsolutePath(); }

    public String getLibprogPath(String prog_name) {
        // executable binaries are stored into the /lib folder of the app
        String dir = getApplicationInfo().nativeLibraryDir;
        return(dir + "/lib" + prog_name + ".so");
    }

    public static void reloadDecryptionList() {
        if((INSTANCE == null) || (INSTANCE.mDecryptionList == null))
            return;

        Log.i(TAG, "reloading TLS decryption whitelist");
        reloadDecryptionList(INSTANCE.mDecryptionList.toListDescriptor());
    }

    public static @NonNull CaptureStats getStats() {
        CaptureStats stats = lastStats.getValue();
        return((stats != null) ? stats : new CaptureStats());
    }

    public static void observeStats(LifecycleOwner lifecycleOwner, Observer<CaptureStats> observer) {
        lastStats.observe(lifecycleOwner, observer);
    }

    public static void observeStatus(LifecycleOwner lifecycleOwner, Observer<ServiceStatus> observer) {
        serviceStatus.observe(lifecycleOwner, observer);
    }

    public static void waitForCaptureStop() {
        if(INSTANCE == null)
            return;

        Log.d(TAG, "waitForCaptureStop " + Thread.currentThread().getName());
        INSTANCE.mLock.lock();
        try {
            while(INSTANCE.mCaptureThread != null) {
                try {
                    INSTANCE.mCaptureStopped.await();
                } catch (InterruptedException ignored) {}
            }
        } finally {
            INSTANCE.mLock.unlock();
        }
        Log.d(TAG, "waitForCaptureStop done " + Thread.currentThread().getName());
    }

    public static boolean hasError() {
        return HAS_ERROR;
    }

    public static @Nullable Utils.PrivateDnsMode getPrivateDnsMode() {
        return isServiceActive() ? INSTANCE.mPrivateDnsMode : null;
    }

    public static native int initLogger(String path, int level);
    public static native int writeLog(int logger, int lvl, String message);
    private static native void initPlatformInfo(String appver, String device, String os);
    private static native void runPacketLoop(int fd, CaptureService vpn, int sdk);
    private static native void stopPacketLoop();
    private static native int getFdSetSize();
    private static native void setPrivateDnsBlocked(boolean to_block);
    private static native void setDnsServer(String server);
    private static native void addPortMapping(int ipproto, int orig_port, int redirect_port, String redirect_ip);
    private static native void reloadBlacklists();
    private static native boolean reloadBlocklist(MatchList.ListDescriptor blocklist);
    private static native boolean reloadFirewallWhitelist(MatchList.ListDescriptor whitelist);
    private static native boolean reloadMalwareWhitelist(MatchList.ListDescriptor whitelist);
    private static native boolean reloadDecryptionList(MatchList.ListDescriptor whitelist);
    public static native void askStatsDump();
    public static native byte[] getPcapHeader();
    public static native void nativeSetFirewallEnabled(boolean enabled);
    public static native int getNumCheckedMalwareConnections();
    public static native int getNumCheckedFirewallConnections();
    public static native int rootCmd(String prog, String args);
    public static native void setPayloadMode(int mode);
    public static native List<String> getL7Protocols();
    public static native void dumpMasterSecret(byte[] secret);
    public static native boolean hasSeenDumpExtensions();
}
