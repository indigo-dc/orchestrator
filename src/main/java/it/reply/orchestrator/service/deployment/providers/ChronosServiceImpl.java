package it.reply.orchestrator.service.deployment.providers;

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

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IntegerType;
import alien4cloud.tosca.normative.SizeType;
import alien4cloud.tosca.normative.StringType;
import alien4cloud.tosca.parser.ParsingException;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.ChronosClient;
import it.infn.ba.indigo.chronos.client.model.v1.Container;
import it.infn.ba.indigo.chronos.client.model.v1.EnvironmentVariable;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.infn.ba.indigo.chronos.client.model.v1.Parameters;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import it.reply.orchestrator.annotation.DeploymentProviderQualifier;
import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.dto.deployment.DeploymentMessage.TemplateTopologicalOrderIterator;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.utils.CommonUtils;

import org.apache.commons.collections.CollectionUtils;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@DeploymentProviderQualifier(DeploymentProvider.CHRONOS)
@PropertySource(value = { "classpath:application.properties", "${chronos.auth.file.path}" })
public class ChronosServiceImpl extends AbstractDeploymentProviderService
    implements DeploymentProviderService {

  private static final Logger LOG = LoggerFactory.getLogger(DeploymentController.class);

  @Autowired
  ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  @Value("${chronos.endpoint}")
  private String endpoint;
  @Value("${chronos.username}")
  private String username;
  @Value("${chronos.password}")
  private String password;

  @Value("${orchestrator.chronos.jobChunkSize}")
  private int jobChunkSize;

  /**
   * Temporary method to instantiate a default Chronos client (<b>just for experimental purpose</b>
   * ).
   * 
   * @return the Chronos client.
   */
  public Chronos getChronosClient() {
    LOG.info("Generating Chronos client with parameters: URL={}, username={}", endpoint, username);
    Chronos client = ChronosClient.getInstanceWithBasicAuth(endpoint, username, password);

    return client;
  }

  public Collection<Job> getJobs(Chronos client) {
    return client.getJobs();
  }

  @Override
  public boolean doDeploy(DeploymentMessage deploymentMessage) {

    Deployment deployment = deploymentMessage.getDeployment();

    try {
      // Update status of the deployment - if not already done (remember the Iterative mode)
      if (deployment.getTask() != Task.DEPLOYER) {
        deployment.setTask(Task.DEPLOYER);
        deployment.setEndpoint("<NO_ENDPOINT>");
        deployment = getDeploymentRepository().save(deployment);
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
            new TemplateTopologicalOrderIterator(topoOrder.stream()
                .map(e -> new Resource(e.getToscaNodeName()))
                .collect(Collectors.toList())));
      }

      // Create Jobs in the required order on Chronos (but 1 at each invocation)
      LOG.debug("Launching <{}> jobs for deployment <{}> on Chronos", jobChunkSize,
          deployment.getId());
      boolean noMoreJob = false;
      Chronos client = getChronosClient();
      for (int i = 0; i < jobChunkSize && !noMoreJob; i++) {
        noMoreJob =
            !createJobsOnChronosIteratively(deployment, deploymentMessage.getChronosJobGraph(),
                deploymentMessage.getTemplateTopologicalOrderIterator(), client);
      }

      if (noMoreJob) {
        // No more jobs to create
        deploymentMessage.setCreateComplete(true);
        // Start over with the polling check
        deploymentMessage.getTemplateTopologicalOrderIterator().reset();
      }

      // No error occurred
      return true;
    } catch (RuntimeException exception) { // Chronos job launch error
      // TODO use a custom exception ?
      updateOnError(deployment.getId(), exception.getMessage());
      LOG.error("Failed to launch jobs for deployment <{}> on Chronos", exception);
      // The job chain creation failed: Just return false...
      return false;
    } catch (Exception ex) {
      LOG.error("Failed to deploy deployment <{}>", ex);
      updateOnError(deployment.getId(), ex);
      return false;
    }
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

  /**
   * Generate the topological ordering for the given jobGraph.
   * 
   * @param jobgraph
   *          the job graph
   * @return a {@link List} of the {@link IndigoJob} in topological order
   * @throws IllegalArgumentException
   *           if the graph has cycles (hence no topological order exists).
   */
  protected List<IndigoJob> getJobsTopologicalOrder(Map<String, IndigoJob> jobgraph) {
    DefaultDirectedGraph<IndigoJob, DefaultEdge> graph =
        new DefaultDirectedGraph<IndigoJob, DefaultEdge>(DefaultEdge.class);

    for (IndigoJob job : jobgraph.values()) {
      graph.addVertex(job);
    }

    for (IndigoJob job : jobgraph.values()) {
      for (IndigoJob parent : job.getParents()) {
        graph.addEdge(parent, job); // job depends on parent
      }
    }

    LOG.debug("IndigoJob graph: {}", graph.toString());

    // Are there cycles in the dependencies.
    CycleDetector<IndigoJob, DefaultEdge> cycleDetector =
        new CycleDetector<IndigoJob, DefaultEdge>(graph);
    if (cycleDetector.detectCycles()) {
      LOG.error("Job graph has cycles!");
      throw new IllegalArgumentException(String
          .format("Failed to generate topological order for a graph with cycles: <%s>", graph));
    }

    TopologicalOrderIterator<IndigoJob, DefaultEdge> orderIterator =
        new TopologicalOrderIterator<IndigoJob, DefaultEdge>(graph);

    List<IndigoJob> topoOrder = Lists.newArrayList(orderIterator);
    LOG.debug("IndigoJob topological order: {}", topoOrder);

    return topoOrder;
  }

  @Override
  public boolean isDeployed(DeploymentMessage deploymentMessage) throws DeploymentException {

    Deployment deployment = deploymentMessage.getDeployment();
    deploymentMessage.setSkipPollInterval(false);
    try {

      Chronos client = getChronosClient();

      // Follow the Job graph and poll Chronos (higher -less dependent- jobs first) and
      // fail-fast

      // Check jobs status based on the topological order
      TemplateTopologicalOrderIterator templateTopologicalOrderIterator =
          deploymentMessage.getTemplateTopologicalOrderIterator();

      LOG.debug("Polling <{}> jobs for deployment <{}> on Chronos", jobChunkSize,
          deployment.getId());
      boolean noMoreJob = templateTopologicalOrderIterator.getCurrent() == null;
      for (int i = 0; i < jobChunkSize && !noMoreJob; i++) {
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
        deploymentMessage.setPollComplete(true);
        return true;
      } else {
        // Poll the following node next time - Disable poll interval
        LOG.debug("Polling next job for deployment <{}>", deployment.getId());
        deploymentMessage.setSkipPollInterval(true);
        return false;
      }

    } catch (DeploymentException dex) {
      // Deploy failed; let caller know (as for the method definition)
      updateOnError(deployment.getId(), dex);
      throw dex;
    } catch (RuntimeException ex) {
      // Temporary error
      LOG.error(String.format("Failed to update status of deployment <%s>", deployment.getId()),
          ex);
      throw ex;
    }

    // TODO (?) Update resources attributes on DB?
    // TODO Update deployment status (No, the WF command currently does it in the finalize method -
    // not good...)

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
        String errorMsg = String.format(
            "Failed to deploy deployment <%s>. Chronos job <%s> (id: <%s>) status is <%s>",
            deployment.getId(), job.getToscaNodeName(), jobName, jobState);
        LOG.error(errorMsg);
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

    // Find the Resource from DB
    for (Resource resource : resourceRepository
        .findByToscaNodeNameAndDeployment_id(job.getToscaNodeName(), deployment.getId())) {
      resource.setState(state);
    }
    // resourceRepository.save(resource);
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
  public void finalizeDeploy(DeploymentMessage deploymentMessage, boolean deployed) {
    if (deployed) {
      try {

        // TODO Generate template outputs
        /*
         * if (deployment.getOutputs().isEmpty()) { Map<String, String> outputs = new
         * HashMap<String, String>(); for (Entry<String, Object> entry :
         * outputValues.getOutputs().entrySet()) { if (entry.getValue() != null) {
         * outputs.put(entry.getKey(), JsonUtility.serializeJson(entry.getValue())); } else {
         * outputs.put(entry.getKey(), ""); } } deployment.setOutputs(outputs); }
         */

        // Update deployment status
        updateOnSuccess(deploymentMessage.getDeploymentId());
      } catch (Exception ex) {
        LOG.error("Error finalizing deployment", ex);
        // Update deployment status
        updateOnError(deploymentMessage.getDeploymentId(), ex);
      }
    } else {
      // Update deployment status
      updateOnError(deploymentMessage.getDeploymentId());
    }

    // TODO (?) Update resources attributes on DB?

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

  @Override
  public void finalizeUndeploy(DeploymentMessage deploymentMessage, boolean undeployed) {

    if (undeployed) {
      updateOnSuccess(deploymentMessage.getDeploymentId());
    } else {
      updateOnError(deploymentMessage.getDeploymentId());
    }

    // TODO (?) Update resources attributes on DB?

    return;
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
    Deployment deployment = deploymentMessage.getDeployment();

    try {
      // Generate INDIGOJob graph
      // FIXME: Do not regenerate every time (just for prototyping!)
      // Generate INDIGOJob graph
      if (deploymentMessage.getChronosJobGraph() == null) {
        LOG.debug("Generating job graph for deployment <{}>", deployment.getId());
        try {
          deploymentMessage.setChronosJobGraph(
              generateJobGraph(deployment, deploymentMessage.getOneDataParameters()));
        } catch (Exception e2) {
          LOG.error("Parsing error for deployment <{}> on Chronos -> No resource to delete",
              deployment.getId(), e2);
          // No resource to delete -> delete completed
          deploymentMessage.setDeleteComplete(true);
          return true;
        }
      }

      // Create nodes iterator if not done yet
      if (deploymentMessage.getTemplateTopologicalOrderIterator() == null) {
        // Create topological order
        List<IndigoJob> topoOrder = getJobsTopologicalOrder(deploymentMessage.getChronosJobGraph());

        deploymentMessage.setTemplateTopologicalOrderIterator(
            new TemplateTopologicalOrderIterator(topoOrder.stream()
                .map(e -> new Resource(e.getToscaNodeName()))
                .collect(Collectors.toList())));
      }

      TemplateTopologicalOrderIterator templateTopologicalOrderIterator =
          deploymentMessage.getTemplateTopologicalOrderIterator();

      // Delete Jobs
      LOG.debug("Deleting <{}> jobs for deployment <{}> on Chronos", jobChunkSize,
          deployment.getId());
      Chronos client = getChronosClient();
      boolean noMoreJob = templateTopologicalOrderIterator.getCurrent() == null;
      for (int i = 0; i < jobChunkSize && !noMoreJob; i++) {
        deleteJobsOnChronosIteratively(deployment, deploymentMessage.getChronosJobGraph(),
            templateTopologicalOrderIterator, client, true);

        noMoreJob = templateTopologicalOrderIterator.getNext() == null;
      }

      if (noMoreJob) {
        // No more nodes
        LOG.debug("All nodes deleted for deployment <{}>", deployment.getId());
        deploymentMessage.setDeleteComplete(true);
      } else {
        // Delete the following node
        LOG.debug("Deleting next node for deployment <{}>", deployment.getId());
      }

      // No error occurred
      return true;
    } catch (RuntimeException exception) { // Chronos job launch error
      // TODO use a custom exception ?
      updateOnError(deployment.getId(), exception.getMessage());
      LOG.error("Failed to delete jobs for deployment <{}> on Chronos", deployment.getId(),
          exception);
      // The job chain deletion failed: Just return false...
      return false;
    } catch (Exception ex) {
      LOG.error("Failed to clean Chronos deployment <{}>", ex);
      updateOnError(deployment.getId(), ex);
      return false;
    }

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
      START, INTERMEDIATE, END
    }

    private Job chronosJob;
    private String toscaNodeName;
    private Collection<IndigoJob> children = new ArrayList<>();
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

    public Collection<IndigoJob> getChildren() {
      return children;
    }

    public Collection<IndigoJob> getParents() {
      return parents;
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
    String deploymentId = deployment.getId();
    Map<String, IndigoJob> jobs = new HashMap<String, ChronosServiceImpl.IndigoJob>();

    // Parse TOSCA template
    Map<String, NodeTemplate> nodes = null;
    try {
      String customizedTemplate = deployment.getTemplate();
      /*
       * FIXME TEMPORARY - Replace hard-coded properties in nodes (WARNING: Cannot be done when
       * receiving the template because we still miss OneData settings that are obtained during the
       * WF after the site choice, which in turns depends on the template nodes and properties...)
       */
      customizedTemplate = replaceHardCodedParams(customizedTemplate, odParameters);

      // Re-parse template (TODO: serialize the template in-memory representation?)
      ArchiveRoot ar = toscaService.prepareTemplate(customizedTemplate, deployment.getParameters());

      nodes = ar.getTopology().getNodeTemplates();
    } catch (IOException | ParsingException ex) {
      throw new OrchestratorException(ex);
    }

    // TODO Iterate on Chronos nodes and related dependencies (just ignore others - also if invalid
    // - for now)

    // Populate resources (nodes) hashmap to speed up job creation (id-name mapping is needed)
    Map<String, Resource> resources = deployment.getResources()
        .stream()
        .collect(Collectors.toMap(Resource::getToscaNodeName, Function.identity()));

    // Only create Indigo Jobs
    for (Map.Entry<String, NodeTemplate> node : nodes.entrySet()) {
      NodeTemplate nodeTemplate = node.getValue();
      String nodeName = node.getKey();
      if (isChronosNode(nodeTemplate)) {
        Job chronosJob = createJob(nodes, deploymentId, nodeName, nodeTemplate, resources);
        IndigoJob job = new IndigoJob(nodeName, chronosJob);
        jobs.put(nodeName, job);
      }
    }

    // Create jobs hierarchy
    for (Map.Entry<String, IndigoJob> job : jobs.entrySet()) {
      IndigoJob indigoJob = job.getValue();
      String nodeName = job.getKey();
      NodeTemplate nodeTemplate = nodes.get(nodeName);

      // Retrieve Job parents
      List<String> parentNames = getJobParents(nodeTemplate, nodeName, nodes);

      if (parentNames != null && !parentNames.isEmpty()) {
        List<String> chronosParentList = new ArrayList<>();

        for (String parentName : parentNames) {
          IndigoJob parentJob = jobs.get(parentName);
          // Add this job to the parent
          parentJob.getChildren().add(indigoJob);
          // Add the parent to this job
          indigoJob.getParents().add(parentJob);

          // Add to the Chronos DSL parent list
          chronosParentList.add(parentJob.getChronosJob().getName());
        }

        // Update Chronos DSL parent list
        indigoJob.getChronosJob().setParents(chronosParentList);
      }
    }

    // Validate (no cycles!)
    // FIXME Shouldn't just return the topological order ?
    getJobsTopologicalOrder(jobs);

    return jobs;
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
              od.getProviders().get(0).endpoint);
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
              od.getProviders().get(0).endpoint);
      LOG.debug("Replaced {} OneData parameters with: {}", "output", od);
    }

    if (odParameters.containsKey("service")) {
      OneData od = odParameters.get("service");
      if (CollectionUtils.isEmpty(od.getProviders())) {
        throw new DeploymentException("No OneData Providers available for service space");
      }
      // Replace OneData properties
      customizedTemplate = customizedTemplate
          .replace("TOKEN_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getToken())
          .replace("DATA_SPACE_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getSpace())
          .replace("PATH_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getPath())
          .replace("ONEDATA_PROVIDERS_TO_BE_SET_BY_THE_ORCHESTRATOR",
              od.getProviders().get(0).endpoint);
      LOG.debug("Replaced {} OneData parameters with: {}", "service", od);
    }

    return customizedTemplate;
  }

  protected void putStringProperty(NodeTemplate nodeTemplate, String name, String value) {
    ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue(value);
    scalarPropertyValue.setPrintable(true);
    if (nodeTemplate.getProperties() == null) {
      nodeTemplate.setProperties(new HashMap<>());
    }
    nodeTemplate.getProperties().put(name, scalarPropertyValue);
  }

  protected boolean isChronosNode(NodeTemplate nodeTemplate) {
    return nodeTemplate.getType().equals("tosca.nodes.indigo.Container.Application.Docker.Chronos");
  }

  protected List<String> getJobParents(NodeTemplate nodeTemplate, String nodeName,
      Map<String, NodeTemplate> nodes) {
    // Get Chronos parent job dependency
    String parentJobCapabilityName = "parent_job";
    Map<String, NodeTemplate> parentJobs =
        toscaService.getAssociatedNodesByCapability(nodes, nodeTemplate, parentJobCapabilityName);

    if (parentJobs.isEmpty()) {
      return null;
    } else {
      // WARNING: cycle check is done later!
      return Lists.newArrayList(parentJobs.keySet());
    }
  }

  protected Job createJob(Map<String, NodeTemplate> nodes, String deploymentId, String nodeName,
      NodeTemplate nodeTemplate, Map<String, Resource> resources) {
    try {
      Job chronosJob = new Job();
      // Init job infos

      // Get the generated UUID for the node (in DB resource ?)
      // FIXME This is just for prototyping... Otherwise is madness!!
      Resource resourceJob = resources.get(nodeName);

      chronosJob.setName(resourceJob.getId());

      // TODO Validation
      Optional<ScalarPropertyValue> retriesProperty =
          toscaService.getTypedNodePropertyByName(nodeTemplate, "retries");
      if (retriesProperty.isPresent()) {
        chronosJob.setRetries(Ints.saturatedCast(
            toscaService.parseScalarPropertyValue(retriesProperty.get(), IntegerType.class)));
      }

      Optional<ScalarPropertyValue> cmdProperty =
          toscaService.getTypedNodePropertyByName(nodeTemplate, "command");
      if (cmdProperty.isPresent()) {
        chronosJob
            .setCommand(toscaService.parseScalarPropertyValue(cmdProperty.get(), StringType.class));
      }

      // TODO Enable epsilon setting in TOSCA tplt ?
      chronosJob.setEpsilon("PT10S");

      Optional<ListPropertyValue> inputUris =
          CommonUtils.optionalCast(toscaService.getNodePropertyByName(nodeTemplate, "uris"));
      if (inputUris.isPresent()) {
        // Convert List<Object> to List<String>
        chronosJob.setUris(inputUris.get()
            .getValue()
            .stream()
            .map(e -> ((PropertyValue<?>) e).getValue().toString())
            .collect(Collectors.toList()));

      }

      List<EnvironmentVariable> envs = new ArrayList<>();
      Optional<ComplexPropertyValue> inputEnvVars = CommonUtils
          .optionalCast(toscaService.getNodePropertyByName(nodeTemplate, "environment_variables"));
      if (inputEnvVars.isPresent()) {
        for (Map.Entry<String, Object> var : inputEnvVars.get().getValue().entrySet()) {
          EnvironmentVariable envVar = new EnvironmentVariable();
          envVar.setName(var.getKey());
          envVar.setValue(((PropertyValue<?>) var.getValue()).getValue().toString());
          envs.add(envVar);
        }
        chronosJob.setEnvironmentVariables(envs);
      }

      // Docker image
      DeploymentArtifact image;
      // <image> artifact available
      if (nodeTemplate.getArtifacts() == null
          || (image = nodeTemplate.getArtifacts().get("image")) == null) {
        throw new IllegalArgumentException(
            String.format("<image> artifact not found in node <%s> of type <%s>", nodeName,
                nodeTemplate.getType()));
      }

      // TODO Remove hard-coded?
      List<String> supportedTypes =
          Lists.newArrayList("tosca.artifacts.Deployment.Image.Container.Docker");
      // <image> artifact type check
      if (!supportedTypes.contains(image.getArtifactType())) {
        throw new IllegalArgumentException(String.format(
            "Unsupported artifact type for <image> artifact in node <%s> of type <%s>. "
                + "Given <%s>, supported <%s>",
            nodeName, nodeTemplate.getType(), image.getArtifactType(), supportedTypes));
      }

      // Requirements

      // Get Docker host dependency
      String dockerCapabilityName = "host";
      Map<String, NodeTemplate> dockerRelationships =
          toscaService.getAssociatedNodesByCapability(nodes, nodeTemplate, dockerCapabilityName);
      Double dockerNumCpus = null;
      Double dockerMemSize = null;
      if (!dockerRelationships.isEmpty()) {
        /*
         * WARNING: The TOSCA validation should already check the limits (currently Alien4Cloud does
         * not...)
         */
        NodeTemplate dockerNode = dockerRelationships.values().iterator().next();
        Capability dockerCapability = dockerNode.getCapabilities().get(dockerCapabilityName);
        dockerNumCpus = Double.parseDouble(CommonUtils
            .<ScalarPropertyValue>optionalCast(
                toscaService.getCapabilityPropertyByName(dockerCapability, "num_cpus"))
            .get()
            .getValue());

        // Converting Memory Size (as TOSCA scalar-unit.size)
        SizeType tmp = new SizeType();
        String memSizeRaw = CommonUtils
            .<ScalarPropertyValue>optionalCast(
                toscaService.getCapabilityPropertyByName(dockerCapability, "mem_size"))
            .get()
            .getValue();
        dockerMemSize = tmp.parse(memSizeRaw).convert("MB"); // Chronos wants MB
      }

      Container container = new Container();
      container.setType("DOCKER");

      // Run the container in Privileged Mode
      Parameters param = new Parameters();
      param.setKey("privileged");
      param.setValue("true");
      Collection<Parameters> parameters = new ArrayList<Parameters>();
      parameters.add(param);
      container.setParameters(parameters);

      // FIXME ForcePullImage must be parametrizable by tosca template
      container.setForcePullImage(true);
      ////////////////////////////////////////////////////////////////

      String imageName =
          CommonUtils.<ScalarPropertyValue>optionalCast(image.getFile())
              .orElseThrow(() -> new IllegalArgumentException(String.format(
                  "<file> field for <image> artifact in node <%s> must be provided", nodeName)))
              .getValue();
      container.setImage(imageName);

      if (dockerNumCpus != null) {
        chronosJob.setCpus(dockerNumCpus);
      }
      if (dockerMemSize != null) {
        chronosJob.setMem(dockerMemSize);
      }

      chronosJob.setContainer(container);

      return chronosJob;
    } catch (Exception ex) {
      throw new RuntimeException(String.format("Failed to parse node <%s> of type <%s>: %s",
          nodeName, nodeTemplate.getType(), ex.getMessage()), ex);
    }
  }

  public enum JobState {
    FRESH, FAILURE, SUCCESS;
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
}
