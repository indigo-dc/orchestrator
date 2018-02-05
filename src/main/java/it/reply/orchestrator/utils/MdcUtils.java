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

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.MDC;

import java.util.List;
import java.util.Objects;

@UtilityClass
public class MdcUtils {

  private static final String REQUEST_ID = "request_id";
  private static final String DEPLOYMENT_ID = "deployment_id";

  private static final List<String> MDC_KEYS = ImmutableList.of(REQUEST_ID, DEPLOYMENT_ID);

  public static void clean() {
    MDC_KEYS.forEach(MDC::remove);
  }

  public static void setDeploymentId(@Nullable String deploymentId) {
    MDC.put(DEPLOYMENT_ID, deploymentId);
  }

  public static MdcCloseable setDeploymentIdCloseable(@Nullable String deploymentId) {
    return new MdcCloseable(DEPLOYMENT_ID, deploymentId);
  }

  public static @Nullable String getDeploymentId() {
    return MDC.get(DEPLOYMENT_ID);
  }

  public static void setRequestId(@Nullable String requestId) {
    MDC.put(REQUEST_ID, requestId);
  }

  public static MdcCloseable setRequestIdCloseable(@Nullable String requestId) {
    return new MdcCloseable(REQUEST_ID, requestId);
  }

  public static @Nullable String getRequestId() {
    return MDC.get(REQUEST_ID);
  }

  public static class MdcCloseable implements AutoCloseable {

    @NonNull
    private final String keyName;

    @Nullable
    private final String oldValue;

    @Nullable
    private final String newValue;

    private MdcCloseable(String keyName, String newValue) {
      this.keyName = Objects.requireNonNull(keyName);
      this.newValue = newValue;
      this.oldValue = MDC.get(keyName);
      if (!Objects.equals(oldValue, newValue)) {
        MDC.put(keyName, newValue);
      }
    }

    @Override
    public final void close() {
      if (oldValue == null) {
        MDC.remove(keyName);
      } else if (!oldValue.equals(newValue)) {
        MDC.put(keyName, oldValue);
      }
    }
  }

}
