/*
 * Copyright © 2015-2021 I.N.F.N.
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

package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.DeploymentType;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeploymentService {

  public Page<Deployment> getDeployments(Pageable pageable, @Nullable String owner,
      @Nullable String userGroup);

  public Deployment getDeployment(String id);

  /**
   * Infer deployment type from provider.
   * @param deploymentProvider the provider
  */
  public static DeploymentType inferDeploymentType(
                DeploymentProvider deploymentProvider) {
    switch (deploymentProvider) {
      case CHRONOS:
        return DeploymentType.CHRONOS;
      case MARATHON:
        return DeploymentType.MARATHON;
      case QCG:
        return DeploymentType.QCG;
      case HEAT:
      case IM:
      default:
        return DeploymentType.TOSCA;
    }
  }

  public Deployment createDeployment(DeploymentRequest request, OidcEntity owner,
      OidcTokenId requestedWithToken);

  public void updateDeployment(String id, DeploymentRequest request,
      OidcTokenId requestedWithToken);

  public void resetDeployment(String id, String status,
      OidcTokenId requestedWithToken);

  public void deleteDeployment(String id, OidcTokenId requestedWithToken);

  public String getDeploymentLog(String id, OidcTokenId requestedWithToken);

  public String getDeploymentExtendedInfo(String id, OidcTokenId requestedWithToken);

  public void throwIfNotOwned(Deployment deployment);

}
