/*
 *
 *  *  Copyright YouTrackDB
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
package com.jetbrains.youtrack.db.internal.server.distributed;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Modifiable Distributed configuration. It's created starting from a DistributedConfiguration
 * object. Every changes increment the field "version".
 */
public class ModifiableDistributedConfiguration extends DistributedConfiguration {

  public ModifiableDistributedConfiguration(final EntityImpl iConfiguration) {
    super(iConfiguration);
  }

  public ModifiableDistributedConfiguration modify() {
    return this;
  }

  /**
   * Sets the server role between MASTER (default) and REPLICA.
   */
  public void setServerRole(final String iServerName, final ROLES role) {
    synchronized (configuration) {
      EntityImpl servers = configuration.field(SERVERS);
      if (servers == null) {
        servers = new EntityImpl();
        configuration.field(SERVERS, servers, PropertyType.EMBEDDED);
      }

      servers.field(iServerName, role);
      incrementVersion();
    }
  }

  /**
   * Adds a server in the configuration. It replaces all the tags &lt;NEW_NODE&gt; with the new
   * server name<br> NOTE: It must be executed in distributed database lock.
   *
   * @param iNode Server name
   * @return the list of changed partitions or null if no changes have applied
   */
  public List<String> addNewNodeInServerList(final String iNode) {
    synchronized (configuration) {
      final List<String> changedPartitions = new ArrayList<String>();
      // ADD THE NODE IN CONFIGURATION. LOOK FOR $newNode TAG
      for (String clusterName : getClusterNames()) {
        final List<String> partitions = getClusterConfiguration(clusterName).field(SERVERS);
        if (partitions != null) {
          final int newNodePos =
              partitions.indexOf(NEW_NODE_TAG);
          if (newNodePos > -1 && !partitions.contains(iNode)) {
            partitions.add(newNodePos, iNode);
            changedPartitions.add(clusterName);
          }
        }
      }

      if (!changedPartitions.isEmpty()) {
        // INCREMENT VERSION
        incrementVersion();

        if (!getRegisteredServers().contains(iNode)) {
          if (getNewNodeStrategy() == NEW_NODE_STRATEGIES.STATIC) {
            // REGISTER THE SERVER AS STATIC AND INCREMENT VERSION
            setServerRole(iNode, getServerRole("*"));
          }
        }
        return changedPartitions;
      }
    }
    return null;
  }

  /**
   * Sets the server as owner for the given cluster. The owner server is the first in server list.
   * <br>
   * NOTE: It must be executed in distributed database lock.
   *
   * @param iClusterName Cluster name or *. Does not accept null.
   */
  public void setServerOwner(final String iClusterName, final String iServerName) {
    if (iClusterName == null) {
      throw new IllegalArgumentException("cluster name cannot be null");
    }

    synchronized (configuration) {
      final EntityImpl clusters = configuration.field(CLUSTERS);
      EntityImpl cluster = clusters.field(iClusterName);

      if (cluster == null)
      // CREATE IT
      {
        cluster = createCluster(iClusterName);
      } else {
        // CHECK IF THE OWNER IS ALREADY CONFIGURED
        final String owner = cluster.field(OWNER);
        if (owner != null && !iServerName.equalsIgnoreCase(owner)) {
          throw new DistributedException(
              "Cannot overwrite ownership of cluster '"
                  + iClusterName
                  + "' to the server '"
                  + iServerName
                  + "', because server '"
                  + owner
                  + "' was already configured as owner");
        }
      }

      List<String> serverList = getClusterConfiguration(iClusterName).field(SERVERS);
      if (serverList == null) {
        serverList = initClusterServers(cluster);
      }

      if (!serverList.isEmpty() && serverList.get(0).equals(iServerName))
      // ALREADY OWNER
      {
        return;
      }

      // REMOVE THE NODE IF ANY
      boolean removed = false;
      for (Iterator<String> it = serverList.iterator(); it.hasNext(); ) {
        if (it.next().equals(iServerName)) {
          it.remove();
          removed = true;
          break;
        }
      }

      if (!removed) {
        throw new DistributedException(
            "Cannot set ownership of cluster '"
                + iClusterName
                + "' to the server '"
                + iServerName
                + "', because the server has no that cluster (sharding)");
      }

      // ADD THE NODE AS FIRST OF THE LIST = MASTER
      serverList.add(0, iServerName);

      incrementVersion();
    }
  }

