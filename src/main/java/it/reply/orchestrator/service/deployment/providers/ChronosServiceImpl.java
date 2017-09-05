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

package it.reply.orchestrator.service.deployment.providers;

import com.google.common.primitives.Ints;

import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IntegerType;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.ChronosClient;
import it.infn.ba.indigo.chronos.client.model.v1.Container;
import it.infn.ba.indigo.chronos.client.model.v1.EnvironmentVariable;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.infn.ba.indigo.chronos.client.model.v1.Parameters;
import it.infn.ba.indigo.chronos.client.model.v1.Volume;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.config.properties.ChronosProperties;
import it.reply.orchestrator.config.properties.OrchestratorProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage.TemplateTopologicalOrderIterator;
import it.reply.orchestrator.dto.mesos.MesosContainer;
import it.reply.orchestrator.dto.mesos.chronos.ChronosJob;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@DeploymentProviderQualifier(DeploymentProvider.CHRONOS)
@Slf4j
@EnableConfigurationProperties(ChronosProperties.class)
public class ChronosServiceImpl extends AbstractMesosDeploymentService<ChronosJob, Job> {

  @Autowired
  private ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private ChronosProperties chronosProperties;

  @Autowired
  private OrchestratorProperties orchestratorProperties;

  /**
   * Temporary method to instantiate a default Chronos client (<b>just for experimental purpose</b>
   * ).
   * 
   * @return the Chronos client.
   */
  public Chronos getChronosClient() {
    LOG.info("Generating Chronos client with parameters: {}", chronosProperties);
    return ChronosClient.getInstanceWithBasicAuth(chronosProperties.getUrl().toString(),
        chronosProperties.getUsername(), chronosProperties.getPassword());
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

    // TODO Get/Check Chronos cluster

    // TODO Replace attribute, inputs, temporary-hard-coded properties in the TOSCA template

    // Generate INDIGOJob graph
    if (deploymentMessage.getChronosJobGraph() == null) {
      LOG.debug("Generating job graph for deployment <{}>", deployment.getId());
      deploymentMessage.setChronosJobGraph(
          generateJobGraph(deployment, deploymentMessage.getOneDataParameters()));
    }

    // Create nodes iterator if not done yet
    if (deploymentMessage.getTemplateTopologicalOrderIterator() == null) {
      // Create topological order
      List<IndigoJob> topoOrder = getJobsTopologicalOrder(deploymentMessage.getChronosJobGraph());

      deploymentMessage.setTemplateTopologicalOrderIterator(
          new TemplateTopologicalOrderIterator(topoOrder
              .stream()
              .map(e -> {
                Resource res = new Resource();
                res.setToscaNodeName(e.getToscaNodeName());
                res.setState(NodeStates.INITIAL);
                return res;
              })
              .collect(Collectors.toList())));
    }

    // Create Jobs in the required order on Chronos (but 1 at each invocation)
    LOG.debug("Launching <{}> jobs for deployment <{}> on Chronos",
        orchestratorProperties.getJobChunkSize(),
        deployment.getId());
    boolean noMoreJob = false;
    Chronos client = getChronosClient();
    for (int i = 0; i < orchestratorProperties.getJobChunkSize() && !noMoreJob; i++) {
      noMoreJob =
          !createJobsOnChronosIteratively(deployment, deploymentMessage.getChronosJobGraph(),
              deploymentMessage.getTemplateTopologicalOrderIterator(), client);
    }

    if (noMoreJob) {
      // Start over with the polling check
      deploymentMessage.getTemplateTopologicalOrderIterator().reset();
    }

    // No error occurred
    return noMoreJob;
  }

