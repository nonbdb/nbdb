package com.jetbrains.youtrack.db.internal.core.metadata.security;

import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.sql.OSQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.YTCommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.parser.OOrBlock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is an LRU cache for already parsed SQL statement executors. It stores itself in the
 * storage as a resource. It also acts an an entry point for the SQL parser.
 */
public class OPredicateCache {

  private final Map<String, OOrBlock> map;
  private final int mapSize;

  /**
   * @param size the size of the cache
   */
  public OPredicateCache(int size) {
    this.mapSize = size;
    map =
        new LinkedHashMap<String, OOrBlock>(size) {
          protected boolean removeEldestEntry(final Map.Entry<String, OOrBlock> eldest) {
            return super.size() > mapSize;
          }
        };
  }

  /**
   * @param statement an SQL statement
   * @return true if the corresponding executor is present in the cache
   */
  public boolean contains(String statement) {
    if (GlobalConfiguration.STATEMENT_CACHE_SIZE.getValueAsInteger() == 0) {
      return false;
    }

    synchronized (map) {
      return map.containsKey(statement);
    }
  }

  /**
   * @param statement an SQL statement
   * @return the corresponding executor, taking it from the internal cache, if it exists
   */
  public OOrBlock get(String statement) {
    if (GlobalConfiguration.STATEMENT_CACHE_SIZE.getValueAsInteger() == 0) {
      return parse(statement);
    }

    OOrBlock result;
    synchronized (map) {
      // LRU
      result = map.remove(statement);
      if (result != null) {
        map.put(statement, result);
      }
    }
    if (result == null) {
      result = parse(statement);
      synchronized (map) {
        map.put(statement, result);
      }
    }
    return result.copy();
  }

  protected static OOrBlock parse(String statement) throws YTCommandSQLParsingException {
    return OSQLEngine.parsePredicate(statement);
  }

  public void clear() {
    if (GlobalConfiguration.STATEMENT_CACHE_SIZE.getValueAsInteger() == 0) {
      return;
    }

    synchronized (map) {
      map.clear();
    }
  }
}