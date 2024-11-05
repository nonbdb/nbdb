/*
 *
 *  *  Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
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
 *  * For more information: http://orientdb.com
 *
 */
package com.orientechnologies.orient.server.network.protocol.http;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.util.OCallable;
import com.orientechnologies.orient.core.config.OContextConfiguration;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.server.OClientConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

/**
 * Maintains information about current HTTP response.
 *
 * @author Luca Garulli (l.garulli--(at)--orientdb.com)
 */
public abstract class OHttpResponseAbstract implements OHttpResponse {

  public static final char[] URL_SEPARATOR = {'/'};
  protected static final Charset utf8 = StandardCharsets.UTF_8;

  private final String httpVersion;
  private final OutputStream out;
  private final OContextConfiguration contextConfiguration;
  private String headers;
  private String[] additionalHeaders;
  private String characterSet;
  private String contentType;
  private String serverInfo;

  private final Map<String, String> headersMap = new HashMap<>();

  private String sessionId;
  private String callbackFunction;
  private String contentEncoding;
  private String staticEncoding;
  private boolean sendStarted = false;
  private String content;
  private int code;
  private boolean keepAlive = true;
  private boolean jsonErrorResponse = true;
  private boolean sameSiteCookie = true;
  private OClientConnection connection;
  private boolean streaming = OGlobalConfiguration.NETWORK_HTTP_STREAMING.getValueAsBoolean();

  public OHttpResponseAbstract(
      final OutputStream iOutStream,
      final String iHttpVersion,
      final String[] iAdditionalHeaders,
      final String iResponseCharSet,
      final String iServerInfo,
      final String iSessionId,
      final String iCallbackFunction,
      final boolean iKeepAlive,
      OClientConnection connection,
      OContextConfiguration contextConfiguration) {
    setStreaming(
        contextConfiguration.getValueAsBoolean(OGlobalConfiguration.NETWORK_HTTP_STREAMING));
    out = iOutStream;
    httpVersion = iHttpVersion;
    setAdditionalHeaders(iAdditionalHeaders);
    setCharacterSet(iResponseCharSet);
    setServerInfo(iServerInfo);
    setSessionId(iSessionId);
    setCallbackFunction(iCallbackFunction);
    setKeepAlive(iKeepAlive);
    this.setConnection(connection);
    this.contextConfiguration = contextConfiguration;
  }

  @Override
  public abstract void send(
      int iCode, String iReason, String iContentType, Object iContent, String iHeaders)
      throws IOException;

  @Override
  public abstract void writeStatus(int iStatus, String iReason) throws IOException;

  @Override
  public void writeHeaders(final String iContentType) throws IOException {
    writeHeaders(iContentType, true);
  }

  @Override
  public void writeHeaders(final String iContentType, final boolean iKeepAlive) throws IOException {
    if (getHeaders() != null) {
      writeLine(getHeaders());
    }

    // Set up a date formatter that prints the date in the Http-date format as
    // per RFC 7231, section 7.1.1.1
    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    writeLine("Date: " + sdf.format(new Date()));
    writeLine("Content-Type: " + iContentType + "; charset=" + getCharacterSet());
    writeLine("Server: " + getServerInfo());
    writeLine("Connection: " + (iKeepAlive ? "Keep-Alive" : "close"));

    // SET CONTENT ENCDOING
    if (getContentEncoding() != null && getContentEncoding().length() > 0) {
      writeLine("Content-Encoding: " + getContentEncoding());
    }

    // INCLUDE COMMON CUSTOM HEADERS
    if (getAdditionalHeaders() != null) {
      for (String h : getAdditionalHeaders()) {
        writeLine(h);
      }
    }
  }

  @Override
  public void writeLine(final String iContent) throws IOException {
    writeContent(iContent);
    getOut().write(OHttpUtils.EOL);
  }

  @Override
  public void writeContent(final String iContent) throws IOException {
    if (iContent != null) {
      getOut().write(iContent.getBytes(utf8));
    }
  }

  @Override
  public void writeResult(final Object result, ODatabaseDocumentInternal databaseDocumentInternal)
      throws InterruptedException, IOException {
    writeResult(result, null, null, null, databaseDocumentInternal);
  }

