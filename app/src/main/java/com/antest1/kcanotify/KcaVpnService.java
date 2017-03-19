package com.antest1.kcanotify;


import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.os.Process;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import eu.faircode.netguard.Allowed;
import eu.faircode.netguard.IPUtil;
import eu.faircode.netguard.Packet;
import eu.faircode.netguard.ResourceRecord;
import eu.faircode.netguard.Rule;
import eu.faircode.netguard.Usage;
import eu.faircode.netguard.Util;

public class KcaVpnService extends VpnService {
    private final static String TAG = "KCAV";

    Resources resources;

    private KcaVpnService.Builder last_builder = null;
    private static ParcelFileDescriptor vpn = null;

    private enum State {none, waiting, enforcing, stats}

    private State state = State.none;

    private native void jni_init();

    private native void jni_start(int tun, boolean fwd53, int loglevel);

    private native void jni_stop(int tun, boolean clr);

    private native int jni_get_mtu();

    private native void jni_socks5(String addr, int port, String username, String password);

    private native void jni_done();

    private volatile Looper commandLooper;
    private volatile Looper logLooper;
    private volatile Looper statsLooper;
    private volatile CommandHandler commandHandler;

    private static final int MSG_SERVICE_INTENT = 0;

    public static final String EXTRA_COMMAND = "Command";
    private static final String EXTRA_REASON = "Reason";

    private static volatile PowerManager.WakeLock wlInstance = null;
    public static boolean is_on = false;

    private boolean registeredInteractiveState = false;
    private boolean phone_state = false;
    private boolean last_interactive = false;
    private static final String ACTION_SCREEN_OFF_DELAYED = "com.antest1.kcanotify.SCREEN_OFF_DELAYED";

    public enum Command {run, start, reload, stop, stats, set, householding, watchdog}

    public static boolean checkOn() {
        return is_on;
    }
    /*
    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (wlInstance == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wlInstance = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getString(R.string.app_name) + " wakelock");
            wlInstance.setReferenceCounted(true);
        }
        return wlInstance;
    }
    */
    private final class CommandHandler extends Handler {
        public int queue = 0;

        public CommandHandler(Looper looper) {
            super(looper);
        }

