package it.reply.orchestrator.dto.cmdb;

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
