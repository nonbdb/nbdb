package com.orientechnologies.orient.core.exception;

/**
 *
 */
public class OLiveQueryInterruptedException extends OCoreException {

  public OLiveQueryInterruptedException(OCoreException exception) {
    super(exception);
  }

  public OLiveQueryInterruptedException(String message) {
    super(message);
  }
}
