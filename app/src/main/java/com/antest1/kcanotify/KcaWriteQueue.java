package com.antest1.kcanotify;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

// Writes values by key on an executor, so the caller does not wait for the disk.
// A value stays readable through get() until it is written; when a key is written again before that,
// only the latest value is written. The executor must run tasks one at a time, in order.
public class KcaWriteQueue {
    public interface Writer {
        // value is null to delete the key; must not throw
        void write(String key, String value);
    }

    public static final class Pending {
        public final String value; // null if the key is being deleted

        Pending(String value) {
            this.value = value;
        }
    }

    private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();
    private final Executor executor;

    public KcaWriteQueue(Executor executor) {
        this.executor = executor;
    }

    // value is null to delete the key
    public void schedule(String key, String value, Writer writer) {
        Pending entry = new Pending(value);
        pending.put(key, entry);
        executor.execute(() -> {
            // a later value of the key replaced this one: its own task writes it
            if (pending.get(key) != entry) return;
            try {
                writer.write(key, value);
            } finally {
                // keep a value scheduled while this one was being written
                pending.remove(key, entry);
            }
        });
    }

    // the value not written yet, or null if there is none
    public Pending get(String key) {
        return pending.get(key);
    }

    // waits until the values scheduled so far are written
    public void flush() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        executor.execute(done::countDown);
        done.await();
    }
}
