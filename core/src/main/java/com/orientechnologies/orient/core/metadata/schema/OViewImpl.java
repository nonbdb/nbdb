package com.orientechnologies.orient.core.metadata.schema;

import com.orientechnologies.orient.core.collate.OCollate;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManagerAbstract;
import com.orientechnologies.orient.core.index.OPropertyMapIndexDefinition.INDEX_BY;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class OViewImpl extends OClassImpl implements OView {

  private OViewConfig cfg;
  private Set<String> activeIndexNames = new HashSet<>();
  protected long lastRefreshTime = 0;

  protected OViewImpl(OSchemaShared iOwner, String iName, OViewConfig cfg, int[] iClusterIds) {
    super(iOwner, iName, iClusterIds);
    this.cfg = cfg.copy();
  }

  protected OViewImpl(OSchemaShared iOwner, String iName) {
    super(iOwner, iName);
  }

  @Override
  public void fromStream(ODocument document) {
    super.fromStream(document);
    String query = document.getProperty("query");
    this.cfg = new OViewConfig(getName(), query);
    this.cfg.setUpdatable(Boolean.TRUE.equals(document.getProperty("updatable")));

    if (document.getProperty("indexes") instanceof List) {
      List<Map<String, Object>> idxData = document.getProperty("indexes");
      for (Map<String, Object> idx : idxData) {
        String type = (String) idx.get("type");
        String engine = (String) idx.get("engine");
        OViewIndexConfig indexConfig = this.cfg.addIndex(type, engine);
        if (idx.get("properties") instanceof Map) {
          Map<String, Object> props = (Map<String, Object>) idx.get("properties");
          for (Map.Entry<String, Object> prop : props.entrySet()) {
            OType proType = null;
            OType linkedType = null;
            String collateName = null;
            INDEX_BY indexBy = null;
            if (prop.getValue() instanceof Map) {
              Map<String, Object> value = (Map<String, Object>) prop.getValue();
              if (value.size() > 0 && value.get("type") != null) {
                proType = OType.valueOf(value.get("type").toString());
              }
              if (value.size() > 1 && value.get("linkedType") != null) {
                linkedType = OType.valueOf(value.get("linkedType").toString());
              }
              if (value.size() > 1 && value.get("collate") != null) {
                collateName = value.get("collate").toString();
              }
              if (value.size() > 1 && value.get("collate") != null) {
                indexBy = INDEX_BY.valueOf(value.get("collate").toString().toUpperCase());
              }
            } else {
              if (prop.getValue() != null) {
                proType = OType.valueOf(prop.getValue().toString());
              }
            }
            OCollate collate = OSQLEngine.getCollate(collateName);
            indexConfig.addProperty(prop.getKey(), proType, linkedType, collate, indexBy);
          }
        }
      }
    }
    if (document.getProperty("updateIntervalSeconds") instanceof Integer) {
      cfg.setUpdateIntervalSeconds(document.getProperty("updateIntervalSeconds"));
    }
    if (document.getProperty("updateStrategy") instanceof String) {
      cfg.setUpdateStrategy(document.getProperty("updateStrategy"));
    }
    if (document.getProperty("watchClasses") instanceof List) {
      cfg.setWatchClasses(document.getProperty("watchClasses"));
    }
    if (document.getProperty("originRidField") instanceof String) {
      cfg.setOriginRidField(document.getProperty("originRidField"));
    }
    if (document.getProperty("nodes") instanceof List) {
      cfg.setNodes(document.getProperty("nodes"));
    }
    if (document.getProperty("activeIndexNames") instanceof Set) {
      activeIndexNames = document.getProperty("activeIndexNames");
    }
    if (document.getProperty("lastRefreshTime") != null) {
      lastRefreshTime = document.getProperty("lastRefreshTime");
    }
  }

  @Override
  public ODocument toStream() {
    ODocument result = super.toStream();
    result.setProperty("query", cfg.getQuery());
    result.setProperty("updatable", cfg.isUpdatable());

    List<Map<String, Object>> indexes = new ArrayList<>();
    for (OViewIndexConfig idx : cfg.indexes) {
      Map<String, Object> indexDescriptor = new HashMap<>();
      indexDescriptor.put("type", idx.type);
      indexDescriptor.put("engine", idx.engine);
      Map<String, Object> properties = new HashMap<>();
      for (OIndexConfigProperty s : idx.props) {
        HashMap<String, Object> entry = new HashMap<>();
        entry.put("type", s.getType().toString());
        if (s.getLinkedType() != null) {
          entry.put("linkedType", s.getLinkedType().toString());
        }
        if (s.getIndexBy() != null) {
          entry.put("indexBy", s.getIndexBy().toString());
        }
        if (s.getCollate() != null) {
          entry.put("collate", s.getCollate().getName());
        }
        properties.put(s.getName(), entry);
      }
      indexDescriptor.put("properties", properties);
      indexes.add(indexDescriptor);
    }
    result.setProperty("indexes", indexes);
    result.setProperty("updateIntervalSeconds", cfg.getUpdateIntervalSeconds());
    result.setProperty("updateStrategy", cfg.getUpdateStrategy());
    result.setProperty("watchClasses", cfg.getWatchClasses());
    result.setProperty("originRidField", cfg.getOriginRidField());
    result.setProperty("nodes", cfg.getNodes());
    result.setProperty("activeIndexNames", activeIndexNames);
    result.setProperty("lastRefreshTime", lastRefreshTime);
    return result;
  }

  @Override
  public ODocument toNetworkStream() {
    ODocument result = super.toNetworkStream();
    result.setProperty("query", cfg.getQuery());
    result.setProperty("updatable", cfg.isUpdatable());
    List<Map<String, Object>> indexes = new ArrayList<>();
    for (OViewIndexConfig idx : cfg.indexes) {
      Map<String, Object> indexDescriptor = new HashMap<>();
      indexDescriptor.put("type", idx.type);
      indexDescriptor.put("engine", idx.engine);
      Map<String, Object> properties = new HashMap<>();
      for (OIndexConfigProperty s : idx.props) {
        HashMap<String, Object> entry = new HashMap<>();
        entry.put("type", s.getType().toString());
        if (s.getLinkedType() != null) {
          entry.put("linkedType", s.getLinkedType().toString());
        }
        if (s.getIndexBy() != null) {
          entry.put("indexBy", s.getIndexBy().toString());
        }
        if (s.getCollate() != null) {
          entry.put("collate", s.getCollate().getName());
        }
        properties.put(s.getName(), entry);
        indexDescriptor.put("properties", properties);
      }
      indexes.add(indexDescriptor);
    }
    result.setProperty("indexes", indexes);
    result.setProperty("updateIntervalSeconds", cfg.getUpdateIntervalSeconds());
    result.setProperty("updateStrategy", cfg.getUpdateStrategy());
    result.setProperty("watchClasses", cfg.getWatchClasses());
    result.setProperty("originRidField", cfg.getOriginRidField());
    result.setProperty("nodes", cfg.getNodes());
    result.setProperty("activeIndexNames", activeIndexNames);
    result.setProperty("lastRefreshTime", lastRefreshTime);
    return result;
  }

  @Override
  public String getQuery() {
    return cfg.getQuery();
  }

  public long count(final boolean isPolymorphic) {
    acquireSchemaReadLock();
    try {
      return getDatabase().countView(getName());
    } finally {
      releaseSchemaReadLock();
    }
  }

  @Override
  public int getUpdateIntervalSeconds() {
    return cfg.updateIntervalSeconds;
  }

  public List<String> getWatchClasses() {
    return cfg.getWatchClasses();
  }

  @Override
  public String getOriginRidField() {
    return cfg.getOriginRidField();
  }

  @Override
  public boolean isUpdatable() {
    return cfg.isUpdatable();
  }

  @Override
  public String getUpdateStrategy() {
    return cfg.getUpdateStrategy();
  }

  @Override
  public List<String> getNodes() {
    return cfg.getNodes();
  }

  @Override
  public List<OViewIndexConfig> getRequiredIndexesInfo() {
    return cfg.getIndexes();
  }

  public Set<OIndex> getClassIndexes() {
    if (activeIndexNames == null || activeIndexNames.isEmpty()) {
      return new HashSet<>();
    }
    acquireSchemaReadLock();
    try {

      final ODatabaseSessionInternal database = getDatabase();
      final OIndexManagerAbstract idxManager = database.getMetadata().getIndexManagerInternal();
      if (idxManager == null) {
        return new HashSet<>();
      }

      return activeIndexNames.stream()
          .map(name -> idxManager.getIndex(database, name))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    } finally {
      releaseSchemaReadLock();
    }
  }

  @Override
  public void getClassIndexes(final Collection<OIndex> indexes) {
    acquireSchemaReadLock();
    try {
      final ODatabaseSessionInternal database = getDatabase();
      final OIndexManagerAbstract idxManager = database.getMetadata().getIndexManagerInternal();
      if (idxManager == null) {
        return;
      }

      activeIndexNames.stream()
          .map(name -> idxManager.getIndex(database, name))
          .filter(Objects::nonNull)
          .forEach(indexes::add);
    } finally {
      releaseSchemaReadLock();
    }
  }

  public List<String> inactivateIndexes() {
    acquireSchemaWriteLock();
    try {
      List<String> oldIndexes = new ArrayList<>(activeIndexNames);
      this.activeIndexNames.clear();
      return oldIndexes;
    } finally {
      releaseSchemaWriteLock();
    }
  }

  public void addActiveIndexes(List<String> names) {
    acquireSchemaWriteLock();
    try {
      this.activeIndexNames.addAll(names);
    } finally {
      releaseSchemaWriteLock();
    }
  }

  public abstract OViewRemovedMetadata replaceViewClusterAndIndex(
      int cluster, List<OIndex> indexes, long lastRefreshTime);

  public Set<String> getActiveIndexNames() {
    return new HashSet<>(activeIndexNames);
  }

  public long getLastRefreshTime() {
    return lastRefreshTime;
  }
}