  /**
   * 
   * @param deployment
   *          the deployment from which create the jobs
   * @param jobgraph
   *          the graph of the jobs
   * @param templateTopologicalOrderIterator
   *          the topological order iterator of the jobs
   * @param client
   *          the Chronos client to use
   * @return <tt>true</tt> if there are more nodes to create, <tt>false</tt> otherwise.
   */
  protected boolean createJobsOnChronosIteratively(Deployment deployment,
      Map<String, IndigoJob> jobgraph,
      TemplateTopologicalOrderIterator templateTopologicalOrderIterator, Chronos client) {

    // Create Jobs in the required order on Chronos
    Resource currentNode = templateTopologicalOrderIterator.getCurrent();
    if (currentNode == null) {
      return false;
    }

    IndigoJob currentJob = jobgraph.get(currentNode.getToscaNodeName());

    // Create jobs based on the topological order
    try {
      String nodeTypeMsg;
      if (currentJob.getParents().isEmpty()) {
        // Scheduled job (not dependent)
        nodeTypeMsg = "scheduled";
        client.createJob(currentJob.getChronosJob());
      } else {
        // Dependent job
        nodeTypeMsg = String.format("parents <%s>", currentJob.getChronosJob().getParents());
        client.createDependentJob(currentJob.getChronosJob());
      }

      LOG.debug("Created job for deployment <{}> on Chronos: name <{}>, {} ({}/{})",
          deployment.getId(), currentJob.getChronosJob().getName(), nodeTypeMsg,
          templateTopologicalOrderIterator.getPosition() + 1,
          templateTopologicalOrderIterator.getNodeSize());
      // Update job status
      updateResource(deployment, currentJob, NodeStates.CREATED);
      // The node in the iterator is not actually an entity (serialization issues)
      currentNode.setState(NodeStates.CREATED);

    } catch (ChronosException exception) { // Chronos job launch error
      // Update job status
      updateResource(deployment, currentJob, NodeStates.ERROR);
      currentNode.setState(NodeStates.ERROR);
      // TODO use a custom exception ?
      throw new RuntimeException(
          String.format("Failed to launch job <%s> on Chronos. Status Code: <%s>",
              currentJob.getChronosJob().getName(), exception.getStatus()));
    }

    return templateTopologicalOrderIterator.getNext() != null;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) throws DeploymentException {

    Deployment deployment = getDeployment(deploymentMessage);
    deploymentMessage.setSkipPollInterval(false);

    Chronos client = getChronosClient();

    // Follow the Job graph and poll Chronos (higher -less dependent- jobs first) and
    // fail-fast

    // Check jobs status based on the topological order
    TemplateTopologicalOrderIterator templateTopologicalOrderIterator =
        deploymentMessage.getTemplateTopologicalOrderIterator();

    LOG.debug("Polling <{}> jobs for deployment <{}> on Chronos",
        orchestratorProperties.getJobChunkSize(),
        deployment.getId());
    boolean noMoreJob = templateTopologicalOrderIterator.getCurrent() == null;
    for (int i = 0; i < orchestratorProperties.getJobChunkSize() && !noMoreJob; i++) {
      boolean jobCompleted =
          checkJobsOnChronosIteratively(deployment, deploymentMessage.getChronosJobGraph(),
              deploymentMessage.getTemplateTopologicalOrderIterator(), client);
      if (!jobCompleted) {
        // Job still in progress
        // Wait before retrying to poll on the same node
        deploymentMessage.setSkipPollInterval(false);
        return false;
      }

      noMoreJob = templateTopologicalOrderIterator.getNext() == null;
    }

    if (noMoreJob) {
      // No more jobs
      LOG.debug("Polling complete for deployment <{}>", deployment.getId());
    } else {
      // Poll the following node next time - Disable poll interval
      LOG.debug("Polling next job for deployment <{}>", deployment.getId());
      deploymentMessage.setSkipPollInterval(true);
    }
    return noMoreJob;

  }

