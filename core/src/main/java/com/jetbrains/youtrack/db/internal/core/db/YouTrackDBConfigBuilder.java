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

package com.jetbrains.youtrack.db.internal.core.db;

import com.jetbrains.youtrack.db.internal.core.config.ContextConfiguration;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal.ATTRIBUTES;
import com.jetbrains.youtrack.db.internal.core.db.config.NodeConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.config.NodeConfigurationBuilder;
import com.jetbrains.youtrack.db.internal.core.security.GlobalUser;
import com.jetbrains.youtrack.db.internal.core.security.GlobalUserImpl;
import com.jetbrains.youtrack.db.internal.core.security.SecurityConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YouTrackDBConfigBuilder {

  private ContextConfiguration configurations = new ContextConfiguration();
  private final Map<ATTRIBUTES, Object> attributes = new HashMap<>();
  private final Set<DatabaseListener> listeners = new HashSet<>();
  private ClassLoader classLoader;
  private final NodeConfigurationBuilder nodeConfigurationBuilder = NodeConfiguration.builder();
  private SecurityConfig securityConfig;
  private final List<GlobalUser> users = new ArrayList<GlobalUser>();

  public YouTrackDBConfigBuilder fromGlobalMap(Map<GlobalConfiguration, Object> values) {
    for (Map.Entry<GlobalConfiguration, Object> entry : values.entrySet()) {
      addConfig(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public YouTrackDBConfigBuilder fromMap(Map<String, Object> values) {
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      configurations.setValue(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public YouTrackDBConfigBuilder addListener(DatabaseListener listener) {
    listeners.add(listener);
    return this;
  }

  public YouTrackDBConfigBuilder addConfig(
      final GlobalConfiguration configuration, final Object value) {
    configurations.setValue(configuration, value);
    return this;
  }

  public YouTrackDBConfigBuilder addAttribute(final ATTRIBUTES attribute, final Object value) {
    attributes.put(attribute, value);
    return this;
  }

  public YouTrackDBConfigBuilder setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public NodeConfigurationBuilder getNodeConfigurationBuilder() {
    return nodeConfigurationBuilder;
  }

  public YouTrackDBConfigBuilder setSecurityConfig(SecurityConfig securityConfig) {
    this.securityConfig = securityConfig;
    return this;
  }

  public YouTrackDBConfig build() {
    return new YouTrackDBConfig(
        configurations,
        attributes,
        listeners,
        classLoader,
        nodeConfigurationBuilder.build(),
        securityConfig,
        users);
  }

  public YouTrackDBConfigBuilder fromContext(final ContextConfiguration contextConfiguration) {
    configurations = contextConfiguration;
    return this;
  }

  public YouTrackDBConfigBuilder addGlobalUser(
      final String user, final String password, final String resource) {
    users.add(new GlobalUserImpl(user, password, resource));
    return this;
  }
}
