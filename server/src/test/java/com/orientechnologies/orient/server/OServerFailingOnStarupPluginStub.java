package com.orientechnologies.orient.server;

import com.orientechnologies.orient.server.distributed.ODistributedException;
import com.orientechnologies.orient.server.plugin.OServerPluginAbstract;

/**
 *
 */
public class OServerFailingOnStarupPluginStub extends OServerPluginAbstract {

  @Override
  public void startup() {
    throw new ODistributedException("this plugin is not starting correctly");
  }

  @Override
  public String getName() {
    return "failing on startup plugin";
  }
}
