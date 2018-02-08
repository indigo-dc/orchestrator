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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@JsonComponent
public class JsonUtils {

  private static ObjectMapper objectMapper;
  private static final CountDownLatch COUNTDOWN_LATCH = new CountDownLatch(1);

  JsonUtils(ObjectMapper objectMapper) {
    JsonUtils.objectMapper = objectMapper;
    COUNTDOWN_LATCH.countDown();
  }

  private static ObjectMapper getObjectMapper() {
    try {
      COUNTDOWN_LATCH.await();
      return objectMapper;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new Error("Thread interrupted while waiting for the objectMapper to be set", e);
    }
  }

  public static <T> @NonNull String serialize(@NonNull T object) throws JsonProcessingException {
    return getObjectMapper().writeValueAsString(object);
  }

  public static <T> @NonNull T deserialize(@NonNull String serializedObject,
      TypeReference<T> typeReference) throws JsonProcessingException {
    try {
      return getObjectMapper().readValue(serializedObject, typeReference);
    } catch (IOException e) {
      // Shouldn't happen as we're reading from a non-null string
      throw new RuntimeException(e);
    }
  }

  public static <T> @NonNull T deserialize(@NonNull String serializedObject,
      Class<T> typeReference) throws JsonProcessingException {
    try {
      return getObjectMapper().readValue(serializedObject, typeReference);
    } catch (IOException e) {
      // Shouldn't happen as we're reading from a non-null string
      throw new RuntimeException(e);
    }
  }

}
