/*
 *
 *
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.orientechnologies.lucene.test;

import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.YTEntity;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class LuceneFacetTest extends BaseLuceneTest {

  @Before
  public void init() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass oClass = schema.createClass("Item");

    oClass.createProperty(db, "name", YTType.STRING);
    oClass.createProperty(db, "category", YTType.STRING);

    db.command(
            "create index Item.name_category on Item (name,category) FULLTEXT ENGINE LUCENE"
                + " METADATA { 'facetFields' : ['category']}")
        .close();

    YTDocument doc = new YTDocument("Item");
    doc.field("name", "Pioneer");
    doc.field("category", "Electronic/HiFi");

    db.begin();
    db.save(doc);
    db.commit();

    doc = new YTDocument("Item");
    doc.field("name", "Hitachi");
    doc.field("category", "Electronic/HiFi");

    db.begin();
    db.save(doc);
    db.commit();

    doc = new YTDocument("Item");
    doc.field("name", "Philips");
    doc.field("category", "Electronic/HiFi");

    db.begin();
    db.save(doc);
    db.commit();

    doc = new YTDocument("Item");
    doc.field("name", "HP");
    doc.field("category", "Electronic/Computer");

    db.begin();
    db.save(doc);
    db.commit();
  }

  @Test
  @Ignore
  public void baseFacetTest() {

    List<YTEntity> result =
        db.command("select *,$facet from Item where name lucene '(name:P*)' limit 1 ").stream()
            .map((o) -> o.toElement())
            .collect(Collectors.toList());

    Assert.assertEquals(result.size(), 1);

    List<YTDocument> facets = result.get(0).getProperty("$facet");

    Assert.assertEquals(facets.size(), 1);

    YTDocument facet = facets.get(0);
    Assert.assertEquals(facet.<Object>field("childCount"), 1);
    Assert.assertEquals(facet.<Object>field("value"), 2);
    Assert.assertEquals(facet.field("dim"), "category");

    List<YTDocument> labelsValues = facet.field("labelsValue");

    Assert.assertEquals(labelsValues.size(), 1);

    YTDocument labelValues = labelsValues.get(0);

    Assert.assertEquals(labelValues.<Object>field("value"), 2);
    Assert.assertEquals(labelValues.field("label"), "Electronic");

    result =
        db
            .command(
                "select *,$facet from Item where name lucene { 'q' : 'H*', 'drillDown' :"
                    + " 'category:Electronic' }  limit 1 ")
            .stream()
            .map((o) -> o.toElement())
            .collect(Collectors.toList());

    Assert.assertEquals(result.size(), 1);

    facets = result.get(0).getProperty("$facet");

    Assert.assertEquals(facets.size(), 1);

    facet = facets.get(0);

    Assert.assertEquals(facet.<Object>field("childCount"), 2);
    Assert.assertEquals(facet.<Object>field("value"), 2);
    Assert.assertEquals(facet.field("dim"), "category");

    labelsValues = facet.field("labelsValue");

    Assert.assertEquals(labelsValues.size(), 2);

    labelValues = labelsValues.get(0);

    Assert.assertEquals(labelValues.<Object>field("value"), 1);
    Assert.assertEquals(labelValues.field("label"), "HiFi");

    labelValues = labelsValues.get(1);

    Assert.assertEquals(labelValues.<Object>field("value"), 1);
    Assert.assertEquals(labelValues.field("label"), "Computer");
  }
}
