/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.orientechnologies.security.auditing;

import com.jetbrains.youtrack.db.internal.common.parser.VariableParser;
import com.jetbrains.youtrack.db.internal.common.parser.VariableParserListener;
import com.jetbrains.youtrack.db.internal.core.security.AuditingOperation;

public abstract class OAuditingConfig {

  public boolean isEnabled(AuditingOperation operation) {
    return false;
  }

  public String formatMessage(final AuditingOperation op, final String subject) {
    return subject;
  }

  // Usage
  // message: The node ${node} has joined
  // varName: node
  // value: Node1
  // Returns: "The node Node1 has joined"
  protected String resolveMessage(final String message, final String varName, final String value) {
    return (String)
        VariableParser.resolveVariables(
            message,
            "${",
            "}",
            new VariableParserListener() {
              @Override
              public Object resolve(final String iVariable) {
                if (iVariable.equalsIgnoreCase(varName)) {
                  return value;
                }

                return null;
              }
            });
  }
}
