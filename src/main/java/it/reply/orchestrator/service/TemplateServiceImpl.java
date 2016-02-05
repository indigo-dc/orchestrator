package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemplateServiceImpl implements TemplateService {

  @Autowired
  private DeploymentRepository deploymentRepository;

  @Override
  public String getTemplate(String uuid) {
    Deployment deployment = deploymentRepository.findOne(uuid);
    if (deployment != null) {
      return deployment.getTemplate();
    } else {
      throw new NotFoundException("The deployment <" + uuid + "> doesn't exist");
    }
  }

}
