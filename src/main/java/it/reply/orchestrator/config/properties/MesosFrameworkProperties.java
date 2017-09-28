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

package it.reply.orchestrator.config.properties;

import it.reply.orchestrator.exception.service.ToscaException;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

@Validated
@Data
@ToString(exclude = "password")
@NoArgsConstructor
public abstract class MesosFrameworkProperties implements InitializingBean {

  @NotNull
  @NonNull
  private URI url;

  @NotNull
  @NonNull
  private String username;

  @NotNull
  @NonNull
  private String password;

  @NotNull
  @NonNull
  private String localVolumesHostBasePath = "";

  /**
   * Generates a localVolumesHostPath appending the groupId to the base path.
   * 
   * @param groupId
   *          the groupId
   * @return the localVolumesHostPath
   */
  public String generateLocalVolumesHostPath(String groupId) {
    if (localVolumesHostBasePath.trim().isEmpty()) {
      throw new ToscaException("Error generating host path: no base host path has been provided");
    } else {
      return localVolumesHostBasePath.concat(groupId);
    }
  }

  @Override
  public void afterPropertiesSet() {
    localVolumesHostBasePath = StringUtils
        .defaultIfBlank(localVolumesHostBasePath, "")
        .trim();
    if (!localVolumesHostBasePath.isEmpty()) {
      if (!localVolumesHostBasePath.startsWith("/")) {
        throw new ValidationException(String.format(
            "Invalid local volume base path %s; it must start with a /", localVolumesHostBasePath));
      }
      if (!localVolumesHostBasePath.endsWith("/")) {
        localVolumesHostBasePath = localVolumesHostBasePath + "/";
      }
    }
  }

}
