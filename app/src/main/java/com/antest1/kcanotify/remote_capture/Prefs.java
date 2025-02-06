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

import android.content.SharedPreferences;

public class Prefs {
    public static final String PREF_TLS_DECRYPTION_SETUP_DONE = "tls_decryption_setup_ok";
    public static final String PREF_CA_INSTALLATION_SKIPPED = "ca_install_skipped";
    public static final String PREF_IGNORED_MITM_VERSION = "ignored_mitm_version";

    /* Prefs with defaults */
    public static boolean isTLSDecryptionSetupDone(SharedPreferences p)     { return(p.getBoolean(PREF_TLS_DECRYPTION_SETUP_DONE, false)); }
    public static boolean isIgnoredMitmVersion(SharedPreferences p, String v) { return p.getString(PREF_IGNORED_MITM_VERSION, "").equals(v); }
}
