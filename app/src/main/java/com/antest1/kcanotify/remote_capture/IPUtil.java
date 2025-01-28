package com.antest1.kcanotify.remote_capture;

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

    Copyright 2015-2024 by Marcel Bokhorst (M66B)
*/

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class IPUtil {
    private static final String TAG = "IPUtil";

    public static List<Cidr> toCidr(String start, String end) throws UnknownHostException {
        return toCidr(InetAddress.getByName(start), InetAddress.getByName(end));
    }

    public static List<Cidr> toCidr(InetAddress start, InetAddress end) throws UnknownHostException {
        List<Cidr> listResult = new ArrayList<>();

        long from = inet2long(start);
        long to = inet2long(end);
        while (to >= from) {
            byte prefix = 32;
            while (prefix > 0) {
                long mask = prefix2mask(prefix - 1);
                if ((from & mask) != from)
                    break;
                prefix--;
            }

            byte max = (byte) (32 - Math.floor(Math.log(to - from + 1) / Math.log(2)));
            if (prefix < max)
                prefix = max;

            listResult.add(new Cidr(long2inet(from).getHostAddress() + "/" + prefix));

            from += Math.pow(2, (32 - prefix));
        }

        for (Cidr Cidr : listResult)
            Log.i(TAG, Cidr.toString());

        return listResult;
    }

    private static long prefix2mask(int bits) {
        return (0xFFFFFFFF00000000L >> bits) & 0xFFFFFFFFL;
    }

    public static long inet2long(InetAddress addr) {
        long result = 0;
        if (addr != null)
            for (byte b : addr.getAddress())
                result = result << 8 | (b & 0xFF);
        return result;
    }

    public static InetAddress long2inet(long addr) {
        try {
            byte[] b = new byte[4];
            for (int i = b.length - 1; i >= 0; i--) {
                b[i] = (byte) (addr & 0xFF);
                addr = addr >> 8;
            }
            return InetAddress.getByAddress(b);
        } catch (UnknownHostException ignore) {
            return null;
        }
    }

    public static InetAddress minus1(InetAddress addr) {
        return long2inet(inet2long(addr) - 1);
    }

    public static InetAddress plus1(InetAddress addr) {
        return long2inet(inet2long(addr) + 1);
    }
}
