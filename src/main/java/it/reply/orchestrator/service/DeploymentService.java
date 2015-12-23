package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.request.DeploymentRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeploymentService {

  public Page<Deployment> getDeployments(Pageable pageable);

  public Deployment getDeployment(String id);

  public Deployment createDeployment(DeploymentRequest request);

  public void deleteDeployment(String id);
}
