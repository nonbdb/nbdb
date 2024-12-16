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
package com.jetbrains.youtrack.db.internal.server.network.protocol.http;

import java.io.IOException;
import java.io.OutputStream;

public class ChunkedResponse extends OutputStream {

  private final HttpResponse response;
  private final byte[] buffer = new byte[8192];
  private int bufferSize = 0;

  public ChunkedResponse(final HttpResponse iHttpResponse) {
    response = iHttpResponse;
  }

  @Override
  public void write(int b) throws IOException {
    buffer[bufferSize++] = (byte) b;
    if (bufferSize >= buffer.length) {
      writeContent();
    }
  }

  @Override
  public void flush() throws IOException {
    writeContent();
    response.flush();
  }

  @Override
  public void close() throws IOException {
    writeContent();
    response.writeLine("0");
    response.writeLine(null);
  }

  protected void writeContent() throws IOException {
    if (bufferSize > 0) {
      response.writeLine(Integer.toHexString(bufferSize));
      response.getOutputStream().write(buffer, 0, bufferSize);
      response.writeLine(null);
      bufferSize = 0;
    }
  }
}
