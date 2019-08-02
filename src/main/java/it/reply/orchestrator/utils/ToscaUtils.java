/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.utils;

import it.reply.orchestrator.exception.service.ToscaException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.normative.types.StringType;
import org.springframework.beans.BeanUtils;

@UtilityClass
public class ToscaUtils {

  /**
   * If the object is a {@link PropertyValue}, it returns the wrapped value, otherwise it returns
   * the object itself.
   *
   * @param wrappedObject
   *     the wrapped object
   * @return the unwrapped object
   */
  public Object unwrapPropertyValue(Object wrappedObject) {
    if (wrappedObject instanceof PropertyValue) {
      return ((PropertyValue) wrappedObject).getValue();
    } else {
      return wrappedObject;
    }
  }


  /**
   * It wraps an object inside an {@link AbstractPropertyValue}.
   *
   * @param v
   *     the original object
   * @return the wrapped object
   */
  public AbstractPropertyValue wrapToPropertyValue(Object v) {
    if (v == null || v instanceof AbstractPropertyValue) {
      return (AbstractPropertyValue) v;
    } else if (v instanceof Map) {
      return new ComplexPropertyValue((Map<String, Object>) v);
    } else if (v instanceof List) {
      return new ListPropertyValue((List<Object>) v);
    } else if (CommonUtils.isPrimitive(v)) {
      return new ScalarPropertyValue(String.valueOf(v));
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + v + " to class " + AbstractPropertyValue.class.getName());
    }
  }

  public Optional<String> extractScalar(Map<String, AbstractPropertyValue> properties, String key) {
    return extractScalar(properties, key, StringType.class);
  }

  /**
   * It extracts a scalar value from a properties map and parses it.
   *
   * @param properties
   *     the properties map
   * @param key
   *     the key of the property
   * @param clazz
   *     the {@link IPropertyType} expected
   * @return the parsed property
   */
  public <T extends IPropertyType<V>, V> Optional<V> extractScalar(
      Map<String, AbstractPropertyValue> properties, String key, Class<T> clazz) {
    return CommonUtils
        .<ScalarPropertyValue>optionalCast(CommonUtils.getFromOptionalMap(properties, key))
        .map(ScalarPropertyValue::getValue)
        .map(value -> ToscaUtils.parseScalar(value, clazz));
  }

  /**
   * It parses a {@link String} into a scalar value.
   *
   * @param value
   *     the String to parse
   * @param clazz
   *     the {@link IPropertyType} expected
   * @return the parsed value
   */
  public <T extends IPropertyType<V>, V> V parseScalar(String value, Class<T> clazz) {
    T propertyParser = BeanUtils.instantiate(clazz);
    try {
      return propertyParser.parse(value);
    } catch (InvalidPropertyValueException ex) {
      throw new ToscaException(String.format("Error parsing scalar value <%s> as <%s>",
          value, propertyParser.getTypeName()), ex);
    }
  }

  public Optional<List<Object>> extractList(Map<String, AbstractPropertyValue> properties,
      String key) {
    return extractList(properties, key, Function.identity());
  }

  /**
   * It extracts an optional {@link List} from a properties map and applies a mapping function on
   * every value before returning.
   *
   * @param properties
   *     the properties map
   * @param key
   *     the key of the property
   * @param mapper
   *     the mapping function
   * @return the parsed list
   */
  public <V> Optional<List<V>> extractList(Map<String, AbstractPropertyValue> properties,
      String key, Function<Object, V> mapper) {
    return CommonUtils
        .<ListPropertyValue>optionalCast(CommonUtils.getFromOptionalMap(properties, key))
        .map(ListPropertyValue::getValue)
        .map(Collection::stream)
        .map(stream -> stream.map(mapper).collect(Collectors.toList()));
  }

  /**
   * It extracts an optional {@link Map} from a properties map and applies a mapping function on
   * every value before returning.
   *
   * @param properties
   *     the properties map
   * @param key
   *     the key of the property
   * @param mapper
   *     the mapping function
   * @return the parsed map
   */
  public <V> Optional<Map<String, V>> extractMap(Map<String, AbstractPropertyValue> properties,
      String key, Function<Object, V> mapper) {
    return CommonUtils
        .<ComplexPropertyValue>optionalCast(CommonUtils.getFromOptionalMap(properties, key))
        .map(ComplexPropertyValue::getValue)
        .map(Map::entrySet)
        .map(Collection::stream)
        .map(stream -> stream
            .collect(Collectors.toMap(Entry::getKey, mapper.compose(Entry::getValue))));
  }

  public Optional<Map<String, Object>> extractMap(Map<String, AbstractPropertyValue> properties,
      String key) {
    return ToscaUtils.extractMap(properties, key, Function.identity());
  }

}
