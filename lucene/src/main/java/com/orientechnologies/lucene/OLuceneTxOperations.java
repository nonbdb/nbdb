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

package com.orientechnologies.lucene;

import com.jetbrains.youtrack.db.internal.core.db.record.Identifiable;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class OLuceneTxOperations {

  public final Set<Identifiable> removed = new HashSet<Identifiable>();
  public final Set<Identifiable> added = new HashSet<Identifiable>();
}
