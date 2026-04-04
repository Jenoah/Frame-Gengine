package nl.framegengine.core.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ObjectPoolTest {

    // -----------------------------------------------------------------------
    // 2.5.1  obtain() on empty pool — creator is called
    // -----------------------------------------------------------------------
    @Test
    void obtain_onEmptyPool_createsNewObject() {
        AtomicInteger createCount = new AtomicInteger(0);
        ObjectPool<StringBuilder> pool = new ObjectPool<>(() -> {
            createCount.incrementAndGet();
            return new StringBuilder();
        });

        StringBuilder obj = pool.obtain();

        assertNotNull(obj);
        assertEquals(1, createCount.get(), "creator should have been called exactly once");
    }

    // -----------------------------------------------------------------------
    // 2.5.2  free() then obtain() — returns the recycled instance, no new alloc
    // -----------------------------------------------------------------------
    @Test
    void obtain_afterFree_returnsRecycledInstance() {
        AtomicInteger createCount = new AtomicInteger(0);
        ObjectPool<StringBuilder> pool = new ObjectPool<>(() -> {
            createCount.incrementAndGet();
            return new StringBuilder();
        });

        StringBuilder original = pool.obtain();  // allocates (count → 1)
        pool.free(original);
        StringBuilder recycled = pool.obtain();  // should come from pool

        assertSame(original, recycled, "obtain() after free() must return the same instance");
        assertEquals(1, createCount.get(), "creator must not be called a second time");
    }

    // -----------------------------------------------------------------------
    // 2.5.3  Multiple free/obtain cycles — pool grows and shrinks correctly
    // -----------------------------------------------------------------------
    @Test
    void multipleFreeThenObtain_drainsProperly() {
        ObjectPool<StringBuilder> pool = new ObjectPool<>(StringBuilder::new);

        StringBuilder a = pool.obtain();
        StringBuilder b = pool.obtain();
        StringBuilder c = pool.obtain();

        pool.free(a);
        pool.free(b);
        pool.free(c);

        // Drain all three — must come from the pool (no additional allocations)
        List<StringBuilder> drained = new ArrayList<>();
        drained.add(pool.obtain());
        drained.add(pool.obtain());
        drained.add(pool.obtain());

        assertTrue(drained.contains(a), "a should be recycled");
        assertTrue(drained.contains(b), "b should be recycled");
        assertTrue(drained.contains(c), "c should be recycled");

        // Pool is now empty; a fourth obtain must create a new object
        StringBuilder extra = pool.obtain();
        assertFalse(drained.contains(extra), "extra object should be freshly created");
    }

    // -----------------------------------------------------------------------
    // 2.5.4  Concurrent obtain/free — pool survives parallel access without
    //         throwing or losing objects (basic thread-safety via
    //         ConcurrentLinkedQueue backing)
    // -----------------------------------------------------------------------
    @Test
    void concurrentObtainAndFree_doesNotCorruptPool() throws InterruptedException {
        ObjectPool<StringBuilder> pool = new ObjectPool<>(StringBuilder::new);
        int threadCount = 8;
        int iterationsPerThread = 200;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        StringBuilder obj = pool.obtain();
                        if (obj == null) errorCount.incrementAndGet();
                        pool.free(obj);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "executor did not finish in time");
        assertEquals(0, errorCount.get(),
                "concurrent obtain/free produced errors or null objects");
    }
}
