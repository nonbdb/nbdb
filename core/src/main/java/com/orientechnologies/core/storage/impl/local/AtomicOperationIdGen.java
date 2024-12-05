package com.orientechnologies.core.storage.impl.local;

import java.util.concurrent.atomic.AtomicLong;

public final class AtomicOperationIdGen {

  private final AtomicLong idGen = new AtomicLong();

  public long nextId() {
    return idGen.incrementAndGet();
  }

  public void setStartId(final long id) {
    idGen.set(id);
  }

  public long getLastId() {
    return idGen.get();
  }
}
