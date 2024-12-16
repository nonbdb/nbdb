package com.jetbrains.youtrack.db.internal.server;

import java.util.List;

/**
 *
 */
public class ClientConnectionStats {

  public int totalRequests = 0;
  public String lastCommandInfo = null;
  public String lastCommandDetail = null;
  public long lastCommandExecutionTime = 0;
  public long lastCommandReceived = 0;
  public String lastDatabase = null;
  public String lastUser = null;
  public long totalCommandExecutionTime = 0;
  public List<String> activeQueries;
}