  @Override
  public void writeResult(
      Object iResult,
      final String iFormat,
      final String iAccept,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws InterruptedException, IOException {
    writeResult(iResult, iFormat, iAccept, null, databaseDocumentInternal);
  }

  @Override
  public void writeResult(
      Object iResult,
      final String iFormat,
      final String iAccept,
      final Map<String, Object> iAdditionalProperties,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws InterruptedException, IOException {
    writeResult(iResult, iFormat, iAccept, iAdditionalProperties, null, databaseDocumentInternal);
  }

  @Override
  public void writeResult(
      Object iResult,
      final String iFormat,
      final String iAccept,
      final Map<String, Object> iAdditionalProperties,
      final String mode,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws InterruptedException, IOException {
    if (iResult == null) {
      send(OHttpUtils.STATUS_OK_NOCONTENT_CODE, "", OHttpUtils.CONTENT_TEXT_PLAIN, null, null);
    } else {
      final Object newResult;

      if (iResult instanceof Map) {
        ODocument doc = new ODocument();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) iResult).entrySet()) {
          String key = keyFromMapObject(entry.getKey());
          doc.field(key, entry.getValue());
        }
        newResult = Collections.singleton(doc).iterator();
      } else if (OMultiValue.isMultiValue(iResult)
          && (OMultiValue.getSize(iResult) > 0
              && !((OMultiValue.getFirstValue(iResult) instanceof OIdentifiable)
                  || ((OMultiValue.getFirstValue(iResult) instanceof OResult))))) {
        newResult = Collections.singleton(new ODocument().field("value", iResult)).iterator();
      } else if (iResult instanceof OIdentifiable) {
        // CONVERT SINGLE VALUE IN A COLLECTION
        newResult = Collections.singleton(iResult).iterator();
      } else if (iResult instanceof Iterable<?>) {
        newResult = ((Iterable<OIdentifiable>) iResult).iterator();
      } else if (OMultiValue.isMultiValue(iResult)) {
        newResult = OMultiValue.getMultiValueIterator(iResult);
      } else {
        newResult = Collections.singleton(new ODocument().field("value", iResult)).iterator();
      }

      if (newResult == null) {
        send(OHttpUtils.STATUS_OK_NOCONTENT_CODE, "", OHttpUtils.CONTENT_TEXT_PLAIN, null, null);
      } else {
        writeRecords(
            newResult,
            null,
            iFormat,
            iAccept,
            iAdditionalProperties,
            mode,
            databaseDocumentInternal);
      }
    }
  }

  @Override
  public void writeRecords(
      final Object iRecords, ODatabaseDocumentInternal databaseDocumentInternal)
      throws IOException {
    writeRecords(iRecords, null, null, null, null, databaseDocumentInternal);
  }

