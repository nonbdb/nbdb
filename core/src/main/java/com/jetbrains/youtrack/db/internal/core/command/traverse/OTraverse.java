/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.core.command.traverse;

import com.jetbrains.youtrack.db.internal.core.command.CommandExecutorAbstract;
import com.jetbrains.youtrack.db.internal.core.command.OCommand;
import com.jetbrains.youtrack.db.internal.core.command.OCommandPredicate;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for traversing.
 */
public class OTraverse implements OCommand, Iterable<YTIdentifiable>, Iterator<YTIdentifiable> {

  private OCommandPredicate predicate;
  private Iterator<? extends YTIdentifiable> target;
  private final List<Object> fields = new ArrayList<Object>();
  private long resultCount = 0;
  private long limit = 0;
  private YTIdentifiable lastTraversed;
  private STRATEGY strategy = STRATEGY.DEPTH_FIRST;
  private final TraverseContext context = new TraverseContext();
  private int maxDepth = -1;

  public OTraverse(YTDatabaseSessionInternal db) {
    context.setDatabase(db);
  }

  public enum STRATEGY {
    DEPTH_FIRST,
    BREADTH_FIRST
  }

  /*
   * Executes a traverse collecting all the result in the returning List<YTIdentifiable>. This could be memory expensive because for
   * large results the list could be huge. it's always better to use it as an Iterable and lazy fetch each result on next() call.
   *
   * @see com.orientechnologies.core.command.OCommand#execute()
   */
  public List<YTIdentifiable> execute(YTDatabaseSessionInternal session) {
    context.setDatabase(session);
    final List<YTIdentifiable> result = new ArrayList<>();

    while (hasNext()) {
      result.add(next());
    }

    return result;
  }

  public OTraverseAbstractProcess<?> nextProcess() {
    return context.next();
  }

  public boolean hasNext() {
    if (limit > 0 && resultCount >= limit) {
      return false;
    }

    if (lastTraversed == null)
    // GET THE NEXT
    {
      lastTraversed = next();
    }

    if (lastTraversed == null && !context.isEmpty()) {
      throw new IllegalStateException("Traverse ended abnormally");
    }

    if (!CommandExecutorAbstract.checkInterruption(context)) {
      return false;
    }

    // BROWSE ALL THE RECORDS
    return lastTraversed != null;
  }

  public YTIdentifiable next() {
    if (Thread.interrupted()) {
      throw new YTCommandExecutionException("The traverse execution has been interrupted");
    }

    if (lastTraversed != null) {
      // RETURN LATEST AND RESET IT
      final YTIdentifiable result = lastTraversed;
      lastTraversed = null;
      return result;
    }

    if (limit > 0 && resultCount >= limit) {
      return null;
    }

    YTIdentifiable result;
    OTraverseAbstractProcess<?> toProcess;
    // RESUME THE LAST PROCESS
    while ((toProcess = nextProcess()) != null) {
      result = toProcess.process();
      if (result != null) {
        resultCount++;
        return result;
      }
    }

    return null;
  }

  public void remove() {
    throw new UnsupportedOperationException("remove()");
  }

  public Iterator<YTIdentifiable> iterator() {
    return this;
  }

  public TraverseContext getContext() {
    return context;
  }

  public OTraverse target(final Iterable<? extends YTIdentifiable> iTarget) {
    return target(iTarget.iterator());
  }

  public OTraverse target(final YTIdentifiable... iRecords) {
    final List<YTIdentifiable> list = new ArrayList<YTIdentifiable>();
    Collections.addAll(list, iRecords);
    return target(list.iterator());
  }

  @SuppressWarnings("unchecked")
  public OTraverse target(final Iterator<? extends YTIdentifiable> iTarget) {
    target = iTarget;
    context.reset();
    new OTraverseRecordSetProcess(this, (Iterator<YTIdentifiable>) target, OTraversePath.empty());
    return this;
  }

  public Iterator<? extends YTIdentifiable> getTarget() {
    return target;
  }

  public OTraverse predicate(final OCommandPredicate iPredicate) {
    predicate = iPredicate;
    return this;
  }

  public OCommandPredicate getPredicate() {
    return predicate;
  }

  public OTraverse field(final Object iField) {
    if (!fields.contains(iField)) {
      fields.add(iField);
    }
    return this;
  }

  public OTraverse fields(final Collection<Object> iFields) {
    for (Object f : iFields) {
      field(f);
    }
    return this;
  }

  public OTraverse fields(final String... iFields) {
    for (String f : iFields) {
      field(f);
    }
    return this;
  }

  public List<Object> getFields() {
    return fields;
  }

  public long getLimit() {
    return limit;
  }

  public OTraverse limit(final long iLimit) {
    if (iLimit < -1) {
      throw new IllegalArgumentException("Limit cannot be negative. 0 = infinite");
    }
    this.limit = iLimit;
    return this;
  }

  @Override
  public String toString() {
    return String.format(
        "OTraverse.target(%s).fields(%s).limit(%d).predicate(%s)",
        target, fields, limit, predicate);
  }

  public long getResultCount() {
    return resultCount;
  }

  public YTIdentifiable getLastTraversed() {
    return lastTraversed;
  }

  public STRATEGY getStrategy() {
    return strategy;
  }

  public void setStrategy(STRATEGY strategy) {
    this.strategy = strategy;
    context.setStrategy(strategy);
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public void setMaxDepth(final int maxDepth) {
    this.maxDepth = maxDepth;
  }
}
