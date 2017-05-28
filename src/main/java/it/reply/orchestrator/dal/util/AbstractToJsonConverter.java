/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import javax.persistence.AttributeConverter;

public abstract class AbstractToJsonConverter<T>
    implements AttributeConverter<@Nullable T, String> {

  private final TypeReference<T> typeReference;

  protected AbstractToJsonConverter(TypeReference<T> typeReference) {
    this.typeReference = Preconditions.checkNotNull(typeReference);
  }

  @Override
  public String convertToDatabaseColumn(T optionalAttribute) {
    return Optional.ofNullable(optionalAttribute).map(attribute -> {
      try {
        return JsonUtility.serializeJson(attribute);
      } catch (Exception ex) {
        throw new IllegalArgumentException(
            String.format("Error serializing attribute <%s> of type %s to JSON String",
                optionalAttribute, typeReference),
            ex);
      }
    }).orElse(null);
  }

  @Override
  public T convertToEntityAttribute(String optionalDbData) {
    return Optional.ofNullable(optionalDbData).<@Nullable T>map(dbData -> {
      try {
        return JsonUtility.deserializeJson(dbData, typeReference);
      } catch (Exception ex) {
        throw new IllegalArgumentException(
            String.format("Error de-serializing DB data with value <%s> to object of type %s",
                optionalDbData, typeReference),
            ex);
      }
    }).orElse(null);
  }

}
