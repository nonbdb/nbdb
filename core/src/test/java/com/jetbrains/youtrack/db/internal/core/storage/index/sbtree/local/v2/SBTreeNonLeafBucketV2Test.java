package com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.local.v2;

import com.jetbrains.youtrack.db.internal.common.directmemory.OByteBufferPool;
import com.jetbrains.youtrack.db.internal.common.directmemory.ODirectMemoryAllocator.Intention;
import com.jetbrains.youtrack.db.internal.common.directmemory.OPointer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.OLongSerializer;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.OLinkSerializer;
import com.jetbrains.youtrack.db.internal.core.storage.cache.OCacheEntry;
import com.jetbrains.youtrack.db.internal.core.storage.cache.OCacheEntryImpl;
import com.jetbrains.youtrack.db.internal.core.storage.cache.OCachePointer;
import com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.local.v2.OSBTreeBucketV2;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @since 12.08.13
 */
public class SBTreeNonLeafBucketV2Test {

  @Test
  public void testInitialization() {
    final OByteBufferPool bufferPool = OByteBufferPool.instance(null);
    final OPointer pointer = bufferPool.acquireDirect(true, Intention.TEST);

    OCachePointer cachePointer = new OCachePointer(pointer, bufferPool, 0, 0);
    OCacheEntry cacheEntry = new OCacheEntryImpl(0, 0, cachePointer, false, null);
    cacheEntry.acquireExclusiveLock();
    cachePointer.incrementReferrer();

    OSBTreeBucketV2<Long, YTIdentifiable> treeBucket = new OSBTreeBucketV2<>(cacheEntry);
    treeBucket.init(false);

    Assert.assertEquals(treeBucket.size(), 0);
    Assert.assertFalse(treeBucket.isLeaf());

    treeBucket = new OSBTreeBucketV2<>(cacheEntry);
    Assert.assertEquals(treeBucket.size(), 0);
    Assert.assertFalse(treeBucket.isLeaf());
    Assert.assertEquals(treeBucket.getLeftSibling(), -1);
    Assert.assertEquals(treeBucket.getRightSibling(), -1);

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }

  @Test
  public void testSearch() {
    long seed = System.currentTimeMillis();
    System.out.println("testSearch seed : " + seed);

    TreeSet<Long> keys = new TreeSet<>();
    Random random = new Random(seed);

    while (keys.size() < 2 * OSBTreeBucketV2.MAX_PAGE_SIZE_BYTES / OLongSerializer.LONG_SIZE) {
      keys.add(random.nextLong());
    }

    final OByteBufferPool bufferPool = OByteBufferPool.instance(null);
    final OPointer pointer = bufferPool.acquireDirect(true, Intention.TEST);

    OCachePointer cachePointer = new OCachePointer(pointer, bufferPool, 0, 0);
    OCacheEntry cacheEntry = new OCacheEntryImpl(0, 0, cachePointer, false, null);
    cacheEntry.acquireExclusiveLock();
    cachePointer.incrementReferrer();

    OSBTreeBucketV2<Long, YTIdentifiable> treeBucket = new OSBTreeBucketV2<>(cacheEntry);
    treeBucket.init(false);

    int index = 0;
    Map<Long, Integer> keyIndexMap = new HashMap<>();
    for (Long key : keys) {
      if (!treeBucket.addNonLeafEntry(
          index,
          OLongSerializer.INSTANCE.serializeNativeAsWhole(key),
          random.nextInt(Integer.MAX_VALUE),
          random.nextInt(Integer.MAX_VALUE),
          true)) {
        break;
      }

      keyIndexMap.put(key, index);
      index++;
    }

    Assert.assertEquals(treeBucket.size(), keyIndexMap.size());

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      int bucketIndex = treeBucket.find(keyIndexEntry.getKey(), OLongSerializer.INSTANCE);
      Assert.assertEquals(bucketIndex, (int) keyIndexEntry.getValue());
    }

    long prevRight = -1;
    for (int i = 0; i < treeBucket.size(); i++) {
      OSBTreeBucketV2.SBTreeEntry<Long, YTIdentifiable> entry =
          treeBucket.getEntry(i, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);
      if (prevRight > 0) {
        Assert.assertEquals(entry.leftChild, prevRight);
      }

      prevRight = entry.rightChild;
    }

