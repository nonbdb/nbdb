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
package com.orientechnologies.orient.server.network.protocol.http;

import com.jetbrains.youtrack.db.internal.common.concur.resource.SharedResourceAbstract;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBManager;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.orientechnologies.orient.server.OServer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Handles the HTTP sessions such as a real HTTP Server.
 */
public class HttpSessionManager extends SharedResourceAbstract {

  private final Map<String, OHttpSession> sessions = new HashMap<String, OHttpSession>();
  private int expirationTime;
  private final Random random = new SecureRandom();

  public HttpSessionManager(OServer server) {
    expirationTime =
        server
            .getContextConfiguration()
            .getValueAsInteger(GlobalConfiguration.NETWORK_HTTP_SESSION_EXPIRE_TIMEOUT)
            * 1000;

    YouTrackDBManager.instance()
        .scheduleTask(
            new Runnable() {
              @Override
              public void run() {
                final int expired = checkSessionsValidity();
                if (expired > 0) {
                  LogManager.instance().debug(this, "Removed %d session because expired", expired);
                }
              }
            },
            expirationTime,
            expirationTime);
  }

  public int checkSessionsValidity() {
    int expired = 0;

    acquireExclusiveLock();
    try {
      final long now = System.currentTimeMillis();

      Entry<String, OHttpSession> s;
      for (Iterator<Map.Entry<String, OHttpSession>> it = sessions.entrySet().iterator();
          it.hasNext(); ) {
        s = it.next();

        if (now - s.getValue().getUpdatedOn() > expirationTime) {
          // REMOVE THE SESSION
          it.remove();
          expired++;
        }
      }

    } finally {
      releaseExclusiveLock();
    }

    return expired;
  }

  public OHttpSession[] getSessions() {
    acquireSharedLock();
    try {

      return sessions.values().toArray(new OHttpSession[sessions.size()]);

    } finally {
      releaseSharedLock();
    }
  }

  public OHttpSession getSession(final String iId) {
    acquireSharedLock();
    try {

      final OHttpSession sess = sessions.get(iId);
      if (sess != null) {
        sess.updateLastUpdatedOn();
      }
      return sess;

    } finally {
      releaseSharedLock();
    }
  }

  public String createSession(
      final String iDatabaseName, final String iUserName, final String iUserPassword) {
    acquireExclusiveLock();
    try {
      final String id = "OS" + System.currentTimeMillis() + random.nextLong();
      sessions.put(id, new OHttpSession(id, iDatabaseName, iUserName, iUserPassword));
      return id;

    } finally {
      releaseExclusiveLock();
    }
  }

  public OHttpSession removeSession(final String iSessionId) {
    acquireExclusiveLock();
    try {
      return sessions.remove(iSessionId);

    } finally {
      releaseExclusiveLock();
    }
  }

  public int getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(int expirationTime) {
    this.expirationTime = expirationTime;
  }

  public void shutdown() {
    sessions.clear();
  }
}