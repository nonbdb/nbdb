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
package com.orientechnologies.orient.core.db;

import com.orientechnologies.orient.core.config.OContextConfiguration;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal.ATTRIBUTES;
import com.orientechnologies.orient.core.db.config.ONodeConfiguration;
import com.orientechnologies.orient.core.security.ODefaultSecurityConfig;
import com.orientechnologies.orient.core.security.OGlobalUser;
import com.orientechnologies.orient.core.security.OSecurityConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class YouTrackDBConfig {

  public static final String LOCK_TYPE_MODIFICATION = "modification";
  public static final String LOCK_TYPE_READWRITE = "readwrite";

  private YouTrackDBConfig parent;
  private OContextConfiguration configurations;
  private Map<ATTRIBUTES, Object> attributes;
  private Set<ODatabaseListener> listeners;
  private ClassLoader classLoader;
  private ONodeConfiguration nodeConfiguration;
  private final OSecurityConfig securityConfig;
  private final List<OGlobalUser> users;

  protected YouTrackDBConfig() {
    configurations = new OContextConfiguration();
    attributes = new HashMap<>();
    parent = null;
    listeners = new HashSet<>();
    classLoader = this.getClass().getClassLoader();
    this.securityConfig = new ODefaultSecurityConfig();
    this.users = new ArrayList<OGlobalUser>();
  }

  protected YouTrackDBConfig(
      OContextConfiguration configurations,
      Map<ATTRIBUTES, Object> attributes,
      Set<ODatabaseListener> listeners,
      ClassLoader classLoader,
      ONodeConfiguration nodeConfiguration,
      OSecurityConfig securityConfig,
      List<OGlobalUser> users) {
    this.configurations = configurations;
    this.attributes = attributes;
    parent = null;
    if (listeners != null) {
      this.listeners = listeners;
    } else {
      this.listeners = Collections.emptySet();
    }
    if (classLoader != null) {
      this.classLoader = classLoader;
    } else {
      this.classLoader = this.getClass().getClassLoader();
    }
    this.nodeConfiguration = nodeConfiguration;
    this.securityConfig = securityConfig;
    this.users = users;
  }

  public static YouTrackDBConfig defaultConfig() {
    return new YouTrackDBConfig();
  }

  public static YouTrackDBConfigBuilder builder() {
    return new YouTrackDBConfigBuilder();
  }

  public Set<ODatabaseListener> getListeners() {
    return listeners;
  }

  public OContextConfiguration getConfigurations() {
    return configurations;
  }

  public Map<ATTRIBUTES, Object> getAttributes() {
    return attributes;
  }

  public ONodeConfiguration getNodeConfiguration() {
    return nodeConfiguration;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public OSecurityConfig getSecurityConfig() {
    return securityConfig;
  }

  public List<OGlobalUser> getUsers() {
    return users;
  }

  public void setParent(YouTrackDBConfig parent) {
    this.parent = parent;
    if (parent != null) {
      if (parent.attributes != null) {
        Map<ATTRIBUTES, Object> attrs = new HashMap<>();
        attrs.putAll(parent.attributes);
        if (attributes != null) {
          attrs.putAll(attributes);
        }
        this.attributes = attrs;
      }

      if (parent.configurations != null) {
        OContextConfiguration confis = new OContextConfiguration();
        confis.merge(parent.configurations);
        if (this.configurations != null) {
          confis.merge(this.configurations);
        }
        this.configurations = confis;
      }

      if (this.classLoader == null) {
        this.classLoader = parent.classLoader;
      }

      if (parent.listeners != null) {
        Set<ODatabaseListener> lis = new HashSet<>();
        lis.addAll(parent.listeners);
        if (this.listeners != null) {
          lis.addAll(this.listeners);
        }
        this.listeners = lis;
      }
    }
  }
}
