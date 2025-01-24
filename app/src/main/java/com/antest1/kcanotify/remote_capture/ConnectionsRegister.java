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

import android.content.Context;
import android.util.SparseIntArray;

import androidx.annotation.Nullable;

import com.antest1.kcanotify.remote_capture.interfaces.ConnectionsListener;
import com.antest1.kcanotify.remote_capture.model.ConnectionDescriptor;
import com.antest1.kcanotify.remote_capture.model.ConnectionUpdate;
import com.antest1.kcanotify.remote_capture.model.PayloadChunk;

import static com.antest1.kcanotify.KcaVpnData.containsKcaServer;
import static com.antest1.kcanotify.KcaVpnData.getDataFromNative;

import com.antest1.kcanotify.KcaVpnData;

import java.util.ArrayList;
import java.util.Arrays;

/* A container for the connections. This is used to store active/closed connections until the capture
 * is stopped. Active connections are also kept in the native side.
 *
 * The ConnectionsRegister can store up to _size items, after which rollover occurs and older
 * connections are replaced with the new ones. Via the addListener method it's possible to listen
 * for connections changes (connections added/removed/updated). The usual listener for such events
 * is the ConnectionsFragment, which then forwards them to the ConnectionsAdapter.
 *
 * Connections are added/updated by the CaptureService in a separate thread. The getter methods are
 * instead called on the UI thread, usually by the ConnectionsAdapter. Methods are synchronized to
 * provide threads safety on this class. Concurrent access to the ConnectionDescriptors fields can
 * occur during connectionsUpdates but it's not protected, check out the ConnectionDescriptor class
 * for more details.
 */
public class ConnectionsRegister {
    private static final String TAG = "ConnectionsRegister";

    private final ConnectionDescriptor[] mItemsRing;
    private int mTail;
    private final int mSize;
    private int mCurItems;
    private final SparseIntArray mConnsByIface;
    private final ArrayList<ConnectionsListener> mListeners;

    public ConnectionsRegister(Context ctx, int _size) {
        mTail = 0;
        mCurItems = 0;
        mSize = _size;
        mItemsRing = new ConnectionDescriptor[mSize];
        mListeners = new ArrayList<>();
        mConnsByIface = new SparseIntArray();
    }

    // returns the position in mItemsRing of the oldest connection
    private synchronized int firstPos() {
        return (mCurItems < mSize) ? 0 : mTail;
    }

    // returns the position in mItemsRing of the newest connection
    private synchronized int lastPos() {
        return (mTail - 1 + mSize) % mSize;
    }

    // called by the CaptureService in a separate thread when new connections should be added to the register
    public synchronized void newConnections(ConnectionDescriptor[] conns) {
        if(conns.length > mSize) {
            // this should only occur while testing with small register sizes
            // take the most recent connections
            conns = Arrays.copyOfRange(conns, conns.length - mSize, conns.length);
        }

        int out_items = conns.length - Math.min((mSize - mCurItems), conns.length);
        int insert_pos = mCurItems;
        ConnectionDescriptor []removedItems = null;

        //Log.d(TAG, "newConnections[" + mNumItems + "/" + mSize +"]: insert " + conns.length +
        //        " items at " + mTail + " (removed: " + out_items + " at " + firstPos() + ")");

        // Remove old connections
        if(out_items > 0) {
            int pos = firstPos();
            removedItems = new ConnectionDescriptor[out_items];

            for(int i=0; i<out_items; i++) {
                ConnectionDescriptor conn = mItemsRing[pos];

                if(conn != null) {
                    if(conn.ifidx > 0) {
                        int num_conn = mConnsByIface.get(conn.ifidx);
                        if(--num_conn <= 0)
                            mConnsByIface.delete(conn.ifidx);
                        else
                            mConnsByIface.put(conn.ifidx, num_conn);
                    }
                }

                removedItems[i] = conn;
                pos = (pos + 1) % mSize;
            }
        }

        // Add new connections
        for(ConnectionDescriptor conn: conns) {
            mItemsRing[mTail] = conn;
            mTail = (mTail + 1) % mSize;
            mCurItems = Math.min(mCurItems + 1, mSize);

            if(conn.ifidx > 0) {
                int num_conn = mConnsByIface.get(conn.ifidx);
                mConnsByIface.put(conn.ifidx, num_conn + 1);
            }

            processPayloadChunks(conn, null);
            conn.dropPayload(); // clear payload after processing
        }

        for(ConnectionsListener listener: mListeners) {
            if(out_items > 0)
                listener.connectionsRemoved(0, removedItems);

            if(conns.length > 0)
                listener.connectionsAdded(insert_pos - out_items, conns);
        }
    }

