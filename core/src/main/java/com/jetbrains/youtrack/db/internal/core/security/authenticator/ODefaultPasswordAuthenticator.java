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
package com.jetbrains.youtrack.db.internal.core.security.authenticator;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.db.YTDatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.ORole;
import com.jetbrains.youtrack.db.internal.core.metadata.security.YTImmutableUser;
import com.jetbrains.youtrack.db.internal.core.metadata.security.YTSecurityUser;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.security.OSecurityManager;
import com.jetbrains.youtrack.db.internal.core.security.OSecuritySystem;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a default password authenticator.
 */
public class ODefaultPasswordAuthenticator extends OSecurityAuthenticatorAbstract {

  // Holds a map of the users specified in the security.json file.
  private ConcurrentHashMap<String, YTSecurityUser> usersMap =
      new ConcurrentHashMap<String, YTSecurityUser>();

  // OSecurityComponent
  // Called once the Server is running.
  public void active() {
    LogManager.instance().debug(this, "ODefaultPasswordAuthenticator is active");
  }

  // OSecurityComponent
  public void config(YTDatabaseSessionInternal session, final EntityImpl jsonConfig,
      OSecuritySystem security) {
    super.config(session, jsonConfig, security);

    try {
      if (jsonConfig.containsField("users")) {
        List<EntityImpl> usersList = jsonConfig.field("users");

        for (EntityImpl userDoc : usersList) {

          YTSecurityUser userCfg = createServerUser(session, userDoc);

          if (userCfg != null) {
            String checkName = userCfg.getName(session);

            if (!isCaseSensitive()) {
              checkName = checkName.toLowerCase(Locale.ENGLISH);
            }

            usersMap.put(checkName, userCfg);
          }
        }
      }
    } catch (Exception ex) {
      LogManager.instance().error(this, "config()", ex);
    }
  }

  // Derived implementations can override this method to provide new server user implementations.
  protected YTSecurityUser createServerUser(YTDatabaseSessionInternal session,
      final EntityImpl userDoc) {
    YTSecurityUser userCfg = null;

    if (userDoc.containsField("username") && userDoc.containsField("resources")) {
      final String user = userDoc.field("username");
      final String resources = userDoc.field("resources");
      String password = userDoc.field("password");

      if (password == null) {
        password = "";
      }
      userCfg = new YTImmutableUser(session, user, YTSecurityUser.SERVER_USER_TYPE);
      // userCfg.addRole(OSecurityShared.createRole(null, user));
    }

    return userCfg;
  }

  // OSecurityComponent
  // Called on removal of the authenticator.
  public void dispose() {
    synchronized (usersMap) {
      usersMap.clear();
      usersMap = null;
    }
  }

  // OSecurityAuthenticator
  // Returns the actual username if successful, null otherwise.
  public YTSecurityUser authenticate(
      YTDatabaseSessionInternal session, final String username, final String password) {

    try {
      YTSecurityUser user = getUser(username, session);

      if (isPasswordValid(session, user)) {
        if (OSecurityManager.checkPassword(password, user.getPassword(session))) {
          return user;
        }
      }
    } catch (Exception ex) {
      LogManager.instance().error(this, "ODefaultPasswordAuthenticator.authenticate()", ex);
    }
    return null;
  }

  // OSecurityAuthenticator
  // If not supported by the authenticator, return false.
  public boolean isAuthorized(YTDatabaseSessionInternal session, final String username,
      final String resource) {
    if (username == null || resource == null) {
      return false;
    }

    YTSecurityUser userCfg = getUser(username, session);

    if (userCfg != null) {
      // TODO: to verify if this logic match previous logic
      return userCfg.checkIfAllowed(session, resource, ORole.PERMISSION_ALL) != null;

      // Total Access
      /*
      if (userCfg.getResources().equals("*")) return true;

      String[] resourceParts = userCfg.getResources().split(",");

      for (String r : resourceParts) {
        if (r.equalsIgnoreCase(resource)) return true;
      }
      */
    }

    return false;
  }

  // OSecurityAuthenticator
  public YTSecurityUser getUser(final String username, YTDatabaseSessionInternal session) {
    YTSecurityUser userCfg = null;

    synchronized (usersMap) {
      if (username != null) {
        String checkName = username;

        if (!isCaseSensitive()) {
          checkName = username.toLowerCase(Locale.ENGLISH);
        }

        if (usersMap.containsKey(checkName)) {
          userCfg = usersMap.get(checkName);
        }
      }
    }

    return userCfg;
  }
}
