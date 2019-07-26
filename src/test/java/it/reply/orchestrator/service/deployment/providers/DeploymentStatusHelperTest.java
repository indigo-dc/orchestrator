/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class DeploymentStatusHelperTest {

  @InjectMocks
  private DeploymentStatusHelperImpl deploymentStatusHelper;

  @Mock
  private DeploymentRepository deploymentRepository;

  Deployment deployment;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    deployment = ControllerTestUtils.createDeployment(2);
    Assertions.assertThat(deployment.getResources()).hasSize(2);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
  }

  @Parameters({ "CREATE_COMPLETE, CREATE_COMPLETE",
      "CREATE_FAILED, UNKNOWN",
      "CREATE_IN_PROGRESS, CREATE_COMPLETE",
      "DELETE_COMPLETE, DELETE_COMPLETE",
      "DELETE_FAILED, UNKNOWN",
      // we dont set it DELETE_COMPLETE because we're going to delete it so hibernate will complain
      // "DELETE_IN_PROGRESS, DELETE_COMPLETE"
      "UPDATE_COMPLETE, UPDATE_COMPLETE",
      "UPDATE_FAILED, UNKNOWN",
      "UPDATE_IN_PROGRESS, UPDATE_COMPLETE",
      "UNKNOWN, UNKNOWN" })
  @Test
  public void updateOnSuccess(Status initialStatus, Status expectedStatus) {

    deployment.setStatus(initialStatus);
    deploymentStatusHelper.updateOnSuccess(deployment.getId());
    Assertions.assertThat(deployment.getStatus()).isEqualTo(expectedStatus);
  }

  @Test
  public void updateOnSuccessDeleteInProgress() {
    deployment.setStatus(Status.DELETE_IN_PROGRESS);
    deploymentStatusHelper.updateOnSuccess(deployment.getId());
    Assertions.assertThat(deployment.getStatus()).isEqualTo(Status.DELETE_IN_PROGRESS);
    Mockito.verify(deploymentRepository, Mockito.times(1)).delete(deployment);
  }

  @Test
  public void updateOnSuccessNullSafe() {
    String id = UUID.randomUUID().toString();
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(null);
    deploymentStatusHelper.updateOnSuccess(id);
    // No NPE should be thrown
  }

  @Parameters({ "CREATE_COMPLETE, CREATE_FAILED",
      "CREATE_FAILED, CREATE_FAILED",
      "CREATE_IN_PROGRESS, CREATE_FAILED",
      "DELETE_COMPLETE, DELETE_FAILED",
      "DELETE_FAILED, DELETE_FAILED",
      "DELETE_IN_PROGRESS, DELETE_FAILED",
      "UPDATE_COMPLETE, UPDATE_FAILED",
      "UPDATE_FAILED, UPDATE_FAILED",
      "UPDATE_IN_PROGRESS, UPDATE_FAILED",
      "UNKNOWN, UNKNOWN" })
  @Test
  public void updateOnError(Status initialStatus, Status expectedStatus) {
    deployment.setStatus(initialStatus);
    deploymentStatusHelper.updateOnError(deployment.getId(), (String) null);
    Assertions.assertThat(deployment.getStatus()).isEqualTo(expectedStatus);
  }

  @Test
  public void updateOnErrorNullSafe() {
    String id = UUID.randomUUID().toString();
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(null);
    deploymentStatusHelper.updateOnError(id, (String) null);
    // No NPE should be thrown
  }

  @Test
  public void updateOnErrorWithBaseAndThrowableMessage() {
    String baseMessage = "baseMessage";
    String throwableMessage = "exceptionMessage";
    Throwable throwable = new Throwable(throwableMessage);
    deploymentStatusHelper.updateOnError(deployment.getId(), baseMessage, throwable);
    Assertions.assertThat(deployment.getStatusReason()).isEqualTo(
        String.format("%s: %s", baseMessage, throwableMessage));
  }

  @Test
  public void updateOnErrorWithNullMessage() {
    String oldStatusReason = "oldStatusReason";
    deployment.setStatusReason(oldStatusReason);
    deploymentStatusHelper.updateOnError(deployment.getId(), (String) null);
    Assertions.assertThat(deployment.getStatusReason()).isEqualTo(oldStatusReason);
  }

  @Parameters({ "CREATE_COMPLETE, STARTED, 2",
      "CREATE_FAILED, ERROR, 2",
      "CREATE_IN_PROGRESS, CREATING, 2",
      "DELETE_FAILED, ERROR, 2",
      "DELETE_IN_PROGRESS, DELETING, 2",
      "UNKNOWN, ERROR, 2" })
  @Test
  public void updateResources(Status deploymentStatus, NodeStates expectedResourceStatus,
      int expectedSize) {
    deployment.setStatus(deploymentStatus);
    deploymentStatusHelper.updateResources(deployment, deployment.getStatus());
    Assertions.assertThat(deployment.getResources()).allMatch(
        resource -> resource.getState() == expectedResourceStatus);
    Assertions.assertThat(deployment.getResources()).hasSize(2);
  }

  @Parameters({ "INITIAL, STARTED, 2",
      "CREATING, STARTED, 2",
      "CREATED, STARTED, 2",
      "CONFIGURING, STARTED, 2",
      "CONFIGURED, STARTED, 2",
      "STARTING, STARTED, 2",
      "STARTED, STARTED, 2",
      "STOPPING, STARTED, 2",
      "DELETING, DELETING, 0",
      "ERROR, STARTED, 2"
  })
  @Test
  public void updateResourcesUpdateComplete(NodeStates resourceStatus,
      NodeStates expectedResourceStatus, int expectedSize) {
    deployment.getResources().forEach(resource -> resource.setState(resourceStatus));
    deployment.setStatus(Status.UPDATE_COMPLETE);
    deploymentStatusHelper.updateResources(deployment, deployment.getStatus());
    Assertions.assertThat(deployment.getResources()).allMatch(
        resource -> resource.getState() == expectedResourceStatus);
    // deleting node must have been removed from the Set
    Assertions.assertThat(deployment.getResources()).allMatch(
        resource -> resource.getState() != NodeStates.DELETING);
    Assertions.assertThat(deployment.getResources()).hasSize(expectedSize);
  }

  @Parameters({ "INITIAL, CONFIGURING, 2",
      "CREATING, CREATING, 2",
      "CREATED, CONFIGURING, 2",
      "CONFIGURING, CONFIGURING, 2",
      "CONFIGURED, CONFIGURING, 2",
      "STARTING, CONFIGURING, 2",
      "STARTED, STARTED, 2",
      "STOPPING, CONFIGURING, 2",
      "DELETING, DELETING, 2",
      "ERROR, CONFIGURING, 2"
  })
  @Test
  public void updateResourcesUpdateInProgress(NodeStates resourceStatus,
      NodeStates expectedResourceStatus, int expectedSize) {
    deployment.getResources().forEach(resource -> resource.setState(resourceStatus));
    deployment.setStatus(Status.UPDATE_IN_PROGRESS);
    deploymentStatusHelper.updateResources(deployment, deployment.getStatus());
    Assertions.assertThat(deployment.getResources()).allMatch(
        resource -> resource.getState() == expectedResourceStatus);
    Assertions.assertThat(deployment.getResources()).hasSize(expectedSize);
  }

  @Parameters({ "INITIAL, ERROR, 2",
      "CREATING, ERROR, 2",
      "CREATED, ERROR, 2",
      "CONFIGURING, ERROR, 2",
      "CONFIGURED, ERROR, 2",
      "STARTING, ERROR, 2",
      "STARTED, STARTED, 2",
      "STOPPING, ERROR, 2",
      "DELETING, ERROR, 2",
      "ERROR, ERROR, 2"
  })
  @Test
  public void updateResourcesUpdateFailed(NodeStates resourceStatus,
      NodeStates expectedResourceStatus, int expectedSizie) {
    deployment.getResources().forEach(resource -> resource.setState(resourceStatus));
    deployment.setStatus(Status.UPDATE_FAILED);
    deploymentStatusHelper.updateResources(deployment, deployment.getStatus());
    Assertions.assertThat(deployment.getResources()).allMatch(
        resource -> resource.getState() == expectedResourceStatus);
    Assertions.assertThat(deployment.getResources()).hasSize(expectedSizie);
  }

  @Test
  public void updateResourcesDeleteComplete() {
    deployment.setStatus(Status.DELETE_COMPLETE);
    deploymentStatusHelper.updateResources(deployment, deployment.getStatus());
    Assertions.assertThat(deployment.getResources()).isEmpty();
  }
}