    // called by the CaptureService in a separate thread when connections should be updated
    public synchronized void connectionsUpdates(ConnectionUpdate[] updates) {
        if(mCurItems == 0)
            return;

        int first_pos = firstPos();
        int last_pos = lastPos();
        int first_id = mItemsRing[first_pos].incr_id;
        int last_id = mItemsRing[last_pos].incr_id;
        int []changed_pos = new int[updates.length];
        int k = 0;

        Log.d(TAG, "connectionsUpdates: items=" + mCurItems + ", first_id=" + first_id + ", last_id=" + last_id);

        for(ConnectionUpdate update: updates) {
            int id = update.incr_id;

            // ignore updates for untracked items
            if((id >= first_id) && (id <= last_id)) {
                int pos = ((id - first_id) + first_pos) % mSize;
                ConnectionDescriptor conn = mItemsRing[pos];
                assert(conn.incr_id == id);

                //Log.d(TAG, "update " + update.incr_id + " -> " + update.update_type);
                processPayloadChunks(conn, update);
                changed_pos[k++] = (pos + mSize - first_pos) % mSize;
            }
        }

        if(k == 0)
            // no updates for items in the ring
            return;

        if(k != updates.length)
            // some untracked items where skipped, shrink the array
            changed_pos = Arrays.copyOf(changed_pos, k);

        for(ConnectionsListener listener: mListeners)
            listener.connectionsUpdated(changed_pos);
    }

    public synchronized void reset() {
        for(int i = 0; i< mSize; i++)
            mItemsRing[i] = null;

        mCurItems = 0;
        mTail = 0;

        for(ConnectionsListener listener: mListeners)
            listener.connectionsChanges(mCurItems);
    }

    public synchronized void addListener(ConnectionsListener listener) {
        mListeners.add(listener);

        // Send the first update to sync it
        listener.connectionsChanges(mCurItems);

        Log.d(TAG, "(add) new connections listeners size: " + mListeners.size());
    }

    public synchronized void removeListener(ConnectionsListener listener) {
        mListeners.remove(listener);

        Log.d(TAG, "(remove) new connections listeners size: " + mListeners.size());
    }

    // get the i-th oldest connection
    public synchronized @Nullable ConnectionDescriptor getConn(int i) {
        if((i < 0) || (i >= mCurItems))
            return null;

        int pos = (firstPos() + i) % mSize;
        return mItemsRing[pos];
    }

    public synchronized int getConnPositionById(int incr_id) {
        if(mCurItems <= 0)
            return -1;

        ConnectionDescriptor first = mItemsRing[firstPos()];
        ConnectionDescriptor last = mItemsRing[lastPos()];

        if((incr_id < first.incr_id) || (incr_id > last.incr_id))
            return -1;

        return(incr_id - first.incr_id);
    }

    public synchronized @Nullable ConnectionDescriptor getConnById(int incr_id) {
        int pos = getConnPositionById(incr_id);
        if(pos < 0)
            return null;

        return getConn(pos);
    }

    public synchronized void releasePayloadMemory() {
        Log.i(TAG, "releaseFullPayloadMemory called");

        for(int i=0; i<mCurItems; i++) {
            ConnectionDescriptor conn = mItemsRing[i];
            conn.dropPayload();
        }
    }

    private void processPayloadChunks(ConnectionDescriptor conn, ConnectionUpdate update) {
        byte[] source;
        byte[] destination;
        int sport;
        int dport;
        int pkt_type;

        // check HTTP traffic only
        if (conn.src_port != 80 && conn.dst_port != 80) return;

        // get new or updated chunks from connection info
        ArrayList<PayloadChunk> payload_chunks = new ArrayList<>();
        if (update == null) {
            for (int i = 0; i < conn.getNumPayloadChunks(); i++) {
                payload_chunks.add(conn.getPayloadChunk(i));
            }
        } else if (update.payload_chunks != null) {
            payload_chunks = update.payload_chunks;
        } else {
            return;
        }

        // read chunk payload
        for (PayloadChunk chunk: payload_chunks) {
            if (chunk != null) {
                if (chunk.is_sent) {
                    pkt_type = KcaVpnData.REQUEST;
                    source = conn.src_ip.getBytes();
                    destination = conn.dst_ip.getBytes();
                    sport = conn.src_port;
                    dport = conn.dst_port;
                } else {
                    pkt_type = KcaVpnData.RESPONSE;
                    source = conn.dst_ip.getBytes();
                    destination = conn.src_ip.getBytes();
                    sport = conn.dst_port;
                    dport = conn.src_port;
                }

                byte[] payload = chunk.payload;
                byte[] head = null;
                if (pkt_type == 1 && payload.length > KcaVpnData.HEADER_INSPECT_SIZE) {
                    head = Arrays.copyOfRange(payload, 0, KcaVpnData.HEADER_INSPECT_SIZE);
                }

                if (containsKcaServer(pkt_type, source, destination, head) == 1) {
                    getDataFromNative(payload, payload.length, pkt_type, source, destination, sport, dport);
                }
            }
        }
    }
}
