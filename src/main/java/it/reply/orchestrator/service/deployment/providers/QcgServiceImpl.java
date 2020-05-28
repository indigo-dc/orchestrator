/*
 * Copyright © 2019-2020 I.N.F.N.
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

import alien4cloud.tosca.model.ArchiveRoot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.mysql.jdbc.StringUtils;

import it.infn.ba.deep.qcg.client.Qcg;
import it.infn.ba.deep.qcg.client.model.Job;
import it.infn.ba.deep.qcg.client.model.JobDescription;
import it.infn.ba.deep.qcg.client.model.JobDescriptionExecution;
import it.infn.ba.deep.qcg.client.model.JobDescriptionResources;
import it.infn.ba.deep.qcg.client.model.JobDescriptionResourcesComponent;
import it.infn.ba.deep.qcg.client.model.JobWorkingDirectoryPolicy;
import it.infn.ba.deep.qcg.client.model.RemoveConditionCreateMode;
import it.infn.ba.deep.qcg.client.model.RemoveConditionWhen;
import it.infn.ba.deep.qcg.client.utils.ModelUtils;
import it.infn.ba.deep.qcg.client.utils.QcgException;
import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.QcgJobsOrderedIterator;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.BusinessWorkflowException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.function.ThrowingConsumer;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService;
import it.reply.orchestrator.service.IndigoInputsPreProcessorService.RuntimeProperties;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.factory.QcgClientFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaConstants.Nodes;
import it.reply.orchestrator.utils.ToscaUtils;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.types.IntegerType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.QCG)
@Slf4j
public class QcgServiceImpl extends AbstractDeploymentProviderService {

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private QcgClientFactory qcgClientFactory;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  protected <R> R executeWithClientForResult(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, ThrowingFunction<Qcg, R, QcgException> function)
          throws QcgException {
    return oauth2TokenService.executeWithClientForResult(requestedWithToken,
        token -> function.apply(qcgClientFactory.build(cloudProviderEndpoint, token)),
        ex -> ex instanceof QcgException && ((QcgException) ex).getStatus() == 401);
  }

  protected void executeWithClient(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken,
      ThrowingConsumer<Qcg, QcgException> consumer) throws QcgException {
    executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
        client -> consumer.asFunction().apply(client));
  }

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);
    // Update status of the deployment - if not already done
    // (remember the Iterative mode)
    if (deployment.getTask() != Task.DEPLOYER) {
      deployment.setTask(Task.DEPLOYER);
    }
    if (deployment.getEndpoint() == null) {
      deployment.setEndpoint("<NO_ENDPOINT>");
    }

    QcgJobsOrderedIterator topologyIterator = deploymentMessage.getQcgJobsIterator();
    // Create nodes iterator if not done yet
    if (topologyIterator == null) {
      topologyIterator = getJobsTopologicalOrder(deploymentMessage, deployment);
      // Create topological order
      deploymentMessage.setQcgJobsIterator(topologyIterator);
    }

    if (topologyIterator.hasNext()) {
      DeepJob currentJob = topologyIterator.next();
      LOG.debug("Creating job {} on Qcg ({}/{})", currentJob.getQcgJob().getId(),
          topologyIterator.currentIndex() + 1,
          topologyIterator.getSize());
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      Job created = createJobOnQcg(cloudProviderEndpoint, requestedWithToken, currentJob);
      // update object with deployment data
      try {
        ModelUtils.updateJob(created, currentJob.getQcgJob());
      } catch (QcgException exception) { // Qcg job update error
        throw new DeploymentException("Failed to create Qcg job", exception);
      }
      deployment.setEndpoint(currentJob.getQcgJob().getId());
      updateResource(deployment, currentJob.getToscaNodeName(),
          currentJob.getQcgJob(), NodeStates.CREATED);
    }
    boolean noMoreJob = !topologyIterator.hasNext();

    if (noMoreJob) {
      // Start over with the polling check
      topologyIterator.reset();
    }

    // No error occurred
    return noMoreJob;

  }

  /**
   * Creates a Job on Qcg.
   *
   * @param cloudProviderEndpoint the {@link CloudProviderEndpoint} of the Qcg
   *                              instance
   * @param requestedWithToken    the token ID of the request
   * @param job                   the DeepJob to be created
   *
   * @return The Job object
   */
  protected Job createJobOnQcg(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, DeepJob job) {
    // Create jobs based on the topological order
    try {
      LOG.debug("Creating scheduled Qcg job\n{}",
          ModelUtils.toString(job.getQcgJob().getDescription()));

      Job created = executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
          client -> client.createJob(job.getQcgJob().getDescription()));
      return created;
    } catch (QcgException exception) { // Qcg job launch error
      throw new DeploymentException("Failed to launch job on Qcg", exception);
    }
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);
    deploymentMessage.setSkipPollInterval(true);

    QcgJobsOrderedIterator topologyIterator = deploymentMessage.getQcgJobsIterator();
    if (!topologyIterator.hasCurrent() && topologyIterator.hasNext()) {
      topologyIterator.next(); // first job
    }

    if (topologyIterator.hasCurrent()) {
      DeepJob currentJob = topologyIterator.current();
      LOG.debug("Polling job {} on Qcg ({}/{})", currentJob.getQcgJob().getId(),
          topologyIterator.currentIndex() + 1, topologyIterator.getSize());
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      Job updatedJob = findJobOnQcg(cloudProviderEndpoint, requestedWithToken,
          currentJob.getQcgJob().getId())
          .orElseThrow(() -> new DeploymentException("Job " + currentJob.getQcgJob().getId()
              + " not found on " + cloudProviderEndpoint.getCpComputeServiceId()));
      boolean jobIsCompleted = checkJobState(updatedJob);
      if (!jobIsCompleted) {
        // Job still in progress
        // Wait before retrying to poll on the same node
        deploymentMessage.setSkipPollInterval(false);
        updateResource(deployment, currentJob.getToscaNodeName(),
            updatedJob, NodeStates.CONFIGURING);
        return false;
      } else {
        updateResource(deployment, currentJob.getToscaNodeName(),
            updatedJob, NodeStates.STARTED);
      }
    }
    boolean noMoreJob = !topologyIterator.hasNext();
    if (noMoreJob) {
      // No more jobs
      LOG.debug("All jobs are ready");
    } else {
      topologyIterator.next();
      // Poll the following node next time - Disable poll interval
      LOG.debug("Polling of next job on Qcg scheduled");
    }
    return noMoreJob;
  }

  private void writeJobToResource(Resource resource, Job job) {
    Map<String,String> resourceMetadata = resource.getMetadata();
    if (resourceMetadata == null) {
      resourceMetadata = new HashMap<>();
      resource.setMetadata(resourceMetadata);
    }
    Job oldJob = null;
    if (resourceMetadata.containsKey("Job")) {
      try {
        oldJob = new ObjectMapper().readValue(resourceMetadata.get("Job"),
            Job.class);
      } catch (IOException e) {
        throw new DeploymentException("Error deserializing Job", e);
      }
    }
    if (oldJob == null || !job.equals(oldJob)) {
      try {
        resourceMetadata.put("Job",
            new ObjectMapper().writeValueAsString(job));
      } catch (IOException e) {
        throw new DeploymentException("Error serializing Job", e);
      }
    }
  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {
    doUndeploy(deploymentMessage);
  }

  /**
   * Gets the Job status.
   *
   * @param cloudProviderEndpoint the {@link CloudProviderEndpoint} of the Qcg
   *                              instance
   * @param requestedWithToken    the token ID of the request
   * @param jobId                 the ID of the Qcg job
   *
   * @return the optional {@link Job}.
   */
  protected boolean checkJobState(Job job) {

    LOG.debug("Qcg job {} current status:\n{}", job.getId(), job);
    JobState jobState = getLastState(job);
    LOG.debug("Status of Qcg job {} is: {}", job.getId(), jobState);

    switch (jobState) {
      case SUBMITTED:
      case EXECUTING:
      case PENDING:
      case COMPLETING:
        LOG.debug("Qcg job {} not ready yet", job.getId());
        return false;
      case FINISHED:
        LOG.debug("Qcg job {} is ready", job.getId());
        return true;
      case FAILED:
        String ermsg = "Qcg job " + job.getId() + " failed to execute";
        if (job.getExit_code() != null) {
          ermsg += " with exit code:" + job.getExit_code().toString();
        }
        if (!StringUtils.isNullOrEmpty(job.getErrors())) {
          ermsg += " - message: " + job.getErrors();
        }
        throw new DeploymentException(ermsg);
      default:
        throw new DeploymentException("Unknown Qcg job status: " + jobState);
    }
  }

  private void updateResource(Deployment deployment, String toscaNodeName,
      Job job, NodeStates state) {

    resourceRepository.findByToscaNodeNameAndDeployment_id(toscaNodeName,
        deployment.getId())
        .forEach(resource -> {
          resource.setState(state);
          resource.setIaasId(job.getId());
          writeJobToResource(resource, job);
        });
  }

  /**
   * Gets the Job status.
   *
   * @param cloudProviderEndpoint the {@link CloudProviderEndpoint} of the Qcg
   *                              instance
   * @param requestedWithToken    the token ID of the request
   * @param jobId                 the ID of the Qcg job
   *
   * @return the optional {@link Job}.
   */
  protected Optional<Job> findJobOnQcg(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, String jobId) {
    try {
      Job job = executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
          client -> client.getJob(jobId));
      return Optional.ofNullable(job);
    } catch (QcgException ex) {
      throw new DeploymentException("Unable to retrieve job " + jobId + " status on Qcg", ex);
    }
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {
    throw new UnsupportedOperationException("Qcg job deployments do not support update.");
  }

  @Override
  public void cleanFailedUpdate(DeploymentMessage deploymentMessage) {
    throw new UnsupportedOperationException("Qcg job deployments do not support update.");
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    // Nothing to wait here... All the jobs are deleted immediately.
    return true;
  }

  @Override
  public void doProviderTimeout(DeploymentMessage deploymentMessage) {
    throw new BusinessWorkflowException(ErrorCode.CLOUD_PROVIDER_ERROR,
      "Error executing request to Qcg service",
      new DeploymentException("Qcg service timeout during deployment"));
  }

  @Data
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @RequiredArgsConstructor
  public static class DeepJob {

    @NonNull
    @NotNull
    private Job qcgJob;

    @NonNull
    @NotNull
    private String toscaNodeName;

  }

  /**
   * Creates the Job graph based on the given {@link Deployment} (the
   * TOSCA template is parsed).
   *
   * @param deploymentMessage the deployment message.
   * @param deployment the input deployment.
   *
   * @return the job graph.
   */
  protected QcgJobsOrderedIterator getJobsTopologicalOrder(DeploymentMessage deploymentMessage,
      Deployment deployment) {
    LOG.debug("Generating job graph");
    Map<String, OneData> odParameters = deploymentMessage.getOneDataParameters();

    ArchiveRoot ar = prepareTemplate(deployment, odParameters);

    Map<String, NodeTemplate> nodes = Optional.ofNullable(ar.getTopology())
        .map(Topology::getNodeTemplates)
        .orElseGet(HashMap::new);

    // don't check for cycles, already validated at web-service time
    DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
        toscaService.buildNodeGraph(nodes, false);

    TopologicalOrderIterator<NodeTemplate, RelationshipTemplate> orderIterator =
        new TopologicalOrderIterator<>(graph);

    List<NodeTemplate> orderedQcgJobs = CommonUtils.iteratorToStream(orderIterator)
        .filter(node -> toscaService.isOfToscaType(node, ToscaConstants.Nodes.Types.QCG))
        .collect(Collectors.toList());

    List<DeepJob> deepJobs = new ArrayList<>();

    for (NodeTemplate qcgNode : orderedQcgJobs) {
      Job job = buildJob(graph, qcgNode);
      DeepJob deepJob = new DeepJob(job, qcgNode.getName());
      deepJobs.add(deepJob);
    }

    return new QcgJobsOrderedIterator(deepJobs);
  }

  /**
   * Build a Job object.
   *
   * @param graph the input nodegraph.
   * @param taskNode the input tasknode.
   *
   * @return the Job.
   */
  protected Job buildJob(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode) {

    JobDescriptionExecution execution = new JobDescriptionExecution();

    // property: executable
    ToscaUtils.extractScalar(taskNode.getProperties(), "executable").map(String::trim)
        .ifPresent(execution::setExecutable);

    if ("".equals(execution.getExecutable())) { // it must be either null or not empty
      throw new ToscaException(
          String.format("<executable> property of node <%s> must not be an empty string",
              taskNode.getName()));
    }

    // property: directory
    ToscaUtils.extractScalar(taskNode.getProperties(), "directory")
        .ifPresent(execution::setDirectory);
    // property: arguments
    ToscaUtils.extractList(taskNode.getProperties(), "arguments", String.class::cast)
        .ifPresent(execution::setArgs);
    // property: environment
    ToscaUtils.extractMap(taskNode.getProperties(), "environment", String.class::cast)
        .ifPresent(execution::setEnvironment);

    // default remove policy
    JobWorkingDirectoryPolicy directorypolicy = new JobWorkingDirectoryPolicy();
    directorypolicy.setCreate(RemoveConditionCreateMode.OVERWRITE);
    directorypolicy.setRemove(RemoveConditionWhen.NEVER);
    execution.setDirectory_policy(directorypolicy);

    // property: stdin
    ToscaUtils.extractScalar(taskNode.getProperties(), "stdin").ifPresent(execution::setStdin);
    // property: stdout
    ToscaUtils.extractScalar(taskNode.getProperties(), "stdout").ifPresent(execution::setStdout);
    // property: std_outerr
    ToscaUtils.extractScalar(taskNode.getProperties(), "std_outerr")
        .ifPresent(execution::setStd_outerr);
    // property: stderr
    ToscaUtils.extractScalar(taskNode.getProperties(), "stderr").ifPresent(execution::setStderr);

    JobDescription description = new JobDescription();

    description.setExecution(execution);
    // property: attributes
    ToscaUtils.extractMap(taskNode.getProperties(), "attributes", String.class::cast)
        .ifPresent(description::setAttributes);
    // property: schema
    ToscaUtils.extractScalar(taskNode.getProperties(), "schema").ifPresent(description::setSchema);
    // property: note
    ToscaUtils.extractScalar(taskNode.getProperties(), "note").ifPresent(description::setNote);

    JobDescriptionResources resources = new JobDescriptionResources();

    // property: queue
    ToscaUtils.extractScalar(taskNode.getProperties(), "queue").ifPresent(resources::setQueue);
    // property: wall_clock
    ToscaUtils.extractScalar(taskNode.getProperties(), "wall_clock")
        .ifPresent(resources::setWall_clock);

    JobDescriptionResourcesComponent component = new JobDescriptionResourcesComponent();

    // property: total_cores
    ToscaUtils.extractScalar(taskNode.getProperties(), "total_cores",
        IntegerType.class).map(Ints::saturatedCast).ifPresent(component::setTotal_cores);
    // property: total_nodes
    ToscaUtils.extractScalar(taskNode.getProperties(), "total_nodes",
        IntegerType.class).map(Ints::saturatedCast).ifPresent(component::setTotal_nodes);
    // property: cores_per_node
    ToscaUtils.extractScalar(taskNode.getProperties(), "cores_per_node",
        IntegerType.class).map(Ints::saturatedCast).ifPresent(component::setCores_per_node);
    // property: memory_per_node
    ToscaUtils.extractScalar(taskNode.getProperties(), "memory_per_node",
        IntegerType.class).map(Ints::saturatedCast).ifPresent(component::setMemory_per_node);
    // property: memory_per_core
    ToscaUtils.extractScalar(taskNode.getProperties(), "memory_per_core",
        IntegerType.class).map(Ints::saturatedCast).ifPresent(component::setMemory_per_core);
    // property: gpus
    Optional<Integer> gpus = ToscaUtils.extractScalar(taskNode.getProperties(), "gpus",
        IntegerType.class).map(Ints::saturatedCast);
    if (gpus.isPresent()) {
      Integer ng = gpus.get();
      if (ng > 0) {
        List<String> nativee =
            component.get_native() == null ? new ArrayList<>() : component.get_native();
        nativee.add("--gres=gpu:" + ng.toString());
        component.set_native(nativee);
      }
    }
    // property: batch_system_options
    Optional<List<String>> batch_system_options = ToscaUtils.extractList(taskNode.getProperties(),
        "batch_system_options", String.class::cast);
    if (batch_system_options.isPresent()) {
      List<String> nativee =
          component.get_native() == null ? new ArrayList<>() : component.get_native();
      nativee.addAll(batch_system_options.get());
      component.set_native(nativee);
    }
    List<JobDescriptionResourcesComponent> components = new ArrayList<>();
    components.add(component);
    resources.setComponents(components);
    description.setResources(resources);

    Job job = new Job();
    job.setDescription(description);

    return job;
  }

  /**
   * Deletes all the deployment jobs from Qcg. <br>
   * Also logs possible errors and updates the deployment status.
   *
   * @param deploymentMessage the deployment message.
   * @return <tt>true</tt> if all jobs have been deleted, <tt>false</tt>
   *         otherwise.
   */
  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    // Delete all Jobs on Qcg
    Deployment deployment = getDeployment(deploymentMessage);
    Iterator<Resource> topologyIterator = deployment.getResources().stream()
        .filter(resource -> toscaService.isOfToscaType(resource, Nodes.Types.QCG))
        // FIXME it should also not be DELETED
        .filter(resource -> resource.getState() != NodeStates.DELETING)
        .collect(Collectors.toList()).iterator();

    if (topologyIterator.hasNext()) {
      Resource qcgResource = topologyIterator.next();
      LOG.debug("Deleting job {} on Qcg", qcgResource.getIaasId());

      // FIXME it should be DELETED, not DELETING
      qcgResource.setState(NodeStates.DELETING);
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      String jobId = deployment.getEndpoint();
      deleteJobsOnQcg(cloudProviderEndpoint, requestedWithToken, jobId);
    }
    boolean noMoreJob = !topologyIterator.hasNext();
    if (noMoreJob) {
      // No more jobs
      LOG.debug("All jobs have been deleted");
    }
    // No error occurred
    return noMoreJob;
  }

  /**
   * Deletes a job from Qcg.
   *
   * @param cloudProviderEndpoint the {@link CloudProviderEndpoint} of the Qcg
   *                              instance
   * @param requestedWithToken    the token ID of the request
   * @param jobId                 the Id of the Qcg job
   */
  protected void deleteJobsOnQcg(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, String jobId) {

    try {
      executeWithClient(cloudProviderEndpoint, requestedWithToken,
          client -> client.deleteJob(jobId));
    } catch (QcgException ex) {
      // Qcg API hack to avoid error 400 if the job to delete does not exist or cannot
      // be deleted
      if (ex.getStatus() != 400 && ex.getStatus() != 404) {
        throw new DeploymentException("Failed to delete job " + jobId + " on Qcg", ex);
      }
    }
  }

  /**
   * Resolves the Tosca functions.
   *
   * @param deployment   the deployment
   * @param odParameters the OneData settings
   *
   * @return the populated {@link ArchiveRoot}
   */
  public ArchiveRoot prepareTemplate(Deployment deployment, Map<String, OneData> odParameters) {

    RuntimeProperties runtimeProperties = new RuntimeProperties();
    odParameters.forEach((nodeName, odParameter) -> {
      runtimeProperties.put(odParameter.getOnezone(), nodeName, "onezone");
      runtimeProperties.put(odParameter.getToken(), nodeName, "token");
      runtimeProperties.put(odParameter.getSelectedOneprovider().getEndpoint(),
          nodeName, "selected_provider");
      if (odParameter.isServiceSpace()) {
        runtimeProperties.put(odParameter.getSpace(), nodeName, "space");
        runtimeProperties.put(odParameter.getPath(), nodeName, "path");
      }
    });

    ArchiveRoot ar = toscaService.parse(deployment.getTemplate());

    indigoInputsPreProcessorService.processFunctions(ar, deployment.getParameters(),
        runtimeProperties);
    return ar;
  }

  public enum JobState {
    SUBMITTED, PENDING, EXECUTING, FAILED, COMPLETING, FINISHED;
  }

  /**
   * Computes the Qcg job's state based on current success and error count.
   *
   * @param job the {@link Job}.
   *
   * @return the {@link JobState}.
   */
  @VisibleForTesting
  protected static JobState getLastState(Job job) {
    if (!StringUtils.isNullOrEmpty(job.getState())) {
      try {
        return JobState.valueOf(job.getState());
      } catch (IllegalArgumentException e) {
        throw new DeploymentException("Unknown Qcg job status: " + job.getState());
      }
    } else {
      throw new DeploymentException("Empty Qcg job status");
    }
  }

  @Override
  public Optional<String> getAdditionalErrorInfoInternal(DeploymentMessage deploymentMessage) {
    return Optional.empty();
  }

  @Override
  public Optional<String> getDeploymentLogInternal(DeploymentMessage deploymentMessage) {
    return Optional.empty();
  }

  @Override
  public Optional<String> getDeploymentExtendedInfoInternal(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);

    Map<Boolean, Set<Resource>> resources =
        resourceRepository
            .findByDeployment_id(deployment.getId())
            .stream()
            .collect(Collectors.partitioningBy(resource ->
              (resource.getIaasId() != null && resource.getMetadata() != null),
                Collectors.toSet()));
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    boolean first = true;
    for (Resource resource : resources.get(true)) {
      Map<String,String> resourceMetadata = resource.getMetadata();
      if (resourceMetadata != null && resourceMetadata.containsKey("Job")) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append(resourceMetadata.get("Job"));
      }
    }
    sb.append("]");
    return Optional.of(sb.toString());
  }
}