  /**
   * 
   * @param deployment
   *          the deployment from which create the jobs
   * @param jobgraph
   *          the graph of the jobs
   * @param templateTopologicalOrderIterator
   *          the topological order iterator of the jobs
   * @param client
   *          the Chronos client to use
   * @return <tt>true</tt> if the currently checked node is ready, <tt>false</tt> if still in
   *         progress.
   * @throws DeploymentException
   *           if the currently node failed.
   */
  protected boolean checkJobsOnChronosIteratively(Deployment deployment,
      Map<String, IndigoJob> jobgraph,
      TemplateTopologicalOrderIterator templateTopologicalOrderIterator, Chronos client)
      throws DeploymentException {

    // Get current job
    Resource currentNode = templateTopologicalOrderIterator.getCurrent();
    IndigoJob job = jobgraph.get(currentNode.getToscaNodeName());

    String jobName = job.getChronosJob().getName();
    Job updatedJob = getJobStatus(client, jobName);
    if (updatedJob == null) {
      String errorMsg = String.format(
          "Failed to deploy deployment <%s>. Chronos job <%s> (id: <%s>) does not exist",
          deployment.getId(), job.getToscaNodeName(), jobName);
      LOG.error(errorMsg);
      // Update job status
      updateResource(deployment, job, NodeStates.ERROR);
      throw new DeploymentException(errorMsg);
    }

    JobState jobState = getLastState(updatedJob);
    LOG.debug("Status of Chronos job <{}> for deployment <{}> is <{}> ({}/{})", jobName,
        deployment.getId(), jobState, templateTopologicalOrderIterator.getPosition() + 1,
        templateTopologicalOrderIterator.getNodeSize());

    // Go ahead only if the job succeeded
    if (jobState != JobState.SUCCESS) {
      if (jobState != JobState.FAILURE) {
        // Job still in progress
        LOG.debug("Polling again job <{}> for deployment <{}>", jobName, deployment.getId());
        return false;
      } else {
        // Job failed -> Deployment failed!
        String errorMsg = String.format("Chronos job <%s> failed to execute", jobName);
        // Update job status
        updateResource(deployment, job, NodeStates.ERROR);
        currentNode.setState(NodeStates.ERROR);
        throw new DeploymentException(errorMsg);
      }
    } else {
      // Job finished -> Update job status
      updateResource(deployment, job, NodeStates.STARTED);
      currentNode.setState(NodeStates.STARTED);
      return true;
    }
  }

  private void updateResource(Deployment deployment, IndigoJob job, NodeStates state) {
    resourceRepository
        .findByToscaNodeNameAndDeployment_id(job.getToscaNodeName(), deployment.getId())
        .forEach(resource -> resource.setState(state));
  }

  /**
   * 
   * @param client
   *          the Chronos client to use
   * @param name
   *          the name of the Chronos job
   * @return the {@link Job} or <tt>null</tt> if no such job exist.
   * @throws RuntimeException
   *           if an error occurred retrieving job status.
   */
  protected Job getJobStatus(Chronos client, String name) throws RuntimeException {
    try {
      Collection<Job> jobList = client.getJob(name);
      if (jobList.isEmpty()) {
        return null;
      }

      return jobList.iterator().next();
    } catch (Exception ex) {
      // TODO Use a custom exception
      throw new RuntimeException(
          String.format("Unable to retrieve job <%s> status on Chronos", name), ex);
    }
  }

  @Override
  public boolean doUpdate(DeploymentMessage deploymentMessage, String template) {
    throw new UnsupportedOperationException("Chronos job deployments do not support update.");
  }

  @Override
  public boolean isUndeployed(DeploymentMessage deploymentMessage) throws DeploymentException {
    // Nothing to wait here... All the jobs are delete immediately.
    return true;
  }


