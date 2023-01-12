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

package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.tosca.model.ArchiveRoot;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MoreCollectors;
import com.google.common.primitives.Ints;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.model.v1.Container;
import it.infn.ba.indigo.chronos.client.model.v1.EnvironmentVariable;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.infn.ba.indigo.chronos.client.model.v1.Parameters;
import it.infn.ba.indigo.chronos.client.model.v1.Volume;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.ChronosService;
import it.reply.orchestrator.dto.cmdb.ChronosService.ChronosServiceProperties;
import it.reply.orchestrator.dto.deployment.ActionMessage;
import it.reply.orchestrator.dto.deployment.ChronosJobsOrderedIterator;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.mesos.MesosContainer;
import it.reply.orchestrator.dto.mesos.chronos.ChronosJob;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.workflow.CloudServiceWf;
import it.reply.orchestrator.dto.workflow.WorkflowListIterator;
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
import it.reply.orchestrator.service.RucioService;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.factory.ChronosClientFactory;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaConstants.Nodes;
import it.reply.orchestrator.utils.ToscaUtils;
import it.reply.orchestrator.utils.WorkflowConstants.ErrorCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.types.IntegerType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DeploymentProviderQualifier(DeploymentProvider.CHRONOS)
@Slf4j
public class ChronosServiceImpl extends AbstractMesosDeploymentService<ChronosJob, Job> {

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private ChronosClientFactory chronosClientFactory;

  @Autowired
  private OAuth2TokenService oauth2TokenService;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Autowired
  private RucioService rucioService;

  protected <R> R executeWithClientForResult(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken,
      ThrowingFunction<Chronos, R, ChronosException> function) throws ChronosException {
    return oauth2TokenService.executeWithClientForResult(requestedWithToken,
        token -> function.apply(chronosClientFactory.build(cloudProviderEndpoint, token)),
        ex -> ex instanceof ChronosException && ((ChronosException) ex).getStatus() == 401);
  }

