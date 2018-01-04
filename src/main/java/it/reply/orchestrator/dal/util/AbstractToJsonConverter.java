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

package it.reply.orchestrator.dal.util;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.core.type.TypeReference;

import it.reply.utils.json.JsonUtility;

import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import javax.persistence.AttributeConverter;

@Slf4j
public abstract class AbstractToJsonConverter<T>
    implements AttributeConverter<@Nullable T, String> {

  private final TypeReference<T> typeReference;

  protected AbstractToJsonConverter(TypeReference<T> typeReference) {
    this.typeReference = Preconditions.checkNotNull(typeReference);
  }

  @Override
  public String convertToDatabaseColumn(T optionalAttribute) {
    String convertedValue = Optional
        .ofNullable(optionalAttribute)
        .map(attribute -> {
          try {
            return JsonUtility.serializeJson(attribute);
          } catch (Exception ex) {
            throw new IllegalArgumentException("Error serializing attribute <" + optionalAttribute
                + "> of type " + typeReference + " to JSON DB value", ex);
          }
        })
        .orElse(null);
    LOG.trace("Converted attribute {} of type {} to JSON DB value <{}>", optionalAttribute,
        typeReference, convertedValue);
    return convertedValue;
  }

  @Override
  @Nullable
  public T convertToEntityAttribute(String optionalDbData) {
    @SuppressWarnings("null")
    @Nullable
    T convertedValue = Optional
        .ofNullable(optionalDbData)
        .map(dbData -> {
          try {
            return JsonUtility.deserializeJson(dbData, typeReference);
          } catch (Exception ex) {
            throw new IllegalArgumentException("Error de-serializing DB data with value <"
                + optionalDbData + "> to object of type " + typeReference, ex);
          }
        })
        .orElse(null);
    LOG.trace("Converted JSON DB value <{}> to attribute {} of type {}", optionalDbData,
        convertedValue, typeReference);
    return convertedValue;
  }

}
