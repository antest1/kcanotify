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
import androidx.annotation.Nullable;

import com.antest1.kcanotify.remote_capture.AppsResolver;

import java.util.ArrayList;

/* Holds the information about a single connection.
 * Equivalent of zdtun_conn_t from zdtun and pd_conn_t from pcapdroid.c .
 *
 * Connections are normally stored into the ConnectionsRegister. Concurrent access to the connection
 * fields can happen when a connection is updated and, at the same time, it is retrieved by the UI
 * thread. However this does not create concurrency problems as the update only increments counters
 * or sets a previously null field to a non-null value.
 */
public class ConnectionDescriptor {
    // sync with zdtun_conn_status_t
    public static final int CONN_STATUS_NEW = 0,
        CONN_STATUS_CONNECTING = 1,
        CONN_STATUS_CONNECTED = 2,
        CONN_STATUS_CLOSED = 3,
        CONN_STATUS_ERROR = 4,
        CONN_STATUS_SOCKET_ERROR = 5,
        CONN_STATUS_CLIENT_ERROR = 6,
        CONN_STATUS_RESET = 7,
        CONN_STATUS_UNREACHABLE = 8;

    // This is an high level status which abstracts the zdtun_conn_status_t
    public enum Status {
        STATUS_INVALID,
        STATUS_ACTIVE,
        STATUS_CLOSED,
        STATUS_UNREACHABLE,
        STATUS_ERROR,
    }

    public enum DecryptionStatus {
        INVALID,
        ENCRYPTED,
        CLEARTEXT,
        DECRYPTED,
        NOT_DECRYPTABLE,
        WAITING_DATA,
        ERROR,
    }

    public enum FilteringStatus {
        INVALID,
        ALLOWED,
        BLOCKED
    }

    /* Metadata */
    public final int ipver;
    public final int ipproto;
    public final String src_ip;
    public final String dst_ip;
    public final int src_port;
    public final int dst_port;
    public final int local_port; // in VPN mode, this is the local port of the Internet connection

    /* Data */
    public long first_seen;
    public long last_seen;
    public long payload_length;
    public long sent_bytes;
    public long rcvd_bytes;
    public int sent_pkts;
    public int rcvd_pkts;
    public int blocked_pkts;
    public String info;
    public String url;
    public String l7proto;
    private final ArrayList<PayloadChunk> payload_chunks; // must be synchronized
    public final int uid;
    public final int ifidx;
    public final int incr_id;
    private final boolean mitm_decrypt; // true if the connection is under mitm for TLS decryption
    public int status;
    private int tcp_flags;
    public boolean is_blocked;
    private boolean port_mapping_applied;
    private boolean decryption_ignored;
    private boolean payload_truncated;
    private boolean encrypted_l7;     // application layer is encrypted (e.g. TLS)
    public boolean encrypted_payload; // actual payload is encrypted (e.g. telegram - see Utils.hasEncryptedPayload)
    public String decryption_error;
    public String js_injected_scripts;
    public String country;

    // NOTE: invoked from JNI
    public ConnectionDescriptor(int _incr_id, int _ipver, int _ipproto, String _src_ip, String _dst_ip, String _country,
                                int _src_port, int _dst_port, int _local_port, int _uid, int _ifidx,
                                boolean _mitm_decrypt, long when) {
        incr_id = _incr_id;
        ipver = _ipver;
        ipproto = _ipproto;
        src_ip = _src_ip;
        dst_ip = _dst_ip;
        src_port = _src_port;
        dst_port = _dst_port;
        local_port = _local_port;
        uid = _uid;
        ifidx = _ifidx;
        first_seen = last_seen = when;
        l7proto = "";
        country = _country;
        payload_chunks = new ArrayList<>();
        mitm_decrypt = _mitm_decrypt;
    }

    public void processUpdate(ConnectionUpdate update) {
        // The "update_type" is used to limit the amount of data sent via the JNI
        if((update.update_type & ConnectionUpdate.UPDATE_STATS) != 0) {
            sent_bytes = update.sent_bytes;
            rcvd_bytes = update.rcvd_bytes;
            sent_pkts = update.sent_pkts;
            rcvd_pkts = update.rcvd_pkts;
            blocked_pkts = update.blocked_pkts;
            status = (update.status & 0x00FF);
            port_mapping_applied = (update.status & 0x2000) != 0;
            decryption_ignored = (update.status & 0x1000) != 0;
            is_blocked = (update.status & 0x0400) != 0;
            last_seen = update.last_seen;
            tcp_flags = update.tcp_flags; // NOTE: only for root capture

            // see MitmReceiver.handlePayload
            if((status == ConnectionDescriptor.CONN_STATUS_CLOSED) && (decryption_error != null))
                status = ConnectionDescriptor.CONN_STATUS_CLIENT_ERROR;

            // with mitm we account the TLS payload length instead
            if(!mitm_decrypt)
                payload_length = update.payload_length;
        }
        if((update.update_type & ConnectionUpdate.UPDATE_INFO) != 0) {
            info = update.info;
            url = update.url;
            l7proto = update.l7proto;
            encrypted_l7 = ((update.info_flags & ConnectionUpdate.UPDATE_INFO_FLAG_ENCRYPTED_L7) != 0);
        }
        if((update.update_type & ConnectionUpdate.UPDATE_PAYLOAD) != 0) {
            // Payload for decryptable connections should be received via the MitmReceiver
            assert(decryption_ignored || isNotDecryptable());

            synchronized (this) {
                if(update.payload_chunks != null)
                    payload_chunks.addAll(update.payload_chunks);
                payload_truncated = update.payload_truncated;
            }

        }
    }

    public boolean matches(AppsResolver res, String filter) {
        filter = filter.toLowerCase();
        AppDescriptor app = res.getAppByUid(uid, 0);

        return(((info != null) && (info.contains(filter))) ||
                dst_ip.contains(filter) ||
                l7proto.toLowerCase().equals(filter) ||
                Integer.toString(uid).equals(filter) ||
                Integer.toString(dst_port).contains(filter) ||
                Integer.toString(src_port).equals(filter) ||
                ((app != null) && (app.matches(filter, true)))
        );
    }

    public void setPayloadTruncatedByAddon() {
        // only for the mitm addon
        assert(!isNotDecryptable());
        payload_truncated = true;
    }

    public boolean isPayloadTruncated() { return payload_truncated; }
    public boolean isPortMappingApplied() { return port_mapping_applied; }

    public boolean isNotDecryptable()   { return !decryption_ignored && (encrypted_payload || !mitm_decrypt); }
    public boolean isDecrypted()        { return !decryption_ignored && !isNotDecryptable() && (getNumPayloadChunks() > 0); }
    public boolean isCleartext()        { return !encrypted_payload && !encrypted_l7; }

    public synchronized int getNumPayloadChunks() { return payload_chunks.size(); }

    public synchronized @Nullable PayloadChunk getPayloadChunk(int idx) {
        if(getNumPayloadChunks() <= idx)
            return null;
        return payload_chunks.get(idx);
    }

    public synchronized void addPayloadChunkMitm(PayloadChunk chunk) {
        payload_chunks.add(chunk);
        payload_length += chunk.payload.length;
    }

    public synchronized void dropPayload() {
        payload_chunks.clear();
    }

    @Override
    public @NonNull String toString() {
        return "[proto=" + ipproto + "/" + l7proto + "]: " + src_ip + ":" + src_port + " -> " +
                dst_ip + ":" + dst_port + " [" + uid + "] " + info;
    }
}
