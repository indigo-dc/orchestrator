/*
 * Copyright © 2019 I.N.F.N.
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

import it.infn.ba.deep.qcg.client.Qcg;
import it.infn.ba.deep.qcg.client.model.Job;
import it.infn.ba.deep.qcg.client.model.JobDescription;
import it.infn.ba.deep.qcg.client.model.JobDescriptionExecution;
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
import it.reply.orchestrator.dto.qcg.QcgJob;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Task;
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
import it.reply.orchestrator.utils.ToscaUtils;
import it.reply.orchestrator.utils.ToscaConstants.Nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
      OidcTokenId requestedWithToken,
      ThrowingFunction<Qcg, R, QcgException> function) throws QcgException {
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
    // Update status of the deployment - if not already done (remember the Iterative mode)
    if (deployment.getTask() != Task.DEPLOYER) {
      deployment.setTask(Task.DEPLOYER);
    }
    if (deployment.getEndpoint() == null) {
      deployment.setEndpoint("<NO_ENDPOINT>");
    }

    // TODO Replace attribute, inputs, temporary-hard-coded properties in the TOSCA template
   
    QcgJobsOrderedIterator topologyIterator = deploymentMessage.getQcgJobsIterator();
    // Create nodes iterator if not done yet
    if (topologyIterator == null) {
      topologyIterator = getJobsTopologicalOrder(deploymentMessage, deployment);
      // Create topological order
      deploymentMessage.setQcgJobsIterator(topologyIterator);
    }

    if (topologyIterator.hasNext()) {
      DeepJob currentJob = topologyIterator.next();
      LOG.debug("Creating job {} on Qcg ({}/{})",
          currentJob.getQcgJob().getId(),
          topologyIterator.currentIndex() + 1,
          topologyIterator.getSize());
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      Job updated = createJobOnQcg(cloudProviderEndpoint, requestedWithToken, currentJob);
      // update object with deployment data
      try {
    	  ModelUtils.updateJob(updated, currentJob.getQcgJob());
      } catch (QcgException ex) {
    	  // TODO cannot update job, do something? 
      }
      deployment.setEndpoint(currentJob.getQcgJob().getId());
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
   *  Creates a Job on Qcg.
   *
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Qcg instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param job
   *          the IndigoJob to be created
   */
  protected Job createJobOnQcg(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, DeepJob job) {
    // Create jobs based on the topological order
    try {
	    LOG.debug("Creating scheduled Qcg job\n{}", job);
	    
	    Job created = executeWithClientForResult(cloudProviderEndpoint, requestedWithToken,
	        client -> client.createJob(job.getQcgJob().getDescription()));
	    return created;
    } catch (QcgException exception) { // Qcg job launch error
      throw new DeploymentException(
          "Failed to launch job <" + job.getQcgJob().getId() + "> on Qcg", exception);
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
      LOG.debug("Polling job {} on Qcg ({}/{})",
          currentJob.getQcgJob().getId(),
          topologyIterator.currentIndex() + 1,
          topologyIterator.getSize());
      final OidcTokenId requestedWithToken = deploymentMessage.getRequestedWithToken();
      CloudProviderEndpoint cloudProviderEndpoint = deployment.getCloudProviderEndpoint();
      boolean jobIsCompleted = checkJobsOnQcg(cloudProviderEndpoint, requestedWithToken,
          currentJob.getQcgJob().getId());
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
      LOG.debug("Polling of next job on Qcg scheduled");
    }
    return noMoreJob;
  }

  @Override
  public void cleanFailedDeploy(DeploymentMessage deploymentMessage) {
    // DO NOTHING
  }

  /**
   * Gets the Job status.
   *
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Qcg instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param jobName
   *     the name of the Qcg job
   * @return the optional {@link Job}.
   */
  protected boolean checkJobsOnQcg(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, String jobId) {

    Job updatedJob = findJobOnQcg(cloudProviderEndpoint, requestedWithToken, jobId)
        .orElseThrow(() -> new DeploymentException("Job " + jobId + " doesn't exist on Qcg"));

    LOG.debug("Qcg job {} current status:\n{}", jobId, updatedJob);
    JobState jobState = getLastState(updatedJob);
    LOG.debug("Status of Qcg job {} is: {}", jobId, jobState);

    switch (jobState) {
      case FRESH:
        LOG.debug("Qcg job {} not ready yet", jobId);
        return false;
      case SUCCESS:
        LOG.debug("Qcg job {} is ready", jobId);
        return true;
      case FAILURE:
        throw new DeploymentException("Qcg job " + jobId + " failed to execute");
      default:
        throw new DeploymentException("Unknown Qcg job status: " + jobState);
    }
  }

  private void updateResource(Deployment deployment, DeepJob job, NodeStates state) {
	  
    resourceRepository
        .findByToscaNodeNameAndDeployment_id(job.getToscaNodeName(), deployment.getId())
        .forEach(resource -> resource.setState(state));
  }

  /**
   * Gets the Job status.
   *
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Qcg instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param jobName
   *     the name of the Qcg job
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
    // Nothing to wait here... All the jobs are delete immediately.
    return true;
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
   * Creates the {@link QcgJob} graph based on the given {@link Deployment} (the TOSCA template
   * is parsed).
   *
   * @param deployment
   *          the input deployment.
   * @return the job graph.
   */
  protected QcgJobsOrderedIterator getJobsTopologicalOrder(DeploymentMessage deploymentMessage,
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

    List<NodeTemplate> orderedQcgJobs = CommonUtils
        .iteratorToStream(orderIterator)
        .filter(node -> toscaService.isOfToscaType(node, 
            ToscaConstants.Nodes.Types.QCG))
        .collect(Collectors.toList());
    
    Map<String, Resource> resources = deployment
            .getResources()
            .stream()
            .filter(resource -> toscaService.isOfToscaType(resource,
                ToscaConstants.Nodes.Types.QCG))
            .collect(Collectors.toMap(Resource::getToscaNodeName, res -> res));
    
    LinkedHashMap<String, QcgJob> jobs = new LinkedHashMap<>();
    
    List<DeepJob> deepJobs = new ArrayList<>();
    
    for (NodeTemplate qcgNode : orderedQcgJobs) {
      Resource jobResource = resources.get(qcgNode.getName());
	    String id = Optional
	        .ofNullable(jobResource.getIaasId())
	        .orElseGet(() -> {
	          jobResource.setIaasId(jobResource.getId());
	          return jobResource.getIaasId();
	        });  
	    
      QcgJob qcgJob = buildTask(graph, qcgNode, id);
      jobs.put(qcgNode.getName(), qcgJob);
      
      Job job = generateExternalTaskRepresentation(qcgJob);
      DeepJob deepJob = new DeepJob(job, qcgNode.getName());
      deepJobs.add(deepJob);
    }

    return new QcgJobsOrderedIterator(deepJobs);
  }
  
  /**
   * Fill node from template values
   *  
   * @param graph
   * @param taskNode
   * @param taskId
   * @return
   */
  public QcgJob buildTask(DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph,
      NodeTemplate taskNode, String taskId) {
	  
    QcgJob qcgjob = new QcgJob();
	  
    // orchestrator internal
    qcgjob.setTaskId(taskId);
    
    //TODO  MAP ALL PROPETIES FROM TOSCA
    
    // property: environment_variables
    ToscaUtils
        .extractMap(taskNode.getProperties(), "environment_variables", String.class::cast)
        .ifPresent(qcgjob::setEnvironment);
    
    // property: executable
    ToscaUtils
      .extractScalar(taskNode.getProperties(), "executable")
      .map(String::trim)
      .ifPresent(qcgjob::setExecutable);      

  	if ("".equals(qcgjob.getExecutable())) { // it must be either null or not empty
  	  throw new ToscaException(String.format(
  	      "<executable> property of node <%s> must not be an empty string", taskNode.getName()));
  	}

	  //property: directory
    ToscaUtils
        .extractScalar(taskNode.getProperties(), "directory")
        .ifPresent(qcgjob::setDirectory);      
    
    //property: arguments
    ToscaUtils
        .extractList(taskNode.getProperties(), "arguments", String.class::cast)
        .ifPresent(qcgjob::setArgs);   
    
    // property: schema
    ToscaUtils
        .extractScalar(taskNode.getProperties(), "schema")
        .ifPresent(qcgjob::setSchema);      
    
    // property: note
    ToscaUtils
        .extractScalar(taskNode.getProperties(), "note")
        .ifPresent(qcgjob::setNote);    	
    
    return qcgjob;
  }  

  /**
   * Convert internal QcgJob object to qcg-client Job object
   * @param qcgjob
   * @return
   */
  @SuppressWarnings("unchecked")
  protected Job generateExternalTaskRepresentation(QcgJob qcgjob) {
	  
    Job job = new Job();
    
    job.setId(qcgjob.getId());
    if (qcgjob.getAttributes() != null)
    	job.setAttributes((HashMap<String,String>)((HashMap<String, String>)qcgjob.getAttributes()).clone());
    job.setUser(qcgjob.getUser());
    job.setState(qcgjob.getState());
    job.setOperation(qcgjob.getOperation());
    job.setNote(qcgjob.getNote());
    
    JobDescription description = new JobDescription();
    
  	JobDescriptionExecution execution = new JobDescriptionExecution();
  	execution.setExecutable(qcgjob.getExecutable());    
  	execution.setDirectory(qcgjob.getDirectory());
  	if (qcgjob.getArgs()!=null)
  	  execution.setArgs((ArrayList<String>)((ArrayList<String>)qcgjob.getArgs()).clone());
  	if (qcgjob.getEnvironment() != null)
  	  execution.setEnvironment((HashMap<String,String>)((HashMap<String,String>)qcgjob.getEnvironment()).clone());
  	
  	//default remove policy
  	JobWorkingDirectoryPolicy directory_policy = new JobWorkingDirectoryPolicy();
  	directory_policy.setCreate(RemoveConditionCreateMode.OVERWRITE);
  	directory_policy.setRemove(RemoveConditionWhen.NEVER);	
  	
  	execution.setDirectory_policy(directory_policy);
  
  	description.setSchema(qcgjob.getSchema());
  	description.setExecution(execution);
  	description.setNote(qcgjob.getNote());
  	job.setDescription(description);
    
    job.setOperation_start(qcgjob.getOperation_start());
    job.setResource(qcgjob.getResource());
    job.setQueue(qcgjob.getQueue());
    job.setLocal_user(qcgjob.getLocal_user());
    job.setLocal_group(qcgjob.getLocal_group());
    job.setLocal_id(qcgjob.getLocal_id());
    job.setSubmit_time(qcgjob.getSubmit_time());
    job.setStart_time(qcgjob.getStart_time());
    job.setFinish_time(qcgjob.getFinish_time());
    job.setUpdated_time(qcgjob.getUpdated_time());
    job.setEta(qcgjob.getEta());
    job.setNodes(qcgjob.getNodes());
    job.setCpus(qcgjob.getCpus());
    job.setExit_code(qcgjob.getExit_code());
    job.setErrors(qcgjob.getErrors());
    job.setResubmit(qcgjob.getResubmit());
    job.setWork_dir(qcgjob.getWork_dir());
    job.setCreated_work_dir(qcgjob.getCreated_work_dir());
    job.setLast_seen(qcgjob.getLast_seen());
    
    return job;
  }  
  
  /**
   * Deletes all the deployment jobs from Qcg. <br/>
   * Also logs possible errors and updates the deployment status.
   * 
   * @param deploymentMessage
   *          the deployment message.
   * @return <tt>true</tt> if all jobs have been deleted, <tt>false</tt> otherwise.
   */
  @Override
  public boolean doUndeploy(DeploymentMessage deploymentMessage) {
    // Delete all Jobs on Qcg
    Deployment deployment = getDeployment(deploymentMessage);
    Iterator<Resource> topologyIterator = deployment
        .getResources()
        .stream()
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
      String jobId = deployment.getEndpoint();//  qcgResource.getId(); // iaasId could have not been set yet
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
   * @param cloudProviderEndpoint
   *     the {@link CloudProviderEndpoint} of the Qcg instance
   * @param requestedWithToken
   *     the token ID of the request
   * @param jobIf
   *     the Id of the Qcg job
   */
  
  protected void deleteJobsOnQcg(CloudProviderEndpoint cloudProviderEndpoint,
      OidcTokenId requestedWithToken, String jobId) {
	  
    try {
      executeWithClient(cloudProviderEndpoint, requestedWithToken,
          client -> client.deleteJob(jobId));
    } catch (QcgException ex) {
      // Qcg API hack to avoid error 400 if the job to delete does not exist or cannot be deleted
      // if in state FINISHED, FAILED
      if (ex.getStatus() != 400 && ex.getStatus() != 404) {
        throw new DeploymentException("Failed to delete job " + jobId + " on Qcg", ex);
      }
    }
    
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

    ArchiveRoot ar = toscaService.parseTemplate(deployment.getTemplate());

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
   * Computes the Qcg job's state based on current success and error count.
   * 
   * @param job
   *          the {@link Job}.
   * @return the {@link JobState}.
   */
  @VisibleForTesting
  protected static JobState getLastState(Job job) {
    
  	//TODO verify logic!
  	  
  	if (job.getErrors() == null || job.getErrors().isEmpty() || job.getErrors() == "null") {
  		if (job.getResubmit() > 0) {
  			return JobState.SUCCESS;
  		} else {
  			return JobState.FRESH;
  		}
  	} else {
  	    return JobState.FAILURE;
  	}
  }

  public Optional<String> getAdditionalErrorInfoInternal(DeploymentMessage deploymentMessage) {
    return Optional.empty();
  }

  protected QcgJob createInternalTaskRepresentation() {
    return new QcgJob();
  }

}
