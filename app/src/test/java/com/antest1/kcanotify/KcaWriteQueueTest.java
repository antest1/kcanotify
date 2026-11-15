package com.antest1.kcanotify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class KcaWriteQueueTest {
    // runs the scheduled tasks only when asked, to control when values are written
    private static class ManualExecutor implements Executor {
        final Deque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void runNext() {
            tasks.remove().run();
        }

        void runAll() {
            while (!tasks.isEmpty()) runNext();
        }
    }

    private final ManualExecutor executor = new ManualExecutor();
    private final KcaWriteQueue queue = new KcaWriteQueue(executor);
    private final Map<String, String> store = new HashMap<>();
    private final List<String> writes = new ArrayList<>();
    private final KcaWriteQueue.Writer writer = (key, value) -> {
        writes.add(key + "=" + value);
        if (value == null) store.remove(key);
        else store.put(key, value);
    };

    @Test
    public void readsValueUntilWritten() {
        queue.schedule("a", "1", writer);
        assertEquals("1", queue.get("a").value);
        assertFalse(store.containsKey("a"));

        executor.runAll();
        assertEquals("1", store.get("a"));
        assertNull(queue.get("a"));
    }

    @Test
    public void writesOnlyLatestValue() {
        queue.schedule("a", "1", writer);
        queue.schedule("a", "2", writer);
        queue.schedule("b", "3", writer);
        assertEquals("2", queue.get("a").value);

        executor.runAll();
        assertEquals(Arrays.asList("a=2", "b=3"), writes);
        assertNull(queue.get("a"));
        assertNull(queue.get("b"));
    }

    @Test
    public void keepsValueScheduledWhileWriting() {
        // a new value comes while the previous one is being written
        KcaWriteQueue.Writer racing = (key, value) -> {
            writer.write(key, value);
            if ("1".equals(value)) queue.schedule("a", "2", writer);
        };
        queue.schedule("a", "1", racing);

        executor.runNext();
        assertEquals("1", store.get("a"));
        assertEquals("2", queue.get("a").value);

        executor.runNext();
        assertEquals("2", store.get("a"));
        assertNull(queue.get("a"));
    }

    @Test
    public void hidesDeletedValueUntilDeleted() {
        store.put("a", "old");
        queue.schedule("a", null, writer);
        KcaWriteQueue.Pending pending = queue.get("a");
        assertNotNull(pending);
        assertNull(pending.value);

        executor.runAll();
        assertFalse(store.containsKey("a"));
        assertNull(queue.get("a"));
    }

    @Test
    public void flushWaitsForScheduledValues() throws InterruptedException {
        ExecutorService background = Executors.newSingleThreadExecutor();
        try {
            KcaWriteQueue backgroundQueue = new KcaWriteQueue(background);
            Map<String, String> written = Collections.synchronizedMap(new HashMap<>());
            KcaWriteQueue.Writer slowWriter = (key, value) -> {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                written.put(key, value);
            };
            for (int i = 0; i < 5; i++) {
                backgroundQueue.schedule("key" + i, String.valueOf(i), slowWriter);
            }

            backgroundQueue.flush();
            assertEquals(5, written.size());
            for (int i = 0; i < 5; i++) {
                assertTrue(written.containsKey("key" + i));
                assertNull(backgroundQueue.get("key" + i));
            }
        } finally {
            background.shutdownNow();
        }
    }
}
