package com.jetbrains.youtrack.db.internal.server.distributed.operation;

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandDistributedReplicateRequest;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.server.YouTrackDBServer;
import com.jetbrains.youtrack.db.internal.server.distributed.DistributedRequestId;
import com.jetbrains.youtrack.db.internal.server.distributed.ODistributedServerManager;
import com.jetbrains.youtrack.db.internal.server.distributed.ORemoteTaskFactory;
import com.jetbrains.youtrack.db.internal.server.distributed.task.RemoteTask;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class NodeOperationTask implements RemoteTask {

  public static final int FACTORYID = 55;
  private NodeOperation task;
  private String nodeSource;

  private Integer messageId;

  private static final Map<Integer, NodeOperationFactory> MESSAGES = new HashMap<>();

  static {
    MESSAGES.put(0, new NodeOperationFactory(null, () -> new NodeOperationResponseFailed()));
  }

  public NodeOperationTask(NodeOperation task) {
    this.task = task;
  }

  public NodeOperationTask() {
  }

  @Override
  public boolean hasResponse() {
    return true;
  }

  @Override
  public String getName() {
    return "Node Task";
  }

  @Override
  public CommandDistributedReplicateRequest.QUORUM_TYPE getQuorumType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object execute(
      DistributedRequestId requestId,
      YouTrackDBServer iServer,
      ODistributedServerManager iManager,
      DatabaseSessionInternal database)
      throws Exception {

    if (task != null) {
      return new NodeOperationTaskResponse(task.getMessageId(), task.execute(iServer, iManager));
    } else {
      return new NodeOperationTaskResponse(
          0,
          new NodeOperationResponseFailed(
              404,
              String.format(
                  "Handler not found for message with id %d in server %s",
                  messageId, iManager.getLocalNodeName())));
    }
  }

  @Override
  public long getDistributedTimeout() {
    return GlobalConfiguration.DISTRIBUTED_HEARTBEAT_TIMEOUT.getValueAsLong();
  }

  @Override
  public long getSynchronousTimeout(int iSynchNodes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getTotalTimeout(int iTotalNodes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RESULT_STRATEGY getResultStrategy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getNodeSource() {
    return this.nodeSource;
  }

  @Override
  public void setNodeSource(String nodeSource) {
    this.nodeSource = nodeSource;
  }

  @Override
  public boolean isIdempotent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNodeOnlineRequired() {
    return true;
  }

  @Override
  public boolean isUsingDatabase() {
    return false;
  }

  @Override
  public int getFactoryId() {
    return FACTORYID;
  }

  @Override
  public void toStream(DataOutput out) throws IOException {
    out.writeInt(task.getMessageId());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    DataOutputStream stream = new DataOutputStream(outputStream);
    task.write(stream);
    byte[] bytes = outputStream.toByteArray();
    out.writeInt(bytes.length);
    out.write(outputStream.toByteArray());
  }

  @Override
  public void fromStream(DataInput in, ORemoteTaskFactory factory) throws IOException {
    messageId = in.readInt();
    int size = in.readInt();
    byte[] message = new byte[size];
    in.readFully(message, 0, size);
    task = createOperation(messageId);
    if (task != null) {
      task.read(new DataInputStream(new ByteArrayInputStream(message)));
    }
  }

  private static NodeOperation createOperation(int messageId) {
    NodeOperationFactory factory = MESSAGES.get(messageId);

    if (factory != null) {
      try {
        return factory.request.call();
      } catch (Exception e) {
        LogManager.instance()
            .warn(NodeOperationTask.class, "Cannot create node operation from id %d", messageId);
        return null;
      }
    } else {
      return null;
    }
  }

  private static class NodeOperationFactory {

    private final Callable<NodeOperation> request;
    private final Callable<NodeOperationResponse> response;

    public NodeOperationFactory(
        Callable<NodeOperation> request, Callable<NodeOperationResponse> response) {
      this.request = request;
      this.response = response;
    }
  }

  public static void register(
      int messageId,
      Callable<NodeOperation> requestFactory,
      Callable<NodeOperationResponse> responseFactory) {
    MESSAGES.put(messageId, new NodeOperationFactory(requestFactory, responseFactory));
  }

  public static NodeOperationResponse createOperationResponse(int messageId) {
    NodeOperationFactory factory = MESSAGES.get(messageId);
    if (factory != null) {
      try {
        return factory.response.call();
      } catch (Exception e) {
        LogManager.instance()
            .warn(
                NodeOperationTask.class,
                "Cannot create node operation response from id %d",
                messageId);
        return null;
      }
    } else {
      return null;
    }
  }
}
