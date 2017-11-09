package eu.faircode.netguard;

/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2015-2017 by Marcel Bokhorst (M66B)
*/

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.net.ConnectivityManagerCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.antest1.kcanotify.KcaUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Util {
    private static final int NETWORK_TYPE_TD_SCDMA = 17;
    private static final int NETWORK_TYPE_IWLAN = 18;
    private static final String TAG = "NetGuard.Util";

    private static native String jni_getprop(String name);

    private static native boolean is_numeric_address(String ip);

    static {
        System.loadLibrary("netguard");
    }

    public static String getSelfVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException ex) {
            return ex.toString();
        }
    }

    public static int getSelfVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            return -1;
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = (cm == null ? null : cm.getActiveNetworkInfo());
        return (ni != null && ni.isConnected());
    }

    public static boolean isWifiActive(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = (cm == null ? null : cm.getActiveNetworkInfo());
        return (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isMeteredNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm != null && ConnectivityManagerCompat.isActiveNetworkMetered(cm));
    }

    public static String getWifiSSID(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ssid = (wm == null ? null : wm.getConnectionInfo().getSSID());
        return (ssid == null ? "NULL" : ssid);
    }

    public static int getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = (cm == null ? null : cm.getActiveNetworkInfo());
        return (ni == null ? TelephonyManager.NETWORK_TYPE_UNKNOWN : ni.getSubtype());
    }

    public static String getNetworkGeneration(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE ? getNetworkGeneration(ni.getSubtype()) : null);
    }

    public static boolean isRoaming(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = (cm == null ? null : cm.getActiveNetworkInfo());
        return (ni != null && ni.isRoaming());
    }

    public static boolean isInternational(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return (tm != null && tm.getSimCountryIso() != null && !tm.getSimCountryIso().equals(tm.getNetworkCountryIso()));
    }

    public static String getNetworkGeneration(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";

            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case NETWORK_TYPE_TD_SCDMA:
                return "3G";

            case TelephonyManager.NETWORK_TYPE_LTE:
            case NETWORK_TYPE_IWLAN:
                return "4G";

            default:
                return "?G";
        }
    }

    public static String getNetworkTypeName(int networkType) {
        switch (networkType) {
            // 2G
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "IDEN";

            // 3G
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "EHRPD";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPAP";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case NETWORK_TYPE_TD_SCDMA:
                return "TD_SCDMA";

            // 4G
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case NETWORK_TYPE_IWLAN:
                return "IWLAN";

            default:
                return Integer.toString(networkType);
        }
    }

    public static String getPhoneTypeName(int phoneType) {
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_NONE:
                return "None";
            case TelephonyManager.PHONE_TYPE_GSM:
                return "GSM";
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.PHONE_TYPE_SIP:
                return "SIP";
            default:
                return "Unknown";
        }
    }

    public static boolean hasPhoneStatePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
        else
            return true;
    }

    public static List<String> getDefaultDNS(Context context) {
        String dns1 = jni_getprop("net.dns1");
        String dns2 = jni_getprop("net.dns2");
        List<String> listDns = new ArrayList<>();
        listDns.add(TextUtils.isEmpty(dns1) ? "8.8.8.8" : dns1);
        listDns.add(TextUtils.isEmpty(dns2) ? "8.8.4.4" : dns2);
        return listDns;
    }

    public static boolean isNumericAddress(String ip) {
        return is_numeric_address(ip);
    }

    public static boolean isInteractive(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH)
            return (pm != null && pm.isScreenOn());
        else
            return (pm != null && pm.isInteractive());
    }

    public static boolean isPackageInstalled(String packageName, Context context) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isSystem(int uid, Context context) {
        PackageManager pm = context.getPackageManager();
        String[] pkgs = pm.getPackagesForUid(uid);
        if (pkgs != null)
            for (String pkg : pkgs)
                if (isSystem(pkg, context))
                    return true;
        return false;
    }

    public static boolean isSystem(String packageName, Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            return ((info.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0);
            /*
            PackageInfo pkg = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return (pkg != null && pkg.signatures != null && pkg.signatures.length > 0 &&
                    sys.signatures.length > 0 && sys.signatures[0].equals(pkg.signatures[0]));
            */
        } catch (PackageManager.NameNotFoundException ignore) {
            return false;
        }
    }

    public static boolean hasInternet(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        return (pm.checkPermission("android.permission.INTERNET", packageName) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasInternet(int uid, Context context) {
        PackageManager pm = context.getPackageManager();
        String[] pkgs = pm.getPackagesForUid(uid);
        if (pkgs != null)
            for (String pkg : pkgs)
                if (hasInternet(pkg, context))
                    return true;
        return false;
    }

    public static boolean isEnabled(PackageInfo info, Context context) {
        int setting;
        try {
            PackageManager pm = context.getPackageManager();
            setting = pm.getApplicationEnabledSetting(info.packageName);
        } catch (IllegalArgumentException ex) {
            setting = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            Log.w(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
        if (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            return info.applicationInfo.enabled;
        else
            return (setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }
/*
    public static List<String> getApplicationNames(int uid, Context context) {
        List<String> listResult = new ArrayList<>();
        if (uid == 0)
            listResult.add(context.getString(R.string.title_root));
        else if (uid == 1013)
            listResult.add(context.getString(R.string.title_mediaserver));
        else if (uid == 9999)
            listResult.add(context.getString(R.string.title_nobody));
        else {
            PackageManager pm = context.getPackageManager();
            String[] pkgs = pm.getPackagesForUid(uid);
            if (pkgs == null)
                listResult.add(Integer.toString(uid));
            else
                for (String pkg : pkgs)
                    try {
                        ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                        listResult.add(pm.getApplicationLabel(info).toString());
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
            Collections.sort(listResult);
        }
        return listResult;
    }
*/
    public static boolean isDebuggable(Context context) {
        return ((context.getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

    public static boolean isPlayStoreInstall(Context context) {
        return "com.android.vending".equals(context.getPackageManager().getInstallerPackageName(context.getPackageName()));
    }

    public static String getFingerprint(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String pkg = context.getPackageName();
            PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            byte[] bytes = digest.digest(cert);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(Integer.toString(b & 0xff, 16).toLowerCase());
            return sb.toString();
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            return null;
        }
    }

    public static int dips2pixels(int dips, Context context) {
        return Math.round(dips * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static String getProtocolName(int protocol, int version, boolean brief) {
        // https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
        String p = null;
        String b = null;
        switch (protocol) {
            case 0:
                p = "HOPO";
                b = "H";
                break;
            case 2:
                p = "IGMP";
                b = "G";
                break;
            case 1:
            case 58:
                p = "ICMP";
                b = "I";
                break;
            case 6:
                p = "TCP";
                b = "T";
                break;
            case 17:
                p = "UDP";
                b = "U";
                break;
            case 50:
                p = "ESP";
                b = "E";
                break;
        }
        if (p == null)
            return Integer.toString(protocol) + "/" + version;
        return ((brief ? b : p) + (version > 0 ? version : ""));
    }

    public interface DoubtListener {
        void onSure();
    }

    private static Map<String, String> mapIPOrganization = new HashMap<>();

    public static String getOrganization(String ip) throws Exception {
        synchronized (mapIPOrganization) {
            if (mapIPOrganization.containsKey(ip))
                return mapIPOrganization.get(ip);
        }
        BufferedReader reader = null;
        try {
            URL url = new URL("http://ipinfo.io/" + ip + "/org");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(15 * 1000);
            connection.connect();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String organization = reader.readLine();
            if ("undefined".equals(organization))
                organization = null;
            synchronized (mapIPOrganization) {
                mapIPOrganization.put(ip, organization);
            }
            return organization;
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public static String md5(String text, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // MD5
        byte[] bytes = MessageDigest.getInstance("MD5").digest((text + salt).getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(KcaUtils.format("%02X", b));
        return sb.toString();
    }

    public static void logExtras(Intent intent) {
        if (intent != null)
            logBundle(intent.getExtras());
    }

    public static void logBundle(Bundle data) {
        if (data != null) {
            Set<String> keys = data.keySet();
            StringBuilder stringBuilder = new StringBuilder();
            for (String key : keys) {
                Object value = data.get(key);
                stringBuilder.append(key)
                        .append("=")
                        .append(value)
                        .append(value == null ? "" : " (" + value.getClass().getSimpleName() + ")")
                        .append("\r\n");
            }
            Log.d(TAG, stringBuilder.toString());
        }
    }

    public static StringBuilder readString(InputStreamReader reader) {
        StringBuilder sb = new StringBuilder(2048);
        char[] read = new char[128];
        try {
            for (int i; (i = reader.read(read)) >= 0; sb.append(read, 0, i)) ;
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
        return sb;
    }

    public static void sendCrashReport(Throwable ex, final Context context) {
        if (!isPlayStoreInstall(context) || Util.isDebuggable(context))
            return;

        try {
            ApplicationErrorReport report = new ApplicationErrorReport();
            report.packageName = report.processName = context.getPackageName();
            report.time = System.currentTimeMillis();
            report.type = ApplicationErrorReport.TYPE_CRASH;
            report.systemApp = false;

            ApplicationErrorReport.CrashInfo crash = new ApplicationErrorReport.CrashInfo();
            crash.exceptionClassName = ex.getClass().getSimpleName();
            crash.exceptionMessage = ex.getMessage();

            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);
            ex.printStackTrace(printer);

            crash.stackTrace = writer.toString();

            StackTraceElement stack = ex.getStackTrace()[0];
            crash.throwClassName = stack.getClassName();
            crash.throwFileName = stack.getFileName();
            crash.throwLineNumber = stack.getLineNumber();
            crash.throwMethodName = stack.getMethodName();

            report.crashInfo = crash;

            final Intent bug = new Intent(Intent.ACTION_APP_ERROR);
            bug.putExtra(Intent.EXTRA_BUG_REPORT, report);
            bug.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (bug.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(bug);
        } catch (Throwable exex) {
            Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex));
        }
    }

    public static String getGeneralInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        sb.append(KcaUtils.format("Interactive %B\r\n", isInteractive(context)));
        sb.append(KcaUtils.format("Connected %B\r\n", isConnected(context)));
        sb.append(KcaUtils.format("WiFi %B\r\n", isWifiActive(context)));
        sb.append(KcaUtils.format("Metered %B\r\n", isMeteredNetwork(context)));
        sb.append(KcaUtils.format("Roaming %B\r\n", isRoaming(context)));

        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY)
            sb.append(KcaUtils.format("SIM %s/%s/%s\r\n", tm.getSimCountryIso(), tm.getSimOperatorName(), tm.getSimOperator()));
        if (tm.getNetworkType() != TelephonyManager.NETWORK_TYPE_UNKNOWN)
            sb.append(KcaUtils.format("Network %s/%s/%s\r\n", tm.getNetworkCountryIso(), tm.getNetworkOperatorName(), tm.getNetworkOperator()));

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            sb.append(KcaUtils.format("Power saving %B\r\n", pm.isPowerSaveMode()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            sb.append(KcaUtils.format("Battery optimizing %B\r\n", batteryOptimizing(context)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            sb.append(KcaUtils.format("Data saving %B\r\n", dataSaving(context)));

        if (sb.length() > 2)
            sb.setLength(sb.length() - 2);

        return sb.toString();
    }

    public static String getNetworkInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ani = cm.getActiveNetworkInfo();
        List<NetworkInfo> listNI = new ArrayList<>();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            listNI.addAll(Arrays.asList(cm.getAllNetworkInfo()));
        else
            for (Network network : cm.getAllNetworks()) {
                NetworkInfo ni = cm.getNetworkInfo(network);
                if (ni != null)
                    listNI.add(ni);
            }

        for (NetworkInfo ni : listNI) {
            sb.append(ni.getTypeName()).append('/').append(ni.getSubtypeName())
                    .append(' ').append(ni.getDetailedState())
                    .append(TextUtils.isEmpty(ni.getExtraInfo()) ? "" : " " + ni.getExtraInfo())
                    .append(ni.getType() == ConnectivityManager.TYPE_MOBILE ? " " + Util.getNetworkGeneration(ni.getSubtype()) : "")
                    .append(ni.isRoaming() ? " R" : "")
                    .append(ani != null && ni.getType() == ani.getType() && ni.getSubtype() == ani.getSubtype() ? " *" : "")
                    .append("\r\n");
        }

        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis != null)
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    if (ni != null && !"lo".equals(ni.getName())) {
                        List<InterfaceAddress> ias = ni.getInterfaceAddresses();
                        if (ias != null)
                            for (InterfaceAddress ia : ias)
                                sb.append(ni.getName())
                                        .append(' ').append(ia.getAddress().getHostAddress())
                                        .append('/').append(ia.getNetworkPrefixLength())
                                        .append(' ').append(ni.getMTU())
                                        .append("\r\n");
                    }
                }
        } catch (Throwable ex) {
            sb.append(ex.toString()).append("\r\n");
        }

        if (sb.length() > 2)
            sb.setLength(sb.length() - 2);

        return sb.toString();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean batteryOptimizing(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static boolean dataSaving(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public static String getSubscriptionInfo(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            return "Not supported";
        if (!hasPhoneStatePermission(context))
            return "No permission";

        StringBuilder sb = new StringBuilder();
        SubscriptionManager sm = SubscriptionManager.from(context);

        sb.append("Slots ")
                .append(sm.getActiveSubscriptionInfoCount())
                .append('/')
                .append(sm.getActiveSubscriptionInfoCountMax())
                .append("\r\n");

        int dataid = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            dataid = sm.getDefaultDataSubscriptionId();

        int voiceid = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            voiceid = sm.getDefaultVoiceSubscriptionId();

        List<SubscriptionInfo> subscriptions = sm.getActiveSubscriptionInfoList();
        if (subscriptions != null)
            for (SubscriptionInfo si : subscriptions)
                sb.append("SIM ")
                        .append(si.getSimSlotIndex() + 1)
                        .append('/')
                        .append(si.getSubscriptionId())
                        .append(' ')
                        .append(si.getCountryIso())
                        .append('/')
                        .append(si.getMcc()).append(si.getMnc())
                        .append(' ')
                        .append(si.getCarrierName())
                        .append(si.getSubscriptionId() == dataid ? " D" : "")
                        .append(si.getSubscriptionId() == voiceid ? " V" : "")
                        .append(si.getDataRoaming() == SubscriptionManager.DATA_ROAMING_ENABLE ? " R" : "")
                        .append("\r\n");

        if (sb.length() > 2)
            sb.setLength(sb.length() - 2);

        return sb.toString();
    }


}
