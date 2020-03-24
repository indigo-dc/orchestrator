/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.dal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;

import it.reply.orchestrator.utils.JsonUtils;

import javax.persistence.AttributeConverter;

import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public abstract class AbstractToJsonConverter<@Nullable T>
    implements AttributeConverter<T, @Nullable String> {

  private final TypeReference<T> typeReference;

  protected AbstractToJsonConverter(TypeReference<T> typeReference) {
    this.typeReference = Preconditions.checkNotNull(typeReference);
  }

  @Override
  public String convertToDatabaseColumn(T optionalAttribute) {
    final String convertedValue;
    if (optionalAttribute == null) {
      convertedValue = null;
    } else {
      try {
        convertedValue = JsonUtils.serialize(optionalAttribute);
      } catch (JsonProcessingException ex) {
        throw new IllegalArgumentException("Error serializing attribute <" + optionalAttribute
            + "> of type " + typeReference + " to JSON DB value", ex);
      }
    }
    LOG.trace("Converted attribute {} of type {} to JSON DB value <{}>", optionalAttribute,
        typeReference, convertedValue);
    return convertedValue;
  }

  @Override
  public T convertToEntityAttribute(String optionalDbData) {
    final T convertedValue;
    if (optionalDbData == null) {
      convertedValue = null;
    } else {
      try {
        convertedValue = JsonUtils.deserialize(optionalDbData, typeReference);
      } catch (JsonProcessingException ex) {
        throw new IllegalArgumentException("Error de-serializing DB data with value <"
            + optionalDbData + "> to object of type " + typeReference, ex);
      }
    }
    LOG.trace("Converted JSON DB value <{}> to attribute {} of type {}", optionalDbData,
        convertedValue, typeReference);
    return convertedValue;
  }

}
