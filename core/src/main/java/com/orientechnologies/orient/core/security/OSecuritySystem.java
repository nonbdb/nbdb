/*
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
package com.orientechnologies.orient.core.security;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.OxygenDBInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityUser;
import com.orientechnologies.orient.core.metadata.security.auth.OAuthenticationInfo;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a basic interface for a modular security system.
 */
public interface OSecuritySystem {

  void shutdown();

  // Some external security implementations may permit falling back to a
  // default authentication mode if external authentication fails.
  boolean isDefaultAllowed();

  // Returns the actual username if successful, null otherwise.
  // Some token-based authentication (e.g., SPNEGO tokens have the user's name embedded in the
  // service ticket).
  OSecurityUser authenticate(
      ODatabaseSession session, final String username, final String password);

  // Used for generating the appropriate HTTP authentication mechanism. The chain of authenticators
  // is used for this.
  String getAuthenticationHeader(final String databaseName);

  default Map<String, String> getAuthenticationHeaders(final String databaseName) {
    return new HashMap<>();
  }

  ODocument getConfig();

  ODocument getComponentConfig(final String name);

  /**
   * Returns the "System User" associated with 'username' from the system database. If not found,
   * returns null. dbName is used to filter the assigned roles. It may be null.
   */
  OSecurityUser getSystemUser(final String username, final String dbName);

  // Walks through the list of Authenticators.
  boolean isAuthorized(ODatabaseSession session, final String username, final String resource);

  boolean isEnabled();

  // Indicates if passwords should be stored when creating new users.
  boolean arePasswordsStored();

  // Indicates if the primary security mechanism supports single sign-on.
  boolean isSingleSignOnSupported();

  /**
   * Logs to the auditing service, if installed.
   *
   * @param session
   * @param dbName  May be null or empty.
   * @param user    May be null or empty.
   */
  void log(
      ODatabaseSessionInternal session, final OAuditingOperation operation,
      final String dbName,
      OSecurityUser user,
      final String message);

  void registerSecurityClass(final Class<?> cls);

  void reload(ODatabaseSessionInternal session, final ODocument jsonConfig);

  void reload(ODatabaseSessionInternal session, OSecurityUser user, final ODocument jsonConfig);

  void reloadComponent(ODatabaseSessionInternal session, OSecurityUser user, final String name,
      final ODocument jsonConfig);

  void unregisterSecurityClass(final Class<?> cls);

  // If a password validator is registered with the security system, it will be called to validate
  // the specified password. An OInvalidPasswordException is thrown if the password does not meet
  // the password validator's requirements.
  void validatePassword(final String username, final String password)
      throws OInvalidPasswordException;

  OAuditingService getAuditing();

  /**
   * Returns the authenticator based on name, if one exists.
   */
  OSecurityAuthenticator getAuthenticator(final String authName);

  /**
   * Returns the first authenticator in the list, which is the primary authenticator.
   */
  OSecurityAuthenticator getPrimaryAuthenticator();

  OSyslog getSyslog();

  /**
   * Some authenticators support maintaining a list of users and associated resources (and sometimes
   * passwords).
   */
  OSecurityUser getUser(final String username, ODatabaseSessionInternal session);

  void onAfterDynamicPlugins(ODatabaseSessionInternal session);

  default void onAfterDynamicPlugins(ODatabaseSessionInternal session, OSecurityUser user) {
    onAfterDynamicPlugins(session);
  }

  OSecurityUser authenticateAndAuthorize(
      ODatabaseSessionInternal session, String iUserName, String iPassword,
      String iResourceToCheck);

  OSecurityUser authenticateServerUser(ODatabaseSession session, String username, String password);

  OSecurityUser getServerUser(ODatabaseSessionInternal session, String username);

  boolean isServerUserAuthorized(ODatabaseSession session, String username, String resource);

  OxygenDBInternal getContext();

  boolean existsUser(String defaultRootUser);

  void addTemporaryUser(String user, String password, String resources);

  OSecurityInternal newSecurity(String database);

  OSecurityUser authenticate(ODatabaseSession session, OAuthenticationInfo authenticationInfo);

  OTokenSign getTokenSign();
}
