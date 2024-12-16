package com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.multivalue.v2;

import com.jetbrains.youtrack.db.internal.core.index.engine.IndexEngineValidator;

class IndexEngineValidatorNullIncrement implements IndexEngineValidator<MultiValueEntry, Byte> {

  private final CellBTreeMultiValueV2NullBucket nullBucket;

  IndexEngineValidatorNullIncrement(CellBTreeMultiValueV2NullBucket nullBucket) {
    this.nullBucket = nullBucket;
  }

  @Override
  public Object validate(MultiValueEntry k, Byte ov, Byte v) {
    if (ov != null) {
      return IndexEngineValidator.IGNORE;
    }

    nullBucket.incrementSize();
    return v;
  }
}
