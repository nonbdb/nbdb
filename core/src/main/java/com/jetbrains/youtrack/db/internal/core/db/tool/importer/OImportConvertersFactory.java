package com.jetbrains.youtrack.db.internal.core.db.tool.importer;

import com.jetbrains.youtrack.db.internal.core.db.record.YTIdentifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.id.YTRID;
import com.jetbrains.youtrack.db.internal.core.id.YTRecordId;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class OImportConvertersFactory {

  public static final YTRID BROKEN_LINK = new YTRecordId(-1, -42);
  public static final OImportConvertersFactory INSTANCE = new OImportConvertersFactory();

  public OValuesConverter getConverter(Object value, OConverterData converterData) {
    if (value instanceof Map) {
      return new OMapConverter(converterData);
    }

    if (value instanceof List) {
      return new OListConverter(converterData);
    }

    if (value instanceof Set) {
      return new OSetConverter(converterData);
    }

    if (value instanceof RidBag) {
      return new ORidBagConverter(converterData);
    }

    if (value instanceof YTIdentifiable) {
      return new OLinkConverter(converterData);
    }

    return null;
  }
}