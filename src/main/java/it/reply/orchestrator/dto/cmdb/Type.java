package it.reply.orchestrator.dto.cmdb;

/*
 * Copyright © 2015-2016 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public enum Type {

  COMPUTE, STORAGE;

  private static Map<String, Type> namesMap = new HashMap<String, Type>(2);

  static {
    namesMap.put("compute", COMPUTE);
    namesMap.put("storage", STORAGE);
  }

  @JsonCreator
  public static Type forValue(String value) {
    return namesMap.get(StringUtils.lowerCase(value));
  }

  /**
   * Returns the String representation of the {@link Type}.
   * 
   * @return the enum value
   */
  @JsonValue
  public String toValue() {
    for (Entry<String, Type> entry : namesMap.entrySet()) {
      if (entry.getValue() == this) {
        return entry.getKey();
      }
    }

    return null; // or fail
  }
}
