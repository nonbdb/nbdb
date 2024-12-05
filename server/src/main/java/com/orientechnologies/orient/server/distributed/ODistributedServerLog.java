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
package com.orientechnologies.orient.server.distributed;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import javax.annotation.Nonnull;

/**
 * Distributed logger.
 */
public class ODistributedServerLog {

  public enum DIRECTION {
    NONE,
    IN,
    OUT,
    BOTH
  }

  public static boolean isDebugEnabled() {
    return LogManager.instance().isDebugEnabled();
  }

  public static void debug(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .debug(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            null,
            iAdditionalArgs);
  }

  public static void debug(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Throwable iException,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .debug(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            iException,
            iAdditionalArgs);
  }

  public static void info(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .info(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            null,
            iAdditionalArgs);
  }

  public static void info(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Throwable iException,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .info(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            iException,
            iAdditionalArgs);
  }

  public static void warn(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .warn(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            null,
            iAdditionalArgs);
  }

  public static void warn(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Throwable iException,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .warn(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            iException,
            iAdditionalArgs);
  }

  public static void error(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .error(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            null,
            iAdditionalArgs);
  }

  public static void error(
      @Nonnull final Object iRequester,
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      @Nonnull final String iMessage,
      final Throwable iException,
      final Object... iAdditionalArgs) {
    LogManager.instance()
        .error(
            iRequester,
            formatMessage(iLocalNode, iRemoteNode, iDirection, iMessage),
            iException,
            iAdditionalArgs);
  }

  protected static String formatMessage(
      final String iLocalNode,
      final String iRemoteNode,
      final DIRECTION iDirection,
      final String iMessage) {
    final StringBuilder message = new StringBuilder(256);

    if (iLocalNode != null) {
      message.append('[');
      message.append(iLocalNode);
      message.append(']');
    }

    if (iRemoteNode != null && !iRemoteNode.equals(iLocalNode)) {
      switch (iDirection) {
        case IN:
          message.append("<-");
          break;
        case OUT:
          message.append("->");
          break;
        case BOTH:
          message.append("<>");
          break;
        case NONE:
          message.append("--");
          break;
      }

      message.append('[');
      message.append(iRemoteNode);
      message.append(']');
    }

    message.append(' ');
    message.append(iMessage);

    return message.toString();
  }
}
