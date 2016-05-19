package it.reply.orchestrator.service.deployment.providers;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.SizeType;
import alien4cloud.tosca.parser.ParsingException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.ChronosClient;
import it.infn.ba.indigo.chronos.client.model.v1.Container;
import it.infn.ba.indigo.chronos.client.model.v1.EnvironmentVariable;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Task;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.ToscaService;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.IndigoJob.JobDependencyType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
@Qualifier("CHRONOS")
public class ChronosServiceImpl extends AbstractDeploymentProviderService
    implements DeploymentProviderService {

  private static final Logger LOG = LogManager.getLogger(DeploymentController.class);

  @Autowired
  ToscaService toscaService;

  @Autowired
  private ResourceRepository resourceRepository;

  /**
   * Temporary method to load properties from file (<b>just for experimental purpose</b>).
   * 
   * @return the properties.
   */
  public Properties readProperties(String path) {
    final Properties properties = new Properties();
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      properties.load(in);
    } catch (Exception ex) {
      throw new RuntimeException(
          "Failed to load properties file ('" + path + "') in classpath 'resources' folder");
    }
    return properties;
  }

  /**
   * Temporary method to instantiate a default Chronos client (<b>just for experimental purpose</b>
   * ).
   * 
   * @return the Chronos client.
   */
  public Chronos getChronosClient() {
    Properties properties = readProperties("chronos.properties");
    String endpoint = properties.getProperty("endpoint");
    String username = properties.getProperty("username");
    String password = properties.getProperty("password");

    LOG.info(String.format("Generating Chronos client with parameters: URL=%s, username=%s",
        endpoint, username));
    Chronos client = ChronosClient.getInstanceWithBasicAuth(endpoint, username, password);

    return client;
  }

  /**
   * Temporary method to generate default OneData settings.
   * 
   * @return the {@link OneData} settings.
   */
  protected OneData generateStubOneData() {
    Properties properties = readProperties("onedata.properties");
    String token = properties.getProperty("token");
    String space = properties.getProperty("space");
    String path = properties.getProperty("path");
    String provider = properties.getProperty("provider");

    LOG.info(String.format("Generating OneData settings with parameters: %s", properties));

    return new OneData(token, space, path, provider);
  }

  public Collection<Job> getJobs(Chronos client) {
    return client.getJobs();
  }

  @Override
  public boolean doDeploy(Deployment deployment) {

    try {
      // Update status of the deployment
      deployment.setTask(Task.DEPLOYER);
      deployment.setDeploymentProvider(DeploymentProvider.CHRONOS);
      deployment = getDeploymentRepository().save(deployment);

      // TODO Get/Check Chronos cluster

      // TODO Replace attribute, inputs, temporary-hard-coded properties in the TOSCA template

      // Generate INDIGOJob graph
      LOG.debug("Generating job graph for deployment <{}>", deployment.getId());
      Multimap<JobDependencyType, IndigoJob> jobgraph =
          generateJobGraph(deployment, generateStubOneData());

      // Create Jobs in the required order on Chronos
      LOG.debug("Launching jobs for deployment <{}> on Chronos", deployment.getId());
      createJobsOnChronos(jobgraph, getChronosClient());

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

  protected void createJobsOnChronos(Multimap<JobDependencyType, IndigoJob> jobgraph,
      Chronos client) {
    // Create Jobs in the required order on Chronos

    IndigoJob currentJob = null;
    try {
      // Create topological order
      List<IndigoJob> topoOrder = getJobsTopologicalOrder(jobgraph);

      // Create jobs based on the topological order
      for (IndigoJob job : topoOrder) {
        currentJob = job;
        String nodeTypeMsg;
        if (job.getParents().isEmpty()) {
          // Scheduled job (not dependent)
          nodeTypeMsg = "scheduled";
          client.createJob(job.getChronosJob());
        } else {
          // Dependent job
          nodeTypeMsg = String.format("parents <%s>", job.getChronosJob().getParents());
          client.createDependentJob(job.getChronosJob());
        }

        LOG.debug("Created job on Chronos: name <{}>, {}", currentJob.getChronosJob().getName(),
            nodeTypeMsg);
      }

    } catch (ChronosException exception) { // Chronos job launch error
      // TODO use a custom exception ?
      throw new RuntimeException(
          String.format("Failed to launch job <%s> on Chronos. Status Code: <%s>",
              currentJob.getChronosJob().getName(), exception.getStatus()));
    }
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
  protected List<IndigoJob>
      getJobsTopologicalOrder(Multimap<JobDependencyType, IndigoJob> jobgraph) {
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
  public boolean isDeployed(Deployment deployment) throws DeploymentException {

    try {
      // Generate INDIGOJob graph
      // FIXME: Do not regenerate every time (just for prototyping!)
      LOG.debug("Generating job graph for deployment <{}>", deployment.getId());
      Multimap<JobDependencyType, IndigoJob> jobgraph =
          generateJobGraph(deployment, generateStubOneData());

      Chronos client = getChronosClient();

      // Follow the Job graph and poll Chronos (higher -less dependent- jobs first) and
      // fail-fast

      // Create topological order
      List<IndigoJob> topoOrder = getJobsTopologicalOrder(jobgraph);

      // Check jobs status based on the topological order
      for (IndigoJob job : topoOrder) {
        String jobName = job.getChronosJob().getName();
        Job updatedJob = getJobStatus(client, jobName);
        JobState jobState = getLastState(updatedJob);
        LOG.debug("Status for Chronos job <{}> is <{}>", jobName, jobState);

        // Go ahead only if the job succeeded
        if (!checkJobState(deployment.getId(), jobName, jobState)) {
          return false;
        }
      }

      // Here all task succeeded -> deployment is ready
      return true;
    } catch (DeploymentException dex) {
      // Deploy failed; let caller know (as for the method definitiion)
      throw dex;
    } catch (RuntimeException ex) {
      LOG.error("Failed to update deployment <{}>", ex);
      return false;
    }

    // TODO (?) Update resources attributes on DB?
    // TODO Update deployment status (No, the WF command currently does it in the finalize method -
    // not good...)

  }

  /**
   * 
   * @param deploymentId
   * @param jobName
   * @param jobState
   * @return <tt>true</tt> if the job succeeded, <tt>false</tt> if the job is still in progress.
   * @throws DeploymentException
   *           if the job failed.
   */
  protected boolean checkJobState(String deploymentId, String jobName, JobState jobState)
      throws DeploymentException {
    if (jobState == JobState.SUCCESS) {
      return true;
    }

    if (jobState != JobState.FAILURE) {
      // Job still in progress
      return false;
    } else {
      // Job failed -> Deployment failed!
      String errorMsg =
          String.format("Failed to deploy deployment <%s>. Chronos job <%s> status is <%s>",
              deploymentId, jobName, jobState);
      LOG.error(errorMsg);
      throw new DeploymentException(errorMsg);
    }
  }

  protected Job getJobStatus(Chronos client, String name) throws RuntimeException {
    try {
      Collection<Job> jobList = client.getJob(name);
      if (jobList.isEmpty()) {
        throw new RuntimeException("Empty list");
      }

      return jobList.iterator().next();
    } catch (Exception ex) {
      // TODO Use a custom exception
      throw new RuntimeException(
          String.format("Unable to retrieve job <%s> status on Chronos", name), ex);
    }
  }

  @Override
  public void finalizeDeploy(Deployment deployment, boolean deployed) {
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
        updateOnSuccess(deployment.getId());
      } catch (Exception ex) {
        LOG.error(ex);
        // Update deployment status
        updateOnError(deployment.getId(), ex);
      }
    } else {
      // Update deployment status
      updateOnError(deployment.getId());
    }

    // TODO (?) Update resources attributes on DB?

  }

  @Override
  public boolean doUpdate(Deployment deployment, String template) {
    throw new UnsupportedOperationException("Chronos job deployments do not support update.");
  }

  @Override
  public boolean isUndeployed(Deployment deployment) throws DeploymentException {
    // Nothing to wait here... All the jobs are delete immediately.
    return true;
  }

  @Override
  public void finalizeUndeploy(Deployment deployment, boolean undeployed) {

    if (undeployed) {
      updateOnSuccess(deployment.getId());
    } else {
      updateOnError(deployment.getId());
    }

    // TODO (?) Update resources attributes on DB?

    return;
  }

  /**
   * Deletes all the deployment jobs from Chronos. <br/>
   * Also logs possible errors and updates the deployment status.
   * 
   * @param deployment
   *          the deployment.
   * @return <tt>true</tt> if all jobs have been deleted, <tt>false</tt> otherwise.
   */
  public boolean doUndeploy(Deployment deployment) {
    // Delete all Jobs on Chronos

    try {
      // Generate INDIGOJob graph
      // FIXME: Do not regenerate every time (just for prototyping!)
      LOG.debug("Generating job graph for deployment <{}>", deployment.getId());
      Multimap<JobDependencyType, IndigoJob> jobgraph =
          generateJobGraph(deployment, generateStubOneData());

      // Create Jobs in the required order on Chronos
      LOG.debug("Deleting jobs for deployment <{}> on Chronos", deployment.getId());
      deleteJobsOnChronos(jobgraph, getChronosClient(), true);

      return true;
    } catch (RuntimeException exception) { // Chronos job launch error
      // TODO use a custom exception ?
      updateOnError(deployment.getId(), exception.getMessage());
      LOG.error("Failed to delete jobs for deployment <{}> on Chronos", exception);
      // The job chain creation failed: Just return false...
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
  protected boolean deleteJobsOnChronos(Multimap<JobDependencyType, IndigoJob> jobgraph,
      Chronos client, boolean failAtFirst) {

    IndigoJob currentJob = null;
    boolean failed = false;

    // Delete all jobs
    for (IndigoJob job : jobgraph.values()) {
      currentJob = job;

      try {
        client.deleteJob(job.getChronosJob().getName());
        LOG.debug("Deleted job on Chronos: name <{}>", currentJob.getChronosJob().getName());
      } catch (ChronosException ce) {
        // Just log the error
        String errorMsg = String.format("Failed to delete job <%s> on Chronos. Status Code: <%s>",
            currentJob.getChronosJob().getName(), ce.getStatus());
        LOG.error(errorMsg);
        failed = true;

        // Only throw exception in required
        if (failAtFirst) {
          // TODO use a custom exception ?
          throw new RuntimeException(errorMsg);
        }
      }
    }

    return !failed;

  }

  public static class IndigoJob {
    public enum JobDependencyType {
      START, INTERMEDIATE, END
    }

    private Job chronosJob;
    private Collection<IndigoJob> children = new ArrayList<>();
    private Collection<IndigoJob> parents = new ArrayList<>();

    public IndigoJob(Job chronosJob) {
      super();
      this.chronosJob = chronosJob;
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
      return "IndigoJob [chronosJob=" + chronosJob.getName();
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
  protected Multimap<JobDependencyType, IndigoJob> generateJobGraph(Deployment deployment,
      OneData odParams) {
    String deploymentId = deployment.getId();
    Multimap<JobDependencyType, IndigoJob> jobGraph = HashMultimap.create();
    Map<String, IndigoJob> jobs = new HashMap<String, ChronosServiceImpl.IndigoJob>();

    // Parse TOSCA template
    Map<String, NodeTemplate> nodes = null;
    try {
      String customizedTemplate = deployment.getTemplate();
      // FIXME TEMPORARY - Replace hard-coded properties in nodes
      customizedTemplate = replaceHardCodedParams(customizedTemplate, odParams);

      ArchiveRoot ar = toscaService.getArchiveRootFromTemplate(customizedTemplate).getResult();

      nodes = ar.getTopology().getNodeTemplates();
    } catch (IOException | ParsingException ex) {
      throw new OrchestratorException(ex);
    }

    // TODO Iterate on Chronos nodes and related dependencies (just ignore others - also if invalid
    // - for now)

    // Only create Indigo Jobs
    for (Map.Entry<String, NodeTemplate> node : nodes.entrySet()) {
      NodeTemplate nodeTemplate = node.getValue();
      String nodeName = node.getKey();
      if (isChronosNode(nodeTemplate)) {
        Job chronosJob = createJob(nodes, deploymentId, nodeName, nodeTemplate);

        IndigoJob job = new IndigoJob(chronosJob);
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

    // Update jobs hierarchy with roles to create the graph
    for (Map.Entry<String, IndigoJob> job : jobs.entrySet()) {
      IndigoJob indigoJob = job.getValue();

      // Switch node type
      if (indigoJob.getParents().isEmpty()) {
        // START node
        jobGraph.put(JobDependencyType.START, indigoJob);
      } else {
        if (indigoJob.getChildren().isEmpty()) {
          // END node
          jobGraph.put(JobDependencyType.END, indigoJob);
        } else {
          // INTERMEDIATE node
          jobGraph.put(JobDependencyType.INTERMEDIATE, indigoJob);
        }
      }
    }

    // Validate (no cycles!)
    // FIXME Shouldn't just return the topological order ?
    getJobsTopologicalOrder(jobGraph);

    return jobGraph;
  }

  /**
   * TEMPORARY method to replace hardcoded INDIGO params in TOSCA template (i.e. OneData) string.
   * 
   * @param template
   *          the string TOSCA template.
   * @param od
   *          the OneData settings.
   */
  protected String replaceHardCodedParams(String template, OneData od) {

    // Replace OneData properties
    template = template.replace("TOKEN_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getToken());
    template = template.replace("DATA_SPACE_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getSpace());
    template = template.replace("PATH_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getPath());
    template =
        template.replace("ONEDATA_PROVIDERS_TO_BE_SET_BY_THE_ORCHESTRATOR", od.getProvider());

    return template;
  }

  public static class OneData {
    private String token;
    private String space;
    private String path;
    private String provider;

    public OneData(String token, String space, String path, String provider) {
      super();
      this.token = token;
      this.space = space;
      this.path = path;
      this.provider = provider;
    }

    public String getToken() {
      return token;
    }

    public String getSpace() {
      return space;
    }

    public String getPath() {
      return path;
    }

    public String getProvider() {
      return provider;
    }

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
    // FIXME Implement parent extraction
    // Requirement parentNode = nodeTemplate.getRequirements().get("job_predecessor");

    // STUB !
    return nodeName.equals("chronos_job_upload") ? Lists.newArrayList("chronos_job") : null;
  }

  protected Job createJob(Map<String, NodeTemplate> nodes, String deploymentId, String nodeName,
      NodeTemplate nodeTemplate) {
    try {
      Job chronosJob = new Job();
      // Init job infos

      // Get the generated UUID for the node (in DB resource ?)
      // FIXME This is just for prototyping... Otherwise is madness!!
      Resource resourceJob =
          resourceRepository.findByToscaNodeNameAndDeployment_id(nodeName, deploymentId);

      chronosJob.setName(resourceJob.getId());

      // TODO Validation
      chronosJob.setRetries(Integer.parseInt(
          (String) toscaService.getNodePropertyValueByName(nodeTemplate, "retries").getValue()));

      chronosJob.setCommand(
          (String) toscaService.getNodePropertyValueByName(nodeTemplate, "command").getValue());

      // TODO Enable epsilon setting in TOSCA tplt ?
      chronosJob.setEpsilon("PT10S");

      ListPropertyValue inputUris =
          (ListPropertyValue) toscaService.getNodePropertyValueByName(nodeTemplate, "uris");
      if (inputUris != null && !inputUris.getValue().isEmpty()) {
        // Convert List<Object> to List<String>
        chronosJob.setUris(inputUris.getValue().stream()
            .map(e -> (String) ((PropertyValue<?>) e).getValue()).collect(Collectors.toList()));

      }

      List<EnvironmentVariable> envs = new ArrayList<>();
      ComplexPropertyValue inputEnvVars = ((ComplexPropertyValue) toscaService
          .getNodePropertyValueByName(nodeTemplate, "environment_variables"));
      if (inputEnvVars != null) {
        for (Map.Entry<String, Object> var : inputEnvVars.getValue().entrySet()) {
          EnvironmentVariable envVar = new EnvironmentVariable();
          envVar.setName(var.getKey());
          envVar.setValue(((PropertyValue<?>) var.getValue()).getValue().toString());
          envs.add(envVar);
        }
        chronosJob.setEnvironmentVariables(envs);
      }

      // Docker image
      // TODO Remove hard-coded?
      String supportedType = "tosca.artifacts.Deployment.Image.Container.Docker";
      DeploymentArtifact image;
      // <image> artifact available
      if (nodeTemplate.getArtifacts() == null
          || (image = nodeTemplate.getArtifacts().get("image")) == null) {
        throw new IllegalArgumentException(
            String.format("<image> artifact not found in node <%s> of type <%s>", nodeName,
                nodeTemplate.getType()));
      }

      // <image> artifact type check
      if (!image.getArtifactType().equals(supportedType)) {
        throw new IllegalArgumentException(String.format(
            "Unsupported artifact type for <image> artifact in node <%s> of type <%s>. Given <%s>, supported <%s>",
            nodeName, nodeTemplate.getType(),
            nodeTemplate.getArtifacts().get("image").getArtifactType(), supportedType));
      }

      // Requirements

      // Get Docker host dependency
      String dockerCapabilityName = "host";
      RelationshipTemplate dockerRelationship =
          toscaService.getRelationshipTemplateByCapabilityName(nodeTemplate.getRelationships(),
              dockerCapabilityName);
      Double dockerNumCpus = null;
      Double dockerMemSize = null;
      if (dockerRelationship != null) {
        String dockerNodeName = dockerRelationship.getTarget();
        NodeTemplate dockerNode = nodes.get(dockerNodeName);
        Capability dockerCapability = dockerNode.getCapabilities().get(dockerCapabilityName);
        dockerNumCpus = Double.parseDouble((String) toscaService
            .getCapabilityPropertyValueByName(dockerCapability, "num_cpus").getValue());

        // Converting Memory Size (as TOSCA scalar-unit.size)
        SizeType tmp = new SizeType();
        String memSizeRaw = (String) toscaService
            .getCapabilityPropertyValueByName(dockerCapability, "mem_size").getValue();
        dockerMemSize = tmp.parse(memSizeRaw).convert("MB"); // Chronos wants MB
      }

      Container container = new Container();
      container.setType("DOCKER");
      container.setImage((String) nodeTemplate.getArtifacts().get("image").getFile());
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