  /**
   * Deletes all the deployment jobs from Chronos. <br/>
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

    try {
      // Generate INDIGOJob graph
      // FIXME: Do not regenerate every time (just for prototyping!)
      // Generate INDIGOJob graph
      if (deploymentMessage.getChronosJobGraph() == null) {
        LOG.debug("Generating job graph for deployment <{}>", deployment.getId());
        deploymentMessage.setChronosJobGraph(
            generateJobGraph(deployment, deploymentMessage.getOneDataParameters()));
      }

      // Create nodes iterator if not done yet
      if (deploymentMessage.getTemplateTopologicalOrderIterator() == null) {
        // Create topological order
        List<IndigoJob> topoOrder = getJobsTopologicalOrder(deploymentMessage.getChronosJobGraph());

        deploymentMessage.setTemplateTopologicalOrderIterator(
            new TemplateTopologicalOrderIterator(topoOrder
                .stream()
                .map(e -> {
                  Resource res = new Resource();
                  res.setToscaNodeName(e.getToscaNodeName());
                  res.setState(NodeStates.INITIAL);
                  return res;
                })
                .collect(Collectors.toList())));
      }
    } catch (RuntimeException ex) {
      LOG.error(
          "Error generating the topology for deployment {} during its deletion.\n{}",
          deployment.getId(), "Deployment will be marked as deleted", ex);
      // if we can't generate the topology -> just set as deleted
      // as we don't check anymore for the nullness of the deployment endpoint, we must be able to
      // delete deployment for which the deploy failure also happened during the topology generation
      return true; // noMoreJob = true
    }

    TemplateTopologicalOrderIterator templateTopologicalOrderIterator =
        deploymentMessage.getTemplateTopologicalOrderIterator();

    // Delete Jobs
    LOG.debug("Deleting <{}> jobs for deployment <{}> on Chronos",
        orchestratorProperties.getJobChunkSize(),
        deployment.getId());
    Chronos client = getChronosClient();
    boolean noMoreJob = templateTopologicalOrderIterator.getCurrent() == null;
    for (int i = 0; i < orchestratorProperties.getJobChunkSize() && !noMoreJob; i++) {
      deleteJobsOnChronosIteratively(deployment, deploymentMessage.getChronosJobGraph(),
          templateTopologicalOrderIterator, client, true);

      noMoreJob = templateTopologicalOrderIterator.getNext() == null;
    }

    if (noMoreJob) {
      // No more nodes
      LOG.debug("All nodes deleted for deployment <{}>", deployment.getId());
    } else {
      // Delete the following node
      LOG.debug("Deleting next node for deployment <{}>", deployment.getId());
    }

    // No error occurred
    return noMoreJob;

  }

  private List<IndigoJob> getJobsTopologicalOrder(Map<String, IndigoJob> chronosJobGraph) {
    return chronosJobGraph.values().stream().collect(Collectors.toList());
  }

  /**
   * Deletes all the jobs from Chronos.
   * 
   * @param jobgraph
   *          the job graph.
   * @param client
   *          the {@link Chronos} client.
   * @param failAtFirst
   *          if <tt>true</tt> throws an exception at the first job deletion error, otherwise it
   *          tries to delete every other job.
   * @return <tt>true</tt> if all jobs have been deleted, <tt>false</tt> otherwise.
   */
  protected boolean deleteJobsOnChronosIteratively(Deployment deployment,
      Map<String, IndigoJob> jobgraph,
      TemplateTopologicalOrderIterator templateTopologicalOrderIterator, Chronos client,
      boolean failAtFirst) {

    Resource currentNode = templateTopologicalOrderIterator.getCurrent();
    if (currentNode == null) {
      return false;
    }

    IndigoJob currentJob = jobgraph.get(currentNode.getToscaNodeName());
    boolean failed = false;

    // Delete current job (all jobs iteratively)
    try {
      try {
        updateResource(deployment, currentJob, NodeStates.DELETING);
        currentNode.setState(NodeStates.DELETING);

        String jobName = currentJob.getChronosJob().getName();

        client.deleteJob(jobName);

        LOG.debug("Deleted Chronos job <{}> for deployment <{}> ({}/{})", jobName,
            deployment.getId(), templateTopologicalOrderIterator.getPosition() + 1,
            templateTopologicalOrderIterator.getNodeSize());
      } catch (ChronosException ce) {
        // Chronos API hack (to avoid error 400 if the job to delete does not exist)
        if (ce.getStatus() != 400 && ce.getStatus() != 404) {
          throw new RuntimeException(String.format("Status Code: <%s>", ce.getStatus()));
        } else {
          // TODO do we need to consider all the other jobs as not existing if endpoint is null? 
          // Attentions: Are we sure that graph dependency generation is stable?
        }
      }
    } catch (Exception ex) {
      // Just log the error
      String errorMsg =
          String.format("Failed to delete job <%s> on Chronos: %s", currentJob, ex.getMessage());
      LOG.error(errorMsg);

      failed = true;
      // Update job status
      updateResource(deployment, currentJob, NodeStates.ERROR);
      currentNode.setState(NodeStates.ERROR);

      // Only throw exception if required
      if (failAtFirst) {
        // TODO use a custom exception ?
        throw new RuntimeException(errorMsg);
      }
    }

    return !failed;
  }

  public static class IndigoJob implements Serializable {

    private static final long serialVersionUID = -1037947811308004122L;

