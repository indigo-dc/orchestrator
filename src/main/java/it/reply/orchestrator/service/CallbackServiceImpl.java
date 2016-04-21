package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CallbackServiceImpl implements CallbackService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Autowired
  private DeploymentResourceAssembler deploymentResourceAssembler;

  @Autowired
  private RestTemplate restTemplate;

  @Override
  public boolean doCallback(String deploymentId) {
    Deployment deployment = deploymentRepository.findOne(deploymentId);
    return doCallback(deployment);
  }

  @Override
  public boolean doCallback(Deployment deployment) {
    if (deployment.getCallback() != null) {
      DeploymentResource deploymentResource = deploymentResourceAssembler.toResource(deployment);
      ResponseEntity<?> response = restTemplate.postForEntity(deployment.getCallback(),
          deploymentResource, Object.class);
      return response.getStatusCode().is2xxSuccessful();
    }
    return false;
  }

}
