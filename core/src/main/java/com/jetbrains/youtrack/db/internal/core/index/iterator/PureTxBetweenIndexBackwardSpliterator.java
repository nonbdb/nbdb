package com.jetbrains.youtrack.db.internal.core.index.iterator;

import com.jetbrains.youtrack.db.internal.common.comparator.ODefaultComparator;
import com.jetbrains.youtrack.db.internal.common.util.ORawPair;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.index.OIndexOneValue;
import com.jetbrains.youtrack.db.internal.core.tx.OTransactionIndexChanges;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PureTxBetweenIndexBackwardSpliterator implements Spliterator<ORawPair<Object, YTRID>> {

  /**
   *
   */
  private final OIndexOneValue oIndexTxAwareOneValue;

  private final OTransactionIndexChanges indexChanges;
  private Object firstKey;

  private Object nextKey;

  public PureTxBetweenIndexBackwardSpliterator(
      OIndexOneValue oIndexTxAwareOneValue,
      Object fromKey,
      boolean fromInclusive,
      Object toKey,
      boolean toInclusive,
      OTransactionIndexChanges indexChanges) {
    this.oIndexTxAwareOneValue = oIndexTxAwareOneValue;
    this.indexChanges = indexChanges;

    if (fromKey != null) {
      fromKey =
          this.oIndexTxAwareOneValue.enhanceFromCompositeKeyBetweenDesc(fromKey, fromInclusive);
    }
    if (toKey != null) {
      toKey = this.oIndexTxAwareOneValue.enhanceToCompositeKeyBetweenDesc(toKey, toInclusive);
    }

    final Object[] keys = indexChanges.firstAndLastKeys(fromKey, fromInclusive, toKey, toInclusive);
    if (keys.length == 0) {
      nextKey = null;
    } else {
      firstKey = keys[0];
      nextKey = keys[1];
    }
  }

  @Override
  public boolean tryAdvance(Consumer<? super ORawPair<Object, YTRID>> action) {
    if (nextKey == null) {
      return false;
    }

    ORawPair<Object, YTRID> result;
    do {
      result = this.oIndexTxAwareOneValue.calculateTxIndexEntry(nextKey, null, indexChanges);
      nextKey = indexChanges.getLowerKey(nextKey);

      if (nextKey != null && ODefaultComparator.INSTANCE.compare(nextKey, firstKey) < 0) {
        nextKey = null;
      }
    } while (result == null && nextKey != null);

    if (result == null) {
      return false;
    }

    action.accept(result);
    return true;
  }

  @Override
  public Spliterator<ORawPair<Object, YTRID>> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return NONNULL | SORTED | ORDERED;
  }

  @Override
  public Comparator<? super ORawPair<Object, YTRID>> getComparator() {
    return (entryOne, entryTwo) ->
        -ODefaultComparator.INSTANCE.compare(entryOne.first, entryTwo.first);
  }
}