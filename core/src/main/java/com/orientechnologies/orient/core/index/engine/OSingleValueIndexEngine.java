package com.orientechnologies.orient.core.index.engine;

import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import java.io.IOException;

public interface OSingleValueIndexEngine extends OV1IndexEngine {

  boolean validatedPut(
      OAtomicOperation atomicOperation,
      Object key,
      YTRID value,
      IndexEngineValidator<Object, YTRID> validator);

  boolean remove(OAtomicOperation atomicOperation, Object key) throws IOException;

  @Override
  default boolean isMultiValue() {
    return false;
  }
}
