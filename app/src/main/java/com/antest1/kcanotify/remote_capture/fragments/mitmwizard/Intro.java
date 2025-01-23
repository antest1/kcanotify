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
 * Copyright 2022 - Emanuele Faranda
 */

package com.antest1.kcanotify.remote_capture.fragments.mitmwizard;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.antest1.kcanotify.R;
import com.antest1.kcanotify.remote_capture.Utils;

public class Intro extends StepFragment {
    private static final String TLS_DECRYPTION_DOCS_URL = "https://emanuele-f.github.io/PCAPdroid/tls_decryption";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStepIcon.setVisibility(View.GONE);
        Utils.setTextUrls(mStepLabel, R.string.mitm_setup_wizard_intro, TLS_DECRYPTION_DOCS_URL);

        nextStep(R.id.navto_install_addon);
    }
}