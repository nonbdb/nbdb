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

package com.orientechnologies.lucene.tests;

import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.io.InputStream;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class OLuceneQueryErrorTest extends OLuceneBaseTest {

  @Before
  public void init() {
    InputStream stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql");

    db.execute("sql", getScriptFromStream(stream));

    db.command("create index Song.title on Song (title) FULLTEXT ENGINE LUCENE");
  }

  @Test
  public void testQueryError() {

    String query = "select * from Song where search_class(\"\")=true ";
    OResultSet result = db.query(query);

    Assertions.assertThat(result).isEmpty();
    result.close();
  }
}
