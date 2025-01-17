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

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.antest1.kcanotify.KcaApplication;
import com.antest1.kcanotify.remote_capture.CaptureService;
import com.antest1.kcanotify.remote_capture.model.MatchList.RuleType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;


public class ListInfo {
    private final Type mType;

    public enum Type {
        DECRYPTION_LIST,
    }

    public ListInfo(Type tp) {
        mType = tp;
    }

    public Type getType() {
        return mType;
    }

    public @NonNull MatchList getList() {
        switch(mType) {
            case DECRYPTION_LIST:
                return KcaApplication.getInstance().getDecryptionList();
        }

        assert false;
        return null;
    }

    public int getTitle() {
        assert false;
        return 0;
    }

    public int getHelpString() {
        assert false;
        return 0;
    }

    public Set<RuleType> getSupportedRules() {
        switch(mType) {
            case DECRYPTION_LIST:
                return new ArraySet<>(Arrays.asList(RuleType.APP, RuleType.IP, RuleType.HOST, RuleType.COUNTRY));
        }

        assert false;
        return null;
    }

    public void reloadRules() {
        switch(mType) {
            case DECRYPTION_LIST:
                CaptureService.reloadDecryptionList();
                break;
        }
    }
}