    long prevLeft = -1;
    for (int i = treeBucket.size() - 1; i >= 0; i--) {
      OSBTreeBucketV2.SBTreeEntry<Long, YTIdentifiable> entry =
          treeBucket.getEntry(i, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);

      if (prevLeft > 0) {
        Assert.assertEquals(entry.rightChild, prevLeft);
      }

      prevLeft = entry.leftChild;
    }

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }

  @Test
  public void testShrink() {
    long seed = System.currentTimeMillis();
    System.out.println("testShrink seed : " + seed);

    TreeSet<Long> keys = new TreeSet<>();
    Random random = new Random(seed);

    while (keys.size() < 2 * OSBTreeBucketV2.MAX_PAGE_SIZE_BYTES / OLongSerializer.LONG_SIZE) {
      keys.add(random.nextLong());
    }

    final OByteBufferPool bufferPool = OByteBufferPool.instance(null);
    final OPointer pointer = bufferPool.acquireDirect(true, Intention.TEST);

    OCachePointer cachePointer = new OCachePointer(pointer, bufferPool, 0, 0);
    OCacheEntry cacheEntry = new OCacheEntryImpl(0, 0, cachePointer, false, null);
    cacheEntry.acquireExclusiveLock();

    cachePointer.incrementReferrer();

    OSBTreeBucketV2<Long, YTIdentifiable> treeBucket = new OSBTreeBucketV2<>(cacheEntry);
    treeBucket.init(false);

    int index = 0;
    for (Long key : keys) {
      if (!treeBucket.addNonLeafEntry(
          index, OLongSerializer.INSTANCE.serializeNativeAsWhole(key), index, index + 1, true)) {
        break;
      }

      index++;
    }

    int originalSize = treeBucket.size();

    treeBucket.shrink(treeBucket.size() / 2, OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);
    Assert.assertEquals(treeBucket.size(), index / 2);

    index = 0;
    final Map<Long, Integer> keyIndexMap = new HashMap<>();

    Iterator<Long> keysIterator = keys.iterator();
    while (keysIterator.hasNext() && index < treeBucket.size()) {
      Long key = keysIterator.next();
      keyIndexMap.put(key, index);
      index++;
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      int bucketIndex = treeBucket.find(keyIndexEntry.getKey(), OLongSerializer.INSTANCE);
      Assert.assertEquals(bucketIndex, (int) keyIndexEntry.getValue());
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      OSBTreeBucketV2.SBTreeEntry<Long, YTIdentifiable> entry =
          treeBucket.getEntry(
              keyIndexEntry.getValue(), OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);

      Assert.assertEquals(
          entry,
          new OSBTreeBucketV2.SBTreeEntry<Long, YTIdentifiable>(
              keyIndexEntry.getValue(),
              keyIndexEntry.getValue() + 1,
              keyIndexEntry.getKey(),
              null));
    }

    int keysToAdd = originalSize - treeBucket.size();
    int addedKeys = 0;
    while (keysIterator.hasNext() && index < originalSize) {
      Long key = keysIterator.next();

      if (!treeBucket.addNonLeafEntry(
          index, OLongSerializer.INSTANCE.serializeNativeAsWhole(key), index, index + 1, true)) {
        break;
      }

      keyIndexMap.put(key, index);
      index++;
      addedKeys++;
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      OSBTreeBucketV2.SBTreeEntry<Long, YTIdentifiable> entry =
          treeBucket.getEntry(
              keyIndexEntry.getValue(), OLongSerializer.INSTANCE, OLinkSerializer.INSTANCE);

      Assert.assertEquals(
          entry,
          new OSBTreeBucketV2.SBTreeEntry<Long, YTIdentifiable>(
              keyIndexEntry.getValue(),
              keyIndexEntry.getValue() + 1,
              keyIndexEntry.getKey(),
              null));
    }

    Assert.assertEquals(treeBucket.size(), originalSize);
    Assert.assertEquals(addedKeys, keysToAdd);

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }
}