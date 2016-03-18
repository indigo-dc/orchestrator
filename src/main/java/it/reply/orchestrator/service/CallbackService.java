package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Deployment;

public interface CallbackService {

  public boolean doCallback(String deploymentId);

  public boolean doCallback(Deployment deployment);

}