  /**
   * Removes a server from the list.<br> NOTE: It must be executed in distributed database lock.
   *
   * @param iNode Server name
   * @return
   */
  public List<String> removeServer(final String iNode) {
    synchronized (configuration) {
      final List<String> changedPartitions = new ArrayList<String>();

      for (String clusterName : getClusterNames()) {
        final Collection<String> nodes = getClusterConfiguration(clusterName).field(SERVERS);
        if (nodes != null) {
          for (String node : nodes) {
            if (node.equals(iNode)) {
              // FOUND: REMOVE IT
              nodes.remove(node);
              changedPartitions.add(clusterName);
              break;
            }
          }
        }
      }

      if (!changedPartitions.isEmpty()) {
        incrementVersion();
        return changedPartitions;
      }
    }
    return null;
  }

  /**
   * Set a server offline. It assures the offline server is never on top of the list.<br> NOTE: It
   * must be executed in distributed database lock.
   *
   * @param iNode                Server name
   * @param newLockManagerServer New Lock Manager server name
   * @return
   */
  public List<String> setServerOffline(final String iNode, final String newLockManagerServer) {
    final List<String> changedPartitions = new ArrayList<String>();

    final String[] clusters = getClusterNames();
    synchronized (configuration) {
      for (String clusterName : clusters) {
        final List<String> nodes = getClusterConfiguration(clusterName).field(SERVERS);
        if (nodes != null && nodes.size() > 1) {
          for (String node : nodes) {
            if (node.equals(iNode)) {
              // FOUND: PUT THE NODE AT THE END (BEFORE ANY TAG <NEW_NODE>)
              nodes.remove(node);

              final boolean newNodeRemoved = nodes.remove(NEW_NODE_TAG);

              nodes.add(node);

              if (newNodeRemoved)
              // REINSERT NEW NODE TAG AT THE END
              {
                nodes.add(NEW_NODE_TAG);
              }

              if (newLockManagerServer != null) {
                // ASSURE THE NEW LOCK MANAGER IS THE FIRST IN THE LIST
                if (nodes.remove(newLockManagerServer)) {
                  nodes.add(0, newLockManagerServer);
                }
              }

              changedPartitions.add(clusterName);
              break;
            }
          }
        }
      }

      if (!changedPartitions.isEmpty()) {
        incrementVersion();
        return changedPartitions;
      }
    }
    return null;
  }

  private void incrementVersion() {
    // INCREMENT VERSION
    Integer oldVersion = configuration.field(VERSION);
    if (oldVersion == null) {
      oldVersion = 0;
    }
    configuration.field(VERSION, oldVersion.intValue() + 1);
  }

  private List<String> initClusterServers(final EntityImpl cluster) {
    final EntityImpl any = getClusterConfiguration(ALL_WILDCARD);

    // COPY THE SERVER LIST FROM ALL_WILDCARD
    final List<String> anyServers = any.field(SERVERS);
    final List<String> servers = new ArrayList<String>(anyServers);
    cluster.field(SERVERS, servers);

    return servers;
  }

  private EntityImpl createCluster(final String iClusterName) {
    // CREATE IT
    final EntityImpl clusters = configuration.field(CLUSTERS);

    EntityImpl cluster = clusters.field(iClusterName);
    if (cluster != null)
    // ALREADY EXISTS
    {
      return clusters;
    }

    cluster = new EntityImpl();
    EntityInternalUtils.addOwner(cluster, clusters);
    clusters.field(iClusterName, cluster, PropertyType.EMBEDDED);

    final List<String> servers = initClusterServers(cluster);

    return cluster;
  }

  public void override(final EntityImpl newCfg) {
    newCfg.copyTo(configuration);
    incrementVersion();
  }
}
