package com.orientechnologies.lucene.engine;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;

/**
 *
 */
public class OLuceneIndexEngineUtils {

  public static void sendTotalHits(String indexName, OCommandContext context, long totalHits) {
    if (context != null) {

      if (context.getVariable("totalHits") == null) {
        context.setVariable("totalHits", totalHits);
      } else {
        context.setVariable("totalHits", null);
      }
      context.setVariable((indexName + ".totalHits").replace(".", "_"), totalHits);
    }
  }

  public static void sendLookupTime(
      String indexName,
      OCommandContext context,
      final TopDocs docs,
      final Integer limit,
      long startFetching) {
    if (context != null) {

      final long finalTime = System.currentTimeMillis() - startFetching;
      context.setVariable(
          (indexName + ".lookupTime").replace(".", "_"),
          new HashMap<String, Object>() {
            {
              put("limit", limit);
              put("totalTime", finalTime);
              put("totalHits", docs.totalHits);
              put("returnedHits", docs.scoreDocs.length);
              if (!Float.isNaN(docs.getMaxScore())) {
                put("maxScore", docs.getMaxScore());
              }
            }
          });
    }
  }

  public static List<SortField> buildSortFields(ODocument metadata) {
    List<Map<String, Object>> sortConf =
        Optional.ofNullable(metadata.<List<Map<String, Object>>>getProperty("sort"))
            .orElse(Collections.emptyList());

    final List<SortField> fields =
        sortConf.stream().map(d -> buildSortField(d)).collect(Collectors.toList());

    return fields;
  }

  /**
   * Builds {@link SortField} from a configuration {@link ODocument}
   *
   * @param conf
   * @return
   */
  public static SortField buildSortField(ODocument conf) {

    return buildSortField(conf.toMap());
  }

  /**
   * Builds a {@link SortField} from a configuration map. The map can contains up to three fields:
   * field (name), reverse (true/false) and type {@link SortField.Type}.
   *
   * @param conf
   * @return
   */
  public static SortField buildSortField(Map<String, Object> conf) {

    final String field = Optional.ofNullable((String) conf.get("field")).orElse(null);
    final String type =
        Optional.ofNullable(((String) conf.get("type")).toUpperCase())
            .orElse(SortField.Type.STRING.name());
    final Boolean reverse = Optional.ofNullable((Boolean) conf.get("reverse")).orElse(false);

    SortField sortField = new SortField(field, SortField.Type.valueOf(type), reverse);

    return sortField;
  }

}