  @Override
  public void writeRecords(
      final Object iRecords,
      final String iFetchPlan,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws IOException {
    writeRecords(iRecords, iFetchPlan, null, null, null, databaseDocumentInternal);
  }

  @Override
  public void writeRecords(
      final Object iRecords,
      final String iFetchPlan,
      String iFormat,
      final String accept,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws IOException {
    writeRecords(iRecords, iFetchPlan, iFormat, accept, null, databaseDocumentInternal);
  }

  @Override
  public void writeRecords(
      final Object iRecords,
      final String iFetchPlan,
      String iFormat,
      final String accept,
      final Map<String, Object> iAdditionalProperties,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws IOException {
    writeRecords(
        iRecords,
        iFetchPlan,
        iFormat,
        accept,
        iAdditionalProperties,
        null,
        databaseDocumentInternal);
  }

  @Override
  public void writeRecords(
      final Object iRecords,
      final String iFetchPlan,
      String iFormat,
      final String accept,
      final Map<String, Object> iAdditionalProperties,
      final String mode,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws IOException {
    if (iRecords == null) {
      send(OHttpUtils.STATUS_OK_NOCONTENT_CODE, "", OHttpUtils.CONTENT_TEXT_PLAIN, null, null);
      return;
    }
    final int size = OMultiValue.getSize(iRecords);
    final Iterator<?> it = OMultiValue.getMultiValueIterator(iRecords);

    if (accept != null && accept.contains("text/csv")) {
      sendStream(
          OHttpUtils.STATUS_OK_CODE,
          OHttpUtils.STATUS_OK_DESCRIPTION,
          OHttpUtils.CONTENT_CSV,
          "data.csv",
          new OCallable<Void, OChunkedResponse>() {

            @Override
            public Void call(final OChunkedResponse iArgument) {
              final LinkedHashSet<String> colNames = new LinkedHashSet<String>();
              final List<OElement> records = new ArrayList<OElement>();

              // BROWSE ALL THE RECORD TO HAVE THE COMPLETE COLUMN
              // NAMES LIST
              while (it.hasNext()) {
                final Object r = it.next();

                if (r instanceof OResult) {

                  OResult result = (OResult) r;
                  records.add(result.toElement());

                  result
                      .toElement()
                      .getSchemaType()
                      .ifPresent(x -> x.properties().forEach(prop -> colNames.add(prop.getName())));
                  for (String fieldName : result.getPropertyNames()) {
                    colNames.add(fieldName);
                  }

                } else if (r != null && r instanceof OIdentifiable) {
                  final ORecord rec = ((OIdentifiable) r).getRecord();
                  if (rec != null) {
                    if (rec instanceof ODocument) {
                      final ODocument doc = (ODocument) rec;
                      records.add(doc);

                      for (String fieldName : doc.fieldNames()) {
                        colNames.add(fieldName);
                      }
                    }
                  }
                }
              }

              final List<String> orderedColumns = new ArrayList<String>(colNames);

              try {
                // WRITE THE HEADER
                for (int col = 0; col < orderedColumns.size(); ++col) {
                  if (col > 0) {
                    iArgument.write(',');
                  }

                  iArgument.write(orderedColumns.get(col).getBytes());
                }
                iArgument.write(OHttpUtils.EOL);

                // WRITE EACH RECORD
                for (OElement doc : records) {
                  for (int col = 0; col < orderedColumns.size(); ++col) {
                    if (col > 0) {
                      iArgument.write(',');
                    }

                    Object value = doc.getProperty(orderedColumns.get(col));
                    if (value != null) {
                      if (!(value instanceof Number)) {
                        value = "\"" + value + "\"";
                      }

                      iArgument.write(value.toString().getBytes());
                    }
                  }
                  iArgument.write(OHttpUtils.EOL);
                }

                iArgument.flush();

              } catch (IOException e) {
                OLogManager.instance().error(this, "HTTP response: error on writing records", e);
              }

              return null;
            }
          });
    } else {
      if (iFormat == null) {
        iFormat = OHttpResponse.JSON_FORMAT;
      } else {
        iFormat = OHttpResponse.JSON_FORMAT + "," + iFormat;
      }

      final String sendFormat = iFormat;
      if (isStreaming()) {
        sendStream(
            OHttpUtils.STATUS_OK_CODE,
            OHttpUtils.STATUS_OK_DESCRIPTION,
            OHttpUtils.CONTENT_JSON,
            null,
            iArgument -> {
              try {
                OutputStreamWriter writer = new OutputStreamWriter(iArgument);
                writeRecordsOnStream(
                    iFetchPlan,
                    sendFormat,
                    iAdditionalProperties,
                    it,
                    writer,
                    databaseDocumentInternal);
                writer.flush();
              } catch (IOException e) {
                OLogManager.instance()
                    .error(this, "Error during writing of records to the HTTP response", e);
              }
              return null;
            });
      } else {
        final StringWriter buffer = new StringWriter();
        writeRecordsOnStream(
            iFetchPlan, iFormat, iAdditionalProperties, it, buffer, databaseDocumentInternal);
        send(
            OHttpUtils.STATUS_OK_CODE,
            OHttpUtils.STATUS_OK_DESCRIPTION,
            OHttpUtils.CONTENT_JSON,
            buffer.toString(),
            null);
      }
    }
  }

  private void writeRecordsOnStream(
      String iFetchPlan,
      String iFormat,
      Map<String, Object> iAdditionalProperties,
      Iterator<?> it,
      Writer buffer,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws IOException {
    final OJSONWriter json = new OJSONWriter(buffer, iFormat);
    json.beginObject();

    final String format = iFetchPlan != null ? iFormat + ",fetchPlan:" + iFetchPlan : iFormat;

    // WRITE RECORDS
    json.beginCollection(-1, true, "result");
    formatMultiValue(it, buffer, format, databaseDocumentInternal);
    json.endCollection(-1, true);

    if (iAdditionalProperties != null) {
      for (Map.Entry<String, Object> entry : iAdditionalProperties.entrySet()) {

        final Object v = entry.getValue();
        if (OMultiValue.isMultiValue(v)) {
          json.beginCollection(-1, true, entry.getKey());
          formatMultiValue(
              OMultiValue.getMultiValueIterator(v), buffer, format, databaseDocumentInternal);
          json.endCollection(-1, true);
        } else {
          json.writeAttribute(entry.getKey(), v);
        }

        if (Thread.currentThread().isInterrupted()) {
          break;
        }
      }
    }

    json.endObject();
  }

  @Override
  public void formatMultiValue(
      final Iterator<?> iIterator,
      final Writer buffer,
      final String format,
      ODatabaseDocumentInternal databaseDocumentInternal)
      throws IOException {
    if (iIterator != null) {
      int counter = 0;
      String objectJson;

      while (iIterator.hasNext()) {
        final Object entry = iIterator.next();
        if (entry != null) {
          if (counter++ > 0) {
            buffer.append(", ");
          }

          if (entry instanceof OResult) {
            objectJson = ((OResult) entry).toJSON();
            buffer.append(objectJson);
          } else if (entry instanceof OIdentifiable) {
            ORecord rec = ((OIdentifiable) entry).getRecord();
            if (rec != null) {
              try {
                if (rec.getIdentity().isValid() && rec.isUnloaded()) {
                  rec = databaseDocumentInternal.bindToSession(rec);
                }

                objectJson = rec.toJSON(format);

                buffer.append(objectJson);
              } catch (Exception e) {
                OLogManager.instance()
                    .error(this, "Error transforming record " + rec.getIdentity() + " to JSON", e);
              }
            }
          } else if (OMultiValue.isMultiValue(entry)) {
            buffer.append("[");
            formatMultiValue(
                OMultiValue.getMultiValueIterator(entry), buffer, format, databaseDocumentInternal);
            buffer.append("]");
          } else {
            buffer.append(OJSONWriter.writeValue(entry, format));
          }
        }
        checkConnection();
      }
    }
  }

  @Override
  public void writeRecord(final ORecord iRecord) throws IOException {
    writeRecord(iRecord, null, null);
  }

  @Override
  public void writeRecord(final ORecord iRecord, final String iFetchPlan, String iFormat)
      throws IOException {
    if (iFormat == null) {
      iFormat = OHttpResponse.JSON_FORMAT;
    }

    final String format = iFetchPlan != null ? iFormat + ",fetchPlan:" + iFetchPlan : iFormat;
    if (iRecord != null) {
      send(
          OHttpUtils.STATUS_OK_CODE,
          OHttpUtils.STATUS_OK_DESCRIPTION,
          OHttpUtils.CONTENT_JSON,
          iRecord.toJSON(format),
          OHttpUtils.HEADER_ETAG + iRecord.getVersion());
    }
  }

  @Override
  public abstract void sendStream(
      int iCode, String iReason, String iContentType, InputStream iContent, long iSize)
      throws IOException;

  @Override
  public abstract void sendStream(
      int iCode,
      String iReason,
      String iContentType,
      InputStream iContent,
      long iSize,
      String iFileName)
      throws IOException;

  @Override
  public abstract void sendStream(
      int iCode,
      String iReason,
      String iContentType,
      InputStream iContent,
      long iSize,
      String iFileName,
      Map<String, String> additionalHeaders)
      throws IOException;

  @Override
  public abstract void sendStream(
      int iCode,
      String iReason,
      String iContentType,
      String iFileName,
      OCallable<Void, OChunkedResponse> iWriter)
      throws IOException;

  // Compress content string
  @Override
  public byte[] compress(String jsonStr) {
    if (jsonStr == null || jsonStr.length() == 0) {
      return null;
    }
    GZIPOutputStream gout = null;
    ByteArrayOutputStream baos = null;
    try {
      byte[] incoming = jsonStr.getBytes(StandardCharsets.UTF_8);
      baos = new ByteArrayOutputStream();
      gout = new GZIPOutputStream(baos, 16384); // 16KB
      gout.write(incoming);
      gout.finish();
      return baos.toByteArray();
    } catch (Exception ex) {
      OLogManager.instance().error(this, "Error on compressing HTTP response", ex);
    } finally {
      try {
        if (gout != null) {
          gout.close();
        }
        if (baos != null) {
          baos.close();
        }
      } catch (Exception ex) {
      }
    }
    return null;
  }

  /**
   * Stores additional headers to send
   */
  @Override
  @Deprecated
  public void setHeader(final String iHeader) {
    setHeaders(iHeader);
  }

  @Override
  public OutputStream getOutputStream() {
    return getOut();
  }

  @Override
  public void flush() throws IOException {
    getOut().flush();
    if (!isKeepAlive()) {
      getOut().close();
    }
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @Override
  public String getContentEncoding() {
    return contentEncoding;
  }

  @Override
  public void setContentEncoding(String contentEncoding) {
    this.contentEncoding = contentEncoding;
  }

  @Override
  public void setStaticEncoding(String contentEncoding) {
    this.staticEncoding = contentEncoding;
  }

  @Override
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public String getContent() {
    return content;
  }

  @Override
  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public void setCode(int code) {
    this.code = code;
  }

  @Override
  public void setJsonErrorResponse(boolean jsonErrorResponse) {
    this.jsonErrorResponse = jsonErrorResponse;
  }

  private String keyFromMapObject(Object key) {
    if (key instanceof String) {
      return (String) key;
    }
    return "" + key;
  }

  @Override
  public void setStreaming(boolean streaming) {
    this.streaming = streaming;
  }

  @Override
  public String getHttpVersion() {
    return httpVersion;
  }

  @Override
  public OutputStream getOut() {
    return out;
  }

  @Override
  public String getHeaders() {
    return headers;
  }

  @Override
  public void setHeaders(String headers) {
    this.headers = headers;
  }

  @Override
  public String[] getAdditionalHeaders() {
    return additionalHeaders;
  }

  @Override
  public void setAdditionalHeaders(String[] additionalHeaders) {
    this.additionalHeaders = additionalHeaders;
  }

  @Override
  public String getCharacterSet() {
    return characterSet;
  }

  @Override
  public void setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
  }

  @Override
  public String getServerInfo() {
    return serverInfo;
  }

  @Override
  public void setServerInfo(String serverInfo) {
    this.serverInfo = serverInfo;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public String getCallbackFunction() {
    return callbackFunction;
  }

  @Override
  public void setCallbackFunction(String callbackFunction) {
    this.callbackFunction = callbackFunction;
  }

  @Override
  public String getStaticEncoding() {
    return staticEncoding;
  }

  @Override
  public boolean isSendStarted() {
    return sendStarted;
  }

  @Override
  public void setSendStarted(boolean sendStarted) {
    this.sendStarted = sendStarted;
  }

  @Override
  public boolean isKeepAlive() {
    return keepAlive;
  }

  @Override
  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  @Override
  public boolean isJsonErrorResponse() {
    return jsonErrorResponse;
  }

  @Override
  public OClientConnection getConnection() {
    return connection;
  }

  @Override
  public void setConnection(OClientConnection connection) {
    this.connection = connection;
  }

  @Override
  public boolean isStreaming() {
    return streaming;
  }

  @Override
  public void setSameSiteCookie(boolean sameSiteCookie) {
    this.sameSiteCookie = sameSiteCookie;
  }

  @Override
  public boolean isSameSiteCookie() {
    return sameSiteCookie;
  }

  @Override
  public OContextConfiguration getContextConfiguration() {
    return contextConfiguration;
  }

  @Override
  public void addHeader(String name, String value) {
    headersMap.put(name, value);
  }

  @Override
  public Map<String, String> getHeadersMap() {
    return Collections.unmodifiableMap(headersMap);
  }
}
