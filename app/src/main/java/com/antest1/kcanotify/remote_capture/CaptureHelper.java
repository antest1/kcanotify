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

package com.antest1.kcanotify.remote_capture;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.antest1.kcanotify.remote_capture.interfaces.CaptureStartListener;
import com.antest1.kcanotify.remote_capture.model.CaptureSettings;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CaptureHelper {
    private static final String TAG = "CaptureHelper";
    private final Context mContext;
    private final @Nullable ActivityResultLauncher<Intent> mLauncher;
    private final boolean mResolveHosts;
    private CaptureSettings mSettings;
    private CaptureStartListener mListener;

    public CaptureHelper(ComponentActivity activity, boolean resolve_hosts) {
        mContext = activity;
        mResolveHosts = resolve_hosts;
        mLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::captureServiceResult);
    }

    /** Note: This constructor does not handle the first-time VPN prepare */
    public CaptureHelper(Context context) {
        mContext = context;
        mResolveHosts = true;
        mLauncher = null;
    }

    private void captureServiceResult(final ActivityResult result) {
        if(result.getResultCode() == Activity.RESULT_OK)
            resolveHosts();
        else if(mListener != null) {
            Toast.makeText(mContext, "vpn_setup_failed", Toast.LENGTH_LONG).show();
            mListener.onCaptureStartResult(false);
        }
    }

    private void startCaptureOk() {
        final Intent intent = new Intent(mContext, CaptureService.class);
        intent.putExtra("settings", mSettings);
        mContext.startService(intent);
        if(mListener != null)
            mListener.onCaptureStartResult(true);
    }

    private static String resolveHost(String host) {
        Log.d(TAG, "Resolving host: " + host);

        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException ignored) {}

        return null;
    }

    private static String doResolveHosts(CaptureSettings settings) {
        // NOTE: hosts must be resolved before starting the VPN and in a separate thread
        String resolved;

        if(settings == null)
            return null;

        if(settings.socks5_enabled) {
            if ((resolved = resolveHost(settings.socks5_proxy_address)) == null)
                return settings.socks5_proxy_address;
            else if (!resolved.equals(settings.socks5_proxy_address)) {
                Log.i(TAG, "Resolved SOCKS5 proxy address: " + resolved);
                settings.socks5_proxy_address = resolved;
            }
        }

        return null;
    }

    private void resolveHosts() {
        if (!mResolveHosts) {
            startCaptureOk();
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());

        (new Thread(() -> {
            String failed_host = doResolveHosts(mSettings);

            handler.post(() -> {
                if(mSettings == null) {
                    mListener.onCaptureStartResult(false);
                    return;
                }

                if(failed_host == null)
                    startCaptureOk();
                else {
                    Toast.makeText(mContext, "host_resolution_failed: " + failed_host, Toast.LENGTH_LONG).show();
                    mListener.onCaptureStartResult(false);
                }
            });
        })).start();
    }

    public void startCapture(CaptureSettings settings) {
        if(CaptureService.isServiceActive())
            CaptureService.stopService();

        mSettings = settings;

        if(settings.root_capture || settings.readFromPcap()) {
            resolveHosts();
            return;
        }

        Intent vpnPrepareIntent = VpnService.prepare(mContext);
        if(vpnPrepareIntent != null) {
            if (mLauncher != null)
                new AlertDialog.Builder(mContext)
                        .setMessage("vpn_setup_msg")
                        .setPositiveButton("ok", (dialog, whichButton) -> {
                            try {
                                mLauncher.launch(vpnPrepareIntent);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(mContext, "no_intent_handler_found", Toast.LENGTH_LONG).show();
                                mListener.onCaptureStartResult(false);
                            }
                        })
                        .setOnCancelListener(dialog -> {
                            Toast.makeText(mContext, "vpn_setup_failed", Toast.LENGTH_LONG).show();
                            mListener.onCaptureStartResult(false);
                        })
                        .show();
            else if (mListener != null)
                mListener.onCaptureStartResult(false);
        } else
            resolveHosts();
    }

    public void setListener(CaptureStartListener listener) {
        mListener = listener;
    }
}