  protected void executeWithClient(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken,
      ThrowingConsumer<Chronos, ChronosException> consumer) throws ChronosException {
    executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
        client -> consumer.asFunction().apply(client));
  }

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);
    // Update status of the deployment - if not already done (remember the Iterative mode)
    if (deployment.getTask() != Task.DEPLOYER) {
      deployment.setTask(Task.DEPLOYER);
    }
    if (deployment.getEndpoint() == null) {
      deployment.setEndpoint("<NO_ENDPOINT>");
    }

    // TODO Replace attribute, inputs, temporary-hard-coded properties in the TOSCA template

    ChronosJobsOrderedIterator topologyIterator = deploymentMessage.getChronosJobsIterator();
    // Create nodes iterator if not done yet
    if (topologyIterator == null) {
      topologyIterator = getJobsTopologicalOrder(deploymentMessage, deployment);
      // Create topological order
      deploymentMessage.setChronosJobsIterator(topologyIterator);
    }

    if (topologyIterator.hasNext()) {
      IndigoJob currentJob = topologyIterator.next();
      LOG.debug("Creating job {} on Chronos ({}/{})",
          currentJob.getChronosJob().getName(),
          topologyIterator.currentIndex() + 1,
          topologyIterator.getSize());
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      createJobOnChronos(cloudProviderEndpoint, requestedWithToken, currentJob);
      updateResource(deployment, currentJob, NodeStates.CREATED);
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
   *  Creates a Job on Chronos.
   *
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Chronos instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param job
   *          the IndigoJob to be created
   */
  protected void createJobOnChronos(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, IndigoJob job) {
    // Create jobs based on the topological order
    try {
      if (CollectionUtils.isEmpty(job.getChronosJob().getParents())) {
        // No parents -> Scheduled job (not dependent)
        LOG.debug("Creating scheduled Chronos job\n{}", job.getChronosJob());
        executeWithClient(cloudProviderEndpoint, requestedWithToken,
            client -> client.createJob(job.getChronosJob()));
      } else {
        // Dependent job
        LOG.debug("Creating dependent Chronos job\n{}", job.getChronosJob());
        executeWithClient(cloudProviderEndpoint, requestedWithToken,
            client -> client.createDependentJob(job.getChronosJob()));
      }
    } catch (ChronosException exception) { // Chronos job launch error
      throw new DeploymentException(
          "Failed to launch job <" + job.getChronosJob().getName() + "> on Chronos", exception);
    }
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) {

    Deployment deployment = getDeployment(deploymentMessage);
    deploymentMessage.setSkipPollInterval(true);

    ChronosJobsOrderedIterator topologyIterator = deploymentMessage.getChronosJobsIterator();
    if (!topologyIterator.hasCurrent() && topologyIterator.hasNext()) {
      topologyIterator.next(); // first job
    }

    if (topologyIterator.hasCurrent()) {
      IndigoJob currentJob = topologyIterator.current();
      LOG.debug("Polling job {} on Chronos ({}/{})",
          currentJob.getChronosJob().getName(),
          topologyIterator.currentIndex() + 1,
          topologyIterator.getSize());
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      boolean jobIsCompleted = checkJobsOnChronos(cloudProviderEndpoint, requestedWithToken,
          currentJob.getChronosJob().getName());
      if (!jobIsCompleted) {
        // Job still in progress
        // Wait before retrying to poll on the same node
        deploymentMessage.setSkipPollInterval(false);
        updateResource(deployment, currentJob, NodeStates.CONFIGURING);
        return false;
      } else {
        updateResource(deployment, currentJob, NodeStates.STARTED);
      }
    }
    boolean noMoreJob = !topologyIterator.hasNext();
    if (noMoreJob) {
      // No more jobs
      LOG.debug("All jobs are ready");
    } else {
      topologyIterator.next();
      // Poll the following node next time - Disable poll interval
      LOG.debug("Polling of next job on Chronos scheduled");
    }
    return noMoreJob;

  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {
    doUndeploy(deploymentMessage);
  }

  @Override
  public void doProviderTimeout(DeploymentMessage deploymentMessage) {
    throw new BusinessWorkflowException(ErrorCode.CLOUD_PROVIDER_ERROR,
        "Error executing request to Chronos service",
        new DeploymentException("Chronos service timeout during deployment"));
  }

  /**
   * Gets the Job status.
   *
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Chronos instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param jobName
   *     the name of the Chronos job
   * @return the optional {@link Job}.
   */
  protected boolean checkJobsOnChronos(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, String jobName) {

    Job updatedJob = findJobOnChronos(cloudProviderEndpoint, requestedWithToken, jobName)
        .orElseThrow(() -> new DeploymentException("Job " + jobName + " doesn't exist on Chronos"));

    LOG.debug("Chronos job {} current status:\n{}", jobName, updatedJob);
    JobState jobState = getLastState(updatedJob);
    LOG.debug("Status of Chronos job {} is: {}", jobName, jobState);

    switch (jobState) {
      case FRESH:
        LOG.debug("Chronos job {} not ready yet", jobName);
        return false;
      case SUCCESS:
        LOG.debug("Chronos job {} is ready", jobName);
        return true;
      case FAILURE:
        throw new DeploymentException("Chronos job " + jobName + " failed to execute");
      default:
        throw new DeploymentException("Unknown Chronos job status: " + jobState);
    }
  }

  private void updateResource(Deployment deployment, IndigoJob job, NodeStates state) {
    resourceRepository
        .findByToscaNodeNameAndDeployment_id(job.getToscaNodeName(), deployment.getId())
        .forEach(resource -> resource.setState(state));
  }

  /**
   * Gets the Job status.
   *
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Chronos instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param jobName
   *     the name of the Chronos job
   * @return the optional {@link Job}.
   */
  protected Optional<Job> findJobOnChronos(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, String jobName) {
    try {
      return Optional
          .ofNullable(executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
              client -> client.getJob(jobName)))
        .map(Collection::stream)
        .flatMap(stream -> stream.collect(MoreCollectors.toOptional()));
    } catch (RuntimeException | ChronosException ex) {
      throw new DeploymentException("Unable to retrieve job " + jobName + " status on Chronos", ex);
    }
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {
    throw new UnsupportedOperationException("Chronos job deployments do not support update.");
  }

  @Override
  public void cleanFailedUpdate(DeploymentMessage deploymentMessage) {
    throw new UnsupportedOperationException("Chronos job deployments do not support update.");
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    // Nothing to wait here... All the jobs are delete immediately.
    return true;
  }

  @Override
  public void finalizeDeploy(DeploymentMessage deploymentMessage) {
    Deployment deployment = getDeployment(deploymentMessage);
    ArchiveRoot ar = prepareTemplate(deployment, deploymentMessage.getOneDataParameters());
    Map<String, OutputDefinition> outputs = Optional
        .ofNullable(ar.getTopology())
        .map(Topology::getOutputs)
        .orElseGet(HashMap::new);
    if (!outputs.isEmpty()) {
      RuntimeProperties runtimeProperties = new RuntimeProperties();
      deployment.setOutputs(indigoInputsPreProcessorService.processOutputs(ar,
          deployment.getParameters(), runtimeProperties));
    }
    super.finalizeDeploy(deploymentMessage);
  }

  /**
   * Deletes all the deployment jobs from Chronos. <br>
   * Also logs possible errors and updates the deployment status.
   *
   * @param deploymentMessage
   *          the deployment message.
   * @return <tt>true</tt> if all jobs have been deleted, <tt>false</tt> otherwise.
   */
  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    // Delete all Jobs on Chronos
    Deployment deployment = getDeployment(deploymentMessage);
    Iterator<Resource> topologyIterator = deployment
        .getResources()
        .stream()
        .filter(resource -> toscaService.isOfToscaType(resource, Nodes.Types.CHRONOS))
        // FIXME it should also not be DELETED
        .filter(resource -> resource.getState() != NodeStates.DELETING)
        .collect(Collectors.toList()).iterator();

    if (topologyIterator.hasNext()) {
      Resource chronosResource = topologyIterator.next();
      LOG.debug("Deleting job {} on Chronos", chronosResource.getIaasId());

      // FIXME it should be DELETED, not DELETING
      chronosResource.setState(NodeStates.DELETING);
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      String jobName = chronosResource.getId(); // iaasId could have not been set yet
      deleteJobsOnChronos(cloudProviderEndpoint, requestedWithToken, jobName);
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
   * Deletes a job from Chronos.
   *
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Chronos instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param jobName
   *     the name of the Chronos job
   */
  protected void deleteJobsOnChronos(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, String jobName) {
    try {
      executeWithClient(cloudProviderEndpoint, requestedWithToken,
          client -> client.deleteJob(jobName));
    } catch (ChronosException ex) {
      // Chronos API hack (to avoid error 400 if the job to delete does not exist)
      if (ex.getStatus() != 400 && ex.getStatus() != 404) {
        throw new DeploymentException("Failed to delete job " + jobName + " on Chronos", ex);
      }
    }
  }

  @Data
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @RequiredArgsConstructor
  public static class IndigoJob {

    @NonNull
    @NotNull
    private Job chronosJob;

    @NonNull
    @NotNull
    private String toscaNodeName;

  }

  /**
   * Creates the {@link IndigoJob} graph based on the given {@link Deployment} (the TOSCA template
   * is parsed).
   *
   * @param deploymentMessage
   *          the deployment message
   * @param deployment
   *          the input deployment.
   * @return the job graph.
   */
  protected ChronosJobsOrderedIterator getJobsTopologicalOrder(DeploymentMessage deploymentMessage,
      Deployment deployment) {
    LOG.debug("Generating job graph");
    Map<String, OneData> odParameters = deploymentMessage.getOneDataParameters();

    ArchiveRoot ar = prepareTemplate(deployment, odParameters);

    Map<String, NodeTemplate> nodes = Optional
        .ofNullable(ar.getTopology())
        .map(Topology::getNodeTemplates)
        .orElseGet(HashMap::new);

    // don't check for cycles, already validated at web-service time
    DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
        toscaService.buildNodeGraph(nodes, false);

    TopologicalOrderIterator<NodeTemplate, RelationshipTemplate> orderIterator =
        new TopologicalOrderIterator<>(graph);

    List<NodeTemplate> orderedChronosJobs = CommonUtils
        .iteratorToStream(orderIterator)
        .filter(node -> toscaService.isOfToscaType(node, ToscaConstants.Nodes.Types.CHRONOS))
        .collect(Collectors.toList());

    Map<String, Resource> resources = deployment
        .getResources()
        .stream()
        .filter(
            resource -> toscaService.isOfToscaType(resource, ToscaConstants.Nodes.Types.CHRONOS))
        .collect(Collectors.toMap(Resource::getToscaNodeName, res -> res));

    ChronosServiceProperties chronosProperties = deploymentMessage
        .getCloudServicesOrderedIterator()
        .currentService(ChronosService.class)
        .getProperties();
    Optional<String> localReplicaPfn = Optional
        .of(deployment)
        .map(Deployment::getDeploymentScheduleEvent)
        .map(DeploymentScheduleEvent::getTempReplicationRule)
        .flatMap(replicationRule -> Optional
            .ofNullable(deploymentMessage.getCloudServicesOrderedIterator())
            .map(WorkflowListIterator::current)
            .map(CloudServiceWf::getRucioRse)
            .flatMap(selectedRse -> {
              String scope = replicationRule.getScope();
              String name = replicationRule.getName();
              return rucioService
                  .getPfnFromReplica(deploymentMessage.getRequestedWithToken(), scope, name,
                      selectedRse);
            }));
    LinkedHashMap<String, ChronosJob> jobs = new LinkedHashMap<>();
    List<IndigoJob> indigoJobs = new ArrayList<>();
    for (NodeTemplate chronosNode : orderedChronosJobs) {
      Resource jobResource = resources.get(chronosNode.getName());
      String id = Optional
          .ofNullable(jobResource.getIaasId())
          .orElseGet(() -> {
            jobResource.setIaasId(jobResource.getId());
            return jobResource.getIaasId();
          });
      ChronosJob mesosTask = buildTask(graph, chronosNode, id);
      localReplicaPfn.ifPresent(pfn -> {
        mesosTask.getEnv().put("LOCAL_REPLICA_PFN", pfn);
        mesosTask.getEnv().put("IAM_TOKEN",
            oauth2TokenService.getAccessToken(deploymentMessage.getRequestedWithToken()));
      });
      jobs.put(chronosNode.getName(), mesosTask);
      List<NodeTemplate> parentNodes = getParentNodes("parent_job", graph, chronosNode);
      mesosTask.setParents(parentNodes
          .stream()
          .map(parentNode -> jobs.get(parentNode.getName()))
          .collect(Collectors.toList()));
      if (mesosTask.getSchedule() != null && !mesosTask.getParents().isEmpty()) {
        throw new ToscaException("Error creating Job <" + chronosNode.getName()
            + ">: 'schedule' parameter and job depencency are both specified");
      }
      Job chronosJob = generateExternalTaskRepresentation(mesosTask);
      CommonUtils
          .nullableCollectionToStream(chronosJob.getContainer().getVolumes())
          .forEach(volume -> {
            // set as /basePath/groupId
            volume.setHostPath(chronosProperties.generateLocalVolumesHostPath(id));
          });
      IndigoJob indigoJob = new IndigoJob(chronosJob, chronosNode.getName());
      indigoJobs.add(indigoJob);
    }

    return new ChronosJobsOrderedIterator(indigoJobs);
  }

  /**
   * Resolves the Tosca functions.
   *
   * @param deployment
   *     the deployment
   * @param odParameters
   *     the OneData settings
   * @return the populated {@link ArchiveRoot}
   */
  public ArchiveRoot prepareTemplate(Deployment deployment, Map<String, OneData> odParameters) {

    RuntimeProperties runtimeProperties = new RuntimeProperties();
    odParameters.forEach((nodeName, odParameter) -> {
      runtimeProperties.put(odParameter.getOnezone(), nodeName, "onezone");
      runtimeProperties.put(odParameter.getToken(), nodeName, "token");
      runtimeProperties
        .put(odParameter.getSelectedOneprovider().getEndpoint(), nodeName, "selected_provider");
      if (odParameter.isServiceSpace()) {
        runtimeProperties.put(odParameter.getSpace(), nodeName, "space");
        runtimeProperties.put(odParameter.getPath(), nodeName, "path");
      }
    });

    ArchiveRoot ar = toscaService.parse(deployment.getTemplate());

    indigoInputsPreProcessorService
      .processFunctions(ar, deployment.getParameters(), runtimeProperties);
    return ar;
  }

  public enum JobState {
    FRESH,
    FAILURE,
    SUCCESS;
  }

  /**
   * Computes the Chronos job's state based on current success and error count.
   *
   * @param job
   *          the {@link Job}.
   * @return the {@link JobState}.
   */
  @VisibleForTesting
  protected static JobState getLastState(Job job) {
    // State = Fresh (success + error = 0), Success (success > 0), Failure (error > 0)
    // NOTE that Chronos increments the error only after all the retries has failed (x retries -> +1
    // error)
    if (job.getSuccessCount() > 0) {
      return JobState.SUCCESS;
    } else {
      if (job.getErrorCount() > 0) {
        return JobState.FAILURE;
      } else {
        return JobState.FRESH;
      }
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
    return Optional.empty();
  }

  @Override
  protected ChronosJob createInternalTaskRepresentation() {
    return new ChronosJob();
  }

  @Override
  public ChronosJob buildTask(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode, String taskId) {
    ChronosJob job = super.buildTask(graph, taskNode, taskId);

    if (job.getCmd() == null) { // command is required in chronos
      throw new ToscaException(
        "<command> property of node <" + taskNode.getName() + "> must be provided");
    }
    ToscaUtils
        .extractScalar(taskNode.getProperties(), "retries", IntegerType.class)
        .map(Ints::saturatedCast)
        .ifPresent(job::setRetries);

    ToscaUtils
        .extractScalar(taskNode.getProperties(), "schedule")
        .ifPresent(job::setSchedule);

    ToscaUtils
        .extractScalar(taskNode.getProperties(), "description")
        .ifPresent(job::setDescription);

    ToscaUtils
        .extractScalar(taskNode.getProperties(), "epsilon")
        .ifPresent(job::setEpsilon);

    ToscaUtils
        .extractScalar(taskNode.getProperties(), "user")
        .ifPresent(job::setRunAsUser);

    return job;
  }

  @Override
  protected Job generateExternalTaskRepresentation(ChronosJob mesosTask) {
    Job chronosJob = new Job();
    chronosJob.setName(mesosTask.getId());
    chronosJob.setSchedule(mesosTask.getSchedule());
    chronosJob.setDescription(mesosTask.getDescription());
    chronosJob.setRetries(mesosTask.getRetries());
    chronosJob.setCommand(mesosTask.getCmd());
    chronosJob.setUris(mesosTask.getUris());
    chronosJob.setRunAsUser(mesosTask.getRunAsUser());
    chronosJob.setEnvironmentVariables(mesosTask
        .getEnv()
        .entrySet()
        .stream()
        .map(entry -> {
          EnvironmentVariable envVar = new EnvironmentVariable();
          envVar.setName(entry.getKey());
          envVar.setValue(entry.getValue());
          return envVar;
        })
        .collect(Collectors.toList()));

    chronosJob.setCpus(mesosTask.getCpus());

    chronosJob.setMem(mesosTask.getMemSize());
    chronosJob.setConstraints(mesosTask.getConstraints());
    chronosJob.setEpsilon(mesosTask.getEpsilon());

    chronosJob.setParents(
        mesosTask
            .getParents()
            .stream()
            .map(ChronosJob::getId)
            .collect(Collectors.toList()));
    if (chronosJob.getParents().isEmpty()) {
      chronosJob.setParents(null);
    }

    mesosTask
        .getContainer()
        .ifPresent(mesosContainer -> {
          Container container = generateContainer(mesosContainer);
          Optional
              .ofNullable(mesosTask.getGpus())
              .filter(gpus -> gpus > 0)
              .ifPresent(gpus -> {
                chronosJob.setGpus(Ints.checkedCast(gpus));
                container.setType("MESOS");
              });
          chronosJob.setContainer(container);
        });

    return chronosJob;
  }

  private Container generateContainer(MesosContainer mesosContainer) {
    Container container = new Container();
    if (mesosContainer.getType() == MesosContainer.Type.DOCKER) {
      container.setType(MesosContainer.Type.DOCKER.getName());
      container.setImage(mesosContainer.getImage());
      container.setVolumes(mesosContainer
          .getVolumes()
          .stream()
          .map(this::generateVolume)
          .collect(Collectors.toList()));
      container.setForcePullImage(mesosContainer.isForcePullImage());
      if (mesosContainer.isPrivileged()) {
        Parameters param = new Parameters();
        param.setKey("privileged");
        param.setValue("true");
        Collection<Parameters> parameters = new ArrayList<>();
        parameters.add(param);
        container.setParameters(parameters);
      }
    } else {
      throw new DeploymentException("Unknown Mesos container type: " + mesosContainer.getType());
    }
    return container;
  }

  private Volume generateVolume(String containerVolumeMount) {

    // split the volumeMount string and extract only the non blank strings
    List<String> volumeMountSegments = Arrays
        .stream(containerVolumeMount.split(":"))
        .sequential()
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());

    if (volumeMountSegments.size() != 2) {
      throw new DeploymentException(
        "Volume mount <" + containerVolumeMount + "> not supported for chronos containers");
    }

    Volume volume = new Volume();
    volume.setContainerPath(volumeMountSegments.get(0));
    volume.setMode(volumeMountSegments.get(1).toUpperCase(Locale.US));
    return volume;
  }

  @Override
  public boolean doAction(ActionMessage deploymentMessage) {
    throw new UnsupportedOperationException("Chronos deployments do not support actions.");
  }

  @Override
  public boolean isActionComplete(ActionMessage deploymentMessage) {
    throw new UnsupportedOperationException("Chronos deployments do not support actions.");
  }

  @Override
  public void validateAction(ActionMessage deploymentMessage) {
    throw new UnsupportedOperationException("Chronos deployments do not support actions.");
  }
}
