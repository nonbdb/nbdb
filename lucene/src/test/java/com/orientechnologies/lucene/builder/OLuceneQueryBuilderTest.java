package com.orientechnologies.lucene.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

import com.jetbrains.youtrack.db.internal.core.index.OIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.sql.parser.ParseException;
import java.util.Collections;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OLuceneQueryBuilderTest {

  private OIndexDefinition indexDef;

  @Before
  public void setUp() throws Exception {
    indexDef = Mockito.mock(OIndexDefinition.class);
    when(indexDef.getFields()).thenReturn(Collections.emptyList());
    when(indexDef.isAutomatic()).thenReturn(true);
    when(indexDef.getClassName()).thenReturn("Song");
  }

  @Test
  public void testUnmaskedQueryReporting() {
    final OLuceneQueryBuilder builder = new OLuceneQueryBuilder(OLuceneQueryBuilder.EMPTY_METADATA);

    final String invalidQuery = "+(song:private{}private)";
    try {
      builder.buildQuery(
          indexDef, invalidQuery, OLuceneQueryBuilder.EMPTY_METADATA, new EnglishAnalyzer());
    } catch (ParseException e) {
      assertThat(e.getMessage()).contains("Cannot parse", invalidQuery);
      return;
    }
    fail("Expected ParseException");
  }

  @Test
  public void testMaskedQueryReporting() {
    final OLuceneQueryBuilder builder = new OLuceneQueryBuilder(OLuceneQueryBuilder.EMPTY_METADATA);

    final String invalidQuery = "+(song:private{}private)";
    try {
      builder.buildQuery(indexDef, invalidQuery,
          Collections.singletonMap("reportQueryAs", "masked"),
          new EnglishAnalyzer());
    } catch (ParseException e) {
      assertThat(e.getMessage()).contains("Cannot parse", "masked").doesNotContain(invalidQuery);
      return;
    }
    fail("Expected ParseException");
  }
}