    public enum JobDependencyType {
      START,
      INTERMEDIATE,
      END
    }

    private Job chronosJob;
    private String toscaNodeName;
    private Collection<IndigoJob> parents = new ArrayList<>();

    /**
     * Generates a new IndigoJob representation.
     * 
     * @param toscaNodeName
     *          the name of the TOSCA node associated to the job
     * @param chronosJob
     *          the {@link Job Chronos job} associated to the job
     */
    public IndigoJob(String toscaNodeName, Job chronosJob) {
      super();
      this.toscaNodeName = toscaNodeName;
      this.chronosJob = chronosJob;
    }

    public String getToscaNodeName() {
      return toscaNodeName;
    }

    public Job getChronosJob() {
      return chronosJob;
    }

    public Collection<IndigoJob> getParents() {
      return parents;
    }
    
    public void setParents(Collection<IndigoJob> parents) {
      this.parents = parents;
    }

    @Override
    public String toString() {
      return "IndigoJob [toscaNodeName=" + toscaNodeName + ", chronosJob=" + chronosJob.getName();
    }

  }

  /**
   * Creates the {@link INDIGOJob} graph based on the given {@link Deployment} (the TOSCA template
   * is parsed).
   * 
   * @param deployment
   *          the input deployment.
   * @return the job graph.
   */
  protected Map<String, IndigoJob> generateJobGraph(Deployment deployment,
      Map<String, OneData> odParameters) {

    /*
     * FIXME TEMPORARY - Replace hard-coded properties in nodes (WARNING: Cannot be done when
     * receiving the template because we still miss OneData settings that are obtained during the WF
     * after the site choice, which in turns depends on the template nodes and properties...)
     */
    String customizedTemplate = replaceHardCodedParams(deployment.getTemplate(), odParameters);

    ArchiveRoot ar = toscaService.prepareTemplate(customizedTemplate, deployment.getParameters());

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
        .filter(node -> toscaService.isOfToscaType(node, ToscaConstants.Nodes.CHRONOS))
        .collect(Collectors.toList());

    Map<String, Resource> resources = deployment
        .getResources()
        .stream()
        .filter(resource -> toscaService.isOfToscaType(resource,
            ToscaConstants.Nodes.CHRONOS))
        .collect(Collectors.toMap(Resource::getToscaNodeName, Function.identity()));

    LinkedHashMap<String, ChronosJob> jobs = new LinkedHashMap<>();
    LinkedHashMap<String, IndigoJob> indigoJobs = new LinkedHashMap<>();
    for (NodeTemplate chronosNode : orderedChronosJobs) {
      Resource jobResource = resources.get(chronosNode.getName());
      String id = jobResource.getIaasId();
      if (id == null) {
        id = jobResource.getId();
        jobResource.setIaasId(id);
      }
      ChronosJob mesosTask = buildTask(graph, chronosNode, id);
      jobs.put(chronosNode.getName(), mesosTask);
      List<NodeTemplate> parentNodes = getParentNodes("parent_job", graph, chronosNode);
      mesosTask.setParents(parentNodes
          .stream()
          .map(parentNode -> jobs.get(parentNode.getName()))
          .collect(Collectors.toList()));
      Job chronosJob = generateExternalTaskRepresentation(mesosTask);
      IndigoJob indigoJob = new IndigoJob(chronosNode.getName(), chronosJob);
      indigoJob.setParents(parentNodes
          .stream()
          .map(parentNode -> indigoJobs.get(parentNode.getName()))
          .collect(Collectors.toList()));
      indigoJobs.put(chronosNode.getName(), indigoJob);
    }

    return indigoJobs;
  }

