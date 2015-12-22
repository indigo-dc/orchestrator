package it.reply.orchestrator.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;

public interface DeploymentService {

  public Page<Deployment> getDeployments(Pageable pageable);

  public Deployment getDeployment(String id);

  public Deployment createDeployment(DeploymentRequest request);

  public void deleteDeployment(String id);
}
