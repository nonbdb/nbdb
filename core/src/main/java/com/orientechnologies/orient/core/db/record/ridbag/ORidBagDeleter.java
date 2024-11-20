package com.orientechnologies.orient.core.db.record.ridbag;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Created by tglman on 01/07/16.
 */
public final class ORidBagDeleter {

  public static void deleteAllRidBags(ODocument document) {
    for (var propertyName : document.getPropertyNamesInternal()) {
      var value = document.getPropertyInternal(propertyName);
      if (value instanceof ORidBag) {
        ((ORidBag) value).delete();
      }
    }
  }
}