  /**
   * TEMPORARY method to replace hardcoded INDIGO params in TOSCA template (i.e. OneData) string.
   * 
   * @param template
   *          the string TOSCA template.
   * @param odParameters
   *          the OneData settings.
   */
  public String replaceHardCodedParams(String template, Map<String, OneData> odParameters) {

    LOG.debug("Replacing OneData parameters");

    String customizedTemplate = template;
    if (odParameters.containsKey("input")) {
      OneData od = odParameters.get("input");
      if (CollectionUtils.isEmpty(od.getProviders())) {
        throw new DeploymentException("No OneData Providers available for input");
      }
      // Replace OneData properties
      customizedTemplate =
          customizedTemplate.replace("INPUT_ONEDATA_PROVIDERS_TO_BE_SET_BY_THE_ORCHESTRATOR",
              od.getProviders().get(0).getEndpoint());
      LOG.debug("Replaced {} OneData parameters with: {}", "input", od);
    }

    if (odParameters.containsKey("output")) {
      OneData od = odParameters.get("output");
      if (CollectionUtils.isEmpty(od.getProviders())) {
        throw new DeploymentException("No OneData Providers available for output");
      }
      // Replace OneData properties
      customizedTemplate =
          customizedTemplate.replace("OUTPUT_ONEDATA_PROVIDERS_TO_BE_SET_BY_THE_ORCHESTRATOR",
              od.getProviders().get(0).getEndpoint());
      LOG.debug("Replaced {} OneData parameters with: {}", "output", od);
    }

    if (odParameters.containsKey("service")) {
      OneData od = odParameters.get("service");
      if (CollectionUtils.isEmpty(od.getProviders())) {
        throw new DeploymentException("No OneData Providers available for service space");
      }
      // Replace OneData properties
      customizedTemplate =
          customizedTemplate
              .replace("TOKEN_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getToken())
              .replace("DATA_SPACE_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getSpace())
              .replace("PATH_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getPath())
              .replace("ONEDATA_PROVIDERS_TO_BE_SET_BY_THE_ORCHESTRATOR",
                  od.getProviders().get(0).getEndpoint());
      LOG.debug("Replaced {} OneData parameters with: {}", "service", od);
    }

    return customizedTemplate;
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
  public static JobState getLastState(Job job) {
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
  protected ChronosJob createInternalTaskRepresentation() {
    return new ChronosJob();
  }

  @Override
  public ChronosJob buildTask(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode, String taskId) {
    ChronosJob job = super.buildTask(graph, taskNode, taskId);

    toscaService
        .<ScalarPropertyValue>getTypedNodePropertyByName(taskNode, "retries")
        .ifPresent(property -> job.setRetries(Ints
            .saturatedCast(toscaService.parseScalarPropertyValue(property, IntegerType.class))));
    
    return job;
  }

  @Override
  protected Job generateExternalTaskRepresentation(ChronosJob mesosTask) {
    Job chronosJob = new Job();
    chronosJob.setName(mesosTask.getId());
    chronosJob.setRetries(mesosTask.getRetries());
    chronosJob.setCommand(mesosTask.getCmd());
    chronosJob.setUris(mesosTask.getUris());
    
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

    chronosJob.setParents(
        mesosTask
            .getParents()
            .stream()
            .map(ChronosJob::getId)
            .collect(Collectors.toList()));
    
    mesosTask
        .getContainer()
        .ifPresent(mesosContainer -> chronosJob
            .setContainer(generateContainer(mesosContainer)));

    //// HARDCODED BITS //////
    chronosJob.setEpsilon("PT10S");
    //////////////////////////

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
      //// HARDCODED BITS //////
      Parameters param = new Parameters();
      param.setKey("privileged");
      param.setValue("true");
      Collection<Parameters> parameters = new ArrayList<>();
      parameters.add(param);
      container.setParameters(parameters);
      container.setForcePullImage(true);
      //////////////////////////

    } else {
      throw new DeploymentException(
          "Unknown Mesos container type: " + mesosContainer.getType().toString());
    }
    return container;
  }
  
  private Volume generateVolume(String containerVolumeMount) {

    // split the volumeMount string and extract only the non blank strings
    List<String> volumeMountSegments = Arrays
        .asList(containerVolumeMount.split(":"))
        .stream()
        .sequential()
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());

    if (volumeMountSegments.size() != 3) {
      throw new DeploymentException(String
          .format("Volume mount <%s> not supported for chronos containers", containerVolumeMount));
    }
    
    Volume volume = new Volume();
    volume.setHostPath(volumeMountSegments.get(0));
    volume.setContainerPath(volumeMountSegments.get(1));
    volume.setMode(volumeMountSegments.get(2).toUpperCase(Locale.US));
    return volume;
  }
}
