package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeploymentService {

	public static final String WF_PARAM_DEPLOYMENT_ID = "DEPLOYMENT_ID";
	public static final String WF_PARAM_DEPLOYMENT_TYPE = "DEPLOYMENT_TYPE";
	public static final String DEPLOYMENT_TYPE_CHRONOS = "CHRONOS";
	public static final String DEPLOYMENT_TYPE_TOSCA = "TOSCA";

	public Page<Deployment> getDeployments(Pageable pageable);

	public Deployment getDeployment(String id);

	public Deployment createDeployment(DeploymentRequest request);

	public void updateDeployment(String id, DeploymentRequest request);

	public void deleteDeployment(String id);
}
