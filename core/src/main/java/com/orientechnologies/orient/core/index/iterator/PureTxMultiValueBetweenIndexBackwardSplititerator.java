package com.orientechnologies.orient.core.index.iterator;

import com.orientechnologies.common.comparator.ODefaultComparator;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.index.OIndexMultiValues;
import com.orientechnologies.orient.core.iterator.OEmptyIterator;
import com.orientechnologies.orient.core.tx.OTransactionIndexChanges;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PureTxMultiValueBetweenIndexBackwardSplititerator
    implements Spliterator<ORawPair<Object, YTRID>> {

  /**
   *
   */
  private final OIndexMultiValues oIndexTxAwareMultiValue;

  private final OTransactionIndexChanges indexChanges;
  private Object firstKey;

  private Object nextKey;

  private Iterator<YTIdentifiable> valuesIterator = new OEmptyIterator<>();
  private Object key;

  public PureTxMultiValueBetweenIndexBackwardSplititerator(
      OIndexMultiValues oIndexTxAwareMultiValue,
      Object fromKey,
      boolean fromInclusive,
      Object toKey,
      boolean toInclusive,
      OTransactionIndexChanges indexChanges) {
    this.oIndexTxAwareMultiValue = oIndexTxAwareMultiValue;
    this.indexChanges = indexChanges;

    if (fromKey != null) {
      fromKey =
          this.oIndexTxAwareMultiValue.enhanceFromCompositeKeyBetweenDesc(fromKey, fromInclusive);
    }
    if (toKey != null) {
      toKey = this.oIndexTxAwareMultiValue.enhanceToCompositeKeyBetweenDesc(toKey, toInclusive);
    }

    final Object[] keys = indexChanges.firstAndLastKeys(fromKey, fromInclusive, toKey, toInclusive);
    if (keys.length == 0) {
      nextKey = null;
    } else {
      firstKey = keys[0];
      nextKey = keys[1];
    }
  }

  private ORawPair<Object, YTRID> nextEntryInternal() {
    final YTIdentifiable identifiable = valuesIterator.next();
    return new ORawPair<>(key, identifiable.getIdentity());
  }

  @Override
  public boolean tryAdvance(Consumer<? super ORawPair<Object, YTRID>> action) {
    if (valuesIterator.hasNext()) {
      final ORawPair<Object, YTRID> entry = nextEntryInternal();
      action.accept(entry);
      return true;
    }

    if (nextKey == null) {
      return false;
    }

    Set<YTIdentifiable> result;
    do {
      result = OIndexMultiValues.calculateTxValue(nextKey, indexChanges);
      key = nextKey;

      nextKey = indexChanges.getLowerKey(nextKey);

      if (nextKey != null && ODefaultComparator.INSTANCE.compare(nextKey, firstKey) < 0) {
        nextKey = null;
      }
    } while ((result == null || result.isEmpty()) && nextKey != null);

    if (result == null || result.isEmpty()) {
      return false;
    }

    valuesIterator = result.iterator();
    final ORawPair<Object, YTRID> entry = nextEntryInternal();
    action.accept(entry);
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
    return NONNULL | ORDERED | SORTED;
  }

  @Override
  public Comparator<? super ORawPair<Object, YTRID>> getComparator() {
    return (entryOne, entryTwo) ->
        -ODefaultComparator.INSTANCE.compare(entryOne.first, entryTwo.first);
  }
}
