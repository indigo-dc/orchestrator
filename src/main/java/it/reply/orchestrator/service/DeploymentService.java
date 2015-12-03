package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.common.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;

import java.util.Map;

public interface DeploymentService {

  public Map<String, Deployment> getDeployments();

  public Deployment getDeployment(String id);

  public Deployment createDeployment(DeploymentRequest request);

  public void deleteDeployment(String id);
}
