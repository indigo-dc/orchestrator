package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ResourceImpl implements ResourceService {

  @Autowired
  private ResourceRepository resourceRepository;

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Override
  public Page<Resource> getResources(String deploymentId, Pageable pageable) {
    if (deploymentRepository.exists(deploymentId)) {
      return resourceRepository.findByDeployment_id(deploymentId, pageable);
    } else {
      throw new NotFoundException("The deployment <" + deploymentId + "> doesn't exist");
    }
  }

  @Override
  public Resource getResource(String uuid, String deploymentId) {
    Resource resource = resourceRepository.findByIdAndDeployment_id(uuid, deploymentId);
    if (resource != null) {
      return resource;
    } else {
      throw new NotFoundException(
          "The resource <" + uuid + "> in deployment <" + deploymentId + "> doesn't exist");
    }
  }

}
