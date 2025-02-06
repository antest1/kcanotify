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
 * Copyright 2020-25 - Emanuele Faranda
 */

package com.antest1.kcanotify.remote_capture;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.text.HtmlCompat;

import com.antest1.kcanotify.R;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Utils {
    static final String TAG = "Utils";

    public static void showToast(Context context, int id, Object... args) {
        String msg = context.getResources().getString(id, args);
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showToastLong(Context context, int id, Object... args) {
        String msg = context.getResources().getString(id, args);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public static boolean supportsFileDialog(Context context, Intent intent) {
        // https://commonsware.com/blog/2017/12/27/storage-access-framework-missing-action.html
        ComponentName comp = intent.resolveActivity(context.getPackageManager());

        return((comp != null) && (!"com.google.android.tv.frameworkpackagestubs".equals(comp.getPackageName())));
    }

    public static boolean launchFileDialog(Context context, Intent intent, ActivityResultLauncher<Intent> launcher) {
        if(Utils.supportsFileDialog(context, intent)) {
            try {
                launcher.launch(intent);
                return true;
            } catch (ActivityNotFoundException ignored) {}
        }

        Utils.showToastLong(context, R.string.no_activity_file_selection);
        return false;
    }

    public static void startActivity(Context ctx, Intent intent) {
        try {
            ctx.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(ctx, "no_intent_handler_found", Toast.LENGTH_LONG).show();
        }
    }

    public static void safeClose(Closeable obj) {
        if(obj == null)
            return;

        try {
            obj.close();
        } catch (IOException e) {
            Log.w(TAG, e.getLocalizedMessage());
        }
    }

    public static X509Certificate x509FromPem(String pem) {
        int begin = pem.indexOf('\n') + 1;
        int end = pem.indexOf('-', begin);

        if((begin > 0) && (end > begin)) {
            String cert64 = pem.substring(begin, end);
            //Log.d(TAG, "Cert: " + cert64);
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                byte[] cert_data = android.util.Base64.decode(cert64, android.util.Base64.DEFAULT);
                return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert_data));
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static boolean isCAInstalled(X509Certificate ca_cert) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            ks.load(null, null);
            return ks.getCertificateAlias(ca_cert) != null;
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isCAInstalled(String ca_pem) {
        if(ca_pem == null)
            return false;

        X509Certificate ca_cert = x509FromPem(ca_pem);
        if(ca_cert == null)
            return false;

        return isCAInstalled(ca_cert);
    }

    // Like Files.copy(src.toPath(), out);
    public static void copy(File src, OutputStream out) throws IOException {
        try(FileInputStream in = new FileInputStream(src)) {
            byte[] bytesIn = new byte[4096];
            int read;
            while((read = in.read(bytesIn)) != -1)
                out.write(bytesIn, 0, read);
        }
    }

    public static void copy(InputStream in, File dst) throws IOException {
        try(FileOutputStream out = new FileOutputStream(dst)) {
            byte[] bytesIn = new byte[4096];
            int read;
            while((read = in.read(bytesIn)) != -1)
                out.write(bytesIn, 0, read);
        }
    }

    // Get a CharSequence which properly displays clickable links obtained by formatting a parametric
    // string resource with the provided args. See setTextUrls
    // https://stackoverflow.com/questions/23503642/how-to-use-formatted-strings-together-with-placeholders-in-android
    public static CharSequence getText(Context context, int resid, String... args) {
        for(int i = 0; i < args.length; ++i)
            args[i] = TextUtils.htmlEncode(args[i]);

        String htmlOnly = String.format(HtmlCompat.toHtml(new SpannedString(context.getText(resid)),
                HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE), (Object[]) args);
        //Log.d(TAG, htmlOnly);
        return HtmlCompat.fromHtml(htmlOnly, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    // Format a resource containing URLs and display it in a TextView, making URls clickable
    public static void setTextUrls(TextView tv, int resid, String... args) {
        CharSequence text = getText(tv.getContext(), resid, args);
        tv.setText(text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @SuppressWarnings({"deprecation"})
    public static PackageInfo getPackageInfo(PackageManager pm, String package_name, int flags) throws PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return pm.getPackageInfo(package_name, PackageManager.PackageInfoFlags.of(flags));
        else
            return pm.getPackageInfo(package_name, flags);
    }

    public static int getMajorVersion(String ver) {
        int start_idx = 0;

        // optionally starts with "v"
        if (ver.startsWith("v"))
            start_idx = 1;

        int end_idx = ver.indexOf('.');
        if (end_idx < 0)
            return -1;

        try {
            return Integer.parseInt(ver.substring(start_idx, end_idx));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    // true if the two provided versions are semantically compatible (i.e. same major)
    public static boolean isSemanticVersionCompatible(String a, String b) {
        int va = getMajorVersion(a);
        return (va >= 0) && (va == getMajorVersion(b));
    }
}
