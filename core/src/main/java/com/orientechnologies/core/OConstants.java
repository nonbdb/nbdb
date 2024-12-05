package com.orientechnologies.core;

import com.orientechnologies.common.log.OLogManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OConstants {

  public static final String ORIENT_URL = "https://www.orientdb.com";
  public static final String COPYRIGHT = "Copyrights (c) 2017 YouTrackDB LTD";

  private static final Properties properties = new Properties();

  static {
    try (final InputStream inputStream =
        OConstants.class.getResourceAsStream("/com/orientechnologies/orientdb.properties")) {
      if (inputStream != null) {
        properties.load(inputStream);
      }
    } catch (IOException e) {
      OLogManager.instance().error(OConstants.class, "Failed to load YouTrackDB properties", e);
    }
  }

  /**
   * @return Major part of YouTrackDB version
   */
  public static int getVersionMajor() {
    final String[] versions = properties.getProperty("version").split("\\.");
    if (versions.length == 0) {
      OLogManager.instance()
          .error(OConstants.class, "Can not retrieve version information for this build", null);
      return -1;
    }

    try {
      return Integer.parseInt(versions[0]);
    } catch (NumberFormatException nfe) {
      OLogManager.instance()
          .error(
              OConstants.class, "Can not retrieve major version information for this build", nfe);
      return -1;
    }
  }

  /**
   * @return Minor part of YouTrackDB version
   */
  public static int getVersionMinor() {
    final String[] versions = properties.getProperty("version").split("\\.");
    if (versions.length < 2) {
      OLogManager.instance()
          .error(
              OConstants.class, "Can not retrieve minor version information for this build", null);
      return -1;
    }

    try {
      return Integer.parseInt(versions[1]);
    } catch (NumberFormatException nfe) {
      OLogManager.instance()
          .error(
              OConstants.class, "Can not retrieve minor version information for this build", nfe);
      return -1;
    }
  }

  /**
   * @return Hotfix part of YouTrackDB version
   */
  @SuppressWarnings("unused")
  public static int getVersionHotfix() {
    final String[] versions = properties.getProperty("version").split("\\.");
    if (versions.length < 3) {
      return 0;
    }

    try {
      String hotfix = versions[2];
      int snapshotIndex = hotfix.indexOf("-SNAPSHOT");

      if (snapshotIndex != -1) {
        hotfix = hotfix.substring(0, snapshotIndex);
      }

      return Integer.parseInt(hotfix);
    } catch (NumberFormatException nfe) {
      OLogManager.instance()
          .error(
              OConstants.class, "Can not retrieve hotfix version information for this build", nfe);
      return -1;
    }
  }

  /**
   * @return Returns only current version without build number and etc.
   */
  public static String getRawVersion() {
    return properties.getProperty("version");
  }

  /**
   * Returns the complete text of the current YouTrackDB version.
   */
  public static String getVersion() {
    return properties.getProperty("version")
        + " (build "
        + properties.getProperty("revision")
        + ", branch "
        + properties.getProperty("branch")
        + ")";
  }

  /**
   * Returns true if current YouTrackDB version is a snapshot.
   */
  public static boolean isSnapshot() {
    return properties.getProperty("version").endsWith("SNAPSHOT");
  }

  /**
   * @return the build number if any.
   */
  public static String getBuildNumber() {
    return properties.getProperty("revision");
  }
}