        public void queue(Intent intent) {
            synchronized (this) {
                queue++;
                //reportQueueSize();
            }
            Message msg = commandHandler.obtainMessage();
            msg.obj = intent;
            msg.what = MSG_SERVICE_INTENT;
            commandHandler.sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_SERVICE_INTENT:
                        handleIntent((Intent) msg.obj);
                        break;
                    default:
                        Log.e(TAG, "Unknown command message=" + msg.what);
                }
            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            } finally {
                synchronized (this) {
                    queue--;
                    //reportQueueSize();
                }
                /*
                try {
                    PowerManager.WakeLock wl = getLock(KcaVpnService.this);
                    if (wl.isHeld())
                        wl.release();
                    else
                        Log.w(TAG, "Wakelock under-locked");
                    Log.i(TAG, "Messages=" + hasMessages(0) + " wakelock=" + wlInstance.isHeld());
                } catch (Throwable ex) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                }
                */
            }
        }

        private void handleIntent(Intent intent) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(KcaVpnService.this);

            Command cmd = (Command) intent.getSerializableExtra(EXTRA_COMMAND);
            String reason = intent.getStringExtra(EXTRA_REASON);
            Log.e(TAG, cmd.toString() + " " + reason);

            if (prefs.getBoolean("screen_on", true)) {
                Log.i(TAG, "Started listening for interactive state changes");
                if (prefs.getBoolean("screen_on", true)) {
                    last_interactive = Util.isInteractive(KcaVpnService.this);
                    IntentFilter ifInteractive = new IntentFilter();
                    ifInteractive.addAction(Intent.ACTION_SCREEN_ON);
                    ifInteractive.addAction(Intent.ACTION_SCREEN_OFF);
                    ifInteractive.addAction(ACTION_SCREEN_OFF_DELAYED);
                    registerReceiver(interactiveStateReceiver, ifInteractive);
                    registeredInteractiveState = true;
                }
            } else {
                Log.i(TAG, "Stopped listening for interactive state changes");
                if (registeredInteractiveState) {
                    unregisterReceiver(interactiveStateReceiver);
                    registeredInteractiveState = false;
                }
            }

            try {
                switch (cmd) {
                    case start:
                        start();
                        is_on = true;
                        break;

                    case reload:
                        reload();
                        is_on = true;
                        break;

                    case stop:
                        stop();
                        is_on = false;
                        break;


                    default:
                        Log.e(TAG, "Unknown command=" + cmd);
                }

            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));

                if (cmd == Command.start || cmd == Command.reload) {
                    if (VpnService.prepare(KcaVpnService.this) == null) {
                        Log.w(TAG, "VPN not prepared connected=");
                        // Retried on connectivity change
                    } else {
                        //
                        prefs.edit().putBoolean("enabled", false).apply();
                    }
                } else {
                    Log.w(TAG, ex.toString());
                }
            }
        }

        private void start() {
            if (vpn == null) {
                last_builder = getBuilder(null, null);
                vpn = startVPN(last_builder);
                if (vpn == null)
                    throw new IllegalStateException("Failed");

                startNative(vpn);
            } else {
                Log.e("KCA", "vpn is not null");
            }
        }

        private void reload() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(KcaVpnService.this);

            KcaVpnService.Builder builder = getBuilder(null, null);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                last_builder = builder;
                Log.i(TAG, "Legacy restart");

                if (vpn != null) {
                    stopNative(vpn, false);
                    stopVPN(vpn);
                    vpn = null;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
                vpn = startVPN(last_builder);

            } else {
                if (vpn != null && builder.equals(last_builder)) {
                    Log.i(TAG, "Native restart");
                    stopNative(vpn, false);

                } else {
                    last_builder = builder;
                    Log.i(TAG, "VPN restart");

                    // Attempt seamless handover
                    ParcelFileDescriptor prev = vpn;
                    vpn = startVPN(builder);

                    if (prev != null && vpn == null) {
                        Log.w(TAG, "Handover failed");
                        stopNative(prev, false);
                        stopVPN(prev);
                        prev = null;
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ignored) {
                        }
                        vpn = startVPN(last_builder);
                        if (vpn == null)
                            throw new IllegalStateException("Handover failed");
                    }

                    if (prev != null) {
                        stopNative(prev, false);
                        stopVPN(prev);
                    }
                }
            }
            if (vpn == null)
                throw new IllegalStateException();

            startNative(vpn);
        }

        private void stop() {
            if (vpn != null) {
                stopNative(vpn, true);
                stopVPN(vpn);
                vpn = null;
                unprepare();
            }
        }
    }

    @Override
    public void onCreate() {
        jni_init();
        super.onCreate();

        HandlerThread commandThread = new HandlerThread(getString(R.string.app_name) + " command");
        commandThread.start();
        commandLooper = commandThread.getLooper();
        commandHandler = new CommandHandler(commandLooper);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Received " + intent);

        //getLock(this).acquire();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = prefs.getBoolean("enabled", false);

        if (intent == null) {
            Log.i(TAG, "Restart");

            // Recreate intent
            intent = new Intent(this, KcaVpnService.class);
            intent.putExtra(EXTRA_COMMAND, enabled ? Command.start : Command.stop);
        }

        Command cmd = (Command) intent.getSerializableExtra(EXTRA_COMMAND);
        Log.e("KCA", cmd.toString());
        if (cmd == null)
            intent.putExtra(EXTRA_COMMAND, enabled ? Command.start : Command.stop);
        String reason = intent.getStringExtra(EXTRA_REASON);
        Log.i(TAG, "Start intent=" + intent + " command=" + cmd + " reason=" + reason +
                " vpn=" + (vpn != null) + " user=" + (Process.myUid() / 100000));

        commandHandler.queue(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (registeredInteractiveState) {
            unregisterReceiver(interactiveStateReceiver);
            registeredInteractiveState = false;
        }

        try {
            if (vpn != null) {
                stopNative(vpn, true);
                stopVPN(vpn);
                vpn = null;
                unprepare();
            }
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
        jni_done();
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        Log.i(TAG, "Revoke");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!prefs.getBoolean("svcenabled", false)) {
            prefs.edit().putBoolean("enabled", false).apply();
            super.onRevoke();
        }
    }

    private Builder getBuilder(List<Rule> listAllowed, List<Rule> listRule) {
        SharedPreferences prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);
        boolean subnet = prefs.getBoolean("subnet", false);
        boolean tethering = prefs.getBoolean("tethering", false);
        boolean lan = prefs.getBoolean("lan", false);
        boolean ip6 = prefs.getBoolean("ip6", true);
        boolean filter = prefs.getBoolean("filter", false);
        boolean system = prefs.getBoolean("manage_system", false);

        // Build VPN service
        Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));

        // VPN address
        String vpn4 = prefs.getString("vpn4", "10.1.10.1");
        Log.i(TAG, "vpn4=" + vpn4);
        builder.addAddress(vpn4, 32);
        if (ip6) {
            String vpn6 = prefs.getString("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1");
            Log.i(TAG, "vpn6=" + vpn6);
            builder.addAddress(vpn6, 128);
        }

        // DNS address
        if (filter)
            for (InetAddress dns : getDns(KcaVpnService.this)) {
                if (ip6 || dns instanceof Inet4Address) {
                    Log.i(TAG, "dns=" + dns);
                    builder.addDnsServer(dns);
                }
            }

        // Subnet routing
        if (subnet) {
            // Exclude IP ranges
            List<IPUtil.CIDR> listExclude = new ArrayList<>();
            listExclude.add(new IPUtil.CIDR("127.0.0.0", 8)); // localhost

            if (tethering) {
                // USB Tethering 192.168.42.x
                // Wi-Fi Tethering 192.168.43.x
                listExclude.add(new IPUtil.CIDR("192.168.42.0", 23));
            }

            if (lan) {
                try {
                    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                    while (nis.hasMoreElements()) {
                        NetworkInterface ni = nis.nextElement();
                        if (ni != null && ni.isUp() && !ni.isLoopback() &&
                                ni.getName() != null && !ni.getName().startsWith("tun"))
                            for (InterfaceAddress ia : ni.getInterfaceAddresses())
                                if (ia.getAddress() instanceof Inet4Address) {
                                    IPUtil.CIDR local = new IPUtil.CIDR(ia.getAddress(), ia.getNetworkPrefixLength());
                                    Log.i(TAG, "Excluding " + ni.getName() + " " + local);
                                    listExclude.add(local);
                                }
                    }
                } catch (SocketException ex) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                }
            }

            Configuration config = getResources().getConfiguration();
            if (config.mcc == 310 && config.mnc == 260) {
                // T-Mobile Wi-Fi calling
                listExclude.add(new IPUtil.CIDR("66.94.2.0", 24));
                listExclude.add(new IPUtil.CIDR("66.94.6.0", 23));
                listExclude.add(new IPUtil.CIDR("66.94.8.0", 22));
                listExclude.add(new IPUtil.CIDR("208.54.0.0", 16));
            }
            listExclude.add(new IPUtil.CIDR("224.0.0.0", 3)); // broadcast

            Collections.sort(listExclude);

            try {
                InetAddress start = InetAddress.getByName("0.0.0.0");
                for (IPUtil.CIDR exclude : listExclude) {
                    Log.i(TAG, "Exclude " + exclude.getStart().getHostAddress() + "..." + exclude.getEnd().getHostAddress());
                    for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart())))
                        try {
                            builder.addRoute(include.address, include.prefix);
                        } catch (Throwable ex) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                        }
                    start = IPUtil.plus1(exclude.getEnd());
                }
                for (IPUtil.CIDR include : IPUtil.toCIDR("224.0.0.0", "255.255.255.255"))
                    try {
                        builder.addRoute(include.address, include.prefix);
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
            } catch (UnknownHostException ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            }
        } else {
            String addresses = prefs.getString("bypass_address", "");
            if (!addresses.equals("")) {
                List<IPUtil.CIDR> listExclude = new ArrayList<>();
                for (String cidr : addresses.split(",")) {
                    String[] cidr_split = cidr.trim().split("/");
                    listExclude.add(new IPUtil.CIDR(cidr_split[0], Integer.parseInt(cidr_split[1])));
                }

                Collections.sort(listExclude);

                try {
                    InetAddress start = InetAddress.getByName("0.0.0.0");

                    for (IPUtil.CIDR exclude : listExclude) {
                        for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart())))
                            try {
                                builder.addRoute(include.address, include.prefix);
                            } catch (Throwable ex) {
                                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                            }
                        start = IPUtil.plus1(exclude.getEnd());
                    }

                    for (IPUtil.CIDR include : IPUtil.toCIDR(start.getHostAddress(), "255.255.255.255"))
                        try {
                            builder.addRoute(include.address, include.prefix);
                        } catch (Throwable ex) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                        }
                } catch (UnknownHostException ex) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                }
            }
            else builder.addRoute("0.0.0.0", 0);
        }

        Log.i(TAG, "IPv6=" + ip6);
        if (ip6)
            builder.addRoute("0:0:0:0:0:0:0:0", 0);

        // MTU
        int mtu = jni_get_mtu();
        Log.i(TAG, "MTU=" + mtu);
        builder.setMtu(mtu);

        /*
        // Add list of allowed applications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            if (last_connected && !filter)
                for (Rule rule : listAllowed)
                    try {
                        builder.addDisallowedApplication(rule.info.packageName);
                    } catch (PackageManager.NameNotFoundException ex) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
            else if (filter)
                for (Rule rule : listRule)
                    if (!rule.apply || (!system && rule.system))
                        try {
                            Log.i(TAG, "Not routing " + rule.info.packageName);
                            builder.addDisallowedApplication(rule.info.packageName);
                        } catch (PackageManager.NameNotFoundException ex) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                        }
        */
        // Build configure intent
        //Intent configure = new Intent(this, MainActivity.class);
        //PendingIntent pi = PendingIntent.getActivity(this, 0, configure, PendingIntent.FLAG_UPDATE_CURRENT);
        //builder.setConfigureIntent(pi);


        return builder;
    }


    public static List<InetAddress> getDns(Context context) {
        List<InetAddress> listDns = new ArrayList<>();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<String> sysDns = Util.getDefaultDNS(context);
        String vpnDns1 = prefs.getString("dns", null);
        String vpnDns2 = prefs.getString("dns2", null);
        Log.i(TAG, "DNS system=" + TextUtils.join(",", sysDns) + " VPN1=" + vpnDns1 + " VPN2=" + vpnDns2);

        if (vpnDns1 != null)
            try {
                InetAddress dns = InetAddress.getByName(vpnDns1);
                if (!(dns.isLoopbackAddress() || dns.isAnyLocalAddress()))
                    listDns.add(dns);
            } catch (Throwable ignored) {
            }

        if (vpnDns2 != null)
            try {
                InetAddress dns = InetAddress.getByName(vpnDns2);
                if (!(dns.isLoopbackAddress() || dns.isAnyLocalAddress()))
                    listDns.add(dns);
            } catch (Throwable ignored) {
            }

        if (listDns.size() <= 1)
            for (String def_dns : sysDns)
                try {
                    InetAddress ddns = InetAddress.getByName(def_dns);
                    if (!listDns.contains(ddns) &&
                            !(ddns.isLoopbackAddress() || ddns.isAnyLocalAddress()))
                        listDns.add(ddns);
                } catch (Throwable ignored) {
                }

        return listDns;
    }


    private ParcelFileDescriptor startVPN(Builder builder) throws SecurityException {
        try {
            return builder.establish();
        } catch (SecurityException ex) {
            throw ex;
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            return null;
        }
    }

    private void startNative(ParcelFileDescriptor vpn) {
        // Prepare rules
        int prio = Log.ERROR;
        SharedPreferences prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);
        String addr = prefs.getString("socks5_address", "");
        String portNum = prefs.getString("socks5_port", "0");
        int port = 0;
        if (!portNum.equals(""))
            port = Integer.parseInt(portNum);
        if (addr.equals("") || port == 0)
            jni_socks5("", 0, "", "");
        else
            jni_socks5(addr, port, "", "");
        jni_start(vpn.getFd(), true, prio);
    }

    private void stopVPN(ParcelFileDescriptor pfd) {
        Log.i(TAG, "Stopping");
        try {
            pfd.close();
        } catch (IOException ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
    }

    private void stopNative(ParcelFileDescriptor vpn, boolean clear) {
        Log.i(TAG, "Stop native clear=" + clear);
        try {
            jni_stop(vpn.getFd(), clear);
        } catch (Throwable ex) {
            // File descriptor might be closed
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            jni_stop(-1, clear);
        }
    }

    private void unprepare() {

    }

    // Called from native code
    private void nativeExit(String reason) {
        Log.w(TAG, "Native exit reason=" + reason);
        if (reason != null) {
            Log.e(TAG, reason);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("enabled", false).apply();
        }
    }

    // Called from native code
    private void nativeError(int error, String message) {
        Log.w(TAG, "Native error " + error + ": " + message);
    }

    // Called from native code
    private void logPacket(Packet packet) {
        Log.e("KCAV", packet.data);
        /*
        Message msg = logHandler.obtainMessage();
        msg.obj = packet;
        msg.what = MSG_PACKET;
        msg.arg1 = (last_connected ? (last_metered ? 2 : 1) : 0);
        msg.arg2 = (last_interactive ? 1 : 0);
        logHandler.sendMessage(msg);
        */
    }

    // Called from native code
    private void dnsResolved(ResourceRecord rr) {
        /*
        if (DatabaseHelper.getInstance(KcaVpnService.this).insertDns(rr)) {
            Log.i(TAG, "New IP " + rr);
            prepareUidIPFilters(rr.QName);
        }*/
    }

    // Called from native code
    private boolean isDomainBlocked(String name) {
        /*
        lock.readLock().lock();
        boolean blocked = (mapHostsBlocked.containsKey(name) && mapHostsBlocked.get(name));
        lock.readLock().unlock();
        return blocked;
        */
        return false;
    }

    private boolean isSupported(int protocol) {
        return (protocol == 1 /* ICMPv4 */ ||
                protocol == 59 /* ICMPv6 */ ||
                protocol == 6 /* TCP */ ||
                protocol == 17 /* UDP */);
    }

    // Called from native code
    private Allowed isAddressAllowed(Packet packet) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //lock.readLock().lock();

        Allowed allowed = new Allowed();
        return allowed;
    }

    // Called from native code
    private void accountUsage(Usage usage) {
        /*
        Message msg = logHandler.obtainMessage();
        msg.obj = usage;
        msg.what = MSG_USAGE;
        logHandler.sendMessage(msg);
        */
    }

    private BroadcastReceiver interactiveStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received " + intent);
            Util.logExtras(intent);

            // Check if rules needs to be reloaded

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(KcaVpnService.this);
            int delay = Integer.parseInt(prefs.getString("screen_delay", "0"));
            boolean interactive = Intent.ACTION_SCREEN_ON.equals(intent.getAction());

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_SCREEN_OFF_DELAYED), PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(pi);

            if (interactive || delay == 0) {
                last_interactive = interactive;
                reload("interactive state changed", KcaVpnService.this);
            } else {
                if (ACTION_SCREEN_OFF_DELAYED.equals(intent.getAction())) {
                    last_interactive = interactive;
                    reload("interactive state changed", KcaVpnService.this);
                } else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                        am.set(AlarmManager.RTC_WAKEUP, new Date().getTime() + delay * 60 * 1000L, pi);
                    else
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, new Date().getTime() + delay * 60 * 1000L, pi);
                }
            }
        }
    };

    private BroadcastReceiver idleStateReceiver = new BroadcastReceiver() {
        @Override
        @TargetApi(Build.VERSION_CODES.M)
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received " + intent);
            Util.logExtras(intent);

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            Log.i(TAG, "device idle=" + pm.isDeviceIdleMode());

            // Reload rules when coming from idle mode
            if (!pm.isDeviceIdleMode())
                reload("idle state changed", KcaVpnService.this);
        }
    };

    private BroadcastReceiver connectivityChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Filter VPN connectivity changes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY);
                if (networkType == ConnectivityManager.TYPE_VPN)
                    return;
            }

            // Reload rules
            Log.i(TAG, "Received " + intent);
            Util.logExtras(intent);
            reload("connectivity changed", KcaVpnService.this);
        }
    };

    public static void start(String reason, Context context) {
        Intent intent = new Intent(context, KcaVpnService.class);
        intent.putExtra(EXTRA_COMMAND, Command.start);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void stop(String reason, Context context) {
        Intent intent = new Intent(context, KcaVpnService.class);
        intent.putExtra(EXTRA_COMMAND, Command.stop);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void reload(String reason, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("enabled", false)) {
            Intent intent = new Intent(context, KcaVpnService.class);
            intent.putExtra(EXTRA_COMMAND, Command.reload);
            intent.putExtra(EXTRA_REASON, reason);
            context.startService(intent);
        }
    }

    static {
        System.loadLibrary("netguard");
    }
}
