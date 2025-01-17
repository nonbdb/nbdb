package com.jetbrains.youtrack.db.internal.common.directmemory;

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.common.directmemory.DirectMemoryAllocator.Intention;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ByteBufferPoolTest {

  @BeforeClass
  public static void beforeClass() {
    GlobalConfiguration.DIRECT_MEMORY_TRACK_MODE.setValue(true);
  }

  @AfterClass
  public static void afterClass() {
    GlobalConfiguration.DIRECT_MEMORY_TRACK_MODE.setValue(false);
  }

  @Test
  public void testByteBufferAllocationZeroPool() {
    final DirectMemoryAllocator allocator = new DirectMemoryAllocator();
    final ByteBufferPool byteBufferPool = new ByteBufferPool(42, allocator, 0);

    final Pointer pointerOne = byteBufferPool.acquireDirect(false, Intention.TEST);
    Assert.assertEquals(42, pointerOne.getNativeByteBuffer().capacity());
    Assert.assertEquals(42, allocator.getMemoryConsumption());

    Assert.assertEquals(0, byteBufferPool.getPoolSize());

    final Pointer pointerTwo = byteBufferPool.acquireDirect(true, Intention.TEST);
    Assert.assertEquals(42, pointerTwo.getNativeByteBuffer().capacity());
    Assert.assertEquals(84, allocator.getMemoryConsumption());

    assertBufferIsClear(pointerTwo.getNativeByteBuffer());

    byteBufferPool.release(pointerOne);
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(42, allocator.getMemoryConsumption());

    byteBufferPool.release(pointerTwo);
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(0, allocator.getMemoryConsumption());

    byteBufferPool.clear();
    byteBufferPool.checkMemoryLeaks();
  }

  @Test
  public void testByteBufferAllocationTwoPagesPool() {
    final DirectMemoryAllocator allocator = new DirectMemoryAllocator();
    final ByteBufferPool byteBufferPool = new ByteBufferPool(42, allocator, 2);

    Pointer pointerOne = byteBufferPool.acquireDirect(false, Intention.TEST);

    Assert.assertEquals(42, pointerOne.getNativeByteBuffer().capacity());
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(42, allocator.getMemoryConsumption());

    Pointer pointerTwo = byteBufferPool.acquireDirect(true, Intention.TEST);
    Assert.assertEquals(42, pointerTwo.getNativeByteBuffer().capacity());
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(84, allocator.getMemoryConsumption());

    assertBufferIsClear(pointerTwo.getNativeByteBuffer());

    Pointer pointerThree = byteBufferPool.acquireDirect(false, Intention.TEST);

    Assert.assertEquals(42, pointerThree.getNativeByteBuffer().capacity());
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    byteBufferPool.release(pointerOne);

    Assert.assertEquals(1, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    byteBufferPool.release(pointerTwo);

    Assert.assertEquals(2, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    byteBufferPool.release(pointerThree);

    Assert.assertEquals(2, byteBufferPool.getPoolSize());
    Assert.assertEquals(84, allocator.getMemoryConsumption());

    pointerOne = byteBufferPool.acquireDirect(true, Intention.TEST);

    Assert.assertEquals(42, pointerOne.getNativeByteBuffer().capacity());
    Assert.assertEquals(1, byteBufferPool.getPoolSize());
    Assert.assertEquals(84, allocator.getMemoryConsumption());

    assertBufferIsClear(pointerOne.getNativeByteBuffer());

    pointerTwo = byteBufferPool.acquireDirect(true, Intention.TEST);

    Assert.assertEquals(42, pointerTwo.getNativeByteBuffer().capacity());
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(84, allocator.getMemoryConsumption());

    assertBufferIsClear(pointerTwo.getNativeByteBuffer());

    pointerThree = byteBufferPool.acquireDirect(false, Intention.TEST);

    Assert.assertEquals(42, pointerThree.getNativeByteBuffer().capacity());
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    byteBufferPool.release(pointerThree);

    Assert.assertEquals(1, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    pointerThree = byteBufferPool.acquireDirect(true, Intention.TEST);

    Assert.assertEquals(42, pointerThree.getNativeByteBuffer().capacity());
    Assert.assertEquals(0, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    assertBufferIsClear(pointerThree.getNativeByteBuffer());

    byteBufferPool.release(pointerThree);

    Assert.assertEquals(1, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    byteBufferPool.release(pointerOne);

    Assert.assertEquals(2, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    Assert.assertEquals(2, byteBufferPool.getPoolSize());
    Assert.assertEquals(126, allocator.getMemoryConsumption());

    byteBufferPool.release(pointerTwo);

    Assert.assertEquals(2, byteBufferPool.getPoolSize());
    Assert.assertEquals(84, allocator.getMemoryConsumption());

    byteBufferPool.clear();

    Assert.assertEquals(0, allocator.getMemoryConsumption());
    Assert.assertEquals(0, byteBufferPool.getPoolSize());

    byteBufferPool.checkMemoryLeaks();
  }

  @Test
  @Ignore
  public void mtTest() throws Exception {
    final DirectMemoryAllocator allocator = new DirectMemoryAllocator();
    final ByteBufferPool byteBufferPool = new ByteBufferPool(42, allocator, 600 * 8);
    final List<Future<Void>> futures = new ArrayList<>();
    final AtomicBoolean stop = new AtomicBoolean();

    final ExecutorService executorService = Executors.newCachedThreadPool();
    for (int i = 0; i < 8; i++) {
      futures.add(executorService.submit(new Allocator(byteBufferPool, stop)));
    }

    Thread.sleep(5 * 60 * 1000);

    stop.set(true);

    for (Future<Void> future : futures) {
      future.get();
    }

    byteBufferPool.clear();

    byteBufferPool.checkMemoryLeaks();
    allocator.checkMemoryLeaks();
  }

  private void assertBufferIsClear(ByteBuffer bufferTwo) {
    while (bufferTwo.position() < bufferTwo.capacity()) {
      Assert.assertEquals(0, bufferTwo.get());
    }
  }

  private static final class Allocator implements Callable<Void> {

    private final ByteBufferPool pool;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final AtomicBoolean stop;
    private final List<Pointer> allocatedPointers = new ArrayList<>();

    private Allocator(ByteBufferPool pool, AtomicBoolean stop) {
      this.pool = pool;
      this.stop = stop;
    }

    @Override
    public Void call() {
      try {
        while (!stop.get()) {
          if (allocatedPointers.size() < 500) {
            Pointer pointer = pool.acquireDirect(false, Intention.TEST);
            allocatedPointers.add(pointer);
          } else if (allocatedPointers.size() < 1000) {
            if (random.nextDouble() <= 0.5) {
              Pointer pointer = pool.acquireDirect(false, Intention.TEST);
              allocatedPointers.add(pointer);
            } else {
              final int bufferToRemove = random.nextInt(allocatedPointers.size());
              final Pointer pointer = allocatedPointers.remove(bufferToRemove);
              pool.release(pointer);
            }
          } else {
            if (random.nextDouble() <= 0.4) {
              Pointer pointer = pool.acquireDirect(false, Intention.TEST);
              allocatedPointers.add(pointer);
            } else {
              final int bufferToRemove = random.nextInt(allocatedPointers.size());
              final Pointer pointer = allocatedPointers.remove(bufferToRemove);
              pool.release(pointer);
            }
          }
        }

        System.out.println("Allocated buffers " + allocatedPointers.size());
        for (Pointer pointer : allocatedPointers) {
          pool.release(pointer);
        }
      } catch (Exception | Error e) {
        e.printStackTrace();
        throw e;
      }

      return null;
    }
  }
}
