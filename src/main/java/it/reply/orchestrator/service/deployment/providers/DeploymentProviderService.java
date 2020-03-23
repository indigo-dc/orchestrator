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

package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.Optional;

public interface DeploymentProviderService {

  /**
   * Executes the deployment of the given <code>deployment</code>, which typically means
   * instantiating the required resources on the underlying system (i.e. IaaS, other service
   * clusters, etc). <br/>
   * <br/>
   * The nodes can be created iteratively (i.e. a given subset for each invocation); the deployment
   * is considered completed when the {@link DeploymentMessage#isCreateComplete()} flag is set to
   * <tt>true</tt>. <br/>
   * <br/>
   * It should handle every error internally (i.e. not throwing exceptions) and just return a
   * boolean indicating the result or the failure. <br/>
   * It should also handle the deployment status update internally.
   *
   * @param deploymentMessage
   *          the deployment message.
   * @return <tt>true</tt> if no error happened (<b>this does not mean the deployment is
   *         complete</b>, see above), <tt>false</tt> otherwise.
   */
  public boolean doDeploy(DeploymentMessage deploymentMessage);

  /**
   * Checks whether the given <tt>deployment</tt> is ready. <br/>
   * <br/>
   * The nodes can be checked iteratively (i.e. a given subset for each invocation); the deployment
   * is considered ready when the result of the invocation is <tt>true</tt>. <br/>
   * <br/>
   * The method should handle node status update internally, but can throw a DeploymentException to
   * notify an unexpected error during the status check. <br/>
   *
   * @param deploymentMessage
   *          the deployment message.
   * @return <tt>true</tt> if the deployment is ready, <tt>false</tt> otherwise.
   * @throws DeploymentException
   *           if the deployment fails.
   */
  public boolean isDeployed(DeploymentMessage deploymentMessage);

  public void finalizeDeploy(DeploymentMessage deploymentMessage);

  public void cleanFailedDeploy(DeploymentMessage deploymentMessage);

  public boolean doUpdate(DeploymentMessage deploymentMessage, String template);

  public void cleanFailedUpdate(DeploymentMessage deploymentMessage);

  /**
   * Executes the undeployment of the given <code>deployment</code>, which typically means deleting
   * the required resources from the underlying system (i.e. IaaS, other service clusters, etc).
   * <br/>
   * <br/>
   * The nodes can be deleted iteratively (i.e. a given subset for each invocation); the
   * undeployment is considered completed when the {@link DeploymentMessage#isDeleteComplete()} flag
   * is set to <tt>true</tt>. <br/>
   * <br/>
   * It should handle every error internally (i.e. not throwing exceptions) and just return a
   * boolean indicating the result or the failure. <br/>
   * It should also handle the deployment status update internally.
   *
   * @param deploymentMessage
   *          the deployment message.
   * @return <tt>true</tt> if no error happened (<b>this does not mean the undeployment is
   *         complete</b>, see above), <tt>false</tt> otherwise.
   */
  public boolean doUndeploy(DeploymentMessage deploymentMessage);

  /**
   * Checks whether the given <tt>deployment</tt> is deleted. <br/>
   * <br/>
   * The nodes can be checked iteratively (i.e. a given subset for each invocation); the deployment
   * is considered deleted when the result of the invocation is <tt>true</tt>. <br/>
   * <br/>
   * The method should handle node status update internally, but can throw a DeploymentException to
   * notify an unexpected error during the status check. <br/>
   *
   * @param deploymentMessage
   *          the deployment message.
   * @return <tt>true</tt> if the deployment is deleted, <tt>false</tt> otherwise.
   * @throws DeploymentException
   *           if the deployment fails.
   */
  public boolean isUndeployed(DeploymentMessage deploymentMessage);

  public void finalizeUndeploy(DeploymentMessage deploymentMessage);

  public void doProviderTimeout(DeploymentMessage deploymentMessage);

  public Optional<String> getAdditionalErrorInfo(DeploymentMessage deploymentMessage);

}
