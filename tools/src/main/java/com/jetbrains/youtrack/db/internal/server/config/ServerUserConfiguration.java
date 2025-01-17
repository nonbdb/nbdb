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
package com.jetbrains.youtrack.db.internal.server.config;

import com.jetbrains.youtrack.db.internal.core.security.GlobalUser;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "user")
@XmlType(propOrder = {"resources", "password", "name"})
public class ServerUserConfiguration implements GlobalUser {

  @XmlAttribute
  public String name;

  @XmlAttribute
  public String password;

  @XmlAttribute
  public String resources;

  public ServerUserConfiguration() {
  }

  public ServerUserConfiguration(
      final String iName, final String iPassword, final String iResources) {
    name = iName;
    password = iPassword;
    resources = iResources;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public String getResources() {
    return resources;
  }
}
