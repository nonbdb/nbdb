/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.orientechnologies.security.auditing;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.core.YouTrackDBManager;
import com.orientechnologies.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.core.db.OSystemDatabase;
import com.orientechnologies.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.core.db.YouTrackDBInternal;
import com.orientechnologies.core.metadata.schema.YTClass;
import com.orientechnologies.core.metadata.schema.YTSchema;
import com.orientechnologies.core.metadata.schema.YTType;
import com.orientechnologies.core.metadata.security.YTSecurityUser;
import com.orientechnologies.core.record.impl.YTEntityImpl;
import com.orientechnologies.core.security.OAuditingOperation;
import com.orientechnologies.core.security.OAuditingService;
import com.orientechnologies.core.security.OSecuritySystem;
import com.orientechnologies.orient.server.OServerAware;
import com.orientechnologies.orient.server.distributed.ODistributedLifecycleListener;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class ODefaultAuditing
    implements OAuditingService, ODatabaseLifecycleListener, ODistributedLifecycleListener {

  public static final String AUDITING_LOG_CLASSNAME = "OAuditingLog";

  private boolean enabled = true;
  private Integer globalRetentionDays = -1;
  private YouTrackDBInternal context;

  private final Timer timer = new Timer();
  private YTAuditingHook globalHook;

  private final Map<String, YTAuditingHook> hooks;

  private TimerTask retainTask;

  protected static final String DEFAULT_FILE_AUDITING_DB_CONFIG = "default-auditing-config.json";
  protected static final String FILE_AUDITING_DB_CONFIG = "auditing-config.json";

  private OAuditingDistribConfig distribConfig;

  private OSystemDBImporter systemDbImporter;

  private OSecuritySystem security;

  public static final String IMPORTER_FLAG = "AUDITING_IMPORTER";

  private class OAuditingDistribConfig extends OAuditingConfig {

    private boolean onNodeJoinedEnabled = false;
    private String onNodeJoinedMessage = "The node ${node} has joined";

    private boolean onNodeLeftEnabled = false;
    private String onNodeLeftMessage = "The node ${node} has left";

    public OAuditingDistribConfig(final YTEntityImpl cfg) {
      if (cfg.containsField("onNodeJoinedEnabled")) {
        onNodeJoinedEnabled = cfg.field("onNodeJoinedEnabled");
      }

      onNodeJoinedMessage = cfg.field("onNodeJoinedMessage");

      if (cfg.containsField("onNodeLeftEnabled")) {
        onNodeLeftEnabled = cfg.field("onNodeLeftEnabled");
      }

      onNodeLeftMessage = cfg.field("onNodeLeftMessage");
    }

    @Override
    public String formatMessage(final OAuditingOperation op, final String subject) {
      if (op == OAuditingOperation.NODEJOINED) {
        return resolveMessage(onNodeJoinedMessage, "node", subject);
      } else if (op == OAuditingOperation.NODELEFT) {
        return resolveMessage(onNodeLeftMessage, "node", subject);
      }

      return subject;
    }

    @Override
    public boolean isEnabled(OAuditingOperation op) {
      if (op == OAuditingOperation.NODEJOINED) {
        return onNodeJoinedEnabled;
      } else if (op == OAuditingOperation.NODELEFT) {
        return onNodeLeftEnabled;
      }

      return false;
    }
  }

  public ODefaultAuditing() {
    hooks = new ConcurrentHashMap<String, YTAuditingHook>(20);
  }

  @Override
  public PRIORITY getPriority() {
    return PRIORITY.LAST;
  }

  @Override
  public void onCreate(final YTDatabaseSessionInternal iDatabase) {
    // Don't audit system database events.
    if (iDatabase.getName().equalsIgnoreCase(OSystemDatabase.SYSTEM_DB_NAME)) {
      return;
    }

    final YTAuditingHook hook = defaultHook(iDatabase);
    hooks.put(iDatabase.getName(), hook);
    iDatabase.registerHook(hook);
    iDatabase.registerListener(hook);
  }

  private YTAuditingHook defaultHook(final YTDatabaseSessionInternal iDatabase) {
    final File auditingFileConfig = getConfigFile(iDatabase.getName());
    String content = null;
    if (auditingFileConfig != null && auditingFileConfig.exists()) {
      content = getContent(auditingFileConfig);

    } else {
      final InputStream resourceAsStream =
          this.getClass().getClassLoader().getResourceAsStream(DEFAULT_FILE_AUDITING_DB_CONFIG);
      try {
        if (resourceAsStream == null) {
          OLogManager.instance().error(this, "defaultHook() resourceAsStream is null", null);
        }

        content = getString(resourceAsStream);
        if (auditingFileConfig != null) {
          try {
            auditingFileConfig.getParentFile().mkdirs();
            auditingFileConfig.createNewFile();

            final FileOutputStream f = new FileOutputStream(auditingFileConfig);
            try {
              f.write(content.getBytes());
              f.flush();
            } finally {
              f.close();
            }
          } catch (IOException e) {
            content = "{}";
            OLogManager.instance().error(this, "Cannot save auditing file configuration", e);
          }
        }
      } finally {
        try {
          if (resourceAsStream != null) {
            resourceAsStream.close();
          }
        } catch (IOException e) {
          OLogManager.instance().error(this, "Cannot read auditing file configuration", e);
        }
      }
    }
    final YTEntityImpl cfg = new YTEntityImpl().fromJSON(content, "noMap");
    return new YTAuditingHook(cfg, security);
  }

  private String getContent(File auditingFileConfig) {
    FileInputStream f = null;
    String content = "";
    try {
      f = new FileInputStream(auditingFileConfig);
      final byte[] buffer = new byte[(int) auditingFileConfig.length()];
      f.read(buffer);

      content = new String(buffer);

    } catch (Exception e) {
      content = "{}";
      OLogManager.instance().error(this, "Cannot get auditing file configuration", e);
    } finally {
      if (f != null) {
        try {
          f.close();
        } catch (IOException e) {
          OLogManager.instance().error(this, "Cannot get auditing file configuration", e);
        }
      }
    }
    return content;
  }

  public String getString(InputStream is) {

    try {
      int ch;
      final StringBuilder sb = new StringBuilder();
      while ((ch = is.read()) != -1) {
        sb.append((char) ch);
      }
      return sb.toString();
    } catch (IOException e) {
      OLogManager.instance().error(this, "Cannot get default auditing file configuration", e);
      return "{}";
    }
  }

  @Override
  public void onOpen(YTDatabaseSessionInternal iDatabase) {
    // Don't audit system database events.
    if (iDatabase.getName().equalsIgnoreCase(OSystemDatabase.SYSTEM_DB_NAME)) {
      return;
    }

    // If the database has been opened by the auditing importer, do not hook it.
    if (iDatabase.getProperty(IMPORTER_FLAG) != null) {
      return;
    }

    YTAuditingHook oAuditingHook = hooks.get(iDatabase.getName());
    if (oAuditingHook == null) {
      oAuditingHook = defaultHook(iDatabase);
      hooks.put(iDatabase.getName(), oAuditingHook);
    }
    iDatabase.registerHook(oAuditingHook);
    iDatabase.registerListener(oAuditingHook);
  }

  @Override
  public void onClose(YTDatabaseSessionInternal iDatabase) {
    final YTAuditingHook oAuditingHook = hooks.get(iDatabase.getName());
    if (oAuditingHook != null) {
      iDatabase.unregisterHook(oAuditingHook);
      iDatabase.unregisterListener(oAuditingHook);
    }
  }

  @Override
  public void onDrop(YTDatabaseSessionInternal iDatabase) {
    onClose(iDatabase);

    final YTAuditingHook oAuditingHook = hooks.get(iDatabase.getName());
    if (oAuditingHook != null) {
      oAuditingHook.shutdown(false);
    }

    File f = getConfigFile(iDatabase.getName());
    if (f != null && f.exists()) {
      OLogManager.instance()
          .info(this, "Removing Auditing config for db : %s", iDatabase.getName());
      f.delete();
    }
  }

  private File getConfigFile(String iDatabaseName) {
    return new File(
        security.getContext().getBasePath()
            + File.separator
            + iDatabaseName
            + File.separator
            + FILE_AUDITING_DB_CONFIG);
  }

  @Override
  public void onCreateClass(YTDatabaseSessionInternal iDatabase, YTClass iClass) {
    final YTAuditingHook oAuditingHook = hooks.get(iDatabase.getName());

    if (oAuditingHook != null) {
      oAuditingHook.onCreateClass(iClass);
    }
  }

  @Override
  public void onDropClass(YTDatabaseSessionInternal iDatabase, YTClass iClass) {
    final YTAuditingHook oAuditingHook = hooks.get(iDatabase.getName());

    if (oAuditingHook != null) {
      oAuditingHook.onDropClass(iClass);
    }
  }

  @Override
  public void onLocalNodeConfigurationRequest(YTEntityImpl iConfiguration) {
  }

  protected void updateConfigOnDisk(final String iDatabaseName, final YTEntityImpl cfg)
      throws IOException {
    final File auditingFileConfig = getConfigFile(iDatabaseName);
    if (auditingFileConfig != null) {
      final FileOutputStream f = new FileOutputStream(auditingFileConfig);
      try {
        f.write(cfg.toJSON("prettyPrint=true").getBytes());
        f.flush();
      } finally {
        f.close();
      }
    }
  }

  /// ///
  // ODistributedLifecycleListener
  public boolean onNodeJoining(String iNode) {
    return true;
  }

  public void onNodeJoined(YTDatabaseSessionInternal session, String iNode) {
    if (distribConfig != null && distribConfig.isEnabled(OAuditingOperation.NODEJOINED)) {
      log(session,
          OAuditingOperation.NODEJOINED,
          distribConfig.formatMessage(OAuditingOperation.NODEJOINED, iNode));
    }
  }

  public void onNodeLeft(YTDatabaseSessionInternal session, String iNode) {
    if (distribConfig != null && distribConfig.isEnabled(OAuditingOperation.NODELEFT)) {
      log(session,
          OAuditingOperation.NODELEFT,
          distribConfig.formatMessage(OAuditingOperation.NODELEFT, iNode));
    }
  }

  public void onDatabaseChangeStatus(
      String iNode, String iDatabaseName, ODistributedServerManager.DB_STATUS iNewStatus) {
  }

  @Deprecated
  public static String getClusterName(final String dbName) {
    return dbName + "_auditing";
  }

  public static String getClassName(final String dbName) {
    return dbName + AUDITING_LOG_CLASSNAME;
  }

  /// ///
  // OAuditingService
  public void changeConfig(
      YTDatabaseSessionInternal session, final YTSecurityUser user, final String iDatabaseName,
      final YTEntityImpl cfg)
      throws IOException {

    // This should never happen, but just in case...
    // Don't audit system database events.
    if (iDatabaseName != null && iDatabaseName.equalsIgnoreCase(OSystemDatabase.SYSTEM_DB_NAME)) {
      return;
    }

    hooks.put(iDatabaseName, new YTAuditingHook(cfg, security));

    updateConfigOnDisk(iDatabaseName, cfg);

    log(session,
        OAuditingOperation.CHANGEDCONFIG,
        user, String.format(
            "The auditing configuration for the database '%s' has been changed", iDatabaseName));
  }

  public YTEntityImpl getConfig(final String iDatabaseName) {
    return hooks.get(iDatabaseName).getConfiguration();
  }

  /**
   * Primarily used for global logging events (e.g., NODEJOINED, NODELEFT).
   */
  public void log(YTDatabaseSessionInternal session, final OAuditingOperation operation,
      final String message) {
    log(session, operation, null, null, message);
  }

  /**
   * Primarily used for global logging events (e.g., NODEJOINED, NODELEFT).
   */
  public void log(YTDatabaseSessionInternal session, final OAuditingOperation operation,
      YTSecurityUser user, final String message) {
    log(session, operation, null, user, message);
  }

  /**
   * Primarily used for global logging events (e.g., NODEJOINED, NODELEFT).
   */
  public void log(
      YTDatabaseSessionInternal session, final OAuditingOperation operation,
      final String dbName,
      YTSecurityUser user,
      final String message) {
    // If dbName is null, then we submit the log message to the global auditing hook.
    // Otherwise, we submit it to the hook associated with dbName.
    if (dbName != null) {
      final YTAuditingHook oAuditingHook = hooks.get(dbName);

      if (oAuditingHook != null) {
        oAuditingHook.log(session, operation, dbName, user, message);
      } else { // Use the global hook.
        globalHook.log(session, operation, dbName, user, message);
      }
    } else { // Use the global hook.
      String userName = null;
      if (user != null) {
        userName = user.getName(session);
      }
      if (globalHook == null) {
        OLogManager.instance()
            .error(
                this,
                "Default Auditing is disabled, cannot log: op=%s db='%s' user=%s message='%s'",
                null,
                operation,
                dbName,
                userName,
                message);
      } else {
        globalHook.log(session, operation, dbName, user, message);
      }
    }
  }

  private void createClassIfNotExists() {
    final YTDatabaseSessionInternal currentDB = ODatabaseRecordThreadLocal.instance()
        .getIfDefined();

    YTDatabaseSessionInternal sysdb = null;

    try {
      sysdb = context.getSystemDatabase().openSystemDatabase();

      YTSchema schema = sysdb.getMetadata().getSchema();
      YTClass cls = schema.getClass(AUDITING_LOG_CLASSNAME);

      if (cls == null) {
        cls = sysdb.getMetadata().getSchema().createClass(AUDITING_LOG_CLASSNAME);
        cls.createProperty(currentDB, "date", YTType.DATETIME);
        cls.createProperty(currentDB, "user", YTType.STRING);
        cls.createProperty(currentDB, "operation", YTType.BYTE);
        cls.createProperty(currentDB, "record", YTType.LINK);
        cls.createProperty(currentDB, "changes", YTType.EMBEDDED);
        cls.createProperty(currentDB, "note", YTType.STRING);
        cls.createProperty(currentDB, "database", YTType.STRING);
      }
    } catch (Exception e) {
      OLogManager.instance().error(this, "Creating auditing class exception", e);
    } finally {
      if (sysdb != null) {
        sysdb.close();
      }

      if (currentDB != null) {
        ODatabaseRecordThreadLocal.instance().set(currentDB);
      } else {
        ODatabaseRecordThreadLocal.instance().remove();
      }
    }
  }

  /// ///
  // OAuditingService (OSecurityComponent)

  // Called once the Server is running.
  public void active() {
    createClassIfNotExists();

    globalHook = new YTAuditingHook(security);

    retainTask =
        new TimerTask() {
          public void run() {
            retainLogs();
          }
        };

    long delay = 1000L;
    long period = 1000L * 60L * 60L * 24L;

    timer.scheduleAtFixedRate(retainTask, delay, period);

    YouTrackDBManager.instance().addDbLifecycleListener(this);
    if (context instanceof OServerAware) {
      if (((OServerAware) context).getDistributedManager() != null) {
        ((OServerAware) context).getDistributedManager().registerLifecycleListener(this);
      }
    }

    if (systemDbImporter != null && systemDbImporter.isEnabled()) {
      systemDbImporter.start();
    }
  }

  public void retainLogs() {

    if (globalRetentionDays > 0) {
      Calendar c = Calendar.getInstance();
      c.setTime(new Date());
      c.add(Calendar.DATE, (-1) * globalRetentionDays);
      retainLogs(c.getTime());
    }
  }

  public void retainLogs(Date date) {
    long time = date.getTime();
    context
        .getSystemDatabase()
        .executeWithDB(
            (db -> {
              db.command("delete from OAuditingLog where date < ?", time).close();
              return null;
            }));
  }

  public void config(YTDatabaseSessionInternal session, final YTEntityImpl jsonConfig,
      OSecuritySystem security) {
    context = security.getContext();
    this.security = security;
    try {
      if (jsonConfig.containsField("enabled")) {
        enabled = jsonConfig.field("enabled");
      }

      if (jsonConfig.containsField("retentionDays")) {
        globalRetentionDays = jsonConfig.field("retentionDays");
      }

      if (jsonConfig.containsField("distributed")) {
        YTEntityImpl distribDoc = jsonConfig.field("distributed");
        distribConfig = new OAuditingDistribConfig(distribDoc);
      }

      if (jsonConfig.containsField("systemImport")) {
        YTEntityImpl sysImport = jsonConfig.field("systemImport");

        systemDbImporter = new OSystemDBImporter(context, sysImport);
      }
    } catch (Exception ex) {
      OLogManager.instance().error(this, "config()", ex);
    }
  }

  // Called on removal of the component.
  public void dispose() {
    if (systemDbImporter != null && systemDbImporter.isEnabled()) {
      systemDbImporter.shutdown();
    }

    if (context instanceof OServerAware) {
      if (((OServerAware) context).getDistributedManager() != null) {
        ((OServerAware) context).getDistributedManager().unregisterLifecycleListener(this);
      }
    }

    YouTrackDBManager.instance().removeDbLifecycleListener(this);

    if (globalHook != null) {
      globalHook.shutdown(false);
      globalHook = null;
    }

    if (retainTask != null) {
      retainTask.cancel();
    }

    if (timer != null) {
      timer.cancel();
    }
  }

  // OSecurityComponent
  public boolean isEnabled() {
    return enabled;
  }
}
