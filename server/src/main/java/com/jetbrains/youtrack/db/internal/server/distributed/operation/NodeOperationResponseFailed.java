package com.jetbrains.youtrack.db.internal.server.distributed.operation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 */
public class NodeOperationResponseFailed implements NodeOperationResponse {

  private Integer code;
  private String message;

  public NodeOperationResponseFailed() {
  }

  public NodeOperationResponseFailed(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(code);
    out.writeUTF(this.message);
  }

  @Override
  public void read(DataInput in) throws IOException {

    this.code = in.readInt();
    this.message = in.readUTF();
  }

  @Override
  public boolean isOk() {
    return false;
  }
}
