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

package it.reply.orchestrator.dto.workflow;

import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.NonNull;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CloudServicesOrderedIterator extends WorkflowListIterator<CloudServiceWf> {

  public CloudServicesOrderedIterator(@NonNull List<CloudServiceWf> items) {
    super(new ArrayList<>(items));
  }

  /**
   * Get the current service if present and cast it to the specified class.
   *
   * @param <T>
   *     the object type
   * @param clazz
   *     the required class
   * @return the casted current service
   */
  public <T extends CloudService> T currentService(Class<T> clazz) {
    CloudService currentService = current().getCloudService();
    if (clazz.isInstance(currentService)) {
      return clazz.cast(currentService);
    } else {
      throw new DeploymentException("Current Cloud Service not of type " + clazz.getSimpleName());
    }
  }

  /**
   * Get the first service present (starting from current) of specified class.
   *
   * @param <T>
   *     the object type
   * @param clazz
   *     the required class
   * @return the casted service
   */
  public <T extends CloudService> T firstService(Class<T> clazz) {
    CloudService currentService = current().getCloudService();
    while (!clazz.isInstance(currentService) && hasNext()) {
      next();
      currentService = current().getCloudService();
    }
    if (clazz.isInstance(currentService)) {
      return clazz.cast(currentService);
    } else {
      throw new DeploymentException("Service of type "
          + clazz.getSimpleName() + " not present.");
    }
  }
}
